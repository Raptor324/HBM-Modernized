package com.hbm_m.api.energy;

import com.hbm_m.api.network.GenNode;
import com.hbm_m.api.network.INetworkProvider;
import com.hbm_m.api.network.NodeDirPos;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Порт api/hbm/energymk2/Nodespace из 1.7.10.
 * Compatibility husk над UniNodespace для энергосети.
 */
public class Nodespace {

    public static final PowerNetProvider THE_POWER_PROVIDER = new PowerNetProvider();

    @SuppressWarnings("unchecked")
    public static PowerNode getNode(ServerLevel level, BlockPos pos) {
        return (PowerNode) com.hbm_m.api.network.UniNodespace.getNode(level, pos, THE_POWER_PROVIDER);
    }

    public static void createNode(ServerLevel level, PowerNode node) {
        com.hbm_m.api.network.UniNodespace.createNode(level, node);
    }

    public static void destroyNode(ServerLevel level, BlockPos pos) {
        com.hbm_m.api.network.UniNodespace.destroyNode(level, pos, THE_POWER_PROVIDER);
    }

    /**
     * Порт Nodespace.PowerNode. Узел энергосети.
     */
    public static class PowerNode extends GenNode<PowerNet> {

        public PowerNode(INetworkProvider<PowerNet> provider, BlockPos pos) {
            super(provider, pos);
        }

        public PowerNode setConnections(NodeDirPos... connections) {
            super.setConnections(connections);
            return this;
        }
    }
}
