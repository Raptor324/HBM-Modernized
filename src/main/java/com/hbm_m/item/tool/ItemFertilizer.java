package com.hbm_m.item.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * 1:1 {@code com.hbm.items.tool.ItemFertilizer} ({@code powder_fertilizer}, Port-ID {@code fertilizer_powder}):
 * duengt ein 3x3x3-Feld um den angeklickten Block, der Mittelblock waechst garantiert. Der Werfer-Effekt
 * ({@code DispenserBehaviorHandler}) haengt am Konstruktor.
 */
public class ItemFertilizer extends Item {

    public ItemFertilizer(Properties properties) {
        super(properties);
        DispenserBlock.registerBehavior(this, new DefaultDispenseItemBehavior() {

            private boolean dispenseSound = true;

            @Override
            protected @NotNull ItemStack execute(BlockSource source, ItemStack stack) {
                Direction facing = source.getBlockState().getValue(DispenserBlock.FACING);
                BlockPos pos = source.getPos().relative(facing);
                this.dispenseSound = useFertillizer(stack, source.getLevel(), pos.getX(), pos.getY(), pos.getZ());
                return stack;
            }

            @Override
            protected void playSound(BlockSource source) {
                if (this.dispenseSound) {
                    source.getLevel().levelEvent(1000, source.getPos(), 0);
                } else {
                    source.getLevel().levelEvent(1001, source.getPos(), 0);
                }
            }
        });
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        Level world = ctx.getLevel();
        ItemStack stack = ctx.getItemInHand();
        BlockPos pos = ctx.getClickedPos();
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        if (player == null || !player.mayUseItemAt(pos, ctx.getClickedFace(), stack)) {
            return InteractionResult.PASS;
        }

        boolean didSomething = false;

        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                for (int k = z - 1; k <= z + 1; k++) {
                    boolean success = fertilize(world, i, j, k, player, i == x && j == y && k == z);
                    didSomething = didSomething || success;
                    if (success && !world.isClientSide) {
                        world.levelEvent(1505, new BlockPos(i, j, k), 0);
                    }
                }
            }
        }

        if (didSomething && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.PASS;
    }

    public static boolean useFertillizer(ItemStack stack, Level world, int x, int y, int z) {

        if (!(world instanceof ServerLevel server)) return false;
        Player player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(server);

        boolean didSomething = false;

        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                for (int k = z - 1; k <= z + 1; k++) {
                    boolean success = fertilize(world, i, j, k, player, i == x && j == y && k == z);
                    didSomething = didSomething || success;
                    if (success && !world.isClientSide) {
                        world.levelEvent(1505, new BlockPos(i, j, k), 0);
                    }
                }
            }
        }

        if (didSomething) stack.shrink(1);

        return didSomething;
    }

    public static boolean fertilize(Level world, int x, int y, int z, Player player, boolean force) {

        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = world.getBlockState(pos);

        // BonemealEvent: -1 = abgebrochen, 1 = ALLOW
        int hook = net.minecraftforge.event.ForgeEventFactory.onApplyBonemeal(player, world, pos, state, ItemStack.EMPTY);
        if (hook < 0) {
            return false;
        }

        if (hook > 0) {
            return true;
        }

        if (state.getBlock() instanceof BonemealableBlock growable) {

            if (growable.isValidBonemealTarget(world, pos, state, world.isClientSide)) {

                if (world instanceof ServerLevel server) {
                    if (force || growable.isBonemealSuccess(world, world.random, pos, state)) {
                        growable.performBonemeal(server, world.random, pos, state);
                    }
                }

                return true;
            }
        }

        return false;
    }
}
