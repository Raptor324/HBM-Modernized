package com.hbm_m.block.rail;

import org.jetbrains.annotations.Nullable;

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

/** 1:1 {@code RailStandardSwitchFlipped} ({@code rail_large_switch_flipped}): Normalspur-Weiche 15 m, rechts abzweigend. */
public class RailStandardSwitchFlipped extends BlockRailWaypointSystem implements EntityBlock {

    public RailStandardSwitchFlipped(Properties properties) {
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

        side.nodes.add(Vec3NT.createVectorHelper(-8.5, 0.1875, -3.5));
        side.nodes.add(Vec3NT.createVectorHelper(-7.5, 0.1875, -3.5));
        side.nodes.add(Vec3NT.createVectorHelper(-6.5, 0.1875, -3.5));
        side.nodes.add(Vec3NT.createVectorHelper(-5.5, 0.1875, -3.5));
        side.nodes.add(Vec3NT.createVectorHelper(-4.5, 0.1875, -3.5));
        side.nodes.add(Vec3NT.createVectorHelper(-3.5, 0.1875, -3.5));
        side.nodes.add(Vec3NT.createVectorHelper(-2.5, 0.1875, -3.5));
        side.nodes.add(Vec3NT.createVectorHelper(-1.5, 0.1875, -3.5));
        side.nodes.add(Vec3NT.createVectorHelper(-0.5, 0.1875, -3.25));
        side.nodes.add(Vec3NT.createVectorHelper(0.5, 0.1875, -2.9375));
        side.nodes.add(Vec3NT.createVectorHelper(1.5, 0.1875, -2.375));
        side.nodes.add(Vec3NT.createVectorHelper(2.5, 0.1875, -1.4625));
        side.nodes.add(Vec3NT.createVectorHelper(3.5, 0.1875, -0.75));
        side.nodes.add(Vec3NT.createVectorHelper(4.5, 0.1875, -0.1875));
        side.nodes.add(Vec3NT.createVectorHelper(5.5, 0.1875, 0.175));
        side.nodes.add(Vec3NT.createVectorHelper(6.5, 0.1875, 0.375));
        side.nodes.add(Vec3NT.createVectorHelper(7.5, 0.1875, 0.5));
        side.nodes.add(Vec3NT.createVectorHelper(8.5, 0.1875, 0.5));
    }

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
        return RailStandardSwitch.canCrossSwitch(world, x, y, z, from, to, def);
    }

    //? if < 1.21.1 {
    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    //?} else {
    /*@Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack hbmHeld, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return com.hbm_m.platform.BlockUseHooks.item(hbmUse(state, world, pos, player, hand, hit));
    }
    private InteractionResult hbmUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    *///?}
        if (!world.isClientSide() && player.isShiftKeyDown()) return InteractionResult.PASS; // Original: geschlichen auf dem Server false
        return RailStandardSwitch.useSwitch(this, world, pos, player, hand);
    }

    @Override
    protected boolean checkRequirement(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        if (!super.checkRequirement(world, x, y, z, dir, o)) return false;

        // Original prueft hier mit UP (nicht DOWN wie beim Setzen)
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

        ForgeDirection rot = dir.getRotation(ForgeDirection.DOWN);
        dir = dir.getOpposite();

        int dX = dir.offsetX;
        int dZ = dir.offsetZ;
        int rX = rot.offsetX;
        int rZ = rot.offsetZ;

        for (int i = 0; i < 4; i++) setBlock(world, x + dX * (2 + i) + rX * 1, y, z + dZ * (2 + i) + rZ * 1, rot.ordinal());
        for (int i = 0; i < 2; i++) setBlock(world, x + dX * (4 + i) + rX * 2, y, z + dZ * (4 + i) + rZ * 2, rot.ordinal());
        setBlock(world, x + dX * 5 + rX * 3, y, z + dZ * 5 + rZ * 3, rot.ordinal());
        for (int j = 0; j < 2; j++) for (int i = 0; i < 2; i++) setBlock(world, x + dX * (6 + j) + rX * (2 + i), y, z + dZ * (6 + j) + rZ * (2 + i), dir.ordinal());
        setBlock(world, x + dX * 7 + rX * 4, y, z + dZ * 7 + rZ * 4, rot.ordinal());
        for (int j = 0; j < 7; j++) for (int i = 0; i < 2; i++) setBlock(world, x + dX * (8 + j) + rX * (3 + i), y, z + dZ * (8 + j) + rZ * (3 + i), dir.ordinal());

        safeRem = false;
    }
}
