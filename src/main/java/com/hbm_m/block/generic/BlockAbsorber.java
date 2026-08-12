package com.hbm_m.block.generic;

import com.hbm_m.radiation.ChunkRadiationManager;

import com.hbm_m.item.BlockAbsorberItem;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/**
 * Поглотитель радиации с четырьмя уровнями (порт {@link com.hbm.blocks.generic.BlockAbsorber} 1.7.10).
 */
public class BlockAbsorber extends Block {

    public static final EnumProperty<EnumAbsorberTier> TIER =
            EnumProperty.create("tier", EnumAbsorberTier.class);

    public BlockAbsorber(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(TIER, EnumAbsorberTier.BASE));
    }

    /** Уровни в порядке meta 0–3: Base, Red, Green, Pink. */
    public enum EnumAbsorberTier implements StringRepresentable {
        BASE(2.5F, "absorber"),
        RED(10F, "absorber_red"),
        GREEN(100F, "absorber_green"),
        PINK(10000F, "absorber_pink");

        public final float absorbAmount;
        public final String textureName;

        EnumAbsorberTier(float absorb, String texture) {
            this.absorbAmount = absorb;
            this.textureName = texture;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase();
        }
    }

    public EnumAbsorberTier getTier(BlockState state) {
        return state.getValue(TIER);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TIER);
    }

    //? if < 1.21.1 {
    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return BlockAbsorberItem.forTier(this, state.getValue(TIER));
    }
    //?} else {
    /*@Override
    public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {
        return BlockAbsorberItem.forTier(this, state.getValue(TIER));
    }
    *///?}

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(BlockAbsorberItem.forTier(this, state.getValue(TIER)));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 10);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        EnumAbsorberTier tier = getTier(state);
        ChunkRadiationManager.getProxy().decrementRad(
                level, pos.getX(), pos.getY(), pos.getZ(), tier.absorbAmount);
        level.scheduleTick(pos, this, 10);
    }
}
