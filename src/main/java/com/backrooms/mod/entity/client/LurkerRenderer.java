package com.backrooms.mod.entity.client;

import com.backrooms.mod.BackroomsMod;
import com.backrooms.mod.entity.LurkerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Рендерер Lurker — использует кастомную модель из Blockbench.
 */
public class LurkerRenderer extends MobEntityRenderer<LurkerEntity, LurkerModel> {

    private static final Identifier TEXTURE =
            new Identifier(BackroomsMod.MOD_ID, "textures/entity/lurker.png");

    public LurkerRenderer(EntityRendererFactory.Context context) {
        super(context, new LurkerModel(context.getPart(LurkerModel.LAYER)), 0.5f);
    }

    @Override
    public Identifier getTexture(LurkerEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(LurkerEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        // Масштаб модели — подгоняем под хитбокс
        matrices.scale(0.8f, 0.8f, 0.8f);
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        matrices.pop();
    }
}
