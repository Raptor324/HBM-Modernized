package com.hbm_m.sound;



import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
/*@OnlyIn(Dist.CLIENT)
*///?}
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
@Environment(EnvType.CLIENT)*///?}
public class ChemicalPlantSoundInstance extends AbstractTickableSoundInstance {

    public ChemicalPlantSoundInstance(BlockPos pos) {
        super(ModSounds.CHEMICAL_PLANT.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.relative = false;
        this.attenuation = Attenuation.LINEAR;
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;
    }

    @Override
    public void tick() {
    }
}
