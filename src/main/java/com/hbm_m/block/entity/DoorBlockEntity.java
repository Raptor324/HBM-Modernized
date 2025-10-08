package com.hbm_m.block.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
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
import com.hbm_m.multiblock.IMultiblockPart;
import com.hbm_m.multiblock.PartRole;
import com.hbm_m.util.DoorDecl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DoorBlockEntity extends BlockEntity implements IMultiblockPart {
    
    private byte state = 0; // 0=закрыта, 1=открыта, 2=закрывается, 3=открывается
    private int animationTicks = 0;
    private boolean locked = false;
    private final DoorDecl doorDecl;
    
    // Мультиблок данные
    private BlockPos controllerPos = null;
    private PartRole partRole = PartRole.DEFAULT;
    private final Set<BlockPos> phantomBlocks = new HashSet<>();
    
    // Оптимизация коллизии
    private VoxelShape cachedCollisionShape = Shapes.block();
    private float lastCollisionProgress = -1f;
    
    public DoorBlockEntity(BlockPos pos, BlockState state, DoorDecl doorDecl) {
        super(ModBlockEntities.DOOR_ENTITY.get(), pos, state);
        this.doorDecl = doorDecl;
    }
    
    public DoorBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, DoorDecl.LARGE_VEHICLE_DOOR);
    }

    @OnlyIn(Dist.CLIENT)
    private SoundInstance loopingSound;

    @OnlyIn(Dist.CLIENT) 
    private SoundInstance loopingSound2;

    private void playLoopingSound(SoundEvent sound) {
        if (level != null && level.isClientSide && sound != null) {
            stopLoopingSound();
            // Создать AbstractTickableSoundInstance
            loopingSound = new AbstractTickableSoundInstance(sound, SoundSource.BLOCKS, 
                SoundInstance.createUnseededRandom()) {
                {
                    this.looping = true;
                    this.x = worldPosition.getX();
                    this.y = worldPosition.getY();
                    this.z = worldPosition.getZ();
                    this.volume = doorDecl.getSoundVolume();
                }
                
                @Override
                public void tick() {
                    if (level.getBlockEntity(worldPosition) != DoorBlockEntity.this) {
                        this.stop();
                    }
                }
            };
            Minecraft.getInstance().getSoundManager().play(loopingSound);
        }
    }

    private void stopLoopingSound() {
        if (loopingSound != null) {
            Minecraft.getInstance().getSoundManager().stop(loopingSound);
            loopingSound = null;
        }
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
        
        // Если controllerPos не установлен, считаем себя контроллером
        if (controllerPos == null) return this;
        
        BlockEntity be = level.getBlockEntity(controllerPos);
        return be instanceof DoorBlockEntity ? (DoorBlockEntity) be : null;
    }
    
    // ==================== Публичные методы для BER ====================
    
    public DoorDecl getDoorDecl() {
        return doorDecl;
    }
    
    public Direction getFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(DoorBlock.FACING) 
            ? state.getValue(DoorBlock.FACING) 
            : Direction.NORTH;
    }
    
    /**
     * Прогресс анимации с partialTick для плавного рендера
     */
    public float getOpenProgress(float partialTick) {
        int totalTime = doorDecl.getOpenTime();
        if (totalTime == 0) return getAnimationProgress();
        
        return switch (state) {
            case 0 -> 0f; // Закрыта
            case 1 -> 1f; // Открыта
            case 2 -> Math.max(0, 1f - ((animationTicks + partialTick) / totalTime)); // Закрывается
            case 3 -> Math.min(1f, (animationTicks + partialTick) / totalTime); // Открывается
            default -> 0f;
        };
    }
    
    // ==================== State Management ====================
    
    public void open() {
        if (state == 0) {
            setState((byte) 3);
            playSound(doorDecl.getOpenSoundStart());
        } else if (state == 2) {
            setState((byte) 3);
            animationTicks = doorDecl.getOpenTime() - animationTicks;
        }
    }
    
    public void close() {
        if (state == 1) {
            setState((byte) 2);
            playSound(doorDecl.getCloseSoundStart());
        } else if (state == 3) {
            setState((byte) 2);
            animationTicks = doorDecl.getOpenTime() - animationTicks;
        }
    }
    
    public void toggle() {
        if (state == 0 || state == 2) open();
        else if (state == 1 || state == 3) close();
    }
    
    private void setState(byte newState) {
        this.state = newState;
        this.animationTicks = 0;
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
        if (be.state == 3) { // Открывается
            be.animationTicks++;
            be.updatePhantomBlocks(level, pos, state); // Добавить
            
            if (be.animationTicks >= be.doorDecl.getOpenTime()) {
                be.state = 1;
                be.playSound(be.doorDecl.getOpenSoundEnd());
                be.syncToClient();
            }
        } else if (be.state == 2) { // Закрывается
            be.animationTicks++;
            be.updatePhantomBlocks(level, pos, state); // Добавить
            
            if (be.animationTicks >= be.doorDecl.getOpenTime()) {
                be.state = 0;
                be.animationTicks = 0;
                be.playSound(be.doorDecl.getCloseSoundEnd());
                be.syncToClient();
            }
        }
    }
    
    // ==================== Dynamic Collision ====================
    
    public VoxelShape getDynamicCollisionShape(Direction facing) {
        float progress = getAnimationProgress();
        
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

    private void updatePhantomBlocks(Level level, BlockPos controllerPos, BlockState controllerState) {
        Direction facing = getFacing();
        int[][] ranges = doorDecl.getDoorOpenRanges();
        
        for (int i = 0; i < ranges.length; i++) {
            int[] range = ranges[i];
            float time = doorDecl.getDoorRangeOpenTime(animationTicks, i);
            
            for (int j = 0; j < Math.abs(range[3]); j++) {
                if ((float) j / Math.abs(range[3] - 1) > time) break;
                
                for (int k = 0; k < range[4]; k++) {
                    BlockPos offset = calculateOffset(range, j, k, facing);
                    BlockPos targetPos = controllerPos.offset(offset.getX(), offset.getY(), offset.getZ());
                    
                    if (!targetPos.equals(controllerPos)) {
                        if (state == 3) { // Открывается - убираем блоки
                            phantomBlocks.add(targetPos);
                            level.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 3);
                        } else if (state == 2) { // Закрывается - возвращаем блоки
                            if (phantomBlocks.remove(targetPos)) {
                                level.setBlock(targetPos, ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(), 3);
                            }
                        }
                    }
                }
            }
        }
    }

    private BlockPos calculateOffset(int[] range, int j, int k, Direction facing) {
        // Логика из TileEntityDoorGeneric.updateEntity() с rotation
        BlockPos add = BlockPos.ZERO;
        switch (range[5]) {
            case 0: add = new BlockPos(0, k, (int)Math.signum(range[3]) * j); break;
            case 1: add = new BlockPos(k, (int)Math.signum(range[3]) * j, 0); break;
            case 2: add = new BlockPos((int)Math.signum(range[3]) * j, k, 0); break;
        }
        
        BlockPos startPos = new BlockPos(range[0], range[1], range[2]);
        return rotatePos(startPos.offset(add), facing);
    }

    private BlockPos rotatePos(BlockPos pos, Direction facing) {
        // Реализовать ротацию относительно Direction
        return switch (facing) {
            case NORTH -> pos;
            case SOUTH -> new BlockPos(-pos.getX(), pos.getY(), -pos.getZ());
            case WEST -> new BlockPos(-pos.getZ(), pos.getY(), pos.getX());
            case EAST -> new BlockPos(pos.getZ(), pos.getY(), -pos.getX());
            default -> pos;
        };
    }
    
    public float getAnimationProgress() {
        int totalTime = doorDecl.getOpenTime();
        if (totalTime == 0) return 0f;
        
        return switch (state) {
            case 0 -> 0f;
            case 1 -> 1f;
            case 2 -> 1f - ((float) animationTicks / totalTime);
            case 3 -> (float) animationTicks / totalTime;
            default -> 0f;
        };
    }
    
    private void invalidateCollisionCache() {
        lastCollisionProgress = -1f;
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }
    
    // ==================== Sound ====================
    
    private void playSound(@Nullable SoundEvent sound) {
        if (sound != null && level != null) {
            level.playSound(null, worldPosition, sound, SoundSource.BLOCKS,
                doorDecl.getSoundVolume(), 1.0f);
        }
    }
    
    // ==================== NBT & Sync ====================
    
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putByte("DoorState", state);
        tag.putInt("AnimTicks", animationTicks);
        tag.putBoolean("Locked", locked);
        
        // Мультиблок данные
        if (controllerPos != null) {
            tag.putLong("ControllerPos", controllerPos.asLong());
        }
        tag.putString("PartRole", partRole.name());
    }
    
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.state = tag.getByte("DoorState");
        this.animationTicks = tag.getInt("AnimTicks");
        this.locked = tag.getBoolean("Locked");
        
        // Мультиблок данные
        if (tag.contains("ControllerPos")) {
            this.controllerPos = BlockPos.of(tag.getLong("ControllerPos"));
        }
        if (tag.contains("PartRole")) {
            try {
                this.partRole = PartRole.valueOf(tag.getString("PartRole"));
            } catch (IllegalArgumentException e) {
                this.partRole = PartRole.DEFAULT;
            }
        }
        
        invalidateCollisionCache();
    }
    
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }
    
    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    
    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        if (pkt.getTag() != null) load(pkt.getTag());
    }
    
    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            setChanged();
        }
    }
    
    public void onStructureFormed() {
        // Сразу создаём физическую структуру при установке
        if (level != null && !level.isClientSide && isController()) {
            Direction facing = getFacing();
            BlockPos basePos = worldPosition;
            
            // Создаём блоки 7x6 структуры
            for (int x = -3; x <= 3; x++) {
                for (int y = 0; y <= 5; y++) {
                    BlockPos offset = rotateOffset(new BlockPos(x, y, 0), facing);
                    BlockPos targetPos = basePos.offset(offset);
                    
                    if (!targetPos.equals(basePos)) {
                        BlockState partState = ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState();
                        level.setBlock(targetPos, partState, 3);
                        
                        // Устанавливаем BlockEntity для части
                        BlockEntity partBE = level.getBlockEntity(targetPos);
                        if (partBE instanceof DoorBlockEntity doorPart) {
                            doorPart.setControllerPos(basePos);
                            doorPart.setPartRole(PartRole.DEFAULT);
                        }
                    }
                }
            }
        }
        syncToClient();
    }

    private BlockPos rotateOffset(BlockPos offset, Direction facing) {
        return switch (facing) {
            case NORTH -> offset;
            case SOUTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case WEST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            case EAST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            default -> offset;
        };
    }
    
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(doorDecl.getRenderRadius());
    }
}