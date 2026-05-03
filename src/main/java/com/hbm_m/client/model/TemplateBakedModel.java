package com.hbm_m.client.model;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.industrial.ItemAssemblyTemplate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Модель предмета: обычный вид шаблона; при зажатом Shift — иконка выхода рецепта (если есть NBT).
 * Реализовано через подкласс {@link ItemOverrides}.
 * <p>
 * Forge/NeoForge: только эта цепочка.
 * <p>
 * Fabric: итоговую модель после {@code resolve} для слота/GUI подменяет mixin
 * {@code MixinItemRenderer} (Sodium не рисует путь Fabric BuiltinItemRenderer); JSON {@code assembly_template_base}
 * — vanilla generated без {@code template_loader}.
 */
public class TemplateBakedModel implements BakedModel {

    private final BakedModel originalModel;
    private final TemplateItemOverrides itemOverrides;

    public TemplateBakedModel(BakedModel originalModel, ModelBaker modelBaker, BlockModel unbakedTemplateModel) {
        this.originalModel = originalModel;
        this.itemOverrides = new TemplateItemOverrides(modelBaker, unbakedTemplateModel);
    }

    @NotNull
    @Override
    public ItemOverrides getOverrides() {
        return this.itemOverrides;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return originalModel.getQuads(state, side, rand);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return originalModel.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return originalModel.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return originalModel.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return originalModel.isCustomRenderer();
    }

    @Override
    public net.minecraft.client.renderer.texture.TextureAtlasSprite getParticleIcon() {
        return originalModel.getParticleIcon();
    }

    @Override
    public net.minecraft.client.renderer.block.model.ItemTransforms getTransforms() {
        return originalModel.getTransforms();
    }

    /** Non-static: нужен доступ к {@link #originalModel} для Fabric без дублирования ссылки и null. */
    private final class TemplateItemOverrides extends ItemOverrides {

        private final Map<Item, BakedModel> cache = new ConcurrentHashMap<>();

        TemplateItemOverrides(ModelBaker modelBaker, BlockModel unbakedTemplateModel) {
            super(modelBaker, unbakedTemplateModel, List.of());
        }

        @Override
        @Nullable
        public BakedModel resolve(
                @NotNull BakedModel model,
                @NotNull ItemStack stack,
                @Nullable ClientLevel level,
                @Nullable LivingEntity entity,
                int seed) {
            BakedModel base = super.resolve(model, stack, level, entity, seed);
            if (!Screen.hasShiftDown()) {
                return base;
            }
            ItemStack outputStack = ItemAssemblyTemplate.getRecipeOutput(stack);
            if (outputStack.isEmpty()) {
                return base;
            }
            Minecraft mc = Minecraft.getInstance();
            ClientLevel resolveLevel = level != null ? level : mc.level;
            return cache.computeIfAbsent(
                    outputStack.getItem(),
                    item -> mc.getItemRenderer().getModel(outputStack, resolveLevel, entity, seed));
        }
    }
}
