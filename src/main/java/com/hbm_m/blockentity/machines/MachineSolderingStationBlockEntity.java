package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineSolderingStationMenu;
import com.hbm_m.inventory.recipes.SolderingRecipes;
import com.hbm_m.inventory.recipes.SolderingRecipes.SolderingRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class MachineSolderingStationBlockEntity extends BaseMachineBlockEntity {

    // ─── Slot map (11 total) ──────────────────────────────────────────────────
    public static final int SLOTS      = 11;
    public static final int SLOT_TOP0  = 0, SLOT_TOP1 = 1, SLOT_TOP2 = 2; // toppings
    public static final int SLOT_PCB0  = 3, SLOT_PCB1 = 4;                // PCB (2 slots)
    public static final int SLOT_SOLDER = 5;                               // solder (1 slot)
    public static final int SLOT_OUT   = 6;  // output
    public static final int SLOT_BAT   = 7;  // battery
    public static final int SLOT_FLUID = 8;  // fluid-ID item
    public static final int SLOT_UPG1  = 9, SLOT_UPG2 = 10; // upgrades

    // ─── Energy constants ─────────────────────────────────────────────────────
    public static final long MAX_ENERGY  = 100_000L;
    public static final long CONSUMPTION =   2_000L;

    // ─── Processing state ─────────────────────────────────────────────────────
    public int  progress    = 0;
    public int  processTime = 200;
    public long consumption = CONSUMPTION;

    // ─── Fluid tank (solder flux / coolant) ──────────────────────────────────
    /** Welding fluid (8,000 mb as in the original). */
    public final FluidTank tank = new FluidTank(8_000);

    // ─── Feature flags ────────────────────────────────────────────────────────
    /**
     * When true, prevents processing no-fluid recipes while a fluid is present
     * in the tank (avoids accidental dry-welding when flux is loaded).
     */
    public boolean collisionPrevention = false;

    // ─── Constructor ──────────────────────────────────────────────────────────

    public MachineSolderingStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLDERING_STATION_BE.get(), pos, state, SLOTS, MAX_ENERGY, CONSUMPTION);
    }

    /** Exposes the machine inventory for SlotItemHandler in the menu. */
    public com.hbm_m.platform.ModItemStackHandler getItemHandler() { return inventory; }

    // ─── Collision Prevention toggle ──────────────────────────────────────────

    /** Called server-side by the GUI toggle packet. */
    public void toggleCollisionPrevention() {
        collisionPrevention = !collisionPrevention;
        setChanged();
        if (level != null)
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }

    // ─── Tick ─────────────────────────────────────────────────────────────────

    public static void tick(Level level, BlockPos pos, BlockState state, MachineSolderingStationBlockEntity be) {
        if (level.isClientSide) return;

        // Charge from battery slot
        com.hbm_m.api.energy.ItemEnergyAccess.getHbmProvider(
                be.inventory.getStackInSlot(SLOT_BAT)).ifPresent(provider -> {
            long want = be.capacity - be.energy;
            long got  = provider.extractEnergy(want, false);
            if (got > 0) be.energy += got;
        });

        var recipe = SolderingRecipes.getRecipe(
                be.inventory.getStackInSlot(SLOT_TOP0), be.inventory.getStackInSlot(SLOT_TOP1), be.inventory.getStackInSlot(SLOT_TOP2),
                be.inventory.getStackInSlot(SLOT_PCB0), be.inventory.getStackInSlot(SLOT_PCB1),
                be.inventory.getStackInSlot(SLOT_SOLDER));
        if (recipe != null) {
            be.processTime  = recipe.duration;
            be.consumption  = recipe.consumption;
        }
        boolean hasRecipe  = recipe != null && (recipe.fluid == null || recipe.fluid.satisfiedBy(be.tank));
        boolean canProcess = hasRecipe && be.canProcess();

        if (canProcess) {
            be.progress++;
            be.energy = Math.max(0, be.energy - be.consumption);
            if (be.progress >= be.processTime) {
                be.progress = 0;
                be.consumeItems(recipe);
                ItemStack out = be.inventory.getStackInSlot(SLOT_OUT);
                if (out.isEmpty()) {
                    be.inventory.setStackInSlot(SLOT_OUT, recipe.output.copy());
                } else {
                    out.grow(recipe.output.getCount());
                }
                if (recipe.fluid != null) recipe.fluid.consume(be.tank);
                be.setChanged();
            }
        } else {
            be.progress = 0;
        }

        level.sendBlockUpdated(pos, state, state, 3);
    }

    private void consumeItems(SolderingRecipe recipe) {
        consumeGroup(recipe.toppings, SLOT_TOP0, SLOT_TOP2 + 1);
        consumeGroup(recipe.pcb,      SLOT_PCB0, SLOT_PCB1 + 1);
        consumeGroup(recipe.solder,   SLOT_SOLDER, SLOT_SOLDER + 1);
    }

    private void consumeGroup(SolderingRecipes.SolderingIngredient[] required, int from, int to) {
        for (SolderingRecipes.SolderingIngredient ing : required) {
            for (int i = from; i < to; i++) {
                ItemStack s = inventory.getStackInSlot(i);
                if (ing.matches(s)) { s.shrink(ing.count()); break; }
            }
        }
    }

    public boolean canProcess() {
        if (energy < consumption) return false;
        // Collision prevention: don't process a fluid-less recipe if tank has fluid
        // (actual recipe fluid check will be added when SolderingRecipes is ported)
        if (collisionPrevention && tank.getFill() > 0) return false;
        // Output slot must be empty or stackable
        ItemStack out = inventory.getStackInSlot(SLOT_OUT);
        return out.isEmpty();
    }

    // ─── Progress helpers (for GUI) ───────────────────────────────────────────

    public int getProgressScaled(int scale) {
        return processTime > 0 ? progress * scale / processTime : 0;
    }

    public int getProgress()    { return progress;    }
    public int getMaxProgress() { return processTime; }

    // ─── Slot validation ──────────────────────────────────────────────────────

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_OUT) return false;
        if (slot < 3) {
            if (SolderingRecipes.toppings.isEmpty()) return true;
            return SolderingRecipes.toppings.stream().anyMatch(i -> i.test(stack));
        }
        if (slot < SLOT_SOLDER) {
            if (SolderingRecipes.pcb.isEmpty()) return true;
            return SolderingRecipes.pcb.stream().anyMatch(i -> i.test(stack));
        }
        if (slot == SLOT_SOLDER) {
            if (SolderingRecipes.solder.isEmpty()) return true;
            return SolderingRecipes.solder.stream().anyMatch(i -> i.test(stack));
        }
        return true;
    }

    // ─── MenuProvider ─────────────────────────────────────────────────────────

    @Override
    protected Component getDefaultName() { return Component.translatable("block.hbm_m.soldering_station"); }
    @Override
    public Component getDisplayName()    { return getDefaultName(); }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return MachineSolderingStationMenu.create(id, inv, this);
    }

    // ─── NBT ──────────────────────────────────────────────────────────────────

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("progress",    progress);
        tag.putInt("processTime", processTime);
        tag.putBoolean("collision", collisionPrevention);
        tank.writeToNBT(tag, "tank");
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress            = tag.getInt("progress");
        processTime         = tag.getInt("processTime");
        if (processTime <= 0) processTime = 200;
        collisionPrevention = tag.getBoolean("collision");
        tank.readFromNBT(tag, "tank");
    }
}
