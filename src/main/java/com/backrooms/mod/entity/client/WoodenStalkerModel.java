package com.backrooms.mod.entity.client;

import com.backrooms.mod.BackroomsMod;
import com.backrooms.mod.entity.WoodenStalkerEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class WoodenStalkerModel extends EntityModel<WoodenStalkerEntity> {
    public static final EntityModelLayer LAYER =
            new EntityModelLayer(new Identifier(BackroomsMod.MOD_ID, "wooden_stalker"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart left_leg;
    private final ModelPart right_leg;
    private final ModelPart left_arm;
    private final ModelPart right_arm;

    public WoodenStalkerModel(ModelPart root) {
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

        modelPartData.addChild("body",
                ModelPartBuilder.create()
                        .uv(16, 16).cuboid(-4.0F, -24.0F, -2.0F, 8.0F, 12.0F, 4.0F, new Dilation(0.0F)),
                ModelTransform.pivot(0.0F, 24.0F, 0.0F));

        modelPartData.addChild("head",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new Dilation(0.0F)),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        modelPartData.addChild("left_leg",
                ModelPartBuilder.create()
                        .uv(0, 16).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new Dilation(0.0F)),
                ModelTransform.pivot(-2.0F, 12.0F, 0.0F));

        modelPartData.addChild("right_leg",
                ModelPartBuilder.create()
                        .uv(0, 16).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new Dilation(0.0F)),
                ModelTransform.pivot(2.0F, 12.0F, 0.0F));

        modelPartData.addChild("left_arm",
                ModelPartBuilder.create()
                        .uv(40, 16).cuboid(-1.0F, -2.0F, -2.0F, 4.0F, 16.0F, 4.0F, new Dilation(0.0F)),
                ModelTransform.pivot(-5.0F, 2.0F, 0.0F));

        modelPartData.addChild("right_arm",
                ModelPartBuilder.create()
                        .uv(40, 16).cuboid(-3.0F, -2.0F, -2.0F, 4.0F, 16.0F, 4.0F, new Dilation(0.0F)),
                ModelTransform.pivot(5.0F, 2.0F, 0.0F));

        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void setAngles(WoodenStalkerEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isFrozen()) {
            this.head.pitch = 0.2f;
            this.head.yaw = 0.0f;
            this.right_arm.pitch = -0.1f;
            this.left_arm.pitch = -0.1f;
            this.right_arm.roll = 0.1f;
            this.left_arm.roll = -0.1f;
            this.right_leg.pitch = 0.0f;
            this.left_leg.pitch = 0.0f;
            return;
        }

        this.head.yaw = netHeadYaw * 0.017453292F;
        this.head.pitch = headPitch * 0.017453292F;

        float swing = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.right_leg.pitch = swing;
        this.left_leg.pitch = -swing;
        this.right_arm.pitch = -swing * 0.8F;
        this.left_arm.pitch = swing * 0.8F;
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
