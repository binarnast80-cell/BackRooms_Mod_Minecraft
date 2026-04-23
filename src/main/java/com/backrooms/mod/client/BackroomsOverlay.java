package com.backrooms.mod.client;

import com.backrooms.mod.sound.ModSounds;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.util.Identifier;

/**
 * Атмосферный оверлей Backrooms + анимации телепортации.
 *
 * Режимы:
 * - NONE: тёплый тинт в Backrooms
 * - SINK: медленное затемнение за 3 сек (погружение в землю)
 * - BLACK: полностью чёрный экран (телепорт)
 * - ARRIVAL: резкое «открытие глаз» (быстрый fade-out 1 сек) + звук
 * - FADE_IN/FADE_OUT: обычные фейды (fallback)
 */
public class BackroomsOverlay {

    private enum Mode { NONE, FADE_IN, FADE_OUT, SINK, BLACK, ARRIVAL }

    private static Mode mode = Mode.NONE;
    private static float fadeAlpha = 0.0f;
    private static int fadeTimer = 0;
    private static boolean arrivalSoundPlayed = false;

    private static final int SINK_TICKS = 60;      // 3 сек погружения
    private static final int ARRIVAL_TICKS = 20;    // 1 сек быстрое «открытие глаз»
    private static final int FADE_IN_TICKS = 40;
    private static final int FADE_OUT_TICKS = 40;

    // Тёплый жёлтый тинт
    private static final int WARM_TINT = 0x1FD4B86A;

    /** Медленное затемнение (погружение) — 3 секунды */
    public static void startSinkFade() {
        mode = Mode.SINK;
        fadeTimer = 0;
        fadeAlpha = 0.0f;
    }

    /** Резкое «открытие глаз» + звук прибытия */
    public static void startArrival() {
        mode = Mode.ARRIVAL;
        fadeTimer = 0;
        fadeAlpha = 1.0f; // Начинаем с полностью чёрного
        arrivalSoundPlayed = false;
    }

    /** Обычный fade (совместимость) */
    public static void startFade(boolean fadeIn) {
        if (fadeIn) {
            mode = Mode.FADE_IN;
            fadeAlpha = 0.0f;
        } else {
            mode = Mode.FADE_OUT;
            fadeAlpha = 1.0f;
        }
        fadeTimer = 0;
    }

    public static void register() {
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            int w = client.getWindow().getScaledWidth();
            int h = client.getWindow().getScaledHeight();

            // ── Тёплый тинт (только в Backrooms) ─────────────────────────
            if (client.world != null && client.player != null) {
                Identifier dimId = client.world.getRegistryKey().getValue();
                if (dimId.getNamespace().equals("backrooms")) {
                    drawContext.fill(0, 0, w, h, WARM_TINT);
                }
            }

            // ── Обработка режимов ─────────────────────────────────────────
            fadeTimer++;

            switch (mode) {
                case SINK:
                    // Медленное плавное затемнение за 3 секунды
                    if (fadeTimer <= SINK_TICKS) {
                        fadeAlpha = (float) fadeTimer / SINK_TICKS;
                    } else {
                        fadeAlpha = 1.0f;
                        mode = Mode.BLACK; // Остаёмся на чёрном экране
                    }
                    break;

                case BLACK:
                    fadeAlpha = 1.0f; // Полностью чёрный, ждём пакет arrival
                    break;

                case ARRIVAL:
                    // Первый кадр — играем звук прибытия
                    if (!arrivalSoundPlayed) {
                        arrivalSoundPlayed = true;
                        if (client.player != null) {
                            client.getSoundManager().play(
                                PositionedSoundInstance.master(ModSounds.BACKROOMS_ARRIVAL, 1.0f, 1.0f)
                            );
                        }
                    }
                    // Быстрое «открытие глаз» — fade-out за 1 сек
                    if (fadeTimer <= ARRIVAL_TICKS) {
                        fadeAlpha = 1.0f - (float) fadeTimer / ARRIVAL_TICKS;
                    } else {
                        fadeAlpha = 0.0f;
                        mode = Mode.NONE;
                    }
                    break;

                case FADE_IN:
                    if (fadeTimer <= FADE_IN_TICKS) {
                        fadeAlpha = (float) fadeTimer / FADE_IN_TICKS;
                    } else {
                        fadeAlpha = 1.0f;
                    }
                    break;

                case FADE_OUT:
                    if (fadeTimer <= FADE_OUT_TICKS) {
                        fadeAlpha = 1.0f - (float) fadeTimer / FADE_OUT_TICKS;
                    } else {
                        fadeAlpha = 0.0f;
                        mode = Mode.NONE;
                    }
                    break;

                case NONE:
                default:
                    break;
            }

            // Рисуем чёрный оверлей
            if (fadeAlpha > 0.0f) {
                int alpha = ((int) (fadeAlpha * 255.0f)) << 24;
                drawContext.fill(0, 0, w, h, alpha);
            }
        });
    }
}
