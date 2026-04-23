package com.backrooms.mod.mixin;

import com.backrooms.mod.world.ModDimensions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MusicTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MusicTracker.class)
public class BackroomsMusicMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void backrooms_disableVanillaMusic(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.getWorld().getRegistryKey() == ModDimensions.BACKROOMS_LEVEL_KEY) {
            // Останавливаем все ванильные музыкальные треки
            client.getSoundManager().stopSounds(null, net.minecraft.sound.SoundCategory.MUSIC);
            ci.cancel();
        }
    }
}
