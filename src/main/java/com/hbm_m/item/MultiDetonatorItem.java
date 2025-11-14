package com.hbm_m.item;

import com.hbm_m.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
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
 * Мульти-детонатор: позволяет сохранять до 4 точек детонации
 * Каждая точка может быть названа (макс. 16 символов)
 * При нажатии R открывается GUI для выбора активной точки
 * При ПКМ (без Shift) активирует текущую выбранную точку
 */
public class MultiDetonatorItem extends Item {

    // NBT константы для основных данных
    private static final String NBT_ACTIVE_POINT = "ActivePoint"; // 0-3
    private static final String NBT_POINTS_TAG = "Points"; // Список точек

    // Константы для каждой точки в ListTag
    private static final String NBT_POINT_X = "X";
    private static final String NBT_POINT_Y = "Y";
    private static final String NBT_POINT_Z = "Z";
    private static final String NBT_POINT_NAME = "Name";
    private static final String NBT_POINT_HAS_TARGET = "HasTarget";

    private static final int MAX_POINTS = 4;
    private static final int MAX_NAME_LENGTH = 16;

    // Класс для хранения данных точки
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

            int activePoint = nbt.getInt(NBT_ACTIVE_POINT);
            if (activePoint < 0 || activePoint >= MAX_POINTS) {
                activePoint = 0;
            }

            if (!nbt.contains(NBT_POINTS_TAG, Tag.TAG_LIST)) {
                nbt.put(NBT_POINTS_TAG, new ListTag());
            }

            ListTag pointsList = nbt.getList(NBT_POINTS_TAG, Tag.TAG_COMPOUND);

            // ⭐ Расширяем список, сохраняя существующие точки
            while (pointsList.size() <= activePoint) {
                CompoundTag newPointTag = createEmptyPointTag();
                pointsList.add(newPointTag);
            }

            // ⭐ ГЛАВНОЕ: Получаем существующую точку и сохраняем ЕЁ ИМЯ
            CompoundTag pointTag = pointsList.getCompound(activePoint);
            String savedName = pointTag.getString(NBT_POINT_NAME);

            // Обновляем координаты
            pointTag.putInt(NBT_POINT_X, pos.getX());
            pointTag.putInt(NBT_POINT_Y, pos.getY());
            pointTag.putInt(NBT_POINT_Z, pos.getZ());
            pointTag.putBoolean(NBT_POINT_HAS_TARGET, true);

            // ⭐ КРИТИЧНО: Возвращаем сохранённое имя
            pointTag.putString(NBT_POINT_NAME, savedName.isEmpty() ?
                    "Point " + (activePoint + 1) : savedName);

            pointsList.set(activePoint, pointTag);
            nbt.put(NBT_POINTS_TAG, pointsList);

            if (!level.isClientSide) {
                String finalName = pointTag.getString(NBT_POINT_NAME);
                player.displayClientMessage(
                        Component.literal("Позиция '" + finalName + "' сохранена: "
                                        + pos.getX() + ", " + pos.getY() + ", " + pos.getZ())
                                .withStyle(ChatFormatting.GREEN),
                        true
                );

                if (ModSounds.TOOL_TECH_BOOP.isPresent()) {
                    SoundEvent soundEvent = ModSounds.TOOL_TECH_BOOP.get();
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            soundEvent, player.getSoundSource(), 1.0F, 1.0F);
                }
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }


    /**
     * use: активация текущей выбранной точки при ПКМ в воздухе (без Shift)
     * GUI открывается при нажатии клавиши R
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
                player.displayClientMessage(
                        Component.literal("Нет заданных координат!")
                                .withStyle(ChatFormatting.RED),
                        true
                );

                if (ModSounds.TOOL_TECH_BOOP.isPresent()) {
                    SoundEvent soundEvent = ModSounds.TOOL_TECH_BOOP.get();
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), soundEvent, player.getSoundSource(), 1.0F, 1.0F);
                }

                return InteractionResultHolder.fail(stack);
            }

            CompoundTag nbt = stack.getTag();
            int activePoint = nbt.getInt(NBT_ACTIVE_POINT);

            if (activePoint >= MAX_POINTS) {
                activePoint = 0;
            }

            PointData pointData = getPointData(stack, activePoint);

            if (pointData == null || !pointData.hasTarget) {
                player.displayClientMessage(
                        Component.literal("Точка " + (activePoint + 1) + " не установлена!")
                                .withStyle(ChatFormatting.RED),
                        true
                );

                if (ModSounds.TOOL_TECH_BOOP.isPresent()) {
                    SoundEvent soundEvent = ModSounds.TOOL_TECH_BOOP.get();
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), soundEvent, player.getSoundSource(), 1.0F, 1.0F);
                }

                return InteractionResultHolder.fail(stack);
            }

            BlockPos targetPos = new BlockPos(pointData.x, pointData.y, pointData.z);

            if (!level.isLoaded(targetPos)) {
                player.displayClientMessage(
                        Component.literal("Позиция не загружена!")
                                .withStyle(ChatFormatting.RED),
                        true
                );

                if (ModSounds.TOOL_TECH_BOOP.isPresent()) {
                    SoundEvent soundEvent = ModSounds.TOOL_TECH_BOOP.get();
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), soundEvent, player.getSoundSource(), 1.0F, 1.0F);
                }

                return InteractionResultHolder.fail(stack);
            }

            BlockState state = level.getBlockState(targetPos);
            Block block = state.getBlock();

            if (block instanceof IDetonatable) {
                IDetonatable detonatable = (IDetonatable) block;

                try {
                    boolean success = detonatable.onDetonate(level, targetPos, state, player);

                    if (success) {
                        // ⭐ ИСПРАВЛЕНО: Теперь выводим ИМЯ точки в сообщение при детонации
                        player.displayClientMessage(
                                Component.literal(pointData.name + " активирован!")
                                        .withStyle(ChatFormatting.GREEN),
                                true
                        );

                        if (ModSounds.TOOL_TECH_BLEEP.isPresent()) {
                            SoundEvent soundEvent = ModSounds.TOOL_TECH_BLEEP.get();
                            level.playSound(null, player.getX(), player.getY(), player.getZ(), soundEvent, player.getSoundSource(), 1.0F, 1.0F);
                        }
                    }
                } catch (Exception e) {
                    player.displayClientMessage(
                            Component.literal("Ошибка при активации!")
                                    .withStyle(ChatFormatting.RED),
                            true
                    );

                    if (ModSounds.TOOL_TECH_BOOP.isPresent()) {
                        SoundEvent soundEvent = ModSounds.TOOL_TECH_BOOP.get();
                        level.playSound(null, player.getX(), player.getY(), player.getZ(), soundEvent, player.getSoundSource(), 1.0F, 1.0F);
                    }

                    e.printStackTrace();
                }
            } else {
                player.displayClientMessage(
                        Component.literal("Блок несовместим!")
                                .withStyle(ChatFormatting.RED),
                        true
                );

                if (ModSounds.TOOL_TECH_BOOP.isPresent()) {
                    SoundEvent soundEvent = ModSounds.TOOL_TECH_BOOP.get();
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), soundEvent, player.getSoundSource(), 1.0F, 1.0F);
                }
            }
        }

        return InteractionResultHolder.success(stack);
    }

    // ====== Методы для работы с NBT данными ======

    /**
     * Получить данные точки по индексу
     */
    public PointData getPointData(ItemStack stack, int pointIndex) {
        if (!stack.hasTag() || pointIndex < 0 || pointIndex >= MAX_POINTS) {
            return null;
        }

        CompoundTag nbt = stack.getTag();

        if (!nbt.contains(NBT_POINTS_TAG, Tag.TAG_LIST)) {
            return null;
        }

        ListTag pointsList = nbt.getList(NBT_POINTS_TAG, Tag.TAG_COMPOUND);

        if (pointIndex >= pointsList.size()) {
            return null;
        }

        CompoundTag pointTag = pointsList.getCompound(pointIndex);

        if (pointTag.isEmpty()) {
            return null;
        }

        String name = pointTag.getString(NBT_POINT_NAME);

        if (name.isEmpty()) {
            name = "Point " + (pointIndex + 1);
        }

        return new PointData(
                pointTag.getInt(NBT_POINT_X),
                pointTag.getInt(NBT_POINT_Y),
                pointTag.getInt(NBT_POINT_Z),
                name,
                pointTag.getBoolean(NBT_POINT_HAS_TARGET)
        );
    }

    /**
     * Установить активную точку
     */
    public void setActivePoint(ItemStack stack, int pointIndex) {
        if (!stack.hasTag()) {
            stack.setTag(new CompoundTag());
        }

        CompoundTag nbt = stack.getTag();

        if (pointIndex >= 0 && pointIndex < MAX_POINTS) {
            nbt.putInt(NBT_ACTIVE_POINT, pointIndex);
        }
    }

    /**
     * Получить активную точку
     */
    public int getActivePoint(ItemStack stack) {
        if (!stack.hasTag()) {
            return 0;
        }

        int activePoint = stack.getTag().getInt(NBT_ACTIVE_POINT);
        return (activePoint >= 0 && activePoint < MAX_POINTS) ? activePoint : 0;
    }

    /**
     * Установить имя точки
     */
    public void setPointName(ItemStack stack, int pointIndex, String name) {
        if (!stack.hasTag()) {
            stack.setTag(new CompoundTag());
        }

        if (pointIndex < 0 || pointIndex >= MAX_POINTS) {
            return;
        }

        CompoundTag nbt = stack.getTag();

        // Инициализируем список точек
        if (!nbt.contains(NBT_POINTS_TAG, Tag.TAG_LIST)) {
            nbt.put(NBT_POINTS_TAG, new ListTag());
        }

        ListTag pointsList = nbt.getList(NBT_POINTS_TAG, Tag.TAG_COMPOUND);

        // ⭐ ФИХ: При расширении списка - копируем имена существующих точек
        while (pointsList.size() <= pointIndex) {
            CompoundTag newPointTag = createEmptyPointTag();
            pointsList.add(newPointTag);
        }

        CompoundTag pointTag = pointsList.getCompound(pointIndex);

        // Ограничиваем длину имени
        String limitedName = name.length() > MAX_NAME_LENGTH ?
                name.substring(0, MAX_NAME_LENGTH) : name;

        pointTag.putString(NBT_POINT_NAME, limitedName);
        pointsList.set(pointIndex, pointTag);
        nbt.put(NBT_POINTS_TAG, pointsList);
    }

    /**
     * Очистить точку (только координаты, имя сохраняется!)
     */
    public void clearPoint(ItemStack stack, int pointIndex) {
        if (!stack.hasTag() || pointIndex < 0 || pointIndex >= MAX_POINTS) {
            return;
        }

        CompoundTag nbt = stack.getTag();

        if (!nbt.contains(NBT_POINTS_TAG, Tag.TAG_LIST)) {
            return;
        }

        ListTag pointsList = nbt.getList(NBT_POINTS_TAG, Tag.TAG_COMPOUND);

        if (pointIndex < pointsList.size()) {
            CompoundTag pointTag = pointsList.getCompound(pointIndex);

            // ⭐ РЕШЕНИЕ: Очищаем ТОЛЬКО координаты и флаг hasTarget, оставляем имя!
            String savedName = pointTag.getString(NBT_POINT_NAME);

            // Создаём очищенный тег с сохранённым именем
            CompoundTag clearedTag = new CompoundTag();
            clearedTag.putInt(NBT_POINT_X, 0);
            clearedTag.putInt(NBT_POINT_Y, 0);
            clearedTag.putInt(NBT_POINT_Z, 0);
            clearedTag.putBoolean(NBT_POINT_HAS_TARGET, false);
            clearedTag.putString(NBT_POINT_NAME, savedName); // ⭐ Сохраняем имя!

            pointsList.set(pointIndex, clearedTag);
            nbt.put(NBT_POINTS_TAG, pointsList);
        }
    }

    /**
     * Получить максимум точек
     */
    public int getMaxPoints() {
        return MAX_POINTS;
    }

    /**
     * Создать пустой тег точки
     */
    private static CompoundTag createEmptyPointTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(NBT_POINT_X, 0);
        tag.putInt(NBT_POINT_Y, 0);
        tag.putInt(NBT_POINT_Z, 0);
        tag.putString(NBT_POINT_NAME, "");
        tag.putBoolean(NBT_POINT_HAS_TARGET, false);
        return tag;
    }
}