package com.hbm_m.inventory.gui;
import com.hbm_m.client.GuiCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import com.hbm_m.blockentity.machines.MachineRadarBlockEntity;
import com.hbm_m.inventory.menu.MachineRadarMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.UpdateRadarC2SPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * GUI радара (порт GUIMachineRadarNT из 1.7.10, текстура gui_radar_nt.png).
 *
 * Безслотовый экран 216x234: радарная развёртка, карта высот (showMap), метки целей,
 * кнопки-тумблеры слева и кнопки перехода/очистки. Слоты вынесены в {@link GUIMachineRadarNTSlots}.
 *
 * Порт дословный: UV тумблеров 238/4..54, боковая панель двумя кусками (+84 и +154),
 * бар энергии 8,221/0,234/200×16, развёртка GL_SMOOTH-клином, метки по blipLevel.
 */
public class GUIMachineRadarNT extends GuiInfoScreen<MachineRadarMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_radar_nt.png");

    private static final int MAP_SIZE = 200 - 8;     // (200D - 8D) в 1.7.10
    private static final int MAP_CENTER_X = 108;
    private static final int MAP_CENTER_Y = 117;
    private static final int TOGGLE_X = -10;

    // Y тумблеров (порт GUIMachineRadarNT.mouseClicked).
    private static final int BTN_MISSILES = 88;
    private static final int BTN_SHELLS = 98;
    private static final int BTN_PLAYERS = 108;
    private static final int BTN_SMART = 118;
    private static final int BTN_RED = 128;
    private static final int BTN_MAP = 138;
    private static final int BTN_GUI1 = 158;
    private static final int BTN_CLEAR = 178;

    private final MachineRadarBlockEntity radar;
    private final Random noiseRandom = new Random();
    /** Текстура карты вместо 40 000 квадов, пересобираемых каждый кадр. */
    private DynamicTexture heightMapTexture;
    private ResourceLocation heightMapTextureLocation;
    private final byte[] renderedMap = new byte[MachineRadarBlockEntity.MAP_LENGTH];
    private boolean hasRenderedMap;

    private int lastMouseX;
    private int lastMouseY;

    public GUIMachineRadarNT(MachineRadarMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.radar = menu.getBlockEntity();
        this.imageWidth = 216;
        this.imageHeight = 234;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        // Боковая панель — два куска (порт drawTexturedModalRect 224,0/66).
        guiGraphics.blit(TEXTURE, this.leftPos - 14, this.topPos + 84, 224, 0, 14, 66);
        guiGraphics.blit(TEXTURE, this.leftPos - 14, this.topPos + 154, 224, 66, 14, 36);

        renderPowerBar(guiGraphics);
        renderModeButtons(guiGraphics);

        // Экран (развёртка/карта/метки) рисуется только при достаточной энергии
        // (порт if(radar.power < radar.consumption) return).
        if (radar.getEnergyStored() < MachineRadarBlockEntity.ENERGY_CONSUMPTION) {
            return;
        }

        if (radar.jammed) {
            renderJammedNoise(guiGraphics);
        } else {
            if (radar.showMap) {
                // В режиме HeightMap статический фон экрана не должен просвечивать
                // между ещё не полученными точками карты.
                guiGraphics.fill(leftPos + 8, topPos + 17,
                        leftPos + 8 + MachineRadarBlockEntity.MAP_DIM,
                        topPos + 17 + MachineRadarBlockEntity.MAP_DIM,
                        0xFF000000);
                renderHeightMap(guiGraphics);
            }
            renderSweep(guiGraphics, partialTick);
            renderRadarContacts(guiGraphics);
        }
    }

    /** Бар энергии: i = power*200/maxPower, source (0,234). */
    private void renderPowerBar(GuiGraphics guiGraphics) {
        int bar = (int) radar.getPowerScaled(200);
        if (bar > 0) {
            guiGraphics.blit(TEXTURE, leftPos + 8, topPos + 221, 0, 234, bar, 16);
        }
    }

    /**
     * Тумблеры: XOR с jammed-шумом (порт scanMissiles ^ (jammed && rand.nextBoolean())).
     * UV каждого индикатора: 238, 4 + index*10.
     */
    private void renderModeButtons(GuiGraphics guiGraphics) {
        if (radar.scanMissiles != (radar.jammed && noiseRandom.nextBoolean())) {
            guiGraphics.blit(TEXTURE, leftPos + TOGGLE_X, topPos + BTN_MISSILES, 238, 4, 8, 8);
        }
        if (radar.scanShells != (radar.jammed && noiseRandom.nextBoolean())) {
            guiGraphics.blit(TEXTURE, leftPos + TOGGLE_X, topPos + BTN_SHELLS, 238, 14, 8, 8);
        }
        if (radar.scanPlayers != (radar.jammed && noiseRandom.nextBoolean())) {
            guiGraphics.blit(TEXTURE, leftPos + TOGGLE_X, topPos + BTN_PLAYERS, 238, 24, 8, 8);
        }
        if (radar.smartMode != (radar.jammed && noiseRandom.nextBoolean())) {
            guiGraphics.blit(TEXTURE, leftPos + TOGGLE_X, topPos + BTN_SMART, 238, 34, 8, 8);
        }
        if (radar.redMode != (radar.jammed && noiseRandom.nextBoolean())) {
            guiGraphics.blit(TEXTURE, leftPos + TOGGLE_X, topPos + BTN_RED, 238, 44, 8, 8);
        }
        if (radar.showMap != (radar.jammed && noiseRandom.nextBoolean())) {
            guiGraphics.blit(TEXTURE, leftPos + TOGGLE_X, topPos + BTN_MAP, 238, 54, 8, 8);
        }
    }

    private void renderJammedNoise(GuiGraphics guiGraphics) {
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                guiGraphics.blit(TEXTURE, leftPos + 8 + i * 40, topPos + 17 + j * 40,
                        216, 118 + noiseRandom.nextInt(81), 40, 40);
            }
        }
    }

    /**
     * Карта высот. Обновляется только при изменении данных и рендерится одним quad'ом.
     * Старый вариант создавал до 160 000 вершин на каждый кадр, что приводило к
     * постоянному росту BufferBuilder и GC/OOM при включённой HeightMap.
     */
    private void renderHeightMap(GuiGraphics guiGraphics) {
        byte[] map = radar.map;
        if (map == null || map.length < MachineRadarBlockEntity.MAP_LENGTH) {
            return;
        }

        if (heightMapTexture == null) {
            heightMapTexture = new DynamicTexture(new NativeImage(
                    MachineRadarBlockEntity.MAP_DIM, MachineRadarBlockEntity.MAP_DIM, true));
            heightMapTextureLocation = Minecraft.getInstance().getTextureManager().register(
                    "hbm_m_radar_heightmap_" + Integer.toHexString(System.identityHashCode(this)),
                    heightMapTexture);
        }

        boolean changed = !hasRenderedMap;
        if (!changed) {
            for (int i = 0; i < MachineRadarBlockEntity.MAP_LENGTH; i++) {
                if (renderedMap[i] != map[i]) {
                    changed = true;
                    break;
                }
            }
        }

        if (changed) {
            NativeImage image = heightMapTexture.getPixels();
            for (int i = 0; i < MachineRadarBlockEntity.MAP_LENGTH; i++) {
                int height = Byte.toUnsignedInt(map[i]);
                int x = i % MachineRadarBlockEntity.MAP_DIM;
                int z = i / MachineRadarBlockEntity.MAP_DIM;
                if (height == 0) {
                    // Прозрачный пиксель оставляет видимыми сетку и компас фоновой текстуры.
                    image.setPixelRGBA(x, z, 0x00000000);
                    continue;
                }
                int green = net.minecraft.util.Mth.clamp((height - 50) * 255 / 78, 0, 255);
                image.setPixelRGBA(x, z, 0xFF000000 | green << 8);
            }
            heightMapTexture.upload();
            System.arraycopy(map, 0, renderedMap, 0, MachineRadarBlockEntity.MAP_LENGTH);
            hasRenderedMap = true;
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0D, 0.0D, 50.0D); // Слой над текстурой интерфейса
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(heightMapTextureLocation, leftPos + 8, topPos + 17,
                0, 0, MachineRadarBlockEntity.MAP_DIM, MachineRadarBlockEntity.MAP_DIM,
                MachineRadarBlockEntity.MAP_DIM, MachineRadarBlockEntity.MAP_DIM);
        guiGraphics.pose().popPose();
    }

    /**
     * Развёртка (sweep): вращающийся зелёный «веер» с GL_SMOOTH-альфой
     * (порт Vec3 tr/tl/bl + rotateAroundZ + QUAD).
     */
    private void renderSweep(GuiGraphics guiGraphics, float partialTick) {
        float rot = (float) -Math.toRadians(
                radar.prevRotation + (radar.rotation - radar.prevRotation) * partialTick + 180F);
        // tr = (100,0) rot; tl = (100,0) rot+0.25; bl = (0,-5) rot.
        double[] tr = rotate(100, 0, rot);
        double[] tl = rotate(100, 0, rot + 0.25F);
        double[] bl = rotate(0, -5, rot);

        int cx = leftPos + MAP_CENTER_X;
        int cy = topPos + MAP_CENTER_Y;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0D, 0.0D, 100.0D); // Поверх фона и HeightMap

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        org.joml.Matrix4f matrix = guiGraphics.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        // Два треугольника вместо QUAD: так клин не зависит от winding/culling.
        buffer.vertex(matrix, cx, cy, 0F).color(0, 255, 0, 0).endVertex();
        buffer.vertex(matrix, cx + (float) tr[0], cy + (float) tr[1], 0F).color(0, 255, 0, 220).endVertex();
        buffer.vertex(matrix, cx + (float) tl[0], cy + (float) tl[1], 0F).color(0, 255, 0, 0).endVertex();
        buffer.vertex(matrix, cx, cy, 0F).color(0, 255, 0, 0).endVertex();
        buffer.vertex(matrix, cx + (float) tl[0], cy + (float) tl[1], 0F).color(0, 255, 0, 0).endVertex();
        buffer.vertex(matrix, cx + (float) bl[0], cy + (float) bl[1], 0F).color(0, 255, 0, 0).endVertex();
        BufferUploader.drawWithShader(buffer.end());

        // Яркая, четкая линия-указатель на ведущем крае радара
        double[] lineOffset1 = rotate(100, -0.5, rot);
        double[] lineOffset2 = rotate(100, 0.5, rot);
        buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, cx, cy, 0F).color(0F, 1F, 0F, 1F).endVertex();
        buffer.vertex(matrix, cx + (float) lineOffset1[0], cy + (float) lineOffset1[1], 0F).color(0F, 1F, 0F, 1F).endVertex();
        buffer.vertex(matrix, cx + (float) lineOffset2[0], cy + (float) lineOffset2[1], 0F).color(0F, 1F, 0F, 1F).endVertex();

        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        guiGraphics.pose().popPose();
    }

    /**
     * Точная реплика {@code Vec3.rotateAroundZ} из 1.7.10 — именно ей оригинальный
     * GUIMachineRadarNT вращает клин развёртки. Minecraft применяет транспонированную
     * форму поворота, а не стандартную CCW-матрицу: x' = x·cos + y·sin, y' = y·cos − x·sin.
     * Прежняя версия использовала CCW-матрицу (x' = x·cos − y·sin, y' = x·sin + y·cos),
     * из-за чего развёртка крутилась в сторону, противоположную оригиналу.
     */
    private static double[] rotate(double x, double y, float rad) {
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        return new double[] { x * cos + y * sin, y * cos - x * sin };
    }

    @Override
    public void removed() {
        if (heightMapTextureLocation != null) {
            Minecraft.getInstance().getTextureManager().release(heightMapTextureLocation);
            heightMapTextureLocation = null;
            heightMapTexture = null;
        }
        super.removed();
    }

    /** Метки целей: иконка 8x8 по blipLevel (contact[4]) из атласа (216, 8*t). */
    private void renderRadarContacts(GuiGraphics guiGraphics) {
        int radarWidth = radar.getRange() * 2 + 1;
        int radarX = radar.getBlockPos().getX();
        int radarZ = radar.getBlockPos().getZ();

        for (int[] contact : radar.nearbyMissiles) {
            if (contact == null || contact.length < 5) {
                continue;
            }
            double px = (contact[0] - radarX) * 1.0D / radarWidth * MAP_SIZE - 4D;
            double py = (contact[2] - radarZ) * 1.0D / radarWidth * MAP_SIZE - 4D;
            int iconIndex = Math.max(0, contact[4]);
            guiGraphics.blit(TEXTURE,
                    leftPos + MAP_CENTER_X + (int) px,
                    topPos + MAP_CENTER_Y + (int) py,
                    150, 216, 8 * iconIndex, 8, 8, 256, 256);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // В 1.7.10 у NT-экрана нет текстовых лейблов поверх.
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        drawElectricityInfo(guiGraphics, mouseX, mouseY,
                8, 221, 200, 7,
                radar.getEnergyStored(), radar.getMaxEnergyStored());

        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                TOGGLE_X, BTN_MISSILES, 8, 8, mouseX, mouseY,
                Component.translatable("gui.hbm_m.radar.tooltip.detect_missiles"));
        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                TOGGLE_X, BTN_SHELLS, 8, 8, mouseX, mouseY,
                Component.translatable("gui.hbm_m.radar.tooltip.detect_shells"));
        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                TOGGLE_X, BTN_PLAYERS, 8, 8, mouseX, mouseY,
                Component.translatable("gui.hbm_m.radar.tooltip.detect_players"));
        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                TOGGLE_X, BTN_SMART, 8, 8, mouseX, mouseY,
                Component.translatable("gui.hbm_m.radar.tooltip.smart_mode"),
                Component.translatable("gui.hbm_m.radar.tooltip.smart_mode.desc"));
        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                TOGGLE_X, BTN_RED, 8, 8, mouseX, mouseY,
                Component.translatable("gui.hbm_m.radar.tooltip.red_mode"),
                Component.translatable("gui.hbm_m.radar.tooltip.red_mode.on"),
                Component.translatable("gui.hbm_m.radar.tooltip.red_mode.off"));
        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                TOGGLE_X, BTN_MAP, 8, 8, mouseX, mouseY,
                Component.translatable("gui.hbm_m.radar.tooltip.show_map"));
        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                TOGGLE_X, BTN_GUI1, 8, 8, mouseX, mouseY,
                Component.translatable("gui.hbm_m.radar.tooltip.toggle_gui"));
        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                TOGGLE_X, BTN_CLEAR, 8, 8, mouseX, mouseY,
                Component.translatable("gui.hbm_m.radar.tooltip.clear_map"));

        renderContactHoverTooltip(guiGraphics, mouseX, mouseY);
        renderCoordTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderContactHoverTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (radar.jammed || radar.nearbyMissiles.isEmpty()) {
            return;
        }
        int radarWidth = radar.getRange() * 2 + 1;
        int radarX = radar.getBlockPos().getX();
        int radarZ = radar.getBlockPos().getZ();

        for (int[] contact : radar.nearbyMissiles) {
            if (contact == null || contact.length < 5) {
                continue;
            }
            int cx = leftPos + MAP_CENTER_X
                    + (int) ((contact[0] - radarX) * 1.0D / radarWidth * MAP_SIZE);
            int cz = topPos + MAP_CENTER_Y
                    + (int) ((contact[2] - radarZ) * 1.0D / radarWidth * MAP_SIZE);
            if (mouseX + 5 > cx && mouseX - 4 <= cx && mouseY + 5 > cz && mouseY - 4 <= cz) {
                List<Component> text = new ArrayList<>();
                text.add(Component.literal(radar.getTargetTypeName(contact[4])));
                text.add(Component.literal(contact[0] + " / " + contact[2]));
                text.add(Component.translatable("gui.hbm_m.radar.contact.alt", contact[1]));
                guiGraphics.renderComponentTooltip(this.font, text, cx, cz);
                return;
            }
        }
    }

    private void renderCoordTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!isPointInRect(8, 17, 200, 200, mouseX, mouseY)) {
            return;
        }
        // Если курсор наведён на метку цели — координатный тултип прячем,
        // остаётся только тултип контакта (renderContactHoverTooltip).
        if (findContactAt(mouseX, mouseY) != null) {
            return;
        }
        int radarWidth = radar.getRange() * 2 + 1;
        int tX = (int) ((mouseX - leftPos - MAP_CENTER_X) * 1.0D / MAP_SIZE * radarWidth
                + radar.getBlockPos().getX());
        int tZ = (int) ((mouseY - topPos - MAP_CENTER_Y) * 1.0D / MAP_SIZE * radarWidth
                + radar.getBlockPos().getZ());
        guiGraphics.renderTooltip(this.font, Component.literal(tX + " / " + tZ), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mx = (int) mouseX;
            int my = (int) mouseY;
            if (sendToggleIfClicked(mx, my, BTN_MISSILES, MachineRadarBlockEntity.ACTION_TOGGLE_MISSILES)) return true;
            if (sendToggleIfClicked(mx, my, BTN_SHELLS, MachineRadarBlockEntity.ACTION_TOGGLE_SHELLS)) return true;
            if (sendToggleIfClicked(mx, my, BTN_PLAYERS, MachineRadarBlockEntity.ACTION_TOGGLE_PLAYERS)) return true;
            if (sendToggleIfClicked(mx, my, BTN_SMART, MachineRadarBlockEntity.ACTION_TOGGLE_SMART)) return true;
            if (sendToggleIfClicked(mx, my, BTN_RED, MachineRadarBlockEntity.ACTION_TOGGLE_RED)) return true;
            if (sendToggleIfClicked(mx, my, BTN_MAP, MachineRadarBlockEntity.ACTION_TOGGLE_MAP)) return true;
            // gui1 — переход в GUI слотов; clear — очистка карты.
            if (isPointInRect(TOGGLE_X, BTN_GUI1, 8, 8, mx, my)) {
                playClickSound();
                ModPacketHandler.sendToServer(ModPacketHandler.UPDATE_RADAR,
                        UpdateRadarC2SPacket.openSlots(radar.getBlockPos()));
                return true;
            }
            if (isPointInRect(TOGGLE_X, BTN_CLEAR, 8, 8, mx, my)) {
                playClickSound();
                ModPacketHandler.sendToServer(ModPacketHandler.UPDATE_RADAR,
                        new UpdateRadarC2SPacket(radar.getBlockPos(), MachineRadarBlockEntity.ACTION_CLEAR_MAP));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean sendToggleIfClicked(int mx, int my, int btnY, int action) {
        if (isPointInRect(TOGGLE_X, btnY, 8, 8, mx, my)) {
            playClickSound();
            ModPacketHandler.sendToServer(ModPacketHandler.UPDATE_RADAR,
                    new UpdateRadarC2SPacket(radar.getBlockPos(), action));
            return true;
        }
        return false;
    }

    /**
     * Клавиши 1-8 над целью → пуск через соответствующий линк-слот (0..7)
     * (порт GUIMachineRadarNT.keyTyped: c='1'..'8' → link=c-'1').
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode >= org.lwjgl.glfw.GLFW.GLFW_KEY_1 && keyCode <= org.lwjgl.glfw.GLFW.GLFW_KEY_8) {
            int linkSlot = keyCode - org.lwjgl.glfw.GLFW.GLFW_KEY_1; // 0..7
            if (isPointInRect(8, 17, 200, 200, lastMouseX, lastMouseY)) {
                int[] target = findContactAt(lastMouseX, lastMouseY);
                if (target != null && target.length >= 7) {
                    ModPacketHandler.sendToServer(ModPacketHandler.UPDATE_RADAR,
                            UpdateRadarC2SPacket.launchAtEntity(radar.getBlockPos(), linkSlot, target[6]));
                    return true;
                }
                // Пустая область → пуск по координатам.
                int radarWidth = radar.getRange() * 2 + 1;
                int tX = (int) ((lastMouseX - leftPos - MAP_CENTER_X) * 1.0D / MAP_SIZE * radarWidth
                        + radar.getBlockPos().getX());
                int tZ = (int) ((lastMouseY - topPos - MAP_CENTER_Y) * 1.0D / MAP_SIZE * radarWidth
                        + radar.getBlockPos().getZ());
                ModPacketHandler.sendToServer(ModPacketHandler.UPDATE_RADAR,
                        UpdateRadarC2SPacket.launchAtCoords(radar.getBlockPos(), linkSlot, tX, tZ));
                return true;
            }
        }
        return false;
    }

    @org.jetbrains.annotations.Nullable
    private int[] findContactAt(int mx, int my) {
        int radarWidth = radar.getRange() * 2 + 1;
        int radarX = radar.getBlockPos().getX();
        int radarZ = radar.getBlockPos().getZ();
        for (int[] contact : radar.nearbyMissiles) {
            if (contact == null || contact.length < 5) {
                continue;
            }
            int cx = leftPos + MAP_CENTER_X
                    + (int) ((contact[0] - radarX) * 1.0D / radarWidth * MAP_SIZE);
            int cz = topPos + MAP_CENTER_Y
                    + (int) ((contact[2] - radarZ) * 1.0D / radarWidth * MAP_SIZE);
            if (mx + 5 > cx && mx - 4 <= cx && my + 5 > cz && my - 4 <= cz) {
                return contact;
            }
        }
        return null;
    }
}
