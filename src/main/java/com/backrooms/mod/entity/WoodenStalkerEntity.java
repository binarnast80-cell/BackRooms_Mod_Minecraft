package com.backrooms.mod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class WoodenStalkerEntity extends HostileEntity {

    private boolean isFrozen = false;

    public WoodenStalkerEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new FleeWhenLowHealthGoal(this, 1.5D));
        this.goalSelector.add(2, new SneakAttackGoal(this, 1.2D, false));
        this.goalSelector.add(3, new WanderAroundFarGoal(this, 0.8D));
        this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 16.0F));
        this.goalSelector.add(5, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    public static DefaultAttributeContainer.Builder createWoodenStalkerAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0)
                .add(EntityAttributes.GENERIC_ARMOR, 5.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.5);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getWorld().isClient()) return;

        // Ищем ближайшего игрока в радиусе 32 блоков
        PlayerEntity player = this.getWorld().getClosestPlayer(this, 32.0);
        if (player != null) {
            isFrozen = isPlayerLookingAtMe(player);
            if (isFrozen) {
                this.getNavigation().stop();
                this.setBodyYaw(this.getHeadYaw()); 
            }
        } else {
            isFrozen = false;
        }
    }

    @Override
    public void travel(Vec3d movementInput) {
        // Если заморожен — полностью блокируем передвижение
        if (this.isFrozen()) {
            super.travel(Vec3d.ZERO);
        } else {
            super.travel(movementInput);
        }
    }

    // Проверка, смотрит ли игрок прямо на моба
    private boolean isPlayerLookingAtMe(PlayerEntity player) {
        Vec3d eyePos = player.getEyePos();
        Vec3d myEyePos = this.getEyePos();
        Vec3d toMe = myEyePos.subtract(eyePos).normalize();
        Vec3d lookDir = player.getRotationVector().normalize();

        double dot = toMe.dotProduct(lookDir);
        if (dot > 0.85) { // Примерно 30 градусов
            var result = this.getWorld().raycast(new RaycastContext(
                    eyePos, myEyePos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    this));
            return result.getType() == net.minecraft.util.hit.HitResult.Type.MISS;
        }
        return false;
    }

    @Override
    public boolean damage(net.minecraft.entity.damage.DamageSource source, float amount) {
        // Игнорируем урон без атакующего (окружение, удушье)
        if (source.getAttacker() == null) {
            return false;
        }
        return super.damage(source, amount);
    }

    public boolean isFrozen() {
        return isFrozen;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    static class FleeWhenLowHealthGoal extends Goal {
        private final WoodenStalkerEntity mob;
        private final double speed;
        private double targetX, targetY, targetZ;

        public FleeWhenLowHealthGoal(WoodenStalkerEntity mob, double speed) {
            this.mob = mob;
            this.speed = speed;
        }

        @Override
        public boolean canStart() {
            if (mob.getHealth() > mob.getMaxHealth() * 0.3) return false;
            LivingEntity target = mob.getTarget();
            if (target == null) return false;

            Vec3d fleePos = net.minecraft.entity.ai.NoPenaltyTargeting.findFrom(mob, 16, 7, target.getPos());
            if (fleePos == null) return false;

            this.targetX = fleePos.x;
            this.targetY = fleePos.y;
            this.targetZ = fleePos.z;
            return true;
        }

        @Override
        public void start() {
            this.mob.getNavigation().startMovingTo(this.targetX, this.targetY, this.targetZ, this.speed);
        }

        @Override
        public boolean shouldContinue() {
            return !this.mob.getNavigation().isIdle() && mob.getHealth() <= mob.getMaxHealth() * 0.3;
        }
    }

    static class SneakAttackGoal extends MeleeAttackGoal {
        private final WoodenStalkerEntity stalker;

        public SneakAttackGoal(WoodenStalkerEntity stalker, double speed, boolean pauseWhenMobIdle) {
            super(stalker, speed, pauseWhenMobIdle);
            this.stalker = stalker;
        }

        @Override
        public boolean canStart() {
            if (stalker.isFrozen()) return false;
            return super.canStart();
        }

        @Override
        public boolean shouldContinue() {
            if (stalker.isFrozen()) return false;
            return super.shouldContinue();
        }

        @Override
        public void tick() {
            if (!stalker.isFrozen()) {
                super.tick();
            }
        }
    }
}
