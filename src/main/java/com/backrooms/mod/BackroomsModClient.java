package com.backrooms.mod;

import com.backrooms.mod.client.BackroomsAmbientSound;
import com.backrooms.mod.client.BackroomsOverlay;
import com.backrooms.mod.client.BackroomsScarySounds;

import com.backrooms.mod.entity.ModEntities;
import com.backrooms.mod.entity.client.HowlerRenderer;
import com.backrooms.mod.entity.client.LurkerModel;
import com.backrooms.mod.entity.client.LurkerRenderer;
import com.backrooms.mod.entity.client.MimicRenderer;
import com.backrooms.mod.entity.client.WoodenStalkerModel;
import com.backrooms.mod.entity.client.WoodenStalkerRenderer;
import com.backrooms.mod.network.ModNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class BackroomsModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModNetworking.registerS2CPackets();
        BackroomsOverlay.register();
        BackroomsAmbientSound.register();
        BackroomsScarySounds.register();

        // Регистрация кастомной модели Lurker из Blockbench
        EntityModelLayerRegistry.registerModelLayer(LurkerModel.LAYER, LurkerModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(WoodenStalkerModel.LAYER, WoodenStalkerModel::getTexturedModelData);

        // Рендереры сущностей
        EntityRendererRegistry.register(ModEntities.LURKER, LurkerRenderer::new);
        EntityRendererRegistry.register(ModEntities.HOWLER, HowlerRenderer::new);
        EntityRendererRegistry.register(ModEntities.MIMIC, MimicRenderer::new);
        EntityRendererRegistry.register(ModEntities.WOODEN_STALKER, WoodenStalkerRenderer::new);
    }
}
