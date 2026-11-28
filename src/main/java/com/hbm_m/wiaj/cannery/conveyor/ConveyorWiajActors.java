package com.hbm_m.wiaj.cannery.conveyor;

import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
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
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Specialized actors for Conveyor & Logistics WIAJ demos:
 * 1) ActorItemStream       — continuous stream of items along waypoints with cycling stacks,
 *                            dynamic speed updates, and parabolic launch off Express belts.
 * 2) ActorWorldHighlight   — pulsing translucent 3D highlight box for I/O ports & bends.
 * 3) ActorSorterGuiPreview — compact 2D overlay of Conveyor Sorter color-coded face filters.
 * 4) ActorMovingItem       — legacy single-item tile actor kept for CanneryConveyorPress.
 */
public final class ConveyorWiajActors {

	private ConveyorWiajActors() { }

	private static final class OverlayTypes extends RenderStateShard {
		private OverlayTypes() { super("conveyor_overlay", () -> {}, () -> {}); }

		static final RenderType DEPTH_QUADS = RenderType.create(
				"conveyor_wiaj_depth_quads",
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

	/**
	 * Continuous stream of items along a polyline of waypoints.
	 * Supports cycling through a list of ItemStacks, configurable spacing, start delay
	 * ({@code phaseOffset}), and parabolic launch arc on the final segment ({@code launchArcHeight}).
	 */
	public static class ActorItemStream extends ActorBase {

		private final List<ItemStack> cycleStacks;
		private final double[][] waypoints;
		private final double[] segLengths;
		private final double totalLength;
		private final double defaultSpeed;
		private final double spacing;
		private final double phaseOffset;
		private final double launchArcHeight;

		public ActorItemStream(ItemStack singleStack, double speed, double spacing, double[]... waypoints) {
			this(List.of(singleStack), speed, spacing, 0.0, 0.0, waypoints);
		}

		public ActorItemStream(List<ItemStack> cycleStacks, double speed, double spacing,
				double phaseOffset, double launchArcHeight, double[]... waypoints) {
			this.cycleStacks = cycleStacks;
			this.defaultSpeed = speed;
			this.spacing = Math.max(0.4, spacing);
			this.phaseOffset = phaseOffset;
			this.launchArcHeight = launchArcHeight;
			this.waypoints = waypoints;

			this.segLengths = new double[Math.max(0, waypoints.length - 1)];
			double sum = 0.0;
			for (int i = 0; i < segLengths.length; i++) {
				double dx = waypoints[i + 1][0] - waypoints[i][0];
				double dy = waypoints[i + 1][1] - waypoints[i][1];
				double dz = waypoints[i + 1][2] - waypoints[i][2];
				segLengths[i] = Math.sqrt(dx * dx + dy * dy + dz * dz);
				sum += segLengths[i];
			}
			this.totalLength = sum;
			this.data.putDouble("dist", 0.0);
			this.data.putDouble("prevDist", 0.0);
			this.data.putBoolean("visible", true);
		}

		@Override
		public void updateActor(WiajScene scene) {
			double cur = data.getDouble("dist");
			data.putDouble("prevDist", cur);
			if (data.getBoolean("paused")) return;

			double speed = data.contains("speed") ? data.getDouble("speed") : defaultSpeed;
			data.putDouble("dist", cur + speed);
		}

		@Override
		public void drawForegroundComponent(GuiGraphics gui, int w, int h, int ticks, float interp) { }

		@Override
		public void drawBackgroundComponent(GuiGraphics gui, VirtualWorld world, int ticks, float interp) {
			if (!data.getBoolean("visible") || totalLength <= 1e-4) return;

			double travel = BobMathUtil.interp(data.getDouble("prevDist"), data.getDouble("dist"), interp);
			double effective = travel - phaseOffset;
			if (effective < 0) return;

			int firstIdx = Math.max(0, (int) Math.floor((effective - totalLength) / spacing) + 1);
			int lastIdx = (int) Math.floor(effective / spacing);

			ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
			double[] pos = new double[3];

			for (int idx = firstIdx; idx <= lastIdx; idx++) {
				double d = effective - idx * spacing;
				if (d < 0.0 || d > totalLength) continue;

				samplePath(d, pos);
				ItemStack stack = cycleStacks.get(Math.floorMod(idx, cycleStacks.size()));
				renderFlatOrBlockItem(gui, itemRenderer, stack, pos[0], pos[1], pos[2]);
			}
		}

		private void samplePath(double d, double[] out) {
			double rem = d;
			for (int i = 0; i < segLengths.length; i++) {
				double len = segLengths[i];
				if (rem <= len || i == segLengths.length - 1) {
					double t = len > 1e-5 ? Mth.clamp(rem / len, 0.0, 1.0) : 0.0;
					double[] p0 = waypoints[i];
					double[] p1 = waypoints[i + 1];
					out[0] = p0[0] + (p1[0] - p0[0]) * t;
					out[1] = p0[1] + (p1[1] - p0[1]) * t;
					out[2] = p0[2] + (p1[2] - p0[2]) * t;

					if (launchArcHeight > 0.0 && i == segLengths.length - 1) {
						out[1] += Math.sin(t * Math.PI) * launchArcHeight;
					}
					return;
				}
				rem -= len;
			}
		}

		private static void renderFlatOrBlockItem(GuiGraphics gui, ItemRenderer renderer,
				ItemStack stack, double x, double y, double z) {
			gui.pose().pushPose();
			gui.pose().translate(x, y, z);

			BakedModel model = renderer.getModel(stack, null, null, 0);
			if (!model.isGui3d()) {
				gui.pose().mulPose(Axis.XP.rotationDegrees(90F));
				gui.pose().scale(0.5F, 0.5F, 0.5F);
			} else {
				gui.pose().translate(0.0, 0.125, 0.0);
				gui.pose().scale(0.5F, 0.5F, 0.5F);
			}

			renderer.render(stack, ItemDisplayContext.FIXED, false, gui.pose(), gui.bufferSource(),
					LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
			gui.pose().popPose();
		}
	}

	/** Pulsing translucent 3D box highlight for key logistics nodes. */
	public static class ActorWorldHighlight extends ActorBase {

		public ActorWorldHighlight(double minX, double minY, double minZ,
				double maxX, double maxY, double maxZ, int argb) {
			data.putDouble("minX", minX); data.putDouble("minY", minY); data.putDouble("minZ", minZ);
			data.putDouble("maxX", maxX); data.putDouble("maxY", maxY); data.putDouble("maxZ", maxZ);
			data.putInt("color", argb);
		}

		@Override public void drawForegroundComponent(GuiGraphics gui, int w, int h, int ticks, float interp) { }
		@Override public void updateActor(WiajScene scene) { }

		@Override
		public void drawBackgroundComponent(GuiGraphics gui, VirtualWorld world, int ticks, float interp) {
			int col = data.getInt("color");
			float a = (((col >> 24) & 0xFF) / 255.0F) * (0.6F + 0.4F * Mth.sin((ticks + interp) * 0.25F));
			float r = ((col >> 16) & 0xFF) / 255.0F;
			float g = ((col >> 8) & 0xFF) / 255.0F;
			float b = (col & 0xFF) / 255.0F;

			double cx = (data.getDouble("minX") + data.getDouble("maxX")) * 0.5;
			double cy = (data.getDouble("minY") + data.getDouble("maxY")) * 0.5;
			double cz = (data.getDouble("minZ") + data.getDouble("maxZ")) * 0.5;
			float sx = (float) (data.getDouble("maxX") - data.getDouble("minX")) * 0.5F + 0.02F;
			float sy = (float) (data.getDouble("maxY") - data.getDouble("minY")) * 0.5F + 0.02F;
			float sz = (float) (data.getDouble("maxZ") - data.getDouble("minZ")) * 0.5F + 0.02F;

			PoseStack pose = gui.pose();
			pose.pushPose();
			pose.translate(cx, cy, cz);
			var mat = pose.last().pose();
			VertexConsumer vc = gui.bufferSource().getBuffer(OverlayTypes.DEPTH_QUADS);

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

	/**
	 * Schematic 2D overlay of Conveyor Sorter face filters.
	 */
	public static class ActorSorterGuiPreview extends ActorBase {

		private final int offsetX, offsetY;
		private final ItemStack northFilter;
		private final ItemStack eastFilter;
		private final ItemStack southFilter;

		public ActorSorterGuiPreview(int offsetX, int offsetY,
				ItemStack northFilter, ItemStack eastFilter, ItemStack southFilter) {
			this.offsetX = offsetX;
			this.offsetY = offsetY;
			this.northFilter = northFilter;
			this.eastFilter = eastFilter;
			this.southFilter = southFilter;
		}

		@Override public void drawBackgroundComponent(GuiGraphics gui, VirtualWorld world, int ticks, float interp) { }
		@Override public void updateActor(WiajScene scene) { }

		@Override
		public void drawForegroundComponent(GuiGraphics gui, int w, int h, int ticks, float interp) {
			Font font = Minecraft.getInstance().font;
			int boxW = 112;
			int boxH = 74;
			int x = w / 2 + offsetX - boxW / 2;
			int y = h / 2 + offsetY - boxH / 2;

			gui.fill(x - 2, y - 2, x + boxW + 2, y + boxH + 2, 0xFF919191);
			gui.fill(x, y, x + boxW, y + boxH, 0xE622252A);

			String title = ConveyorWiajHelpers.tr("cannery.conveyor.sorter_gui.title");
			gui.drawString(font, title, x + (boxW - font.width(title)) / 2, y + 5, 0xFFFFD64A, false);

			drawRow(gui, font, x + 6, y + 18, 0xFFFFFF00, "N", northFilter);
			drawRow(gui, font, x + 6, y + 36, 0xFF8000FF, "E", eastFilter);
			drawRow(gui, font, x + 6, y + 54, 0xFF00FF00, "S", southFilter);
		}

		private static void drawRow(GuiGraphics gui, Font font, int x, int y, int borderCol, String dir, ItemStack stack) {
			gui.fill(x, y, x + 100, y + 16, borderCol);
			gui.fill(x + 1, y + 1, x + 99, y + 15, 0xFF30343B);
			gui.drawString(font, dir, x + 5, y + 4, 0xFFFFFFFF, false);
			gui.renderItem(stack, x + 20, y);
			gui.flush();
			gui.drawString(font, stack.getHoverName().getString(), x + 40, y + 4, 0xFFE0E0E0, false);
		}
	}

	/**
	 * Legacy single-item tile actor kept for {@link com.hbm_m.wiaj.cannery.CanneryConveyorPress}.
	 */
	public static class ActorMovingItem implements ITileActorRenderer {

		private final ItemStack defaultStack;
		private final double[][] waypoints;
		private final double defaultSpeed;
		private final boolean loop;

		public ActorMovingItem(ItemStack stack, double speed, boolean loop, double[]... waypoints) {
			this.defaultStack = stack;
			this.defaultSpeed = speed;
			this.loop = loop;
			this.waypoints = waypoints;
		}

		@Override
		public void renderActor(GuiGraphics gui, VirtualWorld world, int ticks, float interp, CompoundTag data) {
			if (data.getBoolean("hidden") || this.waypoints == null || this.waypoints.length < 2) return;

			double curX = data.getDouble("curX");
			double curY = data.getDouble("curY");
			double curZ = data.getDouble("curZ");
			double prevX = data.contains("prevX") ? data.getDouble("prevX") : curX;
			double prevY = data.contains("prevY") ? data.getDouble("prevY") : curY;
			double prevZ = data.contains("prevZ") ? data.getDouble("prevZ") : curZ;

			double rx = BobMathUtil.interp(prevX, curX, interp);
			double ry = BobMathUtil.interp(prevY, curY, interp);
			double rz = BobMathUtil.interp(prevZ, curZ, interp);

			ItemStack stack = this.defaultStack;
			if (data.contains("item")) {
				String itemKey = data.getString("item");
				if ("iron_plate".equals(itemKey)) {
					stack = ModMaterialItems.stack(ModMaterials.IRON, MaterialShape.PLATE, 1);
				} else if ("iron_ingot".equals(itemKey)) {
					stack = new ItemStack(Items.IRON_INGOT);
				} else if ("gold_ingot".equals(itemKey)) {
					stack = new ItemStack(Items.GOLD_INGOT);
				} else if ("copper_ingot".equals(itemKey)) {
					stack = new ItemStack(Items.COPPER_INGOT);
				} else if ("coal".equals(itemKey)) {
					stack = new ItemStack(Items.COAL);
				} else if ("crate".equals(itemKey)) {
					stack = new ItemStack(ModBlocks.CRATE.get());
				}
			}

			gui.pose().pushPose();
			gui.pose().translate(rx, ry, rz);

			ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
			BakedModel model = itemRenderer.getModel(stack, null, null, 0);
			if (!model.isGui3d()) {
				gui.pose().mulPose(Axis.XP.rotationDegrees(90F));
				gui.pose().scale(0.5F, 0.5F, 0.5F);
			} else {
				gui.pose().translate(0.0, 0.125, 0.0);
				gui.pose().scale(0.5F, 0.5F, 0.5F);
			}

			itemRenderer.render(stack, ItemDisplayContext.FIXED, false, gui.pose(), gui.bufferSource(),
					LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
			gui.pose().popPose();
		}

		@Override
		public void updateActor(int ticks, CompoundTag data) {
			if (this.waypoints == null || this.waypoints.length < 2) return;

			if (!data.contains("initialized")) {
				data.putBoolean("initialized", true);
				data.putInt("seg", 0);
				data.putDouble("progress", 0.0);
				data.putDouble("curX", waypoints[0][0]);
				data.putDouble("curY", waypoints[0][1]);
				data.putDouble("curZ", waypoints[0][2]);
				data.putDouble("prevX", waypoints[0][0]);
				data.putDouble("prevY", waypoints[0][1]);
				data.putDouble("prevZ", waypoints[0][2]);
			}

			if (data.getBoolean("paused")) {
				data.putDouble("prevX", data.getDouble("curX"));
				data.putDouble("prevY", data.getDouble("curY"));
				data.putDouble("prevZ", data.getDouble("curZ"));
				return;
			}

			data.putDouble("prevX", data.getDouble("curX"));
			data.putDouble("prevY", data.getDouble("curY"));
			data.putDouble("prevZ", data.getDouble("curZ"));

			int seg = data.getInt("seg");
			double progress = data.getDouble("progress");
			double speed = data.contains("speed") ? data.getDouble("speed") : this.defaultSpeed;

			if (seg >= waypoints.length - 1) {
				if (this.loop) {
					seg = 0;
					progress = 0.0;
					data.putDouble("prevX", waypoints[0][0]);
					data.putDouble("prevY", waypoints[0][1]);
					data.putDouble("prevZ", waypoints[0][2]);
					data.putDouble("curX", waypoints[0][0]);
					data.putDouble("curY", waypoints[0][1]);
					data.putDouble("curZ", waypoints[0][2]);
				} else {
					return;
				}
			}

			double[] p0 = waypoints[seg];
			double[] p1 = waypoints[seg + 1];
			double dx = p1[0] - p0[0];
			double dy = p1[1] - p0[1];
			double dz = p1[2] - p0[2];
			double len = Math.sqrt(dx * dx + dy * dy + dz * dz);

			progress += speed;

			while (len > 1.0e-5 && progress >= len && seg < waypoints.length - 1) {
				progress -= len;
				seg++;
				if (seg < waypoints.length - 1) {
					p0 = waypoints[seg];
					p1 = waypoints[seg + 1];
					dx = p1[0] - p0[0];
					dy = p1[1] - p0[1];
					dz = p1[2] - p0[2];
					len = Math.sqrt(dx * dx + dy * dy + dz * dz);
				}
			}

			if (seg >= waypoints.length - 1) {
				if (this.loop) {
					seg = 0;
					progress = 0.0;
					p0 = waypoints[0];
					p1 = waypoints[1];
					dx = p1[0] - p0[0];
					dy = p1[1] - p0[1];
					dz = p1[2] - p0[2];
					len = Math.sqrt(dx * dx + dy * dy + dz * dz);
				} else {
					seg = waypoints.length - 1;
					data.putInt("seg", seg);
					data.putDouble("progress", 0.0);
					data.putDouble("curX", waypoints[seg][0]);
					data.putDouble("curY", waypoints[seg][1]);
					data.putDouble("curZ", waypoints[seg][2]);
					return;
				}
			}

			double t = len > 1.0e-5 ? (progress / len) : 0.0;
			data.putInt("seg", seg);
			data.putDouble("progress", progress);
			data.putDouble("curX", p0[0] + dx * t);
			data.putDouble("curY", p0[1] + dy * t);
			data.putDouble("curZ", p0[2] + dz * t);
		}
	}
}
