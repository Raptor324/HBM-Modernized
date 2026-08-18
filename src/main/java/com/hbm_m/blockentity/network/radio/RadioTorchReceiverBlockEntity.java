package com.hbm_m.blockentity.network.radio;

import com.hbm_m.blockentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of {@code TileEntityRadioTorchReceiver} (1.7.10 Original). Listens on {@link #channel} and
 * outputs the received value as a redstone signal via {@link #lastState}.
 * <p>
 * SCOPE-Vereinfachung: Das Original hat einen Sonder-Payload {@code "selfdestruct"}, der eine
 * Explosion ausloest (Anti-Griefing-Gag). Nicht portiert - kein tragendes Feature, birgt nur
 * Risiko fuer unbeabsichtigte Explosionen bei einer wortwoertlichen Portierung.
 */
public class RadioTorchReceiverBlockEntity extends RadioTorchBaseBlockEntity {

    public RadioTorchReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADIO_TORCH_RECEIVER_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RadioTorchReceiverBlockEntity be) {
        if (level.isClientSide) return;
        RTTYNetwork.tickIfNeeded(level.getGameTime());
        if (be.channel.isEmpty()) return;

        RTTYNetwork.RttyChannel chan = RTTYNetwork.listen(level, be.channel);
        if (chan == null) return;
        if (!(be.polling || (chan.timeStamp > be.lastUpdate - 1 && chan.timeStamp != -1))) return;

        String msg = String.valueOf(chan.signal);
        be.lastUpdate = level.getGameTime();
        int nextState = 0;

        if (be.customMap) {
            for (int i = 15; i >= 0; i--) {
                if (msg.equals(be.mapping[i])) {
                    nextState = i;
                    break;
                }
            }
        } else {
            int sig;
            try {
                sig = Integer.parseInt(msg);
            } catch (NumberFormatException x) {
                sig = 0;
            }
            nextState = Math.max(0, Math.min(15, sig));
        }

        if (chan.timeStamp < be.lastUpdate - 2 && be.polling) nextState = 0;

        if (be.lastState != nextState) {
            be.lastState = nextState;
            be.setChanged();
            be.syncToClient();
            level.updateNeighborsAt(pos, state.getBlock());
        }
    }
}
