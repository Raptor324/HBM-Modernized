package com.hbm_m.block.generic;

import java.util.function.Supplier;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-Port von {@code BlockOreBasalt}: fuenf Basalterze aus erstarrter Vulkanlava. Das Asbesterz
 * duenstet beim Darueberlaufen Asbestgas aus (1:10, mit Aura-Partikeln) und hinterlaesst beim
 * Abbau eine Gaswolke. Im Original ein Block mit Metadaten; der Port folgt seiner Konvention und
 * teilt in fuenf Bloecke ({@code ore_basalt_<typ>} = die {@code getUnlocalizedMultiName}-Namen).
 */
public class BlockOreBasalt extends Block {

    public enum Type {
        SULFUR(() -> ModItems.SULFUR.get()),
        FLUORITE(() -> ModItems.FLUORITE.get()),
        ASBESTOS(() -> ModMaterialItems.item(ModMaterials.ASBESTOS, MaterialShape.INGOT)),
        GEM(() -> ModItems.GEM_VOLCANIC.get()),
        MOLYSITE(() -> ModItems.MOLYSITE.get());

        public final Supplier<Item> drop;

        Type(Supplier<Item> drop) {
            this.drop = drop;
        }
    }

    public final Type type;

    public BlockOreBasalt(Properties properties, Type type) {
        super(properties);
        this.type = type;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (type == Type.ASBESTOS && level.getBlockState(pos.above()).isAir()) {
            if (!level.isClientSide && level.random.nextInt(10) == 0) {
                level.setBlockAndUpdate(pos.above(), ModBlocks.GAS_ASBESTOS.get().defaultBlockState());
            }
            for (int i = 0; i < 5; i++) {
                level.addParticle(ParticleTypes.MYCELIUM, pos.getX() + level.random.nextFloat(), pos.getY() + 1.1, pos.getZ() + level.random.nextFloat(), 0.0D, 0.0D, 0.0D);
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void destroy(net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockState state) {
        super.destroy(level, pos, state);
        // Original dropBlockAsItemWithChance: Asbesterz hinterlaesst eine Gaswolke.
        if (type == Type.ASBESTOS && !level.isClientSide()) {
            level.setBlock(pos, ModBlocks.GAS_ASBESTOS.get().defaultBlockState(), 3);
        }
    }
}
