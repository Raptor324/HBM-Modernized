package com.hbm_m.client.render;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
*///?}

// =====================================================================
// Phase 2 split of InstancedStaticPartRenderer (contract; logic stays in class).
//
// Declares the public surface for the Iris/Oculus companion-mesh renderer
// path when an external shader pack is active.
//
// {@link InstancedStaticPartRenderer} implements this interface today.
//
// Sister contract for vanilla: {@link VanillaInstancedMeshRenderer}.
// =====================================================================

/**
 * Contract for the Iris/Oculus <b>companion-mesh renderer</b> surface of
 * {@link InstancedStaticPartRenderer}.
 * <p>
 * This path drives the {@code IrisCompanionMesh} (NEW_ENTITY format,
 * iris_Entity / mc_midTexCoord / at_tangent attributes) through Iris's
 * {@code ExtendedShader}, producing correct G-buffer / shadow-pass /
 * pack-uniform output. Implementation: {@link InstancedStaticPartRenderer}.
 */
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public interface IrisCompanionMeshRenderer {

    /**
     * Per-machine draw through the Iris {@code ExtendedShader} obtained from
     * {@code ShaderMap}. Used both as the main entry for single-instance
     * Iris draws and as the shadow-pass bypass invoked from
     * {@code addInstance} when {@code ShaderCompatibilityDetector
     * .isRenderingShadowPass()} returns true.
     * <p>
     * Source: {@code InstancedStaticPartRenderer#drawSingleWithIrisExtended(
     * PoseStack, int, BlockPos, BlockEntity)}.
     *
     * @return {@code true} if the draw was performed (companion mesh + shader
     *         resolved); {@code false} to signal the caller should fall through
     *         to the classic {@code putBulkData} path.
     */
    boolean drawSingleWithIrisExtended(PoseStack poseStack, int packedLight,
                                       BlockPos blockPos, @Nullable BlockEntity blockEntity);

    /**
     * Iris-compatible batch flush: a sequence of per-machine draws through
     * the {@code ExtendedShader}, with framebuffer/CustomUniforms hoisted
     * outside the loop and only {@code ModelViewMat} / {@code iris_*}
     * derived uniforms mutated per instance. Renders both main and shadow
     * passes so machines correctly cast shadows under shader packs.
     * <p>
     * Source: {@code InstancedStaticPartRenderer#flushBatchIris(Matrix4f)}.
     */
    void flushBatchIris(Matrix4f projectionMatrix);

    // -----------------------------------------------------------------
    // Internal helpers that will move with the Iris path. Listed here
    // as documentation only — they are package-private/private today
    // and will NOT be promoted to the public interface.
    //
    //   - getOrBuildIrisCompanion()
    //       Source: InstancedStaticPartRenderer#getOrBuildIrisCompanion().
    //       Lazily constructs the IrisCompanionMesh from quadsForIris and
    //       caches the {built|failed|attempted} tri-state.
    //
    //   - sampleCornersForSingleDraw(PoseStack, BlockPos, BlockEntity, int)
    //       Source: InstancedStaticPartRenderer#sampleCornersForSingleDraw(...).
    //       Reconstructs the per-BE local pose (strip viewRot + subtract
    //       block-relative offset) and fills tmpCornerUV via LightSampleCache.
    //
    //   - cached iris_* uniform locations:
    //       cachedLocModelViewInverse, cachedLocNormalMat, irisLocationsResolved
    //       Source: fields on InstancedStaticPartRenderer; resolved lazily
    //       inside flushBatchIris and invalidated by updateUniformCache().
    //
    //   - per-instance scratch (no GC in hot loop):
    //       irisMvFloats[16], irisMvInverseFloats[16], irisNormalMatFloats[9],
    //       irisMvInverseTmp, irisNormalTmp, irisSingleUV[2], irisQuatTmp,
    //       tmpInstanceMat
    //       Source: field block in InstancedStaticPartRenderer.
    //
    //   - per-vertex lightmap staging:
    //       instanceLightUV[], instanceLightmapSlot[], tmpCornerShort[]
    //       Source: fields + the perVertexLight pre-loop in flushBatchIris.
    //       Drives IrisCompanionMesh.allocLightmapSlot / writeInstanceLightmap
    //       / bindLightmapForInstance.
    //
    //   - shadow-pass / phase guards:
    //       IrisPhaseGuard.pushBlockEntities(),
    //       IrisRenderBatch.active() fast-path (batch.drawCompanion / batch
    //       .drawCompanionWithPerVertexLight),
    //       IrisExtendedShaderAccess.setCurrentRenderedBlockEntity(0) reset.
    //       Source: drawSingleWithIrisExtended + flushBatchIris bodies.
    // -----------------------------------------------------------------
}
