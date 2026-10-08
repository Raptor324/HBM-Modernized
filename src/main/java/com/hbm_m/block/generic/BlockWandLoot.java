package com.hbm_m.block.generic;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.api.block.IToolable;
import com.hbm_m.api.bomb.IBomb;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.generic.WandLootBlockEntity;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.item.ItemKeyPin;
import com.hbm_m.item.tool.ItemLock;

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
import net.minecraft.world.level.block.EntityBlock;
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
 * 1:1 {@code BlockWandLoot} (wand_loot): Platzhalter fuer Lootbehaelter/-stapel in .nbt-Strukturen.
 * Schraubenzieher = Mindestzahl, Handbohrer = Hoechstzahl, Entschaerfer = Pool durchschalten (Schleichen = zurueck),
 * ein Behaelterblock in der Hand wird zum Ersatzblock, ein Vorhaengeschloss legt das Schloss fest, ein Zuender loest
 * den Austausch aus. Die Oberseite zeigt die Ausrichtung (IBlockSideRotation).
 */
public class BlockWandLoot extends BaseEntityBlock implements ILookOverlay, IToolable, IBomb {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BlockWandLoot(Properties properties) {
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
        if (world.getBlockEntity(pos) instanceof WandLootBlockEntity loot) {
            loot.placedRotation = player.getYRot();
            loot.sync();
        }
    }

    @Override
    public void printHook(GuiGraphics guiGraphics, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof WandLootBlockEntity loot)) return;

        List<Component> text = new ArrayList<>();
        text.add(Component.literal("Will replace with: " + loot.replaceBlock.getDescriptionId()));
        text.add(Component.literal("   meta: " + loot.replaceMeta));
        text.add(Component.literal("Loot pool: " + loot.poolName));
        if (loot.replaceBlock != ModBlocks.DECO_LOOT.get()) {
            text.add(Component.literal("Minimum items: " + loot.minItems));
            text.add(Component.literal("Maximum items: " + loot.maxItems));
        }

        if (loot.lockCode != 0) {
            text.add(Component.literal("Container will be locked"));
            text.add(Component.literal("Lockpicking chance:" + loot.lockMod));
        }

        ILookOverlay.printGeneric(guiGraphics, getName(), 0xffff00, 0x404000, text);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("Define loot crates/piles in .nbt structures"));
        list.add(Component.literal("Use screwdriver to increase/decrease minimum loot").withStyle(ChatFormatting.GOLD));
        list.add(Component.literal("Use hand drill to increase/decrease maximum loot").withStyle(ChatFormatting.GOLD));
        list.add(Component.literal("Use defuser to cycle loot types").withStyle(ChatFormatting.GOLD));
        list.add(Component.literal("Use container block to set the block that spawns with loot inside").withStyle(ChatFormatting.GOLD));
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof WandLootBlockEntity loot)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);

        if (!player.isShiftKeyDown()) {
            Block block = getLootableBlock(world, pos, held);

            if (block != null) {
                if (!world.isClientSide) {
                    loot.replaceBlock = block;
                    loot.replaceMeta = 0;

                    List<String> poolNames = loot.getPoolNames(block == ModBlocks.DECO_LOOT.get());
                    if (!poolNames.contains(loot.poolName)) {
                        loot.poolName = poolNames.get(0);
                    }
                    loot.sync();
                }
                return InteractionResult.sidedSuccess(world.isClientSide);
            }
        }

        if (!held.isEmpty() && held.getItem() instanceof ItemLock lock) {
            if (!world.isClientSide) {
                loot.lockMod = (float) lock.lockMod;
                loot.lockCode = ItemKeyPin.getPins(held);
                loot.sync();
            }
        }

        return InteractionResult.PASS;
    }
    //?}

    @Nullable
    private Block getLootableBlock(Level world, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem bi)) return null;
        Block block = bi.getBlock();

        if (block == ModBlocks.DECO_LOOT.get()) return block;

        if (block instanceof EntityBlock eb) {
            BlockEntity te = eb.newBlockEntity(pos, block.defaultBlockState());
            if (com.hbm_m.itempool.ItemPool.isInventory(te)) return block;
        }

        return null;
    }

    @Override
    public boolean onScrew(Level world, Player player, BlockPos pos, Direction side, float fX, float fY, float fZ, InteractionHand hand, ToolType tool) {
        if (!(world.getBlockEntity(pos) instanceof WandLootBlockEntity loot)) return false;

        switch (tool) {
            case SCREWDRIVER:
                if (player.isShiftKeyDown()) {
                    loot.minItems--;
                    if (loot.minItems < 0) loot.minItems = 0;
                } else {
                    loot.minItems++;
                    loot.maxItems = Math.max(loot.minItems, loot.maxItems);
                }
                loot.sync();
                return true;

            case HAND_DRILL:
                if (player.isShiftKeyDown()) {
                    loot.maxItems--;
                    if (loot.maxItems < 0) loot.maxItems = 0;
                    loot.minItems = Math.min(loot.minItems, loot.maxItems);
                } else {
                    loot.maxItems++;
                }
                loot.sync();
                return true;

            case DEFUSER: {
                List<String> poolNames = loot.getPoolNames(loot.replaceBlock == ModBlocks.DECO_LOOT.get());
                int index = poolNames.indexOf(loot.poolName);

                index += player.isShiftKeyDown() ? -1 : 1;
                index = Mth.clamp(index, 0, poolNames.size() - 1);

                loot.poolName = poolNames.get(index);
                loot.sync();
                return true;
            }

            default: return false;
        }
    }

    @Override
    public BombReturnCode explode(Level world, BlockPos pos) {
        // Original null - im Port UNDEFINED
        if (!(world.getBlockEntity(pos) instanceof WandLootBlockEntity loot)) return BombReturnCode.UNDEFINED;

        loot.setTriggerReplace();

        return BombReturnCode.TRIGGERED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WandLootBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.WAND_LOOT.get(), WandLootBlockEntity::tick);
    }
}
