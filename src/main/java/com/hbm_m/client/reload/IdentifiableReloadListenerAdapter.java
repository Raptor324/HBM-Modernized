//? if fabric {
package com.hbm_m.client.reload;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Fabric требует {@link IdentifiableResourceReloadListener} для регистрации в {@code ResourceManagerHelper}.
 * На Forge это не нужно, поэтому класс существует только на Fabric через stonecutter.
 */
public final class IdentifiableReloadListenerAdapter implements IdentifiableResourceReloadListener {
    private final ResourceLocation id;
    private final PreparableReloadListener delegate;

    public IdentifiableReloadListenerAdapter(ResourceLocation id, PreparableReloadListener delegate) {
        this.id = id;
        this.delegate = delegate;
    }

    @Override
    public ResourceLocation getFabricId() {
        return id;
    }

    @Override
    public Collection<ResourceLocation> getFabricDependencies() {
        return Collections.emptyList();
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier,
                                         ResourceManager resourceManager,
                                         net.minecraft.util.profiling.ProfilerFiller preparationsProfiler,
                                         net.minecraft.util.profiling.ProfilerFiller reloadProfiler,
                                         Executor backgroundExecutor,
                                         Executor gameExecutor) {
        return delegate.reload(preparationBarrier, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor);
    }
}
//?}

