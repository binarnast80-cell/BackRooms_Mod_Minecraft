package com.backrooms.mod.sound;

import com.backrooms.mod.BackroomsMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {

    // Гул люминесцентных ламп (зацикленный)
    public static final Identifier LAMP_HUM_ID = new Identifier(BackroomsMod.MOD_ID, "lamp_hum");
    public static final SoundEvent LAMP_HUM = SoundEvent.of(LAMP_HUM_ID);

    // Жуткий эмбиент Backrooms (зацикленный)
    public static final Identifier AMBIENT_BACKROOMS_ID = new Identifier(BackroomsMod.MOD_ID, "ambient_backrooms");
    public static final SoundEvent AMBIENT_BACKROOMS = SoundEvent.of(AMBIENT_BACKROOMS_ID);

    // Звук Howler (крик)
    public static final Identifier HOWLER_CRY_ID = new Identifier(BackroomsMod.MOD_ID, "howler_cry");
    public static final SoundEvent HOWLER_CRY = SoundEvent.of(HOWLER_CRY_ID);

    public static void register() {
        Registry.register(Registries.SOUND_EVENT, LAMP_HUM_ID, LAMP_HUM);
        Registry.register(Registries.SOUND_EVENT, AMBIENT_BACKROOMS_ID, AMBIENT_BACKROOMS);
        Registry.register(Registries.SOUND_EVENT, HOWLER_CRY_ID, HOWLER_CRY);

        BackroomsMod.LOGGER.info("Backrooms sounds registered");
    }
}
