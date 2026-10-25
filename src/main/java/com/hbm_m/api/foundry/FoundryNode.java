package com.hbm_m.api.foundry;

import com.hbm_m.api.network.GenNode;
import com.hbm_m.api.network.INetworkProvider;
import com.hbm_m.api.network.NodeDirPos;
import com.hbm_m.inventory.material.NTMMaterial;

import net.minecraft.core.BlockPos;

/** 1:1 {@code FoundryChannel.FoundryNode}: Knoten einer Giessrinne mit dem Material, das gerade in ihr steht. */
public class FoundryNode extends GenNode<FoundryNetwork> {

    public NTMMaterial type;

    public FoundryNode(INetworkProvider<FoundryNetwork> provider, BlockPos... positions) {
        super(provider, positions);
    }

    @Override
    public FoundryNode setConnections(NodeDirPos... connections) {
        super.setConnections(connections);
        return this;
    }
}
