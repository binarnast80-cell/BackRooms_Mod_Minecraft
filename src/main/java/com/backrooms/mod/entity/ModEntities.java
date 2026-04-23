package com.backrooms.mod.entity;

import com.backrooms.mod.BackroomsMod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.Heightmap;

public class ModEntities {

    public static final EntityType<LurkerEntity> LURKER = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(BackroomsMod.MOD_ID, "lurker"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, LurkerEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.8f))
                    .trackRangeChunks(8)
                    .build());

    public static final EntityType<HowlerEntity> HOWLER = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(BackroomsMod.MOD_ID, "howler"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, HowlerEntity::new)
                    .dimensions(EntityDimensions.fixed(0.7f, 1.9f))
                    .trackRangeChunks(10)
                    .build());

    public static final EntityType<MimicEntity> MIMIC = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(BackroomsMod.MOD_ID, "mimic"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, MimicEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.8f))
                    .trackRangeChunks(10)
                    .build());

    public static final EntityType<WoodenStalkerEntity> WOODEN_STALKER = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(BackroomsMod.MOD_ID, "wooden_stalker"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, WoodenStalkerEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.85f)) // Немного меньше, чтобы не застревал
                    .trackRangeChunks(10)
                    .build());

    public static void register() {
        FabricDefaultAttributeRegistry.register(LURKER, LurkerEntity.createLurkerAttributes());
        FabricDefaultAttributeRegistry.register(HOWLER, HowlerEntity.createHowlerAttributes());
        FabricDefaultAttributeRegistry.register(MIMIC, MimicEntity.createMimicAttributes());
        FabricDefaultAttributeRegistry.register(WOODEN_STALKER, WoodenStalkerEntity.createWoodenStalkerAttributes());

        // Разрешаем спавн только на уровне лабиринта (Y от 100 до 105)
        SpawnRestriction.register(LURKER,
                SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                (type, world, reason, pos, random) -> pos.getY() >= 100 && pos.getY() <= 106
        );
        SpawnRestriction.register(HOWLER,
                SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                (type, world, reason, pos, random) -> pos.getY() >= 100 && pos.getY() <= 106);
        SpawnRestriction.register(MIMIC,
                SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                (type, world, reason, pos, random) -> pos.getY() >= 100 && pos.getY() <= 106);
        SpawnRestriction.register(WOODEN_STALKER,
                SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                (type, world, reason, pos, random) -> pos.getY() >= 100 && pos.getY() <= 106 && com.backrooms.mod.world.BackroomsChunkGenerator.isInfected(pos.getX(), pos.getZ()));

        BackroomsMod.LOGGER.info("Backrooms entities registered");
    }
}
