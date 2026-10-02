package com.hbm_m.sound;

import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.common.util.ForgeSoundType;

/** Eigene Klangarten des Originals ({@code com.hbm.blocks.ModSoundTypes}). */
public final class ModSoundTypes {

    private ModSoundTypes() {}

    /**
     * {@code grate = customStep(soundTypeStone, "hbm:step.metalBlock", 0.5F, 1.0F)}: Abbau/Setzen wie Stein, Schritte
     * (in 1.7 auch Treffer und Aufprall, die dort den Schrittklang nehmen) als Metallblock, Lautstaerke 0.5.
     */
    public static final ForgeSoundType GRATE = new ForgeSoundType(0.5F, 1.0F,
            () -> SoundEvents.STONE_BREAK, () -> HbmSoundsNT.get("step.metalblock"), () -> SoundEvents.STONE_PLACE,
            () -> HbmSoundsNT.get("step.metalblock"), () -> HbmSoundsNT.get("step.metalblock"));
}
