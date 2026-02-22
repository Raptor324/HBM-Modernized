package com.hbm_m.block.entity.custom.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.item.IItemFluidIdentifier;
import com.hbm_m.menu.MachineFluidTankMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public class MachineFluidTankBlockEntity extends BlockEntity implements MenuProvider {

    // Слоты (как в оригинале 1.7.10)
    public static final int SLOT_ID_IN = 0;      // Fluid identifier (input)
    public static final int SLOT_ID_OUT = 1;     // Fluid identifier (output)
    public static final int SLOT_LOAD_IN = 2;    // Load input (заливка в танк)
    public static final int SLOT_LOAD_OUT = 3;   // Load output
    public static final int SLOT_UNLOAD_IN = 4;  // Unload input (слив из танка)
    public static final int SLOT_UNLOAD_OUT = 5; // Unload output

    public static final int MODES = 4;
    private static final int TANK_CAPACITY = 256000; // 256 ведер

    private short mode = 0;
    @Nullable
    private Fluid filterFluid = null;

    private final FluidTank fluidTank;
    private final ItemStackHandler itemHandler;
    protected final ContainerData data;

    private final LazyOptional<IItemHandler> lazyItemHandler;
    private final LazyOptional<IFluidHandler> lazyFluidHandler;

    public MachineFluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_TANK_BE.get(), pos, state);

        this.fluidTank = new FluidTank(TANK_CAPACITY) {
            @Override
            protected void onContentsChanged() {
                setChanged();
                if (level != null && !level.isClientSide) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                if (stack.isEmpty()) return false;
                Fluid f = stack.getFluid();
                if (f == null || f == Fluids.EMPTY) return false;
                if (filterFluid != null && filterFluid != Fluids.EMPTY && f != filterFluid) return false;
                FluidStack current = getFluid();
                if (!current.isEmpty() && !current.getFluid().isSame(f)) return false;
                return true;
            }
        };

        this.itemHandler = new ItemStackHandler(6) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return slot != SLOT_ID_OUT && slot != SLOT_LOAD_OUT && slot != SLOT_UNLOAD_OUT;
            }
        };

        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                FluidStack stack = fluidTank.getFluid();
                switch (index) {
                    case 0:
                        return stack.getAmount();
                    case 1:
                        return stack.isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(stack.getFluid());
                    case 2:
                        return mode;
                    case 3:
                        return filterFluid == null || filterFluid == Fluids.EMPTY ? -1 : BuiltInRegistries.FLUID.getId(filterFluid);
                    default:
                        return 0;
                }
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return 4;
            }
        };

        this.lazyItemHandler = LazyOptional.of(() -> itemHandler);
        this.lazyFluidHandler = LazyOptional.of(() -> fluidTank);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineFluidTankBlockEntity entity) {
        if (level.isClientSide) return;

        entity.processIdentifierSlot();
        entity.processLeftSlots();
        entity.processRightSlots();
    }

    private void processIdentifierSlot() {
        ItemStack idStack = itemHandler.getStackInSlot(SLOT_ID_IN);
        if (idStack.isEmpty() || !(idStack.getItem() instanceof IItemFluidIdentifier idItem)) return;

        Fluid newType = idItem.getType(level, worldPosition, idStack);
        if (newType == null || newType == Fluids.EMPTY) return;

        if (filterFluid != newType) {
            filterFluid = newType;
            fluidTank.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);

            ItemStack outStack = itemHandler.getStackInSlot(SLOT_ID_OUT);
            if (SLOT_ID_IN == SLOT_ID_OUT) {
                // in == out: just set type, keep identifier
            } else if (outStack.isEmpty()) {
                itemHandler.setStackInSlot(SLOT_ID_OUT, idStack.copy());
                itemHandler.setStackInSlot(SLOT_ID_IN, ItemStack.EMPTY);
            } else {
                // output occupied, don't move
            }
            setChanged();
        }
    }

    private void processLeftSlots() {
        ItemStack inputStack = itemHandler.getStackInSlot(SLOT_LOAD_IN);
        if (inputStack.isEmpty()) return;

        inputStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(handler -> {
            FluidStack fluidInItem = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);

            if (fluidInItem != null && !fluidInItem.isEmpty()) {
                int filledAmount = fluidTank.fill(fluidInItem, IFluidHandler.FluidAction.SIMULATE);

                if (filledAmount > 0) {
                    fluidTank.fill(fluidInItem, IFluidHandler.FluidAction.EXECUTE);
                    handler.drain(filledAmount, IFluidHandler.FluidAction.EXECUTE);

                    ItemStack container = handler.getContainer();
                    boolean isInfiniteSource = !container.isEmpty() &&
                            ItemStack.isSameItemSameTags(inputStack, container);

                    if (!isInfiniteSource) {
                        ItemStack outputStack = itemHandler.getStackInSlot(SLOT_LOAD_OUT);
                        if (!outputStack.isEmpty() &&
                                (!ItemStack.isSameItemSameTags(outputStack, container) ||
                                        outputStack.getCount() >= outputStack.getMaxStackSize())) {
                            return;
                        }

                        inputStack.shrink(1);

                        if (!container.isEmpty()) {
                            if (outputStack.isEmpty()) {
                                itemHandler.setStackInSlot(SLOT_LOAD_OUT, container);
                            } else if (ItemStack.isSameItemSameTags(outputStack, container) && outputStack.getCount() < outputStack.getMaxStackSize()) {
                                outputStack.grow(1);
                            }
                        }
                    }
                }
            }
        });
    }

    private void processRightSlots() {
        ItemStack inputStack = itemHandler.getStackInSlot(SLOT_UNLOAD_IN);
        if (inputStack.isEmpty()) return;

        ItemStack outputStack = itemHandler.getStackInSlot(SLOT_UNLOAD_OUT);
        if (!outputStack.isEmpty() && outputStack.getCount() >= outputStack.getMaxStackSize()) return;

        FluidStack fluidInTank = fluidTank.getFluid();
        if (fluidInTank.isEmpty()) return;

        inputStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(handler -> {
            int filled = handler.fill(fluidInTank, IFluidHandler.FluidAction.SIMULATE);

            ItemStack currentContainer = handler.getContainer();
            boolean isReusableContainer = !currentContainer.isEmpty() &&
                    ItemStack.isSameItem(inputStack, currentContainer);

            if (filled > 0) {
                FluidStack drainedFromTank = fluidTank.drain(filled, IFluidHandler.FluidAction.SIMULATE);

                if (drainedFromTank.getAmount() == filled) {
                    fluidTank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                    handler.fill(drainedFromTank, IFluidHandler.FluidAction.EXECUTE);

                    ItemStack filledContainer = handler.getContainer();

                    if (isReusableContainer) {
                        // Reusable container stays in slot
                    } else {
                        inputStack.shrink(1);

                        if (outputStack.isEmpty()) {
                            itemHandler.setStackInSlot(SLOT_UNLOAD_OUT, filledContainer);
                        } else if (ItemStack.isSameItemSameTags(outputStack, filledContainer) && outputStack.getCount() < outputStack.getMaxStackSize()) {
                            outputStack.grow(1);
                        }
                    }
                }
            } else if (isReusableContainer) {
                if (outputStack.isEmpty()) {
                    itemHandler.setStackInSlot(SLOT_UNLOAD_OUT, inputStack.copy());
                    inputStack.shrink(1);
                }
            }
        });
    }

    public void handleModeButton() {
        mode = (short) ((mode + 1) % MODES);
        setChanged();
    }

    /** Вызывается при shift+click по блоку с IItemFluidIdentifier в руке */
    public void setFilterFromIdentifier(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IItemFluidIdentifier idItem)) return;

        Fluid newType = idItem.getType(level, worldPosition, stack);
        if (newType == null || newType == Fluids.EMPTY) return;

        if (filterFluid != newType) {
            filterFluid = newType;
            fluidTank.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
            setChanged();
        }
    }

    public short getMode() {
        return mode;
    }

    @Nullable
    public Fluid getFilterFluid() {
        return filterFluid;
    }

    public FluidTank getFluidTank() {
        return fluidTank;
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        fluidTank.readFromNBT(tag.getCompound("Fluid"));
        mode = tag.getShort("mode");
        if (tag.contains("filterFluid")) {
            ResourceLocation rl = ResourceLocation.tryParse(tag.getString("filterFluid"));
            if (rl != null) {
                filterFluid = BuiltInRegistries.FLUID.get(rl);
                if (filterFluid == Fluids.EMPTY) filterFluid = null;
            } else {
                filterFluid = null;
            }
        } else {
            filterFluid = null;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", itemHandler.serializeNBT());
        tag.put("Fluid", fluidTank.writeToNBT(new CompoundTag()));
        tag.putShort("mode", mode);
        if (filterFluid != null && filterFluid != Fluids.EMPTY) {
            tag.putString("filterFluid", BuiltInRegistries.FLUID.getKey(filterFluid).toString());
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return lazyFluidHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        lazyItemHandler.invalidate();
        lazyFluidHandler.invalidate();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm_m.fluid_tank");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MachineFluidTankMenu(id, inventory, this, this.data);
    }
}
