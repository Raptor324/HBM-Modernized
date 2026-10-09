package com.hbm_m.particle.helper;

import java.awt.Color;
import java.util.Map;

import com.hbm_m.client.handler.ClientVanishHandler;
import com.hbm_m.client.render.RenderOverhead;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.particle.ModParticleTypes;
import com.hbm_m.particle.nt.MukeCloudBFParticle;
import com.hbm_m.particle.nt.MukeCloudParticle;
import com.hbm_m.particle.nt.MukeFlashParticle;
import com.hbm_m.particle.nt.MukeWaveParticle;
import com.hbm_m.particle.nt.ParticleAmatFlashNT;
import com.hbm_m.particle.nt.ParticleContrailNT;
import com.hbm_m.particle.nt.ParticleDebugNT;
import com.hbm_m.particle.nt.ParticleDropNT;
import com.hbm_m.particle.nt.ParticleEngineNT;
import com.hbm_m.particle.nt.ParticleExSmokeNT;
import com.hbm_m.particle.nt.ParticleFoamNT;
import com.hbm_m.particle.nt.ParticleFoundryNT;
import com.hbm_m.particle.nt.ParticleGibletNT;
import com.hbm_m.particle.nt.ParticleHazeNT;
import com.hbm_m.particle.nt.ParticlePlasmaBlastNT;
import com.hbm_m.particle.nt.ParticleRiftNT;
import com.hbm_m.particle.nt.ParticleRocketFlameNT;
import com.hbm_m.particle.nt.ParticleSmokeFXNT;
import com.hbm_m.particle.nt.ParticleSmokePlumeNT;
import com.hbm_m.particle.nt.ParticleTextNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Client-Einstieg fuer die NT-Partikel (aufgerufen aus {@code AuxParticlePacket}).
 *
 * <p>1:1-Port von {@code ClientProxy.effectNT} (1.7.10): zuerst die {@link ParticleCreators}
 * ("MK3"), danach die lange Kette der aelteren Typen. Vanilla-Partikel des Originals
 * ({@code EntityFlameFX}, {@code EntityReddustFX}, {@code EntityBlockDustFX} ...) laufen ueber die
 * gleichwertigen Vanilla-Typen von 1.20.</p>
 *
 * <p>Noch nicht angeschlossen: "casing" (Huelsenauswurf) - kommt mit dem Waffensystem.</p>
 */
public final class ParticleEffectClient {

    private ParticleEffectClient() {}

    /** Original {@code MainRegistry.proxy.me().getDistanceSq(x, y, z)}; ohne Spieler unendlich weit. */
    public static double localPlayerDistanceSq(double x, double y, double z) {
        Player p = Minecraft.getInstance().player;
        return p == null ? Double.MAX_VALUE : p.distanceToSqr(x, y, z);
    }

    public static void effectNT(CompoundTag data) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> effectNTNow(data));
    }

    /** Synchroner Einstieg, z.B. fuer {@code FlameCreator.composeEffectClient}. */
    public static void effectNTNow(CompoundTag data) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel world = mc.level;
        if (world == null) return;
        Player player = mc.player;
        if (player == null) return;
        int particleSetting = mc.options.particles().get().getId();
        RandomSource rand = world.random;
        String type = data.getString("type");
        double x = data.getDouble("posX");
        double y = data.getDouble("posY");
        double z = data.getDouble("posZ");

        IParticleCreator creator = ParticleCreators.particleCreators().get(type);
        if (creator != null) {
            creator.makeParticle(world, player, rand, x, y, z, data);
            return;
        }

        ParticleEngineNT engine = ParticleEngineNT.INSTANCE;

        switch (type) {
            // Old MK1 system ported to MK3:
            case "waterSplash" -> {
                for (int i = 0; i < 10; i++) {
                    engine.add(new ParticleSmokeFXNT(world, x + rand.nextGaussian(), y + rand.nextGaussian(), z + rand.nextGaussian(), 0.0, 0.0, 0.0, 1F));
                }
            }
            case "cloudFX2" -> engine.add(new ParticleSmokeFXNT(world, x, y, z, 0.0, 0.1, 0.0, 1F));
            case "ABMContrail" -> engine.add(new ParticleContrailNT(world, x, y, z));

            // Old MK2 system ported to MK3:
            case "launchSmoke" -> {
                ParticleSmokePlumeNT contrail = new ParticleSmokePlumeNT(world, x, y, z);
                contrail.xd = data.getDouble("moX");
                contrail.yd = data.getDouble("moY");
                contrail.zd = data.getDouble("moZ");
                engine.add(contrail);
            }
            case "exKerosene" -> engine.add(new ParticleContrailNT(world, x, y, z, 0F, 0F, 0F, 1F));
            case "exSolid" -> engine.add(new ParticleContrailNT(world, x, y, z, 0.3F, 0.2F, 0.05F, 1F));
            case "exHydrogen" -> engine.add(new ParticleContrailNT(world, x, y, z, 0.7F, 0.7F, 0.7F, 1F));
            case "exBalefire" -> engine.add(new ParticleContrailNT(world, x, y, z, 0.2F, 0.7F, 0.2F, 1F));
            case "radFog" -> world.addParticle(ModParticleTypes.RAD_FOG_PARTICLE.get(), x, y, z, 0, 0, 0);

            case "missileContrail" -> {
                if (new Vec3(player.getX() - x, player.getY() - y, player.getZ() - z).length() > 350) return;
                float scale = data.contains("scale") ? data.getFloat("scale") : 1F;
                ParticleRocketFlameNT fx = new ParticleRocketFlameNT(world, x, y, z).setScale(scale);
                fx.xd = data.getDouble("moX");
                fx.yd = data.getDouble("moY");
                fx.zd = data.getDouble("moZ");
                if (data.contains("maxAge")) fx.setMaxAge(data.getInt("maxAge"));
                engine.add(fx);
            }

            case "smoke" -> smoke(world, rand, data, x, y, z);
            case "exhaust" -> exhaust(world, player, rand, data, x, y, z);

            case "fireworks" -> {
                int color = data.getInt("color");
                char c = (char) data.getInt("char");
                engine.add(ParticleTextNT.letter(world, x, y, z, color, c));
                for (int i = 0; i < 50; i++) {
                    Particle blast = add(ParticleTypes.FIREWORK, x, y, z,
                            0.4 * rand.nextGaussian(), 0.4 * rand.nextGaussian(), 0.4 * rand.nextGaussian());
                    if (blast != null) setColour(blast, color);
                }
            }

            case "vanillaburst" -> vanillaBurst(rand, data, x, y, z);
            case "vanillaExt" -> vanillaExt(world, rand, data, x, y, z);

            case "vanilla" -> {
                ParticleOptions opt = vanillaByName(data.getString("mode"));
                //? if >= 1.21.1 {
                /*if (opt == null) opt = mobSpellOption(data.getString("mode"), data.getDouble("mX"), data.getDouble("mY"), data.getDouble("mZ"));
                *///?}
                if (opt != null) world.addParticle(opt, x, y, z, data.getDouble("mX"), data.getDouble("mY"), data.getDouble("mZ"));
            }

            case "jetpack", "jetpack_bj", "jetpack_dns", "bnuuy" -> jetpack(world, player, particleSetting, type, data);

            case "muke" -> {
                engine.add(new MukeWaveParticle(world, x, y, z));
                engine.add(new MukeFlashParticle(world, x, y, z, data.getBoolean("balefire")));
                tilt(player, 15, 15);
            }

            case "tinytot" -> {
                engine.add(new MukeWaveParticle(world, x, y, z));
                for (double d = 0.0D; d <= 1.6D; d += 0.1) {
                    engine.add(new MukeCloudParticle(world, x, y, z, rand.nextGaussian() * 0.05, d + rand.nextGaussian() * 0.02, rand.nextGaussian() * 0.05));
                }
                for (int i = 0; i < 50; i++) {
                    engine.add(new MukeCloudParticle(world, x, y + 0.5, z, rand.nextGaussian() * 0.5, rand.nextInt(5) == 0 ? 0.02 : 0, rand.nextGaussian() * 0.5));
                }
                for (int i = 0; i < 15; i++) {
                    double ix = rand.nextGaussian() * 0.2;
                    double iz = rand.nextGaussian() * 0.2;
                    if (ix * ix + iz * iz > 0.75) {
                        ix *= 0.5;
                        iz *= 0.5;
                    }
                    double iy = 1.6 + (rand.nextDouble() * 2 - 1) * (0.75 - (ix * ix + iz * iz)) * 0.5;
                    engine.add(new MukeCloudParticle(world, x, y, z, ix, iy + rand.nextGaussian() * 0.02, iz));
                }
                tilt(player, 15, 15);
            }

            case "ufo" -> {
                double motion = data.getDouble("motion");
                engine.add(new MukeCloudParticle(world, x, y, z, rand.nextGaussian() * motion, 0, rand.nextGaussian() * motion));
            }
            case "bf" -> engine.add(new MukeCloudBFParticle(world, x, y, z, 0, 0, 0));
            case "haze" -> engine.add(new ParticleHazeNT(world, x, y, z));

            case "plasmablast" -> {
                ParticlePlasmaBlastNT cloud = new ParticlePlasmaBlastNT(world, x, y, z, data.getFloat("r"), data.getFloat("g"), data.getFloat("b"), data.getFloat("pitch"), data.getFloat("yaw"));
                cloud.setScale(data.getFloat("scale"));
                engine.add(cloud);
            }

            case "justTilt" -> tilt(player, data.getInt("time"), data.getInt("time"));
            case "properJolt" -> tilt(player, data.getInt("time"), data.getInt("maxTime"));

            case "sweat" -> {
                Entity e = world.getEntity(data.getInt("entity"));
                BlockState b = blockById(data.getInt("block"));
                if (e instanceof LivingEntity) {
                    AABB bb = e.getBoundingBox();
                    for (int i = 0; i < data.getInt("count"); i++) {
                        double ix = bb.minX - 0.2 + (bb.maxX - bb.minX + 0.4) * rand.nextDouble();
                        double iy = bb.minY + (bb.maxY - bb.minY + 0.2) * rand.nextDouble();
                        double iz = bb.minZ - 0.2 + (bb.maxZ - bb.minZ + 0.4) * rand.nextDouble();
                        Particle fx = add(new BlockParticleOption(ParticleTypes.BLOCK, b), ix, iy, iz, 0, 0, 0);
                        if (fx != null) {
                            fx.setParticleSpeed(0, 0, 0);
                            fx.setLifetime(150 + rand.nextInt(50));
                        }
                    }
                }
            }

            case "radiation" -> {
                for (int i = 0; i < data.getInt("count"); i++) {
                    Particle flash = add(ModParticleTypes.TOWNAURA.get(),
                            player.getX() + rand.nextGaussian() * 4, player.getY() + rand.nextGaussian() * 2, player.getZ() + rand.nextGaussian() * 4,
                            0, 0, 0);
                    if (flash != null) {
                        flash.setColor(0F, 0.75F, 1F);
                        flash.setParticleSpeed(rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian());
                    }
                }
            }

            case "schrabfog" -> {
                Particle flash = add(ModParticleTypes.TOWNAURA.get(), x, y, z, 0, 0, 0);
                if (flash != null) flash.setColor(0F, 1F, 1F);
            }

            case "rift" -> engine.add(new ParticleRiftNT(world, x, y, z));
            case "rbmkflame" -> world.addParticle(ModParticleTypes.RBMK_FLAME.get(), x, y, z, data.getInt("maxAge"), 0, 0);
            case "rbmksteam" -> world.addParticle(ModParticleTypes.RBMK_STEAM.get(), x, y, z, 0, 0, 0);
            case "rbmkmush" -> world.addParticle(ModParticleTypes.RBMK_MUSH.get(), x, y, z, data.getFloat("scale"), 0, 0);

            case "splash" -> {
                if (particleSetting == 0 || (particleSetting == 1 && rand.nextBoolean())) {
                    ParticleDropNT fx = ParticleDropNT.splash(world, x, y, z);
                    if (data.contains("color")) {
                        Color color = new Color(data.getInt("color"));
                        float f = 1F - rand.nextFloat() * 0.2F;
                        fx.setColor(color.getRed() / 255F * f, color.getGreen() / 255F * f, color.getBlue() / 255F * f);
                    }
                    engine.add(fx);
                }
            }

            case "fluidfill" -> {
                Particle fx = add(ParticleTypes.CRIT, x, y, z, data.getDouble("mX"), data.getDouble("mY"), data.getDouble("mZ"));
                if (fx != null && data.contains("color")) {
                    Color color = new Color(data.getInt("color"));
                    fx.setColor(color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F);
                }
            }

            case "deadleaf" -> {
                if (particleSetting == 0 || (particleSetting == 1 && rand.nextBoolean())) engine.add(ParticleDropNT.leaf(world, x, y, z));
            }

            case "vanish" -> ClientVanishHandler.vanish(data.getInt("ent"));

            case "giblets" -> {
                int ent = data.getInt("ent");
                int gibType = data.getInt("gibType");
                ClientVanishHandler.vanish(ent);
                Entity e = world.getEntity(ent);
                if (e == null) return;
                int gW = (int) (e.getBbWidth() / 0.25F);
                int gH = (int) (e.getBbHeight() / 0.25F);
                int count = (int) (gW * 1.5 * gH);
                if (data.contains("cDiv")) count = (int) Math.ceil(count / (double) data.getInt("cDiv"));
                boolean blowMeIntoTheGodDamnStratosphere = rand.nextInt(15) == 0;
                double mult = blowMeIntoTheGodDamnStratosphere ? 10D : 1D;
                for (int i = 0; i < count; i++) {
                    engine.add(new ParticleGibletNT(world, x, y, z, rand.nextGaussian() * 0.25 * mult, rand.nextDouble() * mult, rand.nextGaussian() * 0.25 * mult, gibType));
                }
            }

            case "amat" -> engine.add(new ParticleAmatFlashNT(world, x, y, z, data.getFloat("scale")));
            case "debug" -> engine.add(ParticleTextNT.text(world, x, y, z, data.getInt("color"), data.getString("text")).scaleBy(data.getFloat("scale")));
            case "debugline" -> engine.add(ParticleDebugNT.line(world, x, y, z, data.getDouble("mX"), data.getDouble("mY"), data.getDouble("mZ"), data.getInt("color")));

            case "debugdrone" -> {
                ItemStack held = player.getMainHandItem();
                if (!held.isEmpty() && isDroneItem(BuiltInRegistries.ITEM.getKey(held.getItem()).getPath())) {
                    engine.add(ParticleDebugNT.line(world, x, y, z, data.getDouble("mX"), data.getDouble("mY"), data.getDouble("mZ"), data.getInt("color")));
                }
            }

            case "network" -> {
                double mX = data.getDouble("mX");
                double mY = data.getDouble("mY");
                double mZ = data.getDouble("mZ");
                if ("power".equals(data.getString("mode"))) engine.add(ParticleDebugNT.power(world, x, y, z, mX, mY, mZ));
                if ("fluid".equals(data.getString("mode"))) engine.add(ParticleDebugNT.fluid(world, x, y, z, mX, mY, mZ, data.getInt("color")));
            }

            case "gasfire" -> {
                float scale = data.getFloat("scale");
                engine.add(ParticleSmokeFXNT.gasFlame(world, x, y, z, data.getDouble("mX"), data.getDouble("mY"), data.getDouble("mZ"), scale > 0 ? scale : 6.5F));
            }

            case "marker" -> {
                int color = data.getInt("color");
                String label = data.getString("label");
                int expires = data.getInt("expires");
                double dist = data.getDouble("dist");
                RenderOverhead.queuedMarkers.put(BlockPos.containing(x, y, z), new RenderOverhead.Marker(color).setDist(dist)
                        .setExpire(expires > 0 ? System.currentTimeMillis() + expires : 0).withLabel(label.isEmpty() ? null : label));
            }

            case "foundry" -> engine.add(new ParticleFoundryNT(world, x, y, z, data.getInt("color"), data.getByte("dir"),
                    data.getFloat("len"), data.getFloat("base"), data.getFloat("off")));

            case "frozen" -> {
                player.setDeltaMovement(0, Math.min(player.getDeltaMovement().y, 0), 0);
                player.zza = 0;
                player.xxa = 0;
            }

            default -> { }
        }
    }

    // ------------------------------------------------------------------ Zweige

    private static void smoke(ClientLevel world, RandomSource rand, CompoundTag data, double x, double y, double z) {
        ParticleEngineNT engine = ParticleEngineNT.INSTANCE;
        String mode = data.getString("mode");
        int count = Math.max(1, data.getInt("count"));

        switch (mode) {
            case "cloud" -> {
                for (int i = 0; i < count; i++) {
                    ParticleExSmokeNT fx = new ParticleExSmokeNT(world, x, y, z);
                    fx.yd = rand.nextGaussian() * (1 + (count / 100));
                    fx.xd = rand.nextGaussian() * (1 + (count / 150));
                    fx.zd = rand.nextGaussian() * (1 + (count / 150));
                    if (rand.nextBoolean()) fx.yd = Math.abs(fx.yd);
                    engine.add(fx);
                }
            }
            case "radial" -> {
                for (int i = 0; i < count; i++) {
                    ParticleExSmokeNT fx = new ParticleExSmokeNT(world, x, y, z);
                    fx.yd = rand.nextGaussian() * (1 + (count / 50));
                    fx.xd = rand.nextGaussian() * (1 + (count / 50));
                    fx.zd = rand.nextGaussian() * (1 + (count / 50));
                    engine.add(fx);
                }
            }
            case "radialDigamma" -> {
                Vec3 vec = new Vec3(2, 0, 0).yRot(rand.nextFloat() * (float) Math.PI * 2F);
                for (int i = 0; i < count; i++) {
                    world.addParticle(ModParticleTypes.DIGAMMA_SMOKE.get(), x, y, z, vec.x, 0, vec.z);
                    vec = vec.yRot((float) Math.PI * 2F / (float) count);
                }
            }
            case "shock" -> {
                double strength = data.getDouble("strength");
                Vec3 vec = new Vec3(strength, 0, 0).yRot(rand.nextInt(360));
                for (int i = 0; i < count; i++) {
                    ParticleExSmokeNT fx = new ParticleExSmokeNT(world, x, y, z);
                    fx.yd = 0;
                    fx.xd = vec.x;
                    fx.zd = vec.z;
                    engine.add(fx);
                    vec = vec.yRot((float) Math.PI * 2F / (float) count);
                }
            }
            case "shockRand" -> {
                double strength = data.getDouble("strength");
                Vec3 vec = new Vec3(strength, 0, 0).yRot(rand.nextInt(360));
                for (int i = 0; i < count; i++) {
                    double r = rand.nextDouble();
                    ParticleExSmokeNT fx = new ParticleExSmokeNT(world, x, y, z);
                    fx.yd = 0;
                    fx.xd = vec.x * r;
                    fx.zd = vec.z * r;
                    engine.add(fx);
                    vec = vec.yRot(360 / count);
                }
            }
            case "wave" -> {
                double strength = data.getDouble("range");
                Vec3 vec = new Vec3(strength, 0, 0);
                for (int i = 0; i < count; i++) {
                    vec = vec.yRot((float) Math.toRadians(rand.nextFloat() * 360F));
                    ParticleExSmokeNT fx = new ParticleExSmokeNT(world, x + vec.x, y, z + vec.z);
                    fx.setMaxAge(50);
                    fx.xd = fx.yd = fx.zd = 0;
                    engine.add(fx);
                    vec = vec.yRot(360 / count);
                }
            }
            case "foamSplash" -> {
                double strength = data.getDouble("range");
                Vec3 vec = new Vec3(strength, 0, 0);
                for (int i = 0; i < count; i++) {
                    vec = vec.yRot((float) Math.toRadians(rand.nextFloat() * 360F));
                    ParticleFoamNT fx = new ParticleFoamNT(world, x + vec.x, y, z + vec.z);
                    fx.setMaxAge(50);
                    fx.xd = fx.yd = fx.zd = 0;
                    engine.add(fx);
                    vec = vec.yRot(360 / count);
                }
            }
            default -> { }
        }
    }

    private static void exhaust(ClientLevel world, Player player, RandomSource rand, CompoundTag data, double x, double y, double z) {
        String mode = data.getString("mode");
        if (!"soyuz".equals(mode) && !"lambda".equals(mode) && !"meteor".equals(mode)) return;
        if (new Vec3(player.getX() - x, player.getY() - y, player.getZ() - z).length() > 350) return;
        int count = Math.max(1, data.getInt("count"));
        double width = data.getDouble("width");
        for (int i = 0; i < count; i++) {
            if ("soyuz".equals(mode)) {
                ParticleRocketFlameNT fx = new ParticleRocketFlameNT(world, x + rand.nextGaussian() * width, y, z + rand.nextGaussian() * width);
                fx.yd = -0.75 + rand.nextDouble() * 0.5;
                ParticleEngineNT.INSTANCE.add(fx);
            } else if ("lambda".equals(mode)) {
                ParticleRocketFlameNT fx = new ParticleRocketFlameNT(world, x + rand.nextGaussian() * width, y, z + rand.nextGaussian() * width).setScale(1.5F);
                fx.yd = -1 + rand.nextDouble() * 0.25;
                ParticleEngineNT.INSTANCE.add(fx);
            } else {
                ParticleEngineNT.INSTANCE.add(new ParticleRocketFlameNT(world, x + rand.nextGaussian() * width, y + rand.nextGaussian() * width, z + rand.nextGaussian() * width));
            }
        }
    }

    private static void vanillaBurst(RandomSource rand, CompoundTag data, double x, double y, double z) {
        double motion = data.getDouble("motion");
        String mode = data.getString("mode");
        for (int i = 0; i < data.getInt("count"); i++) {
            double mX = rand.nextGaussian() * motion;
            double mY = rand.nextGaussian() * motion;
            double mZ = rand.nextGaussian() * motion;
            switch (mode) {
                case "flame" -> add(ParticleTypes.FLAME, x, y, z, mX, mY, mZ);
                case "cloud" -> add(ParticleTypes.CLOUD, x, y, z, mX, mY, mZ);
                case "reddust" -> {
                    Particle fx = add(dust(0F, 0F, 0F), x, y, z, 0, 0, 0);
                    if (fx != null) fx.setParticleSpeed(mX, mY, mZ);
                }
                case "bluedust" -> add(dust(0.01F, 0.01F, 1F), x, y, z, 0, 0, 0);
                case "greendust" -> add(dust(0.01F, 0.5F, 0.1F), x, y, z, 0, 0, 0);
                case "blockdust" -> {
                    Particle fx = add(new BlockParticleOption(ParticleTypes.BLOCK, blockById(data.getInt("block"))), x, y, z, mX, mY + 0.2, mZ);
                    if (fx != null) {
                        fx.setParticleSpeed(mX, mY + 0.2, mZ);
                        fx.setLifetime(50 + rand.nextInt(50));
                    }
                }
                default -> { }
            }
        }
    }

    private static void vanillaExt(ClientLevel world, RandomSource rand, CompoundTag data, double x, double y, double z) {
        double mX = data.getDouble("mX");
        double mY = data.getDouble("mY");
        double mZ = data.getDouble("mZ");
        String mode = data.getString("mode");
        Particle fx = null;

        switch (mode) {
            case "flame" -> fx = add(ParticleTypes.FLAME, x, y, z, mX, mY, mZ);
            case "smoke" -> fx = add(ParticleTypes.SMOKE, x, y, z, mX, mY, mZ);
            case "volcano" -> {
                ParticleSmokeFXNT smoke = new ParticleSmokeFXNT(world, x, y, z, mX, mY, mZ, 1F);
                smoke.smokeParticleScale = 100;
                smoke.setMaxAge(200 + rand.nextInt(50));
                smoke.noClip = true;
                smoke.yd = 2.5 + rand.nextDouble();
                smoke.xd = rand.nextGaussian() * 0.2;
                smoke.zd = rand.nextGaussian() * 0.2;
                if (data.getInt("overrideAge") > 0) smoke.setMaxAge(data.getInt("overrideAge"));
                ParticleEngineNT.INSTANCE.add(smoke);
                return;
            }
            case "cloud" -> {
                fx = add(ParticleTypes.CLOUD, x, y, z, mX, mY, mZ);
                if (fx != null && data.contains("r")) {
                    float rng = rand.nextFloat() * 0.1F;
                    fx.setColor(data.getFloat("r") + rng, data.getFloat("g") + rng, data.getFloat("b") + rng);
                    fx.scale(7.5F / 2.5F);
                    fx.setParticleSpeed(0, 0, 0);
                }
            }
            case "reddust" -> fx = add(dust((float) mX, (float) mY, (float) mZ), x, y, z, 0, 0, 0);
            case "bluedust" -> fx = add(dust(0.01F, 0.01F, 1F), x, y, z, 0, 0, 0);
            case "greendust" -> fx = add(dust(0.01F, 0.5F, 0.1F), x, y, z, 0, 0, 0);
            case "fireworks" -> fx = add(ParticleTypes.FIREWORK, x, y, z, 0, 0, 0);
            case "largeexplode" -> {
                fx = add(ParticleTypes.EXPLOSION, x, y, z, data.getFloat("size"), 0.0F, 0.0F);
                float r = 1.0F - rand.nextFloat() * 0.2F;
                if (fx != null) fx.setColor(1F * r, 0.9F * r, 0.5F * r);
                for (int i = 0; i < data.getByte("count"); i++) {
                    Particle sec = add(ParticleTypes.POOF, x, y, z, 0.0F, 0.0F, 0.0F);
                    if (sec != null) {
                        float r2 = 1.0F - rand.nextFloat() * 0.5F;
                        sec.setColor(0.5F * r2, 0.5F * r2, 0.5F * r2);
                        sec.scale(i + 1);
                    }
                }
            }
            case "townaura" -> {
                fx = add(ModParticleTypes.TOWNAURA.get(), x, y, z, 0, 0, 0);
                if (fx != null) {
                    float color = 0.5F + rand.nextFloat() * 0.5F;
                    fx.setColor(0.8F * color, 0.9F * color, 1.0F * color);
                    fx.setParticleSpeed(mX, mY, mZ);
                }
            }
            case "blockdust" -> {
                fx = add(new BlockParticleOption(ParticleTypes.BLOCK, blockById(data.getInt("block"))), x, y, z, mX, mY + 0.2, mZ);
                if (fx != null) {
                    fx.setParticleSpeed(mX, mY + 0.2, mZ);
                    fx.setLifetime(10 + rand.nextInt(20));
                }
            }
            case "colordust" -> {
                fx = add(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.WHITE_WOOL.defaultBlockState()), x, y, z, mX, mY + 0.2, mZ);
                if (fx != null) {
                    fx.setParticleSpeed(mX, mY + 0.2, mZ);
                    fx.setColor(data.getFloat("r"), data.getFloat("g"), data.getFloat("b"));
                    fx.setLifetime(10 + rand.nextInt(20));
                }
            }
            default -> { }
        }

        if (fx != null && data.getInt("overrideAge") > 0) fx.setLifetime(data.getInt("overrideAge"));
        if (fx != null && data.getBoolean("noclip")) NoClipAccess.noClip(fx);
    }

    private static void jetpack(ClientLevel world, Player player, int particleSetting, String type, CompoundTag data) {
        if (particleSetting == 2) return;
        Entity ent = world.getEntity(data.getInt("player"));
        if (!(ent instanceof Player p)) return;

        float angle = (float) -Math.toRadians(p.yHeadRot - (p.yHeadRot - p.yBodyRot));
        Vec3 offset = new Vec3(0.125, 0, 0);
        Vec3 vec;
        double ix, iy, iz;

        switch (type) {
            case "jetpack" -> {
                vec = new Vec3(0, 0, -0.25).yRot(angle);
                offset = offset.yRot(angle);
                ix = p.getX() + vec.x;
                iy = p.getY() + p.getEyeHeight() - 1;
                iz = p.getZ() + vec.z;
                double moX = 0, moY = 0, moZ = 0;
                int mode = data.getInt("mode");
                if (mode == 0) moY -= 0.2;
                if (mode == 1) {
                    Vec3 look = p.getLookAngle();
                    moX -= look.x * 0.1D;
                    moY -= look.y * 0.1D;
                    moZ -= look.z * 0.1D;
                }
                if (particleSetting == 0) groundDust(world, player, ix, iy, iz, new Vec3(moX, moY, moZ).normalize());

                Vec3 m = p.getDeltaMovement();
                double mX2 = safeClamp(m.x + moX * 2, -5, 5);
                double mY2 = safeClamp(m.y + moY * 2, -5, 5);
                double mZ2 = safeClamp(m.z + moZ * 2, -5, 5);
                double mX3 = safeClamp(m.x + moX * 2, -10, 10);
                double mY3 = safeClamp(m.y + moY * 2, -10, 10);
                double mZ3 = safeClamp(m.z + moZ * 2, -10, 10);
                add(ParticleTypes.FLAME, ix + offset.x, iy, iz + offset.z, mX2, mY2, mZ2);
                add(ParticleTypes.FLAME, ix - offset.x, iy, iz - offset.z, mX2, mY2, mZ2);
                if (particleSetting == 0) {
                    add(ParticleTypes.SMOKE, ix + offset.x, iy, iz + offset.z, mX3, mY3, mZ3);
                    add(ParticleTypes.SMOKE, ix - offset.x, iy, iz - offset.z, mX3, mY3, mZ3);
                }
            }
            case "bnuuy" -> {
                vec = new Vec3(0, 0, -0.6).yRot(angle);
                offset = new Vec3(0.275, 0, 0).yRot(angle);
                ix = p.getX() + vec.x;
                iy = p.getY() + p.getEyeHeight() - 1 + 0.4;
                iz = p.getZ() + vec.z;
                if (player.isShiftKeyDown()) iy += 0.25;
                vec = vec.normalize();
                double mult = 0.025D;
                for (int i = 0; i < 2; i++) {
                    ParticleSmokeFXNT fx = new ParticleSmokeFXNT(world, ix + offset.x * (i == 0 ? -1 : 1), iy, iz + offset.z * (i == 0 ? -1 : 1), vec.x * mult, 0, vec.z * mult, 1F);
                    fx.smokeParticleScale = 0.5F;
                    ParticleEngineNT.INSTANCE.add(fx);
                }
            }
            case "jetpack_bj" -> {
                vec = new Vec3(0, 0, -0.3125).yRot(angle);
                offset = offset.yRot(angle);
                ix = p.getX() + vec.x;
                iy = p.getY() + p.getEyeHeight() - 0.9375;
                iz = p.getZ() + vec.z;
                if (particleSetting == 0) groundDust(world, player, ix, iy, iz, new Vec3(0, -1, 0));
                dustAt(ix + offset.x, iy, iz + offset.z, 0.8F, 0.5F, 1.0F, p.getDeltaMovement());
                dustAt(ix - offset.x, iy, iz - offset.z, 0.8F, 0.5F, 1.0F, p.getDeltaMovement());
            }
            case "jetpack_dns" -> {
                offset = offset.yRot(angle);
                ix = p.getX();
                iy = p.getY() - 0.5D;
                iz = p.getZ();
                if (particleSetting == 0) groundDust(world, player, ix, iy, iz, new Vec3(0, -1, 0));
                dustAt(ix + offset.x, iy, iz + offset.z, 0.01F, 1.0F, 1.0F, p.getDeltaMovement());
                dustAt(ix - offset.x, iy, iz - offset.z, 0.01F, 1.0F, 1.0F, p.getDeltaMovement());
            }
            default -> { }
        }
    }

    private static void dustAt(double x, double y, double z, float r, float g, float b, Vec3 velocity) {
        Particle dust = add(dust(r, g, b), x, y, z, 0, 0, 0);
        if (dust != null) dust.setParticleSpeed(velocity.x, velocity.y, velocity.z);
    }

    /** Aufgewirbelter Staub unter dem Strahl, sobald er auf eine Oberseite trifft. */
    private static void groundDust(ClientLevel world, Player player, double ix, double iy, double iz, Vec3 thrust) {
        Vec3 pos = new Vec3(ix, iy, iz);
        Vec3 target = pos.add(thrust.x * 10, thrust.y * 10, thrust.z * 10);
        BlockHitResult mop = world.clip(new ClipContext(pos, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (mop.getType() == HitResult.Type.BLOCK && mop.getDirection() == Direction.UP) {
            BlockState b = world.getBlockState(mop.getBlockPos());
            Vec3 hit = mop.getLocation();
            Vec3 delta = new Vec3(ix - hit.x, iy - hit.y, iz - hit.z);
            Vec3 vel = new Vec3(0.75 - delta.length() * 0.075, 0, 0);
            for (int i = 0; i < (10 - delta.length()); i++) {
                vel = vel.yRot(world.random.nextFloat() * (float) Math.PI * 2F);
                Particle fx = add(new BlockParticleOption(ParticleTypes.BLOCK, b), hit.x, hit.y + 0.1, hit.z, vel.x, 0.1, vel.z);
                if (fx != null) fx.setParticleSpeed(vel.x, 0.1, vel.z);
            }
        }
    }

    // ------------------------------------------------------------------ Hilfen

    private static void tilt(Player player, int time, int maxTime) {
        player.hurtTime = time;
        player.hurtDuration = maxTime;
        // attackedAtYaw = 0: in 1.20 geschuetzt; animateHurt(0) setzt die Richtung, Zeiten danach neu.
        player.animateHurt(0F);
        player.hurtTime = time;
        player.hurtDuration = maxTime;
    }

    private static double safeClamp(double val, double min, double max) {
        if (Double.isNaN(val)) return 0;
        return Mth.clamp(val, min, max);
    }

    private static DustParticleOptions dust(float r, float g, float b) {
        // EntityReddustFX: Rot 0 wird zu 1 - die klassische Redstone-Farbe.
        if (r == 0.0F) r = 1.0F;
        return new DustParticleOptions(new Vector3f(r, g, b), 1.0F);
    }

    private static void setColour(Particle p, int color) {
        p.setColor(((color >> 16) & 255) / 255.0F, ((color >> 8) & 255) / 255.0F, (color & 255) / 255.0F);
    }

    public static Particle add(ParticleOptions opt, double x, double y, double z, double mx, double my, double mz) {
        return Minecraft.getInstance().particleEngine.createParticle(opt, x, y, z, mx, my, mz);
    }

    /** {@code Block.getBlockById} - im Port ist die "ID" die Blockstate-ID ({@link Block#getId}). */
    public static BlockState blockById(int id) {
        BlockState state = Block.stateById(id);
        return state == null ? Blocks.AIR.defaultBlockState() : state;
    }

    private static final Map<String, ParticleOptions> VANILLA = Map.ofEntries(
            Map.entry("bubble", ParticleTypes.BUBBLE), Map.entry("suspended", ParticleTypes.UNDERWATER),
            Map.entry("depthsuspend", ParticleTypes.MYCELIUM), Map.entry("townaura", ParticleTypes.MYCELIUM),
            Map.entry("crit", ParticleTypes.CRIT), Map.entry("magicCrit", ParticleTypes.ENCHANTED_HIT),
            //? if < 1.21.1 {
            Map.entry("smoke", ParticleTypes.SMOKE), Map.entry("mobSpell", ParticleTypes.ENTITY_EFFECT),
            Map.entry("mobSpellAmbient", ParticleTypes.AMBIENT_ENTITY_EFFECT), Map.entry("spell", ParticleTypes.EFFECT),
            //?} else {
            /*// mobSpell/mobSpellAmbient: auf 1.21.1 steckt die Farbe in der Option, siehe mobSpellOption
            Map.entry("smoke", ParticleTypes.SMOKE), Map.entry("spell", ParticleTypes.EFFECT),
            *///?}
            Map.entry("instantSpell", ParticleTypes.INSTANT_EFFECT), Map.entry("witchMagic", ParticleTypes.WITCH),
            Map.entry("note", ParticleTypes.NOTE), Map.entry("portal", ParticleTypes.PORTAL),
            Map.entry("enchantmenttable", ParticleTypes.ENCHANT), Map.entry("explode", ParticleTypes.POOF),
            Map.entry("flame", ParticleTypes.FLAME), Map.entry("lava", ParticleTypes.LAVA),
            Map.entry("footstep", ParticleTypes.POOF), Map.entry("splash", ParticleTypes.SPLASH),
            Map.entry("wake", ParticleTypes.FISHING), Map.entry("largesmoke", ParticleTypes.LARGE_SMOKE),
            Map.entry("cloud", ParticleTypes.CLOUD), Map.entry("reddust", DustParticleOptions.REDSTONE),
            Map.entry("snowballpoof", ParticleTypes.ITEM_SNOWBALL), Map.entry("dripWater", ParticleTypes.DRIPPING_WATER),
            Map.entry("dripLava", ParticleTypes.DRIPPING_LAVA), Map.entry("snowshovel", ParticleTypes.ITEM_SNOWBALL),
            Map.entry("slime", ParticleTypes.ITEM_SLIME), Map.entry("heart", ParticleTypes.HEART),
            Map.entry("angryVillager", ParticleTypes.ANGRY_VILLAGER), Map.entry("happyVillager", ParticleTypes.HAPPY_VILLAGER),
            Map.entry("hugeexplosion", ParticleTypes.EXPLOSION_EMITTER), Map.entry("largeexplode", ParticleTypes.EXPLOSION),
            Map.entry("fireworksSpark", ParticleTypes.FIREWORK));

    //? if >= 1.21.1 {
    /*/^* 1.20.1: ENTITY_EFFECT/AMBIENT_ENTITY_EFFECT faerben sich ueber die Geschwindigkeit (r, g, b), ambient mit Alpha 0.15. ^/
    private static ParticleOptions mobSpellOption(String name, double r, double g, double b) {
        float a;
        if (name.equals("mobSpell")) a = 1.0F;
        else if (name.equals("mobSpellAmbient")) a = 0.15F;
        else return null;
        return net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT,
                net.minecraft.util.FastColor.ARGB32.colorFromFloat(a, (float) r, (float) g, (float) b));
    }
    *///?}

    /** 1.7.10-Partikelnamen fuer {@code world.spawnParticle(name, ...)}. */
    public static ParticleOptions vanillaByName(String name) {
        return VANILLA.get(name);
    }

    private static boolean isDroneItem(String id) {
        return id.equals("drone") || id.equals("drone_crate_provider") || id.equals("drone_crate_requester")
                || id.equals("drone_dock") || id.equals("drone_waypoint_request") || id.equals("drone_waypoint")
                || id.equals("drone_linker");
    }

    /**
     * Аура радиации вокруг игрока при дозе &gt;600 RAD. Порт client-ветки {@code EntityEffectHandler.handleRadiationFX} (1.7.10).
     */
    public static void tickRadiationAura(Player player) {
        if (!ModClothConfig.get().enableRadiation || player == null) {
            return;
        }

        float radiation = HbmLivingProps.getRadiation(player);
        if (radiation <= 600F) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }

        int count = radiation > 900F ? 4 : radiation > 800F ? 2 : 1;
        RandomSource rand = level.random;

        for (int i = 0; i < count; i++) {
            mc.particleEngine.createParticle(
                    ParticleTypes.END_ROD,
                    player.getX() + rand.nextGaussian() * 4.0D,
                    player.getY() + rand.nextGaussian() * 2.0D,
                    player.getZ() + rand.nextGaussian() * 4.0D,
                    rand.nextGaussian(),
                    rand.nextGaussian(),
                    rand.nextGaussian());
        }
    }

    /** Zugriff auf das geschuetzte {@code hasPhysics} der Vanilla-Partikel (Original: {@code noClip}). */
    private static final class NoClipAccess {
        private static java.lang.reflect.Field field;
        private static boolean failed;

        static void noClip(Particle p) {
            if (failed) return;
            try {
                if (field == null) {
                    for (String name : new String[] { "hasPhysics", "f_107219_" }) {
                        try {
                            field = Particle.class.getDeclaredField(name);
                            break;
                        } catch (NoSuchFieldException ignored) { }
                    }
                    if (field == null) { failed = true; return; }
                    field.setAccessible(true);
                }
                field.setBoolean(p, false);
            } catch (Throwable t) {
                failed = true;
            }
        }
    }
}
