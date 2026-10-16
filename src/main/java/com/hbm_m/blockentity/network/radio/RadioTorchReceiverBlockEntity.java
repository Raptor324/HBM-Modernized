package com.hbm_m.blockentity.network.radio;

import com.hbm_m.blockentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of {@code TileEntityRadioTorchReceiver} (1.7.10 Original). Listens on {@link #channel} and
 * outputs the received value as a redstone signal via {@link #lastState}.
 * <p>
 * <p><b>Ein Wort sprengt ihn.</b> Kommt auf dem Kanal die Nachricht {@code selfdestruct} an,
 * fliegt der Empfaenger in die Luft. Das ist im Original Absicht: wer den Funkkanal einer fremden
 * Anlage kennt, kann sie damit ausschalten - also waehlt man seine Kanaele mit Bedacht.</p>
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

        // 1:1-Port: der Sonderbefehl sprengt den Empfaenger. Wer den Kanal kennt, kann eine
        // fremde Anlage damit lahmlegen - genau das ist im Original der Reiz daran.
        if ("selfdestruct".equals(msg)) {
            level.destroyBlock(pos, false);
            level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    5.0F, Level.ExplosionInteraction.BLOCK);
            return;
        }

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
