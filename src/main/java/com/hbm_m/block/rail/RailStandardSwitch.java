package com.hbm_m.block.rail;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.special.ItemTrain;
import com.hbm_m.util.ForgeDirection;
import com.hbm_m.util.Vec3NT;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** 1:1 {@code RailStandardSwitch} ({@code rail_large_switch}): Normalspur-Weiche 15 m, links abzweigend. */
public class RailStandardSwitch extends BlockRailWaypointSystem implements EntityBlock {

    public RailStandardSwitch(Properties properties) {
        super(properties);

        RailDef main = new RailDef("main");
        RailDef side = new RailDef("side");
        railDefs.add(main);
        railDefs.add(side);

        main.nodes.add(Vec3NT.createVectorHelper(-8.5, 0.1875, 0.5));
        main.nodes.add(Vec3NT.createVectorHelper(-7.5, 0.1875, 0.5));
        main.nodes.add(Vec3NT.createVectorHelper(6.5, 0.1875, 0.5));
        main.nodes.add(Vec3NT.createVectorHelper(7.5, 0.1875, 0.5));
        main.nodes.add(Vec3NT.createVectorHelper(8.5, 0.1875, 0.5));

        side.nodes.add(Vec3NT.createVectorHelper(-8.5, 0.1875, 4.5));
        side.nodes.add(Vec3NT.createVectorHelper(-7.5, 0.1875, 4.5));
        side.nodes.add(Vec3NT.createVectorHelper(-6.5, 0.1875, 4.5));
        side.nodes.add(Vec3NT.createVectorHelper(-5.5, 0.1875, 4.5));
        side.nodes.add(Vec3NT.createVectorHelper(-4.5, 0.1875, 4.5));
        side.nodes.add(Vec3NT.createVectorHelper(-3.5, 0.1875, 4.5));
        side.nodes.add(Vec3NT.createVectorHelper(-2.5, 0.1875, 4.5));
        side.nodes.add(Vec3NT.createVectorHelper(-1.5, 0.1875, 4.5));
        side.nodes.add(Vec3NT.createVectorHelper(-0.5, 0.1875, 4.25));
        side.nodes.add(Vec3NT.createVectorHelper(0.5, 0.1875, 3.9375));
        side.nodes.add(Vec3NT.createVectorHelper(1.5, 0.1875, 3.375));
        side.nodes.add(Vec3NT.createVectorHelper(2.5, 0.1875, 2.4625));
        side.nodes.add(Vec3NT.createVectorHelper(3.5, 0.1875, 1.75));
        side.nodes.add(Vec3NT.createVectorHelper(4.5, 0.1875, 1.1875));
        side.nodes.add(Vec3NT.createVectorHelper(5.5, 0.1875, 0.875));
        side.nodes.add(Vec3NT.createVectorHelper(6.5, 0.1875, 0.625));
        side.nodes.add(Vec3NT.createVectorHelper(7.5, 0.1875, 0.5));
        side.nodes.add(Vec3NT.createVectorHelper(8.5, 0.1875, 0.5));
    }

    /** Port: die Weichenstellung liegt nur im Kern (im Original hat jede Zelle ein ungenutztes TE). */
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12 ? new RailSwitchBlockEntity(pos, state) : null;
    }

    @Override
    public int[] getDimensions() {
        return new int[] {0, 0, 7, 7, 1, 0};
    }

    @Override
    public int getOffset() {
        return 7;
    }

    @Override
    public TrackGauge getGauge(Level world, int x, int y, int z) {
        return TrackGauge.STANDARD;
    }

    @Override
    public boolean canCross(Level world, int x, int y, int z, Vec3NT from, Vec3NT to, RailDef def) {
        return canCrossSwitch(world, x, y, z, from, to, def);
    }

    /** gemeinsam mit {@link RailStandardSwitchFlipped} */
    static boolean canCrossSwitch(Level world, int x, int y, int z, Vec3NT from, Vec3NT to, RailDef def) {
        if (!(world.getBlockEntity(new BlockPos(x, y, z)) instanceof RailSwitchBlockEntity tile)) return true;

        ForgeDirection dir = ForgeDirection.getOrientation(tile.getBlockState().getValue(META) - 10);

        if (dir == ForgeDirection.EAST) if (from.xCoord < to.xCoord) return true;
        if (dir == ForgeDirection.WEST) if (from.xCoord > to.xCoord) return true;
        if (dir == ForgeDirection.SOUTH) if (from.zCoord < to.zCoord) return true;
        if (dir == ForgeDirection.NORTH) if (from.zCoord > to.zCoord) return true;

        if (dir == ForgeDirection.EAST) if (to.xCoord < x + 0.5 + 7) return true;
        if (dir == ForgeDirection.WEST) if (to.xCoord > x + 0.5 - 7) return true;
        if (dir == ForgeDirection.SOUTH) if (to.zCoord < z + 0.5 + 7) return true;
        if (dir == ForgeDirection.NORTH) if (to.zCoord > z + 0.5 - 7) return true;

        if (tile.isSwitched) {
            if ("side".equals(def.name)) return true;
        } else {
            if ("main".equals(def.name)) return true;
        }

        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return useSwitch(this, world, pos, player, hand);
    }

    /** Original {@code onBlockActivated}: Weiche umstellen. */
    static InteractionResult useSwitch(RailDummyableBlock block, Level world, BlockPos pos, Player player, InteractionHand hand) {

        if (world.isClientSide) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (player.getItemInHand(hand).getItem() instanceof ItemTrain) return InteractionResult.PASS;

        int[] core = block.findCore(world, pos.getX(), pos.getY(), pos.getZ());

        if (core != null) {
            BlockPos corePos = new BlockPos(core[0], core[1], core[2]);

            if (world.getBlockEntity(corePos) instanceof RailSwitchBlockEntity sw) {
                sw.isSwitched = !sw.isSwitched;
                sw.setChanged();
                BlockState coreState = world.getBlockState(corePos);
                world.sendBlockUpdated(corePos, coreState, coreState, 3);
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean checkRequirement(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        if (!super.checkRequirement(world, x, y, z, dir, o)) return false;

        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        dir = dir.getOpposite();

        int dX = dir.offsetX;
        int dZ = dir.offsetZ;
        int rX = rot.offsetX;
        int rZ = rot.offsetZ;

        for (int i = 0; i < 4; i++) if (!isReplaceable(world, x + dX * (2 + i) + rX * 2, y, z + dZ * (2 + i) + rZ * 2)) return false;
        for (int i = 0; i < 2; i++) if (!isReplaceable(world, x + dX * (4 + i) + rX * 3, y, z + dZ * (4 + i) + rZ * 3)) return false;
        if (!isReplaceable(world, x + dX * 5 + rX * 4, y, z + dZ * 5 + rZ * 4)) return false;
        for (int j = 0; j < 2; j++) for (int i = 0; i < 2; i++) if (!isReplaceable(world, x + dX * (6 + j) + rX * (3 + i), y, z + dZ * (6 + j) + rZ * (3 + i))) return false;
        if (!isReplaceable(world, x + dX * 7 + rX * 5, y, z + dZ * 7 + rZ * 5)) return false;
        for (int j = 0; j < 7; j++) for (int i = 0; i < 2; i++) if (!isReplaceable(world, x + dX * (8 + j) + rX * (4 + i), y, z + dZ * (8 + j) + rZ * (4 + i))) return false;

        return true;
    }

    @Override
    protected void fillSpace(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        super.fillSpace(world, x, y, z, dir, o);

        safeRem = true;

        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        dir = dir.getOpposite();

        int dX = dir.offsetX;
        int dZ = dir.offsetZ;
        int rX = rot.offsetX;
        int rZ = rot.offsetZ;

        for (int i = 0; i < 4; i++) setBlock(world, x + dX * (2 + i) + rX * 2, y, z + dZ * (2 + i) + rZ * 2, rot.ordinal());
        for (int i = 0; i < 2; i++) setBlock(world, x + dX * (4 + i) + rX * 3, y, z + dZ * (4 + i) + rZ * 3, rot.ordinal());
        setBlock(world, x + dX * 5 + rX * 4, y, z + dZ * 5 + rZ * 4, rot.ordinal());
        for (int j = 0; j < 2; j++) for (int i = 0; i < 2; i++) setBlock(world, x + dX * (6 + j) + rX * (3 + i), y, z + dZ * (6 + j) + rZ * (3 + i), dir.ordinal());
        setBlock(world, x + dX * 7 + rX * 5, y, z + dZ * 7 + rZ * 5, rot.ordinal());
        for (int j = 0; j < 7; j++) for (int i = 0; i < 2; i++) setBlock(world, x + dX * (8 + j) + rX * (4 + i), y, z + dZ * (8 + j) + rZ * (4 + i), dir.ordinal());

        safeRem = false;
    }
}
