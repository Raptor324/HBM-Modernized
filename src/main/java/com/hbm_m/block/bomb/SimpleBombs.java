package com.hbm_m.block.bomb;

import com.hbm_m.api.bomb.IBomb;
import com.hbm_m.entity.effect.EntityEMPBlast;
import com.hbm_m.explosion.ExplosionChaos;
import com.hbm_m.explosion.ExplosionNukeGeneric;
import com.hbm_m.explosion.ExplosionThermo;
import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm_m.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm_m.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm_m.explosion.vanillant.standard.ExplosionEffectTiny;
import com.hbm_m.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm_m.particle.helper.ExplosionCreator;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Einfache Redstone-Bomben des Originals: {@code BombThermo}, {@code BombFloat}, {@code BombFlameWar}. */
public final class SimpleBombs {

    private SimpleBombs() {}

    /** Gemeinsam: Redstone-Signal zuendet ({@code onNeighborBlockChange}). */
    public abstract static class RedstoneBomb extends Block implements IBomb {
        public RedstoneBomb(Properties p) { super(p); }

        @Override
        public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
            if (world.hasNeighborSignal(pos)) explode(world, pos);
        }
    }

    /** 1:1 {@code BombThermo}: {@code therm_endo} vereist (Radius 15, Lebewesen 20), {@code therm_exo} verbrennt; dann Explosion 5. */
    public static class Thermo extends RedstoneBomb {
        public final boolean exo;
        public Thermo(Properties p, boolean exo) { super(p); this.exo = exo; }

        @Override
        public BombReturnCode explode(Level world, BlockPos pos) {
            if (world.isClientSide) return BombReturnCode.DETONATED;
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            world.removeBlock(pos, false);
            if (!exo) {
                ExplosionThermo.freeze(world, x, y, z, 15);
                ExplosionThermo.freezer(world, x, y, z, 20);
            } else {
                ExplosionThermo.scorch(world, x, y, z, 15);
                ExplosionThermo.setEntitiesOnFire(world, x, y, z, 20);
            }
            world.explode(null, x, y, z, 5.0F, Level.ExplosionInteraction.TNT);
            return BombReturnCode.DETONATED;
        }
    }

    /** 1:1 {@code BombFloat}: {@code float_bomb} hebt die Umgebung an, {@code emp_bomb} EMP-Welle Radius 50. */
    public static class Float extends RedstoneBomb {
        public final boolean emp;
        public Float(Properties p, boolean emp) { super(p); this.emp = emp; }

        @Override
        public BombReturnCode explode(Level world, BlockPos pos) {
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            world.playSound(null, x, y, z, HbmSoundsNT.get("weapon.sparkShoot"), SoundSource.BLOCKS, 5.0f, world.random.nextFloat() * 0.2F + 0.9F);
            if (!world.isClientSide) {
                world.removeBlock(pos, false);
                if (!emp) {
                    ExplosionChaos.floater(world, x, y, z, 15, 50);
                    ExplosionChaos.move(world, x, y, z, 15, 0, 50, 0);
                } else {
                    ExplosionNukeGeneric.empBlast(world, x, y, z, 50);
                    EntityEMPBlast wave = new EntityEMPBlast(world, 50);
                    wave.setPos(x + 0.5, y + 0.5, z + 0.5);
                    world.addFreshEntity(wave);
                }
            }
            return BombReturnCode.DETONATED;
        }
    }

    /** 1:1 {@code BombFlameWar}: 150 kleine Explosionen verstreut, dann VNT 15 ohne Drops. */
    public static class FlameWar extends RedstoneBomb {
        public FlameWar(Properties p) { super(p); }

        @Override
        public BombReturnCode explode(Level world, BlockPos pos) {
            if (world instanceof ServerLevel server) {
                int x = pos.getX(), y = pos.getY(), z = pos.getZ();
                world.destroyBlock(pos, false);
                for (int i = 0; i < 150; i++) {
                    ExplosionVNT vnt = new ExplosionVNT(world, x + world.random.nextInt(51) - 25, y + world.random.nextInt(11) - 5, z + world.random.nextInt(51) - 25, 4, null);
                    vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, 25));
                    vnt.setPlayerProcessor(new PlayerProcessorStandard());
                    vnt.setSFX(new ExplosionEffectTiny());
                    vnt.explode();
                }
                ExplosionVNT xnt = new ExplosionVNT(world, x + 0.5, y + 0.5, z + 0.5, 15F);
                xnt.setBlockAllocator(new BlockAllocatorStandard(32));
                xnt.setBlockProcessor(new BlockProcessorStandard().setNoDrop());
                xnt.setEntityProcessor(new EntityProcessorCrossSmooth(2, 200));
                xnt.setPlayerProcessor(new PlayerProcessorStandard());
                xnt.explode();
                ExplosionCreator.composeEffectSmall(server, x + 0.5, y + 0.5, z + 0.5);
            }
            return BombReturnCode.DETONATED;
        }
    }
}
