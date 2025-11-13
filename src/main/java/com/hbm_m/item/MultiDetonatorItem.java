package com.hbm_m.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm_m.block.IDetonatable;

/**
 * Мульти-детонатор: позволяет сохранять до 6 точек детонации
 * Каждая точка может быть названа (макс. 16 символов)
 * При нажатии R открывается GUI для выбора активной точки
 * При ПКМ (без Shift) активирует текущую выбранную точку
 */
public class MultiDetonatorItem extends Item {

    // NBT константы для основных данных
    private static final String NBT_ACTIVE_POINT = "ActivePoint"; // 0-5
    private static final String NBT_POINTS_TAG = "Points"; // Список точек

    // Константы для каждой точки в ListTag
    private static final String NBT_POINT_X = "X";
    private static final String NBT_POINT_Y = "Y";
    private static final String NBT_POINT_Z = "Z";
    private static final String NBT_POINT_NAME = "Name";
    private static final String NBT_POINT_HAS_TARGET = "HasTarget";

    private static final int MAX_POINTS = 6;
    private static final int MAX_NAME_LENGTH = 16;

    public MultiDetonatorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /**
     * useOn: сохранение позиции в текущую активную точку (при Shift+ПКМ на блок)
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player == null) {
            return InteractionResult.PASS;
        }

        // Только при приседании сохраняем позицию
        if (player.isCrouching()) {
            if (!stack.hasTag()) {
                stack.setTag(new CompoundTag());
            }

            CompoundTag nbt = stack.getTag();

            // Получаем активную точку (по умолчанию 0)
            int activePoint = nbt.getInt(NBT_ACTIVE_POINT);
            if (activePoint >= MAX_POINTS) {
                activePoint = 0;
            }

            // Инициализируем список точек, если его еще нет
            if (!nbt.contains(NBT_POINTS_TAG, Tag.TAG_LIST)) {
                nbt.put(NBT_POINTS_TAG, new ListTag());
            }

            ListTag pointsList = nbt.getList(NBT_POINTS_TAG, Tag.TAG_COMPOUND);

            // Расширяем список до нужного размера
            while (pointsList.size() <= activePoint) {
                pointsList.add(createEmptyPointTag());
            }

            // Получаем текущую точку и обновляем координаты
            CompoundTag pointTag = pointsList.getCompound(activePoint);
            pointTag.putInt(NBT_POINT_X, pos.getX());
            pointTag.putInt(NBT_POINT_Y, pos.getY());
            pointTag.putInt(NBT_POINT_Z, pos.getZ());
            pointTag.putBoolean(NBT_POINT_HAS_TARGET, true);

            // Если имя пусто, генерируем дефолтное
            if (!pointTag.contains(NBT_POINT_NAME)) {
                pointTag.putString(NBT_POINT_NAME, "Point " + (activePoint + 1));
            }

            pointsList.set(activePoint, pointTag);
            nbt.put(NBT_POINTS_TAG, pointsList);

            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.literal("Позиция сохранена в точку " + (activePoint + 1) + ": "
                                        + pos.getX() + ", " + pos.getY() + ", " + pos.getZ())
                                .withStyle(ChatFormatting.GREEN),
                        true
                );
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    /**
     * use: активация текущей выбранной точки при ПКМ в воздухе (без Shift)
     * GUI открывается при нажатии клавиши R (см. MultiDetonatorKeyInputHandler)
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isCrouching()) {
            // При приседании на use - ничего не делаем (это обрабатывается useOn)
            return InteractionResultHolder.pass(stack);
        }

        // На сервере: активируем текущую выбранную точку при ПКМ в воздухе
        if (!level.isClientSide) {
            if (!stack.hasTag()) {
                if (!level.isClientSide) {
                    player.displayClientMessage(
                            Component.literal("Нет сохраненных точек!")
                                    .withStyle(ChatFormatting.RED),
                            true
                    );
                }
                return InteractionResultHolder.fail(stack);
            }

            CompoundTag nbt = stack.getTag();
            int activePoint = nbt.getInt(NBT_ACTIVE_POINT);

            if (!nbt.contains(NBT_POINTS_TAG, Tag.TAG_LIST)) {
                player.displayClientMessage(
                        Component.literal("Нет сохраненных точек!")
                                .withStyle(ChatFormatting.RED),
                        true
                );
                return InteractionResultHolder.fail(stack);
            }

            ListTag pointsList = nbt.getList(NBT_POINTS_TAG, Tag.TAG_COMPOUND);

            if (activePoint >= pointsList.size()) {
                player.displayClientMessage(
                        Component.literal("Неверная точка!")
                                .withStyle(ChatFormatting.RED),
                        true
                );
                return InteractionResultHolder.fail(stack);
            }

            CompoundTag pointTag = pointsList.getCompound(activePoint);

            if (!pointTag.getBoolean(NBT_POINT_HAS_TARGET)) {
                player.displayClientMessage(
                        Component.literal("Точка " + (activePoint + 1) + " не установлена!")
                                .withStyle(ChatFormatting.RED),
                        true
                );
                return InteractionResultHolder.fail(stack);
            }

            // Получаем координаты точки и активируем ее
            int x = pointTag.getInt(NBT_POINT_X);
            int y = pointTag.getInt(NBT_POINT_Y);
            int z = pointTag.getInt(NBT_POINT_Z);
            BlockPos targetPos = new BlockPos(x, y, z);

            // Проверяем, загружен ли чанк
            if (!level.isLoaded(targetPos)) {
                player.displayClientMessage(
                        Component.literal("Позиция не загружена!")
                                .withStyle(ChatFormatting.RED),
                        true
                );
                return InteractionResultHolder.fail(stack);
            }

            BlockState state = level.getBlockState(targetPos);
            Block block = state.getBlock();

            // Проверяем поддержку IDetonatable
            if (block instanceof IDetonatable) {
                IDetonatable detonatable = (IDetonatable) block;
                boolean success = detonatable.onDetonate(level, targetPos, state, player);

                if (success) {
                    player.displayClientMessage(
                            Component.literal("Точка " + (activePoint + 1) + " успешно активирована!")
                                    .withStyle(ChatFormatting.GREEN),
                            true
                    );
                    return InteractionResultHolder.success(stack);
                } else {
                    player.displayClientMessage(
                            Component.literal("Блок не поддерживает детонацию или не готов!")
                                    .withStyle(ChatFormatting.RED),
                            true
                    );
                    return InteractionResultHolder.fail(stack);
                }
            } else {
                player.displayClientMessage(
                        Component.literal("На позиции нет совместимого блока!")
                                .withStyle(ChatFormatting.RED),
                        true
                );
                return InteractionResultHolder.fail(stack);
            }
        }

        return InteractionResultHolder.pass(stack);
    }

    /**
     * Получить текущую активную точку
     */
    public int getActivePoint(ItemStack stack) {
        if (!stack.hasTag()) return 0;
        return stack.getTag().getInt(NBT_ACTIVE_POINT);
    }

    /**
     * Установить активную точку
     */
    public void setActivePoint(ItemStack stack, int pointIndex) {
        if (pointIndex < 0 || pointIndex >= MAX_POINTS) return;

        if (!stack.hasTag()) {
            stack.setTag(new CompoundTag());
        }

        stack.getTag().putInt(NBT_ACTIVE_POINT, pointIndex);
    }

    /**
     * Получить информацию о точке (x, y, z, имя, hasTarget)
     */
    public PointData getPointData(ItemStack stack, int pointIndex) {
        if (pointIndex < 0 || pointIndex >= MAX_POINTS) return null;

        if (!stack.hasTag()) return null;

        CompoundTag nbt = stack.getTag();
        if (!nbt.contains(NBT_POINTS_TAG, Tag.TAG_LIST)) return null;

        ListTag pointsList = nbt.getList(NBT_POINTS_TAG, Tag.TAG_COMPOUND);
        if (pointIndex >= pointsList.size()) return null;

        CompoundTag pointTag = pointsList.getCompound(pointIndex);

        return new PointData(
                pointTag.getInt(NBT_POINT_X),
                pointTag.getInt(NBT_POINT_Y),
                pointTag.getInt(NBT_POINT_Z),
                pointTag.getString(NBT_POINT_NAME),
                pointTag.getBoolean(NBT_POINT_HAS_TARGET)
        );
    }

    /**
     * Установить имя точки
     */
    public void setPointName(ItemStack stack, int pointIndex, String name) {
        if (pointIndex < 0 || pointIndex >= MAX_POINTS) return;
        if (name.length() > MAX_NAME_LENGTH) {
            name = name.substring(0, MAX_NAME_LENGTH);
        }

        if (!stack.hasTag()) {
            stack.setTag(new CompoundTag());
        }

        CompoundTag nbt = stack.getTag();
        if (!nbt.contains(NBT_POINTS_TAG, Tag.TAG_LIST)) {
            nbt.put(NBT_POINTS_TAG, new ListTag());
        }

        ListTag pointsList = nbt.getList(NBT_POINTS_TAG, Tag.TAG_COMPOUND);

        // Расширяем список до нужного размера
        while (pointsList.size() <= pointIndex) {
            pointsList.add(createEmptyPointTag());
        }

        CompoundTag pointTag = pointsList.getCompound(pointIndex);
        pointTag.putString(NBT_POINT_NAME, name);
        pointsList.set(pointIndex, pointTag);
        nbt.put(NBT_POINTS_TAG, pointsList);
    }

    /**
     * Очистить точку
     */
    public void clearPoint(ItemStack stack, int pointIndex) {
        if (pointIndex < 0 || pointIndex >= MAX_POINTS) return;

        if (!stack.hasTag()) return;

        CompoundTag nbt = stack.getTag();
        if (!nbt.contains(NBT_POINTS_TAG, Tag.TAG_LIST)) return;

        ListTag pointsList = nbt.getList(NBT_POINTS_TAG, Tag.TAG_COMPOUND);
        if (pointIndex >= pointsList.size()) return;

        CompoundTag pointTag = pointsList.getCompound(pointIndex);
        pointTag.putBoolean(NBT_POINT_HAS_TARGET, false);
        pointTag.putString(NBT_POINT_NAME, "Point " + (pointIndex + 1));
        pointsList.set(pointIndex, pointTag);
        nbt.put(NBT_POINTS_TAG, pointsList);
    }

    /**
     * Вспомогательный метод: создание пустого тега точки
     */
    private static CompoundTag createEmptyPointTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(NBT_POINT_X, 0);
        tag.putInt(NBT_POINT_Y, 0);
        tag.putInt(NBT_POINT_Z, 0);
        tag.putString(NBT_POINT_NAME, "Point");
        tag.putBoolean(NBT_POINT_HAS_TARGET, false);
        return tag;
    }

    /**
     * Структура данных для хранения информации о точке
     */
    public static class PointData {
        public int x, y, z;
        public String name;
        public boolean hasTarget;

        public PointData(int x, int y, int z, String name, boolean hasTarget) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.name = name;
            this.hasTarget = hasTarget;
        }
    }
}