package com.backrooms.mod.world;

import com.backrooms.mod.BackroomsMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

public class ModDimensions {
    public static final RegistryKey<World> BACKROOMS_LEVEL_KEY =
            RegistryKey.of(RegistryKeys.WORLD, new Identifier(BackroomsMod.MOD_ID, "the_backrooms"));

    public static final RegistryKey<DimensionType> BACKROOMS_DIM_TYPE =
            RegistryKey.of(RegistryKeys.DIMENSION_TYPE, new Identifier(BackroomsMod.MOD_ID, "backrooms_type"));

    public static void register() {
        // Регистрация кодека кастомного ChunkGenerator
        Registry.register(Registries.CHUNK_GENERATOR,
                new Identifier(BackroomsMod.MOD_ID, "backrooms_chunkgen"),
                BackroomsChunkGenerator.CODEC);

        BackroomsMod.LOGGER.info("Backrooms dimension registered");
    }
}
