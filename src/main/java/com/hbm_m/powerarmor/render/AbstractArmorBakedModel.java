package com.hbm_m.powerarmor.render;

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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.client.model.AbstractMultipartBakedModel;
import com.hbm_m.powerarmor.ModPowerArmorItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Абстрактный базовый класс для рендеринга иконок брони в GUI.
 * Содержит всю общую логику рендеринга, оставляя подклассам только конфигурацию.
 * Аналогично AbstractObjArmorLayer для entity рендеринга.
 */
@OnlyIn(Dist.CLIENT)
public abstract class AbstractArmorBakedModel extends AbstractMultipartBakedModel implements AbstractMultipartBakedModel.PartNamesProvider {

    // ModelProperty для передачи armorType через ModelData
    protected static final ModelProperty<ArmorItem.Type> ARMOR_TYPE_PROPERTY = new ModelProperty<>();
    
    // ThreadLocal для отслеживания текущего рендеринга (fallback, если ItemOverrides не вызывается)
    protected static final ThreadLocal<ArmorItem.Type> CURRENT_ARMOR_TYPE = new ThreadLocal<>();

    protected final IArmorModelConfig config;
    private final AbstractArmorItemOverrides itemOverrides;

    public AbstractArmorBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms, IArmorModelConfig config) {
        super(parts, transforms);
        this.config = config;
        this.itemOverrides = createItemOverrides();
    }

    /**
     * Создает ItemOverrides для данного сета брони.
     * Может быть переопределен в подклассах для кастомизации.
     */
    protected AbstractArmorItemOverrides createItemOverrides() {
        return new AbstractArmorItemOverrides();
    }

    /**
     * Создает новую модель с теми же частями, но другими трансформациями.
     * Используется для применения индивидуальных display трансформаций из JSON для каждого предмета.
     */
    public abstract AbstractArmorBakedModel withTransforms(ItemTransforms newTransforms);

    @Override
    public String[] getPartNames() {
        return config.getPartOrder();
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
        // Базовый getQuads НЕ должен рендерить части напрямую - это делает ArmorPartBakedModel через ItemOverrides
        // Если вызывается базовый getQuads без ItemOverrides, возвращаем пустой список
        if (state == null) {
            // Пытаемся получить armorType из ModelData (устанавливается через ArmorPartBakedModel.getQuads)
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

    /**
     * Получает квады для рендеринга предмета в GUI/руке.
     */
    protected List<BakedQuad> getItemQuads(@Nullable Direction side, RandomSource rand,
                                         ModelData modelData,
                                         @Nullable net.minecraft.client.renderer.RenderType renderType,
                                         @Nullable ArmorItem.Type armorType) {
        List<BakedQuad> allQuads = new ArrayList<>();
        
        // Определяем, какие части нужно рендерить через конфигурацию
        String[] partsToRender = config.getPartsForType(armorType);
        if (partsToRender == null || partsToRender.length == 0) {
            // Если конфигурация не вернула части, используем все части как fallback
            partsToRender = config.getPartOrder();
        }

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

    /**
     * Публичный метод для доступа из wrapper модели.
     */
    public List<BakedQuad> getItemQuadsForType(@Nullable Direction side, RandomSource rand,
                                               ModelData modelData,
                                               @Nullable net.minecraft.client.renderer.RenderType renderType,
                                               @Nullable ArmorItem.Type armorType) {
        List<BakedQuad> quads = getItemQuads(side, rand, modelData, renderType, armorType);
        // Трансформации из JSON (display.gui.scale и т.д.) применяются автоматически через ItemTransforms
        // Не нужно масштабировать квады вручную
        return quads;
    }

    /**
     * Общий класс ItemOverrides для всех сетов брони.
     */
    protected class AbstractArmorItemOverrides extends ItemOverrides {
        private final Map<Item, BakedModel> cache = new ConcurrentHashMap<>();
        // Кэш для быстрого доступа к armorType по Item
        private final Map<Item, ArmorItem.Type> armorTypeCache = new ConcurrentHashMap<>();

        public AbstractArmorItemOverrides() {
            super();
        }

        @Override
        @Nullable
        public BakedModel resolve(@Nullable BakedModel model, @Nullable ItemStack stack, 
                                  @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            if (stack == null || !config.isItemValid(stack)) {
                return model;
            }

            if (!(stack.getItem() instanceof ModPowerArmorItem armorItem)) {
                return model;
            }

            // Получаем тип брони
            ArmorItem.Type armorType = armorItem.getType();
            
            // Сохраняем armorType в кэш для использования в базовом getQuads
            armorTypeCache.put(stack.getItem(), armorType);
            
            // Устанавливаем armorType в ThreadLocal для fallback (если ItemOverrides не применяется автоматически)
            // Примечание: ThreadLocal будет очищен в ArmorPartBakedModel.getQuads()
            // Но на случай, если getQuads не будет вызван, очищаем здесь тоже
            try {
                CURRENT_ARMOR_TYPE.set(armorType);
                
                // Создаем wrapper модель, которая будет рендерить только нужные части
                BakedModel result = cache.computeIfAbsent(stack.getItem(), item -> 
                    new ArmorPartBakedModel(AbstractArmorBakedModel.this, armorType)
                );
                return result;
            } catch (Exception e) {
                // В случае исключения очищаем ThreadLocal
                CURRENT_ARMOR_TYPE.remove();
                throw e;
            }
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
     * Wrapper модель, которая рендерит только определенные части в зависимости от типа брони.
     * Общий для всех сетов брони.
     */
    protected static class ArmorPartBakedModel extends BakedModelWrapper<AbstractArmorBakedModel> {
        private final ArmorItem.Type armorType;

        public ArmorPartBakedModel(AbstractArmorBakedModel originalModel, ArmorItem.Type armorType) {
            super(originalModel);
            this.armorType = armorType;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                       RandomSource rand, ModelData modelData,
                                       @Nullable net.minecraft.client.renderer.RenderType renderType) {
            // Устанавливаем armorType в ThreadLocal для fallback
            // Используем try-finally для гарантированной очистки даже при исключениях
            CURRENT_ARMOR_TYPE.set(armorType);
            try {
                // Устанавливаем armorType в ModelData для передачи в базовую модель
                ModelData dataWithArmorType = modelData.derive()
                    .with(ARMOR_TYPE_PROPERTY, armorType)
                    .build();
                return originalModel.getItemQuadsForType(side, rand, dataWithArmorType, renderType, armorType);
            } finally {
                // Очищаем ThreadLocal после рендеринга (гарантированно, даже при исключениях)
                CURRENT_ARMOR_TYPE.remove();
            }
        }
    }
}



