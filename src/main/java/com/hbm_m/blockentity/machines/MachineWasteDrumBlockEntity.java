package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.item.rbmk.RBMKRodItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * Port of {@code TileEntityWasteDrum} (1.7.10 Original) - a 12-slot spent-fuel-rod cooling pool.
 * While the drum has at least one adjacent water block, every stored {@code RBMKRodItem} cools down
 * (releasing heat into the surrounding pool) instead of remaining at reactor operating temperature.
 * <p>
 * Alles andere aus {@link com.hbm_m.inventory.recipes.FuelPoolRecipes} (heisser Abfall, heisse PWR-Staebe)
 * kuehlt zufaellig ab: Chance 1 / (60 * 60 * 20 / Wasserseiten) pro Tick und Slot.
 */
public class MachineWasteDrumBlockEntity extends BaseMachineBlockEntity {

    public static final int INVENTORY_SIZE = 12;
    private static final double COOL_MOD = 0.025D;
    private static final double PROVIDE_HEAT = 20D;

    public MachineWasteDrumBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_WASTE_DRUM_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineWasteDrumBlockEntity be) {
        if (level.isClientSide) return;

        int water = 0;
        for (Direction dir : Direction.values()) {
            if (level.getFluidState(pos.relative(dir)).is(Fluids.WATER)) water++;
        }
        if (water <= 0) return;

        int r = 60 * 60 * 20 / water;

        for (int i = 0; i < INVENTORY_SIZE; i++) {
            ItemStack stack = be.inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof RBMKRodItem rod) {
                rod.updateHeat(level, stack, COOL_MOD);
                rod.provideHeat(level, stack, PROVIDE_HEAT, COOL_MOD);
            } else if (level.random.nextInt(r) == 0) {
                ItemStack out = com.hbm_m.inventory.recipes.FuelPoolRecipes.recipes().get(stack.getItem());
                if (out != null) be.inventory.setStackInSlot(i, out.copy());
            }
        }
        be.setChanged();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return com.hbm_m.inventory.recipes.FuelPoolRecipes.isInput(stack) || stack.getItem() instanceof RBMKRodItem;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.machine_waste_drum");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return com.hbm_m.inventory.menu.MachineWasteDrumMenu.create(id, inventory, this);
    }
}
