package com.hbm_m.block.generic;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.api.block.IToolable;
import com.hbm_m.api.bomb.IBomb;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.generic.WandLogicBlockEntity;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.util.ForgeDirection;
import com.hbm_m.world.gen.util.LogicBlockActions;
import com.hbm_m.world.gen.util.LogicBlockConditions;
import com.hbm_m.world.gen.util.LogicBlockInteractions;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code BlockWandLogic} (wand_logic): Strukturplatzhalter fuer einen Logikblock. Schraubenzieher/Entschaerfer/
 * Handbohrer schalten Aktion/Bedingung/Interaktion durch (Schleichen = rueckwaerts), ein Block in der Hand wird zur
 * Tarnung, ein Zuender (IBomb) verwandelt ihn in den eigentlichen Logikblock. Die Oberseite zeigt die Ausrichtung.
 */
public class BlockWandLogic extends BaseEntityBlock implements ILookOverlay, IToolable, IBomb {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BlockWandLogic(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity player, ItemStack itemStack) {
        if (player == null) return;
        int i = Mth.floor(player.getYRot() * 4.0F / 360.0F + 0.5D) & 3;

        ForgeDirection dir = ForgeDirection.UNKNOWN;
        switch (i) {
            case 0: dir = ForgeDirection.SOUTH; break;
            case 1: dir = ForgeDirection.WEST; break;
            case 2: dir = ForgeDirection.NORTH; break;
            case 3: dir = ForgeDirection.EAST; break;
        }
        if (world.getBlockEntity(pos) instanceof WandLogicBlockEntity logic) {
            logic.placedRotation = dir.ordinal();
            logic.sync();
        }
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    //?} else {
    /*@Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack hbmHeld, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return com.hbm_m.platform.BlockUseHooks.item(hbmUse(state, world, pos, player, hand, hit));
    }
    private InteractionResult hbmUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    *///?}
        ItemStack stack = player.getItemInHand(hand);

        if (!stack.isEmpty() && stack.getItem() instanceof BlockItem ib && !player.isShiftKeyDown()) {
            Block block = ib.getBlock();
            BlockState disguise = block.defaultBlockState();

            // Original renderAsNormalBlock: nur volle, normal gerenderte Wuerfel
            if (disguise.getRenderShape() == RenderShape.MODEL && Block.isShapeFullBlock(disguise.getShape(world, pos)) && block != this) {

                if (world.getBlockEntity(pos) instanceof WandLogicBlockEntity logic) {
                    if (!world.isClientSide) {
                        logic.disguise = disguise;
                        logic.sync();
                    }
                    return InteractionResult.sidedSuccess(world.isClientSide);
                }
            }
        }
        //? if < 1.21.1 {
        return super.use(state, world, pos, player, hand, hit);
        //?} else {
        /*return net.minecraft.world.InteractionResult.PASS;
        *///?}
    }

    @Override
    public boolean onScrew(Level world, Player player, BlockPos pos, Direction side, float fX, float fY, float fZ, InteractionHand hand, ToolType tool) {
        if (!(world.getBlockEntity(pos) instanceof WandLogicBlockEntity logic)) return false;

        switch (tool) {
            case SCREWDRIVER: {
                List<String> actionNames = LogicBlockActions.getActionNames();
                int indexA = actionNames.indexOf(logic.actionID);

                indexA += player.isShiftKeyDown() ? -1 : 1;
                indexA = Mth.clamp(indexA, 0, actionNames.size() - 1);

                logic.actionID = actionNames.get(indexA);
                logic.sync();
                return true;
            }
            case DEFUSER: {
                List<String> conditionNames = LogicBlockConditions.getConditionNames();
                int indexC = conditionNames.indexOf(logic.conditionID);

                indexC += player.isShiftKeyDown() ? -1 : 1;
                indexC = Mth.clamp(indexC, 0, conditionNames.size() - 1);

                logic.conditionID = conditionNames.get(indexC);
                logic.sync();
                return true;
            }
            case HAND_DRILL: {
                List<String> interactionNames = LogicBlockInteractions.getInteractionNames();
                int indexI = interactionNames.indexOf(logic.interactionID);

                indexI += player.isShiftKeyDown() ? -1 : 1;
                indexI = Mth.clamp(indexI, 0, interactionNames.size() - 1);

                logic.interactionID = interactionNames.get(indexI);
                logic.sync();
                return true;
            }
            default: return false;
        }
    }

    @Override
    public void printHook(GuiGraphics guiGraphics, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof WandLogicBlockEntity logic)) return;

        List<Component> text = new ArrayList<>();
        text.add(Component.literal("Action: " + logic.actionID));
        text.add(Component.literal("Condition: " + logic.conditionID));
        text.add(Component.literal("Interaction: " + (logic.interactionID != null ? logic.interactionID : "None")));

        String block;

        if (logic.disguise != null && !logic.disguise.isAir())
            block = logic.disguise.getBlock().getName().getString();
        else
            block = "None";

        text.add(Component.literal("Disguise Block: " + block));

        ILookOverlay.printGeneric(guiGraphics, getName(), 0xffff00, 0x404000, text);
    }

    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, TooltipFlag flag) {
    *///?}
        list.add(Component.literal("Use screwdriver to cycle forwards through the action list, shift click to go back").withStyle(ChatFormatting.GOLD));
        list.add(Component.literal("Use defuser to cycle forwards through the condition list, shift click to go back").withStyle(ChatFormatting.GOLD));
        list.add(Component.literal("Use hand drill to cycle forwards through the interaction list, shift click to go back").withStyle(ChatFormatting.GOLD));
        list.add(Component.literal("Use a detonator to transform").withStyle(ChatFormatting.YELLOW));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WandLogicBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.WAND_LOGIC.get(), WandLogicBlockEntity::tick);
    }

    @Override
    public BombReturnCode explode(Level world, BlockPos pos) {
        // Original null - im Port UNDEFINED, damit die Zuender nicht ins Leere greifen
        if (!(world.getBlockEntity(pos) instanceof WandLogicBlockEntity logic)) return BombReturnCode.UNDEFINED;

        logic.setTriggerReplace();

        return BombReturnCode.TRIGGERED;
    }
    //? if >= 1.21.1 {
    /*public static final com.mojang.serialization.MapCodec<BlockWandLogic> CODEC = simpleCodec(BlockWandLogic::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
