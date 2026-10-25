package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineArcFurnaceBlockEntity;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.item.material.ItemScraps;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code MachineArcFurnaceLarge}: {@code getDimensions {4,0,2,2,2,2}} plus Quader {@code {4,0,3,-2,1,1}},
 * {@code getOffset 2}; sechs Anschluesse (Inventar + Strom). Schaufel leert die Schmelze als Schrott, sonst GUI.
 */
public class MachineArcFurnaceBlock extends DummyableMachineBlock {

    public MachineArcFurnaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(4, 0, 2, 2, 2, 2)
                .box(4, 0, 3, -2, 1, 1)
                .extra(2, 0, 1)
                .extra(2, 0, -1)
                .extra(1, 0, 2)
                .extra(-1, 0, 2)
                .extra(1, 0, -2)
                .extra(-1, 0, -2)
                .placementOffset(2)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineArcFurnaceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.ARC_FURNACE_BE.get(), MachineArcFurnaceBlockEntity::tick);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return activate(level, pos, player, player.getItemInHand(hand), hit);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return activate(level, pos, player, player.getMainHandItem(), hit);
    }
    *///?}

    private InteractionResult activate(Level world, BlockPos pos, Player player, ItemStack held, BlockHitResult hit) {

        if (world.isClientSide) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.SUCCESS;
        if (!(world.getBlockEntity(pos) instanceof MachineArcFurnaceBlockEntity crucible)) return InteractionResult.PASS;

        if (FoundryBlockUtil.isShovel(held)) {
            for (MaterialStack stack : crucible.liquids) {
                ItemStack scrap = ItemScraps.create(new MaterialStack(stack.material, stack.amount));
                if (!player.getInventory().add(scrap)) {
                    world.addFreshEntity(new ItemEntity(world, hit.getLocation().x, hit.getLocation().y, hit.getLocation().z, scrap));
                }
            }

            player.inventoryMenu.broadcastChanges();
            crucible.liquids.clear();
            crucible.setChanged();
        } else {
            MenuRegistry.openExtendedMenu((ServerPlayer) player, crucible, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.CONSUME;
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineArcFurnaceBlock> CODEC = simpleCodec(MachineArcFurnaceBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
