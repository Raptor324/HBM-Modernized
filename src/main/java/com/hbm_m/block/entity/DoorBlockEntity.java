package com.hbm_m.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.DoorBlock;
import com.hbm_m.client.ClientSoundManager;
import com.hbm_m.client.DoorDeclRegistry;
import com.hbm_m.multiblock.IMultiblockPart;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import java.util.ArrayList;
import java.util.List;

public class DoorBlockEntity extends BlockEntity implements IMultiblockPart {
    
    // 0=закрыта, 1=открыта, 2=закрывается, 3=открывается
    public byte state = 0;
    private int openTicks = 0;
    public long animStartTime = 0;
    private boolean locked = false;
    private static final float COLLISION_UPDATE_THRESHOLD = 0.05f;

    private final String doorDeclId;
    
    // Мультиблок данные
    private BlockPos controllerPos = null;
    private PartRole partRole = PartRole.DEFAULT;
    
    // Оптимизация коллизии
    private VoxelShape cachedCollisionShape = Shapes.block();
    private float lastCollisionProgress = -1f;
    
    @OnlyIn(Dist.CLIENT)
    private Object loopingSound;

    public DoorBlockEntity(BlockPos pos, BlockState state, String doorDeclId) {
        super(ModBlockEntities.DOOR_ENTITY.get(), pos, state);
        this.doorDeclId = doorDeclId;
    }

    public DoorBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, "large_vehicle_door");
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
        return controllerPos != null && controllerPos.equals(worldPosition);
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
        syncToClient();
    }

    // ==================== Публичные методы ====================

    @OnlyIn(Dist.CLIENT)
    public DoorDecl getDoorDecl() {
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

    public float getOpenProgress(float partialTick) {
        // ИСПРАВЛЕНО: Не используем doorDecl на сервере
        if (level != null && level.isClientSide) {
            DoorDecl decl = getDoorDecl();
            if (decl != null && decl.getOpenTime() == 0) {
                return state == 1 || state == 3 ? 1f : 0f;
            }
        }
        
        // Для сервера используем фиксированное время
        int openTime = 60; // Fallback значение
        if (level != null && level.isClientSide) {
            DoorDecl decl = getDoorDecl();
            if (decl != null) {
                openTime = decl.getOpenTime();
            }
        }
        
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - animStartTime;
        int totalTime = openTime * 50;
        
        return switch (state) {
            case 0 -> 0f;
            case 1 -> 1f;
            case 2 -> Math.max(0f, 1f - ((float) elapsedTime / totalTime));
            case 3 -> Math.min(1f, (float) elapsedTime / totalTime);
            default -> 0f;
        };
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
     * Проверяет, находится ли дверь в процессе движения
     */
    public boolean isMoving() {
        return state == 2 || state == 3;
    }

    private void setState(byte newState) {

        this.state = newState;
        this.animStartTime = System.currentTimeMillis();
        
        if (newState == 3) {
        this.openTicks = 0;
        } else if (newState == 2) {
            // ИСПРАВЛЕНО: Используем фиксированное значение или получаем на клиенте
            int openTime = 60; // Fallback для сервера
            if (level != null && level.isClientSide) {
                DoorDecl decl = getDoorDecl();
                if (decl != null) {
                    openTime = decl.getOpenTime();
                }
            }
            this.openTicks = openTime;
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
        int openTime = 60;
        boolean shouldSync = false;
        
        if (be.state == 3) { // Открывается
            be.openTicks++;
            be.updatePhantomBlocks(level, pos, openTime);
            
            // ДОБАВЛЕНО: Обновляем коллизию каждый тик
            be.updateCollisionShape(level, pos);
            
            if (be.openTicks >= openTime) {
                be.state = 1;
                be.openTicks = openTime;
                shouldSync = true;
                be.notifyNeighborsOfStateChange(level, pos);
            }
        } else if (be.state == 2) { // Закрывается
            be.openTicks--;
            be.updatePhantomBlocks(level, pos, openTime);
            
            // ДОБАВЛЕНО: Обновляем коллизию каждый тик
            be.updateCollisionShape(level, pos);
            
            if (be.openTicks <= 0) {
                be.state = 0;
                be.openTicks = 0;
                shouldSync = true;
                be.notifyNeighborsOfStateChange(level, pos);
            }
        }
        
        if (shouldSync) {
            be.syncToClient();
        }
    }
    
    // НОВЫЙ МЕТОД: Принудительное обновление коллизии
    private void updateCollisionShape(Level level, BlockPos pos) {
        // Сбрасываем кеш коллизии
        this.lastCollisionProgress = -1f;
        
        // Уведомляем клиент и сервер о необходимости пересчета коллизии
        BlockState currentState = level.getBlockState(pos);
        level.sendBlockUpdated(pos, currentState, currentState, Block.UPDATE_CLIENTS);
    }    

    private void notifyNeighborsOfStateChange(Level level, BlockPos controllerPos) {
        // Получаем structureHelper из блока
        BlockState blockState = getBlockState();
        if (!(blockState.getBlock() instanceof DoorBlock doorBlock)) {
            return;
        }
        
        Direction facing = blockState.getValue(DoorBlock.FACING);
        MultiblockStructureHelper structureHelper = doorBlock.getStructureHelper();
        
        // Обновляем ВСЕ блоки мультиблока
        for (BlockPos partPos : structureHelper.getAllPartPositions(controllerPos, facing)) {
            BlockState partState = level.getBlockState(partPos);
            // Принудительно обновляем блок для пересчета коллизии
            level.sendBlockUpdated(partPos, partState, partState, 3);
        }
    }

    // ==================== Dynamic Collision ====================

    public VoxelShape getDynamicCollisionShape(Direction facing) {
        float progress = getOpenProgress(0);
        
        // Обновляем только если прогресс изменился значительно
        if (Math.abs(progress - lastCollisionProgress) < COLLISION_UPDATE_THRESHOLD) {
            return cachedCollisionShape;
        }
        
        lastCollisionProgress = progress;
        
        // Получаем bounds из DoorDecl (на клиенте) или серверной логики
        List<AABB> bounds;
        if (level != null && level.isClientSide) {
            DoorDecl decl = getDoorDecl();
            bounds = decl != null ? decl.getCollisionBounds(progress, facing) : List.of();
        } else {
            bounds = getServerCollisionBounds(progress, facing);
        }
        
        if (bounds.isEmpty()) {
            cachedCollisionShape = Shapes.empty();
            return cachedCollisionShape;
        }
        
        VoxelShape shape = Shapes.empty();
        for (AABB aabb : bounds) {
            shape = Shapes.or(shape, Shapes.create(aabb));
        }
        
        cachedCollisionShape = shape.optimize();
        return cachedCollisionShape;
    }

    // Упрощенная серверная коллизия
    private List<AABB> getServerCollisionBounds(float progress, Direction facing) {
        List<AABB> bounds = new ArrayList<>();
        if (progress >= 0.99f) {
            return bounds; // Полностью открыта
        }
    
        // ИСПРАВЛЕНО: Используем ту же логику, что и в DoorDecl.LARGE_VEHICLE_DOOR
        // Левая створка (движется влево по оси X, а не Z)
        double leftMovement = progress * 3.0;
        double leftWidth = Math.max(0.0, 3.0 - leftMovement);
        if (leftWidth > 0.05) {
            AABB leftDoor = new AABB(-3.0, 0.0, 0.0, -3.0 + leftWidth, 6.0, 1.0);
            bounds.add(rotateAABBServer(leftDoor, facing));
        }
    
        // Правая створка (движется вправо по оси X, а не Z)
        double rightMovement = progress * 3.0;
        double rightOffset = rightMovement;
        if (3.0 - rightOffset > 0.05) {
            AABB rightDoor = new AABB(rightOffset, 0.0, 0.0, 3.0, 6.0, 1.0);
            bounds.add(rotateAABBServer(rightDoor, facing));
        }
    
        return bounds;
    }
    
    protected AABB rotateAABBServer(AABB aabb, Direction facing) {
        switch (facing) {
            // NORTH: базовая ориентация + разворот на 180°
            case NORTH:
                return new AABB(
                    aabb.maxX, aabb.minY, aabb.maxZ,
                    aabb.minX, aabb.maxY, aabb.minZ
                );
            
            // WEST: исходная трансформация + разворот на 180°
            case WEST:
                return new AABB(
                    -aabb.maxZ, aabb.minY, aabb.maxX,
                    -aabb.minZ, aabb.maxY, aabb.minX
                );
            
            // SOUTH: исходная трансформация + разворот на 180°
            case SOUTH:
                return new AABB(
                    aabb.maxX, aabb.minY, -aabb.maxZ,
                    aabb.minX, aabb.maxY, -aabb.minZ
                );
            
            // EAST: исходная трансформация + разворот на 180°
            case EAST:
                return new AABB(
                    aabb.maxZ, aabb.minY, -aabb.maxX,
                    aabb.minZ, aabb.maxY, -aabb.minX
                );
            
            default:
                return new AABB(
                    -aabb.maxX, aabb.minY, -aabb.maxZ,
                    -aabb.minX, aabb.maxY, -aabb.minZ
                );
        }
    }
    
    private void updatePhantomBlocks(Level level, BlockPos controllerPos, int openTime) {
        Direction facing = getFacing();
        
        // ИСПРАВЛЕНО: Используем фиксированные значения для сервера
        // Для клиента можно получить из DoorDecl, но для сервера используем стандартные
        int[][] ranges = {
            {0, 0, 0, -5, 6, 2},  // Левая створка
            {0, 0, 0, 4, 6, 2}    // Правая створка
        };
        
        for (int i = 0; i < ranges.length; i++) {
            int[] range = ranges[i];
            float time = getDoorRangeOpenTime(openTicks, openTime);
            
            for (int j = 0; j < Math.abs(range[3]); j++) {
                float threshold = (float) j / Math.max(1, Math.abs(range[3] - 1));
                if (state == 3 && threshold > time) break;
                if (state == 2 && threshold < time) continue;
                
                for (int k = 0; k < range[4]; k++) {
                    BlockPos offset = calculateOffset(range, j, k, facing);
                    BlockPos targetPos = controllerPos.offset(offset.getX(), offset.getY(), offset.getZ());
                    
                    if (!targetPos.equals(controllerPos)) {
                        BlockState currentState = level.getBlockState(targetPos);
                        if (currentState.hasProperty(DoorBlock.OPEN)) {
                            boolean shouldOpen = (state == 3);
                            level.setBlock(targetPos,
                                currentState.setValue(DoorBlock.OPEN, shouldOpen), 3);
                        }
                    }
                }
            }
        }
    }

    private float getDoorRangeOpenTime(int currentTick, int maxTime) {
        if (maxTime == 0) return 0;
        return Math.max(0, Math.min(1, (float) currentTick / maxTime));
    }

    private BlockPos calculateOffset(int[] range, int j, int k, Direction facing) {
        BlockPos add = BlockPos.ZERO;
        switch (range[5]) {
            case 0: add = new BlockPos(0, k, (int) Math.signum(range[3]) * j); break;
            case 1: add = new BlockPos(k, (int) Math.signum(range[3]) * j, 0); break;
            case 2: add = new BlockPos((int) Math.signum(range[3]) * j, k, 0); break;
        }
        
        BlockPos startPos = new BlockPos(range[0], range[1], range[2]);
        return rotatePos(startPos.offset(add), facing);
    }

    private BlockPos rotatePos(BlockPos pos, Direction facing) {
        return switch (facing) {
            case NORTH -> pos;
            case SOUTH -> new BlockPos(-pos.getX(), pos.getY(), -pos.getZ());
            case WEST -> new BlockPos(-pos.getZ(), pos.getY(), pos.getX());
            case EAST -> new BlockPos(pos.getZ(), pos.getY(), -pos.getX());
            default -> pos;
        };
    }

    // ==================== Client Sound Handling ====================
    @OnlyIn(Dist.CLIENT)
    private void handleNewState(byte oldState, byte newState) {
        if (oldState == newState) return;
        
        // КРИТИЧЕСКИ ВАЖНО: звук воспроизводится ТОЛЬКО на контроллере (главном блоке)!
        // Это предотвращает дублирование звука в многоблочных структурах
        if (!isController()) {
            return; // Не воспроизводим звук на дочерних блоках
        }
        
        DoorDecl decl = getDoorDecl();
        
        if (oldState == 0 && newState == 3) {
            handleSoundTransition(decl.getOpenSoundStart(), decl.getOpenSoundLoop());
            
        } else if (oldState == 1 && newState == 2) {
            handleSoundTransition(decl.getCloseSoundStart(), decl.getCloseSoundLoop());
            
        } else if (oldState == 3 && newState == 1) {
            handleSoundEnd(decl.getOpenSoundEnd());
            
        } else if (oldState == 2 && newState == 0) {
            handleSoundEnd(decl.getCloseSoundEnd());
            
        } else {
            ClientSoundManager.stopSound(worldPosition);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void handleSoundTransition(SoundEvent startSound, SoundEvent loopSound) {
        // 1. Воспроизводим разовый звук начала
        if (startSound != null) {
            ClientSoundManager.playOneShotSound(worldPosition, startSound, getDoorDecl().getSoundVolume());
        }
        
        // 2. Запускаем зацикленный звук движения
        if (loopSound != null) {
            ClientSoundManager.updateDoorSound(
                worldPosition,
                true, // дверь движется
                () -> createLoopingSound(loopSound)
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void handleSoundEnd(SoundEvent endSound) {
        // 1. Останавливаем зацикленный звук движения
        ClientSoundManager.stopSound(worldPosition);
        
        // 2. Воспроизводим разовый звук конца
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
        tag.putBoolean("locked", locked);
        if (controllerPos != null) {
            tag.putLong("controllerPos", controllerPos.asLong());
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
        
        if (tag.contains("controllerPos")) {
            this.controllerPos = BlockPos.of(tag.getLong("controllerPos"));
        }
        
        if (tag.contains("partRole")) {
            try {
                this.partRole = PartRole.valueOf(tag.getString("partRole"));
            } catch (IllegalArgumentException e) {
                this.partRole = PartRole.DEFAULT;
            }
        }
        
        // ИСПРАВЛЕНО: Вызываем handleNewState ТОЛЬКО на клиенте при РЕАЛЬНОМ изменении
        if (level != null && level.isClientSide && oldState != this.state) {
            handleNewState(oldState, this.state);
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
        load(tag); // load() уже отследит изменение state и вызовет handleNewState
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
            load(tag); // load() уже отследит изменение state и вызовет handleNewState
        }
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            setChanged();
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        // ИСПРАВЛЕНО: Используем фиксированный радиус или получаем на клиенте
        double radius = 8.0; // Fallback
        if (level != null && level.isClientSide) {
            DoorDecl decl = getDoorDecl();
            if (decl != null) {
                radius = decl.getRenderRadius();
            }
        }
        return new AABB(worldPosition).inflate(radius);
    }
}
