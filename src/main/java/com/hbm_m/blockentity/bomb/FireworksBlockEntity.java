package com.hbm_m.blockentity.bomb;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.entity.effect.EntityFireworks;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code TileEntityFireworks}: mit Redstone-Signal alle 30 Ticks eine Rakete je Buchstabe der Nachricht (3x3-Raster
 * auf dem Block), bis die Ladungen verbraucht sind; nach der ganzen Nachricht 100 Ticks Pause.
 */
public class FireworksBlockEntity extends BaseHbmBlockEntity {

    public int color = 0xff0000;
    public String message = "NUCLEAR TECH";
    public int charges;

    int index;
    int delay;

    public FireworksBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FIREWORKS.get(), pos, state);
    }

    public static void serverTick(Level world, BlockPos pos, BlockState state, FireworksBlockEntity te) {
        if (world.hasNeighborSignal(pos) && !te.message.isEmpty() && te.charges > 0) {
            te.delay--;
            if (te.delay <= 0) {
                te.delay = 30;
                int c = te.message.charAt(te.index);
                int mod = te.index % 9;
                double offX = (mod / 3 - 1) * 0.3125;
                double offZ = (mod % 3 - 1) * 0.3125;

                EntityFireworks fireworks = new EntityFireworks(world, pos.getX() + 0.5 + offX, pos.getY() + 1.5, pos.getZ() + 0.5 + offZ, te.color, c);
                world.addFreshEntity(fireworks);
                world.playSound(null, fireworks.getX(), fireworks.getY(), fireworks.getZ(), HbmSoundsNT.get("weapon.rocketFlame"), SoundSource.BLOCKS, 3.0F, 1.0F);

                te.charges--;
                te.setChanged();

                CompoundTag data = new CompoundTag();
                data.putString("type", "vanillaExt");
                data.putString("mode", "flame");
                com.hbm_m.particle.helper.IParticleCreator.sendPacket((ServerLevel) world, pos.getX() + 0.5 + offX, pos.getY() + 1.125, pos.getZ() + 0.5 + offZ, 100, data);

                te.index++;
                if (te.index >= te.message.length()) {
                    te.index = 0;
                    te.delay = 100;
                }
            }
        } else {
            te.delay = 0;
            te.index = 0;
        }
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag nbt, @Nullable HolderLookup.Provider registries) {
        charges = nbt.getInt("charges");
        color = nbt.getInt("color");
        message = nbt.getString("message");
    }

    @Override
    protected void writeNbtData(@NotNull CompoundTag nbt, @Nullable HolderLookup.Provider registries) {
        nbt.putInt("charges", charges);
        nbt.putInt("color", color);
        nbt.putString("message", message);
    }
}
