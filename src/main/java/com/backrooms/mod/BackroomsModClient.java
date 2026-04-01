package com.backrooms.mod;

import com.backrooms.mod.client.BackroomsAmbientSound;
import com.backrooms.mod.client.BackroomsOverlay;
import com.backrooms.mod.entity.ModEntities;
import com.backrooms.mod.entity.client.HowlerRenderer;
import com.backrooms.mod.entity.client.LurkerRenderer;
import com.backrooms.mod.network.ModNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class BackroomsModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModNetworking.registerS2CPackets();
        BackroomsOverlay.register();
        BackroomsAmbientSound.register();

        EntityRendererRegistry.register(ModEntities.LURKER, LurkerRenderer::new);
        EntityRendererRegistry.register(ModEntities.HOWLER, HowlerRenderer::new);
    }
}
