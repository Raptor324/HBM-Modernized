package com.hbm_m.wiaj.cannery.pa;

import java.util.function.BiConsumer;

import com.hbm_m.item.machine.ItemPACoil;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.RenderHooks;
import com.hbm_m.util.BobMathUtil;
import com.hbm_m.wiaj.VirtualWorld;
import com.hbm_m.wiaj.WiajScene;
import com.hbm_m.wiaj.actor.ActorBase;
import com.hbm_m.wiaj.actor.ITileActorRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;

import net.minecraft.ChatFormatting;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Specialized WIAJ actors for Particle Accelerator demos:
 * 1) ActorNucleusMachine — renders PA blocks via Nucleus MachineBer or NO_CULL baked model
 * 2) ActorParticleBeam   — animated particle beam (X-ray) with 2D telemetry HUD (M / D)
 * 3) ActorWorldHighlight — 3D colored translucent highlight box (optional X-ray)
 * 4) ActorGuiPreview     — 2D GUI preview using real PA GUI textures
 */
public final class PaWiajActors {

	public static final int GUI_POS_OFFSET = 30_000_000;

	private PaWiajActors() { }

	private static final class PaOverlayRenderTypes extends RenderStateShard {
		private PaOverlayRenderTypes() { super("pa_overlay", () -> {}, () -> {}); }

		static final RenderType XRAY_QUADS = RenderType.create(
				"pa_wiaj_xray_quads",
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
				"pa_wiaj_depth_quads",
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

	public static class ActorNucleusMachine<T extends BlockEntity> implements ITileActorRenderer {

		private final BiFunctionBE<T> factory;
		private final BiConsumer<T, CompoundTag> stateSync;
		private T fakeBe;
		private int lastDirOrdinal = -1;

		@FunctionalInterface
		public interface BiFunctionBE<T extends BlockEntity> {
			T create(BlockPos pos, Direction facing);
		}

		public ActorNucleusMachine(BiFunctionBE<T> factory) {
			this(factory, null);
		}

		public ActorNucleusMachine(BiFunctionBE<T> factory, BiConsumer<T, CompoundTag> stateSync) {
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

		public static CompoundTag posTagForBeam(double x, double y, double z, Direction beamDir) {
			return posTag(x, y, z, PaWiajHelpers.facingForBeam(beamDir));
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

			// Undo GuiWorldInAJar's negative scale (-10,-10,-10) and Z-flattening (1,1,0.5) on the normal matrix
			// so view-space directional lighting on OBJ models does not invert/glitch when the camera rotates.
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
				// GuiWorldInAJar flips winding via scale(-10,-10,-10), so static models MUST use NO_CULL
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

	public static class ActorParticleBeam extends ActorBase {

		private final double[][] waypoints;
		private final double defaultSpeed;
		private final boolean loop;

		public ActorParticleBeam(double speed, boolean loop, double[]... waypoints) {
			this.defaultSpeed = speed;
			this.loop = loop;
			this.waypoints = waypoints;
			this.data.putBoolean("visible", true);
			this.data.putBoolean("showHud", true);
			this.data.putInt("momentum", 0);
			this.data.putInt("defocus", 0);
			this.data.putBoolean("crashed", false);
			this.data.putBoolean("penalty", false);
			this.data.putString("statusNote", "");
		}

		@Override
		public void updateActor(WiajScene scene) {
			if (waypoints == null || waypoints.length < 2) return;

			if (!data.contains("init")) {
				data.putBoolean("init", true);
				data.putInt("seg", 0);
				data.putDouble("prog", 0.0);
				setPos(waypoints[0][0], waypoints[0][1], waypoints[0][2], true);
			}

			if (data.getBoolean("paused") || data.getBoolean("crashed")) {
				data.putDouble("prevX", data.getDouble("curX"));
				data.putDouble("prevY", data.getDouble("curY"));
				data.putDouble("prevZ", data.getDouble("curZ"));
				if (data.getBoolean("crashed")) {
					data.putInt("crashTick", data.getInt("crashTick") + 1);
				}
				return;
			}

			data.putDouble("prevX", data.getDouble("curX"));
			data.putDouble("prevY", data.getDouble("curY"));
			data.putDouble("prevZ", data.getDouble("curZ"));

			int seg = data.getInt("seg");
			double prog = data.getDouble("prog");
			double speed = data.contains("speed") ? data.getDouble("speed") : defaultSpeed;

			if (seg >= waypoints.length - 1) {
				if (loop) {
					seg = 0;
					prog = 0.0;
					setPos(waypoints[0][0], waypoints[0][1], waypoints[0][2], true);
				} else {
					return;
				}
			}

			double[] p0 = waypoints[seg];
			double[] p1 = waypoints[seg + 1];
			double dx = p1[0] - p0[0], dy = p1[1] - p0[1], dz = p1[2] - p0[2];
			double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
			prog += speed;

			while (len > 1e-5 && prog >= len && seg < waypoints.length - 1) {
				prog -= len;
				seg++;
				if (seg < waypoints.length - 1) {
					p0 = waypoints[seg];
					p1 = waypoints[seg + 1];
					dx = p1[0] - p0[0]; dy = p1[1] - p0[1]; dz = p1[2] - p0[2];
					len = Math.sqrt(dx * dx + dy * dy + dz * dz);
				}
			}

			if (seg >= waypoints.length - 1) {
				if (loop) {
					seg = 0; prog = 0.0;
					p0 = waypoints[0]; p1 = waypoints[1];
					dx = p1[0] - p0[0]; dy = p1[1] - p0[1]; dz = p1[2] - p0[2];
					len = Math.sqrt(dx * dx + dy * dy + dz * dz);
				} else {
					data.putInt("seg", waypoints.length - 1);
					data.putDouble("prog", 0.0);
					setPos(waypoints[waypoints.length - 1][0], waypoints[waypoints.length - 1][1], waypoints[waypoints.length - 1][2], false);
					return;
				}
			}

			double t = len > 1e-5 ? prog / len : 0.0;
			data.putInt("seg", seg);
			data.putDouble("prog", prog);
			setPos(p0[0] + dx * t, p0[1] + dy * t, p0[2] + dz * t, false);
		}

		private void setPos(double x, double y, double z, boolean snapPrev) {
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

			boolean crashed = data.getBoolean("crashed");
			boolean penalty = data.getBoolean("penalty");
			int defocus = data.getInt("defocus");

			if (!crashed && defocus > 0) {
				double amp = Math.min(0.18, defocus / 1000.0 * 0.25);
				ry += Math.sin((ticks + interp) * 1.9) * amp;
			}

			float r = 0.25F, g = 0.88F, b = 1.0F, a = 0.95F;
			float radius = 0.28F;
			if (crashed) {
				r = 1.0F; g = 0.15F; b = 0.15F;
				float ct = data.getInt("crashTick") + interp;
				radius = 0.35F + Math.min(1.1F, ct * 0.12F);
				a = Mth.clamp(1.0F - ct * 0.06F, 0.0F, 0.95F);
			} else if (penalty || defocus >= 300) {
				r = 1.0F; g = 0.72F; b = 0.15F;
				radius = 0.32F;
			} else if (defocus > 0) {
				r = 0.95F; g = 0.92F; b = 0.35F;
			}

			if (a <= 0.01F) return;

			drawColoredCube(gui, rx, ry, rz, radius, r, g, b, a, true);
			if (!crashed) {
				drawColoredCube(gui, rx, ry, rz, radius * 1.45F, r, g, b, 0.30F, true);
			}
		}

		@Override
		public void drawForegroundComponent(GuiGraphics gui, int w, int h, int ticks, float interp) {
			if (!data.getBoolean("visible") || !data.getBoolean("showHud")) return;

			Font font = Minecraft.getInstance().font;
			int m = data.getInt("momentum");
			int d = data.getInt("defocus");
			boolean crashed = data.getBoolean("crashed");
			String note = data.getString("statusNote");

			String mLabel = PaWiajHelpers.tr("cannery.pa.hud.momentum");
			String dLabel = PaWiajHelpers.tr("cannery.pa.hud.defocus");
			String cLabel = PaWiajHelpers.tr("cannery.pa.hud.crash");
			String line1 = crashed
					? "§c✖ " + cLabel + "!  §7" + mLabel + ": " + m + " | " + dLabel + ": " + d
					: "§b⚡ " + mLabel + ": §f" + m + "   " + (d > 0 ? "§e⚠ " + dLabel + ": §c" + d : "§a✔ " + dLabel + ": 0");
			if (note.startsWith("cannery.")) note = PaWiajHelpers.tr(note);

			int boxW = Math.max(font.width(line1), note.isEmpty() ? 0 : font.width(note)) + 12;
			int boxH = note.isEmpty() ? 18 : 30;
			int bx = w / 2 - boxW / 2;
			int by = 14;

			int border = crashed ? 0xFFE53935 : (d > 0 ? 0xFFFAD64A : 0xFF39ACFF);
			gui.fill(bx - 1, by - 1, bx + boxW + 1, by + boxH + 1, border);
			gui.fill(bx, by, bx + boxW, by + boxH, 0xE6181C20);
			gui.drawString(font, line1, bx + 6, by + 5, 0xFFFFFFFF, false);
			if (!note.isEmpty()) {
				gui.drawString(font, note, bx + 6, by + 17, 0xFFE0E0E0, false);
			}
		}
	}

	public static class ActorWorldHighlight extends ActorBase {

		public ActorWorldHighlight(double minX, double minY, double minZ,
				double maxX, double maxY, double maxZ, int argb) {
			data.putDouble("minX", minX);
			data.putDouble("minY", minY);
			data.putDouble("minZ", minZ);
			data.putDouble("maxX", maxX);
			data.putDouble("maxY", maxY);
			data.putDouble("maxZ", maxZ);
			data.putInt("color", argb);
			data.putBoolean("pulse", true);
			data.putBoolean("xray", false);
		}

		public ActorWorldHighlight xray() {
			data.putBoolean("xray", true);
			return this;
		}

		@Override
		public void drawForegroundComponent(GuiGraphics gui, int w, int h, int ticks, float interp) { }

		@Override
		public void drawBackgroundComponent(GuiGraphics gui, VirtualWorld world, int ticks, float interp) {
			int col = data.getInt("color");
			float a = ((col >> 24) & 0xFF) / 255.0F;
			float r = ((col >> 16) & 0xFF) / 255.0F;
			float g = ((col >> 8) & 0xFF) / 255.0F;
			float b = (col & 0xFF) / 255.0F;

			if (data.getBoolean("pulse")) {
				a *= 0.65F + 0.35F * Mth.sin((ticks + interp) * 0.25F);
			}

			double cx = (data.getDouble("minX") + data.getDouble("maxX")) * 0.5;
			double cy = (data.getDouble("minY") + data.getDouble("maxY")) * 0.5;
			double cz = (data.getDouble("minZ") + data.getDouble("maxZ")) * 0.5;
			float sx = (float) (data.getDouble("maxX") - data.getDouble("minX")) * 0.5F + 0.02F;
			float sy = (float) (data.getDouble("maxY") - data.getDouble("minY")) * 0.5F + 0.02F;
			float sz = (float) (data.getDouble("maxZ") - data.getDouble("minZ")) * 0.5F + 0.02F;

			drawColoredBox(gui, cx, cy, cz, sx, sy, sz, r, g, b, a, data.getBoolean("xray"));
		}

		@Override
		public void updateActor(WiajScene scene) { }
	}

	public static class ActorGuiPreview extends ActorBase {

		public enum Mode { SOURCE, DETECTOR, DIPOLE, QUADRUPOLE }

		private static final ResourceLocation TEX_SOURCE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/particleaccelerator/gui_source.png");
		private static final ResourceLocation TEX_DETECTOR = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/particleaccelerator/gui_detector.png");
		private static final ResourceLocation TEX_DIPOLE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/particleaccelerator/gui_dipole.png");
		private static final ResourceLocation TEX_QUADRUPOLE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/particleaccelerator/gui_quadrupole.png");

		private final Mode mode;
		private final int offsetX;
		private final int offsetY;

		private ItemStack slotTopLeft = ItemStack.EMPTY;
		private ItemStack slotTopRight = ItemStack.EMPTY;
		private ItemStack slotBotLeft = ItemStack.EMPTY;
		private ItemStack slotBotRight = ItemStack.EMPTY;
		private ItemStack coilStack = ItemStack.EMPTY;

		public ActorGuiPreview(Mode mode, int offsetXFromCenter, int offsetYFromCenter) {
			this.mode = mode;
			this.offsetX = offsetXFromCenter;
			this.offsetY = offsetYFromCenter;
			data.putInt("tempK", 293);
			data.putBoolean("powered", false);
			data.putBoolean("hasCoil", false);
			data.putString("status", "Idle");
			data.putInt("threshold", 0);
			data.putString("dirLow", "N");
			data.putString("dirHigh", "N");
			data.putString("dirRedstone", "N");
			data.putString("playerDir", "N");
			data.putString("highlight", "");
			data.putString("tooltip", "");
		}

		public ActorGuiPreview withSlots(ItemStack tl, ItemStack tr, ItemStack bl, ItemStack br) {
			this.slotTopLeft = tl; this.slotTopRight = tr;
			this.slotBotLeft = bl; this.slotBotRight = br;
			return this;
		}

		public ActorGuiPreview withCoil(ItemStack coil) {
			this.coilStack = coil;
			return this;
		}

		@Override
		public void drawBackgroundComponent(GuiGraphics gui, VirtualWorld world, int ticks, float interp) { }

		@Override
		public void updateActor(WiajScene scene) { }

		@Override
		public void drawForegroundComponent(GuiGraphics gui, int w, int h, int ticks, float interp) {
			Font font = Minecraft.getInstance().font;
			float scale = 0.84F;
			int panelW = Math.round(176 * scale);
			int panelH = Math.round(97 * scale);
			int sx = w / 2 + offsetX - panelW / 2;
			int sy = h / 2 + offsetY - panelH / 2;

			ResourceLocation tex = switch (mode) {
				case SOURCE -> TEX_SOURCE;
				case DETECTOR -> TEX_DETECTOR;
				case DIPOLE -> TEX_DIPOLE;
				case QUADRUPOLE -> TEX_QUADRUPOLE;
			};

			gui.pose().pushPose();
			gui.pose().translate(sx, sy, 0);
			gui.pose().scale(scale, scale, 1.0F);

			// Real PA GUI top panel (176x96, cropped cleanly above player inventory)
			gui.blit(tex, 0, 0, 0, 0, 176, 96);
			gui.fill(0, 95, 176, 97, 0xFF555555);

			String titleKey = switch (mode) {
				case SOURCE -> "block.hbm_m.pa_source";
				case DETECTOR -> "block.hbm_m.pa_detector";
				case DIPOLE -> "block.hbm_m.dipole";
				case QUADRUPOLE -> "block.hbm_m.quadrupole";
			};
			String titleStr = PaWiajHelpers.tr(titleKey);
			int titleXOff = (mode == Mode.DETECTOR) ? -8 : -9;
			int titleY = (mode == Mode.SOURCE) ? 4 : (mode == Mode.DETECTOR ? 5 : 6);
			gui.drawString(font, titleStr, 176 / 2 - font.width(titleStr) / 2 + titleXOff, titleY, 0xFFFFFF, false);

			int powerX = (mode == Mode.QUADRUPOLE) ? 26 : 8;
			int tankX = (mode == Mode.QUADRUPOLE) ? 116 : 134;
			int tempX = (mode == Mode.QUADRUPOLE) ? 118 : 136;

			boolean powered = data.getBoolean("powered");
			if (powered) {
				gui.blit(tex, powerX, 18, 184, 0, 16, 52);
			}

			int tempK = data.getInt("tempK");
			boolean coldOk = tempK <= 123;
			if (coldOk) {
				gui.fill(tankX, 42, tankX + 16, 88, 0xFF39C5BB);
				gui.fill(tankX + 18, 72, tankX + 34, 88, 0xFF9EA7AD);
			}

			gui.drawString(font, ChatFormatting.AQUA + "/123K", tempX, 22, 0x404040, false);
			String heatLabel = (tempK > 123 ? ChatFormatting.RED : ChatFormatting.AQUA) + "" + tempK + "K";
			gui.drawString(font, heatLabel, tempX + 30 - font.width(heatLabel), 12, 0x404040, false);

			String hl = data.getString("highlight");
			boolean blink = ((ticks / 6) % 2) == 0;

			if (mode == Mode.SOURCE) {
				if (coldOk) gui.blit(tex, 44, 16, 176, 8, 8, 8);
				if (powered) gui.blit(tex, 44, 41, 176, 8, 8, 8);

				drawSlotItem(gui, font, 62, 16, slotTopLeft);
				drawSlotItem(gui, font, 80, 16, slotTopRight);
				drawSlotItem(gui, font, 62, 43, slotBotLeft);
				drawSlotItem(gui, font, 80, 43, slotBotRight);

				String stRaw = data.getString("status");
				int stCol = "Success".equals(stRaw) ? 0x00FF00 : ("Running".equals(stRaw) ? 0xFFFF00 : ("Crash".equals(stRaw) ? 0xFF0000 : 0x808080));
				String st = switch (stRaw) {
					case "Success" -> PaWiajHelpers.tr("pa.success");
					case "Running" -> PaWiajHelpers.tr("pa.running");
					case "Crash" -> PaWiajHelpers.tr("cannery.pa.hud.crash");
					default -> PaWiajHelpers.tr("pa.idle");
				};
				float r = Math.min(1F, (stCol & 0xFF0000) >> 16);
				float g = Math.min(1F, (stCol & 0x00FF00) >> 8);
				float b = Math.min(1F, stCol & 0x0000FF);
				RenderSystem.setShaderColor(r, g, b, 1.0F);
				gui.blit(tex, 45, 73, 176, 52, 68, 14);
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
				gui.drawString(font, st, 79 - font.width(st) / 2, 76, stCol, false);

				if (blink) {
					if ("topSlots".equals(hl))  drawHighlightRect(gui, 60, 14, 38, 20, 0xFFFFD600);
					if ("botSlots".equals(hl))  drawHighlightRect(gui, 60, 41, 38, 20, 0xFF4CD964);
					if ("infoBtn".equals(hl))   drawHighlightRect(gui, 104, 15, 12, 12, 0xFF39ACFF);
					if ("cancelBtn".equals(hl)) drawHighlightRect(gui, 104, 27, 12, 12, 0xFFFF5252);
				}
			} else if (mode == Mode.DETECTOR) {
				if (coldOk) gui.blit(tex, 43, 18, 176, 8, 8, 8);
				if (powered) gui.blit(tex, 43, 43, 176, 8, 8, 8);

				drawSlotItem(gui, font, 62, 18, slotTopLeft);
				drawSlotItem(gui, font, 80, 18, slotTopRight);
				drawSlotItem(gui, font, 62, 45, slotBotLeft);
				drawSlotItem(gui, font, 80, 45, slotBotRight);

				if (blink) {
					if ("topSlots".equals(hl)) drawHighlightRect(gui, 60, 16, 38, 20, 0xFFFFD600);
					if ("botSlots".equals(hl)) drawHighlightRect(gui, 60, 43, 38, 20, 0xFF4CD964);
				}
			} else if (mode == Mode.DIPOLE) {
				if (powered) gui.blit(tex, 83, 54, 176, 8, 8, 8);
				if (coldOk)  gui.blit(tex, 93, 54, 176, 8, 8, 8);
				boolean hasCoil = data.getBoolean("hasCoil");
				if (hasCoil) {
					gui.blit(tex, 103, 54, 176, 8, 8, 8);
					ItemPACoil.CoilType ct = ItemPACoil.typeOf(coilStack);
					if (ct == ItemPACoil.CoilType.GOLD)             gui.blit(tex, 83, 20, 200, 96, 28, 28);
					else if (ct == ItemPACoil.CoilType.NIOBIUM)     gui.blit(tex, 83, 20, 228, 96, 28, 28);
					else if (ct == ItemPACoil.CoilType.BSCCO)       gui.blit(tex, 83, 20, 200, 124, 28, 28);
					else if (ct == ItemPACoil.CoilType.CHLOROPHYTE) gui.blit(tex, 83, 20, 228, 124, 28, 28);
					drawSlotItem(gui, font, 89, 26, coilStack);
				}

				float pYaw = dirYaw(data.getString("playerDir"));
				drawCompass(gui, 68, 35, pYaw, dirIdx(data.getString("dirLow")));
				drawCompass(gui, 68, 49, pYaw, dirIdx(data.getString("dirHigh")));
				drawCompass(gui, 68, 63, pYaw, dirIdx(data.getString("dirRedstone")));

				gui.drawString(font, String.valueOf(data.getInt("threshold")), 47, 77, 0xFF00FF00, false);

				if (blink) {
					if ("row1".equals(hl))      drawHighlightRect(gui, 60, 27, 16, 16, 0xFF39ACFF);
					if ("row2".equals(hl))      drawHighlightRect(gui, 60, 41, 16, 16, 0xFF4CD964);
					if ("row3".equals(hl))      drawHighlightRect(gui, 60, 55, 16, 16, 0xFFFF5252);
					if ("threshold".equals(hl)) drawHighlightRect(gui, 45, 74, 70, 13, 0xFFFFD600);
					if ("coil".equals(hl))      drawHighlightRect(gui, 83, 20, 28, 28, 0xFFFFD600);
				}
			} else if (mode == Mode.QUADRUPOLE) {
				if (powered) gui.blit(tex, 65, 64, 176, 8, 8, 8);
				if (coldOk)  gui.blit(tex, 75, 64, 176, 8, 8, 8);
				if (data.getBoolean("hasCoil")) {
					gui.blit(tex, 85, 64, 176, 8, 8, 8);
					ItemPACoil.CoilType ct = ItemPACoil.typeOf(coilStack);
					if (ct == ItemPACoil.CoilType.GOLD)             gui.blit(tex, 65, 30, 200, 0, 28, 28);
					else if (ct == ItemPACoil.CoilType.NIOBIUM)     gui.blit(tex, 65, 30, 228, 0, 28, 28);
					else if (ct == ItemPACoil.CoilType.BSCCO)       gui.blit(tex, 65, 30, 200, 28, 28, 28);
					else if (ct == ItemPACoil.CoilType.CHLOROPHYTE) gui.blit(tex, 65, 30, 228, 28, 28, 28);
					drawSlotItem(gui, font, 71, 36, coilStack);
				}
				if (blink && "coil".equals(hl)) drawHighlightRect(gui, 65, 30, 28, 28, 0xFFFFD600);
			}

			if (blink && "temp".equals(hl))  drawHighlightRect(gui, tempX - 4, 10, 40, 80, 0xFF26C6DA);
			if (blink && "power".equals(hl)) drawHighlightRect(gui, powerX - 1, 17, 18, 54, 0xFFFFD600);

			gui.pose().popPose();

			String tip = data.getString("tooltip");
			if (tip.startsWith("cannery.")) tip = PaWiajHelpers.tr(tip);
			if (!tip.isEmpty()) {
				int tw = font.width(tip) + 10;
				int tx = sx + panelW / 2 - tw / 2;
				int ty = sy + panelH + 6;
				gui.fill(tx - 1, ty - 1, tx + tw + 1, ty + 15, 0xFF5000FF);
				gui.fill(tx, ty, tx + tw, ty + 14, 0xF0100010);
				gui.drawString(font, tip, tx + 5, ty + 3, 0xFFFFFFFF, false);
			}
		}

		private static void drawSlotItem(GuiGraphics gui, Font font, int x, int y, ItemStack stack) {
			if (!stack.isEmpty()) {
				gui.renderItem(stack, x, y);
				gui.renderItemDecorations(font, stack, x, y);
				gui.flush();
			}
		}

		private static void drawCompass(GuiGraphics gui, int x, int y, float playerYaw, int outDirIdx) {
			drawNeedle(gui, x, y, 0xFF8080FF, 180F);
			drawNeedle(gui, x, y, 0xFFFF0000, playerYaw - outDirIdx * 90F);
		}

		private static void drawNeedle(GuiGraphics gui, int x, int y, int color, float yaw) {
			gui.pose().pushPose();
			gui.pose().translate(x, y, 0);
			gui.pose().mulPose(Axis.ZN.rotationDegrees(yaw));
			gui.fill(-1, 0, 2, 6, color);
			gui.pose().popPose();
		}

		private static float dirYaw(String d) {
			return switch (d) { case "E" -> -90F; case "S" -> 0F; case "W" -> 90F; default -> 180F; };
		}

		private static int dirIdx(String d) {
			return switch (d) { case "E" -> 1; case "S" -> 2; case "W" -> 3; default -> 0; };
		}

		private static void drawHighlightRect(GuiGraphics gui, int x, int y, int w, int h, int col) {
			gui.fill(x, y, x + w, y + 1, col);
			gui.fill(x, y + h - 1, x + w, y + h, col);
			gui.fill(x, y, x + 1, y + h, col);
			gui.fill(x + w - 1, y, x + w, y + h, col);
		}
	}

	private static void drawColoredCube(GuiGraphics gui, double cx, double cy, double cz, float rHalf,
			float r, float g, float b, float a, boolean xray) {
		drawColoredBox(gui, cx, cy, cz, rHalf, rHalf, rHalf, r, g, b, a, xray);
	}

	private static void drawColoredBox(GuiGraphics gui, double cx, double cy, double cz,
			float sx, float sy, float sz, float r, float g, float b, float a, boolean xray) {
		PoseStack pose = gui.pose();
		pose.pushPose();
		pose.translate(cx, cy, cz);
		var mat = pose.last().pose();
		VertexConsumer vc = gui.bufferSource().getBuffer(
				xray ? PaOverlayRenderTypes.XRAY_QUADS : PaOverlayRenderTypes.DEPTH_QUADS);

		float x0 = -sx, x1 = sx, y0 = -sy, y1 = sy, z0 = -sz, z1 = sz;
		quad(vc, mat, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, r, g, b, a); // UP
		quad(vc, mat, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r, g, b, a); // DOWN
		quad(vc, mat, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, r, g, b, a); // NORTH
		quad(vc, mat, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, a); // SOUTH
		quad(vc, mat, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r, g, b, a); // WEST
		quad(vc, mat, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, r, g, b, a); // EAST

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
