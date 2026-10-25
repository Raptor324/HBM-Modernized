package com.hbm_m.block.machines;

import javax.annotation.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineWatzPowerplantBlockEntity;
import com.hbm_m.item.ModItems;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code Watz} ({@code BlockDummyable}): achteckiger 7x7x3-Reaktor, dessen Kern in der Mitte der untersten Lage
 * sitzt. Neun Anschlusszellen ({@code makeExtra}, Original {@code TileEntityProxyCombo().inventory().fluid()}) an den
 * vier Seitenmitten unten und oben sowie mittig oben. Wird der Kern zerstoert, fallen die Bauteile (48 Stabilitaets-
 * elemente, 3x64 HSS-Bolzen, 36 Reaktionskammern, 26 Superkuehler, 1 Kernbauteil) heraus; der Block selbst droppt nicht.
 * Gebaut wird er ueber {@code struct_watz_core}.
 */
public class MachineWatzPowerplantBlock extends DummyableMachineBlock {

    /** Original: {@code Watz.drop}. */
    public static boolean drop = true;

    public MachineWatzPowerplantBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        // Original: getAllDimensions() + fillSpace(makeExtra ...), Offset 3
        return DummyableStructureBuilder.create()
                .box(2, 0, 3, 3, 1, 1)
                .box(2, 0, 2, 2, 2, -2)
                .box(2, 0, 2, 2, -2, 2)
                .box(2, 0, 1, 1, 3, -3)
                .box(2, 0, 1, 1, -3, 3)
                .extra(0, 0, 2)
                .extra(0, 0, -2)
                .extra(2, 0, 0)
                .extra(-2, 0, 0)
                .extra(0, 2, 2)
                .extra(0, 2, -2)
                .extra(2, 2, 0)
                .extra(-2, 2, 0)
                .extra(0, 2, 0)
                .placementOffset(3)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && drop) {
            double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
            level.addFreshEntity(new ItemEntity(level, x, y, z, new ItemStack(ModBlocks.WATZ_END.get(), 48)));
            for (int j = 0; j < 3; j++) level.addFreshEntity(new ItemEntity(level, x, y, z, new ItemStack(ModItems.BOLT_HIGHSPEED_STEEL.get(), 64)));
            level.addFreshEntity(new ItemEntity(level, x, y, z, new ItemStack(ModBlocks.WATZ_ELEMENT.get(), 36)));
            level.addFreshEntity(new ItemEntity(level, x, y, z, new ItemStack(ModBlocks.WATZ_COOLER.get(), 26)));
            level.addFreshEntity(new ItemEntity(level, x, y, z, new ItemStack(ModBlocks.STRUCT_WATZ_CORE.get(), 1)));
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineWatzPowerplantBlockEntity(pos, state);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return openMenu(level, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return openMenu(level, pos, player);
    }
    *///?}

    /** Original: {@code standardOpenBehavior}. */
    private InteractionResult openMenu(Level level, BlockPos pos, Player player) {
        if (player.isShiftKeyDown()) return InteractionResult.sidedSuccess(level.isClientSide()); // Original standardOpenBehavior: geschlichen true ohne GUI
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MenuProvider p)
            MenuRegistry.openExtendedMenu((ServerPlayer) player, p, buf -> buf.writeBlockPos(pos));
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.WATZ_POWERPLANT_BE.get(), MachineWatzPowerplantBlockEntity::tick);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineWatzPowerplantBlock> CODEC = simpleCodec(MachineWatzPowerplantBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
