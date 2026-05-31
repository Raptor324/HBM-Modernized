package com.hbm_m.handler;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.mob.EntityCreeperNuclear;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.radiation.ChunkRadiationManager;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.util.ContaminationUtil;
import com.hbm_m.util.ContaminationUtil.ContaminationType;
import com.hbm_m.util.ContaminationUtil.HazardType;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;

/**
 * Эффекты сущностей от радиации (накопление из чанка, трансформации, смерть).
 * Порт {@link com.hbm.handler.EntityEffectHandler} (1.7.10), радиационная часть.
 */
public final class EntityEffectHandler {

    private EntityEffectHandler() {
    }

    public static void onUpdate(LivingEntity entity) {
        if (entity.level().isClientSide()) {
            return;
        }

        if (entity.tickCount % 20 == 0) {
            HbmLivingProps.setRadBuf(entity, HbmLivingProps.getRadEnv(entity));
            HbmLivingProps.setRadEnv(entity, 0F);
        }

        if (!(entity instanceof Player)) {
            handleRadiationEffect(entity);
        }

        // Чанковая доза для всех сущностей, включая игроков (1.7.10: handleRadiationFX → contaminate → radEnv).
        handleRadiationFromChunk(entity);

        handleRadiationFX(entity);
    }

    /** Трансформации и летальные/статусные эффекты от накопленной дозы (как в 1.7.10). */
    private static void handleRadiationEffect(LivingEntity entity) {
        if (!entity.isAlive()) {
            return;
        }

        Level level = entity.level();
        float eRad = HbmLivingProps.getRadiation(entity);

        if (entity.getClass() == Creeper.class && eRad >= 200F && entity.getHealth() > 0F) {
            if (level.random.nextInt(3) == 0) {
                EntityCreeperNuclear creep = ModEntities.ENTITY_MOB_NUCLEAR_CREEPER.get().create(level);
                if (creep != null) {
                    creep.moveTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
                    level.addFreshEntity(creep);
                    entity.discard();
                }
            } else {
                entity.hurt(ModDamageSources.radiation(level), 100F);
            }
            return;
        }

        // EntityDuck → EntityQuackos: пропущено до портирования утки и Quackos (1.7.10 parity)

        if (entity instanceof Cow && !(entity instanceof MushroomCow) && eRad >= 50F) {
            MushroomCow cow = net.minecraft.world.entity.EntityType.MOOSHROOM.create(level);
            if (cow != null) {
                cow.moveTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
                level.addFreshEntity(cow);
                entity.discard();
            }
            return;
        }

        if (entity instanceof Villager && eRad >= 500F) {
            Zombie zomb = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
            if (zomb != null) {
                zomb.moveTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
                level.addFreshEntity(zomb);
                entity.discard();
            }
            return;
        }

        if (eRad < 200F || ContaminationUtil.isRadImmune(entity)) {
            return;
        }
        if (eRad > 2500F) {
            HbmLivingProps.setRadiation(entity, 2500F);
            eRad = 2500F;
        }

        if (eRad >= 1000F) {
            entity.hurt(ModDamageSources.radiation(level), 1000F);
            HbmLivingProps.setRadiation(entity, 0F);

            if (entity.getHealth() > 0F) {
                entity.setHealth(0F);
                entity.die(ModDamageSources.radiation(level));
            }
        } else if (eRad >= 800F) {
            if (level.random.nextInt(300) == 0) {
                entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 5 * 30, 0));
            }
            if (level.random.nextInt(300) == 0) {
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10 * 20, 2));
            }
            if (level.random.nextInt(300) == 0) {
                entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 10 * 20, 2));
            }
            if (level.random.nextInt(500) == 0) {
                entity.addEffect(new MobEffectInstance(MobEffects.POISON, 3 * 20, 2));
            }
            if (level.random.nextInt(700) == 0) {
                entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 3 * 20, 1));
            }
        } else if (eRad >= 600F) {
            if (level.random.nextInt(300) == 0) {
                entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 5 * 30, 0));
            }
            if (level.random.nextInt(300) == 0) {
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10 * 20, 2));
            }
            if (level.random.nextInt(300) == 0) {
                entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 10 * 20, 2));
            }
            if (level.random.nextInt(500) == 0) {
                entity.addEffect(new MobEffectInstance(MobEffects.POISON, 3 * 20, 1));
            }
        } else if (eRad >= 400F) {
            if (level.random.nextInt(300) == 0) {
                entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 5 * 30, 0));
            }
            if (level.random.nextInt(500) == 0) {
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5 * 20, 0));
            }
            if (level.random.nextInt(300) == 0) {
                entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 5 * 20, 1));
            }
        } else if (eRad >= 200F) {
            if (level.random.nextInt(300) == 0) {
                entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 5 * 20, 0));
            }
            if (level.random.nextInt(500) == 0) {
                entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 5 * 20, 0));
            }
        }
    }

    /** Накопление радиации из фона чанка ({@link ChunkRadiationManager}). */
    private static void handleRadiationFromChunk(LivingEntity entity) {
        if (!ModClothConfig.get().enableRadiation || !ModClothConfig.get().enableChunkRads) {
            return;
        }
        if (ContaminationUtil.isRadImmune(entity)) {
            return;
        }

        float rad = ChunkRadiationManager.getRadiation(
                entity.level(),
                entity.getBlockX(),
                entity.getBlockY(),
                entity.getBlockZ()
        );

        if (rad > 0F) {
            ContaminationUtil.contaminate(entity, HazardType.RADIATION, ContaminationType.CREATIVE, rad / 20F);
        }
    }

    /**
     * Частицы рвоты и звук при накопленной дозе. Порт {@code EntityEffectHandler.handleRadiationFX} (1.7.10).
     */
    private static void handleRadiationFX(LivingEntity entity) {
        if (!ModClothConfig.get().enableRadiation) {
            return;
        }

        Level level = entity.level();
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (ContaminationUtil.isRadImmune(entity)) {
            return;
        }

        if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return;
        }

        float eRad = HbmLivingProps.getRadiation(entity);
        long worldTime = level.getGameTime();
        // Фиксированный offset на сущность (как new Random(entity.getEntityId()) в 1.7.10), не world random каждый тик
        RandomSource entityRand = RandomSource.create(entity.getId());
        int r600 = entityRand.nextInt(600);
        int r1200 = entityRand.nextInt(1200);

        if (eRad > 600F) {
            if ((worldTime + r600) % 600 < 20 && canVomit(entity)) {
                sendVomitPacket(serverLevel, entity, "blood", 25);

                if ((worldTime + r600) % 600 == 1) {
                    playVomitSound(level, entity);
                    entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 19));
                }
            }
        } else if (eRad > 200F && (worldTime + r1200) % 1200 < 20 && canVomit(entity)) {
            sendVomitPacket(serverLevel, entity, "normal", 15);

            if ((worldTime + r1200) % 1200 == 1) {
                playVomitSound(level, entity);
                entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 19));
            }
        }
    }

    private static void sendVomitPacket(ServerLevel level, LivingEntity entity, String mode, int count) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("type", "vomit");
        nbt.putString("mode", mode);
        nbt.putInt("count", count);
        nbt.putInt("entity", entity.getId());
        IParticleCreator.sendPacket(level, entity.getX(), entity.getY(), entity.getZ(), 25, nbt);
    }

    private static void playVomitSound(Level level, LivingEntity entity) {
        level.playSound(
                null,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                ModSounds.PLAYER_VOMIT.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
    }

    private static boolean canVomit(LivingEntity entity) {
        return !(entity instanceof WaterAnimal);
    }
}
