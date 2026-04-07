package com.backrooms.mod.entity.client;

import com.backrooms.mod.entity.MimicEntity;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

/**
 * Renderer for Mimic — копирует скин игрока, чтобы быть максимально криповым
 */
public class MimicRenderer extends BipedEntityRenderer<MimicEntity, PlayerEntityModel<MimicEntity>> {
    private static final Identifier DEFAULT_TEXTURE = new Identifier("minecraft", "textures/entity/steve.png");

    public MimicRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false), 0.6f);
        this.shadowRadius = 0.5f;
    }

    @Override
    public Identifier getTexture(MimicEntity entity) {
        // Стараемся использовать текстуру ближайшего игрока
        // В будущем можно добавить кэширование скинов игроков
        return DEFAULT_TEXTURE;
    }
}
