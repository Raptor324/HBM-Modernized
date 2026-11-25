package com.hbm_m.test;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.doors.DoorDecl;
import com.hbm_m.client.model.variant.DoorModelSelection;
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
 * GameTest-верификация паритета Sliding Blast Door с 1.7.10 (M5: sliding_blast_door).
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class SlidingBlastDoorGameTest {

    private static final float EPS = 1.0E-4F;

    private SlidingBlastDoorGameTest() {
    }

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    @GameTest(template = "empty3x3x3", batch = "sliding_blast_door", timeoutTicks = 100)
    public static void slidingBlastDoorPropertiesAndParity(GameTestHelper helper) {
        // 1. Hardness 10.0F, Blast Resistance 750.0F (1.7.10 parity)
        float resistance = ModBlocks.SLIDE_DOOR.get().getExplosionResistance();
        check(Math.abs(resistance - 750.0F) < EPS,
                "Sliding Blast Door resistance must be 750.0F like in 1.7.10, got " + resistance);

        float destroySpeed = ModBlocks.SLIDE_DOOR.get().defaultDestroyTime();
        check(Math.abs(destroySpeed - 10.0F) < EPS,
                "Sliding Blast Door destroy speed must be 10.0F like in 1.7.10, got " + destroySpeed);

        // 2. Completion status COMPLETE on both block and item
        check(CompletionTracker.getStatus(ModBlocks.SLIDE_DOOR.getId()) == CompletionStatus.COMPLETE,
                "ModBlocks.SLIDE_DOOR must be marked CompletionStatus.COMPLETE");
        check(CompletionTracker.getStatus(ModItems.SLIDE_DOOR.getId()) == CompletionStatus.COMPLETE,
                "ModItems.SLIDE_DOOR must be marked CompletionStatus.COMPLETE");

        // 3. DoorDecl structure: 7x4 footprint (x in [-3, 3], y in [0, 3], z = 0)
        DoorDecl decl = DoorDecl.SLIDING_BLAST_DOOR;
        DoorDecl.DoorStructureDefinition def = decl.getStructureDefinition();
        check(def != null, "Sliding Blast Door structure definition must be defined");

        Map<BlockPos, VoxelShape> closedShapes = def.getClosedShapes();
        Map<BlockPos, VoxelShape> openShapes = def.getOpenShapes();
        Map<BlockPos, PartRole> roles = def.getRoles();

        for (int x = -3; x <= 3; x++) {
            for (int y = 0; y <= 3; y++) {
                BlockPos pos = new BlockPos(x, y, 0);
                check(roles.containsKey(pos), "Footprint must contain block at " + pos);
                check(closedShapes.containsKey(pos) && !closedShapes.get(pos).isEmpty(),
                        "Closed state must be solid at " + pos);

                boolean isPassage = (x >= -2 && x <= 2);
                if (isPassage) {
                    if (y == 1 || y == 2) {
                        VoxelShape shape = openShapes.get(pos);
                        check(shape == null || shape.isEmpty(),
                                "Interior passage at " + pos + " must be empty when open");
                    } else if (y == 0) {
                        // Low threshold (1 pixel rail)
                        VoxelShape shape = openShapes.get(pos);
                        check(shape != null && !shape.isEmpty(),
                                "Floor rail at " + pos + " must have low threshold shape");
                    } else if (y == 3) {
                        // Upper frame collision (half block)
                        VoxelShape shape = openShapes.get(pos);
                        check(shape != null && !shape.isEmpty(),
                                "Upper frame collision at " + pos + " must have half block shape");
                    }
                } else {
                    // Outer side columns (x = -3 and x = 3) must remain solid
                    check(openShapes.containsKey(pos) && !openShapes.get(pos).isEmpty(),
                            "Outer frame at " + pos + " must remain solid when open");
                }
            }
        }

        // Controller at bottom middle (0, 0, 0)
        check(roles.get(BlockPos.ZERO) == PartRole.CONTROLLER,
                "Controller must be located at (0, 0, 0)");

        // 4. Timing: openTime = 24 ticks (1.2 seconds fast heavy blast door)
        check(decl.getOpenTime() == 24, "Sliding Blast Door open time must be 24 ticks (1.2 seconds)");

        // 5. Door open ranges: {{-2, 0, 0, 4, 5, 1}}
        int[][] ranges = decl.getDoorOpenRanges();
        check(ranges.length == 1, "Door open ranges must have length 1");
        check(ranges[0][0] == -2 && ranges[0][1] == 0 && ranges[0][2] == 0
                        && ranges[0][3] == 4 && ranges[0][4] == 5 && ranges[0][5] == 1,
                "Door open range must be {{-2, 0, 0, 4, 5, 1}}");

        // Retraction progression along Y (axis 1):
        // progress 0: none retracted
        check(!decl.isPartRetracted(new BlockPos(0, 0, 0), 0.0f), "At 0.0 progress nothing retracted");
        // progress 0.1f (> 0/3): y=0 retracted, y=1,2,3 not
        check(decl.isPartRetracted(new BlockPos(0, 0, 0), 0.1f), "At 0.1 progress y=0 retracted");
        check(!decl.isPartRetracted(new BlockPos(0, 1, 0), 0.1f), "At 0.1 progress y=1 not retracted");
        // progress 0.4f (> 1/3): y=0, 1 retracted, y=2,3 not
        check(decl.isPartRetracted(new BlockPos(0, 1, 0), 0.4f), "At 0.4 progress y=1 retracted");
        check(!decl.isPartRetracted(new BlockPos(0, 2, 0), 0.4f), "At 0.4 progress y=2 not retracted");
        // progress 0.7f (> 2/3): y=0, 1, 2 retracted, y=3 not
        check(decl.isPartRetracted(new BlockPos(0, 2, 0), 0.7f), "At 0.7 progress y=2 retracted");
        check(!decl.isPartRetracted(new BlockPos(0, 3, 0), 0.7f), "At 0.7 progress y=3 not retracted");
        // progress 1.0f: all retracted
        check(decl.isPartRetracted(new BlockPos(0, 3, 0), 1.0f), "At 1.0 progress y=3 retracted");

        // 6. Skins: 2 skins (Default and Industrial)
        check(decl.hasSkins(), "Sliding Blast Door must declare hasSkins = true");
        check(decl.getSkinCount() == 2, "Sliding Blast Door skin count must be 2, got " + decl.getSkinCount());

        DoorSkin[] skins = decl.getSkins();
        check(skins != null && skins.length == 2, "Sliding Blast Door skins array must have length 2");
        check("default".equals(skins[0].getId()), "Skin 0 must be default");
        check("industrial".equals(skins[1].getId()), "Skin 1 must be industrial");

        // 7. Sounds
        check(Math.abs(decl.getSoundVolume() - 2.0f) < EPS, "Sound volume must be 2.0");
        check(decl.getOpenSoundLoop() == ModSounds.SLIDING_DOOR_OPENING.get(), "Open sound loop must be sliding_door_opening");
        check(decl.getSoundLoop2() == ModSounds.SLIDING_DOOR_OPENING.get(), "Sound loop 2 must be sliding_door_opening");
        check(decl.getCloseSoundLoop() == ModSounds.SLIDING_DOOR_OPENING.get(), "Close sound loop must be sliding_door_opening");
        check(decl.getOpenSoundEnd() == ModSounds.SLIDING_DOOR_OPENED.get(), "Open sound end must be sliding_door_opened");
        check(decl.getCloseSoundEnd() == ModSounds.SLIDING_DOOR_SHUT.get(), "Close sound end must be sliding_door_shut");

        // 8. Animation transforms
        DoorModelSelection sel = DoorModelSelection.modern(DoorSkin.DEFAULT);
        float[] orig = new float[3];
        decl.getOrigin("LeftLock", orig, sel);
        check(Math.abs(orig[0] - 0f) < EPS && Math.abs(orig[1] - 1.8125f) < EPS && Math.abs(orig[2] - 0f) < EPS,
                "LeftLock pivot origin must be (0, 1.8125, 0)");
        decl.getOrigin("RightLock", orig, sel);
        check(Math.abs(orig[0] - 0f) < EPS && Math.abs(orig[1] - 1.8125f) < EPS && Math.abs(orig[2] - 0f) < EPS,
                "RightLock pivot origin must be (0, 1.8125, 0)");

        float[] rot = new float[3];
        float[] trans = new float[3];

        // Latch rotation: tick 0 is 90 deg, tick 4+ is 180 deg
        decl.getRotation("LeftLock", 0f, rot, sel);
        check(Math.abs(rot[0] - 90f) < EPS, "LeftLock rotation at tick 0 must be 90 deg");
        decl.getRotation("LeftLock", 2f, rot, sel);
        check(Math.abs(rot[0] - 135f) < EPS, "LeftLock rotation at tick 2 must be 135 deg");
        decl.getRotation("LeftLock", 4f, rot, sel);
        check(Math.abs(rot[0] - 180f) < EPS, "LeftLock rotation at tick 4 must be 180 deg");

        // Door slide:
        // Tick 0..7: 0
        decl.getTranslation("LeftDoor", 0f, false, trans, sel);
        check(Math.abs(trans[2]) < EPS, "LeftDoor translation at tick 0 must be 0");
        decl.getTranslation("LeftDoor", 7f, false, trans, sel);
        check(Math.abs(trans[2]) < EPS, "LeftDoor translation at tick 7 must be 0");

        // Tick 11 (200ms crack phase): 0.05 * 2.125 = 0.10625
        decl.getTranslation("LeftDoor", 11f, false, trans, sel);
        float expected11 = 2.125f * 0.05f;
        check(Math.abs(trans[2] - expected11) < EPS, "LeftDoor translation at tick 11 must be " + expected11 + ", got " + trans[2]);

        // Tick 24: full open 2.125
        decl.getTranslation("LeftDoor", 24f, false, trans, sel);
        check(Math.abs(trans[2] - 2.125f) < EPS, "LeftDoor translation at tick 24 must be 2.125");

        // RightDoor slides in -Z
        decl.getTranslation("RightDoor", 24f, false, trans, sel);
        check(Math.abs(trans[2] - (-2.125f)) < EPS, "RightDoor translation at tick 24 must be -2.125");

        helper.succeed();
    }
}
