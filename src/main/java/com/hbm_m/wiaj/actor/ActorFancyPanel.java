package com.hbm_m.wiaj.actor;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.wiaj.VirtualWorld;
import com.hbm_m.wiaj.WiajScene;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Styled screen-space tooltip panel anchored to an offset from the screen center, with a
 * beveled frame, a palette-colored arrow and mixed text/item lines.
 * <p>
 * Port of {@code com.hbm.wiaj.actors.ActorFancyPanel} (1.7.10). The duplicated 1.7.10
 * tooltip-drawing helpers ({@code drawRect}/{@code drawGradientRect}/{@code drawTexturedModalRect})
 * collapse into {@link GuiGraphics} primitives.
 */
public class ActorFancyPanel implements ISpecialActor {

	static final ResourceLocation guiUtil = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/gui_utility.png");

	List<Object[]> lines = new ArrayList<>();
	Font font;

	int x;
	int y;

	boolean consistentHeight = false;
	int lineDist = 2;
	int tallestElement = 0;
	static final int STACK_HEIGHT = 18;

	Orientation o = Orientation.CENTER;
	int colorBrighter = 0xFFCCCCCC;
	int colorDarker = 0xFF7D7D7D;
	int colorFrame = 0xFFA0A0A0;
	int colorBg = 0xFF302E36;

	public ActorFancyPanel(Font font, int x, int y, Object[][] raw, int autowrap) {

		this.font = font;
		this.x = x;
		this.y = y;

		if(autowrap <= 0) { //if autowrap is off, just add the lines 1:1
			for(Object[] o : raw) {
				lines.add(o);
			}
		} else {

			for(Object[] line : raw) {

				if(line.length == 1 && line[0] instanceof String) { //auto wrap can only apply to text-only lines
					List<String> frags = autoBreak(font, (String) line[0], autowrap);
					for(String s : frags) {
						lines.add(new Object[] { s });
					}
				} else {
					lines.add(line);
				}
			}
		}
	}

	/** Port of {@code I18nUtil.autoBreak} (1.7.10): greedy word wrap on the space character. */
	public static List<String> autoBreak(Font font, String text, int width) {
		List<String> lines = new ArrayList<>();
		String[] words = text.split(" ");
		StringBuilder current = new StringBuilder();

		for(String word : words) {
			if(current.length() == 0) {
				current.append(word);
			} else if(font.width(current + " " + word) <= width) {
				current.append(" ").append(word);
			} else {
				lines.add(current.toString());
				current = new StringBuilder(word);
			}
		}

		// the original always yields at least one (possibly empty) line
		if(current.length() > 0 || lines.isEmpty()) lines.add(current.toString());
		return lines;
	}

	public ActorFancyPanel enforceConsistentHeight() {
		this.consistentHeight = true;
		return this;
	}

	public ActorFancyPanel setLineDist(int dist) {
		this.lineDist = dist;
		return this;
	}

	public ActorFancyPanel setOrientation(Orientation o) {
		this.o = o;
		return this;
	}

	public ActorFancyPanel setColors(int brighter, int frame, int darker, int background) {
		this.colorBrighter = brighter;
		this.colorFrame = frame;
		this.colorDarker = darker;
		this.colorBg = background;
		return this;
	}

	public ActorFancyPanel setColors(int[] colors) {
		return setColors(colors[0], colors[1], colors[2], colors[3]);
	}

	public int getTallestElement() {
		if(this.tallestElement > 0) {
			return this.tallestElement;
		}

		for(Object[] line : this.lines) {
			for(Object element : line) {

				int height = getElementHeight(element);

				if(height > this.tallestElement) {
					this.tallestElement = height;
				}
			}
		}

		return this.tallestElement;
	}

	public int getElementHeight(Object element) {

		if(element instanceof String) {
			return this.font.lineHeight;
		}

		if(element instanceof ItemStack) {
			return STACK_HEIGHT;
		}

		if(element instanceof Object[]) {
			Object[] scaledStack = (Object[]) element; //[0] ItemStack, [1] double
			return (int) Math.ceil(STACK_HEIGHT * (double) scaledStack[1]);
		}

		return 0;
	}

	public int getElementWidth(Object element) {

		if(element instanceof String) {
			return this.font.width((String) element);
		}

		if(element instanceof ItemStack) {
			return STACK_HEIGHT;
		}

		if(element instanceof Object[]) {
			Object[] scaledStack = (Object[]) element; //[0] ItemStack, [1] double
			return (int) Math.ceil(STACK_HEIGHT * (double) scaledStack[1]);
		}

		return 0;
	}

	int blockHeight = 0;
	private int getBlockHeight() {

		if(this.blockHeight > 0) {
			return this.blockHeight;
		}

		for(Object[] line : this.lines) {

			if(this.blockHeight > 0) {
				this.blockHeight += this.lineDist;
			}

			int lineHeight = this.font.lineHeight;

			if(this.consistentHeight) {
				lineHeight = Math.max(lineHeight, getTallestElement());
			} else {

				for(Object o : line) {
					lineHeight = Math.max(lineHeight, getElementHeight(o));
				}
			}

			this.blockHeight += lineHeight;
		}

		return this.blockHeight;
	}

	int blockWidth = 0;
	private int getBlockWidth() {

		if(this.blockWidth > 0) {
			return this.blockWidth;
		}

		for(Object[] line : this.lines) {

			int lineWidth = 0;

			for(Object o : line) {

				if(lineWidth > 0) {
					lineWidth += 2;
				}

				lineWidth += getElementWidth(o);
			}

			if(lineWidth > this.blockWidth) {
				this.blockWidth = lineWidth;
			}
		}

		return this.blockWidth;
	}

	@Override
	public void drawForegroundComponent(GuiGraphics gui, int w, int h, int ticks, float interp) {

		int height = this.getBlockHeight();
		int width = this.getBlockWidth();

		int posX = w / 2 + x;
		int posY = h / 2 + y;

		switch(o) {
		case TOP:
			posX -= width / 2;
			posY += 15;
			break;
		case BOTTOM:
			posX -= width / 2;
			posY -= height + 15;
			break;
		case LEFT:
			posX += 15;
			posY -= height / 2;
			break;
		case RIGHT:
			posX -= width + 15;
			posY -= height / 2;
			break;
		case CENTER:
			posX -= width / 2;
			posY -= height / 2;
			break;
		}

		gui.fill(posX - 5, posY - 5, posX + width + 5, posY + height + 5, colorFrame);

		gui.fill(posX - 5, posY - 5, posX - 4, posY + height + 4, colorBrighter);
		gui.fill(posX - 5, posY - 5, posX + width + 4, posY - 4, colorBrighter);
		gui.fill(posX + width + 2, posY - 2, posX + width + 3, posY + height + 3, colorBrighter);
		gui.fill(posX - 2, posY + height + 2, posX + width + 3, posY + height + 3, colorBrighter);

		gui.fill(posX - 3, posY - 3, posX - 2, posY + height + 2, colorDarker);
		gui.fill(posX - 3, posY - 3, posX + width + 2, posY - 2, colorDarker);
		gui.fill(posX + width + 4, posY - 4, posX + width + 5, posY + height + 5, colorDarker);
		gui.fill(posX - 4, posY + height + 4, posX + width + 5, posY + height + 5, colorDarker);

		gui.fill(posX - 2, posY - 2, posX + width + 2, posY + height + 2, colorBg);

		switch(o) {
		case TOP:
			this.drawArrow(gui, posX + width / 2 - 7, posY - 15, 40, 14, 14, 10);
			break;
		case BOTTOM:
			this.drawArrow(gui, posX + width / 2 - 7, posY + height + 5, 54, 14, 14, 10);
			break;
		case LEFT:
			this.drawArrow(gui, posX - 15, posY + height / 2 - 7, 40, 0, 10, 14);
			break;
		case RIGHT:
			this.drawArrow(gui, posX + width + 5, posY + height / 2 - 7, 50, 0, 10, 14);
			break;
		case CENTER: break;
		}

		int offsetY = 0;

		for(Object[] line : this.lines) {

			if(offsetY > 0) offsetY += this.lineDist;
			int lineHeight = 0;
			for(Object element : line) lineHeight = Math.max(lineHeight, this.getElementHeight(element));

			int indent = 0;
			for(Object element : line) {

				if(indent > 0) {
					indent += 2;
				}

				drawElement(gui, posX + indent, posY + offsetY + lineHeight / 2, element);
				indent += getElementWidth(element);
			}

			offsetY += lineHeight;
		}
	}

	private void drawArrow(GuiGraphics gui, int posX, int posY, int sourceX, int sourceY, int sizeX, int sizeY) {
		this.drawTintedBlit(gui, posX, posY, sourceX + 28 * 0, sourceY, sizeX, sizeY, this.colorBrighter);
		this.drawTintedBlit(gui, posX, posY, sourceX + 28 * 1, sourceY, sizeX, sizeY, this.colorFrame);
		this.drawTintedBlit(gui, posX, posY, sourceX + 28 * 2, sourceY, sizeX, sizeY, this.colorDarker);
		this.drawTintedBlit(gui, posX, posY, sourceX + 28 * 3, sourceY, sizeX, sizeY, this.colorBg);
	}

	/** Tinted blit: GuiGraphics batches vertices, so the shader color must be flushed between passes. */
	private void drawTintedBlit(GuiGraphics gui, int posX, int posY, int u, int v, int sizeX, int sizeY, int color) {
		float a = (float) (color >> 24 & 255) / 255.0F;
		float r = (float) (color >> 16 & 255) / 255.0F;
		float g = (float) (color >> 8 & 255) / 255.0F;
		float b = (float) (color & 255) / 255.0F;

		gui.flush();
		com.mojang.blaze3d.systems.RenderSystem.setShaderColor(r, g, b, a);
		gui.blit(guiUtil, posX, posY, u, v, sizeX, sizeY, 256, 256);
		gui.flush();
		com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
	}

	private void drawElement(GuiGraphics gui, int x, int y, Object element) {

		if(element instanceof String) {
			String text = (String) element;
			// the original drew captions without a drop shadow (FontRenderer.drawString, not drawStringWithShadow)
			gui.drawString(this.font, text, x, y - this.font.lineHeight / 2, 0xFFFFFFFF, false);

		} else if(element instanceof ItemStack) {

			ItemStack stack = (ItemStack) element;

			if(stack.getCount() == 0) {
				gui.fill(x - 1, y - 1 - 8, x + 17, y + 17, 0xffff0000);
				gui.fill(x, y - 8, x + 16, y + 16, 0xffb0b0b0);
			}
			gui.renderItem(stack, x, y - 8);
			gui.renderItemDecorations(this.font, stack, x, y - 8);
			// drain the item buffer so a following text line cannot be painted under it
			gui.flush();
		} else if(element instanceof Object[]) { //scaled stack: [0] ItemStack, [1] double
			Object[] scaledStack = (Object[]) element;
			ItemStack stack = (ItemStack) scaledStack[0];
			double scale = (Double) scaledStack[1];
			//anchor so the scaled item stays vertically centered on y, like the unscaled branch
			gui.pose().pushPose();
			gui.pose().translate(x, y - 8 * scale, 0);
			gui.pose().scale((float) scale, (float) scale, (float) scale);
			if(stack.getCount() == 0) {
				gui.fill(-1, -1, 17, 17, 0xffff0000);
				gui.fill(0, 0, 16, 16, 0xffb0b0b0);
			}
			gui.renderItem(stack, 0, 0);
			gui.renderItemDecorations(this.font, stack, 0, 0);
			gui.flush();
			gui.pose().popPose();
		}
	}

	@Override public void drawBackgroundComponent(GuiGraphics gui, VirtualWorld world, int ticks, float interp) { }
	@Override public void updateActor(WiajScene scene) { }
	@Override public void setActorData(CompoundTag data) { }
	@Override public void setDataPoint(String tag, Object o) { }

	/** where the arrow should be or if the box should be centered around the home position */
	public static enum Orientation {
		TOP,
		BOTTOM,
		LEFT,
		RIGHT,
		CENTER
	}
}
