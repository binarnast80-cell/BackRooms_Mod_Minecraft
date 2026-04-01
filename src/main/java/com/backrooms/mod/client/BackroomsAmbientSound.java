package com.backrooms.mod.client;

import com.backrooms.mod.sound.ModSounds;
import com.backrooms.mod.world.ModDimensions;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.random.Random;

/**
 * Управление зацикленными звуками эмбиента в Backrooms.
 * Автоматически запускает гул ламп и жуткий эмбиент,
 * когда игрок находится в измерении Backrooms.
 */
public class BackroomsAmbientSound {

    private static boolean isPlaying = false;
    private static BackroomsLoopSound lampHumSound = null;
    private static BackroomsLoopSound ambientSound = null;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                stopSounds(client);
                return;
            }

            boolean inBackrooms = client.player.getWorld().getRegistryKey() == ModDimensions.BACKROOMS_LEVEL_KEY;

            if (inBackrooms && !isPlaying) {
                startSounds(client);
            } else if (!inBackrooms && isPlaying) {
                stopSounds(client);
            }
        });
    }

    private static void startSounds(MinecraftClient client) {
        if (client.player == null) return;

        lampHumSound = new BackroomsLoopSound(client.player, ModSounds.LAMP_HUM, 0.3f, true);
        ambientSound = new BackroomsLoopSound(client.player, ModSounds.AMBIENT_BACKROOMS, 0.5f, true);

        client.getSoundManager().play(lampHumSound);
        client.getSoundManager().play(ambientSound);

        isPlaying = true;
    }

    private static void stopSounds(MinecraftClient client) {
        if (lampHumSound != null) {
            client.getSoundManager().stop(lampHumSound);
            lampHumSound = null;
        }
        if (ambientSound != null) {
            client.getSoundManager().stop(ambientSound);
            ambientSound = null;
        }
        isPlaying = false;
    }

    /**
     * Зацикленный звук, привязанный к позиции игрока.
     */
    private static class BackroomsLoopSound extends MovingSoundInstance {
        private final PlayerEntity player;

        public BackroomsLoopSound(PlayerEntity player, net.minecraft.sound.SoundEvent sound,
                                  float volume, boolean loop) {
            super(sound, SoundCategory.AMBIENT, Random.create());
            this.player = player;
            this.repeat = loop;
            this.volume = volume;
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
            this.attenuationType = SoundInstance.AttenuationType.NONE;
        }

        @Override
        public void tick() {
            if (player.isRemoved()) {
                this.setDone();
                return;
            }
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
        }

        @Override
        public boolean shouldAlwaysPlay() {
            return true;
        }
    }
}
