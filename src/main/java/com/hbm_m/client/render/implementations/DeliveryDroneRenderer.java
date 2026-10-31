package com.hbm_m.client.render.implementations;

import com.hbm_m.entity.drone.EntityDeliveryDrone;
import com.hbm_m.entity.drone.EntityDroneBase;
import com.hbm_m.entity.drone.EntityRequestDrone;
import com.hbm_m.client.model.nmesh.NMeshData;
import com.hbm_m.client.model.nmesh.NMeshLoader;
import com.hbm_m.client.render.cache.RenderCacheManager;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.*;
import com.hbm_m.platform.RenderHooks;

/**
 * Port von {@code RenderDeliveryDrone} (1.7.10 Original). Rendert das gemeinsame {@code drone.obj}
 * Multi-Part-Modell (Gruppen "Drone"/"Crate"/"Barrel", per {@code o}-Statement getrennt - im
 * Original per einfachem Ein-/Ausblenden der 3 Teile je nach {@code getAppearance()}), mit der
 * Textur abhaengig vom Drohnentyp (normal/express/request). Laed-/Parse-Logik uebernommen vom
 * bereits etablierten Trophaeen-Renderer-Muster, hier nach Objektname statt Materialname
 * gruppiert, da alle drei Teile im Original dasselbe Material nutzen.
 */
public class DeliveryDroneRenderer extends EntityRenderer<EntityDroneBase> {

    private static final Map<String, List<float[]>> OBJ_PARTS = new LinkedHashMap<>();
    private static boolean loaded = false;

    // The parsed OBJ and the load latch must not survive a resource reload - dropping them makes
    // the next ensureLoaded() re-read the OBJ from the current resource pack (and re-register
    // the three SimpleTextures, closing the replaced ones).
    static {
        RenderCacheManager.register(reason -> {
            OBJ_PARTS.clear();
            loaded = false;
        });
    }

    private static final String OBJ_PATH = "models/block/machines/drone.obj";

    private static final ResourceLocation TEX_NORMAL =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/drone.png");
    private static final ResourceLocation TEX_EXPRESS =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/drone_express.png");
    private static final ResourceLocation TEX_REQUEST =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/drone_request.png");

    private static final float SCALE = 0.3f;

    public DeliveryDroneRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try { loadObj(); } catch (Exception ignored) {}
        var tm = Minecraft.getInstance().getTextureManager();
        tm.register(TEX_NORMAL, new SimpleTexture(TEX_NORMAL));
        tm.register(TEX_EXPRESS, new SimpleTexture(TEX_EXPRESS));
        tm.register(TEX_REQUEST, new SimpleTexture(TEX_REQUEST));
    }

    /** Nmesh-first: бинарный .nmesh (raw .obj — только resourcepack-fallback внутри NMeshLoader), части по objectName/groupName. */
    private static void loadObj() {
        NMeshData nmesh = NMeshLoader.load(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, OBJ_PATH));
        if (nmesh == null) return;

        for (NMeshData.SubMesh sm : nmesh.subMeshes()) {
            String key = !sm.objectName.isEmpty() ? sm.objectName
                    : (!sm.groupName.isEmpty() ? sm.groupName : "default");
            List<float[]> tris = OBJ_PARTS.computeIfAbsent(key, k -> new ArrayList<>());
            float[] verts = sm.vertices;
            int[] idx = sm.indices;
            for (int i = 0; i + 2 < idx.length; i += 3) {
                float[] t = new float[24];
                for (int c = 0; c < 3; c++) {
                    int b = idx[i + c] * 8;
                    t[c * 8 + 0] = verts[b + 0];
                    t[c * 8 + 1] = verts[b + 1];
                    t[c * 8 + 2] = verts[b + 2];
                    // u,v сырые: render() применяет (1 - v) при выводе.
                    t[c * 8 + 3] = verts[b + 6];
                    t[c * 8 + 4] = verts[b + 7];
                    t[c * 8 + 5] = verts[b + 3];
                    t[c * 8 + 6] = verts[b + 4];
                    t[c * 8 + 7] = verts[b + 5];
                }
                tris.add(t);
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(EntityDroneBase entity) {
        if (entity instanceof EntityRequestDrone) return TEX_REQUEST;
        if (entity instanceof EntityDeliveryDrone delivery && delivery.isExpress()) return TEX_EXPRESS;
        return TEX_NORMAL;
    }

    @Override
    public void render(EntityDroneBase entity, float entityYaw, float partialTick, PoseStack ps,
                        MultiBufferSource buf, int packedLight) {
        ensureLoaded();
        if (OBJ_PARTS.isEmpty()) {
            super.render(entity, entityYaw, partialTick, ps, buf, packedLight);
            return;
        }

        ps.pushPose();
        ps.translate(0, 0.1, 0);
        ps.scale(SCALE, SCALE, SCALE);

        Matrix4f m = ps.last().pose();
        ResourceLocation tex = getTextureLocation(entity);
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(tex));

        int appearance = entity.getAppearance();
        for (var entry : OBJ_PARTS.entrySet()) {
            String part = entry.getKey();
            if (part.equalsIgnoreCase("Crate") && appearance != EntityDroneBase.APPEARANCE_CRATE) continue;
            if (part.equalsIgnoreCase("Barrel") && appearance != EntityDroneBase.APPEARANCE_BARREL) continue;

            for (float[] tri : entry.getValue()) {
                for (int pass = 0; pass < 4; pass++) {
                    int base = Math.min(pass, 2) * 8;
                    float x = tri[base], y = tri[base + 1], z = tri[base + 2];
                    float u = tri[base + 3], v = 1f - tri[base + 4];
                    float nx = tri[base + 5], ny = tri[base + 6], nz = tri[base + 7];
                    RenderHooks.vertexFull(vc, m, x, y, z, 255, 255, 255, 255, u, v, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, packedLight, nx, ny, nz);
                }
            }
        }

        ps.popPose();
        super.render(entity, entityYaw, partialTick, ps, buf, packedLight);
    }
}
