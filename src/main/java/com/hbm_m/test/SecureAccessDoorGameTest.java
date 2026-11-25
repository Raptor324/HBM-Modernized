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
 * GameTest-верификация паритета Secure Access Door с 1.7.10 (M7: secure_access_door).
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class SecureAccessDoorGameTest {

    private static final float EPS = 1.0E-4F;

    private SecureAccessDoorGameTest() {
    }

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    @GameTest(template = "empty3x3x3", batch = "secure_access_door", timeoutTicks = 100)
    public static void secureAccessDoorPropertiesAndParity(GameTestHelper helper) {
        // 1. Hardness 20.0F, Blast Resistance 2000.0F (1.7.10 parity)
        float resistance = ModBlocks.SECURE_ACCESS_DOOR.get().getExplosionResistance();
        check(Math.abs(resistance - 2000.0F) < EPS,
                "Secure Access Door resistance must be 2000.0F like in 1.7.10, got " + resistance);

        float destroySpeed = ModBlocks.SECURE_ACCESS_DOOR.get().defaultDestroyTime();
        check(Math.abs(destroySpeed - 20.0F) < EPS,
                "Secure Access Door destroy speed must be 20.0F like in 1.7.10, got " + destroySpeed);

        // 2. Completion status COMPLETE on both block and item
        check(CompletionTracker.getStatus(ModBlocks.SECURE_ACCESS_DOOR.getId()) == CompletionStatus.COMPLETE,
                "ModBlocks.SECURE_ACCESS_DOOR must be marked CompletionStatus.COMPLETE");
        check(CompletionTracker.getStatus(ModItems.SECURE_ACCESS_DOOR.getId()) == CompletionStatus.COMPLETE,
                "ModItems.SECURE_ACCESS_DOOR must be marked CompletionStatus.COMPLETE");

        // 3. DoorDecl structure: 5x5 footprint (x in [-2, 2], y in [0, 4], z = 0)
        DoorDecl decl = DoorDecl.SECURE_ACCESS_DOOR;
        DoorDecl.DoorStructureDefinition def = decl.getStructureDefinition();
        check(def != null, "Secure Access Door structure definition must be defined");

        Map<BlockPos, VoxelShape> closedShapes = def.getClosedShapes();
        Map<BlockPos, VoxelShape> openShapes = def.getOpenShapes();
        Map<BlockPos, PartRole> roles = def.getRoles();

        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 4; y++) {
                BlockPos pos = new BlockPos(x, y, 0);
                check(roles.containsKey(pos), "Footprint must contain block at " + pos);
                check(closedShapes.containsKey(pos) && !closedShapes.get(pos).isEmpty(),
                        "Closed state must be solid at " + pos);

                if (y == 0) {
                    // Full solid threshold across bottom row (x in [-2, 2])
                    VoxelShape openShape = openShapes.get(pos);
                    check(openShape != null && !openShape.isEmpty(),
                            "Floor threshold at " + pos + " must remain solid when open");
                } else if (y >= 1 && y <= 3) {
                    // Central and side passage completely clear when open
                    VoxelShape openShape = openShapes.get(pos);
                    check(openShape == null || openShape.isEmpty(),
                            "Passage at " + pos + " must be empty when open");
                } else if (y == 4) {
                    // Top row header frame collision (Block.box(0, 6, 0, 16, 16, 16))
                    VoxelShape openShape = openShapes.get(pos);
                    check(openShape != null && !openShape.isEmpty(),
                            "Top row at " + pos + " must have upper frame collision when open");
                }
            }
        }

        // Controller at (0, 0, 0)
        check(roles.get(BlockPos.ZERO) == PartRole.CONTROLLER,
                "Controller must be located at (0, 0, 0)");

        // 4. Timing: openTime = 120 ticks (6.0 seconds heavy bunker door)
        check(decl.getOpenTime() == 120, "Secure Access Door open time must be 120 ticks (6.0 seconds)");

        // 5. Door open ranges: {{-2, 1, 0, 4, 5, 1}}
        int[][] ranges = decl.getDoorOpenRanges();
        check(ranges.length == 1, "Door open ranges must have length 1");
        check(ranges[0][0] == -2 && ranges[0][1] == 1 && ranges[0][2] == 0
                        && ranges[0][3] == 4 && ranges[0][4] == 5 && ranges[0][5] == 1,
                "Door open range must be {{-2, 1, 0, 4, 5, 1}}");

        // Retraction progression along Y (axis 1, startY = 1, steps = 4):
        // Thresholds: j=0 (y=1) -> 0/3 = 0.0f; j=1 (y=2) -> 1/3 = 0.3333f; j=2 (y=3) -> 2/3 = 0.6667f; j=3 (y=4) -> 3/3 = 1.0f.
        // y=0 is outside the range and NEVER retracts
        check(!decl.isPartRetracted(new BlockPos(0, 0, 0), 0.0f), "At 0.0 progress y=0 not retracted");
        check(!decl.isPartRetracted(new BlockPos(0, 0, 0), 0.5f), "At 0.5 progress y=0 not retracted");
        check(!decl.isPartRetracted(new BlockPos(0, 0, 0), 1.0f), "At 1.0 progress y=0 never retracted");

        // Progress 0.0f: nothing in range retracted
        for (int y = 1; y <= 4; y++) {
            check(!decl.isPartRetracted(new BlockPos(0, y, 0), 0.0f),
                    "At 0.0 progress y=" + y + " must not be retracted");
        }

        // Progress 0.1f (> 0/3): y=1 retracted, y=2,3,4 not
        check(decl.isPartRetracted(new BlockPos(0, 1, 0), 0.1f), "At 0.1 progress y=1 must be retracted");
        check(!decl.isPartRetracted(new BlockPos(0, 2, 0), 0.1f), "At 0.1 progress y=2 must not be retracted");
        check(!decl.isPartRetracted(new BlockPos(0, 3, 0), 0.1f), "At 0.1 progress y=3 must not be retracted");
        check(!decl.isPartRetracted(new BlockPos(0, 4, 0), 0.1f), "At 0.1 progress y=4 must not be retracted");

        // Progress 0.5f (> 1/3): y=1,2 retracted, y=3,4 not
        check(decl.isPartRetracted(new BlockPos(0, 1, 0), 0.5f), "At 0.5 progress y=1 must be retracted");
        check(decl.isPartRetracted(new BlockPos(0, 2, 0), 0.5f), "At 0.5 progress y=2 must be retracted");
        check(!decl.isPartRetracted(new BlockPos(0, 3, 0), 0.5f), "At 0.5 progress y=3 must not be retracted");
        check(!decl.isPartRetracted(new BlockPos(0, 4, 0), 0.5f), "At 0.5 progress y=4 must not be retracted");

        // Progress 0.8f (> 2/3): y=1,2,3 retracted, y=4 not
        check(decl.isPartRetracted(new BlockPos(0, 1, 0), 0.8f), "At 0.8 progress y=1 must be retracted");
        check(decl.isPartRetracted(new BlockPos(0, 2, 0), 0.8f), "At 0.8 progress y=2 must be retracted");
        check(decl.isPartRetracted(new BlockPos(0, 3, 0), 0.8f), "At 0.8 progress y=3 must be retracted");
        check(!decl.isPartRetracted(new BlockPos(0, 4, 0), 0.8f), "At 0.8 progress y=4 must not be retracted");

        // Progress 1.0f: all in range (y=1..4) retracted
        for (int y = 1; y <= 4; y++) {
            check(decl.isPartRetracted(new BlockPos(0, y, 0), 1.0f),
                    "At 1.0 progress y=" + y + " must be retracted");
        }

        // 6. Skins: 4 skins (Default, Gray, Black, Yellow)
        check(decl.hasSkins(), "Secure Access Door must declare hasSkins = true");
        check(decl.getSkinCount() == 4, "Secure Access Door skin count must be 4, got " + decl.getSkinCount());

        DoorSkin[] skins = decl.getSkins();
        check(skins != null && skins.length == 4, "Secure Access Door skins array must have length 4");
        check("default".equals(skins[0].getId()), "Skin 0 must be default");
        check("gray".equals(skins[1].getId()), "Skin 1 must be gray");
        check("black".equals(skins[2].getId()), "Skin 2 must be black");
        check("yellow".equals(skins[3].getId()), "Skin 3 must be yellow");

        // 7. Sounds
        check(Math.abs(decl.getSoundVolume() - 2.0f) < EPS, "Sound volume must be 2.0, got " + decl.getSoundVolume());
        check(decl.getOpenSoundLoop() == ModSounds.GARAGE_MOVE.get(), "Open sound loop must be garage_move");
        check(decl.getCloseSoundLoop() == ModSounds.GARAGE_MOVE.get(), "Close sound loop must be garage_move");
        check(decl.getOpenSoundEnd() == ModSounds.GARAGE_STOP.get(), "Open sound end must be garage_stop");
        check(decl.getCloseSoundEnd() == ModSounds.GARAGE_STOP.get(), "Close sound end must be garage_stop");
        check(decl.getOpenSoundStart() == null, "Open sound start must be null");
        check(decl.getCloseSoundStart() == null, "Close sound start must be null");

        // 8. Animation transforms: door part translates by 3.5 along Y; frame/base stays static
        float[] trans = new float[3];

        // Tick 0: 0 translation
        decl.getTranslation("door", 0f, false, trans);
        check(Math.abs(trans[0]) < EPS && Math.abs(trans[1]) < EPS && Math.abs(trans[2]) < EPS,
                "Door translation at tick 0 must be (0, 0, 0)");

        // Tick 60 (halfway, 60/120 = 0.5): 3.5 * 0.5 = 1.75
        decl.getTranslation("door", 60f, false, trans);
        check(Math.abs(trans[1] - 1.75f) < EPS,
                "Door translation at tick 60 must be 1.75 along Y, got " + trans[1]);

        // Tick 120 (full open): 3.5
        decl.getTranslation("door", 120f, false, trans);
        check(Math.abs(trans[1] - 3.5f) < EPS,
                "Door translation at tick 120 must be 3.5 along Y, got " + trans[1]);

        // Frame / base part must never translate
        decl.getTranslation("base", 120f, false, trans);
        check(Math.abs(trans[0]) < EPS && Math.abs(trans[1]) < EPS && Math.abs(trans[2]) < EPS,
                "Base part must not translate");
        decl.getTranslation("frame", 120f, false, trans);
        check(Math.abs(trans[0]) < EPS && Math.abs(trans[1]) < EPS && Math.abs(trans[2]) < EPS,
                "Frame part must not translate");

        // Dynamic shape & clipping planes
        check(decl.isDynamicShape(), "Secure Access Door must be dynamic shape");
        double[][] clip = decl.getClippingPlanes();
        check(clip != null && clip.length == 1, "Clipping planes must be defined with length 1");
        check(clip[0][1] == -1.0 && clip[0][3] == 5.0, "Clipping plane must be at Y=5");

        helper.succeed();
    }
}
