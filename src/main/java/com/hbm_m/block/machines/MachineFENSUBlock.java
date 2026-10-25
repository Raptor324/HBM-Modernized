package com.hbm_m.block.machines;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineFENSUBlockEntity;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.util.BobMathUtil;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code MachineFENSU} (LEGACY, ohne Kreativ-Tab): BlockDummyable {@code {4, 0, 1, 1, 2, 2}}, Offset 1 - fuenf
 * hoch, drei tief, fuenf breit, Kern unten in der Mitte. Strom nur ueber das Feld unter dem Kern (siehe
 * {@link MachineFENSUBlockEntity}). GUI = Batterie-GUI ({@code standardOpenBehavior}), Abbau behaelt die Ladung
 * ({@code IPersistentNBT.getDrops}), Blick-Anzeige mit Ladung und Prozent ({@code printHook}).
 */
public class MachineFENSUBlock extends DummyableMachineBlock implements ILookOverlay {

    public MachineFENSUBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(4, 0, 1, 1, 2, 2)
                .placementOffset(1)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineFENSUBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.MACHINE_FENSU_BE.get(), MachineFENSUBlockEntity::tickFensu);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return open(level, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return open(level, pos, player);
    }
    *///?}

    /** {@code standardOpenBehavior}. */
    private InteractionResult open(Level level, BlockPos pos, Player player) {
        if (player.isShiftKeyDown()) return InteractionResult.sidedSuccess(level.isClientSide()); // Original standardOpenBehavior: geschlichen true ohne GUI
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MachineFENSUBlockEntity fensu) {
            MenuRegistry.openExtendedMenu((ServerPlayer) player, fensu, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics g, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof MachineFENSUBlockEntity battery)) return;

        List<Component> text = new ArrayList<>();
        text.add(Component.literal(BobMathUtil.getShortNumber(battery.getEnergyStored()) + " / " + BobMathUtil.getShortNumber(battery.getMaxEnergyStored()) + "HE"));

        double percent = (double) battery.getEnergyStored() / (double) battery.getMaxEnergyStored();
        int charge = (int) Math.floor(percent * 10_000D);
        int color = ((int) (0xFF - 0xFF * percent)) << 16 | ((int) (0xFF * percent) << 8);

        text.add(Component.literal((charge / 100D) + "%").withStyle(s -> s.withColor(TextColor.fromRgb(color))));

        ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineFENSUBlock> CODEC = simpleCodec(MachineFENSUBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
