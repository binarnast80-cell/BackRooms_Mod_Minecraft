package com.backrooms.mod.entity.client;

import com.backrooms.mod.entity.MimicEntity;
import java.util.Optional;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
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
        if (entity == null) {
            return DEFAULT_TEXTURE;
        }

        ClientPlayNetworkHandler networkHandler = MinecraftClient.getInstance().getNetworkHandler();
        if (networkHandler == null) {
            return DEFAULT_TEXTURE;
        }

        return entity.getSkinOwnerUuid()
                .flatMap(uuid -> {
                    PlayerListEntry entry = networkHandler.getPlayerListEntry(uuid);
                    return entry != null ? Optional.ofNullable(entry.getSkinTexture()) : Optional.empty();
                })
                .orElse(DEFAULT_TEXTURE);
    }
}
