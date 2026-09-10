package com.hbm_m.entity.projectile;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.item.ModItems;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1-Port von {@code EntityCog} (1.7.10): das Zahnrad, das aus einem ueberdrehten Stirlingmotor
 * fliegt.
 *
 * <p>Das ist kein blosser Effekt. Das Rad <b>toetet, was es trifft</b> (tausend Schaden, also
 * alles), <b>sprengt beim Aufprall</b> auf einen Block und prallt dabei ab - erst wenn es langsam
 * genug ist, bleibt es liegen. Dann laesst es sich mit einem Rechtsklick <b>aufsammeln</b> und
 * wieder in den Motor einbauen.</p>
 *
 * <p>Genau darum ist ein durchgehender Stirlingmotor gefaehrlich und nicht nur teuer: das Rad
 * fliegt in die Halle, und wer im Weg steht, ist weg.</p>
 */
public class CogEntity extends Entity {

    /** Original: {@code dataWatcher} 10 - unter 6 fliegt es, ab 6 liegt es. */
    private static final EntityDataAccessor<Integer> ORIENTATION =
            SynchedEntityData.defineId(CogEntity.class, EntityDataSerializers.INT);

    /** Original: {@code newExplosion(..., 3F, false)} beim Aufprall. */
    private static final float IMPACT_POWER = 3F;
    /** Original: unter dieser Geschwindigkeit bleibt es liegen. */
    private static final double REST_SPEED = 0.75D;

    private int age = 0;

    public CogEntity(EntityType<? extends CogEntity> type, Level level) {
        super(type, level);
    }

    public static CogEntity create(Level level, double x, double y, double z, Direction facing) {
        CogEntity cog = new CogEntity(ModEntities.COG.get(), level);
        cog.setPos(x, y, z);
        cog.entityData.set(ORIENTATION, facing.ordinal());
        return cog;
    }

    //? if < 1.21.1 {

    @Override
    protected void defineSynchedData() {
        entityData.define(ORIENTATION, 0);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        builder.define(ORIENTATION, 0);
    }
    *///?}

    public int getOrientation() { return entityData.get(ORIENTATION); }

    /** Original: {@code orientation >= 6} - es liegt und dreht sich nicht mehr. */
    public boolean isResting() { return getOrientation() >= 6; }

    @Override
    public void tick() {
        if (isResting()) return;

        Vec3 motion = getDeltaMovement();
        Vec3 start = position();
        Vec3 end = start.add(motion);

        // Alles Lebende auf der Bahn - das Rad macht keinen Unterschied.
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().expandTowards(motion).inflate(0.5D), LivingEntity::isAlive)) {

            //? if < 1.21.1 {
            victim.hurt(com.hbm_m.damagesource.ModDamageSources.rubble(level()), 1000F);
            //?} else {
            /*if (level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                victim.hurt(serverLevel, com.hbm_m.damagesource.ModDamageSources.rubble(level()), 1000F);
            }
            *///?}
        }

        BlockHitResult hit = level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

        if (hit.getType() != HitResult.Type.MISS && age > 1) {
            onBlockHit(hit);
        } else {
            move(MoverType.SELF, motion);
        }

        // Schwerkraft und Reibung.
        motion = getDeltaMovement();
        setDeltaMovement(motion.x * 0.98D, (motion.y - 0.04D) * 0.98D, motion.z * 0.98D);

        if (!level().isClientSide()) age++;
    }

    /** 1:1-Port von {@code onImpact} fuer Blocktreffer: abprallen und sprengen, sonst liegenbleiben. */
    private void onBlockHit(BlockHitResult hit) {
        Vec3 motion = getDeltaMovement();

        if (motion.length() < REST_SPEED) {
            // Es ist ausgerollt - ab jetzt liegt es da und laesst sich aufheben.
            entityData.set(ORIENTATION, getOrientation() + 6);
            setDeltaMovement(Vec3.ZERO);
            return;
        }

        Direction side = hit.getDirection();
        setDeltaMovement(
                motion.x * (1 - Math.abs(side.getStepX()) * 2),
                motion.y * (1 - Math.abs(side.getStepY()) * 2),
                motion.z * (1 - Math.abs(side.getStepZ()) * 2));

        if (!level().isClientSide()) {
            level().explode(this, getX(), getY(), getZ(), IMPACT_POWER, Level.ExplosionInteraction.NONE);
        }
    }

    /** 1:1-Port von {@code interactFirst}: aufheben gibt das Zahnrad zurueck. */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide()) return InteractionResult.SUCCESS;

        if (player.getInventory().add(new ItemStack(ModItems.GEAR_LARGE.get()))) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_PICKUP,
                    SoundSource.PLAYERS, 0.2F, 1.0F);
            discard();
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(ORIENTATION, tag.getInt("orientation"));
        age = tag.getInt("age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("orientation", getOrientation());
        tag.putInt("age", age);
    }

    /** Nur zur Vollstaendigkeit - das Rad wird nie geschoben. */
    @Override
    public boolean isPushable() {
        return false;
    }
}
