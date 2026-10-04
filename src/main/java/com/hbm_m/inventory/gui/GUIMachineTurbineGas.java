package com.hbm_m.inventory.gui;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.blockentity.machines.MachineTurbineGasBlockEntity;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.trait.FT_Combustible;
import com.hbm_m.inventory.fluid.trait.FT_Combustible.FuelGrade;
import com.hbm_m.inventory.menu.MachineTurbineGasMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 {@code GUIMachineTurbineGas}: Start/Stop-Rundknopf (rot/orange/gruen), AUTO-Taste, ziehbarer Leistungsschieber,
 * Drehzahl-Bogenanzeige, Thermometer, siebenstellige Leistungsanzeige (mit Hochlauf-Animation beim Start), Speicherbalken,
 * vier Tanks und drei Info-Felder (Automatik, zulaessige Kraftstoffe, Warnung bei Kraftstoff-/Schmiermittelmangel).
 */
public class GUIMachineTurbineGas extends GuiInfoScreen<MachineTurbineGasMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/generators/gui_turbinegas.png");

    private final MachineTurbineGasBlockEntity turbinegas;

    int yStart;
    int slidStart;

    public GUIMachineTurbineGas(MachineTurbineGasMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.turbinegas = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 223;
    }

    private void send(CompoundTag data) {
        com.hbm_m.network.NBTControlPacket.sendToServer(turbinegas.getBlockPos(), data);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        boolean result = super.mouseClicked(mx, my, button);
        if (turbinegas == null) return result;

        int x = (int) mx;
        int y = (int) my;

        slidStart = turbinegas.powerSliderPos;
        yStart = y;

        if (Math.sqrt(Math.pow((x - leftPos - 88), 2) + Math.pow((y - topPos - 40), 2)) <= 8) { // Start/Stop

            if (turbinegas.counter == 0 || turbinegas.counter == 579) {
                int state = turbinegas.state - 1; // aus(0) -> Anlauf(-1), Betrieb(1) -> aus(0)
                playClickSound();
                CompoundTag data = new CompoundTag();
                data.putInt("state", state);
                send(data);
            } else {
                return true;
            }
        }

        if (turbinegas.state == 1 && x > leftPos + 74 && x <= leftPos + 74 + 29 && y >= topPos + 86 && y < topPos + 86 + 13) { // AUTO
            playClickSound();
            CompoundTag data = new CompoundTag();
            data.putBoolean("autoMode", !turbinegas.autoMode);
            send(data);
        }

        if (turbinegas.state == 1 && (topPos + 97 - slidStart) <= yStart && (topPos + 103 - slidStart) > yStart && leftPos + 36 < x && leftPos + 52 >= x) { // Schieber
            CompoundTag data = new CompoundTag();
            data.putBoolean("autoMode", false); // Anfassen des Schiebers schaltet die Automatik ab
            send(data);
            playClickSound();
        }

        return result;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        boolean result = super.mouseDragged(mx, my, button, dx, dy);
        if (turbinegas == null) return result;

        int x = (int) mx;
        int y = (int) my;

        if (!turbinegas.autoMode && turbinegas.state == 1 && leftPos + 36 < x && leftPos + 52 >= x && topPos + 37 < y && topPos + 103 >= y) {
            if ((topPos + 97 - slidStart) <= yStart && (topPos + 103 - slidStart) > yStart) {
                int slidPos = topPos + 100 - y;
                if (slidPos > 60) slidPos = 60;
                else if (slidPos < 0) slidPos = 0;

                CompoundTag data = new CompoundTag();
                data.putDouble("slidPos", slidPos);
                send(data);
            }
        }
        return result;
    }

    private static Component[] lines(String key) {
        String[] split = Component.translatable(key).getString().split("\\$");
        Component[] out = new Component[split.length];
        for (int i = 0; i < split.length; i++) out[i] = Component.literal(split[i]);
        return out;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        if (turbinegas != null) {
            drawElectricityInfo(g, mouseX, mouseY, 26, 108, 142, 16, turbinegas.getEnergyStored(), MachineTurbineGasBlockEntity.maxPower);

            if (turbinegas.state == 1) {
                double consumption = MachineTurbineGasBlockEntity.fuelMaxCons.getOrDefault(turbinegas.tanks[0].getTankType(), 5D);
                drawCustomInfoStat(g, mouseX, mouseY, 36, 36, 16, 66, mouseX, mouseY,
                        Component.literal("Fuel consumption: " + 20 * (consumption * 0.05D + consumption * turbinegas.throttle / 100) + " mb/s"));
            } else {
                drawCustomInfoStat(g, mouseX, mouseY, 36, 36, 16, 66, mouseX, mouseY, Component.literal("Generator offline"));
            }

            if (turbinegas.temp >= 20)
                drawCustomInfoStat(g, mouseX, mouseY, 133, 23, 8, 72, mouseX, mouseY, Component.literal("Temperature: " + turbinegas.temp + "°C"));
            else
                drawCustomInfoStat(g, mouseX, mouseY, 133, 23, 8, 72, mouseX, mouseY, Component.literal("Temperature: 20°C"));

            turbinegas.tanks[0].renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 8, topPos + 16, 16, 48);
            turbinegas.tanks[1].renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 8, topPos + 70, 16, 32);
            turbinegas.tanks[2].renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 147, topPos + 61, 16, 36);
            turbinegas.tanks[3].renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 147, topPos + 21, 16, 36);

            drawCustomInfoStat(g, mouseX, mouseY, -16, 34, 16, 16, leftPos - 8, topPos + 44 + 16, lines("desc.gui.turbinegas.automode"));

            List<Component> fuels = new ArrayList<>();
            fuels.add(Component.translatable("desc.gui.turbinegas.fuels"));
            for (ModFluids.FluidEntry entry : HbmFluidRegistry.getOrderedFluids()) {
                Fluid fluid = entry.getSource();
                FT_Combustible trait = FluidType.getTrait(fluid, FT_Combustible.class);
                if (trait != null && trait.getGrade() == FuelGrade.GAS) {
                    fuels.add(Component.literal("  ").append(FluidType.forFluid(fluid).getLocalizedName()));
                }
            }
            drawCustomInfoStat(g, mouseX, mouseY, -16, 34 + 16, 16, 16, leftPos - 8, topPos + 44 + 16, fuels.toArray(new Component[0]));

            if (turbinegas.tanks[0].getFill() < 5000 || turbinegas.tanks[1].getFill() < 1000)
                drawCustomInfoStat(g, mouseX, mouseY, -16, 34 + 32, 16, 16, leftPos - 8, topPos + 44 + 16, lines("desc.gui.turbinegas.warning"));
        }

        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 94, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (turbinegas == null) return; // тайл может отсутствовать в реплее Flashback

        if (turbinegas.autoMode) g.blit(TEXTURE, leftPos + 74, topPos + 86, 194, 11, 29, 13);
        else g.blit(TEXTURE, leftPos + 74, topPos + 86, 194, 24, 29, 13);

        switch (turbinegas.state) {
            case 0 -> g.blit(TEXTURE, leftPos + 80, topPos + 32, 178, 38, 16, 16);
            case -1 -> {
                g.blit(TEXTURE, leftPos + 80, topPos + 32, 194, 38, 16, 16);
                displayStartup(g);
            }
            case 1 -> {
                g.blit(TEXTURE, leftPos + 80, topPos + 32, 210, 38, 16, 16);
                drawPowerMeterDisplay(g, 20 * turbinegas.instantPowerOutput);
            }
            default -> { }
        }

        g.blit(TEXTURE, leftPos + 36, topPos + 97 - turbinegas.powerSliderPos, 178, 0, 16, 6);

        int power = (int) (turbinegas.getEnergyStored() * 142 / MachineTurbineGasBlockEntity.maxPower);
        g.blit(TEXTURE, leftPos + 26, topPos + 109, 0, 223, power, 16);

        drawSmoothTextureModalCircle(g, leftPos + 64, topPos + 16, 176, 64, 48, 48, (double) turbinegas.rpm / 100);
        drawThermometer(g, turbinegas.temp);

        drawInfoPanel(g, -16, 34, PanelType.LARGE_GREEN_INFO);
        drawInfoPanel(g, -16, 34 + 16, PanelType.LARGE_BLUE_INFO);
        if (turbinegas.tanks[0].getFill() < 5000 || turbinegas.tanks[1].getFill() < 1000)
            drawInfoPanel(g, -16, 34 + 32, PanelType.LARGE_YELLOW_EXCLAMATION);
        if (turbinegas.tanks[0].getFill() == 0 || turbinegas.tanks[1].getFill() == 0)
            drawInfoPanel(g, -16, 34 + 32, PanelType.LARGE_RED_EXCLAMATION);

        turbinegas.tanks[0].renderTank(g, leftPos + 8, topPos + 17, 16, 48);
        turbinegas.tanks[1].renderTank(g, leftPos + 8, topPos + 71, 16, 32);
        turbinegas.tanks[2].renderTank(g, leftPos + 147, topPos + 62, 16, 36);
        turbinegas.tanks[3].renderTank(g, leftPos + 147, topPos + 22, 16, 36);
    }

    int numberToDisplay = 0;
    int digitNumber = 0;
    int exponent = 0;

    /** Original: beim Anlauf zaehlt die Anzeige auf 8888888 hoch und erlischt dann. */
    public void displayStartup(GuiGraphics g) {
        if (numberToDisplay < 8888888 && turbinegas.counter < 60) {
            digitNumber++;
            if (digitNumber == 9) {
                digitNumber = 1;
                exponent++;
            }
            numberToDisplay += (int) Math.pow(10, exponent);
        }

        if (turbinegas.counter > 50) numberToDisplay = 0;

        drawPowerMeterDisplay(g, numberToDisplay);
    }

    protected void drawPowerMeterDisplay(GuiGraphics g, int number) {
        int firstDigitX = 65;
        int firstDigitY = 62;

        int[] digit = new int[7];

        for (int i = 6; i >= 0; i--) {
            digit[i] = number % 10;
            number = number / 10;
            g.blit(TEXTURE, leftPos + firstDigitX + i * 7, topPos + 9 + firstDigitY, 194 + digit[i] * 5, 0, 5, 11);
        }

        int uselessZeros = 0;
        for (int i = 0; i < 6; i++) {
            if (digit[i] == 0) uselessZeros++;
            else break;
        }

        for (int i = 0; i < uselessZeros; i++) {
            g.blit(TEXTURE, leftPos + firstDigitX + i * 7, topPos + 9 + firstDigitY, 244, 0, 5, 11);
        }
    }

    protected void drawThermometer(GuiGraphics g, int temp) {
        int h = 64 * temp / 800;
        if (h <= 0) return;
        RenderSystem.enableBlend();
        g.blit(TEXTURE, leftPos + 136, topPos + 28 + 64 - h, 176, 64 - h, 2, h);
        RenderSystem.disableBlend();
    }

    /** 1:1 {@code GUIElements.drawSmoothTextureModalCircle}: Bogen ueber 270 Grad, aus Dreiecken zusammengesetzt. */
    private static void drawSmoothTextureModalCircle(GuiGraphics g, int xDraw, int yDraw, int xStart, int yStart, int xDelta, int yDelta, double progress) {
        float var7 = 0.00390625F;
        float var8 = 0.00390625F;

        progress = Math.max(0, Math.min(1, progress));
        float angle = (float) (-progress * 270.0);
        double theta = Math.toRadians(angle - 135);
        int addons = 0;
        double xTarget = 0;
        double yTarget = 0;

        if (angle >= -180 && angle < -90) addons = 1;
        else if (angle >= -270 && angle < -180) addons = 2;

        if (angle >= -90) {
            xTarget = -1;
            yTarget = -Math.tan(theta);
        } else if (angle > -135 && angle < -90) {
            xTarget = Math.tan(Math.PI / 2 - theta);
            yTarget = 1;
        } else if (angle > -180 && angle < -135) {
            xTarget = Math.tan(Math.PI / 2 - theta);
            yTarget = 1;
        } else if (angle <= -180) {
            xTarget = 1;
            yTarget = Math.tan(theta);
        } else if (angle == -135) {
            xTarget = 0;
            yTarget = 1;
        }

        double xMid = (double) xDelta / 2;
        double yMid = (double) yDelta / 2;

        xTarget *= xMid;
        yTarget *= yMid;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        Matrix4f m = g.pose().last().pose();
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);

        // Dreieck 1: links unten -> Mitte -> (links oben | Ziel)
        vertex(buf, m, xDraw, yDraw + yDelta, xStart * var7, (yStart + yDelta) * var8);
        vertex(buf, m, xDraw + xMid, yDraw + yMid, (float) (xStart + xMid) * var7, (float) (yStart + yMid) * var8);
        if (addons == 2 || addons == 1) {
            vertex(buf, m, xDraw, yDraw, xStart * var7, yStart * var8);
            vertex(buf, m, xDraw, yDraw, xStart * var7, yStart * var8);
            vertex(buf, m, xDraw + xMid, yDraw + yMid, (float) (xStart + xMid) * var7, (float) (yStart + yMid) * var8);
        }
        if (addons == 2) {
            vertex(buf, m, xDraw + xDelta, yDraw, (xStart + xDelta) * var7, yStart * var8);
            vertex(buf, m, xDraw + xDelta, yDraw, (xStart + xDelta) * var7, yStart * var8);
            vertex(buf, m, xDraw + xMid, yDraw + yMid, (float) (xStart + xMid) * var7, (float) (yStart + yMid) * var8);
        }
        vertex(buf, m, xDraw + xTarget + xMid, yDraw - yTarget + yMid, (float) (xStart + xTarget + xMid) * var7, (float) (yStart - yTarget + yMid) * var8);

        //? if < 1.21.1 {
        Tesselator.getInstance().end();
        //?} else {
        /*com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(buf.buildOrThrow());
        *///?}
    }

    private static void vertex(BufferBuilder buf, Matrix4f m, double x, double y, float u, float v) {
        buf.vertex(m, (float) x, (float) y, 0F).uv(u, v).endVertex();
    }
}
