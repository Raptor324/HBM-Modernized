package com.hbm_m.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/**
 * Seitentrennung: Zugriffe auf den Client ({@link Minecraft}) fuer Code, der in gemeinsamen Klassen
 * steht (Bloecke, Items, BEs, Pakete). Die Rueckgabetypen sind bewusst die gemeinsamen Oberklassen
 * ({@link Player} statt LocalPlayer, {@link Level} statt ClientLevel): so sieht der JVM-Verifier der
 * aufrufenden Klasse nie einen Client-Typ, und der dedizierte Server laedt diese Klasse nie
 * (Aufrufe nur hinter isClientSide-/Dist-Pruefung bzw. aus client-only Hooks wie animateTick).
 */
public final class ClientAccess {

    private ClientAccess() {}

    /** {@code Minecraft.getInstance().player} */
    public static Player player() {
        return Minecraft.getInstance().player;
    }

    /** {@code Minecraft.getInstance().level} */
    public static Level level() {
        return Minecraft.getInstance().level;
    }

    /** {@code Minecraft.getInstance().hitResult} */
    public static HitResult hitResult() {
        return Minecraft.getInstance().hitResult;
    }

    /** {@code mc.particleEngine.destroy(pos, state)}, nur mit geladener Welt. */
    public static void destroyBlockParticles(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        mc.particleEngine.destroy(pos, state);
    }

    /** UI-Sound wie {@code SimpleSoundInstance.forUI(sound, pitch)}. */
    public static void playUiSound(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch));
    }
}
