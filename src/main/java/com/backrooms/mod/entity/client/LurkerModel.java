package com.backrooms.mod.entity.client;

import com.backrooms.mod.BackroomsMod;
import com.backrooms.mod.entity.LurkerEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * 3D-модель Lurker из Blockbench.
 * Разделена на отдельные части для анимации ходьбы.
 * Текстура: 128×128
 */
public class LurkerModel extends EntityModel<LurkerEntity> {

    public static final EntityModelLayer LAYER =
            new EntityModelLayer(new Identifier(BackroomsMod.MOD_ID, "lurker"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart left_leg;
    private final ModelPart right_leg;
    private final ModelPart left_arm;
    private final ModelPart right_arm;

    public LurkerModel(ModelPart root) {
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.left_leg = root.getChild("left_leg");
        this.right_leg = root.getChild("right_leg");
        this.left_arm = root.getChild("left_arm");
        this.right_arm = root.getChild("right_arm");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();

        // Тело (торс) — pivot на уровне талии
        modelPartData.addChild("body",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-5.0F, -17.0F, -4.0F, 9.0F, 18.0F, 5.0F, new Dilation(0.0F)),
                ModelTransform.pivot(0.0F, 1.0F, 0.0F));

        // Голова — pivot на макушке тела
        modelPartData.addChild("head",
                ModelPartBuilder.create()
                        .uv(0, 23).cuboid(-4.0F, -9.0F, -5.0F, 7.0F, 9.0F, 7.0F, new Dilation(0.0F)),
                ModelTransform.pivot(0.0F, -16.0F, 0.0F));

        // Левая нога — pivot на бедре
        modelPartData.addChild("left_leg",
                ModelPartBuilder.create()
                        .uv(28, 26).cuboid(-2.5F, 0.0F, -1.5F, 3.0F, 23.0F, 3.0F, new Dilation(0.0F)),
                ModelTransform.pivot(-2.5F, 1.0F, -1.5F));

        // Правая нога — pivot на бедре
        modelPartData.addChild("right_leg",
                ModelPartBuilder.create()
                        .uv(28, 0).cuboid(-0.5F, 0.0F, -1.5F, 3.0F, 23.0F, 3.0F, new Dilation(0.0F)),
                ModelTransform.pivot(2.5F, 1.0F, -1.5F));

        // Левая рука — pivot на плече
        modelPartData.addChild("left_arm",
                ModelPartBuilder.create()
                        .uv(0, 39).cuboid(-1.0F, 0.0F, -1.5F, 2.0F, 23.0F, 3.0F, new Dilation(0.0F)),
                ModelTransform.pivot(-6.0F, -15.0F, -1.5F));

        // Правая рука — pivot на плече
        modelPartData.addChild("right_arm",
                ModelPartBuilder.create()
                        .uv(0, 39).cuboid(-1.0F, 0.0F, -1.5F, 2.0F, 23.0F, 3.0F, new Dilation(0.0F)),
                ModelTransform.pivot(5.0F, -15.0F, -1.5F));

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(LurkerEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // Поворот головы за игроком
        this.head.yaw = netHeadYaw * 0.017453292F;   // Градусы → радианы
        this.head.pitch = headPitch * 0.017453292F;

        // Анимация ходьбы — ноги и руки качаются ВПЕРЁД-НАЗАД (ось pitch = X)
        float swing = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;

        // Ноги — противоположные друг другу
        this.right_leg.pitch = swing;
        this.left_leg.pitch = -swing;

        // Руки — противоположные ногам (как при ходьбе человека)
        this.right_arm.pitch = -swing * 0.8F;
        this.left_arm.pitch = swing * 0.8F;

        // Лёгкое покачивание рук в стороны
        this.right_arm.roll = 0.05F;
        this.left_arm.roll = -0.05F;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertexConsumer,
                       int light, int overlay, float red, float green, float blue, float alpha) {
        body.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
        head.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
        left_leg.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
        right_leg.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
        left_arm.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
        right_arm.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
    }
}
