package com.hbm_m.blockentity.network.radio;

import com.hbm_m.api.redstoneoverradio.IRORInteractive;
import com.hbm_m.api.redstoneoverradio.RORFunctionException;
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
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of {@code TileEntityRadioTorchController} (1.7.10 Original) - listens on {@link #channel},
 * and forwards each received message as a remote function call ({@code commandName!p1:p2:...}) into
 * the backing block (must implement {@link IRORInteractive}).
 * <p>
 * SCOPE-Vereinfachung: Der {@code "selfdestruct"}-Sonderfall entfaellt, siehe
 * {@link RadioTorchReceiverBlockEntity}.
 */
public class RadioTorchControllerBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity implements IRadioTorchConfigurable {

    public String channel = "";
    public String prev = "";
    public boolean polling = true;

    public RadioTorchControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADIO_TORCH_CONTROLLER_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RadioTorchControllerBlockEntity be) {
        if (level.isClientSide) return;
        RTTYNetwork.tickIfNeeded(level.getGameTime());
        if (be.channel.isEmpty()) return;

        Direction facing = state.hasProperty(RadioTorchBaseBlock.FACING) ? state.getValue(RadioTorchBaseBlock.FACING) : Direction.UP;
        BlockPos sourcePos = pos.relative(facing.getOpposite());
        if (!(level.getBlockEntity(sourcePos) instanceof IRORInteractive ror)) return;

        RTTYNetwork.RttyChannel chan = RTTYNetwork.listen(level, be.channel);
        if (chan == null) return;

        String rec = String.valueOf(chan.signal);
        if ((be.polling && chan.timeStamp >= level.getGameTime() - 1) || !rec.equals(be.prev)) {
            try {
                if (!rec.isEmpty()) ror.runRORFunction(IRORInteractive.PREFIX_FUNCTION + IRORInteractive.getCommand(rec), IRORInteractive.getParams(rec));
            } catch (RORFunctionException ignored) {}
            be.prev = rec;
        }
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("polling")) polling = data.getBoolean("polling");
        if (data.contains("channel")) channel = data.getString("channel");
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        tag.putBoolean("polling", polling);
        tag.putString("channel", channel);
        tag.putString("prev", prev);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        polling = tag.getBoolean("polling");
        channel = tag.getString("channel");
        prev = tag.getString("prev");
    }

}
