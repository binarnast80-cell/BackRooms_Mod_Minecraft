package com.backrooms.mod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;

/**
 * Mimic — криповый монстр Backrooms, который пытается копировать движения игрока
 * и подкрадываться незаметно, прежде чем внезапно атаковать.
 */
public class MimicEntity extends HostileEntity {
    private static final int COPY_DELAY_TICKS = 12;
    private final Deque<PlayerSnapshot> playerMovementQueue = new ArrayDeque<>();

    public MimicEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.add(2, new CopyPlayerMovementGoal(this, 1.0));
        this.goalSelector.add(3, new WanderAroundFarGoal(this, 0.75));
        this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 20.0f));
        this.goalSelector.add(5, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.world.isClient) {
            if (this.getTarget() instanceof PlayerEntity player) {
                recordPlayerMovement(player);
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

    public static DefaultAttributeContainer.Builder createMimicAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.1)
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
}
