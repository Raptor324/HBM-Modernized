package com.hbm_m.block.gas;

import com.hbm_m.item.gasmask.IGasMask;
import com.hbm_m.item.gasmask.GasMaskUtil;
import com.hbm_m.platform.BlockProps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
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
                .noCollission()
                .noOcclusion()
                .replaceable()
                .instabreak()
                .randomTicks()
                .strength(-1.0F, 6000000.0F)
                .noLootTable()
                .mapColor(net.minecraft.world.level.material.MapColor.NONE);
    }

    @Override
    @Deprecated
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

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

    @Override
    public void randomTick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, RandomSource random) {
        // 20% за тик — газ рассеивается (как в оригинале).
        if (random.nextInt(5) == 0) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        level.addParticle(ParticleTypes.SMOKE,
                pos.getX() + random.nextDouble(),
                pos.getY() + random.nextDouble(),
                pos.getZ() + random.nextDouble(),
                0.0D, 0.0D, 0.0D);
    }

    /** Воздействие на сущность внутри газа; защищённый фильтр — износ фильтра. */
    protected abstract void affect(LivingEntity living);

    /** Общий износ фильтра при контакте с газом (1 ед./тик, как в оригинале). */
    protected static void damageWornFilter(LivingEntity living) {
        ItemStack mask = GasMaskUtil.resolveMask(living.getItemBySlot(EquipmentSlot.HEAD));
        if (!mask.isEmpty() && mask.getItem() instanceof IGasMask) {
            IGasMask.damageFilter(mask, 1);
        }
    }
}
