package com.hbm_m.entity.rbmk;

import com.hbm_m.handler.rbmk.RBMKDials;
import com.hbm_m.item.ModItems;
import com.hbm_m.util.ContaminationUtil;
import com.hbm_m.util.ContaminationUtil.ContaminationType;
import com.hbm_m.util.ContaminationUtil.HazardType;

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
import net.minecraft.world.phys.AABB;
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
        entityData.define(TYPE, 0);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        builder.define(TYPE, 0);
    }
    *///?}

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
     * Fuel and graphite chunks stay dangerously hot. The original applies its radiation potion at
     * amplifier 9 (fuel) or 4 (graphite); this port has no radiation potion and instead doses
     * through {@link ContaminationUtil}, the same route {@code ZirnoxDebrisEntity} already takes,
     * with the two tiers kept proportional to the original's amplifiers.
     */
    private void irradiateNearby() {
        // CE irradiates from FUEL debris only, and does it through ContaminationUtil.radiate with a
        // range of 16 and a source strength of 100 - an inverse-square field, not a flat dose. The
        // port applied a flat 2000/900 rads to everything within 2.5 blocks and included graphite,
        // which made standing next to a graphite chunk far deadlier than standing next to fuel.
        if (getDebrisType() != DebrisType.FUEL) return;

        AABB area = getBoundingBox().inflate(RAD_RANGE);
        double cx = getX(), cy = getY(), cz = getZ();

        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, area)) {
            double dx = e.getX() - cx;
            double dy = (e.getY() + e.getEyeHeight()) - cy;
            double dz = e.getZ() - cz;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len > RAD_RANGE) continue;

            // CE clamps the effective distance at 5% of the range so point-blank is finite.
            double dmgLen = Math.max(len, RAD_RANGE * 0.05D);
            float dose = (float) (RAD_STRENGTH / (dmgLen * dmgLen));
            ContaminationUtil.contaminate(e, HazardType.RADIATION, ContaminationType.CREATIVE, dose);
        }
    }

    private static final double RAD_RANGE = 16D;
    private static final float RAD_STRENGTH = 100F;

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
     * Ticks before despawning, once the perma-scrap dial is switched off. Every one of these was a
     * tenth of CE's figure ({@code EntityRBMKDebris.getLifetime}), so scrap from a meltdown swept
     * itself off the site ten times faster than it should - graphite in particular is meant to lie
     * around for two and a half hours, not fifteen minutes.
     */
    private int getLifetime() {
        return switch (getDebrisType()) {
            case BLANK, ELEMENT -> 30 * 60 * 20;
            case FUEL           -> 100 * 60 * 20;
            case GRAPHITE       -> 150 * 60 * 20;
            case LID            -> 300 * 20;
            case ROD            -> 600 * 20;
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
