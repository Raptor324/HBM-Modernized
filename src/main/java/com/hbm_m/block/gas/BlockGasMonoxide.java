package com.hbm_m.block.gas;

import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.handler.ArmorRegistry;
import com.hbm_m.handler.HazardClass;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * 1:1-Port von {@code BlockGasMonoxide} (1.7.10): Kohlenmonoxid. Ohne Atemschutz 1 Schaden je Tick
 * Kontakt, mit Schutz nutzt sich stattdessen der Filter ab. Das Gas ist schwerer als Luft und
 * sinkt zu Boden; es haelt sich lange (Zerfall 1/100).
 */
public class BlockGasMonoxide extends BlockGasBase {

    public BlockGasMonoxide() {
        super(0.1F, 0.1F, 0.1F);
    }

    @Override
    protected void affect(LivingEntity living) {
        if (ArmorRegistry.hasProtection(living, 3, HazardClass.GAS_LUNG)) {
            damageWornFilter(living);
        } else if (living.level() instanceof ServerLevel serverLevel) {
            living.hurt(ModDamageSources.monoxide(serverLevel), 1.0F);
        }
    }

    @Override
    public Direction getFirstDirection(Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) == 0) return Direction.UP;
        return Direction.DOWN;
    }

    @Override
    public Direction getSecondDirection(Level level, BlockPos pos, RandomSource random) {
        return randomHorizontal(random);
    }

    @Override
    protected int getDecayChance() {
        return 100;
    }
}
