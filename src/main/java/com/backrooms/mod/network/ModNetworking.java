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
 * Сетевые пакеты для эффектов телепортации.
 */
public class ModNetworking {

    public static final Identifier FADE_PACKET_ID =
            new Identifier(BackroomsMod.MOD_ID, "fade_effect");
    
    // Пакет погружения (3 сек медленное затемнение)
    public static final Identifier SINK_FADE_PACKET_ID =
            new Identifier(BackroomsMod.MOD_ID, "sink_fade");
    
    // Пакет прибытия (резкое «открытие глаз» + звук)
    public static final Identifier ARRIVAL_PACKET_ID =
            new Identifier(BackroomsMod.MOD_ID, "arrival");

    public static void sendFadePacket(ServerPlayerEntity player, boolean fadeIn) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(fadeIn);
        ServerPlayNetworking.send(player, FADE_PACKET_ID, buf);
    }

    /** Отправить пакет начала погружения (медленный fade-in 3 сек) */
    public static void sendSinkFadePacket(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        ServerPlayNetworking.send(player, SINK_FADE_PACKET_ID, buf);
    }

    /** Отправить пакет прибытия (быстрый fade-out + звук) */
    public static void sendArrivalPacket(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        ServerPlayNetworking.send(player, ARRIVAL_PACKET_ID, buf);
    }

    public static void registerC2SPackets() {
        // Зарезервировано
    }

    public static void registerS2CPackets() {
        ClientPlayNetworking.registerGlobalReceiver(FADE_PACKET_ID, (client, handler, buf, responseSender) -> {
            boolean fadeIn = buf.readBoolean();
            client.execute(() -> BackroomsOverlay.startFade(fadeIn));
        });

        ClientPlayNetworking.registerGlobalReceiver(SINK_FADE_PACKET_ID, (client, handler, buf, responseSender) -> {
            client.execute(() -> BackroomsOverlay.startSinkFade());
        });

        ClientPlayNetworking.registerGlobalReceiver(ARRIVAL_PACKET_ID, (client, handler, buf, responseSender) -> {
            client.execute(() -> BackroomsOverlay.startArrival());
        });
    }
}
