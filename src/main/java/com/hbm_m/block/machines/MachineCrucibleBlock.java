package com.hbm_m.block.machines;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineCrucibleBlockEntity;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.item.material.ItemScraps;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code MachineCrucible}: {@code getDimensions {1,0,1,1,1,1}}, {@code getOffset 1}, alle Zellen sind
 * Inventarzugaenge. Rechtsklick oeffnet die GUI, mit der Schaufel wird der ganze Inhalt als Schrott ausgeschoepft.
 * Beim Abbau faellt der Inhalt als Schrott heraus. Gezeichnet vom {@code CrucibleRenderer}.
 */
public class MachineCrucibleBlock extends DummyableMachineBlock {

    public MachineCrucibleBlock(Properties properties) {
        super(properties);
    }

    /** w16b: Original {@code MachineCrucible.bounding} (detaillierte Hitbox, um die Kernmitte), je FACING gedreht. */
    private static final java.util.Map<net.minecraft.core.Direction, net.minecraft.world.phys.shapes.VoxelShape> BOUNDING_W16B =
            com.hbm_m.multiblock.MultiblockStructureHelper.boundingMasters(new double[][] {
            {-1.5D, 0D, -1.5D, 1.5D, 0.5D, 1.5D},
            {-1.25D, 0.5D, -1.25D, 1.25D, 1.5D, -1D},
            {-1.25D, 0.5D, -1.25D, -1D, 1.5D, 1.25D},
            {-1.25D, 0.5D, 1D, 1.25D, 1.5D, 1.25D},
            {1D, 0.5D, -1.25D, 1.25D, 1.5D, 1.25D}
    });

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getCustomMasterVoxelShape(BlockState state) {
        return BOUNDING_W16B.get(state.getValue(FACING));
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        DummyableStructureBuilder b = DummyableStructureBuilder.create().box(1, 0, 1, 1, 1, 1);
        for (int up = 0; up <= 1; up++)
            for (int f = -1; f <= 1; f++)
                for (int s = -1; s <= 1; s++)
                    if (up != 0 || f != 0 || s != 0) b = b.extra(f, up, s);
        return b.placementOffset(1).build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineCrucibleBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.CRUCIBLE_BE.get(),
                (lvl, pos, st, be) -> MachineCrucibleBlockEntity.tick(lvl, pos, st, (MachineCrucibleBlockEntity) be));
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
        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        } else if (!player.isShiftKeyDown()) {

            if (!(world.getBlockEntity(pos) instanceof MachineCrucibleBlockEntity crucible)) return InteractionResult.PASS;

            if (FoundryBlockUtil.isShovel(held)) {
                List<MaterialStack> stacks = new ArrayList<>();
                stacks.addAll(crucible.recipeStack);
                stacks.addAll(crucible.wasteStack);

                for (MaterialStack stack : stacks) {
                    ItemStack scrap = ItemScraps.create(new MaterialStack(stack.material, stack.amount));
                    if (!player.getInventory().add(scrap)) {
                        world.addFreshEntity(new ItemEntity(world, hit.getLocation().x, hit.getLocation().y, hit.getLocation().z, scrap));
                    }
                }

                player.inventoryMenu.broadcastChanges();
                crucible.recipeStack.clear();
                crucible.wasteStack.clear();
                crucible.setChanged();
            } else {
                MenuRegistry.openExtendedMenu((ServerPlayer) player, crucible, buf -> buf.writeBlockPos(pos));
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !world.isClientSide && world.getBlockEntity(pos) instanceof MachineCrucibleBlockEntity crucible) {
            List<MaterialStack> stacks = new ArrayList<>();
            stacks.addAll(crucible.recipeStack);
            stacks.addAll(crucible.wasteStack);

            for (MaterialStack stack : stacks) {
                ItemStack scrap = ItemScraps.create(new MaterialStack(stack.material, stack.amount));
                if (!scrap.isEmpty()) world.addFreshEntity(new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, scrap));
            }

            crucible.recipeStack.clear();
            crucible.wasteStack.clear();
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
        com.hbm_m.util.StandardInfo.add(list, getDescriptionId() + ".desc");
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineCrucibleBlock> CODEC = simpleCodec(MachineCrucibleBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
