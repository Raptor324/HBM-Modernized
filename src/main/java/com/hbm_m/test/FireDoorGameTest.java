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
 * GameTest-верификация паритета Fire Door с 1.7.10 (M6: fire_door).
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class FireDoorGameTest {

    private static final float EPS = 1.0E-4F;

    private FireDoorGameTest() {
    }

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    @GameTest(template = "empty3x3x3", batch = "fire_door", timeoutTicks = 100)
    public static void fireDoorPropertiesAndParity(GameTestHelper helper) {
        // 1. Hardness 10.0F, Blast Resistance 1000.0F (1.7.10 parity)
        float resistance = ModBlocks.FIRE_DOOR.get().getExplosionResistance();
        check(Math.abs(resistance - 1000.0F) < EPS,
                "Fire Door resistance must be 1000.0F like in 1.7.10, got " + resistance);

        float destroySpeed = ModBlocks.FIRE_DOOR.get().defaultDestroyTime();
        check(Math.abs(destroySpeed - 10.0F) < EPS,
                "Fire Door destroy speed must be 10.0F like in 1.7.10, got " + destroySpeed);

        // 2. Completion status COMPLETE on both block and item
        check(CompletionTracker.getStatus(ModBlocks.FIRE_DOOR.getId()) == CompletionStatus.COMPLETE,
                "ModBlocks.FIRE_DOOR must be marked CompletionStatus.COMPLETE");
        check(CompletionTracker.getStatus(ModItems.FIRE_DOOR.getId()) == CompletionStatus.COMPLETE,
                "ModItems.FIRE_DOOR must be marked CompletionStatus.COMPLETE");

        // 3. DoorDecl structure: 4x3 footprint (x in [-1, 2], y in [0, 2], z = 0)
        DoorDecl decl = DoorDecl.FIRE_DOOR;
        DoorDecl.DoorStructureDefinition def = decl.getStructureDefinition();
        check(def != null, "Fire Door structure definition must be defined");

        Map<BlockPos, VoxelShape> closedShapes = def.getClosedShapes();
        Map<BlockPos, VoxelShape> openShapes = def.getOpenShapes();
        Map<BlockPos, PartRole> roles = def.getRoles();

        for (int x = -1; x <= 2; x++) {
            for (int y = 0; y <= 2; y++) {
                BlockPos pos = new BlockPos(x, y, 0);
                check(roles.containsKey(pos), "Footprint must contain block at " + pos);
                check(closedShapes.containsKey(pos) && !closedShapes.get(pos).isEmpty(),
                        "Closed state must be solid at " + pos);

                boolean isInteriorPassage = (x == 0 || x == 1);
                if (isInteriorPassage) {
                    if (y == 1) {
                        VoxelShape shape = openShapes.get(pos);
                        check(shape == null || shape.isEmpty(),
                                "Interior passage at " + pos + " must be empty when open");
                    } else if (y == 0) {
                        // Low threshold (3-pixel floor plate 'M')
                        VoxelShape shape = openShapes.get(pos);
                        check(shape != null && !shape.isEmpty(),
                                "Floor rail at " + pos + " must have floor plate shape");
                    } else if (y == 2) {
                        // Upper frame collision (half block 'T')
                        VoxelShape shape = openShapes.get(pos);
                        check(shape != null && !shape.isEmpty(),
                                "Upper frame collision at " + pos + " must have upper frame shape");
                    }
                } else {
                    // Outer side columns (x = -1 and x = 2) must remain solid/semi-solid
                    check(openShapes.containsKey(pos) && !openShapes.get(pos).isEmpty(),
                            "Outer frame at " + pos + " must have border frame shape when open");
                }
            }
        }

        // Controller at (0, 0, 0)
        check(roles.get(BlockPos.ZERO) == PartRole.CONTROLLER,
                "Controller must be located at (0, 0, 0)");

        // 4. Timing: openTime = 160 ticks (8.0 seconds slow drop guillotine)
        check(decl.getOpenTime() == 160, "Fire Door open time must be 160 ticks (8.0 seconds)");

        // 5. Door open ranges: {{-1, 0, 0, 3, 4, 1}}
        int[][] ranges = decl.getDoorOpenRanges();
        check(ranges.length == 1, "Door open ranges must have length 1");
        check(ranges[0][0] == -1 && ranges[0][1] == 0 && ranges[0][2] == 0
                        && ranges[0][3] == 3 && ranges[0][4] == 4 && ranges[0][5] == 1,
                "Door open range must be {{-1, 0, 0, 3, 4, 1}}");

        // Retraction progression along Y (axis 1):
        // progress 0: none retracted
        check(!decl.isPartRetracted(new BlockPos(0, 0, 0), 0.0f), "At 0.0 progress nothing retracted");
        // progress 0.1f (> 0/2): y=0 retracted, y=1,2 not
        check(decl.isPartRetracted(new BlockPos(0, 0, 0), 0.1f), "At 0.1 progress y=0 retracted");
        check(!decl.isPartRetracted(new BlockPos(0, 1, 0), 0.1f), "At 0.1 progress y=1 not retracted");
        // progress 0.6f (> 1/2): y=0, 1 retracted, y=2 not
        check(decl.isPartRetracted(new BlockPos(0, 1, 0), 0.6f), "At 0.6 progress y=1 retracted");
        check(!decl.isPartRetracted(new BlockPos(0, 2, 0), 0.6f), "At 0.6 progress y=2 not retracted");
        // progress 1.0f: all retracted
        check(decl.isPartRetracted(new BlockPos(0, 2, 0), 1.0f), "At 1.0 progress y=2 retracted");

        // 6. Skins: 5 skins (Default, Black, Orange, Yellow, Trefoil)
        check(decl.hasSkins(), "Fire Door must declare hasSkins = true");
        check(decl.getSkinCount() == 5, "Fire Door skin count must be 5, got " + decl.getSkinCount());

        DoorSkin[] skins = decl.getSkins();
        check(skins != null && skins.length == 5, "Fire Door skins array must have length 5");
        check("default".equals(skins[0].getId()), "Skin 0 must be default");
        check("black".equals(skins[1].getId()), "Skin 1 must be black");
        check("orange".equals(skins[2].getId()), "Skin 2 must be orange");
        check("yellow".equals(skins[3].getId()), "Skin 3 must be yellow");
        check("trefoil".equals(skins[4].getId()), "Skin 4 must be trefoil");

        // 7. Sounds
        check(Math.abs(decl.getSoundVolume() - 2.0f) < EPS, "Sound volume must be 2.0, got " + decl.getSoundVolume());
        check(decl.getOpenSoundLoop() == ModSounds.WGH_START.get(), "Open sound loop must be wgh_start");
        check(decl.getSoundLoop2() == ModSounds.ALARM_6.get(), "Sound loop 2 must be alarm_6");
        check(decl.getCloseSoundLoop() == ModSounds.WGH_START.get(), "Close sound loop must be wgh_start");
        check(decl.getOpenSoundEnd() == ModSounds.WGH_STOP.get(), "Open sound end must be wgh_stop");
        check(decl.getCloseSoundEnd() == ModSounds.WGH_STOP.get(), "Close sound end must be wgh_stop");

        // 8. Animation transforms
        DoorModelSelection sel = DoorModelSelection.modern(DoorSkin.DEFAULT);
        float[] trans = new float[3];

        // Frame stays static
        decl.getTranslation("frame", 80f, false, trans, sel);
        check(Math.abs(trans[0]) < EPS && Math.abs(trans[1]) < EPS && Math.abs(trans[2]) < EPS,
                "Frame translation must be (0, 0, 0)");

        // Door raises vertically: 0 at tick 0, 1.375 at tick 80, 2.75 at tick 160
        decl.getTranslation("door", 0f, false, trans, sel);
        check(Math.abs(trans[1] - 0f) < EPS, "Door translation at tick 0 must be 0, got " + trans[1]);

        decl.getTranslation("door", 80f, false, trans, sel);
        check(Math.abs(trans[1] - 1.375f) < EPS, "Door translation at tick 80 must be 1.375, got " + trans[1]);

        decl.getTranslation("door", 160f, false, trans, sel);
        check(Math.abs(trans[1] - 2.75f) < EPS, "Door translation at tick 160 must be 2.75, got " + trans[1]);

        helper.succeed();
    }
}
