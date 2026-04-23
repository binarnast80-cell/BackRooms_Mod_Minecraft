package com.backrooms.mod.event;

import com.backrooms.mod.BackroomsMod;
import com.backrooms.mod.network.ModNetworking;
import com.backrooms.mod.world.ModDimensions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
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
 * Через 120 секунд (2400 тиков) запускает:
 * 1. Fade-in (затемнение) — 2 сек
 * 2. Телепортация в Backrooms (пока экран чёрный)
 * 3. Fade-out (осветление) — 2 сек
 */
public class BackroomsTeleportHandler {

    private static final int TELEPORT_DELAY_TICKS = 400; // 20 сек × 20 тиков
    private static final int FADE_DURATION_TICKS = 40; // 2 сек анимации

    // Состояния:
    // 0 = отсчёт до затемнения
    // 1 = fade-in отправлен, ждём завершения
    // 2 = телепортирован, ждём перед fade-out
    // 3 = fade-out отправлен
    // 4 = завершено
    private static final Map<UUID, Integer> playerState = new HashMap<>();
    private static final Map<UUID, Integer> playerTicks = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                UUID uuid = player.getUuid();
                int state = playerState.getOrDefault(uuid, 0);

                // Завершено — ничего не делаем
                if (state == 4) {
                    continue;
                }

                int ticks = playerTicks.getOrDefault(uuid, 0) + 1;
                playerTicks.put(uuid, ticks);

                switch (state) {
                    case 0: // Отсчёт в обычном мире
                        // Если уже в Backrooms (переподключение) — пропускаем
                        if (player.getWorld().getRegistryKey() == ModDimensions.BACKROOMS_LEVEL_KEY) {
                            playerState.put(uuid, 4);
                            break;
                        }
                        if (ticks >= TELEPORT_DELAY_TICKS) {
                            ModNetworking.sendFadePacket(player, true); // Fade IN
                            playerState.put(uuid, 1);
                            playerTicks.put(uuid, 0); // Сброс счётчика для следующей фазы
                            BackroomsMod.LOGGER.info("Fade-in started for: {}", player.getName().getString());
                        }
                        break;

                    case 1: // Ждём завершения fade-in
                        if (ticks >= FADE_DURATION_TICKS) {
                            teleportToBackrooms(player);
                            playerState.put(uuid, 2);
                            playerTicks.put(uuid, 0);
                            BackroomsMod.LOGGER.info("Teleported: {}", player.getName().getString());
                        }
                        break;

                    case 2: // Телепортирован, ждём немного перед fade-out
                        if (ticks >= 20) { // 1 сек задержка
                            ModNetworking.sendFadePacket(player, false); // Fade OUT
                            playerState.put(uuid, 3);
                            playerTicks.put(uuid, 0);
                            BackroomsMod.LOGGER.info("Fade-out started for: {}", player.getName().getString());
                        }
                        break;

                    case 3: // Ждём завершения fade-out
                        if (ticks >= FADE_DURATION_TICKS) {
                            playerState.put(uuid, 4); // Готово!
                            BackroomsMod.LOGGER.info("Backrooms sequence complete for: {}",
                                    player.getName().getString());
                        }
                        break;
                }
            }
        });

        // Очистка при отключении
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.getPlayer().getUuid();
            playerTicks.remove(uuid);
            // НЕ удаляем playerState — чтобы не телепортировать снова
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

    private static void teleportToBackrooms(ServerPlayerEntity player) {
        ServerWorld backroomsWorld = player.getServer().getWorld(ModDimensions.BACKROOMS_LEVEL_KEY);
        if (backroomsWorld == null) {
            BackroomsMod.LOGGER.error("Backrooms world is null!");
            return;
        }

        double spawnX = 3.5;
        double spawnY = 1.0;
        double spawnZ = 3.5;

        player.teleport(backroomsWorld, spawnX, spawnY, spawnZ, player.getYaw(), player.getPitch());
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

            // Safe check: floor is backrooms_floor, Y=1 and Y=2 are air
            BlockPos floor = new BlockPos(x, 0, z);
            BlockPos feet = new BlockPos(x, 1, z);
            BlockPos head = new BlockPos(x, 2, z);

            if (world.getBlockState(floor).isOf(ModBlocks.BACKROOMS_FLOOR) &&
                world.getBlockState(feet).isAir() &&
                world.getBlockState(head).isAir()) {

                player.teleport(world, rx, 1.0, rz, player.getYaw(), player.getPitch());
                return;
            }
        }

        // Fallback: spawn at original death pos but Y=1
        player.teleport(world, startX, 1.0, startZ, player.getYaw(), player.getPitch());
    }
}
