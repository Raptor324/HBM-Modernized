package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.block.machines.MachineChemicalFactoryBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IFrameSupportable;
import com.hbm_m.inventory.UpgradeManager;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineChemicalFactoryMenu;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType;
import com.hbm_m.module.machine.MachineModuleChemFactoryLane;
import com.hbm_m.multiblock.MultiblockFrameHelper;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.platform.ModItemStackHandler;
import com.hbm_m.recipe.ChemicalPlantRecipe;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

/**
 * Chemical Factory — порт 1.7.10 {@code TileEntityMachineChemicalFactory}.
 *
 * <p>Оригинал держит 4 экземпляра {@code ModuleMachineChemplant} (по сути 4 параллельных Chemical
 * Plant), общий контур охлаждения (вода -&gt; отработанный пар) и элаборированную геометрию портов
 * ввода/вывода для соседних труб/кабелей 1.7.10. В этом порту:
 * <ul>
 *   <li>4 линии реализованы через {@link MachineModuleChemFactoryLane}; как и в оригинале, рецепт
 *       каждой линии выбирается вручную через GUI ({@code GUIScreenRecipeSelector}), выбор ограничен
 *       пулом папки чертежей в слоте шаблона линии (слоты 4 + lane*7);</li>
 *   <li>контур охлаждения — пара {@link FluidTank} (вода/спент-стим): при обработке каждой активной
 *       линии списывается 100 мБ воды и добавляется 100 мБ спент-стима, как в оригинале;</li>
 *   <li>внешний ввод/вывод жидкостей — через стандартный {@link IFluidStandardTransceiverMK2} (как у
 *       Chemical Plant), а не через ручную геометрию портов 1.7.10: капабилити-система этого порта
 *       уже решает "к какой трубе что течёт" без вычисления фиксированных мировых координат портов.</li>
 * </ul>
 */
public class MachineChemicalFactoryBlockEntity extends BaseMachineBlockEntity
        implements IFluidStandardTransceiverMK2, IFrameSupportable,
                   com.hbm_m.api.fluids.IPositionalFluidTransceiver,
                   com.hbm_m.api.redstoneoverradio.IRORValueProvider {

    private static final int LANE_COUNT = 4;

    private static final int SLOT_BATTERY = 0;
    private static final int SLOT_UPGRADE_START = 1;
    private static final int SLOT_UPGRADE_END = 3;
    private static final int LANE_SLOT_BASE = 4;
    private static final int LANE_SLOT_STRIDE = 7; // 1 template (blueprint) + 3 solid in + 3 solid out per lane
    private static final int SLOT_COUNT = LANE_SLOT_BASE + LANE_COUNT * LANE_SLOT_STRIDE; // 32

    /** Слот шаблона (папки чертежей) линии — как в оригинале 4 + lane*7. */
    public static int getTemplateSlot(int lane) { return LANE_SLOT_BASE + lane * LANE_SLOT_STRIDE; }

    private static final int TANK_CAPACITY = 24_000;
    private static final int COOLANT_TANK_CAPACITY = 4_000;
    private static final int COOLANT_PER_PROCESS = 100;
    private static final long BASE_MAX_POWER = 1_000_000L;
    private static final long MAX_RECEIVE = 20_000L;

    private final FluidTank[] inputTanks = new FluidTank[LANE_COUNT * 3];
    private final FluidTank[] outputTanks = new FluidTank[LANE_COUNT * 3];
    private final FluidTank water;
    private final FluidTank lps;
    private boolean tanksDirty = false;

    private final MachineModuleChemFactoryLane[] lanes = new MachineModuleChemFactoryLane[LANE_COUNT];
    private final boolean[] didProcess = new boolean[LANE_COUNT];
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
            if (index >= 0 && index < LANE_COUNT) {
                return lanes[index] != null ? lanes[index].getProgressInt() : 0;
            }
            if (index >= LANE_COUNT && index < LANE_COUNT * 2) {
                return lanes[index - LANE_COUNT] != null ? lanes[index - LANE_COUNT].getMaxProgress() : 100;
            }
            if (index >= LANE_COUNT * 2 && index < LANE_COUNT * 3) {
                return lanes[index - LANE_COUNT * 2] != null && lanes[index - LANE_COUNT * 2].getDidProcess() ? 1 : 0;
            }
            return switch (index - LANE_COUNT * 3) {
                case 0 -> canCool() ? 1 : 0;
                case 1 -> (int) (getEnergyStored() & 0xFFFFFFFFL);
                case 2 -> (int) ((getEnergyStored() >> 32) & 0xFFFFFFFFL);
                case 3 -> (int) (getMaxEnergyStored() & 0xFFFFFFFFL);
                case 4 -> (int) ((getMaxEnergyStored() >> 32) & 0xFFFFFFFFL);
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {}
        @Override public int getCount() { return LANE_COUNT * 3 + 5; }
    };

    public MachineChemicalFactoryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHEMICAL_FACTORY_BE.get(), pos, state, SLOT_COUNT, BASE_MAX_POWER, MAX_RECEIVE);

        for (int i = 0; i < inputTanks.length; i++) {
            inputTanks[i] = new FluidTank(TANK_CAPACITY) {
                @Override public void onContentsChanged() { setChanged(); tanksDirty = true; }
            };
            outputTanks[i] = new FluidTank(TANK_CAPACITY) {
                @Override public void onContentsChanged() { setChanged(); tanksDirty = true; }
            };
        }
        water = new FluidTank(Fluids.WATER, COOLANT_TANK_CAPACITY) {
            @Override public void onContentsChanged() { setChanged(); tanksDirty = true; }
        };
        lps = new FluidTank(ModFluids.SPENTSTEAM.getSource(), COOLANT_TANK_CAPACITY) {
            @Override public void onContentsChanged() { setChanged(); tanksDirty = true; }
        };

        for (int i = 0; i < LANE_COUNT; i++) {
            int base = LANE_SLOT_BASE + i * LANE_SLOT_STRIDE;
            int[] solidIn = { base + 1, base + 2, base + 3 };
            int[] solidOut = { base + 4, base + 5, base + 6 };
            FluidTank[] laneIn = { inputTanks[i * 3], inputTanks[i * 3 + 1], inputTanks[i * 3 + 2] };
            FluidTank[] laneOut = { outputTanks[i * 3], outputTanks[i * 3 + 1], outputTanks[i * 3 + 2] };
            lanes[i] = new MachineModuleChemFactoryLane(i, this, inventory, solidIn, solidOut, laneIn, laneOut, this.level);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineChemicalFactoryBlockEntity be) {
        be.prevAnim = be.anim;

        if (level.isClientSide) {
            for (MachineModuleChemFactoryLane lane : be.lanes) lane.setLevel(level);
            if (be.isAnyLaneActive()) be.anim++;
            be.clientTick();
            return;
        }

        be.ensureNetworkInitialized();
        for (MachineModuleChemFactoryLane lane : be.lanes) lane.setLevel(level);

        long nextMaxPower = 0;
        for (MachineModuleChemFactoryLane lane : be.lanes) {
            ChemicalPlantRecipe recipe = lane.peekRecipe(be.level);
            if (recipe != null) nextMaxPower += recipe.getPowerConsumption() * 100L;
        }
        long desiredCap = Math.max(BASE_MAX_POWER, nextMaxPower);
        desiredCap = Math.max(desiredCap, be.getEnergyStored());
        if (desiredCap != be.getMaxEnergyStored()) {
            be.setEnergyCapacity(desiredCap);
        }

        be.chargeFromBattery();
        be.upgradeManager.checkSlots(be.inventory, SLOT_UPGRADE_START, SLOT_UPGRADE_END, VALID_UPGRADES);

        if (level.getGameTime() % 10L == 0L) {
            be.updateEnergyDelta(be.getEnergyStored());
        }

        int s = Math.min(be.upgradeManager.getLevel(UpgradeType.SPEED), 3);
        int p = Math.min(be.upgradeManager.getLevel(UpgradeType.POWER), 3);
        int o = Math.min(be.upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3);

        double speed = 1.0 + s / 3.0 + o;
        double pow = 1.0 - 0.25 * p + s + (10.0 / 3.0) * o;

        boolean dirty = false;
        boolean canCool = be.canCool();
        for (int i = 0; i < LANE_COUNT; i++) {
            // Как в оригинале: blueprint-слот линии гейтит выбранный рецепт по пулу папки.
            // update(speed * 2, pow * 2) — фабрика работает вдвое быстрее и вдвое прожорливее
            // одиночной Chemical Plant (оригинал: chemplantModule[i].update(speed * 2D, pow * 2D, ...)).
            dirty |= be.lanes[i].updateAndGetDirty(speed * 2.0, pow * 2.0, canCool, be.inventory.getStackInSlot(getTemplateSlot(i)));
            boolean processed = be.lanes[i].getDidProcess();
            be.didProcess[i] = processed;
            if (processed) {
                be.water.drainMb(COOLANT_PER_PROCESS);
                be.lps.fillMb(be.lps.getConfiguredFluid(), COOLANT_PER_PROCESS);
            }
        }

        // Внутреннее выравнивание жидкостей: если у одной линии не хватает входа, а у другой лежит
        // избыток того же типа/давления на выходе — перекачиваем немного между баками (как в оригинале).
        for (FluidTank in : be.inputTanks) {
            if (in.isEmpty() && !com.hbm_m.inventory.fluid.tank.FluidTank.isFluidTypeExplicitlySet(in.getConfiguredFluid())) continue;
            for (FluidTank out : be.outputTanks) {
                if (out.isEmpty()) continue;
                if (!com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(out.getStoredFluid(), in.getConfiguredFluid())) continue;
                if (out.getPressure() != in.getPressure()) continue;
                int toMove = Math.min(Math.min(in.getSpaceMb(), out.getFluidAmountMb()), 50);
                if (toMove > 0) {
                    in.fillMb(out.getStoredFluid(), toMove);
                    out.drainMb(toMove);
                }
            }
        }

        if (be.tanksDirty) {
            dirty = true;
            be.tanksDirty = false;
        }

        if (dirty) {
            be.setChanged();
            be.sendUpdateToClient();
        }
    }

    private boolean isAnyLaneActive() {
        return didProcess[0] || didProcess[1] || didProcess[2] || didProcess[3];
    }

    public boolean canCool() {
        return water.getFluidAmountMb() >= COOLANT_PER_PROCESS
                && lps.getFluidAmountMb() <= lps.getCapacityMb() - COOLANT_PER_PROCESS;
    }

    // Рамка мультиблока: видима при блоке над любой клеткой верхнего пояса (MultiblockFrameHelper),
    // состояние хранится в BlockState (FRAME property), как у advanced assembler.

    @Override
    public void checkForFrame() {
        if (level != null && !level.isClientSide) {
            MultiblockStructureHelper.updateFrameForController(level, worldPosition);
        }
    }

    @Override
    public boolean setFrameVisible(boolean visible) {
        if (level != null && !level.isClientSide) {
            return MultiblockFrameHelper.applyFrameToBlockState(level, worldPosition, visible);
        }
        return false;
    }

    @Override
    public boolean isFrameVisible() {
        return MultiblockFrameHelper.isFrameVisible(getBlockState());
    }

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        // Машина 5×3×5 + опциональная труба над ядром: дефолтный 1-блочный AABB
        // ошибочно культил бы рендер, когда ядро за кадром (оригинал: x-2..x+3, y..y+3).
        AABB fallback = new AABB(
                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                worldPosition.getX() + 5, worldPosition.getY() + 4, worldPosition.getZ() + 5);
        if (!(getBlockState().getBlock() instanceof MachineChemicalFactoryBlock block)) {
            return fallback;
        }
        var structureMap = block.getStructureHelper().getStructureMap();
        if (structureMap == null || structureMap.isEmpty()) {
            return fallback;
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

    private void chargeFromBattery() {
        ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
        if (!stack.isEmpty() && stack.getItem() instanceof ItemCreativeBattery) {
            setEnergyStored(getMaxEnergyStored());
            return;
        }
        chargeFromBatterySlot(SLOT_BATTERY);
    }

    public MachineModuleChemFactoryLane[] getLanes() { return lanes; }
    public FluidTank[] getInputTanks() { return inputTanks; }
    public FluidTank[] getOutputTanks() { return outputTanks; }
    public FluidTank getWaterTank() { return water; }
    public FluidTank getSpentSteamTank() { return lps; }
    public boolean getLaneDidProcess(int lane) { return didProcess[lane]; }

    /** Папка чертежей в слоте шаблона линии (как в оригинале slots[4 + lane*7]). */
    public ItemStack getBlueprintFolder(int lane) {
        return inventory.getStackInSlot(getTemplateSlot(lane));
    }

    /** Рецепты, доступные линии: без пула + совпадающие с пулом папки чертежей линии. */
    public List<ChemicalPlantRecipe> getAvailableRecipes(int lane) {
        if (level == null) return List.of();
        ItemStack folder = getBlueprintFolder(lane);
        String installedPool = com.hbm_m.item.industrial.ItemBlueprintFolder.getBlueprintPool(folder);
        List<ChemicalPlantRecipe> all = com.hbm_m.recipe.index.ModRecipeIndex.of(level.getRecipeManager())
                .getAll(ChemicalPlantRecipe.Type.INSTANCE);
        return all.stream().filter(r -> {
            String pool = r.getBlueprintPool();
            if (pool == null || pool.isEmpty()) return true;
            return installedPool != null && !installedPool.isEmpty() && installedPool.equals(pool);
        }).toList();
    }

    @Nullable
    public ResourceLocation getSelectedRecipeId(int lane) {
        if (lane < 0 || lane >= LANE_COUNT) return null;
        return lanes[lane].getSelectedRecipeId();
    }

    public void setSelectedRecipe(int lane, @Nullable ResourceLocation recipeId) {
        if (lane < 0 || lane >= LANE_COUNT) return;
        lanes[lane].setSelectedRecipe(recipeId);
        if (level != null && !level.isClientSide) {
            lanes[lane].syncTankConfigurationToRecipe(level);
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            sendUpdateToClient();
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

    // =====================================================================================
    // IFluidStandardTransceiverMK2 + позиционные порты.
    //
    // Как в оригинале: стандартный трансивер машины — ТОЛЬКО рецептурные баки
    // (getConPos: inputTanks/outputTanks), а контур охлаждения вода/спент-стим
    // живёт на выделенных портах-торцах через делегат (аналог DelegateChemicalFactoy:
    // getCoolPos + getDelegateForPosition). Реализация через IPositionalFluidTransceiver:
    // UniversalMachinePartBlockEntity на торцевом коннекторе подписывает в сеть
    // coolantDelegate вместо this.
    // =====================================================================================

    /**
     * Торец охлаждения: часть на внешнем слое вдоль оси FACING на высоте ядра.
     * Оригинал: getCoolPos — 4 позиции dir*3±rot*1; getDelegateForPosition —
     * весь торец dir*2±rot.
     */
    public boolean isCoolantPort(BlockPos connectorPos) {
        Direction facing = getBlockState().hasProperty(MachineChemicalFactoryBlock.FACING)
                ? getBlockState().getValue(MachineChemicalFactoryBlock.FACING)
                : Direction.NORTH;
        int dy = connectorPos.getY() - worldPosition.getY();
        if (dy != 0) return false;
        int d = facing.getAxis() == Direction.Axis.X
                ? connectorPos.getX() - worldPosition.getX()
                : connectorPos.getZ() - worldPosition.getZ();
        return Math.abs(d) == 2;
    }

    /** Делегат торца охлаждения — порт 1.7.10 DelegateChemicalFactoy: только вода/спент-стим. */
    private final com.hbm_m.api.fluids.IFluidStandardTransceiverMK2 coolantDelegate =
            new com.hbm_m.api.fluids.IFluidStandardTransceiverMK2() {
                @Override public FluidTank[] getReceivingTanks() { return new FluidTank[]{ water }; }
                @Override public FluidTank[] getSendingTanks() { return new FluidTank[]{ lps }; }
                @Override public FluidTank[] getAllTanks() { return new FluidTank[]{ water, lps }; }
                @Override public boolean isLoaded() { return MachineChemicalFactoryBlockEntity.this.isLoaded(); }
            };

    @Override
    public com.hbm_m.api.fluids.IFluidUserMK2 getFluidTransceiverFor(BlockPos connectorPos) {
        return isCoolantPort(connectorPos) ? coolantDelegate : this;
    }

    @Override
    public FluidTank[] getReceivingTanks() {
        return inputTanks;
    }

    @Override
    public FluidTank[] getSendingTanks() {
        return outputTanks;
    }

    @Override
    public FluidTank[] getAllTanks() {
        FluidTank[] all = new FluidTank[inputTanks.length + outputTanks.length + 2];
        System.arraycopy(inputTanks, 0, all, 0, inputTanks.length);
        System.arraycopy(outputTanks, 0, all, inputTanks.length, outputTanks.length);
        all[all.length - 2] = water;
        all[all.length - 1] = lps;
        return all;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    public java.util.Map<UpgradeType, Integer> getValidUpgrades() {
        return VALID_UPGRADES;
    }

    @Override
    protected Component getDefaultName() { return Component.translatable("container.hbm_m.chemical_factory"); }

    @Override
    public Component getDisplayName() { return getDefaultName(); }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) {
            if (stack.isEmpty()) return false;
            if (stack.getItem() instanceof ItemCreativeBattery) return true;
            return isEnergyProviderItem(stack);
        }
        if (slot >= SLOT_UPGRADE_START && slot <= SLOT_UPGRADE_END) {
            return stack.getItem() instanceof ItemMachineUpgrade;
        }
        for (int i = 0; i < LANE_COUNT; i++) {
            if (slot == getTemplateSlot(i)) {
                return stack.getItem() instanceof com.hbm_m.item.industrial.ItemBlueprintFolder;
            }
            int base = LANE_SLOT_BASE + i * LANE_SLOT_STRIDE;
            if (slot >= base + 4 && slot <= base + 6) return false; // solid output — не вставляем руками
            if (slot >= base + 1 && slot <= base + 3) return true; // solid input — принимаем любой предмет
        }
        return false;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineChemicalFactoryMenu(containerId, playerInventory, this, data);
    }

    public boolean isAnyProcessing() { return isAnyLaneActive(); }

    // =====================================================================================
    // Звук (порт createAudioLoop: NTMSounds.CHEMPLANT_LOOP, тот же луп, что у химзавода).
    // =====================================================================================

    private static final String CHEMICAL_PLANT_SOUND_INSTANCE = "com.hbm_m.sound.ChemicalPlantSoundInstance";

    //? if forge {
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    //?}
    private void clientTick() {
        com.hbm_m.sound.ClientSoundBootstrap.updateSound(this, isChemFactoryEffectsActive(), this::newChemFactorySoundInstance);
    }

    /** Клиентский эффект звука: как isProgressing оригинала — только при реальном didProcess. */
    public boolean isChemFactoryEffectsActive() {
        return isAnyLaneActive();
    }

    private Object newChemFactorySoundInstance() {
        try {
            return Class.forName(CHEMICAL_PLANT_SOUND_INSTANCE).getConstructor(BlockPos.class).newInstance(this.getBlockPos());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setRemoved() {
        // Если BE удалили (сломали блок/выгрузили чанк), clientTick больше не вызовется → стопаем loop-звук.
        if (level != null && level.isClientSide) {
            com.hbm_m.sound.ClientSoundBootstrap.updateSound(this, false, null);
        }
        super.setRemoved();
    }

    // =====================================================================================
    // Redstone over Radio — значения progress1..4 / recipe1..4 / active1..4 / anyactive
    // (порт provideRORValue + getFunctionInfo оригинала).
    // =====================================================================================

    @Override
    public String[] getFunctionInfo() {
        String p = com.hbm_m.api.redstoneoverradio.IRORInfo.PREFIX_VALUE;
        return new String[] {
                p + "progress1", p + "progress2", p + "progress3", p + "progress4",
                p + "recipe1", p + "recipe2", p + "recipe3", p + "recipe4",
                p + "anyactive",
                p + "active1", p + "active2", p + "active3", p + "active4",
        };
    }

    @Override
    public String provideRORValue(String name) {
        String p = com.hbm_m.api.redstoneoverradio.IRORInfo.PREFIX_VALUE;
        if ((p + "anyactive").equals(name)) return isAnyLaneActive() ? "1" : "0";
        for (int i = 0; i < LANE_COUNT; i++) {
            if ((p + "progress" + (i + 1)).equals(name))
                return String.valueOf(Math.round(lanes[i].getProgressPercent() * 100.0));
            if ((p + "recipe" + (i + 1)).equals(name)) {
                ResourceLocation id = lanes[i].getSelectedRecipeId();
                return id != null ? id.getPath() : null;
            }
            if ((p + "active" + (i + 1)).equals(name)) return didProcess[i] ? "1" : "0";
        }
        return null;
    }

    // =====================================================================================
    // Автоматизация предметов (аналог getAccessibleSlotsFromSide/canExtractItem оригинала:
    // воронки/трубы забирают только из выходов и "засорённых" входов; вставка ограничена
    // isItemValidForSlot). GUI этот handler не использует — ограничений не имеет.
    // =====================================================================================

    private ModItemStackHandler automationHandler;

    @Override
    protected ModItemStackHandler getAutomationItemHandler() {
        if (automationHandler == null) {
            automationHandler = new ModItemStackHandler(inventory.getSlots()) {
                @Override public int getSlots() { return inventory.getSlots(); }
                @Override public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
                @Override public void setStackInSlot(int slot, @NotNull ItemStack stack) { inventory.setStackInSlot(slot, stack); }
                @Override public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) { return inventory.insertItem(slot, stack, simulate); }
                @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
                    if (!canAutomatedExtract(slot)) return ItemStack.EMPTY;
                    return inventory.extractItem(slot, amount, simulate);
                }
                @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
                @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return inventory.isItemValid(slot, stack); }
            };
        }
        return automationHandler;
    }

    private boolean canAutomatedExtract(int slot) {
        for (int i = 0; i < LANE_COUNT; i++) {
            int base = LANE_SLOT_BASE + i * LANE_SLOT_STRIDE;
            if (slot >= base + 4 && slot <= base + 6) return true; // solid output
            if (lanes[i].isSlotClogged(slot)) return true;         // засорённый вход (рецепт сменился)
        }
        return false;
    }

    public float getAnim(float partialTicks) {
        return prevAnim + (anim - prevAnim) * partialTicks;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        for (int i = 0; i < inputTanks.length; i++) tag.put("inputTank" + i, inputTanks[i].writeNBT(new CompoundTag()));
        for (int i = 0; i < outputTanks.length; i++) tag.put("outputTank" + i, outputTanks[i].writeNBT(new CompoundTag()));
        tag.put("water", water.writeNBT(new CompoundTag()));
        tag.put("lps", lps.writeNBT(new CompoundTag()));
        for (int i = 0; i < LANE_COUNT; i++) {
            tag.putBoolean("didProcess" + i, didProcess[i]);
            CompoundTag laneTag = new CompoundTag();
            lanes[i].writeToNBT(laneTag);
            tag.put("lane" + i, laneTag);
        }
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        for (int i = 0; i < inputTanks.length; i++) if (tag.contains("inputTank" + i)) inputTanks[i].readNBT(tag.getCompound("inputTank" + i));
        for (int i = 0; i < outputTanks.length; i++) if (tag.contains("outputTank" + i)) outputTanks[i].readNBT(tag.getCompound("outputTank" + i));
        if (tag.contains("water")) water.readNBT(tag.getCompound("water"));
        if (tag.contains("lps")) lps.readNBT(tag.getCompound("lps"));
        for (int i = 0; i < LANE_COUNT; i++) {
            didProcess[i] = tag.getBoolean("didProcess" + i);
            if (tag.contains("lane" + i)) lanes[i].readFromNBT(tag.getCompound("lane" + i));
        }
    }
}
