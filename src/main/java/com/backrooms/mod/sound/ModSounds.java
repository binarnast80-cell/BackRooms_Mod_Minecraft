package com.backrooms.mod.sound;

import com.backrooms.mod.BackroomsMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {

    // Жуткий эмбиент Backrooms (зацикленный, включает гул ламп)
    public static final Identifier BACKROOMS_SOUND_ID = new Identifier(BackroomsMod.MOD_ID, "backrooms_sound");
    public static final SoundEvent BACKROOMS_SOUND = SoundEvent.of(BACKROOMS_SOUND_ID);

    // Звук Howler (крик)
    public static final Identifier HOWLER_CRY_ID = new Identifier(BackroomsMod.MOD_ID, "howler_cry");
    public static final SoundEvent HOWLER_CRY = SoundEvent.of(HOWLER_CRY_ID);

    // Страшные звуки
    public static final Identifier SCARY_WHISPER_ID = new Identifier(BackroomsMod.MOD_ID, "scary_whisper");
    public static final SoundEvent SCARY_WHISPER = SoundEvent.of(SCARY_WHISPER_ID);

    public static final Identifier SCARY_SOUND_ID = new Identifier(BackroomsMod.MOD_ID, "scary_sound");
    public static final SoundEvent SCARY_SOUND = SoundEvent.of(SCARY_SOUND_ID);

    public static final Identifier SCARY_FOOTSTEPS_ID = new Identifier(BackroomsMod.MOD_ID, "scary_footsteps");
    public static final SoundEvent SCARY_FOOTSTEPS = SoundEvent.of(SCARY_FOOTSTEPS_ID);

    public static final Identifier SCARY_BREATH_ID = new Identifier(BackroomsMod.MOD_ID, "scary_breath");
    public static final SoundEvent SCARY_BREATH = SoundEvent.of(SCARY_BREATH_ID);

    public static void register() {
        Registry.register(Registries.SOUND_EVENT, BACKROOMS_SOUND_ID, BACKROOMS_SOUND);
        Registry.register(Registries.SOUND_EVENT, HOWLER_CRY_ID, HOWLER_CRY);
        Registry.register(Registries.SOUND_EVENT, SCARY_WHISPER_ID, SCARY_WHISPER);
        Registry.register(Registries.SOUND_EVENT, SCARY_SOUND_ID, SCARY_SOUND);
        Registry.register(Registries.SOUND_EVENT, SCARY_FOOTSTEPS_ID, SCARY_FOOTSTEPS);
        Registry.register(Registries.SOUND_EVENT, SCARY_BREATH_ID, SCARY_BREATH);

        BackroomsMod.LOGGER.info("Backrooms sounds registered");
    }
}
