package com.hbm_m.block.generic;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.ModItems;
import com.hbm_m.sound.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code BlockSupplyCrate} (crate_supply): die Kiste, die der C-130-Abwurf hinterlaesst. Haelt eine beliebig lange
 * Liste von Gegenstaenden. Abgebaut (nicht kreativ) faellt sie samt Inhalt als NBT-Gegenstand
 * ({@code amount}/{@code slotN}) heraus und nimmt ihn beim Setzen wieder mit; die Brechstange hebelt sie auf.
 */
public class SupplyCrateBlock extends BaseEntityBlock {

    public SupplyCrateBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SupplyCrateBlockEntity(pos, state);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level world, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {

        if (!player.getAbilities().instabuild && !world.isClientSide && willHarvest) {

            ItemStack drop = new ItemStack(this);
            CompoundTag nbt = new CompoundTag();

            if (world.getBlockEntity(pos) instanceof SupplyCrateBlockEntity inv) {
                for (int i = 0; i < inv.items.size(); i++) {
                    ItemStack stack = inv.items.get(i);
                    if (stack == null || stack.isEmpty()) continue;
                    CompoundTag slot = new CompoundTag();
                    stack.save(slot);
                    nbt.put("slot" + i, slot);
                }
                nbt.putInt("amount", inv.items.size());
            }

            if (!nbt.isEmpty()) drop.setTag(nbt);
            world.addFreshEntity(new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop));
        }
        return super.onDestroyedByPlayer(state, world, pos, player, willHarvest, fluid);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {

        if (world.getBlockEntity(pos) instanceof SupplyCrateBlockEntity inv && stack.hasTag()) {
            int amount = stack.getTag().getInt("amount");
            for (int i = 0; i < amount; i++) {
                inv.items.add(ItemStack.of(stack.getTag().getCompound("slot" + i)));
            }
            inv.setChanged();
        }

        super.setPlacedBy(world, pos, state, placer, stack);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).is(ModItems.CROWBAR.get())) {
            if (!world.isClientSide) {
                dropContents(world, pos);
                world.destroyBlock(pos, false);
                var sounds = List.of(ModSounds.CRATEBREAK1, ModSounds.CRATEBREAK2, ModSounds.CRATEBREAK3, ModSounds.CRATEBREAK4, ModSounds.CRATEBREAK5);
                world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), sounds.get(world.random.nextInt(sounds.size())).get(), SoundSource.BLOCKS, 0.5F, 1.0F);
            }
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
        return InteractionResult.PASS;
    }

    public void dropContents(Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof SupplyCrateBlockEntity crate) {
            for (ItemStack item : crate.items) {
                if (item != null && !item.isEmpty()) net.minecraft.world.level.block.Block.popResource(world, pos, item.copy());
            }
            crate.items.clear();
        }
    }

    public static class SupplyCrateBlockEntity extends BlockEntity {

        public List<ItemStack> items = new ArrayList<>();

        public SupplyCrateBlockEntity(BlockPos pos, BlockState state) {
            super(com.hbm_m.blockentity.ModBlockEntities.CRATE_SUPPLY_BE.get(), pos, state);
        }

        @Override
        public void load(CompoundTag nbt) {
            super.load(nbt);
            items.clear();
            ListTag list = nbt.getList("items", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                items.add(ItemStack.of(list.getCompound(i)));
            }
        }

        @Override
        protected void saveAdditional(CompoundTag nbt) {
            super.saveAdditional(nbt);
            ListTag list = new ListTag();
            for (ItemStack stack : items) {
                list.add(stack.save(new CompoundTag()));
            }
            nbt.put("items", list);
        }
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<SupplyCrateBlock> CODEC = simpleCodec(SupplyCrateBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
