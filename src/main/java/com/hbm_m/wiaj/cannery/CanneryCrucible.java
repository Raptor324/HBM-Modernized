package com.hbm_m.wiaj.cannery;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.machines.MachineCrucibleBlockEntity;
import com.hbm_m.blockentity.machines.MachineFoundryBasinBlockEntity;
import com.hbm_m.inventory.material.MaterialShapes;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.ItemScraps;
import com.hbm_m.wiaj.VirtualWorld;
import com.hbm_m.wiaj.WiajScene;
import com.hbm_m.wiaj.WiajScript;
import com.hbm_m.wiaj.action.ActionCreateActor;
import com.hbm_m.wiaj.action.ActionRemoveActor;
import com.hbm_m.wiaj.action.ActionRotateBy;
import com.hbm_m.wiaj.action.ActionSetBlock;
import com.hbm_m.wiaj.action.ActionSetTile;
import com.hbm_m.wiaj.action.ActionSetZoom;
import com.hbm_m.wiaj.action.ActionUpdateActor;
import com.hbm_m.wiaj.action.ActionWait;
import com.hbm_m.wiaj.actor.ActorFancyPanel;
import com.hbm_m.wiaj.actor.ActorTileEntity;
import com.hbm_m.wiaj.actor.ITileActorRenderer;
import com.hbm_m.wiaj.actor.ActorFancyPanel.Orientation;
import com.hbm_m.wiaj.cannery.CanneryFirebox.ActorFirebox;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Crucible & foundry casting demo.
 * <p>
 * Port of {@code com.hbm.wiaj.cannery.CanneryCrucible} (1.7.10). The original rendered the
 * crucible and the foundry basin through their ISBRH/TESR {@code RenderCrucible}/{@code RenderFoundry}
 * actors, pulling the tile from the jar's tile map at the scripted position; here the machines
 * render through their registered Nucleus {@code MachineBer} ({@code CrucibleRenderer},
 * {@code MachineFoundryRenderer}), with the actor fetching the tile-map block entity and
 * dispatching it like the in-world renderer.
 * <p>
 * The original {@code TileEntityFauxCrucible} overrode {@code getBlockMetadata()} to return
 * {@code meta = 14} so the TESR drew the crucible rotated 180&deg; - the modern renderer takes the
 * rotation from the block state's {@code FACING} ({@code CardinalOrientation}: WEST = 180&deg;),
 * so the faux crucible is created with {@code FACING=WEST} and no meta analog is needed.
 * <p>
 * The original {@code RenderFoundry} drew mold/cast item and melt surface for the basin; the
 * modern {@code MachineFoundryRenderer} hook does the same for any registered foundry BE.
 * <p>
 * The original "renderable but empty" mold stacks were {@code new ItemStack(ModItems.mold, 0, meta)}
 * (count 0); modern count-0 stacks are {@code isEmpty()}, which the foundry hook skips, so the
 * molds here are normal count-1 stacks (same deviation as {@code CanneryFoundryChannel}).
 */
public class CanneryCrucible extends CanneryBase {

	@Override
	public ItemStack getIcon() {
		return new ItemStack(ModItems.CRUCIBLE.get());
	}

	@Override
	public String getName() {
		return "cannery.crucible";
	}

	public CanneryBase[] seeAlso() {
		return new CanneryBase[] {
				new CanneryFoundryChannel()
		};
	}

	public WiajScript createScript() {
		VirtualWorld world = new VirtualWorld(5, 4, 5);
		WiajScript script = new WiajScript(world);

		WiajScene scene0 = new WiajScene(script);
		scene0.add(new ActionSetZoom(3, 0));

		for(int x = 0; x < 5 ; x++) {
			for(int z = 0; z < 5; z++) {
				scene0.add(new ActionSetBlock(x, 0, z, Blocks.BRICKS));
			}
		}
		scene0.add(new ActionWait(5));
		for(int x = 1; x < 4 ; x++) {
			for(int z = 1; z < 4; z++) {
				scene0.add(new ActionSetBlock(x, 1, z, Blocks.BRICKS));
			}
		}
		scene0.add(new ActionWait(5));

		scene0.add(new ActionSetTile(2, 2, 2, fauxCrucible()));
		CompoundTag crucibleTag = new CompoundTag(); crucibleTag.putInt("x", 2); crucibleTag.putInt("y", 2); crucibleTag.putInt("z", 2);
		scene0.add(new ActionCreateActor(1, new ActorTileEntity(new ActorCrucible(), crucibleTag).smoothPlace()));

		scene0.add(new ActionWait(10));
		scene0.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, 0, -30, new Object[][] {{resolveKey("cannery.crucible.0")}}, 200)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

		scene0.add(new ActionWait(60));
		scene0.add(new ActionRemoveActor(0));
		scene0.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, 0, -30, new Object[][] {{resolveKey("cannery.crucible.1")}}, 200)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

		scene0.add(new ActionWait(60));
		scene0.add(new ActionRemoveActor(0));
		scene0.add(new ActionWait(10));

		for(int x = 1; x < 4 ; x++) for(int z = 1; z < 4; z++) scene0.add(new ActionSetBlock(x, 1, z, Blocks.AIR));

		CompoundTag firebox = new CompoundTag(); firebox.putDouble("x", 2); firebox.putDouble("y", 1); firebox.putDouble("z", 2); firebox.putInt("rotation", 5);
		scene0.add(new ActionCreateActor(2, new ActorTileEntity(new ActorFirebox(), firebox).smoothPlace()));

		scene0.add(new ActionUpdateActor(2, "open", true));
		scene0.add(new ActionWait(30));
		scene0.add(new ActionUpdateActor(2, "isOn", true));

		scene0.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, -50, 25, new Object[][] {{new ItemStack(Items.COAL)}}, 0)
				.setColors(colorCopper).setOrientation(Orientation.RIGHT)));

		scene0.add(new ActionWait(20));
		scene0.add(new ActionRemoveActor(0));
		scene0.add(new ActionWait(10));
		scene0.add(new ActionUpdateActor(2, "open", false));
		scene0.add(new ActionWait(30));

		WiajScene scene1 = new WiajScene(script);

		scene1.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, 0, -30, new Object[][] {{resolveKey("cannery.crucible.2")}}, 200)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
		scene1.add(new ActionWait(60));

		scene1.add(new ActionRemoveActor(0));
		scene1.add(new ActionRotateBy(45, -60, 20));
		scene1.add(new ActionWait(10));

		scene1.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, 0, 0, new Object[][] {{resolveKey("cannery.crucible.3")}}, 200)
				.setColors(colorCopper).setOrientation(Orientation.CENTER)));
		scene1.add(new ActionWait(40));

		scene1.add(new ActionRemoveActor(0));
		scene1.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, -30, 0, new Object[][] {{resolveKey("cannery.crucible.4")}}, 200)
				.setColors(colorCopper).setOrientation(Orientation.LEFT)));
		scene1.add(new ActionWait(60));

		scene1.add(new ActionRemoveActor(0));
		scene1.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, -30, 0, new Object[][] {{resolveKey("cannery.crucible.5")}}, 200)
				.setColors(colorCopper).setOrientation(Orientation.LEFT)));
		scene1.add(new ActionWait(60));

		scene1.add(new ActionRemoveActor(0));
		scene1.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, -30, 0, new Object[][] {{resolveKey("cannery.crucible.6")}}, 200)
				.setColors(colorCopper).setOrientation(Orientation.LEFT)));
		scene1.add(new ActionWait(60));

		scene1.add(new ActionRemoveActor(0));
		scene1.add(new ActionWait(20));
		scene1.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, 30, 0, new Object[][] {{resolveKey("cannery.crucible.7")}}, 200)
				.setColors(colorCopper).setOrientation(Orientation.RIGHT)));
		scene1.add(new ActionWait(60));

		scene1.add(new ActionRemoveActor(0));
		scene1.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, 30, 0, new Object[][] {{resolveKey("cannery.crucible.8")}}, 200)
				.setColors(colorCopper).setOrientation(Orientation.RIGHT)));
		scene1.add(new ActionWait(60));

		scene1.add(new ActionRemoveActor(0));
		scene1.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, 30, 0, new Object[][] {{resolveKey("cannery.crucible.9")}}, 200)
				.setColors(colorCopper).setOrientation(Orientation.RIGHT)));
		scene1.add(new ActionWait(60));

		scene1.add(new ActionRemoveActor(0));
		scene1.add(new ActionRotateBy(-45, 60, 20));
		scene1.add(new ActionWait(10));

		WiajScene scene2 = new WiajScene(script);
		scene2.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, 30, 0, new Object[][] {{resolveKey("cannery.crucible.10")}}, 200)
				.setColors(colorCopper).setOrientation(Orientation.RIGHT)));
		scene2.add(new ActionWait(60));

		scene2.add(new ActionRemoveActor(0));
		scene2.add(new ActionWait(10));
		scene2.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, 0, -40, new Object[][] {{new ItemStack(Items.GOLD_INGOT, 11)}}, 0)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
		scene2.add(new ActionWait(20));

		scene2.add(new ActionRemoveActor(0));
		MachineCrucibleBlockEntity filled = fauxCrucible();
		filled.recipeStack.add(new MaterialStack(Mats.MAT_GOLD, MaterialShapes.BLOCK.q(16)));
		scene2.add(new ActionSetTile(2, 2, 2, filled));
		scene2.add(new ActionWait(10));

		scene2.add(new ActionSetTile(0, 1, 2, newBasin()));
		CompoundTag basinTag = new CompoundTag(); basinTag.putInt("x", 0); basinTag.putInt("y", 1); basinTag.putInt("z", 2);
		scene2.add(new ActionCreateActor(3, new ActorTileEntity(new ActorFoundry(), basinTag).smoothPlace()));
		scene2.add(new ActionSetBlock(0, 1, 2, ModBlocks.FOUNDRY_BASIN.get()));

		scene2.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, 42, 25, new Object[][] {{new ItemStack(ModItems.MOLD_BLOCK.get())}}, 0)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
		scene2.add(new ActionWait(20));
		scene2.add(new ActionRemoveActor(0));
		MachineFoundryBasinBlockEntity molded = newBasin();
		molded.slots[0] = new ItemStack(ModItems.MOLD_BLOCK.get());
		scene2.add(new ActionSetTile(0, 1, 2, molded));

		scene2.add(new ActionWait(20));

		for(int i = 0; i < 60; i++) {
			final int j = i;
			MachineCrucibleBlockEntity pouring = fauxCrucible();
			pouring.recipeStack.add(new MaterialStack(Mats.MAT_GOLD, MaterialShapes.BLOCK.q((60 - j), 60) * 16));
			scene2.add(new ActionSetTile(2, 2, 2, pouring));
			MachineFoundryBasinBlockEntity cast = newBasin();
			cast.slots[0] = new ItemStack(ModItems.MOLD_BLOCK.get());
			cast.type = Mats.MAT_GOLD;
			cast.amount = MaterialShapes.BLOCK.q(j + 1, 60);
			scene2.add(new ActionSetTile(0, 1, 2, cast));
			scene2.add(new ActionWait(1));
		}

		scene2.add(new ActionWait(40));
		MachineFoundryBasinBlockEntity solid = newBasin();
		solid.slots[0] = new ItemStack(ModItems.MOLD_BLOCK.get());
		solid.slots[1] = new ItemStack(Blocks.GOLD_BLOCK);
		scene2.add(new ActionSetTile(0, 1, 2, solid));
		scene2.add(new ActionWait(20));
		MachineFoundryBasinBlockEntity emptied = newBasin();
		emptied.slots[0] = new ItemStack(ModItems.MOLD_BLOCK.get());
		scene2.add(new ActionSetTile(0, 1, 2, emptied));
		scene2.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, 42, 25, new Object[][] {{new ItemStack(Blocks.GOLD_BLOCK)}}, 0)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
		scene2.add(new ActionWait(20));
		scene2.add(new ActionRemoveActor(0));

		scene2.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, 0, -30, new Object[][] {{resolveKey("cannery.crucible.11")}}, 200)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
		scene2.add(new ActionWait(60));
		scene2.add(new ActionRemoveActor(0));
		scene2.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, 0, -30, new Object[][] {{new ItemStack(Items.IRON_SHOVEL), " -> ", ItemScraps.create(new MaterialStack(Mats.MAT_GOLD, 1))}}, 200)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
		scene2.add(new ActionSetTile(2, 2, 2, fauxCrucible()));
		scene2.add(new ActionWait(20));
		scene2.add(new ActionRemoveActor(0));

		script.addScene(scene0).addScene(scene1).addScene(scene2);

		return script;
	}

	/**
	 * The original {@code TileEntityFauxCrucible} with {@code meta = 14}: the meta only fed the
	 * 1.7.10 TESR rotation (180&deg;), which the modern renderer derives from the state's
	 * {@code FACING} ({@code CardinalOrientation}: WEST = 180&deg;).
	 */
	private static MachineCrucibleBlockEntity fauxCrucible() {
		return new MachineCrucibleBlockEntity(
				new BlockPos(2, 2, 2),
				crucibleState());
	}

	private static BlockState crucibleState() {
		return ModBlocks.CRUCIBLE.get().defaultBlockState()
				.setValue(DummyableMachineBlock.FACING, Direction.WEST);
	}

	/** The original {@code new TileEntityFoundryBasin()} placeholder, at the scripted basin position. */
	private static MachineFoundryBasinBlockEntity newBasin() {
		return new MachineFoundryBasinBlockEntity(
				new BlockPos(0, 1, 2),
				ModBlocks.FOUNDRY_BASIN.get().defaultBlockState());
	}

	/** Original {@code I18nUtil.resolveKey}: resolves the key immediately for the panel lines. */
	private static String resolveKey(String key) {
		return net.minecraft.client.resources.language.I18n.get(key);
	}

	/**
	 * Renders the crucible through its registered Nucleus {@code MachineBer}: the original actor
	 * called {@code RenderCrucible} on the tile fetched from the jar's tile map; here the tile-map
	 * block entity's melt content ({@code recipeStack}/{@code wasteStack}) is mirrored into a
	 * display entity (so the ActionSetTile-per-tick pour loop drives the renderer exactly like the
	 * original) and dispatched. The display BE lives at virtual coordinates offset far beyond the
	 * world border (see {@link CannerySILEX.ActorFEL}) so its {@code BlockPos.asLong()} roster/cache
	 * keys can never collide with a real machine; the body's 180&deg; yaw (original {@code meta = 14})
	 * comes from the state's {@code FACING=WEST} via the spec's block transform.
	 */
	public static class ActorCrucible implements ITileActorRenderer {

		/** X offset isolating the display BE's pos-keyed Nucleus state from real machines. */
		static final int GUI_POS_OFFSET = 30_000_000;

		private MachineCrucibleBlockEntity display;

		@Override
		public void renderActor(GuiGraphics gui, VirtualWorld world, int ticks, float interp, CompoundTag data) {
			int x = data.getInt("x");
			int y = data.getInt("y");
			int z = data.getInt("z");

			BlockEntity source = world.getBlockEntity(new BlockPos(x, y, z));
			if(!(source instanceof MachineCrucibleBlockEntity crucible)) return;

			if(this.display == null) {
				this.display = new MachineCrucibleBlockEntity(
						new BlockPos(x + GUI_POS_OFFSET, y, z),
						crucibleState());
			}
			this.display.recipeStack = crucible.recipeStack;
			this.display.wasteStack = crucible.wasteStack;

			gui.pose().pushPose();
			// Original RenderCrucible.renderActor: GL11.glTranslated(x + 0.5D, y, z + 0.5D) - the
			// renderer's block-local transform centers on (0.5, 0, 0.5), so translating by the
			// corner (x, y, z) lands the model at the same spot (same convention as ActorFEL).
			gui.pose().translate(x, y, z);

			BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
			BlockEntityRenderer<BlockEntity> ber = dispatcher.getRenderer(this.display);
			if(ber != null) {
				ber.render(this.display, interp, gui.pose(), gui.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
			}

			gui.pose().popPose();
		}

		@Override
		public void updateActor(int ticks, CompoundTag data) { }
	}

	/**
	 * Renders the foundry basin through its registered Nucleus {@code MachineBer}: the original
	 * actor called {@code RenderFoundry} on the tile fetched from the jar's tile map (mold slot 0,
	 * cast slot 1, melt surface); here the tile-map block entity's content is mirrored into a
	 * display entity at an isolated position (see {@link ActorCrucible}) and dispatched. The basin
	 * body itself is drawn by the scene's block pass ({@code ActionSetBlock}), like the original
	 * where {@code RenderBlocks} drew the block and the TESR only drew the content.
	 */
	public static class ActorFoundry implements ITileActorRenderer {

		private MachineFoundryBasinBlockEntity display;

		@Override
		public void renderActor(GuiGraphics gui, VirtualWorld world, int ticks, float interp, CompoundTag data) {
			int x = data.getInt("x");
			int y = data.getInt("y");
			int z = data.getInt("z");

			BlockEntity source = world.getBlockEntity(new BlockPos(x, y, z));
			if(!(source instanceof MachineFoundryBasinBlockEntity basin)) return;

			if(this.display == null) {
				this.display = new MachineFoundryBasinBlockEntity(
						new BlockPos(x + ActorCrucible.GUI_POS_OFFSET, y, z),
						ModBlocks.FOUNDRY_BASIN.get().defaultBlockState());
			}
			this.display.slots[0] = basin.slots[0];
			this.display.slots[1] = basin.slots[1];
			this.display.type = basin.type;
			this.display.amount = basin.amount;

			gui.pose().pushPose();
			// Original RenderFoundry.renderTileEntityAt: GL11.glTranslated(x + 0.5D, y, z + 0.5D)
			// - same corner-based translate as ActorCrucible above.
			gui.pose().translate(x, y, z);

			BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
			BlockEntityRenderer<BlockEntity> ber = dispatcher.getRenderer(this.display);
			if(ber != null) {
				ber.render(this.display, interp, gui.pose(), gui.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
			}

			gui.pose().popPose();
		}

		@Override
		public void updateActor(int ticks, CompoundTag data) { }
	}
}
