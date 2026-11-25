package com.hbm_m.explosion;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.entity.effect.EntityFreakyAlienFace;
import com.hbm_m.entity.effect.EntityFreakyUfo;
import com.hbm_m.item.ModItems;
import com.hbm_m.sound.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Joke detonation of the Freaky Alien Block ("+500 Salted"). No 1.7.10 analog - user-requested content, not a port.
 * Replaces the whole explosion ladder: terrain in the blast radius turns into random junk/slime/cheese, the giant
 * translucent face appears in the sky with the bass-boosted theme, levitation 255 for 100 blocks and a rain of pigs.
 */
public final class FreakyAlienExplosion {

    private FreakyAlienExplosion() {}

    private static final RandomSource RAND = RandomSource.create();

    public static void explode(Level level, double x, double y, double z, float dirty) {

        if (level.isClientSide) return;

        // Bass-boosted X-Files theme, hearable far and wide
        level.playSound(null, x, y, z, ModSounds.BOMB_FREAKY_ALIEN.get(), SoundSource.BLOCKS, 100.0F, 1.0F);

        // Junk terrain: everything in the blast radius becomes random trash/slime/cheese
        int r = (int) Math.round(EntityFreakyAlienFace.TERRAIN_RADIUS);
        int rSq = r * r;
        BlockPos origin = BlockPos.containing(x, y, z);
        int minY = Math.max(level.getMinBuildHeight() + 1, origin.getY() - r);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, origin.getY() + r);

        int cheeseDropped = 0;
        final int maxCheeseDrops = 24;

        for (BlockPos pos : BlockPos.betweenClosed(origin.getX() - r, minY, origin.getZ() - r, origin.getX() + r, maxY, origin.getZ() + r)) {
            double dx = pos.getX() + 0.5 - x;
            double dy = pos.getY() + 0.5 - y;
            double dz = pos.getZ() + 0.5 - z;
            if (dx * dx + dy * dy + dz * dz > rSq) continue;
            if (level.isOutsideBuildHeight(pos) || !level.isLoaded(pos)) continue;

            BlockState old = level.getBlockState(pos);
            if (old.isAir() || old.getBlock() instanceof BushBlock) continue;
            if (old.is(Blocks.BEDROCK) || old.getDestroySpeed(level, pos) < 0.0F) continue;
            if (!old.getFluidState().isEmpty() || old.hasBlockEntity()) continue;

            BlockState junk = randomJunk(pos);
            if (junk != null) {
                level.setBlock(pos, junk, 2);
            } else if (cheeseDropped < maxCheeseDrops && RAND.nextFloat() < 0.05F) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                ItemEntity cheese = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        new ItemStack(ModItems.CHEESE.get(), 1 + RAND.nextInt(3)));
                level.addFreshEntity(cheese);
                cheeseDropped++;
            }
        }

        // Primary face (levitation + pig rain) and 1 UFO that flies in, fires a laser beam, and leaves
        EntityFreakyAlienFace face = new EntityFreakyAlienFace(level, x, Math.max(y + 25, 60), z, true);
        level.addFreshEntity(face);
        EntityFreakyUfo ufo = new EntityFreakyUfo(level, x, y, z);
        level.addFreshEntity(ufo);
    }

    /** Weighted junk table: gravel / slime / scrap / dirt / dead bush (null = cheese drop instead). */
    private static BlockState randomJunk(BlockPos pos) {

        float roll = RAND.nextFloat();

        if (roll < 0.30F) return Blocks.GRAVEL.defaultBlockState();
        if (roll < 0.50F) return Blocks.SLIME_BLOCK.defaultBlockState();
        if (roll < 0.65F) return ModBlocks.BLOCK_SCRAP.get().defaultBlockState();
        if (roll < 0.80F) return Blocks.DIRT.defaultBlockState();
        if (roll < 0.95F) return Blocks.DEAD_BUSH.defaultBlockState();
        return null; // cheese rain
    }
}
