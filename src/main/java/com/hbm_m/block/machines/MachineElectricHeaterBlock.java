package com.hbm_m.block.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineElectricHeaterBlockEntity;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

/**
 * 1:1-Port von {@code HeaterElectric} (1.7.10): der elektrische Waermeerzeuger.
 *
 * <p>Er ist wie im Original <b>zwei Felder tief und drei breit</b>
 * ({@code getDimensions {0,0,1,2,1,1}}, Setzversatz 2) - die vordere Zelle ist zugleich der
 * Anschluss, an dem das Kabel andockt.</p>
 */
public class MachineElectricHeaterBlock extends DummyableMachineBlock implements com.hbm_m.api.block.IToolable, com.hbm_m.interfaces.ILookOverlay {

    /** w16b: Original {@code HeaterElectric.printHook} (Kern ueber findCore - Dummy-Zellen reichen an den Kern weiter). */
    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics g, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof MachineElectricHeaterBlockEntity heater)) return;
        java.util.List<Component> text = new java.util.ArrayList<>();
        text.add(Component.literal(String.format(java.util.Locale.US, "%,d", heater.getHeatStored()) + " TU"));
        text.add(Component.literal("-> ").withStyle(net.minecraft.ChatFormatting.GREEN)
                .append(Component.literal(heater.getConsumption() + " HE/t").withStyle(net.minecraft.ChatFormatting.RESET)));
        text.add(Component.literal("<- ").withStyle(net.minecraft.ChatFormatting.RED)
                .append(Component.literal(heater.getHeatGen() + " TU/t").withStyle(net.minecraft.ChatFormatting.RESET)));
        com.hbm_m.interfaces.ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    public MachineElectricHeaterBlock(Properties properties) { super(properties); }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        // Original: getDimensions {0,0,1,2,1,1}, getOffset 2, makeExtra(x, y, z) auf dem angeklickten Feld
        // (Kern = Klick + dir * -2, also Fassade = Kern plus zwei in Blickrichtung dir).
        return DummyableStructureBuilder.create()
                .box(0, 0, 1, 2, 1, 1)
                .extra(2, 0, 0)
                .placementOffset(2)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineElectricHeaterBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.ELECTRIC_HEATER_BE.get(),
                (lvl, pos, st, be) -> MachineElectricHeaterBlockEntity.tick(lvl, pos, st, (MachineElectricHeaterBlockEntity) be));
    }

    /** 1:1 {@code HeaterElectric.onScrew}: Schraubenzieher schaltet die Heizstufe um (kein onBlockActivated, kein GUI). */
    @Override
    public boolean onScrew(Level world, Player player, BlockPos pos, net.minecraft.core.Direction side, float fX, float fY, float fZ, InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        if (world.isClientSide()) return true;
        if (!(world.getBlockEntity(pos) instanceof MachineElectricHeaterBlockEntity tile)) return false;
        tile.cycleSetting(); // toggleSetting + markDirty
        return true;
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineElectricHeaterBlock> CODEC = simpleCodec(MachineElectricHeaterBlock::new);

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
