package com.hbm_m.block.gas;

import com.hbm_m.handler.ArmorRegistry;
import com.hbm_m.handler.HazardClass;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * 1:1-Port von {@code BlockGasClorine} (1.7.10, Tippfehler im Original): Chlorgas. Ohne
 * Vollschutz setzt es gleich fuenf Effekte, mit Schutz nutzt sich nur der Filter ab.
 * Sinkt zu Boden, steigt mit 1/5 auch mal auf, Zerfall 1/10.
 */
public class BlockGasChlorine extends BlockGasBase {

    public BlockGasChlorine() {
        super(0.7F, 0.8F, 0.6F);
    }

    @Override
    protected void affect(LivingEntity living) {
        if (ArmorRegistry.hasProtection(living, 3, HazardClass.GAS_LUNG)) {
            damageWornFilter(living);
            return;
        }

        living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,   5 * 20, 0));
        living.addEffect(new MobEffectInstance(MobEffects.POISON,     20 * 20, 2));
        living.addEffect(new MobEffectInstance(MobEffects.WITHER,      1 * 20, 1));
        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30 * 20, 1));
        living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,      30 * 20, 2));
    }

    @Override
    public Direction getFirstDirection(ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) == 0) return Direction.UP;
        return Direction.DOWN;
    }

    @Override
    public Direction getSecondDirection(ServerLevel level, BlockPos pos, RandomSource random) {
        return randomHorizontal(random);
    }

    @Override
    protected int getDecayChance() {
        return 10;
    }
}
