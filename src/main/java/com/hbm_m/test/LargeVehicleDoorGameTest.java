package com.hbm_m.test;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.doors.DoorDecl;
import com.hbm_m.client.model.variant.DoorSkin;
import com.hbm_m.item.ModItems;
import com.hbm_m.multiblock.PartRole;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.util.CompletionStatus;
import com.hbm_m.util.CompletionTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

/**
 * GameTest-верификация 1:1 паритета Large Vehicle Door с 1.7.10 (Milestone M8: large_vehicle_door).
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class LargeVehicleDoorGameTest {

    private static final float EPS = 1.0E-4F;

    private LargeVehicleDoorGameTest() {
    }

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    @GameTest(template = "empty3x3x3", batch = "large_vehicle_door", timeoutTicks = 100)
    public static void largeVehicleDoorPropertiesAndParity(GameTestHelper helper) {
        // 1. Hardness 10.0F, Blast Resistance 1000.0F (1.7.10 parity)
        float resistance = ModBlocks.LARGE_VEHICLE_DOOR.get().getExplosionResistance();
        check(Math.abs(resistance - 1000.0F) < EPS,
                "Large Vehicle Door resistance must be 1000.0F like in 1.7.10, got " + resistance);

        float destroySpeed = ModBlocks.LARGE_VEHICLE_DOOR.get().defaultDestroyTime();
        check(Math.abs(destroySpeed - 10.0F) < EPS,
                "Large Vehicle Door destroy speed must be 10.0F like in 1.7.10, got " + destroySpeed);

        // 2. Completion status COMPLETE on both block and item
        check(CompletionTracker.getStatus(ModBlocks.LARGE_VEHICLE_DOOR.getId()) == CompletionStatus.COMPLETE,
                "ModBlocks.LARGE_VEHICLE_DOOR must be marked CompletionStatus.COMPLETE");
        check(CompletionTracker.getStatus(ModItems.LARGE_VEHICLE_DOOR.getId()) == CompletionStatus.COMPLETE,
                "ModItems.LARGE_VEHICLE_DOOR must be marked CompletionStatus.COMPLETE");

        // 3. DoorDecl structure: 7x6 footprint (x in [-3, 3], y in [0, 5], z = 0) -> 42 blocks
        DoorDecl decl = DoorDecl.LARGE_VEHICLE_DOOR;
        DoorDecl.DoorStructureDefinition def = decl.getStructureDefinition();
        check(def != null, "Large Vehicle Door structure definition must be defined");

        Map<BlockPos, VoxelShape> closedShapes = def.getClosedShapes();
        Map<BlockPos, VoxelShape> openShapes = def.getOpenShapes();
        Map<BlockPos, PartRole> roles = def.getRoles();

        check(roles.size() == 42, "Large Vehicle Door must define exactly 42 blocks in footprint, got " + roles.size());

        for (int x = -3; x <= 3; x++) {
            for (int y = 0; y <= 5; y++) {
                BlockPos pos = new BlockPos(x, y, 0);
                check(roles.containsKey(pos), "Footprint must contain block at " + pos);
                check(closedShapes.containsKey(pos) && !closedShapes.get(pos).isEmpty(),
                        "Closed state must be solid at " + pos);

                if (y == 0) {
                    // Floor row: passage x in [-2, 2] has thin threshold plate M; x = -3 and x = 3 have corner steps S/R
                    VoxelShape openShape = openShapes.get(pos);
                    check(openShape != null && !openShape.isEmpty(),
                            "Floor threshold/step at " + pos + " must have collision when open");
                } else if (y >= 1 && y <= 4) {
                    // Central doorway: x in [-2, 2] is completely open passage (O: Shapes.empty())
                    // Columns x = -3 and x = 3 have vertical side pillars H and G
                    VoxelShape openShape = openShapes.get(pos);
                    if (x >= -2 && x <= 2) {
                        check(openShape == null || openShape.isEmpty(),
                                "Passage at " + pos + " must be empty when open");
                    } else {
                        check(openShape != null && !openShape.isEmpty(),
                                "Side pillar at " + pos + " must have frame collision when open");
                    }
                } else if (y == 5) {
                    // Header row: upper frame collision T for x in [-2, 2]; corner blocks X for x = -3 and x = 3
                    VoxelShape openShape = openShapes.get(pos);
                    check(openShape != null && !openShape.isEmpty(),
                            "Top row header at " + pos + " must have upper frame collision when open");
                }
            }
        }

        // Controller at (0, 0, 0)
        check(roles.get(BlockPos.ZERO) == PartRole.CONTROLLER,
                "Controller must be located at (0, 0, 0)");

        // 4. Timing: openTime = 60 ticks (3.0 seconds, 1.7.10 parity)
        check(decl.getOpenTime() == 60, "Large Vehicle Door open time must be 60 ticks (3.0 seconds)");

        // 5. Door open ranges: {{ 0, 0, 0, -4, 6, 2 }, { 0, 0, 0, 4, 6, 2 }}
        int[][] ranges = decl.getDoorOpenRanges();
        check(ranges.length == 2, "Door open ranges must have length 2");
        check(ranges[0][0] == 0 && ranges[0][1] == 0 && ranges[0][2] == 0
                        && ranges[0][3] == -4 && ranges[0][4] == 6 && ranges[0][5] == 2,
                "Range 0 must be {0, 0, 0, -4, 6, 2}");
        check(ranges[1][0] == 0 && ranges[1][1] == 0 && ranges[1][2] == 0
                        && ranges[1][3] == 4 && ranges[1][4] == 6 && ranges[1][5] == 2,
                "Range 1 must be {0, 0, 0, 4, 6, 2}");

        // Retraction progression along X (axis 2, steps = 4, threshold = j / 4.0f):
        // j=0 (x=0) -> threshold 0.0f
        // j=1 (x=±1) -> threshold 0.25f
        // j=2 (x=±2) -> threshold 0.50f
        // j=3 (x=±3) -> threshold 0.75f

        // Progress 0.0f: nothing retracted
        for (int x = -3; x <= 3; x++) {
            for (int y = 0; y <= 5; y++) {
                check(!decl.isPartRetracted(new BlockPos(x, y, 0), 0.0f),
                        "At 0.0 progress (" + x + ", " + y + ") must not be retracted");
            }
        }

        // Progress 0.1f (> 0.0): column x = 0 is retracted; x != 0 are NOT retracted
        check(decl.isPartRetracted(new BlockPos(0, 2, 0), 0.1f), "At 0.1 progress x=0 must be retracted");
        check(!decl.isPartRetracted(new BlockPos(-1, 2, 0), 0.1f), "At 0.1 progress x=-1 must not be retracted");
        check(!decl.isPartRetracted(new BlockPos(1, 2, 0), 0.1f), "At 0.1 progress x=1 must not be retracted");
        check(!decl.isPartRetracted(new BlockPos(-2, 2, 0), 0.1f), "At 0.1 progress x=-2 must not be retracted");
        check(!decl.isPartRetracted(new BlockPos(2, 2, 0), 0.1f), "At 0.1 progress x=2 must not be retracted");
        check(!decl.isPartRetracted(new BlockPos(-3, 2, 0), 0.1f), "At 0.1 progress x=-3 must not be retracted");
        check(!decl.isPartRetracted(new BlockPos(3, 2, 0), 0.1f), "At 0.1 progress x=3 must not be retracted");

        // Progress 0.3f (> 0.25): columns x in [-1, 1] are retracted; |x| >= 2 are NOT retracted
        check(decl.isPartRetracted(new BlockPos(0, 2, 0), 0.3f), "At 0.3 progress x=0 must be retracted");
        check(decl.isPartRetracted(new BlockPos(-1, 2, 0), 0.3f), "At 0.3 progress x=-1 must be retracted");
        check(decl.isPartRetracted(new BlockPos(1, 2, 0), 0.3f), "At 0.3 progress x=1 must be retracted");
        check(!decl.isPartRetracted(new BlockPos(-2, 2, 0), 0.3f), "At 0.3 progress x=-2 must not be retracted");
        check(!decl.isPartRetracted(new BlockPos(2, 2, 0), 0.3f), "At 0.3 progress x=2 must not be retracted");
        check(!decl.isPartRetracted(new BlockPos(-3, 2, 0), 0.3f), "At 0.3 progress x=-3 must not be retracted");
        check(!decl.isPartRetracted(new BlockPos(3, 2, 0), 0.3f), "At 0.3 progress x=3 must not be retracted");

        // Progress 0.6f (> 0.50): columns x in [-2, 2] are retracted; |x| == 3 are NOT retracted
        for (int x = -2; x <= 2; x++) {
            check(decl.isPartRetracted(new BlockPos(x, 2, 0), 0.6f),
                    "At 0.6 progress x=" + x + " must be retracted");
        }
        check(!decl.isPartRetracted(new BlockPos(-3, 2, 0), 0.6f), "At 0.6 progress x=-3 must not be retracted");
        check(!decl.isPartRetracted(new BlockPos(3, 2, 0), 0.6f), "At 0.6 progress x=3 must not be retracted");

        // Progress 0.8f (> 0.75): all columns x in [-3, 3] are retracted
        for (int x = -3; x <= 3; x++) {
            check(decl.isPartRetracted(new BlockPos(x, 2, 0), 0.8f),
                    "At 0.8 progress x=" + x + " must be retracted");
        }

        // Progress 1.0f: all columns x in [-3, 3] retracted
        for (int x = -3; x <= 3; x++) {
            for (int y = 0; y <= 5; y++) {
                check(decl.isPartRetracted(new BlockPos(x, y, 0), 1.0f),
                        "At 1.0 progress (" + x + ", " + y + ") must be retracted");
            }
        }

        // 6. Skins: 6 skins (clear/Standard default, default/Clean, rad, black, blue, red)
        check(decl.hasSkins(), "Large Vehicle Door must declare hasSkins = true");
        check(decl.getSkinCount() == 6, "Large Vehicle Door skin count must be 6, got " + decl.getSkinCount());
        check("clear".equals(decl.getDefaultModelSelection().getSkin().getId()),
                "Large Vehicle Door default skin must be clear, got " + decl.getDefaultModelSelection().getSkin().getId());

        DoorSkin[] skins = decl.getSkins();
        check(skins != null && skins.length == 6, "Large Vehicle Door skins array must have length 6");
        check("clear".equals(skins[0].getId()), "Skin 0 must be clear");
        check("default".equals(skins[1].getId()), "Skin 1 must be default");
        check("rad".equals(skins[2].getId()), "Skin 2 must be rad");
        check("black".equals(skins[3].getId()), "Skin 3 must be black");
        check("blue".equals(skins[4].getId()), "Skin 4 must be blue");
        check("red".equals(skins[5].getId()), "Skin 5 must be red");

        // 7. Sounds: garage_move loop, garage_stop end, volume 2.0F
        check(Math.abs(decl.getSoundVolume() - 2.0f) < EPS, "Sound volume must be 2.0, got " + decl.getSoundVolume());
        check(decl.getOpenSoundLoop() == ModSounds.GARAGE_MOVE.get(), "Open sound loop must be garage_move");
        check(decl.getCloseSoundLoop() == ModSounds.GARAGE_MOVE.get(), "Close sound loop must be garage_move");
        check(decl.getOpenSoundEnd() == ModSounds.GARAGE_STOP.get(), "Open sound end must be garage_stop");
        check(decl.getCloseSoundEnd() == ModSounds.GARAGE_STOP.get(), "Close sound end must be garage_stop");
        check(decl.getOpenSoundStart() == null, "Open sound start must be null");
        check(decl.getCloseSoundStart() == null, "Close sound start must be null");

        // 8. Animation transforms: doorLeft (+3.0 along Z) & doorRight (-3.0 along Z) biparting leaves, frame static
        float[] trans = new float[3];

        // Tick 0: 0 translation
        decl.getTranslation("doorLeft", 0f, false, trans);
        check(Math.abs(trans[0]) < EPS && Math.abs(trans[1]) < EPS && Math.abs(trans[2]) < EPS,
                "doorLeft translation at tick 0 must be (0, 0, 0)");
        decl.getTranslation("doorRight", 0f, false, trans);
        check(Math.abs(trans[0]) < EPS && Math.abs(trans[1]) < EPS && Math.abs(trans[2]) < EPS,
                "doorRight translation at tick 0 must be (0, 0, 0)");

        // Tick 30 (halfway, 30/60 = 0.5): doorLeft = +1.5, doorRight = -1.5 along Z
        decl.getTranslation("doorLeft", 30f, false, trans);
        check(Math.abs(trans[2] - 1.5f) < EPS,
                "doorLeft translation at tick 30 must be +1.5 along Z, got " + trans[2]);
        decl.getTranslation("doorRight", 30f, false, trans);
        check(Math.abs(trans[2] - (-1.5f)) < EPS,
                "doorRight translation at tick 30 must be -1.5 along Z, got " + trans[2]);

        // Tick 60 (full open, 60/60 = 1.0): doorLeft = +3.0, doorRight = -3.0 along Z
        decl.getTranslation("doorLeft", 60f, false, trans);
        check(Math.abs(trans[2] - 3.0f) < EPS,
                "doorLeft translation at tick 60 must be +3.0 along Z, got " + trans[2]);
        decl.getTranslation("doorRight", 60f, false, trans);
        check(Math.abs(trans[2] - (-3.0f)) < EPS,
                "doorRight translation at tick 60 must be -3.0 along Z, got " + trans[2]);

        // Support for 1.7.10 part names "Left" and "Right"
        decl.getTranslation("Left", 60f, false, trans);
        check(Math.abs(trans[2] - 3.0f) < EPS,
                "'Left' translation at tick 60 must be +3.0 along Z, got " + trans[2]);
        decl.getTranslation("Right", 60f, false, trans);
        check(Math.abs(trans[2] - (-3.0f)) < EPS,
                "'Right' translation at tick 60 must be -3.0 along Z, got " + trans[2]);

        // Frame part must remain static at (0, 0, 0)
        decl.getTranslation("frame", 60f, false, trans);
        check(Math.abs(trans[0]) < EPS && Math.abs(trans[1]) < EPS && Math.abs(trans[2]) < EPS,
                "Frame part must not translate");

        // 9. Dynamic shape & clipping planes
        check(decl.isDynamicShape(), "Large Vehicle Door must be dynamic shape");
        double[][] clip = decl.getClippingPlanes();
        check(clip != null && clip.length == 2, "Clipping planes must be defined with length 2");
        check(clip[0][2] == 1.0 && clip[0][3] == 3.5, "Plane 0 must be +Z at 3.5");
        check(clip[1][2] == -1.0 && clip[1][3] == 3.5, "Plane 1 must be -Z at 3.5");

        helper.succeed();
    }
}
