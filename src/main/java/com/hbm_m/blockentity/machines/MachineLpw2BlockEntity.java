package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * LPW2 - Port von {@code TileEntityMachineLPW2} (1.7.10 Original). Rein dekorativ - das Original
 * selbst hat keinerlei Logik (kein Inventar, kein Strom, keine Fluids), nur eine grosse
 * Render-Bounding-Box fuer seinen aufwendig animierten TESR ({@code RenderLPW2}: schwingende
 * Kabel, rotierender Rotor/Turbine, klappernde Shroud-Lamellen, flackernder Fehlerbildschirm).
 * Diese Teil-fuer-Teil-Animation entfaellt hier ersatzlos (rein kosmetisch, kein Gameplay-Effekt,
 * Aufwand ausser Verhaeltnis) - das Modell wird stattdessen statisch ueber ein composite-OBJ
 * gerendert, analog zur Rotor-Animation die bereits bei {@code MachineSteamEngineBlockEntity}
 * entfiel.
 */
public class MachineLpw2BlockEntity extends BlockEntity {
    public MachineLpw2BlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LPW2_BE.get(), pos, state);
    }
}
