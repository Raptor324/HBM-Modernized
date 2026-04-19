package com.hbm_m.client.render;

import com.hbm_m.client.render.shader.IrisExtendedShaderAccess;
import com.hbm_m.client.render.shader.IrisPhaseGuard;
import com.hbm_m.client.render.shader.IrisRenderBatch;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
@OnlyIn(Dist.CLIENT)
public abstract class SingleMeshVboRenderer extends AbstractGpuMesh {

    /** Optional companion mesh in Iris-extended {@code NEW_ENTITY} format, lazy-built. */
    @Nullable
    private IrisCompanionMesh irisCompanion;
    private boolean irisCompanionAttempted;

    protected abstract VboData buildVboData();

    /**
     * Квады для Iris-совместимого пути (BufferBuilder + GameRenderer shader).
     * Переопределяется в рендерерах, созданных через GlobalMeshCache.
     */
    protected List<BakedQuad> getQuadsForIrisPath() {
        return null;
    }

    static final class TextureBinder {
        static void bindForModelIfNeeded(ShaderInstance shader) {
            var minecraft = Minecraft.getInstance();
            var textureManager = minecraft.getTextureManager();

            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
            var blockAtlas = textureManager.getTexture(TextureAtlas.LOCATION_BLOCKS);
            RenderSystem.bindTexture(blockAtlas.getId());
        }
    }

    private boolean shouldRenderWithCulling(BlockPos blockPos, @Nullable BlockEntity blockEntity) {
        if (blockEntity == null || blockEntity.getLevel() == null) {
            return true;
        }

        AABB renderBounds = blockEntity.getRenderBoundingBox();
        return OcclusionCullingHelper.shouldRender(blockPos, blockEntity.getLevel(), renderBounds);
    }

    @Nullable
    private IrisCompanionMesh getOrBuildIrisCompanion() {
        if (irisCompanion != null && irisCompanion.isBuilt()) return irisCompanion;
        if (irisCompanion != null && irisCompanion.isFailed()) return null;
        if (irisCompanionAttempted && irisCompanion == null) return null;

        List<BakedQuad> quads = getQuadsForIrisPath();
        if (quads == null || quads.isEmpty()) {
            irisCompanionAttempted = true;
            return null;
        }
        if (irisCompanion == null) {
            irisCompanion = new IrisCompanionMesh(quads);
            irisCompanionAttempted = true;
        }
        return irisCompanion.ensureBuilt() ? irisCompanion : null;
    }

    protected void initVbo() {
        if (initialized) return;

        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        int previousElementArrayBuffer = GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);

        VboData data = null;

        try {
            vaoId = GL30.glGenVertexArrays();
            vboId = GL15.glGenBuffers();

            data = buildVboData();
            if (data == null) {
                MainRegistry.LOGGER.warn("VboData is null, cannot initialize VBO");
                throw new IllegalStateException("VboData is null");
            }
            indexCount = data.indices != null ? data.indices.remaining() : 0;

            GL30.glBindVertexArray(vaoId);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data.byteBuffer, GL15.GL_STATIC_DRAW);

            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 32, 0);

            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 32, 12);

            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, 32, 24);

            if (data.indices != null && data.indices.remaining() > 0) {
                eboId = GL15.glGenBuffers();
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboId);
                GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, data.indices, GL15.GL_STATIC_DRAW);
            }

            GL30.glBindVertexArray(0);

            data.close();

            initialized = true;

        } catch (Exception e) {
            if (data != null) {
                data.close();
            }

            if (vaoId != -1) {
                GL30.glDeleteVertexArrays(vaoId);
                vaoId = -1;
            }
            if (vboId != -1) {
                GL15.glDeleteBuffers(vboId);
                vboId = -1;
            }
            if (eboId != -1) {
                GL15.glDeleteBuffers(eboId);
                eboId = -1;
            }

            throw e;

        } finally {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            GL30.glBindVertexArray(previousVao);

            if (previousVao == 0) {
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, previousElementArrayBuffer);
            }
        }
    }

    private void renderToBufferSource(PoseStack poseStack, int packedLight, List<BakedQuad> quads, MultiBufferSource bufferSource) {
        if (quads == null || quads.isEmpty() || bufferSource == null) return;
        var consumer = bufferSource.getBuffer(RenderType.solid());
        var pose = poseStack.last();
        for (BakedQuad quad : quads) {
            consumer.putBulkData(pose, quad, 1f, 1f, 1f, 1f, packedLight, OverlayTexture.NO_OVERLAY, false);
        }
    }

    public void render(PoseStack poseStack, int packedLight, BlockPos blockPos) {
        render(poseStack, packedLight, blockPos, null, null);
    }

    public void render(PoseStack poseStack, int packedLight, BlockPos blockPos,
                       @Nullable BlockEntity blockEntity) {
        render(poseStack, packedLight, blockPos, blockEntity, null);
    }

    public void render(PoseStack poseStack, int packedLight, BlockPos blockPos,
                       @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource) {
        if (!shouldRenderWithCulling(blockPos, blockEntity)) return;

        if (ShaderCompatibilityDetector.isExternalShaderActive()) {
            // 1) Try the Iris ExtendedShader path with our companion mesh - gives correct G-buffer
            //    output, shadow casting and pack uniforms.
            if (renderWithIrisExtended(poseStack, packedLight)) {
                return;
            }
            // 2) Fallback: classic putBulkData delegation lets Iris's pipeline render us as
            //    plain terrain quads. Used when companion mesh build failed or Iris reflection
            //    is unavailable (e.g. very old / very new Iris release we don't support yet).
            List<BakedQuad> irisQuads = getQuadsForIrisPath();
            if (irisQuads != null && bufferSource != null) {
                renderToBufferSource(poseStack, packedLight, irisQuads, bufferSource);
            }
            return;
        }

        if (!initialized && !initFailed) {
            try {
                initVbo();
            } catch (Exception e) {
                initFailed = true;
                MainRegistry.LOGGER.debug("VBO init failed (part has no geometry or other error), skipping: {}", e.getMessage());
                vaoId = -1;
                vboId = -1;
                eboId = -1;
                return;
            }
        }
        if (initFailed) return;
        if (!initialized || vaoId <= 0 || vboId <= 0) {
            return;
        }

        if (eboId <= 0 || indexCount <= 0) {
            return;
        }

        ShaderInstance shader = ModShaders.getBlockLitSimpleShader();
        if (shader == null) {
            // Shader not loaded yet (resource reload race) - fall back to putBulkData.
            List<BakedQuad> fallbackQuads = getQuadsForIrisPath();
            if (fallbackQuads != null && bufferSource != null) {
                renderToBufferSource(poseStack, packedLight, fallbackQuads, bufferSource);
            }
            return;
        }

        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        boolean previousCullFaceEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);

        try {
            RenderSystem.setShader(() -> shader);
            if (shader.MODEL_VIEW_MATRIX != null)
                shader.MODEL_VIEW_MATRIX.set(poseStack.last().pose());
            if (shader.PROJECTION_MATRIX != null)
                shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());

            float brightness = calculateBrightness(packedLight);
            var brightnessUniform = shader.getUniform("Brightness");
            if (brightnessUniform != null) {
                brightnessUniform.set(brightness);
            }

            var fogStartUniform = shader.getUniform("FogStart");
            if (fogStartUniform != null) fogStartUniform.set(RenderSystem.getShaderFogStart());
            var fogEndUniform = shader.getUniform("FogEnd");
            if (fogEndUniform != null) fogEndUniform.set(RenderSystem.getShaderFogEnd());
            var fogColorUniform = shader.getUniform("FogColor");
            if (fogColorUniform != null) {
                float[] fogColor = RenderSystem.getShaderFogColor();
                fogColorUniform.set(fogColor[0], fogColor[1], fogColor[2], fogColor[3]);
            }

            var sampler0 = shader.getUniform("Sampler0");
            if (sampler0 != null) sampler0.set(0);

            TextureBinder.bindForModelIfNeeded(shader);
            shader.apply();

            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.disableCull();

            GL30.glBindVertexArray(vaoId);
            GL11.glDrawElements(GL11.GL_TRIANGLES, indexCount, GL11.GL_UNSIGNED_INT, 0);

        } catch (Exception e) {
            MainRegistry.LOGGER.error("Error during VBO render", e);
        } finally {
            GL30.glBindVertexArray(previousVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);

            if (previousCullFaceEnabled) {
                RenderSystem.enableCull();
            } else {
                RenderSystem.disableCull();
            }

            RenderSystem.setShader(GameRenderer::getRendertypeSolidShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        }
    }

    /**
     * Render through the Iris {@code ExtendedShader} with the lazy companion mesh.
     * Returns {@code true} if rendering happened; the caller should fall through to a
     * different path on {@code false}.
     * <p>
     * <b>Fast path:</b> when an {@link IrisRenderBatch} session is currently open
     * (typically opened by the BlockEntityRenderer that wraps multiple part draws
     * for one machine), we skip the entire shader setup and only emit the per-part
     * VAO bind, ModelViewMat upload and {@code glDrawElements}. The session pays
     * the heavy {@code apply}/{@code clear} cost once for all parts in the batch
     * - see {@link IrisRenderBatch} for the full rationale.
     */
    private boolean renderWithIrisExtended(PoseStack poseStack, int packedLight) {
        IrisCompanionMesh companion = getOrBuildIrisCompanion();
        if (companion == null) {
            return false;
        }

        // Fast path: a batch session is open - every other part of the same
        // BlockEntity is draining apply()/clear() through it as well, so we
        // just submit our draw and exit. The session takes care of state
        // restoration on its own close().
        IrisRenderBatch batch = IrisRenderBatch.active();
        if (batch != null) {
            batch.drawCompanion(companion, poseStack.last().pose(), packedLight);
            return true;
        }

        boolean shadowPass = ShaderCompatibilityDetector.isRenderingShadowPass();
        ShaderInstance shader = IrisExtendedShaderAccess.getBlockShader(shadowPass);
        if (shader == null) {
            return false;
        }

        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        boolean previousCullFaceEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);

        // Neutral blockEntityId so BSL & co. don't take EMISSIVE_RECOLOR /
        // DrawEndPortal branches based on whatever BE Iris rendered last.
        int previousBlockEntityId = IrisExtendedShaderAccess.setCurrentRenderedBlockEntity(0);

        try (IrisPhaseGuard ignored = IrisPhaseGuard.pushBlockEntities()) {
            RenderSystem.setShader(() -> shader);

            if (shader.MODEL_VIEW_MATRIX != null) shader.MODEL_VIEW_MATRIX.set(poseStack.last().pose());
            if (shader.PROJECTION_MATRIX != null) shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());

            var brightnessUniform = shader.getUniform("Brightness");
            if (brightnessUniform != null) brightnessUniform.set(calculateBrightness(packedLight));

            var sampler0 = shader.getUniform("Sampler0");
            if (sampler0 != null) sampler0.set(0);

            // ExtendedShader.apply() reads RenderSystem.getShaderTexture(0..2)
            // and binds those IDs to the IrisSamplers ALBEDO/OVERLAY/LIGHTMAP
            // units. Other rendering paths (Embeddium chunk uploads, particle
            // batches) can leave wrong IDs in those slots, which would cause
            // the pack shader to sample the lightmap as the albedo and render
            // the model as a solid orange. Explicitly re-point the slots to
            // the correct atlas/overlay/lightmap textures before apply().
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
            Minecraft.getInstance().gameRenderer.overlayTexture().setupOverlayColor();
            Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
            TextureBinder.bindForModelIfNeeded(shader);
            shader.apply();

            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.disableCull();

            GL30.glBindVertexArray(companion.getVaoId());

            // Bind the Iris-extended attributes (iris_Entity, mc_midTexCoord,
            // at_tangent) to their linker-resolved locations on this VAO with
            // pointers into our VBO at the correct byte offsets. Iris's
            // MixinBufferBuilder.iris$beforeNext already populated the VBO with
            // valid per-vertex data for these attributes, so once bound at the
            // location the GLSL linker actually picked, the shader reads stable
            // real data and is no longer susceptible to "current value bank"
            // pollution from Embeddium chunk uploads, redstone particle batches
            // or any other immediate-mode draw - the root cause of the
            // intermittent broken-geometry symptom near torches and powered
            // redstone components. Cached per program ID; F3+T re-link
            // automatically invalidates by minting a new ID.
            companion.prepareForShader(shader.getId());

            // Per-draw lightmap: feed the pack shader's `vaUV2` via the disabled
            // attribute's generic constant - see InstancedStaticPartRenderer for
            // the full rationale.
            int uv2Loc = companion.getUv2Location();
            if (uv2Loc != -1) {
                // Pack-shader vaUV2 is ivec2 → must use the integer attribute bank.
                int blockU = Math.max(0, Math.min(240, packedLight & 0xFFFF));
                int skyV   = Math.max(0, Math.min(240, (packedLight >>> 16) & 0xFFFF));
                GL30.glVertexAttribI2i(uv2Loc, blockU, skyV);
            }

            GL11.glDrawElements(GL11.GL_TRIANGLES, companion.getIndexCount(), GL11.GL_UNSIGNED_INT, 0);
            shader.clear();
            return true;
        } catch (Exception e) {
            MainRegistry.LOGGER.error("SingleMeshVboRenderer.renderWithIrisExtended failed", e);
            return false;
        } finally {
            GL30.glBindVertexArray(previousVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            if (previousCullFaceEnabled) RenderSystem.enableCull();
            else RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getRendertypeSolidShader);
            IrisExtendedShaderAccess.restoreCurrentRenderedBlockEntity(previousBlockEntityId);
        }
    }

    private float calculateBrightness(int packedLight) {
        int blockLight = LightTexture.block(packedLight);
        int skyLight = LightTexture.sky(packedLight);

        var level = Minecraft.getInstance().level;
        if (level == null) {
            return Math.max(0.05f, Math.max(blockLight, skyLight) / 15.0f);
        }

        float skyDarken = level.getSkyDarken(1.0f);
        float skyBrightness = 0.05f + (skyDarken * 0.95f);

        float effectiveSkyLight = skyLight * skyBrightness;
        float maxLight = Math.max(blockLight, effectiveSkyLight);

        return 0.05f + (maxLight / 15.0f) * 0.95f;
    }

    @Override
    public void cleanup() {
        super.cleanup();
        IrisCompanionMesh toDestroy = this.irisCompanion;
        this.irisCompanion = null;
        if (toDestroy != null) {
            toDestroy.destroy();
        }
    }

    public static class VboData implements AutoCloseable {
        public final ByteBuffer byteBuffer;
        public final IntBuffer indices;
        private boolean consumed = false;

        public VboData(ByteBuffer byteBuffer, IntBuffer indices) {
            this.byteBuffer = byteBuffer;
            this.indices = indices;
        }

        @Override
        public void close() {
            if (consumed) {
                return;
            }
            consumed = true;
            if (byteBuffer != null) {
                MemoryUtil.memFree(byteBuffer);
            }
            if (indices != null) {
                MemoryUtil.memFree(indices);
            }
        }
    }
}
