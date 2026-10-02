package com.hbm_m.entity.conveyor;

import java.util.List;

import com.hbm_m.block.network.IConveyorBelt;
import com.hbm_m.block.network.IEnterablePackageBlock;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityMovingPackage} (mit der Laufschleife aus {@code EntityMovingConveyorObject}): ein Paket mehrerer Stapel
 * auf dem Band. Es betritt {@link IEnterablePackageBlock}-Bloecke nur, wenn deren {@code canPackageEnter} fuer die
 * Eintrittsseite zustimmt, faellt von oben in solche Bloecke hinein, sprengt bei 25 Objekten auf einem Fleck das Band und
 * wirft beim Verlassen des Bandes seinen Inhalt mit Schwung ab. Gezeichnet wird es als Kiste.
 */
public class MovingConveyorPackageEntity extends Entity {

    private static final double MOVE_SPEED = 0.0625D;

    protected ItemStack[] contents = new ItemStack[0];

    public MovingConveyorPackageEntity(EntityType<? extends MovingConveyorPackageEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static MovingConveyorPackageEntity create(Level level, double x, double y, double z, ItemStack[] contents) {
        MovingConveyorPackageEntity entity = new MovingConveyorPackageEntity(ModEntities.MOVING_CONVEYOR_PACKAGE.get(), level);
        entity.setPos(x, y, z);
        entity.setContents(contents);
        return entity;
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() { }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) { }
    *///?}

    /** Original: {@code setItemStacks} mit {@code carefulCopyArray}. */
    public void setContents(ItemStack[] stacks) {
        ItemStack[] copy = new ItemStack[stacks.length];
        for (int i = 0; i < stacks.length; i++) copy[i] = stacks[i] == null ? ItemStack.EMPTY : stacks[i].copy();
        this.contents = copy;
    }

    public ItemStack[] getContents() { return contents; }

    @Override public boolean isPickable() { return true; }
    @Override public boolean isAttackable() { return true; }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        hitByEntity();
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        hitByEntity();
        return true;
    }

    private void hitByEntity() {
        if (!this.level().isClientSide && !this.isRemoved()) {
            this.discard();
            for (ItemStack stack : contents) {
                if (stack.isEmpty()) continue;
                this.level().addFreshEntity(new ItemEntity(this.level(), getX(), getY() + 0.125, getZ(), stack));
            }
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && !this.isRemoved()) {
            for (ItemStack stack : contents) {
                if (stack.isEmpty()) continue;
                if (!player.getInventory().add(stack.copy())) {
                    this.level().addFreshEntity(new ItemEntity(this.level(), getX(), getY() + 0.125, getZ(), stack));
                }
            }
            this.discard();
        }
        return InteractionResult.PASS;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) return;
        if (this.tickCount <= 5) return;

        // Staupruefung alle 20 s
        if ((this.tickCount + this.getId()) % 400 == 0) {
            List<Entity> objs = this.level().getEntitiesOfClass(Entity.class, this.getBoundingBox().inflate(0.125, 0.125, 0.125),
                    e -> e instanceof MovingConveyorPackageEntity || e instanceof MovingConveyorItemEntity);
            if (objs.size() >= 25) {
                for (Entity obj : objs) obj.discard();
                this.level().explode(this, getX(), getY() + 0.125, getZ(), 1.0F, Level.ExplosionInteraction.BLOCK);
                BlockPos p = BlockPos.containing(getX(), getY(), getZ());
                if (this.level().getBlockState(p).getBlock() instanceof IConveyorBelt) this.level().destroyBlock(p, false);
            }
        }

        BlockPos blockPos = BlockPos.containing(getX(), getY(), getZ());
        BlockState state = this.level().getBlockState(blockPos);
        Vec3 currentPos = new Vec3(getX(), getY(), getZ());

        boolean isOnConveyor = state.getBlock() instanceof IConveyorBelt belt && belt.canItemStay(this.level(), blockPos, currentPos);

        if (!isOnConveyor) {
            if (onLeaveConveyor()) return;
        } else {
            Vec3 target = ((IConveyorBelt) state.getBlock()).getTravelLocation(this.level(), blockPos, currentPos, MOVE_SPEED);
            this.setDeltaMovement(target.x - getX(), target.y - getY(), target.z - getZ());
        }

        BlockPos lastPos = BlockPos.containing(getX(), getY(), getZ());
        Vec3 m = getDeltaMovement();
        this.setPos(getX() + m.x, getY() + m.y, getZ() + m.z);
        BlockPos newPos = BlockPos.containing(getX(), getY(), getZ());

        if (!lastPos.equals(newPos)) {
            Block newBlock = this.level().getBlockState(newPos).getBlock();

            if (newBlock instanceof IEnterablePackageBlock enterable) {
                Direction dir = null;
                BlockPos d = lastPos.subtract(newPos);
                if (d.distManhattan(BlockPos.ZERO) == 1) dir = Direction.fromDelta(d.getX(), d.getY(), d.getZ());
                enterBlock(enterable, newPos, dir);
            } else if (!this.level().getBlockState(newPos).isSolid()) {
                BlockPos below = newPos.below();
                if (this.level().getBlockState(below).getBlock() instanceof IEnterablePackageBlock enterable) {
                    enterBlock(enterable, below, Direction.UP);
                }
            }
        }
    }

    public void enterBlock(IEnterablePackageBlock enterable, BlockPos pos, Direction dir) {
        if (this.isRemoved()) return;
        if (enterable.canPackageEnter(this.level(), pos, dir, this)) {
            enterable.onPackageEnter(this.level(), pos, this);
            this.discard();
        }
    }

    /** @return true, wenn die Schleife enden soll */
    public boolean onLeaveConveyor() {
        this.discard();
        Vec3 m = getDeltaMovement();
        for (ItemStack stack : contents) {
            if (stack.isEmpty()) continue;
            ItemEntity item = new ItemEntity(this.level(), getX() + m.x * 2, getY() + m.y * 2, getZ() + m.z * 2, stack);
            item.setDeltaMovement(m.x * 2, 0.1, m.z * 2);
            this.level().addFreshEntity(item);
        }
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        this.contents = new ItemStack[nbt.getInt("count")];
        java.util.Arrays.fill(this.contents, ItemStack.EMPTY);
        ListTag list = nbt.getList("contents", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            int j = c.getByte("slot") & 255;
            if (j >= 0 && j < this.contents.length) this.contents[j] = PlatformHooks.itemStackOf(c, this.level().registryAccess());
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        ListTag list = new ListTag();
        for (int i = 0; i < this.contents.length; ++i) {
            if (!this.contents[i].isEmpty()) {
                CompoundTag c = PlatformHooks.safeItemSave(this.contents[i], this.level().registryAccess());
                c.putByte("slot", (byte) i);
                list.add(c);
            }
        }
        nbt.put("contents", list);
        nbt.putInt("count", this.contents.length);
    }
}
