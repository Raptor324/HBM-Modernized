package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Coolable;
import com.hbm_m.inventory.fluid.trait.FT_Coolable.CoolingType;
import com.hbm_m.interfaces.IHeatSource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of {@code TileEntityHeaterHeatex} (1.7.10 Original) - converts a hot fluid into its cooled
 * form (via the already-ported {@link FT_Coolable}/{@code CoolingType.HEATEXCHANGER} fluid-trait
 * data, e.g. hot reactor coolant &rarr; coolant) and, as a byproduct, generates heat exposed via
 * {@link IHeatSource}.
 * <p>
 * SCOPE-Vereinfachung: Das Original ist ein {@code BlockDummyable}-Multiblock mit 4 diagonalen
 * Rohranschluss-Zellen und einem Item-Slot zur Neuzuweisung des heissen Fluid-Typs. Hier:
 * einzelnes Block, Tanks fest auf {@code coolant_hot}/{@code coolant} typisiert, Anschluss ueber
 * die MK2-Rohrnetzwerk-API (wie beim Basic Boiler) statt eigener diagonaler Proxy-Zellen.
 */
public class MachineHeatexBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2, IHeatSource {

    private static final int TANK_CAPACITY = 24_000;
    private static final int MAX_HEAT = 100_000;
    private static final int MAX_OPS_PER_TICK = 100;

    private final FluidTank hotTank = new FluidTank(ModFluids.COOLANT_HOT.getSource(), TANK_CAPACITY);
    private final FluidTank coldTank = new FluidTank(ModFluids.COOLANT.getSource(), TANK_CAPACITY);

    private int heat = 0;

    public MachineHeatexBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEATEX_BE.get(), pos, state, 0, 0L, 0L, 0L);
    }

    //? if forge {
    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
            return hotTank.getCapability().cast();
        }
        return super.getCapability(cap, side);
    }
    //?}

    @Override
    public FluidTank[] getReceivingTanks() {
        return new FluidTank[] { hotTank };
    }

    @Override
    public FluidTank[] getSendingTanks() {
        return new FluidTank[] { coldTank };
    }

    @Override
    public FluidTank[] getAllTanks() {
        return new FluidTank[] { hotTank, coldTank };
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    public FluidTank getHotTank() { return hotTank; }
    public FluidTank getColdTank() { return coldTank; }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineHeatexBlockEntity be) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;
        be.serverTick(serverLevel, pos);
    }

    private void serverTick(ServerLevel level, BlockPos pos) {
        if (level.getGameTime() % 20 == 0) {
            for (Direction dir : Direction.values()) {
                trySubscribe(hotTank.getTankType(), level, pos.relative(dir), dir);
                tryProvide(coldTank, level, pos.relative(dir), dir);
            }
        }

        tryConvert();

        setChanged();
    }

    private void tryConvert() {
        FT_Coolable trait = FluidType.getTrait(hotTank.getStoredFluid(), FT_Coolable.class);
        double eff = trait != null ? trait.getEfficiency(CoolingType.HEATEXCHANGER) : 0.0D;

        if (trait == null || eff <= 0.0D || trait.amountReq <= 0) {
            heat = Math.max(heat - Math.max(heat / 1000, 1), 0);
            return;
        }

        int inputOps = hotTank.getFluidAmountMb() / trait.amountReq;
        int outputOps = trait.amountProduced > 0 ? coldTank.getSpaceMb() / trait.amountProduced : 0;
        int ops = Math.min(Math.min(inputOps, outputOps), MAX_OPS_PER_TICK);

        if (ops <= 0) {
            heat = Math.max(heat - Math.max(heat / 1000, 1), 0);
            return;
        }

        hotTank.drainMb(trait.amountReq * ops);
        coldTank.fillMb(trait.coolsTo, trait.amountProduced * ops);
        heat = Math.min(MAX_HEAT, heat + (int) (trait.heatEnergy * ops * eff));
    }

    @Override
    public int getHeatStored() {
        return heat;
    }

    @Override
    public int getMaxHeatStored() {
        return MAX_HEAT;
    }

    @Override
    public void useUpHeat(int amount) {
        heat = Math.max(0, heat - amount);
        setChanged();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
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
        return null;
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("heat", heat);
        tag.put("hotTank", hotTank.writeNBT(new CompoundTag()));
        tag.put("coldTank", coldTank.writeNBT(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        heat = tag.getInt("heat");
        if (tag.contains("hotTank")) hotTank.readNBT(tag.getCompound("hotTank"));
        if (tag.contains("coldTank")) coldTank.readNBT(tag.getCompound("coldTank"));
    }
}
