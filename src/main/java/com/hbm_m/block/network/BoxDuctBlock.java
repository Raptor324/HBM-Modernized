package com.hbm_m.block.network;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.api.fluids.IFluidConnectorMK2;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.BoxDuctBlockEntities;
import com.hbm_m.blockentity.network.PaintableDuctBlockEntity;
import com.hbm_m.interfaces.IEnergyConnector;
import com.hbm_m.interfaces.IItemFluidIdentifier;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.inventory.fluid.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 {@code FluidDuctBox} ({@code fluid_duct_box}: 5 Groessen x Silber/Kupfer/Weiss in Metadaten 0-14),
 * {@code FluidDuctBoxExhaust} ({@code fluid_duct_exhaust}: fuehrt die drei Rauchsorten) und
 * {@code PowerCableBox} ({@code red_cable_box}: 5 Groessen). Die Verbindungen stehen fuer den Chunk-Renderer im
 * Blockzustand; gezeichnet wird mit {@code BoxDuctBakedModel} wie {@code RenderBoxDuct}.
 */
public class BoxDuctBlock extends BaseEntityBlock implements ILookOverlay {

    public static final IntegerProperty META = IntegerProperty.create("meta", 0, 14);
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH, SOUTH = BlockStateProperties.SOUTH, EAST = BlockStateProperties.EAST,
            WEST = BlockStateProperties.WEST, UP = BlockStateProperties.UP, DOWN = BlockStateProperties.DOWN;

    public final BoxDuctGeometry.Kind kind;

    public BoxDuctBlock(Properties p, BoxDuctGeometry.Kind kind) {
        super(p);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(META, 0).setValue(NORTH, false).setValue(SOUTH, false).setValue(EAST, false)
                .setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(META, NORTH, SOUTH, EAST, WEST, UP, DOWN); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    public static BooleanProperty prop(Direction d) {
        return switch (d) { case NORTH -> NORTH; case SOUTH -> SOUTH; case EAST -> EAST; case WEST -> WEST; case UP -> UP; default -> DOWN; };
    }

    public int maxMeta() { return kind == BoxDuctGeometry.Kind.CABLE ? 4 : 14; }

    // ---- Verbindungen ({@code canConnectTo}) ----

    private boolean connects(BlockGetter world, BlockPos pos, Direction dir) {
        BlockEntity n = world.getBlockEntity(pos.relative(dir));
        if (kind == BoxDuctGeometry.Kind.CABLE) return n instanceof IEnergyConnector c && c.canConnectEnergy(dir.getOpposite());
        if (!(n instanceof IFluidConnectorMK2 c)) return false;
        if (kind == BoxDuctGeometry.Kind.EXHAUST) {
            return c.canConnect(ModFluids.SMOKE.getSource(), dir.getOpposite()) || c.canConnect(ModFluids.SMOKE_LEADED.getSource(), dir.getOpposite())
                    || c.canConnect(ModFluids.SMOKE_POISON.getSource(), dir.getOpposite());
        }
        if (!(world.getBlockEntity(pos) instanceof PaintableDuctBlockEntity self)) return false;
        return c.canConnect(self.getFluidType(), dir.getOpposite());
    }

    public BlockState connections(BlockGetter world, BlockPos pos, BlockState s) {
        for (Direction d : Direction.values()) s = s.setValue(prop(d), connects(world, pos, d));
        return s;
    }

    /** Nach einem Fluessigkeitswechsel: eigene und benachbarte Kastenrohre neu verbinden. */
    public static void refreshAround(Level level, BlockPos pos) {
        refresh(level, pos);
        for (Direction d : Direction.values()) refresh(level, pos.relative(d));
    }

    private static void refresh(Level level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        if (s.getBlock() instanceof BoxDuctBlock b) {
            BlockState n = b.connections(level, p, s);
            if (n != s) level.setBlock(p, n, Block.UPDATE_ALL);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        int meta = Math.max(0, Math.min(maxMeta(), metaOf(ctx.getItemInHand())));
        return connections(ctx.getLevel(), ctx.getClickedPos(), defaultBlockState().setValue(META, meta));
    }

    @Override
    public BlockState updateShape(BlockState s, Direction d, BlockState n, LevelAccessor w, BlockPos pos, BlockPos np) {
        return s.setValue(prop(d), connects(w, pos, d));
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        BlockState ns = connections(level, pos, state);
        if (ns != state) level.setBlock(pos, ns, Block.UPDATE_CLIENTS);
    }

    // ---- Formen ----

    private static boolean[] conn(BlockState s) {
        return new boolean[] { s.getValue(EAST), s.getValue(WEST), s.getValue(UP), s.getValue(DOWN), s.getValue(SOUTH), s.getValue(NORTH) };
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter w, BlockPos pos, CollisionContext ctx) {
        boolean[] c = conn(s);
        double[] b = BoxDuctGeometry.selection(kind, s.getValue(META), c[0], c[1], c[2], c[3], c[4], c[5]);
        return Shapes.box(b[0], b[1], b[2], b[3], b[4], b[5]);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState s, BlockGetter w, BlockPos pos, CollisionContext ctx) {
        boolean[] c = conn(s);
        VoxelShape shape = Shapes.empty();
        for (double[] b : BoxDuctGeometry.collision(kind, s.getValue(META), c[0], c[1], c[2], c[3], c[4], c[5]))
            shape = Shapes.or(shape, Shapes.box(b[0], b[1], b[2], b[3], b[4], b[5]));
        return shape;
    }

    // ---- Gegenstand je Metadaten (BlockStateTag) ----

    public static ItemStack stack(Item item, int meta) {
        ItemStack s = new ItemStack(item);
        CompoundTag bst = new CompoundTag();
        bst.putString("meta", Integer.toString(meta));
        s.getOrCreateTag().put("BlockStateTag", bst);
        return s;
    }

    public static int metaOf(ItemStack stack) {
        CompoundTag t = stack.getTag();
        if (t == null || !t.contains("BlockStateTag")) return 0;
        try { return Integer.parseInt(t.getCompound("BlockStateTag").getString("meta")); } catch (NumberFormatException e) { return 0; }
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        int m = state.getValue(META);
        return stack(asItem(), kind == BoxDuctGeometry.Kind.CABLE ? m % 5 : m % 15);
    }

    // ---- Benutzung / Anzeige ----

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return identify(level, pos, player, player.getItemInHand(hand));
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return identify(level, pos, player, player.getMainHandItem());
    }
    *///?}

    /** {@code FluidDuctBase.onBlockActivated}: der Identifikator setzt die Fluessigkeit. */
    private InteractionResult identify(Level level, BlockPos pos, Player player, ItemStack held) {
        if (kind != BoxDuctGeometry.Kind.FLUID || held.isEmpty() || !(held.getItem() instanceof IItemFluidIdentifier)) return InteractionResult.PASS;
        return com.hbm_m.api.fluids.PipeTypeChanger.onIdentifier(level, pos, player, held) ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
    }

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics g, Level level, BlockPos pos) {
        if (kind == BoxDuctGeometry.Kind.CABLE) return;
        List<Component> text = new ArrayList<>();
        if (kind == BoxDuctGeometry.Kind.EXHAUST) {
            for (Fluid f : new Fluid[] { ModFluids.SMOKE.getSource(), ModFluids.SMOKE_LEADED.getSource(), ModFluids.SMOKE_POISON.getSource() })
                text.add(Component.literal(HbmFluidRegistry.getFluidName(f)));
        } else {
            if (!(level.getBlockEntity(pos) instanceof PaintableDuctBlockEntity duct)) return;
            Fluid fluid = duct.getFluidType();
            if (fluid == null || fluid == Fluids.EMPTY) fluid = ModFluids.NONE.getSource();
            text.add(Component.literal(HbmFluidRegistry.getFluidName(fluid)).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(HbmFluidRegistry.getTintColor(fluid) & 0xFFFFFF))));
        }
        ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return kind == BoxDuctGeometry.Kind.CABLE ? new BoxDuctBlockEntities.Cable(pos, state) : new BoxDuctBlockEntities.Pipe(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        if (kind == BoxDuctGeometry.Kind.CABLE) return createTickerHelper(type, ModBlockEntities.RED_CABLE_BOX.get(), BoxDuctBlockEntities.Cable::serverTick);
        return createTickerHelper(type, ModBlockEntities.FLUID_DUCT_BOX.get(), PaintableDuctBlockEntity::serverTick);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<BoxDuctBlock> CODEC = simpleCodec(p -> new BoxDuctBlock(p, BoxDuctGeometry.Kind.FLUID));
    @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
