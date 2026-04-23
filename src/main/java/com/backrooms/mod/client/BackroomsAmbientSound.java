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
 * Автоматически запускает жуткий эмбиент (включая гул ламп),
 * когда игрок находится в измерении Backrooms.
 */
public class BackroomsAmbientSound {

    private static boolean isPlaying = false;
    private static BackroomsLoopSound ambientSound = null;
    private static DynamicLampSound lampSound = null;

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

        ambientSound = new BackroomsLoopSound(client.player, ModSounds.BACKROOMS_SOUND, 0.5f, true);
        lampSound = new DynamicLampSound(client.player, ModSounds.LAMP_HUM, true);

        client.getSoundManager().play(ambientSound);
        client.getSoundManager().play(lampSound);

        isPlaying = true;
    }

    private static void stopSounds(MinecraftClient client) {
        if (ambientSound != null) {
            client.getSoundManager().stop(ambientSound);
            ambientSound = null;
        }
        if (lampSound != null) {
            client.getSoundManager().stop(lampSound);
            lampSound = null;
        }
        isPlaying = false;
    }

    /**
     * Зацикленный постоянный звук для всего измерения.
     */
    private static class BackroomsLoopSound extends MovingSoundInstance {
        private final PlayerEntity player;

        public BackroomsLoopSound(PlayerEntity player, net.minecraft.sound.SoundEvent sound, float volume, boolean loop) {
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

    /**
     * Звук ламп, громкость которого зависит от дистанции до ближайшей лампы.
     */
    private static class DynamicLampSound extends MovingSoundInstance {
        private final PlayerEntity player;
        private int tickCount = 0;
        private float targetVolume = 0.0f;

        public DynamicLampSound(PlayerEntity player, net.minecraft.sound.SoundEvent sound, boolean loop) {
            super(sound, SoundCategory.AMBIENT, Random.create());
            this.player = player;
            this.repeat = loop;
            this.volume = 0.0f;
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

            tickCount++;
            if (tickCount % 10 == 0) {
                double minDistanceSq = 16 * 16 + 1;
                net.minecraft.world.World world = player.getWorld();
                int px = (int) player.getX();
                int pz = (int) player.getZ();
                
                // Ищем ближайшую лампу на потолке (Y=106)
                for (int dx = -16; dx <= 16; dx++) {
                    for (int dz = -16; dz <= 16; dz++) {
                        if (dx*dx + dz*dz > 16*16) continue;
                        
                        net.minecraft.util.math.BlockPos checkPos = new net.minecraft.util.math.BlockPos(px + dx, 106, pz + dz);
                        if (world.getBlockState(checkPos).isOf(com.backrooms.mod.block.ModBlocks.BACKROOMS_LAMP)) {
                            double distSq = dx*dx + dz*dz; // 2D distance
                            if (distSq < minDistanceSq) {
                                minDistanceSq = distSq;
                            }
                        }
                    }
                }
                
                double dist = Math.sqrt(minDistanceSq);
                if (dist <= 12.0) {
                    targetVolume = 0.35f; // Максимальная громкость снижена
                } else if (dist <= 16.0) {
                    // Плавное затухание
                    targetVolume = (float) (0.35f * (1.0 - (dist - 12.0) / 4.0));
                } else {
                    targetVolume = 0.0f; // Не слышно
                }
            }
            
            // Плавное изменение громкости (интерполяция)
            this.volume += (targetVolume - this.volume) * 0.1f;
        }

        @Override
        public boolean shouldAlwaysPlay() {
            return true;
        }
    }
}
