package com.hbm_m.item.liquids;

import java.util.function.Supplier;

import com.hbm_m.entity.grenades.EntityDisperserCanister;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 {@code com.hbm.items.weapon.ItemDisperser} ({@code disperser_canister}, {@code glyphid_gland}): wird geworfen und
 * setzt beim Aufprall eine Nebelwolke der Fluessigkeit frei ({@link EntityDisperserCanister}).
 */
public class ItemDisperser extends ItemFluidTank {

    public ItemDisperser(Properties properties, Supplier<Item> empty) {
        super(properties, empty);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Fluid fluid = getFluid(stack);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.5F, 0.4F / (world.random.nextFloat() * 0.4F + 0.8F));

        if (!world.isClientSide) {
            EntityDisperserCanister canister = new EntityDisperserCanister(world, player);
            canister.setType(this);
            canister.setFluid(fluid);
            world.addFreshEntity(canister);
        }
        return InteractionResultHolder.sidedSuccess(stack, world.isClientSide);
    }

    @Override
    protected boolean accepts(Fluid f) {
        FluidType type = FluidType.forFluid(f);
        if (type.isDispersable() && this == ModItems.DISPERSER_CANISTER.get()) return true;
        // Original-Klammerung: PHEROMONE immer, SULFURIC_ACID nur fuer die Druese
        return f == ModFluids.PHEROMONE.getSource() || f == ModFluids.SULFURIC_ACID.getSource() && this == ModItems.GLYPHID_GLAND.get();
    }

    @Override
    public Component getName(ItemStack stack) {
        Component s = Component.translatable(getDescriptionId(stack));
        Component s1 = fluidName(getFluid(stack));
        return this == ModItems.GLYPHID_GLAND.get() ? s1.copy().append(" ").append(s) : s.copy().append(" ").append(s1);
    }
}
