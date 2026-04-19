package com.backrooms.mod.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Кастомный туман для Backrooms.
 *
 * Тёплый жёлто-бежевый туман — как в настоящих Backrooms:
 * - Мягкий blur на расстоянии (стены плавно растворяются)
 * - Тёплый цвет старых флуоресцентных ламп
 * - НЕ чёрный, НЕ тёмный — именно тёплый и жёлтый
 *
 * Цвет: #C4A86A (тёплый жёлтый)
 * Дальность: 10-28 блоков (мягкий, не давящий)
 */
@Mixin(BackgroundRenderer.class)
public class BackroomsFogMixin {

    /**
     * Тёплый жёлтый цвет тумана — атмосфера Backrooms.
     */
    @Inject(method = "render", at = @At("RETURN"))
    private static void backrooms_overrideFogColor(Camera camera, float tickDelta,
            net.minecraft.client.world.ClientWorld world, int viewDistance,
            float skyDarkness, CallbackInfo ci) {
        if (world == null) return;

        Identifier dimId = world.getRegistryKey().getValue();
        if (!dimId.getNamespace().equals("backrooms")) return;

        // Тёплый жёлтый: #C4A86A
        // R=196/255=0.769, G=168/255=0.659, B=106/255=0.416
        RenderSystem.clearColor(0.769f, 0.659f, 0.416f, 1.0f);
    }

    /**
     * Мягкий blur-туман на расстоянии.
     * Start=10 — близко всё чёткое
     * End=28   — далеко мягко растворяется (не резко обрезается)
     */
    @Inject(method = "applyFog", at = @At("RETURN"))
    private static void backrooms_overrideFogDistance(Camera camera,
            BackgroundRenderer.FogType fogType, float viewDistance,
            boolean thickFog, float tickDelta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        Identifier dimId = client.world.getRegistryKey().getValue();
        if (!dimId.getNamespace().equals("backrooms")) return;

        if (camera.getSubmersionType() != CameraSubmersionType.NONE) return;

        // Мягкий плавный туман: чёткость до 10 блоков, blur с 10 до 28
        RenderSystem.setShaderFogStart(10.0f);
        RenderSystem.setShaderFogEnd(28.0f);
    }
}
