package com.backrooms.mod.client;

import com.backrooms.mod.sound.ModSounds;
import com.backrooms.mod.world.ModDimensions;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.random.Random;

/**
 * Управление случайными страшными звуками в Backrooms.
 * Случайно воспроизводит звуки у игрока с низкой вероятностью.
 */
public class BackroomsScarySounds {

    private static final SoundEvent[] SCARY_SOUNDS = {
        ModSounds.SCARY_WHISPER,
        ModSounds.SCARY_SOUND,
        ModSounds.SCARY_FOOTSTEPS,
        ModSounds.SCARY_BREATH,
        ModSounds.HOWLER_CRY
    };

    private static final float PLAY_CHANCE = 0.00055f; // Чуть-чуть чаще (на 10% по просьбе пользователя)

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            boolean inBackrooms = client.player.getWorld().getRegistryKey() == ModDimensions.BACKROOMS_LEVEL_KEY;

            if (inBackrooms && client.world.random.nextFloat() < PLAY_CHANCE) {
                playRandomScarySound(client);
            }
        });
    }

    private static void playRandomScarySound(MinecraftClient client) {
        if (client.player == null) return;

        SoundEvent sound = SCARY_SOUNDS[client.world.random.nextInt(SCARY_SOUNDS.length)];
        float volume = 0.8f + client.world.random.nextFloat() * 0.4f; // Случайная громкость 0.8-1.2
        float pitch = 0.8f + client.world.random.nextFloat() * 0.4f; // Случайная высота тона 0.8-1.2

        client.getSoundManager().play(PositionedSoundInstance.master(sound, pitch, volume));
    }
}