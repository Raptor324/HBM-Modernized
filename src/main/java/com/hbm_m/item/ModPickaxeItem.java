package com.hbm_m.item;

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
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class ModPickaxeItem extends PickaxeItem {
    private static final String NBT_VEIN_MINER = "VeinMinerEnabled";
    private static final String NBT_AOE = "AOEEnabled";
    private static final String NBT_SILK_TOUCH = "SilkTouchEnabled";
    private static final String NBT_FORTUNE = "FortuneEnabled";

    private static final Set<Block> EXCLUDED_BLOCKS = Set.of(
            Blocks.STONE, Blocks.ANDESITE, Blocks.DIORITE, Blocks.GRANITE,
            Blocks.GRAVEL, Blocks.DIRT, Blocks.COARSE_DIRT,
            Blocks.SAND, Blocks.RED_SAND,
            Blocks.DEEPSLATE, Blocks.NETHERRACK, Blocks.END_STONE
    );

    private final int veinMinerLevel;  // 0-6
    private final int aoeLevel;        // 0-3
    private final int silkTouchLevel;  // 0-1
    private final int fortuneLevel;    // 0-5

    /**
     * Универсальный конструктор для кирки со способностями
     * @param veinMinerLevel - уровень жилкового майнера (0 = нет, 1-6 = радиус 3-8)
     * @param aoeLevel - уровень зоны действия (0 = нет, 1 = 3x3x3, 2 = 5x5x5, 3 = 7x7x7)
     * @param silkTouchLevel - шёлковое касание (0 = нет, 1 = есть)
     * @param fortuneLevel - удача (0 = нет, 1-5 = уровень)
     */
    public ModPickaxeItem(Tier tier, int attackDamage, float attackSpeed, Properties properties,
                          int veinMinerLevel, int aoeLevel, int silkTouchLevel, int fortuneLevel) {
        super(tier, attackDamage, attackSpeed, properties);
        this.veinMinerLevel = Math.max(0, Math.min(6, veinMinerLevel));
        this.aoeLevel = Math.max(0, Math.min(3, aoeLevel));
        this.silkTouchLevel = Math.max(0, Math.min(1, silkTouchLevel));
        this.fortuneLevel = Math.max(0, Math.min(5, fortuneLevel));
    }

    /**
     * Конструктор для обычной кирки без способностей
     */
    public ModPickaxeItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        this(tier, attackDamage, attackSpeed, properties, 0, 0, 0, 0);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && player.isShiftKeyDown()) {
            // Цикл переключения: жилковый -> зона -> шёлк -> обычно
            if (veinMinerLevel > 0 && !isVeinMinerEnabled(stack) && !isAOEEnabled(stack) && !isSilkTouchEnabled(stack)) {
                toggleVeinMiner(stack, player, true);
            } else if (veinMinerLevel > 0 && isVeinMinerEnabled(stack)) {
                toggleVeinMiner(stack, player, false);
                if (aoeLevel > 0) {
                    toggleAOE(stack, player, true);
                }
            } else if (aoeLevel > 0 && isAOEEnabled(stack)) {
                toggleAOE(stack, player, false);
                if (silkTouchLevel > 0) {
                    toggleSilkTouch(stack, player, true);
                }
            } else if (silkTouchLevel > 0 && isSilkTouchEnabled(stack)) {
                toggleSilkTouch(stack, player, false);
            } else {
                // Сброс - включаем первую доступную способность
                if (veinMinerLevel > 0) {
                    toggleVeinMiner(stack, player, true);
                } else if (aoeLevel > 0) {
                    toggleAOE(stack, player, true);
                } else if (silkTouchLevel > 0) {
                    toggleSilkTouch(stack, player, true);
                }
            }

            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        boolean result = super.mineBlock(stack, level, state, pos, entity);

        if (level.isClientSide()) return result;

        Block minedBlock = state.getBlock();

        // Жилковый майнер
        if (veinMinerLevel > 0 && isVeinMinerEnabled(stack) && !EXCLUDED_BLOCKS.contains(minedBlock)) {
            int radius = 2 + veinMinerLevel; // Уровень 1 = радиус 3, уровень 6 = радиус 8
            veinMine(level, pos, minedBlock, stack, entity, radius);
        }
        // Зона действия (AOE)
        else if (aoeLevel > 0 && isAOEEnabled(stack)) {
            int size = 1 + (aoeLevel * 2); // Уровень 1 = 3, уровень 2 = 5, уровень 3 = 7
            aoeMine(level, pos, stack, entity, size);
        }

        return result;
    }

    // ============================================
    // ПЕРЕКЛЮЧЕНИЕ РЕЖИМОВ
    // ============================================

    private void toggleVeinMiner(ItemStack stack, Player player, boolean enable) {
        stack.getOrCreateTag().putBoolean(NBT_VEIN_MINER, enable);
        if (enable) {
            stack.getOrCreateTag().putBoolean(NBT_AOE, false);
            stack.getOrCreateTag().putBoolean(NBT_SILK_TOUCH, false);
        }
        playToggleSound(player, enable);
        player.displayClientMessage(
                Component.literal((enable ? "§e" : "§6") + "\"Жилковый майнер " + veinMinerLevel + "\" " +
                        (enable ? "активирован!" : "деактивирован!")),
                true
        );
    }

    private void toggleAOE(ItemStack stack, Player player, boolean enable) {
        stack.getOrCreateTag().putBoolean(NBT_AOE, enable);
        if (enable) {
            stack.getOrCreateTag().putBoolean(NBT_VEIN_MINER, false);
            stack.getOrCreateTag().putBoolean(NBT_SILK_TOUCH, false);
        }
        playToggleSound(player, enable);
        int size = 1 + (aoeLevel * 2);
        player.displayClientMessage(
                Component.literal((enable ? "§e" : "§6") + "\"Зона действия " + size + "x" + size + "x" + size + "\" " +
                        (enable ? "активирована!" : "деактивирована!")),
                true
        );
    }

    private void toggleSilkTouch(ItemStack stack, Player player, boolean enable) {
        stack.getOrCreateTag().putBoolean(NBT_SILK_TOUCH, enable);
        if (enable) {
            stack.getOrCreateTag().putBoolean(NBT_VEIN_MINER, false);
            stack.getOrCreateTag().putBoolean(NBT_AOE, false);
        }
        playToggleSound(player, enable);
        player.displayClientMessage(
                Component.literal((enable ? "§e" : "§6") + "\"Шёлковое касание\" " +
                        (enable ? "активировано!" : "деактивировано!")),
                true
        );
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

    // ============================================
    // ЖИЛКОВЫЙ МАЙНЕР
    // ============================================

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
            mineBlockWithEnchantments(level, pos, stack, entity);
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

    // ============================================
    // ЗОНА ДЕЙСТВИЯ (AOE)
    // ============================================

    private void aoeMine(Level level, BlockPos centerPos, ItemStack stack, LivingEntity entity, int size) {
        int halfSize = size / 2;

        for (int x = -halfSize; x <= halfSize; x++) {
            for (int y = -halfSize; y <= halfSize; y++) {
                for (int z = -halfSize; z <= halfSize; z++) {
                    if (x == 0 && y == 0 && z == 0) continue; // Пропускаем центральный блок

                    BlockPos pos = centerPos.offset(x, y, z);
                    mineBlockWithEnchantments(level, pos, stack, entity);

                    if (stack.isEmpty()) return;
                }
            }
        }
    }

    // ============================================
    // ДОБЫЧА С УЧЁТОМ ЗАЧАРОВАНИЙ
    // ============================================

    private void mineBlockWithEnchantments(Level level, BlockPos pos, ItemStack stack, LivingEntity entity) {
        BlockState blockState = level.getBlockState(pos);

        if (!stack.isCorrectToolForDrops(blockState)) return;

        // Определяем, использовать ли шёлковое касание
        boolean useSilk = isSilkTouchEnabled(stack) && silkTouchLevel > 0;

        if (useSilk) {
            // Шёлковое касание - дропаем сам блок
            Block.dropResources(blockState, level, pos, level.getBlockEntity(pos), entity, stack);
            level.removeBlock(pos, false);
        } else {
            // Обычная добыча с удачей
            if (level instanceof ServerLevel serverLevel) {
                // Создаём параметры лута с учётом удачи
                LootParams.Builder builder = new LootParams.Builder(serverLevel)
                        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                        .withParameter(LootContextParams.TOOL, stack)
                        .withOptionalParameter(LootContextParams.BLOCK_ENTITY, serverLevel.getBlockEntity(pos));

                // Применяем удачу (базовая + дополнительная)
                int totalFortune = fortuneLevel;
                List<ItemStack> drops = blockState.getDrops(builder);

                // Для уровней удачи 4-5 увеличиваем количество дропа
                if (totalFortune >= 4) {
                    for (ItemStack drop : drops) {
                        drop.setCount(drop.getCount() + (totalFortune - 3));
                    }
                }

                for (ItemStack drop : drops) {
                    Block.popResource(serverLevel, pos, drop);
                }
            }
            level.removeBlock(pos, false);
        }

        // Урон инструменту
        stack.hurtAndBreak(1, entity, (e) -> {
            e.broadcastBreakEvent(InteractionHand.MAIN_HAND);
        });
    }
}