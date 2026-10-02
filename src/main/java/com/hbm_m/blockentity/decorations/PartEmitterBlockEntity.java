package com.hbm_m.blockentity.decorations;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.util.ParticleUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code PartEmitter.TileEntityPartEmitter}: Effekt 1 Gasflamme 4.5 ueber dem Block, 2 dunkle Rauchsaeule,
 * 3 breite helle Rauchsaeule ueber einer 3x3-Flaeche (Partikel an Spieler im Umkreis 150).
 */
public class PartEmitterBlockEntity extends BaseHbmBlockEntity {

    public static final int RANGE = 150;
    public static final int EFFECT_COUNT = 4;

    public int effect = 0;

    public PartEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PART_EMITTER.get(), pos, state);
    }

    public static void serverTick(Level world, BlockPos pos, BlockState state, PartEmitterBlockEntity te) {
        if (!(world instanceof ServerLevel server)) return;
        RandomSource rand = world.getRandom();
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        CompoundTag data = new CompoundTag();

        if (te.effect == 1) {
            ParticleUtil.spawnGasFlame(world, pos.getX() + rand.nextDouble(), pos.getY() + 4.5 + rand.nextDouble(), pos.getZ() + rand.nextDouble(),
                    rand.nextGaussian() * 0.2, 0.1, rand.nextGaussian() * 0.2);
        }
        if (te.effect == 2) {
            data.putString("type", "tower");
            data.putFloat("lift", 5F);
            data.putFloat("base", 0.25F);
            data.putFloat("max", 5F);
            data.putInt("life", 560 + rand.nextInt(20));
            data.putInt("color", 0x404040);
        }
        if (te.effect == 3) {
            data.putString("type", "tower");
            data.putFloat("lift", 0.5F);
            data.putFloat("base", 1F);
            data.putFloat("max", 10F);
            data.putInt("life", 750 + rand.nextInt(250));
            x = pos.getX() + 0.5 + rand.nextDouble() * 3 - 1.5;
            y = pos.getY() + 1;
            z = pos.getZ() + 0.5 + rand.nextDouble() * 3 - 1.5;
        }

        if (data.contains("type")) {
            com.hbm_m.particle.helper.IParticleCreator.sendPacket(server, x, y, z, RANGE, data);
        }
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag nbt, @Nullable HolderLookup.Provider registries) {
        this.effect = nbt.getInt("effect");
    }

    @Override
    protected void writeNbtData(@NotNull CompoundTag nbt, @Nullable HolderLookup.Provider registries) {
        nbt.putInt("effect", this.effect);
    }
}
