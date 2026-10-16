package com.hbm_m.client.render.machine;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.hbm_m.client.render.ClientRenderFlags;
import com.hbm_m.client.render.InstancedGlCompat;
import com.hbm_m.client.render.InstancedStaticPartRenderer;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.PartGeometry;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * GPU-держатель ОДНОЙ части станка, создаваемый движком автоматически.
 * <p>
 * Цепочка деградации per draw (в порядке попыток):
 * <ol>
 *   <li>MDI (внутри {@link InstancedStaticPartRenderer#flush} — автоматически,
 *       если GL43/ARB доступны и нет shader pack);</li>
 *   <li>hardware instancing ({@code glDrawElementsInstanced});</li>
 *   <li>per-BE VBO ({@link SingleMeshVboRenderer});</li>
 *   <li>ванильный immediate ({@code putBulkData}) — также принудительно через
 *       {@code forceVanillaImmediatePath}.</li>
 * </ol>
 * Геометрия строится один раз лениво на render thread; до этого вызовы повторяются.
 */
final class MachinePartRenderer {

    private final String key;
    private final String partName;
    private final int boneId;
    private final boolean dynamic;

    @Nullable private InstancedStaticPartRenderer instanced;
    @Nullable private SingleMeshVboRenderer single;
    @Nullable private List<BakedQuad> quads;
    private boolean attempted;

    // scratch для bone-пути
    private final Matrix4f tmpPartLocal = new Matrix4f();

    MachinePartRenderer(String key, String partName, int boneId, boolean dynamic) {
        this.key = key;
        this.partName = partName;
        this.boneId = boneId;
        this.dynamic = dynamic;
    }

    /** true, если попытка построения уже была (успешной или нет) — квад resolver больше не вызывать. */
    boolean isAttempted() { return attempted; }

    boolean matches(MachineSpec.PartDef<?> part, String cacheKey) {
        return this.key.equals(cacheKey) && this.boneId == part.boneId();
    }

    String key() { return key; }

    /** Ленивое построение. Вне render thread — просто отложить попытку. */
    void ensureBuilt(@Nullable BakedModel partModel, @Nullable List<BakedQuad> dynamicQuadsIn) {
        if (attempted) return;
        if (!RenderSystem.isOnRenderThread()) return;

        this.attempted = true;
        SingleMeshVboRenderer.VboData data = null;
        if (dynamic) {
            List<BakedQuad> resolved = (dynamicQuadsIn == null || dynamicQuadsIn.isEmpty())
                    ? List.of() : dynamicQuadsIn;
            this.quads = resolved;
            if (!resolved.isEmpty()) {
                data = PartGeometry.buildVboDataFromQuads(resolved, partName, boneId);
                this.single = MeshRenderCache.getOrCreateRendererFromQuadList(key, resolved);
            }
        } else {
            PartGeometry geo = (partModel != null)
                    ? MeshRenderCache.getOrCompilePartGeometry(key, partModel)
                    : PartGeometry.EMPTY;
            this.quads = geo.solidQuads();
            if (!geo.isEmpty()) {
                data = geo.toVboData(partName, boneId);
                this.single = MeshRenderCache.getOrCreateRenderer(key, partModel);
            }
        }

        if (data != null) {
            if (InstancedGlCompat.supportsInstancedAttributeDivisor()) {
                InstancedStaticPartRenderer r = new InstancedStaticPartRenderer(data, quads, boneId > 0);
                r.setMdiTraceTag(key);
                this.instanced = r;
            } else {
                data.close();
            }
        }
    }

    boolean hasGeometry() {
        return instanced != null || single != null || (quads != null && !quads.isEmpty());
    }

    InstancedStaticPartRenderer instanced() { return instanced; }

    /**
     * Добавляет текущий кадр-инстанс или рисует fallback-путём.
     *
     * @param poseStack  стек с блочным трансформом + аниматором части (composed pose)
     * @param blockPose  матрица блочного трансформа БЕЗ аниматора (снимок до push аниматора)
     * @param basePoseStack вспомогательный стек, last().pose() которого движок выставляет в blockPose
     * @param sharedLight общий 8-corner световой сэмпл машины (или null)
     */
    void enqueue(PoseStack poseStack, Matrix4f blockPose, PoseStack basePoseStack,
                 int packedLight, BlockPos blockPos, BlockEntity blockEntity,
                 @Nullable MultiBufferSource bufferSource, @Nullable float[] sharedLight) {
        if (ClientRenderFlags.forceVanillaImmediate()) {
            renderQuadsFallback(poseStack, packedLight, blockEntity, bufferSource);
            return;
        }
        if (instanced != null && instanced.isInitialized() && ClientRenderFlags.useInstancedBatching()) {
            if (boneId > 0) {
                // partLocal = block⁻¹ × composed (адд-метод композитит base×part сам)
                tmpPartLocal.set(blockPose).invert().mul(poseStack.last().pose());
                basePoseStack.last().pose().set(blockPose);
                instanced.addInstanceGpuBones(basePoseStack, tmpPartLocal, packedLight, blockPos,
                        blockEntity, bufferSource, sharedLight);
            } else {
                instanced.addInstance(poseStack, packedLight, blockPos, blockEntity, bufferSource, sharedLight);
            }
            return;
        }
        if (single != null) {
            single.render(poseStack, packedLight, blockPos, blockEntity, bufferSource);
            return;
        }
        renderQuadsFallback(poseStack, packedLight, blockEntity, bufferSource);
    }

    /** Ванильный immediate: универсальный последний уровень и ручной резерв из конфига. */
    void renderQuadsFallback(PoseStack poseStack, int packedLight, BlockEntity blockEntity,
                             @Nullable MultiBufferSource bufferSource) {
        if (quads == null || quads.isEmpty() || bufferSource == null) return;
        float fade = SingleMeshVboRenderer.getFadeAlpha();
        VertexConsumer consumer = bufferSource.getBuffer(fade < 0.99f ? RenderType.translucent() : RenderType.solid());
        PoseStack.Pose pose = poseStack.last();
        for (BakedQuad quad : quads) {
            RenderHooks.putBulkData(consumer, pose, quad, 1f, 1f, 1f, fade, packedLight,
                    OverlayTexture.NO_OVERLAY, false);
        }
    }

    void flush(Matrix4f projection) {
        if (instanced != null) {
            instanced.flush(projection);
        }
    }

    void clear() {
        if (instanced != null) {
            instanced.cleanup();
            instanced = null;
        }
        single = null;   // владелец — MeshRenderCache (чистится им)
        quads = null;
        attempted = false;
    }
}
