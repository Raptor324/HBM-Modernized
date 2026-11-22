package com.hbm_m.wiaj.cannery;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.MachineFoundryChannelBlock;
import com.hbm_m.block.machines.MachineFoundryOutletBlock;
import com.hbm_m.blockentity.machines.MachineFoundryBasinBlockEntity;
import com.hbm_m.blockentity.machines.MachineFoundryChannelBlockEntity;
import com.hbm_m.blockentity.machines.MachineFoundryMoldBlockEntity;
import com.hbm_m.blockentity.machines.MachineFoundryOutletBlockEntity;
import com.hbm_m.blockentity.machines.MachineFoundryTankBlockEntity;
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
import com.hbm_m.wiaj.action.ActionSetBlock;
import com.hbm_m.wiaj.action.ActionSetTile;
import com.hbm_m.wiaj.action.ActionSetZoom;
import com.hbm_m.wiaj.action.ActionWait;
import com.hbm_m.wiaj.actor.ActorFancyPanel;
import com.hbm_m.wiaj.actor.ActorTileEntity;
import com.hbm_m.wiaj.actor.ITileActorRenderer;
import com.hbm_m.wiaj.actor.ActorFancyPanel.Orientation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Foundry casting chain demo: tank -> outlet -> channels -> outlets -> basin/molds, then the
 * cast gold block/ingots.
 * <p>
 * Port of {@code com.hbm.wiaj.cannery.CanneryFoundryChannel} (1.7.10); scene structure, action
 * order, timings, camera moves, panel keys ({@code cannery.foundryChannel.0..4}) and the copper
 * palette kept 1:1.
 * <p>
 * Mechanism changes mandated by the modern engine:
 * <ul>
 * <li>The original one {@code RenderFoundry} TESR (single {@code IRenderFoundry} path for
 * basin/mold/channel/tank) maps to the per-type registered Nucleus {@code MachineBer} specs
 * ({@code foundry_basin}/{@code foundry_mold}/{@code foundry_channel}/{@code foundry_tank}),
 * resolved per frame through the BE render dispatcher (see {@link ActorFoundry}). The outlet
 * has no BER - its body is the plain blockstate model placed by {@code ActionSetBlock}, same
 * as the original meta-2 ISBRH bounds.</li>
 * <li>The original drew the channel and tank melt through the ISBRH block path
 * ({@code RenderBlocks.renderBlockByRenderType} reading the jar-world TEs); the modern melt is
 * a {@code MachineRenderHook} that only runs through the BER, so {@link ActorFoundry} viewers
 * were added for the two channels and the tank (actor ids 4-6) - the original action list had
 * no actors there. The viewers read the jar-world TE every frame exactly like the original
 * renderer did, so the scripted {@code ActionSetTile} swaps drive them live.</li>
 * <li>Channel connectivity: the original ISBRH probed jar-world neighbors live; modern channels
 * carry their arms in the BlockState, set explicitly to the same topology (both on the placed
 * block and on the fake BE whose state feeds the melt quads).</li>
 * <li>The original {@code TileEntityFauxOutlet} nested class pinned {@code isClosed()} to false;
 * the modern {@code isClosed()} is level-gated, so the fake outlet BEs are kept level-less and
 * report open without a subclass - no FauxOutlet analog needed (the scene never closes them).</li>
	 * <li>The fake renderable BEs sit at jar coordinates offset far beyond the world border (same
	 * trick as {@code CannerySILEX.ActorFEL#GUI_POS_OFFSET}) so their pos-keyed Nucleus state never
	 * collides with a real machine, and are kept level-less like the SILEX pilot: the Nucleus BER's
	 * presence invariant ({@code AbstractPartBasedRenderer.render}) suppresses any BE whose level
	 * is a {@code ClientLevel} that does not hold that exact BE at its pos - always true for these
	 * never-placed fake BEs - so attaching the client level would drop every hook draw. The
	 * basin/mold/channel hooks use no level and render; the tank hook is level-gated
	 * ({@code TankHook} returns when {@code getLevel()} is null), so the tank's melt surface
	 * cannot render in the demo - recorded gap (the tank body still comes from the block pass).</li>
 * <li>Original mold stacks were {@code new ItemStack(ModItems.mold, 0, meta)} (the 1.7.10
 * count-0 "renderable but empty" trick); modern count-0 stacks are {@code isEmpty()}, which the
 * foundry hooks skip, so the molds are placed as normal count-1 stacks.</li>
 * <li>Original icon/molds {@code ModItems.mold} meta 2/12 map to the registered
 * {@code MOLD_INGOT}/{@code MOLD_BLOCK} cast-mold items (same mold sizes 0/1 and costs).</li>
 * </ul>
 */
public class CanneryFoundryChannel extends CanneryBase {

	@Override
	public ItemStack getIcon() {
		return new ItemStack(ModBlocks.FOUNDRY_CHANNEL.get());
	}

	@Override
	public String getName() {
		return "cannery.foundryChannel";
	}

	@Override
	public CanneryBase[] seeAlso() {
		return new CanneryBase[] {
				new CanneryCrucible()
		};
	}

	public WiajScript createScript() {
		VirtualWorld world = new VirtualWorld(5, 4, 4);
		WiajScript script = new WiajScript(world);

		WiajScene scene0 = new WiajScene(script);

		/// SETUP ///
		scene0.add(new ActionSetZoom(3, 0));

		for(int x = world.sizeX - 1; x >= 0; x--) {
			for(int z = 0; z < world.sizeZ; z++) {
				scene0.add(new ActionSetBlock(x, 0, z, Blocks.BRICKS));
			}
		}

		scene0.add(new ActionWait(5));
		scene0.add(new ActionSetBlock(1, 1, 2, Blocks.BRICKS));
		scene0.add(new ActionSetBlock(2, 1, 2, Blocks.BRICKS));
		scene0.add(new ActionSetBlock(3, 1, 2, Blocks.BRICKS));
		scene0.add(new ActionSetBlock(3, 1, 3, Blocks.BRICKS));

		MachineFoundryBasinBlockEntity basin = basin(3, 1, 1);
		basin.slots[0] = new ItemStack(ModItems.MOLD_BLOCK.get());
		scene0.add(new ActionSetTile(3, 1, 1, basin));
		scene0.add(new ActionCreateActor(1, new ActorTileEntity(new ActorFoundry(), jarPosTag(3, 1, 1)).smoothPlace()));
		scene0.add(new ActionSetBlock(3, 1, 1, ModBlocks.FOUNDRY_BASIN.get()));
		MachineFoundryMoldBlockEntity mold = mold(2, 1, 1);
		mold.slots[0] = new ItemStack(ModItems.MOLD_INGOT.get());
		scene0.add(new ActionSetTile(2, 1, 1, mold));
		scene0.add(new ActionCreateActor(2, new ActorTileEntity(new ActorFoundry(), jarPosTag(2, 1, 1)).smoothPlace()));
		scene0.add(new ActionSetBlock(2, 1, 1, ModBlocks.FOUNDRY_MOLD.get()));

		scene0.add(new ActionWait(5));
		scene0.add(new ActionSetBlock(3, 2, 3, Blocks.BRICKS));
		MachineFoundryMoldBlockEntity moldHigh = mold(1, 2, 2);
		moldHigh.slots[0] = new ItemStack(ModItems.MOLD_INGOT.get());
		scene0.add(new ActionSetTile(1, 2, 2, moldHigh));
		scene0.add(new ActionCreateActor(3, new ActorTileEntity(new ActorFoundry(), jarPosTag(1, 2, 2)).smoothPlace()));
		scene0.add(new ActionSetBlock(1, 2, 2, ModBlocks.FOUNDRY_MOLD.get()));
		// Channel A (2,2,2): north arm into outlet O1, west into the high mold, east into channel B.
		scene0.add(new ActionSetTile(2, 2, 2, channel(2, 2, 2, true, true, false, true)));
		scene0.add(new ActionSetBlock(2, 2, 2, channelState(true, true, false, true)));
		// Channel B (3,2,2): north arm into outlet O2, west into channel A.
		scene0.add(new ActionSetTile(3, 2, 2, channel(3, 2, 2, true, false, false, true)));
		scene0.add(new ActionSetBlock(3, 2, 2, channelState(true, false, false, true)));
		// Added viewers: the original channels drew their melt through the ISBRH block path.
		scene0.add(new ActionCreateActor(4, new ActorTileEntity(new ActorFoundry(), jarPosTag(2, 2, 2)).smoothPlace()));
		scene0.add(new ActionCreateActor(5, new ActorTileEntity(new ActorFoundry(), jarPosTag(3, 2, 2)).smoothPlace()));

		scene0.add(new ActionSetTile(2, 2, 1, outlet(2, 2, 1)));
		scene0.add(new ActionSetBlock(2, 2, 1, outletState()));
		scene0.add(new ActionSetTile(3, 2, 1, outlet(3, 2, 1)));
		scene0.add(new ActionSetBlock(3, 2, 1, outletState()));
		scene0.add(new ActionWait(5));
		MachineFoundryTankBlockEntity tank = tank(3, 3, 3);
		tank.type = Mats.MAT_GOLD;
		tank.amount = MaterialShapes.BLOCK.q(1);
		scene0.add(new ActionSetTile(3, 3, 3, tank));
		scene0.add(new ActionSetBlock(3, 3, 3, ModBlocks.FOUNDRY_TANK.get()));
		// Added viewer: the original tank melt drew through the ISBRH block path.
		scene0.add(new ActionCreateActor(6, new ActorTileEntity(new ActorFoundry(), jarPosTag(3, 3, 3)).smoothPlace()));
		scene0.add(new ActionSetTile(3, 3, 2, outlet(3, 3, 2)));
		scene0.add(new ActionSetBlock(3, 3, 2, outletState()));

		scene0.add(new ActionWait(10));

		scene0.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, 0, -25, new Object[][] {{resolveKey("cannery.foundryChannel.0")}}, 200)
				.setColors(colorCopper).setOrientation(Orientation.LEFT)));

		scene0.add(new ActionWait(60));
		scene0.add(new ActionRemoveActor(0));
		scene0.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, -5, -40, new Object[][] {{resolveKey("cannery.foundryChannel.1")}}, 200)
				.setColors(colorCopper).setOrientation(Orientation.TOP)));

		scene0.add(new ActionWait(60));
		scene0.add(new ActionRemoveActor(0));
		scene0.add(new ActionWait(10));
		MachineFoundryChannelBlockEntity filled = channel(3, 2, 2, true, false, false, true);
		filled.type = Mats.MAT_GOLD;
		filled.amount = MaterialShapes.INGOT.q(1);
		scene0.add(new ActionSetTile(3, 2, 2, filled));

		scene0.add(new ActionWait(10));
		scene0.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, 0, -25, new Object[][] {{resolveKey("cannery.foundryChannel.2")}}, 200)
				.setColors(colorCopper).setOrientation(Orientation.LEFT)));

		scene0.add(new ActionWait(60));
		scene0.add(new ActionRemoveActor(0));

		for(int i = 0; i < 60; i++) {
			MachineFoundryBasinBlockEntity pouring = basin(3, 1, 1);
			pouring.slots[0] = new ItemStack(ModItems.MOLD_BLOCK.get());
			pouring.type = Mats.MAT_GOLD;
			pouring.amount = MaterialShapes.BLOCK.q(i, 60);
			scene0.add(new ActionSetTile(3, 1, 1, pouring));
			scene0.add(new ActionWait(1));
		}

		WiajScene scene1 = new WiajScene(script);

		scene1.add(new ActionWait(10));
		scene1.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, 0, -25, new Object[][] {{resolveKey("cannery.foundryChannel.3")}}, 200)
				.setColors(colorCopper).setOrientation(Orientation.LEFT)));

		scene1.add(new ActionWait(60));
		scene1.add(new ActionRemoveActor(0));

		scene1.add(new ActionWait(10));
		MachineFoundryChannelBlockEntity filledA = channel(2, 2, 2, true, true, false, true);
		filledA.type = Mats.MAT_GOLD;
		filledA.amount = MaterialShapes.INGOT.q(1);
		scene1.add(new ActionSetTile(2, 2, 2, filledA));
		scene1.add(new ActionWait(10));

		for(int i = 0; i < 60; i++) {
			MachineFoundryMoldBlockEntity pouringA = mold(2, 1, 1);
			pouringA.slots[0] = new ItemStack(ModItems.MOLD_INGOT.get());
			pouringA.type = Mats.MAT_GOLD;
			pouringA.amount = MaterialShapes.INGOT.q(i, 60);
			scene1.add(new ActionSetTile(2, 1, 1, pouringA));
			MachineFoundryMoldBlockEntity pouringB = mold(1, 2, 2);
			pouringB.slots[0] = new ItemStack(ModItems.MOLD_INGOT.get());
			pouringB.type = Mats.MAT_GOLD;
			pouringB.amount = MaterialShapes.INGOT.q(i, 60);
			scene1.add(new ActionSetTile(1, 2, 2, pouringB));
			scene1.add(new ActionWait(1));
		}

		scene1.add(new ActionWait(2));
		scene1.add(new ActionSetTile(3, 3, 3, tank(3, 3, 3)));
		scene1.add(new ActionWait(20));

		scene1.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, 0, -25, new Object[][] {{resolveKey("cannery.foundryChannel.4")}}, 200)
				.setColors(colorCopper).setOrientation(Orientation.LEFT)));
		scene1.add(new ActionWait(40));
		scene1.add(new ActionRemoveActor(0));
		scene1.add(new ActionWait(10));

		scene1.add(new ActionSetTile(2, 2, 2, channel(2, 2, 2, true, true, false, true)));
		scene1.add(new ActionSetTile(3, 2, 2, channel(3, 2, 2, true, false, false, true)));
		scene1.add(new ActionCreateActor(0, new ActorFancyPanel(Minecraft.getInstance().font, 0, -25, new Object[][] {{new ItemStack(Items.IRON_SHOVEL), " -> ", ItemScraps.create(new MaterialStack(Mats.MAT_GOLD, 1))}}, 200)
				.setColors(colorCopper).setOrientation(Orientation.LEFT)));
		scene1.add(new ActionWait(40));
		scene1.add(new ActionRemoveActor(0));

		scene1.add(new ActionWait(60));
		MachineFoundryMoldBlockEntity doneA = mold(2, 1, 1);
		doneA.slots[1] = new ItemStack(Items.GOLD_INGOT);
		scene1.add(new ActionSetTile(2, 1, 1, doneA));
		MachineFoundryMoldBlockEntity doneB = mold(1, 2, 2);
		doneB.slots[1] = new ItemStack(Items.GOLD_INGOT);
		scene1.add(new ActionSetTile(1, 2, 2, doneB));
		MachineFoundryBasinBlockEntity doneBasin = basin(3, 1, 1);
		doneBasin.slots[1] = new ItemStack(Blocks.GOLD_BLOCK);
		scene1.add(new ActionSetTile(3, 1, 1, doneBasin));

		script.addScene(scene0).addScene(scene1);
		return script;
	}

	// -- Jar-world fake BE helpers -----------------------------------------

	/**
	 * Jar-world fake BEs sit beyond the world border so their BlockPos.asLong() roster/cache
	 * keys can never collide with a real machine ({@code CannerySILEX.ActorFEL} convention).
	 */
	static final int GUI_POS_OFFSET = 30_000_000;

	private static BlockPos jarPos(int x, int y, int z) {
		return new BlockPos(x + GUI_POS_OFFSET, y, z);
	}

	/** Original actor data tag: {@code new NBTTagCompound() {{ setInteger("x", ...); }}}. */
	private static CompoundTag jarPosTag(int x, int y, int z) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("x", x);
		tag.putInt("y", y);
		tag.putInt("z", z);
		return tag;
	}

	/**
	 * Attaches nothing: fake BEs are kept level-less like {@code CannerySILEX.ActorFEL}. The
	 * Nucleus BER's presence invariant suppresses any BE whose level is a {@code ClientLevel}
	 * that doesn't hold it at its pos (always true for never-placed BEs), and the basin/mold/
	 * channel hooks need no level; the tank hook is level-gated, so its melt stays a recorded
	 * gap (see the class javadoc).
	 */
	private static <T extends BlockEntity> T fake(T be) {
		return be;
	}

	private static MachineFoundryBasinBlockEntity basin(int x, int y, int z) {
		return fake(new MachineFoundryBasinBlockEntity(jarPos(x, y, z), ModBlocks.FOUNDRY_BASIN.get().defaultBlockState()));
	}

	private static MachineFoundryMoldBlockEntity mold(int x, int y, int z) {
		return fake(new MachineFoundryMoldBlockEntity(jarPos(x, y, z), ModBlocks.FOUNDRY_MOLD.get().defaultBlockState()));
	}

	private static MachineFoundryChannelBlockEntity channel(int x, int y, int z, boolean north, boolean east, boolean south, boolean west) {
		return fake(new MachineFoundryChannelBlockEntity(jarPos(x, y, z), channelState(north, east, south, west)));
	}

	private static MachineFoundryTankBlockEntity tank(int x, int y, int z) {
		return fake(new MachineFoundryTankBlockEntity(jarPos(x, y, z), ModBlocks.FOUNDRY_TANK.get().defaultBlockState()));
	}

	/**
	 * No client level attached on purpose: the original {@code TileEntityFauxOutlet} pinned
	 * {@code isClosed()} to false, and the modern {@code isClosed()} is level-gated - a
	 * level-less fake BE reports open, exactly like the original subclass.
	 */
	private static MachineFoundryOutletBlockEntity outlet(int x, int y, int z) {
		return new MachineFoundryOutletBlockEntity(jarPos(x, y, z), outletState());
	}

	/** A channel state with explicit connection arms (the modern melt quads read the BE's state). */
	private static BlockState channelState(boolean north, boolean east, boolean south, boolean west) {
		return ModBlocks.FOUNDRY_CHANNEL.get().defaultBlockState()
				.setValue(MachineFoundryChannelBlock.NORTH, north)
				.setValue(MachineFoundryChannelBlock.EAST, east)
				.setValue(MachineFoundryChannelBlock.SOUTH, south)
				.setValue(MachineFoundryChannelBlock.WEST, west);
	}

	/**
	 * Original {@code foundry_outlet} meta 2: spout body on the south side (z 10..16 px), which
	 * is the modern {@code FACING = NORTH} state (body opposite the pour facing).
	 */
	private static BlockState outletState() {
		return ModBlocks.FOUNDRY_OUTLET.get().defaultBlockState()
				.setValue(MachineFoundryOutletBlock.FACING, Direction.NORTH);
	}

	/** Original {@code I18nUtil.resolveKey}: resolves the key immediately for the panel lines. */
	private static String resolveKey(String key) {
		return net.minecraft.client.resources.language.I18n.get(key);
	}

	/**
	 * The modern analog of the original {@code ActorTileEntity(new RenderFoundry(), NBT{x,y,z})}:
	 * looks the jar-world foundry BE up at the actor's position every frame and renders it
	 * through its registered Nucleus {@code MachineBer} (basin/mold hooks draw mold, cast item
	 * and melt surface; the channel's "Melt" and the tank's melt are dynamic parts/hooks keyed
	 * on the current content), so the scripted {@code ActionSetTile} content swaps drive the
	 * demo live. The pose is block-corner based (translate(x, y, z), no manual rotation), the
	 * same convention as {@code CannerySILEX.ActorFEL}.
	 */
	public static class ActorFoundry implements ITileActorRenderer {

		@Override
		public void renderActor(GuiGraphics gui, VirtualWorld world, int ticks, float interp, CompoundTag data) {
			int x = data.getInt("x");
			int y = data.getInt("y");
			int z = data.getInt("z");

			// Original: renderTileEntityAt(world.getTileEntity(x, y, z), x, y, z, interp);
			BlockEntity be = world.getBlockEntity(new BlockPos(x, y, z));
			if(be == null) return;

			gui.pose().pushPose();
			gui.pose().translate(x, y, z);

			BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
			BlockEntityRenderer<BlockEntity> ber = dispatcher.getRenderer(be);
			if(ber != null) {
				ber.render(be, interp, gui.pose(), gui.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
			}

			gui.pose().popPose();
		}

		@Override
		public void updateActor(int ticks, CompoundTag data) { }
	}
}
