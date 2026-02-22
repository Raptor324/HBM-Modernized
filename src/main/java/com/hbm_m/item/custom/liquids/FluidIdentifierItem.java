package com.hbm_m.item.custom.liquids;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.api.fluids.ModFluids;
import com.hbm_m.client.overlay.FluidIdentifierScreen;
import com.hbm_m.item.IItemControlReceiver;
import com.hbm_m.item.IItemFluidIdentifier;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

/**
 * Universal fluid identifier. Has two slots (primary/secondary) for fluid types.
 * RMB: swap primary and secondary. Shift+RMB: open GUI to select fluids.
 */
public class FluidIdentifierItem extends Item implements IItemFluidIdentifier, IItemControlReceiver {

    private static final String NBT_FLUID1 = "fluid1";
    private static final String NBT_FLUID2 = "fluid2";

    public FluidIdentifierItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) return InteractionResultHolder.pass(stack);

        if (player.isShiftKeyDown()) {
            // Open GUI on client
            if (level.isClientSide) {
                Minecraft.getInstance().setScreen(new FluidIdentifierScreen(player));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        } else {
            // Swap primary and secondary on server
            if (!level.isClientSide) {
                Fluid primary = getType(stack, true);
                Fluid secondary = getType(stack, false);
                setType(stack, secondary, true);
                setType(stack, primary, false);
                player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.25f, 1.25f);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.hbm_m.fluid_identifier.info"));
        tooltip.add(Component.literal("   ").append(getFluidDisplayName(getType(stack, true))));
        tooltip.add(Component.translatable("item.hbm_m.fluid_identifier.info2"));
        tooltip.add(Component.literal("   ").append(getFluidDisplayName(getType(stack, false))));
    }

    private static Component getFluidDisplayName(Fluid fluid) {
        if (fluid == null || fluid == net.minecraft.world.level.material.Fluids.EMPTY) {
            return Component.translatable("fluid.hbm_m.none");
        }
        return Component.translatable(fluid.getFluidType().getDescriptionId());
    }

    @Override
    public Fluid getType(Level level, BlockPos pos, ItemStack stack) {
        return getType(stack, true);
    }

    @Override
    public void receiveControl(ItemStack stack, CompoundTag data) {
        if (data.contains(NBT_FLUID1)) {
            setType(stack, data.getString(NBT_FLUID1), true);
        }
        if (data.contains(NBT_FLUID2)) {
            setType(stack, data.getString(NBT_FLUID2), false);
        }
    }

    public static Fluid getType(ItemStack stack, boolean primary) {
        String name = getTypeName(stack, primary);
        if (name == null || name.isEmpty() || "none".equals(name)) {
            return net.minecraft.world.level.material.Fluids.EMPTY;
        }
        ModFluids.FluidEntry entry = ModFluids.getEntry(name);
        return entry != null ? entry.getSource() : net.minecraft.world.level.material.Fluids.EMPTY;
    }

    public static String getTypeName(ItemStack stack, boolean primary) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return "none";
        String key = primary ? NBT_FLUID1 : NBT_FLUID2;
        return tag.getString(key);
    }

    public static void setType(ItemStack stack, Fluid fluid, boolean primary) {
        setType(stack, fluid != null ? HbmFluidRegistry.getFluidName(fluid) : "none", primary);
    }

    public static void setType(ItemStack stack, String fluidName, boolean primary) {
        stack.getOrCreateTag().putString(primary ? NBT_FLUID1 : NBT_FLUID2,
            fluidName != null ? fluidName : "none");
    }

    /** Returns tint color for primary fluid (for ItemColor). */
    public static int getTintColor(ItemStack stack) {
        Fluid f = getType(stack, true);
        if (f == null || f == net.minecraft.world.level.material.Fluids.EMPTY) return 0xFFFFFF;
        return HbmFluidRegistry.getTintColor(f);
    }
}
