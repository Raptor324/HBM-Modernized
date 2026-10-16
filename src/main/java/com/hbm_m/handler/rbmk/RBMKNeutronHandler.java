package com.hbm_m.handler.rbmk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.blockentity.machines.rbmk.*;
import com.hbm_m.radiation.ChunkRadiationManager;

/**
 * 1:1 port of the CE {@code com.hbm.handler.neutron.RBMKNeutronHandler}.
 *
 * <p>The dials used by the stream maths are read <b>once per server tick</b> into the static fields
 * below (see {@link NeutronNodeWorld#tick}) rather than per stream step, exactly as CE's
 * {@code NeutronHandler.onServerTick} does - a single reactor fires thousands of stream steps a
 * tick and a game-rule lookup per step is measurably slow.</p>
 *
 * <p>Note {@link #columnHeight}: it is the <b>total</b> number of stacked blocks in a column
 * (the raw {@code dialColumnHeight} gamerule), i.e. {@code RBMKDials.getColumnHeight() + 1}, not
 * the block-offset form used everywhere else. Getting this one off by one silently changes how
 * much a solid block shadows a neutron stream.</p>
 */
public class RBMKNeutronHandler {

    // -- Per-tick dial cache (see NeutronNodeWorld.tick) --------------------------
    static double moderatorEfficiency = 1.0;
    static double reflectorEfficiency = 1.0;
    static double absorberEfficiency  = 1.0;
    static double absorberHeatConversion = 0.05;
    static int columnHeight = 4;
    static int fluxRange    = 5;

    public enum RBMKType {
        ROD, MODERATOR, CONTROL_ROD, REFLECTOR, ABSORBER, OUTGASSER, OTHER
    }

    public static RBMKNeutronNode makeNode(NeutronNodeWorld.StreamWorld sw, RBMKColumnBlockEntity tile) {
        BlockPos pos = tile.getBlockPos();
        RBMKNeutronNode existing = sw.getNode(pos);
        return existing != null ? existing : new RBMKNeutronNode(tile, tile.getRBMKType(), tile.hasLid());
    }

    private static BlockEntity blockPosToTE(Level level, BlockPos pos) {
        return level.getBlockEntity(pos);
    }

    // ------------------------------------------------------
    // Node
    // ------------------------------------------------------

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

        /**
         * Every position inside the ReaSim disc of radius {@link #fluxRange} centred on this node.
         * CE walks a square and rejects the corners with {@code x*x + z*z <= range*range}; the port
         * does the same, but collects only the positions that pass instead of emitting nulls.
         */
        private List<BlockPos> getReaSimNodes() {
            List<BlockPos> out = new ArrayList<>();
            for (int x = -fluxRange; x <= fluxRange; x++) {
                for (int z = -fluxRange; z <= fluxRange; z++) {
                    if (x * x + z * z <= fluxRange * fluxRange)
                        out.add(pos.offset(x, 0, z));
                }
            }
            return out;
        }

        /**
         * Returns the positions that should be evicted from the node cache, 1:1 with CE's
         * {@code RBMKNeutronNode.checkNode}. Run periodically (not every tick) so an idle reactor
         * stops holding on to a node per column forever.
         */
        public List<BlockPos> checkNode(NeutronNodeWorld.StreamWorld sw, Level level) {
            List<BlockPos> list = new ArrayList<>();

            // A dead (unfuelled or zero-flux) fuel channel takes its whole downstream path with it.
            if (tile instanceof RBMKRodBlockEntity rod && !rod.isReaSim()) {
                if (!rod.hasRod || rod.lastFluxQuantity == 0) {
                    for (Direction dir : RBMKRodBlockEntity.FLUX_DIRS) {
                        Vec3 vec = new Vec3(dir.getStepX(), 0, dir.getStepZ());
                        for (RBMKNeutronNode n : getNodes(sw, level, pos, vec, false))
                            if (n != null) list.add(n.pos);
                    }
                    return list;
                }
            }

            // Same for a dead ReaSim channel, except its reach is the whole disc.
            if (tile instanceof RBMKRodBlockEntity reaSim && reaSim.isReaSim()) {
                if (!reaSim.hasRod || reaSim.lastFluxQuantity == 0) {
                    list.addAll(getReaSimNodes());
                    return list;
                }
            }

            // Non-rod nodes: keep them only while some live rod is within ReaSim reach ...
            boolean rodInRange = false;
            for (BlockPos nodePos : getReaSimNodes()) {
                RBMKNeutronNode node = sw.getNode(nodePos);
                if (node != null && node.tile instanceof RBMKRodBlockEntity rod
                        && rod.hasRod && rod.lastFluxQuantity > 0) {
                    rodInRange = true;
                    break;
                }
            }
            if (!rodInRange) {
                list.add(pos);
                return list;
            }

            // ... or while a rod sits on one of the four cardinal stream paths.
            for (Direction dir : RBMKRodBlockEntity.FLUX_DIRS) {
                Vec3 vec = new Vec3(dir.getStepX(), 0, dir.getStepZ());
                for (RBMKNeutronNode n : getNodes(sw, level, pos, vec, false))
                    if (n != null && n.tile instanceof RBMKRodBlockEntity) return list;
            }

            list.add(pos);
            return list;
        }
    }

    /**
     * Walks {@link #fluxRange} steps along {@code vector} from {@code origin} and resolves the
     * cached node at each step, optionally creating and caching one for any RBMK column found in
     * the world that was not cached yet. Mirrors CE's {@code RBMKNeutronStream.getNodes}.
     */
    static RBMKNeutronNode[] getNodes(NeutronNodeWorld.StreamWorld sw, Level level,
                                      BlockPos origin, Vec3 vector, boolean addNode) {
        RBMKNeutronNode[] positions = new RBMKNeutronNode[fluxRange];
        for (int i = 1; i <= fluxRange; i++) {
            int x = (int) Math.floor(0.5 + vector.x * i);
            int z = (int) Math.floor(0.5 + vector.z * i);
            BlockPos pos = origin.offset(x, 0, z);

            RBMKNeutronNode node = sw.getNode(pos);
            if (node != null) {
                positions[i - 1] = node;
            } else {
                BlockEntity te = blockPosToTE(level, pos);
                if (te instanceof RBMKColumnBlockEntity rbmk) {
                    node = makeNode(sw, rbmk);
                    positions[i - 1] = node;
                    if (addNode) sw.addNode(node);
                }
            }
        }
        return positions;
    }

    // ------------------------------------------------------
    // Stream
    // ------------------------------------------------------

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

            BlockPos pos = origin.pos;

            // Re-resolve the origin: the column may have been replaced since the stream was queued.
            RBMKColumnBlockEntity originTE;
            RBMKNeutronNode originNode = sw.getNode(pos);
            if (originNode != null) {
                originTE = originNode.tile;
            } else {
                if (!(blockPosToTE(level, pos) instanceof RBMKColumnBlockEntity te)) return;
                originTE = te;
                sw.addNode(new RBMKNeutronNode(te, te.getRBMKType(), te.hasLid()));
            }

            int moderatedCount = 0;

            for (int i = 1; i <= fluxRange; i++) {
                if (fluxQuantity == 0) return;

                int dx = (int) Math.floor(0.5 + vector.x * i);
                int dz = (int) Math.floor(0.5 + vector.z * i);
                BlockPos targetPos = pos.offset(dx, 0, dz);

                RBMKNeutronNode targetNode = sw.getNode(targetPos);
                if (targetNode == null) {
                    BlockEntity te = blockPosToTE(level, targetPos);
                    if (te instanceof RBMKColumnBlockEntity rbmk) {
                        targetNode = makeNode(sw, rbmk);
                        sw.addNode(targetNode);
                    } else {
                        // Not a reactor column: solid blocks shadow the stream, and whatever gets
                        // through irradiates the spot it crossed.
                        int hits = getHits(level, targetPos);
                        if (hits == columnHeight) return;
                        if (hits > 0) {
                            irradiateFromFlux(level, targetPos, hits);
                            fluxQuantity *= 1.0 - ((double) hits / columnHeight);
                            continue;
                        }
                        irradiateFromFlux(level, targetPos, 0);
                        continue;
                    }
                }

                RBMKType type = targetNode.type;
                if (type == RBMKType.OTHER || type == null) continue;

                RBMKColumnBlockEntity nodeTE = targetNode.tile;

                // An open (lidless) column leaks whatever the stream is still carrying.
                if (!targetNode.hasLid)
                    ChunkRadiationManager.incrementRad(level, targetPos.getX(), targetPos.getY(), targetPos.getZ(),
                            (float) (fluxQuantity * 0.05F));

                if (type == RBMKType.MODERATOR || nodeTE.isModerated()) {
                    moderatedCount++;
                    moderateStream();
                }

                if (type == RBMKType.ROD) {
                    RBMKRodBlockEntity rod = (RBMKRodBlockEntity) nodeTE;
                    if (rod.hasRod) { rod.receiveFlux(this); return; }
                } else if (type == RBMKType.OUTGASSER) {
                    RBMKOutgasserBlockEntity outgasser = (RBMKOutgasserBlockEntity) nodeTE;
                    if (outgasser.canProcess()) { outgasser.receiveFlux(this); return; }
                } else if (type == RBMKType.CONTROL_ROD) {
                    RBMKControlBlockEntity rod = (RBMKControlBlockEntity) nodeTE;
                    if (rod.level > 0.0) { fluxQuantity *= rod.getMult(); continue; }
                    return;
                } else if (type == RBMKType.REFLECTOR) {
                    if (originTE.isModerated()) moderatedCount++;
                    if (fluxRatio > 0 && moderatedCount > 0)
                        for (int m = 0; m < moderatedCount; m++) moderateStream();

                    if (reflectorEfficiency != 1.0) { fluxQuantity *= reflectorEfficiency; continue; }
                    if (originTE instanceof RBMKRodBlockEntity rod) rod.receiveFlux(this);
                    return;
                } else if (type == RBMKType.ABSORBER) {
                    nodeTE.heat += absorberHeatConversion * fluxQuantity;
                    if (absorberEfficiency == 1.0) return;
                    fluxQuantity *= absorberEfficiency;
                }
            }

            // Tail: the stream ran out of range without being consumed. Whatever is left has to go
            // somewhere, so it irradiates the far end - and a control rod sitting on the last step
            // gets one more attenuation pass (CE, resolving upstream issue #1933).
            RBMKNeutronNode[] nodes = getNodes(sw, level, pos, vector, true);
            RBMKNeutronNode lastNode = nodes[nodes.length - 1];

            if (lastNode == null) {
                irradiateFromFlux(level, pos.offset((int) vector.x, 0, (int) vector.z));
                return;
            }

            if (lastNode.type == RBMKType.CONTROL_ROD) {
                RBMKControlBlockEntity rod = (RBMKControlBlockEntity) lastNode.tile;
                if (rod.getMult() > 0.0) {
                    fluxQuantity *= rod.getMult();
                    BlockPos posAfter = lastNode.pos.offset((int) vector.x, 0, (int) vector.z);

                    if (sw.getNode(pos) == null) {
                        BlockEntity te = blockPosToTE(level, posAfter);
                        if (te instanceof RBMKColumnBlockEntity rbmk) {
                            sw.addNode(makeNode(sw, rbmk));
                        } else {
                            irradiateFromFlux(level, posAfter);
                        }
                    }
                }
            }
        }

        /** How many of the {@link #columnHeight} blocks in that column are opaque. */
        public int getHits(Level level, BlockPos pos) {
            int hits = 0;
            for (int h = 0; h < columnHeight; h++) {
                BlockPos p = pos.above(h);
                if (level.getBlockState(p).isSolidRender(level, p)) hits++;
            }
            return hits;
        }

        public void irradiateFromFlux(Level level, BlockPos pos) {
            irradiateFromFlux(level, pos, getHits(level, pos));
        }

        public void irradiateFromFlux(Level level, BlockPos pos, int hits) {
            ChunkRadiationManager.incrementRad(level, pos.getX(), pos.getY(), pos.getZ(),
                    (float) (fluxQuantity * 0.05F * (1 - (double) hits / columnHeight)));
        }

        public void moderateStream() {
            fluxRatio *= (1 - moderatorEfficiency);
        }
    }
}
