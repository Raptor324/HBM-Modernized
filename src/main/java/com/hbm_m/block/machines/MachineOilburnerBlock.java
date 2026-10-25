package com.hbm_m.block.machines;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineOilburnerBlockEntity;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.trait.FT_Flammable;
import com.hbm_m.item.ModItems;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1-Port von {@code HeaterOilburner} (1.7.10), auch fuer {@code oilburner_hp}: der Oelbrenner.
 *
 * <p>Zwei Felder hoch und drei mal drei breit ({@code getDimensions {1,0,1,1,1,1}}). Angeschlossen
 * wird an den vier Seiten und oben - fuenf Zellen, an denen Rohre andocken koennen.</p>
 */
public class MachineOilburnerBlock extends DummyableMachineBlock implements com.hbm_m.interfaces.ILookOverlay {

    public MachineOilburnerBlock(Properties properties) { super(properties); }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        // Original: getDimensions {1,0,1,1,1,1}, getOffset 1, fuenf Zusatzzellen (vier Seiten + oben).
        return DummyableStructureBuilder.create()
                .box(1, 0, 1, 1, 1, 1)
                .extra(1, 0, 0)
                .extra(-1, 0, 0)
                .extra(0, 0, 1)
                .extra(0, 0, -1)
                .extra(0, 1, 0)
                .placementOffset(1)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineOilburnerBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.OILBURNER_BE.get(),
                (lvl, pos, st, be) -> MachineOilburnerBlockEntity.tick(lvl, pos, st, (MachineOilburnerBlockEntity) be));
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.sidedSuccess(level.isClientSide()); // Original standardOpenBehavior: geschlichen true ohne GUI
        return hbmOnUse(state, level, pos, player, hand);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.sidedSuccess(level.isClientSide()); // Original standardOpenBehavior: geschlichen true ohne GUI
        return hbmOnUse(state, level, pos, player, InteractionHand.MAIN_HAND);
    }
    *///?}

    private InteractionResult hbmOnUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() == ModItems.SCREWDRIVER.get()) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MachineOilburnerBlockEntity be) {
                be.cycleSetting();
                player.displayClientMessage(Component.literal("Setting: " + be.getSetting()), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof MenuProvider menuProvider && player instanceof ServerPlayer serverPlayer) {
                MenuRegistry.openExtendedMenu(serverPlayer, menuProvider, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** Port HeaterOilburner.printHook: Brennstoff und Abwaerme in TU/t. */
    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics guiGraphics, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MachineOilburnerBlockEntity heater)) return;

        List<Component> text = new ArrayList<>();
        text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(heater.getSetting() + " mB/t")));

        var tankType = heater.getOilTank().getTankType();
        FT_Flammable trait = FluidType.getTrait(tankType, FT_Flammable.class);
        if (trait != null) {
            // Original: heatEnergy * setting / 1000 (Multiplikation VOR der Division).
            long heat = trait.getHeatEnergy() * (long) heater.getSetting() / 1000L;
            text.add(Component.literal("<- ").withStyle(ChatFormatting.RED)
                    .append(Component.literal(String.format(java.util.Locale.US, "%,d", heat) + " TU/t")));
        }
        com.hbm_m.interfaces.ILookOverlay.printGeneric(guiGraphics,
                Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    /** Original {@code addInformation}: {@code addStandardInfo} (Umschalttaste zeigt {@code .desc}). */
    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable net.minecraft.world.level.BlockGetter level,
                                List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx,
                                List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
    *///?}
        com.hbm_m.util.StandardInfo.add(tooltip, getDescriptionId() + ".desc");
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineOilburnerBlock> CODEC = simpleCodec(MachineOilburnerBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
