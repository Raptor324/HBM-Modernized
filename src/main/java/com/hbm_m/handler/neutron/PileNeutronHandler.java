package com.hbm_m.handler.neutron;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.pile.BlockGraphiteDrilledBase;
import com.hbm_m.block.machines.pile.BlockGraphiteNeutronDetector;
import com.hbm_m.block.machines.pile.BlockGraphiteRod;
import com.hbm_m.block.machines.pile.GraphitePileBlocks;
import com.hbm_m.blockentity.machines.pile.BlockEntityPileBase;
import com.hbm_m.blockentity.machines.pile.GraphitePileBlockEntities;
import com.hbm_m.handler.neutron.NeutronNodeWorld.StreamWorld;
import com.hbm_m.handler.neutron.PileNeutronHandler.PileNeutronNode;
import com.hbm_m.handler.neutron.PileNeutronHandler.PileNeutronStream;
import com.hbm_m.interfaces.IPileNeutronReceiver;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.util.ContaminationUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 1:1 port of NEXT {@code com.hbm.handler.neutron.PileNeutronHandler}. Tags moved to the
 * {@code hbm_m} namespace ({@code pile/flux_absorber}, {@code pile/flux_attenuator});
 * {@link PileNeutronStream#runStreamInteraction} is verbatim - the compounding 0.5-step march,
 * ray death at the first BE-less cell, absorber/attenuator stops, the detector pass-through and
 * the per-cell radiation dose.
 *
 * <p>Also hosts the legacy Chicago-Pile-MK1 stream queue from FFA: {@link #addStream} collects
 * the rays cast by {@link GraphitePileBlockEntities.Base} and {@link #tick} marches them after
 * all BE ticks (1:1 FFA {@code runStreamInteraction}).</p>
 */
public class PileNeutronHandler {

    private static final TagKey<Block> FLUX_ABSORBER =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "pile/flux_absorber"));
    private static final TagKey<Block> FLUX_ATTENUATOR =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "pile/flux_attenuator"));
    public static int range = 5;

    public static PileNeutronNode makeNode(StreamWorld streamWorld, BlockEntityPileBase tile) {
        NeutronNode node = streamWorld.getNode(tile.getBlockPos());
        return node instanceof PileNeutronNode pile ? pile : new PileNeutronNode(tile);
    }

    private static @Nullable BlockEntity blockPosToTE(Level level, BlockPos pos) {
        return level.getBlockEntity(pos);
    }

    public static class PileNeutronNode extends NeutronNode {

        public PileNeutronNode(BlockEntityPileBase tile) {
            super(tile, NeutronStream.NeutronType.PILE);
        }
    }

    public static class PileNeutronStream extends NeutronStream {

        public PileNeutronStream(NeutronNode origin, Vec3 vector, double flux) {
            super(origin, vector, flux, 0D, NeutronType.PILE);
        }

        @Override
        public void runStreamInteraction(Level level, StreamWorld streamWorld) {

            BlockEntity originTE = origin.tile;
            BlockPos.MutableBlockPos pos = originTE.getBlockPos().mutable();

            for (float i = 1; i <= range; i += 0.5F) {

                BlockPos nodePos =
                        new BlockPos(
                                (int) Math.floor(pos.getX() + 0.5 + vector.x * i),
                                (int) Math.floor(pos.getY() + 0.5 + vector.y * i),
                                (int) Math.floor(pos.getZ() + 0.5 + vector.z * i));

                if (nodePos.equals(pos)) continue;

                pos.set(nodePos);

                BlockEntity tile;

                NeutronNode node = streamWorld.getNode(nodePos);
                if (node instanceof PileNeutronNode) {
                    tile = node.tile;
                } else {
                    tile = blockPosToTE(level, nodePos);
                    if (tile == null) return;

                    if (tile instanceof BlockEntityPileBase pileTile) {
                        streamWorld.addNode(new PileNeutronNode(pileTile));
                    }
                }

                BlockState state = tile.getBlockState();
                if (!(tile instanceof BlockEntityPileBase)) {

                    if (state.is(FLUX_ABSORBER)) return;
                    else if (state.is(FLUX_ATTENUATOR)) fluxQuantity *= 0.25;

                    if (state.getBlock() instanceof BlockGraphiteRod
                            && !state.getValue(BlockGraphiteDrilledBase.WITHDRAWN)) return;
                }

                if (tile instanceof IPileNeutronReceiver rec) {

                    rec.receiveNeutrons((int) Math.floor(fluxQuantity));

                    if (!(state.getBlock() instanceof BlockGraphiteNeutronDetector)
                            || !state.getValue(BlockGraphiteDrilledBase.WITHDRAWN)) return;
                }

                int x = (int) (nodePos.getX() + 0.5);
                int y = (int) (nodePos.getY() + 0.5);
                int z = (int) (nodePos.getZ() + 0.5);
                List<LivingEntity> entities =
                        level.getEntitiesOfClass(LivingEntity.class, new AABB(x, y, z, x, y, z));

                for (LivingEntity e : entities) {
                    // Modernized ContaminationUtil.contaminate takes a float (NEXT passes a double).
                    ContaminationUtil.contaminate(
                            e,
                            ContaminationUtil.HazardType.RADIATION,
                            ContaminationUtil.ContaminationType.CREATIVE,
                            (float) (fluxQuantity / 4D));
                }
            }
        }
    }

    // ================================================================================================
    // Legacy Chicago Pile MK1 (FFA): queued streams. {@code castRay} only enqueues here; the rays
    // are marched once per server tick from {@link #tick}, driven by MainRegistry's SERVER_POST hook.
    // ================================================================================================

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

            if (tile instanceof com.hbm_m.api.block.IPileNeutronReceiver rec) {
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
