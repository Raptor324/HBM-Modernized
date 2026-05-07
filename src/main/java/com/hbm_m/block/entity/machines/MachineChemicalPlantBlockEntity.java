package com.hbm_m.block.entity.machines;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.entity.BaseMachineBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.machines.MachineChemicalPlantBlock;
import com.hbm_m.interfaces.IUpgradeInfoProvider;
import com.hbm_m.inventory.UpgradeManager;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineChemicalPlantMenu;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.item.industrial.ItemBlueprintFolder;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType;
import com.hbm_m.module.machine.MachineModuleChemplant;
import com.hbm_m.recipe.ChemicalPlantRecipe;
import com.hbm_m.sound.ClientSoundBootstrap;

import dev.architectury.fluid.FluidStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
//? if forge {
/*import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
*///?}

//? if fabric {
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import team.reborn.energy.api.EnergyStorage;
//?}

/**
 * Chemical Plant BlockEntity - порт с 1.7.10.
 * 22 слота, 6 FluidTank (3 input, 3 output), энергия.
 * Логика крафтов - заглушка.
 */
public class MachineChemicalPlantBlockEntity extends BaseMachineBlockEntity implements IUpgradeInfoProvider {

    private static final String CHEMICAL_PLANT_SOUND_INSTANCE = "com.hbm_m.sound.ChemicalPlantSoundInstance";

    private static final int SLOT_COUNT = 22;
    private static final int SLOT_BATTERY = 0;
    private static final int SLOT_SCHEMATIC = 1;
    private static final int SLOT_UPGRADE_START = 2;
    private static final int SLOT_UPGRADE_END = 3;
    private static final int SLOT_SOLID_INPUT_START = 4;
    private static final int SLOT_SOLID_INPUT_END = 6;
    private static final int SLOT_SOLID_OUTPUT_START = 7;
    private static final int SLOT_SOLID_OUTPUT_END = 9;
    private static final int SLOT_FLUID_INPUT_START = 10;
    private static final int SLOT_FLUID_INPUT_END = 12;
    private static final int SLOT_FLUID_INPUT_EMPTY_START = 13;
    private static final int SLOT_FLUID_INPUT_EMPTY_END = 15;
    private static final int SLOT_FLUID_OUTPUT_START = 16;
    private static final int SLOT_FLUID_OUTPUT_END = 18;
    private static final int SLOT_FLUID_OUTPUT_EMPTY_START = 19;
    private static final int SLOT_FLUID_OUTPUT_EMPTY_END = 21;

    private static final int TANK_CAPACITY = 24_000;
    private static final long MAX_POWER = 100_000;

    private final FluidTank[] inputTanks = new FluidTank[3];
    private final FluidTank[] outputTanks = new FluidTank[3];

    //? if forge {
    /*private final LazyOptional<IFluidHandler>[] inputTankHandlers = new LazyOptional[3];
    private final LazyOptional<IFluidHandler>[] outputTankHandlers = new LazyOptional[3];
    *///?}

    private MachineModuleChemplant module;
    private final UpgradeManager upgradeManager = new UpgradeManager();

    private static final java.util.Map<UpgradeType, Integer> VALID_UPGRADES = java.util.Map.of(
            UpgradeType.SPEED, 3,
            UpgradeType.POWER, 3,
            UpgradeType.OVERDRIVE, 3
    );

    private float anim = 0.0F;
    private float prevAnim = 0.0F;

    private int renderCooldownTimer = 0;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> module != null ? module.getProgress() : 0;
                case 1 -> module != null ? module.getMaxProgress() : 100;
                case 2 -> (int) (getEnergyStored() & 0xFFFFFFFFL);
                case 3 -> (int) ((getEnergyStored() >> 32) & 0xFFFFFFFFL);
                case 4 -> (int) (getMaxEnergyStored() & 0xFFFFFFFFL);
                case 5 -> (int) ((getMaxEnergyStored() >> 32) & 0xFFFFFFFFL);
                case 6 -> module != null && module.getDidProcess() ? 1 : 0;
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {}
        @Override public int getCount() { return 7; }
    };

    private void updateFrameBlockState() {
        if (level == null) return;
        BlockState st = getBlockState();
        if (!st.hasProperty(MachineChemicalPlantBlock.FRAME)) return;
        boolean frame = !level.getBlockState(worldPosition.above(3)).isAir();
        if (st.getValue(MachineChemicalPlantBlock.FRAME) != frame) {
            level.setBlock(worldPosition, st.setValue(MachineChemicalPlantBlock.FRAME, frame), 3);
        }
    }

    private void syncRenderActiveStub() {
        // TODO: crafting progress → RENDER_ACTIVE + chunk rebuild
    }

    //? if forge {
    /*@Override
            *///?}
    public AABB getRenderBoundingBox() {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof MachineChemicalPlantBlock block)) {
            return new AABB(worldPosition.offset(-1, 0, -1), worldPosition.offset(2, 3, 2));
        }
        var structureHelper = block.getStructureHelper();
        var structureMap = structureHelper.getStructureMap();
        if (structureMap == null || structureMap.isEmpty()) {
            return new AABB(worldPosition.offset(-1, 0, -1), worldPosition.offset(2, 3, 2));
        }
        int minX = 0, minY = 0, minZ = 0;
        int maxX = 0, maxY = 0, maxZ = 0;
        for (BlockPos offset : structureMap.keySet()) {
            minX = Math.min(minX, offset.getX());
            minY = Math.min(minY, offset.getY());
            minZ = Math.min(minZ, offset.getZ());
            maxX = Math.max(maxX, offset.getX());
            maxY = Math.max(maxY, offset.getY());
            maxZ = Math.max(maxZ, offset.getZ());
        }
        double margin = 1.5;
        return new AABB(
                worldPosition.getX() + minX - margin,
                worldPosition.getY() + minY - margin,
                worldPosition.getZ() + minZ - margin,
                worldPosition.getX() + maxX + 1 + margin,
                worldPosition.getY() + maxY + 1 + margin,
                worldPosition.getZ() + maxZ + 1 + margin
        );
    }

    public MachineChemicalPlantBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHEMICAL_PLANT_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, MAX_POWER);

        for (int i = 0; i < 3; i++) {
            inputTanks[i] = new FluidTank(TANK_CAPACITY) {
                @Override
                public void onContentsChanged() {
                    setChanged();
                    if (level != null && !level.isClientSide) {
                        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                    }
                }
            };
            outputTanks[i] = new FluidTank(TANK_CAPACITY) {
                @Override
                public void onContentsChanged() {
                    setChanged();
                    if (level != null && !level.isClientSide) {
                        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                    }
                }
            };
            //? if forge {
            /*inputTankHandlers[i] = inputTanks[i].getCapability();
            outputTankHandlers[i] = outputTanks[i].getCapability();
            *///?}
        }

        this.module = new MachineModuleChemplant(
                this, inventory,
                new int[]{SLOT_SOLID_INPUT_START, SLOT_SOLID_INPUT_START + 1, SLOT_SOLID_INPUT_START + 2},
                new int[]{SLOT_SOLID_OUTPUT_START, SLOT_SOLID_OUTPUT_START + 1, SLOT_SOLID_OUTPUT_START + 2},
                inputTanks, outputTanks);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineChemicalPlantBlockEntity entity) {
        entity.prevAnim = entity.anim;

        if (level.isClientSide) {
            if (entity.module.getDidProcess()) {
                entity.anim++;
            }
            entity.clientTick();
            return;
        }

        entity.updateFrameBlockState();
        entity.syncRenderActiveStub();

        entity.ensureNetworkInitialized();
        entity.chargeFromBattery();
        entity.upgradeManager.checkSlots(entity.inventory, SLOT_UPGRADE_START, SLOT_UPGRADE_END, VALID_UPGRADES);
        entity.transferFluidsFromItems();
        entity.transferFluidsToItems();

        if (level.getGameTime() % 10L == 0L) {
            entity.updateEnergyDelta(entity.getEnergyStored());
        }

        int s = Math.min(entity.upgradeManager.getLevel(UpgradeType.SPEED), 3);
        int p = Math.min(entity.upgradeManager.getLevel(UpgradeType.POWER), 3);
        int o = Math.min(entity.upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3);

        double speed = 1.0 + s / 3.0 + o;
        double pow = 1.0 - 0.25 * p + s + (10.0 / 3.0) * o;

        ItemStack blueprint = entity.inventory.getStackInSlot(SLOT_SCHEMATIC);
        boolean dirty = entity.module.update(level, blueprint, speed, pow);

        if (entity.module.getDidProcess()) {
            entity.renderCooldownTimer = 100;
        } else if (entity.renderCooldownTimer > 0) {
            entity.renderCooldownTimer--;
        }
        boolean shouldRenderActive = entity.renderCooldownTimer > 0;
        if (state.hasProperty(MachineChemicalPlantBlock.RENDER_ACTIVE)) {
            boolean active = state.getValue(MachineChemicalPlantBlock.RENDER_ACTIVE);
            if (active != shouldRenderActive) {
                level.setBlock(pos, state.setValue(MachineChemicalPlantBlock.RENDER_ACTIVE, shouldRenderActive), 3);
            }
        }

        if (dirty) {
            entity.setChanged();
            entity.sendUpdateToClient();
        }
    }

    public MachineModuleChemplant getModule() {
        return module;
    }

    @Override
    public java.util.Map<UpgradeType, Integer> getValidUpgrades() {
        return VALID_UPGRADES;
    }

    //? if forge {
    /*@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
            *///?}
    private void clientTick() {
        ClientSoundBootstrap.updateSound(this, this.module.getDidProcess(), () -> newChemicalPlantSoundInstance());
    }

    private Object newChemicalPlantSoundInstance() {
        try {
            return Class.forName(CHEMICAL_PLANT_SOUND_INSTANCE).getConstructor(BlockPos.class).newInstance(this.getBlockPos());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private void chargeFromBattery() {
        ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
        if (!stack.isEmpty() && stack.getItem() instanceof ItemCreativeBattery) {
            setEnergyStored(getMaxEnergyStored());
            return;
        }
        chargeFromBatterySlot(SLOT_BATTERY);
    }

    private void transferFluidsFromItems() {
        for (int i = 0; i < 3; i++) {
            int fullContainerSlot = SLOT_FLUID_INPUT_START + i;
            int emptyContainerSlot = SLOT_FLUID_INPUT_EMPTY_START + i;
            ItemStack fullContainer = inventory.getStackInSlot(fullContainerSlot);
            if (fullContainer.isEmpty()) continue;
            if (!inventory.getStackInSlot(emptyContainerSlot).isEmpty()) continue;

            //? if forge {
            /*IFluidHandler handler = new GuardedInputTankFluidHandler(this, i);
            var result = FluidUtil.tryEmptyContainer(fullContainer, handler, TANK_CAPACITY, null, true);
                if (result.isSuccess()) {
                    fullContainer.shrink(1);
                    inventory.setStackInSlot(emptyContainerSlot, result.getResult());
                    setChanged();
                }
            *///?}

            //? if fabric {
            Fluid configuredFluid = inputTanks[i].getConfiguredFluid();
            if (configuredFluid == Fluids.EMPTY) continue;

            ItemStack one = fullContainer.copy();
            one.setCount(1);
            Storage<FluidVariant> itemStorage = FluidStorage.ITEM.find(one, ContainerItemContext.withConstant(one));
            if (itemStorage == null) continue;
            StorageView<FluidVariant> view = null;
            for (StorageView<FluidVariant> v : itemStorage) {
                if (!v.isResourceBlank() && v.getAmount() > 0) { view = v; break; }
            }
            if (view == null) continue;

            if (!com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(configuredFluid, view.getResource().getFluid())) continue;

            Storage<FluidVariant> tankStorage = inputTanks[i].getStorage();
            long spaceDroplets = (long) inputTanks[i].getSpaceMb() * 81L; // 81 droplets = 1 mB
            if (spaceDroplets <= 0) continue;

            boolean moved = false;
            try (Transaction tx = Transaction.openOuter()) {
                FluidVariant variant = view.getResource();
                long movable = Math.min(view.getAmount(), spaceDroplets);
                long inserted = tankStorage.insert(variant, movable, tx);
                if (inserted <= 0) {
                    // no-op
                } else {
                    long extracted = itemStorage.extract(variant, inserted, tx);
                    if (extracted == inserted) {
                        tx.commit();
                        moved = true;
                    }
                }
            }
            if (!moved) continue;

            fullContainer.shrink(1);
            inventory.setStackInSlot(emptyContainerSlot, one);
            setChanged();
            //?}
        }
    }

    private void transferFluidsToItems() {
        for (int i = 0; i < 3; i++) {
            int emptyContainerSlot = SLOT_FLUID_OUTPUT_START + i;
            int filledContainerSlot = SLOT_FLUID_OUTPUT_EMPTY_START + i;
            ItemStack emptyContainer = inventory.getStackInSlot(emptyContainerSlot);
            if (emptyContainer.isEmpty()) continue;
            if (!inventory.getStackInSlot(filledContainerSlot).isEmpty()) continue;

            //? if forge {
            /*IFluidHandler handler = outputTanks[i].getCapability().orElse(null);
            if (handler != null) {
                var result = FluidUtil.tryFillContainer(emptyContainer, handler, TANK_CAPACITY, null, true);
                if (result.isSuccess()) {
                    emptyContainer.shrink(1);
                    inventory.setStackInSlot(filledContainerSlot, result.getResult());
                    setChanged();
                }
            }
            *///?}

            //? if fabric {
            if (outputTanks[i].isEmpty()) continue;

            ItemStack one = emptyContainer.copy();
            one.setCount(1);
            Storage<FluidVariant> itemStorage = FluidStorage.ITEM.find(one, ContainerItemContext.withConstant(one));
            if (itemStorage == null) continue;

            Storage<FluidVariant> tankStorage = outputTanks[i].getStorage();
            StorageView<FluidVariant> view = null;
            for (StorageView<FluidVariant> v : tankStorage) {
                if (!v.isResourceBlank() && v.getAmount() > 0) { view = v; break; }
            }
            if (view == null) continue;

            boolean moved = false;
            try (Transaction tx = Transaction.openOuter()) {
                FluidVariant variant = view.getResource();
                long available = view.getAmount();
                long toMove = Math.min(available, (long) TANK_CAPACITY * 81L);
                long inserted = itemStorage.insert(variant, toMove, tx);
                if (inserted <= 0) {
                    // no-op
                } else {
                    long extracted = tankStorage.extract(variant, inserted, tx);
                    if (extracted == inserted) {
                        tx.commit();
                        moved = true;
                    }
                }
            }
            if (!moved) continue;

            emptyContainer.shrink(1);
            inventory.setStackInSlot(filledContainerSlot, one);
            setChanged();
            //?}
        }
    }

    public void drops() {
        if (level == null) return;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
            }
        }
    }

    @Override
    protected Component getDefaultName() { return Component.translatable("container.hbm_m.chemical_plant"); }

    @Override
    public Component getDisplayName() { return getDefaultName(); }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) {
            if (stack.isEmpty()) return false;
            if (stack.getItem() instanceof ItemCreativeBattery) return true;
            return isEnergyProviderItem(stack);
        }
        if (slot == SLOT_SCHEMATIC) {
            return stack.getItem() instanceof ItemBlueprintFolder;
        }
        if (slot >= SLOT_UPGRADE_START && slot <= SLOT_UPGRADE_END) {
            return stack.getItem() instanceof ItemMachineUpgrade;
        }
        if (slot >= SLOT_SOLID_OUTPUT_START && slot <= SLOT_SOLID_OUTPUT_END) {
            return false;
        }
        if (slot >= SLOT_FLUID_INPUT_START && slot <= SLOT_FLUID_INPUT_END) {
            //? if forge {
            /*return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
            *///?} else {
            return FluidStorage.ITEM.find(stack, null) != null;
             //?}
        }
        if (slot >= SLOT_FLUID_OUTPUT_START && slot <= SLOT_FLUID_OUTPUT_END) {
            //? if forge {
            /*return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
            *///?} else {
            return FluidStorage.ITEM.find(stack, null) != null;
             //?}
        }
        if (slot >= SLOT_FLUID_INPUT_EMPTY_START && slot <= SLOT_FLUID_INPUT_EMPTY_END) {
            //? if forge {
            /*return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
            *///?} else {
            return FluidStorage.ITEM.find(stack, null) != null;
             //?}
        }
        if (slot >= SLOT_FLUID_OUTPUT_EMPTY_START && slot <= SLOT_FLUID_OUTPUT_EMPTY_END) {
            //? if forge {
            /*return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
            *///?} else {
            return FluidStorage.ITEM.find(stack, null) != null;
             //?}
        }
        return true;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineChemicalPlantMenu(containerId, playerInventory, this, data);
    }

    public FluidTank[] getInputTanks() { return inputTanks; }
    public FluidTank[] getOutputTanks() { return outputTanks; }

    public List<ChemicalPlantRecipe> getAvailableRecipes() {
        if (level == null) return List.of();
        ItemStack folder = inventory.getStackInSlot(SLOT_SCHEMATIC);
        String installedPool = ItemBlueprintFolder.getBlueprintPool(folder);
        List<ChemicalPlantRecipe> all = level.getRecipeManager().getAllRecipesFor(ChemicalPlantRecipe.Type.INSTANCE);
        return all.stream().filter(r -> {
            String pool = r.getBlueprintPool();
            if (pool == null || pool.isEmpty()) return true;
            return installedPool != null && !installedPool.isEmpty() && installedPool.equals(pool);
        }).toList();
    }

    public ItemStack getBlueprintFolder() {
        return inventory.getStackInSlot(SLOT_SCHEMATIC);
    }

    public boolean getDidProcess() {
        return module.getDidProcess();
    }

    @Nullable
    public ResourceLocation getSelectedRecipeId() {
        return module.getSelectedRecipeId();
    }

    public void setSelectedRecipe(@Nullable ResourceLocation recipeId) {
        module.setSelectedRecipe(recipeId);
        setChanged();
        if (level != null && !level.isClientSide) {
            sendUpdateToClient();
        }
    }

    public int getProgress() {
        return module.getProgress();
    }

    public int getMaxProgress() {
        return module.getMaxProgress();
    }

    public float getAnim(float partialTicks) {
        return prevAnim + (anim - prevAnim) * partialTicks;
    }

    public FluidStack getFluid() {
        if (inputTanks[0].isEmpty()) return FluidStack.create(Fluids.EMPTY, 0L);
        return FluidStack.create(inputTanks[0].getStoredFluid(), (long) inputTanks[0].getFluidAmountMb());
    }

    public float getFluidFillFraction() {
        if (inputTanks[0].getCapacityMb() <= 0) return 0.0F;
        return Math.min(1.0F, Math.max(0.0F, inputTanks[0].getFluidAmountMb() / (float) inputTanks[0].getCapacityMb()));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        for (int i = 0; i < 3; i++) {
            tag.put("inputTank" + i, inputTanks[i].writeNBT(new CompoundTag()));
            tag.put("outputTank" + i, outputTanks[i].writeNBT(new CompoundTag()));
        }
        tag.putBoolean("didProcess", module.getDidProcess());
        module.writeNBT(tag);
        tag.putFloat("anim", anim);
        tag.putFloat("prevAnim", prevAnim);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (int i = 0; i < 3; i++) {
            if (tag.contains("inputTank" + i)) inputTanks[i].readNBT(tag.getCompound("inputTank" + i));
            if (tag.contains("outputTank" + i)) outputTanks[i].readNBT(tag.getCompound("outputTank" + i));
        }
        module.readNBT(tag);
        // didProcess хранится отдельно от module.writeNBT(); нужен на клиенте для звука/анимации.
        module.didProcess = tag.getBoolean("didProcess");
        anim = tag.getFloat("anim");
        prevAnim = tag.getFloat("prevAnim");
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // Если BE удалили (сломали блок/выгрузили чанк), clientTick больше не вызовется → стопаем loop-звук.
        if (level != null && level.isClientSide) {
            ClientSoundBootstrap.updateSound(this, false, null);
        }
    }

    //? if forge {
    /*private LazyOptional<IFluidHandler> combinedFluidHandler = LazyOptional.empty();
    private static final LazyOptional<?> EMPTY_CAP = LazyOptional.empty();

    @Override
    public void onLoad() {
        super.onLoad();
        combinedFluidHandler = LazyOptional.of(() -> new CombinedChemPlantFluidHandler(this));
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            if (side == null || side.getAxis().isHorizontal()) return combinedFluidHandler.cast();
            return (LazyOptional<T>) EMPTY_CAP;
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        combinedFluidHandler.invalidate();
        for (int i = 0; i < 3; i++) {
            inputTankHandlers[i].invalidate();
            outputTankHandlers[i].invalidate();
        }
    }

    private static class CombinedChemPlantFluidHandler implements net.minecraftforge.fluids.capability.IFluidHandler {
        private final MachineChemicalPlantBlockEntity be;
        CombinedChemPlantFluidHandler(MachineChemicalPlantBlockEntity be) { this.be = be; }

        @Override public int getTanks() { return 6; }

        @Override
        public net.minecraftforge.fluids.FluidStack getFluidInTank(int tank) {
            FluidTank t = tank < 3 ? be.inputTanks[tank] : be.outputTanks[tank - 3];
            if (t.isEmpty()) return net.minecraftforge.fluids.FluidStack.EMPTY;
            return new net.minecraftforge.fluids.FluidStack(t.getStoredFluid(), t.getFluidAmountMb());
        }

        @Override public int getTankCapacity(int tank) { return TANK_CAPACITY; }

        @Override
        public boolean isFluidValid(int tank, net.minecraftforge.fluids.FluidStack stack) {
            if (tank >= 3) return false;
            // Для труб/коннекта всегда "валидно": реальный приём решает fill() (по выбранному рецепту).
            return true;
        }

        @Override
        public int fill(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return 0;
            if (be.module == null || be.module.getSelectedRecipeId() == null) return 0;
            for (int i = 0; i < 3; i++) {
                FluidTank tank = be.inputTanks[i];
                Fluid configured = tank.getConfiguredFluid();
                if (configured == Fluids.EMPTY) continue;
                if (!com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(configured, resource.getFluid())) continue;
                int space = tank.getCapacityMb() - tank.getFluidAmountMb();
                if (space <= 0) continue;
                int toFill = Math.min(resource.getAmount(), space);
                if (action.execute()) {
                    tank.fillMb(resource.getFluid(), toFill);
                    be.setChanged();
                }
                return toFill;
            }
            return 0;
        }

        @Override
        public net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return net.minecraftforge.fluids.FluidStack.EMPTY;
            for (int i = 0; i < 3; i++) {
                FluidTank tank = be.outputTanks[i];
                if (tank.isEmpty()) continue;
                if (!com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(tank.getStoredFluid(), resource.getFluid())) continue;
                int available = tank.getFluidAmountMb();
                int toDrain = Math.min(resource.getAmount(), available);
                if (toDrain <= 0) continue;
                if (action.execute()) {
                    tank.drainMb(toDrain);
                    be.setChanged();
                }
                return new net.minecraftforge.fluids.FluidStack(tank.getStoredFluid(), toDrain);
            }
            return net.minecraftforge.fluids.FluidStack.EMPTY;
        }

        @Override
        public net.minecraftforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) return net.minecraftforge.fluids.FluidStack.EMPTY;
            for (int i = 0; i < 3; i++) {
                FluidTank tank = be.outputTanks[i];
                if (tank.isEmpty()) continue;
                int available = tank.getFluidAmountMb();
                int toDrain = Math.min(maxDrain, available);
                if (toDrain <= 0) continue;
                if (action.execute()) {
                    tank.drainMb(toDrain);
                    be.setChanged();
                }
                return new net.minecraftforge.fluids.FluidStack(tank.getStoredFluid(), toDrain);
            }
            return net.minecraftforge.fluids.FluidStack.EMPTY;
        }
    }

    /^*
     * Handler для заливки из предметов в конкретный входной танк.
     * Должен уважать выбранный рецепт/сконфигуренную жидкость, чтобы ручной залив через GUI
     * не обходил логику хим. установки.
     ^/
    private static final class GuardedInputTankFluidHandler implements IFluidHandler {
        private final MachineChemicalPlantBlockEntity be;
        private final int tankIndex;

        private GuardedInputTankFluidHandler(MachineChemicalPlantBlockEntity be, int tankIndex) {
            this.be = be;
            this.tankIndex = tankIndex;
        }

        @Override public int getTanks() { return 1; }

        @Override
        public net.minecraftforge.fluids.FluidStack getFluidInTank(int tank) {
            FluidTank t = be.inputTanks[tankIndex];
            if (t.isEmpty()) return net.minecraftforge.fluids.FluidStack.EMPTY;
            return new net.minecraftforge.fluids.FluidStack(t.getStoredFluid(), t.getFluidAmountMb());
        }

        @Override public int getTankCapacity(int tank) { return TANK_CAPACITY; }

        @Override
        public boolean isFluidValid(int tank, net.minecraftforge.fluids.FluidStack stack) {
            return true;
        }

        @Override
        public int fill(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return 0;
            if (be.module == null || be.module.getSelectedRecipeId() == null) return 0;

            FluidTank tank = be.inputTanks[tankIndex];
            Fluid configured = tank.getConfiguredFluid();
            if (configured == Fluids.EMPTY) return 0;
            if (!com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(configured, resource.getFluid())) return 0;

            int space = tank.getCapacityMb() - tank.getFluidAmountMb();
            if (space <= 0) return 0;
            int toFill = Math.min(resource.getAmount(), space);
            if (action.execute()) {
                tank.fillMb(resource.getFluid(), toFill);
                be.setChanged();
            }
            return toFill;
        }

        @Override
        public net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            return net.minecraftforge.fluids.FluidStack.EMPTY;
        }

        @Override
        public net.minecraftforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
            return net.minecraftforge.fluids.FluidStack.EMPTY;
        }
    }
    *///?}

    //? if fabric {
    @SuppressWarnings("UnstableApiUsage")
    @Nullable
    public net.fabricmc.fabric.api.transfer.v1.storage.Storage<net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant> getFluidStorage(@Nullable Direction side) {
        return new ChemPlantFabricFluidStorage(this);
    }

    @SuppressWarnings("UnstableApiUsage")
    private static class ChemPlantFabricFluidStorage implements net.fabricmc.fabric.api.transfer.v1.storage.Storage<net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant> {
        private final MachineChemicalPlantBlockEntity be;
        ChemPlantFabricFluidStorage(MachineChemicalPlantBlockEntity be) { this.be = be; }

        @Override
        public long insert(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant resource, long maxAmount,
                           net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext transaction) {
            if (resource.isBlank() || maxAmount <= 0) return 0;
            for (int i = 0; i < 3; i++) {
                FluidTank tank = be.inputTanks[i];
                Fluid configured = tank.getConfiguredFluid();
                if (configured == Fluids.EMPTY) continue;
                if (!com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(configured, resource.getFluid())) continue;
                long spaceMb = tank.getCapacityMb() - tank.getFluidAmountMb();
                if (spaceMb <= 0) continue;
                long dropletsToFill = Math.min(maxAmount, spaceMb * 81L);
                if (dropletsToFill <= 0) continue;
                long inserted = tank.getStorage().insert(resource, dropletsToFill, transaction);
                if (inserted > 0) be.setChanged();
                return inserted;
            }
            return 0;
        }

        @Override
        public long extract(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant resource, long maxAmount,
                            net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext transaction) {
            if (resource.isBlank() || maxAmount <= 0) return 0;
            for (int i = 0; i < 3; i++) {
                FluidTank tank = be.outputTanks[i];
                if (tank.isEmpty()) continue;
                if (!com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(tank.getStoredFluid(), resource.getFluid())) continue;
                long availableDroplets = (long) tank.getFluidAmountMb() * 81L;
                long dropletsToExtract = Math.min(maxAmount, availableDroplets);
                if (dropletsToExtract <= 0) continue;
                long extracted = tank.getStorage().extract(resource, dropletsToExtract, transaction);
                if (extracted > 0) be.setChanged();
                return extracted;
            }
            return 0;
        }

        @Override public java.util.Iterator<net.fabricmc.fabric.api.transfer.v1.storage.StorageView<net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant>> iterator() {
            return java.util.Collections.emptyIterator();
        }
    }
    //?}
}
