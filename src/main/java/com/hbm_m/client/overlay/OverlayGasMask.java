package com.hbm_m.client.overlay;

import com.hbm_m.item.gasmask.ArmorGasMaskItem;
import com.hbm_m.item.gasmask.IGasMask;
import com.hbm_m.item.gasmask.ItemGasMaskFilter;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
//?}

/**
 * Полноэкранный оверлей противогаза: при ношении маски экран затягивает «дымкой»,
 * которая густеет по мере выработки фильтра (6 стадий).
 * Порт {@code ArmorGasMask.renderHelmetOverlay} (1.7.10).
 */
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
public class OverlayGasMask {

    private static final String BASE_GASMASK = "textures/misc/overlay_gasmask.png";
    private static final String BASE_GOGGLES = "textures/misc/overlay_goggles.png";

    //? if forge {
    public static final IGuiOverlay OVERLAY = (gui, gfx, partialTick, screenWidth, screenHeight) ->
            render(gfx);
    //?}

    public static void render(GuiGraphics gfx) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) {
            return;
        }

        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        // Маска-шлем на голове или маска в слоте лица Curios (опционально).
        ItemStack maskStack = head.getItem() instanceof ArmorGasMaskItem
                ? head
                : com.hbm_m.client.compat.curios.CuriosClientCompat.getFaceMask(player);
        if (!(maskStack.getItem() instanceof ArmorGasMaskItem mask)) {
            return;
        }

        boolean gasmaskBase = mask.variant == ArmorGasMaskItem.Variant.GAS_MASK;

        // Без фильтра — базовая (наиболее плотная) текстура; с фильтром — стадия по износу.
        String path;
        if (!IGasMask.hasFilter(maskStack)) {
            path = gasmaskBase ? BASE_GASMASK : BASE_GOGGLES;
        } else {
            int max = maskStack.getItem() instanceof ItemGasMaskFilter f ? f.maxFilterDamage : ItemGasMaskFilter.DEFAULT_MAX_DAMAGE;
            int dmg = IGasMask.getFilterDamage(maskStack);
            int stage = Math.min((int) (dmg / (float) max * 6F), 5);
            path = String.format(mask.variant.overlayPattern, stage);
        }

        drawOverlay(gfx, ResourceLocation.fromNamespaceAndPath("hbm_m", path), gfx.guiWidth(), gfx.guiHeight());
    }

    private static void drawOverlay(GuiGraphics gfx, ResourceLocation texture, int width, int height) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        gfx.blit(texture, 0, 0, width, height, 0.0F, 0.0F, 1, 1, 1, 1);
        RenderSystem.disableBlend();
    }
}
