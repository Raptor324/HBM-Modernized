package com.hbm_m.block.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineElectricHeaterBlockEntity;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

/**
 * 1:1-Port von {@code HeaterElectric} (1.7.10): der elektrische Waermeerzeuger.
 *
 * <p>Er ist wie im Original <b>zwei Felder tief und drei breit</b>
 * ({@code getDimensions {0,0,1,2,1,1}}, Setzversatz 2) - die vordere Zelle ist zugleich der
 * Anschluss, an dem das Kabel andockt.</p>
 */
public class MachineElectricHeaterBlock extends DummyableMachineBlock {

    public MachineElectricHeaterBlock(Properties properties) { super(properties); }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        // Original: getDimensions {0,0,1,2,1,1}, getOffset 2, makeExtra auf der Fassade
        // (= Kern minus zwei in Blickrichtung).
        return DummyableStructureBuilder.create()
                .box(0, 0, 1, 2, 1, 1)
                .extra(-2, 0, 0)
                .placementOffset(2)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineElectricHeaterBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.ELECTRIC_HEATER_BE.get(),
                (lvl, pos, st, be) -> MachineElectricHeaterBlockEntity.tick(lvl, pos, st, (MachineElectricHeaterBlockEntity) be));
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() == ModItems.SCREWDRIVER.get()) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MachineElectricHeaterBlockEntity be) {
                be.cycleSetting();
                player.displayClientMessage(Component.literal("Setting: " + be.getSetting()), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
        }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (held.getItem() == ModItems.SCREWDRIVER.get()) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MachineElectricHeaterBlockEntity be) {
                be.cycleSetting();
                player.displayClientMessage(Component.literal("Setting: " + be.getSetting()), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
        }
    *///?}


    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineElectricHeaterBlock> CODEC = simpleCodec(MachineElectricHeaterBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
