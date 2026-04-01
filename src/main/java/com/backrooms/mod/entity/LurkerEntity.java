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
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false; // Не деспавнится — Backrooms не отпускают
    }
}
