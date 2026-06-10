package com.hbm_m.block.bomb;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.effect.ModEffects;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import com.hbm_m.entity.mob.EntityCreeperTainted;

import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Блок порчи — распространяется по соседним блокам, замедляет сущности и накладывает эффект taint.
 * Порт {@link com.hbm.blocks.bomb.BlockTaint} (1.7.10).
 */
public class BlockTaint extends Block {

    public static final IntegerProperty AGE = BlockStateProperties.AGE_15;
    private static final VoxelShape COLLISION_SHAPE = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.75, 1.0);

    public BlockTaint(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION_SHAPE;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        int meta = state.getValue(AGE);
        if (meta >= 15) {
            return;
        }

        for (int i = -3; i <= 3; i++) {
            for (int j = -3; j <= 3; j++) {
                for (int k = -3; k <= 3; k++) {
                    if (Math.abs(i) + Math.abs(j) + Math.abs(k) > 4) {
                        continue;
                    }
                    if (rand.nextFloat() > 0.25F) {
                        continue;
                    }

                    BlockPos targetPos = pos.offset(i, j, k);
                    BlockState targetState = level.getBlockState(targetPos);
                    if (isImmuneToTaint(targetState)) {
                        continue;
                    }

                    int targetMeta = meta + 1;
                    boolean hasAir = false;
                    for (Direction dir : Direction.values()) {
                        if (level.isEmptyBlock(targetPos.relative(dir))) {
                            hasAir = true;
                            break;
                        }
                    }
                    if (!hasAir) {
                        targetMeta = meta + 3;
                    }
                    if (targetMeta > 15) {
                        continue;
                    }

                    BlockState existing = level.getBlockState(targetPos);
                    if (existing.is(this) && existing.getValue(AGE) >= targetMeta) {
                        continue;
                    }

                    BlockState newState = defaultBlockState().setValue(AGE, targetMeta);
                    level.setBlock(targetPos, newState, 3);

                    if (rand.nextFloat() < 0.25F && FallingBlock.isFree(level.getBlockState(targetPos.below()))) {
                        FallingBlockEntity.fall(level, targetPos, newState);
                    }
                }
            }
        }
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        int meta = state.getValue(AGE);
        int effectLevel = 15 - meta;

        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x * 0.6, motion.y, motion.z * 0.6);

        if (entity instanceof LivingEntity living) {
            if (level.random.nextInt(50) == 0) {
                living.addEffect(new MobEffectInstance(ModEffects.TAINT.get(), 15 * 20, effectLevel));
            }
        }

        if (!level.isClientSide
                && entity.isAlive()
                && !entity.isRemoved()
                && entity.getClass() == Creeper.class) {
            EntityCreeperTainted.convertFromCreeper((Creeper) entity);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("DO NOT TOUCH, BREATHE OR STARE AT.").withStyle(ChatFormatting.GRAY));
    }

    /** Устанавливает блок порчи с заданным «возрастом» (бывш. metadata). */
    public static BlockState stateWithAge(int age) {
        return ModBlocks.TAINT.get().defaultBlockState().setValue(AGE, Math.min(15, Math.max(0, age)));
    }

    /**
     * Блоки, которые нельзя заменять порчей (технические, порталы, барьер и т.п.).
     * Опирается на ванильные теги {@link BlockTags#WITHER_IMMUNE} / {@link BlockTags#PORTALS}.
     */
    public static boolean isImmuneToTaint(BlockState state) {
        if (state.isAir()) {
            return true;
        }
        return state.is(BlockTags.WITHER_IMMUNE)
                || state.is(BlockTags.PORTALS)
                || state.is(Blocks.STRUCTURE_VOID);
    }

    /** Можно ли заменить этот блок порчей (взрыв крипера, ракета, след эффекта). */
    public static boolean canBeReplacedByTaint(Level level, BlockPos pos, BlockState state) {
        if (isImmuneToTaint(state)) {
            return false;
        }
        return state.isSolidRender(level, pos);
    }
}
