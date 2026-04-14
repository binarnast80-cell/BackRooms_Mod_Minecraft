package com.backrooms.mod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

/**
 * Mimic — криповый монстр Backrooms, который пытается копировать движения игрока
 * и подкрадываться незаметно, прежде чем внезапно атаковать.
 */
public class MimicEntity extends HostileEntity {
    private static final TrackedData<Optional<UUID>> SKIN_OWNER = DataTracker.registerData(MimicEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final int COPY_DELAY_TICKS = 12;
    private static final double MOVEMENT_RECORD_DISTANCE = 0.2;
    private static final float MOVEMENT_RECORD_ANGLE = 3.0f;
    private static final double SUDDEN_ATTACK_RANGE = 2.5;
    private static final int TELEPORT_CHECK_INTERVAL = 100;
    private static final int AMBUSH_RANGE = 20;
    private static final int AMBUSH_COOLDOWN = 200;
    private static final int LUNGE_COOLDOWN = 40;
    private static final double LUNGE_SPEED_MULTIPLIER = 1.8;
    private static final double AMBUSH_DISTANCE_MIN = 6.0;

    private final Deque<PlayerSnapshot> playerMovementQueue = new ArrayDeque<>();
    private int ticksSinceLastTeleport = 0;
    private int ambushCooldown = 0;
    private int lungeCooldown = 0;
    private boolean shouldTeleport = false;

    public MimicEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(SKIN_OWNER, Optional.empty());
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new SuddenAttackGoal(this, 8.0, 1.0));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.add(3, new CopyPlayerMovementGoal(this, 1.0));
        this.goalSelector.add(4, new WatchingGoal(this));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.75));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 20.0f));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.getWorld().isClient) {
            if (this.ambushCooldown > 0) {
                this.ambushCooldown--;
            }
            if (this.lungeCooldown > 0) {
                this.lungeCooldown--;
            }
            this.ticksSinceLastTeleport++;

            if (this.getTarget() instanceof PlayerEntity player) {
                setSkinOwner(player);

                if (shouldRecordMovement(player)) {
                    recordPlayerMovement(player);
                }

                maybeAmbush(player);
                maybeTeleportBehind(player);
            } else {
                this.playerMovementQueue.clear();
            }
        }
    }

    private void recordPlayerMovement(PlayerEntity player) {
        this.playerMovementQueue.addLast(new PlayerSnapshot(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch()));
        if (this.playerMovementQueue.size() > COPY_DELAY_TICKS) {
            this.playerMovementQueue.removeFirst();
        }
    }

    private void setSkinOwner(PlayerEntity player) {
        this.dataTracker.set(SKIN_OWNER, Optional.of(player.getUuid()));
    }

    public Optional<UUID> getSkinOwnerUuid() {
        return this.dataTracker.get(SKIN_OWNER);
    }

    private void maybeAmbush(PlayerEntity player) {
        if (this.ambushCooldown > 0) {
            return;
        }

        double distance = this.squaredDistanceTo(player);
        if (distance > AMBUSH_RANGE * AMBUSH_RANGE || distance < AMBUSH_DISTANCE_MIN * AMBUSH_DISTANCE_MIN) {
            return;
        }

        if (player.canSee(this)) {
            return;
        }

        if (this.random.nextFloat() < 0.12f) {
            teleportBehindPlayer(player);
            this.ambushCooldown = AMBUSH_COOLDOWN;
        }
    }

    private void maybeTeleportBehind(PlayerEntity player) {
        double distSquared = this.squaredDistanceTo(player);
        if (this.ticksSinceLastTeleport > TELEPORT_CHECK_INTERVAL && distSquared > 100.0 && this.random.nextFloat() < 0.12f) {
            if (teleportBehindPlayer(player)) {
                this.ticksSinceLastTeleport = 0;
            }
        }
    }

    private boolean teleportBehindPlayer(PlayerEntity player) {
        Vec3d playerDir = player.getRotationVector();
        double teleportDist = 4.0 + this.random.nextDouble() * 4.0;
        double newX = player.getX() - playerDir.x * teleportDist;
        double newY = player.getY();
        double newZ = player.getZ() - playerDir.z * teleportDist;
        
        boolean success = this.teleport(newX, newY, newZ);
        if (success) {
            this.getNavigation().stop();
            return true;
        }
        return false;
    }

    public static DefaultAttributeContainer.Builder createMimicAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.12)  // Как у игрока
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0)
                .add(EntityAttributes.GENERIC_ARMOR, 3.0);
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    private static final class PlayerSnapshot {
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private final float pitch;

        private PlayerSnapshot(double x, double y, double z, float yaw, float pitch) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    private static final class CopyPlayerMovementGoal extends Goal {
        private final MimicEntity mimic;
        private final double speed;

        private CopyPlayerMovementGoal(MimicEntity mimic, double speed) {
            this.mimic = mimic;
            this.speed = speed;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            return this.mimic.getTarget() instanceof PlayerEntity && !this.mimic.playerMovementQueue.isEmpty();
        }

        @Override
        public void tick() {
            if (!(this.mimic.getTarget() instanceof PlayerEntity)) {
                return;
            }

            if (this.mimic.playerMovementQueue.isEmpty()) {
                return;
            }

            PlayerSnapshot snapshot = this.mimic.playerMovementQueue.peekFirst();
            if (snapshot == null) {
                return;
            }

            this.mimic.getNavigation().startMovingTo(snapshot.x, snapshot.y, snapshot.z, this.speed);
            this.mimic.getLookControl().lookAt(snapshot.x, snapshot.y, snapshot.z, 30.0f, 30.0f);

            if (this.mimic.squaredDistanceTo(snapshot.x, snapshot.y, snapshot.z) < 2.5) {
                this.mimic.playerMovementQueue.removeFirst();
            }
        }
    }

    /**
     * Внезапная атака когда игрок слишком близко
     */
    private static final class SuddenAttackGoal extends Goal {
        private final MimicEntity mimic;
        private final double attackRange;
        private final double speed;
        private int chargeTicks = 0;

        private SuddenAttackGoal(MimicEntity mimic, double attackRange, double speed) {
            this.mimic = mimic;
            this.attackRange = attackRange;
            this.speed = speed;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            if (!(this.mimic.getTarget() instanceof PlayerEntity)) return false;
            PlayerEntity target = (PlayerEntity) this.mimic.getTarget();
            double distance = this.mimic.squaredDistanceTo(target);
            return distance < (attackRange * attackRange);
        }

        @Override
        public void start() {
            this.chargeTicks = 6;
        }

        @Override
        public void tick() {
            if (!(this.mimic.getTarget() instanceof PlayerEntity)) return;
            PlayerEntity target = (PlayerEntity) this.mimic.getTarget();

            if (this.chargeTicks > 0) {
                this.chargeTicks--;
                this.mimic.getLookControl().lookAt(target, 30.0f, 30.0f);
                return;
            }

            Vec3d direction = target.getRotationVector();
            this.mimic.setVelocity(direction.x * this.speed * LUNGE_SPEED_MULTIPLIER, 0.0, direction.z * this.speed * LUNGE_SPEED_MULTIPLIER);
            this.mimic.velocityModified = true;
            this.mimic.getNavigation().startMovingTo(target, this.speed * LUNGE_SPEED_MULTIPLIER);
            this.mimic.getLookControl().lookAt(target, 30.0f, 30.0f);

            if (this.mimic.squaredDistanceTo(target) < SUDDEN_ATTACK_RANGE * SUDDEN_ATTACK_RANGE) {
                this.mimic.tryAttack(target);
            }
        }
    }

    /**
     * Поведение "смотрит" - иногда просто стоит и наблюдает
     */
    private static final class WatchingGoal extends Goal {
        private final MimicEntity mimic;
        private int nextWatchTick = 0;

        private WatchingGoal(MimicEntity mimic) {
            this.mimic = mimic;
            this.setControls(EnumSet.noneOf(Control.class));
        }

        @Override
        public boolean canStart() {
            if (!(this.mimic.getTarget() instanceof PlayerEntity) || this.mimic.playerMovementQueue.isEmpty()) {
                return false;
            }
            // 15% шанс начать наблюдение
            return this.mimic.random.nextFloat() < 0.15;
        }

        @Override
        public void start() {
            this.nextWatchTick = 40 + this.mimic.random.nextInt(60); // Смотрит 2-5 секунд
        }

        @Override
        public void tick() {
            if (this.mimic.getTarget() instanceof PlayerEntity target) {
                this.mimic.getLookControl().lookAt(target.getX(), target.getEyeY(), target.getZ(), 30.0f, 30.0f);
            }
            this.nextWatchTick--;
        }

        @Override
        public boolean shouldContinue() {
            return this.nextWatchTick > 0 && this.mimic.getTarget() instanceof PlayerEntity;
        }
    }
}

