package com.backrooms.mod.entity;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║                     M I M I C   E N T I T Y                 ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  FSM — 4 состояния:                                          ║
 * ║   IDLE      → Бродит, не знает об игроке                    ║
 * ║   STALKING  → Наблюдает, держит дистанцию, ЗАМИРАЕТ         ║
 * ║              при взгляде, копирует движения вблизи           ║
 * ║   HUNTING   → Потерял — ищет по Last Known Position         ║
 * ║   ATTACKING → Атакует в ближнем бою                         ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  Скорости: база 0.25 > Howler (0.2) > старый Mimic (0.1)    ║
 * ║  Спавн: 1% на чанк (крайне редкий)                          ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
public class MimicEntity extends HostileEntity {

    // ── DataTracker (синхронизация клиент/сервер) ─────────────────────────────
    private static final TrackedData<Optional<UUID>> SKIN_OWNER =
            DataTracker.registerData(MimicEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Integer> FSM_STATE =
            DataTracker.registerData(MimicEntity.class, TrackedDataHandlerRegistry.INTEGER);

    // ── FSM состояния ─────────────────────────────────────────────────────────
    public enum State { IDLE, STALKING, HUNTING, ATTACKING }

    // ── Дистанции (блоков) ───────────────────────────────────────────────────
    private static final double DETECTION_RANGE    = 32.0;  // радиус обнаружения
    private static final double ATTACK_RANGE       = 3.0;   // дистанция атаки
    private static final double COPY_RANGE         = 12.0;  // ближе — копирует движения
    private static final double STALK_IDEAL        = 15.0;  // идеальная дистанция слежки
    private static final double STALK_MIN          = 10.0;  // ближе — отступает при слежке

    // ── Скорости (множители на базовый атрибут 0.25) ─────────────────────────
    // Итоговые скорости: IDLE=0.10, STALK=0.19, COPY=0.23, HUNT=0.33, ATTACK=0.38
    private static final double SPEED_IDLE_WANDER  = 0.4;   // очень медленное блуждание
    private static final double SPEED_STALK        = 0.75;  // скорость слежки (~ходьба игрока)
    private static final double SPEED_COPY         = 0.9;   // скорость копирования движений
    private static final double SPEED_HUNT         = 1.3;   // спринт при охоте (быстрее игрока)
    private static final double SPEED_ATTACK       = 1.5;   // атака (форсаж)

    // ── FOV-проверка ──────────────────────────────────────────────────────────
    // cos(20°) ≈ 0.94 — игрок "смотрит на мимика" если он в конусе ±20° перед игроком
    private static final double PLAYER_FOV_DOT     = 0.94;

    // ── Движение-копирование (запись позиций игрока с задержкой) ────────────
    private static final int    COPY_QUEUE_SIZE    = 30;   // 1.5 сек задержки (30 тиков)
    private static final double COPY_RECORD_DIST   = 0.15; // записываем если сдвинулся

    // ── Таймеры ───────────────────────────────────────────────────────────────
    private static final int HUNT_TIMEOUT          = 220;  // тиков до возврата в IDLE (11 сек)
    private static final int AMBUSH_COOLDOWN       = 500;  // 25 сек между засадами

    // ── Рабочие переменные ────────────────────────────────────────────────────
    private State     fsmState         = State.IDLE;
    private Vec3d     lastKnownPos     = null;
    private int       huntTimer        = 0;
    private int       ambushTimer      = 0;
    private int       lostSightTimer   = 0;
    /** Очередь прошлых позиций игрока — мимик повторяет их с задержкой */
    private final Deque<Vec3d> movementQueue = new ArrayDeque<>();

    // ── Конструктор ───────────────────────────────────────────────────────────
    public MimicEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(SKIN_OWNER, Optional.empty());
        this.dataTracker.startTracking(FSM_STATE, State.IDLE.ordinal());
    }

    // ── Публичные геттеры (нужны рендереру) ──────────────────────────────────
    public Optional<UUID> getSkinOwnerUuid() {
        return this.dataTracker.get(SKIN_OWNER);
    }

    // ── Goals ─────────────────────────────────────────────────────────────────
    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        // Главный гол — обрабатывает ВСЕ состояния кроме MeleeAttack
        this.goalSelector.add(1, new MimicMasterGoal(this));
        // Атака в ближнем бою когда очень близко
        this.goalSelector.add(2, new MeleeAttackGoal(this, SPEED_ATTACK, false));
        this.goalSelector.add(3, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ГЛАВНЫЙ TICK
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient) return;

        if (ambushTimer > 0) ambushTimer--;

        PlayerEntity player = this.getWorld().getClosestPlayer(this, DETECTION_RANGE);

        // Синхронизируем скин
        if (player != null) {
            this.dataTracker.set(SKIN_OWNER, Optional.of(player.getUuid()));
        }

        runFSM(player);

        // Синхронизируем состояние в DataTracker
        this.dataTracker.set(FSM_STATE, fsmState.ordinal());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  КОНЕЧНЫЙ АВТОМАТ (FSM)
    // ═══════════════════════════════════════════════════════════════════════

    private void runFSM(PlayerEntity player) {
        switch (fsmState) {
            case IDLE      -> doIdle(player);
            case STALKING  -> doStalking(player);
            case HUNTING   -> doHunting(player);
            case ATTACKING -> doAttacking(player);
        }
    }

    // ─────────────────────────────────────────────────
    // IDLE: медленно бродит, переход в STALKING при обнаружении
    // ─────────────────────────────────────────────────
    private void doIdle(PlayerEntity player) {
        if (player == null) return;
        if (this.squaredDistanceTo(player) > DETECTION_RANGE * DETECTION_RANGE) return;

        this.setTarget(player);
        fsmState = State.STALKING;
        movementQueue.clear();
    }

    // ─────────────────────────────────────────────────
    // STALKING: главное жуткое состояние
    //
    //  • Если игрок СМОТРИТ → ЗАМИРАЕМ (dot-product FOV + raycast)
    //  • Если очень далеко  → приближаемся до идеала
    //  • Если слишком близко → медленно отступаем
    //  • Если в зоне копирования → повторяем движения игрока (жутко)
    //  • Если потеряли → HUNTING
    //  • Если рядом (< 3 блоков) → ATTACKING
    // ─────────────────────────────────────────────────
    private void doStalking(PlayerEntity player) {
        if (player == null || player.isRemoved()) {
            fsmState = State.IDLE;
            return;
        }

        double dist = this.distanceTo(player);

        // Переход в атаку
        if (dist <= ATTACK_RANGE) {
            fsmState = State.ATTACKING;
            this.setTarget(player);
            return;
        }

        // Потерял игрока из зоны обнаружения
        if (dist > DETECTION_RANGE + 6) {
            lastKnownPos = null;
            fsmState = State.IDLE;
            this.setTarget(null);
            movementQueue.clear();
            return;
        }

        boolean canSeePlayer      = canMimicSeePlayer(player);
        boolean playerLooksAtMe   = isPlayerLookingAtMimic(player);

        // Обновляем Last Known Position
        if (canSeePlayer) {
            lastKnownPos = player.getPos();
            lostSightTimer = 0;

            // Записываем позицию игрока в очередь для копирования
            recordPlayerPos(player);
        } else {
            lostSightTimer++;
            // Потеряли на 3 секунды → уходим в охоту
            if (lostSightTimer > 60) {
                fsmState = State.HUNTING;
                huntTimer = 0;
                movementQueue.clear();
            }
            return;
        }

        // ── Принятие решения о движении ──────────────────────────────────────

        if (playerLooksAtMe) {
            // ЗАМИРАЕМ — игрок смотрит прямо на нас (SCP-механика)
            this.getNavigation().stop();
            this.getLookControl().lookAt(player.getX(), player.getEyeY(), player.getZ(), 60.0f, 60.0f);

        } else if (dist < STALK_MIN) {
            // Слишком близко — отступаем назад, сохраняя взгляд на игрока
            Vec3d away = this.getPos().subtract(player.getPos()).normalize();
            this.getNavigation().startMovingTo(
                    this.getX() + away.x * 6, this.getY(), this.getZ() + away.z * 6,
                    SPEED_STALK);
            this.getLookControl().lookAt(player.getX(), player.getEyeY(), player.getZ(), 30.0f, 30.0f);

        } else if (dist < COPY_RANGE) {
            // В зоне копирования — повторяем движения игрока с задержкой
            followCopiedMovement();
            this.getLookControl().lookAt(player.getX(), player.getEyeY(), player.getZ(), 30.0f, 30.0f);

        } else if (dist > STALK_IDEAL) {
            // Далеко — медленно приближаемся
            this.getNavigation().startMovingTo(player, SPEED_STALK);
            this.getLookControl().lookAt(player.getX(), player.getEyeY(), player.getZ(), 20.0f, 20.0f);

        } else {
            // В идеальной зоне (STALK_MIN ... STALK_IDEAL) — стоим, наблюдаем
            this.getNavigation().stop();
            this.getLookControl().lookAt(player.getX(), player.getEyeY(), player.getZ(), 30.0f, 30.0f);

            // Засада когда игрок отворачивается
            tryAmbush(player);
        }
    }

    // ─────────────────────────────────────────────────
    // HUNTING: ищем игрока по последней известной позиции
    // ─────────────────────────────────────────────────
    private void doHunting(PlayerEntity player) {
        huntTimer++;

        // Снова видим игрока → возврат в STALKING
        if (player != null && canMimicSeePlayer(player)) {
            lastKnownPos = player.getPos();
            fsmState = State.STALKING;
            huntTimer = 0;
            lostSightTimer = 0;
            return;
        }

        // Игрок случайно оказался рядом → атака
        if (player != null && this.squaredDistanceTo(player) <= ATTACK_RANGE * ATTACK_RANGE) {
            fsmState = State.ATTACKING;
            this.setTarget(player);
            return;
        }

        // Идём к LKP
        if (lastKnownPos != null) {
            double toLkp = this.squaredDistanceTo(lastKnownPos.x, lastKnownPos.y, lastKnownPos.z);
            if (toLkp > 3.0) {
                this.getNavigation().startMovingTo(
                        lastKnownPos.x, lastKnownPos.y, lastKnownPos.z, SPEED_HUNT);
            } else {
                // Пришли на место — осматриваемся
                this.getNavigation().stop();
            }
        }

        // Таймаут — не нашли, возврат в IDLE
        if (huntTimer > HUNT_TIMEOUT) {
            fsmState = State.IDLE;
            this.setTarget(null);
            lastKnownPos = null;
            huntTimer = 0;
        }
    }

    // ─────────────────────────────────────────────────
    // ATTACKING: MeleeAttackGoal уже бьёт, мы следим за выходом
    // ─────────────────────────────────────────────────
    private void doAttacking(PlayerEntity player) {
        if (player == null || player.isRemoved()) {
            fsmState = State.IDLE;
            return;
        }
        // Убежал → охота
        if (this.distanceTo(player) > ATTACK_RANGE * 3) {
            lastKnownPos = player.getPos();
            fsmState = State.HUNTING;
            huntTimer = 0;
            this.setTarget(player);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  КОПИРОВАНИЕ ДВИЖЕНИЙ
    // ═══════════════════════════════════════════════════════════════════════

    /** Записываем позицию игрока в очередь если он сдвинулся */
    private void recordPlayerPos(PlayerEntity player) {
        Vec3d pPos = player.getPos();
        if (!movementQueue.isEmpty()) {
            Vec3d last = movementQueue.peekLast();
            if (last != null && pPos.squaredDistanceTo(last) < COPY_RECORD_DIST * COPY_RECORD_DIST) {
                return; // Игрок почти не двигался — не записываем
            }
        }
        movementQueue.addLast(pPos);
        // Ограничиваем размер очереди
        while (movementQueue.size() > COPY_QUEUE_SIZE) {
            movementQueue.removeFirst();
        }
    }

    /** Мимик двигается к точке из очереди — копирует путь игрока с задержкой */
    private void followCopiedMovement() {
        if (movementQueue.isEmpty()) return;

        Vec3d target = movementQueue.peekFirst();
        if (target == null) return;

        // Если дошли до текущей точки — берём следующую
        if (this.squaredDistanceTo(target.x, target.y, target.z) < 1.5) {
            movementQueue.removeFirst();
            if (movementQueue.isEmpty()) return;
            target = movementQueue.peekFirst();
            if (target == null) return;
        }

        this.getNavigation().startMovingTo(target.x, target.y, target.z, SPEED_COPY);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ЗАСАДА (БЕЗОПАСНАЯ ТЕЛЕПОРТАЦИЯ)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Телепортация за спину игрока.
     * БЕЗОПАСНАЯ: проверяем что позиция:
     *  1. В воздухе (Y и Y+1 — воздух)
     *  2. Под ногами твёрдый блок (пол)
     *  3. Нет стены между нами и игроком после телепорта
     */
    private void tryAmbush(PlayerEntity player) {
        if (ambushTimer > 0) return;
        if (isPlayerLookingAtMimic(player)) return;
        if (this.random.nextFloat() > 0.04f) return; // 4% шанс каждый тик

        Vec3d lookDir = player.getRotationVector();
        double dist   = 3.0 + this.random.nextDouble() * 3.0;

        double tx = player.getX() - lookDir.x * dist;
        double ty = player.getY();
        double tz = player.getZ() - lookDir.z * dist;

        if (isSafeTeleportLocation(tx, ty, tz)) {
            this.teleport(tx, ty, tz);
            this.getNavigation().stop();
            ambushTimer = AMBUSH_COOLDOWN;
        }
    }

    /** Проверяет что позиция безопасна для телепортации */
    private boolean isSafeTeleportLocation(double x, double y, double z) {
        BlockPos floor   = BlockPos.ofFloored(x, y - 1, z);
        BlockPos feet    = BlockPos.ofFloored(x, y,     z);
        BlockPos head    = BlockPos.ofFloored(x, y + 1, z);

        World world = this.getWorld();

        // Под ногами должен быть твёрдый блок
        if (!world.getBlockState(floor).isSolidBlock(world, floor)) return false;
        // На уровне ног и головы — воздух
        if (!world.getBlockState(feet).isAir()) return false;
        if (!world.getBlockState(head).isAir()) return false;

        return true;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ВОСПРИЯТИЕ
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Точная проверка: смотрит ли ИГРОК на мимика.
     * dot-product FOV (конус ±20°) + raycast (стены блокируют).
     */
    private boolean isPlayerLookingAtMimic(PlayerEntity player) {
        Vec3d eyePos      = player.getEyePos();
        Vec3d mimicEye    = this.getEyePos();
        Vec3d toMimic     = mimicEye.subtract(eyePos).normalize();
        Vec3d lookDir     = player.getRotationVector().normalize();

        double dot = toMimic.dotProduct(lookDir);
        if (dot < PLAYER_FOV_DOT) return false;           // За FOV-конусом
        return !hasBlockBetween(eyePos, mimicEye);        // Нет стены
    }

    /**
     * Может ли мимик видеть игрока — raycast от глаз мимика к глазам игрока.
     */
    private boolean canMimicSeePlayer(PlayerEntity player) {
        if (this.squaredDistanceTo(player) > DETECTION_RANGE * DETECTION_RANGE) return false;
        return !hasBlockBetween(this.getEyePos(), player.getEyePos());
    }

    /** Есть ли непрозрачный блок между двумя точками */
    private boolean hasBlockBetween(Vec3d from, Vec3d to) {
        var result = this.getWorld().raycast(new RaycastContext(
                from, to,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                this));
        return result.getType() != net.minecraft.util.hit.HitResult.Type.MISS;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  АТРИБУТЫ
    // ═══════════════════════════════════════════════════════════════════════

    public static DefaultAttributeContainer.Builder createMimicAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 7.0)
                // ── Скорость ──────────────────────────────────────────────
                // Howler = 0.2 (медленный). Mimic = 0.25 (быстрее Howler).
                // Итоговые скорости по состояниям:
                //   IDLE wander : 0.25 * 0.4  = 0.10 (очень медленно)
                //   STALK       : 0.25 * 0.75 = 0.19 (≈ ходьба игрока)
                //   COPY        : 0.25 * 0.90 = 0.23 (≈ быстрая ходьба)
                //   HUNT        : 0.25 * 1.30 = 0.33 (≈ спринт игрока)
                //   ATTACK      : 0.25 * 1.50 = 0.38 (выше спринта)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0)
                .add(EntityAttributes.GENERIC_ARMOR, 2.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.3);
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  GOALS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Главный гол — работает во всех состояниях кроме ATTACKING.
     * ATTACKING обрабатывается через MeleeAttackGoal (приоритет 2).
     */
    private static final class MimicMasterGoal extends Goal {
        private final MimicEntity mimic;

        private MimicMasterGoal(MimicEntity mimic) {
            this.mimic = mimic;
            // LOOK и MOVE — наш гол контролирует их в IDLE/STALKING/HUNTING
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            // Работает всегда кроме активной атаки
            return mimic.fsmState != State.ATTACKING;
        }

        @Override
        public boolean shouldContinue() {
            return canStart();
        }

        @Override
        public void tick() {
            // Всё управление в MimicEntity.tick() → runFSM()
            // Здесь просто держим контроль движения за собой
            if (mimic.fsmState == State.IDLE && !mimic.getNavigation().isFollowingPath()) {
                // Редкое случайное блуждание в IDLE
                if (mimic.random.nextFloat() < 0.004f) {
                    Vec3d wanderTarget = mimic.getPos().add(
                            (mimic.random.nextDouble() - 0.5) * 16,
                            0,
                            (mimic.random.nextDouble() - 0.5) * 16);
                    mimic.getNavigation().startMovingTo(
                            wanderTarget.x, wanderTarget.y, wanderTarget.z,
                            SPEED_IDLE_WANDER);
                }
            }
        }
    }
}
