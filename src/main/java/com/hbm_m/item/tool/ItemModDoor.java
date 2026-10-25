package com.hbm_m.item.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * 1:1 {@code com.hbm.items.tool.ItemModDoor} ({@code door_metal}, {@code door_office}, {@code door_bunker},
 * {@code door_red}): Stapel 1, nur auf die Oberseite setzbar, Ausrichtung nach Blickrichtung, Scharnier wie
 * {@code placeDoorBlock} (Nachbartuer bzw. mehr Vollbloecke rechts = Scharnier rechts). Bleibt BlockItem,
 * damit Mittelklick und Blockzuordnung stimmen.
 */
public class ItemModDoor extends BlockItem {

    /** Alte Tuer-Meta 0..3 -> Ausrichtung (Weltkonverter: 0 Ost, 1 Sued, 2 West, 3 Nord). */
    private static final Direction[] META_FACING = { Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH };

    public ItemModDoor(Block block, Properties props) {
        super(block, props.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getClickedFace() != Direction.UP) return InteractionResult.PASS;

        Level world = ctx.getLevel();
        Player player = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();
        BlockPos pos = ctx.getClickedPos().above();
        Block block = getBlock();

        if (player != null && (!player.mayUseItemAt(pos, Direction.UP, stack) || !player.mayUseItemAt(pos.above(), Direction.UP, stack))) {
            return InteractionResult.PASS;
        }
        if (!canPlaceBlockAt(world, pos)) return InteractionResult.PASS;

        float yaw = player != null ? player.getYRot() : 0.0F;
        int i1 = Mth.floor((double) ((yaw + 180.0F) * 4.0F / 360.0F) - 0.5D) & 3;
        placeDoorBlock(world, pos, i1, block);
        if (player == null || !player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResult.sidedSuccess(world.isClientSide);
    }

    /** {@code BlockDoor.canPlaceBlockAt}: unter der Bauhoehe, feste Oberseite darunter, beide Felder ersetzbar. */
    private static boolean canPlaceBlockAt(Level world, BlockPos pos) {
        if (pos.getY() >= world.getMaxBuildHeight() - 1) return false;
        BlockPos below = pos.below();
        return world.getBlockState(below).isFaceSturdy(world, below, Direction.UP)
                && world.getBlockState(pos).canBeReplaced()
                && world.getBlockState(pos.above()).canBeReplaced();
    }

    public static void placeDoorBlock(Level world, BlockPos pos, int meta, Block door) {
        int offsetX = 0;
        int offsetZ = 0;

        if (meta == 0) offsetZ = 1;
        if (meta == 1) offsetX = -1;
        if (meta == 2) offsetZ = -1;
        if (meta == 3) offsetX = 1;

        BlockPos minus = pos.offset(-offsetX, 0, -offsetZ);
        BlockPos plus = pos.offset(offsetX, 0, offsetZ);

        int i1 = (isNormalCube(world, minus) ? 1 : 0) + (isNormalCube(world, minus.above()) ? 1 : 0);
        int j1 = (isNormalCube(world, plus) ? 1 : 0) + (isNormalCube(world, plus.above()) ? 1 : 0);
        boolean flag = world.getBlockState(minus).is(door) || world.getBlockState(minus.above()).is(door);
        boolean flag1 = world.getBlockState(plus).is(door) || world.getBlockState(plus.above()).is(door);
        boolean flag2 = false;

        if (flag && !flag1) {
            flag2 = true;
        } else if (j1 > i1) {
            flag2 = true;
        }

        BlockState lower = door.defaultBlockState()
                .setValue(DoorBlock.FACING, META_FACING[meta])
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
                .setValue(DoorBlock.HINGE, flag2 ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT)
                .setValue(DoorBlock.OPEN, false)
                .setValue(DoorBlock.POWERED, false);

        world.setBlock(pos, lower, Block.UPDATE_CLIENTS);
        world.setBlock(pos.above(), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_CLIENTS);
        world.updateNeighborsAt(pos, door);
        world.updateNeighborsAt(pos.above(), door);
    }

    /** {@code Block.isNormalCube}: undurchsichtiger Vollblock, der keinen Strom liefert. */
    private static boolean isNormalCube(Level world, BlockPos pos) {
        return world.getBlockState(pos).isRedstoneConductor(world, pos);
    }
}
