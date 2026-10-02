package com.hbm_m.handler.neutron;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.hbm_m.api.block.IPileNeutronReceiver;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.pile.GraphitePileBlocks;
import com.hbm_m.blockentity.machines.pile.GraphitePileBlockEntities;
import com.hbm_m.util.ContaminationUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code PileNeutronHandler} (Chicago Pile MK1): Strahlen werden wie im Original gesammelt und am Ende des
 * Server-Ticks abgearbeitet. Wie im Original rechnet jeder Schritt vom zuletzt getroffenen Block aus weiter
 * ({@code pos.mutate}), die Schritte werden also immer laenger. Jeder Strahl laeuft in halben Schritten bis 5 Bloecke weit, aber nur durch Blockentities -
 * der erste Block ohne Blockentity (auch Luft) beendet ihn, ebenso ein Empfaenger (ausser ein ausgeloester Melder).
 */
public final class PileNeutronHandler {

    private PileNeutronHandler() {}

    public static int range = 5;

    /** {@code concrete_colored} des Originals ist im Port je Farbe ein eigener Block. */
    private static final java.util.Set<String> COLORED = new java.util.HashSet<>();
    static {
        for (String c : new String[] { "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "silver", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black" })
            COLORED.add("concrete_" + c);
    }

    private record Stream(GraphitePileBlockEntities.Base origin, Vec3 vector, double flux) {}

    private static final Map<Level, List<Stream>> STREAMS = new WeakHashMap<>();

    public static void addStream(Level level, GraphitePileBlockEntities.Base origin, Vec3 vector, double flux) {
        STREAMS.computeIfAbsent(level, k -> new ArrayList<>()).add(new Stream(origin, vector, flux));
    }

    /** Aus dem Server-Nachtick fuer jede Welt. */
    public static void tick(Level level) {
        List<Stream> list = STREAMS.get(level);
        if (list == null || list.isEmpty()) return;
        List<Stream> run = new ArrayList<>(list);
        list.clear();
        for (Stream s : run) runStreamInteraction(level, s);
    }

    private static void runStreamInteraction(Level world, Stream stream) {
        double fluxQuantity = stream.flux();
        Vec3 vector = stream.vector();
        BlockPos origin = stream.origin().getBlockPos();
        BlockPos.MutableBlockPos pos = origin.mutable();

        for (float i = 1; i <= range; i += 0.5F) {
            BlockPos nodePos = new BlockPos(
                    (int) Math.floor(pos.getX() + 0.5 + vector.x * i),
                    (int) Math.floor(pos.getY() + 0.5 + vector.y * i),
                    (int) Math.floor(pos.getZ() + 0.5 + vector.z * i));

            if (nodePos.equals(pos)) continue;
            pos.set(nodePos);

            BlockEntity tile = world.getBlockEntity(nodePos);
            if (tile == null) return;

            Block block = tile.getBlockState().getBlock();
            int meta = GraphitePileBlocks.meta(tile.getBlockState());
            // Im Original unerreichbar (diese Bloecke haben kein Blockentity, der Strahl endete schon oben) - 1:1 behalten
            if (!(tile instanceof GraphitePileBlockEntities.Base)) {
                String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getPath();
                if (id.equals("block_boron")) return;
                else if (block == ModBlocks.CONCRETE.get() || block == ModBlocks.CONCRETE_SMOOTH.get() || block == ModBlocks.CONCRETE_ASBESTOS.get()
                        || COLORED.contains(id) || block == ModBlocks.BRICK_CONCRETE.get())
                    fluxQuantity *= 0.25;
                if (block == ModBlocks.BLOCK_GRAPHITE_ROD.get() && (meta & 8) == 0) return;
            }

            if (tile instanceof IPileNeutronReceiver rec) {
                rec.receiveNeutrons((int) Math.floor(fluxQuantity));
                if (block != ModBlocks.BLOCK_GRAPHITE_DETECTOR.get() || (meta & 8) == 0) return;
            }

            int x = (int) (nodePos.getX() + 0.5);
            int y = (int) (nodePos.getY() + 0.5);
            int z = (int) (nodePos.getZ() + 0.5);
            for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, new AABB(x, y, z, x, y, z)))
                ContaminationUtil.contaminate(e, ContaminationUtil.HazardType.RADIATION, ContaminationUtil.ContaminationType.CREATIVE, (float) (fluxQuantity / 4D));
        }
    }
}
