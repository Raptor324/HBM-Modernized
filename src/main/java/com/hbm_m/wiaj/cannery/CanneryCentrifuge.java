package com.hbm_m.wiaj.cannery;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.FluidDuctBlockEntity;
import com.hbm_m.blockentity.machines.MachineGasCentBlockEntity;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
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
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Gas centrifuge / UF6 enrichment demo.
 * <p>
 * Port of {@code com.hbm.wiaj.cannery.CanneryCentrifuge} (1.7.10). The original actor rendered
 * the {@code ResourceManager.gascent} model parts directly; here the machine renders through
 * its registered Nucleus {@code MachineBer} ("gascent" spec: static {@code Flag} part plus the
 * {@code Centrifuge} rotor jointed around (0.5, 0.5), animated by {@code getAnim}), so the pose
 * convention is the same as {@link CannerySILEX}: block-corner pose, the spec's blockTransform
 * applies the centering. The original {@code rotation} codes (3:0°, 5:90°, 2:180°, 4:270°) map
 * to an explicit yaw applied about the block center (the model-space origin after the block
 * transform), reproducing the original {@code glTranslated(x + 0.5)·glRotatef} net transform.
 * <p>
 * The original pipe run was a {@code TileEntityPipeBaseNT(UF6)} between two invisible
 * {@code JarDummyConnector}s so the 1.7.10 ISBRH pipe renderer drew arms into the barrel and
 * the centrifuge. Modern ducts carry their connections in the {@code BlockState} and
 * {@code VirtualWorld} applies no neighbor updates, so the duct state is set explicitly
 * (EAST into the barrel at (7,1,2), WEST into the centrifuge at (5,1,2)); the duct keeps a
 * real {@link FluidDuctBlockEntity} with the UF6 type (as the original's pipe tile), and the
 * two {@link Dummies.JarDummyConnector} placements are kept 1:1 - they are inert in the
 * modern engine but preserve the original action sequence.
 */
public class CanneryCentrifuge extends CanneryBase {

	@Override
	public ItemStack getIcon() {
		return new ItemStack(ModItems.MACHINE_GASCENT.get());
	}

	@Override
	public String getName() {
		return "cannery.centrifuge";
	}

	public WiajScript createScript() {
		VirtualWorld world = new VirtualWorld(9, 5, 5);
		WiajScript script = new WiajScript(world);

		WiajScene scene0 = new WiajScene(script);

		scene0.add(new ActionSetZoom(2, 0));

		for(int x = world.sizeX - 1; x >= 0; x--) {
			for(int z = 0; z < world.sizeZ; z++) {
				scene0.add(new ActionSetBlock(x, 0, z, net.minecraft.world.level.block.Blocks.BRICKS));
			}

			if(x == 7) {
				scene0.add(new ActionSetTile(7, 1, 2, new Dummies.JarDummyConnector()));
				CompoundTag barrel = new CompoundTag();
				barrel.putDouble("x", 7D);
				barrel.putDouble("y", 1D);
				barrel.putDouble("z", 2D);
				// id 10, NOT 1: the original barrel was a static BLOCK (ActionSetBlock barrel_tcalloy)
				// that lives for the whole demo, while the original's text panels reuse actor id 1 -
				// the port's barrel-as-actor inherited that id and every panel creation replaced it,
				// deleting the barrel the moment the first caption appeared
				scene0.add(new ActionCreateActor(10, new ActorTileEntity(new CannerySILEX.ActorBarrel(ModFluids.UF6.getSource()), barrel).smoothPlace()));
			}

			if(x == 6) {
				// Original: TileEntityPipeBaseNT with UF6 between two dummy connectors; modern ducts
				// connect via BlockState (EAST arm into the barrel, WEST into the centrifuge), and the
				// pipe tile maps to a real FluidDuctBlockEntity carrying the same fluid type.
				BlockState duct = duct(false, false, true, true, false, false);
				FluidDuctBlockEntity ductTile = new FluidDuctBlockEntity(new BlockPos(6, 1, 2), duct);
				ductTile.setFluidTypeSilent(ModFluids.UF6.getSource());
				scene0.add(new ActionSetTile(6, 1, 2, ductTile));
				scene0.add(new ActionSetBlock(6, 1, 2, duct));
			}

			if(x == 5) {
				scene0.add(new ActionSetTile(5, 1, 2, new Dummies.JarDummyConnector()));
				CompoundTag cent = new CompoundTag();
				cent.putDouble("x", 5);
				cent.putDouble("y", 1);
				cent.putDouble("z", 2);
				cent.putInt("rotation", 2);
				scene0.add(new ActionCreateActor(0, new ActorTileEntity(new ActorGasCent(), cent).smoothPlace()));
			}

			scene0.add(new ActionWait(2));
		}

		scene0.add(new ActionCreateActor(1, new ActorFancyPanel(Minecraft.getInstance().font, -15, -50, new Object[][] {{resolveKey("cannery.centrifuge.0")}}, 200)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

		scene0.add(new ActionWait(60));
		scene0.add(new ActionRemoveActor(1));

		WiajScene scene1 = new WiajScene(script);

		scene1.add(new ActionCreateActor(1, new ActorFancyPanel(Minecraft.getInstance().font, -15, 10, new Object[][] {{resolveKey("cannery.centrifuge.1")}}, 200)
				.setColors(colorCopper).setOrientation(Orientation.CENTER)));

		scene1.add(new ActionWait(60));
		scene1.add(new ActionRemoveActor(1));

		scene1.add(new ActionSetZoom(4, 20));

		scene1.add(new ActionCreateActor(1, new ActorFancyPanel(Minecraft.getInstance().font, 0, 40, new Object[][] {{resolveKey("cannery.centrifuge.2")}}, 150)
				.setColors(colorCopper).setOrientation(Orientation.LEFT)));

		scene1.add(new ActionWait(60));
		scene1.add(new ActionRemoveActor(1));
		scene1.add(new ActionSetZoom(-2, 20));
		scene1.add(new ActionWait(20));

		CompoundTag c2 = new CompoundTag(); c2.putDouble("x", 4); c2.putDouble("y", 1); c2.putDouble("z", 2); c2.putInt("rotation", 2);
		scene1.add(new ActionCreateActor(1, new ActorTileEntity(new ActorGasCent(), c2).smoothPlace()));
		scene1.add(new ActionWait(10));

		scene1.add(new ActionCreateActor(2, new ActorFancyPanel(Minecraft.getInstance().font, 0, 0, new Object[][] {{resolveKey("cannery.centrifuge.3")}}, 200)
				.setColors(colorCopper).setOrientation(Orientation.CENTER)));

		scene1.add(new ActionWait(100));
		scene1.add(new ActionRemoveActor(2));
		scene1.add(new ActionSetZoom(-2, 20));

		scene1.add(new ActionCreateActor(2, new ActorFancyPanel(Minecraft.getInstance().font, 0, 0, new Object[][] {{resolveKey("cannery.centrifuge.4")}}, 200)
				.setColors(colorCopper).setOrientation(Orientation.CENTER)));

		scene1.add(new ActionWait(60));
		scene1.add(new ActionRemoveActor(2));

		CompoundTag c3 = new CompoundTag(); c3.putDouble("x", 3); c3.putDouble("y", 1); c3.putDouble("z", 2); c3.putInt("rotation", 2);
		scene1.add(new ActionCreateActor(2, new ActorTileEntity(new ActorGasCent(), c3).smoothPlace()));
		scene1.add(new ActionWait(10));
		CompoundTag c4 = new CompoundTag(); c4.putDouble("x", 2); c4.putDouble("y", 1); c4.putDouble("z", 2); c4.putInt("rotation", 2);
		scene1.add(new ActionCreateActor(3, new ActorTileEntity(new ActorGasCent(), c4).smoothPlace()));
		scene1.add(new ActionWait(10));

		scene1.add(new ActionCreateActor(4, new ActorFancyPanel(Minecraft.getInstance().font, 0, 0, new Object[][] {{resolveKey("cannery.centrifuge.5")}}, 200)
				.setColors(colorCopper).setOrientation(Orientation.CENTER)));

		scene1.add(new ActionWait(60));
		scene1.add(new ActionRemoveActor(4));

		// The original 1.7.10 nugget_u238 / nugget_u235 items map to the material system's
		// nugget_u238 / nugget_u235 (same ids, registered from ModMaterials.URANIUM238/235).
		scene1.add(new ActionCreateActor(4, new ActorFancyPanel(Minecraft.getInstance().font, 28, -30, new Object[][] {{new ItemStack(ModItems.UPGRADE_GC_SPEED.get())}}, 0)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

		scene1.add(new ActionCreateActor(5, new ActorFancyPanel(Minecraft.getInstance().font, 45, 35, new Object[][] {{" = ",
				ModMaterialItems.stack(ModMaterials.URANIUM238, MaterialShape.NUGGET, 11),
				ModMaterialItems.stack(ModMaterials.URANIUM235, MaterialShape.NUGGET, 1)}}, 0)
				.setColors(colorCopper).setOrientation(Orientation.LEFT)));

		script.addScene(scene0).addScene(scene1);
		return script;
	}

	/** Original {@code I18nUtil.resolveKey}: resolves the key immediately for the panel lines. */
	private static String resolveKey(String key) {
		return net.minecraft.client.resources.language.I18n.get(key);
	}

	/** A fluid duct state with explicit connections and the matching render shape. */
	private static BlockState duct(boolean north, boolean south, boolean east, boolean west, boolean up, boolean down) {
		return ModBlocks.FLUID_DUCT.get().defaultBlockState()
				.setValue(com.hbm_m.block.machines.FluidDuctBlock.NORTH, north)
				.setValue(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, south)
				.setValue(com.hbm_m.block.machines.FluidDuctBlock.EAST, east)
				.setValue(com.hbm_m.block.machines.FluidDuctBlock.WEST, west)
				.setValue(com.hbm_m.block.machines.FluidDuctBlock.UP, up)
				.setValue(com.hbm_m.block.machines.FluidDuctBlock.DOWN, down)
				.setValue(com.hbm_m.block.machines.FluidDuctBlock.SHAPE,
						com.hbm_m.block.machines.PipeRenderShape.fromConnections(north, south, east, west, up, down));
	}

	/**
	 * Renders the gas centrifuge through its registered Nucleus {@code MachineBer}: the
	 * {@code Centrifuge} rotor and {@code Flag} parts are the same code path as in-world
	 * (the rotor joint reads {@code getAnim}, which is the original's static no-spin pose for
	 * a never-ticked fake BE).
	 * <p>
	 * The pose is block-corner based; the original demo's yaw (from the {@code rotation} code)
	 * is applied about the block center, which is the model-space origin after the spec's
	 * {@code blockTransform} - so an unrotated actor is pixel-identical to the in-world
	 * dispatcher pose. The fake BE lives at virtual coordinates offset far beyond the world
	 * border, so its {@code BlockPos.asLong()} roster/cache keys can never collide with a
	 * real machine.
	 */
	public static class ActorGasCent implements ITileActorRenderer {

		/** Original rotation code (3:0°, 5:90°, 2:180°, 4:270°) → yaw in degrees; other codes drew unrotated. */
		static final int[] YAW_MAP = {0, 0, 180, 0, 270, 90};

		/** X offset isolating the fake BE's pos-keyed Nucleus state (roster slots, caches) from real machines. */
		static final int GUI_POS_OFFSET = 30_000_000;

		private MachineGasCentBlockEntity gascent;
		private int lastRotation = -1;

		@Override
		public void renderActor(GuiGraphics gui, VirtualWorld world, int ticks, float interp, CompoundTag data) {
			double x = data.getDouble("x");
			double y = data.getDouble("y");
			double z = data.getDouble("z");
			int rotation = data.getInt("rotation");

			gui.pose().pushPose();
			gui.pose().translate(x, y, z);
			// Original switch had no default: any code outside {2,3,4,5} drew unrotated.
			int yaw = (rotation >= 0 && rotation < YAW_MAP.length) ? YAW_MAP[rotation] : 0;
			// yaw about the block center = the model-space origin after the spec's blockTransform
			gui.pose().translate(0.5D, 0.0D, 0.5D);
			gui.pose().mulPose(com.mojang.math.Axis.YP.rotationDegrees(yaw));
			gui.pose().translate(-0.5D, 0.0D, -0.5D);

			if(this.gascent == null || this.lastRotation != rotation) {
				this.lastRotation = rotation;
				// FACING=WEST: the gascent spec's blockTransform is IDENTITY for WEST, so the
				// only rotation is the demo's yaw map above - 1:1 with the original demo actor.
				// defaultBlockState() (FACING=NORTH) would add +270 deg on top and put every
				// machine 90 deg off, pointing the connector flag at the wrong side.
				this.gascent = new MachineGasCentBlockEntity(
						new BlockPos((int) x + GUI_POS_OFFSET, (int) y, (int) z),
						ModBlocks.MACHINE_GASCENT.get().defaultBlockState()
								.setValue(com.hbm_m.block.machines.MachineGasCentBlock.FACING, net.minecraft.core.Direction.WEST));
			}

			BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
			BlockEntityRenderer<BlockEntity> ber = dispatcher.getRenderer(this.gascent);
			if(ber != null) {
				ber.render(this.gascent, interp, gui.pose(), gui.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
			}

			gui.pose().popPose();
		}

		@Override
		public void updateActor(int ticks, CompoundTag data) { }
	}
}
