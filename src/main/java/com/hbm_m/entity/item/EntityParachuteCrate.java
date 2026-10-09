package com.hbm_m.entity.item;

import com.hbm_m.platform.StackNbt;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.generic.SupplyCrateBlock.SupplyCrateBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityParachuteCrate}: schwebt am Fallschirm (hoechstens 0,2 Bloecke/Tick) herab und wird beim ersten
 * Nicht-Luft-Block zu einer {@code crate_supply} einen Block hoeher, in die der Inhalt wandert.
 */
public class EntityParachuteCrate extends Entity {

    public List<ItemStack> items = new ArrayList<>();

    public EntityParachuteCrate(EntityType<? extends EntityParachuteCrate> type, Level world) {
        super(type, world);
        this.noCulling = true;
        this.noPhysics = true;
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

        BlockPos below = new BlockPos(Mth.floor(getX()), Mth.floor(getY()), Mth.floor(getZ()));
        if (!level().getBlockState(below).isAir()) {

            this.discard();

            if (!level().isClientSide) {
                BlockPos at = below.above();
                level().setBlockAndUpdate(at, ModBlocks.CRATE_SUPPLY.get().defaultBlockState());
                if (level().getBlockEntity(at) instanceof SupplyCrateBlockEntity crate) {
                    crate.items.addAll(this.items);
                    crate.setChanged();
                }
            }
        }
    }

    @Override public boolean fireImmune() { return true; }
    @Override public boolean isNoGravity() { return true; }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return true; }

    //? if < 1.21.1 {
    @Override protected void defineSynchedData() { }
    //?} else {
    /*@Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) { }
    *///?}

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        items.clear();
        ListTag list = nbt.getList("items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            items.add(StackNbt.parse(list.getCompound(i)));
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        ListTag list = new ListTag();
        for (ItemStack stack : items) list.add(StackNbt.save(stack, new CompoundTag()));
        nbt.put("items", list);
    }
}
