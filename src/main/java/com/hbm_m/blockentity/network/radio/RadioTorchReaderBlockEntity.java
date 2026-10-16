package com.hbm_m.blockentity.network.radio;

import com.hbm_m.api.redstoneoverradio.IRORValueProvider;
import com.hbm_m.block.machines.radio.RadioTorchBaseBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.Locale;

/**
 * Port of {@code TileEntityRadioTorchReader} (1.7.10 Original) - reads up to 8 named stats off the
 * backing block (must implement {@link IRORValueProvider}) and broadcasts each on its own channel.
 */
public class RadioTorchReaderBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity implements IRadioTorchConfigurable {

    public final String[] channels = new String[8];
    public final String[] names = new String[8];
    public final String[] prev = new String[8];
    public boolean polling = false;

    public RadioTorchReaderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADIO_TORCH_READER_BE.get(), pos, state);
        Arrays.fill(channels, "");
        Arrays.fill(names, "");
        Arrays.fill(prev, "");
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RadioTorchReaderBlockEntity be) {
        if (level.isClientSide) return;
        RTTYNetwork.tickIfNeeded(level.getGameTime());

        Direction facing = state.hasProperty(RadioTorchBaseBlock.FACING) ? state.getValue(RadioTorchBaseBlock.FACING) : Direction.UP;
        BlockPos sourcePos = pos.relative(facing.getOpposite());
        if (!(level.getBlockEntity(sourcePos) instanceof IRORValueProvider provider)) return;

        for (int i = 0; i < 8; i++) {
            String channel = be.channels[i];
            String name = be.names[i];
            if (channel == null || channel.isEmpty() || name == null || name.isEmpty()) continue;

            String value = provider.provideRORValue(IRORValueProvider.PREFIX_VALUE + name.toLowerCase(Locale.US));
            if (value == null) continue;

            if (be.polling || !value.equals(be.prev[i])) {
                RTTYNetwork.broadcast(level, channel, value);
                be.prev[i] = value;
            }
        }
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("polling")) polling = data.getBoolean("polling");
        for (int i = 0; i < 8; i++) if (data.contains("channel" + i)) channels[i] = data.getString("channel" + i);
        for (int i = 0; i < 8; i++) if (data.contains("name" + i)) names[i] = data.getString("name" + i);
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        tag.putBoolean("polling", polling);
        for (int i = 0; i < 8; i++) tag.putString("channel" + i, channels[i]);
        for (int i = 0; i < 8; i++) tag.putString("name" + i, names[i]);
        for (int i = 0; i < 8; i++) tag.putString("prev" + i, prev[i]);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        polling = tag.getBoolean("polling");
        for (int i = 0; i < 8; i++) channels[i] = tag.contains("channel" + i) ? tag.getString("channel" + i) : "";
        for (int i = 0; i < 8; i++) names[i] = tag.contains("name" + i) ? tag.getString("name" + i) : "";
        for (int i = 0; i < 8; i++) prev[i] = tag.contains("prev" + i) ? tag.getString("prev" + i) : "";
    }

}
