package com.backrooms.mod;

import com.backrooms.mod.block.ModBlocks;
import com.backrooms.mod.entity.ModEntities;
import com.backrooms.mod.event.BackroomsTeleportHandler;
import com.backrooms.mod.item.ModItems;
import com.backrooms.mod.network.ModNetworking;
import com.backrooms.mod.sound.ModSounds;
import com.backrooms.mod.world.ModDimensions;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackroomsMod implements ModInitializer {
    public static final String MOD_ID = "backrooms";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Backrooms Mod...");

        ModBlocks.register();
        ModItems.register();
        ModSounds.register();
        ModEntities.register();
        ModDimensions.register();
        ModNetworking.registerC2SPackets();
        BackroomsTeleportHandler.register();

        LOGGER.info("Backrooms Mod initialized!");
    }
}

