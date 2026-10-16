package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import com.hbm_m.client.render.culling.OcclusionCullingHelper;
import com.hbm_m.client.render.shader.IrisExtendedShaderAccess;
import com.hbm_m.client.render.shader.IrisPhaseGuard;
import com.hbm_m.client.render.shader.IrisRenderBatch;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.client.render.shader.ModShaders;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.platform.GlStateManager;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

/**
 * Direct GPU VBO renderer for individual machine parts and dynamic models.
 * <p>
 * Manages GPU vertex buffer objects (VBO) and vertex array objects (VAO) for standalone
 * meshes rendered outside instanced multi-draw indirect (MDI) batches. Supports:
 * <ul>
 *   <li>Vanilla OpenGL core profile rendering using {@code block_lit.vsh} / {@code block_lit.fsh}.</li>
 *   <li>Extended shader pipeline support under Iris / Oculus with lazy companion meshes and per-vertex lightmap sampling.</li>
 *   <li>Dynamic trilinear 8-corner light probing via {@link LightSampleCache}.</li>
 *   <li>Fade alpha modulation for smooth distance-based LOD dissolving.</li>
 *   <li>Specialized depth bias and overlay handling for missile tracking and in-flight models.</li>
 * </ul>
 *
 * @credit Flywheel / Iris
 */
@OnlyIn(Dist.CLIENT)
public abstract class SingleMeshVboRenderer extends AbstractGpuMesh {

    /**
     * Vertex stride for instanced/static machine part meshes:
     * pos(12) + normal(12) + uv(8) + int {@code bone_id}(4) = 36 bytes.
     * See {@code block_lit.vsh} (USE_VERTEX_BONE_ID) and bone UBO in {@link InstancedStaticPartRenderer}.
     */
    public static final int MACHINE_PART_VERTEX_STRIDE_BYTES = 36;

    /** Optional companion mesh in Iris-extended {@code NEW_ENTITY} format, lazy-built. */
    @Nullable
    private IrisCompanionMesh irisCompanion;
    private boolean irisCompanionAttempted;

    /**
     * Thread-local fade alpha for distance-based dissolve. Set by BER callers
     * via {@link #setFadeAlpha(float)} before invoking {@link #render}; the
     * value is uploaded to the {@code FadeAlpha} shader uniform and also applied
     * to the Iris putBulkData fallback path via alpha modulation.
     * Defaults to 1.0 (fully opaque). NOTE: the value persists across {@link #render}
     * calls — the renderer itself does NOT auto-reset between part renders, so a
     * BER renders multiple parts under the same fade by setting it once.
     * Restoration is centralized in {@code MachineBer.renderParts} — the single
     * convergence point of BOTH machine draw paths (the BE dispatcher via
     * {@code AbstractPartBasedRenderer.render} and the dispatcher bypass via
     * {@code NucleusDispatcherBypass.collectOne}); it snapshots the value on entry
     * and restores it in {@code finally}. Anything that renders outside that
     * hierarchy and does not set the fade itself (missiles, doors, custom world
     * renders) relies on the restored ambient value — a stale sub-1.0 alpha here
     * means blend + depthMask(false): a translucent mesh that writes no depth and
     * is painted over by later flushes (symptom: launch pad missile rendered
     * behind / flickering against farther machines).
     */
    // Fade/missile flags — rendered strictly on the Render Thread. ThreadLocal caused
    // ThreadLocalMap.getEntryAfterMiss overhead in hot render loops (~0.4% frame time).
    private static float currentFadeAlpha = 1.0f;
    private static boolean worldMissileOverlayDraw = false;
    private static boolean entityMissileDepthBias = false;
    private static final float ENTITY_MISSILE_DEPTH_FACTOR = -4.0F;
    private static final float ENTITY_MISSILE_DEPTH_UNITS = -4.0F;


    public static void setFadeAlpha(float alpha) {
        currentFadeAlpha = alpha;
    }

    public static float getFadeAlpha() {
        return currentFadeAlpha;
    }

    public static void setWorldMissileOverlayDraw(boolean enabled) {
        worldMissileOverlayDraw = enabled;
    }

    public static boolean isWorldMissileOverlayDraw() {
        return worldMissileOverlayDraw;
    }

    public static void setEntityMissileDepthBias(boolean enabled) {
        entityMissileDepthBias = enabled;
    }

    private static void beginEntityMissileDepthBias() {
        if (entityMissileDepthBias) {
            RenderSystem.enablePolygonOffset();
            RenderSystem.polygonOffset(ENTITY_MISSILE_DEPTH_FACTOR, ENTITY_MISSILE_DEPTH_UNITS);
        }
    }

    private static void endEntityMissileDepthBias() {
        if (entityMissileDepthBias) {
            RenderSystem.disablePolygonOffset();
        }
    }

    // Scratch for 8-corner trilinear uniform upload in the non-instanced path.
    // tmpLocalPose holds the per-BE transform stripped of both the camera view
    // rotation (baked in by GameRenderer.renderLevel) and the
    // (blockPos - cameraPos) offset (applied by LevelRenderer). See the long
    // comment in {@link InstancedStaticPartRenderer#addInstance} for the
    // derivation and the precision argument.
    private final Matrix4f tmpLocalPose = new Matrix4f();
    private final Matrix4f tmpInvViewRot = new Matrix4f();
    private final float[] tmpCornerUV = new float[16];
    /** Scratch MV composite for the Iris companion path (rendering is single-threaded). */
    private final Matrix4f tmpFullMv = new Matrix4f();

    // Cached block_lit uniform handles (per renderer instance, invalidated on shader relink).
    private ShaderInstance cachedBlockLitShader;
    private int cachedBlockLitProgramId = -1;
    private long cachedBlockLitPipelineGen = -1L;
    private Uniform cachedBboxMinU;
    private Uniform cachedBboxSizeU;
    private Uniform cachedLightC01;
    private Uniform cachedLightC23;
    private Uniform cachedLightC45;
    private Uniform cachedLightC67;
    private Uniform cachedFogStartU;
    private Uniform cachedFogEndU;
    private Uniform cachedFogColorU;
    private Uniform cachedFadeAlphaU;

    private void updateBlockLitUniformCache(ShaderInstance shader) {
        int programId = (shader != null) ? shader.getId() : -1;
        long pipelineGen = IrisExtendedShaderAccess.getPipelineGeneration();
        if (cachedBlockLitShader == shader
                && cachedBlockLitProgramId == programId
                && cachedBlockLitPipelineGen == pipelineGen
                && cachedBlockLitShader != null) {
            return;
        }
        cachedBlockLitShader = shader;
        cachedBlockLitProgramId = programId;
        cachedBlockLitPipelineGen = pipelineGen;
        if (shader == null) {
            cachedBboxMinU = null;
            cachedBboxSizeU = null;
            cachedLightC01 = null;
            cachedLightC23 = null;
            cachedLightC45 = null;
            cachedLightC67 = null;
            cachedFogStartU = null;
            cachedFogEndU = null;
            cachedFogColorU = null;
            cachedFadeAlphaU = null;
            return;
        }
        cachedBboxMinU = shader.getUniform("BboxMin");
        cachedBboxSizeU = shader.getUniform("BboxSize");
        cachedLightC01 = shader.getUniform("LightC01");
        cachedLightC23 = shader.getUniform("LightC23");
        cachedLightC45 = shader.getUniform("LightC45");
        cachedLightC67 = shader.getUniform("LightC67");
        cachedFogStartU = shader.getUniform("FogStart");
        cachedFogEndU = shader.getUniform("FogEnd");
        cachedFogColorU = shader.getUniform("FogColor");
        cachedFadeAlphaU = shader.getUniform("FadeAlpha");
    }

    protected abstract VboData buildVboData();

    /**
     * Baked quads for Iris-compatible fallback path (BufferBuilder + GameRenderer shader).
     * Overridden in renderers instantiated via {@link MeshRenderCache}.
     */
    protected List<BakedQuad> getQuadsForIrisPath() {
        return null;
    }

    static final class TextureBinder {
        static void bindForModelIfNeeded(ShaderInstance shader) {
            var minecraft = Minecraft.getInstance();
            var textureManager = minecraft.getTextureManager();

            // Managed binds only: raw glActiveTexture/glBindTexture desynchronizes GlStateManager's
            // texture state cache, causing subsequent vanilla managed binds to no-op (manifesting
            // as black sun/moon quads or corrupted player hand rendering).
            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
            var blockAtlas = textureManager.getTexture(TextureAtlas.LOCATION_BLOCKS);
            GlStateManager._bindTexture(blockAtlas.getId());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // REGRESSION GUARD: instanced block_lit — white models missing atlas texture
    // ═══════════════════════════════════════════════════════════════════════════
    // Symptom: instancing enabled -> all OBJ models render solid white; disabled -> normal.
    // Cause A: Sampler2 uniform = 0 -> fragment shader reads atlas instead of lightmap.
    // Cause B: turnOnLightLayer() while TEXTURE0 is active -> unit 0 atlas texture overwritten.
    // Cause C: flush executed in AFTER_LEVEL rather than AFTER_BLOCK_ENTITIES -> dirty GL slots.
    // Contract: prepareBlockLitSamplers -> apply -> bindBlockLitSamplerTextures -> draw.
    // FORBIDDEN: calling apply() alone; setSampler without GL bind; flushing at level end.
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Primes the shader's sampler map so that {@link ShaderInstance#apply()} binds the
     * block atlas to {@code Sampler0} and the dynamic lightmap to {@code Sampler2} for
     * the non-Iris {@code block_lit} pipeline. MUST be called <b>before</b>
     * {@code shader.apply()}.
     * <p>
     * <b>Why.</b> {@code ShaderInstance.apply()} iterates samplers by JSON array index
     * {@code j}, uploads {@code Sampler"j"} uniform = {@code j}, activates
     * {@code GL_TEXTURE0+j}, and binds the texture from {@code samplerMap}. If
     * {@code samplerMap.get(name)} is {@code null}, the entry is skipped — uniform
     * stays at its link-time default of 0, so every sampler reads from
     * {@code GL_TEXTURE0} (the block atlas). That's the root cause of the
     * "everything is solid white" regression: {@code texture(Sampler2, lightmapUV)}
     * was reading the atlas at {@code uv ≈ (0.97, 0.97)} — a near-white atlas
     * corner — instead of the lightmap.
     * <p>
     * Vanilla's {@code VertexBuffer._drawWithShader} solves this by calling
     * {@code shader.setSampler("Sampler" + i, RenderSystem.getShaderTexture(i))} for
     * {@code i = 0..11} before {@code apply()}. We replicate that here because our
     * render path goes straight through {@code glDrawElements} and bypasses
     * {@code VertexBuffer}.
     * <p>
     * For JSON samplers {@code ["Sampler0", "Sampler2"]}:
     * <ul>
     *   <li>{@code j=0}: {@code Sampler0} uniform = 0, atlas bound to {@code GL_TEXTURE0}</li>
     *   <li>{@code j=1}: {@code Sampler2} uniform = 1, lightmap bound to {@code GL_TEXTURE1}</li>
     * </ul>
     * The shader's {@code texture(Sampler2, lightmapUV)} then correctly reads
     * {@code GL_TEXTURE1} (the lightmap) via the {@code Sampler2} uniform value of 1.
     * The name "Sampler2" is purely cosmetic — vanilla uses the same convention for
     * {@code rendertype_solid}.
     */
    public static void prepareBlockLitSamplers(ShaderInstance shader) {
        if (shader == null) {
            return;
        }
        RenderFrameLight.ensureLightTextureUpdated();
        primeBlockLitSamplerMap(shader, Minecraft.getInstance());
    }

    /**
     * Fills {@code samplerMap} so {@link ShaderInstance#apply()} binds atlas → unit 0 and
     * lightmap → unit 1. Mirrors {@code LevelRenderer} / {@code VertexBuffer._drawWithShader}.
     */
    /**
     * Iris variant of {@link #prepareBlockLitSamplers} for custom ExtendedShader
     * (hbm_m:iris/block_lit_instanced_iris): samplers in JSON are named with prefix
     * {@code iris_} (ExtendedShader.getUniform() resolves "iris_" + name), so
     * {@code setSampler("Sampler0")} would miss the mapping, resulting in black output.
     * GL unit activation order matches primeBlockLitSamplerMap.
     */
    public static void primeIrisInstancedSamplerMap(ShaderInstance shader, Minecraft mc) {
        var textureManager = mc.getTextureManager();

        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        textureManager.bindForSetup(TextureAtlas.LOCATION_BLOCKS);

        RenderSystem.activeTexture(GL13.GL_TEXTURE1);
        mc.gameRenderer.lightTexture().turnOnLightLayer();

        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        textureManager.bindForSetup(TextureAtlas.LOCATION_BLOCKS);

        AbstractTexture atlasTex = textureManager.getTexture(TextureAtlas.LOCATION_BLOCKS);
        int lightmapGlId = resolveLightmapGlId(mc);
        if (atlasTex != null) {
            shader.setSampler("iris_Sampler0", atlasTex);
        }
        if (lightmapGlId > 0) {
            shader.setSampler("iris_Sampler2", lightmapGlId);
        }
        // Sampler unit uniforms (MUST be called after glUseProgram of our shader).
        // NOTE: getUniform on ExtendedShader automatically prefixes "iris_" — pass
        // BASE names, otherwise looking for "iris_iris_Sampler2" fails and uniform remains 0
        // (GLSL default 0 = atlas bound to unit 0 instead of lightmap = pitch black machines).
        com.mojang.blaze3d.shaders.Uniform s0 = shader.getUniform("Sampler0");
        if (s0 != null) { s0.set(0); s0.upload(); }
        com.mojang.blaze3d.shaders.Uniform s2 = shader.getUniform("Sampler2");
        if (s2 != null) { s2.set(1); s2.upload(); }
    }

    private static void primeBlockLitSamplerMap(ShaderInstance shader, Minecraft mc) {
        var textureManager = mc.getTextureManager();

        // DO NOT call turnOnLightLayer() while TEXTURE0 is active — it will overwrite atlas (white models).
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        textureManager.bindForSetup(TextureAtlas.LOCATION_BLOCKS);

        RenderSystem.activeTexture(GL13.GL_TEXTURE1);
        mc.gameRenderer.lightTexture().turnOnLightLayer();

        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        textureManager.bindForSetup(TextureAtlas.LOCATION_BLOCKS);

        AbstractTexture atlasTex = textureManager.getTexture(TextureAtlas.LOCATION_BLOCKS);
        int lightmapGlId = resolveLightmapGlId(mc);
        if (atlasTex != null) {
            shader.setSampler("Sampler0", atlasTex);
        } else {
            int atlasGlId = resolveBlockAtlasGlId(mc);
            if (atlasGlId > 0) {
                shader.setSampler("Sampler0", atlasGlId);
            }
        }
        // JSON sampler name "Sampler2" != GL_TEXTURE2: apply() maps it to unit 1 (index j in array).
        if (lightmapGlId > 0) {
            shader.setSampler("Sampler2", lightmapGlId);
        }
        // Parametric joint specs (attrib 15 AnimParams.w): the 4th JSON sampler -
        // setSampler registers it; NucleusJointSpecs.applyBinding pins the actual
        // unit (SAMPLER_UNIT = 4) because apply()'s declaration-index mapping is
        // unreliable. Id 0 (no joints) is valid: a joint delta only executes when
        // AnimParams.w >= 0.
        shader.setSampler("uJointSpecs", NucleusJointSpecs.getTextureId());
    }

    /** GL texture id for the block atlas — never trust slot 0 alone after chunk/MDI draws. */
    private static int resolveBlockAtlasGlId(Minecraft mc) {
        AbstractTexture atlas = mc.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
        if (atlas != null) {
            return atlas.getId();
        }
        int fromSlot = RenderSystem.getShaderTexture(0);
        return fromSlot > 0 ? fromSlot : -1;
    }

    /**
     * GL texture id for the dynamic lightmap. {@link LightTexture#turnOnLightLayer()} must run
     * while {@code GL_TEXTURE1} is active (see {@link #prepareBlockLitSamplers}).
     */
    private static int resolveLightmapGlId(Minecraft mc) {
        int fromSlot = RenderSystem.getShaderTexture(2);
        if (fromSlot > 0) {
            return fromSlot;
        }
        RenderSystem.activeTexture(GL13.GL_TEXTURE1);
        int bound = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        return bound > 0 ? bound : -1;
    }

    /**
     * Re-binds block atlas ({@code Sampler0} → {@code GL_TEXTURE0}) and dynamic lightmap
     * ({@code Sampler2} → {@code GL_TEXTURE1}) immediately before instanced draws.
     * <p>
     * {@link ShaderInstance#apply()} maps JSON sampler index {@code j} to {@code GL_TEXTURE0 + j}
     * (so {@code Sampler2} uses unit 1, not 2). If {@code Sampler2} stays at 0, the fragment
     * shader samples the atlas near {@code (0.97, 0.97)} and machines look solid white.
     * VAO / instance-buffer work between {@code apply()} and the draw must not rely on
     * {@code apply()} alone — force GL binds here.
     * <p>
     * Must be invoked <b>after</b> {@link ShaderInstance#apply()} and <b>before</b> any instanced glDraw*.
     * Omitting this call causes white models (see REGRESSION GUARD above).
     */
    public static void bindBlockLitSamplerTextures(ShaderInstance shader) {
        if (shader == null) return;
        RenderSystem.assertOnRenderThread();
        Minecraft mc = Minecraft.getInstance();

        int atlasGlId = resolveBlockAtlasGlId(mc);
        int lightmapGlId = resolveLightmapGlId(mc);
        if (atlasGlId <= 0 || lightmapGlId <= 0) return;

        // TU0: Atlas (managed bind ensures GlStateManager cache stays valid)
        GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        GlStateManager._bindTexture(atlasGlId);

        // TU1: Overlay
        GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE1);
        mc.gameRenderer.overlayTexture().setupOverlayColor(); // Internally uses GlStateManager

        // TU2: Lightmap
        GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE2);
        GlStateManager._bindTexture(lightmapGlId);

        // ALWAYS restore active texture and state back to TU0 for the rest of game rendering!
        GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);

        shader.setSampler("Sampler0", atlasGlId);
        shader.setSampler("Sampler2", lightmapGlId);

        // Explicit sampler-unit assignment by raw location. ShaderInstance.apply()
        // maps declared samplers by declaration INDEX and silently drops entries
        // absent from the GLSL (Sampler1 is not declared in block_lit.fsh - see the
        // registration warn), so every later sampler shifts down a unit: Sampler2
        // landed on TU1 (the overlay) and uJointSpecs on TU2 (the lightmap) - the
        // joint specs fetch read lightmap pixels and parametric joints went wild
        // the moment their angle left identity. The program is still active here
        // (this runs right after apply()); uniform values persist per program.
        int program = shader.getId();
        GL20.glUniform1i(GL20.glGetUniformLocation(program, "Sampler0"), 0);
        GL20.glUniform1i(GL20.glGetUniformLocation(program, "Sampler2"), 2);
        NucleusJointSpecs.applyBinding(program);

        var uSampler0 = shader.getUniform("Sampler0");
        if (uSampler0 != null) uSampler0.set(0);
        var uSampler1 = shader.getUniform("Sampler1");
        if (uSampler1 != null) uSampler1.set(1);
        var uSampler2 = shader.getUniform("Sampler2");
        if (uSampler2 != null) uSampler2.set(2);
    }

    private boolean shouldRenderWithCulling(BlockPos blockPos, @Nullable BlockEntity blockEntity) {
        if (blockEntity == null || blockEntity.getLevel() == null) {
            return true;
        }

        AABB renderBounds = worldBoundsFromMesh(blockEntity);
        return OcclusionCullingHelper.shouldRender(blockPos, blockEntity.getLevel(), renderBounds);
    }

    /** Computes world-space AABB from BlockEntity position and object-space {@link #objBbox}. */
    private AABB worldBoundsFromMesh(BlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        return new AABB(
                pos.getX() + objBbox[0], pos.getY() + objBbox[1], pos.getZ() + objBbox[2],
                pos.getX() + objBbox[3], pos.getY() + objBbox[4], pos.getZ() + objBbox[5]
        );
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
            setObjBboxFrom(data);

            int vs = data.bytesPerVertex;

            GL30.glBindVertexArray(vaoId);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data.byteBuffer, GL15.GL_STATIC_DRAW);

            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, vs, 0);

            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, vs, 12);

            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, vs, 24);

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
        float fade = currentFadeAlpha;
        var consumer = bufferSource.getBuffer(fade < 0.99f ? RenderType.translucent() : RenderType.cutout());
        var pose = poseStack.last();
        for (BakedQuad quad : quads) {
            RenderHooks.putBulkData(consumer, pose, quad, 1f, 1f, 1f, fade, packedLight, OverlayTexture.NO_OVERLAY, false);
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
        // Per-part culling removed: all current call sites (multiblock BERs)
        // perform a single per-BlockEntity OcclusionCullingHelper.shouldRender()
        // check with the full multiblock AABB BEFORE invoking render() on each
        // part. The per-mesh AABB computed by worldBoundsFromMesh() is much
        // smaller and does not cover the structure's dummy blocks, so the
        // structure's own solid blocks register as occluders and cause the
        // model to flicker when the camera moves. This matches the same fix
        // already applied in InstancedStaticPartRenderer.addInstance().

        if (ShaderCompatibilityDetector.isExternalShaderActive()) {
            // 1) Iris ExtendedShader path through our companion mesh.
            if (renderWithIrisExtended(poseStack, packedLight, blockPos, blockEntity)) {
                return;
            }
            // 2) Fallback: classic putBulkData delegation lets Iris's pipeline render us as
            //    plain terrain quads. Used when companion mesh build failed or Iris reflection
            //    is unavailable.
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
        // Snapshot depth state: the try body force-sets depthFunc/depthMask/depthTest
        // for both the overlay and the normal draw path. Previously only the overlay
        // path restored depth in finally, so the normal path left the caller's
        // depthFunc (GL_LEQUAL) and depthMask(true) permanently clobbered.
        int previousDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean previousDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean previousDepthTestEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);

        ShaderInstance previousShader = RenderSystem.getShader();
        int previousTexture0 = RenderSystem.getShaderTexture(0);

        try {
            RenderSystem.setShader(() -> shader);
            // Camera rotation source for missile meshes: ambient RenderSystem ModelViewMat
            // under Oculus (even with shaderpack disabled) can be overwritten with identity
            // by external bookkeeping inside our push window (vbo.mvm diagnostics:
            // rsMV=identity in tracking frames). Therefore, in tracking context we take
            // the captured copy from RenderHooks; where ambient is valid, results are identical.
            // Machine/door BERs (flag false) continue reading ambient = R_cam of BE phase.
            org.joml.Matrix4f fullModelView;
            if (entityMissileDepthBias) {
                org.joml.Matrix4f levelRot =
                        com.hbm_m.platform.RenderHooks.currentLevelRotation();
                if (levelRot != null) {
                    // Track path: camera rotation is ALREADY baked into poseStack
                    // (MissileTrackWorldRender multiplies R_cam when building stack —
                    // in clean vanilla ambient MV at AFTER_WEATHER is identity,
                    // multiplication would yield double rotation / garbage).
                    fullModelView = new Matrix4f(poseStack.last().pose());
                } else {
                    fullModelView = new org.joml.Matrix4f(RenderSystem.getModelViewMatrix())
                            .mul(poseStack.last().pose());
                }
            } else {
                fullModelView = new org.joml.Matrix4f(RenderSystem.getModelViewMatrix())
                        .mul(poseStack.last().pose());
            }
            if (shader.MODEL_VIEW_MATRIX != null)
                shader.MODEL_VIEW_MATRIX.set(fullModelView);
            if (shader.PROJECTION_MATRIX != null)
                shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());

            // Lighting uniforms: either 8-corner trilinear or 2x4x2 sliced probes.
            //
            // The BER poseStack carries BOTH the camera view rotation (baked
            // in by GameRenderer.renderLevel before calling LevelRenderer) AND
            // the per-dispatch translate(blockPos - cameraPos) Mojang applies
            // in LevelRenderer's block-entities loop:
            //
            //   mat = viewRot * T( (float)(blockPos - cameraPos) ) * perBELocal
            //
            // Naively composing T(cameraPos) * mat leaves an extra viewRot
            // between cameraPos and the offset: the 8 sampled corners rotate
            // with the camera and drift into opaque blocks / underground /
            // sky (symptom: "models darken from the bottom up when looking
            // up"). Even after stripping viewRot, building a full absolute
            // world pose inside a Matrix4f loses float32 precision at large
            // camera offsets - (float)cameraPos + (float)(blockPos - cameraPos)
            // doesn't exactly equal (float)blockPos at cameraPos > 10^4, and
            // Mth.floor on the composed translation jitters between adjacent
            // blocks as the player moves sub-block distances (symptom:
            // "model shimmers between light and dark near a torch").
            //
            // Fix: keep the math block-relative. Strip viewRot using
            // RenderSystem.getInverseViewRotationMatrix() (Mojang stamps this
            // right before dispatching the level), then subtract the
            // (blockPos - cameraPos) translation column using the EXACT same
            // float cast LevelRenderer used - rounding errors cancel
            // bit-for-bit and we end up with a clean perBELocal matrix.
            // LightSampleCache then derives world sample positions as
            // blockPos.getX() + floor(perBELocal * corner.x), with no
            // absolute-world float arithmetic in the flooring step.
            //
            // See the matching (more detailed) comment in
            // InstancedStaticPartRenderer.addInstance.
            long partHash = System.identityHashCode(this);
            if (LightSampleCache.BASE_POSE_SET.get()) {
                tmpLocalPose.set(LightSampleCache.BASE_POSE.get()).invert().mul(poseStack.last().pose());
            } else {
                var cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
                //? if < 1.21.1 {
                tmpInvViewRot.identity().set(RenderSystem.getInverseViewRotationMatrix());
                //?} else {
                /*// rotation(camera.rotation()) = R_cam⁻¹ without extra invert (see
                // FrameViewState.capture) — earlier extraneous invert rotated 8-corner light sample points.
                tmpInvViewRot.identity().rotation(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
                *///?}
                tmpLocalPose.set(tmpInvViewRot).mul(poseStack.last().pose());
                tmpLocalPose.m30(tmpLocalPose.m30() - (float) (blockPos.getX() - cam.x));
                tmpLocalPose.m31(tmpLocalPose.m31() - (float) (blockPos.getY() - cam.y));
                tmpLocalPose.m32(tmpLocalPose.m32() - (float) (blockPos.getZ() - cam.z));
            }
            LightSampleCache.getOrSample8(blockEntity, partHash, objBbox, blockPos,
                                          tmpLocalPose, packedLight, tmpCornerUV);

            updateBlockLitUniformCache(shader);
            if (cachedBboxMinU != null) cachedBboxMinU.set(objBbox[0], objBbox[1], objBbox[2]);
            if (cachedBboxSizeU != null) {
                cachedBboxSizeU.set(
                    Math.max(1e-4f, objBbox[3] - objBbox[0]),
                    Math.max(1e-4f, objBbox[4] - objBbox[1]),
                    Math.max(1e-4f, objBbox[5] - objBbox[2])
                );
            }
            if (cachedLightC01 != null) cachedLightC01.set(tmpCornerUV[0], tmpCornerUV[1], tmpCornerUV[2], tmpCornerUV[3]);
            if (cachedLightC23 != null) cachedLightC23.set(tmpCornerUV[4], tmpCornerUV[5], tmpCornerUV[6], tmpCornerUV[7]);
            if (cachedLightC45 != null) cachedLightC45.set(tmpCornerUV[8], tmpCornerUV[9], tmpCornerUV[10], tmpCornerUV[11]);
            if (cachedLightC67 != null) cachedLightC67.set(tmpCornerUV[12], tmpCornerUV[13], tmpCornerUV[14], tmpCornerUV[15]);

            if (worldMissileOverlayDraw || entityMissileDepthBias) {
                if (cachedFogStartU != null) cachedFogStartU.set(1.0E8F);
                if (cachedFogEndU != null) cachedFogEndU.set(1.0E8F);
            } else {
                if (cachedFogStartU != null) cachedFogStartU.set(RenderSystem.getShaderFogStart());
                if (cachedFogEndU != null) cachedFogEndU.set(RenderSystem.getShaderFogEnd());
            }
            if (cachedFogColorU != null) {
                float[] fogColor = RenderSystem.getShaderFogColor();
                cachedFogColorU.set(fogColor[0], fogColor[1], fogColor[2], fogColor[3]);
            }

            if (cachedFadeAlphaU != null) cachedFadeAlphaU.set(currentFadeAlpha);

            // Must come BEFORE apply() - apply() reads samplerMap populated here and
            // does glUseProgram + glUniform1i + glBindTexture in one shot.
            prepareBlockLitSamplers(shader);
            // PROGRAM CACHE DESYNC PROTECTION (Oculus with shaderpack disabled):
            // VanillaRenderingPipeline.beginLevelRendering() once per frame invokes raw
            // GlStateManager._glUseProgram(0) without clearing static ShaderInstance.lastProgramId.
            // If the previous frame ended on our block_lit, subsequent shader.apply() would SKIP
            // the real glUseProgram (cache match), directing all glUniform/glDrawElements to program 0
            // ("No active program", producing black meshes with corrupted matrices).
            // ShaderBindResync.ensureFreshBind performs an explicit check and forces a clean re-bind.
            com.hbm_m.client.render.shader.ShaderBindResync.ensureFreshBind(shader);
            shader.apply();
            bindBlockLitSamplerTextures(shader);

            float fade = currentFadeAlpha;
            boolean overlay = worldMissileOverlayDraw;
            if (overlay) {
                RenderSystem.disableDepthTest();
                // Managed call: raw GL11.glDepthMask bypassed GlStateManager cache, desynchronizing
                // depthMask for subsequent translucent draws (particles writing to depth buffer).
                RenderSystem.depthMask(false);
            } else {
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
                RenderSystem.depthMask(true);
            }
            RenderSystem.disableCull();
            if (fade < 0.99f) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                // Translucent immediate parts do not write depth: this draw dispatches from BER phase,
                // BEFORE the instanced/MDI flush of opaque bases, and writing depth would depth-reject
                // opaque base geometry (showing chunk through machine). previousDepthMask restored in finally.
                RenderSystem.depthMask(false);
            }

            GlVaoSafety.bindVertexArray(vaoId);
            beginEntityMissileDepthBias();
            GL11.glDrawElements(GL11.GL_TRIANGLES, indexCount, GL11.GL_UNSIGNED_INT, 0);
            endEntityMissileDepthBias();

            if (fade < 0.99f) {
                RenderSystem.disableBlend();
            }

        } catch (Exception e) {
            MainRegistry.LOGGER.error("Error during VBO render", e);
        } finally {
            GlVaoSafety.bindVertexArray(0);
            if (previousShader != null) {
                RenderSystem.setShader(() -> previousShader);
            }
            RenderSystem.setShaderTexture(0, previousTexture0);

            // IMPORTANT: DO NOT unbind TU1/TU2. Raw glBindTexture(0) left GlStateManager cache with
            // "live" overlay/lightmap textures — subsequent managed binds no-oped, causing subsequent
            // draws (hand, Fast GUI, next frame sky) to sample empty textures. Managed binds in
            // bindBlockLitSamplerTextures point to valid vanilla textures, so state leakage is harmless.

            GlVaoSafety.bindVertexArray(previousVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);

            if (previousCullFaceEnabled) {
                RenderSystem.enableCull();
            } else {
                RenderSystem.disableCull();
            }
            RenderSystem.depthFunc(previousDepthFunc);
            // Managed restoration — symmetric with setup above.
            RenderSystem.depthMask(previousDepthMask);
            if (previousDepthTestEnabled) {
                RenderSystem.enableDepthTest();
            } else {
                RenderSystem.disableDepthTest();
            }
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
    private boolean renderWithIrisExtended(PoseStack poseStack, int packedLight,
                                           BlockPos blockPos, @Nullable BlockEntity blockEntity) {
        // Shadow pass: only through ACTIVE per-BE batch (see IrisRenderBatch.begin — non-persistent,
        // closed before BER returns). Standalone path (apply per part) in shadow pass is prohibited:
        // ExtendedShader.clear() teardown rebinds MAIN FBO, whose spurious dispatches during the
        // main pass caused duplicated foliage on 1.20.1. Without batching, return false to route
        // through bufferSource (Iris renders using SHADOW_BLOCK program on endBatch).
        if (ShaderCompatibilityDetector.isRenderingShadowPass()) {
            if (IrisShadowBatchCollector.isBatchingEnabled()) {
                IrisCompanionMesh companionMesh = getOrBuildIrisCompanion();
                if (companionMesh != null && companionMesh.isBuilt()) {
                    Matrix4f currentMv = RenderSystem.getModelViewMatrix();
                    IrisShadowBatchCollector.stashShadowMatrices(RenderSystem.getProjectionMatrix());
                    Matrix4f shadowWorld = new Matrix4f(currentMv).invertAffine().mul(new Matrix4f(currentMv).mul(poseStack.last().pose()));
                    IrisShadowBatchCollector.recordCustomMesh(companionMesh, shadowWorld);
                    return true;
                }
            }
            if (IrisRenderBatch.active() == null) {
                return false;
            }
        }

        IrisCompanionMesh companion = getOrBuildIrisCompanion();
        if (companion == null) {
            return false;
        }

        // Skip 8-corner sampling entirely during Iris's shadow pass. Shadow
        // maps are depth-only and pack shadow programs ignore vaUV2; the
        // sampling also populates LightSampleCache under the shadow camera's
        // RenderSystem state, which the main pass then re-uses from the same
        // frame and renders incorrect block-light gradients with (symptom:
        // "the bright stripe runs sideways across a row of machines when I
        // pitch the camera up/down"). See IrisRenderBatch.drawCompanionWith-
        // PerVertexLight for the matching short-circuit on the draw side.
        boolean shadowPassEarly = ShaderCompatibilityDetector.isRenderingShadowPass();
        IrisRenderBatch batchEarly = IrisRenderBatch.active();
        if (batchEarly != null) shadowPassEarly = batchEarly.isShadowPass();

        // Sample world-space light probes for this draw: 2×2×2 corners (16 floats).
        // See {@link #render} for the same localPose reconstruction as the
        // vanilla / instanced path.
        boolean haveCorners = false;
        if (!shadowPassEarly && companion.supportsPerVertexLightmap()) {
            BlockPos anchor = (blockEntity != null) ? blockEntity.getBlockPos() : blockPos;
            if (anchor == null) anchor = BlockPos.ZERO;
            if (LightSampleCache.BASE_POSE_SET.get()) {
                tmpLocalPose.set(LightSampleCache.BASE_POSE.get()).invert().mul(poseStack.last().pose());
            } else {
                var cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
                //? if < 1.21.1 {
                tmpInvViewRot.identity().set(RenderSystem.getInverseViewRotationMatrix());
                //?} else {
                /*// rotation(camera.rotation()) = R_cam⁻¹ without extra invert (see
                // FrameViewState.capture).
                tmpInvViewRot.identity().rotation(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
                *///?}
                tmpLocalPose.set(tmpInvViewRot).mul(poseStack.last().pose());
                tmpLocalPose.m30(tmpLocalPose.m30() - (float) (anchor.getX() - cam.x));
                tmpLocalPose.m31(tmpLocalPose.m31() - (float) (anchor.getY() - cam.y));
                tmpLocalPose.m32(tmpLocalPose.m32() - (float) (anchor.getZ() - cam.z));
            }

            long partHash = System.identityHashCode(this);
            LightSampleCache.getOrSample8(blockEntity, partHash, objBbox, anchor,
                                          tmpLocalPose, packedLight, tmpCornerUV);
            haveCorners = true;
        }

        // Fast path: a batch session is open - every other part of the same
        // BlockEntity is draining apply()/clear() through it as well, so we
        // just submit our draw and exit. The session takes care of state
        // restoration on its own close(). Use the per-vertex variant when we
        // successfully gathered the 8 corner samples, else fall back to the
        // legacy constant-UV2 path.
        IrisRenderBatch batch = IrisRenderBatch.active();
        if (batch != null) {
            // R_cam resides in RenderSystem.getModelViewMatrix() on BOTH versions (1.20.1 baked into
            // dispatcher pose, 1.21.1 in modelViewStack; see fix in InstancedStaticPartRenderer.addInstance).
            // Passing raw pose here on 1.21.1 without R_cam displaced models (door leaves in forced vanilla
            // immediate mode, DAE nodes).
            Matrix4f fullModelView = tmpFullMv.set(RenderSystem.getModelViewMatrix())
                    .mul(poseStack.last().pose());
            if (haveCorners) {
                batch.drawCompanionWithPerVertexLight(companion, fullModelView,
                                                      tmpCornerUV, packedLight);
            } else {
                batch.drawCompanion(companion, fullModelView, packedLight);
            }
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
        // Try block sets depthFunc/depthMask/depthTest for both branches (overlay and standard);
        // finally block restores them symmetrically.
        int previousDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean previousDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean previousDepthTestEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);

        // Neutral blockEntityId so BSL & co. don't take EMISSIVE_RECOLOR /
        // DrawEndPortal branches based on whatever BE Iris rendered last.
        int previousBlockEntityId = IrisExtendedShaderAccess.setCurrentRenderedBlockEntity(0);

        try (IrisPhaseGuard ignored = IrisPhaseGuard.pushBlockEntities()) {
            RenderSystem.setShader(() -> shader);

            if (shader.MODEL_VIEW_MATRIX != null) {
                // Track path (AFTER_WEATHER, under Iris/Oculus): R_cam is already baked into poseStack
                // (MissileTrackWorldRender), while ambient RenderSystem.getModelViewMatrix() here is NOT
                // equal to R_cam — external bookkeeping resets it to identity/garbage -> double rotation
                // or displaced mesh. Take pose directly as in the vanilla VBO path above. For BER/machines
                // (outside tracking context), ambient * pose composite remains correct (ambient = R_cam).
                if (entityMissileDepthBias
                        && com.hbm_m.platform.RenderHooks.currentLevelRotation() != null) {
                    shader.MODEL_VIEW_MATRIX.set(tmpFullMv.set(poseStack.last().pose()));
                } else {
                    shader.MODEL_VIEW_MATRIX.set(tmpFullMv.set(RenderSystem.getModelViewMatrix()).mul(poseStack.last().pose()));
                }
            }
            if (shader.PROJECTION_MATRIX != null) shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());

            var brightnessUniform = shader.getUniform("Brightness");
            if (brightnessUniform != null) brightnessUniform.set(calculateBrightness(packedLight));

            if (worldMissileOverlayDraw) {
                var fogStart = shader.getUniform("FogStart");
                if (fogStart != null) fogStart.set(1.0E8F);
                var fogEnd = shader.getUniform("FogEnd");
                if (fogEnd != null) fogEnd.set(1.0E8F);
            }

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

            com.mojang.blaze3d.platform.GlStateManager._glBindVertexArray(companion.getVaoId());
            
            if (!com.hbm_m.client.render.shader.IrisShaderApply.tryApply(shader)) {
                return false;
            }

            boolean overlay = worldMissileOverlayDraw;
            if (overlay) {
                RenderSystem.disableDepthTest();
                // Managed call instead of raw GL11.glDepthMask (see vanilla path).
                RenderSystem.depthMask(false);
            } else {
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
                RenderSystem.depthMask(true);
            }
            RenderSystem.disableCull();

            

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

            // Per-draw lightmap. Prefer the per-vertex trilinear path so the
            // pack shader gets a smooth gradient across the mesh (a torch on
            // one side of the part actually brightens just that side).
            // Falls back to the legacy constant-UV2 path when per-vertex
            // isn't available or we didn't sample the 8 corners above
            // (degenerate mesh, Iris pre-flush race).
            int uv2Loc = companion.getUv2Location();
            if (haveCorners && companion.supportsPerVertexLightmap()) {
                companion.ensureLightmapCapacity(1);
                companion.writeInstanceLightmap(0, tmpCornerUV);
                companion.finishLightmapWrites();
                companion.activatePerVertexLightmap();
                companion.bindLightmapForInstance(0);
            } else if (uv2Loc != -1) {
                companion.restoreConstantLightmap();
                int blockU = Math.max(0, Math.min(240, packedLight & 0xFFFF));
                int skyV   = Math.max(0, Math.min(240, (packedLight >>> 16) & 0xFFFF));
                companion.bindVaoIfNeeded();
                GL30.glVertexAttribI2i(uv2Loc, blockU, skyV);
            }

            beginEntityMissileDepthBias();
            companion.bindVaoIfNeeded();
            GL11.glDrawElements(GL11.GL_TRIANGLES, companion.getIndexCount(), GL11.GL_UNSIGNED_INT, 0);
            endEntityMissileDepthBias();
            shader.clear();
            return true;
        } catch (Exception e) {
            MainRegistry.LOGGER.error("SingleMeshVboRenderer.renderWithIrisExtended failed", e);
            return false;
        } finally {
            if (companion != null) {
                companion.restoreConstantLightmap();
            }
            GlVaoSafety.bindVertexArray(0);
            RenderSystem.setShader(GameRenderer::getRendertypeSolidShader);
            GlVaoSafety.bindVertexArray(previousVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            RenderSystem.depthFunc(previousDepthFunc);
            // Managed restoration — symmetric with setup above.
            RenderSystem.depthMask(previousDepthMask);
            if (previousDepthTestEnabled) {
                RenderSystem.enableDepthTest();
            } else {
                RenderSystem.disableDepthTest();
            }
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
        /** Object-space AABB of the mesh, computed once while packing vertices. */
        public final float minX, minY, minZ, maxX, maxY, maxZ;
        /** Vertex stride in byteBuffer: pos(12) + normal(12) + uv(8) + boneId(int32) = 36 bytes. */
        public final int bytesPerVertex;
        
        private final java.util.concurrent.atomic.AtomicBoolean consumed = new java.util.concurrent.atomic.AtomicBoolean(false);
        private final java.lang.ref.Cleaner.Cleanable cleanable;

        private static final java.lang.ref.Cleaner CLEANER = java.lang.ref.Cleaner.create();

        private static record NativeResourceReleaser(long bbAddress, long ibAddress) implements Runnable {
            @Override
            public void run() {
                if (bbAddress != 0L) {
                    MemoryUtil.nmemFree(bbAddress);
                }
                if (ibAddress != 0L) {
                    MemoryUtil.nmemFree(ibAddress);
                }
            }
        }

        public VboData(ByteBuffer byteBuffer, IntBuffer indices) {
            this(byteBuffer, indices, 0f, 0f, 0f, 0f, 0f, 0f, MACHINE_PART_VERTEX_STRIDE_BYTES);
        }

        public VboData(ByteBuffer byteBuffer, IntBuffer indices,
                       float minX, float minY, float minZ,
                       float maxX, float maxY, float maxZ) {
            this(byteBuffer, indices, minX, minY, minZ, maxX, maxY, maxZ, MACHINE_PART_VERTEX_STRIDE_BYTES);
        }

        public VboData(ByteBuffer byteBuffer, IntBuffer indices,
                       float minX, float minY, float minZ,
                       float maxX, float maxY, float maxZ,
                       int bytesPerVertex) {
            this.byteBuffer = byteBuffer;
            this.indices = indices;
            this.minX = minX; this.minY = minY; this.minZ = minZ;
            this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
            this.bytesPerVertex = bytesPerVertex;

            long bbAddr = byteBuffer != null ? MemoryUtil.memAddress(byteBuffer) : 0L;
            long ibAddr = indices != null ? MemoryUtil.memAddress(indices) : 0L;
            this.cleanable = (bbAddr != 0L || ibAddr != 0L) 
                    ? CLEANER.register(this, new NativeResourceReleaser(bbAddr, ibAddr)) 
                    : null;
        }

        public boolean isConsumed() {
            return consumed.get();
        }

        @Override
        public void close() {
            if (consumed.compareAndSet(false, true)) {
                if (cleanable != null) {
                    cleanable.clean();
                }
            }
        }
    }
}
