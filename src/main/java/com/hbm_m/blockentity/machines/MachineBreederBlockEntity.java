package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineBreederMenu;
import com.hbm_m.recipe.BreederRecipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Breeder - true multiblock port of the original 1.7.10 {@code MachineReactorBreeding}/
 * {@code TileEntityMachineReactorBreeding}: a simple two-slot (input/output) item transmutation
 * machine (see {@link BreederRecipes} for the exact material substitutions, since the original's
 * {@code ItemBreedingRod} meta-item system does not exist in this port).
 * <p>
 * SCOPE-Vereinfachung: the original drew "neutron flux" from an adjacent
 * {@code TileEntityReactorResearch} (Research Reactor), which was never ported to this codebase.
 * Power here instead comes purely from the wired HBM/FE energy network (no battery item slot,
 * matching {@code gui_breeder.png}'s simple 2-slot layout, which has no art for one).
 * <p>
 * Earlier revisions of this port had grown a battery slot, a fluid tank (driven by
 * {@code FluidBreederRecipes}), and upgrade slots bolted on - none of which exist in the original
 * (its own source comments admit fluid irradiation was never actually wired to this machine, only
 * to the separate Fusion Breeder) and none of which the GUI texture has art for. Removed to match
 * the original 1:1.
 */
public class MachineBreederBlockEntity extends BaseMachineBlockEntity {

    private static final int SLOT_INPUT = 0;
    private static final int SLOT_OUTPUT = 1;

    private static final int SLOT_COUNT = 2;
    private static final long MAX_POWER = 1_000_000;
    private static final long MAX_RECEIVE = 1_000;
    /** GIT original: progress += 0.0025F per tick at the minimum required flux -> 1.0F / 0.0025F = 400 ticks. */
    private static final int DEFAULT_DURATION = 400;

    private int progress = 0;
    private final int duration = DEFAULT_DURATION;
    private boolean isOn = false;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> getDuration();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return 2;
        }
    };

    public MachineBreederBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BREEDER_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, MAX_RECEIVE);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineBreederBlockEntity entity) {
        if (level.isClientSide) {
            return;
        }

        entity.ensureNetworkInitialized();

        entity.isOn = false;
        if (entity.canProcess()) {
            entity.progress++;
            entity.setEnergyStored(entity.getEnergyStored() - entity.getPowerRequired());
            entity.isOn = true;

            if (entity.progress >= entity.getDuration()) {
                entity.progress = 0;
                entity.processItem();
            }
            entity.setChanged();
            entity.sendUpdateToClient();
        } else {
            if (entity.progress != 0) {
                entity.progress = 0;
                entity.setChanged();
            }
        }
    }

    private boolean canProcess() {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        if (input.isEmpty()) return false;

        BreederRecipes.BreederRecipe recipe = BreederRecipes.getOutput(input);
        if (recipe == null) return false;

        if (getEnergyStored() < recipe.energyPerTick) return false;

        ItemStack output = inventory.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) return true;

        if (!ItemStack.isSameItemSameTags(output, recipe.output)) return false;
        return output.getCount() < output.getMaxStackSize();
    }

    private void processItem() {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        BreederRecipes.BreederRecipe recipe = BreederRecipes.getOutput(input);
        if (recipe == null) return;

        ItemStack output = inventory.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, recipe.output.copy());
        } else if (ItemStack.isSameItemSameTags(output, recipe.output)) {
            output.grow(recipe.output.getCount());
        }

        input.shrink(1);
    }

    /** Reuses the current recipe's "flux" balance number 1:1 as an FE-per-tick draw (see class javadoc). */
    public int getPowerRequired() {
        BreederRecipes.BreederRecipe recipe = BreederRecipes.getOutput(inventory.getStackInSlot(SLOT_INPUT));
        return recipe != null ? recipe.energyPerTick : 0;
    }

    public int getDuration() {
        return duration;
    }

    public long getPowerScaled(int scale) {
        long max = getMaxEnergyStored();
        return max <= 0 ? 0 : (getEnergyStored() * scale) / max;
    }

    public int getProgressScaled(int scale) {
        int dur = getDuration();
        return dur <= 0 ? 0 : (progress * scale) / dur;
    }

    public boolean isOn() {
        return isOn;
    }

    public ContainerData getContainerData() {
        return data;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.breeder");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_OUTPUT) {
            return false;
        }
        if (slot == SLOT_INPUT) {
            return BreederRecipes.getOutput(stack) != null;
        }
        return true;
    }

    @Override
    public AABB getRenderBoundingBox() {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof com.hbm_m.block.machines.MachineBreederBlock block)) {
            return super.getRenderBoundingBox();
        }
        Direction facing = state.getValue(com.hbm_m.block.machines.MachineBreederBlock.FACING);
        return block.getStructureHelper().getRenderBoundingBox(worldPosition, facing, 0.0);
    }

    public boolean stillValid(Player player) {
        return !this.isRemoved() && player.distanceToSqr(this.getBlockPos().getCenter()) <= 64.0D;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineBreederMenu(containerId, playerInventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("progress", progress);
        tag.putBoolean("isOn", isOn);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress = tag.getInt("progress");
        isOn = tag.getBoolean("isOn");
    }
}
