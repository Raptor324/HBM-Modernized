package com.hbm_m.client.overlay;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.client.model.variant.DoorModelRegistry;
import com.hbm_m.client.model.variant.DoorModelSelection;
import com.hbm_m.client.model.variant.DoorModelType;
import com.hbm_m.client.model.variant.DoorSkin;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.ServerboundDoorModelPacket;
import com.hbm_m.sound.ModSounds;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * UI Меню для выбора модели и скина двери.
 * Использует текстуру door_modification_GUI.png с кнопками скинов в сетке 4x2.
 *
 * @author HBM-M Team
 */
@OnlyIn(Dist.CLIENT)
public class DoorModelSelectionScreen extends Screen {

    // Масштаб GUI. Масштабирование через PoseStack — без дублирования текстуры.
    private static final float GUI_SCALE = 1.7f;

    // Размеры текстуры: 256x256
    private static final int TEX_WIDTH = 256;
    private static final int TEX_HEIGHT = 256;

    // Основное окно: 218x175, UV (38, 0)
    private static final int BG_U = 38;
    private static final int BG_V = 0;
    private static final int MENU_WIDTH = 218;
    private static final int MENU_HEIGHT = 175;

    // Кнопки перелистывания: 40x11
    // Нажатая влево: UV (57, 194), нажатая вправо: UV (106, 194)
    // Обычная влево: UV (57, 212), обычная вправо: UV (106, 212)
    private static final int PAGE_LEFT_U = 57;
    private static final int PAGE_LEFT_PRESSED_V = 194;
    private static final int PAGE_LEFT_NORMAL_V = 212;
    private static final int PAGE_RIGHT_U = 106;
    private static final int PAGE_RIGHT_PRESSED_V = 194;
    private static final int PAGE_RIGHT_NORMAL_V = 212;
    private static final int PAGE_BTN_W = 40;
    private static final int PAGE_BTN_H = 11;
    // Позиции кнопок в GUI: левая (74, 148), правая (157, 148)
    private static final int PAGE_LEFT_X = 36;
    private static final int PAGE_RIGHT_X = 119;
    private static final int PAGE_Y = 148;

    // Кнопки выбора скина: 20x20
    // Нажатая: UV (168, 191), обычная: UV (168, 215)
    private static final int SKIN_BTN_SIZE = 20;
    private static final int SKIN_BTN_PRESSED_U = 168;
    private static final int SKIN_BTN_PRESSED_V = 191;
    private static final int SKIN_BTN_NORMAL_U = 168;
    private static final int SKIN_BTN_NORMAL_V = 215;
    // Начальное положение кнопки: (66, 78), сетка 4x2
    private static final int SKIN_GRID_START_X = 28;
    private static final int SKIN_GRID_START_Y = 78;
    private static final int SKIN_GRID_COLS = 4;
    private static final int SKIN_GRID_ROWS = 2;
    private static final int SKIN_GRID_GAP = 20;
    private static final int SKIN_GRID_ROW_HEIGHT = 32;
    private static final int ITEMS_PER_PAGE = SKIN_GRID_COLS * SKIN_GRID_ROWS;

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/door_modification_GUI.png");

    // Данные двери
    private final BlockPos doorPos;
    private final String doorId;
    private DoorModelSelection currentSelection;
    private DoorModelSelection selectedSelection;

    // Плоский список вариантов (LEGACY+default, MODERN+skin1..N)
    private List<DoorModelSelection> flatSelectionList;
    private int currentPage = 0;
    private int totalPages = 1;

    // UI
    private int leftPos;
    private int topPos;

    public DoorModelSelectionScreen(BlockPos doorPos, String doorId, DoorModelSelection currentSelection) {
        super(Component.translatable("gui.hbm_m.door_model_selection.title"));
        this.doorPos = doorPos;
        this.doorId = doorId;
        this.currentSelection = currentSelection;
        this.selectedSelection = currentSelection;
    }

    @Override
    protected void init() {
        super.init();
        int scaledW = (int) (MENU_WIDTH * GUI_SCALE);
        int scaledH = (int) (MENU_HEIGHT * GUI_SCALE);
        this.leftPos = (this.width - scaledW) / 2;
        this.topPos = (this.height - scaledH) / 2;
        buildFlatSelectionList();
        this.totalPages = Math.max(1, (flatSelectionList.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        if (minecraft != null && minecraft.getSoundManager() != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.METAL_BOX_OPEN.get(), 1.0F, 1.5F));
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null && minecraft.getSoundManager() != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.METAL_BOX_CLOSE.get(), 1.0F,0.9F));
        }
        super.onClose();
    }

    /** Преобразует экранные координаты мыши в локальные координаты GUI (с учётом масштаба). */
    private int toLocalX(double screenX) {
        return (int) ((screenX - leftPos) / GUI_SCALE);
    }
    private int toLocalY(double screenY) {
        return (int) ((screenY - topPos) / GUI_SCALE);
    }

    /**
     * Собирает плоский список: LEGACY+default, затем MODERN+skin1..N
     */
    private void buildFlatSelectionList() {
        flatSelectionList = new ArrayList<>();
        DoorModelRegistry registry = DoorModelRegistry.getInstance();

        // LEGACY: один вариант
        flatSelectionList.add(DoorModelSelection.legacy());

        // MODERN: по одному варианту на каждый скин
        List<DoorSkin> modernSkins = registry.getSkins(doorId);
        for (DoorSkin skin : modernSkins) {
            flatSelectionList.add(new DoorModelSelection(DoorModelType.MODERN, skin));
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        int localMouseX = toLocalX(mouseX);
        int localMouseY = toLocalY(mouseY);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos, topPos, 0);
        guiGraphics.pose().scale(GUI_SCALE, GUI_SCALE, 1f);
        renderMenuPanel(guiGraphics);
        renderSkinButtons(guiGraphics, localMouseX, localMouseY);
        renderPageButtons(guiGraphics, localMouseX, localMouseY);
        guiGraphics.pose().popPose();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderMenuPanel(GuiGraphics guiGraphics) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        guiGraphics.blit(TEXTURE, 0, 0, BG_U, BG_V, MENU_WIDTH, MENU_HEIGHT, TEX_WIDTH, TEX_HEIGHT);
    }

    private void renderSkinButtons(GuiGraphics guiGraphics, int localMouseX, int localMouseY) {
        ItemStack doorStack = getDoorItemStack();
        int startIndex = currentPage * ITEMS_PER_PAGE;

        for (int pageIndex = 0; pageIndex < ITEMS_PER_PAGE; pageIndex++) {
            int flatIndex = startIndex + pageIndex;
            if (flatIndex >= flatSelectionList.size()) break;

            int col = pageIndex % SKIN_GRID_COLS;
            int row = pageIndex / SKIN_GRID_COLS;
            int x = SKIN_GRID_START_X + col * (SKIN_BTN_SIZE + SKIN_GRID_GAP);
            int y = SKIN_GRID_START_Y + row * SKIN_GRID_ROW_HEIGHT;

            DoorModelSelection selection = flatSelectionList.get(flatIndex);
            boolean hovered = localMouseX >= x && localMouseX < x + SKIN_BTN_SIZE && localMouseY >= y && localMouseY < y + SKIN_BTN_SIZE;
            boolean selected = selection.equals(selectedSelection);

            int u = (hovered || selected) ? SKIN_BTN_PRESSED_U : SKIN_BTN_NORMAL_U;
            int v = (hovered || selected) ? SKIN_BTN_PRESSED_V : SKIN_BTN_NORMAL_V;
            guiGraphics.blit(TEXTURE, x, y, u, v, SKIN_BTN_SIZE, SKIN_BTN_SIZE, TEX_WIDTH, TEX_HEIGHT);

            DoorModelFakeItemRenderer.renderDoorModel(guiGraphics, selection, doorId, doorStack, x + 2, y + 2, SKIN_BTN_SIZE - 4);

            Component labelComponent = selection.getDisplayName(doorId);
            String label = labelComponent.getString();
            int labelWidth = font.width(label);
            int labelX = x + (SKIN_BTN_SIZE - labelWidth) / 2;
            int labelY = y + SKIN_BTN_SIZE ;
            guiGraphics.drawString(font, label, labelX, labelY, 0xFFFFFF);
        }
    }

    private void renderPageButtons(GuiGraphics guiGraphics, int localMouseX, int localMouseY) {
        int leftX = PAGE_LEFT_X;
        int rightX = PAGE_RIGHT_X;
        int btnY = PAGE_Y;

        boolean canFlip = totalPages > 1;
        boolean leftHovered = canFlip && localMouseX >= leftX && localMouseX < leftX + PAGE_BTN_W && localMouseY >= btnY && localMouseY < btnY + PAGE_BTN_H;
        boolean rightHovered = canFlip && localMouseX >= rightX && localMouseX < rightX + PAGE_BTN_W && localMouseY >= btnY && localMouseY < btnY + PAGE_BTN_H;

        int leftV = leftHovered ? PAGE_LEFT_PRESSED_V : PAGE_LEFT_NORMAL_V;
        int rightV = rightHovered ? PAGE_RIGHT_PRESSED_V : PAGE_RIGHT_NORMAL_V;

        guiGraphics.blit(TEXTURE, leftX, btnY, PAGE_LEFT_U, leftV, PAGE_BTN_W, PAGE_BTN_H, TEX_WIDTH, TEX_HEIGHT);
        guiGraphics.blit(TEXTURE, rightX, btnY, PAGE_RIGHT_U, rightV, PAGE_BTN_W, PAGE_BTN_H, TEX_WIDTH, TEX_HEIGHT);
    }

    private ItemStack getDoorItemStack() {
        Item doorItem = getDoorItemForId(doorId);
        return new ItemStack(doorItem);
    }

    private static Item getDoorItemForId(String doorId) {
        if (doorId == null || doorId.isEmpty()) {
            return ModItems.LARGE_VEHICLE_DOOR.get();
        }
        return switch (doorId) {
            case "large_vehicle_door" -> ModItems.LARGE_VEHICLE_DOOR.get();
            case "round_airlock_door" -> ModItems.ROUND_AIRLOCK_DOOR.get();
            case "transition_seal" -> ModItems.TRANSITION_SEAL.get();
            case "fire_door" -> ModItems.FIRE_DOOR.get();
            case "sliding_blast_door" -> ModItems.SLIDE_DOOR.get();
            case "sliding_seal_door" -> ModItems.SLIDING_SEAL_DOOR.get();
            case "secure_access_door" -> ModItems.SECURE_ACCESS_DOOR.get();
            case "qe_sliding_door" -> ModItems.QE_SLIDING.get();
            case "qe_containment_door" -> ModItems.QE_CONTAINMENT.get();
            case "water_door" -> ModItems.WATER_DOOR.get();
            case "silo_hatch" -> ModItems.SILO_HATCH.get();
            case "silo_hatch_large" -> ModItems.SILO_HATCH_LARGE.get();
            default -> ModItems.LARGE_VEHICLE_DOOR.get();
        };
    }

    private void playButtonClickSound() {
        if (minecraft != null && minecraft.getSoundManager() != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // ПКМ — выход из GUI
        if (button == 1) {
            onClose();
            return true;
        }
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int lx = toLocalX(mouseX);
        int ly = toLocalY(mouseY);

        // Кнопки перелистывания — всегда кликабельны, но при скинах < 8 ничего не делают
        int leftX = PAGE_LEFT_X;
        int rightX = PAGE_RIGHT_X;
        int btnY = PAGE_Y;

        if (lx >= leftX && lx < leftX + PAGE_BTN_W && ly >= btnY && ly < btnY + PAGE_BTN_H) {
            playButtonClickSound();
            if (totalPages > 1) {
                currentPage = Math.max(0, currentPage - 1);
            }
            return true;
        }
        if (lx >= rightX && lx < rightX + PAGE_BTN_W && ly >= btnY && ly < btnY + PAGE_BTN_H) {
            playButtonClickSound();
            if (totalPages > 1) {
                currentPage = Math.min(totalPages - 1, currentPage + 1);
            }
            return true;
        }

        // Кнопки выбора скина
        int startIndex = currentPage * ITEMS_PER_PAGE;
        for (int pageIndex = 0; pageIndex < ITEMS_PER_PAGE; pageIndex++) {
            int flatIndex = startIndex + pageIndex;
            if (flatIndex >= flatSelectionList.size()) break;

            int col = pageIndex % SKIN_GRID_COLS;
            int row = pageIndex / SKIN_GRID_COLS;
            int x = SKIN_GRID_START_X + col * (SKIN_BTN_SIZE + SKIN_GRID_GAP);
            int y = SKIN_GRID_START_Y + row * SKIN_GRID_ROW_HEIGHT;

            if (lx >= x && lx < x + SKIN_BTN_SIZE && ly >= y && ly < y + SKIN_BTN_SIZE) {
                playButtonClickSound();
                DoorModelSelection newSelection = flatSelectionList.get(flatIndex);
                selectedSelection = newSelection;
                if (!newSelection.equals(currentSelection)) {
                    ServerboundDoorModelPacket.send(doorPos, newSelection);
                    currentSelection = newSelection;
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
