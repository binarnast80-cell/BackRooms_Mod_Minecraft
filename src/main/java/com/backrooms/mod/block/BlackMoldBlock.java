package com.backrooms.mod.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Чёрная плесень — тонкий оверлейный блок, который покрывает поверхности.
 * Имеет 3 варианта плотности (0 = лёгкая, 1 = средняя, 2 = тяжёлая).
 * При контакте вызывает тошноту.
 * Испускает тёмные частицы (чёрные споры).
 */
public class BlackMoldBlock extends Block {

    // Плотность плесени: 0 = редкие пиксели, 1 = средние, 2 = густые
    public static final IntProperty DENSITY = IntProperty.of("density", 0, 2);

    // Тончайший VoxelShape (1 пиксель = 1/16 блока), лежит на полу
    private static final VoxelShape SHAPE = Block.createCuboidShape(0, 0, 0, 16, 1, 16);

    public BlackMoldBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(DENSITY, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(DENSITY);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty(); // Не мешает ходить
    }

    @Override
    public boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
        return true;
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient && entity instanceof PlayerEntity player) {
            // Тошнота при контакте с плесенью (5 секунд)
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 100, 0, false, false, true));
        }
        super.onEntityCollision(state, world, pos, entity);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, net.minecraft.util.math.random.Random random) {
        // Чёрные споры — чем плотнее плесень, тем больше частиц
        int density = state.get(DENSITY);
        int particleChance = 8 - density * 2; // 8, 6, 4

        if (random.nextInt(particleChance) == 0) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + 0.1 + random.nextDouble() * 0.3;
            double z = pos.getZ() + random.nextDouble();
            // Тёмные частицы дыма, медленно поднимающиеся
            world.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.015, 0);
        }

        // Дополнительные мелкие споры для густой плесени
        if (density >= 2 && random.nextInt(4) == 0) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble() * 0.5;
            double z = pos.getZ() + random.nextDouble();
            world.addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, 0, 0.005, 0);
        }
    }

    /**
     * Проверяет, находится ли игрок рядом с плесенью (в радиусе 3 блоков).
     */
    public static boolean isPlayerNearMold(PlayerEntity player) {
        World world = player.getWorld();
        BlockPos playerPos = player.getBlockPos();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos checkPos = playerPos.add(dx, dy, dz);
                    if (world.getBlockState(checkPos).isOf(ModBlocks.BLACK_MOLD)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
