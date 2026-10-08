package com.hbm_m.item.weapon.sedna.factory;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import com.hbm_m.entity.effect.EntityFireLingering;
import com.hbm_m.entity.projectile.EntityBulletBeamBase;
import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm_m.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.item.weapon.sedna.BulletConfig;
import com.hbm_m.item.weapon.sedna.Crosshair;
import com.hbm_m.item.weapon.sedna.GunConfig;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm_m.item.weapon.sedna.Receiver;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm_m.item.weapon.sedna.mags.MagazineBelt;
import com.hbm_m.item.weapon.sedna.mags.MagazineFullReload;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.powerarmor.resist.DamageResistanceHandler.DamageClass;
import com.hbm_m.render.anim.AnimationEnums.GunAnimation;
import com.hbm_m.render.anim.BusAnimation;
import com.hbm_m.render.anim.BusAnimationKeyframe.IType;
import com.hbm_m.render.anim.BusAnimationSequence;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.ForgeDirection;
import com.hbm_m.util.MovingObjectPosition;
import com.hbm_m.util.MovingObjectPosition.MovingObjectType;
import com.hbm_m.util.Vec3NT;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** 1:1 {@code XFactoryEnergy}: Tesla-Kanone, Laserpistolen und Lasergewehr. */
public class XFactoryEnergy {

    public static final ResourceLocation scope_luna = ResourceLocation.fromNamespaceAndPath("hbm_m", "textures/misc/scope_amat.png");

    public static BulletConfig energy_tesla;
    public static BulletConfig energy_tesla_overcharge;
    public static BulletConfig energy_tesla_ir;
    public static BulletConfig energy_tesla_ir_sub;

    public static BulletConfig energy_las;
    public static BulletConfig energy_las_overcharge;
    public static BulletConfig energy_las_ir;
    public static BulletConfig energy_emerald;
    public static BulletConfig energy_emerald_overcharge;
    public static BulletConfig energy_emerald_ir;

    /** Original {@code world.playSoundEffect(x, y, z, key, vol, pitch)}. */
    static void playSound(Level world, double x, double y, double z, SoundEvent ev, float volume, float pitch) {
        if (ev != null) world.playSound(null, x, y, z, ev, SoundSource.PLAYERS, volume, pitch);
    }

    public static BiConsumer<EntityBulletBeamBase, MovingObjectPosition> LAMBDA_LIGHTNING_HIT = (beam, mop) -> {

        if (mop.typeOfHit == MovingObjectType.BLOCK) {
            ForgeDirection dir = ForgeDirection.getOrientation(mop.sideHit);
            mop.hitVec.xCoord += dir.offsetX * 0.5;
            mop.hitVec.yCoord += dir.offsetY * 0.5;
            mop.hitVec.zCoord += dir.offsetZ * 0.5;
        }

        ExplosionVNT vnt = new ExplosionVNT(beam.level(), mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord, 2F, beam.getThrower());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, beam.damage).setDamageClass(beam.config.dmgClass));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.explode();
        playSound(beam.level(), mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord, HbmSoundsNT.get("hbm:entity.ufoBlast"), 5.0F, 0.9F + beam.level().random.nextFloat() * 0.2F);
        playSound(beam.level(), mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord, SoundEvents.FIREWORK_ROCKET_BLAST, 5.0F, 0.5F);

        float yaw = beam.level().random.nextFloat() * 180F;
        for (int i = 0; i < 3; i++) {
            CompoundTag data = new CompoundTag();
            data.putString("type", "plasmablast");
            data.putFloat("r", 0.5F);
            data.putFloat("g", 0.5F);
            data.putFloat("b", 1.0F);
            data.putFloat("pitch", -60F + 60F * i);
            data.putFloat("yaw", yaw);
            data.putFloat("scale", 2F);
            if (beam.level() instanceof ServerLevel sl) IParticleCreator.sendPacket(sl, mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord, 100, data);
        }

        if (mop.typeOfHit == MovingObjectType.ENTITY) {
            if (mop.entityHit instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 9));
                living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 9));
            }
        }
    };

    public static BiConsumer<EntityBulletBeamBase, MovingObjectPosition> LAMBDA_LIGHTNING_SPLIT = (beam, mop) -> {
        LAMBDA_LIGHTNING_HIT.accept(beam, mop);
        if (mop.typeOfHit != MovingObjectType.ENTITY) return;

        double range = 20;
        List<LivingEntity> potentialTargets = new java.util.ArrayList<>(beam.level().getEntitiesOfClass(LivingEntity.class, new AABB(mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord, mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord).inflate(range, range, range)));
        Collections.shuffle(potentialTargets);

        for (LivingEntity target : potentialTargets) {
            if (target == beam.thrower) continue;
            if (target == mop.entityHit) continue;

            Vec3NT delta = Vec3NT.createVectorHelper(target.getX() - mop.hitVec.xCoord, target.getY() + target.getBbHeight() / 2 - mop.hitVec.yCoord, target.getZ() - mop.hitVec.zCoord);
            if (delta.lengthVector() > 20) continue;
            EntityBulletBeamBase sub = new EntityBulletBeamBase(beam.thrower, energy_tesla_ir_sub, beam.damage);
            sub.setPos(mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord);
            sub.setRotationsFromVector(delta);
            sub.performHitscanExternal(delta.lengthVector());
            beam.level().addFreshEntity(sub);
        }
    };

    public static BiConsumer<EntityBulletBeamBase, MovingObjectPosition> LAMBDA_IR_HIT = (beam, mop) -> {
        BulletConfig.LAMBDA_STANDARD_BEAM_HIT.accept(beam, mop);

        if (mop.typeOfHit == MovingObjectType.ENTITY) {
            if (mop.entityHit instanceof LivingEntity living) {
                if (HbmLivingProps.getFire(living) < 100) HbmLivingProps.setFire(living, 100);
            }
        }

        if (mop.typeOfHit == MovingObjectType.BLOCK) {
            Level world = beam.level();
            BlockPos pos = mop.getBlockPos();
            BlockState b = world.getBlockState(pos);
            ForgeDirection dir = ForgeDirection.getOrientation(mop.sideHit);
            if (b.isFlammable(world, pos, dir.getOpposite().toDirection())) {
                BlockPos side = pos.offset(dir.offsetX, dir.offsetY, dir.offsetZ);
                if (world.getBlockState(side).isAir()) {
                    world.setBlockAndUpdate(side, Blocks.FIRE.defaultBlockState());
                    return;
                }
            }

            EntityFireLingering fire = new EntityFireLingering(beam.level()).setArea(2, 1).setDuration(100).setType(EntityFireLingering.TYPE_DIESEL);
            fire.setPosition(mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord);
            beam.level().addFreshEntity(fire);
        }
    };

    private static ItemStack polymer(int n) {
        return new ItemStack(ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.INGOT), n);
    }

    public static void init() {

        energy_tesla = new BulletConfig().setItem(EnumAmmo.CAPACITOR).setCasing(polymer(2), 4).setupDamageClass(DamageClass.ELECTRIC).setBeam().setSpread(0.0F).setLife(5).setRenderRotations(false).setDoesPenetrate(true)
                .setOnBeamImpact(LAMBDA_LIGHTNING_HIT);
        energy_tesla_overcharge = new BulletConfig().setItem(EnumAmmo.CAPACITOR_OVERCHARGE).setCasing(polymer(2), 4).setupDamageClass(DamageClass.ELECTRIC).setBeam().setSpread(0.0F).setLife(5).setRenderRotations(false).setDoesPenetrate(true)
                .setDamage(1.5F).setOnBeamImpact(LAMBDA_LIGHTNING_HIT);
        energy_tesla_ir = new BulletConfig().setItem(EnumAmmo.CAPACITOR_IR).setCasing(polymer(2), 4).setupDamageClass(DamageClass.ELECTRIC).setBeam().setSpread(0.0F).setLife(5).setRenderRotations(false)
                .setDamage(0.8F).setOnBeamImpact(LAMBDA_LIGHTNING_SPLIT);
        energy_tesla_ir_sub = new BulletConfig().setItem(EnumAmmo.CAPACITOR_IR).setupDamageClass(DamageClass.ELECTRIC).setBeam().setSpread(0.0F).setLife(3).setWear(3F).setRenderRotations(false).setDoesPenetrate(true)
                .setDamage(0.5F).setOnBeamImpact(BulletConfig.LAMBDA_STANDARD_BEAM_HIT);

        energy_las = new BulletConfig().setItem(EnumAmmo.CAPACITOR).setCasing(polymer(2), 4).setupDamageClass(DamageClass.LASER).setBeam().setSpread(0.0F).setLife(5).setRenderRotations(false).setOnBeamImpact(BulletConfig.LAMBDA_STANDARD_BEAM_HIT);
        energy_las_overcharge = new BulletConfig().setItem(EnumAmmo.CAPACITOR_OVERCHARGE).setCasing(polymer(2), 4).setupDamageClass(DamageClass.LASER).setBeam().setSpread(0.0F).setLife(5).setRenderRotations(false).setDoesPenetrate(true).setOnBeamImpact(BulletConfig.LAMBDA_STANDARD_BEAM_HIT);
        energy_las_ir = new BulletConfig().setItem(EnumAmmo.CAPACITOR_IR).setCasing(polymer(2), 4).setupDamageClass(DamageClass.FIRE).setBeam().setSpread(0.0F).setLife(5).setRenderRotations(false).setOnBeamImpact(LAMBDA_IR_HIT);

        energy_emerald = energy_las.clone().setArmorPiercing(0.5F).setThresholdNegation(10F);
        energy_emerald_overcharge = energy_las_overcharge.clone().setArmorPiercing(0.5F).setThresholdNegation(15F);
        energy_emerald_ir = energy_las_ir.clone().setArmorPiercing(0.5F).setThresholdNegation(10F);

        GunFactory.reg("gun_tesla_cannon", new ItemGunBaseNT(WeaponQuality.A_SIDE, new GunConfig()
                .dura(1_000).draw(10).inspect(33).crosshair(Crosshair.CIRCLE)
                .rec(new Receiver(0)
                        .dmg(35F).delay(20).spreadHipfire(1.5F).reload(44).jam(19).sound("hbm:weapon.fire.tesla", 1.0F, 1.0F)
                        .mag(new MagazineBelt().addConfigs(energy_tesla, energy_tesla_overcharge, energy_tesla_ir))
                        .offset(0.75, 0, -0.375).offsetScoped(0.75, 0, -0.25)
                        .setupStandardFire().recoil(LAMBDA_RECOIL_ENERGY))
                .setupStandardConfiguration()
                .anim(LAMBDA_TESLA_ANIMS).orchestra(Orchestras.ORCHESTRA_TESLA)
                ).setDefaultAmmo(EnumAmmo.CAPACITOR, 15));

        GunFactory.reg("gun_laser_pistol", new ItemGunBaseNT(WeaponQuality.A_SIDE, new GunConfig()
                .dura(500).draw(10).inspect(26).crosshair(Crosshair.CIRCLE)
                .rec(new Receiver(0)
                        .dmg(25F).delay(5).spread(1F).spreadHipfire(1F).reload(45).jam(37).sound("hbm:weapon.fire.laserPistol", 1.0F, 1.0F)
                        .mag(new MagazineFullReload(0, 30).addConfigs(energy_las, energy_las_overcharge, energy_las_ir))
                        .offset(0.75, -0.0625 * 1.5, -0.1875)
                        .setupStandardFire().recoil(LAMBDA_RECOIL_ENERGY))
                .setupStandardConfiguration()
                .anim(LAMBDA_LASER_PISTOL).orchestra(Orchestras.ORCHESTRA_LASER_PISTOL)
                ).setDefaultAmmo(EnumAmmo.CAPACITOR, 15));
        GunFactory.reg("gun_laser_pistol_pew_pew", new ItemGunBaseNT(WeaponQuality.B_SIDE, new GunConfig()
                .dura(500).draw(10).inspect(26).crosshair(Crosshair.CIRCLE)
                .rec(new Receiver(0)
                        .dmg(30F).rounds(5).delay(10).spread(0.25F).spreadHipfire(1F).reload(45).jam(37).sound("hbm:weapon.fire.laserPistol", 1.0F, 0.8F)
                        .mag(new MagazineFullReload(0, 10).addConfigs(energy_las, energy_las_overcharge, energy_las_ir))
                        .offset(0.75, -0.0625 * 1.5, -0.1875)
                        .setupStandardFire().recoil(LAMBDA_RECOIL_ENERGY))
                .setupStandardConfiguration()
                .anim(LAMBDA_LASER_PISTOL).orchestra(Orchestras.ORCHESTRA_LASER_PISTOL)
                ).setDefaultAmmo(EnumAmmo.CAPACITOR_OVERCHARGE, 10));
        GunFactory.reg("gun_laser_pistol_morning_glory", new ItemGunBaseNT(WeaponQuality.LEGENDARY, new GunConfig()
                .dura(1_500).draw(10).inspect(26).crosshair(Crosshair.CIRCLE)
                .rec(new Receiver(0)
                        .dmg(20F).delay(7).spread(0F).spreadHipfire(0.5F).reload(45).jam(37).sound("hbm:weapon.fire.laserPistol", 1.0F, 1.1F)
                        .mag(new MagazineFullReload(0, 20).addConfigs(energy_emerald, energy_emerald_overcharge, energy_emerald_ir))
                        .offset(0.75, -0.0625 * 1.5, -0.1875)
                        .setupStandardFire().recoil(LAMBDA_RECOIL_ENERGY))
                .setupStandardConfiguration()
                .anim(LAMBDA_LASER_PISTOL).orchestra(Orchestras.ORCHESTRA_LASER_PISTOL)
                ).setDefaultAmmo(EnumAmmo.CAPACITOR_OVERCHARGE, 20));

        GunFactory.reg("gun_lasrifle", new ItemGunBaseNT(WeaponQuality.A_SIDE, new GunConfig()
                .dura(2_000).draw(10).inspect(26).crosshair(Crosshair.CIRCLE).scopeTexture(scope_luna)
                .rec(new Receiver(0)
                        .dmg(50F).delay(8).spreadHipfire(1F).reload(44).jam(36).sound("hbm:weapon.fire.laser", 1.0F, 1.0F)
                        .mag(new MagazineFullReload(0, 24).addConfigs(energy_las, energy_las_overcharge, energy_las_ir))
                        .offset(0.75, -0.0625 * 1.5, -0.1875)
                        .setupStandardFire().recoil(LAMBDA_RECOIL_ENERGY))
                .setupStandardConfiguration()
                .anim(LAMBDA_LASRIFLE).orchestra(Orchestras.ORCHESTRA_LASRIFLE)
                ).setDefaultAmmo(EnumAmmo.CAPACITOR, 24));
    }

    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_ENERGY = (stack, ctx) -> { };

    @SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_TESLA_ANIMS = (stack, type) -> {
        Player me = com.hbm_m.client.weapon.GunClientHooks.clientPlayer();
        int amount = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, me != null ? me.getInventory() : null);
        switch (type) {
            case EQUIP: return new BusAnimation()
                    .addBus("EQUIP", new BusAnimationSequence().addPos(60, 0, 0, 0).addPos(0, 0, 0, 1000, IType.SIN_DOWN));
            case CYCLE: return new BusAnimation()
                    .addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, ItemGunBaseNT.getIsAiming(stack) ? -0.5 : -1, 100, IType.SIN_DOWN).addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("CYCLE", new BusAnimationSequence().addPos(0, 0, 0, 150).addPos(0, 0, 22.5, 350))
                    .addBus("COUNT", new BusAnimationSequence().addPos(amount, 0, 0, 0));
            case CYCLE_DRY: return new BusAnimation()
                    .addBus("CYCLE", new BusAnimationSequence().addPos(0, 0, 0, 150).addPos(0, 0, 22.5, 350));
            case INSPECT: return new BusAnimation()
                    .addBus("YOMI", new BusAnimationSequence().addPos(8, -4, 0, 0).addPos(4, -1, 0, 500, IType.SIN_DOWN).addPos(4, -1, 0, 1000).addPos(6, -6, 0, 500, IType.SIN_UP))
                    .addBus("SQUEEZE", new BusAnimationSequence().addPos(1, 1, 1, 0).addPos(1, 1, 1, 750).addPos(1, 1, 0.5, 125).addPos(1, 1, 1, 125));
        }

        return null;
    };

    @SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_LASER_PISTOL = (stack, type) -> {
        switch (type) {
            case EQUIP: return new BusAnimation()
                    .addBus("EQUIP", new BusAnimationSequence().addPos(60, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
            case CYCLE: return new BusAnimation()
                    .addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, -0.5, 50, IType.SIN_DOWN).addPos(0, 0, 0, 150, IType.SIN_FULL));
            case RELOAD: return new BusAnimation()
                    .addBus("LATCH", new BusAnimationSequence().addPos(0, -20, 0, 100).hold(1900).addPos(0, 0, 0, 100))
                    .addBus("LIFT", new BusAnimationSequence().hold(100).addPos(-45, 0, 0, 250, IType.SIN_FULL).hold(500).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("JOLT", new BusAnimationSequence().hold(350).addPos(0, 0, 0.5, 100, IType.SIN_FULL).addPos(0, 0, -1.5, 100, IType.SIN_UP).addPos(0, 0, 0, 150, IType.SIN_FULL).holdUntil(2100).addPos(-0.0625, 0, 0, 50, IType.SIN_UP).addPos(0, 0, 0, 100, IType.SIN_FULL))
                    .addBus("BATTERY", new BusAnimationSequence().hold(550).addPos(0, 0, 5, 250).hold(550).setPos(0, -2, -2).addPos(0, 0, -2, 250, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_UP));
            case JAMMED: return new BusAnimation()
                    .addBus("LATCH", new BusAnimationSequence().hold(500).addPos(0, -20, 0, 100).hold(250).addPos(0, 0, 0, 100))
                    .addBus("JOLT", new BusAnimationSequence().hold(950).addPos(-0.0625, 0, 0, 50, IType.SIN_UP).addPos(0, 0, 0, 100, IType.SIN_FULL))
                    .addBus("EQUIP", new BusAnimationSequence().hold(1500).addPos(7.5, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 250, IType.SIN_FULL));
            case INSPECT: return new BusAnimation()
                    .addBus("SWIRL", new BusAnimationSequence().addPos(-720, 0, 0, 750, IType.SIN_FULL).hold(500).addPos(0, 0, 0, 750, IType.SIN_FULL));
        }
        return null;
    };

    @SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_LASRIFLE = (stack, type) -> {
        switch (type) {
            case EQUIP: return new BusAnimation()
                    .addBus("EQUIP", new BusAnimationSequence().addPos(60, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
            case CYCLE: return new BusAnimation()
                    .addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, -0.5, 50, IType.SIN_DOWN).addPos(0, 0, 0, 150, IType.SIN_FULL));
            case RELOAD: return new BusAnimation()
                    .addBus("LEVER", new BusAnimationSequence().addPos(-90, 0, 0, 350, IType.SIN_UP).addPos(-90, 0, 0, 1500).addPos(0, 0, 0, 350, IType.SIN_UP))
                    .addBus("MAG", new BusAnimationSequence().addPos(0, 0, 0, 350).addPos(0, -5, 0, 350, IType.SIN_UP).addPos(0, -5, 0, 500).addPos(0, -0.25, 0, 500, IType.SIN_FULL).addPos(0, -0.25, 0, 150).addPos(0, 0, 0, 350))
                    .addBus("EQUIP", new BusAnimationSequence().addPos(0, 0, 0, 1700).addPos(-2, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL));
            case JAMMED: return new BusAnimation()
                    .addBus("LEVER", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(-90, 0, 0, 350, IType.SIN_UP).addPos(-90, 0, 0, 600).addPos(0, 0, 0, 350, IType.SIN_UP))
                    .addBus("MAG", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(0, 0, 0, 350).addPos(0, -2, 0, 200, IType.SIN_UP).addPos(0, -0.25, 0, 250, IType.SIN_FULL).addPos(0, -0.25, 0, 150).addPos(0, 0, 0, 350))
                    .addBus("EQUIP", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(0, 0, 0, 800).addPos(-2, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL));
            case INSPECT: return new BusAnimation()
                    .addBus("LEVER", new BusAnimationSequence().addPos(-90, 0, 0, 350, IType.SIN_UP).addPos(-90, 0, 0, 600).addPos(0, 0, 0, 350, IType.SIN_UP))
                    .addBus("MAG", new BusAnimationSequence().addPos(0, 0, 0, 350).addPos(0, -2, 0, 200, IType.SIN_UP).addPos(0, -0.25, 0, 250, IType.SIN_FULL).addPos(0, -0.25, 0, 150).addPos(0, 0, 0, 350))
                    .addBus("EQUIP", new BusAnimationSequence().addPos(0, 0, 0, 800).addPos(-2, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL));
        }

        return null;
    };
}
