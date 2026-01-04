package com.hbm_m.client.model;

import com.hbm_m.item.armor.ModPowerArmorItem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class T51ArmorBakedModel extends AbstractMultipartBakedModel implements AbstractMultipartBakedModel.PartNamesProvider {

    private static final String[] ORDER = {
            "Helmet", "Chest", "RightArm", "LeftArm", "RightLeg", "LeftLeg", "RightBoot", "LeftBoot"
    };
    
    // ModelProperty для передачи armorType через ModelData
    private static final ModelProperty<ArmorItem.Type> ARMOR_TYPE_PROPERTY = new ModelProperty<>();
    
    // ThreadLocal для отслеживания текущего рендеринга (fallback, если ItemOverrides не вызывается)
    private static final ThreadLocal<ArmorItem.Type> CURRENT_ARMOR_TYPE = new ThreadLocal<>();

    private final T51ItemOverrides itemOverrides;

    public T51ArmorBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms) {
        super(parts, transforms);
        this.itemOverrides = new T51ItemOverrides();
    }
    
    /**
     * Создает новую модель с теми же частями, но другими трансформациями.
     * Используется для применения индивидуальных display трансформаций из JSON для каждого предмета.
     */
    public T51ArmorBakedModel withTransforms(ItemTransforms newTransforms) {
        return new T51ArmorBakedModel(this.parts, newTransforms);
    }

    @Override
    public String[] getPartNames() {
        return ORDER;
    }

    @Override
    protected boolean shouldSkipWorldRendering(@Nullable BlockState state) {
        // Это НЕ блок — world render нам не нужен.
        // Но для GUI-рендеринга (state == null) нужно рендерить модель
        return state != null;
    }

    @Override
    public ItemOverrides getOverrides() {
        return itemOverrides;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                     RandomSource rand, ModelData modelData,
                                     @Nullable net.minecraft.client.renderer.RenderType renderType) {
        // WORLD RENDER: Пропускаем (броня рендерится через RenderLayer)
        if (shouldSkipWorldRendering(state)) {
            return Collections.emptyList();
        }

        // ITEM RENDER: Рендерим для GUI и руки
        // Если state == null, это запрос для item-рендеринга (GUI, рука, земля)
        // Базовый getQuads НЕ должен рендерить части напрямую - это делает T51ArmorPartBakedModel через ItemOverrides
        // Если вызывается базовый getQuads без ItemOverrides, возвращаем пустой список
        if (state == null) {
            // Пытаемся получить armorType из ModelData (устанавливается через T51ArmorPartBakedModel.getQuads)
            ArmorItem.Type armorType = modelData.get(ARMOR_TYPE_PROPERTY);
            
            // Если не найден в ModelData, пытаемся получить из ThreadLocal (fallback)
            if (armorType == null) {
                armorType = CURRENT_ARMOR_TYPE.get();
            }
            
            // Если armorType найден, рендерим только нужные части
            if (armorType != null) {
                List<BakedQuad> quads = getItemQuads(side, rand, modelData, renderType, armorType);
                // Трансформации из JSON применяются автоматически через ItemTransforms
                // Не нужно масштабировать квады вручную - это делает Minecraft через ItemTransforms
                return quads;
            } else {
                // Если armorType не найден, это означает, что вызывается базовый getQuads без ItemOverrides
                // Возвращаем пустой список - рендеринг должен происходить через ItemOverrides.resolve()
                return Collections.emptyList();
            }
        }

        return Collections.emptyList();
    }

    private List<BakedQuad> getItemQuads(@Nullable Direction side, RandomSource rand,
                                         ModelData modelData,
                                         @Nullable net.minecraft.client.renderer.RenderType renderType,
                                         @Nullable ArmorItem.Type armorType) {
        List<BakedQuad> allQuads = new ArrayList<>();
        
        // Определяем, какие части нужно рендерить
        String[] partsToRender = getPartsForArmorType(armorType);

        // Рендерим только нужные части
        for (String partName : partsToRender) {
            BakedModel part = parts.get(partName);
            if (part != null) {
                // Добавляем квады для всех направлений
                if (side != null) {
                    List<BakedQuad> partQuads = part.getQuads(null, side, rand, modelData, renderType);
                    allQuads.addAll(partQuads);
                } else {
                    // Если side == null, добавляем квады для всех направлений
                    for (Direction dir : Direction.values()) {
                        List<BakedQuad> partQuads = part.getQuads(null, dir, rand, modelData, renderType);
                        allQuads.addAll(partQuads);
                    }
                    // И квады без направления
                    List<BakedQuad> generalQuads = part.getQuads(null, null, rand, modelData, renderType);
                    allQuads.addAll(generalQuads);
                }
            }
        }

        return allQuads;
    }

    private String[] getPartsForArmorType(@Nullable ArmorItem.Type armorType) {
        if (armorType == null) {
            // Если тип не указан, рендерим все части (fallback)
            return ORDER;
        }

        return switch (armorType) {
            case HELMET -> new String[]{"Helmet"};
            case CHESTPLATE -> new String[]{"Chest", "RightArm", "LeftArm"};
            case LEGGINGS -> new String[]{"RightLeg", "LeftLeg"};
            case BOOTS -> new String[]{"RightBoot", "LeftBoot"};
        };
    }

    private class T51ItemOverrides extends ItemOverrides {
        private final Map<Item, BakedModel> cache = new ConcurrentHashMap<>();
        // Кэш для быстрого доступа к armorType по Item
        private final Map<Item, ArmorItem.Type> armorTypeCache = new ConcurrentHashMap<>();

        public T51ItemOverrides() {
            super();
        }

        @Override
        @Nullable
        public BakedModel resolve(@Nullable BakedModel model, @Nullable ItemStack stack, 
                                  @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            if (stack == null || !(stack.getItem() instanceof ModPowerArmorItem armorItem)) {
                return model;
            }

            // Получаем тип брони
            ArmorItem.Type armorType = armorItem.getType();
            
            // Сохраняем armorType в кэш для использования в базовом getQuads
            armorTypeCache.put(stack.getItem(), armorType);
            
            // Устанавливаем armorType в ThreadLocal для fallback (если ItemOverrides не применяется автоматически)
            CURRENT_ARMOR_TYPE.set(armorType);
            
            // Создаем wrapper модель, которая будет рендерить только нужные части
            BakedModel result = cache.computeIfAbsent(stack.getItem(), item -> 
                new T51ArmorPartBakedModel(T51ArmorBakedModel.this, armorType)
            );
            return result;
        }
        
        /**
         * Получает armorType для предмета (для использования в базовом getQuads)
         */
        @Nullable
        public ArmorItem.Type getArmorType(Item item) {
            return armorTypeCache.get(item);
        }
    }

    /**
     * Wrapper модель, которая рендерит только определенные части в зависимости от типа брони
     */
    private static class T51ArmorPartBakedModel extends BakedModelWrapper<T51ArmorBakedModel> {
        private final ArmorItem.Type armorType;

        public T51ArmorPartBakedModel(T51ArmorBakedModel originalModel, ArmorItem.Type armorType) {
            super(originalModel);
            this.armorType = armorType;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                       RandomSource rand, ModelData modelData,
                                       @Nullable net.minecraft.client.renderer.RenderType renderType) {
            // Устанавливаем armorType в ThreadLocal для fallback
            CURRENT_ARMOR_TYPE.set(armorType);
            
            // Устанавливаем armorType в ModelData для передачи в базовую модель
            ModelData dataWithArmorType = modelData.derive()
                .with(ARMOR_TYPE_PROPERTY, armorType)
                .build();
            // Используем приватный метод оригинальной модели через рефлексию или создадим публичный метод
            // Для простоты, создадим публичный метод в T51ArmorBakedModel
            List<BakedQuad> quads = originalModel.getItemQuadsForType(side, rand, dataWithArmorType, renderType, armorType);
            
            // Очищаем ThreadLocal после рендеринга
            CURRENT_ARMOR_TYPE.remove();
            return quads;
        }
    }

    // Публичный метод для доступа из wrapper модели
    public List<BakedQuad> getItemQuadsForType(@Nullable Direction side, RandomSource rand,
                                               ModelData modelData,
                                               @Nullable net.minecraft.client.renderer.RenderType renderType,
                                               @Nullable ArmorItem.Type armorType) {
        List<BakedQuad> quads = getItemQuads(side, rand, modelData, renderType, armorType);
        // Трансформации из JSON (display.gui.scale и т.д.) применяются автоматически через ItemTransforms
        // Не нужно масштабировать квады вручную
        return quads;
    }
    
}
