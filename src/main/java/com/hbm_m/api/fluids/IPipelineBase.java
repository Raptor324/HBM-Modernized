package com.hbm_m.api.fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;

/**
 * Port-Schnittstelle fuer {@code com.hbm.tileentity.network.TileEntityPipelineBase} (Rohrleitungs-Anker, die mit
 * dem Schraubenschluessel per Luftleitung verbunden werden). Die Anker-Bloecke selbst ({@code pipe_anchor}) kommen
 * mit der Block-Runde; der Schraubenschluessel arbeitet schon gegen diese Schnittstelle.
 */
public interface IPipelineBase {

    enum ConnectionType { SMALL }

    ConnectionType getConnectionType();
    Vec3 getMountPos();
    double getMaxPipeLength();
    Fluid getPipelineFluid();
    void setPipelineFluid(Fluid fluid);
    BlockPos getPipelinePos();
    void addConnection(BlockPos pos);

    default Vec3 getConnectionPoint() {
        BlockPos p = getPipelinePos();
        return getMountPos().add(p.getX(), p.getY(), p.getZ());
    }

    /**
     * Returns a status code based on the operation.<br>
     * 0: Connected<br>
     * 1: Connections are incompatible<br>
     * 2: Both parties are the same block<br>
     * 3: Connection length exceeds maximum<br>
     * 4: Pipeline fluid types do not match
     */
    static int canConnect(IPipelineBase first, IPipelineBase second) {

        if (first.getConnectionType() != second.getConnectionType()) return 1;
        if (first == second) return 2;

        // connect with NONE type anchors
        Fluid none = net.minecraft.world.level.material.Fluids.EMPTY;
        if (first.getPipelineFluid() == none && second.getPipelineFluid() != first.getPipelineFluid()) first.setPipelineFluid(second.getPipelineFluid());
        if (second.getPipelineFluid() == none && first.getPipelineFluid() != second.getPipelineFluid()) second.setPipelineFluid(first.getPipelineFluid());

        if (first.getPipelineFluid() != second.getPipelineFluid()) return 4;

        double len = Math.min(first.getMaxPipeLength(), second.getMaxPipeLength());

        Vec3 delta = second.getConnectionPoint().subtract(first.getConnectionPoint());

        return len >= delta.length() ? 0 : 3;
    }
}
