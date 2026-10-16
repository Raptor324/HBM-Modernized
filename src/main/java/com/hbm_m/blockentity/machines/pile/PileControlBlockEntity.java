package com.hbm_m.blockentity.machines.pile;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.pile.PileBlock;
import com.hbm_m.block.machines.pile.PileBlockType;
import com.hbm_m.blockentity.LoadedMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-Port von {@code TileEntityPileControl} (1.7.10): der Steuerstabantrieb, der oben auf einem
 * Steuerkanal sitzt.
 *
 * <p>Er faehrt den Stab zwischen ganz eingefahren (0) und ganz gezogen (1) und braucht dafuer drei
 * Sekunden - eine Sechzigstel Bewegung je Tick. Ein Redstonesignal an seiner Rueckseite zieht den
 * Stab, das Wegfallen des Signals faehrt ihn wieder ein. Die erreichte Stellung gibt er jeden Tick
 * an den Steuerkanal weiter, aus dem der Kern seine Daempfung errechnet.</p>
 *
 * <p>Ueber Redstone-over-Radio laesst er sich auch fernsteuern: {@code setrods} setzt die
 * Zielstellung auf einen Prozentwert, {@code extendrods} verschiebt sie um einen - auch
 * negativen - Betrag.</p>
 */
public class PileControlBlockEntity extends LoadedMachineBlockEntity
        implements com.hbm_m.api.redstoneoverradio.IRORInteractive {

    /** Original: {@code SPEED = 1D / 60D}, ein voller Hub dauert drei Sekunden. */
    public static final double SPEED = 1D / 60D;

    private double level_ = 0D;
    private double targetLevel = 0D;
    private boolean wasRedstone;
    private int chanNum;

    public PileControlBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PILE_CONTROL_BE.get(), pos, state);
    }

    /** Original: {@code getOrientation} - die Seite, an der das Geraet klebt. */
    public Direction getOrientation() {
        BlockState state = getBlockState();
        return state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
    }

    public double getExtension()    { return level_; }
    public int getChannelNum()  { return chanNum; }

    public void setTarget(double target) {
        this.targetLevel = Math.max(0D, Math.min(1D, target));
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PileControlBlockEntity be) {
        if (level.isClientSide()) return;

        boolean canMove = false;
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);

        if (belowState.is(ModBlocks.PILE_BLOCK.get())
                && belowState.hasProperty(PileBlock.TYPE)
                && belowState.getValue(PileBlock.TYPE) == PileBlockType.CONTROL) {

            BlockEntity tile = level.getBlockEntity(below);
            if (tile instanceof PileBaseBlockEntity pile) {
                PileCoreBlockEntity core = pile.getCore(level);

                if (core != null) {
                    PileChannel controlChan = core.getControlChannel(below);
                    if (controlChan != null) {
                        canMove = true;
                        be.chanNum = core.getControlChannelNum(controlChan);
                        controlChan.control = be.level_;
                    }
                }
            }
        }

        if (canMove && be.level_ != be.targetLevel) {
            if (Math.abs(be.level_ - be.targetLevel) <= SPEED) {
                be.level_ = be.targetLevel;
            } else if (be.level_ < be.targetLevel) {
                be.level_ += SPEED;
            } else {
                be.level_ -= SPEED;
            }
        }

        // Original: das Signal wird an der Rueckseite des Geraets abgegriffen.
        Direction dir = be.getOrientation();
        boolean redstone = level.hasSignal(pos.relative(dir), dir);

        if (redstone && !be.wasRedstone) be.setTarget(1D);
        if (!redstone && be.wasRedstone) be.setTarget(0D);
        be.wasRedstone = redstone;

        be.setChanged();
        be.sendUpdateToClient();
    }

    // ── Redstone-over-Radio ──

    /** 1:1 aus {@code TileEntityPileControl.getFunctionInfo}. */
    @Override
    public String[] getFunctionInfo() {
        return new String[] {
                PREFIX_FUNCTION + "setrods" + NAME_SEPARATOR + "percent",
                PREFIX_FUNCTION + "extendrods" + NAME_SEPARATOR + "percent"
        };
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setrods").equals(name) && params.length > 0) {
            int percent = com.hbm_m.api.redstoneoverradio.IRORInteractive.parseInt(params[0], 0, 100);
            setTarget(percent / 100D);
            return null;
        }

        if ((PREFIX_FUNCTION + "extendrods").equals(name) && params.length > 0) {
            int percent = com.hbm_m.api.redstoneoverradio.IRORInteractive.parseInt(params[0], -100, 100);
            setTarget(Math.max(0D, Math.min(1D, targetLevel + percent / 100D)));
            return null;
        }

        return null;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putDouble("level", level_);
        tag.putDouble("targetLevel", targetLevel);
        tag.putBoolean("wasRedstone", wasRedstone);
        tag.putInt("chanNum", chanNum);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        level_ = tag.getDouble("level");
        targetLevel = tag.getDouble("targetLevel");
        wasRedstone = tag.getBoolean("wasRedstone");
        chanNum = tag.getInt("chanNum");
    }
}
