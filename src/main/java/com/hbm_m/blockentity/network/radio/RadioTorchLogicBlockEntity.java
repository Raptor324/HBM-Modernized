package com.hbm_m.blockentity.network.radio;

import com.hbm_m.blockentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of {@code TileEntityRadioTorchLogic} (1.7.10 Original) - a 16-rule piecewise comparator:
 * listens on one channel, walks its 16 (comparison value, operator) rule slots in ascending or
 * descending order, and outputs the index of the first matching rule as its redstone signal (0 if
 * none match). Operators 0-5 are numeric ({@code <, <=, >=, >, ==, !=}), 6-9 are string
 * ({@code equals, !equals, contains, !contains}).
 */
public class RadioTorchLogicBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity implements IRadioTorchConfigurable {

    public String channel = "";
    public int lastState = 0;
    public long lastUpdate = 0;
    public boolean polling = false;
    public boolean descending = false;
    public final String[] mapping = new String[16];
    public final int[] conditions = new int[16];

    public RadioTorchLogicBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADIO_TORCH_LOGIC_BE.get(), pos, state);
        java.util.Arrays.fill(mapping, "");
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RadioTorchLogicBlockEntity be) {
        if (level.isClientSide) return;
        RTTYNetwork.tickIfNeeded(level.getGameTime());
        if (be.channel.isEmpty()) return;

        RTTYNetwork.RttyChannel chan = RTTYNetwork.listen(level, be.channel);
        if (chan == null) return;
        if (!(be.polling || (chan.timeStamp > be.lastUpdate - 1 && chan.timeStamp != -1))) return;

        String msg = String.valueOf(chan.signal);
        be.lastUpdate = level.getGameTime();
        if (chan.timeStamp < be.lastUpdate - 2 && be.polling) msg = "0";

        int nextState = 0;
        if (be.descending) {
            for (int i = 15; i >= 0; i--) {
                if (!be.mapping[i].isEmpty() && be.parseSignal(msg, i)) { nextState = i; break; }
            }
        } else {
            for (int i = 0; i <= 15; i++) {
                if (!be.mapping[i].isEmpty() && be.parseSignal(msg, i)) { nextState = i; break; }
            }
        }

        if (be.lastState != nextState) {
            be.lastState = nextState;
            be.setChanged();
            be.syncToClient();
            level.updateNeighborsAt(pos, state.getBlock());
        }
    }

    private boolean parseSignal(String signal, int index) {
        if (conditions[index] <= 5) {
            long sig, map;
            try {
                sig = Long.parseLong(signal);
                map = Long.parseLong(mapping[index]);
            } catch (NumberFormatException x) {
                return false;
            }
            return switch (conditions[index]) {
                case 1 -> sig <= map;
                case 2 -> sig >= map;
                case 3 -> sig > map;
                case 4 -> sig == map;
                case 5 -> sig != map;
                default -> sig < map;
            };
        }
        return switch (conditions[index]) {
            case 7 -> !signal.equals(mapping[index]);
            case 8 -> signal.contains(mapping[index]);
            case 9 -> !signal.contains(mapping[index]);
            default -> signal.equals(mapping[index]);
        };
    }

    public void receiveControl(CompoundTag data) {
        if (data.contains("polling")) polling = data.getBoolean("polling");
        if (data.contains("channel")) channel = data.getString("channel");
        if (data.contains("descending")) descending = data.getBoolean("descending");
        for (int i = 0; i < 16; i++) if (data.contains("mapping" + i)) mapping[i] = data.getString("mapping" + i);
        for (int i = 0; i < 16; i++) if (data.contains("cond" + i)) conditions[i] = data.getInt("cond" + i);
        setChanged();
        syncToClient();
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        tag.putBoolean("polling", polling);
        tag.putBoolean("descending", descending);
        tag.putInt("lastState", lastState);
        tag.putLong("lastUpdate", lastUpdate);
        tag.putString("channel", channel);
        for (int i = 0; i < 16; i++) tag.putString("mapping" + i, mapping[i]);
        for (int i = 0; i < 16; i++) tag.putInt("cond" + i, conditions[i]);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        polling = tag.getBoolean("polling");
        descending = tag.getBoolean("descending");
        lastState = tag.getInt("lastState");
        lastUpdate = tag.getLong("lastUpdate");
        channel = tag.getString("channel");
        for (int i = 0; i < 16; i++) mapping[i] = tag.contains("mapping" + i) ? tag.getString("mapping" + i) : "";
        for (int i = 0; i < 16; i++) conditions[i] = tag.getInt("cond" + i);
    }

}
