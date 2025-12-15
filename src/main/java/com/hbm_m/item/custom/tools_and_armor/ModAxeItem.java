package com.hbm_m.item.custom.tools_and_armor;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class ModAxeItem extends AxeItem {
    private static final String NBT_VEIN_MINER = "VeinMinerEnabled";
    private static final String NBT_SILK_TOUCH = "SilkTouchEnabled";

    private static final Set<Block> EXCLUDED_BLOCKS = Set.of(
            Blocks.STONE, Blocks.ANDESITE, Blocks.DIORITE, Blocks.GRANITE,
            Blocks.GRAVEL, Blocks.DIRT, Blocks.COARSE_DIRT,
            Blocks.SAND, Blocks.RED_SAND,
            Blocks.DEEPSLATE, Blocks.NETHERRACK, Blocks.END_STONE
    );

    private final int veinMinerLevel;
    private final int silkTouchLevel;

    public ModAxeItem(Tier tier, float attackDamage, float attackSpeed, Properties properties,
                      int veinMinerLevel, int silkTouchLevel) {
        super(tier, attackDamage, attackSpeed, properties);
        this.veinMinerLevel = Math.max(0, Math.min(6, veinMinerLevel));
        this.silkTouchLevel = Math.max(0, Math.min(1, silkTouchLevel));
    }

    public ModAxeItem(Tier tier, float attackDamage, float attackSpeed, Properties properties) {
        this(tier, attackDamage, attackSpeed, properties, 0, 0);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        // Заголовок списка способностей
        tooltip.add(Component.translatable("tooltip.hbm_m.abilities").withStyle(ChatFormatting.BLUE));

        // Vein Miner
        if (veinMinerLevel > 0) {
            boolean isActive = isVeinMinerEnabled(stack);
            ChatFormatting color = isActive ? ChatFormatting.YELLOW : ChatFormatting.GOLD;
            tooltip.add(Component.literal("  ")
                    .append(Component.translatable("tooltip.hbm_m.vein_miner", veinMinerLevel)
                            .withStyle(color)));
        }

        // Silk Touch
        if (silkTouchLevel > 0) {
            boolean isActive = isSilkTouchEnabled(stack);
            ChatFormatting color = isActive ? ChatFormatting.YELLOW : ChatFormatting.GOLD;
            tooltip.add(Component.literal("  ")
                    .append(Component.translatable("tooltip.hbm_m.silk_touch")
                            .withStyle(color)));
        }

        // Инструкции по использованию
        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable("tooltip.hbm_m.right_click").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.hbm_m.shift_right_click").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isVeinMinerEnabled(stack) || isSilkTouchEnabled(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            if (player.isShiftKeyDown()) {
                // Shift + ПКМ = выключить всё
                disableAllAbilities(stack, player);
            } else {
                // Обычный ПКМ = переключить на следующую способность
                cycleAbilities(stack, player);
            }
            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    private void cycleAbilities(ItemStack stack, Player player) {
        boolean anyActive = isVeinMinerEnabled(stack) || isSilkTouchEnabled(stack);

        if (!anyActive) {
            // Включаем первую доступную способность
            if (veinMinerLevel > 0) {
                toggleVeinMiner(stack, player, true);
            } else if (silkTouchLevel > 0) {
                toggleSilkTouch(stack, player, true);
            }
        } else if (isVeinMinerEnabled(stack)) {
            toggleVeinMiner(stack, player, false);
            if (silkTouchLevel > 0) {
                toggleSilkTouch(stack, player, true);
            } else {
                disableAllAbilities(stack, player);
            }
        } else if (isSilkTouchEnabled(stack)) {
            disableAllAbilities(stack, player);
        }
    }

    private void disableAllAbilities(ItemStack stack, Player player) {
        stack.getOrCreateTag().putBoolean(NBT_VEIN_MINER, false);
        stack.getOrCreateTag().putBoolean(NBT_SILK_TOUCH, false);
        removeAllEnchantments(stack);
        playToggleSound(player, false);
        player.displayClientMessage(
                Component.translatable("message.hbm_m.disabled").withStyle(ChatFormatting.RED),
                true
        );
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        boolean result = super.mineBlock(stack, level, state, pos, entity);

        if (level.isClientSide()) return result;

        Block minedBlock = state.getBlock();

        if (veinMinerLevel > 0 && isVeinMinerEnabled(stack) && !EXCLUDED_BLOCKS.contains(minedBlock)) {
            int radius = 2 + veinMinerLevel;
            veinMine(level, pos, minedBlock, stack, entity, radius);
        }

        return result;
    }

    private void toggleVeinMiner(ItemStack stack, Player player, boolean enable) {
        stack.getOrCreateTag().putBoolean(NBT_VEIN_MINER, enable);
        if (enable) {
            stack.getOrCreateTag().putBoolean(NBT_SILK_TOUCH, false);
            removeAllEnchantments(stack);
        }
        playToggleSound(player, enable);
        player.displayClientMessage(
                Component.translatable(
                        enable ? "message.hbm_m.vein_miner.enabled" : "message.hbm_m.vein_miner.disabled",
                        veinMinerLevel
                ).withStyle(enable ? ChatFormatting.YELLOW : ChatFormatting.RED),
                true
        );
    }

    private void toggleSilkTouch(ItemStack stack, Player player, boolean enable) {
        stack.getOrCreateTag().putBoolean(NBT_SILK_TOUCH, enable);
        if (enable) {
            stack.getOrCreateTag().putBoolean(NBT_VEIN_MINER, false);
            removeAllEnchantments(stack);
            stack.enchant(Enchantments.SILK_TOUCH, 1);
        } else {
            removeAllEnchantments(stack);
        }
        playToggleSound(player, enable);
        player.displayClientMessage(
                Component.translatable(
                        enable ? "message.hbm_m.silk_touch.enabled" : "message.hbm_m.silk_touch.disabled"
                ).withStyle(enable ? ChatFormatting.YELLOW : ChatFormatting.RED),
                true
        );
    }

    private void removeAllEnchantments(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("Enchantments")) {
            stack.getTag().remove("Enchantments");
        }
    }

    private void playToggleSound(Player player, boolean enable) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS,
                0.7F, enable ? 1.2F : 0.8F);
    }

    private boolean isVeinMinerEnabled(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(NBT_VEIN_MINER);
    }

    private boolean isSilkTouchEnabled(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(NBT_SILK_TOUCH);
    }

    private void veinMine(Level level, BlockPos startPos, Block targetBlock, ItemStack stack, LivingEntity entity, int radius) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> toCheck = new LinkedList<>();
        Set<BlockPos> toMine = new HashSet<>();

        toCheck.add(startPos);
        visited.add(startPos);

        boolean isCreative = entity instanceof Player player && player.isCreative();
        int maxBlocks = isCreative ? 512 : 64;

        while (!toCheck.isEmpty() && toMine.size() < maxBlocks) {
            BlockPos current = toCheck.poll();

            for (BlockPos neighbor : getNeighbors(current, startPos, radius)) {
                if (visited.contains(neighbor)) continue;

                visited.add(neighbor);
                BlockState neighborState = level.getBlockState(neighbor);
                Block neighborBlock = neighborState.getBlock();

                if (neighborBlock == targetBlock) {
                    toMine.add(neighbor);
                    toCheck.add(neighbor);
                }
            }
        }

        for (BlockPos pos : toMine) {
            mineBlockWithSilkTouch(level, pos, stack, entity);
            if (!isCreative && stack.isEmpty()) break;
        }
    }

    private Set<BlockPos> getNeighbors(BlockPos pos, BlockPos startPos, int radius) {
        Set<BlockPos> neighbors = new HashSet<>();

        BlockPos[] directions = {
                pos.above(), pos.below(),
                pos.north(), pos.south(),
                pos.west(), pos.east()
        };

        for (BlockPos neighbor : directions) {
            if (neighbor.distManhattan(startPos) <= radius) {
                neighbors.add(neighbor);
            }
        }

        return neighbors;
    }

    private void mineBlockWithSilkTouch(Level level, BlockPos pos, ItemStack stack, LivingEntity entity) {
        BlockState blockState = level.getBlockState(pos);

        if (!stack.isCorrectToolForDrops(blockState)) return;

        if (level instanceof ServerLevel serverLevel) {
            Block.dropResources(blockState, level, pos, level.getBlockEntity(pos), entity, stack);
            level.removeBlock(pos, false);
        }

        // Урон инструменту (только если не в креативе)
        if (!(entity instanceof Player player && player.isCreative())) {
            stack.hurtAndBreak(1, entity, (e) -> {
                e.broadcastBreakEvent(InteractionHand.MAIN_HAND);
            });
        }
    }
}
