package com.hbm_m.block.machines.albion;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.albion.PABeamlineBlockEntity;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-Port von {@code BlockPABeamline} (1.7.10): das blosse Strahlrohr.
 *
 * <p>Drei Felder lang entlang der Strahlachse - je eine Dummyzelle vor und hinter dem Kern. Das
 * Teilchen tritt an der vorderen ein und verlaesst das Rohr zwei Felder hinter dem Kern, also
 * genau dort, wo das naechste Bauteil seine Eingangszelle hat.</p>
 *
 * <p>Der Schraubendreher schaltet das Sichtfenster um.</p>
 */
public class PABeamlineBlock extends PAMultiblockBlock implements com.hbm_m.api.block.IToolable {

    public PABeamlineBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        // Original: BlockPABeamline - getDimensions {0,0,0,0,1,1}, getOffset 0, keine Zusatzzellen.
        return DummyableStructureBuilder.create()
                .box(0, 0, 0, 0, 1, 1)
                .placementOffset(0)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    /**
     * 1:1-Port von {@code onScrew} (IToolable, kein onBlockActivated): der Schraubendreher schaltet das
     * Sichtfenster um; der Server meldet wie im Original {@code false} (keine Abnutzung).
     */
    @Override
    public boolean onScrew(Level world, net.minecraft.world.entity.player.Player player, BlockPos pos, net.minecraft.core.Direction side,
            float fX, float fY, float fZ, net.minecraft.world.InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        if (world.isClientSide()) return true;

        if (world.getBlockEntity(pos) instanceof PABeamlineBlockEntity tile) {
            tile.setWindow(!tile.hasWindow()); // window = !window; markDirty
        }
        return false;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PABeamlineBlockEntity(pos, state);
    }

    /** Original updateEntity: Aufleuchten beim Durchflug (Client) und dessen Meldung (Server). */
    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return createTickerHelper(type, com.hbm_m.blockentity.ModBlockEntities.PA_BEAMLINE_BE.get(), PABeamlineBlockEntity::tick);
    }

    /** Rohr, Fenster und leuchtendes Glas zeichnet PABeamlineRenderer (RenderPABeamline). */
    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.ENTITYBLOCK_ANIMATED;
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<PABeamlineBlock> CODEC = simpleCodec(PABeamlineBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}

    /** Original {@code addInformation}: {@code addStandardInfo} (Umschalttaste zeigt {@code .desc}). */
    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, @org.jetbrains.annotations.Nullable net.minecraft.world.level.BlockGetter level,
                                java.util.List<net.minecraft.network.chat.Component> list, net.minecraft.world.item.TooltipFlag flag) {
        com.hbm_m.util.StandardInfo.add(list, getDescriptionId() + ".desc");
    }
}
