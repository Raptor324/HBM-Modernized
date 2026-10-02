package com.hbm_m.item.tool;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.network.ConveyorBend;
import com.hbm_m.block.network.ConveyorBendableBlock;
import com.hbm_m.block.network.ConveyorBlockBase;
import com.hbm_m.interfaces.ILookOverlay;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.items.tool.ItemConveyorWand} ({@code conveyor_wand}, Metadaten REGULAR/EXPRESS/DOUBLE/TRIPLE
 * als eigene Gegenstaende): der einzige Weg, Foerderbaender zu setzen. Erster Klick merkt Start und Seite, zweiter
 * Klick baut die Strecke inklusive Kurven, Aufzuegen und Rutschen; Schleichklick setzt ein einzelnes Band bzw. wandelt
 * ein gerades Band nach oben/unten in Aufzug/Rutsche um. Im Kreativmodus reisst Schleich-Abbauen die ganze Strecke ab.
 * <p>
 * Metadaten des Originals: gerade Baender {@code 2..5} (Richtung), {@code +4} Linkskurve, {@code +8} Rechtskurve;
 * Aufzug/Rutsche tragen nur die Richtung. Hier als {@link ConveyorBlockBase#FACING} / {@link ConveyorBendableBlock#BEND}.
 */
public class ItemConveyorWand extends Item implements ILookOverlay {

    public enum ConveyorType {
        REGULAR,
        EXPRESS,
        DOUBLE,
        TRIPLE
    }

    public final ConveyorType type;

    public ItemConveyorWand(Properties properties, ConveyorType type) {
        super(properties);
        this.type = type;
    }

    public static ConveyorType getType(ItemStack stack) {
        if (stack.getItem() instanceof ItemConveyorWand wand) return wand.type;
        return ConveyorType.REGULAR;
    }

    public static Block getConveyorBlock(ConveyorType type) {
        switch (type) {
            case EXPRESS: return ModBlocks.CONVEYOR_EXPRESS.get();
            case DOUBLE: return ModBlocks.CONVEYOR_DOUBLE.get();
            case TRIPLE: return ModBlocks.CONVEYOR_TRIPLE.get();
            default: return ModBlocks.CONVEYOR.get();
        }
    }

    public static boolean hasSnakesAndLadders(ConveyorType type) {
        return type == ConveyorType.REGULAR;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            for (String s : I18n.get("item.hbm_m.conveyor_wand.desc").split("\\$")) {
                list.add(Component.literal(s).withStyle(ChatFormatting.YELLOW));
            }
            if (hasSnakesAndLadders(getType(stack))) {
                list.add(Component.translatable("item.hbm_m.conveyor_wand.vertical.desc").withStyle(ChatFormatting.AQUA));
            }
        } else {
            list.add(Component.literal("Hold <").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
                    .append(Component.literal("LSHIFT").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC))
                    .append(Component.literal("> to display more info").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        ItemStack stack = ctx.getItemInHand();
        Player player = ctx.getPlayer();
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Direction side = ctx.getClickedFace();
        if (player == null) return InteractionResult.PASS;

        if (player.isShiftKeyDown() && !stack.hasTag()) {
            Direction dir = side;
            BlockState onState = world.getBlockState(pos);
            Block onBlock = onState.getBlock();
            ConveyorType type = getType(stack);

            if (hasSnakesAndLadders(type) && onBlock == ModBlocks.CONVEYOR.get() && onState.getValue(ConveyorBendableBlock.BEND) == ConveyorBend.STRAIGHT) {
                if (dir == Direction.UP) {
                    onBlock = ModBlocks.CONVEYOR_LIFT.get();
                    world.setBlock(pos, onBlock.defaultBlockState().setValue(ConveyorBlockBase.FACING, onState.getValue(ConveyorBlockBase.FACING)), 3);
                } else if (dir == Direction.DOWN) {
                    onBlock = ModBlocks.CONVEYOR_CHUTE.get();
                    world.setBlock(pos, onBlock.defaultBlockState().setValue(ConveyorBlockBase.FACING, onState.getValue(ConveyorBlockBase.FACING)), 3);
                }
            }

            Block toPlace = getConveyorBlock(type);
            if (hasSnakesAndLadders(type)) {
                if (onBlock == ModBlocks.CONVEYOR_LIFT.get() && dir == Direction.UP) toPlace = ModBlocks.CONVEYOR_LIFT.get();
                if (onBlock == ModBlocks.CONVEYOR_CHUTE.get() && dir == Direction.DOWN) toPlace = ModBlocks.CONVEYOR_CHUTE.get();
            }

            BlockPos target = pos.relative(dir);

            if (world.getBlockState(target).canBeReplaced()) {
                // setBlock + onBlockPlacedBy: Ausrichtung nach Blickrichtung des Spielers
                world.setBlock(target, toPlace.defaultBlockState().setValue(ConveyorBlockBase.FACING, getFacing(player)), 3);
                if (!player.getAbilities().instabuild) stack.shrink(1);
            }

            return InteractionResult.sidedSuccess(world.isClientSide);
        }

        // If placing on top of a conveyor block, auto-snap to edge if possible
        // this makes it easier to connect without having to click the small edge of a conveyor
        BlockState onState = world.getBlockState(pos);
        if (onState.getBlock() instanceof ConveyorBendableBlock bendable) {
            Direction moveDir = stack.hasTag() ? bendable.getInputDirection(onState) : bendable.getOutputDirection(onState);
            if (world.getBlockState(pos.relative(moveDir)).canBeReplaced()) {
                side = moveDir;
            }
        }

        if (!stack.hasTag()) {
            // Starting placement
            CompoundTag nbt = new CompoundTag();
            nbt.putInt("x", pos.getX());
            nbt.putInt("y", pos.getY());
            nbt.putInt("z", pos.getZ());
            nbt.putInt("side", side.get3DDataValue());

            int count = 0;
            if (player.getAbilities().instabuild) {
                count = 256;
            } else {
                for (ItemStack inventoryStack : player.getInventory().items) {
                    if (!inventoryStack.isEmpty() && inventoryStack.getItem() == this) {
                        count += inventoryStack.getCount();
                    }
                }
            }

            nbt.putInt("count", count);
            stack.setTag(nbt);
        } else {
            // Constructing conveyor
            CompoundTag nbt = stack.getTag();

            BlockPos start = new BlockPos(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"));
            Direction sSide = Direction.from3DDataValue(nbt.getInt("side"));
            int count = nbt.getInt("count");

            if (!world.isClientSide) {
                ConveyorType type = getType(stack);

                // pretend to construct, if it doesn't fail, actually construct
                int constructCount = construct(world, null, type, player, start, sSide, pos, side, count);
                if (constructCount > 0) {
                    int toRemove = construct(world, (p, s) -> world.setBlock(p, s, 3), type, player, start, sSide, pos, side, count);

                    if (!player.getAbilities().instabuild) {
                        for (ItemStack inventoryStack : player.getInventory().items) {
                            if (!inventoryStack.isEmpty() && inventoryStack.getItem() == this) {
                                int removing = Math.min(toRemove, inventoryStack.getCount());
                                inventoryStack.shrink(removing);
                                toRemove -= removing;
                            }

                            if (toRemove <= 0) break;
                        }

                        player.inventoryMenu.broadcastChanges();
                    }

                    player.sendSystemMessage(Component.literal("Conveyor built!"));
                } else if (constructCount == 0) {
                    player.sendSystemMessage(Component.literal("Not enough conveyors, build cancelled"));
                } else {
                    player.sendSystemMessage(Component.literal("Conveyor obstructed, build cancelled"));
                }
            } else {
                com.hbm_m.client.render.ConveyorWandPreview.clear();
            }

            stack.setTag(null);
        }

        return InteractionResult.sidedSuccess(world.isClientSide); // always eat interactions
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean inHand) {
        if (!(entity instanceof Player player)) return;

        if (!inHand && stack.hasTag()) {
            ItemStack held = player.getMainHandItem();
            if (held.isEmpty() || held.getItem() != this) {
                stack.setTag(null);
                if (world.isClientSide) com.hbm_m.client.render.ConveyorWandPreview.clear();
            }
        }

        // clientside prediction only
        if (world.isClientSide && inHand) {
            com.hbm_m.client.render.ConveyorWandPreview.update(this, stack, world, player);
        }
    }

    /** Bauschritt, der einen Block setzt ({@code World.setBlock} oder die Vorschau). */
    public interface BlockSink {
        void set(BlockPos pos, BlockState state);
    }

    /** Horizontale Richtung + Kurve (Original-Metadaten) als Blockzustand. */
    private static BlockState stateFor(Block block, Direction facing, int bend) {
        BlockState state = block.defaultBlockState().setValue(ConveyorBlockBase.FACING, facing);
        if (state.hasProperty(ConveyorBendableBlock.BEND)) {
            state = state.setValue(ConveyorBendableBlock.BEND, bend == 1 ? ConveyorBend.LEFT : bend == 2 ? ConveyorBend.RIGHT : ConveyorBend.STRAIGHT);
        }
        return state;
    }

    /** attempts to construct a conveyor between two points, including bends, lifts, and chutes */
    public static int construct(Level routeWorld, @Nullable BlockSink buildWorld, ConveyorType type, Player player,
                                BlockPos p1, Direction side1, BlockPos p2, Direction side2, int max) {
        Direction dir = side1;
        Direction targetDir = side2;
        int x1 = p1.getX(), y1 = p1.getY(), z1 = p1.getZ();
        int x2 = p2.getX(), y2 = p2.getY(), z2 = p2.getZ();

        // if placing within a single block, we have to handle rotation specially, treating it like a manual placement with player facing
        if (p1.equals(p2) && side1 == side2 && (dir == Direction.UP || dir == Direction.DOWN)) {
            Direction facing = getFacing(player);

            y1 += dir.getStepY();
            BlockPos at = new BlockPos(x1, y1, z1);

            if (!routeWorld.getBlockState(at).canBeReplaced()) return -1;

            if (buildWorld != null) buildWorld.set(at, stateFor(getConveyorBlock(type), facing, 0));

            return 1;
        }

        boolean hasVertical = hasSnakesAndLadders(type);

        int tx = x2 + targetDir.getStepX();
        int ty = y2 + targetDir.getStepY();
        int tz = z2 + targetDir.getStepZ();

        int x = x1 + dir.getStepX();
        int y = y1 + dir.getStepY();
        int z = z1 + dir.getStepZ();

        if (dir == Direction.UP || dir == Direction.DOWN) {
            dir = getTargetDirection(x, y, z, x2, y2, z2, hasVertical);
        }

        Block targetBlock = routeWorld.getBlockState(p2).getBlock();
        boolean isTargetHorizontal = targetDir != Direction.UP && targetDir != Direction.DOWN;
        boolean shouldTurnToTarget = isTargetHorizontal || isCrane(targetBlock)
                || targetBlock == ModBlocks.CONVEYOR_LIFT.get() || targetBlock == ModBlocks.CONVEYOR_CHUTE.get();

        Direction horDir = dir == Direction.UP || dir == Direction.DOWN ? getFacing(player).getOpposite() : dir;

        // Initial dropdown to floor level, if possible
        if (hasVertical && y > ty) {
            if (routeWorld.getBlockState(new BlockPos(x, y - 1, z)).canBeReplaced()) {
                dir = Direction.DOWN;
            }
        }

        for (int loopDepth = 1; loopDepth <= max; loopDepth++) {
            if (!routeWorld.getBlockState(new BlockPos(x, y, z)).canBeReplaced()) return -1;

            Block block = getConveyorForDirection(type, dir);
            Direction facing = getConveyorFacingForDirection(block, dir, targetDir, horDir);
            int bend = 0;

            int ox = x + dir.getStepX();
            int oy = y + dir.getStepY();
            int oz = z + dir.getStepZ();

            // check if we should turn before continuing
            int fromDistance = taxiDistance(x, y, z, tx, ty, tz);
            int toDistance = taxiDistance(ox, oy, oz, tx, ty, tz);
            int finalDistance = taxiDistance(ox, oy, oz, x2, y2, z2);
            boolean notAtTarget = (shouldTurnToTarget ? finalDistance : fromDistance) > 0;
            boolean willBeObstructed = notAtTarget && !routeWorld.getBlockState(new BlockPos(ox, oy, oz)).canBeReplaced();
            boolean shouldTurn = (toDistance >= fromDistance && notAtTarget) || willBeObstructed;

            if (shouldTurn) {
                Direction newDir = getTargetDirection(x, y, z, shouldTurnToTarget ? x2 : tx, shouldTurnToTarget ? y2 : ty, shouldTurnToTarget ? z2 : tz, tx, ty, tz, dir, willBeObstructed, hasVertical);

                if (newDir == Direction.UP) {
                    block = ModBlocks.CONVEYOR_LIFT.get();
                } else if (newDir == Direction.DOWN) {
                    block = ModBlocks.CONVEYOR_CHUTE.get();
                } else if (rotUp(dir) == newDir) {
                    bend = 2; // meta += 8
                } else if (rotDown(dir) == newDir) {
                    bend = 1; // meta += 4
                }

                dir = newDir;
                if (dir != Direction.UP && dir != Direction.DOWN) horDir = dir;
            }

            if (buildWorld != null) buildWorld.set(new BlockPos(x, y, z), stateFor(block, facing, bend));

            if (x == tx && y == ty && z == tz) return loopDepth;

            x += dir.getStepX();
            y += dir.getStepY();
            z += dir.getStepZ();
        }

        return 0;
    }

    /** {@code instanceof BlockCraneBase}: Boxer, Extractor, Grabber, Inserter, Unboxer. */
    private static boolean isCrane(Block b) {
        return b instanceof com.hbm_m.block.machines.MachineCraneBoxerBlock || b instanceof com.hbm_m.block.machines.MachineCraneExtractorBlock
                || b instanceof com.hbm_m.block.machines.MachineCraneGrabberBlock || b instanceof com.hbm_m.block.machines.MachineCraneInserterBlock
                || b instanceof com.hbm_m.block.machines.MachineCraneUnboxerBlock;
    }

    /** ForgeDirection.getRotation(UP) fuer horizontale Richtungen, vertikale bleiben. */
    private static Direction rotUp(Direction d) {
        return d.getAxis().isHorizontal() ? d.getClockWise() : d;
    }

    /** ForgeDirection.getRotation(DOWN) fuer horizontale Richtungen, vertikale bleiben. */
    private static Direction rotDown(Direction d) {
        return d.getAxis().isHorizontal() ? d.getCounterClockWise() : d;
    }

    /** {@code getFacingMeta}: 0/1/2/3 -> 2/5/3/4, also die Gegenrichtung des Blicks. */
    public static Direction getFacing(Player player) {
        int meta = Mth.floor(player.getYRot() * 4.0F / 360.0F + 0.5D) & 3;
        switch (meta) {
            case 0: return Direction.NORTH;
            case 1: return Direction.EAST;
            case 2: return Direction.SOUTH;
            case 3: return Direction.WEST;
        }
        return Direction.NORTH;
    }

    private static Direction getConveyorFacingForDirection(Block block, Direction dir, Direction targetDir, Direction horDir) {
        if (block != ModBlocks.CONVEYOR_CHUTE.get() && block != ModBlocks.CONVEYOR_LIFT.get()) return dir.getOpposite();
        if (targetDir == Direction.UP || targetDir == Direction.DOWN) return horDir.getOpposite();
        return targetDir;
    }

    private static Block getConveyorForDirection(ConveyorType type, Direction dir) {
        if (dir == Direction.UP) return ModBlocks.CONVEYOR_LIFT.get();
        if (dir == Direction.DOWN) return ModBlocks.CONVEYOR_CHUTE.get();
        return getConveyorBlock(type);
    }

    private static Direction getTargetDirection(int x1, int y1, int z1, int x2, int y2, int z2, boolean hasVertical) {
        return getTargetDirection(x1, y1, z1, x2, y2, z2, x2, y2, z2, null, false, hasVertical);
    }

    private static Direction getTargetDirection(int x1, int y1, int z1, int x2, int y2, int z2, int tx, int ty, int tz, @Nullable Direction heading, boolean willBeObstructed, boolean hasVertical) {
        if (hasVertical && (y1 != y2 || y1 != ty) && (willBeObstructed || (x1 == x2 && z1 == z2) || (x1 == tx && z1 == tz))) return y1 > y2 ? Direction.DOWN : Direction.UP;

        if (Math.abs(x1 - x2) > Math.abs(z1 - z2)) {
            if (heading == Direction.EAST || heading == Direction.WEST) return z1 > z2 ? Direction.NORTH : Direction.SOUTH;
            return x1 > x2 ? Direction.WEST : Direction.EAST;
        } else {
            if (heading == Direction.NORTH || heading == Direction.SOUTH) return x1 > x2 ? Direction.WEST : Direction.EAST;
            return z1 > z2 ? Direction.NORTH : Direction.SOUTH;
        }
    }

    private static int taxiDistance(int x1, int y1, int z1, int x2, int y2, int z2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2) + Math.abs(z1 - z2);
    }

    // In creative, auto delete connected conveyors
    @Override
    public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, Player playerEntity) {
        if (!playerEntity.isShiftKeyDown()) return false;

        Level world = playerEntity.level();
        BlockState state = world.getBlockState(pos);

        if (!playerEntity.getAbilities().instabuild) return false;
        if (!(playerEntity instanceof ServerPlayer player)) return false;

        if (!world.isClientSide && state.getBlock() instanceof ConveyorBlockBase conveyor) {
            Direction input = conveyor.getInputDirection(state);
            Direction output = conveyor.getOutputDirection(state);
            breakExtra(world, player, pos.relative(input), 32);
            breakExtra(world, player, pos.relative(output), 32);
        }

        return false;
    }

    private void breakExtra(Level world, ServerPlayer player, BlockPos pos, int depth) {
        depth--;
        if (depth <= 0) return;

        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof ConveyorBlockBase conveyor)) return;

        Direction input = conveyor.getInputDirection(state);
        Direction output = conveyor.getOutputDirection(state);

        if (net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(world, player.gameMode.getGameModeForPlayer(), player, pos) == -1) return;

        state.getBlock().playerWillDestroy(world, pos, state, player);
        if (state.onDestroyedByPlayer(world, pos, player, false, world.getFluidState(pos))) {
            state.getBlock().destroy(world, pos, state);
        }

        player.connection.send(new ClientboundBlockUpdatePacket(world, pos));
        breakExtra(world, player, pos.relative(input), depth);
        breakExtra(world, player, pos.relative(output), depth);
    }

    @Override
    public void printHook(GuiGraphics guiGraphics, Level world, BlockPos pos) {
        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null || !player.isShiftKeyDown() || !player.getAbilities().instabuild) return;

        Block block = world.getBlockState(pos).getBlock();
        if (block instanceof ConveyorBlockBase) {
            List<Component> text = new ArrayList<>();
            text.add(Component.literal("Break whole conveyor line"));
            ILookOverlay.printGeneric(guiGraphics, block.getName(), 0xffff00, 0x404000, text);
        }
    }

    /** Fuer die Vorschau: ersetzbar laut echter Welt. */
    public static boolean replaceable(BlockGetter world, BlockPos pos) {
        return world.getBlockState(pos).canBeReplaced();
    }
}
