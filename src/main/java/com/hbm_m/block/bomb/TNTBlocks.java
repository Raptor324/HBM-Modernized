package com.hbm_m.block.bomb;

import javax.annotation.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.generic.FissureBlock;
import com.hbm_m.entity.item.EntityTNTPrimedBase;
import com.hbm_m.explosion.ExplosionNukeSmall;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Die vier {@link BlockTNTBase}-Sorten des Originals. */
public final class TNTBlocks {

    private TNTBlocks() {}

    /** 1:1 {@code BlockDynamite}: Explosion 8. */
    public static class Dynamite extends BlockTNTBase {
        public Dynamite(Properties p) { super(p); }
        @Override public void explodeEntity(Level world, double x, double y, double z, @Nullable EntityTNTPrimedBase entity) {
            world.explode(entity, x, y, z, 8F, Level.ExplosionInteraction.TNT);
        }
    }

    /** 1:1 {@code BlockSemtex}: Explosion 12. */
    public static class Semtex extends BlockTNTBase {
        public Semtex(Properties p) { super(p); }
        @Override public void explodeEntity(Level world, double x, double y, double z, @Nullable EntityTNTPrimedBase entity) {
            world.explode(entity, x, y, z, 12F, Level.ExplosionInteraction.TNT);
        }
    }

    /** 1:1 {@code BlockC4} ({@code c4}): Explosion 15. */
    public static class C4 extends BlockTNTBase {
        public C4(Properties p) { super(p); }
        @Override public void explodeEntity(Level world, double x, double y, double z, @Nullable EntityTNTPrimedBase entity) {
            world.explode(entity, x, y, z, 15F, Level.ExplosionInteraction.TNT);
        }
    }

    /** 1:1 {@code BlockTNT} ({@code tnt_ntm}): Explosion 10. */
    public static class TNT extends BlockTNTBase {
        public TNT(Properties p) { super(p); }
        @Override public void explodeEntity(Level world, double x, double y, double z, @Nullable EntityTNTPrimedBase entity) {
            world.explode(entity, x, y, z, 10F, Level.ExplosionInteraction.TNT);
        }
    }

    /**
     * 1:1 {@code BlockFissureBomb}: mittlere Mini-Atomexplosion, dann im Radius 5 Grundgestein-Erz zu {@code ore_volcano}
     * (im Kraterbiom mit radioaktiver Lava) und Grundgestein-Oel zu Grundgestein.
     */
    public static class FissureBomb extends BlockTNTBase {
        public FissureBomb(Properties p) { super(p); }

        @Override public void explodeEntity(Level world, double x, double y, double z, @Nullable EntityTNTPrimedBase entity) {
            ExplosionNukeSmall.explode(world, x, y, z, ExplosionNukeSmall.PARAMS_MEDIUM);

            int range = 5;
            boolean crater = isCraterBiome(world, BlockPos.containing(x, y, z));
            // ore_bedrock (BlockBedrockOreTE) folgt mit dem Grundgesteinerz-System; bis dahin gibt es hier nichts umzuwandeln
            Block oreBedrock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("hbm_m", "ore_bedrock"));

            for (int i = -range; i <= range; i++) {
                for (int j = -range; j <= range; j++) {
                    for (int k = -range; k <= range; k++) {
                        BlockPos p = BlockPos.containing(Math.floor(x + i), Math.floor(y + j), Math.floor(z + k));
                        BlockState state = world.getBlockState(p);
                        if (oreBedrock != Blocks.AIR && state.is(oreBedrock)) {
                            world.setBlock(p, ModBlocks.ORE_VOLCANO.get().defaultBlockState().setValue(FissureBlock.CRATER, crater), 3);
                        } else if (state.is(ModBlocks.ORE_BEDROCK_OIL.get())) {
                            world.setBlock(p, Blocks.BEDROCK.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
    }

    /** {@code BiomeGenCraterBase}: die Kraterbiome des Originals ({@code crater}, {@code crater_inner}, {@code crater_outer}). */
    public static boolean isCraterBiome(Level world, BlockPos pos) {
        return world.getBiome(pos).unwrapKey().map(k -> k.location().getNamespace().equals("hbm_m") && k.location().getPath().startsWith("crater")).orElse(false);
    }
}
