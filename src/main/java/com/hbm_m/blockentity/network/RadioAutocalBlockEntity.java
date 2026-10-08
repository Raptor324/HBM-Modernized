package com.hbm_m.blockentity.network;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.radio.IRadioTorchConfigurable;
import com.hbm_m.blockentity.network.radio.RTTYNetwork;
import com.hbm_m.module.autocal.IParse;
import com.hbm_m.module.autocal.IParse.EnumStatementReturn;
import com.hbm_m.module.autocal.IParse.ParseContext;
import com.hbm_m.module.autocal.ParseMSES1Ext1;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code TileEntityRadioAUTOCAL}: der Automatische Rechner. Fuehrt ein MS-ES1.1-Skript ({@link ParseMSES1Ext1})
 * mit {@code clockSpeed} Zeilen je Tick aus, Ein/Aus, "Fehler ignorieren" und "Automatischer Neustart" als Schalter,
 * die letzten Zeilen als Verlauf. Ein- und Ausgabe ausschliesslich ueber Redstone-over-Radio.
 */
public class RadioAutocalBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity implements IRadioTorchConfigurable {

    public boolean isOn = false;
    public boolean ignoreError = false;
    public boolean autoReboot = false;

    public String[] script = new String[0];
    public IParse msesv1ext = new ParseMSES1Ext1();
    public ParseContext ctx;

    public String[] history = new String[] {"", "", "", "", "", ""};

    public RadioAutocalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADIO_AUTOCAL_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RadioAutocalBlockEntity be) {
        if (level.isClientSide) return;
        RTTYNetwork.tickIfNeeded(level.getGameTime());
        be.update(level);
    }

    private void update(Level worldObj) {

        if (worldObj.getGameTime() % 60 == 0) this.setChanged(); // ensure we're always saved to disk

        if (this.ctx == null) {
            this.ctx = new ParseContext(worldObj);
        }
        if (this.ctx.world != worldObj) this.ctx.world = worldObj;

        if (!this.isOn && this.autoReboot) {
            this.isOn = true;
        }

        if (this.isOn) {

            int emergencyBrake = 100;
            for (int i = 0; i < this.ctx.clockSpeed && emergencyBrake > 0; i++) {
                emergencyBrake--;

                if (this.ctx.current == this.script.length) { this.stop("Program has terminated"); break; }
                if (this.ctx.current < 0 || this.ctx.current >= this.script.length) { this.stop("Program index is out of bounds"); break; }

                try {
                    int index = this.ctx.current;
                    this.ctx.current++;
                    String line = this.script[index];
                    EnumStatementReturn ret = msesv1ext.eval(ctx, line);
                    if (ret != EnumStatementReturn.SKIP) pushMsg(index + ": " + line);
                    this.history[0] = "Buffer: " + ctx.readBuffer();
                    if (ret == EnumStatementReturn.END_TICK) break;
                    if (ret == EnumStatementReturn.SHUTDOWN) this.stop("Program requested shutdown");
                    if (!this.ignoreError) {
                        if (ret == EnumStatementReturn.UNRECOGNIZED_COMMAND) this.stop("Unrecognized command");
                        if (ret == EnumStatementReturn.PARAMETER_ERROR) this.stop("Parameter error");
                        if (ret == EnumStatementReturn.UNDEFINED) this.stop("Undefined behavior");
                        if (ret == EnumStatementReturn.STACK_EXCEEDED) this.stop("Stack exceeded capacity");
                    }
                    if (ret == EnumStatementReturn.SKIP) i--;
                } catch (Exception ex) {
                    this.stop("Evaluation unsuccessful");
                }
            }
        }

        // Original networkPackNT(15) - Anzeige von Schaltern und Verlauf
        if (worldObj.getGameTime() % 5 == 0) worldObj.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }

    public void pushMsg(String msg) {

        for (int i = 2; i < history.length; i++) {
            history[i - 1] = history[i];
        }

        history[history.length - 1] = msg;
    }

    public void stop(String reason) {
        this.isOn = false;
        if (this.ctx != null) this.ctx.turnOff();
        this.pushMsg(reason);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (this.ctx == null) this.ctx = new ParseContext(level);
        if (data.contains("on")) {
            if (this.isOn) stop("User requested shutdown");
            else this.isOn = true;
        }
        if (data.contains("ignore")) this.ignoreError = !this.ignoreError;
        if (data.contains("auto")) this.autoReboot = !this.autoReboot;

        if (data.contains("payload")) {
            this.ctx.jmp.clear();
            this.script = data.getString("payload").split("\n");
            for (int i = 0; i < script.length; i++) {
                script[i] = script[i].trim();
                this.msesv1ext.generateJumpPoints(ctx, script[i], i);
            }
            if (this.isOn) stop("Script has changed");
        }

        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        nbt.putBoolean("isOn", isOn);
        nbt.putBoolean("ignoreError", ignoreError);
        nbt.putBoolean("autoReboot", autoReboot);

        ListTag lineList = new ListTag();
        for (String line : this.script) {
            lineList.add(StringTag.valueOf(line));
        }
        nbt.put("script", lineList);

        if (this.ctx == null) this.ctx = new ParseContext(level);
        this.ctx.writeToNBT(nbt);

        // Original serialize(): Verlauf nur fuer den Client
        for (int i = 0; i < history.length; i++) nbt.putString("history" + i, history[i]);
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        this.isOn = nbt.getBoolean("isOn");
        this.ignoreError = nbt.getBoolean("ignoreError");
        this.autoReboot = nbt.getBoolean("autoReboot");

        ListTag lineList = nbt.getList("script", 8);
        this.script = new String[lineList.size()];
        for (int i = 0; i < script.length; i++) {
            this.script[i] = lineList.getString(i);
        }

        this.ctx = new ParseContext(null);
        this.ctx.readFromNBT(nbt, script, msesv1ext);

        for (int i = 0; i < history.length; i++) if (nbt.contains("history" + i)) history[i] = nbt.getString("history" + i);
    }
}
