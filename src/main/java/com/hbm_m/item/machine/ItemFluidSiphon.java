package com.hbm_m.item.machine;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Unsiphonable;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tool.ItemPipette;
import com.hbm_m.platform.FluidHooks;
import com.hbm_m.util.CompatExternal;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 {@code com.hbm.items.machine.ItemFluidSiphon}: saugt die Empfangstanks einer Maschine in leere Behaelter
 * im Inventar (Behaelter = Gegenstaende mit Fluessigkeits-Capability statt FluidContainerRegistry), den Rest bis
 * 1000 mB in eine passende Pipette (keine Laborpipette, keine, die zerfallen wuerde).
 */
public class ItemFluidSiphon extends Item {

    public ItemFluidSiphon(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        BlockEntity te = CompatExternal.getCoreFromPos(ctx.getLevel(), ctx.getClickedPos());

        if (te instanceof IFluidStandardReceiverMK2 receiver) {
            FluidTank[] tanks = receiver.getReceivingTanks();

            boolean hasDrainedTank = false;

            // We need to iterate through the inventory for _each_ siphonable
            // tank, so we can handle fluids that can only go into certain containers
            // After we successfully siphon any fluid from a tank, we stop
            // further processing, multiple fluid types require multiple clicks
            for (FluidTank tank : tanks) {
                if (tank.getFill() <= 0)
                    continue;

                ItemStack availablePipette = null;
                Fluid tankType = tank.getTankType();

                if (FluidType.hasTrait(tankType, FT_Unsiphonable.class))
                    continue;

                for (int j = 0; j < player.getInventory().items.size(); j++) {
                    ItemStack inventoryStack = player.getInventory().items.get(j);
                    if (inventoryStack.isEmpty())
                        continue;

                    if (availablePipette == null && inventoryStack.getItem() instanceof ItemPipette pipette) {
                        if (!pipette.willFizzle(tankType) && pipette != ModItems.PIPETTE_LABORATORY.get()) { // Ignoring laboratory pipettes for now
                            availablePipette = inventoryStack;
                        }
                    }

                    if (inventoryStack.getItem() instanceof ItemPipette) continue;

                    // FluidContainerRegistry.getContainer: ein leerer Behaelter, der genau diese Fluessigkeit aufnimmt
                    ItemStack single = inventoryStack.copyWithCount(1);
                    if (FluidHooks.extractFluidFromItem(single, Integer.MAX_VALUE, true).amount() > 0) continue;
                    int content = FluidHooks.getItemFluidCapacity(single);
                    if (content <= 0) continue;
                    Fluid toInsert = VanillaFluidEquivalence.forVanillaContainerFill(tank.getStoredFluid());
                    FluidHooks.FluidInsertion sim = FluidHooks.insertFluidIntoItem(single, toInsert, content, true);
                    if (sim.amountInserted() != content) continue;
                    ItemStack full = FluidHooks.insertFluidIntoItem(single.copy(), toInsert, content, false).remainder();

                    while (tank.getFill() >= content && inventoryStack.getCount() > 0) {
                        hasDrainedTank = true;

                        inventoryStack.shrink(1);
                        if (inventoryStack.isEmpty()) {
                            player.getInventory().items.set(j, ItemStack.EMPTY);
                        }

                        ItemStack filledContainer = full.copy();
                        tank.drainMb(content, false);
                        if (!player.getInventory().add(filledContainer)) player.drop(filledContainer, false);
                    }
                }

                // If the remainder of the tank can only fit into a pipette,
                // fill a pipette with the remainder
                // Will not auto-fill fizzlable pipettes, there is no feedback
                // for the fizzle in this case, and that's a touch too unfair
                if (availablePipette != null && tank.getFill() < 1000) {
                    ItemPipette pipette = (ItemPipette) availablePipette.getItem();

                    if (pipette.acceptsFluid(tankType, availablePipette)) {
                        hasDrainedTank = true;
                        int rest = pipette.tryFill(tankType, tank.getFill(), availablePipette);
                        tank.setFluid(tank.getStoredFluid(), rest);
                    }
                }

                if (hasDrainedTank)
                    return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
            }
        }

        return InteractionResult.PASS;
    }
}
