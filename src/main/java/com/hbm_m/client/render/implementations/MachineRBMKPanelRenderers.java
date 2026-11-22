package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.rbmk.RBMKGaugeBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKGraphBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKIndicatorBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKKeyPadBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKLeverBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKNumitronBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKPanelDeviceBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKTerminalBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderHook;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.Map;

/**
 * Nucleus specs for the seven RBMK control-room panel devices handled here
 * (gauge, indicator, lever, keypad, numitron, graph, terminal), replacing the
 * corresponding subclasses of the legacy {@code RBMKPanelRenderer} abstract BER
 * (1:1 ports of the original's {@code RenderRBMK*} tile entity special
 * renderers). The display mini-map lives in its own spec,
 * {@link MachineRbmkDisplayRenderer}.
 *
 * <p>Follows the same pattern as {@link MachineRBMKColumnRenderer}: the panels
 * carry no baked machine-parts geometry — their device meshes come from the
 * shared static OBJ cache ({@code RBMKColumnRenderer.getObj}) and their faces
 * carry per-frame dynamic content (needles, lamps, labels, graphs, console
 * text), so everything draws in the {@link MachineRenderHook} on the immediate
 * path while the engine supplies culling, distance fade, the Iris tiers and
 * roster handling around it.</p>
 *
 * <p>The spec's block transform is left at the factory default
 * {@code T(0.5,0,0.5)·R_y(90+facing)}: for {@code RBMKMiniPanelBlock.FACING}
 * that is NORTH→90°, WEST→180°, SOUTH→270°, EAST→0° — exactly the
 * metadata-to-angle switch the legacy {@code RBMKPanelRenderer.begin()}
 * applied, so every hook's local coordinates are identical to the legacy
 * renderer's after {@code begin()}.</p>
 *
 * <p>One spec per BlockEntityType (the factory keys on the BE type); the hook
 * content of each is ported 1:1 from the corresponding legacy subclass.</p>
 */
public final class MachineRBMKPanelRenderers {

	/** Original: {@code RenderArcFurnace.fullbright(true)} - lightmap pinned to maximum. */
	private static final int FULLBRIGHT = LightTexture.FULL_BRIGHT;

	/** Panel body/device OBJ models all live under this folder in the port. */
	private static final String MODEL_ROOT = "models/rbmk/models/";

	public static void register() {
		// The display mini-map is a separate hook-only spec (MachineRbmkDisplayRenderer).
		panel("rbmk_panel_gauge",    ModBlockEntities.RBMK_GAUGE_BE,    RBMKGaugeBlockEntity.class,    MachineRBMKPanelRenderers::renderGauge);
		panel("rbmk_panel_indicator",ModBlockEntities.RBMK_INDICATOR_BE,RBMKIndicatorBlockEntity.class,MachineRBMKPanelRenderers::renderIndicator);
		panel("rbmk_panel_lever",    ModBlockEntities.RBMK_LEVER_BE,    RBMKLeverBlockEntity.class,    MachineRBMKPanelRenderers::renderLever);
		panel("rbmk_panel_keypad",   ModBlockEntities.RBMK_KEYPAD_BE,   RBMKKeyPadBlockEntity.class,   MachineRBMKPanelRenderers::renderKeyPad);
		panel("rbmk_panel_numitron", ModBlockEntities.RBMK_NUMITRON_BE, RBMKNumitronBlockEntity.class, MachineRBMKPanelRenderers::renderNumitron);
		panel("rbmk_panel_graph",    ModBlockEntities.RBMK_GRAPH_BE,    RBMKGraphBlockEntity.class,    MachineRBMKPanelRenderers::renderGraph);
		panel("rbmk_panel_terminal", ModBlockEntities.RBMK_TERMINAL_BE, RBMKTerminalBlockEntity.class, MachineRBMKPanelRenderers::renderTerminal);
	}

	/**
	 * One spec per panel BE type. The hook is never empty (the panel face always
	 * draws), so {@code mayDrawContent} is constant true. 64 blocks matches the
	 * vanilla BER default view distance the legacy renderer ran at.
	 */
	private static <T extends RBMKPanelDeviceBlockEntity> void panel(
			String id,
			dev.architectury.registry.registries.RegistrySupplier<net.minecraft.world.level.block.entity.BlockEntityType<T>> type,
			Class<T> cls,
			PanelContent<T> content) {
		MachineRenderers.machine(id, type.get(), cls)
			.hook(new MachineRenderHook<T>() {
				@Override
				public void render(T be, float pt, PoseStack ps, MultiBufferSource buf,
						int light, int overlay, MachineRenderApi api) {
					content.render(be, pt, ps, buf, light, overlay);
				}

				@Override
				public boolean mayDrawContent(T be) {
					return true;
				}
			})
			.viewDistance(64)
			.register();
	}

	/** Per-type hook content, 1:1 from the legacy RBMK*Renderer subclasses. */
	private interface PanelContent<T extends RBMKPanelDeviceBlockEntity> {
		void render(T be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay);
	}

	// ─── Shared plumbing (ported from the legacy RBMKPanelRenderer base) ────

	/** The original's per-metadata rotation, expressed against FACING (documentation only -
	 * the factory's default block transform reproduces it). */
	// NORTH(2)→90°, WEST(4)→180°, SOUTH(3)→270°, EAST(5)→0°.

	protected static Map<String, List<float[]>> model(String name) {
		return RBMKColumnRenderer.getObj(MODEL_ROOT + name + ".obj");
	}

	protected static TextureAtlasSprite panelSprite(String name) {
		return RBMKColumnRenderer.sprite(RefStrings.MODID, "block/rbmk/panel/" + name);
	}

	/** Draws one named OBJ group with a flat tint, matching the original's {@code renderPart}. */
	protected static void part(PoseStack ps, MultiBufferSource buf, Map<String, List<float[]>> obj,
			String group, TextureAtlasSprite sprite,
			float r, float g, float b, int light, int overlay) {
		List<float[]> mesh = obj.get(group);
		if (mesh == null) return;
		VertexConsumer vc = buf.getBuffer(RenderType.solid());
		RBMKColumnRenderer.renderObjGroup(vc, ps.last().pose(), mesh, sprite, r, g, b, light, overlay);
	}

	/** Draws every group of a model, matching the original's {@code renderAll}. */
	protected static void all(PoseStack ps, MultiBufferSource buf, Map<String, List<float[]>> obj,
			TextureAtlasSprite sprite, float r, float g, float b, int light, int overlay) {
		VertexConsumer vc = buf.getBuffer(RenderType.solid());
		for (List<float[]> mesh : obj.values())
			RBMKColumnRenderer.renderObjGroup(vc, ps.last().pose(), mesh, sprite, r, g, b, light, overlay);
	}

	protected static Font font() {
		return Minecraft.getInstance().font;
	}

	/**
	 * A single quad in the panel's face plane (constant local X) with an explicit UV sub-rect on
	 * the given sprite. The original drew these straight onto a bound texture with raw 0-1 UVs;
	 * here the same fractions are remapped into the sprite's slot in the block atlas.
	 */
	protected static void uvQuad(PoseStack ps, MultiBufferSource buf, TextureAtlasSprite sprite,
			float x, float y0, float y1, float z0, float z1,
			float u0, float u1, float v0, float v1,
			float r, float g, float b, int light, int overlay) {
		VertexConsumer vc = buf.getBuffer(RenderType.solid());
		var m = ps.last().pose();
		float au0 = sprite.getU0() + u0 * (sprite.getU1() - sprite.getU0());
		float au1 = sprite.getU0() + u1 * (sprite.getU1() - sprite.getU0());
		float av0 = sprite.getV0() + v0 * (sprite.getV1() - sprite.getV0());
		float av1 = sprite.getV0() + v1 * (sprite.getV1() - sprite.getV0());
		RenderHooks.vertexFull(vc, m, x, y0, z0, (int)(r * 255f), (int)(g * 255f), (int)(b * 255f), (int)(1f * 255f), au0, av1, overlay, light, 1, 0, 0);
		RenderHooks.vertexFull(vc, m, x, y1, z0, (int)(r * 255f), (int)(g * 255f), (int)(b * 255f), (int)(1f * 255f), au0, av0, overlay, light, 1, 0, 0);
		RenderHooks.vertexFull(vc, m, x, y1, z1, (int)(r * 255f), (int)(g * 255f), (int)(b * 255f), (int)(1f * 255f), au1, av0, overlay, light, 1, 0, 0);
		RenderHooks.vertexFull(vc, m, x, y0, z1, (int)(r * 255f), (int)(g * 255f), (int)(b * 255f), (int)(1f * 255f), au1, av1, overlay, light, 1, 0, 0);
	}

	/** One line segment, replacing the original's {@code GL_LINES} tessellator batch. */
	protected static void line(PoseStack ps, MultiBufferSource buf,
			float x0, float y0, float z0, float x1, float y1, float z1,
			float r, float g, float b) {
		VertexConsumer vc = buf.getBuffer(RenderType.lines());
		var pose = ps.last();
		RenderHooks.vertexLine(vc, pose, x0, y0, z0, (int)(r * 255f), (int)(g * 255f), (int)(b * 255f), (int)(1f * 255f), 1, 0, 0);
		RenderHooks.vertexLine(vc, pose, x1, y1, z1, (int)(r * 255f), (int)(g * 255f), (int)(b * 255f), (int)(1f * 255f), 1, 0, 0);
	}

	/**
	 * The label-drawing block every panel repeats verbatim: scale down, face outwards along the
	 * panel normal, and draw the string centered. The original does
	 * {@code glScalef(f3,-f3,f3); glRotatef(90,0,1,0); font.drawString(label, -width/2, -height/2, color)}
	 * - the Y flip is what turns Minecraft's downward-growing text space the right way up in world
	 * space, and is reproduced here by the negative Y scale.
	 */
	protected static void label(PoseStack ps, MultiBufferSource buf, String text,
			float maxWidth, int color, int light) {
		if (text == null || text.isEmpty()) return;
		Font font = font();
		int width = font.width(text);
		float scale = Math.min(0.0125f, maxWidth / Math.max(width, 1));

		ps.pushPose();
		ps.scale(scale, -scale, scale);
		ps.mulPose(Axis.YP.rotationDegrees(90));
		font.drawInBatch(text, -width / 2f, -font.lineHeight / 2f, color, false,
				ps.last().pose(), buf, Font.DisplayMode.NORMAL, 0, light);
		ps.popPose();
	}

	/** Left-aligned variant used by the gauge scale ticks and the graph's bound labels. */
	protected static void labelRaw(PoseStack ps, MultiBufferSource buf, String text,
			float scale, float x, int color, int light) {
		if (text == null || text.isEmpty()) return;
		Font font = font();
		ps.pushPose();
		ps.scale(scale, -scale, scale);
		ps.mulPose(Axis.YP.rotationDegrees(90));
		font.drawInBatch(text, x, -font.lineHeight / 2f, color, false,
				ps.last().pose(), buf, Font.DisplayMode.NORMAL, 0, light);
		ps.popPose();
	}

	protected static float red(int rgb)   { return ((rgb >> 16) & 0xFF) / 255f; }
	protected static float green(int rgb) { return ((rgb >>  8) & 0xFF) / 255f; }
	protected static float blue(int rgb)  { return ( rgb        & 0xFF) / 255f; }

	// ─── Gauge (1:1 from RBMKGaugeRenderer) ─────────────────────────────────

	private static void renderGauge(RBMKGaugeBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf,
			int light, int overlay) {
		Map<String, List<float[]>> obj = model("gauge");
		TextureAtlasSprite tex = panelSprite("gauge");

		ps.pushPose();

		for (int i = 0; i < RBMKGaugeBlockEntity.UNITS; i++) {
			if (!be.isUnitActive(i)) continue;

			ps.pushPose();
			ps.translate(0.25, (i / 2) * -0.5 + 0.25, (i % 2) * -0.5 + 0.25);

			part(ps, buf, obj, "Gauge", tex, 1f, 1f, 1f, light, overlay);

			// ── Needle ────────────────────────────────────────────────────────
			int color = be.getUnitColor(i);
			double value = Mth.lerp(pt, be.lastValue[i], be.value[i]);
			double lower = Math.min(be.min[i], be.max[i]);
			double upper = Math.max(be.min[i], be.max[i]);
			if (lower == upper) upper += 1;
			double angle = (value - lower) / (upper - lower) * 50D;
			if (be.min[i] > be.max[i]) angle = 50 - angle;
			angle = Mth.clamp(angle, 0, 80);

			ps.pushPose();
			ps.translate(0, 0.4375, -0.125);
			ps.mulPose(Axis.XN.rotationDegrees((float) (angle - 85)));
			ps.translate(0, -0.4375, 0.125);
			part(ps, buf, obj, "Needle", tex,
					red(color), green(color), blue(color), FULLBRIGHT, overlay);
			ps.popPose();

			// ── Scale end labels, printed along the dial arc ──────────────────
			String lineLower = gaugeShortNumber(be.min[i]);
			String lineUpper = gaugeShortNumber(be.max[i]);
			for (int j = 0; j < 2; j++) {
				ps.pushPose();
				ps.translate(0, 0.4375, -0.125);
				ps.mulPose(Axis.XN.rotationDegrees(10 + j * 50));
				ps.translate(0, -0.4375, 0.125);
				ps.translate(0.032, 0.4375, 0.125);
				labelRaw(ps, buf, j == 0 ? lineLower : lineUpper, 0.0025f, 0f, 0x000000, light);
				ps.popPose();
			}

			ps.translate(0.01, 0.3125, 0);
			label(ps, buf, be.getUnitLabel(i), 0.4f, 0x00FF00, FULLBRIGHT);

			ps.popPose();
		}

		ps.popPose();
	}

	/** Original: {@code Math.abs(v) <= 10_000 ? v + "" : BobMathUtil.getShortNumber(v)}. */
	private static String gaugeShortNumber(double v) {
		long l = (long) v;
		if (Math.abs(l) <= 10_000) return Long.toString(l);
		long abs = Math.abs(l);
		if (abs < 1_000_000L)         return (l / 1_000L) + "k";
		if (abs < 1_000_000_000L)     return (l / 1_000_000L) + "M";
		if (abs < 1_000_000_000_000L) return (l / 1_000_000_000_000L) + "G";
		return (l / 1_000_000_000_000L) + "T";
	}

	// ─── Indicator (1:1 from RBMKIndicatorRenderer) ─────────────────────────

	private static void renderIndicator(RBMKIndicatorBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf,
			int light, int overlay) {
		Map<String, List<float[]>> obj = model("indicator");
		TextureAtlasSprite tex = panelSprite("indicator");

		ps.pushPose();

		for (int i = 0; i < RBMKIndicatorBlockEntity.UNITS; i++) {
			if (!be.isUnitActive(i)) continue;

			ps.pushPose();
			ps.translate(0.25, (i / 2) * -0.3125 + 0.3125, (i % 2) * -0.5 + 0.25);

			part(ps, buf, obj, "Base", tex, 1f, 1f, 1f, light, overlay);

			boolean lit = be.state[i];
			float mult = lit ? 1f : 0.35f;
			int color = be.getUnitColor(i);
			part(ps, buf, obj, "Light", tex,
					red(color) * mult, green(color) * mult, blue(color) * mult,
					lit ? FULLBRIGHT : light, overlay);

			ps.translate(0.0725, 0.5, 0);
			label(ps, buf, be.getUnitLabel(i), 0.3f, 0x000000, light);

			ps.popPose();
		}

		ps.popPose();
	}

	// ─── Lever (1:1 from RBMKLeverRenderer) ─────────────────────────────────

	private static void renderLever(RBMKLeverBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf,
			int light, int overlay) {
		Map<String, List<float[]>> obj = model("lever");
		TextureAtlasSprite tex = panelSprite("lever");

		ps.pushPose();

		for (int i = 0; i < RBMKLeverBlockEntity.UNITS; i++) {
			if (!be.isUnitActive(i)) continue;

			ps.pushPose();
			ps.translate(0.25, 0, i * -0.5 + 0.25);

			part(ps, buf, obj, "Base", tex, 1f, 1f, 1f, light, overlay);

			float progress = (float) Mth.lerp(pt, be.prevFlip[i], be.flip[i]);
			ps.pushPose();
			ps.translate(0.125, 0.5625, 0);
			ps.mulPose(Axis.ZP.rotationDegrees(-180f * progress));
			ps.translate(-0.125, -0.5625, 0);
			part(ps, buf, obj, "Lever", tex, 1f, 1f, 1f, light, overlay);
			ps.popPose();

			ps.translate(0.01, 0.0625, 0);
			label(ps, buf, be.getUnitLabel(i), 0.4f, 0x00FF00, FULLBRIGHT);

			ps.popPose();
		}

		ps.popPose();
	}

	// ─── KeyPad (1:1 from RBMKKeyPadRenderer) ───────────────────────────────

	private static void renderKeyPad(RBMKKeyPadBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf,
			int light, int overlay) {
		Map<String, List<float[]>> obj = model("button");
		TextureAtlasSprite tex = panelSprite("keypad");

		ps.pushPose();

		for (int i = 0; i < RBMKKeyPadBlockEntity.UNITS; i++) {
			if (!be.isUnitActive(i)) continue;

			boolean glow = be.isPressed[i];
			float mult = glow ? 1f : 0.65f;

			ps.pushPose();
			ps.translate(0.25, (i / 2) * -0.5 + 0.25, (i % 2) * -0.5 + 0.25);

			part(ps, buf, obj, "Socket", tex, 1f, 1f, 1f, light, overlay);

			ps.pushPose();
			ps.translate(glow ? -0.03125 : 0, 0, 0);
			int color = be.getUnitColor(i);
			part(ps, buf, obj, "Button", tex,
					red(color) * mult, green(color) * mult, blue(color) * mult,
					glow ? FULLBRIGHT : light, overlay);
			ps.popPose();

			ps.translate(0.01, 0.3125, 0);
			label(ps, buf, be.getUnitLabel(i), 0.4f, 0x00FF00, FULLBRIGHT);

			ps.popPose();
		}

		ps.popPose();
	}

	// ─── Numitron (1:1 from RBMKNumitronRenderer) ───────────────────────────

	private static final int NUMITRON_DIGITS = 7;
	private static final double NUMITRON_SCALE = 200D;
	private static final float NUMITRON_W = (float) (8D / NUMITRON_SCALE);
	private static final float NUMITRON_H = (float) (13D / NUMITRON_SCALE);
	private static final float NUMITRON_Y_OFFSET = 0.5625f;
	private static final float NUMITRON_PLANE_X = 0.03135f;

	private static void renderNumitron(RBMKNumitronBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf,
			int light, int overlay) {
		Map<String, List<float[]>> obj = model("numitron");
		TextureAtlasSprite body   = panelSprite("numitron");
		TextureAtlasSprite lights = panelSprite("numitron_lights");

		ps.pushPose();

		for (int i = 0; i < RBMKNumitronBlockEntity.UNITS; i++) {
			if (!be.isUnitActive(i)) continue;

			ps.pushPose();
			ps.translate(0.25, i * -0.5 + 0.25, 0);

			all(ps, buf, obj, body, 1f, 1f, 1f, light, overlay);

			String value = numitronFormat(be.value[i], be.shortenNumber[i], be.leadingZeroes[i]);

			for (int j = 0; j < NUMITRON_DIGITS; j++) {
				// 0x40 is the leftmost digit's bit, matching the original's mask walk.
				if ((be.activeDigits[i] & (0x40L >> j)) == 0) continue;

				char c = value.charAt(j);
				if (c == ' ') continue;

				float[] uv = numitronCell(c);
				float zOffset = (float) ((j - 3) * 0.1D);
				uvQuad(ps, buf, lights, NUMITRON_PLANE_X,
						-NUMITRON_H + NUMITRON_Y_OFFSET, NUMITRON_H + NUMITRON_Y_OFFSET,
						NUMITRON_W - zOffset, -NUMITRON_W - zOffset,
						uv[0], uv[0] + 0.1f, uv[1], uv[1] + 0.5f,
						1f, 1f, 1f, FULLBRIGHT, overlay);
			}

			ps.translate(0.01, 0.3125, 0);
			label(ps, buf, be.getUnitLabel(i), 0.75f, 0x00FF00, FULLBRIGHT);

			ps.popPose();
		}

		ps.popPose();
	}

	/** The original's character-to-UV table; anything unknown falls back to '-'. */
	private static float[] numitronCell(char c) {
		if (c >= '0' && c <= '9') return new float[]{0.1f * (c - '0'), 0.0f};
		return switch (c) {
			case '.' -> new float[]{0.9f, 0.5f};
			case 'k' -> new float[]{0.0f, 0.5f};
			case 'M' -> new float[]{0.1f, 0.5f};
			case 'G' -> new float[]{0.2f, 0.5f};
			case 'T' -> new float[]{0.3f, 0.5f};
			case 'P' -> new float[]{0.4f, 0.5f};
			case 'E' -> new float[]{0.5f, 0.5f};
			default  -> new float[]{0.8f, 0.5f}; // '-'
		};
	}

	/**
	 * Original: clamp to the tube's range, then pad to seven characters - with the padding zeroes
	 * inserted *after* a leading minus sign rather than before it.
	 */
	private static String numitronFormat(double raw, boolean shorten, boolean leadingZeroes) {
		long v = (long) raw;
		String value;
		if (shorten) {
			value = numitronShortNumber(v);
		} else if (v > 9999999L) {
			value = "9999999";
		} else if (v < -999999L) {
			value = "-999999";
		} else {
			value = Long.toString(v);
		}

		if (value.length() < NUMITRON_DIGITS && !value.isEmpty() && value.charAt(0) == '-' && leadingZeroes) {
			String body = value.substring(1);
			while (body.length() < NUMITRON_DIGITS - 1) body = "0" + body;
			value = "-" + body;
		} else {
			String fill = leadingZeroes ? "0" : " ";
			while (value.length() < NUMITRON_DIGITS) value = fill + value;
		}
		return value.length() > NUMITRON_DIGITS ? value.substring(0, NUMITRON_DIGITS) : value;
	}

	private static String numitronShortNumber(long l) {
		long abs = Math.abs(l);
		if (abs < 1_000L)                 return Long.toString(l);
		if (abs < 1_000_000L)             return (l / 1_000L) + "k";
		if (abs < 1_000_000_000L)         return (l / 1_000_000L) + "M";
		if (abs < 1_000_000_000_000L)     return (l / 1_000_000_000_000L) + "G";
		if (abs < 1_000_000_000_000_000L) return (l / 1_000_000_000_000_000L) + "T";
		return (l / 1_000_000_000_000_000L) + "P";
	}

	// ─── Graph (1:1 from RBMKGraphRenderer) ─────────────────────────────────

	private static final int GRAPH_GREEN = 0x00FF00;

	private static void renderGraph(RBMKGraphBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf,
			int light, int overlay) {
		Map<String, List<float[]>> obj = model("numitron");
		TextureAtlasSprite body = panelSprite("numitron");

		ps.pushPose();

		for (int i = 0; i < RBMKGraphBlockEntity.UNITS; i++) {
			if (!be.isUnitActive(i)) continue;

			ps.pushPose();
			ps.translate(0.25, i * -0.5 + 0.25, 0);

			all(ps, buf, obj, body, 1f, 1f, 1f, light, overlay);

			long[] values = be.history[i];
			long lowest  = be.minBound[i] ? be.graphMin[i] : graphMin(values);
			long highest = be.maxBound[i] ? be.graphMax[i] : graphMax(values);
			long range = Math.max(highest - lowest, 1);

			// The original emits GL_LINES vertex pairs; each pair is one segment here.
			for (int v = 0; v < values.length - 1; v++) {
				float[] a = graphPoint(values, v,     lowest, highest, range);
				float[] b = graphPoint(values, v + 1, lowest, highest, range);
				line(ps, buf, a[0], a[1], a[2], b[0], b[1], b[2],
						red(GRAPH_GREEN), green(GRAPH_GREEN), blue(GRAPH_GREEN));
			}

			// Axis bound labels, drawn at the low then high end of the plot area.
			ps.pushPose();
			ps.translate(0.032, 0.5 - 0.03125 * 1.5, -0.375 + 0.03125);
			String lower = Long.toString(lowest);
			String upper = Long.toString(highest);
			labelRaw(ps, buf, lower, 0.0025f, -font().width(lower), GRAPH_GREEN, FULLBRIGHT);
			ps.translate(0, -0.03125 * 7, 0);
			labelRaw(ps, buf, upper, 0.0025f, -font().width(upper), GRAPH_GREEN, FULLBRIGHT);
			ps.popPose();

			ps.translate(0.01, 0.3125, 0);
			label(ps, buf, be.getUnitLabel(i), 0.75f, GRAPH_GREEN, FULLBRIGHT);

			ps.popPose();
		}

		ps.popPose();
	}

	private static float[] graphPoint(long[] values, int k, long lowest, long highest, long range) {
		long flux = Math.max(lowest, Math.min(highest, values[k]));
		float dx = 0.03225f;
		float dy = (float) (0.5 - 0.03125 + (flux - lowest) * 0.1875D / range);
		float dz = (float) (0.375 - k * 0.75 / (values.length - 1));
		return new float[]{dx, dy, dz};
	}

	private static long graphMin(long[] v) {
		long m = Long.MAX_VALUE;
		for (long l : v) m = Math.min(m, l);
		return m == Long.MAX_VALUE ? 0 : m;
	}

	private static long graphMax(long[] v) {
		long m = Long.MIN_VALUE;
		for (long l : v) m = Math.max(m, l);
		return m == Long.MIN_VALUE ? 0 : m;
	}

	// ─── Terminal (1:1 from RBMKTerminalRenderer) ───────────────────────────

	private static final int TERMINAL_LINES = 18;
	private static final int TERMINAL_MAX_WIDTH = 172;
	private static final float TERMINAL_SCALE = 1f / 250f;
	private static final String TERMINAL_PREFIX = "> ";

	private static void renderTerminal(RBMKTerminalBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf,
			int light, int overlay) {
		Map<String, List<float[]>> obj = model("terminal");
		TextureAtlasSprite tex = panelSprite("terminal");

		ps.pushPose();

		ps.translate(0.25, 0, 0);
		all(ps, buf, obj, tex, 1f, 1f, 1f, light, overlay);

		ps.translate(0.0635, 0.125, 0.0625 * 5.5);

		Font font = font();
		int prefixWidth = font.width(TERMINAL_PREFIX);
		int color = be.doesRepeat ? 0xFF8000 : 0x00FF00;

		for (int i = 0; i < TERMINAL_LINES; i++) {
			String source = i == 0 ? "" : be.history[i - 1];
			if (source == null) source = "";

			StringBuilder lineBuilder = new StringBuilder(40);
			if (i == 0 || !source.isEmpty()) lineBuilder.append(TERMINAL_PREFIX);

			int width = prefixWidth;
			for (int j = 0; j < source.length(); j++) {
				char c = source.charAt(j);
				width += font.width(String.valueOf(c));
				if (width > TERMINAL_MAX_WIDTH) break;
				lineBuilder.append(c);
			}

			ps.translate(0, 10 * TERMINAL_SCALE, 0);

			ps.pushPose();
			ps.scale(TERMINAL_SCALE, -TERMINAL_SCALE, TERMINAL_SCALE);
			ps.mulPose(Axis.YP.rotationDegrees(90));
			font.drawInBatch(lineBuilder.toString(), 0, -font.lineHeight / 2f, color, false,
					ps.last().pose(), buf, Font.DisplayMode.NORMAL, 0, FULLBRIGHT);
			ps.popPose();
		}

		ps.popPose();
	}
}
