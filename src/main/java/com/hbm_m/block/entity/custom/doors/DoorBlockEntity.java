package com.hbm_m.block.entity.custom.doors;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.custom.decorations.DoorBlock;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.client.ClientSoundManager;
import com.hbm_m.client.model.variant.DoorModelRegistry;
import com.hbm_m.client.model.variant.DoorModelSelection;
import com.hbm_m.client.model.variant.DoorModelType;
import com.hbm_m.client.model.variant.DoorSkin;
import com.hbm_m.client.render.DoorChunkInvalidationHelper;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.multiblock.IMultiblockPart;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

public class DoorBlockEntity extends BlockEntity implements IMultiblockPart {
    
    // 0=закрыта, 1=открыта, 2=закрывается, 3=открывается
    public byte state = 0;
    private int openTicks = 0;
    public long animStartTime = 0;
    private boolean locked = false;
    private boolean lastRedstoneState = false;

    /**
     * Property для передачи выбора модели через ModelData
     */
    @OnlyIn(Dist.CLIENT)
    public static final ModelProperty<DoorModelSelection> MODEL_SELECTION_PROPERTY = 
        new ModelProperty<>();
    
    /**
     * Property для передачи состояния движения двери через ModelData
     * Используется для переключения между baked geometry и BER рендером при активном шейдере
     */
    @OnlyIn(Dist.CLIENT)
    public static final ModelProperty<Boolean> DOOR_MOVING_PROPERTY = 
        new ModelProperty<>();

    /**
     * Property для передачи состояния open/closed через ModelData.
     * Используется в baked-модели когда BlockState может быть не синхронизирован.
     */
    @OnlyIn(Dist.CLIENT)
    public static final ModelProperty<Boolean> OPEN_PROPERTY = 
        new ModelProperty<>();

    /**
     * Property: true когда дверь в положении open/closed (state 0/1), но BER ещё рисует створки
     * (период overlap). BakedModel показывает полную геометрию, BER — анимированные части.
     * Наслоение устраняет моргание при переключении.
     */
    @OnlyIn(Dist.CLIENT)
    public static final ModelProperty<Boolean> OVERLAP_PROPERTY = 
        new ModelProperty<>();
    
    /**
     * Текущий выбор модели и скина
     */
    private DoorModelSelection modelSelection = DoorModelSelection.DEFAULT;
    
    /**
     * Кэшированные ModelData для производительности
     */
    @OnlyIn(Dist.CLIENT)
    private ModelData cachedModelData;

    private String doorDeclId;
    
    // Мультиблок данные
    private BlockPos controllerPos = null;
    private PartRole partRole = PartRole.DEFAULT;

    private java.util.Set<Direction> allowedClimbSides = java.util.EnumSet.noneOf(Direction.class);
    
    @OnlyIn(Dist.CLIENT)
    private Object loopingSound;

    /**
     * Задержка (мс): baked model и BER наслаиваются — оба рисуют створки в open/closed.
     * Устраняет моргание при переключении. После истечения — только baked model.
     */
    private static final long ANIMATION_DELAY_MS = 500;

    @OnlyIn(Dist.CLIENT)
    private long animationDelayUntilMs = 0;

    @OnlyIn(Dist.CLIENT)
    private static final List<DelayEntry> ANIMATION_DELAY_QUEUE = new ArrayList<>();

    @OnlyIn(Dist.CLIENT)
    private static record DelayEntry(long untilMs, WeakReference<DoorBlockEntity> ref) {}

    /** Вызывать из ClientTickEvent. По истечении delay — отключаем BER, оставляем только baked model. */
    @OnlyIn(Dist.CLIENT)
    public static void processAnimationDelayQueue() {
        long now = System.currentTimeMillis();
        Iterator<DelayEntry> it = ANIMATION_DELAY_QUEUE.iterator();
        while (it.hasNext()) {
            DelayEntry entry = it.next();
            if (now >= entry.untilMs) {
                DoorBlockEntity be = entry.ref.get();
                if (be != null && !be.isRemoved() && be.level != null) {
                    be.animationDelayUntilMs = 0;
                    be.cachedModelData = null;
                    be.requestModelDataUpdate();
                    DoorChunkInvalidationHelper.scheduleChunkInvalidation(be.worldPosition);
                }
                it.remove();
            }
        }
    }

    public DoorBlockEntity(BlockPos pos, BlockState state, String doorDeclId) {
        super(ModBlockEntities.DOOR_ENTITY.get(), pos, state);
        this.doorDeclId = doorDeclId;
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
                requestModelDataUpdate();
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
            initModelSelection();
        }
        syncToClient();
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
    
        if (powered) {
            // Если дверь закрыта или в процессе закрытия — открываем
            if (state == 0 || state == 2) {
                open();
            }
        } else {
            // Сигнал пропал
            if (state == 1) { // Полностью открыта
                close();
            } else if (state == 3) { // В процессе открытия
                int openTime = getDoorDecl().getOpenTime();
                if (openTime <= 25) { // Только для быстрых дверей
                    close();
                }
            }
        }
        setChanged();
    }

    /**
     * Получает время открытия двери в тиках (серверная версия без клиентских зависимостей).
     */
    // private int getServerOpenTime() {
    //     return switch (doorDeclId) {
    //         case "qe_sliding_door" -> 10;
    //         case "sliding_seal_door" -> 20;
    //         case "sliding_blast_door" -> 24;
    //         case "secure_access_door" -> 120;
    //         case "qe_containment_door" -> 160;
    //         case "water_door" -> 60;
    //         case "large_vehicle_door" -> 60;
    //         case "fire_door" -> 160;
    //         case "silo_hatch" -> 60;
    //         case "silo_hatch_large" -> 60;
    //         default -> 60;
    //     };
    // }    

    private int getServerOpenTime() {
        DoorDecl decl = getDoorDecl();
        return decl != null ? decl.getOpenTime() : 60;
    }

    // ОБНОВИТЕ существующий метод getOpenProgress(float):
    public float getOpenProgress(float partialTick) {
        int openTime = getServerOpenTime();
        if (openTime <= 0) return state == 1 || state == 3 ? 1f : 0f;
        
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - animStartTime;
        int totalTimeMs = openTime * 50; 

        return switch (state) {
            case 0 -> 0f;
            case 1 -> 1f;
            case 2 -> Math.max(0f, 1f - ((float) elapsedTime / totalTimeMs));
            case 3 -> Math.min(1f, (float) elapsedTime / totalTimeMs);
            default -> 0f;
        };
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

        if (state == 2 || state == 3) {
            return; // Дверь в процессе движения - игнорируем клик
        }
        
        // Переключаем только если дверь полностью открыта или закрыта
        if (state == 0) {
            open();
        } else if (state == 1) {
            close();
        }
    }

    /**
     * Проверяет, находится ли дверь в процессе движения.
     * На клиенте: включает задержку + grace period после полного открытия/закрытия —
     * анимированная часть остаётся видимой, пока baked model не пересоберётся.
     */
    public boolean isMoving() {
        if (state == 2 || state == 3) return true;
        if (level != null && level.isClientSide) {
            if (animationDelayUntilMs > 0 && System.currentTimeMillis() < animationDelayUntilMs) return true;
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
            requestModelDataUpdate();
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

    // ==================== Server Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, DoorBlockEntity be) {
        int openTime = be.getServerOpenTime();
        boolean shouldSync = false;
    
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
    
        if (shouldSync) {
            be.syncToClient();
        }
    }

    private void notifyNeighborsOfStateChange(Level level, BlockPos controllerPos) {
        BlockState blockState = getBlockState();
        if (!(blockState.getBlock() instanceof DoorBlock doorBlock)) return;
        
        Direction facing = blockState.getValue(DoorBlock.FACING);
        MultiblockStructureHelper structureHelper = doorBlock.getStructureHelper();
        
        // Контроллер не входит в getAllPartPositions (BlockPos.ZERO исключён из structureMap)
        BlockState controllerState = level.getBlockState(controllerPos);
        level.sendBlockUpdated(controllerPos, controllerState, controllerState, 3);
        
        for (BlockPos partPos : structureHelper.getAllPartPositions(controllerPos, facing)) {
            BlockState partState = level.getBlockState(partPos);
            // Флаг 3 (UPDATE_ALL) заставляет клиент перерисовать блок и обновить коллизию
            level.sendBlockUpdated(partPos, partState, partState, 3);
            level.getLightEngine().checkBlock(partPos);
            level.updateNeighborsAt(partPos, blockState.getBlock());
        }
        level.getLightEngine().checkBlock(controllerPos);
    }

    public DoorDecl getServerDoorDecl() {
        return DoorDeclRegistry.getById(this.doorDeclId);
    }

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
    @OnlyIn(Dist.CLIENT)
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
            ClientSoundManager.stopSound(worldPosition);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void handleSoundTransition(SoundEvent startSound, SoundEvent loopSound, SoundEvent loopSound2) {
        // 1. Разовый звук старта
        if (startSound != null) {
            ClientSoundManager.playOneShotSound(worldPosition, startSound, getDoorDecl().getSoundVolume());
        }
        
        // 2. Первый цикл (основной)
        if (loopSound != null) {
            ClientSoundManager.updateDoorSound(worldPosition, "loop1", true, () -> createLoopingSound(loopSound));
        }
        
        // 3. Второй цикл (дополнительный, например сирена)
        if (loopSound2 != null) {
            ClientSoundManager.updateDoorSound(worldPosition, "loop2", true, () -> createLoopingSound(loopSound2));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void handleSoundEnd(SoundEvent endSound) {
        // Останавливаем ОБА цикла
        ClientSoundManager.stopSpecificSound(worldPosition, "loop1");
        ClientSoundManager.stopSpecificSound(worldPosition, "loop2");
        
        // Воспроизводим звук финиша
        if (endSound != null) {
            ClientSoundManager.playOneShotSound(worldPosition, endSound, getDoorDecl().getSoundVolume());
        }
    }

    @OnlyIn(Dist.CLIENT)
    private net.minecraft.client.resources.sounds.AbstractTickableSoundInstance createLoopingSound(SoundEvent sound) {
        return new net.minecraft.client.resources.sounds.AbstractTickableSoundInstance(sound, SoundSource.BLOCKS, RandomSource.create()) {
            {
                this.x = DoorBlockEntity.this.worldPosition.getX() + 0.5;
                this.y = DoorBlockEntity.this.worldPosition.getY() + 0.5;
                this.z = DoorBlockEntity.this.worldPosition.getZ() + 0.5;
                this.volume = getDoorDecl().getSoundVolume();
                this.pitch = 1.0f;
                this.looping = true;
            }
            
            @Override
            public void tick() {
                Level level = net.minecraft.client.Minecraft.getInstance().level; //  Полное имя
                if (level == null) {
                    this.stop();
                    return;
                }
                
                BlockEntity be = level.getBlockEntity(DoorBlockEntity.this.worldPosition);
                if (!(be instanceof DoorBlockEntity doorBE) ||
                    (doorBE.state != 2 && doorBE.state != 3)) {
                    this.stop();
                }
            }
        };
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) {
            ClientSoundManager.stopSound(worldPosition);
        }
    }

    // ==================== NBT & Sync ====================
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putByte("state", state);
        tag.putInt("openTicks", openTicks);
        tag.putLong("animStartTime", animStartTime);
        tag.putString("doorDeclId", doorDeclId);
        tag.putBoolean("locked", locked);
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
    public void load(CompoundTag tag) {
        super.load(tag);
        byte oldState = this.state; // Запоминаем старое состояние
        
        this.state = tag.getByte("state");
        this.openTicks = tag.getInt("openTicks");
        this.animStartTime = tag.getLong("animStartTime");
        this.locked = tag.getBoolean("locked");
        this.lastRedstoneState = tag.getBoolean("redstoneState");

        if (tag.contains("modelType")) {
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
            initModelSelection();
            handleNewState(oldState, this.state);
            // ИСПРАВЛЕНИЕ МОРГАНИЯ: при получении state 2/3 (движение) используем клиентское время.
            // Серверный animStartTime приводит к рассинхрону часов и скачкам прогресса анимации.
            if (this.state == 2 || this.state == 3) {
                this.animStartTime = System.currentTimeMillis();
            }
            // Задержка: при переходе из moving (2/3) в static (0/1) — анимированная часть остаётся ещё ANIMATION_DELAY_MS
            if ((oldState == 2 || oldState == 3) && (this.state == 0 || this.state == 1)) {
                animationDelayUntilMs = System.currentTimeMillis() + ANIMATION_DELAY_MS;
                ANIMATION_DELAY_QUEUE.add(new DelayEntry(animationDelayUntilMs, new WeakReference<>(this)));
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public ModelData getModelData() {
        if (cachedModelData == null) {
            // isMoving: state 2/3 или период overlap (BER продолжает рисовать створки)
            boolean inDelay = animationDelayUntilMs > 0 && System.currentTimeMillis() < animationDelayUntilMs;
            boolean isOverlap = (state == 0 || state == 1) && inDelay;
            boolean isMoving = state == 2 || state == 3 || isOverlap;
            boolean isOpen = state == 1;
            cachedModelData = ModelData.builder()
                .with(MODEL_SELECTION_PROPERTY, modelSelection)
                .with(DOOR_MOVING_PROPERTY, isMoving)
                .with(OPEN_PROPERTY, isOpen)
                .with(OVERLAP_PROPERTY, isOverlap)
                .build();
        }
        return cachedModelData;
    }

    /**
     * Инициализирует выбор модели на основе конфигурации.
     * Вызывается автоматически при загрузке на клиенте.
     */
    @OnlyIn(Dist.CLIENT)
    public void initModelSelection() {
        // Если уже установлен не-default выбор - оставляем как есть
        if (modelSelection != null && !modelSelection.equals(DoorModelSelection.DEFAULT)) {
            return;
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

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            load(tag);
            // Инвалидируем чанк — baked model должен пересобраться с новым ModelData (OPEN, DOOR_MOVING)
            if (level != null && level.isClientSide) {
                requestModelDataUpdate();
                DoorChunkInvalidationHelper.scheduleChunkInvalidation(worldPosition);
            }
        }
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide && level instanceof ServerLevel serverLevel) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            setChanged();
            // Явно отправляем BlockEntity пакет — клиент должен получить state/OPEN/DOOR_MOVING
            // для baked model (ModelData) и BER. Без этого клиентский BlockEntity остаётся со старым state.
            var packet = ClientboundBlockEntityDataPacket.create(this);
            for (ServerPlayer player : serverLevel.players()) {
                if (player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) < 64 * 64) {
                    player.connection.send(packet);
                }
            }
        }
    }

    @Override
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
        this.allowedClimbSides = java.util.EnumSet.copyOf(sides);
        this.setChanged();
    }

    @Override
    public java.util.Set<Direction> getAllowedClimbSides() {
        return this.allowedClimbSides;
    }
}
