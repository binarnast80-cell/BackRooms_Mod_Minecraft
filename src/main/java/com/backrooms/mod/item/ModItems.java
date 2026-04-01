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

    public static void register() {
        Registry.register(Registries.ITEM, new Identifier(BackroomsMod.MOD_ID, "backrooms_wall"), BACKROOMS_WALL);
        Registry.register(Registries.ITEM, new Identifier(BackroomsMod.MOD_ID, "backrooms_floor"), BACKROOMS_FLOOR);
        Registry.register(Registries.ITEM, new Identifier(BackroomsMod.MOD_ID, "backrooms_ceiling"), BACKROOMS_CEILING);

        // Добавляем в креативную вкладку "Строительные блоки"
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(content -> {
            content.add(BACKROOMS_WALL);
            content.add(BACKROOMS_FLOOR);
            content.add(BACKROOMS_CEILING);
        });
    }
}
