package com.hbm_m.api.fluids;

import com.hbm_m.api.network.GenNode;
import com.hbm_m.api.network.INetworkProvider;
import com.hbm_m.api.network.NodeDirPos;

import net.minecraft.core.BlockPos;

/**
 * Узел жидкостной сети (аналог FluidNode из 1.7.10).
 */
public class FluidNode extends GenNode<FluidNet> {

    public FluidNode(INetworkProvider<FluidNet> provider, BlockPos... positions) {
        super(provider, positions);
    }

    @Override
    public FluidNode setConnections(NodeDirPos... connections) {
        super.setConnections(connections);
        return this;
    }
}
