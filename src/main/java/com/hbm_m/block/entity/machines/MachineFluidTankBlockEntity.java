package com.hbm_m.block.entity.machines;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
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
import net.minecraft.resources.ResourceLocation;
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
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;

public class MachineFluidTankBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_ID_IN = 0;
    public static final int SLOT_ID_OUT = 1;
    public static final int SLOT_LOAD_IN = 2;
    public static final int SLOT_LOAD_OUT = 3;
    public static final int SLOT_UNLOAD_IN = 4;
    public static final int SLOT_UNLOAD_OUT = 5;

    public static final int MODES = 4;
    private static final int TANK_CAPACITY = 256000;

    private short mode = 0; 
    public boolean hasExploded = false;
    public boolean onFire = false;
    private byte lastRedstone = 0;
    private int age = 0;

    @Nullable
    private Fluid filterFluid = null;

    private final FluidTank fluidTank;
    private final ItemStackHandler itemHandler;
    protected final ContainerData data;

    private final LazyOptional<IItemHandler> lazyItemHandler;
    private final LazyOptional<IFluidHandler> lazyFluidHandler;

    public static final ModelProperty<ResourceLocation> FLUID_TEXTURE_PROPERTY = new ModelProperty<>();

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
                    case 2: mode = (short) value; break;
                    case 3: hasExploded = value == 1; break;
                    case 4: onFire = value == 1; break;
                }
            }
            @Override
            public int getCount() { return 6; }
        };

        this.lazyItemHandler = LazyOptional.of(() -> itemHandler);
        this.lazyFluidHandler = LazyOptional.of(() -> new NetworkFluidHandlerWrapper(this));
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineFluidTankBlockEntity entity) {
        if (level.isClientSide) return;

        if (!entity.hasExploded) {
            entity.age++;
            if (entity.age >= 20) {
                entity.age = 0;
                entity.setChanged();
            }

            if (entity.fluidTank.getFill() > 0) {
                Fluid type = entity.fluidTank.getTankType();
                if (FluidTraitManager.hasTrait(type, FT_Amat.class)) {
                    entity.explode();
                    entity.fluidTank.fill(0);
                    level.explode(null, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 5F, Level.ExplosionInteraction.BLOCK);
                }
                FT_Corrosive corrosive = FluidTraitManager.getTrait(type, FT_Corrosive.class);
                if (corrosive != null && corrosive.isHighlyCorrosive()) {
                    entity.explode();
                }
            }

            if (entity.hasExploded) {
                entity.updateLeak(entity.calculateLeakAmount());
                return;
            }

            // --- ОБРАБОТКА ИНВЕНТАРЯ ---
            boolean changed = false;
            ItemStack[] slotsArray = entity.getSlotsArray();

            // 1. Идентификация жидкости (Фильтр)
            if (entity.fluidTank.setType(SLOT_ID_IN, SLOT_ID_OUT, slotsArray)) {
                changed = true;
            }

            // 2. TANK -> ITEM (Filling Item)
            if (entity.fluidTank.unloadTank(SLOT_UNLOAD_IN, SLOT_UNLOAD_OUT, slotsArray)) {
                changed = true;
            }

            // 3. ITEM -> TANK (Emptying Item)
            if (entity.fluidTank.loadTank(SLOT_LOAD_IN, SLOT_LOAD_OUT, slotsArray)) {
                changed = true;
            }

            if (changed) {
                // Применяем изменения слотов обратно в Handler, так как FluidTank работает с массивом
                entity.applySlotsArray(slotsArray);
                entity.setChanged();
                level.sendBlockUpdated(pos, state, state, 3);
            }

        } else {
            if (entity.fluidTank.getFill() > 0) {
                entity.updateLeak(entity.calculateLeakAmount());
            }
        }

        byte comp = entity.getComparatorPower();
        if (comp != entity.lastRedstone) {
            entity.lastRedstone = comp;
            level.updateNeighborsAt(pos, state.getBlock());
            entity.setChanged();
        }
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt) {
        // Сохраняем старую жидкость для проверки
        Fluid oldFluid = getFilterFluid();
        Fluid oldTankFluid = fluidTank.getTankType();

        // Этот супер-вызов применит новые данные из пакета (вызовет метод load)
        super.onDataPacket(net, pkt);

        if (level != null && level.isClientSide) {
            Fluid newFluid = getFilterFluid();
            Fluid newTankFluid = fluidTank.getTankType();

            // Проверяем, изменилась ли жидкость, чтобы не перерисовывать чанк лишний раз
            if (oldFluid != newFluid || oldTankFluid != newTankFluid) {
                // Говорим клиенту обновить ModelData
                requestModelDataUpdate();
                // Флаг 8 (Block.UPDATE_CLIENTS) заставляет клиентскую сторону перестроить меш чанка
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 8);
            }
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 8);
        }
    }

    public ResourceLocation getTankTextureLocation() {
        Fluid fluid = fluidTank.getTankType();
        if (fluid == null || fluid == Fluids.EMPTY) {
            fluid = getFilterFluid();
        }
    
        if (fluid == null || fluid == Fluids.EMPTY) {
            return ResourceLocation.fromNamespaceAndPath("hbm_m", "block/tank/tank_none"); 
        }
    
        ResourceLocation typeId = net.minecraftforge.registries.ForgeRegistries.FLUID_TYPES.get().getKey(fluid.getFluidType());
        String fluidName = typeId != null ? typeId.getPath() : "none";
        
        return ResourceLocation.fromNamespaceAndPath("hbm_m", "block/tank/tank_" + fluidName);
    }

    @Override
    public @NotNull ModelData getModelData() {
        return ModelData.builder()
                .with(FLUID_TEXTURE_PROPERTY, getTankTextureLocation())
                .build();
    }

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
            return current; 
        } else if (FluidTraitManager.hasTrait(type, FT_Gaseous.class)) {
            return Math.min(current, max / 100); 
        } else {
            return Math.min(current, max / 10000); 
        }
    }

    private void updateLeak(int amount) {
        if (!hasExploded || amount <= 0) return;

        fluidTank.fill(Math.max(0, fluidTank.getFill() - amount));
        Fluid type = fluidTank.getTankType();

        if (FluidTraitManager.hasTrait(type, FT_Amat.class)) {
            level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5, 5F, ExplosionInteraction.BLOCK);
        } else if (FluidTraitManager.hasTrait(type, FT_Flammable.class) && onFire) {
            AABB fireBox = new AABB(worldPosition).inflate(2.5, 5.0, 2.5);
            List<LivingEntity> affected = level.getEntitiesOfClass(LivingEntity.class, fireBox);
            for (LivingEntity e : affected) {
                e.setSecondsOnFire(5);
            }
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.FLAME, worldPosition.getX() + level.random.nextDouble(), worldPosition.getY() + 0.5 + level.random.nextDouble(), worldPosition.getZ() + level.random.nextDouble(), 3, 0.1, 0.1, 0.1, 0.05);
            }
        } else if (FluidTraitManager.hasTrait(type, FT_Gaseous.class)) {
            if (level.getGameTime() % 5 == 0 && level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, 5, 0.2, 0.5, 0.2, 0.05);
            }
        }
    }

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
    
    private ItemStack[] getSlotsArray() {
        ItemStack[] arr = new ItemStack[6];
        for (int i = 0; i < 6; i++) arr[i] = itemHandler.getStackInSlot(i);
        return arr;
    }

    private void applySlotsArray(ItemStack[] arr) {
        for (int i = 0; i < 6; i++) itemHandler.setStackInSlot(i, arr[i]);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(3.0D);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        fluidTank.readFromNBT(tag, "tank");
        mode = tag.getShort("mode");
        hasExploded = tag.getBoolean("exploded");
        onFire = tag.getBoolean("onFire");

        if (tag.contains("filterFluid")) {
            filterFluid = ForgeRegistries.FLUIDS.getValue(ResourceLocation.parse(tag.getString("filterFluid")));
        } else {
            filterFluid = null;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", itemHandler.serializeNBT());
        fluidTank.writeToNBT(tag, "tank");
        tag.putShort("mode", mode);
        tag.putBoolean("exploded", hasExploded);
        tag.putBoolean("onFire", onFire);

        if (filterFluid != null && filterFluid != Fluids.EMPTY) {
            ResourceLocation key = ForgeRegistries.FLUIDS.getKey(filterFluid);
            if (key != null) {
                tag.putString("filterFluid", key.toString());
            }
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
        return Component.translatable(ModBlocks.FLUID_TANK.get().getDescriptionId().toString());
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
            fluidTank.fill(0);
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    @Nullable
    public Fluid getFilterFluid() {
        return filterFluid;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

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
            if (entity.hasExploded || entity.mode == 2 || entity.mode == 3) return 0;
            return internal.fill(resource, action);
        }

        @NotNull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
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