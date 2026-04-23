package com.backrooms.mod.block;

import com.backrooms.mod.BackroomsMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class ModBlocks {

    // Коллекции для отслеживания кликов (используются GREEN_WALLPAPER)
    public static final HashMap<BlockPos, Integer> clickCounts = new HashMap<>();
    public static final HashSet<BlockPos> activeBlocks = new HashSet<>();
    public static final ArrayList<String> clickHistory = new ArrayList<>();
    public static final LinkedList<String> lastActions = new LinkedList<>();

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

    // Потолок Backrooms — НЕУЯЗВИМ, теперь НЕ светится (света нет)
    public static final Block BACKROOMS_CEILING = new Block(
            FabricBlockSettings.create()
                    .strength(-1.0f, 3600000.0f)
                    .sounds(BlockSoundGroup.GLASS)
                    .dropsNothing()
    );

    // Зелёные обои — при клике отслеживает статистику, при наступании отравляет
    public static final Block GREEN_WALLPAPER = new Block(
            FabricBlockSettings.create()
                    .strength(1.5f)
                    .sounds(BlockSoundGroup.STONE)
    ) {
        @Override
        public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
            if (!world.isClient) {
                int clicks = clickCounts.getOrDefault(pos, 0) + 1;
                clickCounts.put(pos, clicks);
                activeBlocks.add(pos);
                clickHistory.add("Игрок " + player.getName().getString() + " кликнул по " + pos.toShortString());
                lastActions.addFirst("Клик на " + pos.getX() + ", " + pos.getZ());
                if (lastActions.size() > 5) lastActions.removeLast();

                player.sendMessage(Text.literal("Кликов: " + clicks + " | Всего блоков: " + activeBlocks.size() + " | История: " + clickHistory.size()), false);
            }
            return ActionResult.SUCCESS;
        }

        @Override
        public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
            if (!world.isClient && entity instanceof PlayerEntity player) {
                player.setHealth(1.0f);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 100, 1));
            }
            super.onSteppedOn(world, pos, state, entity);
        }
    };

    // Мокрая стена — замедляет движение
    public static final Block SOGGY_WALL = new Block(
            FabricBlockSettings.create()
                    .strength(2.0f, 6.0f)
                    .sounds(BlockSoundGroup.MUD)
                    .velocityMultiplier(0.6f)
                    .jumpVelocityMultiplier(0.8f)
    );

    // Лампа Backrooms — единственный источник света (уровень 12)
    public static final Block BACKROOMS_LAMP = new Block(
            FabricBlockSettings.create()
                    .strength(-1.0f, 3600000.0f)
                    .sounds(BlockSoundGroup.GLASS)
                    .luminance(state -> 12)
                    .dropsNothing()
    );

    public static void register() {
        // Регистрация блоков
        Registry.register(Registries.BLOCK, new Identifier(BackroomsMod.MOD_ID, "backrooms_wall"), BACKROOMS_WALL);
        Registry.register(Registries.BLOCK, new Identifier(BackroomsMod.MOD_ID, "backrooms_floor"), BACKROOMS_FLOOR);
        Registry.register(Registries.BLOCK, new Identifier(BackroomsMod.MOD_ID, "backrooms_ceiling"), BACKROOMS_CEILING);
        Registry.register(Registries.BLOCK, new Identifier(BackroomsMod.MOD_ID, "backrooms_lamp"), BACKROOMS_LAMP);
        Registry.register(Registries.BLOCK, new Identifier(BackroomsMod.MOD_ID, "soggy_wall"), SOGGY_WALL);
        Registry.register(Registries.BLOCK, new Identifier(BackroomsMod.MOD_ID, "green_wallpaper"), GREEN_WALLPAPER);
    }
}
