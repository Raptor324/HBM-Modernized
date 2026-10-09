package com.hbm_m.entity.projectile;

import com.hbm_m.platform.PlatformHooks;

import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.damagesource.ModDamageTypes;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.mob.glyphid.EntityGlyphid;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.trait.FT_Combustible;
import com.hbm_m.inventory.fluid.trait.FT_Corrosive;
import com.hbm_m.inventory.fluid.trait.FT_Flammable;
import com.hbm_m.inventory.fluid.trait.FT_Pheromone;
import com.hbm_m.inventory.fluid.trait.FT_Poison;
import com.hbm_m.inventory.fluid.trait.FT_VentRadiation;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple;
import com.hbm_m.particle.helper.FlameCreator;
import com.hbm_m.radiation.ChunkRadiationManager;
import com.hbm_m.util.ArmorUtil;
import com.hbm_m.util.ContaminationUtil;
import com.hbm_m.util.ContaminationUtil.ContaminationType;
import com.hbm_m.util.ContaminationUtil.HazardType;
import com.hbm_m.util.EnchantmentUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 1:1 {@code EntityChemical}: Spruehgeschoss des Chemiewerfers und des Behemoth-Saeureatems. Das Verhalten haengt am
 * Fluid: Antimaterie als Strahl, Gase als kurze Wolke, Fluessigkeiten als Sprue mit Schwerkraft, Brennbares entzuendet
 * sich. Wirkungen nach Temperatur und Fluideigenschaften (aetzend, giftig, radioaktiv, Pheromone ...).
 *
 */
public class EntityChemical extends EntityThrowableNT {

    private static final EntityDataAccessor<String> FLUID = SynchedEntityData.defineId(EntityChemical.class, EntityDataSerializers.STRING);

    public EntityChemical(EntityType<? extends EntityChemical> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public EntityChemical(Level world, LivingEntity thrower, double sideOffset, double heightOffset, double frontOffset) {
        super(ModEntities.CHEMICAL.get(), world, thrower);
        this.noCulling = true;
    }

    //? if < 1.21.1 {
    @Override
    protected void defineExtraData() {
        this.entityData.define(FLUID, "");
    }
    //?} else {
    /*@Override
    protected void defineExtraData(SynchedEntityData.Builder builder) {
        builder.define(FLUID, "");
    }
    *///?}

    @Override
    public boolean fireImmune() {
        return true;
    }

    public EntityChemical setFluid(Fluid fluid) {
        this.entityData.set(FLUID, net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid).toString());
        return this;
    }

    public Fluid getFluid() {
        String id = this.entityData.get(FLUID);
        if (id.isEmpty()) return ModFluids.NONE.getSource();
        return net.minecraft.core.registries.BuiltInRegistries.FLUID.get(net.minecraft.resources.ResourceLocation.parse(id));
    }

    public FluidType getFluidType() {
        return FluidType.forFluid(getFluid());
    }

    @Override
    public void tick() {

        if (!level().isClientSide) {

            if (this.tickCount > this.getMaxAge()) {
                this.discard();
            }

            FluidType type = this.getFluidType();

            if (type.hasTrait(FluidTraitSimple.FT_Gaseous.class) || type.hasTrait(FluidTraitSimple.FT_Gaseous_ART.class)) {

                double intensity = 1D - (double) this.tickCount / (double) this.getMaxAge();
                List<Entity> affected = level().getEntities(this.getThrower(), this.getBoundingBox().inflate(intensity * 2.5));

                for (Entity e : affected) {
                    if (e == this) continue;
                    this.affect(e, intensity);
                }
            }

        } else {

            Fluid fluid = getFluid();
            FluidType type = getFluidType();
            ChemicalStyle style = getStyle();
            Player me = null;
            for (Player p : level().players()) if (p.isLocalPlayer()) { me = p; break; }

            if (fluid == ModFluids.BALEFIRE.getSource()) {

                if (me != null && me.distanceTo(this) < 100)
                    FlameCreator.composeEffectClient(getX(), getY() - 0.125, getZ(), FlameCreator.META_BALEFIRE);

            } else if (style == ChemicalStyle.LIQUID) {

                int color = type.getColor();
                var motion = getDeltaMovement();

                CompoundTag data = new CompoundTag();
                data.putString("type", "vanillaExt");
                data.putString("mode", "colordust");
                data.putDouble("posX", getX());
                data.putDouble("posY", getY());
                data.putDouble("posZ", getZ());
                data.putDouble("mX", motion.x + random.nextGaussian() * 0.05);
                data.putDouble("mY", motion.y - 0.2 + random.nextGaussian() * 0.05);
                data.putDouble("mZ", motion.z + random.nextGaussian() * 0.05);
                data.putFloat("r", (color >> 16 & 255) / 255F);
                data.putFloat("g", (color >> 8 & 255) / 255F);
                data.putFloat("b", (color & 255) / 255F);
                com.hbm_m.particle.helper.ParticleEffectClient.effectNT(data);

            } else if (style == ChemicalStyle.BURNING) {

                if (me != null && me.distanceTo(this) < 100)
                    FlameCreator.composeEffectClient(getX(), getY() - 0.125, getZ(), FlameCreator.META_FIRE);
            }
        }
        super.tick();
    }

    /** {@code EntityDamageUtil.attackEntityFromIgnoreIFrame}. */
    private static void hurtIgnoreIFrame(Entity e, DamageSource source, float amount) {
        e.invulnerableTime = 0;
        e.hurt(source, amount);
    }

    protected void affect(Entity e, double intensity) {

        ChemicalStyle style = getStyle();
        Fluid fluid = getFluid();
        FluidType type = getFluidType();
        LivingEntity living = e instanceof LivingEntity l ? l : null;

        if (style == ChemicalStyle.LIQUID || style == ChemicalStyle.BURNING) //ignore range penalty for liquids
            intensity = 1D;

        if (style == ChemicalStyle.AMAT) {
            hurtIgnoreIFrame(e, ModDamageSources.radiation(level()), 1F);
            if (living != null) {
                ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.CREATIVE, 50F * (float) intensity);
                return;
            }
        }

        if (style == ChemicalStyle.LIGHTNING) {
            hurtIgnoreIFrame(e, ModDamageSources.electricity(level()), 0.5F);
            if (living != null) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 9));
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 9));
                return;
            }
        }

        if (type.temperature >= 100) {
            hurtIgnoreIFrame(e, getDamage(ModDamageTypes.BOIL), Math.min(0.25F + (type.temperature - 100) * 0.001F, 15F)); //.25 damage at 100°C with one extra damage every 1000°C

            if (type.temperature >= 500) {
                PlatformHooks.setSecondsOnFire(e, 10); //afterburn for 10 seconds
            }
        }

        if (style == ChemicalStyle.LIQUID || style == ChemicalStyle.GAS) {
            if (type.temperature < -20) {
                if (living != null) { //only living things are affected
                    hurtIgnoreIFrame(e, getDamage(ModDamageTypes.BOIL), Math.min(0.25F + (-type.temperature) * 0.01F, 2F));
                }
            }

            if (type.hasTrait(FluidTraitSimple.FT_Delicious.class)) {
                if (living != null && living.isAlive()) {
                    living.heal(2F * (float) intensity);
                }
            }
        }

        if (style == ChemicalStyle.LIQUID) {

            if (type.hasTrait(FT_Flammable.class)) {
                if (living != null) {
                    HbmLivingProps.setOil(living, 300); //doused in oil for 15 seconds
                }
            }
            if (type.hasTrait(FluidTraitSimple.FT_Delicious.class)) {
                if (living != null && living.isAlive()) {
                    living.heal(2F * (float) intensity);
                }
            }

        }

        if (this.isExtinguishing()) {
            e.clearFire();
        }

        if (style == ChemicalStyle.BURNING) {
            FT_Combustible trait = type.getTrait(FT_Combustible.class);
            hurtIgnoreIFrame(e, getDamage(ModDamageTypes.FLAMETHROWER), 0.2F + (trait != null ? (Math.min(trait.getCombustionEnergy() / 100_000F, 15F)) : 0));
            PlatformHooks.setSecondsOnFire(e, 5);
        }

        if (style == ChemicalStyle.GASFLAME) {
            FT_Flammable flammable = type.getTrait(FT_Flammable.class);
            FT_Combustible combustible = type.getTrait(FT_Combustible.class);

            float heat = Math.max(flammable != null ? flammable.getHeatEnergy() / 50_000F : 0, combustible != null ? Math.min(combustible.getCombustionEnergy() / 100_000F, 15F) : 0);
            heat *= intensity;
            hurtIgnoreIFrame(e, getDamage(ModDamageTypes.FLAMETHROWER), (0.2F + heat) * (float) intensity);
            PlatformHooks.setSecondsOnFire(e, (int) Math.ceil(5 * intensity));
        }

        if (type.hasTrait(FT_Corrosive.class)) {
            FT_Corrosive trait = type.getTrait(FT_Corrosive.class);

            if (living != null) {
                hurtIgnoreIFrame(living, getDamage(ModDamageTypes.ACID_PLAYER), trait.getRating() / 50F);
                for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD }) {
                    ArmorUtil.damageSuit(living, slot, trait.getRating() / 40);
                }
            }
        }

        if (type.hasTrait(FT_VentRadiation.class)) {
            FT_VentRadiation trait = type.getTrait(FT_VentRadiation.class);
            if (living != null) {
                ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.CREATIVE, trait.getRadPerMB() * 5);
            }
            ChunkRadiationManager.incrementRad(level(), Mth.floor(e.getX()), Mth.floor(e.getY()), Mth.floor(e.getZ()), trait.getRadPerMB() * 5);
        }

        if (type.hasTrait(FT_Poison.class)) {
            FT_Poison trait = type.getTrait(FT_Poison.class);

            if (living != null) {
                living.addEffect(new MobEffectInstance(trait.isWithering() ? MobEffects.WITHER : MobEffects.POISON, (int) (5 * 20 * intensity)));
            }
        }

        if (type.hasTrait(com.hbm_m.inventory.fluid.trait.FT_Toxin.class)) {
            com.hbm_m.inventory.fluid.trait.FT_Toxin trait = type.getTrait(com.hbm_m.inventory.fluid.trait.FT_Toxin.class);

            if (living != null) {
                trait.affect(living, intensity);
            }
        }

        if (type.hasTrait(FT_Pheromone.class)) {

            FT_Pheromone pheromone = type.getTrait(FT_Pheromone.class);

            if (living != null) {
                living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 2 * 60 * 20, 2));
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 5 * 60 * 20, 1));
                living.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 2 * 60 * 20, 4));

                if (living instanceof EntityGlyphid && pheromone.getType() == 1) {
                    living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 5 * 60 * 20, 4));
                    living.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60 * 20, 0));
                    living.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 60 * 20, 19));

                } else if (living instanceof Player && pheromone.getType() == 2) {
                    living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 2 * 60 * 20, 2));
                }
            }
        }

        if (fluid == ModFluids.XPJUICE.getSource()) {

            if (e instanceof Player player) {
                EnchantmentUtil.addExperience(player, 1, false);
                this.discard();
            }
        }

        if (fluid == ModFluids.ENDERJUICE.getSource()) {
            this.teleportRandomly(e);
        }
    }

    /* the extinguish type for burning multiblocks, roughly identical to the fire extinguisher */
    protected com.hbm_m.api.tile.IRepairable.EnumExtinguishType getExtinguishingType(Fluid fluid) {
        return fluid == ModFluids.CARBONDIOXIDE.getSource() ? com.hbm_m.api.tile.IRepairable.EnumExtinguishType.CO2
                : fluid == ModFluids.WATER.getSource() || fluid == ModFluids.HEAVYWATER.getSource() || fluid == ModFluids.COOLANT.getSource() ? com.hbm_m.api.tile.IRepairable.EnumExtinguishType.WATER : null;
    }

    /* whether this type should extinguish entities */
    protected boolean isExtinguishing() {
        return this.getStyle() == ChemicalStyle.LIQUID && this.getFluidType().temperature < 50 && !this.getFluidType().hasTrait(FT_Flammable.class);
    }

    //terribly copy-pasted from EntityEnderman.class
    public boolean teleportRandomly(Entity e) {
        double x = this.getX() + (this.random.nextDouble() - 0.5D) * 64.0D;
        double y = this.getY() + (this.random.nextInt(64) - 32);
        double z = this.getZ() + (this.random.nextDouble() - 0.5D) * 64.0D;
        return this.teleportTo(e, x, y, z);
    }

    public boolean teleportTo(Entity e, double x, double y, double z) {

        double targetX = e.getX();
        double targetY = e.getY();
        double targetZ = e.getZ();
        boolean flag = false;
        int i = Mth.floor(x);
        int j = Mth.floor(y);
        int k = Mth.floor(z);
        double py = y;

        if (e.level().hasChunkAt(new BlockPos(i, j, k))) {
            boolean flag1 = false;

            while (!flag1 && j > e.level().getMinBuildHeight()) {
                BlockState block = e.level().getBlockState(new BlockPos(i, j - 1, k));

                if (block.blocksMotion()) {
                    flag1 = true;
                } else {
                    --py;
                    --j;
                }
            }

            if (flag1) {
                e.setPos(x, py, z);

                if (e.level().noCollision(e) && !e.level().containsAnyLiquid(e.getBoundingBox())) {
                    flag = true;
                }
            }
        }

        if (!flag) {
            e.setPos(targetX, targetY, targetZ);
            return false;
        } else {
            short short1 = 128;

            for (int l = 0; l < short1; ++l) {
                double d6 = (double) l / ((double) short1 - 1.0D);
                float f = (this.random.nextFloat() - 0.5F) * 0.2F;
                float f1 = (this.random.nextFloat() - 0.5F) * 0.2F;
                float f2 = (this.random.nextFloat() - 0.5F) * 0.2F;
                double d7 = targetX + (e.getX() - targetX) * d6 + (this.random.nextDouble() - 0.5D) * e.getBbWidth() * 2.0D;
                double d8 = targetY + (e.getY() - targetY) * d6 + this.random.nextDouble() * e.getBbHeight();
                double d9 = targetZ + (e.getZ() - targetZ) * d6 + (this.random.nextDouble() - 0.5D) * e.getBbWidth() * 2.0D;
                e.level().addParticle(ParticleTypes.PORTAL, d7, d8, d9, f, f1, f2);
            }

            e.level().playSound(null, targetX, targetY, targetZ, SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.0F, 1.0F);
            e.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
            return true;
        }
    }

    @Override
    protected void onImpact(HitResult mop) {

        if (!level().isClientSide) {

            if (mop instanceof EntityHitResult ehr) {
                this.affect(ehr.getEntity(), 1D - (double) this.tickCount / (double) this.getMaxAge());
            }

            if (mop instanceof BlockHitResult bhr) {

                Fluid fluid = getFluid();
                FluidType type = getFluidType();
                BlockPos pos = bhr.getBlockPos();
                int x = pos.getX();
                int y = pos.getY();
                int z = pos.getZ();

                if (type.hasTrait(FT_VentRadiation.class)) {
                    FT_VentRadiation trait = type.getTrait(FT_VentRadiation.class);
                    ChunkRadiationManager.incrementRad(level(), x, y, z, trait.getRadPerMB() * 5);
                }

                ChemicalStyle style = getStyle();

                if (style == ChemicalStyle.BURNING || style == ChemicalStyle.GASFLAME) {

                    for (Direction dir : Direction.values()) {

                        Block fire = fluid == ModFluids.BALEFIRE.getSource() ? ModBlocks.BALEFIRE.get() : Blocks.FIRE;
                        BlockPos p = pos.relative(dir);

                        if (level().getBlockState(p).isAir()) {
                            level().setBlockAndUpdate(p, fire.defaultBlockState());
                        }
                    }
                }

                if (this.isExtinguishing()) {

                    for (Direction dir : Direction.values()) {
                        BlockPos p = pos.relative(dir);
                        if (level().getBlockState(p).is(Blocks.FIRE)) {
                            level().removeBlock(p, false);
                        }
                    }
                }

                com.hbm_m.api.tile.IRepairable.EnumExtinguishType fext = this.getExtinguishingType(fluid);
                if (fext != null) {
                    // Original CompatExternal.getCoreFromPos: Dummy-Zellen fuehren zum Kern
                    BlockPos corePos = pos;
                    if (level().getBlockEntity(pos) instanceof com.hbm_m.interfaces.IMultiblockPart part && part.getControllerPos() != null) corePos = part.getControllerPos();
                    if (level().getBlockEntity(corePos) instanceof com.hbm_m.api.tile.IRepairable repairable) {
                        repairable.tryExtinguish(level(), pos, fext);
                    }
                }

                if (fext == com.hbm_m.api.tile.IRepairable.EnumExtinguishType.WATER && style == ChemicalStyle.LIQUID) {
                    for (int i = -2; i <= 2; i++) {
                        for (int j = 0; j <= 1; j++) {
                            for (int k = -2; k <= 2; k++) {
                                BlockPos p = new BlockPos(x + i, y + j, z + k);
                                if (level().getBlockState(p).is(ModBlocks.NUCLEAR_FALLOUT.get())) {
                                    level().removeBlock(p, false);
                                }
                            }
                        }
                    }
                }

                if (fluid == ModFluids.SEEDSLURRY.getSource()) {

                    for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
                        BlockPos p = new BlockPos(x + i, y + j, z + k);
                        BlockState state = level().getBlockState(p);
                        Block block = state.getBlock();
                        if (block == Blocks.DIRT || block == ModBlocks.WASTE_EARTH.get() || block == ModBlocks.DIRT_DEAD.get() || block == ModBlocks.DIRT_OILY.get()) {

                            BlockPos above = p.above();
                            if (level().getMaxLocalRawBrightness(above) >= 9 && level().getBlockState(above).getLightBlock(level(), above) <= 2) {
                                level().setBlockAndUpdate(p, Blocks.GRASS_BLOCK.defaultBlockState());
                            }
                        }
                        if (block == Blocks.COBBLESTONE) level().setBlockAndUpdate(p, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
                        if (block == Blocks.STONE_BRICKS) level().setBlockAndUpdate(p, Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
                        if (block == ModBlocks.WASTE_EARTH.get()) level().setBlockAndUpdate(p, Blocks.GRASS_BLOCK.defaultBlockState());
                        if (block == ModBlocks.BRICK_CONCRETE.get()) level().setBlockAndUpdate(p, ModBlocks.BRICK_CONCRETE_MOSSY.get().defaultBlockState());
                        if (block == ModBlocks.BRICK_CONCRETE_SLAB.get()) level().setBlockAndUpdate(p, ModBlocks.BRICK_CONCRETE_MOSSY_SLAB.get().defaultBlockState()
                                .setValue(SlabBlock.TYPE, state.getValue(SlabBlock.TYPE)).setValue(SlabBlock.WATERLOGGED, state.getValue(SlabBlock.WATERLOGGED)));
                        if (block == ModBlocks.BRICK_CONCRETE_STAIRS.get()) level().setBlockAndUpdate(p, ModBlocks.BRICK_CONCRETE_MOSSY_STAIRS.get().defaultBlockState()
                                .setValue(StairBlock.FACING, state.getValue(StairBlock.FACING)).setValue(StairBlock.HALF, state.getValue(StairBlock.HALF))
                                .setValue(StairBlock.SHAPE, state.getValue(StairBlock.SHAPE)).setValue(StairBlock.WATERLOGGED, state.getValue(StairBlock.WATERLOGGED)));
                    }
                }

                this.discard();
            }
        }
    }

    @Override
    protected float getAirDrag() {

        ChemicalStyle type = getStyle();

        if (type == ChemicalStyle.AMAT) return 1F;
        if (type == ChemicalStyle.LIGHTNING) return 1F;
        if (type == ChemicalStyle.GAS) return 0.95F;

        return 0.99F;
    }

    @Override
    protected float getWaterDrag() {

        ChemicalStyle type = getStyle();

        if (type == ChemicalStyle.AMAT) return 1F;
        if (type == ChemicalStyle.LIGHTNING) return 1F;
        if (type == ChemicalStyle.GAS) return 1F;

        return 0.8F;
    }

    public int getMaxAge() {

        switch (this.getStyle()) {
            case AMAT: return 100;
            case LIGHTNING: return 5;
            case BURNING: return 600;
            case GAS: return 60;
            case GASFLAME: return 20;
            case LIQUID: return 600;
            default: return 100;
        }
    }

    @Override
    public double getGravityVelocity() {

        ChemicalStyle type = getStyle();

        if (type == ChemicalStyle.AMAT) return 0D;
        if (type == ChemicalStyle.LIGHTNING) return 0D;
        if (type == ChemicalStyle.GAS) return 0D;
        if (type == ChemicalStyle.GASFLAME) return -0.01D;

        return 0.03D;
    }

    public ChemicalStyle getStyle() {
        return getStyleFromType(this.getFluid());
    }

    public static ChemicalStyle getStyleFromType(Fluid fluid) {

        FluidType type = FluidType.forFluid(fluid);

        if (fluid == ModFluids.IONGEL.getSource()) {
            return ChemicalStyle.LIGHTNING;
        }

        if (type.isAntimatter()) {
            return ChemicalStyle.AMAT;
        }

        if (type.hasTrait(FluidTraitSimple.FT_Gaseous.class) || type.hasTrait(FluidTraitSimple.FT_Gaseous_ART.class)) {

            if (type.hasTrait(FT_Flammable.class) || type.hasTrait(FT_Combustible.class)) {
                return ChemicalStyle.GASFLAME;
            } else {
                return ChemicalStyle.GAS;
            }
        }

        if (type.hasTrait(FluidTraitSimple.FT_Liquid.class)) {

            if (type.hasTrait(FT_Combustible.class)) {
                return ChemicalStyle.BURNING;
            } else {
                return ChemicalStyle.LIQUID;
            }
        }

        return ChemicalStyle.NULL;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putString("fluid", this.entityData.get(FLUID));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.entityData.set(FLUID, nbt.getString("fluid"));
    }

    /**
     * The general type of the chemical, determines rendering and movement
     */
    public enum ChemicalStyle {
        AMAT,       //renders as beam
        LIGHTNING,  //renders as beam
        LIQUID,     //no renderer, fluid particles
        GAS,        //renders as particles
        GASFLAME,   //renders as fire particles
        BURNING,    //no renderer, fire particles
        NULL
    }
}
