package com.backrooms.mod.world;

import com.backrooms.mod.block.ModBlocks;
import com.backrooms.mod.entity.ModEntities;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Backrooms Level 0 — аутентичный генератор бесконечного лабиринта.
 *
 * Архитектура генерации:
 * - Мир делится на "секторы" 32×32 блока
 * - Каждый сектор содержит уникальную комбинацию комнат и коридоров
 * - Длинные коридоры (3 блока шириной, до 30+ блоков длиной)
 * - Большие залы (до 20×20 блоков)
 * - Т-образные перекрёстки и углы
 * - Столбы и колонны в больших залах
 * - Высота потолка: 5 блоков (Y=0 пол, Y=1-5 воздух, Y=6 потолок)
 *
 * Алгоритм основан на Perlin-подобном шуме для определения
 * "плотности" стен в каждой точке, создавая органичные комнаты.
 */
public class BackroomsChunkGenerator extends ChunkGenerator {

    public static final Codec<BackroomsChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource))
            .apply(instance, BackroomsChunkGenerator::new));

    // Высоты — потолок поднят до 5 блоков
    private static final int FLOOR_Y = 0;
    private static final int WALL_MIN_Y = 1;
    private static final int WALL_MAX_Y = 5; // 5 блоков высота стен
    private static final int CEILING_Y = 6; // Потолок на Y=6

    private static final long WORLD_SEED = 48291537L;

    public BackroomsChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender,
            NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
        BlockState floorState = ModBlocks.BACKROOMS_FLOOR.getDefaultState();
        BlockState wallState = ModBlocks.BACKROOMS_WALL.getDefaultState();
        BlockState ceilingState = ModBlocks.BACKROOMS_CEILING.getDefaultState();
        BlockState lampState = ModBlocks.BACKROOMS_LAMP.getDefaultState();
        BlockState airState = Blocks.AIR.getDefaultState();

        int startX = chunk.getPos().getStartX();
        int startZ = chunk.getPos().getStartZ();
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int wx = startX + lx;
                int wz = startZ + lz;

                // Проверка на Зараженную зону (доски)
                boolean infected = isInfected(wx, wz);
                BlockState currentFloor = infected ? Blocks.OAK_PLANKS.getDefaultState() : floorState;
                BlockState currentWall = infected ? Blocks.OAK_PLANKS.getDefaultState() : wallState;
                BlockState currentCeiling = infected ? Blocks.OAK_PLANKS.getDefaultState() : ceilingState;

                // Пол
                chunk.setBlockState(mutable.set(lx, FLOOR_Y, lz), currentFloor, false);

                // Потолок (в зараженной зоне ламп нет)
                boolean wall = isWall(wx, wz);
                boolean isLamp = !infected && !wall && isLampPosition(wx, wz);
                chunk.setBlockState(mutable.set(lx, CEILING_Y, lz),
                        isLamp ? lampState : currentCeiling, false);

                // Стены или воздух
                for (int y = WALL_MIN_Y; y <= WALL_MAX_Y; y++) {
                    chunk.setBlockState(mutable.set(lx, y, lz), wall ? currentWall : airState, false);
                }
            }
        }

        return CompletableFuture.completedFuture(chunk);
    }

    // ======================== ГЕНЕРАЦИЯ ЛАМП ========================

    /**
     * Размещение ламп по 2 в ряд (вдоль оси X).
     * Сетка 16×16 — в каждой ячейке с ~12% шансом появляется пара ламп.
     * Лампы тусклые (luminance=7), создают островки света в темноте.
     */
    private boolean isLampPosition(int wx, int wz) {
        int gridSize = 16; // крупная сетка — лампы далеко друг от друга
        int cellX = Math.floorDiv(wx, gridSize);
        int cellZ = Math.floorDiv(wz, gridSize);
        int localX = Math.floorMod(wx, gridSize);
        int localZ = Math.floorMod(wz, gridSize);

        // Хеш ячейки — определяет есть ли лампа в этой ячейке
        long cellHash = mixHash(WORLD_SEED + 777, cellX * 31337L, cellZ * 27191L);
        boolean cellHasLamp = (Math.abs(cellHash) % 100) < 12; // ~12% ячеек имеют лампы
        if (!cellHasLamp) return false;

        // Лампа по центру ячейки: пара из 2 блоков вдоль X
        // localZ == 7 (одна линия), localX == 7 или 8 (два блока в ряд)
        return localZ == 7 && (localX == 7 || localX == 8);
    }

    // ======================== ГЕНЕРАЦИЯ СТЕН ========================

    /**
     * Многослойная система генерации стен Backrooms Level 0.
     *
     * Слой 1: Основная структура — крупные стены каждые 32 блока
     * создают большие секторы/залы
     * Слой 2: Коридоры и комнаты — стены каждые 16 блоков
     * делят секторы на подзоны с длинными коридорами
     * Слой 3: Внутренние перегородки — стены каждые 8 блоков
     * создают маленькие офисные кабинки (только в некоторых зонах)
     * Слой 4: Столбы — одиночные колонны для атмосферы
     */
    private boolean isWall(int wx, int wz) {
        // === СЛОЙ 5: Клеточная генерация (плотная сетка коридоров) ===
        // В 30% зон размером 48x48 генерируется только клеточная структура
        int cellZoneX = Math.floorDiv(wx, 48);
        int cellZoneZ = Math.floorDiv(wz, 48);
        long cellZoneHash = mixHash(WORLD_SEED + 300, cellZoneX * 8191L, cellZoneZ * 8111L);
        if (Math.abs(cellZoneHash) % 10 < 3) {
            // Сетка 7х7: 6 блоков - сплошная стена, 1 блок - коридор (воздух)
            int modX = Math.floorMod(wx, 7);
            int modZ = Math.floorMod(wz, 7);
            
            if (modX < 6 && modZ < 6) {
                return true; // Блок-колонна 6x6
            } else {
                return false; // Принудительно коридор 1-блок шириной (игнорирует другие слои)
            }
        }

        // === СЛОЙ 1: Основные стены (каждые 24 блока вместо 32) ===
        // Создают огромные секции — основной каркас лабиринта
        if (isGridWall(wx, wz, 24, 2, WORLD_SEED, 5, true)) {
            return true;
        }

        // === СЛОЙ 2: Средние стены (каждые 12 блоков вместо 16) ===
        // Длинные коридоры и средние комнаты — 60% вероятность существования
        if (isGridWall(wx, wz, 12, 1, WORLD_SEED + 1, 4, false)) {
            // Проверяем — эта стена нужна в данном секторе?
            int sectorX = Math.floorDiv(wx, 24);
            int sectorZ = Math.floorDiv(wz, 24);
            long sectorHash = mixHash(WORLD_SEED + 100, sectorX * 7919L, sectorZ * 7927L);
            if (Math.abs(sectorHash) % 10 < 6) { // 60% секторов имеют средние стены
                return true;
            }
        }

        // === СЛОЙ 3: Мелкие перегородки (каждые 8 блоков) ===
        // Офисные кабинки — только в "плотных" зонах (~20%)
        if (isGridWall(wx, wz, 8, 1, WORLD_SEED + 2, 3, false)) {
            int zoneX = Math.floorDiv(wx, 48);
            int zoneZ = Math.floorDiv(wz, 48);
            long zoneHash = mixHash(WORLD_SEED + 200, zoneX * 6271L, zoneZ * 6277L);
            if (Math.abs(zoneHash) % 10 < 2) { // Только 20% зон "плотные"
                return true;
            }
        }

        // === СЛОЙ 4: Колонны ===
        if (isPillar(wx, wz)) {
            return true;
        }

        return false;
    }

    /**
     * Универсальная функция стен на сетке.
     *
     * @param wx,             wz Мировые координаты
     * @param gridSize        Размер ячейки сетки
     * @param wallThickness   Толщина стены (1 или 2 блока)
     * @param seed            Seed для этого слоя
     * @param doorWidth       Ширина проходов
     * @param guaranteedDoors Гарантировать хотя бы 1 проход в каждой стене
     */
    private boolean isGridWall(int wx, int wz, int gridSize, int wallThickness,
            long seed, int doorWidth, boolean guaranteedDoors) {
        int modX = Math.floorMod(wx, gridSize);
        int modZ = Math.floorMod(wz, gridSize);

        boolean onWallX = (modX < wallThickness);
        boolean onWallZ = (modZ < wallThickness);

        // Не на стене — воздух
        if (!onWallX && !onWallZ) {
            return false;
        }

        // Перекрёсток — всегда стена (опора конструкции)
        if (onWallX && onWallZ) {
            return true;
        }

        // Определяем ячейку и стену
        int cellX = Math.floorDiv(wx, gridSize);
        int cellZ = Math.floorDiv(wz, gridSize);

        if (onWallX) {
            // Стена вдоль оси X, проходы вырезают по Z
            int posInSegment = modZ;
            return !isDoorway(seed, cellX, cellZ, posInSegment, gridSize, wallThickness,
                    doorWidth, guaranteedDoors, true);
        } else {
            // Стена вдоль оси Z, проходы вырезают по X
            int posInSegment = modX;
            return !isDoorway(seed, cellX, cellZ, posInSegment, gridSize, wallThickness,
                    doorWidth, guaranteedDoors, false);
        }
    }

    /**
     * Определяет, является ли позиция дверным проёмом.
     * Каждая стена может иметь 1-3 прохода в зависимости от хеша.
     */
    private boolean isDoorway(long seed, int cellX, int cellZ, int pos,
            int gridSize, int wallThickness, int doorWidth,
            boolean guaranteed, boolean isXWall) {
        int usableLength = gridSize - wallThickness;
        if (pos < wallThickness || pos >= gridSize) {
            return false; // На опоре перекрёстка
        }

        int posInUsable = pos - wallThickness;

        // Первый проход (гарантированный если guaranteed=true)
        long h1 = isXWall ? mixHash(seed, cellX * 100003L, cellZ * 200017L)
                : mixHash(seed, cellX * 300019L, cellZ * 400009L);
        int door1Start = (int) (Math.abs(h1) % Math.max(1, usableLength - doorWidth - 2)) + 1;
        if (posInUsable >= door1Start && posInUsable < door1Start + doorWidth) {
            return true;
        }

        // Второй проход (50-70% шанс)
        long h2 = isXWall ? mixHash(seed, cellX * 500029L, cellZ * 600011L)
                : mixHash(seed, cellX * 700001L, cellZ * 800021L);
        if (Math.abs(h2) % 10 < 6) { // 60% шанс
            int door2Start = (int) (Math.abs(h2 >>> 16) % Math.max(1, usableLength - doorWidth - 2)) + 1;
            if (posInUsable >= door2Start && posInUsable < door2Start + doorWidth) {
                return true;
            }
        }

        // Третий проход (у больших стен, 30% шанс)
        if (gridSize >= 24) {
            long h3 = isXWall ? mixHash(seed, cellX * 900007L, cellZ * 1100003L)
                    : mixHash(seed, cellX * 1200011L, cellZ * 1300027L);
            if (Math.abs(h3) % 10 < 3) {
                int door3Start = (int) (Math.abs(h3 >>> 24) % Math.max(1, usableLength - doorWidth - 2)) + 1;
                if (posInUsable >= door3Start && posInUsable < door3Start + doorWidth) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Декоративные колонны — создают атмосферу офиса.
     * Появляются только в открытых пространствах, не на стенах.
     */
    private boolean isPillar(int wx, int wz) {
        // Сетка колонн каждые 10 блоков, но со смещением
        int pillarGridX = Math.floorMod(wx + 5, 10);
        int pillarGridZ = Math.floorMod(wz + 5, 10);

        if (pillarGridX == 0 && pillarGridZ == 0) {
            // 25% колонн существуют
            long h = mixHash(WORLD_SEED + 500, (long) wx, (long) wz);
            return Math.abs(h) % 4 == 0;
        }
        return false;
    }

    // ======================== ХЕШИРОВАНИЕ ========================

    private static long mixHash(long seed, long a, long b) {
        long h = seed ^ a;
        h ^= b * 0x9E3779B97F4A7C15L;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }

    // ======================== ГЕНЕРАЦИЯ ЗАРАЖЕННОЙ ЗОНЫ ========================

    private boolean isInfected(int wx, int wz) {
        // Базовый шум, масштаб 100 блоков (определяет общую форму зоны)
        double baseNoise = simpleNoise(wx / 100.0, wz / 100.0, WORLD_SEED + 500);
        
        // Красивый "комковатый" высокочастотный шум для эффекта смешивания (масштаб 3.5 блока)
        // Это создаст органичные пятна на границе, а не просто телевизионный пиксельный шум
        double detailNoise = simpleNoise(wx / 3.5, wz / 3.5, WORLD_SEED + 501); 
        
        // Чтобы переходник был СТРОГО от 10 до 20 блоков:
        // Градиент baseNoise (при масштабе 100) равен примерно 0.01-0.015 на блок.
        // Умножая detailNoise на 0.16, мы создаем зону смешивания (дельта 0.16).
        // В блоках это даст ровно ~10-15 блоков переходной зоны.
        double combined = baseNoise + (detailNoise - 0.5) * 0.16;
        
        // Если итоговое значение > 0.6 — это деревянная зона
        return combined > 0.6;
    }

    private double simpleNoise(double x, double z, long seed) {
        int ix = (int) Math.floor(x);
        int iz = (int) Math.floor(z);
        double fx = x - ix;
        double fz = z - iz;

        // Плавная интерполяция
        double ux = fx * fx * (3.0 - 2.0 * fx);
        double uz = fz * fz * (3.0 - 2.0 * fz);

        double v00 = hashNoiseLocal(ix, iz, seed);
        double v10 = hashNoiseLocal(ix + 1, iz, seed);
        double v01 = hashNoiseLocal(ix, iz + 1, seed);
        double v11 = hashNoiseLocal(ix + 1, iz + 1, seed);

        double i1 = v00 + ux * (v10 - v00);
        double i2 = v01 + ux * (v11 - v01);

        return i1 + uz * (i2 - i1);
    }

    private double hashNoiseLocal(int x, int z, long seed) {
        long h = mixHash(seed, (long) x, (long) z);
        return (double) (Math.abs(h) % 10000) / 10000.0;
    }

    // ======================== СУЩНОСТИ ========================

    @Override
    public void populateEntities(ChunkRegion region) {
        int cx = region.getCenterPos().getStartX();
        int cz = region.getCenterPos().getStartZ();
        Random random = new CheckedRandom(region.getSeed() + region.getCenterPos().toLong());

        // Lurker: Довольно часто (1 шанс из 15)
        if (random.nextInt(15) == 0) {
            spawnEntitySafe(region, random, cx, cz, ModEntities.LURKER);
        }

        // Howler: В 2 раза реже Ларкера (1 шанс из 30)
        if (random.nextInt(30) == 0) {
            spawnEntitySafe(region, random, cx, cz, ModEntities.HOWLER);
        }

        // Mimic: В 10 раз реже Ларкера и в 5 раз реже Хаулера (1 шанс из 150)
        if (random.nextInt(150) == 0) {
            spawnEntitySafe(region, random, cx, cz, ModEntities.MIMIC);
        }

        // Wooden Stalker: ~3.3% шанс на чанк (1 шанс из 30)
        if (random.nextInt(30) == 0) {
            spawnEntitySafe(region, random, cx, cz, ModEntities.WOODEN_STALKER);
        }
    }

    /**
     * Безопасный спавн сущности — ТОЛЬКО внутри лабиринта.
     * Проверяет:
     * - Позиция не в стене
     * - Блок под ногами = пол
     * - Блок на уровне головы = воздух (не потолок)
     * - Y координата строго = 1 (на полу внутри лабиринта)
     */
    private void spawnEntitySafe(ChunkRegion region, Random random,
            int chunkStartX, int chunkStartZ, EntityType<?> type) {
        for (int attempt = 0; attempt < 10; attempt++) {
            int x = chunkStartX + random.nextInt(16);
            int z = chunkStartZ + random.nextInt(16);

            // Проверяем что координата — внутри лабиринта (не стена, не столб)
            if (isWall(x, z)) {
                continue;
            }

            // Проверяем блоки: пол под ногами, воздух на Y=1 и Y=2
            BlockPos floorPos = new BlockPos(x, FLOOR_Y, z);
            BlockPos feetPos = new BlockPos(x, WALL_MIN_Y, z);
            BlockPos headPos = new BlockPos(x, WALL_MIN_Y + 1, z);

            BlockState floorBlock = region.getBlockState(floorPos);
            BlockState feetBlock = region.getBlockState(feetPos);
            BlockState headBlock = region.getBlockState(headPos);

            if (floorBlock.isOf(ModBlocks.BACKROOMS_FLOOR) &&
                    feetBlock.isAir() && headBlock.isAir()) {

                Entity entity = type.create(region.toServerWorld());
                if (entity != null) {
                    // Спавн строго на Y=1 (на полу)
                    entity.refreshPositionAndAngles(
                            x + 0.5, WALL_MIN_Y, z + 0.5,
                            random.nextFloat() * 360.0f, 0.0f);
                    region.spawnEntity(entity);
                    return;
                }
            }
        }
    }

    // ======================== СТАНДАРТНЫЕ МЕТОДЫ ========================

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        return CEILING_Y + 1;
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        BlockState[] states = new BlockState[CEILING_Y + 1];
        boolean infected = isInfected(x, z);
        
        BlockState currentFloor = infected ? Blocks.OAK_PLANKS.getDefaultState() : ModBlocks.BACKROOMS_FLOOR.getDefaultState();
        BlockState currentWall = infected ? Blocks.OAK_PLANKS.getDefaultState() : ModBlocks.BACKROOMS_WALL.getDefaultState();
        BlockState currentCeiling = infected ? Blocks.OAK_PLANKS.getDefaultState() : ModBlocks.BACKROOMS_CEILING.getDefaultState();

        states[FLOOR_Y] = currentFloor;
        boolean wall = isWall(x, z);
        for (int y = WALL_MIN_Y; y <= WALL_MAX_Y; y++) {
            states[y] = wall ? currentWall : Blocks.AIR.getDefaultState();
        }
        states[CEILING_Y] = currentCeiling;
        return new VerticalBlockSample(0, states);
    }

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig,
            BiomeAccess biomeAccess, StructureAccessor structureAccessor,
            Chunk chunk, GenerationStep.Carver carverStep) {
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures,
            NoiseConfig noiseConfig, Chunk chunk) {
    }

    @Override
    public int getMinimumY() {
        return 0;
    }

    @Override
    public int getWorldHeight() {
        return 64;
    }

    @Override
    public int getSeaLevel() {
        return -1;
    }

    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        text.add("Backrooms Level 0");
    }
}
