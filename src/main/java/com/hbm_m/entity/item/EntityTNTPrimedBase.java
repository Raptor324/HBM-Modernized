package com.hbm_m.entity.item;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.api.block.IFuckingExplode;
import com.hbm_m.entity.ModEntities;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityTNTPrimedBase}: gezuendeter Sprengblock (Lunte 80 Ticks, Bewegung wie TNT), der beim Ablauf die
 * Wirkung seines Blocks ausloest ({@link IFuckingExplode}). Der Block steht als Zustands-ID im Datenwaechter.
 */
public class EntityTNTPrimedBase extends Entity {

    private static final EntityDataAccessor<Integer> BLOCK = SynchedEntityData.defineId(EntityTNTPrimedBase.class, EntityDataSerializers.INT);

    public boolean detonateOnCollision;
    public int fuse;
    @Nullable private LivingEntity tntPlacedBy;

    public EntityTNTPrimedBase(EntityType<? extends EntityTNTPrimedBase> type, Level world) {
        super(type, world);
        this.blocksBuilding = true;
        this.fuse = 80;
        this.detonateOnCollision = false;
    }

    public EntityTNTPrimedBase(Level world, double x, double y, double z, @Nullable LivingEntity placer, Block bomb) {
        this(ModEntities.TNT_PRIMED_BASE.get(), world);
        this.setPos(x, y, z);
        double f = Math.random() * Math.PI * 2.0D;
        this.setDeltaMovement(-Math.sin(f) * 0.02F, 0.2D, -Math.cos(f) * 0.02F);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.tntPlacedBy = placer;
        this.entityData.set(BLOCK, Block.getId(bomb.defaultBlockState()));
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        this.entityData.define(BLOCK, 0);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        builder.define(BLOCK, 0);
    }
    *///?}

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public void tick() {
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        Vec3 m = this.getDeltaMovement().add(0, -0.04D, 0);
        this.setDeltaMovement(m);
        this.move(MoverType.SELF, m);
        this.setDeltaMovement(this.getDeltaMovement().scale(0.98D));

        if (this.onGround()) {
            Vec3 v = this.getDeltaMovement();
            this.setDeltaMovement(v.x * 0.7D, v.y * -0.5D, v.z * 0.7D);
        }

        if (this.fuse-- <= 0 || (this.detonateOnCollision && (this.horizontalCollision || this.verticalCollision))) {
            this.discard();
            if (!this.level().isClientSide) this.explode();
        } else {
            this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5D, this.getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    private void explode() {
        if (getBlock() instanceof IFuckingExplode bomb) bomb.explodeEntity(level(), getX(), getY(), getZ(), this);
    }

    public BlockState getBlockState() {
        return Block.stateById(this.entityData.get(BLOCK));
    }

    public Block getBlock() {
        BlockState s = getBlockState();
        return s == null ? Blocks.AIR : s.getBlock();
    }

    @Nullable
    public LivingEntity getTntPlacedBy() {
        return this.tntPlacedBy;
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        nbt.putByte("Fuse", (byte) this.fuse);
        nbt.putInt("Tile", this.entityData.get(BLOCK));
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        this.fuse = nbt.getByte("Fuse");
        this.entityData.set(BLOCK, nbt.getInt("Tile"));
    }

    //? if < 1.21.1 {
    @NotNull
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(this);
    }
    //?} else {
    /*@NotNull
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(net.minecraft.server.level.ServerEntity serverEntity) {
        return new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(this, serverEntity);
    }
    *///?}
}
