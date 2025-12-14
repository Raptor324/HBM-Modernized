package com.hbm_m.block.custom.machines.armormod.item;

// Это базовый класс для всех предметов-модификаций брони.
import com.google.common.collect.Multimap;
import com.hbm_m.block.custom.machines.armormod.util.ArmorModificationHelper;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class ItemArmorMod extends Item {

    // Тип слота, к которому привязан мод (0-8)
    public final int type;
    // К каким типам брони подходит

    public ItemArmorMod(Properties pProperties, int type) {
        super(pProperties.stacksTo(1));
        this.type = type;
    }

    /**
     * Этот метод должны переопределять моды, дающие атрибуты.
     * @param armor Стак брони, на который ставится мод.
     * @return Multimap с атрибутами.
     */
    @Nullable
    public Multimap<Attribute, AttributeModifier> getModifiers(ItemStack armor) {
        return null;
    }

    /**
     * МЕТОД, который должны переопределять дочерние классы.
     * Он возвращает ТОЛЬКО строки, описывающие эффекты мода.
     * @return Список компонентов с описанием эффектов.
     */
    public List<Component> getEffectTooltipLines() {
        // По умолчанию моды не имеют эффектов.
        return Collections.emptyList();
    }
    
    @Override
    public void appendHoverText(@Nonnull ItemStack pStack, @Nullable Level pLevel, @Nonnull List<Component> pTooltip, @Nonnull TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pLevel, pTooltip, pIsAdvanced);
        // Добавляем строки с эффектами, которые определены в дочерних классах.
        pTooltip.addAll(this.getEffectTooltipLines());
    }

    /**
     * Вспомогательный метод для создания модификатора с корректным UUID.
     * @param armorStack Броня, для которой создается модификатор.
     * @param attribute Сам атрибут (например, Attributes.MAX_HEALTH).
     * @param name Имя модификатора.
     * @param value Значение.
     * @param operation Операция (ADDITION, MULTIPLY_BASE, MULTIPLY_TOTAL).
     * @return Готовый AttributeModifier.
     */
    protected static AttributeModifier createModifier(ItemStack armorStack, Attribute attribute, String name, double value, AttributeModifier.Operation operation) {
        if (armorStack.getItem() instanceof ArmorItem armorItem) {
            // Получаем UUID, соответствующий слоту брони (шлем, ботинки и т.д.)
            UUID modifierUUID = ArmorModificationHelper.MODIFIER_UUIDS.get(armorItem.getType().getSlot().getIndex());
            if (modifierUUID != null) {
                return new AttributeModifier(modifierUUID, name, value, operation);
            }
        }
        // Возвращаем временный модификатор, если что-то пошло не так
        return new AttributeModifier(name, value, operation);
    }
}