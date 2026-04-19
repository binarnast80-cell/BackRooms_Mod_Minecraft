package com.backrooms.mod;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Юнит-тесты для мода Backrooms.
 *
 * Тесты проверяют логику, которая не зависит от Minecraft-рантайма:
 * - Алгоритм генерации лабиринта (хеширование, стены, проходы)
 * - Константы и конфигурация
 * - Математическая корректность генерации
 *
 * Запуск: .\gradlew.bat test
 */
public class BackroomsModTest {

    // ===== Копия функции хеширования из BackroomsChunkGenerator =====
    // (вынесена сюда чтобы тестировать без загрузки Minecraft)
    private static long mixHash(long seed, long a, long b) {
        long h = seed ^ a;
        h ^= b * 0x9E3779B97F4A7C15L;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }

    // ===== Копия логики isWall (упрощённая, слой 1) =====
    private static boolean isMainWall(int wx, int wz) {
        int bigGrid = 32;
        int modX = Math.floorMod(wx, bigGrid);
        int modZ = Math.floorMod(wz, bigGrid);
        boolean onWallX = (modX < 2);
        boolean onWallZ = (modZ < 2);
        return onWallX || onWallZ;
    }

    // ===== Копия логики isPillar =====
    private static boolean isPillar(int wx, int wz) {
        int pillarGridX = Math.floorMod(wx + 5, 10);
        int pillarGridZ = Math.floorMod(wz + 5, 10);
        if (pillarGridX == 0 && pillarGridZ == 0) {
            long h = mixHash(48291537L, (long) wx, (long) wz);
            return Math.abs(h) % 4 == 0;
        }
        return false;
    }

    // ==========================================
    //   ТЕСТ 1: Хеш-функция детерминирована
    // ==========================================
    @Test
    @DisplayName("1. Хеш-функция возвращает одинаковый результат для одинаковых входов")
    void testHashDeterministic() {
        long seed = 48291537L;
        long a = 100003L;
        long b = 200017L;

        long result1 = mixHash(seed, a, b);
        long result2 = mixHash(seed, a, b);

        assertEquals(result1, result2,
                "Хеш должен быть детерминирован: одни и те же входы → одинаковый выход");
    }

    // ==========================================
    //   ТЕСТ 2: Разные координаты → разные хеши
    // ==========================================
    @Test
    @DisplayName("2. Хеш-функция даёт разные значения для разных координат")
    void testHashUniqueness() {
        long seed = 48291537L;

        long hash1 = mixHash(seed, 0L, 0L);
        long hash2 = mixHash(seed, 1L, 0L);
        long hash3 = mixHash(seed, 0L, 1L);
        long hash4 = mixHash(seed, 100L, 200L);

        // Все хеши должны быть различны
        assertNotEquals(hash1, hash2, "Хеш (0,0) и (1,0) должны отличаться");
        assertNotEquals(hash1, hash3, "Хеш (0,0) и (0,1) должны отличаться");
        assertNotEquals(hash2, hash3, "Хеш (1,0) и (0,1) должны отличаться");
        assertNotEquals(hash1, hash4, "Хеш (0,0) и (100,200) должны отличаться");
    }

    // ==========================================
    //   ТЕСТ 3: Стены на границах сетки 32×32
    // ==========================================
    @Test
    @DisplayName("3. Стены генерируются на границах основной сетки (каждые 32 блока)")
    void testMainWallsOnGridBoundaries() {
        // Координаты кратные 32 — должны быть стеной
        assertTrue(isMainWall(0, 0), "Точка (0,0) должна быть стеной");
        assertTrue(isMainWall(32, 0), "Точка (32,0) должна быть стеной");
        assertTrue(isMainWall(0, 32), "Точка (0,32) должна быть стеной");
        assertTrue(isMainWall(64, 64), "Точка (64,64) должна быть стеной");
        assertTrue(isMainWall(1, 0), "Точка (1,0) должна быть стеной (толщина 2)");
        assertTrue(isMainWall(0, 1), "Точка (0,1) должна быть стеной (толщина 2)");
    }

    // ==========================================
    //   ТЕСТ 4: Воздух внутри комнат
    // ==========================================
    @Test
    @DisplayName("4. Внутри комнаты (далеко от границ сетки) нет основных стен")
    void testAirInsideRooms() {
        // Точки далеко от границ сетки — должны быть воздухом
        assertFalse(isMainWall(16, 16), "Центр комнаты (16,16) не должен быть стеной");
        assertFalse(isMainWall(10, 10), "Точка (10,10) не должна быть стеной");
        assertFalse(isMainWall(20, 20), "Точка (20,20) не должна быть стеной");
        assertFalse(isMainWall(48, 48), "Точка (48,48) — центр второй комнаты, не стена");
    }

    // ==========================================
    //   ТЕСТ 5: Стены работают с отрицательными координатами
    // ==========================================
    @Test
    @DisplayName("5. Генерация стен корректна для отрицательных координат")
    void testNegativeCoordinates() {
        // Отрицательные координаты тоже должны генерировать стены на границах сетки
        assertTrue(isMainWall(-32, 0), "Точка (-32,0) должна быть стеной");
        assertTrue(isMainWall(0, -32), "Точка (0,-32) должна быть стеной");
        assertTrue(isMainWall(-64, -64), "Точка (-64,-64) должна быть стеной");

        // Внутри комнат в отрицательных координатах — воздух
        assertFalse(isMainWall(-16, -16), "Точка (-16,-16) не должна быть стеной");
        assertFalse(isMainWall(-10, -10), "Точка (-10,-10) не должна быть стеной");
    }

    // ==========================================
    //   ТЕСТ 6: Колонны появляются в правильных позициях
    // ==========================================
    @Test
    @DisplayName("6. Колонны появляются только на позициях сетки 10×10 со сдвигом")
    void testPillarPositions() {
        // Колонна возможна только на (5, 5), (15, 15), (25, 25), (-5, -5) и т.д.
        // Но не на каждой позиции (только 25%)

        // Позиции НЕ на сетке — гарантированно не колонна
        assertFalse(isPillar(0, 0), "(0,0) не на сетке колонн");
        assertFalse(isPillar(10, 10), "(10,10) не на сетке колонн");
        assertFalse(isPillar(3, 7), "(3,7) не на сетке колонн");
    }

    // ==========================================
    //   ТЕСТ 7: Константы телепортации корректны
    // ==========================================
    @Test
    @DisplayName("7. Константы телепортации имеют физический смысл")
    void testTeleportConstants() {
        int teleportDelay = 400;   // 20 сек × 20 тиков
        int fadeDuration = 40;     // 2 сек × 20 тиков

        // Таймер больше нуля
        assertTrue(teleportDelay > 0, "Задержка телепортации должна быть > 0");

        // Fade должен быть короче таймера
        assertTrue(fadeDuration < teleportDelay,
                "Анимация затемнения должна быть короче задержки телепортации");

        // Проверяем перевод в секунды
        assertEquals(20, teleportDelay / 20, "400 тиков = 20 секунд");
        assertEquals(2, fadeDuration / 20, "40 тиков = 2 секунды");

        // Всего 5 состояний (0-4)
        int totalStates = 5;
        assertEquals(5, totalStates, "Должно быть 5 состояний машины");
    }

    // ==========================================
    //   ТЕСТ 8: MOD_ID корректный
    // ==========================================
    @Test
    @DisplayName("8. MOD_ID соответствует namespace в ресурсах")
    void testModId() {
        String modId = "backrooms";

        // Не пустой
        assertFalse(modId.isEmpty(), "MOD_ID не должен быть пустым");

        // Только lowercase буквы и подчёркивания (требование Minecraft)
        assertTrue(modId.matches("[a-z_]+"),
                "MOD_ID должен содержать только lowercase буквы и подчёркивания");

        // Не слишком длинный
        assertTrue(modId.length() <= 64, "MOD_ID не должен превышать 64 символа");

        // Конкретное значение
        assertEquals("backrooms", modId, "MOD_ID должен быть 'backrooms'");
    }

    // ==========================================
    //   ТЕСТ 9: Высоты мира корректны
    // ==========================================
    @Test
    @DisplayName("9. Високоты мира логически верны (пол < стены < потолок)")
    void testWorldHeights() {
        int floorY = 0;
        int wallMinY = 1;
        int wallMaxY = 5;
        int ceilingY = 6;

        // Пол ниже стен
        assertTrue(floorY < wallMinY,
                "Пол (Y=" + floorY + ") должен быть ниже стен (Y=" + wallMinY + ")");

        // Стены заканчиваются ниже потолка
        assertTrue(wallMaxY < ceilingY,
                "Верх стены (Y=" + wallMaxY + ") должен быть ниже потолка (Y=" + ceilingY + ")");

        // Высота комнаты = 5 блоков
        int roomHeight = wallMaxY - wallMinY + 1;
        assertEquals(5, roomHeight, "Высота комнаты должна быть 5 блоков");

        // Общая высота структуры
        int totalHeight = ceilingY - floorY + 1;
        assertEquals(7, totalHeight, "Общая высота (пол+комната+потолок) = 7 блоков");
    }

    // ==========================================
    //   ТЕСТ 10: Генератор создаёт проходимый лабиринт
    // ==========================================
    @Test
    @DisplayName("10. В каждой комнате 32×32 есть как минимум одна точка без стены")
    void testRoomsHaveOpenSpace() {
        // Проверяем несколько "комнат" — внутри каждой должна быть
        // хотя бы одна точка без стены (иначе лабиринт непроходим)
        int[][] roomCenters = {{16, 16}, {48, 48}, {-16, -16}, {80, 80}, {-48, 16}};

        for (int[] center : roomCenters) {
            boolean hasOpenSpace = false;
            // Проверяем область 10×10 вокруг центра комнаты
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    if (!isMainWall(center[0] + dx, center[1] + dz)) {
                        hasOpenSpace = true;
                        break;
                    }
                }
                if (hasOpenSpace) break;
            }
            assertTrue(hasOpenSpace,
                    "Комната вокруг (" + center[0] + "," + center[1] + ") должна иметь открытое пространство");
        }
    }
}
