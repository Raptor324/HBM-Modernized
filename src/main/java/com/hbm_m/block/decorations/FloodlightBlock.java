package com.hbm_m.block.decorations;

import javax.annotation.Nullable;

import com.hbm_m.api.block.IToolable;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.decorations.FloodlightBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * 1:1 {@code com.hbm.blocks.machine.Floodlight}: Metadaten 0-5 = angeklickte Seite, +6 bei Boden/Decke und Blick
 * Nord/Sued ({@link #META}). Der Neigungswinkel kommt aus dem Blickwinkel des Spielers (in 5-Grad-Schritten), der
 * Schraubenzieher stellt ihn neu ein.
 */
public class FloodlightBlock extends BaseEntityBlock implements IToolable {

    public static final IntegerProperty META = IntegerProperty.create("meta", 0, 11);

    public FloodlightBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(META, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(META);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FloodlightBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.FLOODLIGHT.get(), FloodlightBlockEntity::serverTick);
    }

    //only method that respects sides, called first for orientation
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(META, ctx.getClickedFace().get3DDataValue());
    }

    //only method with player param, called second for variable rotation
    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity player, ItemStack stack) {
        if (player != null) setAngle(world, pos, player, true);
    }

    @Override
    public boolean onScrew(Level world, Player player, BlockPos pos, Direction side, float fX, float fY, float fZ, InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        setAngle(world, pos, player, false);
        return true;
    }

    public void setAngle(Level world, BlockPos pos, LivingEntity player, boolean updateMeta) {
        int i = Mth.floor(player.getYRot() * 4.0F / 360.0F + 0.5D) & 3;
        float rotation = player.getXRot();

        if (world.getBlockEntity(pos) instanceof FloodlightBlockEntity floodlight) {
            BlockState state = world.getBlockState(pos);
            int meta = state.getValue(META) % 6;

            if (meta == 0 || meta == 1) {
                if (i == 0 || i == 2) if (updateMeta) world.setBlock(pos, state.setValue(META, meta + 6), 3);
                if (meta == 1) if (i == 0 || i == 1) rotation = 180F - rotation;
                if (meta == 0) if (i == 0 || i == 3) rotation = 180F - rotation;
            }

            floodlight.rotation = -Math.round(rotation / 5F) * 5F;
            if (floodlight.isOn) floodlight.destroyLights();
            floodlight.setChanged();
            world.sendBlockUpdated(pos, state, world.getBlockState(pos), 3);
        }
    }
}
