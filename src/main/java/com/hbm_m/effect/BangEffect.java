package com.hbm_m.effect;

import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.item.ModItems;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;

/**
 * 1:1-Port von {@code HbmPotion.bang} (1.7.10): kurz nach dem Setzen zerlegt es den Traeger.
 * Kuehe hinterlassen dabei Kaese.
 *
 * <p>Abweichung: den Klang {@code hbm:weapon.laserBang} gibt es im Port noch nicht, hier steht
 * ersatzweise der Explosionsklang von Minecraft.</p>
 */
public class BangEffect extends HbmEffect {

    public BangEffect() {
        super(MobEffectCategory.HARMFUL, 0x111111, 3, 0);
    }

    /** Original: {@code return par1 <= 10;} - schlaegt erst kurz vor Ablauf zu. */
    @Override
    protected boolean isReady(int duration, int amplifier) {
        return duration <= 10;
    }

    @Override
    protected void tick(@NotNull LivingEntity entity, int amplifier) {
        // Kaese faellt, solange die Kuh noch lebt.
        if (entity instanceof Cow cow) {
            int toDrop = cow.isBaby() ? 10 : 3;
            cow.spawnAtLocation(new ItemStack(ModItems.CHEESE.get(), toDrop), 1.0F);
        }

        entity.hurt(ModDamageSources.bang(entity.level()), 1000);
        entity.setHealth(0.0F);

        if (!(entity instanceof Player)) {
            entity.discard();
        }

        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 100.0F, 1.0F);

        // Original: ExplosionLarge.spawnParticles(..., 10) - zehn "largeexplode"-Partikel.
        if (entity.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    entity.getX(), entity.getY(), entity.getZ(), 10, 0.5D, 0.5D, 0.5D, 0.0D);
        }
    }
}
