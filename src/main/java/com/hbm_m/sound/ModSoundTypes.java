package com.hbm_m.sound;

import net.minecraft.sounds.SoundEvents;
//? if forge {
import net.minecraftforge.common.util.ForgeSoundType;
//?}

/** Eigene Klangarten des Originals ({@code com.hbm.blocks.ModSoundTypes}). */
public final class ModSoundTypes {

    private ModSoundTypes() {}

    /**
     * {@code grate = customStep(soundTypeStone, "hbm:step.metalBlock", 0.5F, 1.0F)}: Abbau/Setzen wie Stein, Schritte
     * (in 1.7 auch Treffer und Aufprall, die dort den Schrittklang nehmen) als Metallblock, Lautstaerke 0.5.
     */
    //? if forge {
    public static final ForgeSoundType GRATE = new ForgeSoundType(0.5F, 1.0F,
    //?} else {
    /*public static final net.neoforged.neoforge.common.util.DeferredSoundType GRATE = new net.neoforged.neoforge.common.util.DeferredSoundType(0.5F, 1.0F,
    *///?}
            () -> SoundEvents.STONE_BREAK, () -> HbmSoundsNT.get("step.metalblock"), () -> SoundEvents.STONE_PLACE,
            () -> HbmSoundsNT.get("step.metalblock"), () -> HbmSoundsNT.get("step.metalblock"));

    /** {@code flesh = placeBreakStep("hbm:block.flesh", ..., 0.5F, 1.0F)}: alles mit dem Fleischklang. */
    //? if forge {
    public static final ForgeSoundType FLESH = new ForgeSoundType(0.5F, 1.0F,
    //?} else {
    /*public static final net.neoforged.neoforge.common.util.DeferredSoundType FLESH = new net.neoforged.neoforge.common.util.DeferredSoundType(0.5F, 1.0F,
    *///?}
            () -> HbmSoundsNT.get("block.flesh"), () -> HbmSoundsNT.get("block.flesh"), () -> HbmSoundsNT.get("block.flesh"),
            () -> HbmSoundsNT.get("block.flesh"), () -> HbmSoundsNT.get("block.flesh"));

    /**
     * {@code pipe = customDig(soundTypeMetal, "hbm:block.pipePlaced", 0.85F, 0.85F)}: Setzen/Abbauen mit dem Rohrklang,
     * Schritte wie Metall. Die zufaellige Tonhoehen-Huelle des Originals (enveloped/pitchFunction) gibt es hier nicht.
     */
    //? if forge {
    public static final ForgeSoundType PIPE = new ForgeSoundType(0.85F, 0.85F,
    //?} else {
    /*public static final net.neoforged.neoforge.common.util.DeferredSoundType PIPE = new net.neoforged.neoforge.common.util.DeferredSoundType(0.85F, 0.85F,
    *///?}
            () -> HbmSoundsNT.get("block.pipeplaced"), () -> SoundEvents.METAL_STEP, () -> HbmSoundsNT.get("block.pipeplaced"),
            () -> SoundEvents.METAL_HIT, () -> SoundEvents.METAL_FALL);

    /** {@code platemetal = placeBreakStep("hbm:block.platemetalPlace", "hbm:block.platemetalPlace", "hbm:step.platemetal", 1.0F, 1.0F)}. */
    //? if forge {
    public static final ForgeSoundType PLATEMETAL = new ForgeSoundType(1.0F, 1.0F,
    //?} else {
    /*public static final net.neoforged.neoforge.common.util.DeferredSoundType PLATEMETAL = new net.neoforged.neoforge.common.util.DeferredSoundType(1.0F, 1.0F,
    *///?}
            () -> HbmSoundsNT.get("block.platemetalplace"), () -> HbmSoundsNT.get("step.platemetal"), () -> HbmSoundsNT.get("block.platemetalplace"),
            () -> HbmSoundsNT.get("step.platemetal"), () -> HbmSoundsNT.get("step.platemetal"));
}
