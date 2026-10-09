package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.MachineDroneCrateBlockEntity;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Drone Crate - Port von {@code DroneCrate} (1.7.10 Original). Passiver Wuerfel, keine Ausrichtung. */
public class MachineDroneCrateBlock extends BaseEntityBlock {

    public MachineDroneCrateBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }


    /** audit10: 1:1 {@code DroneCrate.breakBlock} - Inhalt faellt beim Entfernen heraus. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof com.hbm_m.blockentity.network.MachineDroneCrateBlockEntity be) {
            com.hbm_m.block.BlockDropUtil.dropSlots(level, pos, be.getInventory(), 0, Integer.MAX_VALUE);
        }
        super.onRemove(state, level, pos, newState, moving);
    }
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineDroneCrateBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.DRONE_CRATE_BE.get(),
                (lvl, pos, st, be) -> MachineDroneCrateBlockEntity.tick(lvl, pos, st, (MachineDroneCrateBlockEntity) be));
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() && player.isShiftKeyDown()) return InteractionResult.PASS; // Original: geschlichen auf dem Server false
        // Original: mit dem Drohnenverknuepfer in der Hand reagiert der Block nicht, der Linker uebernimmt
        if (player.getItemInHand(hand).getItem() instanceof com.hbm_m.item.tools_and_armor.ItemDroneLinker) return InteractionResult.PASS;

        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof MenuProvider menuProvider) {
                MenuRegistry.openExtendedMenu((ServerPlayer) player, menuProvider, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
        }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player.isShiftKeyDown()) return InteractionResult.PASS; // Original: geschlichen auf dem Server false

        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof MenuProvider menuProvider) {
                MenuRegistry.openExtendedMenu((ServerPlayer) player, menuProvider, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
        }
    *///?}


    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineDroneCrateBlock> CODEC = simpleCodec(MachineDroneCrateBlock::new);

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
