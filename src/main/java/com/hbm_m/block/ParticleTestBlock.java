package com.hbm_m.block;

import com.hbm_m.particle.nt.MissileContrailNT;
import com.hbm_m.particle.nt.ParticleEngineNT;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * ДИАГНОСТИЧЕСКИЙ блок: непрерывный ВЫСОКИЙ столб частиц вверх.
 *
 * Эмитит ДВА потока из одной точки — для сравнения путей рендера:
 *  - ванильная частица FLAME (ванильный ParticleEngine, ванильный шейдер);
 *  - NT-частица MissileContrailNT (наш ParticleEngineNT / far-pass пайплайн).
 *
 * Если под Iris/DH столбы ведут себя по-разному (один «уезжает», другой нет) —
 * проблема локализована в нашем пайплайне; если оба — в интеграции DH/Iris.
 */
public class ParticleTestBlock extends Block {

    public ParticleTestBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 1.1D;
        double z = pos.getZ() + 0.5D;

        // Контрольный образец: ванильный путь (короткие языки пламени).
        level.addParticle(ParticleTypes.FLAME, x - 0.4D, y, z,
                0.0D, 0.3D, 0.0D);
        level.addParticle(ParticleTypes.FLAME, x + 0.4D, y + 0.3D, z,
                0.0D, 0.35D, 0.0D);

        // Наш путь: готовая NT-частица ракетного выхлопа.
        // ГУСТОЙ ВЫСОКИЙ СТОЛБ: гашение скорости 0.91/тик даёт высоту ~11*vy,
        // т.е. при vy до 24 столб уходит на ~260 блоков вверх и виден издалека.
        if (MissileContrailNT.sprites != null && level instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel) {
            for (int i = 0; i < 10; i++) {
                double ox = (random.nextDouble() - 0.5D) * 5.0D;
                double oz = (random.nextDouble() - 0.5D) * 5.0D;
                double vy = 8.0D + random.nextDouble() * 16.0D;
                float scale = 2.0F + random.nextFloat() * 3.0F;
                ParticleEngineNT.INSTANCE.add(new MissileContrailNT(
                        clientLevel,
                        x + ox, y + random.nextDouble() * 2.0D, z + oz,
                        (random.nextDouble() - 0.5D) * 0.6D,
                        vy,
                        (random.nextDouble() - 0.5D) * 0.6D,
                        scale));
            }
        }
    }
}
