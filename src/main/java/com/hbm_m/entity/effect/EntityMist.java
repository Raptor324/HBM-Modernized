package com.hbm_m.entity.effect;

import java.util.List;

import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.damagesource.ModDamageTypes;
import com.hbm_m.entity.mob.glyphid.EntityGlyphid;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.trait.FT_Corrosive;
import com.hbm_m.inventory.fluid.trait.FT_Flammable;
import com.hbm_m.inventory.fluid.trait.FT_Pheromone;
import com.hbm_m.inventory.fluid.trait.FT_Poison;
import com.hbm_m.inventory.fluid.trait.FT_Toxin;
import com.hbm_m.inventory.fluid.trait.FT_VentRadiation;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple;
import com.hbm_m.radiation.ChunkRadiationManager;
import com.hbm_m.util.ArmorUtil;
import com.hbm_m.util.ContaminationUtil;
import com.hbm_m.util.ContaminationUtil.ContaminationType;
import com.hbm_m.util.ContaminationUtil.HazardType;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code EntityMist}: Fluidwolke mit allen Wirkungen nach Temperatur und Fluideigenschaften (Hitze, Kaelte, Oel,
 * Loeschen, Saeure, Strahlung, Gift, Toxine, Pheromone, Enderteleport); brennbare Wolken explodieren, sobald sie
 * Feuer fangen. Clientseitig steigt farbiger Dunst auf.
 */
public class EntityMist extends Entity {

    private static final EntityDataAccessor<Integer> DATA_FLUID_ID =
            SynchedEntityData.defineId(EntityMist.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_WIDTH =
            SynchedEntityData.defineId(EntityMist.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEIGHT =
            SynchedEntityData.defineId(EntityMist.class, EntityDataSerializers.FLOAT);

    private int maxAge = 150;

    public EntityMist(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    //? if < 1.21.1 {

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_FLUID_ID, BuiltInRegistries.FLUID.getId(ModFluids.NONE.getSource()));
        this.entityData.define(DATA_WIDTH, 0.0F);
        this.entityData.define(DATA_HEIGHT, 0.0F);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {

        builder.define(DATA_FLUID_ID, BuiltInRegistries.FLUID.getId(ModFluids.NONE.getSource()));
        builder.define(DATA_WIDTH, 0.0F);
        builder.define(DATA_HEIGHT, 0.0F);

    }
    *///?}

    public EntityMist setFluidType(FluidType fluidType) {
        Fluid fluid = fluidType.getFluid();
        this.entityData.set(DATA_FLUID_ID, BuiltInRegistries.FLUID.getId(fluid));
        return this;
    }

    public FluidType getFluidType() {
        Fluid fluid = BuiltInRegistries.FLUID.byId(this.entityData.get(DATA_FLUID_ID));
        return FluidType.forFluid(fluid);
    }

    public EntityMist setArea(float width, float height) {
        this.entityData.set(DATA_WIDTH, width);
        this.entityData.set(DATA_HEIGHT, height);
        return this;
    }

    public EntityMist setDuration(int duration) {
        this.maxAge = duration;
        return this;
    }

    public int getMaxAge() {
        return this.maxAge;
    }

    @Override
    public void tick() {
        float width = this.entityData.get(DATA_WIDTH);
        float height = this.entityData.get(DATA_HEIGHT);
        this.setPos(this.getX(), this.getY(), this.getZ());
        this.setBoundingBox(new AABB(
                this.getX() - width / 2.0,
                this.getY(),
                this.getZ() - width / 2.0,
                this.getX() + width / 2.0,
                this.getY() + height,
                this.getZ() + width / 2.0));

        if (!this.level().isClientSide) {

            if (this.tickCount >= this.getMaxAge()) {
                this.discard();
            }

            FluidType type = this.getFluidType();

            if (type.hasTrait(FT_VentRadiation.class)) {
                FT_VentRadiation trait = type.getTrait(FT_VentRadiation.class);
                ChunkRadiationManager.incrementRad(level(), (int) Math.floor(getX()), (int) Math.floor(getY()), (int) Math.floor(getZ()), trait.getRadPerMB() * 2);
            }

            double intensity = 1.0D - (double) this.tickCount / (double) this.getMaxAge();

            if (type.hasTrait(FT_Flammable.class) && this.isOnFire()) {
                level().explode(this, getX(), getY() + height / 2, getZ(), (float) intensity * 15F, true, Level.ExplosionInteraction.TNT);
                this.discard();
                return;
            }

            // Original: boundingBox.copy().offset(-width / 2, 0, -width / 2)
            AABB box = this.getBoundingBox().move(-width / 2.0, 0.0, -width / 2.0);
            List<Entity> affected = this.level().getEntities(this, box);
            for (Entity entity : affected) {
                this.affect(entity, type, intensity);
            }
        } else {

            AABB bb = this.getBoundingBox();
            for (int i = 0; i < 2; i++) {
                double x = bb.minX + (random.nextDouble() - 0.5) * (bb.maxX - bb.minX);
                double y = bb.minY + random.nextDouble() * (bb.maxY - bb.minY);
                double z = bb.minZ + (random.nextDouble() - 0.5) * (bb.maxZ - bb.minZ);

                CompoundTag fx = new CompoundTag();
                fx.putString("type", "tower");
                fx.putFloat("lift", 0.5F);
                fx.putFloat("base", 0.75F);
                fx.putFloat("max", 2F);
                fx.putInt("life", 50 + level().random.nextInt(10));
                fx.putInt("color", this.getFluidType().getColor());
                fx.putDouble("posX", x);
                fx.putDouble("posY", y);
                fx.putDouble("posZ", z);
                com.hbm_m.particle.helper.ParticleEffectClient.effectNT(fx);
            }
        }
    }

    /** {@code EntityDamageUtil.attackEntityFromIgnoreIFrame}. */
    private static void hurtIgnoreIFrame(Entity e, DamageSource source, float amount) {
        e.invulnerableTime = 0;
        e.hurt(source, amount);
    }

    /* the original notes that EntityChemical cannot be reused here, the effects often differ */
    protected void affect(Entity e, FluidType type, double intensity) {

        LivingEntity living = e instanceof LivingEntity l ? l : null;
        Level level = this.level();

        if (type.temperature >= 100) {
            hurtIgnoreIFrame(e, ModDamageSources.create(level, ModDamageTypes.BOIL), 0.2F + (type.temperature - 100) * 0.02F);

            if (type.temperature >= 500) {
                e.setSecondsOnFire(10); //afterburn for 10 seconds
            }
        }
        if (type.temperature < -20) {
            if (living != null) { //only living things are affected
                hurtIgnoreIFrame(e, ModDamageSources.create(level, ModDamageTypes.ICE), 0.2F + (type.temperature + 20) * -0.05F); //5 damage at -20°C with one extra damage every -20°C
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
                living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 4));
            }
        }

        if (type.hasTrait(FluidTraitSimple.FT_Delicious.class)) {
            if (living != null && living.isAlive()) {
                living.heal(2F * (float) intensity);
            }
        }

        if (type.hasTrait(FT_Flammable.class) && type.hasTrait(FluidTraitSimple.FT_Liquid.class)) {
            if (living != null) {
                HbmLivingProps.setOil(living, 200); //doused in oil for 10 seconds
            }
        }

        if (this.isExtinguishing(type)) {
            e.clearFire();
        }

        if (type.hasTrait(FT_Corrosive.class)) {
            FT_Corrosive trait = type.getTrait(FT_Corrosive.class);

            if (living != null) {
                hurtIgnoreIFrame(living, ModDamageSources.acid(level), trait.getRating() / 60F);
                for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD }) {
                    ArmorUtil.damageSuit(living, slot, trait.getRating() / 50);
                }
            }
        }

        if (type.hasTrait(FT_VentRadiation.class)) {
            FT_VentRadiation trait = type.getTrait(FT_VentRadiation.class);
            if (living != null) {
                ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.CREATIVE, trait.getRadPerMB() * 5);
            }
        }

        if (type.hasTrait(FT_Poison.class)) {
            FT_Poison trait = type.getTrait(FT_Poison.class);

            if (living != null) {
                living.addEffect(new MobEffectInstance(trait.isWithering() ? MobEffects.WITHER : MobEffects.POISON, (int) (5 * 20 * intensity)));
            }
        }

        if (type.hasTrait(FT_Toxin.class)) {
            FT_Toxin trait = type.getTrait(FT_Toxin.class);

            if (living != null) {
                trait.affect(living, intensity);
            }
        }

        if (type.getFluid() == ModFluids.ENDERJUICE.getSource() && living != null) {
            teleportRandomly(living);
        }

        if (type.hasTrait(FT_Pheromone.class)) {

            FT_Pheromone pheromone = type.getTrait(FT_Pheromone.class);

            if (living != null) {
                if ((living instanceof EntityGlyphid && pheromone.getType() == 1) || (living instanceof Player && pheromone.getType() == 2)) {
                    int mult = pheromone.getType();

                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, mult * 60 * 20, 1));
                    living.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, mult * 60 * 20, 1));
                    living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, mult * 2 * 20, 0));
                    living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, mult * 60 * 20, 0));
                    living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, mult * 60 * 20, 1));
                    living.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, mult * 60 * 20, 0));
                }
            }
        }
    }

    protected boolean isExtinguishing(FluidType type) {
        return this.getFluidType().temperature < 50 && !type.hasTrait(FT_Flammable.class);
    }

    /** Original {@code teleportRandomly} (Endermann-Kopie): bis 32 Bloecke weit auf festen Boden. */
    public void teleportRandomly(Entity e) {
        double x = this.getX() + (this.random.nextDouble() - 0.5D) * 64.0D;
        double y = this.getY() + (this.random.nextInt(64) - 32);
        double z = this.getZ() + (this.random.nextDouble() - 0.5D) * 64.0D;
        if (e instanceof LivingEntity living) living.randomTeleport(x, y, z, true);
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(DATA_FLUID_ID, tag.getInt("fluidId"));
        this.setArea(tag.getFloat("width"), tag.getFloat("height"));
        this.maxAge = tag.getInt("maxAge");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("fluidId", this.entityData.get(DATA_FLUID_ID));
        tag.putFloat("width", this.entityData.get(DATA_WIDTH));
        tag.putFloat("height", this.entityData.get(DATA_HEIGHT));
        tag.putInt("maxAge", this.maxAge);
    }

    @Override
    public void setPos(double x, double y, double z) {
        if (this.tickCount == 0) {
            super.setPos(x, y, z);
        }
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    protected boolean canRide(Entity vehicle) {
        return false;
    }
}
