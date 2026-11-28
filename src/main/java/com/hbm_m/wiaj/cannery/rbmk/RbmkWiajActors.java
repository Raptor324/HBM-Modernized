package com.hbm_m.wiaj.cannery.rbmk;

import java.util.function.BiConsumer;

import com.hbm_m.platform.RenderHooks;
import com.hbm_m.util.BobMathUtil;
import com.hbm_m.wiaj.VirtualWorld;
import com.hbm_m.wiaj.WiajScene;
import com.hbm_m.wiaj.actor.ActorBase;
import com.hbm_m.wiaj.actor.ITileActorRenderer;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Specialized WIAJ actors for RBMK reactor demos:
 * 1) ActorRbmkMachine    — Nucleus MachineBer / baked model renderer for columns, turbines, cooling tower, console
 * 2) ActorNeutronFlux    — animated 3D neutron/fluid pulses + optional 2D telemetry HUD
 * 3) ActorWorldHighlight — pulsing 3D translucent highlight box (optional X-ray)
 * 4) ActorRbmkGuiPreview — schematic 2D GUI overlays (manual/auto control, 15x15 console, flux curves, fuel lifecycle)
 * 5) ActorMeltdownEffect — expanding 3D overheat/meltdown flash
 */
public final class RbmkWiajActors {

	public static final int GUI_POS_OFFSET = 30_000_000;

	private RbmkWiajActors() { }

	private static final class RbmkOverlayTypes extends RenderStateShard {
		private RbmkOverlayTypes() { super("rbmk_overlay", () -> {}, () -> {}); }

		static final RenderType XRAY_QUADS = RenderType.create(
				"rbmk_wiaj_xray_quads",
				DefaultVertexFormat.POSITION_COLOR,
				VertexFormat.Mode.QUADS,
				256, false, true,
				RenderType.CompositeState.builder()
						.setShaderState(POSITION_COLOR_SHADER)
						.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
						.setDepthTestState(NO_DEPTH_TEST)
						.setWriteMaskState(COLOR_WRITE)
						.setCullState(NO_CULL)
						.createCompositeState(false));

		static final RenderType DEPTH_QUADS = RenderType.create(
				"rbmk_wiaj_depth_quads",
				DefaultVertexFormat.POSITION_COLOR,
				VertexFormat.Mode.QUADS,
				256, false, true,
				RenderType.CompositeState.builder()
						.setShaderState(POSITION_COLOR_SHADER)
						.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
						.setWriteMaskState(COLOR_WRITE)
						.setCullState(NO_CULL)
						.createCompositeState(false));
	}

	// =========================================================================
	// 1) ActorRbmkMachine
	// =========================================================================
	public static class ActorRbmkMachine<T extends BlockEntity> implements ITileActorRenderer {

		@FunctionalInterface
		public interface BiFunctionBE<T extends BlockEntity> {
			T create(BlockPos pos, Direction facing);
		}

		private final BiFunctionBE<T> factory;
		private final BiConsumer<T, CompoundTag> stateSync;
		private T fakeBe;
		private int lastDirOrdinal = -1;

		public ActorRbmkMachine(BiFunctionBE<T> factory) {
			this(factory, null);
		}

		public ActorRbmkMachine(BiFunctionBE<T> factory, BiConsumer<T, CompoundTag> stateSync) {
			this.factory = factory;
			this.stateSync = stateSync;
		}

		public static CompoundTag posTag(double x, double y, double z, Direction facing) {
			CompoundTag tag = new CompoundTag();
			tag.putDouble("x", x);
			tag.putDouble("y", y);
			tag.putDouble("z", z);
			tag.putInt("facing", facing.get3DDataValue());
			return tag;
		}

		@Override
		public void renderActor(GuiGraphics gui, VirtualWorld world, int ticks, float interp, CompoundTag data) {
			double x = data.getDouble("x");
			double y = data.getDouble("y");
			double z = data.getDouble("z");
			int dirVal = data.contains("facing") ? data.getInt("facing") : Direction.NORTH.get3DDataValue();

			if (this.fakeBe == null || this.lastDirOrdinal != dirVal) {
				this.lastDirOrdinal = dirVal;
				Direction facing = Direction.from3DDataValue(dirVal);
				this.fakeBe = factory.create(new BlockPos((int) x + GUI_POS_OFFSET, (int) y, (int) z), facing);
			}
			if (this.stateSync != null) {
				this.stateSync.accept(this.fakeBe, data);
			}

			gui.pose().pushPose();
			gui.pose().translate(x, y, z);

			// Compensate negative GuiWorldInAJar scale (-10, -10, -10) for OBJ lighting
			if (gui.pose().last().normal().determinant() < 0.0F) {
				float invF = Mth.fastInvCubeRoot(2.0F);
				gui.pose().last().normal().scaleLocal(-invF, -invF, -0.5F * invF);
			}

			BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
			BlockEntityRenderer<BlockEntity> ber = dispatcher.getRenderer(this.fakeBe);
			if (ber != null) {
				ber.render(this.fakeBe, interp, gui.pose(), gui.bufferSource(),
						LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
			} else {
				BlockState st = this.fakeBe.getBlockState();
				var br = Minecraft.getInstance().getBlockRenderer();
				var model = br.getBlockModel(st);
				var vc = gui.bufferSource().getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
				br.getModelRenderer().renderModel(gui.pose().last(), vc, st, model,
						1.0F, 1.0F, 1.0F, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
			}

			gui.pose().popPose();
		}

		@Override
		public void updateActor(int ticks, CompoundTag data) { }
	}

	// =========================================================================
	// 2) ActorNeutronFlux
	// =========================================================================
	public static class ActorNeutronFlux extends ActorBase {

		private final double[][] points;
		private final int[] segColors;
		private final double speed;
		private final boolean loop;

		public ActorNeutronFlux(double speed, boolean loop, double[][] points, int[] segColors) {
			this.speed = speed;
			this.loop = loop;
			this.points = points;
			this.segColors = segColors;
			this.data.putBoolean("visible", true);
			this.data.putBoolean("showHud", false);
			this.data.putInt("flux", 0);
			this.data.putInt("tempC", 20);
			this.data.putString("statusNote", "");
		}

		public ActorNeutronFlux withHud() {
			this.data.putBoolean("showHud", true);
			return this;
		}

		@Override
		public void updateActor(WiajScene scene) {
			if (points == null || points.length < 2) return;
			if (!data.contains("init")) {
				data.putBoolean("init", true);
				data.putInt("seg", 0);
				data.putDouble("prog", 0.0);
				snapPos(points[0][0], points[0][1], points[0][2], true);
			}
			if (data.getBoolean("paused")) {
				snapPos(data.getDouble("curX"), data.getDouble("curY"), data.getDouble("curZ"), true);
				return;
			}

			data.putDouble("prevX", data.getDouble("curX"));
			data.putDouble("prevY", data.getDouble("curY"));
			data.putDouble("prevZ", data.getDouble("curZ"));

			int seg = data.getInt("seg");
			double prog = data.getDouble("prog") + speed;

			while (seg < points.length - 1) {
				double len = segLen(seg);
				if (len > 1e-5 && prog >= len) {
					prog -= len;
					seg++;
				} else {
					break;
				}
			}

			if (seg >= points.length - 1) {
				if (loop) {
					seg = 0;
					prog = 0.0;
					snapPos(points[0][0], points[0][1], points[0][2], true);
				} else {
					data.putInt("seg", points.length - 1);
					data.putDouble("prog", 0.0);
					snapPos(points[points.length - 1][0], points[points.length - 1][1], points[points.length - 1][2], false);
					return;
				}
			}

			double len = segLen(seg);
			double t = len > 1e-5 ? prog / len : 0.0;
			double[] p0 = points[seg];
			double[] p1 = points[seg + 1];
			data.putInt("seg", seg);
			data.putDouble("prog", prog);
			snapPos(p0[0] + (p1[0] - p0[0]) * t, p0[1] + (p1[1] - p0[1]) * t, p0[2] + (p1[2] - p0[2]) * t, false);
		}

		private double segLen(int seg) {
			double dx = points[seg + 1][0] - points[seg][0];
			double dy = points[seg + 1][1] - points[seg][1];
			double dz = points[seg + 1][2] - points[seg][2];
			return Math.sqrt(dx * dx + dy * dy + dz * dz);
		}

		private void snapPos(double x, double y, double z, boolean snapPrev) {
			data.putDouble("curX", x);
			data.putDouble("curY", y);
			data.putDouble("curZ", z);
			if (snapPrev) {
				data.putDouble("prevX", x);
				data.putDouble("prevY", y);
				data.putDouble("prevZ", z);
			}
		}

		@Override
		public void drawBackgroundComponent(GuiGraphics gui, VirtualWorld world, int ticks, float interp) {
			if (!data.getBoolean("visible") || !data.contains("curX")) return;

			double rx = BobMathUtil.interp(data.getDouble("prevX"), data.getDouble("curX"), interp);
			double ry = BobMathUtil.interp(data.getDouble("prevY"), data.getDouble("curY"), interp);
			double rz = BobMathUtil.interp(data.getDouble("prevZ"), data.getDouble("curZ"), interp);

			int seg = Mth.clamp(data.getInt("seg"), 0, segColors.length - 1);
			int col = segColors[seg];
			float a = ((col >> 24) & 0xFF) / 255.0F;
			if (a <= 0.01F) return;
			float r = ((col >> 16) & 0xFF) / 255.0F;
			float g = ((col >> 8) & 0xFF) / 255.0F;
			float b = (col & 0xFF) / 255.0F;

			drawColoredBox(gui, rx, ry, rz, 0.22F, 0.22F, 0.22F, r, g, b, a, true);
			drawColoredBox(gui, rx, ry, rz, 0.34F, 0.34F, 0.34F, r, g, b, a * 0.30F, true);
		}

		@Override
		public void drawForegroundComponent(GuiGraphics gui, int w, int h, int ticks, float interp) {
			if (!data.getBoolean("visible") || !data.getBoolean("showHud")) return;

			Font font = Minecraft.getInstance().font;
			int flux = data.getInt("flux");
			int temp = data.getInt("tempC");
			String note = data.getString("statusNote");
			if (note.startsWith("cannery.")) note = RbmkWiajHelpers.tr(note);

			String tempCol = temp >= 1400 ? "§c" : (temp >= 900 ? "§e" : "§a");
			String line1 = "§b⚛ " + RbmkWiajHelpers.tr("cannery.rbmk.hud.flux") + ": §f" + flux
					+ "   " + tempCol + "🔥 " + temp + "°C";

			int boxW = Math.max(font.width(line1), note.isEmpty() ? 0 : font.width(note)) + 14;
			int boxH = note.isEmpty() ? 18 : 30;
			int bx = w / 2 - boxW / 2;
			int by = 12;

			int border = temp >= 1400 ? 0xFFE53935 : (temp >= 900 ? 0xFFFAD64A : 0xFF39ACFF);
			gui.fill(bx - 1, by - 1, bx + boxW + 1, by + boxH + 1, border);
			gui.fill(bx, by, bx + boxW, by + boxH, 0xE6181C20);
			gui.drawString(font, line1, bx + 7, by + 5, 0xFFFFFFFF, false);
			if (!note.isEmpty()) {
				gui.drawString(font, note, bx + 7, by + 17, 0xFFE0E0E0, false);
			}
		}
	}

	// =========================================================================
	// 3) ActorWorldHighlight
	// =========================================================================
	public static class ActorWorldHighlight extends ActorBase {

		public ActorWorldHighlight(double minX, double minY, double minZ,
				double maxX, double maxY, double maxZ, int argb) {
			data.putDouble("minX", minX); data.putDouble("minY", minY); data.putDouble("minZ", minZ);
			data.putDouble("maxX", maxX); data.putDouble("maxY", maxY); data.putDouble("maxZ", maxZ);
			data.putInt("color", argb);
			data.putBoolean("xray", false);
		}

		public ActorWorldHighlight xray() {
			data.putBoolean("xray", true);
			return this;
		}

		@Override public void drawForegroundComponent(GuiGraphics gui, int w, int h, int ticks, float interp) { }
		@Override public void updateActor(WiajScene scene) { }

		@Override
		public void drawBackgroundComponent(GuiGraphics gui, VirtualWorld world, int ticks, float interp) {
			int col = data.getInt("color");
			float a = (((col >> 24) & 0xFF) / 255.0F) * (0.65F + 0.35F * Mth.sin((ticks + interp) * 0.25F));
			float r = ((col >> 16) & 0xFF) / 255.0F;
			float g = ((col >> 8) & 0xFF) / 255.0F;
			float b = (col & 0xFF) / 255.0F;

			double cx = (data.getDouble("minX") + data.getDouble("maxX")) * 0.5;
			double cy = (data.getDouble("minY") + data.getDouble("maxY")) * 0.5;
			double cz = (data.getDouble("minZ") + data.getDouble("maxZ")) * 0.5;
			float sx = (float) (data.getDouble("maxX") - data.getDouble("minX")) * 0.5F + 0.02F;
			float sy = (float) (data.getDouble("maxY") - data.getDouble("minY")) * 0.5F + 0.02F;
			float sz = (float) (data.getDouble("maxZ") - data.getDouble("minZ")) * 0.5F + 0.02F;

			drawColoredBox(gui, cx, cy, cz, sx, sy, sz, r, g, b, a, data.getBoolean("xray"));
		}
	}

	// =========================================================================
	// 4) ActorRbmkGuiPreview
	// =========================================================================
	public static class ActorRbmkGuiPreview extends ActorBase {

		public enum Mode {
			CONTROL_ROD,
			AUTO_CONTROL,
			CONSOLE_15X15,
			FLUX_FUNCTIONS,
			FUEL_LIFECYCLE
		}

		private final Mode mode;
		private final int offsetX;
		private final int offsetY;

		public ActorRbmkGuiPreview(Mode mode, int offsetX, int offsetY) {
			this.mode = mode;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
			data.putInt("rodLevel", 0);
			data.putBoolean("az5", false);
			data.putString("highlight", "");
		}

		@Override public void drawBackgroundComponent(GuiGraphics gui, VirtualWorld world, int ticks, float interp) { }
		@Override public void updateActor(WiajScene scene) { }

		@Override
		public void drawForegroundComponent(GuiGraphics gui, int w, int h, int ticks, float interp) {
			Font font = Minecraft.getInstance().font;
			int boxW = (mode == Mode.CONSOLE_15X15 || mode == Mode.FLUX_FUNCTIONS || mode == Mode.FUEL_LIFECYCLE) ? 164 : 134;
			int boxH = 92;
			int x = w / 2 + offsetX - boxW / 2;
			int y = h / 2 + offsetY - boxH / 2;
			boolean blink = ((ticks / 6) % 2) == 0;
			String hl = data.getString("highlight");

			gui.fill(x - 1, y - 1, x + boxW + 1, y + boxH + 1, 0xFF8A929A);
			gui.fill(x, y, x + boxW, y + boxH, 0xEB1E2227);

			switch (mode) {
				case CONTROL_ROD    -> drawControlRod(gui, font, x, y, hl, blink);
				case AUTO_CONTROL   -> drawAutoControl(gui, font, x, y, hl, blink);
				case CONSOLE_15X15  -> drawConsole(gui, font, x, y, hl, blink);
				case FLUX_FUNCTIONS -> drawFluxFunctions(gui, font, x, y);
				case FUEL_LIFECYCLE -> drawFuelLifecycle(gui, font, x, y);
			}
		}

		private void drawControlRod(GuiGraphics gui, Font font, int x, int y, String hl, boolean blink) {
			gui.drawString(font, RbmkWiajHelpers.tr("cannery.rbmk.gui.control"), x + 6, y + 5, 0xFFFFD64A, false);
			int[] cols = { 0xFFE53935, 0xFFFDD835, 0xFF43A047, 0xFF1E88E5, 0xFF8E24AA };
			for (int i = 0; i < cols.length; i++) {
				gui.fill(x + 8, y + 18 + i * 13, x + 22, y + 29 + i * 13, cols[i]);
			}
			int lvl = Mth.clamp(data.getInt("rodLevel"), 0, 100);
			int barTop = y + 18, barBot = y + 80;
			gui.fill(x + 36, barTop, x + 54, barBot, 0xFF333840);
			int fillH = (int) ((barBot - barTop) * (lvl / 100.0F));
			gui.fill(x + 38, barBot - fillH, x + 52, barBot, 0xFF4CD964);
			gui.drawString(font, lvl + "%", x + 36, y + 82, 0xFFFFFFFF, false);

			String[] presets = { "100%", "75%", "50%", "25%", "0%" };
			for (int i = 0; i < presets.length; i++) {
				int py = y + 18 + i * 13;
				gui.fill(x + 70, py, x + 120, py + 11, 0xFF3B4048);
				gui.drawString(font, presets[i], x + 82, py + 2, 0xFFE0E0E0, false);
			}
			if (blink && "level".equals(hl)) drawBorder(gui, x + 34, y + 16, 22, 66, 0xFFFFD600);
		}

		private void drawAutoControl(GuiGraphics gui, Font font, int x, int y, String hl, boolean blink) {
			gui.drawString(font, RbmkWiajHelpers.tr("cannery.rbmk.gui.auto"), x + 6, y + 5, 0xFF39ACFF, false);
			String[] rows = { "Max % (T min): 100", "Min % (T max): 0", "T max: 1100°C", "T min: 400°C" };
			for (int i = 0; i < rows.length; i++) {
				gui.drawString(font, rows[i], x + 8, y + 19 + i * 13, 0xFFB8FFB8, false);
			}
			gui.fill(x + 8, y + 73, x + 124, y + 86, 0xFF1F7A33);
			gui.drawString(font, RbmkWiajHelpers.tr("cannery.rbmk.gui.save"), x + 34, y + 76, 0xFFFFFFFF, false);
			if (blink && "save".equals(hl)) drawBorder(gui, x + 7, y + 72, 118, 15, 0xFFFFD600);
		}

		private void drawConsole(GuiGraphics gui, Font font, int x, int y, String hl, boolean blink) {
			gui.drawString(font, RbmkWiajHelpers.tr("cannery.rbmk.gui.console"), x + 6, y + 5, 0xFFFFD64A, false);
			int gx = x + 84, gy = y + 17;
			for (int r = 0; r < 7; r++) {
				for (int c = 0; c < 7; c++) {
					int cellCol = 0xFF2B2F36;
					boolean fuel = (r == 1 || r == 5) && (c == 1 || c == 5);
					boolean ctrl = (r == 1 || r == 5) && c == 2 || (c == 1 || c == 5) && r == 2;
					boolean boiler = (r == 1 || r == 5) && c == 4 || (c == 1 || c == 5) && r == 4;
					if (fuel) cellCol = 0xFF4CD964;
					else if (ctrl) cellCol = 0xFF39ACFF;
					else if (boiler) cellCol = 0xFFE57373;
					gui.fill(gx + c * 10, gy + r * 10, gx + c * 10 + 9, gy + r * 10 + 9, cellCol);
				}
			}
			int lvl = data.getInt("rodLevel");
			gui.drawString(font, "LVL: " + lvl + "%", x + 8, y + 22, 0xFF4CD964, false);
			gui.drawString(font, "15x15 MAX", x + 8, y + 36, 0xFFAAAAAA, false);
			boolean az5 = data.getBoolean("az5");
			gui.fill(x + 8, y + 58, x + 68, y + 82, az5 ? 0xFFFF1744 : 0xFFB71C1C);
			gui.drawString(font, "AZ-5", x + 25, y + 66, 0xFFFFFFFF, false);
			if (blink && "az5".equals(hl)) drawBorder(gui, x + 6, y + 56, 64, 28, 0xFFFFD600);
		}

		private void drawFluxFunctions(GuiGraphics gui, Font font, int x, int y) {
			gui.drawString(font, RbmkWiajHelpers.tr("cannery.rbmk.gui.funcs"), x + 6, y + 5, 0xFFFFD64A, false);
			gui.drawString(font, "§a● Sigmoid / Euler (Th): Safe", x + 6, y + 20, 0xFFFFFFFF, false);
			gui.drawString(font, "§e● Log / Sqrt (MOX, MEP): Med", x + 6, y + 36, 0xFFFFFFFF, false);
			gui.drawString(font, "§c● Linear / -Quad (HES): High", x + 6, y + 52, 0xFFFFFFFF, false);
			gui.drawString(font, "§d● Quad (Digamma): NO REFL!", x + 6, y + 68, 0xFFFFFFFF, false);
		}

		private void drawFuelLifecycle(GuiGraphics gui, Font font, int x, int y) {
			gui.drawString(font, RbmkWiajHelpers.tr("cannery.rbmk.gui.cycle"), x + 6, y + 5, 0xFF26C6DA, false);
			gui.drawString(font, "1. Depleted Rod (>25%)", x + 6, y + 20, 0xFFE0E0E0, false);
			gui.drawString(font, "2. Cool in Pool Drum (Water)", x + 6, y + 36, 0xFF80DEEA, false);
			gui.drawString(font, "3. Craft -> Fuel Pellets", x + 6, y + 52, 0xFFFFD64A, false);
			gui.drawString(font, "4. SILEX (IR) -> Pu / Waste", x + 6, y + 68, 0xFF4CD964, false);
		}

		private static void drawBorder(GuiGraphics gui, int x, int y, int w, int h, int col) {
			gui.fill(x, y, x + w, y + 1, col);
			gui.fill(x, y + h - 1, x + w, y + h, col);
			gui.fill(x, y, x + 1, y + h, col);
			gui.fill(x + w - 1, y, x + w, y + h, col);
		}
	}

	// =========================================================================
	// 5) ActorMeltdownEffect
	// =========================================================================
	public static class ActorMeltdownEffect extends ActorBase {

		public ActorMeltdownEffect(double cx, double cy, double cz) {
			data.putDouble("cx", cx);
			data.putDouble("cy", cy);
			data.putDouble("cz", cz);
			data.putInt("age", 0);
			data.putBoolean("active", false);
		}

		@Override
		public void updateActor(WiajScene scene) {
			if (data.getBoolean("active")) {
				data.putInt("age", data.getInt("age") + 1);
			}
		}

		@Override public void drawForegroundComponent(GuiGraphics gui, int w, int h, int ticks, float interp) { }

		@Override
		public void drawBackgroundComponent(GuiGraphics gui, VirtualWorld world, int ticks, float interp) {
			if (!data.getBoolean("active")) return;
			float t = data.getInt("age") + interp;
			float a = Mth.clamp(1.0F - t * 0.035F, 0.0F, 0.9F);
			if (a <= 0.01F) return;

			double cx = data.getDouble("cx");
			double cy = data.getDouble("cy");
			double cz = data.getDouble("cz");
			float rad = 0.6F + t * 0.18F;
			drawColoredBox(gui, cx, cy, cz, rad, rad * 1.2F, rad, 1.0F, 0.35F, 0.05F, a, true);
		}
	}

	// =========================================================================
	// Shared 3D colored box helper
	// =========================================================================
	private static void drawColoredBox(GuiGraphics gui, double cx, double cy, double cz,
			float sx, float sy, float sz, float r, float g, float b, float a, boolean xray) {
		PoseStack pose = gui.pose();
		pose.pushPose();
		pose.translate(cx, cy, cz);
		var mat = pose.last().pose();
		VertexConsumer vc = gui.bufferSource().getBuffer(
				xray ? RbmkOverlayTypes.XRAY_QUADS : RbmkOverlayTypes.DEPTH_QUADS);

		quad(vc, mat, -sx,  sy, -sz, -sx,  sy,  sz,  sx,  sy,  sz,  sx,  sy, -sz, r, g, b, a);
		quad(vc, mat, -sx, -sy, -sz,  sx, -sy, -sz,  sx, -sy,  sz, -sx, -sy,  sz, r, g, b, a);
		quad(vc, mat, -sx, -sy, -sz, -sx,  sy, -sz,  sx,  sy, -sz,  sx, -sy, -sz, r, g, b, a);
		quad(vc, mat, -sx, -sy,  sz,  sx, -sy,  sz,  sx,  sy,  sz, -sx,  sy,  sz, r, g, b, a);
		quad(vc, mat, -sx, -sy, -sz, -sx, -sy,  sz, -sx,  sy,  sz, -sx,  sy, -sz, r, g, b, a);
		quad(vc, mat,  sx, -sy, -sz,  sx,  sy, -sz,  sx,  sy,  sz,  sx, -sy,  sz, r, g, b, a);
		pose.popPose();
	}

	private static void quad(VertexConsumer vc, org.joml.Matrix4f m,
			float x1, float y1, float z1, float x2, float y2, float z2,
			float x3, float y3, float z3, float x4, float y4, float z4,
			float r, float g, float b, float a) {
		RenderHooks.vertexColor(vc, m, x1, y1, z1, r, g, b, a);
		RenderHooks.vertexColor(vc, m, x2, y2, z2, r, g, b, a);
		RenderHooks.vertexColor(vc, m, x3, y3, z3, r, g, b, a);
		RenderHooks.vertexColor(vc, m, x4, y4, z4, r, g, b, a);
	}
}
