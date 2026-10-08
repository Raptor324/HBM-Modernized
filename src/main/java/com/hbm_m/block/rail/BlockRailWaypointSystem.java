package com.hbm_m.block.rail;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.entity.train.EntityRailCarBase;
import com.hbm_m.util.BobMathUtil;
import com.hbm_m.util.ParticleUtil;
import com.hbm_m.util.Vec3NT;

import net.minecraft.world.level.Level;

/**
 * 1:1 {@code BlockRailWaypointSystem}: Schiene, deren Fahrweg aus Wegpunkt-Ketten ({@link RailDef}) besteht
 * (Weichen). Die Debug-Linien ({@code spawnDroneLine}) des Originals bleiben erhalten.
 */
public abstract class BlockRailWaypointSystem extends RailDummyableBlock implements IRailNTM {

    public List<RailDef> railDefs = new ArrayList<>();

    public BlockRailWaypointSystem(Properties properties) {
        super(properties);
    }

    /** Ob der Zug an FROM entlang railDef zum Wegpunkt TO fahren darf; Kernposition wird mitgegeben. */
    public boolean canCross(Level world, int x, int y, int z, Vec3NT from, Vec3NT to, RailDef def) {
        return true;
    }

    @Override
    public Vec3NT getSnappingPos(Level world, int x, int y, int z, double trainX, double trainY, double trainZ) {
        return snapAndMove(world, x, y, z, trainX, trainY, trainZ, 0, 0, 0, 0, new RailContext());
    }

    @Override
    public Vec3NT getTravelLocation(Level world, int x, int y, int z, double trainX, double trainY, double trainZ, double motionX, double motionY, double motionZ, double speed, RailContext info, MoveContext context) {
        return snapAndMove(world, x, y, z, trainX, trainY, trainZ, motionX, motionY, motionZ, speed, info);
    }

    /** Original {@code Pair<Vec3[], RailDef>}. */
    private record Link(Vec3NT[] key, RailDef value) { }

    public Vec3NT snapAndMove(Level world, int x, int y, int z, double trainX, double trainY, double trainZ, double motionX, double motionY, double motionZ, double speed, RailContext info) {
        int[] pos = this.findCore(world, x, y, z);
        if (pos == null) return Vec3NT.createVectorHelper(trainX, trainY, trainZ);
        int cX = pos[0];
        int cY = pos[1];
        int cZ = pos[2];
        int meta = getMeta(world, cX, cY, cZ);
        double moveAngle = Math.atan2(motionX, motionZ) * 180D / Math.PI + 90;
        Vec3NT trainPos = Vec3NT.createVectorHelper(trainX, trainY, trainZ);

        // Wegpunkte in Weltpositionen umrechnen
        Vec3NT train = Vec3NT.createVectorHelper(trainX, trainY, trainZ);
        Vec3NT core = Vec3NT.createVectorHelper(cX + 0.5, cY, cZ + 0.5);
        List<List<Link>> links = new ArrayList<>();

        for (RailDef def : railDefs) {
            List<Link> linkList = new ArrayList<>();
            links.add(linkList);

            for (int i = 0; i < def.nodes.size() - 1; i++) {
                Vec3NT vec1 = getPositionFromNode(world, x, y, z, core, def.nodes.get(i), meta);
                Vec3NT vec2 = getPositionFromNode(world, x, y, z, core, def.nodes.get(i + 1), meta);
                ParticleUtil.spawnDroneLine(world, vec1.xCoord, vec1.yCoord, vec1.zCoord, vec2.xCoord - vec1.xCoord, vec2.yCoord - vec1.yCoord, vec2.zCoord - vec1.zCoord, 0xff0000);
                linkList.add(new Link(new Vec3NT[] {vec1, vec2}, def));
            }
        }

        // naechsten Abschnitt suchen
        Link closest = null;
        Vec3NT startingPos = null;
        /* naechste Kette */
        List<Link> cDef = null;
        double angularDiff;
        double linkAngle;
        double dist = Double.MAX_VALUE;
        /* Richtung */
        boolean d = true;

        for (List<Link> chain : links) {
            for (Link link : chain) {
                Vec3NT[] array = link.key();
                Vec3NT point = getClosestPointOnLink(array[0], array[1], train);

                if (point != null) {
                    Vec3NT delta = point.subtract(train);
                    double length = delta.lengthVector();

                    if (!canCross(world, cX, cY, cZ, trainPos, point, link.value())) continue;

                    linkAngle = EntityRailCarBase.generateYaw(array[1], array[0]);
                    angularDiff = BobMathUtil.angularDifference(linkAngle, -moveAngle);
                    if (angularDiff < -180) { angularDiff += 180; linkAngle += 180; d = false; }
                    if (angularDiff > 0) { angularDiff -= 180; linkAngle -= 180; d = false; }

                    if (length < dist) {
                        closest = link;
                        startingPos = point;
                        cDef = chain;
                        dist = length;
                    }
                }
            }
        }

        if (closest == null) {
            return Vec3NT.createVectorHelper(trainX, trainY, trainZ);
        }

        double distRemaining = speed;
        boolean engaged = false;
        Vec3NT currentPos = startingPos;
        for (int i = d ? 0 : cDef.size() - 1; d ? (i < cDef.size()) : (i >= 0); i += d ? 1 : -1) {

            Link link = cDef.get(i);
            Vec3NT[] array = link.key();

            if (!engaged) {
                if (link == closest) {
                    engaged = true;
                } else {
                    continue;
                }
            }

            Vec3NT nextNode = array[d ? 1 : 0];
            Vec3NT delta = nextNode.subtract(currentPos);

            if (!canCross(world, cX, cY, cZ, currentPos, nextNode, link.value())) break;

            double len = delta.lengthVector();
            if (len >= distRemaining) {
                info.overshoot = 0;
                double newYaw = EntityRailCarBase.generateYaw(nextNode, currentPos);
                if (Math.abs(BobMathUtil.angularDifference(newYaw, moveAngle)) < 45) info.yaw = (float) newYaw;
                else info.yaw = (float) moveAngle;
                delta.normalize(); // Ergebnis verworfen wie im Original
                return Vec3NT.createVectorHelper(currentPos.xCoord - delta.xCoord * distRemaining / len, currentPos.yCoord - delta.yCoord * distRemaining / len, currentPos.zCoord - delta.zCoord * distRemaining / len);
            }

            distRemaining -= len;
            currentPos = nextNode;
        }

        info.overshoot = distRemaining;
        info.pos = IRailNTM.pos(currentPos.xCoord, currentPos.yCoord, currentPos.zCoord);

        return currentPos;
    }

    public Vec3NT getClosestPointOnLink(Vec3NT pointA, Vec3NT pointB, Vec3NT pointP) {
        Vec3NT ap = Vec3NT.createVectorHelper(pointP.xCoord - pointA.xCoord, 0, pointP.zCoord - pointA.zCoord);
        Vec3NT ab = Vec3NT.createVectorHelper(pointB.xCoord - pointA.xCoord, 0, pointB.zCoord - pointA.zCoord);

        double magAB = ab.xCoord * ab.xCoord + ab.zCoord * ab.zCoord;
        double dotProd = ap.xCoord * ab.xCoord + ap.zCoord * ab.zCoord;
        double dist = dotProd / magAB;

        if (dist < 0) return pointA;
        if (dist > 1) return pointB;

        return Vec3NT.createVectorHelper(pointA.xCoord + ab.xCoord * dist, pointA.yCoord + (pointB.yCoord - pointA.yCoord) * dist, pointA.zCoord + ab.zCoord * dist);
    }

    /** Weltposition eines Wegpunkts aus Wegpunkt und Kernposition. */
    public Vec3NT getPositionFromNode(Level world, int x, int y, int z, Vec3NT core, Vec3NT node, int meta) {
        float rotation = 0;
        if (meta == 12) rotation = 90F / 180F * (float) Math.PI;
        if (meta == 14) rotation = 180F / 180F * (float) Math.PI;
        if (meta == 13) rotation = 270F / 180F * (float) Math.PI;
        Vec3NT copy = Vec3NT.createVectorHelper(node.xCoord, node.yCoord, node.zCoord);
        copy.rotateAroundY(rotation);
        return core.addVector(copy.xCoord, copy.yCoord, copy.zCoord);
    }

    public static class RailDef {
        String name;
        public List<Vec3NT> nodes = new ArrayList<>();

        public RailDef(String name) {
            this.name = name;
        }
    }
}
