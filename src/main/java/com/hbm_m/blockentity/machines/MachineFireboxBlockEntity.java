package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IHeatSource;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of {@code TileEntityHeaterFirebox} (1.7.10 Original) - solid-fuel heat generator, 2 fuel
 * input slots.
 * <p>
 * SCOPE-Vereinfachung: Das Original benutzt eine eigene {@code ModuleBurnTime}-Tabelle mit
 * Brennzeit-/Waerme-Multiplikatoren je Brennstoffart und wirft Asche in einen darunterliegenden
 * Ashpit. Hier: Brenndauer per vanilla {@link AbstractFurnaceBlockEntity#getFuel()} (wie bereits
 * beim {@code MachineWoodBurnerBlockEntity} dieses Ports), fester Waerme-Ertrag pro Tick waehrend
 * des Brennens - kein Ashpit-Ausstoss.
 */
public class MachineFireboxBlockEntity extends BaseMachineBlockEntity implements IHeatSource {

    public static final int INVENTORY_SIZE = 2;
    private static final int MAX_HEAT = 100_000;
    private static final int BURN_HEAT_PER_TICK = 100;

    private int burnTime = 0;
    private int maxBurnTime = 0;
    private int heat = 0;

    public MachineFireboxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FIREBOX_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineFireboxBlockEntity be) {
        if (level.isClientSide) return;

        if (be.burnTime <= 0) {
            be.startBurning();
        }

        if (be.burnTime > 0) {
            be.burnTime--;
            be.heat = Math.min(MAX_HEAT, be.heat + BURN_HEAT_PER_TICK);
        } else {
            be.heat = Math.max(be.heat - Math.max(be.heat / 1000, 1), 0);
        }

        be.setChanged();
    }

    private void startBurning() {
        for (int slot = 0; slot < INVENTORY_SIZE; slot++) {
            ItemStack fuelStack = this.inventory.getStackInSlot(slot);
            if (fuelStack.isEmpty()) continue;

            int burnTicks = AbstractFurnaceBlockEntity.getFuel().getOrDefault(fuelStack.getItem(), 0);
            if (burnTicks <= 0) continue;

            this.maxBurnTime = burnTicks;
            this.burnTime = burnTicks;

            if (fuelStack.getItem() == Items.LAVA_BUCKET) {
                this.inventory.setStackInSlot(slot, new ItemStack(Items.BUCKET));
            } else {
                fuelStack.shrink(1);
            }
            return;
        }
    }

    public boolean isBurning() {
        return burnTime > 0;
    }

    public int getBurnTime() { return burnTime; }
    public int getMaxBurnTime() { return maxBurnTime; }

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
        return AbstractFurnaceBlockEntity.getFuel().getOrDefault(stack.getItem(), 0) > 0;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.firebox");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return com.hbm_m.inventory.menu.MachineFireboxMenu.create(id, inventory, this);
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("burnTime", burnTime);
        tag.putInt("maxBurnTime", maxBurnTime);
        tag.putInt("heat", heat);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        burnTime = tag.getInt("burnTime");
        maxBurnTime = tag.getInt("maxBurnTime");
        heat = tag.getInt("heat");
    }
}
