package com.hbm_m.block.machines;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.block.IToolable;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineFoundryBaseBlockEntity;
import com.hbm_m.blockentity.machines.MachineFoundryBasinBlockEntity;
import com.hbm_m.blockentity.machines.MachineFoundryCastingBaseBlockEntity;
import com.hbm_m.item.material.ItemMold;
import com.hbm_m.item.material.ItemMold.Mold;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 {@code FoundryBasin} / {@code FoundryCastingBase}: Rechtsklick nimmt zuerst das Gussstueck heraus, setzt sonst eine
 * passende Form ein; die Schaufel leert das fluessige Material als Schrott aus, der Schraubenzieher nimmt die Form
 * (nur leer) wieder heraus. Beim Abbau fallen Inhalt, Form und Gussstueck heraus. Volle Formen dampfen.
 */
public class MachineFoundryBasinBlock extends BaseEntityBlock implements IToolable, com.hbm_m.interfaces.ILookOverlay {

    private static final VoxelShape SHAPE = Shapes.or(
            box(0, 0, 0, 16, 2, 16),
            box(0, 2, 0, 2, 16, 16),
            box(14, 2, 0, 16, 16, 16),
            box(2, 2, 0, 14, 16, 2),
            box(2, 2, 14, 14, 16, 16)
    );

    public MachineFoundryBasinBlock(Properties props) { super(props); }

    /** Original {@code maxY}: Hoehe, auf der Gegenstaende herausfallen. */
    protected double maxY() { return 0.999D; }

    @Override public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
    @Override public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return handleUse(level, pos, player, player.getItemInHand(hand));
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return handleUse(level, pos, player, player.getMainHandItem());
    }
    *///?}

    private InteractionResult handleUse(Level world, BlockPos pos, Player player, ItemStack held) {

        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(world.getBlockEntity(pos) instanceof MachineFoundryCastingBaseBlockEntity cast)) return InteractionResult.PASS;

        //remove casted item
        if (!cast.slots[1].isEmpty()) {
            if (!player.getInventory().add(cast.slots[1].copy())) {
                world.addFreshEntity(new ItemEntity(world, pos.getX() + 0.5, pos.getY() + maxY(), pos.getZ() + 0.5, cast.slots[1].copy()));
            } else {
                player.inventoryMenu.broadcastChanges();
            }
            cast.slots[1] = ItemStack.EMPTY;
            cast.markForUpdate();
            return InteractionResult.CONSUME;
        }

        //insert mold
        if (!held.isEmpty() && cast.slots[0].isEmpty()) {
            Mold mold = ItemMold.getMold(held);
            if (mold != null && mold.size == cast.getMoldSize()) {
                cast.slots[0] = held.copyWithCount(1);
                held.shrink(1);
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, com.hbm_m.sound.HbmSoundsNT.get("hbm:item.upgradePlug"), SoundSource.BLOCKS, 1.0F, 1.0F);
                cast.markForUpdate();
                return InteractionResult.CONSUME;
            }
        }

        if (FoundryBlockUtil.isShovel(held)) {
            FoundryBlockUtil.shovelOut(world, pos, player, cast, maxY());
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !world.isClientSide && world.getBlockEntity(pos) instanceof MachineFoundryCastingBaseBlockEntity cast) {
            FoundryBlockUtil.dropContents(world, pos, cast, maxY());
            for (ItemStack stack : cast.slots) {
                if (!stack.isEmpty()) {
                    world.addFreshEntity(new ItemEntity(world, pos.getX() + 0.5, pos.getY() + maxY(), pos.getZ() + 0.5, stack.copy()));
                }
            }
        }
        super.onRemove(state, world, pos, newState, moving);
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource rand) {
        super.animateTick(state, world, pos, rand);
        if (world.getBlockEntity(pos) instanceof MachineFoundryCastingBaseBlockEntity cast) {
            if (cast.amount > 0 && cast.amount >= cast.getCapacity()) {
                world.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.25 + rand.nextDouble() * 0.5, pos.getY() + maxY(), pos.getZ() + 0.25 + rand.nextDouble() * 0.5, 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    public boolean onScrew(Level world, Player player, BlockPos pos, Direction side, float fX, float fY, float fZ, InteractionHand hand, ToolType tool) {

        if (tool != ToolType.SCREWDRIVER)
            return false;

        if (!(world.getBlockEntity(pos) instanceof MachineFoundryCastingBaseBlockEntity cast)) return false;

        if (cast.slots[0].isEmpty()) return false;
        if (cast.amount > 0) return false;
        if (world.isClientSide) return true;

        if (!player.getInventory().add(cast.slots[0].copy())) {
            world.addFreshEntity(new ItemEntity(world, pos.getX() + 0.5, pos.getY() + maxY(), pos.getZ() + 0.5, cast.slots[0].copy()));
        } else {
            player.inventoryMenu.broadcastChanges();
        }

        cast.slots[0] = ItemStack.EMPTY;
        cast.markForUpdate();
        return true;
    }

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics g, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof MachineFoundryCastingBaseBlockEntity cast)) return;
        List<Component> text = new ArrayList<>();

        if (cast.slots[0].isEmpty()) {
            text.add(Component.translatable("foundry.noCast").withStyle(ChatFormatting.RED));
        } else {
            Mold mold = ItemMold.getMold(cast.slots[0]);
            if (mold != null) text.add(mold.getTitle().copy().withStyle(ChatFormatting.BLUE));
        }

        if (cast.type != null && cast.amount > 0) {
            text.add(cast.type.getLocalizedName().copy().append(": " + cast.amount + " / " + cast.getCapacity()).withStyle(ChatFormatting.YELLOW));
        }

        com.hbm_m.interfaces.ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xFF4000, 0x401000, text);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineFoundryBasinBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.FOUNDRY_BASIN_BE.get(),
                (lvl, pos, st, be) -> MachineFoundryBaseBlockEntity.tick(lvl, pos, st, be));
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineFoundryBasinBlock> CODEC = simpleCodec(MachineFoundryBasinBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
