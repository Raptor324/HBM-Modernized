package com.hbm_m.client.gui;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.config.schema.ConfigField;
import com.hbm_m.config.schema.ConfigSchema;
import com.hbm_m.config.schema.ConfigSide;
import com.hbm_m.network.ConfigEditC2SPacket;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Собственное меню конфигурации на vanilla-виджетах (замена Cloth Config GUI).
 *
 * <p><b>Раскладка как в Cloth Config:</b>
 * <ul>
 *   <li>слева — вертикальный переключатель <b>Клиент/Сервер</b>;</li>
 *   <li>сверху (справа от переключателя) — горизонтальные вкладки <b>категорий</b>
 *       с плавной пиксельной прокруткой (колесо над полосой / Shift+колесо / стрелки ◀ ▶);
 *       возможность прокрутки НЕ зависит от выбранной вкладки;</li>
 *   <li>в центре — прокручиваемый список полей выбранной категории
 *       (со сбросом ↺ у каждого поля и глобальной кнопкой «Сбросить всё»);</li>
 * </ul>
 *
 * <p><b>Фон:</b> поверх текущего экрана рисуется только полупрозрачный тёмный
 * оверлей (земляной «dirt» фон НЕ используется — ни у экрана, ни у списка).
 *
 * <p><b>Виджеты строятся из {@link ConfigSchema}:</b> boolean→тоггл-кнопка
 * ON/OFF, enum→циклическая кнопка, ограниченный int→слайдер (bounded discrete),
 * остальное→{@link EditBox} с подсветкой ошибок и клэмпом. Для изменений,
 * требующих перезапуска/перезагрузки ресурсов, — подтверждение через vanilla
 * {@link ConfirmScreen}.
 *
 * <p><b>Прокрутка списка:</b> {@link ContainerObjectSelectionList} корректно
 * маршрутизирует клики в дочерние виджеты, только если позиция скроллбара
 * сдвинута к правому краю списка (переопределение {@code getScrollbarPosition()}).
 * Без этого дефолтный {@code width/2 + 124} оказывается посередине экрана и
 * клики по правым виджетам отбрасываются.
 */
public class ConfigScreen extends Screen {

    // ── Геометрия ──────────────────────────────────────────────────
    private static final int SIDEBAR_X = 8;
    private static final int SIDEBAR_W = 84;
    private static final int CONTENT_LEFT = SIDEBAR_X + SIDEBAR_W + 8; // 100
    private static final int TAB_Y = 26;
    private static final int TAB_H = 18;
    private static final int TAB_GAP = 2;
    private static final int ARROW_W = 16;
    private static final int CONTENT_TOP = TAB_Y + TAB_H + 6;          // 50
    private static final int LIST_BOTTOM_MARGIN = 34;
    private static final int WIDGET_WIDTH = 120;
    private static final int BOOL_WIDTH = 60;
    private static final int ROW_HEIGHT = 24;
    private static final int TAB_TEXT_PAD = 12;
    private static final int RESET_BTN_W = 18;
    private static final double TAB_WHEEL_STEP = 28.0;
    private static final int RESET_ALL_SIZE = 16;
    private static final int RESET_ALL_MARGIN = 8;

    // ── Палитра «современного» плоского дизайна (ARGB) ──────────────
    private static final int COL_OVERLAY       = 0xCC0E0E14; // тёмный полупрозрачный фон
    private static final int COL_PANEL         = 0x99161620;
    private static final int COL_PANEL_BORDER  = 0x55FFFFFF;
    private static final int COL_SIDEBAR       = 0xB014141C;
    private static final int COL_ACCENT        = 0xFFD56A;   // HBM gold
    private static final int COL_TITLE         = 0xFFE89B33;
    private static final int COL_ROW_ALT       = 0x12FFFFFF;
    private static final int COL_ROW_HOVER     = 0x26FFFFFF;
    private static final int COL_ACTIVE_FILL   = 0x33FFD56A;
    /** Рамка-подсветка кнопки «Сброс» у изменённого (не дефолтного) поля. */
    private static final int COL_RESET_DIRTY_EDGE = 0xCCFFD56A;

    private final boolean showServer;
    private ConfigSide currentSide = ConfigSide.CLIENT;
    private String currentCategory = "";
    private ConfigList list;
    /** key → строковое значение; накапливается виджетами, применяется по «Сохранить». */
    private final Map<String, String> pendingEdits = new LinkedHashMap<>();
    /** parentObject → развёрнут ли соответствующий аккордеон (по умолчанию все свёрнуты). */
    private final Set<String> expandedParents = new HashSet<>();

    // ── Прокрутка вкладок (пиксельная, плавная) ────────────────────
    private final List<String> currentCats = new ArrayList<>();
    private final List<TabInfo> tabLayout = new ArrayList<>();
    private boolean tabArrows = false;
    private int tabAreaStart = 0;
    private int tabAreaEnd = 0;
    private int tabOriginX = 0;
    private double tabScrollX = 0;       // текущее (анимированное) смещение полосы
    private double tabScrollTargetX = 0; // целевое смещение
    private double tabMaxScrollX = 0;
    /** Категория под курсором в этом кадре (тултип/подсветка). */
    private String hoveredTab;
    /** Наведение на маленькую красную кнопку «Сбросить всё» (для тултипа). */
    private boolean resetAllHover = false;

    // ── Ссылки на видимые кнопки (тултипы / выделение) ─────────────
    private Button clientSideBtn, serverSideBtn;

    /** Тултип, который нужно отрисовать в этом кадре (собирается виджетами). */
    private List<Component> tooltipToShow;

    public ConfigScreen() {
        super(Component.translatable("config.hbm_m.title"));
        Minecraft mc = Minecraft.getInstance();
        this.showServer = mc.getSingleplayerServer() != null
                || (mc.player != null && mc.player.hasPermissions(2));
    }

    /** Точка входа (например, из обработчика хоткея OPEN_CONFIG). */
    public static void open() {
        Minecraft.getInstance().setScreen(new ConfigScreen());
    }

    // ================================================================
    //  init()
    // ================================================================

    @Override
    protected void init() {
        super.init();

        // ── Левый вертикальный переключатель Клиент/Сервер ────────────
        // Обе кнопки кликабельны; «недоступность» сервера — только через active.
        clientSideBtn = Button.builder(Component.translatable("config.hbm_m.tab.client"),
                        b -> switchSide(ConfigSide.CLIENT))
                .pos(SIDEBAR_X, 42).size(SIDEBAR_W, 20).build();
        serverSideBtn = Button.builder(Component.translatable("config.hbm_m.tab.server"),
                        b -> switchSide(ConfigSide.SERVER))
                .pos(SIDEBAR_X, 66).size(SIDEBAR_W, 20).build();
        serverSideBtn.active = showServer;
        addRenderableWidget(clientSideBtn);
        addRenderableWidget(serverSideBtn);

        // ── Категории текущей стороны ─────────────────────────────────
        currentCats.clear();
        currentCats.addAll(ConfigSchema.categories(currentSide));
        if (!currentCats.contains(currentCategory)) {
            currentCategory = currentCats.isEmpty() ? "" : currentCats.get(0);
        }
        layoutTabs();

        // ── Список полей выбранной (сторона, категория) ────────────────
        int contentRight = this.width - 8;
        int rowW = contentRight - CONTENT_LEFT;
        this.list = new ConfigList(minecraft, CONTENT_LEFT, rowW, this.height,
                CONTENT_TOP, this.height - LIST_BOTTOM_MARGIN, ROW_HEIGHT);
        addWidget(list);
        list.rebuild();

        // ── Нижние кнопки: Сохранить / Отмена ──────────────────────────
        // (Глобальный «Сбросить всё» — отдельная маленькая красная кнопка
        //  в дальнем углу, см. drawResetAllButton — чтобы не нажать случайно.)
        int cx = this.width / 2;
        addRenderableWidget(Button.builder(Component.translatable("config.hbm_m.save"), b -> onSave())
                .pos(cx - 154, this.height - 28).size(150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .pos(cx + 4, this.height - 28).size(150, 20).build());
    }

    // ================================================================
    //  Вкладки категорий (ширина по тексту + пролистывание)
    // ================================================================

    private int contentRight() {
        return this.width - 8;
    }

    private int tabLabelWidth(String cat) {
        int textW = this.font.width(categoryLabel(cat));
        return Math.min(textW + TAB_TEXT_PAD * 2, 180);
    }

    /**
     * Вычисляет геометрию полосы вкладок (виджеты НЕ создаём — вкладки рисуем
     * вручную в {@link #drawTabStrip}). Прокрутка пиксельная и НЕ зависит от
     * выбранной категории: если все вкладки умещаются — прокрутки нет (maxScroll=0).
     */
    private void layoutTabs() {
        tabLayout.clear();
        int right = contentRight();
        int cum = 0;
        for (String c : currentCats) {
            int w = tabLabelWidth(c);
            tabLayout.add(new TabInfo(c, w, cum));
            cum += w + TAB_GAP;
        }
        int totalW = Math.max(0, cum - (currentCats.isEmpty() ? 0 : TAB_GAP));
        int fullAvail = Math.max(0, right - CONTENT_LEFT);

        tabArrows = totalW > fullAvail;
        tabAreaStart = tabArrows ? CONTENT_LEFT + ARROW_W + TAB_GAP : CONTENT_LEFT;
        tabAreaEnd = tabArrows ? right - ARROW_W - TAB_GAP : right;
        int areaW = Math.max(0, tabAreaEnd - tabAreaStart);
        tabMaxScrollX = Math.max(0, totalW - areaW);
        // Без переполнения — центрируем всю полосу.
        tabOriginX = tabArrows ? tabAreaStart : tabAreaStart + Math.max(0, (areaW - totalW) / 2);

        if (tabScrollTargetX > tabMaxScrollX) tabScrollTargetX = tabMaxScrollX;
        if (tabScrollTargetX < 0) tabScrollTargetX = 0;
        if (tabScrollX > tabMaxScrollX) tabScrollX = tabMaxScrollX;
    }

    /** Плавная прокрутка вкладок на delta пикселей (положительное — вправо). */
    private void scrollTabsBy(double delta) {
        tabScrollTargetX = Mth.clamp(tabScrollTargetX + delta, 0, tabMaxScrollX);
    }

    /** Категория под курсором (в видимой части полосы) или null. */
    private String tabAt(double mx, double my) {
        if (my < TAB_Y || my > TAB_Y + TAB_H) return null;
        for (TabInfo t : tabLayout) {
            int sx = (int) (tabOriginX + t.cumX - tabScrollX);
            if (mx >= sx && mx < sx + t.width && sx + t.width > tabAreaStart && sx < tabAreaEnd) {
                return t.cat;
            }
        }
        return null;
    }

    private void switchSide(ConfigSide side) {
        if (side == ConfigSide.SERVER && !showServer) return;
        this.currentSide = side;
        this.currentCategory = ""; // init() выберет первую категорию новой стороны
        this.tabScrollX = 0;
        this.tabScrollTargetX = 0;
        this.rebuildWidgets();
    }

    private void selectCategory(String category) {
        if (category.equals(this.currentCategory)) return;
        // Звук клика по вкладке (UI_BUTTON_CLICK — стандартный UI-клик).
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
        // Выбор вкладки не влияет на прокрутку полосы — перестраиваем только список.
        this.currentCategory = category;
        // При переключении вкладки открываем список с самого верха,
        // иначе сохранённая глубокая прокрутка оставит экран пустым.
        this.list.setScrollAmount(0);
        this.list.rebuild();
    }

    /** Сворачивает/разворачивает группу вложенного объекта и перестраивает список. */
    private void toggleCollapse(String parent) {
        if (expandedParents.contains(parent)) expandedParents.remove(parent);
        else expandedParents.add(parent);
        list.rebuild();
    }

    /**
     * Значение поля для GUI: сначала правка из {@link #pendingEdits},
     * иначе текущее значение из конфига.
     */
    private String currentValueString(ConfigField f) {
        if (pendingEdits.containsKey(f.getKey())) {
            return pendingEdits.get(f.getKey());
        }
        return f.getAsString(ModClothConfig.get());
    }

    /** true, если текущее значение поля отличается от значения по умолчанию. */
    private boolean isModified(ConfigField f) {
        String current = currentValueString(f);
        String def = ConfigSchema.defaultAsString(f);
        return !current.equals(def);
    }

    /** Сбрасывает одно поле к значению по умолчанию (стейтится в pendingEdits). */
    private void resetField(ConfigField f) {
        pendingEdits.put(f.getKey(), ConfigSchema.defaultAsString(f));
        list.rebuild();
    }

    /** Глобальный сброс всех полей текущей стороны к значениям по умолчанию. */
    private void onResetAll() {
        Minecraft.getInstance().setScreen(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        for (ConfigField f : ConfigSchema.bySide(currentSide)) {
                            pendingEdits.put(f.getKey(), ConfigSchema.defaultAsString(f));
                        }
                        list.rebuild();
                    }
                    Minecraft.getInstance().setScreen(this);
                },
                Component.translatable("config.hbm_m.reset.title"),
                Component.translatable("config.hbm_m.reset.message"),
                Component.translatable("config.hbm_m.reset"),
                Component.translatable("gui.cancel")
        ));
    }

    // ================================================================
    //  Колесо мыши: Shift → прокрутка вкладок, иначе — прокрутка списка
    // ================================================================

    //? if <1.21 {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return handleMouseScrolled(mouseX, mouseY, amount);
    }
    //?} else {
    /*@Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double amount) {
        return handleMouseScrolled(mouseX, mouseY, amount);
    }
    *///?}

    private boolean handleMouseScrolled(double mouseX, double mouseY, double amount) {
        // Колесо над полосой вкладок (или Shift в любом месте) → прокрутка вкладок,
        // иначе — прокрутка списка полей.
        boolean overTabs = mouseY >= TAB_Y && mouseY <= TAB_Y + TAB_H
                && mouseX >= CONTENT_LEFT && mouseX < this.width;
        if (this.hasShiftDown() || overTabs) {
            scrollTabsBy(-amount * TAB_WHEEL_STEP);
            return true;
        }
        //? if <1.21 {
        return super.mouseScrolled(mouseX, mouseY, amount);
        //?} else {
        /*return super.mouseScrolled(mouseX, mouseY, 0.0D, amount);
        *///?}
    }

    // ================================================================
    //  Клик по полосе вкладок / стрелкам (вкладки рисуем вручную)
    // ================================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int rx = resetAllX(), ry = resetAllY();
            if (mouseX >= rx && mouseX <= rx + RESET_ALL_SIZE
                    && mouseY >= ry && mouseY <= ry + RESET_ALL_SIZE) {
                onResetAll();
                return true;
            }
        }
        if (button == 0 && mouseY >= TAB_Y && mouseY <= TAB_Y + TAB_H) {
            if (tabArrows) {
                if (mouseX >= CONTENT_LEFT && mouseX <= CONTENT_LEFT + ARROW_W) {
                    scrollTabsBy(-(tabAreaEnd - tabAreaStart) * 0.8);
                    return true;
                }
                if (mouseX >= contentRight() - ARROW_W && mouseX <= contentRight()) {
                    scrollTabsBy((tabAreaEnd - tabAreaStart) * 0.8);
                    return true;
                }
            }
            String t = tabAt(mouseX, mouseY);
            if (t != null) {
                selectCategory(t);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ================================================================
    //  Сохранение
    // ================================================================

    private void onSave() {
        if (pendingEdits.isEmpty()) {
            onClose();
            return;
        }
        boolean needsAction = false;
        for (String key : pendingEdits.keySet()) {
            ConfigField f = ConfigSchema.get(key);
            if (f != null && (f.requiresRestart() || f.requiresResourceReload())) {
                needsAction = true;
                break;
            }
        }
        if (needsAction) {
            Minecraft.getInstance().setScreen(new ConfirmScreen(
                    confirmed -> {
                        if (confirmed) {
                            doApplyEdits();
                        } else {
                            Minecraft.getInstance().setScreen(this);
                        }
                    },
                    Component.translatable("config.hbm_m.restart.title"),
                    Component.translatable("config.hbm_m.restart.message"),
                    Component.translatable("config.hbm_m.apply"),
                    Component.translatable("gui.cancel")
            ));
        } else {
            doApplyEdits();
        }
    }

    /** Применяет накопленные правки: клиентские — локально + saveClient, серверные — пакетом. */
    private void doApplyEdits() {
        Map<String, String> clientEdits = new LinkedHashMap<>();
        Map<String, String> serverEdits = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : pendingEdits.entrySet()) {
            ConfigField f = ConfigSchema.get(e.getKey());
            if (f == null) continue;
            if (f.isClient()) clientEdits.put(e.getKey(), e.getValue());
            else if (f.isServer()) serverEdits.put(e.getKey(), e.getValue());
        }

        if (!clientEdits.isEmpty()) {
            ConfigSchema.applyAll(ModClothConfig.get(), ConfigSide.CLIENT, clientEdits);
            ModClothConfig.saveClient();
        }
        if (!serverEdits.isEmpty() && showServer) {
            // Сервер применит, сохранит и рассылает sync всем (включая нас).
            ConfigEditC2SPacket.sendToServer(serverEdits);
        }
        pendingEdits.clear();
        onClose();
    }

    // ================================================================
    //  Рендер
    // ================================================================

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        // Фон: только полупрозрачный тёмный оверлей (без земляного dirt-фона).
        g.fill(0, 0, this.width, this.height, COL_OVERLAY);

        int contentRight = contentRight();
        int panelBottom = this.height - LIST_BOTTOM_MARGIN + 4;

        // Левая панель-переключатель стороны (верх выровнен с панелью контента — y=22).
        panel(g, SIDEBAR_X - 2, 22, SIDEBAR_W + 4, panelBottom - 22, COL_SIDEBAR);

        // Выделение активной стороны (заливка + левый акцент-барь).
        int sideY = currentSide == ConfigSide.SERVER ? 66 : 42;
        g.fill(SIDEBAR_X - 2, sideY - 2, SIDEBAR_X + SIDEBAR_W + 2, sideY + 22, COL_ACTIVE_FILL);
        g.fill(SIDEBAR_X - 2, sideY - 2, SIDEBAR_X + 1, sideY + 22, COL_ACCENT);

        // Панель контента (вкладки + список).
        panel(g, CONTENT_LEFT - 4, 22, contentRight - CONTENT_LEFT + 8, panelBottom - 22, COL_PANEL);

        // Плавная анимация прокрутки вкладок к целевому смещению.
        if (tabScrollX != tabScrollTargetX) {
            double diff = tabScrollTargetX - tabScrollX;
            tabScrollX += diff * 0.35;
            if (Math.abs(diff) < 0.5) tabScrollX = tabScrollTargetX;
        }

        // Полоса вкладок (ручная отрисовка с пиксельной прокруткой и клиппингом).
        hoveredTab = tabAt(mouseX, mouseY);
        drawTabStrip(g, mouseX, mouseY);

        // Список полей (строки могут добавить тултип).
        tooltipToShow = null;
        this.list.render(g, mouseX, mouseY, partial);

        // Кнопки (sidebar/tabs/bottom).
        super.render(g, mouseX, mouseY, partial);

        // Маленькая красная кнопка «Сбросить всё» в дальнем углу.
        drawResetAllButton(g, mouseX, mouseY);

        // Тултипы верхних кнопок (если курсор над ними).
        collectTopButtonTooltip();

        g.drawCenteredString(this.font, this.title, this.width / 2, 5, COL_TITLE);

        if (tooltipToShow != null && !tooltipToShow.isEmpty()) {
            g.renderComponentTooltip(this.font, tooltipToShow, mouseX, mouseY);
        }
    }

    /** Плоская панель: заливка + тонкая рамка. */
    private static void panel(GuiGraphics g, int x, int y, int w, int h, int fill) {
        if (w <= 0 || h <= 0) return;
        g.fill(x, y, x + w, y + h, fill);
        int b = COL_PANEL_BORDER;
        g.fill(x, y, x + w, y + 1, b);
        g.fill(x, y + h - 1, x + w, y + h, b);
        g.fill(x, y, x + 1, y + h, b);
        g.fill(x + w - 1, y, x + w, y + h, b);
    }

    // ================================================================
    //  Отрисовка полосы вкладок (ручная, с пиксельной прокруткой)
    // ================================================================

    private void drawTabStrip(GuiGraphics g, int mouseX, int mouseY) {
        if (tabArrows) {
            int right = contentRight();
            boolean hL = mouseX >= CONTENT_LEFT && mouseX <= CONTENT_LEFT + ARROW_W
                    && mouseY >= TAB_Y && mouseY <= TAB_Y + TAB_H;
            boolean hR = mouseX >= right - ARROW_W && mouseX <= right
                    && mouseY >= TAB_Y && mouseY <= TAB_Y + TAB_H;
            drawArrow(g, CONTENT_LEFT, true, hL, tabScrollTargetX > 0.5);
            drawArrow(g, right - ARROW_W, false, hR, tabScrollTargetX < tabMaxScrollX - 0.5);
        }
        g.enableScissor(tabAreaStart, TAB_Y, tabAreaEnd, TAB_Y + TAB_H);
        for (TabInfo t : tabLayout) {
            int sx = (int) (tabOriginX + t.cumX - tabScrollX);
            int ex = sx + t.width;
            if (ex <= tabAreaStart || sx >= tabAreaEnd) continue;
            boolean active = t.cat.equals(currentCategory);
            boolean hover = t.cat.equals(hoveredTab);
            drawTab(g, sx, TAB_Y, t.width, TAB_H, active, hover, categoryLabel(t.cat));
            if (active) {
                g.fill(sx, TAB_Y + TAB_H, ex, TAB_Y + TAB_H + 2, COL_ACCENT);
            }
        }
        g.disableScissor();
    }

    private void drawTab(GuiGraphics g, int x, int y, int w, int h,
                         boolean active, boolean hover, Component label) {
        int fill = active ? COL_ACTIVE_FILL : (hover ? COL_ROW_HOVER : 0x22FFFFFF);
        g.fill(x, y, x + w, y + h, fill);
        g.drawCenteredString(this.font, label, x + w / 2, y + 5,
                active ? COL_ACCENT : 0xE0E0E0);
    }

    private void drawArrow(GuiGraphics g, int x, boolean left, boolean hover, boolean enabled) {
        g.fill(x, TAB_Y, x + ARROW_W, TAB_Y + TAB_H, hover && enabled ? COL_ROW_HOVER : 0x22FFFFFF);
        int col = enabled ? (hover ? COL_ACCENT : 0xE0E0E0) : 0x55FFFFFF;
        g.drawCenteredString(this.font, Component.literal(left ? "\u25C0" : "\u25B6"),
                x + ARROW_W / 2, TAB_Y + 5, col);
    }

    // ── Маленькая красная кнопка «Сбросить всё» (дальний верхний угол) ──

    private int resetAllX() {
        return this.width - RESET_ALL_MARGIN - RESET_ALL_SIZE;
    }

    private int resetAllY() {
        return 3;
    }

    private void drawResetAllButton(GuiGraphics g, int mouseX, int mouseY) {
        int x = resetAllX();
        int y = resetAllY();
        int s = RESET_ALL_SIZE;
        boolean hover = mouseX >= x && mouseX <= x + s && mouseY >= y && mouseY <= y + s;
        resetAllHover = hover;
        int fill = hover ? 0xAAFF3333 : 0x77BB2222;
        g.fill(x, y, x + s, y + s, fill);
        int edge = 0xFFFF5555;
        g.fill(x, y, x + s, y + 1, edge);
        g.fill(x, y + s - 1, x + s, y + s, edge);
        g.fill(x, y, x + 1, y + s, edge);
        g.fill(x + s - 1, y, x + s, y + s, edge);
        g.drawCenteredString(this.font, Component.literal("\u21BA"),
                x + s / 2, y + 4, 0xFFFF6B6B);
    }

    /** Описание одной вкладки: категория, ширина, левый отступ в полосе. */
    private static final class TabInfo {
        final String cat;
        final int width;
        final int cumX;
        TabInfo(String cat, int width, int cumX) {
            this.cat = cat;
            this.width = width;
            this.cumX = cumX;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ================================================================
    //  Тултипы (строятся из существующих метаданных схемы)
    // ================================================================

    private void collectTopButtonTooltip() {
        if (resetAllHover) {
            tooltipToShow = langLines("config.hbm_m.reset.all");
            return;
        }
        if (hoveredTab != null) {
            List<Component> tt = categoryTooltip(hoveredTab);
            if (tt != null) {
                tooltipToShow = tt;
                return;
            }
        }
        if (clientSideBtn != null && clientSideBtn.isHovered()) {
            tooltipToShow = langLines("config.hbm_m.tab.client.tooltip");
        } else if (serverSideBtn != null && serverSideBtn.isHovered() && showServer) {
            tooltipToShow = langLines("config.hbm_m.tab.server.tooltip");
        }
    }

    /** Тултип категории — только если есть перевод config.hbm_m.category.<cat>.tooltip. */
    private List<Component> categoryTooltip(String category) {
        return langLines("config.hbm_m.category." + category + ".tooltip");
    }

    /**
     * Тултип поля: явный перевод {@code config.hbm_m.field.<key>.tooltip} (если есть),
     * плюс диапазон, значение по умолчанию и предупреждение о рестарте/перезагрузке.
     */
    private List<Component> fieldTooltip(ConfigField f) {
        List<Component> lines = new ArrayList<>();
        List<Component> desc = fieldDescription(f);
        if (desc != null) lines.addAll(desc);

        if (f.getMin() != null && f.getMax() != null) {
            lines.add(Component.literal("[" + fmtNum(f.getMin()) + " .. " + fmtNum(f.getMax()) + "]")
                    .withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.literal("\u00BB " + ConfigSchema.defaultAsString(f))
                .withStyle(ChatFormatting.DARK_GRAY));

        if (f.requiresRestart()) {
            lines.add(Component.literal("\u26A0 restart").withStyle(ChatFormatting.GOLD));
        } else if (f.requiresResourceReload()) {
            lines.add(Component.literal("\u21BB F3+T").withStyle(ChatFormatting.YELLOW));
        }
        return lines;
    }

    /**
     * Описание поля: сначала явный {@code config.hbm_m.field.<key>.tooltip}, затем
     * Cloth-совместимый {@code text.autoconfig.hbm_m.option.<key>.@Tooltip}.
     */
    private static List<Component> fieldDescription(ConfigField f) {
        List<Component> primary = langLines(ConfigSchema.tooltipKey(f));
        if (primary != null) return primary;
        return langLines("text.autoconfig.hbm_m.option." + f.getKey() + ".@Tooltip");
    }

    /** Список строк перевода (с разделением по \n), либо null, если перевода нет. */
    private static List<Component> langLines(String key) {
        if (key == null || !Language.getInstance().has(key)) return null;
        String text = Language.getInstance().getOrDefault(key);
        List<Component> l = new ArrayList<>();
        for (String part : text.split("\n")) {
            if (!part.isEmpty()) {
                l.add(Component.literal(part).withStyle(ChatFormatting.GRAY));
            }
        }
        return l.isEmpty() ? null : l;
    }

    /** 1000.0 → "1000", 0.5 → "0.5". */
    private static String fmtNum(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) return String.valueOf((long) d);
        return String.valueOf(d);
    }

    // ================================================================
    //  Локализация с fallback (переиспользование text.autoconfig.* переводов)
    // ================================================================

    /** Подпись поля: если есть явный config.hbm_m.field.* — он, иначе старый text.autoconfig.*. */
    private static Component fieldLabel(ConfigField f) {
        String k = ConfigSchema.labelKey(f);
        if (Language.getInstance().has(k)) return Component.translatable(k);
        return Component.translatable("text.autoconfig.hbm_m.option." + f.getKey());
    }

    /** Заголовок категории: config.hbm_m.category.* с fallback на text.autoconfig.*. */
    private static Component categoryLabel(String category) {
        String k = ConfigSchema.categoryKey(category);
        if (Language.getInstance().has(k)) return Component.translatable(k);
        return Component.translatable("text.autoconfig.hbm_m.category." + category);
    }

    /** Название группы вложенного объекта (аккордеон). */
    private static Component groupLabel(String parent) {
        String k = "config.hbm_m.group." + parent;
        if (Language.getInstance().has(k)) return Component.translatable(k);
        return Component.translatable("text.autoconfig.hbm_m.option." + parent);
    }

    // ================================================================
    //  Виджет для поля (по типу)
    // ================================================================

    @SuppressWarnings({"rawtypes", "unchecked"})
    private AbstractWidget createWidget(ConfigField f) {
        switch (f.getType()) {
            case BOOLEAN:
                return createBoolToggle(f);
            case ENUM:
                return createEnumCycle(f);
            case INT:
                // Bounded discrete → слайдер (как @BoundedDiscrete в Cloth Config).
                if (f.getMin() != null && f.getMax() != null) {
                    return new ConfigSlider(f);
                }
                return numericEditBox(f);
            default:
                // LONG / FLOAT / DOUBLE — точный ввод с подсветкой ошибки.
                return numericEditBox(f);
        }
    }

    /** Boolean → plain-кнопка ON/OFF (без префикса/двоеточия CycleButton). */
    private AbstractWidget createBoolToggle(ConfigField f) {
        boolean cur = readBool(f);
        Button b = Button.builder(boolMessage(cur), btn -> {
            boolean now = !readBool(f);
            pendingEdits.put(f.getKey(), String.valueOf(now));
            btn.setMessage(boolMessage(now));
        }).bounds(0, 0, BOOL_WIDTH, 20).build();
        return b;
    }

    private boolean readBool(ConfigField f) {
        if (pendingEdits.containsKey(f.getKey())) {
            return Boolean.parseBoolean(pendingEdits.get(f.getKey()));
        }
        return (boolean) f.get(ModClothConfig.get());
    }

    private static Component boolMessage(boolean on) {
        return Component.literal(on ? "ON" : "OFF")
                .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    /** Enum → циклическая кнопка по константам (без префикса CycleButton). */
    private AbstractWidget createEnumCycle(ConfigField f) {
        Object cur = readEnumValue(f);
        Object[] constants = cur.getClass().getEnumConstants();
        if (constants == null || constants.length == 0) {
            return numericEditBox(f);
        }
        int[] idx = { indexOf(constants, cur) };
        Button b = Button.builder(enumMessage(cur), btn -> {
            idx[0] = (idx[0] + 1) % constants.length;
            Object v = constants[idx[0]];
            pendingEdits.put(f.getKey(), String.valueOf(v));
            btn.setMessage(enumMessage(v));
        }).bounds(0, 0, WIDGET_WIDTH, 20).build();
        return b;
    }

    private Object readEnumValue(ConfigField f) {
        if (pendingEdits.containsKey(f.getKey())) {
            try {
                return f.parse(pendingEdits.get(f.getKey()));
            } catch (IllegalArgumentException ignored) {
                // повреждённое значение — упадём на текущем значении
            }
        }
        return f.get(ModClothConfig.get());
    }

    private static Component enumMessage(Object v) {
        return Component.literal(String.valueOf(v));
    }

    private static int indexOf(Object[] arr, Object v) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(v)) return i;
        }
        return 0;
    }

    private EditBox numericEditBox(ConfigField f) {
        boolean hasPending = pendingEdits.containsKey(f.getKey());
        EditBox box = new EditBox(this.font, 0, 0, WIDGET_WIDTH, 18, Component.empty());
        box.setValue(hasPending ? pendingEdits.get(f.getKey()) : f.getAsString(ModClothConfig.get()));
        box.setResponder(s -> {
            try {
                f.parse(s);
                pendingEdits.put(f.getKey(), s);
                box.setTextColor(0xE0E0E0);
            } catch (IllegalArgumentException ex) {
                box.setTextColor(0xFF5555);
            }
        });
        return box;
    }

    // ================================================================
    //  Сброс-кнопка поля (подсветка изменённых значений)
    // ================================================================

    /**
     * Рисует золотую рамку-подсветку вокруг маленькой кнопки «Сброс» поля.
     * Vanilla-кнопка рисует собственный спрайт, поэтому подсветку накладываем
     * ПОВЕРХ кнопки (после её render). Если значение совпадает с дефолтным —
     * рамка не рисуется.
     */
    private static void drawResetHighlight(GuiGraphics g, Button btn, boolean dirty) {
        if (!dirty) return;
        int x = btn.getX();
        int y = btn.getY();
        int w = btn.getWidth();
        int h = btn.getHeight();
        int col = COL_RESET_DIRTY_EDGE;
        // Верх и низ.
        g.fill(x - 1, y - 1, x + w + 1, y, col);
        g.fill(x - 1, y + h, x + w + 1, y + h + 1, col);
        // Лево и право.
        g.fill(x - 1, y, x, y + h, col);
        g.fill(x + w, y, x + w + 1, y + h, col);
    }

    // ================================================================
    //  Слайдер для ограниченного числового поля (bounded discrete)
    // ================================================================

    private final class ConfigSlider extends AbstractSliderButton {
        private final ConfigField field;
        private final double min;
        private final double max;
        /** INT/LONG — дискретный слайдер: значение и подпись — целые. */
        private final boolean integer;

        ConfigSlider(ConfigField f) {
            super(0, 0, WIDGET_WIDTH, 20, Component.empty(), 0.0);
            this.field = f;
            this.min = f.getMin();
            this.max = f.getMax();
            this.integer = f.getType() == ConfigField.FieldType.INT
                    || f.getType() == ConfigField.FieldType.LONG;
            this.value = percentOf(currentValue(f));
            updateMessage();
        }

        private double currentValue(ConfigField f) {
            if (pendingEdits.containsKey(f.getKey())) {
                try {
                    return Double.parseDouble(pendingEdits.get(f.getKey()));
                } catch (NumberFormatException e) {
                    return min;
                }
            }
            Object o = f.get(ModClothConfig.get());
            return o instanceof Number ? ((Number) o).doubleValue() : min;
        }

        private double percentOf(double v) {
            if (max <= min) return 0.0;
            return Mth.clamp((v - min) / (max - min), 0.0, 1.0);
        }

        /** Значение поля по позиции ползунка (для int/long — округлённое). */
        private double valueOf() {
            double v = min + (max - min) * this.value;
            return integer ? Math.round(v) : v;
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(fmtNum(valueOf())));
        }

        @Override
        protected void applyValue() {
            double v = valueOf();
            pendingEdits.put(field.getKey(),
                    integer ? String.valueOf((long) v) : String.valueOf(v));
        }
    }

    // ================================================================
    //  Список и строки
    // ================================================================

    private class ConfigList extends ContainerObjectSelectionList<ConfigList.Row> {
        private final int rowW;

        ConfigList(Minecraft mc, int leftPos, int rowW, int screenHeight, int y0, int y1, int itemHeight) {
            super(mc, rowW, screenHeight, y0, y1, itemHeight);
            this.rowW = rowW;
            setLeftPos(leftPos);
            // Не рисуем ванильный dirt-фон списка — сквозь него виден наш тёмный оверлей.
            setRenderBackground(false);
            setRenderTopAndBottom(false);
        }

        @Override
        public int getRowWidth() {
            return rowW;
        }

        /**
         * КРИТИЧНО: позиция скроллбара по умолчанию = width/2 + 124 — это середина
         * экрана. При этом {@code getEntryAtPosition} отбрасывает клики с
         * {@code x >= getScrollbarPosition()}, из-за чего правые виджеты (тогглы,
         * слайдеры) не получают ввод. Сдвигаем скроллбар к правому краю списка.
         */
        @Override
        protected int getScrollbarPosition() {
            return this.getRight();
        }

        /**
         * Перестраивает список полей выбранной категории. Поля одного вложенного объекта
         * (parentObject) группируются под кликабельным аккордеоном; свёрнутые группы скрыты.
         */
        public void rebuild() {
            clearEntries();
            if (currentCategory == null || currentCategory.isEmpty()) return;
            String sentinel = "\u0000";
            String prevGroup = sentinel;
            for (ConfigField f : ConfigSchema.byCategory(currentSide, currentCategory)) {
                String parent = f.getParentObject();
                if (parent != null && !parent.equals(prevGroup)) {
                    addEntry(new CollapsibleRow(parent));
                    prevGroup = parent;
                } else if (parent == null) {
                    prevGroup = sentinel;
                }
                if (parent == null || expandedParents.contains(parent)) {
                    addEntry(new FieldRow(f));
                }
            }
            // После перестройки ограничиваем прокрутку новым максимумом:
            // иначе при переключении вкладки/сворачивании группы список
            // может остаться «пустым» из-за старой глубокой позиции.
            setScrollAmount(getScrollAmount());
        }

        abstract class Row extends ContainerObjectSelectionList.Entry<Row> {
        }

        /** Заголовок-аккордеон группы вложенного объекта. */
        class CollapsibleRow extends Row {
            private final String parent;
            private final Button header;

            CollapsibleRow(String parent) {
                this.parent = parent;
                this.header = Button.builder(groupLabel(parent), b -> toggleCollapse(parent))
                        .pos(0, 0).size(Math.max(60, rowW - 4), 18).build();
                updateHeaderText();
            }

            private void updateHeaderText() {
                String arrow = expandedParents.contains(parent) ? "\u25BE " : "\u25B8 ";
                header.setMessage(Component.literal(arrow).append(groupLabel(parent)));
            }

            @Override
            public void render(GuiGraphics g, int index, int top, int left, int width, int height,
                               int mouseX, int mouseY, boolean hovered, float partial) {
                header.setPosition(left + 2, top);
                header.render(g, mouseX, mouseY, partial);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(header);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of((NarratableEntry) header);
            }
        }

        /** Строка поля: подпись слева (с маркером перезапуска), [виджет][↺ reset] справа. */
        class FieldRow extends Row {
            private final ConfigField field;
            private final AbstractWidget widget;
            private final Button resetBtn;
            private final Component label;

            FieldRow(ConfigField f) {
                this.field = f;
                this.label = fieldLabel(f);
                this.widget = createWidget(f);
                this.resetBtn = Button.builder(Component.literal("\u21BA"), b -> resetField(field))
                        .bounds(0, 0, RESET_BTN_W, 18).build();
            }

            @Override
            public void render(GuiGraphics g, int index, int top, int left, int width, int height,
                               int mouseX, int mouseY, boolean hovered, float partial) {
                g.fill(left, top, left + width, top + height, hovered ? COL_ROW_HOVER : (index % 2 == 0 ? COL_ROW_ALT : 0));
                String marker = field.requiresRestart() ? "\u26A0 "
                        : (field.requiresResourceReload() ? "\u21BB " : "");
                Component lbl = marker.isEmpty()
                        ? label
                        : Component.literal(marker).append(label);
                g.drawString(ConfigScreen.this.font, lbl, left + 2, top + 7, 0xE0E0E0);

                // Кластер справа: виджет + кнопка сброса, с зазором под скроллбар.
                int rightEdge = left + width - 10;
                int resetX = rightEdge - RESET_BTN_W;
                int widgetX = resetX - 2 - widget.getWidth();
                // Центрируем виджет по вертикали относительно 20px-виджетов (кнопки/слайдеры):
                // EditBox высотой 18px получает +1px, чтобы не «всплывать» вверх.
                int widgetY = top + (20 - widget.getHeight()) / 2;
                this.widget.setPosition(widgetX, widgetY);
                this.widget.render(g, mouseX, mouseY, partial);
                this.resetBtn.setPosition(resetX, top + 1);
                this.resetBtn.render(g, mouseX, mouseY, partial);
                // Подсветка поверх кнопки «Сброс», если значение отличается от дефолта.
                drawResetHighlight(g, resetBtn, isModified(field));

                if (hovered) {
                    ConfigScreen.this.tooltipToShow = ConfigScreen.this.fieldTooltip(field);
                }
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(widget, resetBtn);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                // AbstractWidget/Button реализуют NarratableEntry → приведение безопасно (upcast).
                return List.of((NarratableEntry) widget, (NarratableEntry) resetBtn);
            }
        }
    }
}
