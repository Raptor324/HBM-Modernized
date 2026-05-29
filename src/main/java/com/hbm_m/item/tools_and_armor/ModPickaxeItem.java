package com.hbm_m.item.tools_and_armor;

import com.hbm_m.client.overlay.OverlayInfoToast;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class ModPickaxeItem extends PickaxeItem {
    private static final String NBT_VEIN_MINER = "VeinMinerEnabled";
    private static final String NBT_AOE = "AOEEnabled";
    private static final String NBT_AOE_LEVEL = "AOELevel";
    private static final String NBT_SILK_TOUCH = "SilkTouchEnabled";
    private static final String NBT_FORTUNE = "FortuneEnabled";
    private static final String NBT_PRE_FORTUNE = "PreModeFortune";
    private static final String NBT_PRE_SILK = "PreModeSilk";

    private static final Set<Block> EXCLUDED_BLOCKS = Set.of(
            Blocks.STONE, Blocks.ANDESITE, Blocks.DIORITE, Blocks.GRANITE,
            Blocks.GRAVEL, Blocks.DIRT, Blocks.COARSE_DIRT,
            Blocks.SAND, Blocks.RED_SAND,
            Blocks.DEEPSLATE, Blocks.NETHERRACK, Blocks.END_STONE
    );

    private final int veinMinerLevel;
    private final int aoeLevel;
    private final int silkTouchLevel;
    private final int fortuneLevel;

    public ModPickaxeItem(Tier tier, int attackDamage, float attackSpeed, Properties properties,
                          int veinMinerLevel, int aoeLevel, int silkTouchLevel, int fortuneLevel) {
        super(tier, attackDamage, attackSpeed, properties);
        this.veinMinerLevel = Math.max(0, Math.min(6, veinMinerLevel));
        this.aoeLevel = Math.max(0, Math.min(3, aoeLevel));
        this.silkTouchLevel = Math.max(0, Math.min(1, silkTouchLevel));
        this.fortuneLevel = Math.max(0, Math.min(5, fortuneLevel));
    }

    public ModPickaxeItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        this(tier, attackDamage, attackSpeed, properties, 0, 0, 0, 0);
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

        // AOE - показываем все уровни вплоть до максимального
        if (aoeLevel > 0) {
            for (int i = 1; i <= aoeLevel; i++) {
                // Проверяем, активен ли этот уровень сейчас
                boolean isActive = isAOEEnabled(stack) && getAOELevelNBT(stack) == i;
                ChatFormatting color = isActive ? ChatFormatting.YELLOW : ChatFormatting.GOLD;

                // Теперь передаем в перевод только переменную 'i' (сам уровень)
                tooltip.add(Component.literal(" ")
                        .append(Component.translatable("tooltip.hbm_m.aoe", i)
                                .withStyle(color)));
            }
        }

        // Silk Touch
        if (silkTouchLevel > 0) {
            boolean isActive = isSilkTouchEnabled(stack);
            ChatFormatting color = isActive ? ChatFormatting.YELLOW : ChatFormatting.GOLD;
            tooltip.add(Component.literal("  ")
                    .append(Component.translatable("tooltip.hbm_m.silk_touch")
                            .withStyle(color)));
        }

        // Fortune
        if (fortuneLevel > 0) {
            boolean isActive = isFortuneEnabled(stack);
            ChatFormatting color = isActive ? ChatFormatting.YELLOW : ChatFormatting.GOLD;
            tooltip.add(Component.literal("  ")
                    .append(Component.translatable("tooltip.hbm_m.fortune", fortuneLevel)
                            .withStyle(color)));
        }

        // Инструкции по использованию
        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable("tooltip.hbm_m.right_click").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.hbm_m.shift_right_click").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return super.isFoil(stack) || isVeinMinerEnabled(stack) || isAOEEnabled(stack)
                || isSilkTouchEnabled(stack) || isFortuneEnabled(stack);
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
        boolean anyActive = isVeinMinerEnabled(stack) || isAOEEnabled(stack) ||
                isSilkTouchEnabled(stack) || isFortuneEnabled(stack);

        if (!anyActive) {
            if (veinMinerLevel > 0) {
                feedback = toggleVeinMiner(stack, player, true, apply);
            } else if (aoeLevel > 0) {
                feedback = toggleAOE(stack, player, 1, apply);
            } else if (silkTouchLevel > 0) {
                feedback = toggleSilkTouch(stack, player, true, apply);
            } else if (fortuneLevel > 0) {
                feedback = toggleFortune(stack, player, true, apply);
            }
        } else if (isVeinMinerEnabled(stack)) {
            feedback = toggleVeinMiner(stack, player, false, apply);
            if (aoeLevel > 0) {
                feedback = toggleAOE(stack, player, 1, apply);
            } else if (silkTouchLevel > 0) {
                feedback = toggleSilkTouch(stack, player, true, apply);
            } else if (fortuneLevel > 0) {
                feedback = toggleFortune(stack, player, true, apply);
            } else {
                feedback = disableAllAbilities(stack, player, apply);
            }
        } else if (isAOEEnabled(stack)) {
            int currentAOELevel = getAOELevelNBT(stack);
            if (currentAOELevel < aoeLevel) {
                feedback = toggleAOE(stack, player, currentAOELevel + 1, apply);
            } else {
                toggleAOE(stack, player, 0, apply);
                if (silkTouchLevel > 0) {
                    feedback = toggleSilkTouch(stack, player, true, apply);
                } else if (fortuneLevel > 0) {
                    feedback = toggleFortune(stack, player, true, apply);
                } else {
                    feedback = disableAllAbilities(stack, player, apply);
                }
            }
        } else if (isSilkTouchEnabled(stack)) {
            feedback = toggleSilkTouch(stack, player, false, apply);
            if (fortuneLevel > 0) {
                feedback = toggleFortune(stack, player, true, apply);
            } else {
                feedback = disableAllAbilities(stack, player, apply);
            }
        } else if (isFortuneEnabled(stack)) {
            feedback = disableAllAbilities(stack, player, apply);
        }
        return feedback;
    }

    private ModeFeedback disableAllAbilities(ItemStack stack, Player player, boolean apply) {
        if (apply) {
            stack.getOrCreateTag().putBoolean(NBT_VEIN_MINER, false);
            stack.getOrCreateTag().putBoolean(NBT_AOE, false);
            stack.getOrCreateTag().putBoolean(NBT_SILK_TOUCH, false);
            stack.getOrCreateTag().putBoolean(NBT_FORTUNE, false);
            clearModeFortune(stack);
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
        } else if (aoeLevel > 0 && isAOEEnabled(stack)) {
            int currentAOELevel = getAOELevelNBT(stack);
            int size = 1 + (currentAOELevel * 2);
            aoeMine(level, pos, stack, entity, size);
        }

        return result;
    }

    private ModeFeedback toggleVeinMiner(ItemStack stack, Player player, boolean enable, boolean apply) {
        if (apply) {
            stack.getOrCreateTag().putBoolean(NBT_VEIN_MINER, enable);
            if (enable) {
                stack.getOrCreateTag().putBoolean(NBT_AOE, false);
                stack.getOrCreateTag().putBoolean(NBT_SILK_TOUCH, false);
                stack.getOrCreateTag().putBoolean(NBT_FORTUNE, false);
                clearModeSilkTouch(stack);
                clearModeFortune(stack);
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

    private ModeFeedback toggleAOE(ItemStack stack, Player player, int level, boolean apply) {
        if (level > 0) {
            if (apply) {
                stack.getOrCreateTag().putBoolean(NBT_AOE, true);
                stack.getOrCreateTag().putInt(NBT_AOE_LEVEL, level);
                stack.getOrCreateTag().putBoolean(NBT_VEIN_MINER, false);
                stack.getOrCreateTag().putBoolean(NBT_SILK_TOUCH, false);
                stack.getOrCreateTag().putBoolean(NBT_FORTUNE, false);
                clearModeSilkTouch(stack);
                clearModeFortune(stack);
                playToggleSound(player, true);
            }
            int size = 1 + (level * 2);
            return new ModeFeedback(
                    Component.translatable("message.hbm_m.aoe.enabled", size).withStyle(ChatFormatting.YELLOW),
                    ChatFormatting.YELLOW
            );
        }
        if (apply) {
            stack.getOrCreateTag().putBoolean(NBT_AOE, false);
            stack.getOrCreateTag().putInt(NBT_AOE_LEVEL, 0);
        }
        return null;
    }

    private ModeFeedback toggleSilkTouch(ItemStack stack, Player player, boolean enable, boolean apply) {
        if (apply) {
            stack.getOrCreateTag().putBoolean(NBT_SILK_TOUCH, enable);
            if (enable) {
                stack.getOrCreateTag().putBoolean(NBT_VEIN_MINER, false);
                stack.getOrCreateTag().putBoolean(NBT_AOE, false);
                stack.getOrCreateTag().putBoolean(NBT_FORTUNE, false);
                clearModeFortune(stack);
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

    private ModeFeedback toggleFortune(ItemStack stack, Player player, boolean enable, boolean apply) {
        if (apply) {
            stack.getOrCreateTag().putBoolean(NBT_FORTUNE, enable);
            if (enable) {
                stack.getOrCreateTag().putBoolean(NBT_VEIN_MINER, false);
                stack.getOrCreateTag().putBoolean(NBT_AOE, false);
                stack.getOrCreateTag().putBoolean(NBT_SILK_TOUCH, false);
                clearModeSilkTouch(stack);
                applyModeFortune(stack);
            } else {
                clearModeFortune(stack);
            }
            playToggleSound(player, enable);
        }
        ChatFormatting color = enable ? ChatFormatting.YELLOW : ChatFormatting.RED;
        return new ModeFeedback(
                Component.translatable(
                        enable ? "message.hbm_m.fortune.enabled" : "message.hbm_m.fortune.disabled",
                        fortuneLevel
                ).withStyle(color),
                color
        );
    }

    private void applyModeFortune(ItemStack stack) {
        int vanilla = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, stack);
        stack.getOrCreateTag().putInt(NBT_PRE_FORTUNE, vanilla);
        Map<Enchantment, Integer> enchants = new HashMap<>(EnchantmentHelper.getEnchantments(stack));
        enchants.put(Enchantments.BLOCK_FORTUNE, Math.max(vanilla, fortuneLevel));
        EnchantmentHelper.setEnchantments(enchants, stack);
    }

    private void clearModeFortune(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(NBT_PRE_FORTUNE)) {
            return;
        }
        int vanilla = stack.getTag().getInt(NBT_PRE_FORTUNE);
        Map<Enchantment, Integer> enchants = new HashMap<>(EnchantmentHelper.getEnchantments(stack));
        if (vanilla > 0) {
            enchants.put(Enchantments.BLOCK_FORTUNE, vanilla);
        } else {
            enchants.remove(Enchantments.BLOCK_FORTUNE);
        }
        EnchantmentHelper.setEnchantments(enchants, stack);
        stack.getTag().remove(NBT_PRE_FORTUNE);
    }

    private void applyModeSilkTouch(ItemStack stack) {
        int vanilla = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, stack);
        stack.getOrCreateTag().putInt(NBT_PRE_SILK, vanilla);
        Map<Enchantment, Integer> enchants = new HashMap<>(EnchantmentHelper.getEnchantments(stack));
        enchants.put(Enchantments.SILK_TOUCH, Math.max(vanilla, 1));
        EnchantmentHelper.setEnchantments(enchants, stack);
    }

    private void clearModeSilkTouch(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(NBT_PRE_SILK)) {
            return;
        }
        int vanilla = stack.getTag().getInt(NBT_PRE_SILK);
        Map<Enchantment, Integer> enchants = new HashMap<>(EnchantmentHelper.getEnchantments(stack));
        if (vanilla > 0) {
            enchants.put(Enchantments.SILK_TOUCH, vanilla);
        } else {
            enchants.remove(Enchantments.SILK_TOUCH);
        }
        EnchantmentHelper.setEnchantments(enchants, stack);
        stack.getTag().remove(NBT_PRE_SILK);
    }

    private int getEffectiveFortune(ItemStack stack) {
        int vanilla = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, stack);
        if (isFortuneEnabled(stack)) {
            return Math.max(vanilla, fortuneLevel);
        }
        return vanilla;
    }

    private ItemStack getToolForDrops(ItemStack stack) {
        ItemStack tool = stack.copy();
        Map<Enchantment, Integer> enchants = new HashMap<>(EnchantmentHelper.getEnchantments(stack));
        if (isFortuneEnabled(stack)) {
            enchants.put(Enchantments.BLOCK_FORTUNE, getEffectiveFortune(stack));
        }
        if (isSilkTouchEnabled(stack)) {
            int silk = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, stack);
            enchants.put(Enchantments.SILK_TOUCH, Math.max(silk, 1));
        }
        EnchantmentHelper.setEnchantments(enchants, tool);
        return tool;
    }

    private void playToggleSound(Player player, boolean enable) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS,
                0.7F, enable ? 1.2F : 0.8F);
    }

    private boolean isVeinMinerEnabled(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(NBT_VEIN_MINER);
    }

    private boolean isAOEEnabled(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(NBT_AOE);
    }

    private boolean isSilkTouchEnabled(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(NBT_SILK_TOUCH);
    }

    private boolean isFortuneEnabled(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(NBT_FORTUNE);
    }

    private int getAOELevelNBT(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt(NBT_AOE_LEVEL) : 0;
    }

    private void veinMine(Level level, BlockPos startPos, Block targetBlock, ItemStack stack, LivingEntity entity, int radius) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> toCheck = new LinkedList<>();
        Set<BlockPos> toMine = new HashSet<>();

        toCheck.add(startPos);
        visited.add(startPos);

        while (!toCheck.isEmpty() && toMine.size() < 64) {
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
            mineBlockWithFortune(level, pos, stack, entity);
            if (stack.isEmpty()) break;
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

    private void aoeMine(Level level, BlockPos centerPos, ItemStack stack, LivingEntity entity, int size) {
        int halfSize = size / 2;

        for (int x = -halfSize; x <= halfSize; x++) {
            for (int y = -halfSize; y <= halfSize; y++) {
                for (int z = -halfSize; z <= halfSize; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;

                    BlockPos pos = centerPos.offset(x, y, z);
                    mineBlockWithFortune(level, pos, stack, entity);

                    if (stack.isEmpty()) return;
                }
            }
        }
    }

    private void mineBlockWithFortune(Level level, BlockPos pos, ItemStack stack, LivingEntity entity) {
        BlockState blockState = level.getBlockState(pos);

        if (!stack.isCorrectToolForDrops(blockState)) return;

        if (level instanceof ServerLevel serverLevel) {
            ItemStack toolForDrops = getToolForDrops(stack);
            Block.dropResources(blockState, level, pos, level.getBlockEntity(pos), entity, toolForDrops);

            int effectiveFortune = getEffectiveFortune(stack);
            if (isFortuneEnabled(stack) && effectiveFortune >= 4) {
                LootParams.Builder builder = new LootParams.Builder(serverLevel)
                        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                        .withParameter(LootContextParams.TOOL, toolForDrops)
                        .withOptionalParameter(LootContextParams.BLOCK_ENTITY, serverLevel.getBlockEntity(pos));

                List<ItemStack> drops = blockState.getDrops(builder);

                int bonusDrops = effectiveFortune - 3;
                for (ItemStack drop : drops) {
                    ItemStack bonusDrop = drop.copy();
                    bonusDrop.setCount(bonusDrops);
                    Block.popResource(serverLevel, pos, bonusDrop);
                }
            }

            level.removeBlock(pos, false);
        }

        stack.hurtAndBreak(1, entity, (e) -> {
            e.broadcastBreakEvent(InteractionHand.MAIN_HAND);
        });
    }
}