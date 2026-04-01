# 📚 Backrooms Mod — Полная техническая документация

> Этот документ подробно описывает **каждый** аспект проекта: архитектуру, код, используемые методы, API, JSON-конфигурации и команды. После прочтения вы будете полностью понимать, как устроен мод изнутри.

---

## 📋 Оглавление

1. [Технологический стек](#-технологический-стек)
2. [Система сборки (Gradle)](#-система-сборки-gradle)
3. [Точки входа мода](#-точки-входа-мода)
4. [Блоки (ModBlocks)](#-блоки-modblocks)
5. [Предметы (ModItems)](#-предметы-moditems)
6. [Сущности (Entities)](#-сущности-entities)
7. [Измерение и генерация мира](#-измерение-и-генерация-мира)
8. [Телепортация (BackroomsTeleportHandler)](#-телепортация-backroomsteleporthandler)
9. [Сетевые пакеты (ModNetworking)](#-сетевые-пакеты-modnetworking)
10. [Клиентский оверлей (BackroomsOverlay)](#-клиентский-оверлей-backroomsoverlay)
11. [Звуковая система (BackroomsAmbientSound)](#-звуковая-система-backroomsambientsound)
12. [JSON ресурсы](#-json-ресурсы)
13. [Текстуры](#-текстуры)
14. [Полезные команды](#-полезные-команды)
15. [Полная схема взаимодействия](#-полная-схема-взаимодействия)

---

## 🛠 Технологический стек

| Компонент | Версия | Назначение |
|-----------|--------|------------|
| **Minecraft** | 1.20.1 | Основная игра |
| **Fabric Loader** | 0.15.7 | Загрузчик модов |
| **Fabric API** | 0.92.0 | Набор API для мододелов |
| **Fabric Loom** | 1.7.4 | Плагин сборки (Gradle) |
| **Yarn Mappings** | 1.20.1+build.10 | Маппинги обфусцированного кода MC |
| **Java** | 17+ (у нас 21) | Язык программирования |
| **Gradle** | 8.8 | Система сборки |

### Почему Fabric, а не Forge?

- **Fabric** — легковесный, быстрый, минималистичный
- Быстрая загрузка (Mixin-based, не ASM-патчи)
- Fabric API модульный — подключаем только то, что нужно
- Лучше подходит для генерации мира и сетевых пакетов

---

## 📦 Система сборки (Gradle)

### Файл `build.gradle`

```gradle
plugins {
    id 'fabric-loom' version '1.7-SNAPSHOT'  // Плагин Fabric Loom
    id 'maven-publish'
}

dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    mappings "net.fabricmc:yarn:${project.yarn_mappings}:v2"
    modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
    modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"
}
```

**Что делает каждая зависимость:**

| Зависимость | Что это |
|-------------|---------|
| `minecraft` | Исходный код Minecraft (декомпилированный) |
| `mappings` | Перевод обфусцированных имён (a.b.c → PlayerEntity, setHealth и т.д.) |
| `fabric-loader` | Загрузчик Fabric — подгружает наш мод в Minecraft |
| `fabric-api` | Набор из 50+ мини-модулей: события, блоки, сущности, сеть и т.д. |

### Файл `gradle.properties`

```properties
minecraft_version=1.20.1
yarn_mappings=1.20.1+build.10
loader_version=0.15.7
fabric_version=0.92.0+1.20.1
```

### Файл `gradle/wrapper/gradle-wrapper.properties`

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.8-bin.zip
networkTimeout=120000  # Таймаут 120 сек (увеличен из-за медленной сети)
```

### Команды сборки

| Команда | Что делает |
|---------|------------|
| `.\gradlew.bat build` | Компилирует мод → JAR файл в `build/libs/` |
| `.\gradlew.bat runClient` | Запускает Minecraft с модом (dev-режим) |
| `.\gradlew.bat clean` | Удаляет все скомпилированные файлы |
| `.\gradlew.bat build --no-daemon` | Сборка без фонового демона (экономит RAM) |

---

## 🚪 Точки входа мода

Fabric использует систему **entrypoints** — точки, через которые мод запускается.

### Определение в `fabric.mod.json`:

```json
"entrypoints": {
    "main": ["com.backrooms.mod.BackroomsMod"],
    "client": ["com.backrooms.mod.BackroomsModClient"]
}
```

| Точка | Класс | Когда запускается | Где работает |
|-------|-------|-------------------|--------------|
| `main` | `BackroomsMod` | При загрузке мода | Сервер + Клиент |
| `client` | `BackroomsModClient` | При загрузке мода | Только клиент |

### `BackroomsMod.java` — серверная точка входа

```java
public class BackroomsMod implements ModInitializer {
    public static final String MOD_ID = "backrooms";        // Уникальный ID мода
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);  // Логгер

    @Override
    public void onInitialize() {
        ModBlocks.register();               // 1. Регистрация блоков
        ModItems.register();                // 2. Регистрация предметов
        ModSounds.register();               // 3. Регистрация звуков
        ModEntities.register();             // 4. Регистрация сущностей
        ModDimensions.register();           // 5. Регистрация измерения + генератора
        ModNetworking.registerC2SPackets(); // 6. Серверные приёмники пакетов
        BackroomsTeleportHandler.register(); // 7. Тик-обработчик телепортации
    }
}
```

**Порядок регистрации важен!** Блоки → Предметы → Звуки → Сущности → Измерения → Сеть → Обработчики.

### `BackroomsModClient.java` — клиентская точка входа

```java
public class BackroomsModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModNetworking.registerS2CPackets();  // Приёмник пакетов от сервера
        BackroomsOverlay.register();          // HUD-оверлей затемнения
        BackroomsAmbientSound.register();     // Фоновые звуки

        // Рендереры сущностей (как они выглядят)
        EntityRendererRegistry.register(ModEntities.LURKER, LurkerRenderer::new);
        EntityRendererRegistry.register(ModEntities.HOWLER, HowlerRenderer::new);
    }
}
```

**Зачем разделять?** На сервере нет графики — нельзя регистрировать рендереры, звуки и HUD. Иначе будет краш на dedicated server.

---

## 🧱 Блоки (ModBlocks)

### Как создаются блоки в Fabric

```java
public static final Block BACKROOMS_WALL = new Block(
    FabricBlockSettings.create()       // Создаём настройки блока
        .strength(50.0f, 1200.0f)      // (прочность, устойчивость к взрывам)
        .sounds(BlockSoundGroup.STONE)  // Звук при ходьбе/ломании
        .requiresTool()                 // Ломается только инструментом
);
```

### Три блока мода

| Блок | Прочность | Взрывоустойчивость | Свет | Звук | Особенности |
|------|-----------|-------------------|------|------|-------------|
| `BACKROOMS_WALL` | 50.0 | 1200.0 | 0 | Stone | `requiresTool()` |
| `BACKROOMS_FLOOR` | -1.0 | 3 600 000.0 | 0 | Stone | `dropsNothing()` — неломаемый |
| `BACKROOMS_CEILING` | -1.0 | 3 600 000.0 | 12 | Glass | `dropsNothing()`, `luminance(12)` |

### Что означает `strength(-1.0f)`?

В Minecraft `-1.0f` hardness — это **специальное значение**, означающее "неразрушимый блок". Так работает бедрок. Ни один инструмент, включая алмазную кирку с Efficiency V, не может его сломать. Даже в креативе он ломается только ЛКМ.

### Что означает `luminance(state -> 12)`?

`luminance` — уровень света, который **излучает** блок. Значение от 0 (нет света) до 15 (максимум). `12` — яркий свет, аналог факелу.

`state -> 12` — это **лямбда-функция**: принимает `BlockState` и возвращает `int`. Мы всегда возвращаем 12, но можно сделать так, чтобы свет зависел от состояния блока.

### Регистрация блоков

```java
Registry.register(
    Registries.BLOCK,                                          // Реестр блоков MC
    new Identifier(BackroomsMod.MOD_ID, "backrooms_wall"),     // ID: "backrooms:backrooms_wall"
    BACKROOMS_WALL                                              // Объект блока
);
```

**`Identifier`** — это пара `namespace:path`. У нас `namespace = "backrooms"`, `path = "backrooms_wall"`. Полный ID: `backrooms:backrooms_wall`.

---

## 📦 Предметы (ModItems)

Каждый блок нужно также зарегистрировать как **предмет (item)**, чтобы его можно было держать в руках и ставить.

```java
Registry.register(
    Registries.ITEM,
    new Identifier(BackroomsMod.MOD_ID, "backrooms_wall"),
    new BlockItem(ModBlocks.BACKROOMS_WALL,                    // Привязка к блоку
        new FabricItemSettings())                               // Настройки предмета
);
```

**`BlockItem`** — специальный класс, который связывает предмет с блоком. Когда игрок ставит предмет — он превращается в блок.

---

## 👾 Сущности (Entities)

### Архитектура сущностей в Minecraft

```
Entity (базовый)
  └─ LivingEntity (имеет HP, броню)
       └─ MobEntity (имеет AI goals)
            └─ HostileEntity (враждебный моб)
                 ├─ LurkerEntity (наш)
                 └─ HowlerEntity (наш)
```

### Как работает AI (система целей)

Minecraft использует систему **Goals** (целей) для AI мобов. Каждая цель имеет **приоритет** (0 = высший):

```java
protected void initGoals() {
    // goalSelector — ЧТО делать
    this.goalSelector.add(0, new SwimGoal(this));           // 0: Плавать (чтобы не утонуть)
    this.goalSelector.add(1, new MeleeAttackGoal(this,      // 1: Атаковать в ближнем бою
            1.2,         // скорость при атаке (множитель)
            false));     // false = не требует видимость цели
    this.goalSelector.add(2, new WanderAroundFarGoal(this,  // 2: Бродить
            0.8));       // скорость при блуждании
    this.goalSelector.add(3, new LookAtEntityGoal(this,     // 3: Смотреть на игрока
            PlayerEntity.class, 16.0f));  // дальность 16 блоков
    this.goalSelector.add(4, new LookAroundGoal(this));     // 4: Оглядываться

    // targetSelector — КОГО атаковать
    this.targetSelector.add(1, new RevengeGoal(this));           // 1: Мстить при ударе
    this.targetSelector.add(2, new ActiveTargetGoal<>(this,      // 2: Охотиться на игрока
            PlayerEntity.class, true));  // true = нужна линия видимости
}
```

**Как это работает:**
1. Каждый тик (1/20 сек) Minecraft проверяет все цели
2. Выполняется цель с **наименьшим номером**, условие которой выполнено
3. Если моб видит игрока → `ActiveTargetGoal` устанавливает цель
4. `MeleeAttackGoal` начинает преследование и атаку
5. Если цели нет → `WanderAroundFarGoal` заставляет бродить

### Атрибуты сущности

```java
public static DefaultAttributeContainer.Builder createLurkerAttributes() {
    return HostileEntity.createHostileAttributes()
        .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)     // HP (2 HP = 1 сердце)
        .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0)   // Урон за удар
        .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.38)  // Скорость (зомби = 0.23)
        .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0)    // Радиус обнаружения
        .add(EntityAttributes.GENERIC_ARMOR, 2.0);           // Броня
}
```

**Сравнение скоростей:**

| Сущность | Скорость | Относительно |
|----------|----------|-------------|
| Зомби | 0.23 | Медленный |
| Игрок (ходьба) | 0.1 | Базовая |
| Игрок (бег) | 0.13 | Sprint |
| **Lurker** | **0.38** | **Очень быстрый!** |
| **Howler** | **0.2** | Медленнее зомби |

### Регистрация сущностей

```java
public static final EntityType<LurkerEntity> LURKER = Registry.register(
    Registries.ENTITY_TYPE,
    new Identifier(BackroomsMod.MOD_ID, "lurker"),
    FabricEntityTypeBuilder.create(
            SpawnGroup.MONSTER,      // Категория: монстр
            LurkerEntity::new        // Конструктор (ссылка на метод)
        )
        .dimensions(EntityDimensions.fixed(0.6f, 1.8f))  // Хитбокс: 0.6 × 1.8
        .trackRangeChunks(8)         // Видимость на расстоянии 8 чанков
        .build()
);
```

### SpawnRestriction (ограничения спавна)

```java
SpawnRestriction.register(LURKER,
    SpawnRestriction.Location.ON_GROUND,              // Спавн на земле
    Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,          // Высотная карта
    (type, world, reason, pos, random) -> true         // Условие: ВСЕГДА ДА
);
```

Без `SpawnRestriction` Minecraft не спавнит мобов. Условие `-> true` означает "спавнить при любом уровне света" (важно для Backrooms, где всегда светло).

### Рендереры сущностей

```java
public class LurkerRenderer extends ZombieEntityRenderer {
    private static final Identifier TEXTURE =
        new Identifier(BackroomsMod.MOD_ID, "textures/entity/lurker.png");

    public LurkerRenderer(EntityRendererFactory.Context context) {
        super(context);  // Используем модель зомби
    }

    @Override
    public Identifier getTexture(ZombieEntity entity) {
        return TEXTURE;  // Но с нашей текстурой
    }
}
```

**Почему наследуем от `ZombieEntityRenderer`?** Чтобы не создавать 3D-модель с нуля. Модель зомби уже есть в Minecraft — мы просто меняем текстуру.

---

## 🌐 Измерение и генерация мира

### Как Minecraft хранит измерения

```
data/backrooms/
├── dimension_type/backrooms_type.json    # ТИП измерения (физика, свет)
├── dimension/the_backrooms.json          # САМО измерение (генератор)
└── worldgen/biome/backrooms_biome.json   # БИОМ (цвета, спавн)
```

### `dimension_type/backrooms_type.json` — физика измерения

```json
{
    "ultrawarm": false,         // Не ад (вода не испаряется)
    "natural": false,            // Компас и часы не работают
    "piglin_safe": true,         // Пиглины не злятся
    "respawn_anchor_works": false,// Якорь возрождения НЕ работает
    "bed_works": false,          // Кровать НЕ работает (не установить спавн)
    "has_raids": false,          // Рейды не происходят
    "has_skylight": false,       // НЕТ солнечного света!
    "has_ceiling": true,         // Есть потолок
    "coordinate_scale": 1.0,     // 1 блок = 1 блок (не как Нижний мир)
    "ambient_light": 0.5,        // Фоновый свет 50%
    "logical_height": 64,        // Макс. высота для телепортации
    "infiniburn": "#minecraft:infiniburn_overworld",
    "min_y": 0,                  // Минимальная высота
    "height": 64,                // Общая высота мира
    "monster_spawn_light_level": 15, // Монстры спавнятся при ЛЮБОМ свете!
    "monster_spawn_block_light_limit": 15
}
```

### `dimension/the_backrooms.json` — определение измерения

```json
{
    "type": "backrooms:backrooms_type",    // Ссылка на dimension_type
    "generator": {
        "type": "backrooms:backrooms_chunkgen",  // Наш кастомный генератор
        "biome_source": {
            "type": "minecraft:fixed",             // Один биом на всю карту
            "biome": "backrooms:backrooms_biome"   // Наш биом
        }
    }
}
```

### `ModDimensions.java` — регистрация в Java

```java
// Ключ мира — используется для проверки: "игрок в Backrooms?"
public static final RegistryKey<World> BACKROOMS_LEVEL_KEY =
    RegistryKey.of(RegistryKeys.WORLD, new Identifier("backrooms", "the_backrooms"));

// Регистрация КОДЕКА генератора (чтобы Minecraft мог его загрузить из JSON)
Registry.register(Registries.CHUNK_GENERATOR,
    new Identifier("backrooms", "backrooms_chunkgen"),
    BackroomsChunkGenerator.CODEC);
```

**Что такое CODEC?** Это сериализатор/десериализатор. Minecraft загружает JSON → CODEC преобразует его в объект Java (`BackroomsChunkGenerator`).

### `BackroomsChunkGenerator.java` — генератор лабиринта

Это **самый сложный файл** в проекте (~300 строк). Разберём его по частям.

#### Наследование

```java
public class BackroomsChunkGenerator extends ChunkGenerator { ... }
```

`ChunkGenerator` — абстрактный класс Minecraft. Мы переопределяем его методы для генерации наших блоков.

#### Методы, которые мы переопределяем

| Метод | Что делает |
|-------|------------|
| `populateNoise()` | **Главный!** Заполняет чанк блоками |
| `populateEntities()` | Спавнит сущностей при генерации чанка |
| `getHeight()` | Возвращает высоту поверхности для координат |
| `getColumnSample()` | Вертикальный срез мира (для предпросмотра) |
| `carve()` | Карвинг (пещеры) — у нас пустой |
| `buildSurface()` | Поверхностный слой — у нас пустой |
| `getCodec()` | Возвращает CODEC для сериализации |

#### Как работает `populateNoise()`

```java
public CompletableFuture<Chunk> populateNoise(..., Chunk chunk) {
    int startX = chunk.getPos().getStartX();  // Мировая X координата первого блока в чанке
    int startZ = chunk.getPos().getStartZ();  // Мировая Z координата

    for (int lx = 0; lx < 16; lx++) {         // Перебираем 16×16 блоков чанка
        for (int lz = 0; lz < 16; lz++) {
            int wx = startX + lx;              // Мировая координата
            int wz = startZ + lz;

            // Y=0: Пол (неуязвимый)
            chunk.setBlockState(mutable.set(lx, 0, lz), floorState, false);

            // Y=6: Потолок (неуязвимый + свет)
            chunk.setBlockState(mutable.set(lx, 6, lz), ceilingState, false);

            // Y=1..5: Стена или воздух
            boolean wall = isWall(wx, wz);
            for (int y = 1; y <= 5; y++) {
                chunk.setBlockState(mutable.set(lx, y, lz),
                    wall ? wallState : airState, false);
            }
        }
    }
    return CompletableFuture.completedFuture(chunk);
}
```

**Что такое чанк?** Minecraft делит мир на кусочки 16×16 блоков (чанки). Генератор вызывается для каждого чанка отдельно.

#### Алгоритм определения стен — `isWall(wx, wz)`

Используется **4-слойная сетка**:

```
Слой 1: Основные стены (каждые 32 блока)
         ┌─────────────────────────┐
         │                         │
         │    Огромный зал 30×30   │
         │                         │
         │         ┌──────┐        │
         │         │ Дверь │        │
─────────┤         └──────┘        ├──────────
         │                         │
         │                         │
         └─────────────────────────┘

Слой 2: Средние стены (каждые 16 блоков) — 60% секторов
         ┌──────────┬──────────────┐
         │          │              │
         │ Комната  ╞══ Коридор    │
         │          │              │
         └──────────┴──────────────┘

Слой 3: Мелкие перегородки (каждые 8 блоков) — 20% зон
         ┌────┬────┐
         │ ○  │  ○ │  (офисные кабинки)
         ├────┤    │
         │    │  ○ │
         └────┴────┘

Слой 4: Колонны — одиночные блоки в залах (25%)
```

#### Функция хеширования

```java
private static long mixHash(long seed, long a, long b) {
    long h = seed ^ a;
    h ^= b * 0x9E3779B97F4A7C15L;  // Золотое сечение × 2^64
    h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
    h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
    return h ^ (h >>> 31);
}
```

Это **SplitMix64** — быстрый хеш с отличным распределением. Почему он, а не `Random`?

- **Детерминизм**: одни и те же координаты = одинаковый результат (мир не меняется)
- **Независимость от чанков**: каждый блок вычисляется независимо
- **Скорость**: нет объекта `Random`, нет состояния, нет блокировок

#### Безопасный спавн сущностей

```java
private void spawnEntitySafe(ChunkRegion region, Random random, ...) {
    for (int attempt = 0; attempt < 10; attempt++) {  // До 10 попыток
        int x = chunkStartX + random.nextInt(16);
        int z = chunkStartZ + random.nextInt(16);

        // Проверка 1: Не в стене?
        if (isWall(x, z)) continue;

        // Проверка 2: Пол под ногами?
        if (!region.getBlockState(floorPos).isOf(ModBlocks.BACKROOMS_FLOOR)) continue;

        // Проверка 3: Воздух на Y=1 и Y=2?
        if (!region.getBlockState(feetPos).isAir()) continue;
        if (!region.getBlockState(headPos).isAir()) continue;

        // Всё ок — спавним на Y=1 (строго внутри лабиринта!)
        entity.refreshPositionAndAngles(x + 0.5, 1, z + 0.5, ...);
        region.spawnEntity(entity);
        return;
    }
}
```

**Зачем 10 попыток?** Если первая случайная позиция — стена, пробуем другую. 10 попыток достаточно для чанка 16×16.

---

## 🔄 Телепортация (BackroomsTeleportHandler)

### Машина состояний

Телепортация использует **конечный автомат** с 5 состояниями:

```
                    400 тиков (20 сек)
    ┌──────────┐  ─────────────────>  ┌──────────┐
    │ Состояние │                      │ Состояние │
    │    0      │                      │    1      │
    │ (отсчёт) │                      │ (fade-in) │
    └──────────┘                      └──────────┘
                                          │
                                   40 тиков (2 сек)
                                          │
                                          ▼
    ┌──────────┐   20 тиков (1 сек)  ┌──────────┐
    │ Состояние │  <─────────────────  │ Состояние │
    │    3      │                      │    2      │
    │(fade-out) │                      │ (телепорт)│
    └──────────┘                      └──────────┘
         │
  40 тиков (2 сек)
         │
         ▼
    ┌──────────┐
    │ Состояние │
    │    4      │
    │(завершено)│
    └──────────┘
```

### Используемые API Fabric

```java
// Тик-обработчик сервера (вызывается 20 раз/сек)
ServerTickEvents.END_SERVER_TICK.register(server -> {
    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
        // Логика для каждого игрока
    }
});

// Обработка отключения игрока
ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
    UUID uuid = handler.getPlayer().getUuid();
    playerTicks.remove(uuid);
});
```

### Как работает телепортация между измерениями

```java
private static void teleportToBackrooms(ServerPlayerEntity player) {
    // Получаем объект мира Backrooms
    ServerWorld backroomsWorld = player.getServer()
        .getWorld(ModDimensions.BACKROOMS_LEVEL_KEY);

    // Телепортация
    player.teleport(
        backroomsWorld,    // Целевой мир
        3.5,               // X (центр первой комнаты)
        1.0,               // Y (на уровне пола + 1)
        3.5,               // Z
        player.getYaw(),   // Сохраняем поворот
        player.getPitch()
    );
}
```

---

## 📡 Сетевые пакеты (ModNetworking)

### Зачем нужна сеть?

Minecraft работает по схеме **Клиент ↔ Сервер** даже в одиночной игре. Сервер решает, КОГДА телепортировать. Но экран может темнеть только на **клиенте**. Поэтому нужны пакеты:

```
Сервер                              Клиент
  │                                   │
  │──── FADE_PACKET (fadeIn=true) ──>│  Экран темнеет
  │                                   │
  │ (ждёт 40 тиков)                  │ (анимация 2 сек)
  │                                   │
  │──── Телепортация ────────────────>│  Игрок перемещается
  │                                   │
  │──── FADE_PACKET (fadeIn=false) ─>│  Экран светлеет
  │                                   │
```

### Как устроены пакеты

**Отправка (сервер → клиент):**

```java
public static void sendFadePacket(ServerPlayerEntity player, boolean fadeIn) {
    PacketByteBuf buf = PacketByteBufs.create();  // Создаём буфер данных
    buf.writeBoolean(fadeIn);                      // Записываем 1 boolean
    ServerPlayNetworking.send(player, FADE_PACKET_ID, buf);  // Отправляем
}
```

**Приём (клиент):**

```java
ClientPlayNetworking.registerGlobalReceiver(FADE_PACKET_ID,
    (client, handler, buf, responseSender) -> {
        boolean fadeIn = buf.readBoolean();         // Читаем boolean из буфера
        client.execute(() -> {                      // Выполняем в рендер-потоке!
            BackroomsOverlay.startFade(fadeIn);      // Запускаем эффект
        });
    }
);
```

**Зачем `client.execute()`?** Пакеты приходят в **сетевом потоке**, а HUD рисуется в **рендер-потоке**. Если изменить состояние из неправильного потока — краш. `execute()` гарантирует выполнение в правильном потоке.

---

## 🌑 Клиентский оверлей (BackroomsOverlay)

### Как рисуется затемнение

```java
HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
    // drawContext — контекст рисования Minecraft (GUI)
    // tickDelta — дробная часть тика (для плавности)

    if (fadingIn) {
        fadeAlpha = (float) fadeTimer / FADE_IN_TICKS;  // 0.0 → 1.0 за 40 тиков
    } else {
        fadeAlpha = 1.0f - (float) fadeTimer / FADE_OUT_TICKS;  // 1.0 → 0.0
    }

    // Рисуем чёрный прямоугольник поверх ВСЕГО
    int alpha = ((int)(fadeAlpha * 255.0f)) << 24;  // Сдвиг в старшие 8 бит
    drawContext.fill(0, 0, width, height, alpha);    // Цвет: 0x{AA}000000
});
```

**Формат цвета:** `ARGB` — 32-битное число: `AARRGGBB`
- `alpha << 24` = `0xAA000000` — чёрный с прозрачностью `AA`
- Когда `alpha = 255` → полностью непрозрачный → чёрный экран
- Когда `alpha = 0` → полностью прозрачный → нормальный вид

---

## 🎵 Звуковая система (BackroomsAmbientSound)

### Автоматическое включение/выключение

```java
ClientTickEvents.END_CLIENT_TICK.register(client -> {
    boolean inBackrooms = client.player.getWorld().getRegistryKey()
        == ModDimensions.BACKROOMS_LEVEL_KEY;

    if (inBackrooms && !isPlaying) {
        startSounds(client);    // Вошёл в Backrooms → включить звуки
    } else if (!inBackrooms && isPlaying) {
        stopSounds(client);     // Вышел из Backrooms → выключить
    }
});
```

### Зацикленный звук (MovingSoundInstance)

```java
private static class BackroomsLoopSound extends MovingSoundInstance {
    public BackroomsLoopSound(PlayerEntity player, SoundEvent sound, float volume) {
        super(sound, SoundCategory.AMBIENT, Random.create());
        this.repeat = true;                        // ЗАЦИКЛЕННЫЙ
        this.volume = volume;                      // Громкость (0.0 - 1.0)
        this.attenuationType = AttenuationType.NONE; // Не затухает с расстоянием
    }

    @Override
    public void tick() {
        // Обновляем позицию звука = позиция игрока
        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();
    }
}
```

**`AttenuationType.NONE`** — звук одинаково громкий независимо от расстояния. Идеально для эмбиента, который "везде вокруг".

---

## 📋 JSON ресурсы

### `fabric.mod.json` — манифест мода

```json
{
    "schemaVersion": 1,
    "id": "backrooms",                           // Уникальный ID
    "version": "1.0.0",
    "name": "Backrooms",
    "environment": "*",                           // Работает везде
    "entrypoints": {
        "main": ["com.backrooms.mod.BackroomsMod"],      // Сервер
        "client": ["com.backrooms.mod.BackroomsModClient"] // Клиент
    },
    "depends": {
        "fabricloader": ">=0.15.0",
        "fabric-api": "*",
        "minecraft": "~1.20.1"                    // Совместимость 1.20.x
    }
}
```

### `sounds.json` — определение звуков

```json
{
    "lamp_hum": {
        "sounds": ["backrooms:sounds/lamp_hum"],  // Путь к .ogg файлу
        "subtitle": "Fluorescent lamp humming"
    }
}
```

**Где искать .ogg файл?** Minecraft ищет по пути:
`assets/backrooms/sounds/lamp_hum.ogg`

### Blockstates / Models — как блок рендерится

```
blockstates/backrooms_wall.json  →  "Какую модель использовать?"
      ↓
models/block/backrooms_wall.json →  "Форма куба + какую текстуру натянуть"
      ↓
textures/block/backrooms_wall.png → Сама картинка 16×16
```

---

## 🎨 Текстуры

### Формат текстур блоков

- **Размер:** 16×16 пикселей (стандарт Minecraft)
- **Формат:** PNG с прозрачностью (RGBA)
- **Расположение:** `assets/backrooms/textures/block/`

### Формат текстур сущностей

- **Размер:** 64×64 пикселей
- **Формат:** Стандартный скин модели зомби (развёртка UV)
- **Расположение:** `assets/backrooms/textures/entity/`

Текущие текстуры — **placeholder** (сплошной цвет). Замените их для улучшения внешнего вида.

---

## 💻 Полезные команды

### Gradle

| Команда | Описание |
|---------|----------|
| `.\gradlew.bat build` | Скомпилировать мод |
| `.\gradlew.bat runClient` | Запустить Minecraft с модом |
| `.\gradlew.bat clean` | Очистить кеш сборки |
| `.\gradlew.bat build --no-daemon` | Сборка без фонового процесса |

### Git

| Команда | Описание |
|---------|----------|
| `git add .` | Добавить все изменения |
| `git commit -m "сообщение"` | Зафиксировать изменения |
| `git push` | Отправить на GitHub |
| `git pull` | Скачать изменения с GitHub |
| `git status` | Посмотреть статус файлов |
| `git log -5` | Последние 5 коммитов |

### Minecraft (в чате игры, клавиша T)

| Команда | Описание |
|---------|----------|
| `/gamemode creative` | Креативный режим |
| `/gamemode survival` | Режим выживания |
| `/tp @s 3 1 3` | Телепорт на координаты |
| `/kill @e[type=backrooms:lurker]` | Убить всех Lurker |
| `/kill @e[type=backrooms:howler]` | Убить всех Howler |
| `/summon backrooms:lurker` | Призвать Lurker |
| `/summon backrooms:howler` | Призвать Howler |

---

## 🔗 Полная схема взаимодействия

```
                           ┌─────────────────────┐
                           │   fabric.mod.json    │
                           │  (точки входа мода)  │
                           └─────────┬───────────┘
                                     │
               ┌─────────────────────┼─────────────────────┐
               ▼                                           ▼
    ┌─────────────────┐                         ┌─────────────────────┐
    │  BackroomsMod    │                         │ BackroomsModClient  │
    │ (СЕРВЕР)         │                         │ (КЛИЕНТ)            │
    ├─────────────────┤                         ├─────────────────────┤
    │ ModBlocks        │                         │ BackroomsOverlay    │
    │ ModItems         │──── Сетевые пакеты ────>│ BackroomsAmbient    │
    │ ModSounds        │    (ModNetworking)       │ LurkerRenderer     │
    │ ModEntities      │<────────────────────────│ HowlerRenderer     │
    │ ModDimensions    │                         │ ModNetworking(S2C)  │
    │ TeleportHandler  │                         └─────────────────────┘
    └─────────────────┘
               │
               ▼
    ┌─────────────────────────────────────┐
    │          BackroomsChunkGenerator     │
    │  ┌──────────┐  ┌─────────────────┐ │
    │  │ isWall() │  │populateEntities()│ │
    │  │ 4 слоя   │  │ Lurker + Howler  │ │
    │  │ сетки    │  │ 10 попыток      │ │
    │  └──────────┘  └─────────────────┘ │
    └─────────────────────────────────────┘
               │
    ┌──────────┼──────────┐
    ▼          ▼          ▼
┌────────┐ ┌────────┐ ┌────────┐
│  JSON  │ │ JSON   │ │  JSON  │
│dimension│ │dim_type│ │ biome  │
└────────┘ └────────┘ └────────┘
```

---

> 📌 **Это живой документ.** Обновляйте его при добавлении новых функций в мод.
