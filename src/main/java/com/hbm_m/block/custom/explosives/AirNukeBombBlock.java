package com.hbm_m.block.custom.explosives;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.grenades.AirNukeBombProjectileEntity;
import com.hbm_m.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AirNukeBombBlock extends Block implements IDetonatable {

    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);
    private static final VoxelShape SHAPE = Shapes.box(0, 0, 0, 1, 1, 1);

    public AirNukeBombBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    // ✅ ДЕТОНАЦИЯ КАК У ВЗРЫВНОГО ЗАРЯДА
    @Override
    public boolean onDetonate(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            ServerLevel serverLevel = (ServerLevel) level;

            // ✅ СПАВНИМ АВИАБОМБУ НАВЕРХ
            spawnAirBombEntity(serverLevel, pos);

            return true;
        }
        return false;
    }

    // ✅ СОЗДАЁТ ПАДАЮЩУЮ АВИАБОМБУ
    private void spawnAirBombEntity(ServerLevel level, BlockPos pos) {
        AirNukeBombProjectileEntity airBomb = new AirNukeBombProjectileEntity(
                ModEntities.AIRNUKEBOMB_PROJECTILE.get(), level
        );

        // ✅ Направление по блоку (ПОВЕРНУТО на 180°)
        float blockYaw = switch (level.getBlockState(pos).getValue(FACING)) {
            case NORTH -> 0.0F;    // Было 180° → 0°
            case SOUTH -> 180.0F;  // Было 0° → 180°
            case EAST -> 90.0F;    // Было 270° → 90°
            case WEST -> 270.0F;   // Было 90° → 270°
            default -> 0.0F;
        };
        airBomb.syncYawWithPlane(blockYaw);


        // Позиция: НАД блоком
        Vec3 spawnPos = Vec3.atCenterOf(pos).add(0, 0, 0);
        airBomb.setPos(spawnPos);

        // Скорость падения
        airBomb.setDeltaMovement(0, -0.5, 0);
        airBomb.setItem(new ItemStack(ModItems.AIRBOMB_A.get()));

        level.addFreshEntity(airBomb);

        // УДАЛЯЕМ БЛОК
        level.removeBlock(pos, false);
    }
}
