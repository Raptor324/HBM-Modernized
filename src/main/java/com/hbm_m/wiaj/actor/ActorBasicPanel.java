package com.hbm_m.wiaj.actor;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.wiaj.VirtualWorld;
import com.hbm_m.wiaj.WiajScene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * Plain vanilla-look tooltip (no frame/arrow/orientation) with mixed text/item lines.
 * <p>
 * Port of {@code com.hbm.wiaj.actors.ActorBasicPanel} (1.7.10).
 */
public class ActorBasicPanel implements ISpecialActor {

	int x;
	int y;
	List<Object[]> lines;

	public ActorBasicPanel(int x, int y, Object[]... objects) {
		this.x = x;
		this.y = y;
		this.lines = new ArrayList<>();

		for(Object[] o : objects) {
			this.lines.add(o);
		}
	}

	@Override
	public void drawForegroundComponent(GuiGraphics gui, int w, int h, int ticks, float interp) {
		drawStackText(gui, lines, x, y, Minecraft.getInstance().font, w, h);
	}

	@Override
	public void drawBackgroundComponent(GuiGraphics gui, VirtualWorld world, int ticks, float interp) { }

	@Override
	public void updateActor(WiajScene scene) { }

	@Override
	public void setActorData(CompoundTag data) { }

	@Override
	public void setDataPoint(String tag, Object o) { }

	/** Shared vanilla-style stack tooltip, also used by the screen's ALT debug tooltip. */
	public static void drawStackText(GuiGraphics gui, List<Object[]> lines, int x, int y, net.minecraft.client.gui.Font font, int w, int h) {

		x += w / 2;
		y += h / 2;

		if(!lines.isEmpty()) {
			int height = 0;
			int longestline = 0;

			for(Object[] line : lines) {
				int lineWidth = 0;

				boolean hasStack = false;

				for(Object o : line) {

					if(o instanceof String) {
						lineWidth += font.width((String) o);
					} else {
						lineWidth += 18;
						hasStack = true;
					}
				}

				if(hasStack) {
					height += 18;
				} else {
					height += 10;
				}

				if(lineWidth > longestline) {
					longestline = lineWidth;
				}
			}

			int minX = x + 12;
			int minY = y - 12;

			if(minX + longestline > w) {
				minX -= 28 + longestline;
			}

			if(minY + height + 6 > h) {
				minY = h - height - 6;
			}

			gui.pose().pushPose();
			gui.pose().translate(0, 0, 300);

			int colorBg = 0xF0100010;
			gui.fill(minX - 3, minY - 4, minX + longestline + 3, minY - 3, colorBg);
			gui.fill(minX - 3, minY + height + 3, minX + longestline + 3, minY + height + 4, colorBg);
			gui.fill(minX - 3, minY - 3, minX + longestline + 3, minY + height + 3, colorBg);
			gui.fill(minX - 4, minY - 3, minX - 3, minY + height + 3, colorBg);
			gui.fill(minX + longestline + 3, minY - 3, minX + longestline + 4, minY + height + 3, colorBg);

			int color0 = 0x505000FF;
			int color1 = (color0 & 0xFEFEFE) >> 1 | color0 & 0xFF000000;
			gui.fillGradient(minX - 3, minY - 3 + 1, minX - 3 + 1, minY + height + 3 - 1, color0, color1);
			gui.fillGradient(minX + longestline + 2, minY - 3 + 1, minX + longestline + 3, minY + height + 3 - 1, color0, color1);
			gui.fill(minX - 3, minY - 3, minX + longestline + 3, minY - 3 + 1, color0);
			gui.fill(minX - 3, minY + height + 2, minX + longestline + 3, minY + height + 3, color1);

			for(int index = 0; index < lines.size(); ++index) {

				Object[] line = lines.get(index);
				int indent = 0;
				boolean hasStack = false;

				for(Object o : line) {
					if(!(o instanceof String)) {
						hasStack = true;
					}
				}

				for(Object o : line) {

					if(o instanceof String) {
						gui.drawString(font, (String) o, minX + indent, minY + (hasStack ? 4 : 0), -1, true);
						indent += font.width((String) o) + 2;
					} else {
						ItemStack stack = (ItemStack) o;

						if(stack.getCount() == 0) {
							gui.fill(minX + indent - 1, minY - 1, minX + indent + 17, minY + 17, 0xffff0000);
							gui.fill(minX + indent, minY, minX + indent + 16, minY + 16, 0xffb0b0b0);
						}
						gui.renderItem(stack, minX + indent, minY);
						gui.renderItemDecorations(font, stack, minX + indent, minY);
						gui.flush();
						indent += 18;
					}
				}

				if(index == 0) {
					minY += 2;
				}

				minY += hasStack ? 18 : 10;
			}

			gui.pose().popPose();
		}
	}
}
