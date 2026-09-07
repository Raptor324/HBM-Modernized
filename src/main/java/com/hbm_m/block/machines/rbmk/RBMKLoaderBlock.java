package com.hbm_m.block.machines.rbmk;

import java.util.List;

import com.hbm_m.api.fluids.IFluidConnectorBlock;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.trait.FT_Coolable;
import com.hbm_m.inventory.fluid.trait.FT_Heatable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/**
 * 1:1 port of {@code RBMKLoader} - the RBMK Steam Connector.
 *
 * <p>Its whole job is to give a boiler/cooler/heater/outgasser channel a reachable fluid outlet at
 * floor level: the column pushes steam into whatever sits against this block's sides and bottom
 * (see {@code RBMKBoilerBlockEntity#getOutputPos}), so the water pipe can go directly under the
 * column and the steam duct against the connector one block lower. Exactly like the original it is
 * a plain block with no block entity of its own - it stores and moves nothing.</p>
 *
 * <p>It used to extend {@link RBMKColumnBlock} with its own column block entity, described as a
 * "loader base for the crane". That made it unusable for the one thing it exists for:
 * {@code RBMKColumnBlock#setPlacedBy} fills the {@code RBMKDials.getColumnHeight()} blocks above a
 * column with invisible filler, so placing this under a reactor column overwrote the pipe and the
 * column standing on top of it, and breaking it tore out their filler again.</p>
 */
public class RBMKLoaderBlock extends Block implements IFluidConnectorBlock {

    public RBMKLoaderBlock(Properties props) { super(props); }

    /**
     * 1:1 with the original's {@code IFluidConnectorBlock#canConnect}: the top face carries the
     * heatable feed (water) up into the column, every other face carries the coolable product
     * (steam of any grade) away, plus perfluoromethyl for the cooler channels.
     *
     * <p>This is what lets a duct attach at all. The MK2 network in this port is block-entity
     * driven and the connector deliberately has none, so {@code FluidDuctBlock#canConnectTo} used
     * to stop at its {@code getBlockEntity(neighbor) != null} check and no duct would ever draw a
     * connection towards it - see {@link IFluidConnectorBlock}.</p>
     */
    @Override
    public boolean canConnect(Fluid fluid, LevelReader level, BlockPos pos, Direction dir) {
        if (dir == Direction.UP) return FluidType.hasTrait(fluid, FT_Heatable.class);
        return FluidType.hasTrait(fluid, FT_Coolable.class)
                || fluid == ModFluids.PERFLUOROMETHYL.getSource();
    }

    //? if < 1.21.1 {
    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        addDesc(tooltip);
    }
    //?} else {
    /*@Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext level, List<Component> tooltip, TooltipFlag flag) {
        addDesc(tooltip);
    }
    *///?}

    /** The original's {@code tile.rbmk_loader.desc}, split into its three "$"-separated lines. */
    private static void addDesc(List<Component> tooltip) {
        for (int i = 0; i < 3; i++) {
            tooltip.add(Component.translatable("block.hbm_m.rbmk_loader.desc" + i).withStyle(ChatFormatting.GRAY));
        }
    }
}
