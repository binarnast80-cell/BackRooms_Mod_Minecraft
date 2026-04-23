package com.backrooms.mod.entity.client;

import com.backrooms.mod.BackroomsMod;
import com.backrooms.mod.entity.WoodenStalkerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class WoodenStalkerRenderer extends MobEntityRenderer<WoodenStalkerEntity, WoodenStalkerModel> {

    private static final Identifier TEXTURE = new Identifier("minecraft", "textures/block/oak_planks.png");

    public WoodenStalkerRenderer(EntityRendererFactory.Context context) {
        super(context, new WoodenStalkerModel(context.getPart(WoodenStalkerModel.LAYER)), 0.5f);
    }

    @Override
    public Identifier getTexture(WoodenStalkerEntity entity) {
        return TEXTURE;
    }
}
