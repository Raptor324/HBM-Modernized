package com.hbm_m.block.machines;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineBoilerBlockEntity;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.trait.FT_Heatable;
import com.hbm_m.inventory.fluid.trait.FT_Heatable.HeatingType;
import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code MachineHeatBoiler}: {@code getDimensions {3,0,1,1,1,1}}, {@code getOffset 1}, Anschlusszellen links und
 * rechts ({@code rot}) sowie oben auf Hoehe 3. Fluidkennung stellt das Eingangsfluid um (nur, was im Boiler heizbar
 * ist), Anzeige beim Hinsehen, geplatzt nur noch 4 Stahlbarren und 8 Kupferplatten. Gezeichnet vom
 * {@code BoilerRenderer}.
 */
public class MachineBoilerBlock extends DummyableMachineBlock implements com.hbm_m.interfaces.ILookOverlay {

    /** Geplatzt - steuert Drops und das Item (Original {@code hasExploded} / Metadaten 1). */
    public static final BooleanProperty EXPLODED = BooleanProperty.create("exploded");

    public MachineBoilerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockState withDefaults(BlockState state) {
        return super.withDefaults(state).setValue(EXPLODED, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(EXPLODED);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(3, 0, 1, 1, 1, 1)
                .extra(0, 0, 1)
                .extra(0, 0, -1)
                .extra(0, 3, 0)
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
        return new MachineBoilerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.BOILER_BE.get(),
                (lvl, pos, st, be) -> MachineBoilerBlockEntity.tick(lvl, pos, st, (MachineBoilerBlockEntity) be));
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
                if (!(world.getBlockEntity(pos) instanceof MachineBoilerBlockEntity boiler)) return InteractionResult.PASS;

                Fluid type = FluidIdentifierItem.resolvePrimaryForTank(held);
                FT_Heatable trait = type == null ? null : FluidType.getTrait(type, FT_Heatable.class);

                if (trait != null && trait.getEfficiency(HeatingType.BOILER) > 0) {
                    boiler.tanks[0].setTankType(type);
                    boiler.setChanged();
                    player.displayClientMessage(Component.literal("Changed type to ").withStyle(ChatFormatting.YELLOW)
                            .append(FluidType.forFluid(type).getLocalizedName()).append(Component.literal("!")), false);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    /** Original {@code getDrops}: geplatzt nur noch Stahl und Kupfer. */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (state.getValue(EXPLODED)) {
            List<ItemStack> ret = new ArrayList<>();
            ret.add(new ItemStack(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT), 4));
            ret.add(new ItemStack(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE), 8));
            return ret;
        }
        return super.getDrops(state, params);
    }

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics g, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof MachineBoilerBlockEntity boiler)) return;
        if (boiler.hasExploded) return;

        List<Component> text = new ArrayList<>();
        text.add(Component.literal(String.format(Locale.US, "%,d", boiler.heat) + "TU"));
        text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN).append(Component.literal("").withStyle(ChatFormatting.WHITE) /* 1.7 §r = Grundfarbe */
                .append(FluidType.forFluid(boiler.tanks[0].getTankType()).getLocalizedName())
                .append(": " + String.format(Locale.US, "%,d", boiler.tanks[0].getFill()) + " / " + String.format(Locale.US, "%,d", boiler.tanks[0].getMaxFill()) + "mB")));
        text.add(Component.literal("<- ").withStyle(ChatFormatting.RED).append(Component.literal("").withStyle(ChatFormatting.WHITE) /* 1.7 §r = Grundfarbe */
                .append(FluidType.forFluid(boiler.tanks[1].getTankType()).getLocalizedName())
                .append(": " + String.format(Locale.US, "%,d", boiler.tanks[1].getFill()) + " / " + String.format(Locale.US, "%,d", boiler.tanks[1].getMaxFill()) + "mB")));

        com.hbm_m.interfaces.ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
        com.hbm_m.util.StandardInfo.add(list, getDescriptionId() + ".desc");
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineBoilerBlock> CODEC = simpleCodec(MachineBoilerBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
