package com.backrooms.mod.event;

import com.backrooms.mod.BackroomsMod;
import com.backrooms.mod.network.ModNetworking;
import com.backrooms.mod.world.ModDimensions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import com.backrooms.mod.block.ModBlocks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Обработчик телепортации в Backrooms.
 *
 * Триггер: игрок получает деревянную кирку в инвентарь (любым способом).
 * Последовательность:
 * 1. Обнаружена кирка → удаляем её, начинается погружение (3 сек)
 * 2. Чёрные частицы + плавное затемнение экрана
 * 3. Экран чёрный → телепорт в Backrooms
 * 4. Резкое «открытие глаз» + звук "Where am I?" через 1.5 сек
 */
public class BackroomsTeleportHandler {

    private static final int SINK_DURATION_TICKS = 60;   // 3 сек погружения
    private static final int EYES_OPEN_DELAY = 60;       // 3 сек перед открытием глаз (даём время на загрузку мира)

    // Состояния:
    // 0 = ожидание триггера (деревянная кирка в инвентаре)
    // 1 = погружение (3 сек)
    // 2 = телепортирован, ждём перед fade-out
    // 3 = fade-out + звук
    // 4 = завершено
    private static final Map<UUID, Integer> playerState = new HashMap<>();
    private static final Map<UUID, Integer> playerTicks = new HashMap<>();
    private static final Map<UUID, Double> playerStartY = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                UUID uuid = player.getUuid();
                int state = playerState.getOrDefault(uuid, 0);

                if (state >= 4) continue;

                switch (state) {
                    case 0: // Ждём деревянную кирку в инвентаре
                        // Уже в Backrooms — пропускаем
                        if (player.getWorld().getRegistryKey() == ModDimensions.BACKROOMS_LEVEL_KEY) {
                            playerState.put(uuid, 4);
                            break;
                        }

                        // Проверяем есть ли деревянная кирка в инвентаре
                        int pickaxeSlot = findWoodenPickaxe(player);
                        if (pickaxeSlot >= 0) {
                            // Удаляем кирку из инвентаря
                            player.getInventory().removeStack(pickaxeSlot, 1);

                            // Запоминаем Y и начинаем погружение
                            playerStartY.put(uuid, player.getY());
                            ModNetworking.sendSinkFadePacket(player);

                            playerState.put(uuid, 1);
                            playerTicks.put(uuid, 0);
                            BackroomsMod.LOGGER.info("Wooden pickaxe detected! Sinking started for: {}",
                                    player.getName().getString());
                        }
                        break;

                    case 1: // Погружение
                        int ticks1 = playerTicks.getOrDefault(uuid, 0) + 1;
                        playerTicks.put(uuid, ticks1);

                        double startY = playerStartY.getOrDefault(uuid, player.getY());

                        if (ticks1 <= SINK_DURATION_TICKS) {
                            double progress = (double) ticks1 / SINK_DURATION_TICKS;
                            double sinkDepth = progress * 2.0;
                            double targetY = startY - sinkDepth;

                            player.setVelocity(0, 0, 0);
                            player.teleport(player.getServerWorld(), player.getX(), targetY, player.getZ(),
                                    player.getYaw(), player.getPitch());
                            player.velocityModified = true;

                            // Чёрные частицы (SCP-106 стиль)
                            ServerWorld world = player.getServerWorld();
                            for (int i = 0; i < 5; i++) {
                                double px = player.getX() + (world.random.nextDouble() - 0.5) * 2;
                                double py = player.getY() + world.random.nextDouble() * 2;
                                double pz = player.getZ() + (world.random.nextDouble() - 0.5) * 2;
                                world.spawnParticles(ParticleTypes.SQUID_INK, px, py, pz,
                                        1, 0.1, 0.1, 0.1, 0.02);
                            }
                            for (int i = 0; i < 3; i++) {
                                double px = player.getX() + (world.random.nextDouble() - 0.5) * 1.5;
                                double py = player.getY() + 0.5;
                                double pz = player.getZ() + (world.random.nextDouble() - 0.5) * 1.5;
                                world.spawnParticles(ParticleTypes.LARGE_SMOKE, px, py, pz,
                                        1, 0.05, 0.1, 0.05, 0.01);
                            }
                        } else {
                            teleportToBackrooms(player);
                            playerState.put(uuid, 2);
                            playerTicks.put(uuid, 0);
                            BackroomsMod.LOGGER.info("Teleported: {}", player.getName().getString());
                        }
                        break;

                    case 2: // Телепортирован, ждём перед открытием глаз
                        int ticks2 = playerTicks.getOrDefault(uuid, 0) + 1;
                        playerTicks.put(uuid, ticks2);

                        if (ticks2 >= EYES_OPEN_DELAY) {
                            // Снимаем чёрный экран
                            ModNetworking.sendArrivalPacket(player);
                            ModNetworking.sendFadePacket(player, false);

                            playerState.put(uuid, 3);
                            playerTicks.put(uuid, 0);
                            BackroomsMod.LOGGER.info("Eyes open for: {}", player.getName().getString());
                        }
                        break;

                    case 3: // Ждём завершения fade-out (3 сек) + 1 сек пауза, потом звук
                        int ticks3 = playerTicks.getOrDefault(uuid, 0) + 1;
                        playerTicks.put(uuid, ticks3);

                        // Fade-out длится 60 тиков (3 сек) + 20 тиков (1 сек пауза) = 80 тиков
                        if (ticks3 >= 80) {
                            // Звук прибытия через 1 сек после того как экран полностью прояснился
                            player.getServerWorld().playSound(null, player.getBlockPos(),
                                    com.backrooms.mod.sound.ModSounds.BACKROOMS_ARRIVAL,
                                    net.minecraft.sound.SoundCategory.MASTER,
                                    1.0f, 1.0f);

                            playerState.put(uuid, 4);
                            playerTicks.put(uuid, 0);
                            BackroomsMod.LOGGER.info("Arrival sound played for: {}", player.getName().getString());
                        }
                        break;

                    case 4: // Завершено
                        playerStartY.remove(uuid);
                        // Не меняем состояние — остаёмся в 4 навсегда
                        break;
                }
            }
        });

        // Очистка при отключении
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.getPlayer().getUuid();
            playerTicks.remove(uuid);
        });

        // Возрождение в Backrooms
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (oldPlayer.getWorld().getRegistryKey() == ModDimensions.BACKROOMS_LEVEL_KEY) {
                ServerWorld backroomsWorld = newPlayer.getServer().getWorld(ModDimensions.BACKROOMS_LEVEL_KEY);
                if (backroomsWorld != null) {
                    teleportToSafeBackroomsSpot(newPlayer, backroomsWorld, oldPlayer.getPos());
                }
            }
        });

        BackroomsMod.LOGGER.info("Backrooms teleport handler registered");
    }

    /**
     * Ищет деревянную кирку в инвентаре игрока.
     * @return индекс слота или -1 если не найдена
     */
    private static int findWoodenPickaxe(ServerPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (player.getInventory().getStack(i).isOf(Items.WOODEN_PICKAXE)) {
                return i;
            }
        }
        return -1;
    }

    private static void teleportToBackrooms(ServerPlayerEntity player) {
        ServerWorld backroomsWorld = player.getServer().getWorld(ModDimensions.BACKROOMS_LEVEL_KEY);
        if (backroomsWorld == null) {
            BackroomsMod.LOGGER.error("Backrooms world is null!");
            return;
        }
        player.teleport(backroomsWorld, 3.5, 101.0, 3.5, player.getYaw(), player.getPitch());
    }

    private static void teleportToSafeBackroomsSpot(ServerPlayerEntity player, ServerWorld world, Vec3d deathPos) {
        java.util.Random random = new java.util.Random();
        double startX = deathPos.x;
        double startZ = deathPos.z;
        for (int i = 0; i < 50; i++) {
            double rx = startX + (random.nextDouble() * 200 - 100);
            double rz = startZ + (random.nextDouble() * 200 - 100);
            int x = (int) Math.floor(rx);
            int z = (int) Math.floor(rz);
            BlockPos floor = new BlockPos(x, 100, z);
            BlockPos feet = new BlockPos(x, 101, z);
            BlockPos head = new BlockPos(x, 102, z);
            if (world.getBlockState(floor).isOf(ModBlocks.BACKROOMS_FLOOR) &&
                world.getBlockState(feet).isAir() && world.getBlockState(head).isAir()) {
                player.teleport(world, rx, 101.0, rz, player.getYaw(), player.getPitch());
                return;
            }
        }
        player.teleport(world, startX, 101.0, startZ, player.getYaw(), player.getPitch());
    }
}
