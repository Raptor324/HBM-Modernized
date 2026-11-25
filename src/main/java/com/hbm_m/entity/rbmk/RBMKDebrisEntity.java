package com.hbm_m.entity.rbmk;

import com.hbm_m.handler.rbmk.RBMKDials;
import com.hbm_m.item.ModItems;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;

/**
 * 1:1 port of {@code com.hbm.entity.projectile.EntityRBMKDebris} - the wreckage a melting RBMK
 * throws into the air.
 *
 * <p>Six kinds, each with its own hitbox, lifetime and salvage item. Fuel and graphite chunks
 * irradiate anything within 2.5 blocks; a launched lid punches a hole through whatever ceiling it
 * hits on the way up, which is the original's signature "the lid goes through the roof" moment.
 * Walking into a piece picks it up. With the perma-scrap dial on (the default) debris never
 * despawns.</p>
 */
public class RBMKDebrisEntity extends Entity {

    private static final EntityDataAccessor<Integer> TYPE =
            SynchedEntityData.defineId(RBMKDebrisEntity.class, EntityDataSerializers.INT);

    public float rot;
    public float lastRot;
    private boolean hasSizeSet = false;

    public RBMKDebrisEntity(EntityType<? extends RBMKDebrisEntity> type, Level level) {
        super(type, level);
    }

    public static RBMKDebrisEntity create(Level level, double x, double y, double z, DebrisType type) {
        RBMKDebrisEntity debris = new RBMKDebrisEntity(com.hbm_m.entity.ModEntities.RBMK_DEBRIS.get(), level);
        debris.setPos(x, y, z);
        debris.setDebrisType(type);
        return debris;
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
        defs.define(TYPE, 0);
    }

    public void setDebrisType(DebrisType type) { entityData.set(TYPE, type.ordinal()); }

    public DebrisType getDebrisType() {
        return DebrisType.values()[Math.abs(entityData.get(TYPE)) % DebrisType.values().length];
    }

    /** Walking into a piece salvages it, matching the original's interactFirst. */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide) {
            ItemStack loot = switch (getDebrisType()) {
                case FUEL     -> new ItemStack(ModItems.DEBRIS_FUEL.get());
                case GRAPHITE -> new ItemStack(ModItems.DEBRIS_GRAPHITE.get());
                case LID      -> new ItemStack(ModItems.RBMK_LID.get());
                default       -> new ItemStack(ModItems.DEBRIS_METAL.get());
            };
            if (player.getInventory().add(loot)) discard();
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void tick() {
        // Entity.tick() is the only thing that advances tickCount, and the despawn check below
        // reads it. Without this, debris lived forever even with the perma-scrap dial off.
        baseTick();


        if (!hasSizeSet) {
            hasSizeSet = true;
            refreshDimensions();
        }

        if (!level().isClientSide) {
            if (getDebrisType() == DebrisType.LID && getDeltaMovement().y > 0) punchThroughCeiling();
            irradiateNearby();

            if (!RBMKDials.getPermaScrap(level()) && tickCount > getLifetime() + getId() % 50) discard();
        }

        xo = getX();
        yo = getY();
        zo = getZ();

        setDeltaMovement(getDeltaMovement().subtract(0, 0.04D, 0));
        move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());

        lastRot = rot;

        Vec3 motion = getDeltaMovement();
        if (onGround()) {
            setDeltaMovement(motion.x * 0.85D, motion.y * -0.5D, motion.z * 0.85D);
        } else {
            rot += 10F;
            if (rot >= 360F) {
                rot -= 360F;
                lastRot -= 360F;
            }
        }
    }

    /** A rising lid clears a rough 3x3x3 out of the first block it meets, then dies there. */
    private void punchThroughCeiling() {
        Vec3 from = position();
        Vec3 to = from.add(getDeltaMovement().scale(2));
        BlockHitResult hit = level().clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() != HitResult.Type.BLOCK) return;

        BlockPos origin = hit.getBlockPos();
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                for (int k = -1; k <= 1; k++) {
                    int rn = Math.abs(i) + Math.abs(j) + Math.abs(k);
                    if (rn <= 1 || random.nextInt(rn) == 0)
                        level().setBlockAndUpdate(origin.offset(i, j, k), Blocks.AIR.defaultBlockState());
                }
            }
        }
        discard();
    }

    /**
     * Fuel and graphite chunks stay dangerously hot. 1:1 with
     * {@code EntityRBMKDebris.onUpdate} (1.7.10 :100-107): BOTH types dose every living being
     * within {@code boundingBox.expand(2.5, 2.5, 2.5)} each tick by applying the radiation
     * potion for 60 seconds - amplifier 9 for fuel, 4 for graphite. No distance-scaled field.
     */
    private void irradiateNearby() {
        DebrisType type = getDebrisType();
        if (type != DebrisType.FUEL && type != DebrisType.GRAPHITE) return;

        int amplifier = type == DebrisType.FUEL ? 9 : 4;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(2.5))) {
            e.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    com.hbm_m.platform.EffectHooks.of(com.hbm_m.effect.ModEffects.RADIATION), 60 * 20, amplifier));
        }
    }

    @Override
    public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) {
        return switch (getDebrisType()) {
            case BLANK    -> net.minecraft.world.entity.EntityDimensions.scalable(0.5F, 0.5F);
            case ELEMENT  -> net.minecraft.world.entity.EntityDimensions.scalable(1F, 1F);
            case FUEL     -> net.minecraft.world.entity.EntityDimensions.scalable(0.25F, 0.25F);
            case GRAPHITE -> net.minecraft.world.entity.EntityDimensions.scalable(0.25F, 0.25F);
            case LID      -> net.minecraft.world.entity.EntityDimensions.scalable(1F, 0.5F);
            case ROD      -> net.minecraft.world.entity.EntityDimensions.scalable(0.75F, 0.5F);
        };
    }

    /**
     * Ticks before despawning, once the perma-scrap dial is switched off. 1:1 with
     * {@code EntityRBMKDebris.getLifetime} (1.7.10 :139-150): blank/element scrap lies around for
     * 3 minutes, fuel for 10, graphite for a full 15, the lid only 30 seconds and a control rod
     * one minute. The despawn check adds the same {@code getEntityId() % 50} jitter as the
     * original.
     */
    private int getLifetime() {
        return switch (getDebrisType()) {
            case BLANK, ELEMENT -> 3 * 60 * 20;
            case FUEL           -> 10 * 60 * 20;
            case GRAPHITE       -> 15 * 60 * 20;
            case LID            -> 30 * 20;
            case ROD            -> 60 * 20;
        };
    }

    @Override public boolean isPickable() { return true; }

    @Override protected void readAdditionalSaveData(CompoundTag tag) { entityData.set(TYPE, tag.getInt("debtype")); }
    @Override protected void addAdditionalSaveData(CompoundTag tag)  { tag.putInt("debtype", entityData.get(TYPE)); }

    public enum DebrisType {
        BLANK,      // just a metal beam
        ELEMENT,    // the entire casing of a fuel assembly
        FUEL,       // spicy
        ROD,        // solid boron rod
        GRAPHITE,   // spicy rock
        LID         // the all destroying harbinger of annihilation
    }
}
