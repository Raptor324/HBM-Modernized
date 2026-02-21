package com.hbm_m.block.entity.custom.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.menu.MachineChemicalPlantMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

// Добавляем MenuProvider для работы GUI
public class MachineChemicalPlantBlockEntity extends BlockEntity implements MenuProvider {

    private float anim = 0.0F;
    private float prevAnim = 0.0F;
    private int progress = 0;
    private int maxProgress = 100;

    // Инвентарь:
    // 0-3: Item Input (2x2 oben links)
    // 4-7: Fluid Container Input (2x2 mit Kanister markierung)
    // 8-11: Item Output (2x2 oben rechts)
    // 12-15: Fluid Container Output (2x2)
    // 16-19: Template/Recipe slots (4 unten)
    // 20: Battery slot
    // 21: Template slot
    private final ItemStackHandler itemHandler = new ItemStackHandler(22) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    // Бак для жидкостей
    private final FluidTank fluidTank = new FluidTank(8_000) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };

    // Container data for syncing to client
    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            FluidStack stack = fluidTank.getFluid();
            switch (index) {
                case 0: return stack.getAmount();
                case 1: return stack.isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(stack.getFluid());
                case 2: return progress;
                case 3: return maxProgress;
                default: return 0;
            }
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 2: progress = value; break;
                case 3: maxProgress = value; break;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    private final LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.of(() -> itemHandler);
    private final LazyOptional<IFluidHandler> lazyFluidHandler = LazyOptional.of(() -> fluidTank);

    public MachineChemicalPlantBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHEMICAL_PLANT_BE.get(), pos, state);
    }

    public void drops() {
        if (this.level != null) {
            // Проходим по всем слотам ItemStackHandler
            for (int i = 0; i < itemHandler.getSlots(); i++) {
                net.minecraft.world.item.ItemStack stack = itemHandler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    // Выбрасываем каждый стак в мир
                    Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
                }
            }
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineChemicalPlantBlockEntity entity) {
        entity.prevAnim = entity.anim;

        if (level.isClientSide) {
            // Анимация вращения/работы
            entity.anim += 0.05F;
            if (entity.anim > (float) (Math.PI * 2.0)) {
                entity.anim -= (float) (Math.PI * 2.0);
            }
        }
    }

    // --- Интерфейс MenuProvider ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.chemical_plant");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineChemicalPlantMenu(containerId, playerInventory, this, this.data);
    }

    // --- Работа с данными ---

    public float getAnim(float partialTicks) {
        return prevAnim + (anim - prevAnim) * partialTicks;
    }

    public FluidStack getFluid() {
        return fluidTank.getFluid();
    }

    public float getFluidFillFraction() {
        if (fluidTank.getCapacity() <= 0) return 0.0F;
        return Math.min(1.0F, Math.max(0.0F, fluidTank.getFluidAmount() / (float) fluidTank.getCapacity()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) {
            CompoundTag invTag = tag.getCompound("Inventory");
            // Migration: Check if old inventory has fewer slots
            int savedSize = invTag.contains("Size") ? invTag.getInt("Size") : 0;
            if (savedSize > 0 && savedSize < 22) {
                // Old inventory format - load items into new handler manually
                ItemStackHandler tempHandler = new ItemStackHandler(savedSize);
                tempHandler.deserializeNBT(invTag);
                // Copy items from old handler to new (up to the smaller size)
                for (int i = 0; i < Math.min(savedSize, itemHandler.getSlots()); i++) {
                    itemHandler.setStackInSlot(i, tempHandler.getStackInSlot(i));
                }
            } else {
                itemHandler.deserializeNBT(invTag);
            }
        }
        if (tag.contains("Fluid")) {
            fluidTank.readFromNBT(tag.getCompound("Fluid"));
        }
        anim = tag.getFloat("Anim");
        prevAnim = tag.getFloat("PrevAnim");
        progress = tag.getInt("Progress");
        maxProgress = tag.getInt("MaxProgress");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", itemHandler.serializeNBT());
        tag.put("Fluid", fluidTank.writeToNBT(new CompoundTag()));
        tag.putFloat("Anim", anim);
        tag.putFloat("PrevAnim", prevAnim);
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
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
}