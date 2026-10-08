package com.hbm_m.item.weapon.grenade;

import static com.hbm_m.item.weapon.grenade.ItemGrenadeShell.EnumGrenadeShell.FRAG;
import static com.hbm_m.item.weapon.grenade.ItemGrenadeShell.EnumGrenadeShell.NUKE;
import static com.hbm_m.item.weapon.grenade.ItemGrenadeShell.EnumGrenadeShell.STICK;
import static com.hbm_m.item.weapon.grenade.ItemGrenadeShell.EnumGrenadeShell.TECH;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.effect.EntityCloudFleija;
import com.hbm_m.entity.effect.EntityFireLingering;
import com.hbm_m.entity.grenade.EntityGrenadeUniversal;
import com.hbm_m.entity.logic.EntityNukeExplosionMK3;
import com.hbm_m.entity.projectile.EntityBulletBaseMK4;
import com.hbm_m.entity.projectile.EntityBulletBeamBase;
import com.hbm_m.explosion.ExplosionNukeGeneric;
import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm_m.explosion.vanillant.standard.BlockMutatorFire;
import com.hbm_m.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm_m.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm_m.explosion.vanillant.standard.ExplosionEffectTiny;
import com.hbm_m.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm_m.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm_m.item.weapon.grenade.ItemGrenadeShell.EnumGrenadeShell;
import com.hbm_m.item.weapon.sedna.BulletConfig;
import com.hbm_m.item.weapon.sedna.factory.Lego;
import com.hbm_m.main.Polaroid;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.powerarmor.resist.DamageResistanceHandler.DamageClass;
import com.hbm_m.radiation.ChunkRadiationManager;
import com.hbm_m.satellite.DetectorEvents;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.MovingObjectPosition;
import com.hbm_m.util.Vec3NT;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code ItemGrenadeFilling}: jede Original-Meta ist ein eigener Gegenstand {@code grenade_filling_<typ>}.
 * Die Geschosskonfigurationen entstehen im Original im Item-Konstruktor (vor {@code GunFactory.init()}); im Port in
 * {@link #initConfigs()}, das {@code GunFactory.init()} als Erstes aufruft (gleiche ID-Reihenfolge).
 */
public class ItemGrenadeFilling extends Item {

    public static BulletConfig fragmentation;
    public static BulletConfig pellets;
    public static BulletConfig pellets_heavy;
    public static BulletConfig laser;

    public final EnumGrenadeFilling type;

    public ItemGrenadeFilling(EnumGrenadeFilling type, Properties props) {
        super(props);
        this.type = type;
    }

    private static boolean configsDone = false;

    public static void initConfigs() {
        if (configsDone) return;
        configsDone = true;
        fragmentation = new BulletConfig().setLife(3).setThresholdNegation(5F).setRicochetAngle(90).setRicochetCount(2);
        pellets = new BulletConfig().setLife(100).setGrav(0.04).setVel(1.5F).setOnImpact(LAMBDA_TINY_EXPLODE);
        pellets_heavy = new BulletConfig().setLife(100).setGrav(0.04).setVel(1.5F).setOnImpact(LAMBDA_EXPLODE);
        laser = new BulletConfig().setBeam().setupDamageClass(DamageClass.LASER).setLife(3).setRenderRotations(false).setThresholdNegation(10F).setOnBeamImpact(BulletConfig.LAMBDA_STANDARD_BEAM_HIT);
    }

    public static enum EnumGrenadeFilling {
        POWDER(L.EXPLODE_POWDER,                0x424242, 0x939176, FRAG, STICK),   // gunpowder
        HE(L.EXPLODE_HE,                        0x595533, 0xA49D62, FRAG, STICK),   // high explosive
        DEMO(L.EXPLODE_DEMO,                    0x595533, 0xDD4029, FRAG, STICK),   // demolition
        INC(L.EXPLODE_INC,                      0x5A5A5A, 0xFF5F21, FRAG, STICK),   // incendiary
        WP(L.EXPLODE_WP,                        0xDCDCDC, 0xFF5F21, FRAG, STICK),   // white phosphorus
        CLUSTER(L.EXPLODE_CLUSTER,              0x5A5A5A, 0xFFC711, FRAG, STICK),   // explosive pellets
        EMP(L.EXPLODE_EMP,                      0x93A1AC, 0x00FFFF, TECH),          // tesla
        PLASMA(L.EXPLODE_PLASMA,                0x655B2C, 0x4CFF00, TECH),          // EMP but more oomph
        LASER(L.EXPLODE_LASER,                  0x493A3A, 0xFF0000, TECH),          // pew pew pew
        CLUSTER_HEAVY(L.EXPLODE_CLUSTER_HEAVY,  0x5A5A5A, 0xFF5F21, NUKE),          // cluster but fat
        NUCLEAR(L.EXPLODE_NUKE,                 0xDFD7A8, 0xA49D62, NUKE),          // nuka grenade
        NUCLEAR_DEMO(L.EXPLODE_NUKE_DEMO,       0xDFD7A8, 0xDD4029, NUKE),          // demolition nuka grenade
        SCHRAB(L.EXPLODE_SCHRAB,                0x00BDBD, 0x000000, NUKE);          // what used to be aschrab

        public Consumer<EntityGrenadeUniversal> explode;
        public Set<EnumGrenadeShell> compatibleShells = new HashSet<>();
        public int bodyColor;
        public int labelColor;

        private EnumGrenadeFilling(Consumer<EntityGrenadeUniversal> explode, int bodyColor, int labelColor, EnumGrenadeShell... compatibleShells) {
            this.explode = explode;
            for (EnumGrenadeShell shell : compatibleShells) this.compatibleShells.add(shell);
            this.bodyColor = bodyColor;
            this.labelColor = labelColor;
        }
    }

    public static Consumer<EntityGrenadeUniversal> EXPLODE_POWDER = L.EXPLODE_POWDER;
    public static Consumer<EntityGrenadeUniversal> EXPLODE_HE = L.EXPLODE_HE;
    public static Consumer<EntityGrenadeUniversal> EXPLODE_CLUSTER = L.EXPLODE_CLUSTER;
    public static Consumer<EntityGrenadeUniversal> EXPLODE_CLUSTER_HEAVY = L.EXPLODE_CLUSTER_HEAVY;
    public static Consumer<EntityGrenadeUniversal> EXPLODE_DEMO = L.EXPLODE_DEMO;
    public static Consumer<EntityGrenadeUniversal> EXPLODE_INC = L.EXPLODE_INC;
    public static Consumer<EntityGrenadeUniversal> EXPLODE_WP = L.EXPLODE_WP;
    public static Consumer<EntityGrenadeUniversal> EXPLODE_EMP = L.EXPLODE_EMP;
    public static Consumer<EntityGrenadeUniversal> EXPLODE_PLASMA = L.EXPLODE_PLASMA;
    public static Consumer<EntityGrenadeUniversal> EXPLODE_LASER = L.EXPLODE_LASER;
    public static Consumer<EntityGrenadeUniversal> EXPLODE_NUKE = L.EXPLODE_NUKE;
    public static Consumer<EntityGrenadeUniversal> EXPLODE_NUKE_DEMO = L.EXPLODE_NUKE_DEMO;
    public static Consumer<EntityGrenadeUniversal> EXPLODE_SCHRAB = L.EXPLODE_SCHRAB;

    /** Lambdas in einer Halterklasse, damit sie vor den Enum-Konstanten stehen. */
    static final class L {

        static final Consumer<EntityGrenadeUniversal> EXPLODE_POWDER = (grenade) -> { standardExplode(grenade, 5F, 10F, 5F, 0F); };

        static final Consumer<EntityGrenadeUniversal> EXPLODE_HE = (grenade) -> { standardExplode(grenade, 7.5F, 25F, 10F, 0.1F); };

        static final Consumer<EntityGrenadeUniversal> EXPLODE_CLUSTER = (grenade) -> {
            standardExplode(grenade, 7.5F, 15F, 10F, 0.1F);
            int frags = 30;
            if (grenade.getShell() == EnumGrenadeShell.FRAG) frags *= 1.25;
            Level level = grenade.level();
            for (int i = 0; i < frags; i++) {
                EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(level, pellets, 15F, 0F, level.random.nextFloat() * 2F * (float) Math.PI, (level.random.nextFloat() * 0.5F + 0.5F) * (float) Math.PI);
                bullet.setPosition(grenade.getX(), grenade.getY() + 0.05, grenade.getZ());
                var m = bullet.getDeltaMovement();
                bullet.setDeltaMovement(m.x * 0.5, m.y * 0.75, m.z * 0.5);
                level.addFreshEntity(bullet);
            }
        };

        static final Consumer<EntityGrenadeUniversal> EXPLODE_CLUSTER_HEAVY = (grenade) -> {
            standardExplode(grenade, 7.5F, 15F, 10F, 0.1F);
            int frags = 15;
            Level level = grenade.level();
            for (int i = 0; i < frags; i++) {
                EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(level, pellets_heavy, 30F, 0F, level.random.nextFloat() * 2F * (float) Math.PI, (level.random.nextFloat() * 0.5F + 0.5F) * (float) Math.PI);
                bullet.setPosition(grenade.getX(), grenade.getY() + 0.05, grenade.getZ());
                var m = bullet.getDeltaMovement();
                bullet.setDeltaMovement(m.x * 0.5, m.y * 1.25, m.z * 0.5);
                level.addFreshEntity(bullet);
            }
        };

        static final Consumer<EntityGrenadeUniversal> EXPLODE_DEMO = (grenade) -> {
            ExplosionVNT vnt = new ExplosionVNT(grenade.level(), grenade.getX(), grenade.getY(), grenade.getZ(), 5F, grenade.getThrower());
            vnt.setBlockAllocator(new BlockAllocatorStandard());
            vnt.setBlockProcessor(new BlockProcessorStandard());
            vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, 10F));
            vnt.setPlayerProcessor(new PlayerProcessorStandard());
            vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
            vnt.explode();
        };

        static final Consumer<EntityGrenadeUniversal> EXPLODE_INC = (grenade) -> {
            Level world = grenade.level();
            standardExplode(grenade, 3F, 10F);
            EntityFireLingering fire = new EntityFireLingering(world).setArea(6, 2).setDuration(200).setType(EntityFireLingering.TYPE_DIESEL);
            fire.setPosition(grenade.getX(), grenade.getY(), grenade.getZ());
            world.addFreshEntity(fire);
            igniteAround(grenade, 2);
        };

        static final Consumer<EntityGrenadeUniversal> EXPLODE_WP = (grenade) -> {
            Level world = grenade.level();
            standardExplode(grenade, 3F, 10F);
            EntityFireLingering fire = new EntityFireLingering(world).setArea(6, 2).setDuration(600).setType(EntityFireLingering.TYPE_PHOSPHORUS);
            fire.setPosition(grenade.getX(), grenade.getY(), grenade.getZ());
            world.addFreshEntity(fire);
            igniteAround(grenade, 3);
            if (world instanceof ServerLevel server) for (int i = 0; i < 3; i++) {
                CompoundTag haze = new CompoundTag();
                haze.putString("type", "haze");
                IParticleCreator.sendPacket(server, grenade.getX() + world.random.nextGaussian() * 4, grenade.getY(), grenade.getZ() + world.random.nextGaussian() * 4, 150, haze);
            }
        };

        static final Consumer<EntityGrenadeUniversal> EXPLODE_EMP = (grenade) -> {
            explodeStandardEnergy(grenade, 15F, 3F, DamageClass.ELECTRIC, 0.5F, 0.5F, 1F, 3F);
            ExplosionNukeGeneric.empBlast(grenade.level(), (int) Math.floor(grenade.getX()), (int) Math.floor(grenade.getY()), (int) Math.floor(grenade.getZ()), 5);
        };

        static final Consumer<EntityGrenadeUniversal> EXPLODE_PLASMA = (grenade) -> {
            explodeStandardEnergy(grenade, 50F, 5F, DamageClass.PLASMA, 0.5F, 1F, 0.5F, 4F); // TODO: unique effect because this sucks
        };

        static final Consumer<EntityGrenadeUniversal> EXPLODE_LASER = (grenade) -> { // yeah this is good, we like this one
            tinyExplode(grenade, 2, 5F);

            double x = grenade.getX();
            double y = grenade.getY() + 0.125;
            double z = grenade.getZ();

            double range = 15;
            List<LivingEntity> potentialTargets = grenade.level().getEntitiesOfClass(LivingEntity.class, new AABB(x, y, z, x, y, z).inflate(range, range, range));
            Collections.shuffle(potentialTargets);

            for (LivingEntity target : potentialTargets) {
                if (target == grenade.getThrower()) continue;

                Vec3NT delta = new Vec3NT(target.getX() - x, target.getY() + target.getBbHeight() / 2 - y, target.getZ() - z);
                if (delta.lengthVector() > range) continue;
                EntityBulletBeamBase sub = new EntityBulletBeamBase(grenade.level(), laser, 30);
                sub.thrower = grenade.getThrower();
                sub.setPos(x, y, z);
                sub.setRotationsFromVector(delta);
                sub.performHitscanExternal(delta.lengthVector());
                grenade.level().addFreshEntity(sub);
            }
        };

        static final Consumer<EntityGrenadeUniversal> EXPLODE_NUKE = (grenade) -> {
            ExplosionVNT vnt = new ExplosionVNT(grenade.level(), grenade.getX(), grenade.getY(), grenade.getZ(), 10);
            vnt.setEntityProcessor(new EntityProcessorCrossSmooth(2, 100).withRangeMod(1.5F));
            vnt.setPlayerProcessor(new PlayerProcessorStandard());
            vnt.explode();

            incrementRad(grenade.level(), grenade.getX(), grenade.getY(), grenade.getZ(), 1F); // Original: XFactoryCatapult.incrementRad (identisch)
            spawnMush(grenade);
        };

        static final Consumer<EntityGrenadeUniversal> EXPLODE_NUKE_DEMO = (grenade) -> {
            ExplosionVNT vnt = new ExplosionVNT(grenade.level(), grenade.getX(), grenade.getY(), grenade.getZ(), 10);
            vnt.setBlockAllocator(new BlockAllocatorStandard(64));
            vnt.setBlockProcessor(new BlockProcessorStandard().withBlockEffect(new BlockMutatorFire()));
            vnt.setEntityProcessor(new EntityProcessorCrossSmooth(2, 50).withRangeMod(1.5F));
            vnt.setPlayerProcessor(new PlayerProcessorStandard());
            vnt.explode();

            incrementRad(grenade.level(), grenade.getX(), grenade.getY(), grenade.getZ(), 1.5F);
            spawnMush(grenade);
        };

        static final Consumer<EntityGrenadeUniversal> EXPLODE_SCHRAB = (grenade) -> {
            Level world = grenade.level();
            EntityNukeExplosionMK3 ex = EntityNukeExplosionMK3.statFacFleija(world, grenade.getX(), grenade.getY(), grenade.getZ(), 20);
            if (!ex.isRemoved()) {
                world.playSound(null, grenade.getX(), grenade.getY(), grenade.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 100.0F, world.random.nextFloat() * 0.1F + 0.9F);
                world.addFreshEntity(ex);
                EntityCloudFleija cloud = new EntityCloudFleija(ModEntities.CLOUD_FLEIJA.get(), world, 20);
                cloud.setPos(grenade.getX(), grenade.getY(), grenade.getZ());
                world.addFreshEntity(cloud);
            }
        };
    }

    /** Gemeinsamer Teil von EXPLODE_INC/EXPLODE_WP: Feuer auf Luftbloecke neben brennbaren Bloecken. */
    private static void igniteAround(EntityGrenadeUniversal grenade, int r) {
        Level world = grenade.level();
        for (int dx = -r; dx <= r; dx++) for (int dy = -r; dy <= r; dy++) for (int dz = -r; dz <= r; dz++) {
            int x = (int) Math.floor(grenade.getX()) + dx; int y = (int) Math.floor(grenade.getY()) + dy; int z = (int) Math.floor(grenade.getZ()) + dz;
            BlockPos pos = new BlockPos(x, y, z);
            if (world.getBlockState(pos).isAir()) for (Direction dir : Direction.values()) {
                BlockPos n = pos.relative(dir);
                if (world.getBlockState(n).isFlammable(world, n, dir.getOpposite())) {
                    world.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
                    break;
                }
            }
        }
    }

    public static void incrementRad(Level world, double posX, double posY, double posZ, float mult) {
        for (int i = -2; i <= 2; i++) { for (int j = -2; j <= 2; j++) {
                if (Math.abs(i) + Math.abs(j) < 4) {
                    ChunkRadiationManager.incrementRad(world, (int) Math.floor(posX + i * 16), (int) Math.floor(posY), (int) Math.floor(posZ + j * 16), 50F / (Math.abs(i) + Math.abs(j) + 1) * mult);
                }
            }
        }
    }

    public static void spawnMush(EntityGrenadeUniversal grenade) {
        Level world = grenade.level();
        DetectorEvents.reportEvent(world, DetectorEvents.DURATION_LOW, DetectorEvents.BurstIntensity.LOW, grenade.getX(), grenade.getZ());
        world.playSound(null, grenade.getX(), grenade.getY(), grenade.getZ(), HbmSoundsNT.get("hbm:weapon.mukeExplosion"), SoundSource.BLOCKS, 15.0F, 1.0F);
        CompoundTag data = new CompoundTag();
        data.putString("type", "muke");
        data.putBoolean("balefire", Polaroid.id() == 11 || world.random.nextInt(100) == 0);
        if (world instanceof ServerLevel server) IParticleCreator.sendPacket(server, grenade.getX(), grenade.getY() + 0.5, grenade.getZ(), 250, data);
    }

    public static void explodeStandardEnergy(EntityGrenadeUniversal grenade, float damage, float range, DamageClass damageClass, float r, float g, float b, float scale) {
        Level world = grenade.level();
        ExplosionVNT vnt = new ExplosionVNT(world, grenade.getX(), grenade.getY(), grenade.getZ(), range, grenade.getThrower());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, damage).setDamageClass(damageClass));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.explode();
        world.playSound(null, grenade.getX(), grenade.getY(), grenade.getZ(), HbmSoundsNT.get("hbm:entity.ufoBlast"), SoundSource.BLOCKS, 5.0F, 0.9F + world.random.nextFloat() * 0.2F);
        world.playSound(null, grenade.getX(), grenade.getY(), grenade.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.BLOCKS, 5.0F, 0.5F);

        float yaw = world.random.nextFloat() * 180F;
        if (world instanceof ServerLevel server) for (int i = 0; i < 3; i++) {
            CompoundTag data = new CompoundTag();
            data.putString("type", "plasmablast");
            data.putFloat("r", r);
            data.putFloat("g", g);
            data.putFloat("b", b);
            data.putFloat("pitch", -60F + 60F * i);
            data.putFloat("yaw", yaw);
            data.putFloat("scale", scale);
            IParticleCreator.sendPacket(server, grenade.getX(), grenade.getY() + 0.125, grenade.getZ(), 100, data);
        }
    }

    public static BiConsumer<EntityBulletBaseMK4, MovingObjectPosition> LAMBDA_TINY_EXPLODE = (bullet, mop) -> {
        if (bullet.tickCount < 2) return;
        Lego.tinyExplode(bullet, mop, 1.5F);
        bullet.setDead();
    };

    public static BiConsumer<EntityBulletBaseMK4, MovingObjectPosition> LAMBDA_EXPLODE = (bullet, mop) -> {
        if (bullet.tickCount < 2) return;
        Lego.standardExplode(bullet, mop, 5F);
        bullet.setDead();
    };

    public static void standardExplode(EntityGrenadeUniversal grenade, float range, float damage) { standardExplode(grenade, range, damage, 0F, 0F); }
    public static void standardExplode(EntityGrenadeUniversal grenade, float range, float damage, float dt, float dr) {
        ExplosionVNT vnt = new ExplosionVNT(grenade.level(), grenade.getX(), grenade.getY(), grenade.getZ(), range, grenade.getThrower());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, damage).setupPiercing(dt, dr));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
        vnt.explode();
    }

    public static void tinyExplode(EntityGrenadeUniversal grenade, float range, float damage) { tinyExplode(grenade, range, damage, 0F, 0F); }
    public static void tinyExplode(EntityGrenadeUniversal grenade, float range, float damage, float dt, float dr) {
        ExplosionVNT vnt = new ExplosionVNT(grenade.level(), grenade.getX(), grenade.getY(), grenade.getZ(), range, grenade.getThrower());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(0.5, damage).setupPiercing(dt, dr).setKnockback(0.25D));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectTiny());
        vnt.explode();
    }

    public static void standardFragmentation(EntityGrenadeUniversal grenade, float frags) {
        if (grenade.getShell() == EnumGrenadeShell.FRAG) frags *= 1.5;
        Level level = grenade.level();
        for (int i = 0; i < frags; i++) {
            EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(level, fragmentation, 10F, 0F, level.random.nextFloat() * 2F * (float) Math.PI, (level.random.nextFloat() - 0.5F) * 2F * (float) Math.PI);
            bullet.setPosition(grenade.getX(), grenade.getY() + 0.05, grenade.getZ());
            level.addFreshEntity(bullet);
        }
    }
}
