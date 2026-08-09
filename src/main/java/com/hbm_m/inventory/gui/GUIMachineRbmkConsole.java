package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineRbmkConsoleBlockEntity;
import com.hbm_m.blockentity.machines.MachineRbmkConsoleBlockEntity.RBMKColumnData;
import com.hbm_m.blockentity.machines.rbmk.RBMKColumnBlockEntity.ColumnType;
import com.hbm_m.inventory.menu.MachineRbmkConsoleMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.RBMKConsoleControlPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class GUIMachineRbmkConsole extends AbstractContainerScreen<MachineRbmkConsoleMenu> {

    // ─── Resources ───────────────────────────────────────────────────────────

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/reactors/gui_rbmk_console.png");

    // ─── Grid layout ─────────────────────────────────────────────────────────

    private static final int GRID_X    = 86;   // column grid left edge (GUI-relative)
    private static final int GRID_Y    = 11;   // column grid top edge
    private static final int CELL      = 10;   // each column cell size in pixels
    private static final int GRID      = 15;   // 15×15 columns

    // ─── Type → texture UV offset (10-px icons at y=172, 18-px at y=238) ────
    //
    // These offsets are taken 1:1 from the real gui_rbmk_console.png sprite
    // sheet (verified against the original 1.7.10 GUIRBMKConsole.java /
    // TileEntityRBMKConsole.ColumnType.offset values). The original enum had
    // extra members (FUEL_SIM, CONTROL_AUTO, BREEDER, HEATEX) that the
    // modernized ColumnType enum collapsed away; where that happened we point
    // at the closest matching icon (e.g. HEATER -> the original "HEATEX"
    // heat-exchanger icon at 130).
    private static int typeOffset10(ColumnType t) {
        return switch (t) {
            case BLANK     -> 0;
            case FUEL      -> 10;
            case CONTROL   -> 20;
            case BOILER    -> 40;
            case MODERATOR -> 50;
            case ABSORBER  -> 60;
            case REFLECTOR -> 70;
            case OUTGASSER -> 80;
            case STORAGE   -> 110;
            case COOLER    -> 120;
            case HEATER    -> 130;
        };
    }

    // The original texture only has 6 mini-screen mode icons at y=238
    // (NONE/COL_TEMP/ROD_EXTRACTION/FUEL_DEPLETION/FUEL_POISON/FUEL_TEMP —
    // see original ScreenType enum), because the original console's 6 mini
    // screens cycled through *display modes*, not column types. The
    // modernized BlockEntity has no ScreenType/RBMKScreen equivalent and
    // instead cycles `screenTypes[]` through ColumnType (11 values) — a
    // deliberate, out-of-scope data-model difference noted by the prior
    // audit. We can't restore the original 6 display-mode icons since that
    // data doesn't exist here, so we wrap ColumnType's ordinal into the 6
    // available icon slots to stay within the texture's actual bounds
    // instead of sampling garbage pixels past x=108.
    private static int typeOffset18(ColumnType t) {
        return (t.ordinal() % 6) * 18;
    }

    // ─── State ───────────────────────────────────────────────────────────────

    private final MachineRbmkConsoleBlockEntity console;
    private final boolean[] selection = new boolean[GRID * GRID];
    private boolean az5Lid    = true;
    private long    lastAz5   = 0;
    private EditBox rodLevelField;

    // ─── Constructor ─────────────────────────────────────────────────────────

    public GUIMachineRbmkConsole(MachineRbmkConsoleMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.console   = menu.getBlockEntity();
        this.imageWidth  = 244;
        this.imageHeight = 172;
    }

    // ─── Init ─────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();
        rodLevelField = new EditBox(this.font, this.leftPos + 9, this.topPos + 84, 35, 9,
                Component.empty());
        rodLevelField.setTextColor(0x00ff00);
        rodLevelField.setBordered(false);
        rodLevelField.setMaxLength(3);
        rodLevelField.setValue("100");
        addWidget(rodLevelField);
    }

    // ─── Rendering ────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        super.render(g, mx, my, pt);
        renderColumnTooltip(g, mx, my);
        renderButtonTooltips(g, mx, my);
        this.renderTooltip(g, mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        // Background
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // AZ-5 lid
        if (az5Lid)
            g.blit(TEXTURE, leftPos + 30, topPos + 138, 228, 172, 28, 28);

        // Screen type icons (6 mini screens, 3 rows × 2 columns, 40 px apart, 18×18)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 2; col++) {
                int id = row * 2 + col;
                if (id < MachineRbmkConsoleBlockEntity.SCREENS) {
                    ColumnType st = console.screenTypes[id];
                    g.blit(TEXTURE, leftPos + 6 + 40 * col, topPos + 8 + 21 * row,
                            typeOffset18(st), 238, 18, 18);
                }
            }
        }

        // Column grid
        renderColumnGrid(g);

        // Flux graph
        renderFluxGraph(g);

        // Level input field
        rodLevelField.render(g, 0, 0, pt);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        // No labels — the console fills the full width
    }

    // ─── Column grid ──────────────────────────────────────────────────────────

    private void renderColumnGrid(GuiGraphics g) {
        for (int i = 0; i < MachineRbmkConsoleBlockEntity.AREA; i++) {
            RBMKColumnData col = console.columns[i];
            if (col == null) continue;

            int cx = leftPos + GRID_X + CELL * (i % GRID);
            int cy = topPos  + GRID_Y + CELL * (i / GRID);

            // Column type tile
            g.blit(TEXTURE, cx, cy, typeOffset10(col.type), 172, CELL, CELL);

            // Heat bar (column heat → height 0-10)
            double heat    = col.data.getDouble("heat");
            double maxHeat = col.data.getDouble("maxHeat");
            if (maxHeat > 0) {
                int h = Mth.clamp((int) Math.ceil((heat - 20) * CELL / maxHeat), 0, CELL);
                if (h > 0) g.blit(TEXTURE, cx, cy + CELL - h, 0, 192 - h, CELL, h);
            }

            // Type-specific overlays
            renderColumnOverlay(g, col, cx, cy);

            // Selection highlight
            if (selection[i])
                g.blit(TEXTURE, cx, cy, 0, 192, CELL, CELL);
        }
    }

    private void renderColumnOverlay(GuiGraphics g, RBMKColumnData col, int cx, int cy) {
        switch (col.type) {
            case CONTROL -> {
                int color = col.data.getShort("color");
                if (color >= 0 && color < 5)
                    g.blit(TEXTURE, cx, cy, color * CELL, 202, CELL, CELL);
                int fr = 8 - (int) Math.ceil(col.data.getDouble("level") * 8);
                g.blit(TEXTURE, cx + 4, cy + 1, 24, 183, 2, Math.max(0, fr));
            }
            case FUEL -> {
                if (col.data.contains("c_heat")) {
                    double cHeat   = col.data.getDouble("c_heat");
                    double cMax    = col.data.getDouble("c_maxHeat");
                    double enrich  = col.data.getDouble("enrichment");
                    double xenon   = col.data.getDouble("xenon");

                    int fh = cMax > 0 ? Mth.clamp((int) Math.ceil((cHeat - 20) * 8 / cMax), 0, 8) : 0;
                    int fe = Mth.clamp((int) Math.ceil(enrich * 8), 0, 8);
                    int fx = Mth.clamp((int) Math.ceil(xenon  * 8 / 100), 0, 8);

                    if (fh > 0) g.blit(TEXTURE, cx + 1, cy + CELL - fh - 1, 11, 191 - fh, 2, fh);
                    if (fe > 0) g.blit(TEXTURE, cx + 4, cy + CELL - fe - 1, 14, 191 - fe, 2, fe);
                    if (fx > 0) g.blit(TEXTURE, cx + 7, cy + CELL - fx - 1, 17, 191 - fx, 2, fx);
                }
            }
            case BOILER -> {
                int maxWater = col.data.getInt("maxWater");
                int maxSteam = col.data.getInt("maxSteam");
                int fw = maxWater > 0 ? Mth.clamp((int) Math.ceil((float) col.data.getInt("water") * 8 / maxWater), 0, 8) : 0;
                int fs = maxSteam > 0 ? Mth.clamp((int) Math.ceil((float) col.data.getInt("steam") * 8 / maxSteam), 0, 8) : 0;
                if (fw > 0) g.blit(TEXTURE, cx + 1, cy + CELL - fw - 1, 41, 191 - fw, 3, fw);
                if (fs > 0) g.blit(TEXTURE, cx + 6, cy + CELL - fs - 1, 46, 191 - fs, 3, fs);
                // Steam grade indicator
                int grade = col.data.getShort("steamGrade");
                if (grade >= 0 && grade < 4)
                    g.blit(TEXTURE, cx + 4, cy + 1 + grade * 2, 44, 183 + grade * 2, 2, 2);
            }
            case HEATER -> {
                // Matches the original HEATEX case (heat exchanger water/steam gauges).
                // NOTE: RBMKHeaterBlockEntity currently doesn't populate "water"/"steam"/
                // "maxWater"/"maxSteam" in getNBTForConsole() (out of scope to add here —
                // that's BlockEntity data, not rendering), so this overlay is a no-op
                // until that data is exposed; kept guarded so it activates automatically
                // once it is.
                if (col.data.contains("maxWater") && col.data.contains("maxSteam")) {
                    int maxWater = col.data.getInt("maxWater");
                    int maxSteam = col.data.getInt("maxSteam");
                    int cc = maxWater > 0 ? Mth.clamp((int) Math.ceil((float) col.data.getInt("water") * 8 / maxWater), 0, 8) : 0;
                    int hc = maxSteam > 0 ? Mth.clamp((int) Math.ceil((float) col.data.getInt("steam") * 8 / maxSteam), 0, 8) : 0;
                    if (cc > 0) g.blit(TEXTURE, cx + 1, cy + CELL - cc - 1, 131, 191 - cc, 3, cc);
                    if (hc > 0) g.blit(TEXTURE, cx + 6, cy + CELL - hc - 1, 136, 191 - hc, 3, hc);
                }
            }
            default -> {}
        }
    }

    // ─── Flux graph ───────────────────────────────────────────────────────────

    private void renderFluxGraph(GuiGraphics g) {
        int[] buf = console.fluxBuffer;
        if (buf == null || buf.length < 2) return;

        int highest = Integer.MIN_VALUE, lowest = Integer.MAX_VALUE;
        for (int v : buf) { if (v > highest) highest = v; if (v < lowest) lowest = v; }
        int range = Math.max(highest - lowest, 1);

        // Graph bounds (GUI-relative)
        int gx0 = leftPos  + 7;
        int gy0 = topPos   + 103;  // top of graph
        int gH  = 24;
        int gW  = 74;

        // Draw bar graph using fill (avoids Tesselator API differences across versions)
        int barW = Math.max(1, gW / buf.length);
        for (int i = 0; i < buf.length; i++) {
            int barH = (int) ((buf[i] - lowest) * (float) gH / range);
            if (barH > 0) {
                int bx = gx0 + i * gW / buf.length;
                g.fill(bx, gy0 + gH - barH, bx + barW, gy0 + gH, 0xFF00FF00);
            }
        }

        // Labels (half-scale), mirrored on both edges of the graph like the original
        String hiStr = String.valueOf(highest);
        String loStr = String.valueOf(lowest);
        int hiW = this.font.width(hiStr);
        int loW = this.font.width(loStr);
        int rightX = gx0 + gW - 1;

        g.pose().pushPose();
        g.pose().scale(0.5f, 0.5f, 1f);
        g.drawString(this.font, hiStr, (int) (gx0 * 2f), (int) ((topPos + 98) * 2f), 0x00ff00, false);
        g.drawString(this.font, hiStr, (int) ((rightX - hiW * 0.5f) * 2f), (int) ((topPos + 98) * 2f), 0x00ff00, false);
        g.drawString(this.font, loStr, (int) (gx0 * 2f), (int) ((topPos + 127) * 2f), 0x00ff00, false);
        g.drawString(this.font, loStr, (int) ((rightX - loW * 0.5f) * 2f), (int) ((topPos + 127) * 2f), 0x00ff00, false);
        g.pose().popPose();
    }

    // ─── Tooltips ─────────────────────────────────────────────────────────────

    private void renderColumnTooltip(GuiGraphics g, int mx, int my) {
        if (mx < leftPos + GRID_X || mx >= leftPos + GRID_X + GRID * CELL) return;
        if (my < topPos  + GRID_Y || my >= topPos  + GRID_Y + GRID * CELL) return;

        int idx = ((mx - GRID_X - leftPos) / CELL) + ((my - GRID_Y - topPos) / CELL) * GRID;
        if (idx < 0 || idx >= MachineRbmkConsoleBlockEntity.AREA) return;
        RBMKColumnData col = console.columns[idx];
        if (col == null) return;

        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(col.type.name()));
        double heat = col.data.getDouble("heat");
        lines.add(Component.literal(String.format("Heat: %.1f / %.0f°C", heat, col.data.getDouble("maxHeat"))));
        if (col.type == ColumnType.FUEL && col.data.contains("enrichment")) {
            lines.add(Component.literal(String.format("Enrich: %.1f%%", col.data.getDouble("enrichment") * 100)));
            lines.add(Component.literal(String.format("Xenon:  %.1f%%", col.data.getDouble("xenon"))));
        }
        if (col.type == ColumnType.CONTROL)
            lines.add(Component.literal(String.format("Level: %.1f%%", col.data.getDouble("level") * 100)));

        g.renderTooltip(this.font, lines, java.util.Optional.empty(), mx, my);
    }

    private void renderButtonTooltips(GuiGraphics g, int mx, int my) {
        tooltip(g, mx, my, leftPos + 61, topPos + 70, 10, 10, "Select all control rods");
        tooltip(g, mx, my, leftPos + 72, topPos + 70, 10, 10, "Deselect all");
        tooltip(g, mx, my, leftPos + 70, topPos + 82, 12, 12, "Cycle boiler steam grade");
        tooltip(g, mx, my, leftPos + 48, topPos + 82, 12, 12, "Apply control rod level");
        tooltip(g, mx, my, leftPos + 30, topPos + 138, 28, 28, az5Lid ? "AZ-5 (lift cover)" : "AZ-5 emergency shutdown");

        String[] colorNames = { "Red", "Yellow", "Green", "Blue", "Purple" };
        for (int k = 0; k < 5; k++)
            tooltip(g, mx, my, leftPos + 6 + k * 11, topPos + 70, 10, 10,
                    "Left: select " + colorNames[k] + " group  Right: assign " + colorNames[k] + " group");
    }

    private void tooltip(GuiGraphics g, int mx, int my, int x, int y, int w, int h, String text) {
        if (mx >= x && mx < x + w && my > y && my <= y + h)
            g.renderTooltip(this.font, Component.literal(text), mx, my);
    }

    // ─── Mouse input ──────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        rodLevelField.mouseClicked(mx, my, btn);

        int imx = (int) mx, imy = (int) my;

        // Column grid — toggle selection
        if (imx >= leftPos + GRID_X && imx < leftPos + GRID_X + GRID * CELL
                && imy >= topPos + GRID_Y && imy < topPos + GRID_Y + GRID * CELL) {
            int idx = ((imx - GRID_X - leftPos) / CELL) + ((imy - GRID_Y - topPos) / CELL) * GRID;
            if (idx >= 0 && idx < selection.length && console.columns[idx] != null) {
                selection[idx] = !selection[idx];
                playClick(0.75f + (selection[idx] ? 0.25f : 0f));
                return true;
            }
        }

        // Select all control rods
        if (inBtn(imx, imy, 61, 70, 10, 10)) {
            java.util.Arrays.fill(selection, false);
            for (int j = 0; j < MachineRbmkConsoleBlockEntity.AREA; j++)
                if (console.columns[j] != null && console.columns[j].type == ColumnType.CONTROL)
                    selection[j] = true;
            playClick(1.5f); return true;
        }

        // Deselect all
        if (inBtn(imx, imy, 72, 70, 10, 10)) {
            java.util.Arrays.fill(selection, false);
            playClick(0.5f); return true;
        }

        // Color group buttons
        for (int k = 0; k < 5; k++) {
            if (inBtn(imx, imy, 6 + k * 11, 70, 10, 10)) {
                if (btn == 0) { // left: select group
                    java.util.Arrays.fill(selection, false);
                    for (int j = 0; j < MachineRbmkConsoleBlockEntity.AREA; j++)
                        if (console.columns[j] != null && console.columns[j].type == ColumnType.CONTROL
                                && console.columns[j].data.getShort("color") == k)
                            selection[j] = true;
                } else if (btn == 1) { // right: assign group
                    RBMKConsoleControlPacket.sendAssignColor(console.getBlockPos(), k, selectedIndices());
                }
                playClick(0.8f + k * 0.1f); return true;
            }
        }

        // Apply control rod level
        if (inBtn(imx, imy, 48, 82, 12, 12)) {
            String txt = rodLevelField.getValue();
            try {
                int pct = Mth.clamp(Integer.parseInt(txt.trim()), 0, 100);
                rodLevelField.setValue(String.valueOf(pct));
                RBMKConsoleControlPacket.sendSetLevel(console.getBlockPos(), pct / 100.0, selectedIndices());
                playClick(1f);
            } catch (NumberFormatException ignored) {}
            return true;
        }

        // Cycle boiler steam grade (for selected boiler columns)
        if (inBtn(imx, imy, 70, 82, 12, 12)) {
            RBMKConsoleControlPacket.sendAssignColor(console.getBlockPos(), -1, selectedIndices()); // placeholder
            playClick(1f); return true;
        }

        // Screen type cycle buttons (left column of each mini-screen pair)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 2; col++) {
                int id = row * 2 + col;
                if (inBtn(imx, imy, 6 + 40 * col, 8 + 21 * row, 18, 18)) {
                    RBMKConsoleControlPacket.sendCycleScreen(console.getBlockPos(), id);
                    playClick(0.5f); return true;
                }
                // Screen assign buttons (right of each pair)
                if (inBtn(imx, imy, 24 + 40 * col, 8 + 21 * row, 18, 18)) {
                    RBMKConsoleControlPacket.sendAssignScreen(console.getBlockPos(), id, selectedIndices());
                    playClick(0.75f); return true;
                }
            }
        }

        // AZ-5
        if (inBtn(imx, imy, 30, 138, 28, 28)) {
            if (az5Lid) {
                az5Lid = false;
                Minecraft.getInstance().getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.IRON_TRAPDOOR_OPEN, 0.5f));
            } else if (System.currentTimeMillis() - lastAz5 > 3000) {
                lastAz5 = System.currentTimeMillis();
                RBMKConsoleControlPacket.sendAZ5(console.getBlockPos());
                Minecraft.getInstance().getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.BEACON_DEACTIVATE, 1f));
            }
            return true;
        }

        return super.mouseClicked(mx, my, btn);
    }

    // ─── Keyboard ─────────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(int key, int scanCode, int mods) {
        if (rodLevelField.isFocused()) {
            if (rodLevelField.keyPressed(key, scanCode, mods)) return true;
            if (key == 256) { rodLevelField.setFocused(false); return true; }
        }
        return super.keyPressed(key, scanCode, mods);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (rodLevelField.isFocused()) return rodLevelField.charTyped(c, mods);
        return super.charTyped(c, mods);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private boolean inBtn(int mx, int my, int rx, int ry, int w, int h) {
        return mx >= leftPos + rx && mx < leftPos + rx + w
            && my > topPos + ry && my <= topPos + ry + h;
    }

    private int[] selectedIndices() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < selection.length; i++) if (selection[i]) list.add(i);
        return list.stream().mapToInt(Integer::intValue).toArray();
    }

    private void playClick(float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
    }
}
