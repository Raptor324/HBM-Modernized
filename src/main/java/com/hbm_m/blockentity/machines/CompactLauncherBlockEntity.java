package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.item.missile.ItemCustomMissilePart.PartSize;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code TileEntityCompactLauncher}: 3x3-Werfer fuer Raketen mit 1.0er-Spitze, Tanks und Feststoff je 25.000,
 * Start nur mit bereitem Zielgeber. Anschluesse an den acht Feldern neben den Eckports und unter den Ecken.
 */
public class CompactLauncherBlockEntity extends CustomLauncherBlockEntity {

    public CompactLauncherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMPACT_LAUNCHER_BE.get(), pos, state, 25000);
    }

    @Override public PartSize getPadSize() { return PartSize.SIZE_10; }
    @Override protected int getTriggerRadius() { return 1; }
    @Override protected boolean needsDesignator() { return true; }
    @Override protected String defaultName() { return "container.compactLauncher"; }

    @Override
    protected void updateConnections(ServerLevel server) {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        Object[][] con = {
                { new BlockPos(x + 2, y, z + 1), Direction.EAST }, { new BlockPos(x + 2, y, z - 1), Direction.EAST },
                { new BlockPos(x - 2, y, z + 1), Direction.WEST }, { new BlockPos(x - 2, y, z - 1), Direction.WEST },
                { new BlockPos(x + 1, y, z + 2), Direction.SOUTH }, { new BlockPos(x - 1, y, z + 2), Direction.SOUTH },
                { new BlockPos(x + 1, y, z - 2), Direction.NORTH }, { new BlockPos(x - 1, y, z - 2), Direction.NORTH },
                { new BlockPos(x + 1, y - 1, z + 1), Direction.DOWN }, { new BlockPos(x + 1, y - 1, z - 1), Direction.DOWN },
                { new BlockPos(x - 1, y - 1, z + 1), Direction.DOWN }, { new BlockPos(x - 1, y - 1, z - 1), Direction.DOWN }
        };
        for (Object[] c : con) {
            BlockPos at = (BlockPos) c[0];
            Direction dir = (Direction) c[1];
            trySubscribe(server, at.getX(), at.getY(), at.getZ(), dir);
            trySubscribe(tanks[0].getTankType(), server, at, dir);
            trySubscribe(tanks[1].getTankType(), server, at, dir);
        }
    }
}
