package com.backrooms.mod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

/**
 * Lurker — быстрое и скрытное существо Backrooms.
 * Преследует игрока с высокой скоростью, наносит средний урон.
 * Имитирует "Бактерию" из лора Backrooms.
 */
public class LurkerEntity extends HostileEntity {

    public LurkerEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initGoals() {
        // Приоритеты: чем меньше число, тем выше приоритет
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 16.0f));
        this.goalSelector.add(4, new LookAroundGoal(this));

        // Таргетинг игрока
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    public static DefaultAttributeContainer.Builder createLurkerAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)       // 10 сердец
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0)     // 3 сердца урона
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.38)   // Быстрее зомби (0.23)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0)     // Большой радиус обнаружения
                .add(EntityAttributes.GENERIC_ARMOR, 2.0);            // Лёгкая броня
    }

    
    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false; // Не деспавнится — Backrooms не отпускают
    }
}
