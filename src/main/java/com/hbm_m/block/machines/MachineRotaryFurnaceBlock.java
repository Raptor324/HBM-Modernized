package com.hbm_m.block.machines;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineRotaryFurnaceBlockEntity;
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
 * 1:1 {@code MachineRotaryFurnace}: {@code getDimensions {4,0,1,1,2,2}}, {@code getOffset 1}; Anschluesse an der ganzen
 * Rueckwand, Fluid seitlich vorn, Schornstein oben, Brennstoffklappe vorn. Blickhilfe zeigt Dampf, Fluid und Brennstoff
 * je nach angeschauter Zelle.
 */
public class MachineRotaryFurnaceBlock extends DummyableMachineBlock implements com.hbm_m.interfaces.ILookOverlay {

    public MachineRotaryFurnaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(4, 0, 1, 1, 2, 2)
                // back (Original: rot = getRotation(DOWN), hier entlang getRotation(UP) gespiegelt)
                .extra(-1, 0, -2)
                .extra(-1, 0, -1)
                .extra(-1, 0, 0)
                .extra(-1, 0, 1)
                .extra(-1, 0, 2)
                // side fluid
                .extra(1, 0, -2)
                // exhaust
                .extra(0, 4, -1)
                // solid fuel
                .extra(1, 0, -1)
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
        return new MachineRotaryFurnaceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.ROTARY_FURNACE_BE.get(), MachineRotaryFurnaceBlockEntity::tick);
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

    /** Original {@code standardOpenBehavior}: ohne Schleichen die GUI. */
    private InteractionResult open(Level level, BlockPos pos, Player player) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.sidedSuccess(level.isClientSide()); // Original standardOpenBehavior: geschlichen true ohne GUI
        if (level.getBlockEntity(pos) instanceof MachineRotaryFurnaceBlockEntity furnace) {
            MenuRegistry.openExtendedMenu((ServerPlayer) player, furnace, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics g, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof MachineRotaryFurnaceBlockEntity furnace)) return;

        BlockPos hit = pos;
        if (net.minecraft.client.Minecraft.getInstance().hitResult instanceof BlockHitResult bhr) hit = bhr.getBlockPos();

        Direction dir = furnace.getBlockState().getValue(FACING);

        List<Component> text = new ArrayList<>();

        //steam
        if (hitCheck(dir, pos, -1, -1, 0, hit) || hitCheck(dir, pos, -1, -2, 0, hit)) {
            text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN).append(FluidType.forFluid(furnace.tanks[1].getTankType()).getLocalizedName()));
            text.add(Component.literal("<- ").withStyle(ChatFormatting.RED).append(FluidType.forFluid(furnace.tanks[2].getTankType()).getLocalizedName()));
        }

        //fluids
        if (hitCheck(dir, pos, 1, 2, 0, hit) || hitCheck(dir, pos, -1, 2, 0, hit)) {
            text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN).append(FluidType.forFluid(furnace.tanks[0].getTankType()).getLocalizedName()));
        }

        if (hitCheck(dir, pos, 1, 1, 0, hit)) {
            text.add(Component.literal("-> ").withStyle(ChatFormatting.YELLOW).append("Fuel"));
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
    /*public static final com.mojang.serialization.MapCodec<MachineRotaryFurnaceBlock> CODEC = simpleCodec(MachineRotaryFurnaceBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
