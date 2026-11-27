package com.hbm_m.platform.legacy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * [26.x] Ersatz für {@code BlockBehaviour#onRemove(BlockState, Level, BlockPos, BlockState, boolean)},
 * das Mojang in 1.21.2 durch {@code affectNeighborsAfterRemoval} / {@code BlockEntity#preRemoveSideEffects}
 * ersetzt hat.
 *
 * <p>Per Interface-Injection ({@code aw/26.3.interfaces.json}) sieht der Compiler dieses Interface auf
 * {@code BlockBehaviour}; zur Laufzeit hängt es {@link com.hbm_m.platform.legacy.mixin.BlockBehaviourLegacyRemovalMixin}
 * an. So kompilieren die ~140 bestehenden {@code @Override onRemove(...)} samt {@code super.onRemove(...)}
 * unverändert. Aufgerufen wird es aus {@link com.hbm_m.platform.legacy.mixin.LevelChunkLegacyRemovalMixin},
 * an derselben Stelle wie in 1.21.1 (nur serverseitig, nach dem Setzen im Section, bevor das BE entfernt wird).
 *
 * <p>Den Default-Teil von 1.21.1 (BE entfernen) erledigt Vanilla in 26.x selbst, daher ist der Default leer.
 */
public interface LegacyBlockRemoval {

    default void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
    }
}
