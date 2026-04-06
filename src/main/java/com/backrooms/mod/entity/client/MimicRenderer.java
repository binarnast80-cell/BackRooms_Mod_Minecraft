package com.backrooms.mod.entity.client;

import com.backrooms.mod.BackroomsMod;
import com.backrooms.mod.entity.MimicEntity;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

/**
 * Renderer for Mimic — uses the player model and the default player texture.
 */
public class MimicRenderer extends BipedEntityRenderer<MimicEntity, PlayerEntityModel<MimicEntity>> {
    private static final Identifier TEXTURE = new Identifier("minecraft", "textures/entity/steve.png");

    public MimicRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false), 0.6f);
        this.shadowRadius = 0.5f;
    }

    @Override
    public Identifier getTexture(MimicEntity entity) {
        return TEXTURE;
    }
}
