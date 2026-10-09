package com.hbm_m.block.weapons;

import com.hbm_m.platform.PlatformHooks;

import com.hbm_m.platform.EffectHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BarbedWireBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BarbedWireBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /**
     * 1:1 {@code BarbedWire.onEntityCollidedWithBlock}: wie Spinnennetz festhalten und bremsen (x/z * 0.15, y * 0.1),
     * dazu je nach Art 2 Kaktusschaden (+ Feuer 1 s / Gift 5 s Stufe 3 / Saeure auf die Ruestung / Wither 5 s Stufe 5)
     * bzw. beim Todesdraht 5 Schaden "pc" und Strahlung 5 s Stufe 10. Jeden Tick, keine Ruestungspruefung.
     */
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity ent) {
        ent.makeStuckInBlock(state, new Vec3(0.15D, 0.1D, 0.15D));

        if (level.isClientSide) return;
        Block self = state.getBlock();

        if (self == com.hbm_m.block.ModBlocks.BARBED_WIRE.get()) {
            ent.hurt(level.damageSources().cactus(), 2.0F);
        }

        if (self == com.hbm_m.block.ModBlocks.BARBED_WIRE_FIRE.get()) {
            ent.hurt(level.damageSources().cactus(), 2.0F);
            PlatformHooks.setSecondsOnFire(ent, 1);
        }

        if (self == com.hbm_m.block.ModBlocks.BARBED_WIRE_POISON.get()) {
            ent.hurt(level.damageSources().cactus(), 2.0F);
            if (ent instanceof LivingEntity living)
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 5 * 20, 2));
        }

        if (self == com.hbm_m.block.ModBlocks.BARBED_WIRE_ACID.get()) {
            ent.hurt(level.damageSources().cactus(), 2.0F);
            if (ent instanceof Player player) {
                com.hbm_m.util.ArmorUtil.damageSuit(player, net.minecraft.world.entity.EquipmentSlot.HEAD, 1);
                com.hbm_m.util.ArmorUtil.damageSuit(player, net.minecraft.world.entity.EquipmentSlot.CHEST, 1);
                com.hbm_m.util.ArmorUtil.damageSuit(player, net.minecraft.world.entity.EquipmentSlot.LEGS, 1);
                com.hbm_m.util.ArmorUtil.damageSuit(player, net.minecraft.world.entity.EquipmentSlot.FEET, 1);
            }
        }

        if (self == com.hbm_m.block.ModBlocks.BARBED_WIRE_WITHER.get()) {
            ent.hurt(level.damageSources().cactus(), 2.0F);
            if (ent instanceof LivingEntity living)
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER, 5 * 20, 4));
        }

        if (self == com.hbm_m.block.ModBlocks.BARBED_WIRE_ULTRADEATH.get()) {
            ent.hurt(com.hbm_m.damagesource.ModDamageSources.create(level, com.hbm_m.damagesource.ModDamageTypes.PC), 5.0F);
            if (ent instanceof LivingEntity living)
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(EffectHooks.of(com.hbm_m.effect.ModEffects.RADIATION), 5 * 20, 9));
        }
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