package com.hbm_m.handler.ability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.explosion.ExplosionNT;
import com.hbm_m.explosion.ExplosionNT.ExAttrib;
import com.hbm_m.item.tool.ItemToolAbility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** 1:1 {@code com.hbm.handler.ability.IToolAreaAbility}. */
public interface IToolAreaAbility extends IBaseAbility {
    // Should call tool.breakExtraBlock on a bunch of blocks.
    // The initial block is implicitly broken, so don't call breakExtraBlock on it.
    // Returning true skips the reference block from being broken
    boolean onDig(int level, Level world, BlockPos pos, Player player, ItemToolAbility tool);

    // Whether breakExtraBlock is called at all. Currently only false for explosion
    default boolean allowsHarvest(int level) {
        return true;
    }

    int SORT_ORDER_BASE = 0;

    // region handlers
    IToolAreaAbility NONE = new IToolAreaAbility() {
        @Override public String getName() { return ""; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 0; }
        @Override public boolean onDig(int level, Level world, BlockPos pos, Player player, ItemToolAbility tool) { return false; }
    };

    IToolAreaAbility RECURSION = new IToolAreaAbility() {
        @Override public String getName() { return "tool.ability.recursion"; }
        @Override public boolean isAllowed() { return ModClothConfig.get().toolAbilityVein; }

        public final int[] radiusAtLevel = { 3, 4, 5, 6, 7, 9, 10 };

        @Override public int levels() { return radiusAtLevel.length; }
        @Override public String getExtension(int level) { return " (" + radiusAtLevel[level] + ")"; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 1; }

        private final Set<BlockPos> pos = new HashSet<>();

        @Override
        public boolean onDig(int level, Level world, BlockPos p, Player player, ItemToolAbility tool) {
            Block b = world.getBlockState(p).getBlock();

            if (b == Blocks.STONE && !ModClothConfig.get().toolRecursiveStone) {
                return false;
            }

            if (b == Blocks.NETHERRACK && !ModClothConfig.get().toolRecursiveNetherrack) {
                return false;
            }

            pos.clear();

            recurse(world, p, p, player, tool, 0, radiusAtLevel[level]);

            return false;
        }

        private final List<BlockPos> offsets = new ArrayList<>(3 * 3 * 3 - 1) {
            {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx != 0 || dy != 0 || dz != 0) {
                                add(new BlockPos(dx, dy, dz));
                            }
                        }
                    }
                }
            }
        };

        private void recurse(Level world, BlockPos p, BlockPos ref, Player player, ItemToolAbility tool, int depth, int radius) {
            List<BlockPos> shuffledOffsets = new ArrayList<>(offsets);
            Collections.shuffle(shuffledOffsets);

            for (BlockPos offset : shuffledOffsets) {
                breakExtra(world, p.offset(offset), ref, player, tool, depth, radius);
            }
        }

        private void breakExtra(Level world, BlockPos p, BlockPos ref, Player player, ItemToolAbility tool, int depth, int radius) {
            if (pos.contains(p))
                return;

            depth += 1;

            if (depth > ModClothConfig.get().toolRecursionDepth)
                return;

            pos.add(p);

            // don't lose the ref block just yet
            if (p.equals(ref))
                return;

            if (new Vec3(p.getX() - ref.getX(), p.getY() - ref.getY(), p.getZ() - ref.getZ()).length() > radius)
                return;

            BlockState b = world.getBlockState(p);
            BlockState r = world.getBlockState(ref);

            if (!isSameBlock(b.getBlock(), r.getBlock()))
                return;

            // Metadaten-Vergleich des Originals: der Blockzustand ohne den Leucht-Zustand des Redstone-Erzes
            if (!sameMeta(b, r))
                return;

            if (player.getMainHandItem().isEmpty())
                return;

            tool.breakExtraBlock(world, p, player, ref);

            recurse(world, p, ref, player, tool, depth, radius);
        }

        private boolean sameMeta(BlockState a, BlockState b) {
            if (isRedstoneOre(a.getBlock()) && isRedstoneOre(b.getBlock())) return true;
            return a.getBlock() != b.getBlock() || a == b;
        }

        private boolean isRedstoneOre(Block b) {
            return b == Blocks.REDSTONE_ORE || b == Blocks.DEEPSLATE_REDSTONE_ORE;
        }

        private boolean isSameBlock(Block b1, Block b2) {
            // 1.20 hat kein lit_redstone_ore mehr (Zustand LIT), die Gleichsetzung ist damit schon b1 == b2
            return b1 == b2;
        }
    };

    IToolAreaAbility HAMMER = new IToolAreaAbility() {
        @Override public String getName() { return "tool.ability.hammer"; }
        @Override public boolean isAllowed() { return ModClothConfig.get().toolAbilityHammer; }

        public final int[] rangeAtLevel = { 1, 2, 3, 4 };

        @Override public int levels() { return rangeAtLevel.length; }
        @Override public String getExtension(int level) { return " (" + rangeAtLevel[level] + ")"; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 2; }

        @Override
        public boolean onDig(int level, Level world, BlockPos p, Player player, ItemToolAbility tool) {
            int range = rangeAtLevel[level];
            int x = p.getX(), y = p.getY(), z = p.getZ();

            for (int a = x - range; a <= x + range; a++) {
                for (int b = y - range; b <= y + range; b++) {
                    for (int c = z - range; c <= z + range; c++) {
                        if (a == x && b == y && c == z)
                            continue;

                        tool.breakExtraBlock(world, new BlockPos(a, b, c), player, p);
                    }
                }
            }

            return false;
        }
    };

    IToolAreaAbility HAMMER_FLAT = new IToolAreaAbility() {
        @Override public String getName() { return "tool.ability.hammer_flat"; }
        @Override public boolean isAllowed() { return ModClothConfig.get().toolAbilityHammer; }

        public final int[] rangeAtLevel = { 1, 2, 3, 4 };

        @Override public int levels() { return rangeAtLevel.length; }
        @Override public String getExtension(int level) { return " (" + rangeAtLevel[level] + ")"; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 3; }

        @Override
        public boolean onDig(int level, Level world, BlockPos p, Player player, ItemToolAbility tool) {
            int range = rangeAtLevel[level];

            BlockHitResult hit = raytraceFromEntity(world, player, false, 4.5D);
            if (hit == null || hit.getType() != HitResult.Type.BLOCK) return true;
            Direction sideHit = hit.getDirection();

            // we successfully destroyed a block. time to do AOE!
            int xRange = range;
            int yRange = range;
            int zRange = 0;
            switch (sideHit) {
                case DOWN, UP -> { yRange = 0; zRange = range; }
                case NORTH, SOUTH -> { xRange = range; zRange = 0; }
                case WEST, EAST -> { xRange = 0; zRange = range; }
            }

            int x = p.getX(), y = p.getY(), z = p.getZ();
            for (int a = x - xRange; a <= x + xRange; a++) {
                for (int b = y - yRange; b <= y + yRange; b++) {
                    for (int c = z - zRange; c <= z + zRange; c++) {
                        if (a == x && b == y && c == z)
                            continue;

                        tool.breakExtraBlock(world, new BlockPos(a, b, c), player, p);
                    }
                }
            }

            return false;
        }

        // Taken from TConstruct, licensed under CC0 (public domain)
        private BlockHitResult raytraceFromEntity(Level world, Player player, boolean par3, double range) {
            Vec3 vec3 = player.getEyePosition(1.0F);
            double d3 = range;
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                //? if forge {
                d3 = sp.getBlockReach();
                //?}
            }
            Vec3 vec31 = vec3.add(player.getViewVector(1.0F).scale(d3));
            return world.clip(new ClipContext(vec3, vec31, ClipContext.Block.OUTLINE,
                    par3 ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, player));
        }
    };

    IToolAreaAbility EXPLOSION = new IToolAreaAbility() {
        @Override public String getName() { return "tool.ability.explosion"; }
        @Override public boolean isAllowed() { return ModClothConfig.get().toolAbilityExplosion; }

        public final float[] strengthAtLevel = { 2.5F, 5F, 10F, 15F };

        @Override public int levels() { return strengthAtLevel.length; }
        @Override public String getExtension(int level) { return " (" + strengthAtLevel[level] + ")"; }
        @Override public boolean allowsHarvest(int level) { return false; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 4; }

        @Override
        public boolean onDig(int level, Level world, BlockPos p, Player player, ItemToolAbility tool) {
            float strength = strengthAtLevel[level];

            ExplosionNT ex = new ExplosionNT(player.level(), player, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, strength);
            ex.addAttrib(ExAttrib.ALLDROP);
            ex.addAttrib(ExAttrib.NOHURT);
            ex.addAttrib(ExAttrib.NOPARTICLE);
            ex.doExplosionA();
            ex.doExplosionB(false);

            player.level().explode(player, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 0.1F, Level.ExplosionInteraction.NONE);

            return true;
        }
    };
    // endregion handlers

    IToolAreaAbility[] abilities = { NONE, RECURSION, HAMMER, HAMMER_FLAT, EXPLOSION };

    static IToolAreaAbility getByName(String name) {
        for (IToolAreaAbility ability : abilities) {
            if (ability.getName().equals(name))
                return ability;
        }

        return NONE;
    }
}
