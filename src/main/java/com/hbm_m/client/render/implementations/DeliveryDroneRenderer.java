package com.hbm_m.client.render.implementations;

import com.hbm_m.entity.drone.EntityDeliveryDrone;
import com.hbm_m.entity.drone.EntityDroneBase;
import com.hbm_m.entity.drone.EntityRequestDrone;
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import com.hbm_m.platform.RenderHooks;

/**
 * Port von {@code RenderDeliveryDrone} (1.7.10 Original). Rendert das gemeinsame {@code drone.obj}
 * Multi-Part-Modell (Gruppen "Drone"/"Crate"/"Barrel", per {@code o}-Statement getrennt - im
 * Original per einfachem Ein-/Ausblenden der 3 Teile je nach {@code getAppearance()}), mit der
 * Textur abhaengig vom Drohnentyp (normal/express/request). Laed-/Parse-Logik uebernommen vom
 * bereits etablierten {@link SU47TrophyRenderer}-Muster, hier nach Objektname statt Materialname
 * gruppiert, da alle drei Teile im Original dasselbe Material nutzen.
 */
public class DeliveryDroneRenderer extends EntityRenderer<EntityDroneBase> {

    private static final Map<String, List<float[]>> OBJ_PARTS = new LinkedHashMap<>();
    private static boolean loaded = false;

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

    private static void loadObj() throws Exception {
        var res = Minecraft.getInstance().getResourceManager()
                .getResource(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, OBJ_PATH))
                .orElse(null);
        if (res == null) return;

        List<float[]> pos = new ArrayList<>(), uv = new ArrayList<>(), nrm = new ArrayList<>();
        String curPart = "Drone";

        try (var r = new BufferedReader(new InputStreamReader(res.open()))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("v ") && !line.startsWith("vt") && !line.startsWith("vn")) {
                    String[] p = line.split("\\s+");
                    pos.add(new float[]{Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3])});
                } else if (line.startsWith("vt ")) {
                    String[] p = line.split("\\s+");
                    uv.add(new float[]{Float.parseFloat(p[1]), Float.parseFloat(p[2])});
                } else if (line.startsWith("vn ")) {
                    String[] p = line.split("\\s+");
                    nrm.add(new float[]{Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3])});
                } else if (line.startsWith("o ")) {
                    curPart = line.substring(2).trim();
                } else if (line.startsWith("f ")) {
                    String[] verts = line.substring(2).trim().split("\\s+");
                    List<float[]> vs = new ArrayList<>();
                    for (String v : verts) {
                        String[] idx = v.split("/");
                        int vi = Integer.parseInt(idx[0]) - 1;
                        int ti = idx.length > 1 && !idx[1].isEmpty() ? Integer.parseInt(idx[1]) - 1 : -1;
                        int ni = idx.length > 2 && !idx[2].isEmpty() ? Integer.parseInt(idx[2]) - 1 : -1;
                        float[] p = pos.get(vi);
                        float u = ti >= 0 ? uv.get(ti)[0] : 0, v2 = ti >= 0 ? uv.get(ti)[1] : 0;
                        float nx = ni >= 0 ? nrm.get(ni)[0] : 0, ny = ni >= 0 ? nrm.get(ni)[1] : 1, nz = ni >= 0 ? nrm.get(ni)[2] : 0;
                        vs.add(new float[]{p[0], p[1], p[2], u, v2, nx, ny, nz});
                    }
                    List<float[]> tris = OBJ_PARTS.computeIfAbsent(curPart, k -> new ArrayList<>());
                    for (int i = 1; i < vs.size() - 1; i++) {
                        float[] t = new float[24];
                        System.arraycopy(vs.get(0), 0, t, 0, 8);
                        System.arraycopy(vs.get(i), 0, t, 8, 8);
                        System.arraycopy(vs.get(i + 1), 0, t, 16, 8);
                        tris.add(t);
                    }
                }
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
