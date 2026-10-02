package com.hbm_m.item.tool;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.api.fluids.IFillableItem;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.trait.FT_Corrosive;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Amat;
import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.item.ModItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * 1:1 {@code com.hbm.items.tool.ItemPipette} ({@code pipette}, {@code pipette_boron}, {@code pipette_laboratory}):
 * einstellbare Fassung (Rechtsklick +, Schleichen -), die normale Pipette zerfaellt bei aetzenden Fluessigkeiten
 * (ausser Peroxid), keine Antimaterie. NBT "type" ist im Port der Registry-Name der Fluessigkeit.
 */
public class ItemPipette extends Item implements IFillableItem, ITooltipProvider {

    public ItemPipette(Properties properties) {
        super(properties.stacksTo(1).setNoRepair());
    }

    public short getMaxFill() {
        if (this == ModItems.PIPETTE_LABORATORY.get()) return 50;
        else return 1_000;
    }

    public void initNBT(ItemStack stack) {
        // Direkt schreiben: ItemStack.hasTag() ist bei leerem Tag false, ueber setFill gaebe das eine Endlosschleife.
        CompoundTag tag = new CompoundTag();
        tag.putString("type", BuiltInRegistries.FLUID.getKey(Fluids.EMPTY).toString()); // sets "type" and "fill" NBT
        tag.putShort("fill", (short) 0);
        tag.putShort("capacity", this.getMaxFill()); // set "capacity"
        stack.setTag(tag);
    }

    public Fluid getType(ItemStack stack) {
        if (!stack.hasTag()) initNBT(stack);
        ResourceLocation id = ResourceLocation.tryParse(stack.getTag().getString("type"));
        if (id == null) return Fluids.EMPTY;
        Fluid f = BuiltInRegistries.FLUID.get(id);
        return f == null ? Fluids.EMPTY : f;
    }

    public short getCapacity(ItemStack stack) {
        if (!stack.hasTag()) initNBT(stack);
        return stack.getTag().getShort("capacity");
    }

    public void setFill(ItemStack stack, Fluid type, short fill) {
        if (!stack.hasTag()) initNBT(stack);
        stack.getTag().putString("type", BuiltInRegistries.FLUID.getKey(type).toString());
        stack.getTag().putShort("fill", fill);
    }

    @Override
    public int getFill(ItemStack stack) {
        if (!stack.hasTag()) initNBT(stack);
        return stack.getTag().getShort("fill");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.hasTag()) initNBT(stack);

        if (!world.isClientSide) {
            if (this.getFill(stack) == 0) {
                int a;
                if (this == ModItems.PIPETTE_LABORATORY.get())
                    a = !player.isShiftKeyDown() ? Math.min(this.getCapacity(stack) + 1, 50) : Math.max(this.getCapacity(stack) - 1, 1);
                else
                    a = !player.isShiftKeyDown() ? Math.min(this.getCapacity(stack) + 50, 1_000) : Math.max(this.getCapacity(stack) - 50, 50);

                stack.getTag().putShort("capacity", (short) a);
                player.sendSystemMessage(Component.literal(a + "/" + this.getMaxFill() + "mB"));
            } else {
                player.sendSystemMessage(Component.translatable("desc.item.pipette.noEmpty"));
            }
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        if (this == ModItems.PIPETTE_LABORATORY.get()) {
            list.add(Component.translatable("desc.item.pipette.corrosive"));
            list.add(Component.translatable("desc.item.pipette.laboratory"));
        }
        if (this == ModItems.PIPETTE_BORON.get())
            list.add(Component.translatable("desc.item.pipette.corrosive"));
        if (this == ModItems.PIPETTE.get())
            list.add(Component.translatable("desc.item.pipette.noCorrosive"));
        list.add(Component.literal("Fluid: ").append(fluidName(this.getType(stack))));
        list.add(Component.literal("Amount: " + this.getFill(stack) + "/" + this.getCapacity(stack) + "mB (" + this.getMaxFill() + "mB)"));
    }

    private static Component fluidName(Fluid fluid) {
        if (fluid == Fluids.EMPTY) return Component.translatable("hbmfluid.none");
        //? if forge {
        return Component.translatable(fluid.getFluidType().getDescriptionId());
        //?} else {
        /*var key = BuiltInRegistries.FLUID.getKey(fluid);
        return Component.translatable("fluid." + key.getNamespace() + "." + key.getPath());
        *///?}
    }

    @Override
    public boolean acceptsFluid(Fluid type, ItemStack stack) {
        return (type == this.getType(stack) || this.getFill(stack) == 0) && !FluidType.hasTrait(type, FT_Amat.class);
    }

    @Override
    public int tryFill(Fluid type, int amount, ItemStack stack) {

        if (!acceptsFluid(type, stack))
            return amount;

        if (this.getFill(stack) == 0)
            this.setFill(stack, type, (short) 0);

        int req = this.getCapacity(stack) - this.getFill(stack);
        int toFill = Math.min(req, amount);

        this.setFill(stack, type, (short) (this.getFill(stack) + toFill));

        // fizzling checks
        if (this.getFill(stack) > 0 && willFizzle(type)) {
            stack.setCount(0);
        }

        return amount - toFill;
    }

    public boolean willFizzle(Fluid type) {
        if (this != ModItems.PIPETTE.get()) return false;
        return FluidType.hasTrait(type, FT_Corrosive.class) && type != ModFluids.PEROXIDE.getSource();
    }

    @Override
    public boolean providesFluid(Fluid type, ItemStack stack) {
        return this.getType(stack) == type;
    }

    @Override
    public int tryEmpty(Fluid type, int amount, ItemStack stack) {
        if (providesFluid(type, stack)) {
            int toUnload = Math.min(amount, this.getFill(stack));
            this.setFill(stack, type, (short) (this.getFill(stack) - toUnload));
            if (this.getFill(stack) == 0)
                this.setFill(stack, Fluids.EMPTY, (short) 0);
            return toUnload;
        }
        return amount;
    }

    @Override
    public @Nullable Fluid getFirstFluidType(ItemStack stack) {
        return this.getType(stack);
    }

    /** {@code getColorFromItemStack}: Pass 1 = Fluessigkeitsfarbe. */
    public int getColor(ItemStack stack, int pass) {
        if (pass == 0) return 0xffffff;
        Fluid f = getType(stack);
        int j = f == Fluids.EMPTY ? -1 : HbmFluidRegistry.getTintColor(f);
        return j < 0 ? 0xffffff : j;
    }

    //? if forge {
    @Override
    public net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new PipetteCapability(stack);
    }

    /** Anbindung an Tanks/Lader des Ports (IFluidHandlerItem ueber tryFill/tryEmpty). */
    private static class PipetteCapability implements net.minecraftforge.common.capabilities.ICapabilityProvider,
            net.minecraftforge.fluids.capability.IFluidHandlerItem {

        private final ItemStack stack;
        private final net.minecraftforge.common.util.LazyOptional<net.minecraftforge.fluids.capability.IFluidHandlerItem> opt;

        PipetteCapability(ItemStack stack) {
            this.stack = stack;
            this.opt = net.minecraftforge.common.util.LazyOptional.of(() -> this);
        }

        private ItemPipette item() { return (ItemPipette) stack.getItem(); }

        @Override
        public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
            return net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM.orEmpty(cap, opt);
        }

        @Override public ItemStack getContainer() { return stack; }
        @Override public int getTanks() { return 1; }

        @Override
        public net.minecraftforge.fluids.FluidStack getFluidInTank(int tank) {
            int fill = item().getFill(stack);
            return fill <= 0 ? net.minecraftforge.fluids.FluidStack.EMPTY : new net.minecraftforge.fluids.FluidStack(item().getType(stack), fill);
        }

        @Override public int getTankCapacity(int tank) { return item().getCapacity(stack); }

        @Override
        public boolean isFluidValid(int tank, net.minecraftforge.fluids.FluidStack fs) {
            return item().acceptsFluid(fs.getFluid(), stack);
        }

        @Override
        public int fill(net.minecraftforge.fluids.FluidStack resource, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction action) {
            if (resource.isEmpty() || !item().acceptsFluid(resource.getFluid(), stack)) return 0;
            if (action.simulate()) {
                return Math.min(resource.getAmount(), item().getCapacity(stack) - item().getFill(stack));
            }
            return resource.getAmount() - item().tryFill(resource.getFluid(), resource.getAmount(), stack);
        }

        @Override
        public net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.FluidStack resource, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction action) {
            if (resource.isEmpty() || resource.getFluid() != item().getType(stack)) return net.minecraftforge.fluids.FluidStack.EMPTY;
            return drain(resource.getAmount(), action);
        }

        @Override
        public net.minecraftforge.fluids.FluidStack drain(int maxDrain, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction action) {
            Fluid type = item().getType(stack);
            int fill = item().getFill(stack);
            if (fill <= 0 || type == Fluids.EMPTY) return net.minecraftforge.fluids.FluidStack.EMPTY;
            int amount = Math.min(maxDrain, fill);
            if (action.execute()) item().tryEmpty(type, amount, stack);
            return new net.minecraftforge.fluids.FluidStack(type, amount);
        }
    }
    //?}
}
