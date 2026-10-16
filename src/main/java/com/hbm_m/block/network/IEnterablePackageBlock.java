package com.hbm_m.block.network;

import com.hbm_m.entity.conveyor.MovingConveyorPackageEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Port des {@code IConveyorPackage}-Teils von {@code api.hbm.conveyor.IEnterableBlock} (1.7.10
 * Original). Eigenstaendiges Interface statt einer zweiten Methode auf {@link IEnterableBlock},
 * da letzteres bereits dokumentiert als "keine Paket-Nutzlast in diesem Port" eingefuehrt und von
 * mehreren Crane-Bloecken (Inserter/Extractor/Grabber/Router) implementiert wurde, bevor Boxer/
 * Unboxer (die einzigen Nutzer von {@link MovingConveyorPackageEntity}) hinzukamen - eine
 * nachtraegliche Erweiterung des bestehenden Interfaces haette alle bereits fertigen Implementierer
 * zu einer No-Op-Methode gezwungen.
 */
public interface IEnterablePackageBlock {
    void onPackageEnter(Level level, BlockPos pos, MovingConveyorPackageEntity item);
}
