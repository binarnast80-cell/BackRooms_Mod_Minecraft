package com.backrooms.mod.entity.client;

import com.backrooms.mod.BackroomsMod;
import com.backrooms.mod.entity.HowlerEntity;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

/**
 * Рендерер Howler — использует модель зомби (масштаб 1.2).
 * Howler визуально крупнее обычного моба.
 */
public class HowlerRenderer extends BipedEntityRenderer<HowlerEntity, BipedEntityModel<HowlerEntity>> {
    private static final Identifier TEXTURE =
            new Identifier(BackroomsMod.MOD_ID, "textures/entity/howler.png");

    public HowlerRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new BipedEntityModel<>(ctx.getPart(EntityModelLayers.ZOMBIE)), 0.7f);
        this.shadowRadius = 0.7f;
    }

    @Override
    public Identifier getTexture(HowlerEntity entity) {
        return TEXTURE;
    }
}
