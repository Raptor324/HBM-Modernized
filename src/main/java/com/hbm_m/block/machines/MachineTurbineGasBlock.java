package com.hbm_m.block.machines;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineTurbineGasBlockEntity;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
 * 1:1 {@code MachineTurbineGas}: {@code getDimensions {2,0,1,1,4,5}}, {@code getOffset 1}; Anschlusszellen fuer
 * Kraftstoff/Schmiermittel vorn und hinten rechts, Wasser vorn und hinten links, Heissdampf links oben, Strom rechts
 * oben. Das Blick-Overlay zeigt je nach angeschauter Anschlusszelle, was dort hinein- oder herausgeht.
 */
public class MachineTurbineGasBlock extends DummyableMachineBlock implements com.hbm_m.interfaces.ILookOverlay {

    public MachineTurbineGasBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(2, 0, 1, 1, 4, 5)
                .extra(-1, 0, 1)
                .extra(1, 0, 1)
                .extra(-1, 0, -4)
                .extra(1, 0, -4)
                .extra(0, 1, 4)
                .extra(0, 1, -5)
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
        return new MachineTurbineGasBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.TURBINEGAS_BE.get(),
                (lvl, pos, st, be) -> MachineTurbineGasBlockEntity.tick(lvl, pos, st, (MachineTurbineGasBlockEntity) be));
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

    private InteractionResult open(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide() && !player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof MachineTurbineGasBlockEntity turbine) {
            MenuRegistry.openExtendedMenu((ServerPlayer) player, turbine, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics g, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof MachineTurbineGasBlockEntity turbine)) return;

        // Original: die angeschaute Zelle entscheidet, welche Zeile erscheint
        BlockPos hit = pos;
        if (net.minecraft.client.Minecraft.getInstance().hitResult instanceof BlockHitResult bhr) hit = bhr.getBlockPos();

        Direction dir = turbine.getBlockState().getValue(FACING);

        List<Component> text = new ArrayList<>();

        if (hitCheck(dir, pos, -1, -1, 0, hit) || hitCheck(dir, pos, 1, -1, 0, hit)) {
            text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN).append(FluidType.forFluid(turbine.tanks[0].getTankType()).getLocalizedName()));
            text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN).append(FluidType.forFluid(turbine.tanks[1].getTankType()).getLocalizedName()));
        }

        if (hitCheck(dir, pos, -1, 4, 0, hit) || hitCheck(dir, pos, 1, 4, 0, hit)) {
            text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN).append(FluidType.forFluid(turbine.tanks[2].getTankType()).getLocalizedName()));
        }

        if (hitCheck(dir, pos, 0, 5, 1, hit)) {
            text.add(Component.literal("<- ").withStyle(ChatFormatting.RED).append(FluidType.forFluid(turbine.tanks[3].getTankType()).getLocalizedName()));
        }

        if (hitCheck(dir, pos, 0, -4, 1, hit)) {
            text.add(Component.literal("<- ").withStyle(ChatFormatting.RED).append("Power"));
        }

        if (!text.isEmpty()) {
            com.hbm_m.interfaces.ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
        }
    }

    /** Original {@code hitCheck}: {@code turn = dir.getRotation(DOWN)}. */
    protected boolean hitCheck(Direction dir, BlockPos core, int exDir, int exRot, int exY, BlockPos hit) {
        Direction turn = dir.getCounterClockWise();
        BlockPos i = core.relative(dir, exDir).relative(turn, exRot).above(exY);
        return i.equals(hit);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineTurbineGasBlock> CODEC = simpleCodec(MachineTurbineGasBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
