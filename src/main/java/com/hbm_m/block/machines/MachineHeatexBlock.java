package com.hbm_m.block.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineHeatexBlockEntity;

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
import org.jetbrains.annotations.Nullable;
import dev.architectury.registry.menu.MenuRegistry;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

/**
 * 1:1-Port von {@code HeaterHeatex} (1.7.10): der Waermetauscher.
 *
 * <p>Ein Feld hoch, drei mal drei breit ({@code getDimensions {0,0,1,1,1,1}}) - und die vier
 * <b>Ecken</b> sind die Anschlusszellen. Das ist der Grund, warum man die Rohre bei diesem Geraet
 * diagonal ansetzt und nicht gerade.</p>
 */
public class MachineHeatexBlock extends DummyableMachineBlock {

    public MachineHeatexBlock(Properties properties) { super(properties); }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        // Original: getDimensions {0,0,1,1,1,1}, getOffset 1, vier diagonale Zusatzzellen.
        return DummyableStructureBuilder.create()
                .box(0, 0, 1, 1, 1, 1)
                .extra(1, 0, 1)
                .extra(1, 0, -1)
                .extra(-1, 0, 1)
                .extra(-1, 0, -1)
                .placementOffset(1)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineHeatexBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.HEATEX_BE.get(),
                (lvl, pos, st, be) -> MachineHeatexBlockEntity.tick(lvl, pos, st, (MachineHeatexBlockEntity) be));
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            // Original HeaterHeatex: geschlichen stellt der Identifikator tanks[0], keine GUI
            if (player.isShiftKeyDown()) {
                net.minecraft.world.item.ItemStack held = player.getItemInHand(hand);
                if (entity instanceof MachineHeatexBlockEntity heatex && !held.isEmpty()
                        && held.getItem() instanceof com.hbm_m.interfaces.IItemFluidIdentifier id) {
                    net.minecraft.world.level.material.Fluid type = id.getType(level, pos, held);
                    heatex.tanks[0].setTankType(type);
                    heatex.setChanged();
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("Changed type to ").withStyle(net.minecraft.ChatFormatting.YELLOW)
                            .append(com.hbm_m.inventory.fluid.FluidType.forFluid(type).getLocalizedName())
                            .append(net.minecraft.network.chat.Component.literal("!")), false);
                }
                return InteractionResult.SUCCESS;
            }
            if (entity instanceof MenuProvider menuProvider) {
                MenuRegistry.openExtendedMenu((ServerPlayer) player, menuProvider, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
        }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            // Original HeaterHeatex: geschlichen stellt der Identifikator tanks[0], keine GUI
            if (player.isShiftKeyDown()) {
                net.minecraft.world.item.ItemStack held = player.getMainHandItem();
                if (entity instanceof MachineHeatexBlockEntity heatex && !held.isEmpty()
                        && held.getItem() instanceof com.hbm_m.interfaces.IItemFluidIdentifier id) {
                    net.minecraft.world.level.material.Fluid type = id.getType(level, pos, held);
                    heatex.tanks[0].setTankType(type);
                    heatex.setChanged();
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("Changed type to ").withStyle(net.minecraft.ChatFormatting.YELLOW)
                            .append(com.hbm_m.inventory.fluid.FluidType.forFluid(type).getLocalizedName())
                            .append(net.minecraft.network.chat.Component.literal("!")), false);
                }
                return InteractionResult.SUCCESS;
            }
            if (entity instanceof MenuProvider menuProvider) {
                MenuRegistry.openExtendedMenu((ServerPlayer) player, menuProvider, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
        }
    *///?}


    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineHeatexBlock> CODEC = simpleCodec(MachineHeatexBlock::new);

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
