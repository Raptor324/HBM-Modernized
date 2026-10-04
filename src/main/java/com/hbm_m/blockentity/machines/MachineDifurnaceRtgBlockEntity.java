package com.hbm_m.blockentity.machines;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.MachineDifurnaceRtgBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.item.machine.ItemRTGPellet;
import com.hbm_m.recipe.BlastFurnaceRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code TileEntityDiFurnaceRTG}: Hochofen-Rezepte, beheizt von sechs RTG-Pellets. Arbeitet erst ab Hitze 15, der
 * Fortschritt waechst um die Hitze je Tick bis 1200. Die Pellets altern bei jedem {@code hasPower()}-Aufruf - genau wie
 * im Original also auch beim reinen Pruefen. Die Eingabeseiten der beiden Eingaenge sind (Rechtsklick auf den leeren
 * Platz) einstellbar; der Block leuchtet, solange er arbeitet.
 */
public class MachineDifurnaceRtgBlockEntity extends BaseMachineBlockEntity {

    public static final int SLOT_INPUT_TOP = 0;
    public static final int SLOT_INPUT_BOTTOM = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int PELLET_SLOT_START = 3;
    public static final int PELLET_SLOT_COUNT = 6;
    public static final int INVENTORY_SIZE = PELLET_SLOT_START + PELLET_SLOT_COUNT;

    private static final short timeRequired = 1200;
    private static final int[] rtgIn = new int[] { 3, 4, 5, 6, 7, 8 };

    public short progress;
    private short processSpeed = 0;
    public byte sideUpper = 1;
    public byte sideLower = 1;

    public MachineDifurnaceRtgBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_DIFURNACE_RTG_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineDifurnaceRtgBlockEntity be) {
        if (level.isClientSide) return;
        be.serverTick(level, pos, state);
    }

    private void serverTick(Level level, BlockPos pos, BlockState state) {
        if (canProcess() && hasPower()) {
            progress += processSpeed;
            if (progress >= timeRequired) {
                processItem();
                progress = 0;
            }
        } else {
            progress = 0;
        }

        boolean lit = isProcessing() || (canProcess() && hasPower());
        if (state.getValue(MachineDifurnaceRtgBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(MachineDifurnaceRtgBlock.LIT, lit), 3);
        }

        setChanged();
        sendUpdateToClient();
    }

    public boolean canProcess() {
        if ((inventory.getStackInSlot(0).isEmpty() || inventory.getStackInSlot(1).isEmpty()) && !hasPower())
            return false;

        Optional<BlastFurnaceRecipe> recipe = getRecipe();
        if (recipe.isEmpty()) return false;
        ItemStack result = recipe.get().getResultItem(level.registryAccess());
        ItemStack out = inventory.getStackInSlot(2);
        if (out.isEmpty()) return true;
        if (!ItemStack.isSameItem(out, result)) return false;
        if (out.getCount() + result.getCount() > 64) return false;
        if (out.getCount() < 64 && out.getCount() < out.getMaxStackSize()) return true;
        return out.getCount() < result.getMaxStackSize();
    }

    private Optional<BlastFurnaceRecipe> getRecipe() {
        if (level == null) return Optional.empty();
        ItemStack top = inventory.getStackInSlot(SLOT_INPUT_TOP);
        ItemStack bottom = inventory.getStackInSlot(SLOT_INPUT_BOTTOM);
        if (top.isEmpty() || bottom.isEmpty()) return Optional.empty();

        SimpleContainer container = new SimpleContainer(4);
        container.setItem(1, top);
        container.setItem(2, bottom);
        return com.hbm_m.platform.recipe.RecipeHooks
                .getAllRecipes(level, BlastFurnaceRecipe.Type.INSTANCE).stream()
                .filter(r -> r.matchesRecipe(new com.hbm_m.platform.recipe.RecipeInputWrapper(container), level))
                .findFirst();
    }

    private void processItem() {
        if (canProcess()) {
            Optional<BlastFurnaceRecipe> recipe = getRecipe();
            if (recipe.isEmpty()) return;
            ItemStack recipeOut = recipe.get().getResultItem(level.registryAccess());
            ItemStack out = inventory.getStackInSlot(2);
            if (out.isEmpty()) {
                inventory.setStackInSlot(2, recipeOut.copy());
            } else if (ItemStack.isSameItem(out, recipeOut)) {
                ItemStack grown = out.copy();
                grown.grow(recipeOut.getCount());
                inventory.setStackInSlot(2, grown);
            }

            for (int i = 0; i < 2; i++) {
                ItemStack s = inventory.getStackInSlot(i).copy();
                s.shrink(1);
                inventory.setStackInSlot(i, s);
            }
            setChanged();
        }
    }

    /** Original {@code hasPower}: summiert die Pelletwaerme (und laesst sie altern), arbeitet ab 15. */
    public boolean hasPower() {
        processSpeed = (short) ItemRTGPellet.updateRTGs(inventory, rtgIn);
        return processSpeed >= 15;
    }

    public int getPower() { return processSpeed; }
    public boolean isProcessing() { return progress > 0; }
    public int getDiFurnaceProgressScaled(int i) { return (progress * i) / timeRequired; }
    public int getProgress() { return progress; }
    public int getMaxProgress() { return timeRequired; }

    /** Original {@code ContainerMachineDiFurnaceRTG.slotClick}: Rechtsklick auf leeren Eingang dreht die Seite weiter. */
    public void cycleSide(int slot) {
        if (slot == 0) sideUpper = (byte) ((sideUpper + 1) % 6);
        if (slot == 1) sideLower = (byte) ((sideLower + 1) % 6);
        setChanged();
        sendUpdateToClient();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_OUTPUT) return false;
        if (stack.getItem() instanceof ItemRTGPellet) return slot > 2;
        return true;
    }

    /** Original {@code canInsertItem}: Eingaenge nur von ihrer eingestellten Seite. */
    private boolean canInsertFrom(int slot, @Nullable Direction side) {
        if (side == null) return true;
        if (slot == 0 && sideUpper != side.get3DDataValue()) return false;
        if (slot == 1 && sideLower != side.get3DDataValue()) return false;
        return true;
    }

    /** Original {@code canExtractItem}: Ausgang, und abgebrannte (nicht mehr RTG-)Reste aus den Pelletplaetzen. */
    private boolean canExtractFrom(int slot) {
        if (slot > 2) return !(inventory.getStackInSlot(slot).getItem() instanceof ItemRTGPellet);
        return slot == 2;
    }

    //? if forge {
    private final Map<Direction, net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler>> sided = new EnumMap<>(Direction.class);

    @Override
    public @NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && side != null) {
            return sided.computeIfAbsent(side, d -> net.minecraftforge.common.util.LazyOptional.of(() -> new net.minecraftforge.items.IItemHandler() {
                @Override public int getSlots() { return inventory.getSlots(); }
                @Override public @NotNull ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
                @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                    if (!canInsertFrom(slot, d) || !isItemValidForSlot(slot, stack)) return stack;
                    return inventory.insertItem(slot, stack, simulate);
                }
                @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                    if (!canExtractFrom(slot)) return ItemStack.EMPTY;
                    return inventory.extractItem(slot, amount, simulate);
                }
                @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
                @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return canInsertFrom(slot, d) && isItemValidForSlot(slot, stack); }
            })).cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        sided.values().forEach(net.minecraftforge.common.util.LazyOptional::invalidate);
        sided.clear();
    }
    //?}

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.machine_difurnace_rtg");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return com.hbm_m.inventory.menu.MachineDifurnaceRtgMenu.create(id, inventory, this);
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putShort("progress", progress);
        tag.putShort("speed", processSpeed);
        tag.putByteArray("modes", new byte[] { sideUpper, sideLower });
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        progress = tag.getShort("progress");
        processSpeed = tag.getShort("speed");
        byte[] modes = tag.getByteArray("modes");
        if (modes.length >= 2) {
            sideUpper = modes[0];
            sideLower = modes[1];
        }
    }
}
