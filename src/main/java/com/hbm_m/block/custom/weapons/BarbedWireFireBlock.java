package com.hbm_m.block.custom.weapons;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BarbedWireFireBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BarbedWireFireBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity instanceof LivingEntity living && !level.isClientSide) {
            // Замедление как паутина
            entity.makeStuckInBlock(state, new Vec3(0.25D, 0.05D, 0.25D));

            // Урон и поджог каждые 0.5 сек (10 тиков)
            if (level.getGameTime() % 10 == 0) {
                if (living instanceof Player player && (player.isCreative() || player.isSpectator())) {
                    return;
                }

                // Проверяем броню — если меньше 8, то жжём и режем
                if (getTotalArmorPoints(living) < 8) {
                    // Урон увеличен до 2 хп (1 сердце) за тик
                    living.hurt(level.damageSources().cactus(), 2.0F);

                    // Поджигаем на 4 секунды (80 тиков)
                    living.setSecondsOnFire(4);
                }
            }
        }
    }

    // Правильный способ посчитать броню в 1.20.1
    private int getTotalArmorPoints(LivingEntity entity) {
        AttributeInstance attribute = entity.getAttribute(Attributes.ARMOR);
        if (attribute == null) return 0;
        return (int) Math.round(attribute.getValue()); // уже учитывает всю экипировку
    }

    // Визуальный хитбокс (поменяй под свою модель)
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Block.box(0, 0, 0, 16, 16, 16); // пока полный блок
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty(); // полностью проходимый
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    // FACING
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }
}