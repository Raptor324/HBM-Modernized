package com.hbm_m.item.tool;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import com.hbm_m.api.block.IToolable;
import com.hbm_m.api.fluids.IFillableItem;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.item.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code com.hbm.items.tool.ItemBlowtorch} ({@code blowtorch}: Gas, {@code acetylene_torch}: ungesaettigte
 * Kohlenwasserstoffe + Sauerstoff). Schweisst {@link IToolable}-Bloecke ({@code ToolType.TORCH}) und verbraucht pro
 * Einsatz 250 mB Gas bzw. 20 mB + 10 mB. NBT-Schluessel ist im Port der Fluessigkeitsname statt der Fluid-ID.
 */
public class ItemBlowtorch extends Item implements IFillableItem, ITooltipProvider {

    public ItemBlowtorch(Properties properties) {
        super(properties.stacksTo(1));
    }

    private static Fluid gas() { return ModFluids.GAS.getSource(); }
    private static Fluid unsaturateds() { return ModFluids.UNSATURATEDS.getSource(); }
    private static Fluid oxygen() { return ModFluids.OXYGEN.getSource(); }

    private boolean isBlowtorch() { return this == ModItems.BLOWTORCH.get(); }
    private boolean isAcetylene() { return this == ModItems.ACETYLENE_TORCH.get(); }

    @Override
    public boolean acceptsFluid(Fluid type, ItemStack stack) {
        if (isBlowtorch()) return type == gas();
        if (isAcetylene()) return type == unsaturateds() || type == oxygen();
        return false;
    }

    @Override
    public int tryFill(Fluid type, int amount, ItemStack stack) {

        if (!acceptsFluid(type, stack))
            return amount;

        int toFill = Math.min(amount, 50);
        toFill = Math.min(toFill, getMaxFill(type) - this.getFill(stack, type));
        this.setFill(stack, type, this.getFill(stack, type) + toFill);

        return amount - toFill;
    }

    private static String key(Fluid type) {
        return BuiltInRegistries.FLUID.getKey(type).toString();
    }

    public int getFill(ItemStack stack, Fluid type) {
        if (!stack.hasTag()) initNBT(stack);
        return stack.getTag().getInt(key(type));
    }

    public int getMaxFill(Fluid type) {
        if (type == gas()) return 4_000;
        if (type == unsaturateds()) return 8_000;
        if (type == oxygen()) return 16_000;
        return 0;
    }

    public void setFill(ItemStack stack, Fluid type, int fill) {
        if (!stack.hasTag()) initNBT(stack);
        stack.getTag().putInt(key(type), fill);
    }

    public void initNBT(ItemStack stack) {
        // Direkt ins Tag schreiben: ItemStack.hasTag() ist bei leerem Tag false, ueber setFill gaebe das eine Endlosschleife.
        CompoundTag tag = new CompoundTag();

        if (isBlowtorch()) {
            tag.putInt(key(gas()), this.getMaxFill(gas()));
        }
        if (isAcetylene()) {
            tag.putInt(key(unsaturateds()), this.getMaxFill(unsaturateds()));
            tag.putInt(key(oxygen()), this.getMaxFill(oxygen()));
        }
        stack.setTag(tag);
    }

    public static ItemStack getEmptyTool(Item item) {
        ItemBlowtorch tool = (ItemBlowtorch) item;
        ItemStack stack = new ItemStack(item);

        if (item == ModItems.BLOWTORCH.get()) {
            tool.setFill(stack, gas(), 0);
        }
        if (item == ModItems.ACETYLENE_TORCH.get()) {
            tool.setFill(stack, unsaturateds(), 0);
            tool.setFill(stack, oxygen(), 0);
        }

        return stack;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        ItemStack stack = ctx.getItemInHand();
        Block b = world.getBlockState(pos).getBlock();

        if (b instanceof IToolable toolable && ctx.getPlayer() != null) {

            if (isBlowtorch()) {
                if (this.getFill(stack, gas()) < 250) return InteractionResult.PASS;
            }

            if (isAcetylene()) {
                if (this.getFill(stack, unsaturateds()) < 20) return InteractionResult.PASS;
                if (this.getFill(stack, oxygen()) < 10) return InteractionResult.PASS;
            }

            Vec3 hit = ctx.getClickLocation();
            float fX = (float) (hit.x - pos.getX()), fY = (float) (hit.y - pos.getY()), fZ = (float) (hit.z - pos.getZ());

            if (toolable.onScrew(world, ctx.getPlayer(), pos, ctx.getClickedFace(), fX, fY, fZ, ctx.getHand(), IToolable.ToolType.TORCH)) {

                if (!world.isClientSide) {

                    if (isBlowtorch()) {
                        this.setFill(stack, gas(), this.getFill(stack, gas()) - 250);
                    }

                    if (isAcetylene()) {
                        this.setFill(stack, unsaturateds(), this.getFill(stack, unsaturateds()) - 20);
                        this.setFill(stack, oxygen(), this.getFill(stack, oxygen()) - 10);
                    }

                    CompoundTag dPart = new CompoundTag();
                    dPart.putString("type", "tau");
                    dPart.putByte("count", (byte) 10);
                    com.hbm_m.particle.helper.IParticleCreator.sendPacket((ServerLevel) world, pos.getX() + fX, pos.getY() + fY, pos.getZ() + fZ, 50, dPart);
                }

                return InteractionResult.sidedSuccess(world.isClientSide);
            }
        }

        return InteractionResult.PASS;
    }

    public double getDurabilityForDisplay(ItemStack stack) {
        double frac = 0D;

        if (isBlowtorch()) {
            frac = (double) this.getFill(stack, gas()) / (double) this.getMaxFill(gas());
        }

        if (isAcetylene()) {
            frac = Math.min(
                    (double) this.getFill(stack, unsaturateds()) / (double) this.getMaxFill(unsaturateds()),
                    (double) this.getFill(stack, oxygen()) / (double) this.getMaxFill(oxygen()));
        }

        return 1 - frac;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getDurabilityForDisplay(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) Math.round(13.0D - getDurabilityForDisplay(stack) * 13.0D);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float f = Math.max(0.0F, (float) (1.0D - getDurabilityForDisplay(stack)));
        return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        if (isBlowtorch()) {
            list.add(getFillGauge(stack, gas()).withStyle(ChatFormatting.YELLOW));
        }
        if (isAcetylene()) {
            list.add(getFillGauge(stack, unsaturateds()).withStyle(ChatFormatting.YELLOW));
            list.add(getFillGauge(stack, oxygen()).withStyle(ChatFormatting.AQUA));
        }
    }

    private net.minecraft.network.chat.MutableComponent getFillGauge(ItemStack stack, Fluid type) {
        //? if forge {
        Component name = Component.translatable(type.getFluidType().getDescriptionId());
        //?} else {
        /*Component name = Component.literal(key(type));
        *///?}
        return Component.empty().append(name).append(": " + String.format(Locale.US, "%,d", this.getFill(stack, type)) + " / " + String.format(Locale.US, "%,d", this.getMaxFill(type)));
    }

    @Override public boolean providesFluid(Fluid type, ItemStack stack) { return false; }
    @Override public int tryEmpty(Fluid type, int amount, ItemStack stack) { return amount; }
    @Override public @Nullable Fluid getFirstFluidType(ItemStack stack) { return null; }
    @Override public int getFill(ItemStack stack) { return 0; }

    //? if forge {
    /** Anbindung an die Tank-Lader des Ports: nur Befuellen (providesFluid = false). */
    @Override
    public net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new FillOnlyCapability(stack, this);
    }

    public static class FillOnlyCapability implements net.minecraftforge.common.capabilities.ICapabilityProvider,
            net.minecraftforge.fluids.capability.IFluidHandlerItem {
        private final ItemStack stack;
        private final IFillableItem item;
        private final net.minecraftforge.common.util.LazyOptional<net.minecraftforge.fluids.capability.IFluidHandlerItem> opt;

        public FillOnlyCapability(ItemStack stack, IFillableItem item) {
            this.stack = stack;
            this.item = item;
            this.opt = net.minecraftforge.common.util.LazyOptional.of(() -> this);
        }

        @Override
        public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
            return net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM.orEmpty(cap, opt);
        }

        @Override public ItemStack getContainer() { return stack; }
        @Override public int getTanks() { return 1; }
        @Override public net.minecraftforge.fluids.FluidStack getFluidInTank(int tank) { return net.minecraftforge.fluids.FluidStack.EMPTY; }
        @Override public int getTankCapacity(int tank) { return 50; }
        @Override public boolean isFluidValid(int tank, net.minecraftforge.fluids.FluidStack fs) { return item.acceptsFluid(fs.getFluid(), stack); }

        @Override
        public int fill(net.minecraftforge.fluids.FluidStack resource, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction action) {
            if (resource.isEmpty() || !item.acceptsFluid(resource.getFluid(), stack)) return 0;
            ItemStack target = action.simulate() ? stack.copy() : stack;
            return resource.getAmount() - item.tryFill(resource.getFluid(), resource.getAmount(), target);
        }

        @Override public net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.FluidStack resource, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction action) { return net.minecraftforge.fluids.FluidStack.EMPTY; }
        @Override public net.minecraftforge.fluids.FluidStack drain(int maxDrain, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction action) { return net.minecraftforge.fluids.FluidStack.EMPTY; }
    }
    //?}
}
