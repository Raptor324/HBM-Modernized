package com.hbm_m.blockentity.network.radio;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of {@code TileEntityRadioTorchBase} (1.7.10 Original) - shared channel/state fields for all
 * six radio-torch block entities (Sender/Receiver/Logic share this directly; Reader/Controller/Counter
 * extend it too even though they use a per-slot channel array on top).
 * <p>
 * SCOPE-Vereinfachung: Die OpenComputers-{@code @Callback}-Methoden des Originals entfallen (kein
 * OpenComputers-Support in diesem Port).
 */
public abstract class RadioTorchBaseBlockEntity extends BlockEntity implements IRadioTorchConfigurable {

    /** Channel we're broadcasting on/listening to. */
    public String channel = "";
    /** Previous redstone state for input/output, needed for state-change detection. */
    public int lastState = 0;
    /** Last update tick, needed for receivers listening for changes. */
    public long lastUpdate = 0;
    /** Switches state-change mode to tick-based polling. */
    public boolean polling = false;
    /** Switches redstone passthrough to custom signal mapping. */
    public boolean customMap = false;
    /** Custom mapping, redstone level (0-15) -> arbitrary string. */
    public final String[] mapping = new String[16];

    protected RadioTorchBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("polling", polling);
        tag.putBoolean("customMap", customMap);
        tag.putInt("lastState", lastState);
        tag.putLong("lastUpdate", lastUpdate);
        tag.putString("channel", channel);
        for (int i = 0; i < 16; i++) if (mapping[i] != null) tag.putString("mapping" + i, mapping[i]);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        polling = tag.getBoolean("polling");
        customMap = tag.getBoolean("customMap");
        lastState = tag.getInt("lastState");
        lastUpdate = tag.getLong("lastUpdate");
        channel = tag.getString("channel");
        for (int i = 0; i < 16; i++) mapping[i] = tag.contains("mapping" + i) ? tag.getString("mapping" + i) : null;
    }

    @Override
    public CompoundTag getUpdateTag() { CompoundTag t = super.getUpdateTag(); saveAdditional(t); return t; }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Applies a settings-save packet from the client GUI. Subclasses extend for their own extra fields. */
    public void receiveControl(CompoundTag data) {
        if (data.contains("polling")) polling = data.getBoolean("polling");
        if (data.contains("customMap")) customMap = data.getBoolean("customMap");
        if (data.contains("channel")) channel = data.getString("channel");
        for (int i = 0; i < 16; i++) if (data.contains("mapping" + i)) mapping[i] = data.getString("mapping" + i);
        setChanged();
        syncToClient();
    }

    protected void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
