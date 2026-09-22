package com.hbm_m.block.generic;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.radiation.ChunkRadiationManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Port von {@code YellowBarrel} (1.7.10): das Fass mit hochradioaktivem Abfall.
 *
 * <p>Im Stehen gibt es jede Sekunde 5 RAD in den Chunk ab. Wird es gesprengt, hinterlaesst es eine
 * Wolke aus dichtem Radongas ({@link ModBlocks#GAS_RADON_DENSE}) im Umkreis von fuenf Bloecken,
 * schuettet 35 RAD aus und wird mit einer Wahrscheinlichkeit von 1/3 selbst zu einem Giftblock -
 * alles 1:1 aus {@code explodeEntity}.</p>
 *
 * <p><b>Abweichung:</b> Das Original ist ein {@code BlockDetonatable}: beim Zerstoeren entsteht
 * erst eine scharfgemachte TNT-Kugel mit Zuender, die dann explodiert. Dieses Zuendersystem
 * ({@code EntityTNTPrimedBase}) ist im Port nicht vorhanden, darum zuendet das Fass hier sofort.
 * Die Hitbox ist wie im Original schmal (2/16 bis 14/16 waagerecht, volle Hoehe).</p>
 */
public class YellowBarrelBlock extends Block {

    /** Original: {@code setBlockBounds(2f, 0, 2f, 14f, 1.0F, 14f)} bei {@code f = 1/16}. */
    private static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);

    /** Original: {@code incrementRad(..., 5.0F)} je Tickintervall. */
    private static final float RAD_PER_TICK = 5.0F;
    /** Original: {@code tickRate(world) == 20}. */
    private static final int TICK_RATE = 20;

    /** Original: {@code incrementRad(..., 35)} beim Zuenden. */
    private static final float RAD_ON_BLAST = 35.0F;
    /** Original: die Gaswolke reicht fuenf Bloecke in jede Richtung. */
    private static final int GAS_RADIUS = 5;
    /** Original: {@code world.rand.nextInt(5) == 0} je Stelle. */
    private static final int GAS_CHANCE = 5;
    /** Original: {@code createExplosion(..., 12.0F, true)}. */
    private static final float BLAST_POWER = 12.0F;

    public YellowBarrelBlock(Properties properties) {
        super(properties);
    }

    @Override
    @Deprecated
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    @Deprecated
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // ═══════════════════════════ Dauerstrahlung ═══════════════════════════

    @Override
    @Deprecated
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, TICK_RATE);
        }
    }

    @Override
    @Deprecated
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        ChunkRadiationManager.incrementRad(level, pos.getX(), pos.getY(), pos.getZ(), RAD_PER_TICK);
        level.scheduleTick(pos, this, TICK_RATE);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Original: "townaura" ueber dem Fass - im Port das Myzelpartikel.
        level.addParticle(ParticleTypes.MYCELIUM,
                pos.getX() + random.nextFloat() * 0.5F + 0.25F,
                pos.getY() + 1.1F,
                pos.getZ() + random.nextFloat() * 0.5F + 0.25F,
                0.0D, 0.0D, 0.0D);
    }

    // ═══════════════════════════════ Zuendung ═══════════════════════════════

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        super.wasExploded(level, pos, explosion);
        if (level instanceof ServerLevel serverLevel) {
            detonate(serverLevel, pos);
        }
    }

    /** 1:1 aus {@code explodeEntity}. */
    private static void detonate(ServerLevel level, BlockPos pos) {
        RandomSource random = level.random;

        if (random.nextInt(3) == 0) {
            level.setBlock(pos, ModBlocks.TOXIC_BLOCK.get().defaultBlockState(), 3);
        } else {
            level.explode(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    BLAST_POWER, Level.ExplosionInteraction.BLOCK);
        }

        for (int i = -GAS_RADIUS; i <= GAS_RADIUS; i++) {
            for (int j = -GAS_RADIUS; j <= GAS_RADIUS; j++) {
                for (int k = -GAS_RADIUS; k <= GAS_RADIUS; k++) {
                    BlockPos target = pos.offset(i, j, k);
                    if (random.nextInt(GAS_CHANCE) == 0 && level.getBlockState(target).isAir()) {
                        level.setBlock(target, ModBlocks.GAS_RADON_DENSE.get().defaultBlockState(), 3);
                    }
                }
            }
        }

        ChunkRadiationManager.incrementRad(level, pos.getX(), pos.getY(), pos.getZ(), RAD_ON_BLAST);
    }
}
