package com.hbm_m.blockentity.machines;

import com.hbm_m.api.energy.Nodespace;
import com.hbm_m.api.network.NodeDirPos;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.PylonBaseBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code TileEntitySubstation} (1.7.10): Umspannwerk als Pylon (QUAD, 20 m), vier Kabelhalter auf 5,25 m Hoehe
 * quer zur Blickrichtung. Der Knoten belegt Kern und die vier Eck-Anschluesse ({@code makeExtra}) und verbindet
 * nach aussen ueber die acht Randpositionen.
 */
public class MachineSubstationBlockEntity extends PylonBaseBlockEntity {

    public MachineSubstationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUBSTATION_BE.get(), pos, state);
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.QUAD;
    }

    @Override
    public Vec3[] getMountPos() {
        double topOff = 5.25;
        // Vec3(1,0,0); bei Metadaten 4/5 rotateAroundY(PI/2) -> (0,0,-1)
        Direction dir = getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)
                ? getBlockState().getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        double vx = 1, vz = 0;
        if (dir == Direction.WEST || dir == Direction.EAST) {
            vx = 0;
            vz = -1;
        }
        return new Vec3[] {
                new Vec3(0.5 + vx * 0.5, topOff, 0.5 + vz * 0.5),
                new Vec3(0.5 + vx * 1.5, topOff, 0.5 + vz * 1.5),
                new Vec3(0.5 - vx * 0.5, topOff, 0.5 - vz * 0.5),
                new Vec3(0.5 - vx * 1.5, topOff, 0.5 - vz * 1.5),
        };
    }

    @Override
    public Vec3 getConnectionPoint() {
        BlockPos p = getBlockPos();
        return new Vec3(p.getX() + 0.5, p.getY() + 5.25, p.getZ() + 0.5);
    }

    @Override
    public double getMaxWireLength() {
        return 20;
    }

    @Override
    protected void addExtraConnections(Nodespace.PowerNode node, BlockPos pos) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        node.positions.add(new BlockPos(x + 1, y, z + 1));
        node.positions.add(new BlockPos(x + 1, y, z - 1));
        node.positions.add(new BlockPos(x - 1, y, z + 1));
        node.positions.add(new BlockPos(x - 1, y, z - 1));
        node.addConnection(new NodeDirPos(x + 2, y, z - 1, Direction.EAST));
        node.addConnection(new NodeDirPos(x + 2, y, z + 1, Direction.EAST));
        node.addConnection(new NodeDirPos(x - 2, y, z - 1, Direction.WEST));
        node.addConnection(new NodeDirPos(x - 2, y, z + 1, Direction.WEST));
        node.addConnection(new NodeDirPos(x - 1, y, z + 2, Direction.SOUTH));
        node.addConnection(new NodeDirPos(x + 1, y, z + 2, Direction.SOUTH));
        node.addConnection(new NodeDirPos(x - 1, y, z - 2, Direction.NORTH));
        node.addConnection(new NodeDirPos(x + 1, y, z - 2, Direction.NORTH));
    }

    // Das Original hat keine GUI; die Zugriffe bleiben nur, damit GUIMachineSubstation/MachineSubstationMenu
    // (nicht mehr geoeffnet) uebersetzbar bleiben. TODO(port): GUI/Menue des Umspannwerks entfernen.
    public long getEnergyStored() { return 0; }
    public long getMaxEnergyStored() { return 0; }
    public int getProgressScaled(int scale) { return 0; }
    public int getProgress() { return 0; }
    public int getMaxProgress() { return 0; }
    public boolean isActive() { return false; }
}
