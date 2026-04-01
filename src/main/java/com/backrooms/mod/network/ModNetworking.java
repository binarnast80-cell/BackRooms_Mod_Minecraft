package com.backrooms.mod.network;

import com.backrooms.mod.BackroomsMod;
import com.backrooms.mod.client.BackroomsOverlay;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Сетевые пакеты для синхронизации эффекта затемнения экрана.
 * Сервер отправляет клиенту команду начать/завершить fade-эффект.
 */
public class ModNetworking {

    // Пакет: начать затемнение (fade in) или осветление (fade out)
    public static final Identifier FADE_PACKET_ID =
            new Identifier(BackroomsMod.MOD_ID, "fade_effect");

    /**
     * Серверная сторона: отправить пакет затемнения игроку.
     * @param player Целевой игрок
     * @param fadeIn true = затемнение, false = осветление
     */
    public static void sendFadePacket(ServerPlayerEntity player, boolean fadeIn) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(fadeIn);
        ServerPlayNetworking.send(player, FADE_PACKET_ID, buf);
    }

    /**
     * Регистрация серверных приёмников (C2S пакеты).
     * Пока пустой — в будущем для механики возврата.
     */
    public static void registerC2SPackets() {
        // Зарезервировано для будущих C2S пакетов
    }

    /**
     * Регистрация клиентских приёмников (S2C пакеты).
     */
    public static void registerS2CPackets() {
        ClientPlayNetworking.registerGlobalReceiver(FADE_PACKET_ID, (client, handler, buf, responseSender) -> {
            boolean fadeIn = buf.readBoolean();
            client.execute(() -> {
                BackroomsOverlay.startFade(fadeIn);
            });
        });
    }
}
