package com.hbm_m.block.machines;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineFractionTowerBlockEntity;
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
 * 1:1 {@code MachineFractionTower}: {@code getDimensions {2,0,1,1,1,1}}, {@code getOffset 1}; vier Anschlusszellen um
 * den Kern. Stapelbar (der naechste Kern sitzt drei Bloecke hoeher). Fluidkennung nur am untersten Segment; kein GUI,
 * dafuer das Blick-Overlay. Gezeichnet vom {@code FractionTowerRenderer}.
 */
public class MachineFractionTowerBlock extends DummyableMachineBlock implements com.hbm_m.interfaces.ILookOverlay {

    public MachineFractionTowerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(2, 0, 1, 1, 1, 1)
                .extra(1, 0, 0)
                .extra(-1, 0, 0)
                .extra(0, 0, 1)
                .extra(0, 0, -1)
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
        return new MachineFractionTowerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.FRACTION_TOWER_BE.get(),
                (lvl, pos, st, be) -> MachineFractionTowerBlockEntity.tick(lvl, pos, st, (MachineFractionTowerBlockEntity) be));
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
                if (!(world.getBlockEntity(pos) instanceof MachineFractionTowerBlockEntity cracker)) return InteractionResult.PASS;

                if (world.getBlockEntity(pos.below(3)) instanceof MachineFractionTowerBlockEntity) {
                    player.displayClientMessage(Component.literal("You can only change the type in the bottom segment!").withStyle(ChatFormatting.RED), false);
                } else {
                    Fluid type = FluidIdentifierItem.resolvePrimaryForTank(held);
                    if (type == null) return InteractionResult.PASS;
                    cracker.tanks[0].setTankType(type);
                    cracker.setChanged();
                    player.displayClientMessage(Component.literal("Changed type to ").withStyle(ChatFormatting.YELLOW)
                            .append(FluidType.forFluid(type).getLocalizedName()).append(Component.literal("!")), false);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics g, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof MachineFractionTowerBlockEntity cracker)) return;

        List<Component> text = new ArrayList<>();
        for (int i = 0; i < cracker.tanks.length; i++) {
            text.add((i == 0 ? Component.literal("-> ").withStyle(ChatFormatting.GREEN) : Component.literal("<- ").withStyle(ChatFormatting.RED))
                    .append(Component.literal("").withStyle(ChatFormatting.WHITE) /* 1.7 §r = Grundfarbe */
                            .append(FluidType.forFluid(cracker.tanks[i].getTankType()).getLocalizedName())
                            .append(": " + cracker.tanks[i].getFill() + "/" + cracker.tanks[i].getMaxFill() + "mB")));
        }

        com.hbm_m.interfaces.ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineFractionTowerBlock> CODEC = simpleCodec(MachineFractionTowerBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
