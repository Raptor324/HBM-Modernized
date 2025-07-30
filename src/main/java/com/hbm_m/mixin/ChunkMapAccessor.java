package com.hbm_m.mixin;

// Пока что нигде не используется. Возможно пригодится в будущем
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkMap.class)
public interface ChunkMapAccessor {
    @Invoker("getChunks")
    Iterable<ChunkHolder> invokeGetChunks();
}
