package com.hbm_m.block.entity.machines;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.api.fluids.IFluidUserMK2;
import com.hbm_m.block.entity.BaseMachineBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.machines.MachineChemicalPlantBlock;
import com.hbm_m.interfaces.IUpgradeInfoProvider;
import com.hbm_m.inventory.UpgradeManager;
import com.hbm_m.inventory.fluid.ModFluids;
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
import net.minecraftforge.fluids.FluidActionResult;
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
@SuppressWarnings("UnstableApiUsage")
public class MachineChemicalPlantBlockEntity extends BaseMachineBlockEntity implements IUpgradeInfoProvider, IFluidStandardTransceiverMK2 {

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
    private boolean tanksDirty = false;

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

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> module != null ? module.getProgressInt() : 0;
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
                    tanksDirty = true;
                }
            };
            outputTanks[i] = new FluidTank(TANK_CAPACITY) {
                @Override
                public void onContentsChanged() {
                    setChanged();
                    tanksDirty = true;
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
                inputTanks, outputTanks,
                this.level);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineChemicalPlantBlockEntity entity) {
        entity.prevAnim = entity.anim;

        if (level.isClientSide) {
            // Модуль создаётся в конструкторе с level=null (BE ещё не привязан к миру),
            // на сервере он обновляется каждый тик; на клиенте тоже нужно обновлять,
            // иначе peekRecipe(level) всегда возвращает null и визуал/цвет берётся только из fallback.
            entity.module.setLevel(level);
            if (entity.isChemplantEffectsActive()) {
                entity.anim++;
            }
            entity.clientTick();
            return;
        }

        entity.updateFrameBlockState();
        entity.syncRenderActiveStub();

        entity.ensureNetworkInitialized();
        entity.module.setLevel(level);
        ChemicalPlantRecipe capRecipe = entity.module.peekRecipe(level);
        long desiredCap = MAX_POWER;
        if (capRecipe != null) {
            desiredCap = Math.max(desiredCap, capRecipe.getPowerConsumption() * 100L);
        }
        desiredCap = Math.max(desiredCap, entity.getEnergyStored());
        if (desiredCap != entity.getMaxEnergyStored()) {
            entity.setEnergyCapacity(desiredCap);
        }

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
        boolean dirty = entity.module.updateAndGetDirty(speed, pow, true, blueprint);

        if (entity.module.getDidProcess()) {
            ItemStack maybeSword = entity.inventory.getStackInSlot(SLOT_BATTERY);
            if (!maybeSword.isEmpty() && maybeSword.is(com.hbm_m.item.ModItems.METEORITE_SWORD.get())) {
                entity.inventory.setStackInSlot(SLOT_BATTERY, new ItemStack(com.hbm_m.item.ModItems.METEORITE_SWORD_SEARED.get()));
                dirty = true;
            }
        }

        // RENDER_ACTIVE должен быть стабильным флагом "идёт процесс", а не 1-tick импульсом didProcess.
        // Иначе на клиенте BER (жидкость/звук/анимация) почти всегда выключен.
        boolean shouldRenderActive = entity.module.getProgressInt() > 0 || entity.module.getDidProcess();
        if (state.hasProperty(MachineChemicalPlantBlock.RENDER_ACTIVE)) {
            boolean active = state.getValue(MachineChemicalPlantBlock.RENDER_ACTIVE);
            if (active != shouldRenderActive) {
                level.setBlock(pos, state.setValue(MachineChemicalPlantBlock.RENDER_ACTIVE, shouldRenderActive), 3);
            }
        }

        if (entity.tanksDirty) {
            dirty = true;
            entity.tanksDirty = false;
        }

        if (dirty) {
            entity.setChanged();
            entity.sendUpdateToClient();
        }
    }

    public MachineModuleChemplant getModule() {
        return module;
    }

    // =====================================================================================
    // IFluidStandardTransceiverMK2 — нативное участие в MK2-сети.
    // Контроллер сам не подписывается на трубы (он живёт внутри мультиблока);
    // подписку выполняет UniversalMachinePartBlockEntity (FLUID_CONNECTOR) в своём тике.
    // =====================================================================================

    @Override
    public FluidTank[] getReceivingTanks() { return inputTanks; }

    @Override
    public FluidTank[] getSendingTanks() { return outputTanks; }

    @Override
    public FluidTank[] getAllTanks() {
        FluidTank[] all = new FluidTank[inputTanks.length + outputTanks.length];
        System.arraycopy(inputTanks, 0, all, 0, inputTanks.length);
        System.arraycopy(outputTanks, 0, all, inputTanks.length, outputTanks.length);
        return all;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public java.util.Map<UpgradeType, Integer> getValidUpgrades() {
        return VALID_UPGRADES;
    }

    /**
     * Количество входных fluid-баков, которые реально задаёт текущий рецепт (без этого «лишние» NONE-баки
     * становятся сетевыми стоками и размазывают воду между слотами). 0 если рецепт не выбран или не содержит жидкостей.
     */
    public int getActiveFluidInputSlotCount() {
        if (level == null) return 0;
        ChemicalPlantRecipe r = module.peekRecipe(level);
        if (r == null) return 0;
        return Math.min(3, r.getFluidInputs().size());
    }

    @Override
    public long getDemand(Fluid fluid, int pressure) {
        long amount = 0;
        int n = getActiveFluidInputSlotCount();
        for (int i = 0; i < n; i++) {
            FluidTank t = inputTanks[i];
            if (!IFluidStandardReceiverMK2.receiverTankMatches(t, fluid, pressure)) continue;
            amount += (long) Math.max(0, t.getMaxFill() - t.getFill());
        }
        return amount;
    }

    @Override
    public long transferFluid(Fluid fluid, int pressure, long amount) {
        if (amount <= 0 || fluid == null || fluid == Fluids.EMPTY || fluid == ModFluids.NONE.getSource()) {
            return amount;
        }
        int n = getActiveFluidInputSlotCount();
        int tanksMatching = 0;
        for (int i = 0; i < n; i++) {
            if (IFluidStandardReceiverMK2.receiverTankMatches(inputTanks[i], fluid, pressure)) tanksMatching++;
        }
        long remain = amount;
        if (tanksMatching > 1 && remain > 0) {
            long share = (long) Math.floor((double) remain / tanksMatching);
            for (int i = 0; i < n; i++) {
                FluidTank t = inputTanks[i];
                if (!IFluidStandardReceiverMK2.receiverTankMatches(t, fluid, pressure)) continue;
                long got = IFluidStandardReceiverMK2.receiverFillReceivingTankMb(t, fluid, Math.min(share, remain));
                remain -= got;
                if (remain <= 0) return 0L;
            }
        }
        if (remain > 0) {
            for (int i = 0; i < n; i++) {
                FluidTank t = inputTanks[i];
                if (!IFluidStandardReceiverMK2.receiverTankMatches(t, fluid, pressure)) continue;
                long got = IFluidStandardReceiverMK2.receiverFillReceivingTankMb(t, fluid, remain);
                remain -= got;
                if (remain <= 0) break;
            }
        }
        return remain;
    }

    @Override
    public int[] getReceivingPressureRange(Fluid fluid) {
        int lowest = IFluidUserMK2.HIGHEST_VALID_PRESSURE;
        int highest = 0;
        int n = getActiveFluidInputSlotCount();
        for (int i = 0; i < n; i++) {
            FluidTank t = inputTanks[i];
            if (!IFluidStandardReceiverMK2.receiverTankMatches(t, fluid, t.getPressure())) continue;
            int pr = t.getPressure();
            if (pr < lowest) lowest = pr;
            if (pr > highest) highest = pr;
        }
        return lowest <= highest ? new int[]{ lowest, highest } : IFluidUserMK2.DEFAULT_PRESSURE_RANGE;
    }

    //? if forge {
    /*@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
            *///?}
    private void clientTick() {
        ClientSoundBootstrap.updateSound(this, this.isChemplantEffectsActive(), () -> newChemicalPlantSoundInstance());
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

    /** Живые ссылки на слоты — для {@link FluidTank#loadTank}, чтобы мутации шли напрямую в инвентарь TE. */
    private ItemStack[] chemPlantInventorySlotBacking() {
        int n = inventory.getSlots();
        ItemStack[] slots = new ItemStack[n];
        for (int s = 0; s < n; s++) {
            slots[s] = inventory.getStackInSlot(s);
        }
        return slots;
    }

    private void transferFluidsFromItems() {
        ItemStack[] slotView = chemPlantInventorySlotBacking();
        for (int i = 0; i < 3; i++) {
            int fullContainerSlot = SLOT_FLUID_INPUT_START + i;
            int emptyContainerSlot = SLOT_FLUID_INPUT_EMPTY_START + i;
            ItemStack fullContainer = inventory.getStackInSlot(fullContainerSlot);
            if (fullContainer.isEmpty()) continue;

            // Бесконечные контейнеры обрабатываются через FluidTank.loadTank / FluidLoaderInfinite
            // и не требуют пустого нижнего слота под «опустошённый» стак Forge.
            // FluidLoaderStandard.emptyItem сам проверит canPlaceItemInSlot и стакаемость.

            if (inputTanks[i].loadTank(fullContainerSlot, emptyContainerSlot, slotView)) {
                inventory.setStackInSlot(fullContainerSlot, slotView[fullContainerSlot]);
                inventory.setStackInSlot(emptyContainerSlot, slotView[emptyContainerSlot]);
                setChanged();
            }
        }
    }

    private void transferFluidsToItems() {
        ItemStack[] slotView = chemPlantInventorySlotBacking();
        for (int i = 0; i < 3; i++) {
            int emptyContainerSlot = SLOT_FLUID_OUTPUT_START + i;
            int filledContainerSlot = SLOT_FLUID_OUTPUT_EMPTY_START + i;

            if (outputTanks[i].unloadTank(emptyContainerSlot, filledContainerSlot, slotView)) {
                inventory.setStackInSlot(emptyContainerSlot, slotView[emptyContainerSlot]);
                inventory.setStackInSlot(filledContainerSlot, slotView[filledContainerSlot]);
                setChanged();
            }
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
        List<ChemicalPlantRecipe> all = com.hbm_m.recipe.index.ModRecipeIndex.of(level.getRecipeManager())
                .getAll(ChemicalPlantRecipe.Type.INSTANCE);
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

    /**
     * Клиентский звук BER, приращение {@code anim} и жидкость не должны зависеть только от {@code didProcess}
     * в одном сетевом снимке: между пакетами он часто false → мигание цвета/UV и обрыв лупа.
     * На клиенте учитываем {@link MachineChemicalPlantBlock#RENDER_ACTIVE} и синхронизированный {@link MachineModuleChemplant#getProgress}.
     */
    public boolean isChemplantEffectsActive() {
        if (level == null) return false;
        if (!level.isClientSide) {
            return module.getDidProcess();
        }
        // На клиенте единственный источник правды — синхронизированный blockstate RENDER_ACTIVE.
        // didProcess / progress могут рассинхронизироваться между пакетами и давать "лишние" 5-8 с.
        BlockState st = getBlockState();
        return st.hasProperty(MachineChemicalPlantBlock.RENDER_ACTIVE)
            && st.getValue(MachineChemicalPlantBlock.RENDER_ACTIVE);
    }

    @Nullable
    public ResourceLocation getSelectedRecipeId() {
        return module.getSelectedRecipeId();
    }

    public void setSelectedRecipe(@Nullable ResourceLocation recipeId) {
        module.setSelectedRecipe(recipeId);
        if (level != null && !level.isClientSide) {
            module.syncTankConfigurationToRecipe(level);
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            sendUpdateToClient();
        }
    }

    public int getProgress() {
        return module.getProgressInt();
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
        // anim/prevAnim только на клиенте — не писать в NBT: иначе при каждом sync с сервера (0,0) сбрасывают интерполяцию.
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
            if (tank >= be.getActiveFluidInputSlotCount()) return false;
            return true;
        }

        // Естественная фильтрация по рецепту:
        //  - без выбора рецепта / без fluid во входах рецепта — ни один входной бак не примет;
        //  - лишние баки (NONE после setupTanks) не участвуют (иначе они «пылесосят» вторую порцию той же жидкости).
        @Override
        public int fill(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return 0;
            int bound = be.getActiveFluidInputSlotCount();
            for (int i = 0; i < bound; i++) {
                FluidTank tank = be.inputTanks[i];
                Fluid configured = tank.getConfiguredFluid();
                if (configured == Fluids.EMPTY || configured == ModFluids.NONE.getSource()) continue;
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
                Fluid normalized = com.hbm_m.api.fluids.VanillaFluidEquivalence.forVanillaContainerFill(tank.getStoredFluid());
                return new net.minecraftforge.fluids.FluidStack(normalized, toDrain);
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
                Fluid normalized = com.hbm_m.api.fluids.VanillaFluidEquivalence.forVanillaContainerFill(tank.getStoredFluid());
                return new net.minecraftforge.fluids.FluidStack(normalized, toDrain);
            }
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
            int bound = be.getActiveFluidInputSlotCount();
            for (int i = 0; i < bound; i++) {
                FluidTank tank = be.inputTanks[i];
                Fluid configured = tank.getConfiguredFluid();
                if (configured == Fluids.EMPTY || configured == ModFluids.NONE.getSource()) continue;
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
