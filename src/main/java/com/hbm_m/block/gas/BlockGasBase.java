package com.hbm_m.block.gas;

import com.hbm_m.item.gasmask.IGasMask;
import com.hbm_m.item.gasmask.GasMaskUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Невидимый газ, заполняющий воздух и травящий сущностей.
 * Порт {@link com.hbm.blocks.gas.BlockGas} (1.7.10).
 */
public abstract class BlockGasBase extends Block {

    protected BlockGasBase() {
        super(gasProps());
    }

    protected static Block.Properties gasProps() {
        return Block.Properties.of()
                .mapColor(MapColor.NONE)
                .noCollission()
                .noOcclusion()
                .replaceable()
                .pushReaction(PushReaction.DESTROY)
                // -1.0F destroyTime = невозможно сломать/добыть в выживании (как бедрок/барьер).
                // 0.0F blastResistance = взрыв развеивает газ без дропа.
                .strength(-1.0F, 0.0F)
                .noLootTable()
                .randomTicks();
    }

    /* =================== РЕНДЕР И ФОРМА =================== */

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Газ полностью прозрачен в мире
        return RenderShape.INVISIBLE;
    }

    @Override
    @Deprecated
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    @Deprecated
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Если игрок рядом держит в руке этот (или любой) газ — визуализируем маркер (как у барьера)
        Player player = level.getNearestPlayer(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 24.0D, EntitySelector.NO_SPECTATORS);
        if (player != null && isHoldingGas(player, this)) {
            level.addParticle(
                    new BlockParticleOption(ParticleTypes.BLOCK_MARKER, state),
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    0.0D, 0.0D, 0.0D
            );
        }

        spawnAmbientParticles(state, level, pos, random);
    }

    protected void spawnAmbientParticles(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // По умолчанию без частиц, подклассы переопределяют
    }

    protected static boolean isHoldingGas(Player player, Block gasBlock) {
        if (player.isHolding(gasBlock.asItem())) {
            return true;
        }
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return (main.getItem() instanceof BlockItem bi1 && bi1.getBlock() instanceof BlockGasBase)
                || (off.getItem() instanceof BlockItem bi2 && bi2.getBlock() instanceof BlockGasBase);
    }

    /* =================== ФИЗИКА И ДВИЖЕНИЕ ГАЗОВ =================== */

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, 10);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide()) {
            if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
                level.scheduleTick(pos, this, 10);
            }
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Проверяем возможность перемещения в первом направлении
        Direction first = getFirstDirection(level, pos, random);
        if (!tryMove(level, pos, state, first)) {
            // Если первое заблокировано — пробуем второе
            Direction second = getSecondDirection(level, pos, random);
            if (!tryMove(level, pos, state, second)) {
                // Если никуда не сместились — планируем следующий тик
                level.scheduleTick(pos, this, getDelay(level, random));
            }
        }
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Страховка от зависания при выгрузке чанков
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, getDelay(level, random));
        }
    }

    protected boolean tryMove(ServerLevel level, BlockPos pos, BlockState state, Direction dir) {
        if (dir == null) return false;
        BlockPos target = pos.relative(dir);
        if (level.getBlockState(target).isAir()) {
            level.removeBlock(pos, false);
            level.setBlock(target, state, 3);
            level.scheduleTick(target, this, getDelay(level, level.random));
            return true;
        }
        return false;
    }

    public abstract Direction getFirstDirection(Level level, BlockPos pos, RandomSource random);

    public Direction getSecondDirection(Level level, BlockPos pos, RandomSource random) {
        return getFirstDirection(level, pos, random);
    }

    public Direction randomHorizontal(RandomSource random) {
        return Direction.Plane.HORIZONTAL.getRandomDirection(random);
    }

    public int getDelay(Level level, RandomSource random) {
        return 2;
    }

    /* =================== ВЗАИМОДЕЙСТВИЕ С СУЩНОСТЯМИ =================== */

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide() || !(entity instanceof LivingEntity living)) {
            return;
        }
        if (living instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return;
        }
        affect(living);
    }

    protected abstract void affect(LivingEntity living);

    protected static void damageWornFilter(LivingEntity living) {
        ItemStack mask = GasMaskUtil.resolveWornMask(living);
        if (!mask.isEmpty() && mask.getItem() instanceof IGasMask) {
            IGasMask.damageFilter(mask, 1);
        }
    }
}