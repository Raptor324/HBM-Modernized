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
 * <p><b>Der Brennstoff macht den Unterschied.</b> Wie im Original ({@code ModuleBurnTime}) haelt
 * Braunkohle, Kohle und Koks ein Viertel laenger und heizt doppelt so stark; Festbrennstoff
 * anderthalbmal so lang bei dreifacher Hitze; Raketentreibstoff dasselbe bei fuenffacher.
 * Hoellenfeuer brennt nur halb so lang - heizt dafuer <b>fuenfzehnfach</b>.</p>
 *
 * <p>Steht eine {@link MachineAshpitBlockEntity Aschegrube} direkt darunter, faellt die Asche
 * hinein - Holz, Kohle oder Sonstiges, je nach dem, was verbrannt wurde.
 */
public class MachineFireboxBlockEntity extends BaseMachineBlockEntity implements IHeatSource {

    public static final int INVENTORY_SIZE = 2;
    private static final int MAX_HEAT = 100_000;
    private static final int BURN_HEAT_PER_TICK = 100;

    private int burnTime = 0;
    private int maxBurnTime = 0;
    private int heat = 0;
    /** Hitze je Tick fuer den gerade brennenden Stoff - siehe {@link #burnMods}. */
    private int heatPerTick = BURN_HEAT_PER_TICK;

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
            be.heat = Math.min(MAX_HEAT, be.heat + be.heatPerTick);
        } else {
            be.heat = Math.max(be.heat - Math.max(be.heat / 1000, 1), 0);
        }

        be.setChanged();
    }

    /**
     * 1:1-Port von {@code ModuleBurnTime}: je Brennstoffsorte ein Zeit- und ein Hitzefaktor.
     *
     * @return {@code {Zeitfaktor, Hitzefaktor}}
     */
    private static double[] burnMods(ItemStack stack) {
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(stack.getItem()).getPath();

        if (id.contains("balefire"))  return new double[] { 0.5D, 15D };
        if (id.contains("rocket_fuel") || id.contains("solid_fuel_presto")) return new double[] { 1.5D, 5D };
        if (id.contains("solid_fuel")) return new double[] { 1.5D, 3D };
        if (id.contains("coke") || id.contains("coal") || id.contains("lignite")) {
            return new double[] { 1.25D, 2D };
        }
        return new double[] { 1D, 1D };
    }

    /** 1:1-Port von {@code getAshFromFuel}: welche Asche dieser Brennstoff hinterlaesst. */
    private static MachineAshpitBlockEntity.AshType ashFromFuel(ItemStack stack) {
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(stack.getItem()).getPath();

        if (id.contains("coke") || id.contains("coal") || id.contains("lignite")) {
            return MachineAshpitBlockEntity.AshType.COAL;
        }
        if (id.contains("log") || id.contains("wood") || id.contains("plank") || id.contains("sapling")) {
            return MachineAshpitBlockEntity.AshType.WOOD;
        }
        return MachineAshpitBlockEntity.AshType.MISC;
    }

    private void startBurning() {
        for (int slot = 0; slot < INVENTORY_SIZE; slot++) {
            ItemStack fuelStack = this.inventory.getStackInSlot(slot);
            if (fuelStack.isEmpty()) continue;

            int baseTime = AbstractFurnaceBlockEntity.getFuel().getOrDefault(fuelStack.getItem(), 0);
            if (baseTime <= 0) continue;

            double[] mods = burnMods(fuelStack);
            int burnTicks = (int) (baseTime * mods[0]);
            this.heatPerTick = (int) (BURN_HEAT_PER_TICK * mods[1]);

            this.maxBurnTime = burnTicks;
            this.burnTime = burnTicks;

            // Original: die Asche faellt in die Grube direkt darunter.
            if (level != null
                    && level.getBlockEntity(worldPosition.below()) instanceof MachineAshpitBlockEntity ashpit) {
                ashpit.addAsh(ashFromFuel(fuelStack), baseTime);
            }

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
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("burnTime", burnTime);
        tag.putInt("maxBurnTime", maxBurnTime);
        tag.putInt("heat", heat);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        burnTime = tag.getInt("burnTime");
        maxBurnTime = tag.getInt("maxBurnTime");
        heat = tag.getInt("heat");
    }
}
