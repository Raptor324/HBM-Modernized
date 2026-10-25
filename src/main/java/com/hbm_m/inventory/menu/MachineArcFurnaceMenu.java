package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineArcFurnaceBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.item.industrial.ItemArcElectrode;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.ArcFurnaceRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.Nullable;
import com.hbm_m.platform.ModItemStackHandler;

/**
 * 1:1 {@code ContainerMachineArcFurnaceLarge}: Elektroden (62-98,22), Batterie (8,108), Upgrade (152,108), 4x5
 * Eingabefelder ab (44,54) mit {@code SlotArcFurnace}-Regeln, Warteschlange (44-116,129), Spielerinventar ab (8,174).
 */
public class MachineArcFurnaceMenu extends AbstractContainerMenu {

    private final MachineArcFurnaceBlockEntity furnace;

    public MachineArcFurnaceMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineArcFurnaceMenu(int id, Inventory playerInv, MachineArcFurnaceBlockEntity tile) {
        super(ModMenuTypes.ARC_FURNACE_MENU.get(), id);
        this.furnace = tile;

        // На клиенте тайл может отсутствовать (реплей Flashback) — подставляем пустую заглушку,
        // чтобы конструктор дошёл до конца и пакет открытия меню не уронил клиент
        Container c = new ModItemStackHandlerContainer(
                tile != null ? tile.getInventory() : new ModItemStackHandler.Dummy(MachineArcFurnaceBlockEntity.INVENTORY_SIZE),
                tile != null ? tile::setChanged : null);

        //Electrodes
        for (int i = 0; i < 3; i++) this.addSlot(new Slot(c, i, 62 + i * 18, 22));
        //Battery
        this.addSlot(new Slot(c, 3, 8, 108));
        //Upgrade
        this.addSlot(new Slot(c, 4, 152, 108));
        //Inputs
        for (int i = 0; i < 4; i++) for (int j = 0; j < 5; j++) this.addSlot(new SlotArcFurnace(c, 5 + j + i * 5, 44 + j * 18, 54 + i * 18));
        //IO
        for (int i = 0; i < 5; i++) this.addSlot(new Slot(c, i + 25, 44 + i * 18, 129));

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 174 + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInv, i, 8 + i * 18, 232));
        }
    }

    /**
     * Kanonische Rezeptsuche (ersetzt das FFA-{@code ArcFurnaceRecipes.getOutput(stack, liquid, level)}):
     * datengetriebene {@link ArcFurnaceRecipe}s aus dem Vanilla-RecipeManager, gefiltert nach Ausgabeart
     * (Fluessig-Modus: Fluid-Ausgang, Fest-Modus: Item-Ausgang). Entspricht dem Muster von
     * {@code MachineCombinationOvenBlockEntity.findRecipe}.
     */
    @Nullable
    private static ArcFurnaceRecipe findRecipe(@Nullable Level level, ItemStack stack, boolean liquid) {
        if (level == null || stack.isEmpty()) return null;
        for (ArcFurnaceRecipe recipe : RecipeHooks.getAllRecipes(level, ArcFurnaceRecipe.Type.INSTANCE)) {
            if (!recipe.matchesInput(stack)) continue;
            boolean hasOutput = liquid ? recipe.hasFluidOutput1() || recipe.hasFluidOutput2() : recipe.hasItemOutput();
            if (hasOutput) return recipe;
        }
        return null;
    }

    /** Original {@code SlotArcFurnace}: im Fest-Modus nur passende Mengen, Stapelgrenze nach Upgrade. */
    private class SlotArcFurnace extends Slot {

        SlotArcFurnace(Container inventory, int id, int x, int y) {
            super(inventory, id, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (furnace == null) return false;
            if (furnace.liquidMode) return true;
            ArcFurnaceRecipe recipe = findRecipe(furnace.getLevel(), stack, furnace.liquidMode);
            if (recipe != null && recipe.hasItemOutput()) {
                ItemStack output = recipe.getOutput();
                return output.getCount() * stack.getCount() <= output.getMaxStackSize() && stack.getCount() <= furnace.getMaxInputSize();
            }
            return false;
        }

        @Override
        public int getMaxStackSize() {
            if (furnace == null) return 1;
            return this.hasItem() ? furnace.getMaxInputSize() : 1;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return Math.min(getMaxStackSize(), stack.getMaxStackSize());
        }
    }

    private static MachineArcFurnaceBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineArcFurnaceBlockEntity arcFurnace) {
            return arcFurnace;
        }
        // На клиенте тайл может отсутствовать (реплей Flashback) — не крашим пакет, возвращаем null.
        // На сервере отсутствие тайла — реальный баг, поэтому там падаем как раньше.
        if (inventory.player.level().isClientSide) {
            return null;
        }
        throw new MenuBlockEntityMissingException("No MachineArcFurnaceBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":arc_furnace_menu");
    }

    public MachineArcFurnaceBlockEntity getBlockEntity() {
        return furnace;
    }

    @Override
    public boolean stillValid(Player player) {
        return MenuReach.stillValid(player, furnace);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack rStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            rStack = stack.copy();

            if (index <= 29) {
                if (!this.moveItemStackTo(stack, 30, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {

                if (rStack.getItem() instanceof com.hbm_m.api.item.IBatteryItem) {
                    if (!this.moveItemStackTo(stack, 3, 4, false)) return ItemStack.EMPTY;
                } else if (rStack.getItem() instanceof ItemArcElectrode) {
                    if (!this.moveItemStackTo(stack, 0, 3, false)) return ItemStack.EMPTY;
                } else if (rStack.getItem() instanceof ItemMachineUpgrade) {
                    if (!this.moveItemStackTo(stack, 4, 5, false)) return ItemStack.EMPTY;
                } else {
                    if (!this.moveItemStackTo(stack, 25, 30, false)) return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return rStack;
    }
}
