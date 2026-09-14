package com.hbm_m.blockentity.network.pneumatic;

import com.hbm_m.api.network.GenNode;
import com.hbm_m.api.network.NodeDirPos;
import com.hbm_m.api.network.UniNodespace;
import com.hbm_m.api.pneumatic.IPneumaticConnector;
import com.hbm_m.api.pneumatic.PneumaticNet;
import com.hbm_m.api.pneumatic.PneumaticNetProvider;
import com.hbm_m.api.pneumatic.StackCache;
import com.hbm_m.blockentity.BaseMachineBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code TileEntityPneumaticMachineBase} (1.7.10): die Grundlage aller Geraete, die
 * <b>auf</b> das Lagernetz zugreifen - also Terminal, Eingabe und Ausgabe.
 *
 * <p>So ein Geraet haengt mit einem eigenen Knoten im Rohrnetz und fuehrt ein
 * {@link StackCache Verzeichnis} darueber, was es von dort aus erreichen kann. Das Verzeichnis
 * fuellt sich nicht selbst: die Lager tragen sich darin ein, sobald es sich beim Netz anmeldet.</p>
 */
public abstract class PneumaticMachineBlockEntity extends BaseMachineBlockEntity implements IPneumaticConnector {

    @Nullable
    protected GenNode<PneumaticNet> node;
    @Nullable
    protected StackCache cache;

    protected PneumaticMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int inventorySize) {
        super(type, pos, state, inventorySize, 0L, 0L, 0L);
    }

    @Nullable
    public StackCache getCache() {
        return cache;
    }

    /**
     * Original: der Rumpf von {@code updateEntity} - Knoten sicherstellen, Verzeichnis
     * sicherstellen, beim Netz anmelden. Wer davon erbt, ruft das zuerst.
     */
    protected void tickNetwork(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        if (node == null || node.expired) {
            // Der Knoten ist weg - damit gilt auch alles, was im Verzeichnis stand, als ungueltig.
            if (cache != null) cache.dissolveCache();

            node = UniNodespace.getNode(serverLevel, pos, PneumaticNetProvider.THE_PROVIDER);

            if (node == null || node.expired) {
                GenNode<PneumaticNet> fresh = new GenNode<>(PneumaticNetProvider.THE_PROVIDER, pos);
                NodeDirPos[] conns = new NodeDirPos[6];
                int i = 0;
                for (Direction dir : Direction.values()) conns[i++] = new NodeDirPos(pos.relative(dir), dir);
                fresh.setConnections(conns);
                UniNodespace.createNode(serverLevel, fresh);
                node = fresh;
            }
        }

        if (cache == null || cache.hasExpired) {
            cache = new StackCache(pos);
        }

        if (node != null && node.hasValidNet()) {
            node.net.addStackCache(cache);
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel && node != null) {
            UniNodespace.destroyNode(serverLevel, worldPosition, PneumaticNetProvider.THE_PROVIDER);
        }
        if (cache != null) cache.dissolveCache();
        super.setRemoved();
    }

    //? if forge {
    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level instanceof ServerLevel serverLevel && node != null) {
            UniNodespace.destroyNode(serverLevel, worldPosition, PneumaticNetProvider.THE_PROVIDER);
        }
        if (cache != null) cache.dissolveCache();
    }
    //?}
}
