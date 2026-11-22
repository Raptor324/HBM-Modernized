package com.hbm_m.wiaj.cannery;

import com.hbm_m.api.fluids.IFluidConnectorMK2;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IEnergyConnector;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Port of {@code com.hbm.wiaj.cannery.Dummies} (1.7.10). The original was an empty
 * {@code TileEntity} implementing {@code IEnergyConnectorMK2} and {@code IFluidConnectorMK2},
 * placed via {@code ActionSetTile} on top of air so the 1.7.10 ISBRH pipe/cable renderers saw a
 * connector and drew their arms into the "invisible" block - visually joining pipes to machines
 * across the scene.
 * <p>
 * Modernized mirrors this with the two modern equivalents of those interfaces:
 * {@link IEnergyConnector} (what {@code WireBlock.canVisuallyConnectTo} tests the neighbor
 * BlockEntity against) and {@link IFluidConnectorMK2} (what the tank renderers test adjacent
 * connectors against). Unlike 1.7.10, the fluid duct itself decides its connections in the
 * {@code BlockState} ({@code FluidDuctBlock#getConnectionState}), and {@code VirtualWorld} runs
 * no neighbor updates - so for duct connections the scenes set the duct's connection properties
 * explicitly (see {@code CannerySILEX#duct}); the dummy is still recognized by the cable visual
 * connect check and by machine/tank renderers that probe neighbor BlockEntities directly, which
 * is exactly the role the original dummy played.
 */
public class Dummies {

	public static class JarDummyConnector extends BlockEntity implements IEnergyConnector, IFluidConnectorMK2 {

		/**
		 * The original had the implicit no-arg TileEntity constructor; modern BlockEntity needs a
		 * type, and since 1.21 the constructor validates it ({@code validateBlockState}: a null
		 * type NPEs there - the 1.20.1 constructor ran no validation, which the port relied on).
		 * Building a fresh standalone type at demo time is not an option either: on 1.21 the
		 * BlockEntityType constructor wires built-in registry holders, and the block registry is
		 * frozen outside loading. So the dummy reuses the registered fluid-duct type with the
		 * matching block state - the role it plays is exactly a pipe/tank connector, and nothing
		 * in the demo screen ever renders, ticks or saves it, so the type is only ever touched by
		 * that constructor validation.
		 */
		public JarDummyConnector() {
			super(ModBlockEntities.FLUID_DUCT_BE.get(), BlockPos.ZERO,
					ModBlocks.FLUID_DUCT.get().defaultBlockState());
		}

		/**
		 * Original {@code IEnergyConnectorMK2.canConnect} default: {@code dir != ForgeDirection.UNKNOWN},
		 * i.e. every real side connects. {@code null} is the modern stand-in for UNKNOWN
		 * (same convention as {@link IFluidConnectorMK2#canConnect}).
		 */
		@Override
		public boolean canConnectEnergy(Direction side) {
			return side != null;
		}
	}
}
