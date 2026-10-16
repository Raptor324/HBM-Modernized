package com.hbm_m.block.machines.fusion;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.fusion.FusionTorusBlockEntity;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-Port von {@code MachineFusionTorus} (1.7.10) - der Fusionstorus selbst.
 *
 * <p>Anders als die uebrigen Maschinen fuellt das Original hier keine Quader, sondern genau die
 * Zellen des {@link #LAYOUT}-Musters: eine 15x15-Scheibe, die ueber fuenf Ebenen gespiegelt wird
 * ({@code l = iy > 2 ? 4 - iy : iy}). Der Wert je Zelle bestimmt beim Zusammenbau durch den
 * Torus-Kern, welches Bauteil dort stehen muss (1 = geschweisste BSCCO-Spule, 2 = Blanket,
 * 3 = Rohre/Motor), fuer die Struktur selbst zaehlt nur "belegt oder nicht".</p>
 */
public class MachineFusionTorusBlock extends FusionMultiblockBlock {

    /** 1:1 aus {@code MachineFusionTorus.layout}. Indizierung: {@code [ebene][x][z]}. */
    public static final int[][][] LAYOUT = new int[][][] {

        new int[][] {
            new int[] {0,0,0,0,3,3,3,3,3,3,3,0,0,0,0},
            new int[] {0,0,0,3,1,1,1,1,1,1,1,3,0,0,0},
            new int[] {0,0,3,1,1,1,1,1,1,1,1,1,3,0,0},
            new int[] {0,3,1,1,1,1,1,1,1,1,1,1,1,3,0},
            new int[] {3,1,1,1,1,3,3,3,3,3,1,1,1,1,3},
            new int[] {3,1,1,1,3,3,3,3,3,3,3,1,1,1,3},
            new int[] {3,1,1,1,3,3,3,3,3,3,3,1,1,1,3},
            new int[] {3,1,1,1,3,3,3,3,3,3,3,1,1,1,3},
            new int[] {3,1,1,1,3,3,3,3,3,3,3,1,1,1,3},
            new int[] {3,1,1,1,3,3,3,3,3,3,3,1,1,1,3},
            new int[] {3,1,1,1,1,3,3,3,3,3,1,1,1,1,3},
            new int[] {0,3,1,1,1,1,1,1,1,1,1,1,1,3,0},
            new int[] {0,0,3,1,1,1,1,1,1,1,1,1,3,0,0},
            new int[] {0,0,0,3,1,1,1,1,1,1,1,3,0,0,0},
            new int[] {0,0,0,0,3,3,3,3,3,3,3,0,0,0,0},
        },
        new int[][] {
            new int[] {0,0,0,0,1,1,3,3,3,1,1,0,0,0,0},
            new int[] {0,0,0,1,1,1,1,1,1,1,1,1,0,0,0},
            new int[] {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0},
            new int[] {0,1,1,2,1,1,1,1,1,1,1,2,1,1,0},
            new int[] {1,1,2,1,1,1,1,1,1,1,1,1,2,1,1},
            new int[] {1,1,2,1,1,3,3,3,3,3,1,1,2,1,1},
            new int[] {3,1,2,1,1,3,3,3,3,3,1,1,2,1,3},
            new int[] {3,1,2,1,1,3,3,3,3,3,1,1,2,1,3},
            new int[] {3,1,2,1,1,3,3,3,3,3,1,1,2,1,3},
            new int[] {1,1,2,1,1,3,3,3,3,3,1,1,2,1,1},
            new int[] {1,1,2,1,1,1,1,1,1,1,1,1,2,1,1},
            new int[] {0,1,1,2,1,1,1,1,1,1,1,2,1,1,0},
            new int[] {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0},
            new int[] {0,0,0,1,1,1,1,1,1,1,1,1,0,0,0},
            new int[] {0,0,0,0,1,1,3,3,3,1,1,0,0,0,0},
        },
        new int[][] {
            new int[] {0,0,0,0,1,1,3,3,3,1,1,0,0,0,0},
            new int[] {0,0,0,1,2,2,2,2,2,2,2,1,0,0,0},
            new int[] {0,0,1,2,2,2,2,2,2,2,2,2,1,0,0},
            new int[] {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0},
            new int[] {1,2,2,2,1,1,1,1,1,1,1,2,2,2,1},
            new int[] {1,2,2,2,1,3,3,3,3,3,1,2,2,2,1},
            new int[] {3,2,2,2,1,3,3,3,3,3,1,2,2,2,3},
            new int[] {3,2,2,2,1,3,3,3,3,3,1,2,2,2,3},
            new int[] {3,2,2,2,1,3,3,3,3,3,1,2,2,2,3},
            new int[] {1,2,2,2,1,3,3,3,3,3,1,2,2,2,1},
            new int[] {1,2,2,2,1,1,1,1,1,1,1,2,2,2,1},
            new int[] {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0},
            new int[] {0,0,1,2,2,2,2,2,2,2,2,2,1,0,0},
            new int[] {0,0,0,1,2,2,2,2,2,2,2,1,0,0,0},
            new int[] {0,0,0,0,1,1,3,3,3,1,1,0,0,0,0},
        }
    };

    /** Gibt den Bauteilwert an einer Position der Torusform zurueck, 0 = Luft. */
    public static int layoutAt(int localX, int y, int localZ) {
        if (y < 0 || y > 4) return 0;
        int ix = localX + 7;
        int iz = localZ + 7;
        if (ix < 0 || ix > 14 || iz < 0 || iz > 14) return 0;
        int l = y > 2 ? 4 - y : y;
        return LAYOUT[l][ix][iz];
    }

    public MachineFusionTorusBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        DummyableStructureBuilder builder = DummyableStructureBuilder.create();

        // Original: MachineFusionTorus.fillSpace - die Form ist vierfach symmetrisch, deshalb
        // spielt die Spiegelung zwischen dem 1.7.10- und dem hiesigen Koordinatensystem keine Rolle.
        for (int y = 0; y < 5; y++) {
            for (int x = -7; x <= 7; x++) {
                for (int z = -7; z <= 7; z++) {
                    if (layoutAt(x, y, z) > 0) builder.cell(-z, y, x);
                }
            }
        }

        // "is that enough ports?" - Original-Kommentar. 25 Anschlusszellen.
        builder.extra(0, 4, 0);

        for (int side : new int[] { 6, -6 }) {
            for (int off : new int[] { 0, 2, -2 }) {
                builder.extra(off, 0, side);
                builder.extra(off, 4, side);
                builder.extra(side, 0, off);
                builder.extra(side, 4, off);
            }
        }

        return builder.placementOffset(7)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    protected boolean hasMenu() {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FusionTorusBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.FUSION_TORUS_BE.get(), FusionTorusBlockEntity::tick);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineFusionTorusBlock> CODEC = simpleCodec(MachineFusionTorusBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
