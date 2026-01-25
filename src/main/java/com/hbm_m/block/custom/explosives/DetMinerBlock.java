package com.hbm_m.block.custom.explosives;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class DetMinerBlock extends Block implements IDetonatable {

    private static final int MINING_RADIUS = 3;
    private static final int DETONATION_RADIUS = 6;

    public DetMinerBlock(Properties properties) {
        super(properties);
    }
    @Override
    public void appendHoverText(ItemStack stack,
                                @Nullable net.minecraft.world.level.BlockGetter level,
                                List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hbm_m.detminer.line1")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.hbm_m.detminer.line4")
                .withStyle(ChatFormatting.GRAY));
    }
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide && level.hasNeighborSignal(pos)) {
            onDetonate(level, pos, state, null);
        }
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
    }

    @Override
    public boolean onDetonate(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide) return false;

        ServerLevel serverLevel = (ServerLevel) level;

        // 1. Разрушаем блоки в радиусе MINING_RADIUS
        destroyBlocksInRadius(serverLevel, pos);

        // 2. Активируем соседние Detonatable блоки по цепочке
        triggerNearbyDetonations(serverLevel, pos, player);

        // 3. Звук взрыва
        serverLevel.playSound(null, pos, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0F, 1.0F);

        // 4. Удаляем этот блок
        serverLevel.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        serverLevel.gameEvent(null, GameEvent.BLOCK_DESTROY, pos);

        return true;
    }

    private void destroyBlocksInRadius(ServerLevel serverLevel, BlockPos pos) {
        List<BlockPos> blocksToDestroy = new ArrayList<>();
        List<ItemStack> collectedDrops = new ArrayList<>();

        for (int x = -MINING_RADIUS; x <= MINING_RADIUS; x++) {
            for (int y = -MINING_RADIUS; y <= MINING_RADIUS; y++) {
                for (int z = -MINING_RADIUS; z <= MINING_RADIUS; z++) {
                    double dist = Math.sqrt(x * x + y * y + z * z);
                    if (dist <= MINING_RADIUS) {
                        BlockPos checkPos = pos.offset(x, y, z);
                        BlockState blockState = serverLevel.getBlockState(checkPos);
                        float hardness = blockState.getDestroySpeed(serverLevel, checkPos);

                        if (!blockState.isAir() && !blockState.is(Blocks.BEDROCK) && !blockState.is(this) && hardness < 30.0F) {
                            blocksToDestroy.add(checkPos);
                            LootParams.Builder lootParamsBuilder = new LootParams.Builder(serverLevel)
                                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(checkPos))
                                    .withParameter(LootContextParams.TOOL, ItemStack.EMPTY);
                            collectedDrops.addAll(blockState.getDrops(lootParamsBuilder));
                        }
                    }
                }
            }
        }

        for (BlockPos blockPos : blocksToDestroy) {
            serverLevel.setBlockAndUpdate(blockPos, Blocks.AIR.defaultBlockState());
            serverLevel.gameEvent(null, GameEvent.BLOCK_DESTROY, blockPos);
        }

        for (ItemStack drop : collectedDrops) {
            if (!drop.isEmpty()) {
                serverLevel.addFreshEntity(new ItemEntity(serverLevel,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop));
            }
        }
    }

    private void triggerNearbyDetonations(ServerLevel serverLevel, BlockPos pos, Player player) {
        for (int x = -DETONATION_RADIUS; x <= DETONATION_RADIUS; x++) {
            for (int y = -DETONATION_RADIUS; y <= DETONATION_RADIUS; y++) {
                for (int z = -DETONATION_RADIUS; z <= DETONATION_RADIUS; z++) {
                    double dist = Math.sqrt(x * x + y * y + z * z);
                    if (dist <= DETONATION_RADIUS && dist > 0) {
                        BlockPos checkPos = pos.offset(x, y, z);
                        BlockState checkState = serverLevel.getBlockState(checkPos);
                        Block block = checkState.getBlock();
                        if (block instanceof IDetonatable) {
                            IDetonatable detonatable = (IDetonatable) block;
                            int delay = (int)(dist * 2); // Задержка зависит от расстояния
                            serverLevel.getServer().tell(new TickTask(delay, () -> {
                                detonatable.onDetonate(serverLevel, checkPos, checkState, player);
                            }));
                        }
                    }
                }
            }
        }
    }
}
