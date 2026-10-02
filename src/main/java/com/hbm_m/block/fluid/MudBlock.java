package com.hbm_m.block.fluid;

import java.util.function.Supplier;

import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.util.ArmorUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * 1:1-Port von {@code MudBlock} (giftiger Schlamm): Netz-Bremse, 8 Schaden pro Beruehrung ohne
 * Vollschutzanzug, loescht benachbarte fremde Fluessigkeiten und zersetzt pro Fliess-Tick die
 * Umgebung - Stein zu Bruchstein zu Kies, Sandstein zu Sand, Terrakotta zu Ton, und alles
 * Weiche (Holz, Pflanzen, Wolle, Glas, Eis, Schnee ... bzw. Widerstand unter 1.2) verschwindet.
 */
public class MudBlock extends HbmFluidBlock {

    public MudBlock(Supplier<? extends FlowingFluid> fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    public Boolean canDisplace(BlockGetter level, BlockPos pos, BlockState target) {
        if (!target.getFluidState().isEmpty()) return false;
        return null;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        setInWeb(entity, state);
        if (entity instanceof Player p && ArmorUtil.checkForHazmat(p)) return;
        entity.hurt(ModDamageSources.mudPoisoning(level), 8);
    }

    @Override
    public void onFluidTick(Level level, BlockPos pos, BlockState state) {
        for (Direction d : Direction.values()) reactToBlocks2(level, pos.relative(d));
    }

    @Override
    protected void onNeighborChange(Level level, BlockPos pos, BlockState state) {
        for (Direction d : Direction.values()) reactToBlocks(level, pos.relative(d));
    }

    private boolean isMud(BlockState s) {
        return s.getBlock() == this;
    }

    public void reactToBlocks(Level level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (!isMud(s) && !s.getFluidState().isEmpty()) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }

    public void reactToBlocks2(Level level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (isMud(s) || s.isAir()) return;
        var b = s.getBlock();
        var rand = level.random;

        if (b == Blocks.STONE || b == Blocks.STONE_BRICK_STAIRS || b == Blocks.STONE_BRICKS || b == Blocks.STONE_SLAB || b == Blocks.SMOOTH_STONE_SLAB || b == Blocks.STONE_BRICK_SLAB) {
            if (rand.nextInt(20) == 0) level.setBlockAndUpdate(pos, Blocks.COBBLESTONE.defaultBlockState());
        } else if (b == Blocks.COBBLESTONE) {
            if (rand.nextInt(15) == 0) level.setBlockAndUpdate(pos, Blocks.GRAVEL.defaultBlockState());
        } else if (b == Blocks.SANDSTONE) {
            if (rand.nextInt(5) == 0) level.setBlockAndUpdate(pos, Blocks.SAND.defaultBlockState());
        } else if (s.is(BlockTags.TERRACOTTA)) {
            if (rand.nextInt(10) == 0) level.setBlockAndUpdate(pos, Blocks.CLAY.defaultBlockState());
        } else if (isSoftMaterial(s)) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        } else if (b.getExplosionResistance() < 1.2F) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    /**
     * Die Materialliste des Originals (wood, cactus, cake, circuits, cloth, coral, craftedSnow,
     * glass, gourd, ice, leaves, packedIce, piston, plants, portal, redstoneLight, snow, sponge,
     * vine, web) - in 1.20 gibt es keine Materialien mehr, darum ueber Tags/Eigenschaften.
     */
    public static boolean isSoftMaterial(BlockState s) {
        var b = s.getBlock();
        SoundType st = s.getSoundType();
        return s.is(BlockTags.LOGS) || s.is(BlockTags.PLANKS) || s.is(BlockTags.WOODEN_SLABS) || s.is(BlockTags.WOODEN_STAIRS)
                || s.is(BlockTags.WOODEN_FENCES) || s.is(BlockTags.WOODEN_DOORS) || s.is(BlockTags.WOODEN_TRAPDOORS)
                || st == SoundType.WOOD || b == Blocks.CACTUS || b == Blocks.CAKE || s.is(BlockTags.WOOL) || s.is(BlockTags.WOOL_CARPETS)
                || s.is(BlockTags.CORAL_BLOCKS) || s.is(BlockTags.CORALS) || b == Blocks.SNOW_BLOCK || b == Blocks.SNOW
                || st == SoundType.GLASS || b == Blocks.PUMPKIN || b == Blocks.MELON || s.is(BlockTags.ICE)
                || s.is(BlockTags.LEAVES)
                || b == Blocks.PISTON || b == Blocks.STICKY_PISTON || b == Blocks.PISTON_HEAD
                || s.is(BlockTags.REPLACEABLE_BY_TREES) || s.is(BlockTags.FLOWERS) || s.is(BlockTags.SAPLINGS) || s.is(BlockTags.CROPS)
                || b == Blocks.NETHER_PORTAL || b == Blocks.REDSTONE_LAMP || b == Blocks.SPONGE || b == Blocks.WET_SPONGE
                || b == Blocks.VINE || b == Blocks.COBWEB || b == Blocks.REDSTONE_WIRE || b == Blocks.REPEATER || b == Blocks.COMPARATOR
                || b == Blocks.LEVER || s.is(BlockTags.BUTTONS) || s.is(BlockTags.RAILS) || b == Blocks.TORCH || b == Blocks.WALL_TORCH
                || b == Blocks.REDSTONE_TORCH || b == Blocks.REDSTONE_WALL_TORCH;
    }
}
