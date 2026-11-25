package com.hbm_m.client.render.implementations;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.api.fluids.IPipelineBase;
import com.hbm_m.blockentity.network.PipeAnchorBlockEntity;
import com.hbm_m.client.render.HbmBerBounds;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.culling.OcclusionCullingHelper;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 1:1 {@code RenderPipeAnchor}: Anker nach Anbringseite gedreht, Leitung (in aufgehellter Fluessigkeitsfarbe) nur vom "dominanten" Anker.
 *  <p>Optimiert gegenueber dem 1:1-Port: Segment-Posen/Farben werden pro Anker gecacht (nur Verbindungsliste, Fluessigkeit
 *  und Chunk-Ladezustand der Gegenanker aendern sie), und Koerper wie Segmente werden frustum-/okklusionsgetestet,
 *  statt mit unendlichen Render-Bounds jeden Anker im 256-Block-Radius voll zu emittieren. */
public class PipeAnchorRenderer<T extends BlockEntity & IPipelineBase> implements HbmBerBounds<T> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/network/pipe_anchor.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/pipe_anchor_model.png");
    public static final ResourceLocation TEX_EXHAUST = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/pipe_anchor_exhaust_model.png");
    public static final ResourceLocation TEX_PNEUMATIC = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/pipe_anchor_pneumatic_model.png");

    private final ResourceLocation texture;

    public PipeAnchorRenderer(BlockEntityRendererProvider.Context ctx) { this(ctx, TEX); }

    public PipeAnchorRenderer(BlockEntityRendererProvider.Context ctx, ResourceLocation texture) { this.texture = texture; }

    /** Eine gezeichnete Leitung: vorgerechnete Pose + Faerbung + weltfeste AABB fuer den Frustum-Test. */
    private static final class Segment {
        final float rotY, rotX;
        final double length;
        final float r, g, b;
        final AABB bounds;

        Segment(float rotY, float rotX, double length, float r, float g, float b, AABB bounds) {
            this.rotY = rotY; this.rotX = rotX;
            this.length = length; this.r = r; this.g = g; this.b = b; this.bounds = bounds;
        }
    }

    private static final BlockPos[] NO_CONNECTIONS = {};
    private static final boolean[] NO_RESOLVED = {};

    private static final class SegmentCache {
        BlockPos[] connections = NO_CONNECTIONS;
        boolean[] resolved = NO_RESOLVED; // Gegenanker geladen + gleiches Netz (Dominanz wird beim Rebuild entschieden)
        Fluid fluid;
        final List<Segment> segments = new ArrayList<>();
    }

    private final WeakHashMap<T, SegmentCache> caches = new WeakHashMap<>();

    @Override
    public void render(T te, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(texture));
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);

        AABB bodyBounds = bodyBounds(te.getBlockPos());
        // Frustum immer (billig), Okklusion nur im CPU-Modus - der Helper cached pro Anker.
        if (OcclusionCullingHelper.passesFrustum(bodyBounds) && OcclusionCullingHelper.shouldRender(te, bodyBounds)) {
            ps.pushPose();
            switch (te.getBlockState().getValue(BlockStateProperties.FACING)) {
                case DOWN -> ps.mulPose(Axis.XP.rotationDegrees(180));
                case UP -> { }
                case NORTH -> { ps.mulPose(Axis.XP.rotationDegrees(90)); ps.mulPose(Axis.ZP.rotationDegrees(180)); }
                case SOUTH -> ps.mulPose(Axis.XP.rotationDegrees(90));
                case WEST -> { ps.mulPose(Axis.XP.rotationDegrees(90)); ps.mulPose(Axis.ZP.rotationDegrees(90)); }
                case EAST -> { ps.mulPose(Axis.XP.rotationDegrees(90)); ps.mulPose(Axis.ZP.rotationDegrees(270)); }
            }
            ps.translate(0, -0.5F, 0);
            MODEL.renderPart("Anchor", ps, vc, light);
            ps.popPose();
        }

        SegmentCache cache = caches.computeIfAbsent(te, k -> new SegmentCache());
        if (!matches(te, cache)) rebuild(te, cache);

        for (Segment s : cache.segments) {
            // Nur Frustum: ein Occlusion-Test ueber den Cache-Schluessel des Gegenankers wuerde mit dessen
            // Koerper-Test kollidieren (gleicher BlockPos, andere AABB → falsch gecachte Ergebnisse).
            if (!OcclusionCullingHelper.passesFrustum(s.bounds)) continue;

            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(s.rotY));
            ps.mulPose(Axis.XP.rotationDegrees(s.rotX));

            ps.pushPose();
            ps.scale(1, (float) s.length, 1);
            ps.translate(0, -0.5, 0);
            MODEL.renderPartTinted("Pipe", ps, vc, light, s.r, s.g, s.b);
            ps.popPose();

            ps.pushPose();
            ps.translate(0, s.length / 2D - 1.5, 0);
            MODEL.renderPart("Ring", ps, vc, light);
            ps.popPose();

            ps.popPose();
        }
        ps.popPose();
    }

    /** Cache noch aktuell? Verbindungen, Gegenanker-Ladezustand und Fluessigkeit sind der komplette Input der Geometrie. */
    private boolean matches(T te, SegmentCache cache) {
        List<BlockPos> con = te.getConnected();
        if (cache.connections.length != con.size() || cache.fluid != te.getPipelineFluid()) return false;
        var level = te.getLevel();
        for (int i = 0; i < con.size(); i++) {
            BlockPos p = con.get(i);
            if (!cache.connections[i].equals(p)) return false;
            BlockEntity tile = level == null ? null : level.getBlockEntity(p);
            boolean ok = tile instanceof IPipelineBase ip && ip.getNetworkType() == te.getNetworkType();
            if (ok != cache.resolved[i]) return false;
        }
        return true;
    }

    private void rebuild(T te, SegmentCache cache) {
        List<BlockPos> con = te.getConnected();
        cache.connections = con.toArray(new BlockPos[0]);
        cache.resolved = new boolean[con.size()];
        cache.fluid = te.getPipelineFluid();
        var level = te.getLevel();
        cache.segments.clear();

        for (int i = 0; i < con.size(); i++) {
            BlockPos p = con.get(i);
            BlockEntity tile = level == null ? null : level.getBlockEntity(p);
            if (!(tile instanceof IPipelineBase other) || other.getNetworkType() != te.getNetworkType()) {
                cache.resolved[i] = false;
                continue;
            }
            cache.resolved[i] = true;
            Vec3 a = te.getConnectionPoint(), b = other.getConnectionPoint();
            if (!isDominant(a, b)) continue;
            double dX = b.x - a.x, dY = b.y - a.y, dZ = b.z - a.z;
            double hyp = Math.sqrt(dX * dX + dZ * dZ);
            float rotY = (float) Math.toDegrees(Math.atan2(dX, dZ));
            float rotX = (float) (90 - Math.toDegrees(Math.atan2(dY, hyp)));
            double length = Math.sqrt(dX * dX + dY * dY + dZ * dZ);

            // Original: nur der Fluessigkeits-Anker faerbt die Leitung, alle anderen bleiben weiss
            Fluid f = te instanceof PipeAnchorBlockEntity fa ? (fa.getFluidType() == Fluids.EMPTY ? ModFluids.NONE.getSource() : fa.getFluidType()) : null;
            int c = f == null ? 0xFFFFFF : HbmFluidRegistry.getTintColor(f);
            float r = (c >> 16 & 255) / 255F, g = (c >> 8 & 255) / 255F, bl = (c & 255) / 255F;
            if (f != null) {
                r = (float) (r + (1 - r) * 0.25D);
                g = (float) (g + (1 - g) * 0.25D);
                bl = (float) (bl + (1 - bl) * 0.25D);
            }
            cache.segments.add(new Segment(rotY, rotX, length, r, g, bl,
                    new AABB(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z),
                             Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z)).inflate(0.75)));
        }
    }

    private static AABB bodyBounds(BlockPos pos) {
        // Ankerkoerper ragt nach Anbringdrehung z.T. aus dem Block heraus - grosszuegig, aber lokal.
        return new AABB(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1,
                        pos.getX() + 2, pos.getY() + 2, pos.getZ() + 2);
    }

    /** {@code isDominant}: nur einer der beiden Anker zeichnet die Leitung. */
    public static boolean isDominant(Vec3 first, Vec3 second) {
        if (first.x < second.x) return true;
        if (first.x > second.x) return false;
        if (first.y < second.y) return true;
        if (first.y > second.y) return false;
        if (first.z < second.z) return true;
        return false;
    }

    @Override public boolean shouldRenderOffScreen(T te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
