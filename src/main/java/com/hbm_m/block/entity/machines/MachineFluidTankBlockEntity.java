package com.hbm_m.block.entity.machines;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Corrosive;
import com.hbm_m.inventory.fluid.trait.FT_Flammable;
import com.hbm_m.inventory.fluid.trait.FluidTraitManager;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Amat;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous;
import com.hbm_m.inventory.menu.MachineFluidTankMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public class MachineFluidTankBlockEntity extends BlockEntity implements MenuProvider {

    // Слоты
    public static final int SLOT_ID_IN = 0;
    public static final int SLOT_ID_OUT = 1;
    public static final int SLOT_LOAD_IN = 2;
    public static final int SLOT_LOAD_OUT = 3;
    public static final int SLOT_UNLOAD_IN = 4;
    public static final int SLOT_UNLOAD_OUT = 5;

    public static final int MODES = 4;
    private static final int TANK_CAPACITY = 256000;

    
    private short mode = 0; // 0 = In, 1 = Buffer, 2 = Out, 3 = None
    public boolean hasExploded = false;
    public boolean onFire = false;
    private byte lastRedstone = 0;
    private int age = 0;

    @Nullable
    private Fluid filterFluid = null;

    // Используем НАШ кастомный танк, а не форджевский
    private final FluidTank fluidTank;
    private final ItemStackHandler itemHandler;
    protected final ContainerData data;

    private final LazyOptional<IItemHandler> lazyItemHandler;
    private final LazyOptional<IFluidHandler> lazyFluidHandler;

    public MachineFluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_TANK_BE.get(), pos, state);

        this.fluidTank = new FluidTank(Fluids.EMPTY, TANK_CAPACITY);

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
                switch (index) {
                    case 0: return fluidTank.getFill();
                    case 1: return BuiltInRegistries.FLUID.getId(fluidTank.getTankType());
                    case 2: return mode;
                    case 3: return hasExploded ? 1 : 0;
                    case 4: return onFire ? 1 : 0;
                    case 5: return fluidTank.getPressure();
                    default: return 0;
                }
            }
            @Override
            public void set(int index, int value) {
                switch (index) {
                    // Клиентская синхронизация опускается для краткости, она делается автоматически через меню
                    case 2: mode = (short) value; break;
                    case 3: hasExploded = value == 1; break;
                    case 4: onFire = value == 1; break;
                }
            }
            @Override
            public int getCount() { return 6; }
        };

        this.lazyItemHandler = LazyOptional.of(() -> itemHandler);
        // Обёртка с учетом модов трубы
        this.lazyFluidHandler = LazyOptional.of(() -> new NetworkFluidHandlerWrapper(this));
    }

    // ================================================================================= //
    // ОСНОВНОЙ ЦИКЛ (TICK)
    // ================================================================================= //

    public static void tick(Level level, BlockPos pos, BlockState state, MachineFluidTankBlockEntity entity) {
        if (level.isClientSide) return;

        if (!entity.hasExploded) {
            entity.age++;
            if (entity.age >= 20) {
                entity.age = 0;
                entity.setChanged(); // Синхронизация раз в секунду
            }

            // --- ПРОВЕРКА ОПАСНЫХ ЖИДКОСТЕЙ ---
            if (entity.fluidTank.getFill() > 0) {
                Fluid type = entity.fluidTank.getTankType();
                
                // Антиматерия мгновенно взрывается
                if (FluidTraitManager.hasTrait(type, FT_Amat.class)) {
                    entity.explode();
                    entity.fluidTank.fill(0);
                    // Заглушка под кастомный взрыв HBM
                    level.explode(null, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 5F, Level.ExplosionInteraction.BLOCK);
                }
                
                // Сильно коррозийная кислота разъедает бак
                FT_Corrosive corrosive = FluidTraitManager.getTrait(type, FT_Corrosive.class);
                if (corrosive != null && corrosive.isHighlyCorrosive()) {
                    entity.explode();
                }
            }

            // Если взорвалось в этом тике - прерываем обычную обработку
            if (entity.hasExploded) {
                entity.updateLeak(entity.calculateLeakAmount());
                return;
            }

            // --- ОБРАБОТКА ПРЕДМЕТОВ (использует логику NTM 1.7.10) ---
            ItemStack[] slotsArray = entity.getSlotsArray();
            
            boolean changed = false;
            changed |= entity.fluidTank.setType(SLOT_ID_IN, SLOT_ID_OUT, slotsArray);
            changed |= entity.fluidTank.loadTank(SLOT_LOAD_IN, SLOT_LOAD_OUT, slotsArray);
            changed |= entity.fluidTank.unloadTank(SLOT_UNLOAD_IN, SLOT_UNLOAD_OUT, slotsArray);
            
            if (changed) {
                entity.applySlotsArray(slotsArray);
                entity.setChanged();
            }

        } else {
            // --- ЛОГИКА УТЕЧКИ ИЗ ВЗОРВАННОГО БАКА ---
            if (entity.fluidTank.getFill() > 0) {
                entity.updateLeak(entity.calculateLeakAmount());
            }
        }

        // --- ЛОГИКА КОМПАРАТОРА ---
        byte comp = entity.getComparatorPower();
        if (comp != entity.lastRedstone) {
            entity.lastRedstone = comp;
            level.updateNeighborsAt(pos, state.getBlock());
            entity.setChanged();
        }
    }

    // ================================================================================= //
    // ВЗРЫВЫ, УТЕЧКИ И ПОЖАРЫ
    // ================================================================================= //

    public void explode() {
        if (this.hasExploded) return;
        this.hasExploded = true;
        this.onFire = FluidTraitManager.hasTrait(fluidTank.getTankType(), FT_Flammable.class);
        this.setChanged();
    }

    public void repair() {
        this.hasExploded = false;
        this.onFire = false;
        this.setChanged();
    }

    private int calculateLeakAmount() {
        Fluid type = fluidTank.getTankType();
        int max = fluidTank.getMaxFill();
        int current = fluidTank.getFill();
        
        if (FluidTraitManager.hasTrait(type, FT_Amat.class)) {
            return current; // Антиматерия вытекает мгновенно
        } else if (FluidTraitManager.hasTrait(type, FT_Gaseous.class)) {
            return Math.min(current, max / 100); // Газ уходит быстро
        } else {
            return Math.min(current, max / 10000); // Жидкость вытекает медленно
        }
    }

    private void updateLeak(int amount) {
        if (!hasExploded || amount <= 0) return;

        fluidTank.fill(Math.max(0, fluidTank.getFill() - amount));
        Fluid type = fluidTank.getTankType();

        // 1. Антиматерия
        if (FluidTraitManager.hasTrait(type, FT_Amat.class)) {
            level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5, 5F, ExplosionInteraction.BLOCK);
        } 
        // 2. Горючие материалы (Пожар)
        else if (FluidTraitManager.hasTrait(type, FT_Flammable.class) && onFire) {
            AABB fireBox = new AABB(worldPosition).inflate(2.5, 5.0, 2.5);
            List<LivingEntity> affected = level.getEntitiesOfClass(LivingEntity.class, fireBox);
            for (LivingEntity e : affected) {
                e.setSecondsOnFire(5);
            }
            
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.FLAME, worldPosition.getX() + level.random.nextDouble(), worldPosition.getY() + 0.5 + level.random.nextDouble(), worldPosition.getZ() + level.random.nextDouble(), 3, 0.1, 0.1, 0.1, 0.05);
            }
            
            // TODO: Подключить PollutionHandler (FluidReleaseType.BURN) когда он будет готов
        } 
        // 3. Газы
        else if (FluidTraitManager.hasTrait(type, FT_Gaseous.class)) {
            if (level.getGameTime() % 5 == 0 && level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, 5, 0.2, 0.5, 0.2, 0.05);
            }
            // TODO: Подключить PollutionHandler (FluidReleaseType.SPILL)
        }
    }

    // ================================================================================= //
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ================================================================================= //

    public byte getComparatorPower() {
        if (fluidTank.getFill() == 0) return 0;
        double frac = (double) fluidTank.getFill() / (double) fluidTank.getMaxFill() * 15D;
        return (byte) (Mth.clamp((int) frac + 1, 0, 15));
    }

    public void handleModeButton() {
        mode = (short) ((mode + 1) % MODES);
        setChanged();
    }

    public short getMode() { return mode; }
    public FluidTank getFluidTank() { return fluidTank; }
    
    // Преобразование ItemStackHandler в массив для совместимости с NTM кастомной цистерной
    private ItemStack[] getSlotsArray() {
        ItemStack[] arr = new ItemStack[6];
        for (int i = 0; i < 6; i++) arr[i] = itemHandler.getStackInSlot(i);
        return arr;
    }

    private void applySlotsArray(ItemStack[] arr) {
        for (int i = 0; i < 6; i++) itemHandler.setStackInSlot(i, arr[i] == null ? ItemStack.EMPTY : arr[i]);
    }

    // ================================================================================= //
    // NBT, CAPABILITIES И СЕТЬ
    // ================================================================================= //

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        fluidTank.readFromNBT(tag, "tank");
        mode = tag.getShort("mode");
        hasExploded = tag.getBoolean("exploded");
        onFire = tag.getBoolean("onFire");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", itemHandler.serializeNBT());
        fluidTank.writeToNBT(tag, "tank");
        tag.putShort("mode", mode);
        tag.putBoolean("exploded", hasExploded);
        tag.putBoolean("onFire", onFire);
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
        return Component.translatable("block.hbm_m.machine_fluidtank");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MachineFluidTankMenu(id, inventory, this, this.data);
    }

    public void setFilterFromIdentifier(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof com.hbm_m.item.IItemFluidIdentifier idItem)) return;

        Fluid newType = idItem.getType(level, worldPosition, stack);
        if (newType == null || newType == Fluids.EMPTY) return;

        boolean isSameType = filterFluid != null && 
                filterFluid != Fluids.EMPTY && 
                filterFluid.getFluidType().equals(newType.getFluidType());
        
        if (!isSameType) {
            filterFluid = newType;
            // Очищаем бак при смене типа (как в оригинале)
            fluidTank.fill(0);
            setChanged();
        }
    }

    @Nullable
    public Fluid getFilterFluid() {
        return filterFluid;
    }

    // ================================================================================= //
    // Обертка для работы с трубами (Учитывает режимы и поломки)
    // ================================================================================= //

    private class NetworkFluidHandlerWrapper implements IFluidHandler {
        private final MachineFluidTankBlockEntity entity;
        private IFluidHandler internal;

        public NetworkFluidHandlerWrapper(MachineFluidTankBlockEntity entity) {
            this.entity = entity;
            this.internal = entity.fluidTank.getCapability().orElse(null);
        }

        @Override
        public int getTanks() { return internal.getTanks(); }

        @Override
        public FluidStack getFluidInTank(int tank) { return internal.getFluidInTank(tank); }

        @Override
        public int getTankCapacity(int tank) { return internal.getTankCapacity(tank); }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return internal.isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            // mode 2 (Provide) и 3 (None) блокируют прием жидкости. Взорванный бак не принимает жидкость.
            if (entity.hasExploded || entity.mode == 2 || entity.mode == 3) return 0;
            return internal.fill(resource, action);
        }

        @NotNull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            // mode 0 (Receive) и 3 (None) блокируют выдачу. Взорванный бак ничего не отдает.
            if (entity.hasExploded || entity.mode == 0 || entity.mode == 3) return FluidStack.EMPTY;
            return internal.drain(resource, action);
        }

        @NotNull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (entity.hasExploded || entity.mode == 0 || entity.mode == 3) return FluidStack.EMPTY;
            return internal.drain(maxDrain, action);
        }
    }
}