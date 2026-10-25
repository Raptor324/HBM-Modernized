package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.item.missile.ItemCustomMissilePart.PartSize;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code TileEntityLaunchTable}: 9x9-Starttisch fuer Baukastenraketen jeder Groesse - die Rampe (1.0, 1.5 oder
 * 2.0) wird in der GUI gewaehlt und muss zur Rumpfspitze passen. Tanks und Feststoff je 100.000, Start auch ohne
 * Zielgeber (dann nur per Radar), Redstone auf dem ganzen Tisch, Anschluesse rundum im Abstand fuenf.
 */
public class LaunchTableBlockEntity extends CustomLauncherBlockEntity {

    public PartSize padSize = PartSize.SIZE_10;
    /** Hoehe des Geruests in Bloecken (Renderer; folgt der zuletzt eingelegten Rakete). */
    public int height = 10;

    public LaunchTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAUNCH_TABLE_BE.get(), pos, state, 100000);
    }

    @Override public PartSize getPadSize() { return padSize; }
    @Override protected int getTriggerRadius() { return 4; }
    @Override protected boolean needsDesignator() { return false; }
    @Override protected String defaultName() { return "container.launchTable"; }
    @Override protected float smokeSpread() { return 0.65F; }

    @Override
    protected void updateConnections(ServerLevel server) {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        for (int i = -4; i <= 4; i++) {
            sub(server, new BlockPos(x + i, y, z + 5), Direction.SOUTH);
            sub(server, new BlockPos(x + i, y, z - 5), Direction.NORTH);
            sub(server, new BlockPos(x + 5, y, z + i), Direction.EAST);
            sub(server, new BlockPos(x - 5, y, z + i), Direction.WEST);
        }
    }

    private void sub(ServerLevel server, BlockPos at, Direction dir) {
        trySubscribe(server, at.getX(), at.getY(), at.getZ(), dir);
        for (int j = 0; j < 2; j++) trySubscribe(tanks[j].getTankType(), server, at, dir);
    }

    /** Original AuxButtonPacket: {@code launcher.padSize = PartSize.values()[value]}. */
    @Override
    public void receiveControl(Player player, CompoundTag data) {
        if (data.contains("padSize")) {
            int v = data.getInt("padSize");
            if (v >= 0 && v < PartSize.values().length) {
                padSize = PartSize.values()[v];
                setChanged();
                sendUpdateToClient();
            }
        }
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("padSize", padSize.ordinal());
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        int v = tag.getInt("padSize");
        padSize = v >= 0 && v < PartSize.values().length ? PartSize.values()[v] : PartSize.SIZE_10;
    }
}
