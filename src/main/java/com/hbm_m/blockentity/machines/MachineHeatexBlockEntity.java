package com.hbm_m.blockentity.machines;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Coolable;
import com.hbm_m.inventory.fluid.trait.FT_Coolable.CoolingType;
import com.hbm_m.inventory.menu.MachineHeatexMenu;
import com.hbm_m.interfaces.IHeatSource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * 1:1 {@code TileEntityHeaterHeatex}: Waermetauscher. Heisses Fluid (Typ per Fluid-ID im Platz 0) wird nach
 * {@code FT_Coolable}/HEATEXCHANGER abgekuehlt; je Zyklus hoechstens {@code amountToCool} Vorgaenge, ein Zyklus alle
 * {@code tickDelay} Ticks (beides in der GUI einstellbar). Die Waerme verfaellt je Tick um 0,1 %.
 * Rohranschluesse vor und hinter den vier Ecken ({@code getConPos}), nur in Blickrichtung und entgegen.
 */
public class MachineHeatexBlockEntity extends BaseMachineBlockEntity
        implements IFluidStandardTransceiverMK2, IHeatSource, com.hbm_m.api.tile.IControlReceiver,
        com.hbm_m.api.redstoneoverradio.IRORValueProvider {

    public final FluidTank[] tanks = new FluidTank[2];
    public int amountToCool = 24_000;
    public int tickDelay = 1;
    public int heatEnergy;

    public MachineHeatexBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEATEX_BE.get(), pos, state, 1, 0L, 0L, 0L);
        this.tanks[0] = new FluidTank(ModFluids.COOLANT_HOT.getSource(), 24_000);
        this.tanks[1] = new FluidTank(ModFluids.COOLANT.getSource(), 24_000);
    }

    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tanks[0] }; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { tanks[1] }; }
    @Override public FluidTank[] getAllTanks() { return tanks; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    public FluidTank getHotTank() { return tanks[0]; }
    public FluidTank getColdTank() { return tanks[1]; }

    private Direction getDir() {
        BlockState s = getBlockState();
        return s.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? s.getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineHeatexBlockEntity be) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;
        be.serverTick(serverLevel, pos);
    }

    private void serverTick(ServerLevel level, BlockPos pos) {

        ItemStack[] slots = { inventory.getStackInSlot(0) };
        if (this.tanks[0].setType(0, slots)) inventory.setStackInSlot(0, slots[0] == null ? ItemStack.EMPTY : slots[0]);
        this.setupTanks();
        this.updateConnections(level);

        this.heatEnergy *= 0.999;

        this.tryConvert(level);

        for (BlockPos[] con : getConPos()) {
            Direction d = con[1].equals(BlockPos.ZERO) ? getDir() : getDir().getOpposite();
            if (this.tanks[1].getFill() > 0) this.tryProvide(tanks[1], level, con[0], d);
        }

        setChanged();
        sendUpdateToClient();
    }

    protected void setupTanks() {
        FT_Coolable trait = FluidType.getTrait(tanks[0].getTankType(), FT_Coolable.class);
        if (trait != null && trait.getEfficiency(CoolingType.HEATEXCHANGER) > 0) {
            tanks[1].setTankType(trait.coolsTo);
            return;
        }

        tanks[0].setTankType(ModFluids.NONE.getSource());
        tanks[1].setTankType(ModFluids.NONE.getSource());
    }

    protected void updateConnections(Level level) {
        for (BlockPos[] con : getConPos()) {
            Direction d = con[1].equals(BlockPos.ZERO) ? getDir() : getDir().getOpposite();
            this.trySubscribe(tanks[0].getTankType(), level, con[0], d);
        }
    }

    protected void tryConvert(Level level) {
        FT_Coolable trait = FluidType.getTrait(tanks[0].getTankType(), FT_Coolable.class);
        if (trait == null) return;
        if (tickDelay < 1) tickDelay = 1;
        if (level.getGameTime() % tickDelay != 0) return;

        int inputOps = tanks[0].getFill() / trait.amountReq;
        int outputOps = (tanks[1].getMaxFill() - tanks[1].getFill()) / trait.amountProduced;
        int opCap = this.amountToCool;

        int ops = Math.min(inputOps, Math.min(outputOps, opCap));
        tanks[0].drainMb(trait.amountReq * ops);
        tanks[1].fillMb(tanks[1].getTankType(), trait.amountProduced * ops);
        this.heatEnergy += trait.heatEnergy * ops * trait.getEfficiency(CoolingType.HEATEXCHANGER);
    }

    /** Original getConPos: {Position, Marker} - Marker ZERO = Richtung dir, sonst dir.getOpposite(). */
    private BlockPos[][] getConPos() {
        Direction dir = getDir();
        Direction rot = dir.getClockWise();
        BlockPos p = worldPosition;
        BlockPos back = new BlockPos(0, 1, 0);
        return new BlockPos[][] {
                { p.relative(dir, 2).relative(rot, 1), BlockPos.ZERO },
                { p.relative(dir, 2).relative(rot, -1), BlockPos.ZERO },
                { p.relative(dir, -2).relative(rot, 1), back },
                { p.relative(dir, -2).relative(rot, -1), back },
        };
    }

    @Override
    public boolean canConnect(net.minecraft.world.level.material.Fluid fluid, Direction dir) {
        Direction facing = getDir();
        return dir == facing || dir == facing.getOpposite();
    }

    @Override public int getHeatStored() { return heatEnergy; }
    @Override public int getMaxHeatStored() { return Integer.MAX_VALUE; }

    @Override
    public void useUpHeat(int amount) {
        heatEnergy = Math.max(0, heatEnergy - amount);
        setChanged();
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) < 16 * 16;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("toCool")) this.amountToCool = Mth.clamp(data.getInt("toCool"), 1, tanks[0].getMaxFill());
        if (data.contains("delay")) this.tickDelay = Math.max(data.getInt("delay"), 1);
        setChanged();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.heatex");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineHeatexMenu.create(id, inventory, this);
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tanks[0].writeToNBT(tag, "0");
        tanks[1].writeToNBT(tag, "1");
        tag.putInt("heatEnergy", heatEnergy);
        tag.putInt("toCool", amountToCool);
        tag.putInt("delay", tickDelay);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        tanks[0].readFromNBT(tag, "0");
        tanks[1].readFromNBT(tag, "1");
        heatEnergy = tag.getInt("heatEnergy");
        amountToCool = tag.contains("toCool") ? tag.getInt("toCool") : 24_000;
        tickDelay = tag.contains("delay") ? tag.getInt("delay") : 1;
    }

    // ── Redstone-over-Radio (1:1 TileEntityHeaterHeatex) ──

    @Override
    public String[] getFunctionInfo() {
        return new String[] {
                PREFIX_VALUE + "hotfluid",
                PREFIX_VALUE + "coldfluid",
                PREFIX_VALUE + "heat"
        };
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "hotfluid").equals(name))  return "" + tanks[0].getFill();
        if ((PREFIX_VALUE + "coldfluid").equals(name)) return "" + tanks[1].getFill();
        if ((PREFIX_VALUE + "heat").equals(name))      return "" + heatEnergy;
        return null;
    }
}
