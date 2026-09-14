package com.hbm_m.block.machines.icf;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.icf.ICFPhantomBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code BlockICF} (1.7.10): der Platzhalter, aus dem der zusammengebaute Laser
 * besteht.
 *
 * <p>Er ersetzt beim Zusammenbau jedes Laserbauteil und merkt sich in seiner
 * {@link ICFPhantomBlockEntity BlockEntity}, welches es war. Zerschlaegt man ihn, kommt das
 * Bauteil zurueck und der ganze Laser gilt als zerlegt.</p>
 *
 * <p>{@link #PORT} unterscheidet die Anschlussstellen von der uebrigen Huelle - sie tragen die
 * eigene Textur und sind die Stellen, an denen Energie hineingeht.</p>
 *
 * <p>Die Aussenhaut setzt sich ueber Blockgrenzen hinweg zusammen und verbindet sich dabei auch
 * mit dem Steuerblock - eine fertige Huelle sieht damit aus wie ein Stueck, nicht wie ein Haufen
 * Wuerfel.</p>
 */
public class ICFPhantomBlock extends BaseEntityBlock {

    public static final BooleanProperty PORT = BooleanProperty.create("port");

    public ICFPhantomBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(PORT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PORT);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ICFPhantomBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.ICF_PHANTOM_BE.get(),
                (lvl, pos, st, be) -> ICFPhantomBlockEntity.tick(lvl, pos, st, (ICFPhantomBlockEntity) be));
    }

    /** Original: {@code getItemDropped} gibt null - der Platzhalter selbst faellt nie. */
    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    /**
     * 1:1-Port von {@code breakBlock}: das gemerkte Bauteil kommt zurueck und der Laser gilt als
     * zerlegt. Alle uebrigen Platzhalter merken das beim naechsten Durchlauf ihrer eigenen Pruefung
     * und loesen sich ebenfalls auf.
     *
     * <p>Das Zuruecksetzen laeuft ueber einen nachgereichten Auftrag an den Server: mitten im
     * Entfernen darf an derselben Stelle kein neuer Block gesetzt werden.</p>
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock() && !level.isClientSide()
                && level.getBlockEntity(pos) instanceof ICFPhantomBlockEntity phantom
                && phantom.getPart() != null) {

            ICFLaserPart restored = phantom.getPart();
            BlockPos corePos = phantom.getCorePos();

            if (corePos != null && level.getBlockEntity(corePos)
                    instanceof com.hbm_m.blockentity.machines.icf.ICFControllerBlockEntity controller) {
                controller.setAssembled(false);
            }

            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                BlockPos target = pos.immutable();
                serverLevel.getServer().submit(() -> {
                    if (serverLevel.getBlockState(target).isAir()) {
                        serverLevel.setBlock(target,
                                com.hbm_m.block.ModBlocks.icfLaserPart(restored).defaultBlockState(), 3);
                    }
                });
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
