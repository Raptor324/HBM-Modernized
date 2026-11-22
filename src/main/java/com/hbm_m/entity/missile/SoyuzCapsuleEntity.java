package com.hbm_m.entity.missile;

import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.SoyuzCapsuleBlockEntity;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.hbm_m.platform.StackNbt;

/**
 * 1:1 {@code EntitySoyuzCapsule}: die Frachtkapsel einer Sojus im Frachtmodus. Sie startet auf Hoehe 600 ueber dem
 * Ziel, faellt am Fallschirm mit hoechstens 0,2 Bloecken/Tick und wird beim ersten Nicht-Luft-Block zu einer
 * {@code soyuz_capsule} einen Block hoeher - mit den 18 Frachtplaetzen und der Rakete im Platz 18.
 */
public class SoyuzCapsuleEntity extends Entity {

    private static final double DESCENT_SPEED = 0.6D;

    /**
     * The launcher force-loads the landing chunk so the capsule can descend into terrain nobody is
     * standing in. It used to do that with TicketType.FORCED and never released it, so every cargo
     * launch leaked a permanently loaded region for the rest of the session. The ticket now belongs
     * to the capsule and goes away with it.
     */
    public static final net.minecraft.server.level.TicketType<java.util.UUID> CHUNK_TICKET =
            net.minecraft.server.level.TicketType.create("hbm_m_soyuz_capsule",
                    java.util.Comparator.comparing(java.util.UUID::toString));
    public static final int CHUNK_TICKET_RADIUS = 2;

    private net.minecraft.world.level.ChunkPos loadedChunk;

    public int soyuz;

    private final NonNullList<ItemStack> payload = NonNullList.withSize(18, ItemStack.EMPTY);

    /** Called by the launcher right after it takes the ticket out for this capsule. */
    public void setLoadedChunk(net.minecraft.world.level.ChunkPos pos) {
        this.loadedChunk = pos;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (loadedChunk != null && level() instanceof net.minecraft.server.level.ServerLevel server) {
            server.getChunkSource().removeRegionTicket(CHUNK_TICKET, loadedChunk, CHUNK_TICKET_RADIUS, getUUID());
            loadedChunk = null;
        }
        super.remove(reason);
    }

    public SoyuzCapsuleEntity(EntityType<? extends SoyuzCapsuleEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.noPhysics = true;
    }

    public void setPayload(List<ItemStack> items) {
        for (int i = 0; i < items.size() && i < payload.size(); i++) {
            payload.set(i, items.get(i).copy());
        }
    }

    @Override
    public void tick() {

        this.xo = this.xOld = getX();
        this.yo = this.yOld = getY();
        this.zo = this.zOld = getZ();
        Vec3 m = getDeltaMovement();
        this.setPos(getX() + m.x, getY() + m.y, getZ() + m.z);

        if (m.y > -0.2) setDeltaMovement(m.x, m.y - 0.02, m.z);

        if (getY() > 600) setPos(getX(), 600, getZ());

        BlockPos at = new BlockPos((int) getX(), (int) getY(), (int) getZ());
        if (!level().getBlockState(at).isAir()) {

            this.discard();

            if (!level().isClientSide) {
                BlockPos pos = new BlockPos((int) getX(), (int) (getY() + 1), (int) getZ());
                level().setBlockAndUpdate(pos, ModBlocks.SOYUZ_CAPSULE.get().defaultBlockState());

                if (level().getBlockEntity(pos) instanceof SoyuzCapsuleBlockEntity capsule) {
                    for (int i = 0; i < payload.size(); i++) {
                        capsule.setItem(i, payload.get(i));
                    }
                    capsule.setItem(18, com.hbm_m.item.special.ItemSoyuz.forSkin(soyuz));
                }
            }
        }
    }

    //? if < 1.21.1 {

    @Override
    protected void defineSynchedData() {

var defs = com.hbm_m.platform.EntityDataHooks.sink(this.entityData);
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {

var defs = com.hbm_m.platform.EntityDataHooks.sink(builder);
    *///?}

    
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        soyuz = tag.getInt("soyuz");
        ListTag list = tag.getList("items", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag itemTag = list.getCompound(i);
            int slot = itemTag.getByte("slot");
            if (slot >= 0 && slot < payload.size()) {
                payload.set(slot, StackNbt.parse(itemTag, PlatformHooks.bestEffortProvider()));
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("soyuz", soyuz);
        ListTag list = new ListTag();
        for (int i = 0; i < payload.size(); i++) {
            ItemStack stack = payload.get(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("slot", (byte) i);
                StackNbt.save(stack, itemTag, PlatformHooks.bestEffortProvider());
                list.add(itemTag);
            }
        }
        tag.put("items", list);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500000;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
