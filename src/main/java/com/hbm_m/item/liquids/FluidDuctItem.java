package com.hbm_m.item.liquids;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.api.fluids.ModFluids;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.machines.FluidDuctBlockEntity;
import com.hbm_m.block.machines.FluidDuctBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Fluid Duct - A pipe item for each fluid type. Each fluid has its own duct variant,
 * stored via NBT. The overlay layer is tinted with the fluid's color.
 * Right-click to place as a FluidDuctBlock.
 */
public class FluidDuctItem extends Item {

    public static final String NBT_FLUID_TYPE = "FluidType";

    public FluidDuctItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        FluidStack fluid = getFluidType(stack);
        if (fluid.isEmpty()) {
            return Component.translatable("item.hbm_m.fluid_duct.empty");
        }
        return Component.translatable("item.hbm_m.fluid_duct", fluid.getDisplayName());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos placePos = clickedPos.relative(face);

        // Check if target space is replaceable
        BlockState existingState = level.getBlockState(placePos);
        if (!existingState.canBeReplaced()) {
            return InteractionResult.FAIL;
        }

        // Check if player can edit
        if (context.getPlayer() != null && !context.getPlayer().mayUseItemAt(placePos, face, context.getItemInHand())) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide()) {
            Block ductBlock = ModBlocks.FLUID_DUCT.get();
            BlockState ductState = ductBlock.defaultBlockState();
            // Place with UPDATE_CLIENTS only — defer neighbor notifications until fluid type is set
            level.setBlock(placePos, ductState, 2);

            // Set the fluid type on the block entity
            BlockEntity be = level.getBlockEntity(placePos);
            if (be instanceof FluidDuctBlockEntity ductBe) {
                FluidStack fluid = getFluidType(context.getItemInHand());
                if (!fluid.isEmpty()) {
                    ductBe.setFluidType(fluid.getFluid());
                }
            }

            // Now recalculate connections (fluid type is known) and notify neighbors
            if (ductBlock instanceof FluidDuctBlock ductBlockInstance) {
                BlockState connected = ductBlockInstance.getConnectionState(level, placePos);
                level.setBlock(placePos, connected, 3);
            }

            // Play placement sound
            SoundType sound = ductState.getSoundType();
            level.playSound(null, placePos, sound.getPlaceSound(), SoundSource.BLOCKS,
                    (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);

            // Consume item
            if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** Gets the fluid type stored in the duct. */
    public static FluidStack getFluidType(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(NBT_FLUID_TYPE)) {
            String fluidName = stack.getTag().getString(NBT_FLUID_TYPE);
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(fluidName));
            if (fluid != null && fluid != net.minecraft.world.level.material.Fluids.EMPTY) {
                return new FluidStack(fluid, 1);
            }
        }
        return FluidStack.EMPTY;
    }

    /** Sets the fluid type stored in the duct. */
    public static void setFluidType(ItemStack stack, Fluid fluid) {
        ResourceLocation loc = ForgeRegistries.FLUIDS.getKey(fluid);
        if (loc != null) {
            stack.getOrCreateTag().putString(NBT_FLUID_TYPE, loc.toString());
        }
    }

    /** Creates an ItemStack of this duct for a specific fluid entry. */
    public static ItemStack createStack(Item ductItem, ModFluids.FluidEntry entry) {
        ItemStack stack = new ItemStack(ductItem);
        setFluidType(stack, entry.getSource());
        return stack;
    }

    /** Returns tint color for overlay layer (for ItemColor handler). */
    public static int getTintColor(ItemStack stack) {
        FluidStack fluid = getFluidType(stack);
        if (fluid.isEmpty()) return 0xFFFFFF;
        return HbmFluidRegistry.getTintColor(fluid.getFluid());
    }
}
