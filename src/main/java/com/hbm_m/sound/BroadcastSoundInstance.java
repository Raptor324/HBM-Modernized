package com.hbm_m.sound;

import java.util.Random;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * Permanenter Loop-Sound des Broadcasters, solange der Block existiert.
 * Lebenszyklus wird extern ueber ClientSoundManager/ClientSoundBootstrap verwaltet.
 * <p>
 * Track-Wahl und Lautstaerke nach NEXT/Original (TileEntityBroadcaster.createAudioLoop):
 * genau EINE der 3 Spuren, deterministisch pro Blockposition (Stable ueber Chunk-Reload
 * und Re-Login), hörbar bis CONFUSION_RADIUS = 25 Blocke.
 */
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}
@OnlyIn(Dist.CLIENT)
public class BroadcastSoundInstance extends AbstractTickableSoundInstance {

    private final BlockPos origin;

    /**
     * Original: {@code new Random(xCoord + yCoord + zCoord)},
     * {@code "hbm:block.broadcast" + (nextInt(3) + 1)} — seeded per position.
     */
    private static SoundEvent trackFor(BlockPos pos) {
        return switch (new Random(pos.getX() + pos.getY() + pos.getZ()).nextInt(3)) {
            case 0 -> ModSounds.BROADCAST1.get();
            case 1 -> ModSounds.BROADCAST2.get();
            default -> ModSounds.BROADCAST3.get();
        };
    }

    public BroadcastSoundInstance(BlockPos pos) {
        // Volume-Mapping: Vanilla clamped den Gain auf 1.0 (SoundEngine#calculateVolume),
        // aber der lineare Rolloff-Radius ist Math.max(volume, 1) * 16 — volume 25/16
        // laesst die Schleife exakt bei 25 Blocken verstummen (NEXT spielt mit wortwoertlichem
        // volume 25 und eigenem 25-Block-Falloff, ebenso bei Gain 1.0 geklemmt).
        super(trackFor(pos), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.volume = 25.0F / 16.0F;
        this.looping = true;
        this.delay = 0;
        this.relative = false;
        this.attenuation = Attenuation.LINEAR;
        this.origin = pos;
        updatePosition();
    }

    // Sable ships move - re-anchor in world space every tick (raw plot-grid
    // coords are millions of blocks out and vanilla mutes the loop).
    private void updatePosition() {
        net.minecraft.world.phys.Vec3 world = SableSoundPosition.project(origin, 0.5D);
        this.x = world.x;
        this.y = world.y;
        this.z = world.z;
    }

    @Override
    public void tick() {
        updatePosition();
    }
}
