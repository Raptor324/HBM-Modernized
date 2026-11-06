package com.hbm_m.wiaj;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.hbm_m.client.GuiCompat;
import com.hbm_m.client.render.ClientRenderFlags;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.wiaj.actor.ActorBasicPanel;
import com.hbm_m.wiaj.actor.ActorFancyPanel;
import com.hbm_m.wiaj.actor.ActorFancyPanel.Orientation;
import com.hbm_m.wiaj.actor.ISpecialActor;
import com.hbm_m.wiaj.cannery.CanneryBase;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

//krass
/**
 * The WIAJ demo screen: renders a miniature isometric world with a scripted timeline.
 * <p>
 * Port of {@code com.hbm.wiaj.GuiWorldInAJar} (1.7.10). Blocks draw through
 * {@code BlockRenderDispatcher#renderSingleBlock}, machine actors through their registered
 * Nucleus {@code MachineBer} (per-BE VBO tier, see {@link ClientRenderFlags#guiRenderFrame()}).
 */
public class GuiWorldInAJar extends Screen {

	private static final ResourceLocation guiUtil = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/gui_utility.png");

	WiajScript jarScript;
	ItemStack icon;
	ActorFancyPanel titlePanel;
	CanneryBase[] seeAlso;
	ActorFancyPanel[] seeAlsoTitles;
	private final net.minecraft.util.RandomSource rand = net.minecraft.util.RandomSource.create();

	public GuiWorldInAJar(WiajScript script, String title, ItemStack icon, CanneryBase... seeAlso) {
		super(Component.literal(title));
		this.font = Minecraft.getInstance().font;

		this.jarScript = script;
		this.icon = icon;
		this.seeAlso = seeAlso;

		this.titlePanel = new ActorFancyPanel(this.font, 40, 27, new Object[][] {{title}}, 0).setColors(CanneryBase.colorGold).setOrientation(Orientation.LEFT);
		this.seeAlsoTitles = new ActorFancyPanel[seeAlso.length];

		for(int i = 0; i < seeAlso.length; i++) {
			this.seeAlsoTitles[i] = new ActorFancyPanel(this.font, 40, 27 + 36 * (i + 1), new Object[][] {{net.minecraft.client.resources.language.I18n.get(seeAlso[i].getName())}}, 0).setColors(CanneryBase.colorGrey).setOrientation(Orientation.LEFT);
		}
	}

	@Override
	public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
		GuiCompat.renderBackground(this, gui, mouseX, mouseY, partialTick);

		// the demo-safe render gate covers BOTH layers: the background layer draws the
		// scene, and any future 3D content in the foreground layer must also take the
		// demo-safe paths instead of the level-pass culling/fade/instanced ones
		ClientRenderFlags.setGuiRenderFrame(true);
		try {
			if(jarScript != null) {
				jarScript.run();
			}
			this.drawGuiContainerBackgroundLayer(gui, partialTick, mouseX, mouseY);
			this.drawGuiContainerForegroundLayer(gui, mouseX, mouseY);
		} catch(Exception ex) {
			ex.printStackTrace();
			for(StackTraceElement line : ex.getStackTrace()) {
				if(this.minecraft != null && this.minecraft.player != null)
					this.minecraft.player.displayClientMessage(Component.literal(line.toString()).withStyle(ChatFormatting.RED), false);
			}

			if(this.minecraft != null) {
				this.minecraft.setScreen(null);
			}
		} finally {
			ClientRenderFlags.setGuiRenderFrame(false);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {

		if(width / 2 - 12 <= mouseX && width / 2 - 12 + 24 > mouseX && height - 36 < mouseY && height - 36 + 24 >= mouseY) {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

			if(this.jarScript.isPaused()) {
				this.jarScript.unpause();
			} else {
				this.jarScript.pause();
			}
		}

		if(width / 2 - 12 - 36 <= mouseX && width / 2 - 12 - 36 + 24 > mouseX && height - 36 < mouseY && height - 36 + 24 >= mouseY) {

			if(this.jarScript.sceneNumber > 0) {
				this.jarScript.rewindOne();
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			}
		}

		if(width / 2 - 12 + 36 <= mouseX && width / 2 - 12 + 36 + 24 > mouseX && height - 36 < mouseY && height - 36 + 24 >= mouseY) {

			if(this.jarScript.sceneNumber < this.jarScript.scenes.size()) {
				this.jarScript.forwardOne();
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			}
		}

		for(int i = 0; i < seeAlso.length; i++) {

			if(15 <= mouseX && 39 > mouseX && 15 + 36 * (i + 1) < mouseY && 39 + 36 * (i + 1) >= mouseY) {
				CanneryBase cannery = seeAlso[i];
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				minecraft.setScreen(new GuiWorldInAJar(cannery.createScript(), Component.translatable(cannery.getName()).getString(), cannery.getIcon(), cannery.seeAlso()));
				return true;
			}
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	private void drawGuiContainerForegroundLayer(GuiGraphics gui, int mouseX, int mouseY) {

		for(Entry<Integer, ISpecialActor> actor : this.jarScript.actors.entrySet()) {
			gui.pose().pushPose();
			actor.getValue().drawForegroundComponent(gui, this.width, this.height, this.jarScript.ticksElapsed, this.jarScript.interp);
			gui.pose().popPose();
		}

		if(Screen.hasAltDown()) {
			List<Object[]> list = new ArrayList<>();
			list.add(new Object[] { (mouseX - width / 2) + " / " + (mouseY - height / 2) });
			ActorBasicPanel.drawStackText(gui, list, mouseX - width / 2, mouseY - height / 2, this.font, this.width, this.height);
		}

		int playButton = this.jarScript.isPaused() ? 64 : 40;

		if(width / 2 - 12 <= mouseX && width / 2 - 12 + 24 > mouseX && height - 36 < mouseY && height - 36 + 24 >= mouseY)
			gui.blit(guiUtil, width / 2 - 12, height - 36, playButton, 24, 24, 24);
		else
			gui.blit(guiUtil, width / 2 - 12, height - 36, playButton, 48, 24, 24);

		if(this.jarScript.sceneNumber == 0)
			gui.blit(guiUtil, width / 2 - 12 - 36, height - 36, 88, 72, 24, 24);
		else if(width / 2 - 12 - 36 <= mouseX && width / 2 - 12 - 36 + 24 > mouseX && height - 36 < mouseY && height - 36 + 24 >= mouseY)
			gui.blit(guiUtil, width / 2 - 12 - 36, height - 36, 88, 24, 24, 24);
		else
			gui.blit(guiUtil, width / 2 - 12 - 36, height - 36, 88, 48, 24, 24);

		if(this.jarScript.sceneNumber >= this.jarScript.scenes.size())
			gui.blit(guiUtil, width / 2 - 12 + 36, height - 36, 112, 72, 24, 24);
		else if(width / 2 - 12 + 36 <= mouseX && width / 2 - 12 + 36 + 24 > mouseX && height - 36 < mouseY && height - 36 + 24 >= mouseY)
			gui.blit(guiUtil, width / 2 - 12 + 36, height - 36, 112, 24, 24, 24);
		else
			gui.blit(guiUtil, width / 2 - 12 + 36, height - 36, 112, 48, 24, 24);

		gui.blit(guiUtil, 15, 15, 136, 48, 24, 24);
		gui.renderItem(this.icon, 19, 19);
		gui.renderItemDecorations(this.font, this.icon, 19, 19);

		if(15 <= mouseX && 39 > mouseX && 15 < mouseY && 39 >= mouseY) {
			this.titlePanel.drawForegroundComponent(gui, 0, 0, this.jarScript.ticksElapsed, this.jarScript.interp);
		}

		for(int i = 0; i < seeAlso.length; i++) {
			CanneryBase also = seeAlso[i];

			gui.blit(guiUtil, 15, 15 + 36 * (i + 1), 136, 72, 24, 24);
			gui.renderItem(also.getIcon(), 19, 19 + 36 * (i + 1));
			gui.renderItemDecorations(this.font, also.getIcon(), 19, 19 + 36 * (i + 1));

			if(15 <= mouseX && 39 > mouseX && 15 + 36 * (i + 1) < mouseY && 39 + 36 * (i + 1) >= mouseY) {
				this.seeAlsoTitles[i].drawForegroundComponent(gui, 0, 0, this.jarScript.ticksElapsed, this.jarScript.interp);
			}
		}
	}

	private void drawGuiContainerBackgroundLayer(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {

		gui.flush();

		PoseStack pose = gui.pose();
		pose.pushPose();
		setupRotation(pose);

		//blocks - tesselated against the virtual world like the original RenderBlocks with
		//enableAO = true. The tesselator samples AO/light from the world at the BlockPos but
		//emits MODEL-SPACE vertices (the vanilla chunk renderer does the per-block pose
		//translate itself), so the pose is translated to each block here. The RenderType is
		//entityCutoutNoCull over the BLOCK atlas: the chunk render types cull back faces at
		//flush time, and the isometric transform has a negative determinant - it flips the
		//projected winding, so culling would hide the outward faces (x-ray blocks).
		for(int x = 0; x < jarScript.world.sizeX; x++) {
			for(int y = 0; y < jarScript.world.sizeY; y++) {
				for(int z = 0; z < jarScript.world.sizeZ; z++) {
					BlockState state = jarScript.world.getBlock(x, y, z);
					if(state.isAir()) continue;
					BlockPos pos = new BlockPos(x, y, z);
					pose.pushPose();
					if(!state.getFluidState().isEmpty()) {
						//fluids: the vanilla liquid renderer (the direct descendant of 1.7.10
						//RenderBlocks' fluid path), into a no-cull translucent type - see
						//WiajFluidRenderType below. LiquidBlockRenderer emits SECTION-RELATIVE
						//vertices through the bare vertex(x,y,z) overload ((pos & 15) + local) and
						//renderLiquid has no PoseStack parameter, so the pose must be baked in at
						//write time - exactly what VertexConsumer.putBulkData does for the block
						//pass (see PoseApplyingVertexConsumer below). The pose is translated to the
						//section origin (x & ~15 etc.) so the &15-relative coords land on the world
						//block: 0 offset for every current jar scene (all axes < 16), and a future
						//jar world placing water at a coordinate >= 16 stays correct too.
						pose.translate(x & ~15, y & ~15, z & ~15);
						pose.last().normal().identity();
						this.minecraft.getBlockRenderer().renderLiquid(pos, jarScript.world,
								new PoseApplyingVertexConsumer(gui.bufferSource().getBuffer(WiajFluidRenderType.WIAJ_FLUID), pose.last()),
								state, state.getFluidState());
					} else {
						pose.translate(x, y, z);
						this.rand.setSeed(state.getSeed(pos));
						this.minecraft.getBlockRenderer().getModelRenderer().tesselateWithAO(
								jarScript.world, this.minecraft.getBlockRenderer().getBlockModel(state), state, pos, pose,
								gui.bufferSource().getBuffer(WiajBlockRenderType.WIAJ_BLOCK), true, this.rand,
								state.getSeed(pos), net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
					}
					pose.popPose();
				}
			}
		}

		gui.flush();
		com.mojang.blaze3d.platform.Lighting.setupFor3DItems();

		//actors
		for(Entry<Integer, ISpecialActor> actor : this.jarScript.actors.entrySet()) {
			pose.pushPose();
			actor.getValue().drawBackgroundComponent(gui, this.jarScript.world, this.jarScript.ticksElapsed, this.jarScript.interp);
			pose.popPose();
		}
		gui.flush();

		pose.popPose();
		gui.flush();

		//The scene pass wrote DEPTH into the GUI depth buffer (blocks, machine VBOs and the
		//depth-tested beam all sit at pose z ~ 200-600, closer than the z=0 foreground quads).
		//RenderType.gui() / text are LEQUAL, so every later panel/button/text quad would be
		//depth-rejected over scene pixels. Vanilla itself clears depth between GUI sections
		//(GameRenderer does RenderSystem.clear(GL_DEPTH_BUFFER_BIT) around the GUI pass) - do
		//the same here so the whole foreground layer draws over the scene, like the original
		//(1.7.10 drew its foreground with GL_DEPTH_TEST disabled).
		com.mojang.blaze3d.systems.RenderSystem.clear(org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
	}

	private void setupRotation(PoseStack pose) {

		double scale = -10;

		pose.translate(width / 2F, height / 2F, 400F);
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.scale(1F, 1F, 0.5F); //incredible flattening power

		double zoom = jarScript.zoom();
		pose.scale((float) zoom, (float) zoom, (float) zoom);

		pose.mulPose(Axis.XP.rotationDegrees((float) jarScript.pitch()));
		pose.mulPose(Axis.YP.rotationDegrees((float) jarScript.yaw()));
		pose.translate(jarScript.world.sizeX / -2F, -jarScript.world.sizeY / 2F, jarScript.world.sizeZ / -2F);
		pose.translate((float) jarScript.offsetX(), (float) jarScript.offsetY(), (float) jarScript.offsetZ());
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private static final class WiajBlockRenderType extends net.minecraft.client.renderer.RenderStateShard {

		private WiajBlockRenderType() { super("wiaj_block", () -> {}, () -> {}); }

		static final net.minecraft.client.renderer.RenderType WIAJ_BLOCK = net.minecraft.client.renderer.RenderType.create(
				"wiaj_block",
				com.mojang.blaze3d.vertex.DefaultVertexFormat.BLOCK,
				com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
				2097152, true, false,
				net.minecraft.client.renderer.RenderType.CompositeState.builder()
						.setLightmapState(LIGHTMAP)
						.setShaderState(RENDERTYPE_CUTOUT_MIPPED_SHADER)
						.setTextureState(BLOCK_SHEET_MIPPED)
						.setCullState(NO_CULL)
						.createCompositeState(true));
	}

	/**
	 * Jar-fluid render type: a twin of vanilla {@code RenderType.translucent()} with NO_CULL,
	 * same states (translucent shader, mipped block sheet, translucent transparency, lightmap,
	 * translucent target). The isometric transform has a negative determinant - it flips the
	 * projected winding (see the block-pass comment above) - so the culling translucent() would
	 * hide the outward water faces. 1.7.10 drew the same quads unculled through immediate-mode
	 * GL state, so the missing no-cull translucent type is a critical-API-gap glue: the
	 * RenderStateShard states are protected static, hence the holder extends it.
	 */
	private static final class WiajFluidRenderType extends net.minecraft.client.renderer.RenderStateShard {

		private WiajFluidRenderType() { super("wiaj_fluid", () -> {}, () -> {}); }

		static final net.minecraft.client.renderer.RenderType WIAJ_FLUID = net.minecraft.client.renderer.RenderType.create(
				"wiaj_fluid",
				com.mojang.blaze3d.vertex.DefaultVertexFormat.BLOCK,
				com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
				2097152, true, true,
				net.minecraft.client.renderer.RenderType.CompositeState.builder()
						.setLightmapState(LIGHTMAP)
						.setShaderState(RENDERTYPE_TRANSLUCENT_SHADER)
						.setTextureState(BLOCK_SHEET_MIPPED)
						.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
						.setOutputState(TRANSLUCENT_TARGET)
						.setCullState(NO_CULL)
						.createCompositeState(true));
	}

	/**
	 * Critical-API-gap glue: {@code BlockRenderDispatcher#renderLiquid} takes no PoseStack, but the
	 * vanilla fluid renderer emits raw section-relative vertices through the bare
	 * {@code vertex(double,double,double)} and {@code normal(float,float,float)} overloads, while
	 * this GUI renders into a buffered batch whose model-view matrix is identity (+GUI z-offset) at
	 * flush time. This delegate applies the pose at write time - the same matrix4f/matrix3f
	 * transform {@code VertexConsumer.putBulkData} applies for the block pass - so the fluid lands
	 * in the isometric scene next to the pose-baked blocks. 1.7.10's RenderBlocks emitted through
	 * immediate mode under the live GL matrix, which buffered rendering cannot reproduce any other
	 * way; nothing else changes (tint, light, uv, overlay calls are forwarded verbatim).
	 */
	private record PoseApplyingVertexConsumer(VertexConsumer delegate, Matrix4f pose, Matrix3f normal) implements VertexConsumer {

		PoseApplyingVertexConsumer(VertexConsumer delegate, PoseStack.Pose pose) {
			this(delegate, pose.pose(), pose.normal());
		}

		private org.joml.Vector3f transformPos(double x, double y, double z) {
			return this.pose.transformPosition((float)x, (float)y, (float)z, new org.joml.Vector3f());
		}

		private org.joml.Vector3f transformNormal(float x, float y, float z) {
			return this.normal.transform(new org.joml.Vector3f(x, y, z));
		}

		//the VertexConsumer API was renamed between targets (vertex/normal/... -> addVertex/setNormal/...)
		//- only the position and normal writers need transforming, everything else forwards verbatim
		//? if < 1.21.1 {
		@Override
		public VertexConsumer vertex(double x, double y, double z) {
			org.joml.Vector3f v = this.transformPos(x, y, z);
			return this.delegate.vertex(v.x(), v.y(), v.z());
		}

		@Override
		public VertexConsumer normal(float x, float y, float z) {
			org.joml.Vector3f v = this.transformNormal(x, y, z);
			return this.delegate.normal(v.x(), v.y(), v.z());
		}

		@Override
		public VertexConsumer color(int red, int green, int blue, int alpha) { return this.delegate.color(red, green, blue, alpha); }

		@Override
		public VertexConsumer uv(float u, float v) { return this.delegate.uv(u, v); }

		@Override
		public VertexConsumer overlayCoords(int u, int v) { return this.delegate.overlayCoords(u, v); }

		@Override
		public VertexConsumer uv2(int u, int v) { return this.delegate.uv2(u, v); }

		@Override
		public void endVertex() { this.delegate.endVertex(); }

		@Override
		public void defaultColor(int red, int green, int blue, int alpha) { this.delegate.defaultColor(red, green, blue, alpha); }

		@Override
		public void unsetDefaultColor() { this.delegate.unsetDefaultColor(); }
		//?} else {
		/*@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			org.joml.Vector3f v = this.transformPos(x, y, z);
			return this.delegate.addVertex(v.x(), v.y(), v.z());
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			org.joml.Vector3f v = this.transformNormal(x, y, z);
			return this.delegate.setNormal(v.x(), v.y(), v.z());
		}

		@Override
		public VertexConsumer setColor(int red, int green, int blue, int alpha) { return this.delegate.setColor(red, green, blue, alpha); }

		@Override
		public VertexConsumer setUv(float u, float v) { return this.delegate.setUv(u, v); }

		@Override
		public VertexConsumer setUv1(int u, int v) { return this.delegate.setUv1(u, v); }

		@Override
		public VertexConsumer setUv2(int u, int v) { return this.delegate.setUv2(u, v); }
		*///?}
	}
}
