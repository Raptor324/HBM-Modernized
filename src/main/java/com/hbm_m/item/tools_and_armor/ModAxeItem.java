package com.hbm_m.item.tools_and_armor;

import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.client.overlay.OverlayInfoToast;
import com.hbm_m.platform.PlatformHooks;
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
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class ModAxeItem extends AxeItem implements ITooltipProvider {
    private static final String NBT_VEIN_MINER = "VeinMinerEnabled";
    private static final String NBT_SILK_TOUCH = "SilkTouchEnabled";
    private static final String NBT_PRE_SILK = "PreModeSilk";

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
    public void appendHbmTooltip(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {

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
        return super.isFoil(stack) || isVeinMinerEnabled(stack) || isSilkTouchEnabled(stack);
    }

    private record ModeFeedback(Component text, ChatFormatting color) {}

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean apply = !level.isClientSide();

        ModeFeedback feedback = player.isShiftKeyDown()
                ? disableAllAbilities(stack, player, apply)
                : cycleAbilities(stack, player, apply);

        if (level.isClientSide() && feedback != null) {
            OverlayInfoToast.showToolMode(feedback.text(), feedback.color());
        }

        return level.isClientSide()
                ? InteractionResultHolder.sidedSuccess(stack, true)
                : InteractionResultHolder.success(stack);
    }

    private ModeFeedback cycleAbilities(ItemStack stack, Player player, boolean apply) {
        ModeFeedback feedback = null;
        boolean anyActive = isVeinMinerEnabled(stack) || isSilkTouchEnabled(stack);

        if (!anyActive) {
            if (veinMinerLevel > 0) {
                feedback = toggleVeinMiner(stack, player, true, apply);
            } else if (silkTouchLevel > 0) {
                feedback = toggleSilkTouch(stack, player, true, apply);
            }
        } else if (isVeinMinerEnabled(stack)) {
            feedback = toggleVeinMiner(stack, player, false, apply);
            if (silkTouchLevel > 0) {
                feedback = toggleSilkTouch(stack, player, true, apply);
            } else {
                feedback = disableAllAbilities(stack, player, apply);
            }
        } else if (isSilkTouchEnabled(stack)) {
            feedback = disableAllAbilities(stack, player, apply);
        }
        return feedback;
    }

    private ModeFeedback disableAllAbilities(ItemStack stack, Player player, boolean apply) {
        if (apply) {
            PlatformHooks.putBoolean(stack, NBT_VEIN_MINER, false);
            PlatformHooks.putBoolean(stack, NBT_SILK_TOUCH, false);
            clearModeSilkTouch(stack);
            playToggleSound(player, false);
        }
        return new ModeFeedback(
                Component.translatable("message.hbm_m.disabled").withStyle(ChatFormatting.RED),
                ChatFormatting.RED
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

    private ModeFeedback toggleVeinMiner(ItemStack stack, Player player, boolean enable, boolean apply) {
        if (apply) {
            PlatformHooks.putBoolean(stack, NBT_VEIN_MINER, enable);
            if (enable) {
                PlatformHooks.putBoolean(stack, NBT_SILK_TOUCH, false);
                clearModeSilkTouch(stack);
            }
            playToggleSound(player, enable);
        }
        ChatFormatting color = enable ? ChatFormatting.YELLOW : ChatFormatting.RED;
        return new ModeFeedback(
                Component.translatable(
                        enable ? "message.hbm_m.vein_miner.enabled" : "message.hbm_m.vein_miner.disabled",
                        veinMinerLevel
                ).withStyle(color),
                color
        );
    }

    private ModeFeedback toggleSilkTouch(ItemStack stack, Player player, boolean enable, boolean apply) {
        if (apply) {
            PlatformHooks.putBoolean(stack, NBT_SILK_TOUCH, enable);
            if (enable) {
                PlatformHooks.putBoolean(stack, NBT_VEIN_MINER, false);
                applyModeSilkTouch(stack);
            } else {
                clearModeSilkTouch(stack);
            }
            playToggleSound(player, enable);
        }
        ChatFormatting color = enable ? ChatFormatting.YELLOW : ChatFormatting.RED;
        return new ModeFeedback(
                Component.translatable(
                        enable ? "message.hbm_m.silk_touch.enabled" : "message.hbm_m.silk_touch.disabled"
                ).withStyle(color),
                color
        );
    }

    private void applyModeSilkTouch(ItemStack stack) {
        int vanilla = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, stack);
        PlatformHooks.putInt(stack, NBT_PRE_SILK, vanilla);
        Map<Enchantment, Integer> enchants = new HashMap<>(EnchantmentHelper.getEnchantments(stack));
        enchants.put(Enchantments.SILK_TOUCH, Math.max(vanilla, 1));
        EnchantmentHelper.setEnchantments(enchants, stack);
    }

    private void clearModeSilkTouch(ItemStack stack) {
        if (!PlatformHooks.hasItemTag(stack) || !PlatformHooks.contains(stack, NBT_PRE_SILK)) {
            return;
        }
        int vanilla = PlatformHooks.getInt(stack, NBT_PRE_SILK);
        Map<Enchantment, Integer> enchants = new HashMap<>(EnchantmentHelper.getEnchantments(stack));
        if (vanilla > 0) {
            enchants.put(Enchantments.SILK_TOUCH, vanilla);
        } else {
            enchants.remove(Enchantments.SILK_TOUCH);
        }
        EnchantmentHelper.setEnchantments(enchants, stack);
        PlatformHooks.remove(stack, NBT_PRE_SILK);
    }

    private ItemStack getToolForDrops(ItemStack stack) {
        ItemStack tool = stack.copy();
        if (!isSilkTouchEnabled(stack)) {
            return tool;
        }
        Map<Enchantment, Integer> enchants = new HashMap<>(EnchantmentHelper.getEnchantments(stack));
        int silk = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, stack);
        enchants.put(Enchantments.SILK_TOUCH, Math.max(silk, 1));
        EnchantmentHelper.setEnchantments(enchants, tool);
        return tool;
    }

    private void playToggleSound(Player player, boolean enable) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS,
                0.7F, enable ? 1.2F : 0.8F);
    }

    private boolean isVeinMinerEnabled(ItemStack stack) {
        return PlatformHooks.hasItemTag(stack) && PlatformHooks.getBoolean(stack, NBT_VEIN_MINER);
    }

    private boolean isSilkTouchEnabled(ItemStack stack) {
        return PlatformHooks.hasItemTag(stack) && PlatformHooks.getBoolean(stack, NBT_SILK_TOUCH);
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
            Block.dropResources(blockState, level, pos, level.getBlockEntity(pos), entity, getToolForDrops(stack));
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
