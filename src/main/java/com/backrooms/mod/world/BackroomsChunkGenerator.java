package com.backrooms.mod.world;

import com.backrooms.mod.block.ModBlocks;
import com.backrooms.mod.block.BlackMoldBlock;
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

    // ======================== НАСТРОЙКИ ГЕНЕРАЦИИ ========================
    // Поднимаем лабиринт на Y=100, чтобы уместить 100 блоков досок снизу
    private static final int FLOOR_Y = 100;
    private static final int WALL_MIN_Y = 101;
    private static final int WALL_MAX_Y = 105; // 5 блоков высота стен
    private static final int CEILING_Y = 106; // Потолок на Y=106

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

                // 100 блоков досок под полом (от 0 до FLOOR_Y - 1)
                BlockState plankState = Blocks.OAK_PLANKS.getDefaultState();
                for (int y = 0; y < FLOOR_Y; y++) {
                    chunk.setBlockState(mutable.set(lx, y, lz), plankState, false);
                }

                // 100 блоков досок над потолком (от CEILING_Y + 1 до 206)
                for (int y = CEILING_Y + 1; y <= 206; y++) {
                    chunk.setBlockState(mutable.set(lx, y, lz), plankState, false);
                }
            }
        }

        // ВТОРОЙ ПРОХОД: Расстановка настенных факелов в зараженной зоне (на высоте 3 блоков от пола)
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int wx = startX + lx;
                int wz = startZ + lz;
                
                if (!isInfected(wx, wz)) continue; // Только в зараженной деревянной зоне
                if (isWall(wx, wz)) continue;      // Нам нужен пустой блок воздуха рядом со стеной
                
                long torchHash = mixHash(WORLD_SEED + 999, wx * 131L, wz * 137L);
                if (Math.abs(torchHash) % 10000 < 21) { // 0.21% шанс
                    net.minecraft.util.math.Direction dir = null;
                    if (isWall(wx - 1, wz)) dir = net.minecraft.util.math.Direction.EAST;
                    else if (isWall(wx + 1, wz)) dir = net.minecraft.util.math.Direction.WEST;
                    else if (isWall(wx, wz - 1)) dir = net.minecraft.util.math.Direction.SOUTH;
                    else if (isWall(wx, wz + 1)) dir = net.minecraft.util.math.Direction.NORTH;

                    if (dir != null) {
                        BlockState torchState = Blocks.WALL_TORCH.getDefaultState().with(net.minecraft.state.property.Properties.HORIZONTAL_FACING, dir);
                        chunk.setBlockState(mutable.set(lx, WALL_MIN_Y + 2, lz), torchState, false); // WALL_MIN_Y + 2 это 3-й блок стены
                    }
                }
            }
        }

        // ТРЕТИЙ ПРОХОД: Генерация пятен чёрной плесени (везде в Backrooms)
        // 8 уровней плотности — от 1-2 пикселей на краю до густой плесени в центре
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int wx = startX + lx;
                int wz = startZ + lz;

                if (isWall(wx, wz)) continue;

                // Многослойный шум для органичных пятен разных размеров (2-17 блоков)
                double moldNoise = simpleNoise(wx / 8.0, wz / 8.0, WORLD_SEED + 700);
                double moldDetail = simpleNoise(wx / 3.0, wz / 3.0, WORLD_SEED + 701);
                double moldMicro = simpleNoise(wx / 1.5, wz / 1.5, WORLD_SEED + 702);
                double moldValue = moldNoise * 0.55 + moldDetail * 0.30 + moldMicro * 0.15;

                // Порог 0.65: начало пятна (единичные пиксели)
                if (moldValue > 0.65) {
                    // Плавный градиент: 0.65 -> density 0, 0.90+ -> density 7
                    double normalized = (moldValue - 0.65) / 0.25; // 0.0 .. 1.0
                    int density = (int) Math.min(7, Math.floor(normalized * 8));

                    BlockState moldState = ModBlocks.BLACK_MOLD.getDefaultState()
                            .with(BlackMoldBlock.DENSITY, density);

                    // Ставим плесень на пол (поверх пола, Y = WALL_MIN_Y)
                    BlockPos floorCheck = new BlockPos(lx, WALL_MIN_Y, lz);
                    if (chunk.getBlockState(floorCheck).isAir()) {
                        chunk.setBlockState(mutable.set(lx, WALL_MIN_Y, lz), moldState, false);
                    }
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

    public static double getInfectionValue(int wx, int wz) {
        // Базовый шум, масштаб 100 блоков (определяет общую форму зоны)
        double baseNoise = simpleNoise(wx / 100.0, wz / 100.0, WORLD_SEED + 500);
        
        // Фрактальный шум для более органичного и красивого перехода (2 слоя)
        double detail1 = simpleNoise(wx / 5.0, wz / 5.0, WORLD_SEED + 501);
        double detail2 = simpleNoise(wx / 2.5, wz / 2.5, WORLD_SEED + 502);
        double detailNoise = (detail1 + detail2 * 0.5) / 1.5;
        
        // Умножая detailNoise на 0.14, мы создаем зону смешивания примерно 7-16 блоков.
        return baseNoise + (detailNoise - 0.5) * 0.14;
    }

    public static boolean isInfected(int wx, int wz) {
        // Если итоговое значение > 0.6 — это деревянная зона
        return getInfectionValue(wx, wz) > 0.6;
    }

    public static double simpleNoise(double x, double z, long seed) {
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

    public static double hashNoiseLocal(int x, int z, long seed) {
        long h = mixHash(seed, (long) x, (long) z);
        return (double) (Math.abs(h) % 10000) / 10000.0;
    }

    // ======================== СУЩНОСТИ ========================

    @Override
    public void populateEntities(ChunkRegion region) {
        int cx = region.getCenterPos().getStartX();
        int cz = region.getCenterPos().getStartZ();
        Random random = new CheckedRandom(region.getSeed() + region.getCenterPos().toLong());

        // Lurker: 1 шанс из 15 (ТОЛЬКО в обычной зоне)
        if (random.nextInt(15) == 0) {
            spawnEntitySafe(region, random, cx, cz, ModEntities.LURKER, false);
        }

        // Howler: 1 шанс из 30 (ТОЛЬКО в обычной зоне)
        if (random.nextInt(30) == 0) {
            spawnEntitySafe(region, random, cx, cz, ModEntities.HOWLER, false);
        }

        // Mimic: 1 шанс из 150 (ТОЛЬКО в обычной зоне)
        if (random.nextInt(150) == 0) {
            spawnEntitySafe(region, random, cx, cz, ModEntities.MIMIC, false);
        }

        // WoodenStalker: спавн по-блочно с шансом 0.22% (ТОЛЬКО в зараженной зоне)
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int wx = cx + lx;
                int wz = cz + lz;

                if (!isInfected(wx, wz)) continue;
                if (isWall(wx, wz)) continue;

                long stalkerHash = mixHash(WORLD_SEED + 888, wx * 151L, wz * 157L);
                if (Math.abs(stalkerHash) % 10000 < 22) { // 0.22% шанс
                    BlockPos feetPos = new BlockPos(wx, WALL_MIN_Y, wz);
                    BlockPos headPos = new BlockPos(wx, WALL_MIN_Y + 1, wz);

                    if (region.getBlockState(feetPos).isAir() && region.getBlockState(headPos).isAir()) {
                        Entity entity = ModEntities.WOODEN_STALKER.create(region.toServerWorld());
                        if (entity != null) {
                            entity.refreshPositionAndAngles(
                                    wx + 0.5, WALL_MIN_Y, wz + 0.5,
                                    random.nextFloat() * 360.0f, 0.0f);
                            region.spawnEntity(entity);
                        }
                    }
                }
            }
        }
    }

    /**
     * Безопасный спавн сущности — ТОЛЬКО внутри лабиринта.
     * @param requireNonInfected если true — моб спавнится ТОЛЬКО в обычной (не зараженной) зоне
     */
    private void spawnEntitySafe(ChunkRegion region, Random random,
            int chunkStartX, int chunkStartZ, EntityType<?> type, boolean requireInfected) {
        for (int attempt = 0; attempt < 10; attempt++) {
            int x = chunkStartX + random.nextInt(16);
            int z = chunkStartZ + random.nextInt(16);

            if (isWall(x, z)) {
                continue;
            }

            // Обычные мобы НЕ спавнятся в зараженной зоне
            if (!requireInfected && isInfected(x, z)) {
                continue;
            }

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
        BlockState[] states = new BlockState[207]; // До 206 Y (включительно)
        boolean infected = isInfected(x, z);
        
        BlockState currentFloor = infected ? Blocks.OAK_PLANKS.getDefaultState() : ModBlocks.BACKROOMS_FLOOR.getDefaultState();
        BlockState currentWall = infected ? Blocks.OAK_PLANKS.getDefaultState() : ModBlocks.BACKROOMS_WALL.getDefaultState();
        BlockState currentCeiling = infected ? Blocks.OAK_PLANKS.getDefaultState() : ModBlocks.BACKROOMS_CEILING.getDefaultState();
        BlockState plankState = Blocks.OAK_PLANKS.getDefaultState();

        for (int y = 0; y < FLOOR_Y; y++) {
            states[y] = plankState;
        }

        states[FLOOR_Y] = currentFloor;
        boolean wall = isWall(x, z);
        for (int y = WALL_MIN_Y; y <= WALL_MAX_Y; y++) {
            states[y] = wall ? currentWall : Blocks.AIR.getDefaultState();
        }
        states[CEILING_Y] = currentCeiling;

        for (int y = CEILING_Y + 1; y <= 206; y++) {
            states[y] = plankState;
        }

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
        return 256;
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
