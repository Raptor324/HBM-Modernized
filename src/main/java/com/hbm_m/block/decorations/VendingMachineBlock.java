package com.hbm_m.block.decorations;

import javax.annotation.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.decorations.VendingMachineBlockEntity;
import com.hbm_m.item.ModItems;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code BlockVendingMachine} ({@code vending_machine}, Untertyp 0 Limonade / 1 Snacks als eigene Bloecke):
 * zwei Bloecke hoch ({@code getDimensions {1,0,0,0,0,0}}). Eine Muenze ({@code coin_token}) wirft eine zufaellige
 * Ware aus {@code ItemPoolsVendingMachine} vor der Front aus.
 */
public class VendingMachineBlock extends DummyableMachineBlock {

    public final boolean snacks;

    public VendingMachineBlock(Properties properties, boolean snacks) {
        super(properties);
        this.snacks = snacks;
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(1, 0, 0, 0, 0, 0)
                .placementOffset(0)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VendingMachineBlockEntity(pos, state);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return vend(state, level, pos, player, hand);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return vend(state, level, pos, player, InteractionHand.MAIN_HAND);
    }
    *///?}

    private InteractionResult vend(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty() || !held.is(ModItems.COIN_TOKEN.get())) return InteractionResult.PASS;

        held.shrink(1);
        if (level.isClientSide) return InteractionResult.SUCCESS;

        ItemStack toDrop = pick(snacks, level.getRandom());
        if (!toDrop.isEmpty()) {
            Direction dir = state.getValue(FACING);
            ItemEntity item = new ItemEntity(level, pos.getX() + 0.5 + dir.getStepX() * 0.75, pos.getY() + 0.25,
                    pos.getZ() + 0.5 + dir.getStepZ() * 0.75, toDrop);
            level.addFreshEntity(item);
        }
        level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, HbmSoundsNT.get("weapon.reload.boltOpen"), SoundSource.BLOCKS, 1F, 0.75F);
        return InteractionResult.SUCCESS;
    }

    /** {@code ItemPoolsVendingMachine}: POOL_SODA / POOL_SNACKS (Gewichte wie im Original). */
    private static ItemStack pick(boolean snacks, RandomSource rand) {
        Object[][] pool = snacks
                ? new Object[][] { { ModItems.DEFINITELYFOOD, 10 }, { ModItems.CANNED_BEEF, 5 }, { ModItems.CANNED_TUBE, 5 },
                        { ModItems.TWINKIE, 10 }, { ModItems.CHOCOLATE, 10 } }
                : new Object[][] { { ModItems.BOTTLE_NUKA, 10 }, { ModItems.BOTTLE_CHERRY, 5 }, { ModItems.BOTTLE_QUANTUM, 1 },
                        { ModItems.CAN_BEPIS, 10 }, { ModItems.CAN_LUNA, 10 }, { ModItems.CAN_MUG, 10 }, { ModItems.CAN_BREEN, 1 } };
        int total = 0;
        for (Object[] e : pool) total += (Integer) e[1];
        int r = rand.nextInt(total);
        for (Object[] e : pool) {
            r -= (Integer) e[1];
            if (r < 0) {
                @SuppressWarnings("unchecked")
                var sup = (dev.architectury.registry.registries.RegistrySupplier<net.minecraft.world.item.Item>) e[0];
                return new ItemStack(sup.get());
            }
        }
        return ItemStack.EMPTY;
    }
    //? if >= 1.21.1 {
    /*public static final com.mojang.serialization.MapCodec<VendingMachineBlock> CODEC = com.hbm_m.platform.BlockCodecs.unsupported(VendingMachineBlock.class);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
