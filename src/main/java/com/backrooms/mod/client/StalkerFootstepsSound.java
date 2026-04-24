package com.backrooms.mod.client;

import com.backrooms.mod.entity.WoodenStalkerEntity;
import com.backrooms.mod.sound.ModSounds;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.random.Random;

/**
 * Непрерывный звук шагов WoodenStalker.
 *
 * Ключевые особенности:
 * - Звук НЕ сбрасывается при остановке моба
 * - Когда моб стоит → громкость плавно затухает (быстро, но не резко)
 * - Когда моб идёт → громкость плавно нарастает
 * - Звук зациклен и продолжается непрерывно
 * - Привязан к позиции сущности (3D звук)
 */
public class StalkerFootstepsSound extends MovingSoundInstance {

    private final WoodenStalkerEntity stalker;
    private float targetVolume = 0.0f;
    private static final float FADE_SPEED = 0.08f;    // Скорость затухания/нарастания
    private static final float MAX_VOLUME = 0.7f;     // Максимальная громкость
    private static final float MOVE_THRESHOLD = 0.01f; // Порог скорости для определения движения

    public StalkerFootstepsSound(WoodenStalkerEntity stalker) {
        super(ModSounds.STALKER_FOOTSTEPS, SoundCategory.HOSTILE, Random.create());
        this.stalker = stalker;
        this.repeat = true; // Зацикленный звук
        this.volume = 0.0f; // Начинаем с тишины
        this.x = stalker.getX();
        this.y = stalker.getY();
        this.z = stalker.getZ();
        this.attenuationType = SoundInstance.AttenuationType.LINEAR; // 3D звук с затуханием по расстоянию
    }

    @Override
    public void tick() {
        // Если моб удалён — заканчиваем звук
        if (stalker.isRemoved() || !stalker.isAlive()) {
            this.setDone();
            return;
        }

        // Обновляем позицию звука (привязка к мобу)
        this.x = stalker.getX();
        this.y = stalker.getY();
        this.z = stalker.getZ();

        // Определяем, двигается ли моб
        double speed = stalker.getVelocity().horizontalLengthSquared();
        boolean isMoving = speed > MOVE_THRESHOLD * MOVE_THRESHOLD;

        // Целевая громкость: максимум если идёт, 0 если стоит
        targetVolume = isMoving ? MAX_VOLUME : 0.0f;

        // Плавная интерполяция громкости (быстрое, но не резкое затухание)
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

    /**
     * Проверяет, подходит ли этот звук к данному сталкеру.
     */
    public WoodenStalkerEntity getStalker() {
        return this.stalker;
    }
}
