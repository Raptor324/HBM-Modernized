package com.hbm_m.block.machines;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineCrackingTowerBlockEntity;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code MachineCatalyticCracker}: fuenf Quader aus {@code getAllDimensions} (Sockel, Turm, Kolonne, Kopf, Seitenkasten),
 * {@code getOffset 3}, acht Anschlusszellen am Sockel. Rechtsklick mit Fluidkennung setzt das Eingangsfluid; kein GUI,
 * dafuer das Blick-Overlay mit allen fuenf Tanks. Gezeichnet vom {@code CatalyticCrackerRenderer}.
 */
public class MachineCrackingTowerBlock extends DummyableMachineBlock implements com.hbm_m.interfaces.ILookOverlay {

    public MachineCrackingTowerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(0, 0, 3, 3, 2, 3)
                .box(8, -1, 3, -1, 2, 0)
                .box(13, 0, 0, 3, 2, 1)
                .box(14, -13, -1, 2, 1, 0)
                .box(3, -1, 2, 3, -1, 3)
                .extra(3, 0, 1)
                .extra(3, 0, -2)
                .extra(-3, 0, 1)
                .extra(-3, 0, -2)
                .extra(2, 0, 2)
                .extra(2, 0, -3)
                .extra(-2, 0, 2)
                .extra(-2, 0, -3)
                .placementOffset(3)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    /**
     * Порт {@code MachineCatalyticCracker.printHook}: список баков, первые два (входные — нефть,
     * пар) зелёным {@code "-> "}, остальные (выходные) красным {@code "<- "}; формат
     * {@code <имя>: <заполнено>/<объём>mB}. В порту баков пять (добавлен бак отработанного пара) —
     * правило «входы/выходы» сохраняется.
     */
    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics guiGraphics, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MachineCrackingTowerBlockEntity cracker)) return;

        java.util.List<net.minecraft.network.chat.Component> text = new java.util.ArrayList<>();
        com.hbm_m.inventory.fluid.tank.FluidTank[] tanks = cracker.getTanks();
        for (int i = 0; i < tanks.length; i++) {
            com.hbm_m.inventory.fluid.tank.FluidTank tank = tanks[i];
            text.add(net.minecraft.network.chat.Component.literal(i < 2 ? "-> " : "<- ")
                    .withStyle(i < 2 ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED)
                    .append(net.minecraft.network.chat.Component.literal("")
                            .append(com.hbm_m.inventory.fluid.FluidType.forFluid(tank.getTankType()).getLocalizedName())
                            .withStyle(net.minecraft.ChatFormatting.WHITE))
                    .append(net.minecraft.network.chat.Component.literal(
                            ": " + tank.getFill() + "/" + tank.getMaxFill() + "mB")
                            .withStyle(net.minecraft.ChatFormatting.WHITE)));
        }
        com.hbm_m.interfaces.ILookOverlay.printGeneric(guiGraphics,
                net.minecraft.network.chat.Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineCrackingTowerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.CRACKING_TOWER_BE.get(),
                (lvl, pos, st, be) -> MachineCrackingTowerBlockEntity.tick(lvl, pos, st, (MachineCrackingTowerBlockEntity) be));
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return activate(level, pos, player, player.getItemInHand(hand));
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return activate(level, pos, player, player.getMainHandItem());
    }
    *///?}

    private InteractionResult activate(Level world, BlockPos pos, Player player, ItemStack held) {
        if (!world.isClientSide && !player.isShiftKeyDown()) {
            if (!held.isEmpty() && held.getItem() instanceof FluidIdentifierItem) {
                if (!(world.getBlockEntity(pos) instanceof MachineCrackingTowerBlockEntity cracker)) return InteractionResult.PASS;

                Fluid type = FluidIdentifierItem.resolvePrimaryForTank(held);
                if (type == null) return InteractionResult.PASS;
                cracker.tanks[0].setTankType(type);
                cracker.setChanged();
                player.displayClientMessage(Component.literal("Changed type to ").withStyle(ChatFormatting.YELLOW)
                        .append(FluidType.forFluid(type).getLocalizedName()).append(Component.literal("!")), false);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineCrackingTowerBlock> CODEC = simpleCodec(MachineCrackingTowerBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
