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
 * - Звук НЕ сбрасывается при остановке моба
 * - Плавное затухание при остановке, плавное нарастание при ходьбе
 * - Зациклен, 3D-позиционирование
 * - Слышен с 18 блоков, играет с 20 блоков
 * - Чем ближе — тем громче (плавный градиент)
 */
public class StalkerFootstepsSound extends MovingSoundInstance {

    private final WoodenStalkerEntity stalker;
    private float targetVolume = 0.0f;
    private static final float FADE_SPEED = 0.05f;
    private static final float MAX_VOLUME = 0.50f;     // +12%
    private static final float MOVE_THRESHOLD = 0.01f;
    private static final double MAX_DISTANCE = 20.0;
    private static final double HEAR_DISTANCE = 18.0;

    public StalkerFootstepsSound(WoodenStalkerEntity stalker) {
        super(ModSounds.STALKER_FOOTSTEPS, SoundCategory.HOSTILE, Random.create());
        this.stalker = stalker;
        this.repeat = true;
        this.volume = 0.001f; // Минимальная ненулевая громкость для инициализации
        this.x = stalker.getX();
        this.y = stalker.getY();
        this.z = stalker.getZ();
        this.attenuationType = SoundInstance.AttenuationType.LINEAR;
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
        double distance = MAX_DISTANCE + 1;
        if (client.player != null) {
            distance = client.player.getPos().distanceTo(stalker.getPos());
        }

        // Множитель по расстоянию
        float distanceFactor;
        if (distance > MAX_DISTANCE) {
            distanceFactor = 0.0f;
        } else if (distance > HEAR_DISTANCE) {
            // 20→18 блоков: от 0 до 0.15
            distanceFactor = (float) ((MAX_DISTANCE - distance) / (MAX_DISTANCE - HEAR_DISTANCE)) * 0.15f;
        } else {
            // 18→0 блоков: от 0.15 до 1.0
            distanceFactor = 0.15f + (float) ((HEAR_DISTANCE - distance) / HEAR_DISTANCE) * 0.85f;
        }

        // Движение моба
        double speed = stalker.getVelocity().horizontalLengthSquared();
        boolean isMoving = speed > MOVE_THRESHOLD * MOVE_THRESHOLD;

        // Целевая громкость
        targetVolume = isMoving ? MAX_VOLUME * distanceFactor : 0.001f;

        // Плавная интерполяция
        if (this.volume < targetVolume) {
            this.volume = Math.min(targetVolume, this.volume + FADE_SPEED);
        } else if (this.volume > targetVolume) {
            this.volume = Math.max(targetVolume, this.volume - FADE_SPEED);
        }

        // Не даём упасть ниже минимума (чтобы звук не "умер")
        if (this.volume < 0.001f) {
            this.volume = 0.001f;
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
