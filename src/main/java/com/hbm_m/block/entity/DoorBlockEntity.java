package com.hbm_m.block.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import com.hbm_m.block.DoorBlock;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.client.ClientSoundManager;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.multiblock.IMultiblockPart;
import com.hbm_m.multiblock.PartRole;
import com.hbm_m.util.DoorDecl;
import com.hbm_m.util.LegacyAnimator;

import java.util.List;

public class DoorBlockEntity extends BlockEntity implements IMultiblockPart {
    
    // 0=закрыта, 1=открыта, 2=закрывается, 3=открывается
    public byte state = 0;
    private int openTicks = 0;
    public long animStartTime = 0;
    private boolean locked = false;
    private final DoorDecl doorDecl;
    
    // Мультиблок данные
    private BlockPos controllerPos = null;
    private PartRole partRole = PartRole.DEFAULT;
    
    // Оптимизация коллизии
    private VoxelShape cachedCollisionShape = Shapes.block();
    private float lastCollisionProgress = -1f;
    
    @OnlyIn(Dist.CLIENT)
    private SoundInstance loopingSound;

    public DoorBlockEntity(BlockPos pos, BlockState state, DoorDecl doorDecl) {
        super(ModBlockEntities.DOOR_ENTITY.get(), pos, state);
        this.doorDecl = doorDecl;
    }

    public DoorBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, DoorDecl.LARGE_VEHICLE_DOOR);
    }

    // ==================== IMultiblockPart ====================

    @Override
    public void setControllerPos(BlockPos controllerPos) {
        this.controllerPos = controllerPos;
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

    public DoorDecl getDoorDecl() {
        return doorDecl;
    }

    public Direction getFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(DoorBlock.FACING)
            ? state.getValue(DoorBlock.FACING)
            : Direction.NORTH;
    }

    public float getOpenProgress(float partialTick) {
        if (doorDecl.getOpenTime() == 0) return state == 1 || state == 3 ? 1f : 0f;
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - animStartTime;
        int totalTime = doorDecl.getOpenTime() * 50;

        return switch (state) {
            case 0 -> 0f;
            case 1 -> 1f;
            case 2 -> Math.max(0f, 1f - ((float) elapsedTime / totalTime));
            case 3 -> Math.min(1f, (float) elapsedTime / totalTime);
            default -> 0f;
        };
    }

    public byte getState() {
        return state;
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
        
        // ИСПРАВЛЕНО: правильная инициализация openTicks
        if (newState == 3) {
            // Начинаем открытие - начинаем с 0
            this.openTicks = 0;
        } else if (newState == 2) {
            // Начинаем закрытие - начинаем с максимума
            this.openTicks = doorDecl.getOpenTime();
        }
        // Для state 0 и 1 не меняем openTicks
        
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
        if (be.state == 3) {
            // ОТКРЫВАЕТСЯ
            be.openTicks++;
            be.updatePhantomBlocks(level, pos);
            if (be.openTicks >= be.doorDecl.getOpenTime()) {
                be.state = 1;
                be.openTicks = be.doorDecl.getOpenTime(); // Сохраняем максимальное значение
                be.syncToClient();
            }
            
        } else if (be.state == 2) {
            // ЗАКРЫВАЕТСЯ
            be.openTicks--;
            be.updatePhantomBlocks(level, pos);
            if (be.openTicks <= 0) {
                be.state = 0;
                be.openTicks = 0;
                be.syncToClient();
            }
        }
    }

    // ==================== Dynamic Collision ====================

    public VoxelShape getDynamicCollisionShape(Direction facing) {
        float progress = getOpenProgress(0);
        if (Math.abs(progress - lastCollisionProgress) < 0.01f) {
            return cachedCollisionShape;
        }

        lastCollisionProgress = progress;
        List<AABB> bounds = doorDecl.getCollisionBounds(progress, facing);
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

    private void updatePhantomBlocks(Level level, BlockPos controllerPos) {
        Direction facing = getFacing();
        int[][] ranges = doorDecl.getDoorOpenRanges();
        
        for (int i = 0; i < ranges.length; i++) {
            int[] range = ranges[i];
            float time = doorDecl.getDoorRangeOpenTime(openTicks, i);
            
            for (int j = 0; j < Math.abs(range[3]); j++) {
                float threshold = (float) j / Math.max(1, Math.abs(range[3] - 1));
                
                if (state == 3 && threshold > time) break;
                if (state == 2 && threshold < time) continue;
                
                for (int k = 0; k < range[4]; k++) {
                    BlockPos offset = calculateOffset(range, j, k, facing);
                    BlockPos targetPos = controllerPos.offset(offset.getX(), offset.getY(), offset.getZ());
                    
                    if (!targetPos.equals(controllerPos)) {
                        BlockState currentState = level.getBlockState(targetPos);
                        
                        // ИСПРАВЛЕНО: Проверяем, что блок имеет OPEN property
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
    private AbstractTickableSoundInstance createLoopingSound(SoundEvent sound) {
        return new AbstractTickableSoundInstance(sound, SoundSource.BLOCKS, RandomSource.create()) {
            {
                this.x = DoorBlockEntity.this.worldPosition.getX() + 0.5;
                this.y = DoorBlockEntity.this.worldPosition.getY() + 0.5;
                this.z = DoorBlockEntity.this.worldPosition.getZ() + 0.5;
                this.volume = getDoorDecl().getSoundVolume();
                this.pitch = 1.0f;
                this.looping = true; // ВАЖНО: зацикливаем звук
            }
            
            @Override
            public void tick() {
                Level level = Minecraft.getInstance().level;
                if (level == null) {
                    this.stop();
                    return;
                }
                
                BlockEntity be = level.getBlockEntity(DoorBlockEntity.this.worldPosition);
                
                // Останавливаем звук если:
                // - блок удален
                // - дверь больше не в состоянии движения (2 или 3)
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
        return new AABB(worldPosition).inflate(doorDecl.getRenderRadius());
    }
}
