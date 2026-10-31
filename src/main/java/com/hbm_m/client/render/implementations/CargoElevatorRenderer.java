package com.hbm_m.client.render.implementations;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hbm_m.blockentity.machines.CargoElevatorBlockEntity;
import com.hbm_m.client.model.nmesh.NMeshData;
import com.hbm_m.client.model.nmesh.NMeshLoader;
import com.hbm_m.client.render.cache.RenderCacheManager;
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
@OnlyIn(Dist.CLIENT)
public class CargoElevatorRenderer implements BlockEntityRenderer<CargoElevatorBlockEntity> {

    private static final Map<String, Map<String, List<float[]>>> OBJ_CACHE = new HashMap<>();
    private static final Map<String, TextureAtlasSprite> SPRITE_CACHE = new HashMap<>();
    private static final String OBJ_PATH = "models/machines/elevator.obj";
    private static final String TEXTURE = "block/machine/elevator";

    // Cached sprites hold the UV rects of the atlas layout they were fetched under, so both
    // caches must be dropped on reload/session end - the next sprite()/getObj() call lazily
    // re-fetches from the re-stitched atlas / current resource pack.
    static {
        RenderCacheManager.register(reason -> {
            OBJ_CACHE.clear();
            SPRITE_CACHE.clear();
        });
    }

    public CargoElevatorRenderer(BlockEntityRendererProvider.Context ctx) {}

    private static TextureAtlasSprite sprite() {
        return SPRITE_CACHE.computeIfAbsent(TEXTURE, k ->
                Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                        .apply(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, TEXTURE)));
    }

    /**
     * Nmesh-backed geometry: бинарный {@code .nmesh} через {@link NMeshLoader} (raw .obj остаётся
     * только как runtime resourcepack-fallback внутри NMeshLoader), сабмеши по objectName/groupName
     * в прежнем формате {@code groupName → [x,y,z, u,v, nx,ny,nz] × 3} (raw V).
     */
    private static Map<String, List<float[]>> getObj() {
        return OBJ_CACHE.computeIfAbsent(OBJ_PATH, path -> {
            NMeshData nmesh = NMeshLoader.load(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, path));
            if (nmesh == null) {
                return Map.of();
            }
            Map<String, List<float[]>> result = new LinkedHashMap<>();
            for (NMeshData.SubMesh sm : nmesh.subMeshes()) {
                String key = !sm.objectName.isEmpty() ? sm.objectName
                        : (!sm.groupName.isEmpty() ? sm.groupName : "default");
                List<float[]> tris = result.computeIfAbsent(key, k -> new ArrayList<>());
                float[] verts = sm.vertices;
                int[] idx = sm.indices;
                for (int i = 0; i + 2 < idx.length; i += 3) {
                    float[] t = new float[24];
                    for (int c = 0; c < 3; c++) {
                        int b = idx[i + c] * 8;
                        t[c * 8 + 0] = verts[b + 0];
                        t[c * 8 + 1] = verts[b + 1];
                        t[c * 8 + 2] = verts[b + 2];
                        // u,v сырые: renderGroup применяет (1 - v) при выводе.
                        t[c * 8 + 3] = verts[b + 6];
                        t[c * 8 + 4] = verts[b + 7];
                        t[c * 8 + 5] = verts[b + 3];
                        t[c * 8 + 6] = verts[b + 4];
                        t[c * 8 + 7] = verts[b + 5];
                    }
                    tris.add(t);
                }
            }
            return result;
        });
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