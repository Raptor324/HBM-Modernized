package com.hbm_m.api.fluids;

import com.hbm_m.block.machines.FluidDuctBlock;
import com.hbm_m.blockentity.machines.FluidDuctBlockEntity;
import com.hbm_m.blockentity.machines.FluidValveBlockEntity;
import com.hbm_m.blockentity.network.PaintableDuctBlockEntity;
import com.hbm_m.blockentity.network.PipeAnchorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import com.hbm_m.extprop.HbmPlayerProps;
import com.hbm_m.handler.EnumKeybind;
import com.hbm_m.interfaces.IItemFluidIdentifier;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.liquids.FluidIdentifierItem;

/**
 * 1:1 {@code IBlockFluidDuct.changeTypeRecursively} ({@code FluidDuctBase}, {@code FluidPipeAnchor}): mit dem
 * Identifikator schleichend angeklickt, faerbt ein Rohr bis zu 64 Schritte weit alle angrenzenden Rohre der alten
 * Fluessigkeit um; Anker folgen ihrer Anschlussseite und ihren Luftleitungen.
 */
public final class PipeTypeChanger {

    private PipeTypeChanger() {}

    public static final int LOOPS = 64;

    public static Fluid typeOf(BlockEntity be) {
        if (be instanceof PaintableDuctBlockEntity p && p.isExhaust()) return null;
        return be instanceof IFluidPipeMK2 pipe ? pipe.getFluidType() : null;
    }

    public static void setType(BlockEntity be, Fluid fluid) {
        if (be instanceof FluidDuctBlockEntity d) {
            d.setFluidType(fluid);
            // Port: Standardrohre tragen die Anschluesse im Blockzustand, der haengt an der Sorte
            Level l = d.getLevel();
            BlockPos p = d.getBlockPos();
            if (l != null && !l.isClientSide && l.getBlockState(p).getBlock() instanceof FluidDuctBlock fd) {
                l.setBlock(p, fd.getConnectionState(l, p), Block.UPDATE_CLIENTS);
                FluidDuctBlock.refreshAdjacentDucts(l, p);
            }
        }
        else if (be instanceof FluidValveBlockEntity v) v.setFluidType(fluid);
        else if (be instanceof PaintableDuctBlockEntity p) p.setFluidType(fluid);
        else if (be instanceof PipeAnchorBlockEntity a) a.setType(fluid);
    }

    public static void changeTypeRecursively(Level world, BlockPos pos, Fluid prevType, Fluid type, int loopsRemaining) {
        BlockEntity te = world.getBlockEntity(pos);
        Fluid cur = te == null ? null : typeOf(te);
        if (cur == null || cur != prevType || cur == type) return;
        setType(te, type);
        if (loopsRemaining <= 0) return;

        if (te instanceof PipeAnchorBlockEntity anchor) {
            Direction dir = te.getBlockState().getValue(BlockStateProperties.FACING).getOpposite();
            changeTypeRecursively(world, pos.relative(dir), prevType, type, loopsRemaining - 1);
            for (BlockPos p : anchor.getConnected()) changeTypeRecursively(world, p, prevType, type, loopsRemaining - 1);
            return;
        }
        for (Direction dir : Direction.values()) changeTypeRecursively(world, pos.relative(dir), prevType, type, loopsRemaining - 1);
    }

    /**
     * 1:1 {@code FluidDuctBase.onBlockActivated} mit Identifikator in der Hand. Ohne Schleichen und ohne TOOL_CTRL
     * nur dieses Rohr (und nur wenn die Sorte abweicht), sonst rekursiv; TOOL_ALT kopiert die Rohrsorte in den
     * Mehrfach-Identifikator. {@code false} = nicht verbraucht (dann tauscht der Identifikator seine Sorten).
     * Der Client rechnet dieselbe Entscheidung nach, veraendert aber nichts.
     */
    public static boolean onIdentifier(Level world, BlockPos pos, Player player, ItemStack held) {
        if (held.isEmpty() || !(held.getItem() instanceof IItemFluidIdentifier id)) return false;
        Fluid type = id.getType(world, pos, held);
        if (type == null) return false;
        if (type == ModFluids.NONE.getSource()) type = Fluids.EMPTY;

        BlockEntity te = world.getBlockEntity(pos);
        Fluid cur = te == null ? null : typeOf(te);
        if (cur == null) return false;

        HbmPlayerProps props = HbmPlayerProps.getData(player);

        if (props.getKeyPressed(EnumKeybind.TOOL_ALT) && held.getItem() instanceof FluidIdentifierItem) {
            if (type != cur) {
                if (!world.isClientSide) {
                    FluidIdentifierItem.setType(held, cur == Fluids.EMPTY ? ModFluids.NONE.getSource() : cur, true);
                    world.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.25F, 0.75F);
                }
                return true;
            }
        }

        if (!props.getKeyPressed(EnumKeybind.TOOL_CTRL) && !player.isShiftKeyDown()) {
            if (cur != type) {
                if (!world.isClientSide) setType(te, type);
                return true;
            }
            return false;
        }

        if (!world.isClientSide) changeTypeRecursively(world, pos, cur, type, LOOPS);
        return true;
    }

    /** {@code FluidDuctBase.onBlockActivated} mit Identifikator: schleichend rekursiv, sonst nur dieses Rohr. */
    public static void apply(Level world, BlockPos pos, Fluid type, boolean sneaking) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te == null) return;
        Fluid cur = typeOf(te);
        if (cur == null) return;
        if (!sneaking) {
            if (cur != type) setType(te, type);
        } else {
            changeTypeRecursively(world, pos, cur, type, LOOPS);
        }
    }
}
