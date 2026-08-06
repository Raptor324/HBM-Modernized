package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.radio.IRadioTorchConfigurable;
import com.hbm_m.blockentity.network.radio.RTTYNetwork;

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
 * <p>
 * SCOPE-Vereinfachung: Das Original dekodiert das empfangene Signal ueber einen eigenen
 * {@code NoteBuilder}-Notencode in eine Sequenz aus Notenblock-Toenen unterschiedlicher Instrumente
 * (harp/bd/snare/hat/bassattack). Hier: ein einzelner Notenblock-Ton pro empfangenem Signal statt
 * vollstaendiger Notenfolge-Dekodierung - der Kernmechanismus (RTTY-Kanal &rarr; hoerbare Ausgabe)
 * bleibt erhalten.
 */
public class RadioRecBlockEntity extends BlockEntity implements IRadioTorchConfigurable {

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
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.RECORDS, 3.0F, 1.0F);
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
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("channel", channel);
        tag.putBoolean("isOn", isOn);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        channel = tag.getString("channel");
        isOn = tag.getBoolean("isOn");
    }
}
