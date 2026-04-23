package com.backrooms.mod.mixin;

import com.backrooms.mod.world.BackroomsChunkGenerator;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.client.render.FogShape;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
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

        // Плавное вычисление цвета:
        // Обычные стены: #4d4533 (0.30, 0.27, 0.20) - ещё более темный и атмосферный
        // Зараженная зона: #242018 (0.14, 0.12, 0.09)
        double inf = BackroomsChunkGenerator.getInfectionValue((int)camera.getPos().x, (int)camera.getPos().z);
        float t = MathHelper.clamp((float) ((inf - 0.45) / 0.20), 0.0f, 1.0f);
        
        float r = MathHelper.lerp(t, 0.30f, 0.14f);
        float g = MathHelper.lerp(t, 0.27f, 0.12f);
        float b = MathHelper.lerp(t, 0.20f, 0.09f);

        RenderSystem.clearColor(r, g, b, 1.0f);
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

        // Делаем туман цилиндрическим
        RenderSystem.setShaderFogShape(FogShape.CYLINDER);

        // Очень плавный и далёкий блюр (как в реальной жизни)
        // Начинается с 5 блоков (эффект виден почти сразу)
        // Заканчивается на 80 блоках (полная непроглядность далеко впереди)
        RenderSystem.setShaderFogStart(5.0f);
        RenderSystem.setShaderFogEnd(80.0f);
    }
}
