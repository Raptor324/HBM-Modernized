package com.hbm_m.blockentity.machines;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.energy.EnergyNetworkManager;
import com.hbm_m.api.fluids.IFluidConnectorMK2;
import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.api.item.IDesignatorItem;
import com.hbm_m.api.network.NodeDirPos;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.IRadarCommandReceiver;
import com.hbm_m.interfaces.IEnergyConnector;
import com.hbm_m.interfaces.IEnergyProvider;
import com.hbm_m.interfaces.IEnergyReceiver;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.missile.MissileBaseEntity;
import com.hbm_m.explosion.MissileWarheadEffects;
import com.hbm_m.particle.ModParticleTypes;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.item.missile.MissileItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Базовый BlockEntity для всех пусковых площадок.
 *
 * Инвентарь (7 слотов), энергия, жидкостные баки, пуск ракет и подписка
 * на энерго-/жидкостные сети через {@link #getConPos()} (угловые коннекторы мультиблока).
 */
public abstract class LaunchPadBaseBlockEntity extends BaseMachineBlockEntity
        implements IRadarCommandReceiver, IFluidStandardReceiverMK2 {

    public static final int TANK_CAPACITY_MB = 24_000;

    // Слоты как в старом контейнере:
    // 0 – ракета
    // 1 – дизайнатор
    // 2 – батарея
    // 3 – топливо (вход)
    // 4 – топливо (выход)
    // 5 – окислитель (вход)
    // 6 – окислитель (выход)
    public static final int SLOT_MISSILE = 0;
    public static final int SLOT_DESIGNATOR = 1;

    /** Missile slot index; rusted pad overrides (same index 0). */
    public int getMissileSlot() {
        return SLOT_MISSILE;
    }

    /** Designator slot index; rusted pad uses slot 3. */
    public int getDesignatorSlot() {
        return SLOT_DESIGNATOR;
    }
    public static final int SLOT_BATTERY = 2;
    public static final int SLOT_FUEL_IN = 3;
    public static final int SLOT_FUEL_OUT = 4;
    public static final int SLOT_OXIDIZER_IN = 5;
    public static final int SLOT_OXIDIZER_OUT = 6;
    public static final int SLOT_COUNT = 7;

    public static final int STATE_MISSING = 0;
    public static final int STATE_LOADING = 1;
    public static final int STATE_READY = 2;

    protected static final long MAX_POWER = 100_000L;
    protected static final long MAX_RECEIVE = 1_000L;

    /** Регистрация ракет: Item -> тип сущности (заполняется в {@link #registerLaunchables()}). */
    protected static final Map<Item, EntityType<? extends MissileBaseEntity>> MISSILES = new HashMap<>();

    /**
     * Связывает предметы ракет с типами сущностей. Вызывается из {@code MainRegistry.commonSetup}.
     */
    public static void registerLaunchables() {
        MISSILES.clear();

        MISSILES.put(ModItems.MISSILE_TEST.get(), ModEntities.MISSILE_TEST.get());
        MISSILES.put(ModItems.MISSILE_ABM.get(), ModEntities.MISSILE_ABM.get());

        // Tier 0
        MISSILES.put(ModItems.MISSILE_MICRO.get(), ModEntities.MISSILE_MICRO.get());
        MISSILES.put(ModItems.MISSILE_SCHRABIDIUM.get(), ModEntities.MISSILE_SCHRABIDIUM.get());
        MISSILES.put(ModItems.MISSILE_BHOLE.get(), ModEntities.MISSILE_BHOLE.get());
        MISSILES.put(ModItems.MISSILE_TAINT.get(), ModEntities.MISSILE_TAINT.get());
        MISSILES.put(ModItems.MISSILE_EMP.get(), ModEntities.MISSILE_EMP.get());

        // Tier 1
        MISSILES.put(ModItems.MISSILE_GENERIC.get(), ModEntities.MISSILE_GENERIC.get());
        MISSILES.put(ModItems.MISSILE_INCENDIARY.get(), ModEntities.MISSILE_INCENDIARY.get());
        MISSILES.put(ModItems.MISSILE_CLUSTER.get(), ModEntities.MISSILE_CLUSTER.get());
        MISSILES.put(ModItems.MISSILE_BUSTER.get(), ModEntities.MISSILE_BUSTER.get());
        MISSILES.put(ModItems.MISSILE_DECOY.get(), ModEntities.MISSILE_DECOY.get());
        MISSILES.put(ModItems.MISSILE_STEALTH.get(), ModEntities.MISSILE_STEALTH.get());

        MISSILES.put(ModItems.MISSILE_STRONG.get(), ModEntities.MISSILE_STRONG.get());
        MISSILES.put(ModItems.MISSILE_INCENDIARY_STRONG.get(), ModEntities.MISSILE_INCENDIARY_STRONG.get());
        MISSILES.put(ModItems.MISSILE_CLUSTER_STRONG.get(), ModEntities.MISSILE_CLUSTER_STRONG.get());
        MISSILES.put(ModItems.MISSILE_BUSTER_STRONG.get(), ModEntities.MISSILE_BUSTER_STRONG.get());
        MISSILES.put(ModItems.MISSILE_EMP_STRONG.get(), ModEntities.MISSILE_EMP_STRONG.get());

        MISSILES.put(ModItems.MISSILE_BURST.get(), ModEntities.MISSILE_BURST.get());
        MISSILES.put(ModItems.MISSILE_INFERNO.get(), ModEntities.MISSILE_INFERNO.get());
        MISSILES.put(ModItems.MISSILE_RAIN.get(), ModEntities.MISSILE_RAIN.get());
        MISSILES.put(ModItems.MISSILE_DRILL.get(), ModEntities.MISSILE_DRILL.get());
        MISSILES.put(ModItems.MISSILE_SHUTTLE.get(), ModEntities.MISSILE_SHUTTLE.get());

        MISSILES.put(ModItems.MISSILE_NUCLEAR.get(), ModEntities.MISSILE_NUCLEAR.get());
        MISSILES.put(ModItems.MISSILE_NUCLEAR_CLUSTER.get(), ModEntities.MISSILE_NUCLEAR_CLUSTER.get());
        MISSILES.put(ModItems.MISSILE_VOLCANO.get(), ModEntities.MISSILE_VOLCANO.get());
        MISSILES.put(ModItems.MISSILE_DOOMSDAY.get(), ModEntities.MISSILE_DOOMSDAY.get());
        MISSILES.put(ModItems.MISSILE_DOOMSDAY_RUSTED.get(), ModEntities.MISSILE_DOOMSDAY_RUSTED.get());
    }

    @Nullable
    public static Item getLaunchItemFor(EntityType<?> entityType) {
        for (Map.Entry<Item, EntityType<? extends MissileBaseEntity>> entry : MISSILES.entrySet()) {
            if (entry.getValue() == entityType) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Текущее логическое состояние площадки (для GUI).
     */
    protected int state = STATE_MISSING;

    protected int redstonePower = 0;
    protected int prevRedstonePower = 0;

    protected ItemStack clientMissilePreview = ItemStack.EMPTY;
    private ItemStack lastSyncedMissile = ItemStack.EMPTY;

    /**
     * Кулдаун перед следующим пуском. Уменьшается каждый серверный тик.
     * Когда > 0 — площадка считается «загружающейся» (STATE_LOADING).
     */
    protected int delay = 0;

    /** Топливный и окислительный баки (как в оригинале — по 24k mB). */
    protected final FluidTank[] tanks = new FluidTank[] {
            new FluidTank(TANK_CAPACITY_MB),
            new FluidTank(TANK_CAPACITY_MB)
    };

    protected LaunchPadBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, SLOT_COUNT, MAX_POWER, MAX_RECEIVE);
        tanks[0].setTankType(ModFluids.NONE.getSource());
        tanks[1].setTankType(ModFluids.NONE.getSource());
    }

    protected static NodeDirPos[] buildStandardConPos(BlockPos controller) {
        int x = controller.getX();
        int y = controller.getY();
        int z = controller.getZ();
        return new NodeDirPos[] {
                new NodeDirPos(x + 2, y, z - 1, Direction.EAST),
                new NodeDirPos(x + 2, y, z + 1, Direction.EAST),
                new NodeDirPos(x - 2, y, z - 1, Direction.WEST),
                new NodeDirPos(x - 2, y, z + 1, Direction.WEST),
                new NodeDirPos(x - 1, y, z + 2, Direction.SOUTH),
                new NodeDirPos(x + 1, y, z + 2, Direction.SOUTH),
                new NodeDirPos(x - 1, y, z - 2, Direction.NORTH),
                new NodeDirPos(x + 1, y, z - 2, Direction.NORTH),
        };
    }

    /** Угловые коннекторы мультиблока для подписки в энерго-/жидкостные сети. */
    public abstract NodeDirPos[] getConPos();

    public static void clientLaunchPadSmokeTick(Level level, BlockPos pos, BlockState state) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        List<MissileBaseEntity> entities = level.getEntitiesOfClass(MissileBaseEntity.class,
                new AABB(x - 0.5, y, z - 0.5, x + 1.5, y + 10, z + 1.5));

        if (entities.isEmpty()) {
            return;
        }

        SimpleParticleType smoke = ModParticleTypes.SMOKE_COLUMN.get();
        for (int i = 0; i < 15; i++) {
            Direction dir = state.getValue(HorizontalDirectionalBlock.FACING);
            if (level.random.nextBoolean()) {
                dir = dir.getOpposite();
            }
            if (level.random.nextBoolean()) {
                dir = dir.getClockWise();
            }
            float moX = (float) (level.random.nextGaussian() * 0.15F + 0.75) * dir.getStepX();
            float moZ = (float) (level.random.nextGaussian() * 0.15F + 0.75) * dir.getStepZ();
            level.addParticle(smoke, x + 0.5, y + 0.25, z + 0.5, moX, 0.0D, moZ);
        }
    }

    /**
     * Общий server‑tick для всех пусковых площадок.
     */
    protected static void commonServerTick(Level level, BlockPos pos, BlockState state,
                                           LaunchPadBaseBlockEntity be) {
        if (level.isClientSide) {
            return;
        }
        be.ensureNetworkInitialized();

        if (level.getGameTime() % 20 == 0) {
            be.subscribeToCornerNetworks(level);
        }

        // 0. Подзарядка из батарейки в слоте 2 (как в оригинале Library.chargeTEFromItems).
        be.chargeLaunchPadBattery();

        boolean fluidsChanged = be.transferFluidContainers();

        boolean tanksChanged = false;
        if (be.isMissileValid()) {
            ItemStack missileStack = be.inventory.getStackInSlot(SLOT_MISSILE);
            if (missileStack.getItem() instanceof MissileItem missileItem) {
                tanksChanged = be.setFuel(missileItem);
            }
        }

        // 1. Кулдаун перезарядки.
        if (be.delay > 0) {
            be.delay--;
        }

        // 2. Пересчёт состояния для GUI.
        // Шкала топлива (getFuelState) и hasFuel — разные вещи: SOLID не имеет gauge, но считается заправленным.
        boolean ready = be.isMissileValid() && be.hasFuel();
        if (!ready) {
            be.state = STATE_MISSING;
            // Если ракета снята или нет топлива — сбрасываем кулдаун с запасом,
            // как в оригинале (предотвращает мгновенный пуск свежепоставленной ракеты).
            be.delay = Math.max(be.delay, 100);
        } else if (be.delay > 0 || !be.isReadyForLaunch()) {
            be.state = STATE_LOADING;
        } else {
            be.state = STATE_READY;
        }

        // 3. Триггер по фронту редстоуна: 0 → положительный.
        if (be.redstonePower > 0 && be.prevRedstonePower <= 0) {
            be.launchFromDesignator();
        }
        be.prevRedstonePower = be.redstonePower;

        ItemStack missileNow = be.inventory.getStackInSlot(SLOT_MISSILE);
        boolean missileChanged = !ItemStack.matches(missileNow, be.lastSyncedMissile);
        if (missileChanged) {
            be.lastSyncedMissile = missileNow.copy();
        }
        if (missileChanged || tanksChanged || fluidsChanged) {
            be.setChanged();
            be.sendUpdateToClient();
        } else if (level.getGameTime() % 10 == 0) {
            be.setChanged();
            be.sendUpdateToClient();
        }
    }

    /** Креативная батарея мгновенно заполняет буфер; обычные — через {@link #chargeFromBatterySlot}. */
    protected void chargeLaunchPadBattery() {
        ItemStack batteryStack = inventory.getStackInSlot(SLOT_BATTERY);
        if (!batteryStack.isEmpty() && batteryStack.getItem() instanceof ItemCreativeBattery) {
            setEnergyStored(getMaxEnergyStored());
            return;
        }
        chargeFromBatterySlot(SLOT_BATTERY);
    }

    /**
     * Заливка бочек в баки. Мутации слотов пишутся обратно в инвентарь
     * (как в {@link MachineChemicalPlantBlockEntity#transferFluidsFromItems}).
     */
    protected boolean transferFluidContainers() {
        ItemStack[] slots = new ItemStack[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) {
            slots[i] = inventory.getStackInSlot(i);
        }
        boolean changed = false;
        if (tanks[0].loadTank(SLOT_FUEL_IN, SLOT_FUEL_OUT, slots)) {
            inventory.setStackInSlot(SLOT_FUEL_IN, slots[SLOT_FUEL_IN]);
            inventory.setStackInSlot(SLOT_FUEL_OUT, slots[SLOT_FUEL_OUT]);
            changed = true;
        }
        if (tanks[1].loadTank(SLOT_OXIDIZER_IN, SLOT_OXIDIZER_OUT, slots)) {
            inventory.setStackInSlot(SLOT_OXIDIZER_IN, slots[SLOT_OXIDIZER_IN]);
            inventory.setStackInSlot(SLOT_OXIDIZER_OUT, slots[SLOT_OXIDIZER_OUT]);
            changed = true;
        }
        return changed;
    }

    /** Tall missiles extend above the 1-block pad; default BE AABB would cull the BER. */
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(1.0D, 7.0D, 1.0D);
    }

    /**
     * Missile stack for BER / client preview. On client uses synced copy when GUI is closed.
     */
    public ItemStack getMissilePreviewStack() {
        ItemStack missile = inventory.getStackInSlot(getMissileSlot());
        if (level != null && level.isClientSide) {
            if (!missile.isEmpty()) {
                return missile;
            }
            if (!clientMissilePreview.isEmpty()) {
                return clientMissilePreview;
            }
        }
        return missile;
    }

    /**
     *
     * @return {@code true}, если тип хотя бы одного бака изменился (для клиентской синхронизации)
     */
    protected boolean setFuel(MissileItem missile) {
        Fluid prevFuel = tanks[0].getTankType();
        Fluid prevOxidizer = tanks[1].getTankType();
        switch (missile.fuel) {
            case ETHANOL_PEROXIDE -> {
                tanks[0].setTankType(ModFluids.ETHANOL.getSource());
                tanks[1].setTankType(ModFluids.PEROXIDE.getSource());
            }
            case KEROSENE_PEROXIDE -> {
                tanks[0].setTankType(ModFluids.KEROSENE.getSource());
                tanks[1].setTankType(ModFluids.PEROXIDE.getSource());
            }
            case KEROSENE_LOXY -> {
                tanks[0].setTankType(ModFluids.KEROSENE.getSource());
                tanks[1].setTankType(ModFluids.OXYGEN.getSource());
            }
            case JETFUEL_LOXY -> {
                tanks[0].setTankType(ModFluids.KEROSENE_REFORM.getSource());
                tanks[1].setTankType(ModFluids.OXYGEN.getSource());
            }
            case SOLID -> { /* предзаправленные — баки не используются */ }
        }
        return tanks[0].getTankType() != prevFuel || tanks[1].getTankType() != prevOxidizer;
    }

    protected int getGaugeState(int tankIndex) {
        ItemStack missileStack = inventory.getStackInSlot(SLOT_MISSILE);
        if (!(missileStack.getItem() instanceof MissileItem missile)) {
            return 0;
        }
        if (missile.fuel == MissileItem.MissileFuel.SOLID) {
            return 0;
        }
        return tanks[tankIndex].getFill() >= missile.fuelCap ? 1 : -1;
    }

    // -----------------------
    // API для GUI / меню
    // -----------------------

    public int getState() {
        return state;
    }

    /**
     * 0  — нет шкалы (твердотопливные ракеты, отсутствует/неверная ракета)
     * 1  — бак полон (топлива хватает для пуска)
     * -1 — топливо есть, но недостаточно
     */
    public int getFuelState() {
        ItemStack missileStack = inventory.getStackInSlot(SLOT_MISSILE);
        if (!isMissileValid(missileStack)) {
            return 0;
        }
        MissileItem missile = (MissileItem) missileStack.getItem();
        return getGaugeState(0);
    }

    public int getOxidizerState() {
        return getGaugeState(1);
    }

    public boolean hasFuel() {
        if (this.energy < 75_000L) {
            return false;
        }
        ItemStack missileStack = inventory.getStackInSlot(SLOT_MISSILE);
        if (!(missileStack.getItem() instanceof MissileItem missile)) {
            return false;
        }
        if (missile.fuel == MissileItem.MissileFuel.SOLID) {
            return true;
        }
        // WIP: проверка заполнения баков временно отключена — для пуска достаточно ракеты, энергии и цели.
        // return tanks[0].getFill() >= missile.fuelCap && tanks[1].getFill() >= missile.fuelCap;
        return true;
    }

    public boolean isMissileValid() {
        ItemStack stack = inventory.getStackInSlot(SLOT_MISSILE);
        return isMissileValid(stack);
    }

    public boolean isMissileValid(@NotNull net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (!(stack.getItem() instanceof MissileItem missile)) {
            return false;
        }
        return missile.launchable && MISSILES.containsKey(stack.getItem());
    }

    public boolean canLaunch() {
        return isMissileValid() && hasFuel() && isReadyForLaunch();
    }

    /** Дополнительные условия в конкретной площадке (задержка, анимации и т.п.). */
    protected abstract boolean isReadyForLaunch();

    /**
     * Запуск по координатам (X/Z). Возвращает true, если ракета реально запущена.
     */
    public boolean launchToCoordinate(int targetX, int targetZ) {
        if (!canLaunch() || level == null || level.isClientSide) {
            return false;
        }

        Entity missile = instantiateMissile(targetX, targetZ);
        if (missile != null) {
            finalizeLaunch(missile);
            return true;
        }
        return false;
    }

    /**
     * Запуск по дизайнатору (слот 1).
     * Если {@link #needsDesignator(Item)} == true (по умолчанию для всех баллистических ракет),
     * требуется заряженный/валидный дизайнатор; иначе пуск отменяется.
     */
    public boolean launchFromDesignator() {
        if (!canLaunch() || level == null || level.isClientSide) {
            return false;
        }

        ItemStack designatorStack = inventory.getStackInSlot(getDesignatorSlot());
        ItemStack missileStack = inventory.getStackInSlot(getMissileSlot());
        boolean needsDesignator = needsDesignator(missileStack.getItem());

        int targetX = worldPosition.getX();
        int targetZ = worldPosition.getZ();

        if (designatorStack.getItem() instanceof IDesignatorItem designator) {
            if (designator.isReady(level, designatorStack, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ())) {
                net.minecraft.world.phys.Vec3 coords = designator.getCoords(level, designatorStack,
                        worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
                targetX = (int) Math.floor(coords.x);
                targetZ = (int) Math.floor(coords.z);
            } else if (needsDesignator) {
                return false;
            }
        } else if (needsDesignator) {
            return false;
        }

        return launchToCoordinate(targetX, targetZ);
    }

    /**
     * Пуск по сущности-цели (радар, сопровождение).
     */
    public boolean launchToEntity(Entity entity) {
        if (!canLaunch() || level == null || level.isClientSide) {
            return false;
        }
        Entity missile = instantiateMissile((int) Math.floor(entity.getX()), (int) Math.floor(entity.getZ()));
        if (missile != null) {
            finalizeLaunch(missile);
            return true;
        }
        return false;
    }

    @Override
    public boolean sendCommandPosition(BlockPos pos) {
        return launchToCoordinate(pos.getX(), pos.getZ());
    }

    @Override
    public boolean sendCommandEntity(Entity target) {
        return launchToEntity(target);
    }

    /**
     * Нужен ли дизайнатор для запуска данной ракеты.
     * По умолчанию — да, как в оригинале (баллистические ракеты не летают «в никуда»).
     * Подклассы могут переопределить (например, для FOB‑шаттлов).
     */
    public boolean needsDesignator(Item item) {
        // ABM запускается без дизайнатора (порт TileEntityLaunchPadBase.needsDesignator:
        // return item != ModItems.missile_anti_ballistic).
        return item != com.hbm_m.item.ModItems.MISSILE_ABM.get();
    }

    /**
     * Создание сущности ракеты из текущего слота.
     */
    protected Entity instantiateMissile(int targetX, int targetZ) {
        if (level == null) {
            return null;
        }
        ItemStack stack = inventory.getStackInSlot(SLOT_MISSILE);
        if (!isMissileValid(stack)) {
            return null;
        }

        EntityType<? extends MissileBaseEntity> type = MISSILES.get(stack.getItem());
        if (type == null) {
            return null;
        }

        MissileBaseEntity missile = type.create(level);
        missile.initLaunch(
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + getLaunchOffset(),
                worldPosition.getZ() + 0.5D,
                targetX, targetZ
        );
        BlockState padState = level.getBlockState(worldPosition);
        if (padState.hasProperty(HorizontalDirectionalBlock.FACING)) {
            missile.setLaunchFacing(padState.getValue(HorizontalDirectionalBlock.FACING));
        }
        return missile;
    }

    /**
     * Финализация пуска: спавн entity, звук, расход ресурсов.
     */
    protected void finalizeLaunch(Entity missile) {
        if (level == null || level.isClientSide) {
            return;
        }

        level.addFreshEntity(missile);
        if (level instanceof ServerLevel server) {
            MissileWarheadEffects.spawnLaunchSmoke(server,
                    worldPosition.getX() + 0.5D,
                    worldPosition.getY() + getLaunchOffset(),
                    worldPosition.getZ() + 0.5D);
        }
        level.playSound(null,
                worldPosition.getX() + 0.5D,
                worldPosition.getY(),
                worldPosition.getZ() + 0.5D,
                com.hbm_m.sound.ModSounds.MISSILE_TAKEOFF.get(),
                SoundSource.PLAYERS,
                2.0F, 1.0F);

        this.energy = Math.max(0, this.energy - 75_000L);

        ItemStack stack = inventory.getStackInSlot(SLOT_MISSILE);
        // WIP: расход топлива из баков временно отключён.
        /*
        if (stack.getItem() instanceof MissileItem missileItem
                && missileItem.fuel != MissileItem.MissileFuel.SOLID) {
            tanks[0].setFill(tanks[0].getFill() - missileItem.fuelCap);
            tanks[1].setFill(tanks[1].getFill() - missileItem.fuelCap);
        }
        */
        stack.shrink(1);
        // Кулдаун до следующего пуска
        this.delay = 100;
        setChanged();
    }

    // -----------------------
    // IBomb‑совместимый запуск
    // -----------------------

    /**
     * Пуск, инициируемый внешним триггером (например, IBomb.explode).
     * Возвращает совместимый с системой бомб код результата.
     */
    public com.hbm_m.api.bomb.IBomb.BombReturnCode triggerLaunch() {
        if (!canLaunch() || level == null || level.isClientSide) {
            return com.hbm_m.api.bomb.IBomb.BombReturnCode.ERROR_MISSING_COMPONENT;
        }
        return launchFromDesignator()
                ? com.hbm_m.api.bomb.IBomb.BombReturnCode.LAUNCHED
                : com.hbm_m.api.bomb.IBomb.BombReturnCode.ERROR_MISSING_COMPONENT;
    }

    // -----------------------
    // Редстоун‑логика (упрощённый порт)
    // -----------------------

    /**
     * Агрегирует редстоун с контроллера и всех частей мультиблока (как у дверей).
     */
    public void checkRedstonePower() {
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState blockState = getBlockState();
        if (!(blockState.getBlock() instanceof IMultiblockController controller)) {
            return;
        }

        MultiblockStructureHelper helper = controller.getStructureHelper();
        Direction facing = blockState.getValue(HorizontalDirectionalBlock.FACING);

        boolean isPowered = level.hasNeighborSignal(worldPosition);
        if (!isPowered) {
            for (BlockPos partPos : helper.getAllPartPositions(worldPosition, facing)) {
                if (level.hasNeighborSignal(partPos)) {
                    isPowered = true;
                    break;
                }
            }
        }

        setControllerRedstone(isPowered);
    }

    public void setControllerRedstone(boolean powered) {
        this.redstonePower = powered ? 1 : -1;
    }

    /** Смещение точки старта ракеты относительно верха блока. */
    protected double getLaunchOffset() {
        return 1.0D;
    }

    // -----------------------
    // NBT
    // -----------------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("launchpad_state", state);
        tag.putInt("launchpad_redstone", redstonePower);
        tag.putInt("launchpad_prev_redstone", prevRedstonePower);
        tag.putInt("launchpad_delay", delay);
        ItemStack missile = inventory.getStackInSlot(SLOT_MISSILE);
        tag.putInt("missile_preview_id", missile.isEmpty() ? -1 : BuiltInRegistries.ITEM.getId(missile.getItem()));
        tanks[0].writeToNBT(tag, "T0");
        tanks[1].writeToNBT(tag, "T1");
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        state = tag.getInt("launchpad_state");
        redstonePower = tag.getInt("launchpad_redstone");
        prevRedstonePower = tag.getInt("launchpad_prev_redstone");
        delay = tag.getInt("launchpad_delay");
        if (tag.contains("T0")) {
            tanks[0].readFromNBT(tag, "T0");
        }
        if (tag.contains("T1")) {
            tanks[1].readFromNBT(tag, "T1");
        }
        readMissilePreviewFromTag(tag);
    }

    private void readMissilePreviewFromTag(CompoundTag tag) {
        int itemId = tag.getInt("missile_preview_id");
        if (itemId >= 0 && itemId < BuiltInRegistries.ITEM.size()) {
            clientMissilePreview = new ItemStack(BuiltInRegistries.ITEM.byId(itemId));
        } else {
            clientMissilePreview = ItemStack.EMPTY;
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    //? if forge {
    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }
    //?}

    // -----------------------
    // BaseMachineBlockEntity overrides
    // -----------------------

    @Override
    protected boolean isItemValidForSlot(int slot, net.minecraft.world.item.ItemStack stack) {
        // Пока никаких особых ограничений, кроме базового количества слотов.
        return slot >= 0 && slot < SLOT_COUNT;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        // Конкретное меню зависит от типа площадки (обычная / большая / ржавая),
        // поэтому реализацию оставляем в подклассах.
        throw new UnsupportedOperationException("LaunchPadBaseBlockEntity is abstract; createMenu must be implemented in subclasses.");
    }

    @Override
    public boolean canConnectEnergy(Direction side) {
        return side != Direction.UP && side != Direction.DOWN;
    }

    public FluidTank[] getTanks() {
        return tanks;
    }

    protected void subscribeToCornerNetworks(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        EnergyNetworkManager energyMgr = EnergyNetworkManager.get(serverLevel);

        for (NodeDirPos con : getConPos()) {
            BlockPos pipePos = con.getPos();
            Direction dir = con.getDir();
            if (dir == null || !level.isLoaded(pipePos)) {
                continue;
            }

            BlockEntity pipeBe = level.getBlockEntity(pipePos);
            if (pipeBe == null) {
                continue;
            }

            if (isEnergyBlock(pipeBe) && !energyMgr.hasNode(pipePos)) {
                energyMgr.addNode(pipePos);
            }

            if (pipeBe instanceof IFluidConnectorMK2) {
                Fluid fuelType = tanks[0].getTankType();
                if (isSubscribableFluid(fuelType)) {
                    trySubscribe(fuelType, level, pipePos, dir);
                }
                Fluid oxidizerType = tanks[1].getTankType();
                if (isSubscribableFluid(oxidizerType)) {
                    trySubscribe(oxidizerType, level, pipePos, dir);
                }
            }
        }
    }

    private static boolean isSubscribableFluid(Fluid fluid) {
        return fluid != null && fluid != Fluids.EMPTY && fluid != ModFluids.NONE.getSource();
    }

    private static boolean isEnergyBlock(BlockEntity be) {
        return be instanceof IEnergyConnector
                || be instanceof IEnergyProvider
                || be instanceof IEnergyReceiver;
    }

    // --- IFluidStandardReceiverMK2 ---

    @Override
    public FluidTank[] getReceivingTanks() {
        return tanks;
    }

    @Override
    public FluidTank[] getAllTanks() {
        return tanks;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != Direction.UP && fromDir != Direction.DOWN;
    }
}
