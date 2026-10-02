package com.hbm_m.handler;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.damagesource.ModDamageTypes;
import com.hbm_m.explosion.ExplosionNukeSmall;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.extprop.HbmPlayerProps;
import com.hbm_m.particle.helper.FlameCreator;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.ArmorUtil;
import com.hbm_m.util.ContaminationUtil;
import com.hbm_m.util.ContaminationUtil.ContaminationType;
import com.hbm_m.util.ContaminationUtil.HazardType;
import com.hbm_m.world.biome.ModBiomes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Die Zweige von {@code EntityEffectHandler} (1.7.10), die dem Port fehlten: Schild-Regeneration
 * und Sync, Zeitbombe, 528-Netherbrand, Kraterstaub, Koerperkontamination, Ansteckung (MKU),
 * Digamma-Schweiss, Oel, Brandzustaende, Dash, Plinking und der Multiblock-Leiter-Hack.
 */
public final class EntityEffectHandlerExtra {

    private EntityEffectHandlerExtra() {}

    static void clientTick(Player player) {
        if (player != net.minecraft.client.Minecraft.getInstance().player) return;
        var biomeKey = player.level().getBiome(player.blockPosition()).unwrapKey().orElse(null);
        if (biomeKey == ModBiomes.CRATER_KEY || biomeKey == ModBiomes.INNER_CRATER_KEY) {
            RandomSource rand = player.getRandom();
            for (int i = 0; i < 3; i++) {
                player.level().addParticle(com.hbm_m.particle.ModParticleTypes.TOWNAURA.get(),
                        player.getX() + rand.nextGaussian() * 3, player.getY() + rand.nextGaussian() * 2, player.getZ() + rand.nextGaussian() * 3, 0, 0, 0);
            }
        }
    }

    static void serverHead(LivingEntity entity) {
        if (entity instanceof ServerPlayer sp) {
            HbmPlayerProps pprps = HbmPlayerProps.get(sp);
            if (pprps.shield < pprps.getEffectiveMaxShield() && entity.tickCount > pprps.lastDamage + 60) {
                int tsd = entity.tickCount - (pprps.lastDamage + 60);
                pprps.shield += Math.min(pprps.getEffectiveMaxShield() - pprps.shield, 0.005F * tsd);
            }
            if (pprps.shield > pprps.getEffectiveMaxShield()) pprps.shield = pprps.getEffectiveMaxShield();
            pprps.save();
            com.hbm_m.network.ExtPropPacket.sendTo(sp);
        }

        int timer = HbmLivingProps.getTimer(entity);
        if (timer > 0) {
            HbmLivingProps.setTimer(entity, timer - 1);
            if (timer == 1) {
                ExplosionNukeSmall.explode(entity.level(), entity.getX(), entity.getY(), entity.getZ(), ExplosionNukeSmall.PARAMS_MEDIUM);
            }
        }

        // only sets players on fire so mod compatibility doesnt die
        if (ModClothConfig.get().enable528NetherBurn && entity instanceof Player && !entity.fireImmune()
                && entity.level().dimensionType().ultraWarm()) {
            entity.setSecondsOnFire(5);
        }
    }

    /** Original {@code handleFauxLadder}: Leitern an Multiblocks ({@code isOnLadder} setzt der Block). */
    static void handleFauxLadder(Player player) {
        HbmPlayerProps props = HbmPlayerProps.get(player);
        if (props.isOnLadder) {
            double climbSpeed = 0.15;
            Vec3 m = player.getDeltaMovement();
            double mx = Math.max(-climbSpeed, Math.min(climbSpeed, m.x));
            double mz = Math.max(-climbSpeed, Math.min(climbSpeed, m.z));
            double my = m.y;
            player.fallDistance = 0.0F;
            if (my < -climbSpeed) my = -climbSpeed;
            if (player.isShiftKeyDown() && my < 0.0D) my = 0.0D;
            if (player.horizontalCollision) my = 0.2D;
            player.setDeltaMovement(mx, my, mz);
            props.isOnLadder = false;
            if (!player.level().isClientSide) ArmorUtil.resetFlightTime(player);
        }
    }

    /** Original {@code handleContamination}: abklingende Koerperkontaminationen strahlen jeden Tick. */
    static void handleContamination(LivingEntity entity) {
        List<HbmLivingProps.ContaminationEffect> contamination = HbmLivingProps.getCont(entity);
        if (contamination.isEmpty()) return;
        List<HbmLivingProps.ContaminationEffect> rem = new ArrayList<>();
        for (HbmLivingProps.ContaminationEffect con : contamination) {
            ContaminationUtil.contaminate(entity, HazardType.RADIATION, con.ignoreArmor ? ContaminationType.RAD_BYPASS : ContaminationType.CREATIVE, con.getRad());
            con.time--;
            if (con.time <= 0) rem.add(con);
        }
        contamination.removeAll(rem);
        HbmLivingProps.setCont(entity, contamination);
    }

    /** Original {@code handleDigamma}: je mehr Digamma, desto oefter Seelensand-Schweiss. */
    static void handleDigamma(LivingEntity entity) {
        float digamma = HbmLivingProps.getDigamma(entity);
        if (digamma < 0.01F) return;
        int chance = Math.max(10 - (int) (digamma), 1);
        if (chance == 1 || entity.getRandom().nextInt(chance) == 0) {
            sendSweat(entity, Blocks.SOUL_SAND, 1);
        }
    }

    static void sendSweat(LivingEntity entity, Block block, int count) {
        if (!(entity.level() instanceof ServerLevel sl)) return;
        CompoundTag data = new CompoundTag();
        data.putString("type", "sweat");
        data.putInt("count", count);
        data.putInt("block", Block.getId(block.defaultBlockState()));
        data.putInt("entity", entity.getId());
        IParticleCreator.sendPacket(sl, entity.getX(), entity.getY(), entity.getZ(), 25, data);
    }

    private static boolean bacteriaProtected(LivingEntity living) {
        return ArmorUtil.checkForHaz2(living) && com.hbm_m.handler.ArmorRegistry.hasProtection(living, 3, com.hbm_m.handler.HazardClass.BACTERIA);
    }

    /** 1:1 {@code handleContagion}: das MKU. */
    static void handleContagion(LivingEntity entity) {
        if (!ModClothConfig.get().enableMKU) return;
        Level world = entity.level();
        RandomSource rand = entity.getRandom();
        int minute = 60 * 20;
        int hour = 60 * minute;

        int contagion = HbmLivingProps.getContagion(entity);

        if (entity instanceof Player player) {
            var inv = player.getInventory();
            int randSlot = rand.nextInt(inv.items.size());
            ItemStack stack = inv.getItem(randSlot);
            if (rand.nextInt(100) == 0) stack = inv.armor.get(rand.nextInt(4));

            // only affect unstackables (e.g. tools and armor) so that the NBT tag's stack restrictions isn't noticeable
            if (!stack.isEmpty() && stack.getMaxStackSize() == 1) {
                if (contagion > 0) {
                    stack.getOrCreateTag().putBoolean("ntmContagion", true);
                } else if (stack.hasTag() && stack.getTag().getBoolean("ntmContagion")) {
                    if (!bacteriaProtected(player)) HbmLivingProps.setContagion(player, 3 * hour);
                }
            }
        }

        if (contagion > 0) {
            HbmLivingProps.setContagion(entity, contagion - 1);

            // aerial transmission only happens once a second 5 minutes into the contagion
            if (contagion < (2 * hour + 55 * minute) && contagion % 20 == 0) {
                double range = entity.isInWaterRainOrBubble() ? 16D : 2D; // avoid rain, just avoid it
                for (Entity ent : world.getEntities(entity, entity.getBoundingBox().inflate(range))) {
                    if (ent instanceof LivingEntity living) {
                        if (HbmLivingProps.getContagion(living) <= 0 && !bacteriaProtected(living)) {
                            HbmLivingProps.setContagion(living, 3 * hour);
                        }
                    }
                    if (ent instanceof ItemEntity item) {
                        item.getItem().getOrCreateTag().putBoolean("ntmContagion", true);
                    }
                }
            }

            // one hour in, add rare and subtle screen fuckery
            if (contagion < 2 * hour && rand.nextInt(1000) == 0) entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20, 0));

            // two hours in, give 'em the full blast
            if (contagion < 1 * hour && rand.nextInt(100) == 0) {
                entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
                entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 4));
            }

            // T-30 minutes, take damage every 20 seconds
            if (contagion < 30 * minute && rand.nextInt(400) == 0) entity.hurt(ModDamageSources.create(world, ModDamageTypes.MKU), 1F);

            // T-5 minutes, take damage every 5 seconds
            if (contagion < 5 * minute && rand.nextInt(100) == 0) entity.hurt(ModDamageSources.create(world, ModDamageTypes.MKU), 2F);

            if (contagion < 30 * minute && (contagion + entity.getId()) % 200 < 20 && !(entity instanceof WaterAnimal) && world instanceof ServerLevel sl) {
                CompoundTag nbt = new CompoundTag();
                nbt.putString("type", "vomit");
                nbt.putString("mode", "blood");
                nbt.putInt("count", 25);
                nbt.putInt("entity", entity.getId());
                IParticleCreator.sendPacket(sl, entity.getX(), entity.getY(), entity.getZ(), 25, nbt);
                if ((contagion + entity.getId()) % 200 == 19) EntityEffectHandler.vomitSound(world, entity);
            }

            // end of contagion, drop dead
            if (contagion == 0) entity.hurt(ModDamageSources.create(world, ModDamageTypes.MKU), 1000F);
        }
    }

    /** Original {@code handleOil}: oeliger Koerper explodiert, sobald er brennt. */
    static void handleOil(LivingEntity entity) {
        int oil = HbmLivingProps.getOil(entity);
        if (oil > 0) {
            if (entity.isOnFire()) {
                HbmLivingProps.setOil(entity, 0);
                entity.level().explode(null, entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(), 3F, false, Level.ExplosionInteraction.MOB);
            } else {
                HbmLivingProps.setOil(entity, oil - 1);
            }
            if (entity.tickCount % 5 == 0) sendSweat(entity, Blocks.COAL_BLOCK, 1);
        }
    }

    /** 1:1 {@code handleTemperature}: Feuer, Phosphor, Balefire und Schwarzes Feuer am Koerper. */
    static void handleTemperature(LivingEntity living) {
        if (!living.isAlive()) return;
        RandomSource rand = living.getRandom();
        Level world = living.level();

        if (living.fireImmune()) {
            HbmLivingProps.setFire(living, 0);
            HbmLivingProps.setPhosphorus(living, 0);
        }

        double x = living.getX(), y = living.getY(), z = living.getZ();
        float w = living.getBbWidth(), h = living.getBbHeight();
        int tick = living.tickCount + living.getId();

        if (living.isInWaterRainOrBubble()) HbmLivingProps.setFire(living, 0);

        int fire = HbmLivingProps.getFire(living);
        if (fire > 0) {
            HbmLivingProps.setFire(living, fire - 1);
            if (tick % 15 == 0) fizz(living, rand);
            if (tick % 40 == 0) living.hurt(world.damageSources().onFire(), 2F);
            flame(living, rand, x, y, z, w, h, FlameCreator.META_FIRE);
        }

        int phosphorus = HbmLivingProps.getPhosphorus(living);
        if (phosphorus > 0) {
            HbmLivingProps.setPhosphorus(living, phosphorus - 1);
            if (tick % 15 == 0) fizz(living, rand);
            if (tick % 40 == 0) living.hurt(world.damageSources().onFire(), 5F);
            flame(living, rand, x, y, z, w, h, FlameCreator.META_FIRE);
        }

        int balefire = HbmLivingProps.getBalefire(living);
        if (balefire > 0) {
            HbmLivingProps.setBalefire(living, balefire - 1);
            if (tick % 15 == 0) fizz(living, rand);
            ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.CREATIVE, 5F);
            if (tick % 20 == 0) living.hurt(world.damageSources().onFire(), 5F);
            flame(living, rand, x, y, z, w, h, FlameCreator.META_BALEFIRE);
        }

        int blackFire = HbmLivingProps.getBlackFire(living);
        if (blackFire > 0) {
            HbmLivingProps.setBlackFire(living, blackFire - 1);
            if (tick % 10 == 0) fizz(living, rand);
            ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.CREATIVE, 5F);
            if (tick % 10 == 0) living.hurt(world.damageSources().onFire(), 10F);
            flame(living, rand, x, y, z, w, h, FlameCreator.META_BLACK);
        }

        if (fire > 0 || phosphorus > 0 || balefire > 0 || blackFire > 0) {
            if (!living.isAlive()) com.hbm_m.util.confetti.ConfettiUtil.decideConfetti(living, world.damageSources().onFire());
        }
    }

    private static void fizz(LivingEntity living, RandomSource rand) {
        living.level().playSound(null, living.getX(), living.getY() + living.getBbHeight() / 2, living.getZ(),
                SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 1F, 1.5F + rand.nextFloat() * 0.5F);
    }

    private static void flame(LivingEntity living, RandomSource rand, double x, double y, double z, float w, float h, int meta) {
        if (living.level() instanceof ServerLevel sl) {
            FlameCreator.composeEffect(sl, x - w / 2 + w * rand.nextDouble(), y + rand.nextDouble() * h, z - w / 2 + w * rand.nextDouble(), meta);
        }
    }

    /** 1:1 {@code handleDashing}: FSB-Ruestung und Dash-Module (Wolke in der Flasche). */
    static void handleDashing(LivingEntity entity) {
        if (!(entity instanceof Player player)) return;
        HbmPlayerProps props = HbmPlayerProps.get(player);
        props.setDashCount(0);

        int armorDashCount = 0;
        int armorModDashCount = 0;

        if (com.hbm_m.powerarmor.ModArmorFSB.hasFSBArmor(player)) {
            ItemStack plate = player.getItemBySlot(EquipmentSlot.CHEST);
            if (plate.getItem() instanceof com.hbm_m.powerarmor.ModArmorFSB fsb) armorDashCount = fsb.dashCount;
        }

        for (ItemStack armorStack : player.getInventory().armor) {
            if (!armorStack.isEmpty() && armorStack.getItem() instanceof ArmorItem) {
                for (ItemStack mod : com.hbm_m.armormod.util.ArmorModificationHelper.pryMods(armorStack)) {
                    if (mod != null && mod.getItem() instanceof com.hbm_m.armormod.item.IArmorModDash dash) armorModDashCount += dash.getDashes();
                }
            }
        }

        int dashCount = armorDashCount + armorModDashCount;
        boolean dashActivated = props.getKeyPressed(EnumKeybind.DASH);

        if (dashCount * 30 < props.getStamina()) props.setStamina(dashCount * 30);

        if (dashCount > 0) {
            int perDash = 30;
            int stamina = props.getStamina();
            props.setDashCount(dashCount);

            if (props.getDashCooldown() <= 0) {
                if (dashActivated && stamina >= perDash) {
                    Vec3 lookingIn = player.getLookAngle();
                    Vec3 strafeVec = player.getLookAngle().yRot((float) Math.PI * 0.5F);

                    int forward = (int) Math.signum(player.zza);
                    int strafe = (int) Math.signum(player.xxa);
                    if (forward == 0 && strafe == 0) forward = 1;

                    player.push(lookingIn.x * forward + strafeVec.x * strafe, 0, lookingIn.z * forward + strafeVec.z * strafe);
                    Vec3 m = player.getDeltaMovement();
                    player.setDeltaMovement(m.x, 0, m.z);
                    player.fallDistance = 0F;
                    player.playSound(HbmSoundsNT.get("hbm:weapon.rocketFlame"), 1.0F, 1.0F);
                    player.hurtMarked = true;

                    props.setDashCooldown(HbmPlayerProps.dashCooldownLength);
                    stamina -= perDash;
                }
            } else {
                props.setDashCooldown(props.getDashCooldown() - 1);
                props.setKeyPressed(EnumKeybind.DASH, false);
            }

            if (stamina < props.getDashCount() * perDash) {
                stamina++;
                if (stamina % perDash == perDash - 1) {
                    player.playSound(HbmSoundsNT.get("hbm:item.techBoop"), 1.0F, (1.0F + ((1F / 12F) * (stamina / perDash))));
                    stamina++;
                }
            }
            props.setStamina(stamina);
        }
    }

    static void handlePlinking(LivingEntity entity) {
        if (entity instanceof Player player) {
            HbmPlayerProps props = HbmPlayerProps.get(player);
            if (props.plinkCooldown > 0) props.plinkCooldown--;
        }
    }
}
