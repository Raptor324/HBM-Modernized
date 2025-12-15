package com.hbm_m.block.custom.explosives;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import com.hbm_m.block.entity.custom.explosives.MineBlockEntity;
import com.hbm_m.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

public class MineBlock extends Block implements EntityBlock {

    private static final VoxelShape COLLISION_SHAPE = Shapes.box(0.25, 0.0, 0.25, 0.75, 0.25, 0.75);
    private static final double DETECTION_RADIUS = 10.0;
    private static final float EXPLOSION_POWER = 2.5F;
    public static final DirectionProperty FACING = DirectionProperty.create("facing");

    public MineBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
    }
    @Override
    public void appendHoverText(ItemStack stack,
                                @Nullable BlockGetter level,
                                List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hbm_m.mine.line1")
                .withStyle(ChatFormatting.GRAY));

    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, te) -> {
            if (te instanceof MineBlockEntity mine) {
                MineBlockEntity.tick(lvl, pos, st, mine);
            }
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return COLLISION_SHAPE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return COLLISION_SHAPE;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide) {
            if (!(entity instanceof Player player && player.isCreative()) && entity instanceof LivingEntity) {
                // Взорвать мину
                explodeMine(level, pos);
                level.removeBlock(pos, false);
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private void explodeMine(Level level, BlockPos pos) {
        playDetonationSound(level, pos);

        // Взрыв с нанесением урона и с разрушением блоков
        level.explode(
                null,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                EXPLOSION_POWER,
                true,
                Level.ExplosionInteraction.NONE
        );

        level.removeBlock(pos, false);
    }

    private static final Random RANDOM = new Random();

    private void playDetonationSound(Level level, BlockPos pos) {
        SoundEvent[] sounds = new SoundEvent[]{
                ModSounds.EXPLOSION_SMALL_NEAR1.orElse(null),
                ModSounds.EXPLOSION_SMALL_NEAR2.orElse(null),
                ModSounds.EXPLOSION_SMALL_NEAR3.orElse(null)
        };

        List<SoundEvent> availableSounds = Arrays.stream(sounds)
                .filter(Objects::nonNull)
                .toList();

        if (!availableSounds.isEmpty()) {
            SoundEvent soundToPlay = availableSounds.get(RANDOM.nextInt(availableSounds.size()));
            level.playSound(
                    null,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    soundToPlay,
                    net.minecraft.sounds.SoundSource.BLOCKS,
                    1.0F,
                    1.0F
            );
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MineBlockEntity(pos, state);
    }
}
