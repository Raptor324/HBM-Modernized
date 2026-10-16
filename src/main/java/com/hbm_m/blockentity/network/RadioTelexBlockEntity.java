package com.hbm_m.blockentity.network;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.radio.IRadioTorchConfigurable;
import com.hbm_m.blockentity.network.radio.RTTYNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Port of {@code TileEntityRadioTelex} (1.7.10 Original) - "Telex Machine" text teleprinter, wired
 * directly into the existing {@link RTTYNetwork} pub/sub bus. Sends its tx buffer one character per
 * tick on {@code txChannel}, decodes incoming characters from {@code rxChannel} into a 5-line rx
 * buffer, and can print the received message as a physical paper item.
 * <p>
 * SCOPE-Vereinfachung: Das Original ist ein {@code BlockDummyable}-Multiblock (wie u.a. bereits bei
 * Boiler/Foundry dieses Ports vereinfacht) - hier ein einzelnes Block. Die OpenComputers-Komponente
 * ({@code ntm_telex}) entfaellt (keine OpenComputers-Entsprechung in diesem Fork). Die
 * Pause-Steuerzeichen-Verzoegerung des Originals entfaellt - Zeichen werden gleichmaessig 1/Tick
 * gesendet, End-of-Line/-Transmission-Steuerzeichen bleiben erhalten.
 */
public class RadioTelexBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity implements IRadioTorchConfigurable {

    private static final int LINE_WIDTH = 33;
    private static final int LINE_COUNT = 5;
    private static final char EOL = '\n';
    private static final char EOT = (char) 4;   // end-of-transmission control char
    private static final char CLEAR = (char) 127; // clear-buffer control char

    public String txChannel = "";
    public String rxChannel = "";
    public final String[] txLines = new String[LINE_COUNT];
    public final String[] rxLines = new String[LINE_COUNT];

    public boolean isSending = false;
    private int sendLine = 0;
    private int sendChar = 0;
    private int writingRxLine = 0;
    private long lastRxTick = -1;

    public RadioTelexBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADIO_TELEX_BE.get(), pos, state);
        for (int i = 0; i < LINE_COUNT; i++) {
            txLines[i] = "";
            rxLines[i] = "";
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RadioTelexBlockEntity be) {
        if (level.isClientSide) return;

        RTTYNetwork.tickIfNeeded(level.getGameTime());

        be.tickSend(level);
        be.tickReceive(level);
    }

    private void tickSend(Level level) {
        if (!isSending || txChannel.isEmpty()) return;

        if (sendLine >= LINE_COUNT) {
            RTTYNetwork.broadcast(level, txChannel, String.valueOf(EOT));
            isSending = false;
            setChanged();
            return;
        }

        String line = txLines[sendLine] != null ? txLines[sendLine] : "";
        if (sendChar >= line.length()) {
            RTTYNetwork.broadcast(level, txChannel, String.valueOf(EOL));
            sendLine++;
            sendChar = 0;
        } else {
            RTTYNetwork.broadcast(level, txChannel, String.valueOf(line.charAt(sendChar)));
            sendChar++;
        }
        setChanged();
    }

    private void tickReceive(Level level) {
        if (rxChannel.isEmpty()) return;

        RTTYNetwork.RttyChannel sig = RTTYNetwork.listen(level, rxChannel);
        if (sig == null || sig.signal == null || sig.timeStamp == lastRxTick) return;
        lastRxTick = sig.timeStamp;

        String signal = String.valueOf(sig.signal);
        if (signal.isEmpty()) return;
        char c = signal.charAt(0);

        if (c == EOT) {
            // end of transmission - nothing further to do
        } else if (c == EOL) {
            writingRxLine = Math.min(writingRxLine + 1, LINE_COUNT - 1);
        } else if (c == CLEAR) {
            clearRx();
        } else {
            String cur = rxLines[writingRxLine] != null ? rxLines[writingRxLine] : "";
            if (cur.length() < LINE_WIDTH) {
                rxLines[writingRxLine] = cur + c;
            }
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void clearRx() {
        for (int i = 0; i < LINE_COUNT; i++) rxLines[i] = "";
        writingRxLine = 0;
    }

    public void startSending() {
        if (isSending) return;
        isSending = true;
        sendLine = 0;
        sendChar = 0;
        setChanged();
    }

    public void print() {
        if (level == null || level.isClientSide) return;

        ItemStack paper = new ItemStack(Items.PAPER);
        //? if < 1.21.1 {
        paper.setHoverName(Component.literal("Message"));
        ListTag lore = new ListTag();
        for (String line : rxLines) {
            lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(line != null ? line : ""))));
        }
        CompoundTag display = paper.getOrCreateTagElement("display");
        display.put("Lore", lore);
        //?} else {
        /*paper.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("Message"));
        List<Component> lore = new java.util.ArrayList<>();
        for (String line : rxLines) {
            lore.add(Component.literal(line != null ? line : ""));
        }
        paper.set(net.minecraft.core.component.DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(lore));
        *///?}

        ItemEntity item = new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, paper);
        level.addFreshEntity(item);
        com.hbm_m.platform.PlatformHooks.playSound(level, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 1.5F);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("txChannel")) txChannel = data.getString("txChannel");
        if (data.contains("rxChannel")) rxChannel = data.getString("rxChannel");
        for (int i = 0; i < LINE_COUNT; i++) {
            String key = "tx" + i;
            if (data.contains(key)) {
                String line = data.getString(key);
                txLines[i] = line.length() > LINE_WIDTH ? line.substring(0, LINE_WIDTH) : line;
            }
        }

        String cmd = data.contains("cmd") ? data.getString("cmd") : "";
        if (cmd.equals("snd")) startSending();
        else if (cmd.equals("rxprt")) print();
        else if (cmd.equals("rxcls")) clearRx();

        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        tag.putString("txChannel", txChannel);
        tag.putString("rxChannel", rxChannel);
        tag.putBoolean("isSending", isSending);
        tag.putInt("sendLine", sendLine);
        tag.putInt("sendChar", sendChar);
        tag.putInt("writingRxLine", writingRxLine);
        for (int i = 0; i < LINE_COUNT; i++) {
            tag.putString("tx" + i, txLines[i] != null ? txLines[i] : "");
            tag.putString("rx" + i, rxLines[i] != null ? rxLines[i] : "");
        }
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        txChannel = tag.getString("txChannel");
        rxChannel = tag.getString("rxChannel");
        isSending = tag.getBoolean("isSending");
        sendLine = tag.getInt("sendLine");
        sendChar = tag.getInt("sendChar");
        writingRxLine = tag.getInt("writingRxLine");
        for (int i = 0; i < LINE_COUNT; i++) {
            txLines[i] = tag.contains("tx" + i) ? tag.getString("tx" + i) : "";
            rxLines[i] = tag.contains("rx" + i) ? tag.getString("rx" + i) : "";
        }
    }
}
