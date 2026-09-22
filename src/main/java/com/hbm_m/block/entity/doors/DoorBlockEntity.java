package com.hbm_m.block.entity.doors;


import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.decorations.DoorBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.client.model.variant.DoorModelRegistry;
import com.hbm_m.client.model.variant.DoorModelSelection;
import com.hbm_m.client.model.variant.DoorModelType;
import com.hbm_m.client.model.variant.DoorSkin;
import com.hbm_m.client.overlay.DoorAnimationDelayHelper;
import com.hbm_m.client.render.DoorChunkInvalidationHelper;
import com.hbm_m.interfaces.IMultiblockPart;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;
import com.hbm_m.sound.ClientSoundBootstrap;
import com.hbm_m.platform.PlatformHooks;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
// Forge-only model-data / distmarker imports intentionally removed for Fabric compilation.


public class DoorBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity implements IMultiblockPart, com.hbm_m.interfaces.ILockable
    //? if fabric {
    /*, net.fabricmc.fabric.api.rendering.data.v1.RenderAttachmentBlockEntity
    *///?}
{
    private static final String DOOR_LOOP_SOUND_FACTORY = "com.hbm_m.client.sound.DoorLoopSoundFactory";

    // 0=закрыта, 1=открыта, 2=закрывается, 3=открывается
    public byte state = 0;
    private int openTicks = 0;
    /** Значение openTicks на предыдущем тике — для интерполяции по partialTick (порт 1.7.10 prevOpenTicks). */
    private int prevOpenTicks = 0;
    public long animStartTime = 0;
    private boolean locked = false;
    private boolean lastRedstoneState = false;

    // ==================== Замок (порт TileEntityLockableBase) ====================
    /** Пин-код замка. 0 = не установлен; замок с кодом 0 повесить нельзя. */
    private int lock = 0;
    /** Базовый шанс взлома отмычкой (0.1 = 10%). */
    private double lockMod = 0.1D;
    /** Можно ли сделать поддельный ключ (key_kit). */
    private boolean cheesable = true;

    /**
     * Текущий выбор модели и скина
     */
    private DoorModelSelection modelSelection = DoorModelSelection.DEFAULT;
    
    /**
     * Кэшированные ModelData для производительности.
     *
     * <p>ВНИМАНИЕ: поле НЕ помечено @OnlyIn(Dist.CLIENT)/@Environment(EnvType.CLIENT).
     * Раньше было, но Forge {@code RuntimeDistCleaner} удаляет @OnlyIn(Dist.CLIENT) ПОЛЯ
     * (а не только методы) на dedicated-сервере → серверный {@code load(CompoundTag)}
     * (m_142466_) падал с {@link NoSuchFieldError} на {@code this.cachedModelData = null;},
     * BlockEntity skipped → Create-disassembly не могла восстановить BlockEntity двери →
     * поезд разбирался наполовину, часть блоков пропадала бесследно. Двойная разборка —
     * первый цикл восстанавливал BE частично (BE null), сущность contraption оставалась
     * живой; второй цикл убивал contraption окончательно, теряя невосстановленные блоки.
     *
     * <p>Plain Object поле безопасно держать и на сервере (null по умолчанию, никто не
     * мутирует на сервере). Все клиент-специфичные методы, которые пишут/читают его,
     * остаются @OnlyIn(Dist.CLIENT) на уровне метода — runtimedistcleaner удаляет только
     * методы, а не поля, поэтому отсутствие @OnlyIn на поле безопасно.
     */
    private Object cachedModelData;

    private String doorDeclId;
    
    // Мультиблок данные
    private BlockPos controllerPos = null;
    private PartRole partRole = PartRole.DEFAULT;

    private java.util.Set<Direction> allowedClimbSides = java.util.EnumSet.noneOf(Direction.class);
    // См. комментарий у cachedModelData: @OnlyIn(Dist.CLIENT) на ПОЛЕ ломает загрузку
    // BlockEntity на dedicated-сервере (NoSuchFieldError после RuntimeDistCleaner) и
    // вторично ломает Create-disassembly поезда. Поле держим plain Object (null default).
    private Object loopingSound;

    /** Called from DoorAnimationDelayHelper when delay expires. Client-only. */

    //? if forge {
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    //?} elif fabric {
    /*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
    *///?} elif neoforge {
    /*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    *///?}
    public void clearAnimationDelayClient() {
        this.cachedModelData = null;
        // requestModelDataUpdate() is Forge-only (model data system). On Fabric it's a no-op.
    }

    public DoorBlockEntity(BlockPos pos, BlockState state, String doorDeclId) {
        super(ModBlockEntities.DOOR_ENTITY.get(), pos, state);
        this.doorDeclId = doorDeclId;
        DoorDecl decl = DoorDeclRegistry.getById(doorDeclId);
        this.modelSelection = decl != null ? decl.getDefaultModelSelection() : DoorModelSelection.DEFAULT;
    }

    public DoorBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, "large_vehicle_door");
    }

    /**
     * Получить текущий выбор модели
     */
    public DoorModelSelection getModelSelection() {
        return modelSelection;
    }

    //? if fabric {
    /*@Override
    public @org.jetbrains.annotations.Nullable Object getRenderAttachmentData() {
        boolean isMoving = state == 2 || state == 3;
        boolean isOpen = state == 1;
        boolean isOverlap = !isMoving && cachedModelData != null;
        return new DoorRenderData(modelSelection, isMoving, isOpen, isOverlap);
    }

    public record DoorRenderData(DoorModelSelection selection, boolean moving, boolean open, boolean overlap) {}
    *///?}
    
    /**
     * Установить выбор модели
     */
    public void setModelSelection(DoorModelSelection selection) {
        if (!this.modelSelection.equals(selection)) {
            this.modelSelection = selection;
            setChanged();
            
            // Инвалидируем кэш
            if (level != null && level.isClientSide) {
                this.cachedModelData = null;
                DoorChunkInvalidationHelper.scheduleChunkInvalidation(worldPosition);
            }
            
            syncToClient();
        }
    }
    
    /**
     * Установить тип модели
     */
    public void setModelType(DoorModelType type) {
        // Сохраняем текущий скин если переключаемся в рамках MODERN
        DoorSkin skin = type.isLegacy() ? DoorSkin.DEFAULT : this.modelSelection.getSkin();
        setModelSelection(new DoorModelSelection(type, skin));
    }
    
    /**
     * Установить скин (только для MODERN модели)
     */
    public void setSkin(DoorSkin skin) {
        if (this.modelSelection.isModern()) {
            setModelSelection(new DoorModelSelection(DoorModelType.MODERN, skin));
        }
    }
    
    /**
     * Быстрое переключение типа модели
     */
    public void toggleModelType() {
        DoorModelType newType = modelSelection.getModelType().isLegacy() 
            ? DoorModelType.MODERN 
            : DoorModelType.LEGACY;
        setModelType(newType);
    }
    
    /**
     * Сбросить к выбору по умолчанию
     */
    public void resetToDefault() {
        if (level != null) {
            DoorModelRegistry registry = DoorModelRegistry.getInstance();
            DoorModelSelection defaultSelection = registry.getDefaultSelection(doorDeclId);
            setModelSelection(defaultSelection);
        }
    }

    // ==================== IMultiblockPart ====================

    @Override
    public synchronized void setControllerPos(BlockPos pos) {
        this.controllerPos = pos;
        setChanged();
    }

    @Override
    @Nullable
    public BlockPos getControllerPos() {
        return controllerPos;
    }

    @Override
    public void setPartRole(PartRole role) {
        this.partRole = role;
        setChanged();
    }

    @Override
    public PartRole getPartRole() {
        return partRole;
    }

    public boolean isController() {
        return (controllerPos != null && controllerPos.equals(worldPosition)) || controllerPos == null;
    }

    @Nullable
    public DoorBlockEntity getController() {
        if (level == null) return null;
        if (controllerPos == null) return this;
        BlockEntity be = level.getBlockEntity(controllerPos);
        return be instanceof DoorBlockEntity ? (DoorBlockEntity) be : null;
    }

    /**
     * Вызывается после формирования структуры
     */
    public void onStructureFormed() {
        // Инициализация после создания мультиблока
        this.state = 0;
        this.openTicks = 0;
        this.animStartTime = System.currentTimeMillis();
        if (level != null && level.isClientSide) {
            initModelSelection(true); // Новая дверь - применить default из конфига
        }
        syncToClient();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (level instanceof ServerLevel serverLevel && !serverLevel.isClientSide()) {
            BlockPos pos = this.getBlockPos();
            // Ставим тик-задачу на СЛЕДУЮЩИЙ тик, а не выполняем прямо сейчас
            serverLevel.getServer().tell(new net.minecraft.server.TickTask(
                    serverLevel.getServer().getTickCount() + 1,
                    () -> {
                        if (serverLevel.isLoaded(pos)) {
                            BlockState state = serverLevel.getBlockState(pos);
                            if (state.getBlock() instanceof IMultiblockController controller) {
                                controller.getStructureHelper().attemptAutoRepair(serverLevel, pos, state, controller);
                            }
                        }
                    }
            ));
        }
    }

    // ==================== Публичные методы ====================

    public DoorDecl getDoorDecl() {
        // Если ID потерян, используем fallback
        if (doorDeclId == null || doorDeclId.isEmpty()) {
            if (getBlockState().getBlock() instanceof DoorBlock db) {
                return DoorDeclRegistry.getById(db.getDoorDeclId());
            }
            return DoorDecl.LARGE_VEHICLE_DOOR;
        }
        return DoorDeclRegistry.getById(doorDeclId);
    }
    
    // Для серверной логики используем строковый ID
    public String getDoorDeclId() {
        return doorDeclId;
    }

    public Direction getFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(DoorBlock.FACING)
            ? state.getValue(DoorBlock.FACING)
            : Direction.NORTH;
    }

    public void checkRedstonePower() {
        if (level == null || level.isClientSide) return;
    
        BlockState blockState = getBlockState();
        if (!(blockState.getBlock() instanceof DoorBlock doorBlock)) return;
    
        MultiblockStructureHelper helper = doorBlock.getStructureHelper();
        Direction facing = blockState.getValue(DoorBlock.FACING);
        
        // Проверяем сам контроллер
        boolean isPowered = level.hasNeighborSignal(worldPosition);
    
        // Если контроллер не запитан, проверяем все фантомы
        if (!isPowered) {
            for (BlockPos partPos : helper.getAllPartPositions(worldPosition, facing)) {
                if (level.hasNeighborSignal(partPos)) {
                    isPowered = true;
                    break;
                }
            }
        }
    
        // Передаем итоговый результат в логику обработки
        updateRedstoneState(isPowered);
    }

    /**
     * Логика обработки редстоун-импульсов
     */
    private void updateRedstoneState(boolean powered) {
        if (powered == this.lastRedstoneState) return;
        this.lastRedstoneState = powered;

        // Порт TileEntityDoorGeneric.updateEntity: запертая дверь игнорирует редстоун
        // (оригинал звал tryToggle(-1), который отказывал при isLocked)
        if (isLocked()) {
            setChanged();
            return;
        }

        if (powered) {
            // Паритет 1.7.10: авто-открытие только из полностью закрытого состояния
            if (state == 0) {
                open();
            }
        } else {
            // Сигнал пропал: авто-закрытие только из полностью открытого состояния
            if (state == 1) {
                close();
            }
        }
        setChanged();
    }
 

    private int getServerOpenTime() {
        DoorDecl decl = getDoorDecl();
        return decl != null ? decl.getOpenTime() : 60;
    }

    // ОБНОВИТЕ существующий метод getOpenProgress(float):
    public float getOpenProgress(float partialTick) {
        int openTime = getServerOpenTime();
        if (openTime <= 0) return state == 1 || state == 3 ? 1f : 0f;

        // Тиковая анимация с интерполяцией по partialTick (как в 1.7.10):
        // openTicks двигается клиентским тикером синхронно с серверным, без wall-clock.
        // openTicks ВСЕГДА = текущая позиция створки (0=закрыто, openTime=открыто)
        // для обоих направлений, поэтому формула единая.
        float ticks = prevOpenTicks + (openTicks - prevOpenTicks) * partialTick;
        return Math.max(0f, Math.min(1f, ticks / openTime));
    }

    /**
     * Получает прогресс открытия БЕЗ партиальных тиков (для серверного использования).
     * @return прогресс от 0.0 до 1.0
     */
    public float getOpenProgress() {
        return getOpenProgress(0f); // Используем 0 партиальных тиков для сервера
    }
    
    public byte getState() {
        return this.state;
    }

    public long getAnimStartTime() {
        return animStartTime;
    }

    public int getSkinIndex() {
        return 0; // Реализовать при необходимости
    }

    // ==================== State Management ====================

    public void open() {
        if (state == 0 || state == 2) {
            setState((byte) 3);
        }
    }

    public void close() {
        if (state == 1 || state == 3) {
            setState((byte) 2);
        }
    }

    public void toggle() {
        tryToggle(null);
    }

    /**
     * Порт {@code TileEntityDoorGeneric.tryToggle(EntityPlayer)}: запертая дверь не
     * отвечает на редстоун/взрыв (player == null) и на руку без доступа; закрытая
     * запитанная дверь не открывается рукой (как железная в ваниле).
     */
    public boolean tryToggle(@Nullable net.minecraft.world.entity.player.Player player) {
        if (isLocked() && player == null) return false;
        if (state == 2 || state == 3) {
            return false; // Дверь в процессе движения - переключение невозможно
        }
        if (state == 0 && lastRedstoneState) {
            return false; // Запитанная закрытая дверь вручную не открывается
        }

        if (state == 0) {
            if (level != null && !level.isClientSide) {
                if (canAccess(player)) {
                    open();
                } else {
                    notifyLocked(player);
                }
            }
            return true;
        } else if (state == 1) {
            if (level != null && !level.isClientSide) {
                if (canAccess(player)) {
                    close();
                } else {
                    notifyLocked(player);
                }
            }
            return true;
        }
        return false;
    }

    /** Поп-ап над хотбаром при отказе доступа (ключ не подошёл, взлом не удался). */
    private void notifyLocked(@Nullable net.minecraft.world.entity.player.Player player) {
        if (player != null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.hbm_m.door_locked"), true);
        }
    }

    /**
     * Проверяет, находится ли дверь в процессе движения.
     * На клиенте: включает задержку + grace period после полного открытия/закрытия -
     * анимированная часть остаётся видимой, пока baked model не пересоберётся.
     */
    public boolean isMoving() {
        if (state == 2 || state == 3) return true;
        if (level != null && level.isClientSide && DoorAnimationDelayHelper.isInDelayPeriod(this)) {
            return true;
        }
        return false;
    }

    private void setState(byte newState) {
        this.state = newState;
        this.animStartTime = System.currentTimeMillis();
        if (newState == 3) {
            this.openTicks = 0;
        } else if (newState == 2) {
            this.openTicks = getServerOpenTime(); // Используем серверный метод
        }
        this.prevOpenTicks = this.openTicks;
        // Обновляем BlockState с DOOR_MOVING и OPEN при изменении состояния
        if (level != null && !level.isClientSide) {
            boolean isMoving = newState == 2 || newState == 3;
            boolean isOpen = newState == 1;
            BlockState currentState = getBlockState();
            if (currentState.getBlock() instanceof DoorBlock) {
                boolean needsUpdate = false;
                BlockState newBlockState = currentState;
                if (currentState.hasProperty(DoorBlock.DOOR_MOVING)
                        && currentState.getValue(DoorBlock.DOOR_MOVING) != isMoving) {
                    newBlockState = newBlockState.setValue(DoorBlock.DOOR_MOVING, isMoving);
                    needsUpdate = true;
                }
                if (currentState.hasProperty(DoorBlock.OPEN)
                        && (newState == 0 || newState == 1)
                        && currentState.getValue(DoorBlock.OPEN) != isOpen) {
                    newBlockState = newBlockState.setValue(DoorBlock.OPEN, isOpen);
                    needsUpdate = true;
                }
                if (needsUpdate) {
                    level.setBlock(worldPosition, newBlockState, 3);
                }
            }
        }
        // Инвалидируем кэш ModelData при изменении состояния движения
        if (level != null && level.isClientSide) {
            this.cachedModelData = null;
            DoorChunkInvalidationHelper.scheduleChunkInvalidation(worldPosition);
        }
        syncToClient();
    }

    public boolean isOpen() { return state == 1; }
    public boolean isLocked() { return locked; }

    public void setLocked(boolean locked) {
        this.locked = locked;
        syncToClient();
    }

    // ==================== ILockable (порт TileEntityLockableBase) ====================

    @Override
    public int getPins() {
        return lock;
    }

    @Override
    public void setPins(int pins) {
        this.lock = pins;
        setChanged();
    }

    @Override
    public void lock() {
        if (lock == 0) {
            com.hbm_m.main.MainRegistry.LOGGER.error("Attempted to lock a door with no pins set at {}", worldPosition);
        }
        setLocked(true);
    }

    @Override
    public void unlock() {
        setLocked(false);
    }

    @Override
    public double getLockMod() {
        return lockMod;
    }

    @Override
    public void setLockMod(double mod) {
        this.lockMod = mod;
        setChanged();
    }

    @Override
    public boolean isCheesable() {
        return cheesable;
    }

    @Override
    public void setCheesable(boolean cheesable) {
        this.cheesable = cheesable;
        setChanged();
    }

    @Override
    public BlockPos getLockPos() {
        return worldPosition;
    }

    @Override
    public Level getLockLevel() {
        return level;
    }

    // ==================== Server Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, DoorBlockEntity be) {
        int openTime = be.getServerOpenTime();
        boolean shouldSync = false;
        be.prevOpenTicks = be.openTicks;

        if (be.state == 3) { // Opening
            be.openTicks++;
            if (be.openTicks >= openTime) {
                be.state = 1;
                be.openTicks = openTime;
                shouldSync = true;
                // Обновляем BlockState OPEN для baked-геометрии
                if (state.hasProperty(DoorBlock.OPEN)) {
                    level.setBlock(pos, state.setValue(DoorBlock.DOOR_MOVING, false).setValue(DoorBlock.OPEN, true), 3);
                }
                be.notifyNeighborsOfStateChange(level, pos);
            }
        } else if (be.state == 2) { // Closing
            be.openTicks--;
            if (be.openTicks <= 0) {
                be.state = 0;
                be.openTicks = 0;
                shouldSync = true;
                // Обновляем BlockState OPEN для baked-геометрии
                if (state.hasProperty(DoorBlock.OPEN)) {
                    level.setBlock(pos, state.setValue(DoorBlock.DOOR_MOVING, false).setValue(DoorBlock.OPEN, false), 3);
                }
                be.notifyNeighborsOfStateChange(level, pos);
            }
        }

        if (be.state == 2 || be.state == 3) {
            DoorDecl decl = be.getDoorDecl();
            if (decl != null) {
                decl.onTick(be);
            }
        }
    
        if (shouldSync) {
            be.syncToClient();
        }
    }

    /**
     * Клиентский тикер: двигает openTicks синхронно с сервером (порт механики
     * 1.7.10, где TE тикал на обеих сторонах). Без синков и block updates.
     * В терминальных состояниях (0/1) "добегает" до концевого положения, чтобы
     * финальный sync-пакет (приходящий на 1-2 тика раньше локального счётчика)
     * не телепортировал створку в конец анимации.
     */
    public static void clientTick(Level level, BlockPos pos, BlockState state, DoorBlockEntity be) {
        be.prevOpenTicks = be.openTicks;
        int openTime = be.getServerOpenTime();
        if (openTime <= 0) return;

        switch (be.state) {
            case 3, 1 -> { if (be.openTicks < openTime) be.openTicks++; } // открытие/добегание до открытой
            case 2, 0 -> { if (be.openTicks > 0) be.openTicks--; }        // закрытие/добегание до закрытой
        }
    }

    private void notifyNeighborsOfStateChange(Level level, BlockPos controllerPos) {
        BlockState blockState = getBlockState();
        if (!(blockState.getBlock() instanceof DoorBlock doorBlock)) return;
        
        Direction facing = blockState.getValue(DoorBlock.FACING);
        MultiblockStructureHelper structureHelper = doorBlock.getStructureHelper();
        boolean isOpen = this.state != 0;
        
        // Контроллер: флаг 2 (NOTIFY_CLIENTS) - оповещаем клиентов о смене блокстейта.
        // updateNeighborsAt только для контроллера (редстоун и т.д.), не для каждого блока двери.
        BlockState controllerState = level.getBlockState(controllerPos);
        level.sendBlockUpdated(controllerPos, controllerState, controllerState, 2);
        level.updateNeighborsAt(controllerPos, controllerState.getBlock());
        level.getLightEngine().checkBlock(controllerPos);

        for (BlockPos partPos : structureHelper.getAllPartPositions(controllerPos, facing)) {
            BlockState partState = level.getBlockState(partPos);
            if (partState.hasProperty(com.hbm_m.block.UniversalMachinePartBlock.PASSABLE)) {
                boolean currentPassable = partState.getValue(com.hbm_m.block.UniversalMachinePartBlock.PASSABLE);
                if (currentPassable != isOpen) {
                    level.setBlock(partPos, partState.setValue(com.hbm_m.block.UniversalMachinePartBlock.PASSABLE, isOpen), 2);
                }
            }
            partState = level.getBlockState(partPos);
            level.sendBlockUpdated(partPos, partState, partState, 2);
            level.getLightEngine().checkBlock(partPos);
        }
    }

    public DoorDecl getServerDoorDecl() {
        return DoorDeclRegistry.getById(this.doorDeclId);
    }

    // ==================== Пошаговая коллизия (порт 1.7.10) ====================

    /**
     * Прогресс открытия для коллизии: {@code openTicks / openTime}. Тиковый (без
     * partialTick — getCollisionShape не получает частичные тики), шаг = 1 тик,
     * как в 1.7.10, где extras переставлялись раз в тик.
     */
    public float getCollisionProgress() {
        int openTime = getServerOpenTime();
        if (openTime <= 0) return state == 0 ? 0.0f : 1.0f;
        return Math.max(0.0f, Math.min(1.0f, openTicks / (float) openTime));
    }

    /**
     * Карта «локальная позиция схемы → коллизия клетки» на текущий тик.
     * Кэш по тику: пересборка не чаще раза в тик на дверь.
     *
     * <p>ВАЖНО: ваниль опрашивает коллизию ПОКЛЕТОЧНО — форма, возвращённая
     * контроллером, учитывается только если позиция контроллера попала в зону
     * запроса. Поэтому коллизия раздаётся каждой клетке отдельно
     * (DoorBlock — клетка ZERO, UniversalMachinePartBlock — своя), а не одним
     * объединённым шейпом на контроллер.
     *
     * @return null ТОЛЬКО если у двери нет символьной схемы (легаси-фоллбэк).
     *         Пустая карта = все клетки ретрактнулись (дверь полностью открыта) —
     *         это НЕ повод уходить в легаси!
     */
    @Nullable
    public Map<BlockPos, VoxelShape> getProgressCollisionShapes() {
        // Ключ кэша: тик + состояние (фейсинг на карту не влияет — она в локальных координатах)
        int key = openTicks | (state << 16);
        if (cachedProgressShapesValid && key == cachedProgressShapesKey) {
            return cachedProgressShapes;
        }

        DoorDecl decl = getDoorDecl();
        Map<BlockPos, VoxelShape> shapes = decl != null && decl.getStructureDefinition() != null
                ? decl.getCollisionShapesAt(getCollisionProgress())
                : null;
        cachedProgressShapesKey = key;
        cachedProgressShapes = shapes;
        cachedProgressShapesValid = true;
        return shapes;
    }

    @Nullable
    private Map<BlockPos, VoxelShape> cachedProgressShapes;
    private int cachedProgressShapesKey = Integer.MIN_VALUE;
    private boolean cachedProgressShapesValid = false;

    /**
     * ЕДИНАЯ рамка выделения двери в текущий момент: объединение форм всех частей
     * по прогрессу, повёрнутое по FACING. Используется ТОЛЬКО для выделения
     * (getShape) — коллизия раздаётся поклеточно (см. getProgressCollisionShapes).
     *
     * @return null, если у двери нет символьной схемы (fallback-структуры) —
     *         вызывающий код откатывается на старый дискретный путь.
     */
    @Nullable
    public VoxelShape getUnifiedSelectionShape() {
        Direction facing = getFacing();
        DoorDecl decl = getDoorDecl();
        boolean staticSelection = decl != null && decl.hasStaticSelectionShape();
        // Статичная рамка: ключ без тиков — считается один раз
        int key = staticSelection ? 0 : (openTicks | (state << 16)) | (facing.get3DDataValue() << 20);
        if (key == cachedUnifiedSelectionKey && cachedUnifiedSelection != null) {
            return cachedUnifiedSelection;
        }

        if (decl == null || decl.getStructureDefinition() == null) {
            return null;
        }
        Map<BlockPos, VoxelShape> localShapes = decl.getCollisionShapesAt(
                staticSelection ? 0.0f : getCollisionProgress());
        if (localShapes.isEmpty()) {
            return null;
        }

        VoxelShape combined = net.minecraft.world.phys.shapes.Shapes.empty();
        for (Map.Entry<BlockPos, VoxelShape> entry : localShapes.entrySet()) {
            BlockPos rotatedPos = MultiblockStructureHelper.rotate(entry.getKey(), facing);
            VoxelShape rotatedShape = MultiblockStructureHelper.rotateShape(entry.getValue(), facing);
            combined = net.minecraft.world.phys.shapes.Shapes.or(combined,
                    rotatedShape.move(rotatedPos.getX(), rotatedPos.getY(), rotatedPos.getZ()));
        }
        cachedUnifiedSelectionKey = key;
        cachedUnifiedSelection = combined.optimize();
        return cachedUnifiedSelection;
    }

    @Nullable
    private VoxelShape cachedUnifiedSelection;
    private int cachedUnifiedSelectionKey = Integer.MIN_VALUE;

    // private void updatePhantomBlocks(Level level, BlockPos controllerPos, int openTime) {
    //     Direction facing = getFacing();
        
    //     // ИСПРАВЛЕНО: Используем фиксированные значения для сервера
    //     // Для клиента можно получить из DoorDecl, но для сервера используем стандартные
    //     int[][] ranges = {
    //         {0, 0, 0, -5, 6, 2},  // Левая створка
    //         {0, 0, 0, 4, 6, 2}    // Правая створка
    //     };
        
    //     for (int i = 0; i < ranges.length; i++) {
    //         int[] range = ranges[i];
    //         float time = getDoorRangeOpenTime(openTicks, openTime);
            
    //         for (int j = 0; j < Math.abs(range[3]); j++) {
    //             float threshold = (float) j / Math.max(1, Math.abs(range[3] - 1));
    //             if (state == 3 && threshold > time) break;
    //             if (state == 2 && threshold < time) continue;
                
    //             for (int k = 0; k < range[4]; k++) {
    //                 BlockPos offset = calculateOffset(range, j, k, facing);
    //                 BlockPos targetPos = controllerPos.offset(offset.getX(), offset.getY(), offset.getZ());
                    
    //                 if (!targetPos.equals(controllerPos)) {
    //                     BlockState currentState = level.getBlockState(targetPos);
    //                     if (currentState.hasProperty(DoorBlock.OPEN)) {
    //                         boolean shouldOpen = (state == 3);
    //                         level.setBlock(targetPos,
    //                             currentState.setValue(DoorBlock.OPEN, shouldOpen), 3);
    //                     }
    //                 }
    //             }
    //         }
    //     }
    // }

    // private float getDoorRangeOpenTime(int currentTick, int maxTime) {
    //     if (maxTime == 0) return 0;
    //     return Math.max(0, Math.min(1, (float) currentTick / maxTime));
    // }

    // private BlockPos calculateOffset(int[] range, int j, int k, Direction facing) {
    //     BlockPos add = BlockPos.ZERO;
    //     switch (range[5]) {
    //         case 0: add = new BlockPos(0, k, (int) Math.signum(range[3]) * j); break;
    //         case 1: add = new BlockPos(k, (int) Math.signum(range[3]) * j, 0); break;
    //         case 2: add = new BlockPos((int) Math.signum(range[3]) * j, k, 0); break;
    //     }
        
    //     BlockPos startPos = new BlockPos(range[0], range[1], range[2]);
    //     return rotatePos(startPos.offset(add), facing);
    // }

    // private BlockPos rotatePos(BlockPos pos, Direction facing) {
    //     return switch (facing) {
    //         case NORTH -> pos;
    //         case SOUTH -> new BlockPos(-pos.getX(), pos.getY(), -pos.getZ());
    //         case WEST -> new BlockPos(-pos.getZ(), pos.getY(), pos.getX());
    //         case EAST -> new BlockPos(pos.getZ(), pos.getY(), -pos.getX());
    //         default -> pos;
    //     };
    // }

    // ==================== Client Sound Handling ====================
    //? if forge {
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    //?} elif fabric {
    /*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
    *///?} elif neoforge {
    /*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    *///?}
    private void handleNewState(byte oldState, byte newState) {
        if (oldState == newState) return;
        if (!isController()) return;
        
        DoorDecl decl = getDoorDecl();
        if (decl == null) return;
        
        if (oldState == 0 && newState == 3) { // Начинает открываться
            handleSoundTransition(decl.getOpenSoundStart(), decl.getOpenSoundLoop(), decl.getSoundLoop2());
            
        } else if (oldState == 1 && newState == 2) { // Начинает закрываться
            handleSoundTransition(decl.getCloseSoundStart(), decl.getCloseSoundLoop(), decl.getSoundLoop2());
            
        } else if (oldState == 3 && newState == 1) { // Полностью открылась
            handleSoundEnd(decl.getOpenSoundEnd());
            
        } else if (oldState == 2 && newState == 0) { // Полностью закрылась
            handleSoundEnd(decl.getCloseSoundEnd());
            
        } else {
            ClientSoundBootstrap.stopSound(level, worldPosition);
        }
    }
    //? if forge {
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    //?} elif fabric {
    /*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
    *///?} elif neoforge {
    /*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    *///?}
    private void handleSoundTransition(SoundEvent startSound, SoundEvent loopSound, SoundEvent loopSound2) {
        // 1. Разовый звук старта
        if (startSound != null) {
            ClientSoundBootstrap.playOneShotSound(level, worldPosition, startSound, getDoorDecl().getSoundVolume());
        }
        
        // 2. Первый цикл (основной)
        if (loopSound != null) {
            ClientSoundBootstrap.updateDoorSoundRaw(level, worldPosition, "loop1", true, () -> createLoopingSoundReflect(loopSound));
        }
        
        // 3. Второй цикл (дополнительный, например сирена)
        if (loopSound2 != null) {
            ClientSoundBootstrap.updateDoorSoundRaw(level, worldPosition, "loop2", true, () -> createLoopingSoundReflect(loopSound2));
        }
    }
    //? if forge {
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    //?} elif fabric {
    /*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
    *///?} elif neoforge {
    /*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    *///?}
    private void handleSoundEnd(SoundEvent endSound) {
        // Останавливаем ОБА цикла
        ClientSoundBootstrap.stopSpecificSound(level, worldPosition, "loop1");
        ClientSoundBootstrap.stopSpecificSound(level, worldPosition, "loop2");
        
        // Воспроизводим звук финиша
        if (endSound != null) {
            ClientSoundBootstrap.playOneShotSound(level, worldPosition, endSound, getDoorDecl().getSoundVolume());
        }
    }

    //? if forge {
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    //?} elif fabric {
    /*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
    *///?} elif neoforge {
    /*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    *///?}
    private Object createLoopingSoundReflect(SoundEvent sound) {
        try {
            return Class.forName(DOOR_LOOP_SOUND_FACTORY)
                .getMethod("create", DoorBlockEntity.class, SoundEvent.class)
                .invoke(null, this, sound);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) {
            ClientSoundBootstrap.stopSound(level, worldPosition);
        }
    }

    // ==================== NBT & Sync ====================
    
    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putByte("state", state);
        tag.putInt("openTicks", openTicks);
        tag.putLong("animStartTime", animStartTime);
        tag.putString("doorDeclId", doorDeclId);
        tag.putBoolean("locked", locked);
        // Замок (ключи как в оригинальном TileEntityLockableBase: lock/cheesable/lockMod;
        // isLocked уже хранится в "locked" — оставляем старый ключ для совместимости сейвов)
        tag.putInt("lock", lock);
        tag.putDouble("lockMod", lockMod);
        tag.putBoolean("cheesable", cheesable);
        tag.putBoolean("redstoneState", lastRedstoneState);
        modelSelection.save(tag);
        if (controllerPos != null) {
            tag.putLong("controllerPos", controllerPos.asLong());
        }
        if (!allowedClimbSides.isEmpty()) {
            int mask = 0;
            for (Direction dir : allowedClimbSides) mask |= (1 << dir.get3DDataValue());
            tag.putInt("climbSides", mask);
        }
        tag.putString("partRole", partRole.name());
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        byte oldState = this.state; // Запоминаем старое состояние
        
        this.state = tag.getByte("state");
        this.openTicks = tag.getInt("openTicks");
        this.animStartTime = tag.getLong("animStartTime");
        this.locked = tag.getBoolean("locked");
        this.lock = tag.getInt("lock");
        this.lockMod = tag.contains("lockMod") ? tag.getDouble("lockMod") : 0.1D;
        this.cheesable = !tag.contains("cheesable") || tag.getBoolean("cheesable");
        this.lastRedstoneState = tag.getBoolean("redstoneState");

        boolean hadModelSelectionInNbt = tag.contains("modelType");
        if (hadModelSelectionInNbt) {
            this.modelSelection = DoorModelSelection.load(tag);
        } else {
            // Совместимость со старыми сохранениями
            this.modelSelection = DoorModelSelection.DEFAULT;
        }
        this.cachedModelData = null;
        
        if (tag.contains("controllerPos")) {
            this.controllerPos = BlockPos.of(tag.getLong("controllerPos"));
        }

        if (tag.contains("doorDeclId")) {
            this.doorDeclId = tag.getString("doorDeclId");
        }
        
        if (tag.contains("partRole")) {
            try {
                this.partRole = PartRole.valueOf(tag.getString("partRole"));
            } catch (IllegalArgumentException e) {
                this.partRole = PartRole.DEFAULT;
            }
        }
        
        if (tag.contains("climbSides")) {
            int mask = tag.getInt("climbSides");
            allowedClimbSides.clear();
            for (Direction dir : Direction.values()) {
                if ((mask & (1 << dir.get3DDataValue())) != 0) {
                    allowedClimbSides.add(dir);
                }
            }
        }

        if (level != null && level.isClientSide) {
            initModelSelection(!hadModelSelectionInNbt);
            handleNewState(oldState, this.state);
            // Тиковая анимация: при получении движущегося состояния начинаем
            // интерполяцию с текущего openTicks (без скачка от устаревшего prev).
            if (this.state == 2 || this.state == 3) {
                this.prevOpenTicks = this.openTicks;
            }
            // Задержка: при переходе из moving (2/3) в static (0/1) - анимированная часть остаётся ещё 500ms
            if ((oldState == 2 || oldState == 3) && (this.state == 0 || this.state == 1)) {
                DoorAnimationDelayHelper.addToQueue(this, 500);
            }
        }
    }
    // Forge-only ModelData hook removed for Fabric compilation.

    /**
     * Инициализирует выбор модели на основе конфигурации.
     * Вызывается только для старых сохранений (без modelType в NBT).
     * Если hadModelSelectionInNbt=false - применить default из JSON (MODERN и т.д.).
     * Если hadModelSelectionInNbt=true - не перезаписывать: значение уже загружено из NBT
     * (в т.ч. явный выбор LEGACY, который равен DoorModelSelection.DEFAULT).
     */

    //? if forge {
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    //?} elif fabric {
    /*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
    *///?} elif neoforge {
    /*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    *///?}
    public void initModelSelection(boolean applyConfigDefault) {
        if (!applyConfigDefault) {
            return; // Значение из NBT - не перезаписывать
        }
        
        DoorModelRegistry registry = DoorModelRegistry.getInstance();
        if (registry.isInitialized()) {
            DoorModelSelection defaultSelection = registry.getDefaultSelection(doorDeclId);
            if (defaultSelection != null && !defaultSelection.equals(DoorModelSelection.DEFAULT)) {
                this.modelSelection = defaultSelection;
                this.cachedModelData = null;
                MainRegistry.LOGGER.debug("Initialized model selection for door {}: {}", 
                    doorDeclId, defaultSelection);
            }
        }
    }

    // getUpdateTag: удалён — BaseHbmBlockEntity уже делает super + writeNbtData.
    //? if forge {
    @Override
    //?}

    public void handleUpdateTag(CompoundTag tag) {
        //? if < 1.21.1 {
        load(tag);
        //?} else {
        /*// 1.21.1: BlockEntity.load(CompoundTag) удалён — loadCustomOnly с registries из level.
        PlatformHooks.loadBlockEntityTag(this, tag, this.level != null ? this.level.registryAccess() : RegistryAccess.EMPTY);
        *///?}
    }


    public int getOpenTicks() {
        return this.openTicks;
    }

    //? if forge {
    @Override
    //?}
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = PlatformHooks.getItemTag(pkt);
        if (tag != null) {
            // Сохраняем предыдущее видимое состояние ДО загрузки - чтобы определить, нужна ли инвалидация
            byte prevState = this.state;
            DoorModelSelection prevSelection = this.modelSelection;

            //? if < 1.21.1 {
            load(tag);
            //?} else {
            /*// 1.21.1: BlockEntity.load(CompoundTag) удалён — loadCustomOnly с registries из level.
            PlatformHooks.loadBlockEntityTag(this, tag, this.level != null ? this.level.registryAccess() : RegistryAccess.EMPTY);
            *///?}

            if (level != null && level.isClientSide) {
                // Инвалидируем чанк только при реальном изменении видимого состояния:
                // DOOR_MOVING/OPEN перехода или смены скина/модели.
                // Иначе каждый BE-пакет (даже с теми же данными) вызывал пересборку.
                boolean visibleChange = isVisibleStateChange(prevState, this.state)
                                     || !prevSelection.equals(this.modelSelection);
                if (visibleChange) {
                    DoorChunkInvalidationHelper.scheduleChunkInvalidation(worldPosition);
                }
            }
        }
    }

    /** Возвращает true только при переходах DOOR_MOVING или OPEN - то, что видит игрок. */
    private static boolean isVisibleStateChange(byte oldS, byte newS) {
        boolean wasMoving = oldS == 2 || oldS == 3;
        boolean isMoving  = newS == 2 || newS == 3;
        boolean wasOpen   = oldS == 1;
        boolean isOpen    = newS == 1;
        return wasMoving != isMoving || wasOpen != isOpen;
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide && level instanceof ServerLevel serverLevel) {
            // sendBlockUpdated() убрано: вызывало 3 события на клиенте за один sync -
            // ClientboundBlockUpdatePacket + broadcastBlockEntityData + явный пакет ниже.
            // Изменения BlockState (OPEN, DOOR_MOVING) отправляются через level.setBlock() в setState().
            // Изменения остальных данных (ModelData, скины) - через явный BE-пакет.
            setChanged();
            var packet = ClientboundBlockEntityDataPacket.create(this);
            for (ServerPlayer player : serverLevel.players()) {
                if (player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) < 64 * 64) {
                    player.connection.send(packet);
                }
            }
        }
    }
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        double radius = 8.0; // Fallback
        if (level != null && level.isClientSide) {
            DoorDecl decl = getDoorDecl();
            if (decl != null) {
                radius = decl.getRenderRadius();
            }
        }
        return new AABB(worldPosition).inflate(radius);
    }

    @Override
    public void setAllowedClimbSides(java.util.Set<Direction> sides) {
        // Безопасная defensive-копия: EnumSet.copyOf бросает IAE на пустой коллекции.
        java.util.EnumSet<Direction> out = java.util.EnumSet.noneOf(Direction.class);
        if (sides != null && !sides.isEmpty()) {
            out.addAll(sides);
        }
        this.allowedClimbSides = out;
        this.setChanged();
    }

    @Override
    public java.util.Set<Direction> getAllowedClimbSides() {
        return this.allowedClimbSides;
    }
}
