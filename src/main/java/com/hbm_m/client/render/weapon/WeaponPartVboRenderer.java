package com.hbm_m.client.render.weapon;

import java.util.List;

import org.joml.Matrix4f;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import com.hbm_m.client.render.AbstractGpuMesh;
import com.hbm_m.client.render.GlVaoSafety;
import com.hbm_m.client.render.IrisCompanionMesh;
import com.hbm_m.client.render.RenderFrameLight;
import com.hbm_m.client.render.RenderStateGuard;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.client.render.shader.IrisDerivedMatrixUniforms;
import com.hbm_m.client.render.shader.IrisExtendedShaderAccess;
import com.hbm_m.client.render.shader.IrisPhaseGuard;
import com.hbm_m.client.render.shader.ModShaders;
import com.hbm_m.client.render.shader.ShaderBindResync;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.client.weapon.GunGL;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Direct GPU VBO renderer for individual weapon parts.
 * Uses 36-byte stride off-heap memory buffers and executes via {@code weapon_lit} core shader.
 * Preserves all GunGL viewmodel properties: custom FOV projection, depth clear, GunGL modes 0/1/2,
 * color tint, and fullbright / lightmap lighting.
 *
 * <p>GUI screens draw dozens of weapons per frame, each with several parts. Consecutive parts that
 * share the same batch key (texture, GunGL mode, light, cull/depth/fullbright/gui state) reuse one
 * GL setup: the expensive full state transition (BufferSource flush, ~12 synchronous glGet queries,
 * shader/texture binds) runs once per key change instead of once per part.
 */
@OnlyIn(Dist.CLIENT)
public class WeaponPartVboRenderer extends AbstractGpuMesh {

    private SingleMeshVboRenderer.VboData pendingData;
    @Nullable
    private final List<BakedQuad> normalizedQuads;
    @Nullable
    private IrisCompanionMesh irisCompanion;
    private boolean irisCompanionAttempted;

    public WeaponPartVboRenderer(SingleMeshVboRenderer.VboData data) {
        this(data, null);
    }

    public WeaponPartVboRenderer(SingleMeshVboRenderer.VboData data, @Nullable List<BakedQuad> normalizedQuads) {
        this.pendingData = data;
        this.normalizedQuads = normalizedQuads;
    }

    @Nullable
    private IrisCompanionMesh getOrBuildIrisCompanion() {
        if (irisCompanion != null && irisCompanion.isBuilt()) return irisCompanion;
        if (irisCompanion != null && irisCompanion.isFailed()) return null;
        if (irisCompanionAttempted && irisCompanion == null) return null;
        if (normalizedQuads == null || normalizedQuads.isEmpty()) {
            irisCompanionAttempted = true;
            return null;
        }
        if (irisCompanion == null) {
            irisCompanion = new IrisCompanionMesh(normalizedQuads);
            irisCompanion.setDebugName("WeaponPart");
            irisCompanionAttempted = true;
        }
        return irisCompanion.ensureBuilt() ? irisCompanion : null;
    }

    @Override
    public void cleanup() {
        if (this.pendingData != null) {
            this.pendingData.close();
            this.pendingData = null;
        }
        IrisCompanionMesh comp = this.irisCompanion;
        this.irisCompanion = null;
        this.irisCompanionAttempted = false;
        if (comp != null) {
            comp.destroy();
        }
        super.cleanup();
    }

    // ─── Hot-path scratch & uniform cache (zero per-part allocation) ──────

    private static final Matrix4f SCRATCH_MODEL_VIEW = new Matrix4f();

    private static ShaderInstance cachedWeaponShader;
    private static int cachedWeaponProgramId = -1;
    private static com.mojang.blaze3d.shaders.Uniform cachedUColor;
    private static com.mojang.blaze3d.shaders.Uniform cachedULightUV;
    private static com.mojang.blaze3d.shaders.Uniform cachedUDiffuse;
    private static com.mojang.blaze3d.shaders.Uniform cachedULight0;
    private static com.mojang.blaze3d.shaders.Uniform cachedULight1;
    private static int cachedSampler0Loc = -1;
    private static int cachedSampler2Loc = -1;

    private static void updateWeaponUniformCache(ShaderInstance shader) {
        int progId = shader.getId();
        if (shader == cachedWeaponShader && progId == cachedWeaponProgramId) {
            return;
        }
        cachedWeaponShader = shader;
        cachedWeaponProgramId = progId;
        cachedUColor = shader.getUniform("uColor");
        cachedULightUV = shader.getUniform("uLightUV");
        cachedUDiffuse = shader.getUniform("uDiffuse");
        cachedULight0 = shader.getUniform("uLight0");
        cachedULight1 = shader.getUniform("uLight1");
        cachedSampler0Loc = GL20.glGetUniformLocation(progId, "Sampler0");
        cachedSampler2Loc = GL20.glGetUniformLocation(progId, "Sampler2");
    }

    @Override
    protected SingleMeshVboRenderer.VboData buildVboData() {
        SingleMeshVboRenderer.VboData data = this.pendingData;
        this.pendingData = null; // one-shot handover to AbstractGpuMesh.initVbo()
        if (data == null) {
            throw new IllegalStateException("WeaponPartVboRenderer: buildVboData() called twice or with null data!");
        }
        return data;
    }

    // ─── Weapon-level GL state batching ────────────────────────────────────

    private static boolean batchActive;
    private static boolean irisBatchActive;
    private static ShaderInstance irisBatchShader;
    private static IrisDerivedMatrixUniforms.Locations irisBatchLocs = IrisDerivedMatrixUniforms.Locations.NONE;
    private static RenderStateGuard irisBatchStateGuard;
    private static IrisPhaseGuard irisBatchPhaseGuard;
    private static int irisBatchPrevBeId = Integer.MIN_VALUE;
    private static int irisBatchVao = -1;
    private static boolean irisBatchShadow;
    private static ResourceLocation batchTexture;
    private static int batchMode = -1;
    private static boolean batchCull, batchDepthMask, batchShaded, batchGui;
    private static int batchLight;

    private static int previousVao, previousArrayBuffer, previousDepthFunc;
    private static boolean previousCullFaceEnabled, previousDepthMask, previousDepthTestEnabled, previousBlendEnabled;
    private static int previousSrcRgb, previousDstRgb, previousSrcAlpha, previousDstAlpha;
    private static ShaderInstance previousShader;
    private static int previousTexture0, previousTexture2;

    /** Set by {@code GunGL.buffer()} when quads are queued into the shared BufferSource;
     * the next weapon part flushes first so submission order (flashes, smoke) is kept. */
    private static boolean quadsPendingSinceBatch;

    public static void markQuadsPending() {
        quadsPendingSinceBatch = true;
    }

    /**
     * Renders this part using current GunGL lighting, overlay, and color tint states.
     */
    public void renderPart(PoseStack pose, ResourceLocation texture) {
        renderPart(pose, texture, GunGL.light(), GunGL.overlay(),
                   GunGL.red(), GunGL.green(), GunGL.blue(), GunGL.alpha());
    }

    /**
     * Renders this weapon part. Returns true if rendered via GPU VBO (either weapon_lit
     * or IrisCompanionMesh), or false if caller should fall back to immediate quads.
     */
    public boolean renderPart(PoseStack pose, ResourceLocation texture,
                              int light, int overlay, float r, float g, float b, float a) {
        boolean shadowPass = ShaderCompatibilityDetector.isRenderingShadowPass();
        boolean externalShader = ShaderCompatibilityDetector.isExternalShaderActive();
        boolean gui = GunGL.isGuiLighting();

        // Under Iris in the world / hand / shadow pass (not GUI), render via IrisCompanionMesh + ExtendedShader
        if ((externalShader || shadowPass) && !gui) {
            if (shadowPass && (GunGL.isBlend() && GunGL.isAdditive())) {
                return true; // Additive flashes do not cast shadows
            }
            return renderPartIris(pose, texture != null ? texture : GunGL.texture(), light, shadowPass, r, g, b, a);
        }

        if (!initialized && !initFailed) {
            initVbo();
        }
        if (!initialized || vaoId <= 0 || eboId <= 0 || indexCount <= 0) {
            return false;
        }

        ShaderInstance shader = ModShaders.getWeaponLitShader();
        if (shader == null) return false;

        // GunGL modes: 0 = cutout (opaque), 1 = translucent, 2 = additive emissive
        boolean blend = GunGL.isBlend();
        boolean additive = GunGL.isAdditive();
        int mode = !blend ? 0 : (additive ? 2 : 1);
        // Mode 2 (additive emissive) bypasses lightmap AND directional shading;
        // modes 0/1 apply dynamic packed light unless GunGL forces FULL_BRIGHT.
        boolean shaded = !(mode == 2 || GunGL.fullbright() || !GunGL.lighting());
        int effectiveLight = shaded ? light : LightTexture.FULL_BRIGHT;
        boolean cull = GunGL.isCull();
        boolean depthMask = GunGL.isDepthMask();
        boolean guiLighting = GunGL.isGuiLighting();
        ResourceLocation texLoc = (texture != null) ? texture : GunGL.texture();

        boolean sameBatch = batchActive
                && batchMode == mode
                && batchShaded == shaded
                && batchCull == cull
                && batchDepthMask == depthMask
                && batchGui == guiLighting
                && batchTexture != null && batchTexture.equals(texLoc);

        if (!sameBatch || quadsPendingSinceBatch) {
            endWeaponBatch();
            // Flush pending immediate BufferSource quads so GL draws don't interleave mid-batch
            flushGunBatch();
            quadsPendingSinceBatch = false;
            beginWeaponBatch(shader, texLoc, mode, effectiveLight, cull, depthMask, shaded, guiLighting);
        }

        // Per-part cost: ModelView matrix + color tint + light UV (if changed) + draw.
        updateWeaponUniformCache(shader);
        if (batchLight != effectiveLight) {
            batchLight = effectiveLight;
            if (cachedULightUV != null) {
                float blockU = Math.max(0, Math.min(240, effectiveLight & 0xFFFF));
                float skyV = Math.max(0, Math.min(240, (effectiveLight >>> 16) & 0xFFFF));
                cachedULightUV.set((blockU + 8.0f) / 256.0f, (skyV + 8.0f) / 256.0f);
                cachedULightUV.upload();
            }
        }
        SCRATCH_MODEL_VIEW.set(RenderSystem.getModelViewMatrix()).mul(pose.last().pose());
        if (shader.MODEL_VIEW_MATRIX != null) {
            shader.MODEL_VIEW_MATRIX.set(SCRATCH_MODEL_VIEW);
            shader.MODEL_VIEW_MATRIX.upload();
        }
        if (cachedUColor != null) {
            cachedUColor.set(r, g, b, a);
            cachedUColor.upload();
        }

        GlVaoSafety.bindVertexArray(vaoId);
        GL11.glDrawElements(GL11.GL_TRIANGLES, indexCount, GL11.GL_UNSIGNED_INT, 0L);
        return true;
    }

    private boolean renderPartIris(PoseStack pose, ResourceLocation texLoc, int light, boolean shadowPass,
                                   float r, float g, float b, float a) {
        IrisCompanionMesh companion = getOrBuildIrisCompanion();
        if (companion == null) {
            return false;
        }
        ShaderInstance shader = IrisExtendedShaderAccess.getBlockShader(shadowPass);
        if (shader == null) {
            return false;
        }

        boolean blend = GunGL.isBlend();
        boolean additive = GunGL.isAdditive();
        int mode = !blend ? 0 : (additive ? 2 : 1);
        boolean unshaded = (mode == 2) || GunGL.fullbright() || !GunGL.lighting();
        int effLight = unshaded ? LightTexture.FULL_BRIGHT : light;
        boolean cull = GunGL.isCull();
        boolean depthMask = GunGL.isDepthMask();

        boolean sameIrisBatch = irisBatchActive
                && irisBatchShader == shader
                && irisBatchShadow == shadowPass
                && batchMode == mode
                && batchCull == cull
                && batchDepthMask == depthMask
                && batchTexture != null && batchTexture.equals(texLoc);

        if (!sameIrisBatch || quadsPendingSinceBatch) {
            endWeaponBatch();
            flushGunBatch();
            quadsPendingSinceBatch = false;
            if (!beginIrisWeaponBatch(shader, companion, texLoc, mode, effLight, cull, depthMask, shadowPass)) {
                return false;
            }
        }

        try {
            int targetVao = companion.getVaoId();
            if (irisBatchVao != targetVao) {
                companion.bindVaoIfNeeded();
                companion.prepareForShader(shader.getId());
                companion.restoreConstantLightmap();
                irisBatchVao = targetVao;
                batchLight = Integer.MIN_VALUE;
            }

            SCRATCH_MODEL_VIEW.set(RenderSystem.getModelViewMatrix()).mul(pose.last().pose());
            IrisDerivedMatrixUniforms.uploadDerivedMatrices(irisBatchLocs, SCRATCH_MODEL_VIEW);

            int uv2Loc = companion.getUv2Location();
            if (uv2Loc != -1 && batchLight != effLight) {
                batchLight = effLight;
                int blockU = Math.max(0, Math.min(240, effLight & 0xFFFF));
                int skyV = Math.max(0, Math.min(240, (effLight >>> 16) & 0xFFFF));
                GL30.glVertexAttribI2i(uv2Loc, blockU, skyV);
            }

            // Пер-частный тинт (GunGL.color): компаньон-меш несёт в вершинах (1,1,1,1),
            // без инжекта под паком тинт терялся — ч/б casings.png + белый vaColor давали
            // полностью белые гильзы. Константный COLOR-атрибут повторно на каждый draw:
            // его текущее значение — глобальное состояние, между батчами его затирают другие.
            int colorLoc = companion.getColorLocation();
            if (colorLoc != -1) {
                GL20.glVertexAttrib4f(colorLoc, r, g, b, a);
            }
            // Страховка для паков, умножающих на ColorModulator вместо vaColor.
            if (shader.COLOR_MODULATOR != null) {
                shader.COLOR_MODULATOR.set(r, g, b, a);
                shader.COLOR_MODULATOR.upload();
            }

            GL11.glDrawElements(GL11.GL_TRIANGLES, companion.getIndexCount(), GL11.GL_UNSIGNED_INT, 0L);
            return true;
        } catch (Exception e) {
            endWeaponBatch();
            return false;
        }
    }

    private static boolean beginIrisWeaponBatch(ShaderInstance shader, IrisCompanionMesh firstCompanion,
                                                ResourceLocation texLoc, int mode, int effLight,
                                                boolean cull, boolean depthMask, boolean shadowPass) {
        irisBatchPrevBeId = IrisExtendedShaderAccess.setCurrentRenderedBlockEntity(0);
        irisBatchStateGuard = RenderStateGuard.snapshot();
        irisBatchPhaseGuard = IrisPhaseGuard.pushBlockEntities();
        try {
            Minecraft mc = Minecraft.getInstance();
            RenderSystem.setShader(() -> shader);
            if (shader.PROJECTION_MATRIX != null) {
                shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
            }
            if (shader.MODEL_VIEW_MATRIX != null) {
                shader.MODEL_VIEW_MATRIX.set(RenderSystem.getModelViewMatrix());
            }

            RenderSystem.setShaderTexture(0, texLoc);
            mc.gameRenderer.overlayTexture().setupOverlayColor();
            mc.gameRenderer.lightTexture().turnOnLightLayer();
            AbstractTexture weaponTex = mc.getTextureManager().getTexture(texLoc);
            if (weaponTex != null) {
                GlStateManager._activeTexture(GL13.GL_TEXTURE0);
                GlStateManager._bindTexture(weaponTex.getId());
            }

            firstCompanion.bindVaoIfNeeded();
            if (!ShaderBindResync.tryApply(shader)) {
                closeIrisBatchGuards();
                return false;
            }
            if (weaponTex != null) {
                GlStateManager._activeTexture(GL13.GL_TEXTURE0);
                GlStateManager._bindTexture(weaponTex.getId());
            }

            if (mode != 0) {
                RenderSystem.enableBlend();
                if (mode == 2) {
                    RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                } else {
                    RenderSystem.defaultBlendFunc();
                }
            } else {
                RenderSystem.disableBlend();
            }
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(depthMask);
            if (cull) {
                RenderSystem.enableCull();
            } else {
                RenderSystem.disableCull();
            }

            irisBatchLocs = IrisDerivedMatrixUniforms.resolve(shader);
            irisBatchShader = shader;
            irisBatchShadow = shadowPass;
            irisBatchVao = -1;
            irisBatchActive = true;
            batchTexture = texLoc;
            batchMode = mode;
            batchCull = cull;
            batchDepthMask = depthMask;
            batchLight = Integer.MIN_VALUE;
            return true;
        } catch (Exception e) {
            closeIrisBatchGuards();
            return false;
        }
    }

    private static void closeIrisBatchGuards() {
        if (irisBatchShader != null) {
            try {
                irisBatchShader.clear();
            } catch (Throwable ignored) {}
            irisBatchShader = null;
        }
        irisBatchLocs = IrisDerivedMatrixUniforms.Locations.NONE;
        irisBatchVao = -1;
        if (irisBatchPhaseGuard != null) {
            try {
                irisBatchPhaseGuard.close();
            } catch (Throwable ignored) {}
            irisBatchPhaseGuard = null;
        }
        if (irisBatchStateGuard != null) {
            try {
                irisBatchStateGuard.close();
            } catch (Throwable ignored) {}
            irisBatchStateGuard = null;
        }
        if (irisBatchPrevBeId != Integer.MIN_VALUE) {
            IrisExtendedShaderAccess.restoreCurrentRenderedBlockEntity(irisBatchPrevBeId);
            irisBatchPrevBeId = Integer.MIN_VALUE;
        }
    }

    /**
     * Full GL/RenderSystem transition into weapon-drawing state. Captures the incoming
     * state for leak-free restoration in {@link #endWeaponBatch()}.
     */
    private static void beginWeaponBatch(ShaderInstance shader, ResourceLocation texLoc, int mode,
                                         int effectiveLight, boolean cull, boolean depthMask,
                                         boolean shaded, boolean guiLighting) {
        // Capture incoming GL and RenderSystem states
        previousVao = GlVaoSafety.currentBinding();
        previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        previousCullFaceEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        previousDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        previousDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        previousDepthTestEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        previousBlendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        previousSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        previousDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        previousSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        previousDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        previousShader = RenderSystem.getShader();
        previousTexture0 = RenderSystem.getShaderTexture(0);
        previousTexture2 = RenderSystem.getShaderTexture(2);

        RenderSystem.setShader(() -> shader);
        if (shader.PROJECTION_MATRIX != null) {
            shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
        }

        // Cache all uniform handles & sampler locations once per shader program
        updateWeaponUniformCache(shader);

        // Lightmap UV (constant within the batch - effectiveLight is part of the key)
        float blockU = Math.max(0, Math.min(240, effectiveLight & 0xFFFF));
        float skyV = Math.max(0, Math.min(240, (effectiveLight >>> 16) & 0xFFFF));
        if (cachedULightUV != null) {
            cachedULightUV.set((blockU + 8.0f) / 256.0f, (skyV + 8.0f) / 256.0f);
        }

        if (cachedUDiffuse != null) {
            cachedUDiffuse.set(shaded ? 1.0f : 0.0f);
        }

        // setupInv (GUI-INVENTORY) points the model's Y axis downward -> light from "model up"
        if (cachedULight0 != null) {
            cachedULight0.set(0.2f, guiLighting ? -1.0f : 1.0f, -0.7f);
        }
        if (cachedULight1 != null) {
            cachedULight1.set(-0.2f, guiLighting ? -1.0f : 1.0f, 0.7f);
        }

        // Sampler0: standalone weapon texture
        Minecraft mc = Minecraft.getInstance();
        RenderFrameLight.ensureLightTextureUpdated();
        RenderSystem.setShaderTexture(0, texLoc);
        AbstractTexture weaponTex = mc.getTextureManager().getTexture(texLoc);
        if (weaponTex != null) {
            shader.setSampler("Sampler0", weaponTex);
        }

        // Sampler2: dynamic lightmap, or 1x1 white texture for mode 2 (NO_LIGHTMAP)
        int activeLightmapGlId;
        if (mode == 2) {
            activeLightmapGlId = resolveWhiteTextureGlId(mc);
        } else {
            RenderSystem.activeTexture(GL13.GL_TEXTURE1);
            mc.gameRenderer.lightTexture().turnOnLightLayer();
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
            activeLightmapGlId = resolveLightmapGlId(mc);
        }
        if (activeLightmapGlId > 0) {
            shader.setSampler("Sampler2", activeLightmapGlId);
        }

        ShaderBindResync.ensureFreshBind(shader);
        shader.apply();
        if (ShaderCompatibilityDetector.isExternalShaderActive()) {
            // Unlock DepthColorStorage in case Oculus MixinShaderInstance.onTail locked
            // color/depth masks when applying our custom weapon_lit shader in GUI.
            ShaderBindResync.forceIrisDepthColorEnabledQuiet();
        }

        // Pin texture unit bindings after apply()
        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        if (weaponTex != null) {
            GlStateManager._bindTexture(weaponTex.getId());
        }
        if (activeLightmapGlId > 0) {
            GlStateManager._activeTexture(GL13.GL_TEXTURE2);
            GlStateManager._bindTexture(activeLightmapGlId);
            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        }

        GL20.glUniform1i(cachedSampler0Loc, 0);
        GL20.glUniform1i(cachedSampler2Loc, 2);

        // GL render states
        if (blend0(mode)) {
            RenderSystem.enableBlend();
            if (mode == 2) {
                RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            } else {
                RenderSystem.defaultBlendFunc();
            }
        } else {
            RenderSystem.disableBlend();
        }
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(depthMask);
        if (cull) {
            RenderSystem.enableCull();
        } else {
            RenderSystem.disableCull();
        }

        batchActive = true;
        batchTexture = texLoc;
        batchMode = mode;
        batchCull = cull;
        batchDepthMask = depthMask;
        batchShaded = shaded;
        batchGui = guiLighting;
        batchLight = effectiveLight;
    }

    private static boolean blend0(int mode) {
        return mode != 0;
    }

    /**
     * Restores the GL/RenderSystem state captured at {@link #beginWeaponBatch}.
     * No-op when no weapon batch is open. Called on batch key change and from
     * {@code GunGL.begin()/end()} so vanilla drawing never sees weapon state.
     */
    public static void endWeaponBatch() {
        if (irisBatchActive) {
            irisBatchActive = false;
            batchTexture = null;
            batchMode = -1;
            closeIrisBatchGuards();
            // The ExtendedShader cleared above may have left BlendMode.lastApplied
            // non-opaque; without this reset the next vanilla opaque-mode apply()
            // disables blending for an already-drawn additive batch (vignette-class bug).
            ShaderBindResync.resetBlendModeToOpaque();
        }
        if (!batchActive) return;
        batchActive = false;
        batchTexture = null;
        batchMode = -1;

        GlVaoSafety.bindVertexArray(0);
        if (previousShader != null) {
            RenderSystem.setShader(() -> previousShader);
        }
        RenderSystem.setShaderTexture(0, previousTexture0);
        RenderSystem.setShaderTexture(1, 0);
        RenderSystem.setShaderTexture(2, previousTexture2);
        Minecraft.getInstance().gameRenderer.lightTexture().turnOffLightLayer();
        GlStateManager._activeTexture(GL13.GL_TEXTURE2);
        GlStateManager._bindTexture(0);
        GlStateManager._activeTexture(GL13.GL_TEXTURE1);
        GlStateManager._bindTexture(0);
        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        GlVaoSafety.bindVertexArray(previousVao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);

        if (previousCullFaceEnabled) {
            RenderSystem.enableCull();
        } else {
            RenderSystem.disableCull();
        }
        RenderSystem.depthFunc(previousDepthFunc);
        RenderSystem.depthMask(previousDepthMask);
        if (previousDepthTestEnabled) {
            RenderSystem.enableDepthTest();
        } else {
            RenderSystem.disableDepthTest();
        }
        // Reset Mojang's BlendMode.lastApplied to opaque BEFORE restoring RenderSystem blend state
        ShaderBindResync.resetBlendModeToOpaque();
        if (previousBlendEnabled) {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(previousSrcRgb, previousDstRgb, previousSrcAlpha, previousDstAlpha);
        } else {
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        }
    }

    public static void flushGunBatch() {
        MultiBufferSource buffers = GunGL.buffers();
        if (buffers instanceof MultiBufferSource.BufferSource bufferSource) {
            bufferSource.endBatch();
            // gun_additive sets BlendMode.lastApplied to non-opaque (opaque = false).
            // Reset it to opaque so Gui.renderVignette() -> position_tex.apply() does not
            // call RenderSystem.disableBlend() and turn the screen black.
            ShaderBindResync.resetBlendModeToOpaque();
            // Трассеры/вспышки рисуются с depthMask(false); vanilla COLOR_DEPTH_WRITE
            // не восстанавливает glDepthMask при writeDepth == true — принудительно
            // возвращаем запись цвета и глубины, чтобы следующие батчи кадра не теряли Z.
            RenderSystem.depthMask(true);
            RenderSystem.colorMask(true, true, true, true);
        }
    }

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

    private static int resolveWhiteTextureGlId(Minecraft mc) {
        AbstractTexture whiteTex = mc.getTextureManager().getTexture(GunGL.WHITE);
        if (whiteTex == null) {
            mc.getTextureManager().bindForSetup(GunGL.WHITE);
            whiteTex = mc.getTextureManager().getTexture(GunGL.WHITE);
        }
        return whiteTex != null ? whiteTex.getId() : -1;
    }
}
