package com.hbm_m.blockentity.machines.pile;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.pile.PileBlock;
import com.hbm_m.block.machines.pile.PileBlockType;
import com.hbm_m.blockentity.LoadedMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.item.machine.ItemPileRodMK2;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-Port von {@code TileEntityPileLoader} (1.7.10): die Ladevorrichtung am Eingang eines
 * Brennstoffkanals.
 *
 * <p>Sie haelt genau einen Brennstab. Ein Rechtsklick mit einem Stab legt ihn ein, ein weiterer
 * Rechtsklick - oder eine Redstoneflanke an ihrer Vorderseite - schiebt ihn in den Kanal. Der Stoss
 * dauert sieben Ticks hin, fuenf Ticks Pause, sieben zurueck; erst am Umkehrpunkt wird geladen. Der
 * neue Stab schiebt die ganze Reihe eine Stelle weiter, sodass hinten der aelteste herausfaellt.</p>
 *
 * <p>Nebenbei liest sie den hintersten Stab des Kanals samt Abbrand und die Kanaltemperatur aus.
 * Ueber Redstone-over-Radio lassen sich dieselben Werte abfragen: {@code depletion},
 * {@code deppercent}, {@code lifetime} und {@code temp} - damit laesst sich der Meiler von aussen
 * ueberwachen und der Stabwechsel selbsttaetig ausloesen.</p>
 */
public class PileLoaderBlockEntity extends LoadedMachineBlockEntity
        implements com.hbm_m.api.redstoneoverradio.IRORValueProvider {

    /** Original: {@code SPEED = 1D / 7D}, ein Hub dauert sieben Ticks. */
    public static final double SPEED = 1D / 7D;

    private double level_ = 0D;
    private boolean loading = false;
    private int delay = 0;
    private boolean wasRedstone;
    private int chanNum;

    /** Der eingelegte Stab, hoechstens einer. */
    private ItemStack stack = ItemStack.EMPTY;

    /** Nur zur Anzeige: der hinterste Stab im Kanal samt Abbrand und Kanalhitze. */
    private ItemStack channelStack = ItemStack.EMPTY;
    private double channelDepletion;
    private double channelTemp;

    public PileLoaderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PILE_LOADER_BE.get(), pos, state);
    }

    public Direction getOrientation() {
        BlockState state = getBlockState();
        return state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
    }

    public double getExtension()          { return level_; }
    public boolean isLoading()        { return loading; }
    public ItemStack getStack()       { return stack; }
    public ItemStack getChannelStack(){ return channelStack; }
    public double getChannelDepletion(){ return channelDepletion; }
    public double getChannelTemp()    { return channelTemp; }
    public int getChannelNum()        { return chanNum; }

    /** Original: {@code isItemLoadable} - nur Meilerstaebe passen hinein. */
    public static boolean isItemLoadable(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemPileRodMK2;
    }

    /** Original: der Rechtsklick auf die Ladevorrichtung, siehe {@code onBlockActivated}. */
    public boolean tryInsert(ItemStack held) {
        if (level_ > 0D || loading) return false;

        if (!held.isEmpty() && stack.isEmpty() && isItemLoadable(held)) {
            stack = held.copy();
            stack.setCount(1);
            held.shrink(1);
            if (level != null) {
                level.playSound(null, worldPosition, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 1F, 1F);
            }
            setChanged();
            return true;
        }

        loading = true;
        setChanged();
        return true;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PileLoaderBlockEntity be) {
        if (level.isClientSide()) return;

        Direction dir = be.getOrientation();
        PileChannel fuelChan = null;

        be.channelStack = ItemStack.EMPTY;
        be.channelDepletion = 0D;
        be.channelTemp = 0D;

        BlockPos channelPos = pos.relative(dir.getOpposite());
        BlockState channelState = level.getBlockState(channelPos);

        if (channelState.is(ModBlocks.PILE_BLOCK.get())
                && channelState.hasProperty(PileBlock.TYPE)
                && channelState.getValue(PileBlock.TYPE) == PileBlockType.FUEL_IN) {

            BlockEntity tile = level.getBlockEntity(channelPos);
            if (tile instanceof PileBaseBlockEntity pile) {
                PileCoreBlockEntity core = pile.getCore(level);

                if (core != null) {
                    fuelChan = core.getFuelChannel(channelPos);
                    if (fuelChan != null && fuelChan.rods.length > 0) {
                        be.chanNum = core.getFuelChannelNum(fuelChan);
                        be.channelStack = fuelChan.rods[fuelChan.rods.length - 1];
                        be.channelDepletion = ItemPileRodMK2.getDepletionPercent(be.channelStack);
                        be.channelTemp = fuelChan.heat;
                    }
                }
            }
        }

        boolean redstone = level.hasSignal(pos.relative(dir), dir);
        if (redstone && !be.wasRedstone && be.delay <= 0 && be.level_ <= 0D) be.loading = true;
        be.wasRedstone = redstone;

        if (be.delay > 0) {
            be.delay--;
        } else if (be.loading) {

            if (be.level_ == 0D) {
                level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, be.getVolume(1F), 1F);
            }

            be.level_ += SPEED;
            if (be.level_ >= 1D) {
                be.level_ = 1D;
                be.loading = false;
                be.delay = 5;
            }
        } else {

            if (be.level_ == 1D) {
                level.playSound(null, pos, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, be.getVolume(1F), 0.75F);
                if (fuelChan != null && !be.stack.isEmpty()) {
                    fuelChan.loadItem(level, be.stack);
                    be.stack = ItemStack.EMPTY;
                }
            }

            if (be.level_ > 0D) {
                be.level_ -= SPEED;
                if (be.level_ < 0D) be.level_ = 0D;
            }
        }

        be.setChanged();
        be.sendUpdateToClient();
    }

    // ── Redstone-over-Radio ──

    /**
     * 1:1 aus {@code TileEntityPileLoader.getFunctionInfo}. Das urspruengliche {@code meta} ist
     * hier weggefallen: es gab die Metadatenzahl des Stabs zurueck, und die gibt es in 1.20 nicht
     * mehr - dieselbe Auskunft liefert {@code lifetime}.
     */
    @Override
    public String[] getFunctionInfo() {
        return new String[] {
                PREFIX_VALUE + "depletion",
                PREFIX_VALUE + "deppercent",
                PREFIX_VALUE + "lifetime",
                PREFIX_VALUE + "temp"
        };
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "deppercent").equals(name)) {
            return "" + (int) Math.round(channelDepletion);
        }
        if ((PREFIX_VALUE + "depletion").equals(name)) {
            if (channelStack.isEmpty()) return "0";
            return "" + (int) Math.round(ItemPileRodMK2.getDepletionPercent(channelStack));
        }
        if ((PREFIX_VALUE + "lifetime").equals(name)) {
            ItemPileRodMK2.EnumPileRod rod = ItemPileRodMK2.rodOf(channelStack);
            return rod == null ? "0" : "" + (int) Math.round(rod.life);
        }
        if ((PREFIX_VALUE + "temp").equals(name)) {
            return "" + (int) Math.round(channelTemp);
        }
        return null;
    }

    /** Original: beim Abbau faellt der eingelegte Stab heraus. */
    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide() && !stack.isEmpty()) {
            net.minecraft.world.Containers.dropItemStack(level,
                    worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, stack);
            stack = ItemStack.EMPTY;
        }
        super.setRemoved();
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putBoolean("loading", loading);
        tag.putDouble("level", level_);
        tag.putInt("delay", delay);
        tag.putBoolean("wasRedstone", wasRedstone);
        tag.putInt("chanNum", chanNum);

        if (!stack.isEmpty()) {
            tag.put("stack", stack.save(new CompoundTag()));
        }
        if (!channelStack.isEmpty()) {
            tag.put("chanStack", channelStack.save(new CompoundTag()));
        }
        tag.putDouble("chanDepletion", channelDepletion);
        tag.putDouble("chanTemp", channelTemp);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        loading = tag.getBoolean("loading");
        level_ = tag.getDouble("level");
        delay = tag.getInt("delay");
        wasRedstone = tag.getBoolean("wasRedstone");
        chanNum = tag.getInt("chanNum");

        stack = tag.contains("stack") ? ItemStack.of(tag.getCompound("stack")) : ItemStack.EMPTY;
        channelStack = tag.contains("chanStack") ? ItemStack.of(tag.getCompound("chanStack")) : ItemStack.EMPTY;
        channelDepletion = tag.getDouble("chanDepletion");
        channelTemp = tag.getDouble("chanTemp");
    }
}
