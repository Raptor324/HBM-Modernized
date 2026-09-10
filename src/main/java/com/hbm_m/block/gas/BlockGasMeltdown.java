package com.hbm_m.block.gas;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.handler.ArmorRegistry;
import com.hbm_m.handler.HazardClass;
import com.hbm_m.radiation.ChunkRadiationManager;
import com.hbm_m.util.ContaminationUtil;
import com.hbm_m.util.ContaminationUtil.ContaminationType;
import com.hbm_m.util.ContaminationUtil.HazardType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-Port von {@code BlockGasMeltdown} (1.7.10): die Wolke ueber einer Kernschmelze. Sie
 * verstrahlt jeden im Inneren ungefiltert, setzt unter freiem Himmel Chunkstrahlung frei und
 * gebiert laufend dichtes Radon. Sehr langlebig (Zerfall 1/350).
 *
 * <p><b>Abweichung:</b> Der zusaetzliche Trank {@code HbmPotion.radiation} des Originals fehlt -
 * dieser Effekt ist im Port nicht vorhanden.</p>
 */
public class BlockGasMeltdown extends BlockGasBase {

    public BlockGasMeltdown() {
        super(0.1F, 0.4F, 0.1F);
    }

    @Override
    protected void affect(LivingEntity living) {
        ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.CREATIVE, 0.5F);

        if (ArmorRegistry.hasProtection(living, 3, HazardClass.PARTICLE_FINE)) {
            damageWornFilter(living);
        } else {
            // Mesotheliom kann auch aus Strahlenbelastung der Lunge entstehen (Originalkommentar).
            HbmLivingProps.incrementAsbestos(living, 5);
        }
    }

    @Override
    protected boolean onGasTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {

        Direction dir = randomAny(random);
        BlockPos target = pos.relative(dir);

        if (random.nextInt(7) == 0 && level.getBlockState(target).isAir()) {
            level.setBlock(target, ModBlocks.GAS_RADON_DENSE.get().defaultBlockState(), 3);
        }

        if (level.canSeeSky(pos)) {
            ChunkRadiationManager.incrementRad(level, pos.getX(), pos.getY(), pos.getZ(), 5F);
        }

        if (random.nextInt(350) == 0) {
            level.removeBlock(pos, false);
            return true;
        }

        return false;
    }

    @Override
    public Direction getFirstDirection(ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(2) == 0) return Direction.UP;
        return Direction.DOWN;
    }

    @Override
    public Direction getSecondDirection(ServerLevel level, BlockPos pos, RandomSource random) {
        return randomHorizontal(random);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        level.addParticle(ParticleTypes.MYCELIUM,
                pos.getX() + random.nextFloat(), pos.getY() + random.nextFloat(), pos.getZ() + random.nextFloat(),
                0.0D, 0.0D, 0.0D);
    }
}
