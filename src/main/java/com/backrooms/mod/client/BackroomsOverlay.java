package com.backrooms.mod.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

/**
 * Атмосферный оверлей Backrooms.
 *
 * Только ТЁПЛЫЙ жёлтый тинт — как свет от старых флуоресцентных ламп.
 * Без виньетки, без чёрных краёв, без мерцания.
 * Чистая, лёгкая, тёплая атмосфера.
 */
public class BackroomsOverlay {

    private static float fadeAlpha = 0.0f;
    private static boolean fading = false;
    private static boolean fadingIn = true;
    private static int fadeTimer = 0;

    private static final int FADE_IN_TICKS = 40;
    private static final int FADE_OUT_TICKS = 40;

    // Тёплый жёлтый тинт: #D4B86A (золотисто-жёлтый), alpha ~12%
    // Лёгкий, еле заметный — как свет от старых ламп
    private static final int WARM_TINT = 0x1FD4B86A;

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

            // ── Фейд при телепортации ─────────────────────────────────────
            if (!fading && fadeAlpha <= 0) return;

            fadeTimer++;
            if (fading) {
                if (fadingIn) {
                    if (fadeTimer <= FADE_IN_TICKS) {
                        fadeAlpha = (float) fadeTimer / FADE_IN_TICKS;
                    } else {
                        fadeAlpha = 1.0f;
                    }
                } else {
                    if (fadeTimer <= FADE_OUT_TICKS) {
                        fadeAlpha = 1.0f - (float) fadeTimer / FADE_OUT_TICKS;
                    } else {
                        fadeAlpha = 0.0f;
                        fading = false;
                    }
                }
            }
            if (fadeAlpha > 0.0f) {
                int alpha = ((int) (fadeAlpha * 255.0f)) << 24;
                drawContext.fill(0, 0, w, h, alpha);
            }
        });
    }
}
