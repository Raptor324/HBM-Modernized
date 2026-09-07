package com.hbm_m.blockentity.machines.fusion;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.fusion.FusionMultiblockBlock;
import com.hbm_m.block.machines.fusion.MachineFusionTorusBlock;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1-Port von {@code TileEntityFusionTorusStruct} (1.7.10).
 *
 * <p>Der Torus-Kern prueft im Sekundentakt, ob rundherum die komplette Torusform aus den richtigen
 * Fusionsbauteilen steht. Sobald das der Fall ist, verwandelt er sich selbst in den fertigen
 * {@code fusion_torus} - der beim Setzen die gesamte Struktur uebernimmt.</p>
 */
public class StructTorusCoreBlockEntity extends BlockEntity {

    public StructTorusCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STRUCT_TORUS_CORE_BE.get(), pos, state);
    }

    /** Bauteil, das an einer Stelle des Musters stehen muss (Original: Metadaten 1/2/3). */
    private static Block componentForLayoutValue(int value) {
        return switch (value) {
            case 1 -> ModBlocks.FUSION_COMPONENT_BSCCO_WELDED.get();
            case 2 -> ModBlocks.FUSION_COMPONENT_BLANKET.get();
            case 3 -> ModBlocks.FUSION_COMPONENT_MOTOR.get();
            default -> null;
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, StructTorusCoreBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (level.getGameTime() % 20 != 0) return;
        be.checkAndAssemble(serverLevel, pos);
    }

    private void checkAndAssemble(ServerLevel level, BlockPos pos) {

        for (int y = 0; y < 5; y++) {
            for (int x = -7; x <= 7; x++) {
                for (int z = -7; z <= 7; z++) {

                    int value = MachineFusionTorusBlock.layoutAt(x, y, z);
                    if (value == 0) continue;                        // Luft ignorieren
                    if (x == 0 && y == 0 && z == 0) continue;        // die eigene Position ignorieren

                    Block expected = componentForLayoutValue(value);
                    if (expected == null) continue;
                    if (!level.getBlockState(pos.offset(x, y, z)).is(expected)) return;
                }
            }
        }

        // Original: setBlock(..., fusion_torus, 12, 3) + fillSpace(..., ForgeDirection.NORTH, 0)
        level.setBlock(pos, ModBlocks.TORUS.get().defaultBlockState()
                .setValue(FusionMultiblockBlock.FACING, Direction.NORTH), 3);
    }

    private AABB renderBounds = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (renderBounds == null) {
            renderBounds = new AABB(
                    worldPosition.getX() - 7, worldPosition.getY(), worldPosition.getZ() - 7,
                    worldPosition.getX() + 8, worldPosition.getY() + 5, worldPosition.getZ() + 8);
        }
        return renderBounds;
    }
}
