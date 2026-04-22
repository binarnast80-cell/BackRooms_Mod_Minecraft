package com.backrooms.mod.entity;

import com.backrooms.mod.sound.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;

/**
 * Howler — медленный, но мощный обитатель Backrooms.
 * Имеет много здоровья, наносит огромный урон.
 * Издаёт жуткие звуки на большом расстоянии.
 */
public class HowlerEntity extends HostileEntity {

    public HowlerEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.0, true)); // Точнее, но медленнее
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 0.5));
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 24.0f));
        this.goalSelector.add(4, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    public static DefaultAttributeContainer.Builder createHowlerAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)       // 20 сердец — танк
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 10.0)    // 5 сердец урона — больно
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2)    // Медленный
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0)     // Огромный радиус обнаружения
                .add(EntityAttributes.GENERIC_ARMOR, 6.0)             // Хорошая броня
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.5); // Устойчив к отбрасыванию
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.HOWLER_CRY;
    }

    @Override
    protected SoundEvent getHurtSound() {
        return ModSounds.HOWLER_CRY;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.HOWLER_CRY;
    }
}
