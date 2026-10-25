package com.hbm_m.blockentity.generic;

import com.hbm_m.block.generic.BlockGeysir;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.entity.effect.EntityModFX;
import com.hbm_m.entity.projectile.EntityShrapnel;
import com.hbm_m.util.ParticleUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityGeysir}: nur mit Luft darueber. Chlor-Geysir: 20 Ticks Ausbruch mit je drei Agent-Orange-Wolken
 * pro Tick, dann 400-499 Ticks Ruhe. Nether-Geysir: 300/450 Ticks Ausbruch (Splitter und Gasflammen, nur mit Spielern
 * im Umkreis 32), dann 80-139 Ticks Ruhe.
 */
public class GeysirBlockEntity extends BlockEntity {

    private int timer;

    public GeysirBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GEYSIR.get(), pos, state);
    }

    public static void serverTick(Level world, BlockPos pos, BlockState state, GeysirBlockEntity te) {
        if (!world.getBlockState(pos.above()).isAir()) return;
        te.timer--;
        boolean erupting = state.getValue(BlockGeysir.ERUPTING);
        boolean nether = state.getBlock() instanceof BlockGeysir g && g.isNether();

        if (te.timer <= 0) {
            te.timer = getDelay(world, nether, erupting);
            world.setBlock(pos, state.setValue(BlockGeysir.ERUPTING, !erupting), 2);
        }

        if (erupting) {
            if (nether) te.fire(world, pos);
            else te.chlorine(world, pos);
        }
    }

    private static int getDelay(Level world, boolean nether, boolean erupting) {
        var rand = world.random;
        if (!nether) return !erupting ? 20 : 400 + rand.nextInt(100);
        return !erupting ? (rand.nextBoolean() ? 300 : 450) : 80 + rand.nextInt(60);
    }

    private void chlorine(Level world, BlockPos pos) {
        for (int i = 0; i < 3; i++) {
            EntityModFX.Orange fx = new EntityModFX.Orange(world, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 0.0, 0.0, 0.0);
            fx.setDeltaMovement(world.random.nextGaussian() * 0.45, timer * 0.3, world.random.nextGaussian() * 0.45);
            world.addFreshEntity(fx);
        }
    }

    private void fire(Level world, BlockPos pos) {
        int range = 32;
        double cx = pos.getX() + 0.5, cy = pos.getY() + 0.5, cz = pos.getZ() + 0.5;
        if (world.getEntitiesOfClass(Player.class, new AABB(cx, cy, cz, cx, cy, cz).inflate(range)).isEmpty()) return;

        if (world.random.nextInt(3) == 0) {
            EntityShrapnel fx = new EntityShrapnel(world);
            fx.setPos(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5);
            fx.setDeltaMovement(world.random.nextGaussian() * 0.05, 0.5 + world.random.nextDouble() * timer * 0.01, world.random.nextGaussian() * 0.05);
            world.addFreshEntity(fx);
        }

        if (timer % 2 == 0) {
            ParticleUtil.spawnGasFlame(world, pos.getX() + 0.5F, pos.getY() + 1.1F, pos.getZ() + 0.5F,
                    world.random.nextGaussian() * 0.05, 0.2, world.random.nextGaussian() * 0.05);
        }
    }
}
