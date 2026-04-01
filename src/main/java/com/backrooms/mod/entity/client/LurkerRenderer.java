package com.backrooms.mod.entity.client;

import com.backrooms.mod.BackroomsMod;
import com.backrooms.mod.entity.LurkerEntity;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

/**
 * Рендерер Lurker — использует модель зомби как заглушку.
 * Текстуру можно заменить на кастомную.
 */
public class LurkerRenderer extends BipedEntityRenderer<LurkerEntity, BipedEntityModel<LurkerEntity>> {
    private static final Identifier TEXTURE =
            new Identifier(BackroomsMod.MOD_ID, "textures/entity/lurker.png");

    public LurkerRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new BipedEntityModel<>(ctx.getPart(EntityModelLayers.ZOMBIE)), 0.5f);
    }

    @Override
    public Identifier getTexture(LurkerEntity entity) {
        return TEXTURE;
    }
}
