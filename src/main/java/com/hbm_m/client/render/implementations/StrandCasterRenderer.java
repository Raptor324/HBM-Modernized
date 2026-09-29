package com.hbm_m.client.render.implementations;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.machines.MachineStrandCasterBlock;
import com.hbm_m.blockentity.machines.MachineStrandCasterBlockEntity;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.platform.RenderHooks;
import com.hbm_m.util.MultipartFacingTransforms;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;


/**
 * Strand Caster (continuous casting machine) on the {@link MachineRenderers} factory --
 * port of {@code RenderStrandCaster} (1.7.10). The "caster" body is static; "plate"
 * (the melt slab) is a dynamic part: in the original, before drawing, a GL clip plane
 * {0,0,-1,0.5} was set in MODEL coordinates BEFORE the shift, then the whole part was
 * shifted by {@code z = max(-offset + 3.4, 0)} -- here the clipping is replaced by a
 * Sutherland-Hodgman cut of the quads with the plane {@code v.z + t <= 0.5}
 * (equivalent to per-fragment culling: only the "sponge" of the slab sticking out of
 * the machine as it fills remains visible), and the shift becomes a static transform.
 * Both parts are tinted with the melt color {@code type.color} (moltenColor);
 * the "Surface" is synthesized: 2x2 (x +-0.9, z +-0.999) at
 * {@code y = 2.3 + level}, fullbright (240/240), sprite {@code lava_gray}.
 */
public final class StrandCasterRenderer {

    private static final RandomSource RANDOM = RandomSource.create(42);
    private static final ResourceLocation LAVA_GRAY_SPRITE =
            ResourceLocation.fromNamespaceAndPath(com.hbm_m.lib.RefStrings.MODID, "block/machine/lava_gray");

    private StrandCasterRenderer() {}

    /** Plate cache key: quantized shift + metal color (the tint is baked into the vertices). */
    private static String plateCacheKey(MachineStrandCasterBlockEntity be) {
        return (int) (moldOffset(be) * 64) + "_" + (be.type != null ? be.type.color : 0);
    }

    /** Surface cache key: melt level + metal color. */
    private static String surfaceCacheKey(MachineStrandCasterBlockEntity be) {
        return (int) (meltLevel(be) * 256) + "_" + (be.type != null ? be.type.color : 0);
    }

    public static void register() {
        MachineRenderers.machine("strand_caster",
                com.hbm_m.blockentity.ModBlockEntities.STRAND_CASTER_BE.get(),
                MachineStrandCasterBlockEntity.class)
            // The melt glows (original: glDisable(GL_LIGHTING) for plate, fullbright for Surface)
            .lightOverride("plate", be -> hasMoltenMetal(be)
                    ? net.minecraft.client.renderer.LightTexture.FULL_BRIGHT : -1)
            .part("caster")
            .dynamicPart("plate", StrandCasterRenderer::plateQuads,
                    StrandCasterRenderer::plateCacheKey,
                    StrandCasterRenderer::plateTransform)
            .dynamicPart("Surface", StrandCasterRenderer::surfaceQuads,
                    StrandCasterRenderer::surfaceCacheKey)
            .blockTransform(StrandCasterRenderer::applyBlockTransform)
            .itemParts("caster", "plate")
            .register();
    }

    // -- Original math --------------------------------------------------

    /** Port of level = amount/capacity * 0.675 (melt surface height). */
    private static double meltLevel(MachineStrandCasterBlockEntity be) {
        if (be.amount == 0) return 0;
        int capacity = be.getCapacity();
        return capacity > 0 ? (double) be.amount / capacity * 0.675 : 0;
    }

    /** Port of offset = amount/cost * 0.375 (slab shift toward the mold). */
    private static double moldOffset(MachineStrandCasterBlockEntity be) {
        if (be.amount == 0) return 0;
        int cost = be.getMoldCostMb();
        return cost > 0 ? (double) be.amount / cost * 0.375 : 0;
    }

    // -- Block transform (the original's legacy GL chain) ---------------

    private static Direction facing(MachineStrandCasterBlockEntity be) {
        var state = be.getBlockState();
        return state.hasProperty(MachineStrandCasterBlock.FACING)
                ? state.getValue(MachineStrandCasterBlock.FACING) : Direction.NORTH;
    }

    /**
     * Original: rotateY(legacy facing) -> translate(0.5,0,0.5) -> rotateY(180).
     * PoseStack applies the calls in order (the last one reaches the vertices first).
     */
    private static void applyBlockTransform(MachineStrandCasterBlockEntity be,
                                            com.hbm_m.client.render.LegacyAnimator animator) {
        // Port of glTranslated(x + 0.5, y, z + 0.5): the BER basis is the block corner; in 1.7.10
        // the TESR started with centering -- without it the model drifts half a block diagonally.
        animator.translate(0.5f, 0f, 0.5f);
        animator.rotate(MultipartFacingTransforms.legacyFacingRotationYDegrees(facing(be)), 0, 1, 0);
        animator.translate(0.5f, 0f, 0.5f);
        animator.rotate(180f, 0f, 1f, 0f);
    }

    // -- Plate: clip at natural z + shift (port of GL_CLIP_PLANE0) ------

    /** Plate transform: z = max(-offset + 3.4, 0) in model coordinates. */
    private static boolean plateTransform(MachineStrandCasterBlockEntity be, float partialTick,
                                          long gameTime, com.mojang.blaze3d.vertex.PoseStack pose) {
        pose.translate(0, 0, Math.max(-moldOffset(be) + 3.4, 0));
        return true;
    }

    /** Port of the render condition: metal is visible only with amount > 0 and a mold installed. */
    private static boolean hasMoltenMetal(MachineStrandCasterBlockEntity be) {
        return be.amount != 0 && be.getInstalledMold() != null;
    }

    /**
     * "plate" part quads with clipping and tint. Port of the GL_CLIP_PLANE0 {0,0,-1,0.5} +
     * glTranslated(0,0,t) combo: the plane is set in model coordinates BEFORE the shift, so
     * a fragment is visible when {@code v.z + t <= 0.5}. Polygons are cut by the plane
     * Sutherland-Hodgman style with UV/color interpolation (per-fragment equivalent); the
     * vertex color is multiplied by moltenColor (port of the original's glColor3f).
     */
    private static List<BakedQuad> plateQuads(MachineStrandCasterBlockEntity be) {
        if (!hasMoltenMetal(be)) return List.of();

        List<BakedQuad> all = collectPartQuads(be, "plate");
        if (all.isEmpty()) return List.of();

        double t = Math.max(-moldOffset(be) + 3.4, 0);
        float clipZ = (float) (0.5 - t);
        int tint = moltenColorRgba(be);

        List<BakedQuad> result = new ArrayList<>();
        for (BakedQuad quad : all) {
            clipQuad(quad, clipZ, tint, result);
        }
        return result;
    }

    /**
     * Cuts a quad with the half-plane {@code z <= clipZ} and appends the result to {@code out}.
     * BLOCK vertex format (8 ints: position x3, color, uv x2, light, normal); at edge
     * intersections position/uv/color are interpolated linearly, light and normal come from
     * the interior vertex. A 4-vertex polygon stays a quad, 3 becomes a quad with a duplicated
     * vertex, 5 becomes a quad + triangle.
     */
    private static void clipQuad(BakedQuad quad, float clipZ, int tint, List<BakedQuad> out) {
        int[] data = quad.getVertices();
        int vertexSize = data.length / 4;
        if (vertexSize != 8) return; // BLOCK format

        int[][] v = new int[4][];
        float[] z = new float[4];
        for (int i = 0; i < 4; i++) {
            v[i] = java.util.Arrays.copyOfRange(data, i * vertexSize, (i + 1) * vertexSize);
            z[i] = Float.intBitsToFloat(v[i][2]);
        }

        java.util.List<int[]> poly = new ArrayList<>(5);
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            boolean inI = z[i] <= clipZ;
            boolean inJ = z[j] <= clipZ;
            if (inI) poly.add(v[i]);
            if (inI != inJ) {
                float s = (clipZ - z[i]) / (z[j] - z[i]);
                poly.add(lerpVertex(v[i], v[j], s));
            }
        }

        int n = poly.size();
        if (n == 0) return;

        int[][] fan;
        if (n == 4) {
            fan = new int[][] { poly.get(0), poly.get(1), poly.get(2), poly.get(3) };
        } else if (n == 3) {
            fan = new int[][] { poly.get(0), poly.get(1), poly.get(2), poly.get(2) };
        } else { // n == 5
            fan = new int[][] {
                    poly.get(0), poly.get(1), poly.get(2), poly.get(3),
                    poly.get(0), poly.get(3), poly.get(4), poly.get(4)
            };
        }

        for (int q = 0; q < fan.length / 4; q++) {
            int[] nd = new int[4 * vertexSize];
            for (int i = 0; i < 4; i++) {
                int[] vert = tintVertex(fan[q * 4 + i], tint);
                System.arraycopy(vert, 0, nd, i * vertexSize, vertexSize);
            }
            out.add(new BakedQuad(nd, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade()));
        }
    }

    /** Linear interpolation of position/uv/color between vertices; light and normal come from {@code a}. */
    private static int[] lerpVertex(int[] a, int[] b, float s) {
        int[] r = new int[a.length];
        r[0] = Float.floatToRawIntBits(lerp(Float.intBitsToFloat(a[0]), Float.intBitsToFloat(b[0]), s));
        r[1] = Float.floatToRawIntBits(lerp(Float.intBitsToFloat(a[1]), Float.intBitsToFloat(b[1]), s));
        r[2] = Float.floatToRawIntBits(lerp(Float.intBitsToFloat(a[2]), Float.intBitsToFloat(b[2]), s));
        r[3] = lerpColor(a[3], b[3], s);
        r[4] = Float.floatToRawIntBits(lerp(Float.intBitsToFloat(a[4]), Float.intBitsToFloat(b[4]), s));
        r[5] = Float.floatToRawIntBits(lerp(Float.intBitsToFloat(a[5]), Float.intBitsToFloat(b[5]), s));
        r[6] = a[6]; // light
        r[7] = a[7]; // normal
        return r;
    }

    private static float lerp(float a, float b, float s) {
        return a + (b - a) * s;
    }

    /** Per-channel interpolation of a packed color (vertex format: r in the lowest byte). */
    private static int lerpColor(int a, int b, float s) {
        int r = Math.round(lerp(a & 0xFF, b & 0xFF, s));
        int g = Math.round(lerp((a >> 8) & 0xFF, (b >> 8) & 0xFF, s));
        int bl = Math.round(lerp((a >> 16) & 0xFF, (b >> 16) & 0xFF, s));
        int al = Math.round(lerp((a >> 24) & 0xFF, (b >> 24) & 0xFF, s));
        return (al << 24) | (bl << 16) | (g << 8) | r;
    }

    /** Multiplies the vertex color by the melt tint (port of glColor3f: component-wise multiplication). */
    private static int[] tintVertex(int[] vert, int tint) {
        int c = vert[3];
        int r = ((c & 0xFF) * (tint & 0xFF)) / 255;
        int g = (((c >> 8) & 0xFF) * ((tint >> 8) & 0xFF)) / 255;
        int b = (((c >> 16) & 0xFF) * ((tint >> 16) & 0xFF)) / 255;
        int a = (c >>> 24) * ((tint >>> 24) & 0xFF) / 255;
        vert[3] = (a << 24) | (b << 16) | (g << 8) | r;
        return vert;
    }

    // -- Surface: synthesized melt surface ------------------------------

    /**
     * Melt surface: a single horizontal 2x2 quad (x +-0.9, z +-0.999),
     * y = 2.3 + level, fullbright, lava_gray sprite, melt color baked
     * into the vertices (RGBA, alpha 255). Format template: the first quad of the "plate" part.
     */
    private static List<BakedQuad> surfaceQuads(MachineStrandCasterBlockEntity be) {
        if (!hasMoltenMetal(be) || be.type == null) return List.of();

        List<BakedQuad> templateQuads = collectPartQuads(be, "plate");
        if (templateQuads.isEmpty()) {
            templateQuads = collectPartQuads(be, "caster");
        }
        if (templateQuads.isEmpty()) return List.of();
        BakedQuad template = templateQuads.get(0);

        int[] oldData = template.getVertices();
        int vertexSize = oldData.length / 4;
        if (vertexSize != 8) return List.of(); // BLOCK format: 8 ints per vertex

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(LAVA_GRAY_SPRITE);

        int[] data = new int[oldData.length];
        // Vertex order: CCW seen from above -> normal +Y (the winding of the "top" template)
        // v0(-x,-z) uv(0,0), v1(-x,+z) uv(0,1), v2(+x,+z) uv(1,1), v3(+x,-z) uv(1,0)
        float level = (float) meltLevel(be);
        float y = 2.3f + level;
        int color = moltenColorRgba(be);

        for (int i = 0; i < 4; i++) {
            int off = i * vertexSize;
            float x = (i == 2 || i == 3) ? 0.9f : -0.9f;
            float z = (i == 1 || i == 2) ? 0.999f : -0.999f;
            float u = (i == 2 || i == 3) ? 1f : 0f;
            float v = (i == 1 || i == 2) ? 1f : 0f;

            data[off + 0] = Float.floatToRawIntBits(x);
            data[off + 1] = Float.floatToRawIntBits(y);
            data[off + 2] = Float.floatToRawIntBits(z);
            data[off + 3] = color;
            data[off + 4] = Float.floatToRawIntBits(sprite.getU(u));
            data[off + 5] = Float.floatToRawIntBits(sprite.getV(v));
            data[off + 6] = LightTexture.FULL_BRIGHT; // fullbright 240/240
            // leave the normal untouched -- the template's byte packing (horizontal quad)
            data[off + 7] = oldData[off + 7];
        }

        return List.of(new BakedQuad(data, -1, Direction.UP, sprite, false));
    }

    /** moltenColor baked into the vertex color: RGBA, alpha 255. */
    private static int moltenColorRgba(MachineStrandCasterBlockEntity be) {
        int c = be.type != null ? be.type.color : 0xFFFFFF;
        int r = (c >> 16) & 0xFF;
        int g = (c >> 8) & 0xFF;
        int b = c & 0xFF;
        return (255 << 24) | (b << 16) | (g << 8) | r;
    }

    // -- Shared helpers -------------------------------------------------

    /** Collects all quads of a strand_caster model part (natural OBJ coordinates). */
    private static List<BakedQuad> collectPartQuads(MachineStrandCasterBlockEntity be, String partName) {
        BakedModel raw = Minecraft.getInstance().getBlockRenderer().getBlockModel(be.getBlockState());
        if (!(raw instanceof com.hbm_m.client.model.AbstractMultipartBakedModel model)) return List.of();
        BakedModel part = model.getPart(partName);
        if (part == null) return List.of();

        List<BakedQuad> quads = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            quads.addAll(partQuads(part, dir, RANDOM));
        }
        quads.addAll(partQuads(part, null, RANDOM));
        return quads;
    }

    private static List<BakedQuad> partQuads(BakedModel part, @Nullable Direction side, RandomSource rand) {
        return RenderHooks.getModelQuads(part, null, side, rand, null);
    }
}
