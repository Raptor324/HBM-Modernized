package com.hbm_m.mixin.client;

import com.mojang.blaze3d.vertex.VertexBuffer;

import net.minecraft.client.renderer.ShaderInstance;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hbm_m.client.render.shader.ModShaders;
import com.hbm_m.client.render.shader.NtDirectPass;
import com.hbm_m.client.render.shader.ShaderBindResync;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;

/**
 * Re-enables color/depth writes right after {@code ShaderInstance.apply()} when
 * the NT direct pass ({@link NtDirectPass}) is flushing OR when one of our own
 * direct (non-{@code ExtendedShader}) mod shaders is drawn under an active Iris
 * shaderpack (muzzle flashes / bullet tracers via {@code gun_additive}).
 *
 * <p>Under an active Iris pack, unknown mod ShaderInstances are masked in
 * {@code MixinShaderInstance.onTail} ({@code DepthColorStorage.disableDepthColor} -
 * colorMask off + depth test off), so our own core shaders (nuke_cloud, nuke_add,
 * nt_color_tex_lightmap, gun_additive) would draw "into nowhere". The render types
 * bind those shaders INSIDE the {@code VertexBuffer._drawWithShader} sequence
 * ({@code apply(); draw(); clear()}), so the only seam between the mask and the
 * actual draw is this injection right after the {@code apply()} invoke.</p>
 *
 * <p>Additionally, when a preceding {@code ExtendedShader.clear()} re-bound the
 * vanilla {@code mainRenderTarget}, our direct shaders would draw into the dead
 * vanilla target which Iris's composite overwrites - {@link NtDirectPass#bindPackFboIfDetached()}
 * re-binds the pack's G-buffer FBO before the draw, and the additive JSON blend of
 * {@code gun_additive} / {@code nuke_add} is enforced explicitly (with depth writes
 * off) so a pack directive cannot override it. After {@code shader.clear()} the
 * {@code BlendMode.lastApplied} cache is reset to opaque so a subsequent vanilla
 * {@code shader.apply()} never calls {@code RenderSystem.disableBlend()} in front of
 * a manually-blended fullscreen draw (the vignette black-screen bug).</p>
 *
 * <p>Same method shape on both targets (verified in the decompiled sources):
 * 1.20.1 Forge and 1.21.1 NeoForge both have
 * {@code private void _drawWithShader(Matrix4f, Matrix4f, ShaderInstance)} ending
 * with {@code apply(); draw...; clear();}.</p>
 */
@Mixin(VertexBuffer.class)
public class VertexBufferMixin {

    @Inject(method = "_drawWithShader",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/renderer/ShaderInstance;apply()V",
                     shift = At.Shift.AFTER))
    private void hbm$unmaskNtDirectDraw(Matrix4f modelView, Matrix4f projection,
                                        ShaderInstance shader, CallbackInfo ci) {
        boolean window = NtDirectPass.isActive();
        boolean modDirect = ShaderCompatibilityDetector.isExternalShaderActive()
                && ModShaders.isModDirectShader(shader);
        if (!window && !modDirect) {
            return;
        }
        // FBO re-bind: a RenderType whose teardown re-binds the vanilla MAIN
        // target (an OutputStateShard carrying TRANSLUCENT_TARGET - on the
        // shard-capable setups its clearRenderState ends with
        // mainRenderTarget.bindWrite) silently redirects every LATER batch of
        // the same in-window flush out of the pack composite, where Iris's
        // final blit erases it. Inside the NtDirectPass window this re-binds
        // the captured pack FBO; outside the window (bare gun_additive /
        // nuke_add draws - muzzle flashes, bullet tracers) it re-applies the
        // pack's textured_color ExtendedShader when an earlier
        // ExtendedShader.clear() had re-bound mainRenderTarget.
        NtDirectPass.bindPackFboIfDetached();
        // COLOR MASK - the raw GL call is the guarantee here, not a fallback:
        // Oculus's MixinShaderInstance.onTail sets the DepthColorStorage lock on
        // EVERY apply of an unknown mod shader, and its GlStateManager
        // interception can re-apply the stored mask (colorMask off) through the
        // MANAGED reset path (RenderSystem.colorMask no-ops against its own
        // cache while the lock is considered engaged). The raw GL11 call writes
        // the physical state directly - nothing between it and glDrawElements
        // can re-mask mid-batch. forceIrisDepthColorEnabledQuiet stays as the
        // secondary (flag reset + managed restore for the rest of the frame).
        org.lwjgl.opengl.GL11.glColorMask(true, true, true, true);
        ShaderBindResync.forceIrisDepthColorEnabledQuiet();
        // Program check: Oculus/Iris can raw-switch or zero the GL program without
        // updating ShaderInstance.lastProgramId (their patched apply/clear skip the
        // rebind when the static cache claims our program is already bound). The
        // result is a draw with program 0 - GL_INVALID_OPERATION "No active
        // program" - and invisible particles. Verify the actual binding and force
        // an honest one before the draw.
        if (shader != null) {
            ShaderBindResync.ensureFreshBind(shader);
            // Raw read: GlStateManager._glGetInteger is gone on 1.21.1, and a raw
            // query does not desync the managed cache (read-only) - same pattern
            // as the DEPTH_WRITEMASK probe in EngineHandler.
            int programId = ((ShaderInstanceAccessor) shader).hbm$getProgramId();
            int actual = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL20.GL_CURRENT_PROGRAM);
            if (actual != programId) {
                com.mojang.blaze3d.platform.GlStateManager._glUseProgram(programId);
                // Belt and suspenders (see ShaderBindResync.ensureFreshBind): if
                // the managed call somehow did not take, force the raw bind.
                if (org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL20.GL_CURRENT_PROGRAM) != programId) {
                    org.lwjgl.opengl.GL20.glUseProgram(programId);
                }
            }
        }
        // Additive blend enforcement for gun_additive / nuke_add: the shader's own
        // JSON blend block was applied by apply(), but a pack directive or a
        // foreign state clobber may have overridden it. Re-assert true additive
        // (SRC_ALPHA, ONE) with depth writes off so muzzle flashes and tracers
        // composite correctly even under a shaderpack.
        if (shader != null && ModShaders.isModAdditiveShader(shader)) {
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.blendFuncSeparate(
                    org.lwjgl.opengl.GL11.GL_SRC_ALPHA, org.lwjgl.opengl.GL11.GL_ONE,
                    org.lwjgl.opengl.GL11.GL_ONE, org.lwjgl.opengl.GL11.GL_ZERO);
            com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
        }
    }

    @Inject(method = "_drawWithShader", at = @At("TAIL"))
    private void hbm$resetBlendAfterModAdditiveDraw(Matrix4f modelView, Matrix4f projection,
                                                    ShaderInstance shader, CallbackInfo ci) {
        // After shader.clear() BlendMode.lastApplied must never stay non-opaque:
        // a subsequent vanilla shader.apply() (e.g. position_tex in
        // Gui.renderVignette()) would otherwise detect an opacity change and call
        // RenderSystem.disableBlend() right before a fullscreen manual-blend draw.
        if (shader != null && ModShaders.isModAdditiveShader(shader)) {
            ShaderBindResync.resetBlendModeToOpaque();
        }
    }
}
