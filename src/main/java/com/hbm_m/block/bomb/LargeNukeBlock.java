package com.hbm_m.block.bomb;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.bomb.IBomb;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.bomb.LargeNukeBlockEntity;
import com.hbm_m.explosion.NuclearExplosionAPI;
import com.hbm_m.interfaces.IDetonatable;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Большая ядерная бомба (Gadget / Little Boy / Ivy Mike / Tsar Bomba).
 * Один класс блока на все типы, вариант определяется полем type.
 */
public class LargeNukeBlock extends NukeBaseBlock implements IMultiblockController, IBomb, IDetonatable {

    public final LargeNukeType type;
    private final MultiblockStructureHelper structureHelper;

    public LargeNukeBlock(LargeNukeType type, Properties properties) {
        super(properties);
        this.type = type;
        this.structureHelper = defineStructure(type);
    }

    @Override
    public boolean onDetonate(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide) return false;
        BombReturnCode result = explode(level, pos);
        return result != null && result.wasSuccessful();
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide && level.getBlockState(pos).getBlock() == this && level.hasNeighborSignal(pos)) {
            explode(level, pos);
        }
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        return structureHelper;
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        if (structureHelper != null) {
            return structureHelper.resolvePartRole(localOffset, this);
        }
        return PartRole.DEFAULT;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LargeNukeBlockEntity(pos, state, type);
    }

    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.MODEL;
    }

    /** Резервный путь (радиус по типу), основной — через переопределённый explode(BlockPos). */
    @Override
    protected void explode(Level level, double x, double y, double z) {
        NuclearExplosionAPI.startLargeNuke(level, x, y, z,
                type.detonationRadius(new net.minecraft.world.item.ItemStack[0]));
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos) {
        if (level.isClientSide) return BombReturnCode.UNDEFINED;
        if (level.getBlockEntity(pos) instanceof LargeNukeBlockEntity nuke) {
            // Радиус считаем ДО очистки контейнера (Tsar без tsar_core бьёт по радиусу Fat Man).
            net.minecraft.world.item.ItemStack[] items = nuke.slots.toArray(new net.minecraft.world.item.ItemStack[0]);
            int radius = type.detonationRadius(items);
            if (nuke.isReady()) {
                // Original clearSlots: die Bombenteile werden verbraucht, nicht fallengelassen
                nuke.clearContent();
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                NuclearExplosionAPI.startLargeNuke(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, radius);
                return BombReturnCode.DETONATED;
            }
            return BombReturnCode.ERROR_MISSING_COMPONENT;
        }
        return BombReturnCode.UNDEFINED;
    }

    private static MultiblockStructureHelper defineStructure(LargeNukeType type) {
        String[][] layers = switch (type) {
            case GADGET -> new String[][] {
                {
                    "AAA",
                    "ACA",
                    "AAA"
                },
                {
                    "AAA",
                    "AAA",
                    "AAA"
                }
            };
            case BOY -> new String[][] {
                {
                    "ACA"
                }
            };
            case MIKE -> new String[][] {
                { "C" },
                { "A" },
                { "A" },
                { "A" },
                { "A" },
                { "A" }
            };
            case TSAR -> new String[][] {
                {
                    "AAAAAAAA",
                    "AAAACAAA",
                    "AAAAAAAA"
                },
                {
                    "AAAAAAAA",
                    "AAAAAAAA",
                    "AAAAAAAA"
                }
            };
        };

        Map<Character, PartRole> roleMap = Map.of(
            'A', PartRole.DEFAULT,
            'C', PartRole.CONTROLLER
        );

        Map<Character, VoxelShape> shapeMap = Map.of(
            'A', Shapes.block(),
            'C', Shapes.block()
        );
        Map<Character, VoxelShape> collisionMap = Map.of(
            'A', Shapes.block(),
            'C', Shapes.block()
        );

        return MultiblockStructureHelper.createFromLayersWithRoles(
            layers,
            Map.of(),
            () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
            roleMap,
            shapeMap,
            collisionMap
        );
    }
}
