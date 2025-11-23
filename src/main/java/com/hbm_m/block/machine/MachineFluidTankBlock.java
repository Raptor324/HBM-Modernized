package com.hbm_m.block.machine;
// package com.hbm_m.block;
// WIP
// import java.util.logging.Level;

// import net.minecraft.client.resources.model.Material;
// import net.minecraft.core.BlockPos;
// import net.minecraft.core.Direction;
// import net.minecraft.server.level.ServerLevel;
// import net.minecraft.world.entity.player.Player;
// import net.minecraft.world.item.ItemStack;
// import net.minecraft.world.item.context.BlockPlaceContext;
// import net.minecraft.world.level.block.Block;
// import net.minecraft.world.level.block.state.BlockBehaviour;
// import net.minecraft.world.level.block.state.BlockState;
// import net.minecraft.world.level.block.state.StateDefinition;
// import net.minecraft.world.level.block.state.properties.DirectionProperty;

// public class MachineFluidTankBlock extends Block {
//     public static final DirectionProperty FACING = DirectionProperty.EnumDirection;

//     public MachineFluidTankBlock() {
//         super(BlockBehaviour.Properties.of(Material.IRON).strength(3.0F));
//         this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
//     }

//     @Override
//     protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
//         builder.add(FACING);
//     }

//     @Override
//     public BlockState getStateForPlacement(BlockPlaceContext context) {
//         return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
//     }

//     @Override
//     public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
//         if (!world.isClientSide) {
//             ((ServerLevel)world).setBlockAndUpdate(pos, state);
//         }
//     }

//     @Override
//     public void neighborChanged(BlockState state, Level world, BlockPos pos, Block neighborBlock, BlockPos fromPos, boolean isMoving) {
//         // Можно добавить логику взаимодействия
//     }

//     @Override
//     public void setPlacedBy(World world, BlockPos pos, BlockState state, Player player, ItemStack stack) {
//         // при размещении, создать TileEntity
//         world.setBlockEntity(pos, new TileEntityFluidTank());
//     }
// }