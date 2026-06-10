package com.hbm_m.client.render;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;

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
// Declares the public surface for the vanilla instanced VBO renderer —
// the path taken when Iris/Oculus is NOT the active shader pipeline.
//
// {@link InstancedStaticPartRenderer} implements this interface today; a
// dedicated extracted class may replace it later.
//
// Sister contract for the Iris path: {@link IrisCompanionMeshRenderer}.
// =====================================================================

/**
 * Contract for the <b>vanilla VBO instanced renderer</b> — the code path
 * active when no external shader pack (Iris/Oculus) is loaded.
 * <p>
 * Implemented by {@link InstancedStaticPartRenderer}; logic may later move
 * into a dedicated class without changing this API surface.
 */
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public interface VanillaInstancedMeshRenderer {

    /**
     * Single-machine immediate draw on the vanilla instanced shader path.
     * <p>
     * Source: {@code InstancedStaticPartRenderer#renderSingle(PoseStack, int, BlockPos, BlockEntity, MultiBufferSource)}
     * — specifically the {@code --- VANILLA VBO PATH ---} branch after the
     * {@code isExternalShaderActive()} early-out.
     */
    void renderSingle(PoseStack poseStack, int packedLight, BlockPos blockPos,
                      @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource);

    /**
     * Enqueue one instance into the per-frame batch buffer for the next
     * {@link #flush(Matrix4f)}.
     * <p>
     * Source: {@code InstancedStaticPartRenderer#addInstance(PoseStack, int, BlockPos, BlockEntity, MultiBufferSource)}
     * — the vanilla accumulation portion (everything below the Iris/shadow-pass
     * early-outs, including the LightSampleCache 8/16-corner sampling and the
     * float-buffer record write at the bottom).
     */
    void addInstance(PoseStack poseStack, int packedLight, BlockPos blockPos,
                     @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource);

    /**
     * Emit a single {@code glDrawElementsInstanced} for the accumulated batch
     * using the projection matrix from the current render stage.
     * <p>
     * Source: {@code InstancedStaticPartRenderer#flush()} and
     * {@code InstancedStaticPartRenderer#flush(Matrix4f)} (the dispatch
     * entry-point), backed by
     * {@code InstancedStaticPartRenderer#flushBatchVanilla(Matrix4f)} for the
     * actual draw.
     */
    void flush(Matrix4f projectionMatrix);

    /** Convenience entry that pulls the projection from {@code RenderSystem}.
     *  Source: {@code InstancedStaticPartRenderer#flush()}. */
    void flush();

    /**
     * @return number of instances currently accumulated in the batch buffer.
     * Source: {@code InstancedStaticPartRenderer#getInstanceCount()}.
     */
    int getInstanceCount();

    /**
     * @return whether the underlying VAO/VBO/EBO triple was successfully
     * created and the renderer is ready to draw.
     * Source: {@code InstancedStaticPartRenderer#isInitialized()}.
     */
    boolean isInitialized();

    /**
     * GL resource teardown (instance VBO, instance FloatBuffer, companion mesh).
     * Source: {@code InstancedStaticPartRenderer#cleanup()}.
     */
    void cleanup();

    // -----------------------------------------------------------------
    // Internal helpers that will move with the vanilla path. Listed here
    // as documentation only — they are package-private/private today and
    // will NOT be promoted to the public interface in the migration.
    //
    //   - uploadSingleInstance(PoseStack, int, BlockEntity)
    //       Source: InstancedStaticPartRenderer#uploadSingleInstance(...).
    //       Primes index 0 of instanceBuffer for the renderSingle path.
    //
    //   - flushBatchVanilla(Matrix4f)
    //       Source: InstancedStaticPartRenderer#flushBatchVanilla(Matrix4f).
    //       Issues the actual instanced draw + restores GL state.
    //
    //   - applyCommonUniforms(ShaderInstance, Matrix4f, Matrix4f)
    //       Source: InstancedStaticPartRenderer#applyCommonUniforms(...).
    //       Shared between renderSingle and flushBatchVanilla; the Iris
    //       flush path also calls it today but will switch to its own
    //       copy after the split (see IrisCompanionMeshRenderer scaffold).
    //
    //   - updateUniformCache(ShaderInstance)
    //       Source: InstancedStaticPartRenderer#updateUniformCache(...).
    //       Pipeline-generation-aware uniform handle cache.
    //
    //   - brightnessFromUV(float, float, float)
    //   - calculateBrightness(int) / calculateBrightness(int, float)
    //       Source: same-named methods in InstancedStaticPartRenderer.
    //
    //   - supportsInstancedAttributeDivisor() (static gate)
    //   - glVertexAttribDivisorCompat / glDrawElementsInstancedCompat
    //       Source: static helpers at the top of InstancedStaticPartRenderer.
    // -----------------------------------------------------------------
}
