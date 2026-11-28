package com.hbm_m.wiaj.cannery.rbmk;

import static com.hbm_m.wiaj.cannery.rbmk.RbmkWiajHelpers.*;

import com.hbm_m.wiaj.VirtualWorld;
import com.hbm_m.wiaj.WiajScene;
import com.hbm_m.wiaj.WiajScript;
import com.hbm_m.wiaj.action.ActionCreateActor;
import com.hbm_m.wiaj.action.ActionRemoveActor;
import com.hbm_m.wiaj.action.ActionSetZoom;
import com.hbm_m.wiaj.action.ActionWait;
import com.hbm_m.wiaj.actor.ActorFancyPanel;
import com.hbm_m.wiaj.actor.ActorFancyPanel.Orientation;
import com.hbm_m.wiaj.cannery.CanneryBase;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/**
 * RBMK Column Guide (11 scenes, 1 per column type):
 * Presents every RBMK column individually, explaining what it is and why it is used.
 */
public class CanneryRbmkIntro extends CanneryBase {

	@Override
	public ItemStack getIcon() {
		return new ItemStack(blockForColumn(RbmkColType.MODERATOR));
	}

	@Override
	public String getName() {
		return "cannery.rbmk_intro";
	}

	@Override
	public CanneryBase[] seeAlso() {
		return new CanneryBase[] {
				new CanneryRbmkBasics(),
				new CanneryRbmkStarter(),
				new CanneryRbmkAdvanced()
		};
	}

	@Override
	public WiajScript createScript() {
		VirtualWorld world = new VirtualWorld(9, 8, 9);
		WiajScript script = new WiajScript(world);
		var font = Minecraft.getInstance().font;

		Object[][] columns = {
				{ RbmkColType.FUEL,         2, 0.0, "cannery.rbmk.intro.fuel" },
				{ RbmkColType.FUEL_MOD,     2, 0.0, "cannery.rbmk.intro.fuel_mod" },
				{ RbmkColType.MODERATOR,    1, 0.0, "cannery.rbmk.intro.moderator" },
				{ RbmkColType.REFLECTOR,    1, 0.0, "cannery.rbmk.intro.reflector" },
				{ RbmkColType.ABSORBER,     1, 0.0, "cannery.rbmk.intro.absorber" },
				{ RbmkColType.CONTROL,      0, 0.5, "cannery.rbmk.intro.control" },
				{ RbmkColType.CONTROL_AUTO, 0, 0.5, "cannery.rbmk.intro.control_auto" },
				{ RbmkColType.BOILER,       1, 0.0, "cannery.rbmk.intro.boiler" },
				{ RbmkColType.OUTGASSER,    1, 0.0, "cannery.rbmk.intro.outgasser" },
				{ RbmkColType.COOLER,       1, 0.0, "cannery.rbmk.intro.cooler" },
				{ RbmkColType.BLANK,        1, 0.0, "cannery.rbmk.intro.blank" }
		};

		for (int i = 0; i < columns.length; i++) {
			WiajScene s = new WiajScene(script);
			if (i == 0) {
				s.add(new ActionSetZoom(1.45, 0));
				buildIndustrialFloor(s, 9, 9);
				s.add(new ActionWait(5));
			}

			RbmkColType type = (RbmkColType) columns[i][0];
			int lid = (int) columns[i][1];
			double rodLevel = (double) columns[i][2];
			String key = (String) columns[i][3];

			placeColumn(s, ID_COLUMN_BASE, 4, 1, 4, type, lid, rodLevel, true);
			s.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, -76,
					new Object[][] { { tr(key) } }, 235)
					.setColors(colorBlue).setOrientation(Orientation.BOTTOM)));
			s.add(new ActionWait(65));

			s.add(new ActionRemoveActor(ID_COLUMN_BASE));
			if (i == columns.length - 1) {
				s.add(new ActionRemoveActor(ID_PANEL_MAIN));
			}
			script.addScene(s);
		}

		return script;
	}
}
