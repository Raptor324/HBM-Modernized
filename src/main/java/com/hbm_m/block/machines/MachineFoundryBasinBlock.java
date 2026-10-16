package com.hbm_m.block.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineFoundryBasinBlockEntity;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.inventory.gui.GUIScreenRecipeSelector;
import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.item.material.ItemCastMold;
import com.hbm_m.item.ModItems;
import com.hbm_m.util.CrucibleUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Port of the 1.7.10 FoundryCastingBase (basin variant). Interaction priority mirrors the original
 * onBlockActivated: take output -> insert mold (size-gated, plays "upgradePlug") -> shovel dumps the
 * molten metal as material scraps -> screwdriver removes the (dry) mold. Breaking the block drops
 * the remaining metal as scraps plus both slots (original breakBlock); a full basin smokes
 * (original randomDisplayTick) and shows the foundry HUD (original printHook).
 */
public class MachineFoundryBasinBlock extends BaseEntityBlock implements ILookOverlay {

    private static final VoxelShape SHAPE = Shapes.or(
            box(0, 0, 0, 16, 2, 16),
            box(0, 2, 0, 2, 16, 16),
            box(14, 2, 0, 16, 16, 16),
            box(2, 2, 0, 14, 16, 2),
            box(2, 2, 14, 14, 16, 16)
    );

    public MachineFoundryBasinBlock(Properties props) { super(props); }

    @Override public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
    @Override public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    /** Верхняя кромка (ориг. this.maxY): 1.0 у бассейна, 0.5 у малой формы. */
    protected float getRimMaxY() { return 1.0F; }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        return handleUse(state, level, pos, player, hand, hit);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return handleUse(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }
    *///?}

    private InteractionResult handleUse(BlockState state, Level level, BlockPos pos,
                                        Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MachineFoundryBasinBlockEntity cast)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);

        // 1) Remove casted item (original: always gives the stack, drops it if the inventory is full)
        ItemStack output = cast.getOutputSlot();
        if (!output.isEmpty()) {
            if (!player.addItem(output.copy())) {
                level.addFreshEntity(new ItemEntity(level,
                        pos.getX() + 0.5, pos.getY() + getRimMaxY(), pos.getZ() + 0.5, output.copy()));
            }
            cast.takeOutput();
            level.sendBlockUpdated(pos, state, state, 3);
            return InteractionResult.SUCCESS;
        }

        // 2) Insert mold — размерная проверка внутри BE (ориг. mold.size == cast.getMoldSize())
        if (!held.isEmpty() && held.getItem() instanceof ItemCastMold) {
            if (cast.insertMold(held)) {
                if (!player.isCreative()) held.shrink(1);
                level.sendBlockUpdated(pos, state, state, 3);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        }

        // 3) Shovel: dump the molten metal as material scraps (original FoundryCastingBase branch)
        if (held.getItem() instanceof ShovelItem) {
            if (cast.amount > 0 && cast.type != null) {
                ItemStack scrap = CrucibleUtil.createScrap(new MaterialStack(cast.type, cast.amount));
                if (!scrap.isEmpty()) {
                    if (!player.addItem(scrap)) {
                        level.addFreshEntity(new ItemEntity(level,
                                pos.getX() + 0.5, pos.getY() + getRimMaxY(), pos.getZ() + 0.5, scrap));
                    }
                }
                cast.amount = 0;
                cast.type = null;
                cast.setChanged();
                level.sendBlockUpdated(pos, state, state, 3);
            }
            return InteractionResult.SUCCESS;
        }

        // 4) Screwdriver: remove the mold, only while there is no metal left (original onScrew)
        if (held.getItem() == ModItems.SCREWDRIVER.get()) {
            if (cast.getMoldSlot().isEmpty() || cast.amount > 0) return InteractionResult.PASS;
            ItemStack mold = cast.takeMold();
            if (mold.isEmpty()) return InteractionResult.PASS;
            if (!player.addItem(mold)) {
                level.addFreshEntity(new ItemEntity(level,
                        pos.getX() + 0.5, pos.getY() + getRimMaxY(), pos.getZ() + 0.5, mold));
            }
            level.sendBlockUpdated(pos, state, state, 3);
            return InteractionResult.SUCCESS;
        }

        // оригинал: пустая рука больше ничего не делает (снятие формы — только отвёрткой)
        return InteractionResult.PASS;
    }

    /** Оригинал FoundryCastingBase.breakBlock: шлак остатка + дроп обоих слотов (форма и готовое изделие). */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MachineFoundryBasinBlockEntity cast) {
            if (cast.amount > 0 && cast.type != null) {
                ItemStack scrap = CrucibleUtil.createScrap(new MaterialStack(cast.type, cast.amount));
                if (!scrap.isEmpty()) {
                    level.addFreshEntity(new ItemEntity(level,
                            pos.getX() + 0.5, pos.getY() + getRimMaxY(), pos.getZ() + 0.5, scrap));
                }
            }
            for (ItemStack stack : new ItemStack[] { cast.getMoldSlot(), cast.getOutputSlot() }) {
                if (!stack.isEmpty()) {
                    level.addFreshEntity(new ItemEntity(level,
                            pos.getX() + 0.5, pos.getY() + getRimMaxY(), pos.getZ() + 0.5, stack.copy()));
                }
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    /** Оригинал randomDisplayTick: дым над полностью заполненной формой. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (level.getBlockEntity(pos) instanceof MachineFoundryBasinBlockEntity cast
                && cast.amount > 0 && cast.amount >= cast.getCapacity()) {
            level.addParticle(ParticleTypes.SMOKE,
                    pos.getX() + 0.25 + random.nextDouble() * 0.5,
                    pos.getY() + getRimMaxY(),
                    pos.getZ() + 0.25 + random.nextDouble() * 0.5,
                    0.0, 0.0, 0.0);
        }
    }

    /** Оригинал FoundryCastingBase.printHook. */
    @Override
    public void printHook(GuiGraphics guiGraphics, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MachineFoundryBasinBlockEntity cast) {
            List<Component> text = new ArrayList<>();

            if (cast.getMoldSlot().isEmpty()) {
                text.add(Component.translatable("foundry.hbm_m.noCast").withStyle(ChatFormatting.RED));
            } else {
                text.add(Component.literal(cast.getMoldSlot().getHoverName().getString())
                        .withStyle(ChatFormatting.BLUE));
            }

            if (cast.type != null && cast.amount > 0) {
                String matName = GUIScreenRecipeSelector.materialNameOf(new MaterialStack(cast.type, 0)).getString();
                text.add(Component.literal(matName + ": " + cast.amount + " / " + cast.getCapacity())
                        .withStyle(ChatFormatting.YELLOW));
            }

            ILookOverlay.printGeneric(guiGraphics, Component.translatable(getDescriptionId()), 0xFF4000, 0x401000, text);
        }
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineFoundryBasinBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.FOUNDRY_BASIN_BE.get(),
                MachineFoundryBasinBlockEntity::tick);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineFoundryBasinBlock> CODEC = simpleCodec(MachineFoundryBasinBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
