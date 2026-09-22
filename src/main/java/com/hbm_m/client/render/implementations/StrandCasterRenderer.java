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
 * Strand Caster (машина непрерывного литья) на фабрике {@link MachineRenderers} —
 * порт {@code RenderStrandCaster} (1.7.10). Корпус "caster" — статика; "plate"
 * (плита расплава) — динамическая часть: в оригинале перед отрисовкой ставился
 * GL clip plane {0,0,-1,0.5} в МОДЕЛЬНЫХ координатах ДО сдвига, затем вся часть
 * сдвигалась на {@code z = max(-offset + 3.4, 0)} — здесь клиппинг заменён
 * Sutherland–Hodgman-разрезанием квадов плоскостью {@code v.z + t <= 0.5}
 * (эквивалент per-fragment отсечения: видимой остаётся только «губка» плиты,
 * выдающаяся из машины по мере заполнения), а сдвиг — статическим трансформом.
 * Обе части тинтируются цветом расплава {@code type.color} (moltenColor),
 * поверхность "Surface" синтезируется: 2×2 (x ±0.9, z ±0.999) на
 * {@code y = 2.3 + level}, fullbright (240/240), спрайт {@code lava_gray}.
 */
public final class StrandCasterRenderer {

    private static final RandomSource RANDOM = RandomSource.create(42);
    private static final ResourceLocation LAVA_GRAY_SPRITE =
            ResourceLocation.fromNamespaceAndPath(com.hbm_m.lib.RefStrings.MODID, "block/machine/lava_gray");

    private StrandCasterRenderer() {}

    /** Кэш-ключ плиты: квантованный сдвиг + цвет металла (тинт запечён в вершины). */
    private static String plateCacheKey(MachineStrandCasterBlockEntity be) {
        return (int) (moldOffset(be) * 64) + "_" + (be.type != null ? be.type.color : 0);
    }

    /** Кэш-ключ поверхности: уровень расплава + цвет металла. */
    private static String surfaceCacheKey(MachineStrandCasterBlockEntity be) {
        return (int) (meltLevel(be) * 256) + "_" + (be.type != null ? be.type.color : 0);
    }

    public static void register() {
        MachineRenderers.machine("strand_caster",
                com.hbm_m.blockentity.ModBlockEntities.STRAND_CASTER_BE.get(),
                MachineStrandCasterBlockEntity.class)
            // Расплав светится (оригинал: glDisable(GL_LIGHTING) для plate, fullbright для Surface)
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

    // ── Математика оригинала ───────────────────────────────────────────

    /** Порт level = amount/capacity * 0.675 (высота поверхности расплава). */
    private static double meltLevel(MachineStrandCasterBlockEntity be) {
        if (be.amount == 0) return 0;
        int capacity = be.getCapacity();
        return capacity > 0 ? (double) be.amount / capacity * 0.675 : 0;
    }

    /** Порт offset = amount/cost * 0.375 (сдвиг плиты к изложнице). */
    private static double moldOffset(MachineStrandCasterBlockEntity be) {
        if (be.amount == 0) return 0;
        int cost = be.getMoldCostMb();
        return cost > 0 ? (double) be.amount / cost * 0.375 : 0;
    }

    // ── Блочный трансформ (легаси GL-цепочка оригинала) ───────────────

    private static Direction facing(MachineStrandCasterBlockEntity be) {
        var state = be.getBlockState();
        return state.hasProperty(MachineStrandCasterBlock.FACING)
                ? state.getValue(MachineStrandCasterBlock.FACING) : Direction.NORTH;
    }

    /**
     * Оригинал: rotateY(legacy facing) → translate(0.5,0,0.5) → rotateY(180).
     * PoseStack применяет вызовы по порядку (последний — к вершинам первым).
     */
    private static void applyBlockTransform(MachineStrandCasterBlockEntity be,
                                            com.hbm_m.client.render.LegacyAnimator animator) {
        // Порт glTranslated(x + 0.5, y, z + 0.5): базис BER — угол блока, в 1.7.10
        // TESR начинался с центрирования — без него модель уезжает на полблока по диагонали.
        animator.translate(0.5f, 0f, 0.5f);
        animator.rotate(MultipartFacingTransforms.legacyFacingRotationYDegrees(facing(be)), 0, 1, 0);
        animator.translate(0.5f, 0f, 0.5f);
        animator.rotate(180f, 0f, 1f, 0f);
    }

    // ── Plate: клип по натуральном z + сдвиг (порт GL_CLIP_PLANE0) ────

    /** Трансформ плиты: z = max(-offset + 3.4, 0) в модельных координатах. */
    private static boolean plateTransform(MachineStrandCasterBlockEntity be, float partialTick,
                                          long gameTime, com.mojang.blaze3d.vertex.PoseStack pose) {
        pose.translate(0, 0, Math.max(-moldOffset(be) + 3.4, 0));
        return true;
    }

    /** Порт условия рендера: металл виден только при amount > 0 и вставленной изложнице. */
    private static boolean hasMoltenMetal(MachineStrandCasterBlockEntity be) {
        return be.amount != 0 && be.getInstalledMold() != null;
    }

    /**
     * Квады части "plate" с клипом и тинтом. Порт связки GL_CLIP_PLANE0 {0,0,-1,0.5} +
     * glTranslated(0,0,t): плоскость задаётся в модельных координатах ДО сдвига, поэтому
     * фрагмент видим при {@code v.z + t <= 0.5}. Полигоны режутся по плоскости
     * Sutherland–Hodgman'ом с интерполяцией UV/цвета (per-fragment эквивалент), цвет
     * вершин домножается на moltenColor (порт glColor3f в оригинале).
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
     * Разрезание квада полуплоскостью {@code z <= clipZ} и добавление результата в {@code out}.
     * Формат вершин BLOCK (8 int: позиция×3, цвет, uv×2, свет, нормаль); на пересечении рёбер
     * позиция/uv/цвет интерполируются линейно, свет и нормаль берутся с внутренней вершины.
     * Полигон из 4 вершин остаётся квадом, 3 — квадом с продублированной вершиной,
     * 5 — квадом + треугольником.
     */
    private static void clipQuad(BakedQuad quad, float clipZ, int tint, List<BakedQuad> out) {
        int[] data = quad.getVertices();
        int vertexSize = data.length / 4;
        if (vertexSize != 8) return; // формат BLOCK

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

    /** Линейная интерполяция позиции/uv/цвета между вершинами; свет и нормаль — от {@code a}. */
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

    /** Поканальная интерполяция упакованного цвета (формат вершин: r в младшем байте). */
    private static int lerpColor(int a, int b, float s) {
        int r = Math.round(lerp(a & 0xFF, b & 0xFF, s));
        int g = Math.round(lerp((a >> 8) & 0xFF, (b >> 8) & 0xFF, s));
        int bl = Math.round(lerp((a >> 16) & 0xFF, (b >> 16) & 0xFF, s));
        int al = Math.round(lerp((a >> 24) & 0xFF, (b >> 24) & 0xFF, s));
        return (al << 24) | (bl << 16) | (g << 8) | r;
    }

    /** Домножение цвета вершины на тинт расплава (порт glColor3f: компонентное умножение). */
    private static int[] tintVertex(int[] vert, int tint) {
        int c = vert[3];
        int r = ((c & 0xFF) * (tint & 0xFF)) / 255;
        int g = (((c >> 8) & 0xFF) * ((tint >> 8) & 0xFF)) / 255;
        int b = (((c >> 16) & 0xFF) * ((tint >> 16) & 0xFF)) / 255;
        int a = (c >>> 24) * ((tint >>> 24) & 0xFF) / 255;
        vert[3] = (a << 24) | (b << 16) | (g << 8) | r;
        return vert;
    }

    // ── Surface: синтез поверхности расплава ──────────────────────────

    /**
     * Поверхность расплава: один горизонтальный квад 2×2 (x ±0.9, z ±0.999),
     * y = 2.3 + level, fullbright, спрайт lava_gray, цвет расплава запечён
     * в вершины (RGBA, alpha 255). Шаблон формата — первый квад части "plate".
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
        if (vertexSize != 8) return List.of(); // формат BLOCK: 8 int на вершину

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(LAVA_GRAY_SPRITE);

        int[] data = new int[oldData.length];
        // Порядок вершин: обход CCW сверху → нормаль +Y (winding шаблона-«верха»)
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
            // нормаль не трогаем — байт-упаковка шаблона (горизонтальный квад)
            data[off + 7] = oldData[off + 7];
        }

        return List.of(new BakedQuad(data, -1, Direction.UP, sprite, false));
    }

    /** moltenColor, запечённый в цвет вершин: RGBA, alpha 255. */
    private static int moltenColorRgba(MachineStrandCasterBlockEntity be) {
        int c = be.type != null ? be.type.color : 0xFFFFFF;
        int r = (c >> 16) & 0xFF;
        int g = (c >> 8) & 0xFF;
        int b = c & 0xFF;
        return (255 << 24) | (b << 16) | (g << 8) | r;
    }

    // ── Общие хелперы ─────────────────────────────────────────────────

    /** Сбор всех квадов части модели strand_caster (натуральные OBJ-координаты). */
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
