package com.hbm_m.block.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.List;

/**
 * Порт {@code com.hbm.blocks.generic.WasteLeaves} (1.7.10) под 1.20.1.
 *
 * "Мёртвая листва" после ядерного взрыва. audit10: 1:1 wie im Original - immer zufallsgetickt
 * ({@code setTickRandomly(true)}); mit 1/30 verschwindet der Block und laesst, wenn darunter Luft ist,
 * eine fallende Laubschicht ({@code leaves_layer}, ohne Item-Drop) herabsinken; sonst normaler Laubzerfall.
 * Faellt selbst nie als Gegenstand heraus ({@code getItemDropped} null).
 */
public class WasteLeaves extends LeavesBlock {

    public WasteLeaves(Properties properties) {
        super(properties);
    }

    /** Original {@code setTickRandomly(true)}: unabhaengig von Abstand/Persistenz. */
    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(30) == 0) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

            if (level.getBlockState(pos.below()).isAir()) {
                FallingBlockEntity leaves = FallingBlockEntity.fall(level, pos, com.hbm_m.block.ModBlocks.LEAVES_LAYER.get().defaultBlockState());
                leaves.time = 2;
                leaves.dropItem = false;
            }
            return; // Original: super.updateTick auf dem nun leeren Platz bewirkt nichts mehr
        }

        super.randomTick(state, level, pos, random);
    }

    /** Original {@code randomDisplayTick}: mit 1/7 und Luft darunter ein fallendes totes Blatt ("deadleaf"). */
    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource rand) {
        super.animateTick(state, world, pos, rand);

        if (rand.nextInt(7) == 0 && world.getBlockState(pos.below()).isAir()) {
            CompoundTag data = new CompoundTag();
            data.putString("type", "deadleaf");
            data.putDouble("posX", pos.getX() + rand.nextDouble());
            data.putDouble("posY", pos.getY() - 0.05);
            data.putDouble("posZ", pos.getZ() + rand.nextDouble());
            com.hbm_m.particle.helper.ParticleEffectClient.effectNT(data);
        }
    }

    /** Original {@code getItemDropped}: nichts. */
    @Override
    public List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        return Collections.emptyList();
    }
}
