package com.backrooms.mod.item;

import com.backrooms.mod.BackroomsMod;
import com.backrooms.mod.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {

    public static final Item BACKROOMS_WALL = new BlockItem(ModBlocks.BACKROOMS_WALL, new Item.Settings());
    public static final Item BACKROOMS_FLOOR = new BlockItem(ModBlocks.BACKROOMS_FLOOR, new Item.Settings());
    public static final Item BACKROOMS_CEILING = new BlockItem(ModBlocks.BACKROOMS_CEILING, new Item.Settings());
    public static final Item BACKROOMS_LAMP = new BlockItem(ModBlocks.BACKROOMS_LAMP, new Item.Settings());
    public static final Item SOGGY_WALL = new BlockItem(ModBlocks.SOGGY_WALL, new Item.Settings());
    public static final Item GREEN_WALLPAPER = new BlockItem(ModBlocks.GREEN_WALLPAPER, new Item.Settings());

    public static void register() {
        Registry.register(Registries.ITEM, new Identifier(BackroomsMod.MOD_ID, "backrooms_wall"), BACKROOMS_WALL);
        Registry.register(Registries.ITEM, new Identifier(BackroomsMod.MOD_ID, "backrooms_floor"), BACKROOMS_FLOOR);
        Registry.register(Registries.ITEM, new Identifier(BackroomsMod.MOD_ID, "backrooms_ceiling"), BACKROOMS_CEILING);
        Registry.register(Registries.ITEM, new Identifier(BackroomsMod.MOD_ID, "backrooms_lamp"), BACKROOMS_LAMP);
        Registry.register(Registries.ITEM, new Identifier(BackroomsMod.MOD_ID, "soggy_wall"), SOGGY_WALL);
        Registry.register(Registries.ITEM, new Identifier(BackroomsMod.MOD_ID, "green_wallpaper"), GREEN_WALLPAPER);

        // Добавляем в креативную вкладку "Строительные блоки"
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(content -> {
            content.add(BACKROOMS_WALL);
            content.add(BACKROOMS_FLOOR);
            content.add(BACKROOMS_CEILING);
            content.add(BACKROOMS_LAMP);
            content.add(SOGGY_WALL);
            content.add(GREEN_WALLPAPER);
        });
    }
}
