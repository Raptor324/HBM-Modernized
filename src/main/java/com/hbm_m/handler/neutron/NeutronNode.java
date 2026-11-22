package com.hbm_m.handler.neutron;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.handler.neutron.NeutronStream.NeutronType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Port of NEXT {@code com.hbm.handler.neutron.NeutronNode}: abstract base of every node cached
 * in a {@link NeutronNodeWorld.StreamWorld}. A parallel world to
 * {@code com.hbm_m.handler.rbmk.NeutronNodeWorld} - that one is concrete-typed to the RBMK
 * lineage, so this package mirrors NEXT's {@code handler/neutron} 1:1 instead of generalizing
 * the RBMK hot path.
 *
 * <p>NEXT reads {@code node.tile} from {@link PileNeutronHandler} via the protected field;
 * same-package placement keeps that access legal.</p>
 */
public abstract class NeutronNode {

    private final Map<Key<?>, Object> data = new HashMap<>();
    protected NeutronType type;
    protected BlockPos pos;
    protected BlockEntity tile;

    public NeutronNode(BlockEntity tile, NeutronType type) {
        this.type = type;
        this.tile = tile;
        this.pos = tile.getBlockPos();
    }

    public <T> void set(Key<T> key, T value) {
        data.put(key, value);
    }

    public <T> @Nullable T get(Key<T> key) {
        return key.type().cast(data.get(key));
    }

    public record Key<T>(String name, Class<T> type) {}
}
