package com.hbm_m.block.custom.weapons;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

public class BarbedWirePoisonBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BarbedWirePoisonBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity instanceof LivingEntity living && !level.isClientSide) {
            // Замедление как паутина
            entity.makeStuckInBlock(state, new Vec3(0.25D, 0.05D, 0.25D));

            // Применяем эффекты каждые 0.5 сек (10 тиков)
            if (level.getGameTime() % 10 == 0) {
                // Пропускаем творческий режим и режим наблюдателя
                if (living instanceof Player player && (player.isCreative() || player.isSpectator())) {
                    return;
                }

                // Если броня < 8 — применяем полный урон и эффекты
                if (getTotalArmorPoints(living) < 8) {
                    // Урон 1 HP (0.5 сердца) каждые 0.5 сек
                    living.hurt(level.damageSources().cactus(), 1.0F);

                    // Отравление на 6 сек (уровень 2)
                    living.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1));
                }
            }
        }
    }

    // Правильный способ посчитать броню в 1.20.1
    private int getTotalArmorPoints(LivingEntity entity) {
        AttributeInstance attribute = entity.getAttribute(Attributes.ARMOR);
        if (attribute == null) return 0;
        return (int) Math.round(attribute.getValue());
    }

    // Визуальный хитбокс
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Block.box(0, 0, 0, 16, 16, 16);
    }

    // Полностью проходимый
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder builder) {
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
