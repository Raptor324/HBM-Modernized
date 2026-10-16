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
 * GL clip plane {0,0,-1,0.5} в МОДЕЛЬНЫХ координатах, затем вся часть
 * сдвигалась на {@code z = max(-offset + 3.4, 0)} — здесь клиппинг заменён
 * фильтрацией квадов (отбрасываются квадов, все 4 вершины которых имеют
 * натуральный z > 0.51), а сдвиг — статическим трансформом. Поверхность расплава
 * "Surface" синтезируется: 2×2 (x ±0.9, z ±0.999) на {@code y = 2.3 + level},
 * fullbright (240/240), спрайт {@code lava_gray} с запечённым в вершины цветом
 * {@code type.color} (moltenColor).
 */
public final class StrandCasterRenderer {

    private static final RandomSource RANDOM = RandomSource.create(42);
    private static final ResourceLocation LAVA_GRAY_SPRITE =
            ResourceLocation.fromNamespaceAndPath(com.hbm_m.lib.RefStrings.MODID, "block/machine/lava_gray");

    private StrandCasterRenderer() {}

    public static void register() {
        MachineRenderers.machine("strand_caster",
                com.hbm_m.blockentity.ModBlockEntities.STRAND_CASTER_BE.get(),
                MachineStrandCasterBlockEntity.class)
            .part("caster")
            .dynamicPart("plate", StrandCasterRenderer::plateQuads,
                    be -> String.valueOf((int) (moldOffset(be) * 64)),
                    StrandCasterRenderer::plateTransform)
            .dynamicPart("Surface", StrandCasterRenderer::surfaceQuads,
                    be -> String.valueOf((int) (meltLevel(be) * 256)))
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

    /**
     * Квады части "plate", отфильтрованные по клип-плоскости {0,0,-1,0.5}:
     * квад рисуется, только если хотя бы одна вершина имеет натуральный z <= 0.51.
     */
    private static List<BakedQuad> plateQuads(MachineStrandCasterBlockEntity be) {
        List<BakedQuad> all = collectPartQuads(be, "plate");
        if (all.isEmpty()) return List.of();

        List<BakedQuad> result = new ArrayList<>();
        for (BakedQuad quad : all) {
            if (!isFullyBeyondClip(quad)) {
                result.add(quad);
            }
        }
        return result;
    }

    /** true — все 4 вершины квада имеют натуральный (модельный) z > 0.5 + эпсилон. */
    private static boolean isFullyBeyondClip(BakedQuad quad) {
        int[] data = quad.getVertices();
        int vertexSize = data.length / 4;
        for (int i = 0; i < 4; i++) {
            int off = i * vertexSize;
            float z = Float.intBitsToFloat(data[off + 2]);
            if (z <= 0.5f + 0.01f) return false;
        }
        return true;
    }

    // ── Surface: синтез поверхности расплава ──────────────────────────

    /**
     * Поверхность расплава: один горизонтальный квад 2×2 (x ±0.9, z ±0.999),
     * y = 2.3 + level, fullbright, спрайт lava_gray, цвет расплава запечён
     * в вершины (RGBA, alpha 255). Шаблон формата — первый квад части "plate".
     */
    private static List<BakedQuad> surfaceQuads(MachineStrandCasterBlockEntity be) {
        if (be.amount == 0 || be.type == null) return List.of();

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
