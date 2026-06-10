package com.hbm_m.block.machines;

import javax.annotation.Nullable;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.entity.machines.MachineDeuteriumTowerBlockEntity;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.MultiblockStructureStubs;
import com.hbm_m.multiblock.PartRole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.network.NetworkHooks;

public class MachineDeuteriumTowerBlock extends BaseEntityBlock implements IMultiblockController {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private final MultiblockStructureHelper structureHelper;
    public MachineDeuteriumTowerBlock(BlockBehaviour.Properties p) {
        super(p);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.structureHelper = MultiblockStructureStubs.singleController();
    }
    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
    @Nullable @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return this.defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite()); }
    @Override public void onRemove(BlockState s, Level l, BlockPos p, BlockState ns, boolean m) {
        if (s.getBlock() != ns.getBlock()) { BlockEntity be = l.getBlockEntity(p); if (be != null) be.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(h -> { for (int i = 0; i < h.getSlots(); i++) Containers.dropItemStack(l, p.getX(), p.getY(), p.getZ(), h.getStackInSlot(i)); }); }
        super.onRemove(s, l, p, ns, m);
    }
    @Nullable @Override public BlockEntity newBlockEntity(BlockPos p, BlockState s) { return new MachineDeuteriumTowerBlockEntity(p, s); }
    @Override public InteractionResult use(BlockState s, Level l, BlockPos p, Player pl, InteractionHand h, BlockHitResult r) {
        if (!l.isClientSide() && l.getBlockEntity(p) instanceof MenuProvider mp) NetworkHooks.openScreen((ServerPlayer) pl, mp, p);
        return InteractionResult.sidedSuccess(l.isClientSide());
    }
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        return createTickerHelper(t, ModBlockEntities.DEUTERIUM_TOWER_BE.get(), MachineDeuteriumTowerBlockEntity::tick);
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        return structureHelper;
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        return structureHelper.resolvePartRole(localOffset, this);
    }
}
