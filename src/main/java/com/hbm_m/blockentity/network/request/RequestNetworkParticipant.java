package com.hbm_m.blockentity.network.request;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import com.hbm_m.blockentity.network.request.RequestNetwork.PathNode;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Port des Netzwerk-Teils von {@code TileEntityRequestNetwork} (1.7.10 Original, Pipeline B) als
 * eigenstaendige Komponente statt gemeinsamer Basisklasse - da Waypoint-Request keine Inventar-
 * Basisklasse braucht, Provider/Requester/Dock aber alle {@code BaseMachineBlockEntity} erweitern
 * muessen (Java-Einfachvererbung), wird die Netzwerklogik hier per Komposition bereitgestellt, exakt
 * wie {@link com.hbm_m.inventory.filter.ModulePatternMatcher} oder {@code UpgradeManager} in diesem
 * Port bereits als eigenstaendige Helfer-Klassen implementiert sind.
 */
public class RequestNetworkParticipant {

    public static final int MAX_RANGE = 24;

    public final Set<PathNode> reachableNodes = new HashSet<>();
    public final Set<PathNode> knownNodes = new HashSet<>();

    private final Function<BlockPos, PathNode> nodeFactory;

    public RequestNetworkParticipant(Function<BlockPos, PathNode> nodeFactory) {
        this.nodeFactory = nodeFactory;
    }

    /** Call once per second (caller gates on {@code level.getGameTime() % 20 == 0}). */
    public void tick(Level level, BlockPos pos, boolean poweredByRedstone) {
        RequestNetwork.updateEntries();

        PathNode newNode = nodeFactory.apply(pos);
        newNode.active = !poweredByRedstone;
        RequestNetwork.push(level, newNode);

        Set<PathNode> localNodes = RequestNetwork.getAllLocalNodes(level, pos.getX(), pos.getZ(), 2);
        localNodes.remove(newNode);

        knownNodes.removeIf(node -> {
            if (!localNodes.contains(node)) {
                reachableNodes.remove(node);
                return true;
            }
            return false;
        });

        for (PathNode known : new HashSet<>(knownNodes)) {
            if (!hasPath(level, pos, known.pos)) {
                reachableNodes.remove(known);
            } else {
                reachableNodes.add(known);
            }
        }

        int newNodeLimit = 5;
        for (PathNode node : localNodes) {
            if (!areNodesConnectable(node, newNode)) continue;
            if (knownNodes.contains(node) || node.equals(newNode)) continue;

            newNodeLimit--;
            knownNodes.add(node);
            if (hasPath(level, pos, node.pos)) reachableNodes.add(node);

            if (newNodeLimit <= 0) break;
        }
    }

    public static boolean areNodesConnectable(PathNode a, PathNode b) {
        return a.torchWaypoint || b.torchWaypoint;
    }

    /** Bidirectional line-of-sight raytrace, mirroring the original's double-check workaround. */
    public static boolean hasPath(Level level, BlockPos pos1, BlockPos pos2) {
        Vec3 vec1 = new Vec3(pos1.getX() + 0.5, pos1.getY() + 0.5, pos1.getZ() + 0.5);
        Vec3 vec2 = new Vec3(pos2.getX() + 0.5, pos2.getY() + 0.5, pos2.getZ() + 0.5);
        if (vec1.distanceTo(vec2) > MAX_RANGE) return false;

        HitResult hit1 = level.clip(new ClipContext(vec1, vec2, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
        HitResult hit2 = level.clip(new ClipContext(vec2, vec1, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
        return hit1.getType() == HitResult.Type.MISS && hit2.getType() == HitResult.Type.MISS;
    }
}
