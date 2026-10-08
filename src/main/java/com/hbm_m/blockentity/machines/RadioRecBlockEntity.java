package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.radio.IRadioTorchConfigurable;
import com.hbm_m.blockentity.network.radio.RTTYNetwork;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of {@code TileEntityRadioRec} (1.7.10 Original) - "FM Radio". Listens on an RTTY channel
 * ({@link RTTYNetwork}) and, on receiving a fresh signal, plays a note-block sound at its position.
 * Das Signal wird wie im Original ueber {@link com.hbm_m.util.NoteBuilder} in Notenblock-Toene zerlegt
 * (harp/bd/snare/hat/bassattack), Tonhoehe {@code 2^((note + 12 * oktave - 12) / 12)}, Lautstaerke 3.
 */
public class RadioRecBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity implements IRadioTorchConfigurable {

    public String channel = "";
    public boolean isOn = false;

    private long lastPlayedTick = -1;

    public RadioRecBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADIOREC_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RadioRecBlockEntity be) {
        if (level.isClientSide) return;

        RTTYNetwork.tickIfNeeded(level.getGameTime());

        if (!be.isOn || be.channel.isEmpty()) return;

        RTTYNetwork.RttyChannel sig = RTTYNetwork.listen(level, be.channel);
        if (sig != null && sig.timeStamp != be.lastPlayedTick && sig.signal != null) {
            be.lastPlayedTick = sig.timeStamp;
            for (com.hbm_m.util.NoteBuilder.Hit note : com.hbm_m.util.NoteBuilder.translate(sig.signal + "")) {
                int noteId = note.note().ordinal() + note.octave().ordinal() * 12;
                net.minecraft.sounds.SoundEvent s = SoundEvents.NOTE_BLOCK_HARP.value();
                switch (note.instrument()) {
                    case BASSDRUM -> s = SoundEvents.NOTE_BLOCK_BASEDRUM.value();
                    case SNARE -> s = SoundEvents.NOTE_BLOCK_SNARE.value();
                    case CLICKS -> s = SoundEvents.NOTE_BLOCK_HAT.value();
                    case BASSGUITAR -> s = SoundEvents.NOTE_BLOCK_BASS.value();
                    default -> { }
                }
                float f = (float) Math.pow(2.0D, (double) (noteId - 12) / 12.0D);
                level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, s, SoundSource.RECORDS, 3.0F, f);
            }
        }
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("channel")) channel = data.getString("channel");
        if (data.contains("isOn")) isOn = data.getBoolean("isOn");
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        tag.putString("channel", channel);
        tag.putBoolean("isOn", isOn);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        channel = tag.getString("channel");
        isOn = tag.getBoolean("isOn");
    }
}
