package com.backrooms.mod.client;

import com.backrooms.mod.entity.WoodenStalkerEntity;
import com.backrooms.mod.sound.ModSounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.random.Random;

/**
 * Непрерывный звук шагов WoodenStalker.
 *
 * Ключевые особенности:
 * - Звук НЕ сбрасывается при остановке моба
 * - Когда моб стоит → громкость плавно затухает
 * - Когда моб идёт → громкость плавно нарастает
 * - Звук зациклен и продолжается непрерывно
 * - Привязан к позиции сущности (3D звук)
 * - Начинает играть с 20 блоков, слышен с 18 блоков
 * - Чем дальше — тем тише (плавное затухание по расстоянию)
 */
public class StalkerFootstepsSound extends MovingSoundInstance {

    private final WoodenStalkerEntity stalker;
    private float targetVolume = 0.0f;
    private static final float FADE_SPEED = 0.05f;     // Более плавное затухание
    private static final float MAX_VOLUME = 0.446f;    // Снижена ещё на 15% (0.525 * 0.85)
    private static final float MOVE_THRESHOLD = 0.01f;
    private static final float PLAY_DISTANCE = 20.0f;  // Начало воспроизведения (20 блоков)
    private static final float HEAR_DISTANCE = 18.0f;  // Слышимый порог (18 блоков)

    public StalkerFootstepsSound(WoodenStalkerEntity stalker) {
        super(ModSounds.STALKER_FOOTSTEPS, SoundCategory.HOSTILE, Random.create());
        this.stalker = stalker;
        this.repeat = true;
        this.volume = 0.0f;
        this.x = stalker.getX();
        this.y = stalker.getY();
        this.z = stalker.getZ();
        this.attenuationType = SoundInstance.AttenuationType.NONE; // Ручное управление громкостью
    }

    @Override
    public void tick() {
        if (stalker.isRemoved() || !stalker.isAlive()) {
            this.setDone();
            return;
        }

        // Обновляем позицию звука
        this.x = stalker.getX();
        this.y = stalker.getY();
        this.z = stalker.getZ();

        // Расстояние до игрока
        MinecraftClient client = MinecraftClient.getInstance();
        double distance = PLAY_DISTANCE;
        if (client.player != null) {
            distance = client.player.getPos().distanceTo(stalker.getPos());
        }

        // Определяем, двигается ли моб
        double speed = stalker.getVelocity().horizontalLengthSquared();
        boolean isMoving = speed > MOVE_THRESHOLD * MOVE_THRESHOLD;

        // Множитель громкости по расстоянию:
        // > 20 блоков = 0 (не слышно)
        // 18-20 блоков = нарастает от 0 до начального уровня
        // 0-18 блоков = плавно нарастает до максимума (чем ближе, тем громче)
        float distanceFactor;
        if (distance > PLAY_DISTANCE) {
            distanceFactor = 0.0f;
        } else if (distance > HEAR_DISTANCE) {
            // Плавный переход от 0 до ~0.15 между 20 и 18 блоками
            distanceFactor = (float) ((PLAY_DISTANCE - distance) / (PLAY_DISTANCE - HEAR_DISTANCE)) * 0.15f;
        } else {
            // От 18 блоков до 0: плавно от 0.15 до 1.0
            distanceFactor = 0.15f + (float) ((HEAR_DISTANCE - distance) / HEAR_DISTANCE) * 0.85f;
        }

        // Целевая громкость: движение × расстояние
        targetVolume = isMoving ? MAX_VOLUME * distanceFactor : 0.0f;

        // Плавная интерполяция (более мягкое затухание)
        if (this.volume < targetVolume) {
            this.volume = Math.min(targetVolume, this.volume + FADE_SPEED);
        } else if (this.volume > targetVolume) {
            this.volume = Math.max(targetVolume, this.volume - FADE_SPEED);
        }
    }

    @Override
    public boolean shouldAlwaysPlay() {
        return true;
    }

    public WoodenStalkerEntity getStalker() {
        return this.stalker;
    }
}
