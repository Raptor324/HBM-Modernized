package com.hbm_m.blockentity.network;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.radio.IRadioTorchConfigurable;
import com.hbm_m.blockentity.network.radio.RTTYNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code TileEntityRadioTelex}: Fernschreiber am RTTY-Netz. Sendet den Puffer zeichenweise (ein Zeichen je Tick,
 * Pause-Zeichen = eine Sekunde warten, Zeilenende/Uebertragungsende als Steuerzeichen) und schreibt Empfangenes in den
 * Empfangspuffer: Glocke, "nach Empfang drucken", "Bildschirm loeschen"; eine neue Uebertragung loescht den alten Text.
 * <p>Er belegt wie im Original zwei Felder nebeneinander ({@code getDimensions {0,0,0,0,1,0}}).
 * Die OpenComputers-Komponente ({@code ntm_telex}) wird nicht portiert (keine OC-Anbindung).</p>
 */
public class RadioTelexBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity implements IRadioTorchConfigurable {

    public static final int lineWidth = 33;
    public String txChannel = "";
    public String rxChannel = "";
    public String[] txBuffer = new String[] {"", "", "", "", ""};
    public String[] rxBuffer = new String[] {"", "", "", "", ""};
    public int sendingLine = 0;
    public int sendingIndex = 0;
    public boolean isSending = false;
    public int sendingWait = 0;
    public int writingLine = 0;
    public boolean printAfterRx = false;
    public boolean deleteOnReceive = true;
    public char sendingChar = ' ';
    private long lastRxStamp = -1;

    public static final char eol = '\n';
    public static final char eot = '\u0004';
    public static final char bell = '\u0007';
    public static final char print = '\u000c';
    public static final char pause = '\u0016';
    public static final char clear = '\u007f';

    public RadioTelexBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADIO_TELEX_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RadioTelexBlockEntity be) {
        if (level.isClientSide) return;
        RTTYNetwork.tickIfNeeded(level.getGameTime());
        be.update(level);
    }

    private void update(Level worldObj) {
        this.sendingChar = ' ';

        if (this.isSending && this.txChannel.isEmpty()) this.isSending = false;

        boolean changedState = false;

        if (this.isSending) {
            changedState = true;
            if (sendingWait > 0) {
                sendingWait--;
            } else {
                String line = txBuffer[sendingLine];

                if (line.length() > sendingIndex) {
                    char c = line.charAt(sendingIndex);
                    sendingIndex++;
                    if (c == pause) {
                        sendingWait = 20;
                    } else {
                        RTTYNetwork.broadcast(worldObj, this.txChannel, c);
                        this.sendingChar = c;
                    }
                } else {
                    if (sendingLine >= 4) {
                        this.isSending = false;
                        RTTYNetwork.broadcast(worldObj, this.txChannel, eot);
                        this.sendingLine = 0;
                        this.sendingIndex = 0;
                    } else {
                        RTTYNetwork.broadcast(worldObj, this.txChannel, eol);
                        this.sendingLine++;
                        this.sendingIndex = 0;
                    }
                }
            }
        }

        if (!this.rxChannel.isEmpty()) {
            RTTYNetwork.RttyChannel chan = RTTYNetwork.listen(worldObj, this.rxChannel);

            // Original: frisch (timeStamp > Weltzeit - 2); hier zusaetzlich jeder Stempel nur einmal
            if (chan != null && chan.signal instanceof Character && (chan.timeStamp > worldObj.getGameTime() - 2 && chan.timeStamp != -1)
                    && chan.timeStamp != lastRxStamp) {
                lastRxStamp = chan.timeStamp;
                char c = (Character) chan.signal;
                changedState = true;

                if (this.deleteOnReceive) {
                    this.deleteOnReceive = false;
                    for (int i = 0; i < 5; i++) this.rxBuffer[i] = "";
                    this.writingLine = 0;
                }

                if (c == eot) {
                    if (this.printAfterRx) {
                        this.printAfterRx = false;
                        this.print();
                    }
                    this.deleteOnReceive = true;
                } else if (c == eol) {
                    if (this.writingLine < 4) this.writingLine++;
                    this.setChanged();
                } else if (c == bell) {
                    worldObj.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 2F, 0.5F);
                } else if (c == print) {
                    this.printAfterRx = true;
                } else if (c == clear) {
                    for (int i = 0; i < 5; i++) this.rxBuffer[i] = "";
                    this.writingLine = 0;
                } else {
                    this.rxBuffer[this.writingLine] += c;
                    this.setChanged();
                }
            }
        }

        if (changedState) {
            worldObj.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    @Override
    public void receiveControl(CompoundTag data) {
        for (int i = 0; i < 5; i++) {
            if (data.contains("tx" + i)) this.txBuffer[i] = data.getString("tx" + i);
        }

        String cmd = data.getString("cmd");

        if ("snd".equals(cmd) && !this.isSending) {
            this.isSending = true;
            this.sendingLine = 0;
            this.sendingIndex = 0;
        }

        if ("rxprt".equals(cmd)) {
            print();
        }

        if ("rxcls".equals(cmd)) {
            for (int i = 0; i < 5; i++) this.rxBuffer[i] = "";
            this.writingLine = 0;
        }

        if ("sve".equals(cmd)) {
            this.txChannel = data.getString("txChan");
            this.rxChannel = data.getString("rxChan");
        }

        if (data.contains("txChannel")) this.txChannel = data.getString("txChannel");
        if (data.contains("rxChannel")) this.rxChannel = data.getString("rxChannel");

        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void print() {
        if (level == null || level.isClientSide) return;

        ItemStack stack = new ItemStack(Items.PAPER);
        List<String> text = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            if (!rxBuffer[i].isEmpty()) text.add(rxBuffer[i]);
        }
        //? if < 1.21.1 {
        stack.setHoverName(Component.literal("Message"));
        ListTag lore = new ListTag();
        for (String line : text) lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(line))));
        stack.getOrCreateTagElement("display").put("Lore", lore);
        //?} else {
        /*stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("Message"));
        List<Component> lore = new ArrayList<>();
        for (String line : text) lore.add(Component.literal(line));
        stack.set(net.minecraft.core.component.DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(lore));
        *///?}
        level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, stack));
        com.hbm_m.platform.LevelHooks.playSound(level, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 1.5F);
    }

    @Override
    protected void writeNbtData(@NotNull CompoundTag nbt, @Nullable HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        for (int i = 0; i < 5; i++) {
            nbt.putString("tx" + i, txBuffer[i]);
            nbt.putString("rx" + i, rxBuffer[i]);
        }
        nbt.putString("txChan", txChannel);
        nbt.putString("rxChan", rxChannel);
        nbt.putBoolean("isSending", isSending);
        nbt.putInt("sendingLine", sendingLine);
        nbt.putInt("sendingIndex", sendingIndex);
        nbt.putInt("sendingChar", sendingChar);
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag nbt, @Nullable HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        for (int i = 0; i < 5; i++) {
            txBuffer[i] = nbt.getString("tx" + i);
            rxBuffer[i] = nbt.getString("rx" + i);
        }
        this.txChannel = nbt.contains("txChan") ? nbt.getString("txChan") : nbt.getString("txChannel");
        this.rxChannel = nbt.contains("rxChan") ? nbt.getString("rxChan") : nbt.getString("rxChannel");
        this.isSending = nbt.getBoolean("isSending");
        this.sendingLine = nbt.getInt("sendingLine");
        this.sendingIndex = nbt.getInt("sendingIndex");
        this.sendingChar = (char) nbt.getInt("sendingChar");
    }
}
