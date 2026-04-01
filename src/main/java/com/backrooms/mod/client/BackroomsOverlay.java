package com.backrooms.mod.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Оверлей затемнения экрана при телепортации в Backrooms.
 * Рисует чёрный прямоугольник поверх всего HUD с изменяемой прозрачностью.
 * Анимация: плавное затемнение → удержание → плавное осветление.
 */
public class BackroomsOverlay {

    private static float fadeAlpha = 0.0f;
    private static boolean fading = false;
    private static boolean fadingIn = true;
    private static int fadeTimer = 0;

    private static final int FADE_IN_TICKS = 40;   // 2 сек — затемнение
    private static final int FADE_OUT_TICKS = 40;  // 2 сек — осветление

    /**
     * Запускает эффект затемнения/осветления.
     * @param fadeIn true = затемнение, false = осветление
     */
    public static void startFade(boolean fadeIn) {
        fading = true;
        fadingIn = fadeIn;
        fadeTimer = 0;
        if (fadeIn) {
            fadeAlpha = 0.0f;
        } else {
            fadeAlpha = 1.0f;
        }
    }

    public static void register() {
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (!fading && fadeAlpha <= 0) return;

            fadeTimer++;

            if (fading) {
                if (fadingIn) {
                    // Плавное затемнение
                    if (fadeTimer <= FADE_IN_TICKS) {
                        fadeAlpha = (float) fadeTimer / FADE_IN_TICKS;
                    } else {
                        fadeAlpha = 1.0f;
                        // Удерживаем чёрный экран до получения пакета fade-out
                    }
                } else {
                    // Плавное осветление
                    if (fadeTimer <= FADE_OUT_TICKS) {
                        fadeAlpha = 1.0f - (float) fadeTimer / FADE_OUT_TICKS;
                    } else {
                        fadeAlpha = 0.0f;
                        fading = false;
                    }
                }
            }

            if (fadeAlpha > 0.0f) {
                MinecraftClient client = MinecraftClient.getInstance();
                int width = client.getWindow().getScaledWidth();
                int height = client.getWindow().getScaledHeight();
                int alpha = ((int) (fadeAlpha * 255.0f)) << 24; // ARGB: alpha в старших битах
                drawContext.fill(0, 0, width, height, alpha); // Чёрный цвет (RGB=0) + alpha
            }
        });
    }
}
