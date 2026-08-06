package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Flammable;
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
 * Port of {@code TileEntityHeaterOilburner} (1.7.10 Original) - fluid-fuel heat generator with a
 * redstone-controllable on/off switch and a screwdriver-adjustable 1..10 burn rate. Also serves as
 * {@code oilburner_hp} ("High Pressure"): the original never actually implemented this variant in
 * code (only orphaned art assets existed) - here it reuses this same class with a larger tank and
 * doubled burn rate, chosen by block identity (same convention as {@code MachineStirlingBlockEntity}'s
 * normal/steel/creative split).
 * <p>
 * SCOPE-Vereinfachung: Das Original ist ein {@code BlockDummyable}-Multiblock mit 3 Item-Slots
 * (Fluessig-Container rein/raus + Fluid-ID-Neuzuweisung). Hier: einzelnes Block, kein Inventar -
 * Befuellen/Entleeren laeuft direkt ueber die Forge-Fluid-Capability (Eimer-Rechtsklick, Rohr-
 * Anschluss), der Tank ist fest auf Heizoel typisiert statt per Item umschaltbar.
 */
public class MachineOilburnerBlockEntity extends BaseMachineBlockEntity implements IFluidStandardReceiverMK2, IHeatSource {

    public static final int MAX_SETTING = 10;
    private static final int MAX_HEAT = 100_000;
    private static final int TANK_CAPACITY_NORMAL = 16_000;
    private static final int TANK_CAPACITY_HP = 32_000;

    private final FluidTank oilTank;
    private final int burnMultiplier;

    private int setting = 1;
    private int heat = 0;

    public MachineOilburnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OILBURNER_BE.get(), pos, state, 0, 0L, 0L, 0L);

        boolean isHp = state.is(ModBlocks.OILBURNER_HP.get());
        this.oilTank = new FluidTank(ModFluids.HEATINGOIL.getSource(), isHp ? TANK_CAPACITY_HP : TANK_CAPACITY_NORMAL);
        this.burnMultiplier = isHp ? 2 : 1;
    }

    //? if forge {
    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
            return oilTank.getCapability().cast();
        }
        return super.getCapability(cap, side);
    }
    //?}

    @Override
    public FluidTank[] getReceivingTanks() {
        return new FluidTank[] { oilTank };
    }

    @Override
    public FluidTank[] getAllTanks() {
        return new FluidTank[] { oilTank };
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    public FluidTank getOilTank() {
        return oilTank;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineOilburnerBlockEntity be) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;
        be.serverTick(serverLevel, pos);
    }

    private void serverTick(ServerLevel level, BlockPos pos) {
        if (level.getGameTime() % 20 == 0) {
            for (Direction dir : Direction.values()) {
                trySubscribe(oilTank.getTankType(), level, pos.relative(dir), dir);
            }
        }

        boolean powered = level.hasNeighborSignal(pos);
        FT_Flammable trait = FluidType.getTrait(oilTank.getStoredFluid(), FT_Flammable.class);

        if (powered && trait != null && heat < MAX_HEAT) {
            int burned = Math.min(setting * burnMultiplier, oilTank.getFluidAmountMb());
            if (burned > 0) {
                oilTank.drainMb(burned);
                heat = Math.min(MAX_HEAT, heat + (int) (trait.getHeatEnergy() / 1000L) * burned);
            } else {
                decayHeat();
            }
        } else {
            decayHeat();
        }

        setChanged();
    }

    private void decayHeat() {
        heat = Math.max(heat - Math.max(heat / 1000, 1), 0);
    }

    public void cycleSetting() {
        setting = setting % MAX_SETTING + 1;
        setChanged();
    }

    public int getSetting() {
        return setting;
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
        return Component.translatable("container.hbm_m.oilburner");
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
        tag.putInt("setting", setting);
        tag.putInt("heat", heat);
        tag.put("oilTank", oilTank.writeNBT(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        setting = tag.contains("setting") ? tag.getInt("setting") : 1;
        heat = tag.getInt("heat");
        if (tag.contains("oilTank")) oilTank.readNBT(tag.getCompound("oilTank"));
    }
}
