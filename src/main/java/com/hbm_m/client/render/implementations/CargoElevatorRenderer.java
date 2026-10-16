package com.hbm_m.client.render.implementations;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hbm_m.blockentity.machines.CargoElevatorBlockEntity;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;

/**
 * Draws the whole cargo-elevator shaft (guide posts for every floor, plus the sliding
 * platform+pistons) from the core position only — non-core cells render nothing. Uses the same
 * manual named-group OBJ rendering approach as {@link RBMKColumnRenderer}.
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class CargoElevatorRenderer implements BlockEntityRenderer<CargoElevatorBlockEntity> {

    private static final Map<String, Map<String, List<float[]>>> OBJ_CACHE = new HashMap<>();
    private static final Map<String, TextureAtlasSprite> SPRITE_CACHE = new HashMap<>();
    private static final String OBJ_PATH = "models/machines/elevator.obj";
    private static final String TEXTURE = "block/machine/elevator";

    public CargoElevatorRenderer(BlockEntityRendererProvider.Context ctx) {}

    private static TextureAtlasSprite sprite() {
        return SPRITE_CACHE.computeIfAbsent(TEXTURE, k ->
                Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                        .apply(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, TEXTURE)));
    }

    private static Map<String, List<float[]>> getObj() {
        return OBJ_CACHE.computeIfAbsent(OBJ_PATH, path -> {
            try {
                var res = Minecraft.getInstance().getResourceManager()
                        .getResource(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, path))
                        .orElse(null);
                if (res == null) return Map.of();
                return parseObj(new BufferedReader(new InputStreamReader(res.open())));
            } catch (Exception e) {
                return Map.of();
            }
        });
    }

    private static Map<String, List<float[]>> parseObj(BufferedReader reader) throws Exception {
        List<float[]> pos = new ArrayList<>(), uv = new ArrayList<>(), nrm = new ArrayList<>();
        Map<String, List<float[]>> result = new LinkedHashMap<>();
        String cur = "default";
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("o ")) {
                cur = line.substring(2).trim();
            } else if (line.startsWith("v ") && !line.startsWith("vt") && !line.startsWith("vn")) {
                String[] p = line.split("\\s+");
                pos.add(new float[]{Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3])});
            } else if (line.startsWith("vt ")) {
                String[] p = line.split("\\s+");
                uv.add(new float[]{Float.parseFloat(p[1]), Float.parseFloat(p[2])});
            } else if (line.startsWith("vn ")) {
                String[] p = line.split("\\s+");
                nrm.add(new float[]{Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3])});
            } else if (line.startsWith("f ")) {
                String[] verts = line.substring(2).trim().split("\\s+");
                List<float[]> vs = new ArrayList<>();
                for (String vert : verts) {
                    String[] idx = vert.split("/");
                    int vi = Integer.parseInt(idx[0]) - 1;
                    int ti = idx.length > 1 && !idx[1].isEmpty() ? Integer.parseInt(idx[1]) - 1 : -1;
                    int ni = idx.length > 2 && !idx[2].isEmpty() ? Integer.parseInt(idx[2]) - 1 : -1;
                    float[] p = pos.get(vi);
                    float u = ti >= 0 ? uv.get(ti)[0] : 0, v = ti >= 0 ? uv.get(ti)[1] : 0;
                    float nx = ni >= 0 ? nrm.get(ni)[0] : 0;
                    float ny = ni >= 0 ? nrm.get(ni)[1] : 1;
                    float nz = ni >= 0 ? nrm.get(ni)[2] : 0;
                    vs.add(new float[]{p[0], p[1], p[2], u, v, nx, ny, nz});
                }
                List<float[]> tris = result.computeIfAbsent(cur, k -> new ArrayList<>());
                for (int i = 1; i < vs.size() - 1; i++) {
                    float[] t = new float[24];
                    System.arraycopy(vs.get(0), 0, t, 0, 8);
                    System.arraycopy(vs.get(i), 0, t, 8, 8);
                    System.arraycopy(vs.get(i + 1), 0, t, 16, 8);
                    tris.add(t);
                }
            }
        }
        return result;
    }

    private static void renderGroup(VertexConsumer vc, Matrix4f m, List<float[]> triangles,
                                     TextureAtlasSprite sprite, int light, int overlay) {
        if (triangles == null || sprite == null) return;
        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();
        for (float[] tri : triangles) {
            for (int pass = 0; pass < 4; pass++) {
                int base = Math.min(pass, 2) * 8;
                float x = tri[base], y = tri[base + 1], z = tri[base + 2];
                float u = tri[base + 3], v = tri[base + 4];
                float nx = tri[base + 5], ny = tri[base + 6], nz = tri[base + 7];
                float au = u0 + u * (u1 - u0);
                float av = v0 + (1f - v) * (v1 - v0);
                RenderHooks.vertexFull(vc, m, x, y, z, 255, 255, 255, 255, au, av, overlay, light, nx, ny, nz);
            }
        }
    }

    @Override
    public void render(CargoElevatorBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buf,
                        int packedLight, int packedOverlay) {
        if (!be.isCore()) {
            return;
        }

        Map<String, List<float[]>> obj = getObj();
        TextureAtlasSprite tex = sprite();
        VertexConsumer vc = buf.getBuffer(RenderType.solid());

        ps.pushPose();
        ps.translate(0.5, 0, 0.5);

        renderGroup(vc, ps.last().pose(), obj.get("Base"), tex, packedLight, packedOverlay);

        ps.pushPose();
        for (int i = 0; i <= be.height; i++) {
            renderGroup(vc, ps.last().pose(), obj.get("Guides"), tex, packedLight, packedOverlay);
            ps.translate(0, 1, 0);
        }
        ps.popPose();

        double extension = be.getRenderExtension(partialTick);
        ps.pushPose();
        ps.translate(0, extension, 0);
        renderGroup(vc, ps.last().pose(), obj.get("Platform"), tex, packedLight, packedOverlay);
        int pistons = (int) Math.floor(extension) + 1;
        for (int i = 0; i < pistons; i++) {
            renderGroup(vc, ps.last().pose(), obj.get("Piston"), tex, packedLight, packedOverlay);
            ps.translate(0, -1, 0);
        }
        ps.popPose();

        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(CargoElevatorBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}