package com.hbm_m.multiblock;

// Утилитарный класс для управления мультиблочными структурами.
// Позволяет определять структуру, проверять возможность постройки, строить и разрушать структуру,
// а также генерировать VoxelShape для всей структуры. Ядро всей мультиблочной логики.

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.network.HighlightBlocksPacket;
import com.hbm_m.network.ModPacketHandler;

public class MultiblockStructureHelper {

    private final Map<BlockPos, Supplier<BlockState>> structureMap;
    private final Supplier<BlockState> phantomBlockState;

    public MultiblockStructureHelper(Map<BlockPos, Supplier<BlockState>> structureMap, Supplier<BlockState> phantomBlockState) {
        this.structureMap = structureMap;
        this.phantomBlockState = phantomBlockState;
    }
    
    private final Set<Block> replaceableBlocks = Set.of(
        Blocks.AIR, Blocks.CAVE_AIR, Blocks.VOID_AIR, Blocks.SNOW, Blocks.VINE, Blocks.WATER, Blocks.LAVA
    );
    
    // Новый приватный метод для проверки, можно ли заменить блок
    private boolean isBlockReplaceable(BlockState state) {
        // Сначала проверяем наш базовый Set
        if (replaceableBlocks.contains(state.getBlock())) {
            return true;
        }
        // Теперь проверяем по тегам. Это включает все цветы, траву, саженцы и т.д.
        return state.is(BlockTags.REPLACEABLE_BY_TREES) || // Трава, папоротники
               state.is(BlockTags.FLOWERS) ||              // Все виды цветов
               state.is(BlockTags.SAPLINGS);               // Саженцы деревьев
    }

    public boolean checkPlacement(Level level, BlockPos controllerPos, Direction facing, Player player) {
        List<BlockPos> obstructions = new ArrayList<>();

        for (BlockPos relativePos : structureMap.keySet()) {
            if (relativePos.equals(BlockPos.ZERO)) continue;

            BlockPos worldPos = getRotatedPos(controllerPos, relativePos, facing);
            BlockState existingState = level.getBlockState(worldPos);

            // Используем наш новый метод для проверки
            if (!isBlockReplaceable(existingState)) {
                obstructions.add(worldPos);
            }
        }

        if (!obstructions.isEmpty()) {
            if (player instanceof ServerPlayer serverPlayer) {
                // Проверяем, включена ли опция в конфиге, перед отправкой пакета
                if (ModClothConfig.get().enableObstructionHighlight) {
                    ModPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new HighlightBlocksPacket(obstructions));
                }
                player.displayClientMessage(Component.translatable("chat.hbm_m.structure.obstructed"), true);
            }
            return false;
        }
        return true;
    }

    /**
     * @return A Set of all local offsets for the multiblock parts, relative to the controller.
     */
    public Set<BlockPos> getPartOffsets() {
        return this.structureMap.keySet();
    }

    public void placeStructure(Level level, BlockPos controllerPos, Direction facing, IMultiblockController controller) {

        if (level.isClientSide) return;
        for (Map.Entry<BlockPos, Supplier<BlockState>> entry : structureMap.entrySet()) {
            BlockPos relativePos = entry.getKey();
            if (relativePos.equals(BlockPos.ZERO)) continue;

            BlockPos worldPos = getRotatedPos(controllerPos, relativePos, facing);
            BlockState partState = phantomBlockState.get().setValue(HorizontalDirectionalBlock.FACING, facing);

            level.setBlock(worldPos, partState, 3);

            // Force neighbor updates and ensure a block update packet is sent.
            level.updateNeighborsAt(worldPos, partState.getBlock());
            level.sendBlockUpdated(worldPos, partState, partState, 3);

            BlockEntity be = level.getBlockEntity(worldPos);
            if (be instanceof IMultiblockPart partBe) {
                partBe.setControllerPos(controllerPos);
                
                PartRole role = controller.getPartRole(relativePos);
                partBe.setPartRole(role);

                // NEW: после назначения роли сразу форсим явные обновления соседних позиций
                for (Direction d : Direction.values()) {
                    BlockPos neigh = worldPos.relative(d);
                    BlockState ns = level.getBlockState(neigh);
                    level.sendBlockUpdated(neigh, ns, ns, 3);
                    level.updateNeighborsAt(neigh, ns.getBlock());
                }

                // NEW: Принудительно пересчитать состояния соседних проводов и установить их BlockState на сервере.
                // Это гарантированно отправит обновления клиентам и обновит визуальную модель провода.
                for (Direction neighDir : Direction.values()) {
                    BlockPos neighPos = worldPos.relative(neighDir);
                    BlockState neighState = level.getBlockState(neighPos);
                    if (neighState.getBlock() instanceof com.hbm_m.block.WireBlock wire) {
                        BlockState recalculated = wire.defaultBlockState();
                        for (Direction connDir : Direction.values()) {
                            boolean can = wire.canConnectTo(level, neighPos, connDir);
                            recalculated = recalculated.setValue(com.hbm_m.block.WireBlock.PROPERTIES_MAP.get(connDir), can);
                        }
                        if (!recalculated.equals(neighState)) {
                            level.setBlock(neighPos, recalculated, 3);
                        } else {
                            // Если state не изменился — всё равно шлём апдейт, чтобы быть уверенными на клиенте
                            level.sendBlockUpdated(neighPos, neighState, neighState, 3);
                        }
                    }
                }
            }
        }
    }

    public void destroyStructure(Level level, BlockPos controllerPos, Direction facing) {
        if (level.isClientSide) return;

        // Destroy the controller itself if it still exists
        if (level.getBlockState(controllerPos).getBlock() instanceof IMultiblockController) {
            level.setBlock(controllerPos, Blocks.AIR.defaultBlockState(), 3);
        }
        
        // Iterate through all parts of the structure
        for (BlockPos relativePos : structureMap.keySet()) {
            if (relativePos.equals(BlockPos.ZERO)) continue; // Skip controller position

            BlockPos worldPos = getRotatedPos(controllerPos, relativePos, facing);
            BlockState stateInWorld = level.getBlockState(worldPos);
            
            // If the block in the world is a phantom block part, remove it.
            if (stateInWorld.getBlock() instanceof com.hbm_m.block.UniversalMachinePartBlock) {
                level.setBlock(worldPos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    /**
     * Automatically generates a VoxelShape for the structure by combining
     * 1x1x1 cubes for each part. Ideal for non-rectangular structures.
     * @param facing The direction of the structure.
     * @return The generated VoxelShape.
     */
    public VoxelShape generateShapeFromParts(Direction facing) {
        VoxelShape finalShape = Shapes.empty();

        // Include the controller in the shape
        finalShape = Shapes.or(finalShape, Block.box(0, 0, 0, 16, 16, 16));

        for (BlockPos localOffset : structureMap.keySet()) {
            if (localOffset.equals(BlockPos.ZERO)) continue;

            BlockPos rotatedOffset = rotate(localOffset, facing);

            // Create a VoxelShape for a single block and move it
            VoxelShape partShape = Block.box(0, 0, 0, 16, 16, 16)
                                        .move(rotatedOffset.getX(), rotatedOffset.getY(), rotatedOffset.getZ());

            // Combine it with the main shape
            finalShape = Shapes.or(finalShape, partShape);
        }
        return finalShape.optimize();
    }

    public BlockPos getRotatedPos(BlockPos controllerPos, BlockPos localOffset, Direction facing) {
        return controllerPos.offset(rotate(localOffset, facing));
    }

    public static BlockPos rotate(BlockPos pos, Direction facing) {
        return switch (facing) {
            case SOUTH -> new BlockPos(-pos.getX(), pos.getY(), -pos.getZ());
            case WEST -> new BlockPos(pos.getZ(), pos.getY(), -pos.getX());
            case EAST -> new BlockPos(-pos.getZ(), pos.getY(), pos.getX());
            default -> pos; // NORTH
        };
    }
}