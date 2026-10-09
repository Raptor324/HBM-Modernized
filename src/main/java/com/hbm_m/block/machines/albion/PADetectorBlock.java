package com.hbm_m.block.machines.albion;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.albion.PADetectorBlockEntity;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-Port von {@code BlockPADetector} (1.7.10): der Detektor am Ende der Strecke.
 *
 * <p>Der groesste Brocken der Anlage - fuenf mal fuenf mal neun. Das Teilchen tritt vier Felder
 * vor dem Kern ein und kommt nicht wieder heraus: hier wird ausgewertet, was dabei herausgekommen
 * ist.</p>
 */
public class PADetectorBlock extends PAMultiblockBlock {

    public PADetectorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        // Original: BlockPADetector - getDimensions {2,2,2,2,4,4}, getOffset 0, fuenf Zusatzzellen.
        return DummyableStructureBuilder.create()
                .box(2, 2, 2, 2, 4, 4)
                .extra(0, 0, -4)
                .extra(0, 1, -4)
                .extra(0, -1, -4)
                .extra(1, 0, -4)
                .extra(-1, 0, -4)
                .placementOffset(0)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    protected boolean hasMenu() {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PADetectorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.PA_DETECTOR_BE.get(),
                (lvl, pos, st, be) -> PADetectorBlockEntity.tick(lvl, pos, st, (PADetectorBlockEntity) be));
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<PADetectorBlock> CODEC = simpleCodec(PADetectorBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}

    /** Original {@code addInformation}: {@code addStandardInfo} (Umschalttaste zeigt {@code .desc}). */
    @Override
    //? if < 1.21.1 {
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, @org.jetbrains.annotations.Nullable net.minecraft.world.level.BlockGetter level,
                                java.util.List<net.minecraft.network.chat.Component> list, net.minecraft.world.item.TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, java.util.List<net.minecraft.network.chat.Component> list, net.minecraft.world.item.TooltipFlag flag) {
    *///?}
        com.hbm_m.util.StandardInfo.add(list, getDescriptionId() + ".desc");
    }
}
