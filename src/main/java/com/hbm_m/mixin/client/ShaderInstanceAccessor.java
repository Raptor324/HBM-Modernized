package com.hbm_m.mixin.client;

import net.minecraft.client.renderer.ShaderInstance;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes {@code ShaderInstance.programId} (private final int on both 1.20.1
 * forge and 1.21.1 neoforge) for the NtDirectPass draw-time program check in
 * {@link VertexBufferMixin}: Iris/Oculus can switch or zero the GL program
 * without updating the {@code ShaderInstance.lastProgramId} static cache, the
 * next {@code apply()} then skips the bind and the draw runs with program 0
 * (GL_INVALID_OPERATION "No active program").
 */
@Mixin(ShaderInstance.class)
public interface ShaderInstanceAccessor {

    @Accessor("programId")
    int hbm$getProgramId();
}
