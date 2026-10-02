package com.hbm_m.block.network;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.api.block.IToolable;
import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.PaintableDuctBlockEntity;
import com.hbm_m.interfaces.IItemFluidIdentifier;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.inventory.fluid.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code FluidDuctPaintable}: Vollblock-Rohr, mit einem Block in der Hand ueberstreichbar, Schraubenzieher
 * entfernt den Anstrich, Entschaerfer blendet die Rohr-Markierung auf dem Anstrich aus/ein ({@code HIDDEN} = Meta 1).
 */
public class PaintableDuctBlock extends BaseEntityBlock implements IToolable, ILookOverlay {

    public static final BooleanProperty HIDDEN = BooleanProperty.create("hidden");

    private final boolean exhaust;

    public PaintableDuctBlock(Properties p, boolean exhaust) {
        super(p);
        this.exhaust = exhaust;
        registerDefaultState(stateDefinition.any().setValue(HIDDEN, false));
    }

    public PaintableDuctBlock(Properties p) { this(p, false); }

    /** {@code FluidDuctPaintableBlockExhaust}: fuehrt die drei Rauchsorten, kein Identifikator. */
    public boolean isExhaust() { return exhaust; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(HIDDEN); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PaintableDuctBlockEntity(pos, state); }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.FLUID_DUCT_PAINTABLE.get(), PaintableDuctBlockEntity::serverTick);
    }

    /** {@code BlockCablePaintable.allowedPaint}: undurchsichtige Vollbloecke ausser Gras und sich selbst. */
    public static boolean allowedPaint(Level level, BlockPos pos, Block paint, Block self) {
        if (paint == Blocks.GRASS_BLOCK || paint == self) return false;
        return paint.defaultBlockState().isSolidRender(level, pos);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return interact(level, pos, player, player.getItemInHand(hand));
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return interact(level, pos, player, player.getMainHandItem());
    }
    public static final com.mojang.serialization.MapCodec<PaintableDuctBlock> CODEC = simpleCodec(PaintableDuctBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    *///?}

    private InteractionResult interact(Level level, BlockPos pos, Player player, ItemStack held) {
        if (!(level.getBlockEntity(pos) instanceof PaintableDuctBlockEntity pipe)) return InteractionResult.PASS;
        if (!held.isEmpty() && held.getItem() instanceof BlockItem ib && allowedPaint(level, pos, ib.getBlock(), this) && pipe.getCamo() == null) {
            if (!level.isClientSide) pipe.setCamo(ib.getBlock().defaultBlockState());
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!exhaust && !held.isEmpty() && held.getItem() instanceof IItemFluidIdentifier id) {
            if (!level.isClientSide) {
                Fluid fluid = id.getType(level, pos, held);
                if (fluid != null) com.hbm_m.api.fluids.PipeTypeChanger.apply(level, pos, fluid == ModFluids.NONE.getSource() ? Fluids.EMPTY : fluid, player.isShiftKeyDown());
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean onScrew(Level world, Player player, BlockPos pos, Direction side, float fX, float fY, float fZ, InteractionHand hand, ToolType tool) {
        if (tool == ToolType.SCREWDRIVER && world.getBlockEntity(pos) instanceof PaintableDuctBlockEntity pipe && pipe.getCamo() != null) {
            if (!world.isClientSide) pipe.setCamo(null);
            return true;
        }
        if (tool == ToolType.DEFUSER) {
            BlockState s = world.getBlockState(pos);
            if (!world.isClientSide) world.setBlock(pos, s.setValue(HIDDEN, !s.getValue(HIDDEN)), 3);
            return true;
        }
        return false;
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, @javax.annotation.Nullable net.minecraft.world.level.BlockGetter level,
                                java.util.List<net.minecraft.network.chat.Component> list, net.minecraft.world.item.TooltipFlag flag) {
        com.hbm_m.util.StandardInfo.add(list, getDescriptionId() + ".desc");
    }

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics g, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof PaintableDuctBlockEntity duct)) return;
        if (exhaust) {
            List<Component> t = new ArrayList<>();
            for (Fluid f : new Fluid[] { ModFluids.SMOKE.getSource(), ModFluids.SMOKE_LEADED.getSource(), ModFluids.SMOKE_POISON.getSource() })
                t.add(Component.literal(HbmFluidRegistry.getFluidName(f)));
            ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, t);
            return;
        }
        Fluid fluid = duct.getFluidType();
        if (fluid == null || fluid == Fluids.EMPTY) fluid = ModFluids.NONE.getSource();
        List<Component> text = new ArrayList<>();
        text.add(Component.literal(HbmFluidRegistry.getFluidName(fluid)).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(HbmFluidRegistry.getTintColor(fluid) & 0xFFFFFF))));
        ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }
}
