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
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.StateManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.random.Random;
import net.minecraft.sound.SoundCategory;
import com.backrooms.mod.sound.ModSounds;

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
    public static final BooleanProperty LIT = Properties.LIT;

    public static final Block BACKROOMS_LAMP = new Block(
            FabricBlockSettings.create()
                    .strength(-1.0f, 3600000.0f)
                    .sounds(BlockSoundGroup.GLASS)
                    .luminance(state -> state.get(LIT) ? 12 : 0)
                    .ticksRandomly()
                    .dropsNothing()
    ) {
        {
            this.setDefaultState(this.stateManager.getDefaultState().with(LIT, true));
        }

        @Override
        protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
            builder.add(LIT);
        }
        @Override
        public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
            super.onBlockAdded(state, world, pos, oldState, notify);
            // Запускаем цепочку мигания сразу (1-16 сек задержка)
            if (!world.isClient() && state.get(LIT)) {
                ServerWorld sw = (ServerWorld) world;
                if (!sw.getBlockTickScheduler().isQueued(pos, this)) {
                    int delay = 20 + sw.random.nextInt(301); // 1-16 секунд
                    sw.scheduleBlockTick(pos, this, delay);
                }
            }
        }

        @Override
        public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
            // Подстраховка: если цепочка не запустилась через onBlockAdded
            if (state.get(LIT) && !world.getBlockTickScheduler().isQueued(pos, this)) {
                int delay = 20 + random.nextInt(101);
                world.scheduleBlockTick(pos, this, delay);
            }
        }

        @Override
        public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
            if (state.get(LIT)) {
                // === МИГАНИЕ: выключаем лампу ===
                // Выключаем эту лампу
                world.setBlockState(pos, state.with(LIT, false), 3);
                world.playSound(null, pos, net.minecraft.sound.SoundEvents.BLOCK_REDSTONE_TORCH_BURNOUT,
                        SoundCategory.BLOCKS, 0.3f, 2.6f + (world.random.nextFloat() - world.random.nextFloat()) * 0.8f);

                // Синхронно выключаем все соседние лампы в ряду (одна "лампа" = несколько блоков в линию)
                for (net.minecraft.util.math.Direction dir : new net.minecraft.util.math.Direction[]{
                        net.minecraft.util.math.Direction.NORTH, net.minecraft.util.math.Direction.SOUTH,
                        net.minecraft.util.math.Direction.EAST, net.minecraft.util.math.Direction.WEST}) {
                    BlockPos neighbor = pos.offset(dir);
                    BlockState neighborState = world.getBlockState(neighbor);
                    if (neighborState.isOf(this) && neighborState.get(LIT)) {
                        world.setBlockState(neighbor, neighborState.with(LIT, false), 3);
                        // Включить обратно одновременно с основной лампой
                        int flickDuration = 3 + random.nextInt(5); // 0.15-0.4 сек
                        world.scheduleBlockTick(neighbor, this, flickDuration);
                    }
                }

                // Включить обратно через короткий момент (0.15-0.4 сек)
                int flickDuration = 3 + random.nextInt(5);
                world.scheduleBlockTick(pos, this, flickDuration);
            } else {
                // === ВКЛЮЧЕНИЕ обратно ===
                world.setBlockState(pos, state.with(LIT, true), 3);

                // Включаем соседние лампы тоже (на случай если они ещё выключены)
                for (net.minecraft.util.math.Direction dir : new net.minecraft.util.math.Direction[]{
                        net.minecraft.util.math.Direction.NORTH, net.minecraft.util.math.Direction.SOUTH,
                        net.minecraft.util.math.Direction.EAST, net.minecraft.util.math.Direction.WEST}) {
                    BlockPos neighbor = pos.offset(dir);
                    BlockState neighborState = world.getBlockState(neighbor);
                    if (neighborState.isOf(this) && !neighborState.get(LIT)) {
                        world.setBlockState(neighbor, neighborState.with(LIT, true), 3);
                    }
                }

                // Планируем следующее мигание через 2-7 секунд
                int nextFlicker = 40 + random.nextInt(101);
                world.scheduleBlockTick(pos, this, nextFlicker);
            }
        }
    };

    // Чёрная плесень — тонкий оверлей, вызывает тошноту
    public static final Block BLACK_MOLD = new BlackMoldBlock(
            FabricBlockSettings.create()
                    .strength(0.1f)
                    .sounds(BlockSoundGroup.SLIME)
                    .noCollision()
                    .nonOpaque()
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
        Registry.register(Registries.BLOCK, new Identifier(BackroomsMod.MOD_ID, "black_mold"), BLACK_MOLD);
    }
}
