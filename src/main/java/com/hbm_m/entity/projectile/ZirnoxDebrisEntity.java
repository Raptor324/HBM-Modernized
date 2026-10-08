package com.hbm_m.entity.projectile;

import java.util.List;

import com.hbm_m.effect.ModEffects;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.handler.rbmk.RBMKDials;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityZirnoxDebris} (auf {@code EntityDebrisBase}): Truemmer der Zirnox-Kernschmelze. Sechs Arten mit
 * eigener Groesse, Lebensdauer und Bergungsgegenstand (Rechtsklick). Aufsteigender Beton und Waermetauscher reissen
 * ein 3x3x3-Loch in die erste Decke; Brennelemente (Strahlung 8) und Graphit (Strahlung 5) verstrahlen alles im Umkreis
 * von 2,5 Bloecken. Mit "perma scrap" bleiben sie liegen.
 */
public class ZirnoxDebrisEntity extends Entity {

    private static final EntityDataAccessor<Integer> TYPE =
            SynchedEntityData.defineId(ZirnoxDebrisEntity.class, EntityDataSerializers.INT);

    public float rot;
    public float lastRot;
    private boolean hasSizeSet = false;

    public ZirnoxDebrisEntity(EntityType<? extends ZirnoxDebrisEntity> type, Level level) {
        super(type, level);
    }

    public static ZirnoxDebrisEntity create(Level level, double x, double y, double z, DebrisType type) {
        ZirnoxDebrisEntity e = new ZirnoxDebrisEntity(ModEntities.ZIRNOX_DEBRIS.get(), level);
        e.setPos(x, y, z);
        e.setDebrisType(type);
        return e;
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        entityData.define(TYPE, 0);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        builder.define(TYPE, 0);
    }
    *///?}

    public void setDebrisType(DebrisType type) {
        entityData.set(TYPE, type.ordinal());
    }

    public DebrisType getDebrisType() {
        DebrisType[] values = DebrisType.values();
        return values[Math.abs(entityData.get(TYPE)) % values.length];
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {

        if (!level().isClientSide) {

            ItemStack loot = switch (getDebrisType()) {
                case BLANK -> new ItemStack(ModItems.DEBRIS_METAL.get());
                case ELEMENT -> new ItemStack(ModItems.DEBRIS_ELEMENT.get());
                case SHRAPNEL -> new ItemStack(ModItems.DEBRIS_SHRAPNEL.get());
                case GRAPHITE -> new ItemStack(ModItems.DEBRIS_GRAPHITE.get());
                case CONCRETE -> new ItemStack(ModItems.DEBRIS_CONCRETE.get());
                case EXCHANGER -> new ItemStack(ModItems.DEBRIS_EXCHANGER.get());
            };
            if (player.getInventory().add(loot)) this.discard();

            player.inventoryMenu.broadcastChanges();
        }

        return InteractionResult.PASS;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return switch (getDebrisType()) {
            case BLANK -> EntityDimensions.scalable(0.5F, 0.5F);
            case ELEMENT -> EntityDimensions.scalable(0.75F, 0.5F);
            case SHRAPNEL -> EntityDimensions.scalable(0.5F, 0.5F);
            case GRAPHITE -> EntityDimensions.scalable(0.25F, 0.25F);
            case CONCRETE -> EntityDimensions.scalable(0.75F, 0.5F);
            case EXCHANGER -> EntityDimensions.scalable(1F, 0.5F);
        };
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (TYPE.equals(key)) refreshDimensions();
    }

    @Override
    public void tick() {

        if (!hasSizeSet) {
            refreshDimensions();
            hasSizeSet = true;
        }

        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();

        Vec3 m = getDeltaMovement();
        this.setDeltaMovement(m.x, m.y - 0.04D, m.z);
        this.move(MoverType.SELF, getDeltaMovement());

        this.lastRot = this.rot;

        m = getDeltaMovement();
        if (this.onGround()) {
            this.setDeltaMovement(m.x * 0.85D, m.y * -0.5D, m.z * 0.85D);

        } else {

            this.rot += 10F;

            if (rot >= 360F) {
                this.rot -= 360F;
                this.lastRot -= 360F;
            }
        }

        if (!level().isClientSide) {
            m = getDeltaMovement();
            if ((this.getDebrisType() == DebrisType.CONCRETE || this.getDebrisType() == DebrisType.EXCHANGER) && m.y > 0) {

                Vec3 pos = position();
                Vec3 next = pos.add(m.x * 2, m.y * 2, m.z * 2);
                BlockHitResult mop = level().clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

                if (mop.getType() == HitResult.Type.BLOCK) {

                    BlockPos p = mop.getBlockPos();

                    for (int i = -1; i <= 1; i++) {
                        for (int j = -1; j <= 1; j++) {
                            for (int k = -1; k <= 1; k++) {

                                int rn = Math.abs(i) + Math.abs(j) + Math.abs(k);

                                if (rn <= 1 || random.nextInt(rn) == 0)
                                    level().setBlockAndUpdate(p.offset(i, j, k), Blocks.AIR.defaultBlockState());
                            }
                        }
                    }

                    this.discard();
                }
            }

            if (this.getDebrisType() == DebrisType.ELEMENT || this.getDebrisType() == DebrisType.GRAPHITE) {
                List<LivingEntity> entities = level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(2.5, 2.5, 2.5));

                int lvl = this.getDebrisType() == DebrisType.ELEMENT ? 7 : 4;
                for (LivingEntity e : entities) {
                    e.addEffect(new MobEffectInstance(ModEffects.RADIATION.get(), 60 * 20, lvl));
                }
            }

            if (!RBMKDials.getPermaScrap(level()) && this.tickCount > getLifetime() + this.getId() % 50)
                this.discard();
        }
    }

    protected int getLifetime() {
        return switch (getDebrisType()) {
            case BLANK -> 3 * 60 * 20;
            case ELEMENT -> 10 * 60 * 20;
            case SHRAPNEL -> 15 * 60 * 20;
            case GRAPHITE -> 15 * 60 * 20;
            case CONCRETE -> 60 * 20;
            case EXCHANGER -> 60 * 20;
        };
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(TYPE, tag.getInt("debtype"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("debtype", entityData.get(TYPE));
    }

    public enum DebrisType {
        BLANK,          //just a metal beam
        ELEMENT,        //fuel element
        SHRAPNEL,       //steel shrapnel from the pipes and walkways
        GRAPHITE,       //spicy rock
        CONCRETE,       //the all destroying harbinger of annihilation
        EXCHANGER       //the all destroying harbinger of annihilation: sideways edition
    }
}
