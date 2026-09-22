package com.hbm_m.entity.effect;

import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.trait.FT_Corrosive;
import com.hbm_m.inventory.fluid.trait.FT_Poison;
import com.hbm_m.inventory.fluid.trait.FT_VentRadiation;
import com.hbm_m.particle.ModParticleTypes;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Облако жидкости/газа (урон, яд, коррозия). Порт {@link com.hbm.entity.effect.EntityMist} (1.7.10).
 * Полный паритет трейтов — по мере портирования; phosgene cloud damage совпадает с {@code ToxinDirectDamage(cloud, 4F, 20)}.
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

var defs = com.hbm_m.platform.EntityDataHooks.sink(this.entityData);
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {

var defs = com.hbm_m.platform.EntityDataHooks.sink(builder);
    *///?}

        defs.define(DATA_FLUID_ID, BuiltInRegistries.FLUID.getId(ModFluids.NONE.getSource()));
        defs.define(DATA_WIDTH, 0.0F);
        defs.define(DATA_HEIGHT, 0.0F);
    
    }

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

        super.tick();

        if (!this.level().isClientSide) {
            if (this.tickCount >= this.getMaxAge()) {
                this.discard();
                return;
            }

            FluidType type = this.getFluidType();
            double intensity = 1.0D - (double) this.tickCount / (double) this.getMaxAge();

            // The original does aabb.offset(-width/2, 0, -width/2) on a corner-anchored box, which
            // just centres it. The port builds the box centred already and then used inflate instead
            // of offset, shrinking it by width/2 per side into a zero-width slab: only an entity
            // straddling the mist axis was ever affected.
            AABB box = this.getBoundingBox();
            List<Entity> affected = this.level().getEntities(this, box);
            for (Entity entity : affected) {
                this.affect(entity, type, intensity);
            }
        } else {
            spawnCloudParticles(width, height);
        }
    }

    /** 1.7.10 client branch: two tinted "tower" puffs per tick anywhere inside the cloud volume. */
    private void spawnCloudParticles(float width, float height) {
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }
        // Colour rides in the speed triple (see MistParticle); no client class is touched here.
        int color = this.getFluidType().getColor();
        double r = ((color >> 16) & 0xFF) / 255.0D;
        double g = ((color >> 8) & 0xFF) / 255.0D;
        double b = (color & 0xFF) / 255.0D;
        RandomSource rand = this.level().random;
        for (int i = 0; i < 2; i++) {
            double px = this.getX() + (rand.nextDouble() - 0.5D) * width;
            double py = this.getY() + rand.nextDouble() * height;
            double pz = this.getZ() + (rand.nextDouble() - 0.5D) * width;
            this.level().addParticle(ModParticleTypes.MIST.get(), px, py, pz, r, g, b);
        }
    }

    protected void affect(Entity entity, FluidType type, double intensity) {
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }

        Level level = this.level();
        DamageSource cloud = ModDamageSources.cloud(level);

        if (type.getFluid() == ModFluids.PHOSGENE.getSource()) {
            if (level.getGameTime() % 20L == 0L) {
                living.hurt(cloud, (float) (4.0D * intensity));
            }
            return;
        }

        FT_Corrosive corrosive = type.getTrait(FT_Corrosive.class);
        if (corrosive != null) {
            living.hurt(cloud, corrosive.getRating() / 60.0F);
        }

        FT_VentRadiation radiation = type.getTrait(FT_VentRadiation.class);
        if (radiation != null) {
            // ChunkRadiationManager — после полного порта радиации из EntityMist 1.7.10
        }

        FT_Poison poison = type.getTrait(FT_Poison.class);
        if (poison != null) {
            living.addEffect(new MobEffectInstance(
                    poison.isWithering() ? MobEffects.WITHER : MobEffects.POISON,
                    (int) (5 * 20 * intensity)));
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // The numeric registry id is fine for the synced accessor - both sides share one session's
        // registry - but not on disk: it depends on registration order, so adding or removing any
        // mod that registers fluids turned a saved cloud into a different one. Old saves still
        // carry the numeric key, so it is read as a fallback.
        if (tag.contains("fluidPath")) {
            Fluid fluid = BuiltInRegistries.FLUID.get(net.minecraft.resources.ResourceLocation
                    .fromNamespaceAndPath(tag.getString("fluidNs"), tag.getString("fluidPath")));
            this.entityData.set(DATA_FLUID_ID, BuiltInRegistries.FLUID.getId(fluid));
        } else {
            this.entityData.set(DATA_FLUID_ID, tag.getInt("fluidId"));
        }
        this.setArea(tag.getFloat("width"), tag.getFloat("height"));
        this.maxAge = tag.getInt("maxAge");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        var key = BuiltInRegistries.FLUID.getKey(BuiltInRegistries.FLUID.byId(this.entityData.get(DATA_FLUID_ID)));
        tag.putString("fluidNs", key.getNamespace());
        tag.putString("fluidPath", key.getPath());
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
