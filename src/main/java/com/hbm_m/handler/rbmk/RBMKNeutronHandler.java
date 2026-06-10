package com.hbm_m.handler.rbmk;

import com.hbm_m.block.entity.machines.rbmk.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class RBMKNeutronHandler {

    public enum RBMKType {
        ROD, MODERATOR, CONTROL_ROD, REFLECTOR, ABSORBER, OUTGASSER, OTHER
    }

    public static RBMKNeutronNode makeNode(NeutronNodeWorld.StreamWorld sw, RBMKColumnBlockEntity tile) {
        BlockPos pos = tile.getBlockPos();
        RBMKNeutronNode existing = sw.getNode(pos);
        return existing != null ? existing : new RBMKNeutronNode(tile, tile.getRBMKType(), tile.hasLid());
    }

    // ──────────────────────────────────────────────────────
    // Node
    // ──────────────────────────────────────────────────────

    public static class RBMKNeutronNode {

        public final RBMKColumnBlockEntity tile;
        public final BlockPos pos;
        public final RBMKType type;
        public boolean hasLid;

        public RBMKNeutronNode(RBMKColumnBlockEntity tile, RBMKType type, boolean hasLid) {
            this.tile = tile;
            this.pos  = tile.getBlockPos().immutable();
            this.type = type;
            this.hasLid = hasLid;
        }

        /** Returns a list of positions that should be evicted from the node cache. */
        public List<BlockPos> checkNode(NeutronNodeWorld.StreamWorld sw, Level level) {
            List<BlockPos> toRemove = new ArrayList<>();

            if (type == RBMKType.OTHER || type == null) {
                toRemove.add(pos);
                return toRemove;
            }

            if (type == RBMKType.ROD) {
                RBMKRodBlockEntity rod = (RBMKRodBlockEntity) tile;
                if (!rod.hasRod || rod.lastFluxQuantity == 0) {
                    for (Direction dir : RBMKRodBlockEntity.FLUX_DIRS) {
                        for (int i = 1; i <= RBMKDials.getFluxRange(level); i++) {
                            BlockPos p = pos.offset(dir.getStepX() * i, 0, dir.getStepZ() * i);
                            RBMKNeutronNode n = sw.getNode(p);
                            if (n != null) toRemove.add(p);
                        }
                    }
                    return toRemove;
                }
            }

            return toRemove;
        }
    }

    // ──────────────────────────────────────────────────────
    // Stream
    // ──────────────────────────────────────────────────────

    public static class RBMKNeutronStream {

        public final RBMKNeutronNode origin;
        public final Vec3 vector;
        public double fluxQuantity;
        public double fluxRatio;

        public RBMKNeutronStream(RBMKNeutronNode origin, Vec3 vector, double flux, double ratio) {
            this.origin = origin;
            this.vector = vector;
            this.fluxQuantity = flux;
            this.fluxRatio = ratio;
        }

        public void runStreamInteraction(Level level, NeutronNodeWorld.StreamWorld sw) {
            if (fluxQuantity == 0) return;

            int columnHeight = RBMKDials.getColumnHeight(level);
            int fluxRange    = RBMKDials.getFluxRange(level);
            double moderatorEff  = RBMKDials.getModeratorEfficiency(level);
            double reflectorEff  = RBMKDials.getReflectorEfficiency(level);
            double absorberEff   = RBMKDials.getAbsorberEfficiency(level);
            double absorberHeat  = RBMKDials.getAbsorberHeatConversion(level);

            int moderatedCount = 0;

            for (int i = 1; i <= fluxRange; i++) {
                if (fluxQuantity == 0) return;

                int dx = (int) Math.floor(0.5 + vector.x * i);
                int dz = (int) Math.floor(0.5 + vector.z * i);
                BlockPos targetPos = origin.pos.offset(dx, 0, dz);

                RBMKNeutronNode targetNode = sw.getNode(targetPos);
                if (targetNode == null) {
                    BlockEntity te = level.getBlockEntity(targetPos);
                    if (te instanceof RBMKColumnBlockEntity rbmk) {
                        targetNode = makeNode(sw, rbmk);
                        sw.addNode(targetNode);
                    } else {
                        // Check solid block coverage
                        int hits = getHits(level, targetPos, columnHeight);
                        if (hits >= columnHeight) return;
                        if (hits > 0) fluxQuantity *= 1.0 - ((double) hits / columnHeight);
                        continue;
                    }
                }

                RBMKType type = targetNode.type;
                if (type == RBMKType.OTHER || type == null) continue;

                RBMKColumnBlockEntity nodeTE = targetNode.tile;

                if (type == RBMKType.MODERATOR || nodeTE.isModerated()) {
                    moderatedCount++;
                    fluxRatio *= (1.0 - moderatorEff);
                }

                if (type == RBMKType.ROD) {
                    RBMKRodBlockEntity rod = (RBMKRodBlockEntity) nodeTE;
                    if (rod.hasRod) {
                        rod.receiveFlux(this);
                        return;
                    }
                } else if (type == RBMKType.OUTGASSER) {
                    RBMKOutgasserBlockEntity outgasser = (RBMKOutgasserBlockEntity) nodeTE;
                    if (outgasser.canProcess()) {
                        outgasser.receiveFlux(this);
                        return;
                    }
                } else if (type == RBMKType.CONTROL_ROD) {
                    RBMKControlBlockEntity rod = (RBMKControlBlockEntity) nodeTE;
                    if (rod.level > 0.0) {
                        fluxQuantity *= rod.getMult();
                        continue;
                    }
                    return;
                } else if (type == RBMKType.REFLECTOR) {
                    if (origin.tile.isModerated()) moderatedCount++;
                    if (fluxRatio > 0 && moderatedCount > 0)
                        for (int m = 0; m < moderatedCount; m++) fluxRatio *= (1.0 - moderatorEff);

                    if (reflectorEff != 1.0) {
                        fluxQuantity *= reflectorEff;
                        continue;
                    }
                    if (origin.tile instanceof RBMKRodBlockEntity rod) {
                        rod.receiveFlux(this);
                    }
                    return;
                } else if (type == RBMKType.ABSORBER) {
                    nodeTE.heat += absorberHeat * fluxQuantity;
                    if (absorberEff == 1.0) return;
                    fluxQuantity *= absorberEff;
                }
            }
        }

        private static int getHits(Level level, BlockPos pos, int columnHeight) {
            int hits = 0;
            for (int h = 0; h < columnHeight; h++) {
                if (level.getBlockState(pos.above(h)).isRedstoneConductor(level, pos.above(h))) hits++;
            }
            return hits;
        }
    }
}
