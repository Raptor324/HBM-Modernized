package com.hbm_m.item.liquids;

import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.block.entity.machines.FluidDuctBlockEntity;
import com.hbm_m.block.machines.FluidDuctBlock;
import com.hbm_m.inventory.fluid.ModFluids;

import dev.architectury.fluid.FluidStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
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

/**
 * Fluid Duct — places the matching {@link FluidDuctBlock} style; fluid type in NBT for tint/transport.
 */
public class FluidDuctItem extends Item {

    public static final String NBT_FLUID_TYPE = "FluidType";

    private final Supplier<Block> placedBlock;
    private final String translationWithFluid;
    private final String translationEmpty;

    public FluidDuctItem(Properties properties, Supplier<Block> placedBlock,
            String translationWithFluid, String translationEmpty) {
        super(properties);
        this.placedBlock = placedBlock;
        this.translationWithFluid = translationWithFluid;
        this.translationEmpty = translationEmpty;
    }

    @Override
    public Component getName(ItemStack stack) {
        FluidStack fluid = getFluidType(stack);
        if (fluid.isEmpty()) {
            return Component.translatable(translationEmpty);
        }
        return Component.translatable(translationWithFluid, fluid.getName());
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

        BlockState existingState = level.getBlockState(placePos);
        if (!existingState.canBeReplaced()) {
            return InteractionResult.FAIL;
        }

        if (context.getPlayer() != null && !context.getPlayer().mayUseItemAt(placePos, face, context.getItemInHand())) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide()) {
            Block ductBlock = placedBlock.get();
            BlockState ductState = ductBlock.defaultBlockState();
            level.setBlock(placePos, ductState, Block.UPDATE_CLIENTS);

            BlockEntity be = level.getBlockEntity(placePos);
            if (be instanceof FluidDuctBlockEntity ductBe) {
                FluidStack fluid = getFluidType(context.getItemInHand());
                if (!fluid.isEmpty()) {
                    ductBe.setFluidType(fluid.getFluid());
                }
            }

            if (ductBlock instanceof FluidDuctBlock fd) {
                BlockState connected = fd.getConnectionState(level, placePos);
                level.setBlock(placePos, connected, Block.UPDATE_ALL);
                FluidDuctBlock.refreshAdjacentDucts(level, placePos);
            }

            SoundType sound = ductState.getSoundType();
            level.playSound(null, placePos, sound.getPlaceSound(), SoundSource.BLOCKS,
                    (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);

            if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    public static FluidStack getFluidType(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(NBT_FLUID_TYPE)) {
            String fluidName = stack.getTag().getString(NBT_FLUID_TYPE);
            Fluid fluid = BuiltInRegistries.FLUID.get(new ResourceLocation(fluidName));
            if (fluid != null && fluid != net.minecraft.world.level.material.Fluids.EMPTY) {
                return FluidStack.create(fluid, 1);
            }
        }
        return FluidStack.empty();
    }

    public static void setFluidType(ItemStack stack, Fluid fluid) {
        ResourceLocation loc = BuiltInRegistries.FLUID.getKey(fluid);
        if (loc != null) {
            stack.getOrCreateTag().putString(NBT_FLUID_TYPE, loc.toString());
        }
    }

    public static ItemStack createStack(Item ductItem, ModFluids.FluidEntry entry) {
        ItemStack stack = new ItemStack(ductItem);
        setFluidType(stack, entry.getSource());
        return stack;
    }

    public static int getTintColor(ItemStack stack) {
        FluidStack fluid = getFluidType(stack);
        if (fluid.isEmpty()) return 0xFFFFFF;
        return HbmFluidRegistry.getTintColor(fluid.getFluid());
    }
}
