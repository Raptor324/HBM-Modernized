package com.hbm_m.mixin.client;

import com.mojang.blaze3d.vertex.VertexBuffer;

import net.minecraft.client.renderer.ShaderInstance;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hbm_m.client.render.shader.NtDirectPass;
import com.hbm_m.client.render.shader.ShaderBindResync;

/**
 * Re-enables color/depth writes right after {@code ShaderInstance.apply()} when
 * the NT direct pass ({@link NtDirectPass}) is flushing.
 *
 * <p>Under an active Iris pack, unknown mod ShaderInstances are masked in
 * {@code MixinShaderInstance.onTail} ({@code DepthColorStorage.disableDepthColor} -
 * colorMask off + depth test off), so our own core shaders (nuke_cloud, nuke_add,
 * nt_color_tex_lightmap) would draw "into nowhere". The NT render types bind those
 * shaders INSIDE the {@code VertexBuffer._drawWithShader} sequence
 * ({@code apply(); draw(); clear()}), so the only seam between the mask and the
 * actual draw is this injection right after the {@code apply()} invoke. The
 * pack's blend/fog directives never interfere: our shaders apply their own JSON
 * blend and vanilla fragment behavior, and the window's pre-bound pack FBO keeps
 * the output inside Iris's pipeline.</p>
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
        if (!NtDirectPass.isActive()) {
            return;
        }
        // FBO re-bind: a RenderType whose teardown re-binds the vanilla MAIN
        // target (an OutputStateShard carrying TRANSLUCENT_TARGET - on the
        // shard-capable setups its clearRenderState ends with
        // mainRenderTarget.bindWrite) silently redirects every LATER batch of
        // the same in-window flush out of the pack composite, where Iris's
        // final blit erases it. Re-bind the pack FBO captured at window open
        // (managed call - keeps the GlStateManager cache coherent) before the
        // draw. -1 = capture failed at begin(); skip rather than bind garbage.
        int packFbo = NtDirectPass.getPackFbo();
        if (packFbo > 0) {
            com.mojang.blaze3d.platform.GlStateManager._glBindFramebuffer(
                    org.lwjgl.opengl.GL30.GL_FRAMEBUFFER, packFbo);
        }
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
    }
}
