package com.hbm_m.block.machines;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineCondenserPoweredBlockEntity;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code MachineCondenserPowered}: {@code getDimensions {2,0,1,1,3,3}}, {@code getOffset 1}, sechs
 * Anschlusszellen auf Hoehe 1, Anzeige beim Hinsehen. Gezeichnet vom {@code CondenserPoweredRenderer}.
 */
public class MachineCondenserPoweredBlock extends DummyableMachineBlock implements com.hbm_m.interfaces.ILookOverlay, com.hbm_m.api.block.IToolable {

    public MachineCondenserPoweredBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(2, 0, 1, 1, 3, 3)
                .extra(0, 1, 3)
                .extra(0, 1, -3)
                .extra(1, 1, 1)
                .extra(1, 1, -1)
                .extra(-1, 1, 1)
                .extra(-1, 1, -1)
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
        return new MachineCondenserPoweredBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.CONDENSER_POWERED_BE.get(),
                (lvl, pos, st, be) -> MachineCondenserPoweredBlockEntity.tick(lvl, pos, st, be));
    }

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics g, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof MachineCondenserPoweredBlockEntity tower)) return;
        // E9: beschaedigt nur die Reparaturliste (Space MachineCondenserPowered.printHook)
        if (tower.damaged) {
            com.hbm_m.api.tile.IRepairable.addGenericOverlay(g, world, pos, Component.translatable(getDescriptionId()));
            return;
        }

        List<Component> text = new ArrayList<>();
        text.add(Component.literal(com.hbm_m.util.BobMathUtil.getShortNumber(tower.getEnergyStored()) + "HE / "
                + com.hbm_m.util.BobMathUtil.getShortNumber(MachineCondenserPoweredBlockEntity.maxPower) + "HE"));

        for (int i = 0; i < tower.tanks.length; i++) {
            text.add(Component.literal(i < 1 ? "-> " : "<- ").withStyle(i < 1 ? ChatFormatting.GREEN : ChatFormatting.RED)
                    .append(FluidType.forFluid(tower.tanks[i].getTankType()).getLocalizedName().copy().withStyle(ChatFormatting.WHITE) /* 1.7 §r = Grundfarbe */)
                    .append(Component.literal(": " + String.format(Locale.US, "%,d", tower.tanks[i].getFill()) + "/"
                            + String.format(Locale.US, "%,d", tower.tanks[i].getMaxFill()) + "mB").withStyle(ChatFormatting.WHITE) /* 1.7 §r = Grundfarbe */));
        }

        com.hbm_m.interfaces.ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }


    // -------------------- E9 (Fork, nur mit Space-Addon): Schaden + Reparatur --------------------

    /** Space {@code onScrew}: Schweissbrenner repariert ueber {@code IRepairable.tryRepairMultiblock}. */
    @Override
    public boolean onScrew(Level world, Player player, BlockPos pos, net.minecraft.core.Direction side, float fX, float fY, float fZ, InteractionHand hand, com.hbm_m.api.block.IToolable.ToolType tool) {
        if (tool != com.hbm_m.api.block.IToolable.ToolType.TORCH) return false;
        return com.hbm_m.api.tile.IRepairable.tryRepairMultiblock(world, pos, player);
    }

    /** Explosionen auf Teilzellen erreichen den Kern (Space {@code findCore}) - nur mit Addon. */
    @Override
    public boolean forwardPartExplosions() {
        return com.hbm_m.api.space.SpaceApi.forkActive();
    }

    /** Space {@code onBlockExploded}: erster Treffer beschaedigt, der zweite zerstoert den Kern. */
    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.Explosion explosion) {
        if (!com.hbm_m.api.space.SpaceApi.forkActive() || !(level.getBlockEntity(pos) instanceof MachineCondenserPoweredBlockEntity core)) {
            super.onBlockExploded(state, level, pos, explosion);
            return;
        }
        if (core.lastExplosion == explosion) return;
        core.lastExplosion = explosion;

        if (!core.damaged) {
            core.markDamaged();
        } else {
            level.removeBlock(pos, false);
        }
    }

    /** Space {@code canDropFromExplosion = false} - nur mit Addon. */
    @Override
    public boolean dropFromExplosion(net.minecraft.world.level.Explosion explosion) {
        return !com.hbm_m.api.space.SpaceApi.forkActive() && super.dropFromExplosion(explosion);
    }

    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, TooltipFlag flag) {
    *///?}
        com.hbm_m.util.StandardInfo.add(list, getDescriptionId() + ".desc");
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineCondenserPoweredBlock> CODEC = simpleCodec(MachineCondenserPoweredBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
