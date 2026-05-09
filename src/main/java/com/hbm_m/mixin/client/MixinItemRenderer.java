//? if fabric {
package com.hbm_m.mixin.client;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.hbm_m.item.industrial.ItemAssemblyTemplate;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Fabric (в т.ч. с Sodium): подмена {@link ItemRenderer#getModel} — базовая иконка из
 * {@code models/item/assembly_template_base.json} (без {@code hbm_m:template_loader}); при Shift — модель выхода рецепта.
 * <p>
 * {@link CallbackInfoReturnable#setReturnValue} требует {@code cancellable = true} на {@link Inject}.
 */
@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {

    private static final ResourceLocation ASSEMBLY_TEMPLATE_ITEM_ID =
            new ResourceLocation(RefStrings.MODID, "assembly_template");

    private static final ModelResourceLocation HBM_ASSEMBLY_TEMPLATE_BASE_MODEL =
            new ModelResourceLocation(new ResourceLocation(RefStrings.MODID, "assembly_template_base"), "inventory");

    @Nullable
    private static volatile Item assemblyTemplateResolved;

    @Inject(method = "getModel", at = @At("RETURN"), cancellable = true)
    private void hbm_m$assemblyTemplateUseVisibleModel(
            ItemStack stack,
            @Nullable Level level,
            @Nullable LivingEntity entity,
            int seed,
            CallbackInfoReturnable<BakedModel> cir) {
        if (stack.isEmpty()) {
            return;
        }
        Item templateItem = assemblyTemplateResolved;
        if (templateItem == null) {
            if (!BuiltInRegistries.ITEM.containsKey(ASSEMBLY_TEMPLATE_ITEM_ID)) {
                return;
            }
            templateItem = BuiltInRegistries.ITEM.get(ASSEMBLY_TEMPLATE_ITEM_ID);
            assemblyTemplateResolved = templateItem;
        }
        if (!stack.is(templateItem)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ItemRenderer self = (ItemRenderer) (Object) this;

        if (Screen.hasShiftDown()) {
            ItemStack out = ItemAssemblyTemplate.getRecipeOutput(stack);
            if (!out.isEmpty()) {
                cir.setReturnValue(self.getModel(out, level, entity, seed));
                return;
            }
        }

        BakedModel missing = mc.getModelManager().getMissingModel();
        BakedModel base = mc.getModelManager().getModel(HBM_ASSEMBLY_TEMPLATE_BASE_MODEL);
        if (base != null && base != missing) {
            cir.setReturnValue(base);
        }
    }
}
//?}
