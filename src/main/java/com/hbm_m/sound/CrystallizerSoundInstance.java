package com.hbm_m.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

//? if forge {
/*import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
*///?}
//? if fabric {
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;//?}

//? if forge {
/*@OnlyIn(Dist.CLIENT)
*///?}
//? if fabric {
@Environment(EnvType.CLIENT)//?}
public class CrystallizerSoundInstance extends AbstractTickableSoundInstance {

    public CrystallizerSoundInstance(BlockPos pos) {
        super(ModSounds.CHEMICAL_PLANT.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.relative = false;
        this.attenuation = Attenuation.LINEAR;

        // Controller is at the base of the tall multiblock, so place the loop near the middle.
        this.x = pos.getX() + 0.5D;
        this.y = pos.getY() + 3.0D;
        this.z = pos.getZ() + 0.5D;

        // Original Crystallizer uses chemicalPlant.ogg with pitch lowered to 0.75.
        this.volume = 1.0F;
        this.pitch = 0.75F;
    }

    @Override
    public void tick() {
        // Lifecycle is controlled by ClientSoundManager via MachineCrystallizerBlockEntity.clientTick().
    }
}
