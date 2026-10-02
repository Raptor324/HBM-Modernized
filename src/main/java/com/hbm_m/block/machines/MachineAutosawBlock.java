package com.hbm_m.block.machines;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.block.IToolable;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineAutosawBlockEntity;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.item.liquids.FluidIdentifierItem;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code MachineAutosaw}: ohne Ausrichtung (der Arm dreht sich frei), Brennstoffwahl per Fluessigkeitskennung,
 * Pausieren per Schraubendreher, Anzeige beim Hinsehen. Gezeichnet vom {@code AutosawRenderer}.
 */
public class MachineAutosawBlock extends BaseEntityBlock implements IToolable, com.hbm_m.interfaces.ILookOverlay {

    public MachineAutosawBlock(Properties properties) {
        super(properties);
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new MachineAutosawBlockEntity(pos, state); }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.AUTOSAW_BE.get(), MachineAutosawBlockEntity::tick);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return activate(world, pos, player, player.getItemInHand(hand));
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        return activate(world, pos, player, player.getMainHandItem());
    }
    *///?}

    private InteractionResult activate(Level world, BlockPos pos, Player player, ItemStack held) {
        if (!world.isClientSide && !player.isShiftKeyDown()) {
            if (!held.isEmpty() && held.getItem() instanceof FluidIdentifierItem && world.getBlockEntity(pos) instanceof MachineAutosawBlockEntity saw) {
                Fluid type = FluidIdentifierItem.resolvePrimaryForTank(held);
                if (type != null && MachineAutosawBlockEntity.acceptedFuels().contains(type)) {
                    saw.tank.setTankType(type);
                    saw.setChanged();
                    player.displayClientMessage(Component.literal("Changed type to ").withStyle(ChatFormatting.YELLOW)
                            .append(FluidType.forFluid(type).getLocalizedName()).append(Component.literal("!")), false);
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean onScrew(Level world, Player player, BlockPos pos, Direction side, float fX, float fY, float fZ, InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        if (!(world.getBlockEntity(pos) instanceof MachineAutosawBlockEntity saw)) return false;
        saw.isSuspended = !saw.isSuspended;
        saw.setChanged();
        return true;
    }

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics g, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof MachineAutosawBlockEntity saw)) return;
        List<Component> text = new ArrayList<>();
        text.add(FluidType.forFluid(saw.tank.getTankType()).getLocalizedName().copy().append(": " + saw.tank.getFill() + "/" + saw.tank.getMaxFill() + "mB"));
        if (saw.isSuspended) text.add(Component.literal(ChatFormatting.RED + "! ").append(Component.translatable(getDescriptionId() + ".suspended")).append(" !"));
        com.hbm_m.interfaces.ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
        com.hbm_m.util.StandardInfo.add(list, getDescriptionId() + ".desc");
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineAutosawBlock> CODEC = simpleCodec(MachineAutosawBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
