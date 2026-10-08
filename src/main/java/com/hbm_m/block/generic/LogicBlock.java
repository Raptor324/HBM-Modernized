package com.hbm_m.block.generic;

import javax.annotation.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.generic.LogicBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code LogicBlock} (logic_block, "Dungeon Action Block"): Traeger der Dungeon-Logik (Aktion/Bedingung/Interaktion,
 * siehe {@code LogicBlockActions} usw.). Erscheint als Tarnblock - das zeichnet der {@code LogicBlockRenderer}.
 */
public class LogicBlock extends BaseEntityBlock {

    public LogicBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Tarnung haengt am Blockentity, daher zeichnet der BER (Tarnblock oder eigenes Modell)
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LogicBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.LOGIC_BLOCK.get(), LogicBlockEntity::tick);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (world.getBlockEntity(pos) instanceof LogicBlockEntity logic && logic.interaction != null) {
            logic.interaction.accept(new Object[] {world, logic, pos.getX(), pos.getY(), pos.getZ(), player, hit.getDirection().get3DDataValue(),
                    (float) (hit.getLocation().x - pos.getX()), (float) (hit.getLocation().y - pos.getY()), (float) (hit.getLocation().z - pos.getZ())});
            return InteractionResult.SUCCESS;
        }

        return super.use(state, world, pos, player, hand, hit);
    }
    //?}
}
