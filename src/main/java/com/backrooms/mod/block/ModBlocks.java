package com.backrooms.mod.block;

import com.backrooms.mod.BackroomsMod;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class ModBlocks {

    // Жёлтая стена Backrooms — очень прочная, но ломается (для будущих механик)
    public static final Block BACKROOMS_WALL = new Block(
            FabricBlockSettings.create()
                    .strength(50.0f, 1200.0f)
                    .sounds(BlockSoundGroup.STONE)
                    .requiresTool()
    );

    // Пол Backrooms — НЕУЯЗВИМ как бедрок (hardness -1)
    public static final Block BACKROOMS_FLOOR = new Block(
            FabricBlockSettings.create()
                    .strength(-1.0f, 3600000.0f)
                    .sounds(BlockSoundGroup.STONE)
                    .dropsNothing()
    );

    // Потолок Backrooms — ТЁМНЫЙ, без свечения. Текстура потолочных плит.
    public static final Block BACKROOMS_CEILING = new Block(
            FabricBlockSettings.create()
                    .strength(-1.0f, 3600000.0f)
                    .sounds(BlockSoundGroup.STONE)
                    .dropsNothing()
    );

    // Лампа Backrooms — тусклый неоновый блок. Свет=7 (из 15) для глубоких теней.
    public static final Block BACKROOMS_LAMP = new Block(
            FabricBlockSettings.create()
                    .strength(-1.0f, 3600000.0f)
                    .sounds(BlockSoundGroup.GLASS)
                    .luminance(state -> 7)
                    .dropsNothing()
    );

    public static void register() {
        Registry.register(Registries.BLOCK, new Identifier(BackroomsMod.MOD_ID, "backrooms_wall"), BACKROOMS_WALL);
        Registry.register(Registries.BLOCK, new Identifier(BackroomsMod.MOD_ID, "backrooms_floor"), BACKROOMS_FLOOR);
        Registry.register(Registries.BLOCK, new Identifier(BackroomsMod.MOD_ID, "backrooms_ceiling"), BACKROOMS_CEILING);
        Registry.register(Registries.BLOCK, new Identifier(BackroomsMod.MOD_ID, "backrooms_lamp"), BACKROOMS_LAMP);
    }
}
