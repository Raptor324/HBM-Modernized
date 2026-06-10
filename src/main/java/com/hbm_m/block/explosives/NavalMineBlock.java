package com.hbm_m.block.explosives;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.sound.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public class NavalMineBlock extends Block {

    private static final float EXPLOSION_RADIUS = 6.0F;

    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);

    public NavalMineBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                 @Nullable BlockGetter level,
                                 List<Component> tooltip,
                                 TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hbm_m.naval_mine.line1")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof Player player && !player.isCreative()) {
            explodeMine(level, pos);
        }
        super.entityInside(state, level, pos, entity);
    }

    private void explodeMine(Level level, BlockPos pos) {
        level.removeBlock(pos, false);

        ModSounds.EXPLOSION_LARGE_NEAR.ifPresent(sound -> level.playSound(
                null,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                sound,
                SoundSource.BLOCKS,
                4.0F,
                1.0F
        ));

        level.explode(
                null,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                EXPLOSION_RADIUS,
                Level.ExplosionInteraction.TNT
        );
    }
}
