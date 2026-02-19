package com.hbm_m.client.overlay;

import com.hbm_m.client.model.variant.DoorModelRegistry;
import com.hbm_m.client.model.variant.DoorModelSelection;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Рендерит 3D модель двери с выбранным скином в GUI через стандартный renderFakeItem.
 * Модель подменяется через ItemOverrides в DoorBakedModel — NBT "hbm_m:door_preview"
 * с выбором (modelType, skin, doorId) заставляет ItemRenderer использовать нужную модель.
 * Это даёт корректный порядок рендера частей и трансформации из display.gui.
 */
@OnlyIn(Dist.CLIENT)
public final class DoorModelFakeItemRenderer {

    private static final String PREVIEW_TAG = "hbm_m:door_preview";
    /** Масштаб иконок (1.0 = стандартный размер, 0.8 = чуть меньше) */
    private static final float ICON_SCALE = 0.9f;

    private DoorModelFakeItemRenderer() {}

    /**
     * Рендерит 3D модель двери с указанным выбором (legacy/modern+skin) в слот GUI.
     * Использует renderFakeItem с ItemStack, в NBT которого записан выбор — DoorItemOverrides
     * подставляет нужную модель, и ItemRenderer рендерит её стандартным образом.
     *
     * @param guiGraphics контекст рендеринга
     * @param selection   выбор модели и скина
     * @param doorId      ID двери (round_airlock_door и т.д.)
     * @param doorStack   ItemStack двери (базовый стек для fallback)
     * @param x           X координата слота
     * @param y           Y координата слота
     * @param size        размер слота (не используется — renderFakeItem сам масштабирует)
     */
    public static void renderDoorModel(net.minecraft.client.gui.GuiGraphics guiGraphics, DoorModelSelection selection,
                                       String doorId, ItemStack doorStack, int x, int y, int size) {
        DoorModelRegistry registry = DoorModelRegistry.getInstance();
        ItemStack stackToRender;
        if (!registry.isRegistered(doorId) || registry.getModelPath(doorId, selection) == null) {
            stackToRender = doorStack;
        } else {
            ItemStack previewStack = doorStack.copy();
            CompoundTag preview = selection.save();
            preview.putString("doorId", doorId);
            previewStack.getOrCreateTag().put(PREVIEW_TAG, preview);
            stackToRender = previewStack;
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x + 8, y + 8, 0);
        guiGraphics.pose().scale(ICON_SCALE, ICON_SCALE, 1f);
        guiGraphics.pose().translate(-8, -8, 0);
        guiGraphics.renderFakeItem(stackToRender, 0, 0);
        guiGraphics.pose().popPose();
    }
}
