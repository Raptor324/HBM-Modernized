package com.hbm_m.inventory.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.inventory.menu.MachineFluidTankMenu;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.network.FluidTankModePacket;
import com.hbm_m.network.ModPacketHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

public class GUIMachineFluidTank extends AbstractContainerScreen<MachineFluidTankMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/gui/storage/gui_fluid_tank.png");

    private final int tankX = 71;
    private final int tankY = 17;
    private final int tankWidth = 34;
    private final int tankHeight = 52;
    private final int tankCapacity = 256000;

    private static final int MODE_BUTTON_X = 151;
    private static final int MODE_BUTTON_Y = 34;
    private static final int MODE_BUTTON_SIZE = 18;

    public GUIMachineFluidTank(MachineFluidTankMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        renderFluidTooltip(guiGraphics, mouseX, mouseY);
        renderModeButtonTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int mode = menu.getMode();
        guiGraphics.blit(TEXTURE, this.leftPos + MODE_BUTTON_X, this.topPos + MODE_BUTTON_Y, 176, mode * MODE_BUTTON_SIZE, MODE_BUTTON_SIZE, MODE_BUTTON_SIZE);

        renderFluid(guiGraphics);
    }

    private void renderFluid(GuiGraphics guiGraphics) {
        FluidStack fluidStack = this.menu.getFluid();
        if (fluidStack.isEmpty()) return;

        int pixelHeight = (int) ((long) fluidStack.getAmount() * tankHeight / tankCapacity);
        if (pixelHeight == 0 && fluidStack.getAmount() > 0) pixelHeight = 1;
        if (pixelHeight > tankHeight) pixelHeight = tankHeight;

        Fluid fluid = fluidStack.getFluid();
        
        // Получаем tint цвет - сначала пробуем HbmFluidRegistry для кастомных жидкостей
        int fluidColor = getFluidTintColor(fluid);
        float r = (fluidColor >> 16 & 255) / 255.0F;
        float g = (fluidColor >> 8 & 255) / 255.0F;
        float b = (fluidColor & 255) / 255.0F;
        float a = ((fluidColor >> 24) & 255) / 255.0F;
        if (a == 0) a = 1.0F; // Если alpha не задана, используем 1.0

        // Получаем текстуру жидкости
        IClientFluidTypeExtensions fluidProps = IClientFluidTypeExtensions.of(fluid);
        ResourceLocation fluidTextureId = fluidProps.getStillTexture(fluidStack);
        if (fluidTextureId == null) return;
        
        TextureAtlasSprite fluidSprite = this.minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(fluidTextureId);

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(r, g, b, a);
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        int x = this.leftPos + tankX;
        int y = this.topPos + tankY + tankHeight - pixelHeight;

        // Рендер с тайлингом текстуры (как в оригинале 1.7.10)
        renderTiledFluid(guiGraphics, x, y, tankWidth, pixelHeight, fluidSprite);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    /**
     * Получает tint цвет для жидкости.
     * Сначала проверяет HbmFluidRegistry для кастомных жидкостей HBM,
     * затем использует стандартный метод Forge.
     */
    private int getFluidTintColor(Fluid fluid) {
        // Для кастомных жидкостей HBM используем HbmFluidRegistry
        String fluidName = HbmFluidRegistry.getFluidName(fluid);
        if (fluidName != null && !fluidName.equals("none") && !fluidName.equals("empty")) {
            int hbmColor = HbmFluidRegistry.getTintColor(fluid);
            if (hbmColor != 0xFFFFFF) {
                return hbmColor;
            }
        }
        
        // Для ванильных и других жидкостей используем Forge API
        IClientFluidTypeExtensions fluidProps = IClientFluidTypeExtensions.of(fluid);
        return fluidProps.getTintColor();
    }

    private void renderTiledFluid(GuiGraphics guiGraphics, int x, int y, int width, int height, TextureAtlasSprite sprite) {
        if (height <= 0 || width <= 0) return;

        PoseStack poseStack = guiGraphics.pose();
        var bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX);

        int texW = 16;
        int texH = 16;

        // Проходим по ширине
        for (int drawX = 0; drawX < width; drawX += texW) {
            int currentWidth = Math.min(texW, width - drawX);

            // Проходим по высоте
            for (int drawY = 0; drawY < height; drawY += texH) {
                int currentHeight = Math.min(texH, height - drawY);
                
                // Рассчитываем смещение по Y. Мы рисуем тайлы снизу вверх, 
                // чтобы частичный верхний слой обрезался сверху, как в 1.7.10
                int yOffset = height - drawY - currentHeight;

                // Вычисляем UV-координаты для текущего куска
                float u0 = sprite.getU0();
                // Срезаем U1, если кусок уже, чем 16 пикселей
                float u1 = sprite.getU0() + (sprite.getU1() - sprite.getU0()) * ((float) currentWidth / texW);
                
                // Срезаем V0, если кусок ниже 16 пикселей (оставляем нижнюю часть текстуры)
                float v0 = sprite.getV0() + (sprite.getV1() - sprite.getV0()) * ((float) (texH - currentHeight) / texH);
                float v1 = sprite.getV1();

                int quadX = x + drawX;
                int quadY = y + yOffset;
                var matrix = poseStack.last().pose();

                // Отрисовка вершин (по часовой стрелке)
                bufferBuilder.vertex(matrix, quadX, quadY + currentHeight, 0).uv(u0, v1).endVertex();
                bufferBuilder.vertex(matrix, quadX + currentWidth, quadY + currentHeight, 0).uv(u1, v1).endVertex();
                bufferBuilder.vertex(matrix, quadX + currentWidth, quadY, 0).uv(u1, v0).endVertex();
                bufferBuilder.vertex(matrix, quadX, quadY, 0).uv(u0, v0).endVertex();
            }
        }

        Tesselator.getInstance().end();
    }

    private void renderFluidTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!isHovering(tankX, tankY, tankWidth, tankHeight, mouseX, mouseY)) return;

        FluidStack fluid = this.menu.getFluid();
        List<Component> lines = new ArrayList<>();

        if (!fluid.isEmpty()) {
            lines.add(fluid.getDisplayName());
            lines.add(Component.literal(fluid.getAmount() + " / " + tankCapacity + " mB"));
        } else {
            int filterId = this.menu.getFilterFluidId();
            if (filterId >= 0) {
                Fluid filterFluid = BuiltInRegistries.FLUID.byId(filterId);
                lines.add(Component.translatable("gui.hbm_m.fluid_tank.empty_filter", Component.translatable(filterFluid.getFluidType().getDescriptionId())));
            } else {
                lines.add(Component.translatable("gui.hbm_m.fluid_tank.empty"));
            }
        }

        guiGraphics.renderTooltip(this.font, lines, Optional.empty(), mouseX, mouseY);
    }

    private void renderModeButtonTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isHovering(MODE_BUTTON_X, MODE_BUTTON_Y, MODE_BUTTON_SIZE, MODE_BUTTON_SIZE, mouseX, mouseY)) {
            int mode = menu.getMode();
            String key = "gui.hbm_m.fluid_tank.mode." + mode;
            guiGraphics.renderTooltip(this.font, Component.translatable(key), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovering(MODE_BUTTON_X, MODE_BUTTON_Y, MODE_BUTTON_SIZE, MODE_BUTTON_SIZE, (int) mouseX, (int) mouseY)) {
            ModPacketHandler.INSTANCE.sendToServer(new FluidTankModePacket(menu.blockEntity.getBlockPos()));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isHovering(int x, int y, int w, int h, int mouseX, int mouseY) {
        return mouseX >= this.leftPos + x && mouseX < this.leftPos + x + w &&
                mouseY >= this.topPos + y && mouseY < this.topPos + y + h;
    }
}
