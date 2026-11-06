package com.hbm_m.wiaj.cannery;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.PartTabMetaItems;
import com.hbm_m.wiaj.VirtualWorld;
import com.hbm_m.wiaj.WiajScene;
import com.hbm_m.wiaj.WiajScript;
import com.hbm_m.wiaj.action.ActionCreateActor;
import com.hbm_m.wiaj.action.ActionRemoveActor;
import com.hbm_m.wiaj.action.ActionSetBlock;
import com.hbm_m.wiaj.action.ActionSetZoom;
import com.hbm_m.wiaj.action.ActionWait;
import com.hbm_m.wiaj.actor.ActorFancyPanel;
import com.hbm_m.wiaj.actor.ActorFancyPanel.Orientation;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Mustard willow growing demo.
 * <p>
 * Port of {@code com.hbm.wiaj.cannery.CanneryWillow} (1.7.10), scene structure, action order,
 * timings, camera moves and palettes kept 1:1.
 * <p>
 * Mechanism changes mandated by the modern engine:
 * <ul>
 * <li>The original single {@code plant_flower}/{@code plant_tall} blocks with
 * {@code EnumFlowerType}/{@code EnumTallFlower} metadata become separate registered blocks
 * ({@code plant_flower_cd0/cd1}, {@code plant_tall_cd2/cd3/cd4}); the original upper-half
 * meta {@code ordinal() + 8} becomes the {@link DoubleBlockHalf} property, and both halves
 * are set explicitly because {@link VirtualWorld} applies no placement side-effects.</li>
 * <li>The original {@code DictFrame.fromOne(ModItems.plant_item, EnumPlantType.MUSTARDWILLOW)}
 * icon and recipe line map to the registered {@code plant_item_mustardwillow} item
 * ({@code PartTabMetaItems}) - the same standalone item {@code Jars} uses as this scene's
 * trigger, replacing the original DictFrame meta lookup.</li>
 * </ul>
 */
public class CanneryWillow extends CanneryBase {

	@Override
	public ItemStack getIcon() {
		// Original: DictFrame.fromOne(ModItems.plant_item, EnumPlantType.MUSTARDWILLOW) - the
		// modern analog is the standalone plant_item_mustardwillow item (same item Jars maps
		// this scene's trigger to), "Mustard Willow Leaf".
		return new ItemStack(PartTabMetaItems.get("plant_item_mustardwillow").get());
	}

	@Override
	public String getName() {
		return "cannery.willow";
	}

	public WiajScript createScript() {
		VirtualWorld world = new VirtualWorld(5, 5, 5);
		WiajScript script = new WiajScript(world);

		WiajScene scene0 = new WiajScene(script);
		scene0.add(new ActionSetZoom(3, 0));

		for(int x = world.sizeX - 1; x >= 0; x--) {
			for(int z = 0; z < world.sizeZ; z++) {
				scene0.add(new ActionSetBlock(x, 1, z, net.minecraft.world.level.block.Blocks.GRASS_BLOCK));
				scene0.add(new ActionSetBlock(x, 0, z, net.minecraft.world.level.block.Blocks.DIRT));
			}
			scene0.add(new ActionWait(2));
		}

		scene0.add(new ActionWait(8));
		scene0.add(new ActionSetBlock(2, 2, 2, ModBlocks.PLANT_FLOWER_CD0.get()));

		scene0.add(new ActionWait(10));

		scene0.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, 0, -15, new Object[][] {{resolveKey("cannery.willow.0")}}, 150)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

		scene0.add(new ActionWait(80));

		scene0.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, 0, -15, new Object[][] {{resolveKey("cannery.willow.1")}}, 150)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

		scene0.add(new ActionWait(80));
		scene0.add(new ActionRemoveActor(0));

		scene0.add(new ActionWait(10));
		scene0.add(new ActionSetBlock(2, 1, 1, net.minecraft.world.level.block.Blocks.AIR));
		scene0.add(new ActionWait(10));
		scene0.add(new ActionSetBlock(2, 1, 1, net.minecraft.world.level.block.Blocks.WATER));

		scene0.add(new ActionWait(20));

		scene0.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, 0, -15, new Object[][] {{resolveKey("cannery.willow.2")}}, 150)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

		scene0.add(new ActionWait(80));
		scene0.add(new ActionRemoveActor(0));
		scene0.add(new ActionWait(20));
		scene0.add(new ActionSetBlock(2, 2, 2, ModBlocks.PLANT_FLOWER_CD1.get()));

		scene0.add(new ActionWait(20));

		scene0.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, 0, -15, new Object[][] {{resolveKey("cannery.willow.3")}}, 150)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

		scene0.add(new ActionWait(80));
		scene0.add(new ActionRemoveActor(0));
		scene0.add(new ActionWait(20));
		scene0.add(new ActionSetBlock(2, 2, 2, tall(ModBlocks.PLANT_TALL_CD2.get(), DoubleBlockHalf.LOWER)));
		scene0.add(new ActionSetBlock(2, 3, 2, tall(ModBlocks.PLANT_TALL_CD2.get(), DoubleBlockHalf.UPPER)));

		scene0.add(new ActionWait(20));
		scene0.add(new ActionSetBlock(2, 2, 2, tall(ModBlocks.PLANT_TALL_CD3.get(), DoubleBlockHalf.LOWER)));
		scene0.add(new ActionSetBlock(2, 3, 2, tall(ModBlocks.PLANT_TALL_CD3.get(), DoubleBlockHalf.UPPER)));
		scene0.add(new ActionWait(20));

		scene0.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, 0, -35, new Object[][] {{resolveKey("cannery.willow.4")}}, 150)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
		scene0.add(new ActionWait(80));
		scene0.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, 0, -35, new Object[][] {{resolveKey("cannery.willow.5")}}, 200)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

		scene0.add(new ActionWait(100));
		scene0.add(new ActionRemoveActor(0));
		scene0.add(new ActionWait(20));
		scene0.add(new ActionSetBlock(2, 1, 2, ModBlocks.DIRT_OILY.get()));

		scene0.add(new ActionWait(20));
		scene0.add(new ActionSetBlock(2, 2, 2, tall(ModBlocks.PLANT_TALL_CD4.get(), DoubleBlockHalf.LOWER)));
		scene0.add(new ActionSetBlock(2, 3, 2, tall(ModBlocks.PLANT_TALL_CD4.get(), DoubleBlockHalf.UPPER)));
		scene0.add(new ActionSetBlock(2, 1, 2, net.minecraft.world.level.block.Blocks.DIRT));
		scene0.add(new ActionWait(20));

		scene0.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, 0, -35, new Object[][] {{resolveKey("cannery.willow.6")}}, 150)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
		scene0.add(new ActionWait(100));
		scene0.add(new ActionRemoveActor(0));

		WiajScene scene1 = new WiajScene(script);
		scene1.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, 0, -35, new Object[][] {{resolveKey("cannery.willow.7")}}, 200)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
		scene1.add(new ActionWait(100));
		scene1.add(new ActionRemoveActor(0));

		scene1.add(new ActionWait(20));
		// Original: {"=", DictFrame.fromOne(ModBlocks.plant_flower, EnumFlowerType.CD0), "x1 + ",
		//            DictFrame.fromOne(ModItems.plant_item, EnumPlantType.MUSTARDWILLOW), "x3-6"}.
		// The willow term maps to plant_item_mustardwillow, same as the icon.
		scene1.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, 10, -25, new Object[][] {{"=", new ItemStack(ModBlocks.PLANT_FLOWER_CD0.get()), "x1 + ", new ItemStack(PartTabMetaItems.get("plant_item_mustardwillow").get()), "x3-6"}}, 0)
				.setColors(colorCopper).setOrientation(Orientation.LEFT)));
		scene1.add(new ActionSetBlock(2, 3, 2, net.minecraft.world.level.block.Blocks.AIR));
		scene1.add(new ActionSetBlock(2, 2, 2, ModBlocks.PLANT_FLOWER_CD0.get()));
		scene1.add(new ActionWait(60));

		scene1.add(new ActionRemoveActor(0));
		scene1.add(new ActionWait(20));
		scene1.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, 0, -15, new Object[][] {{resolveKey("cannery.willow.8")}}, 200)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
		scene1.add(new ActionWait(100));

		scene1.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, 0, -15, new Object[][] {{resolveKey("cannery.willow.9")}}, 150)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
		scene1.add(new ActionWait(100));
		scene1.add(new ActionRemoveActor(0));

		script.addScene(scene0).addScene(scene1);
		return script;
	}

	/** Original {@code I18nUtil.resolveKey}: resolves the key immediately for the panel lines. */
	private static String resolveKey(String key) {
		return net.minecraft.client.resources.language.I18n.get(key);
	}

	/**
	 * A tall plant state for the given half. The original encoded the two halves as
	 * {@code EnumTallFlower.ordinal()} (+8 for the upper half) metadata on one block; the
	 * modern blocks carry a {@link DoubleBlockHalf} property instead.
	 */
	private static net.minecraft.world.level.block.state.BlockState tall(net.minecraft.world.level.block.Block block, DoubleBlockHalf half) {
		return block.defaultBlockState().setValue(com.hbm_m.block.generic.NTMPlants.TallPlant.HALF, half);
	}
}
