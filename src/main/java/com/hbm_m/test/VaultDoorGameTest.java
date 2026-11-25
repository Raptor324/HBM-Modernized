package com.hbm_m.test;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.doors.DoorDecl;
import com.hbm_m.client.model.variant.DoorSkin;
import com.hbm_m.item.ModItems;
import com.hbm_m.multiblock.PartRole;
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
 * GameTest-верификация паритета Vault Door с 1.7.10 (M4: vault_door).
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class VaultDoorGameTest {

    private static final float EPS = 1.0E-4F;

    private VaultDoorGameTest() {
    }

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    @GameTest(template = "empty3x3x3", batch = "vault_door", timeoutTicks = 100)
    public static void vaultDoorPropertiesAndParity(GameTestHelper helper) {
        // 1. Hardness 10.0F, Blast Resistance 1000.0F (1.7.10 parity)
        float resistance = ModBlocks.VAULT_DOOR.get().getExplosionResistance();
        check(Math.abs(resistance - 1000.0F) < EPS,
                "Vault Door resistance must be 1000.0F like in 1.7.10, got " + resistance);

        float destroySpeed = ModBlocks.VAULT_DOOR.get().defaultDestroyTime();
        check(Math.abs(destroySpeed - 10.0F) < EPS,
                "Vault Door destroy speed must be 10.0F like in 1.7.10, got " + destroySpeed);

        // 2. Completion status COMPLETE on both block and item
        check(CompletionTracker.getStatus(ModBlocks.VAULT_DOOR.getId()) == CompletionStatus.COMPLETE,
                "ModBlocks.VAULT_DOOR must be marked CompletionStatus.COMPLETE");
        check(CompletionTracker.getStatus(ModItems.VAULT_DOOR.getId()) == CompletionStatus.COMPLETE,
                "ModItems.VAULT_DOOR must be marked CompletionStatus.COMPLETE");

        // 3. DoorDecl structure: 5x5 footprint, 3x3 circular opening
        DoorDecl decl = DoorDecl.VAULT_DOOR;
        DoorDecl.DoorStructureDefinition def = decl.getStructureDefinition();
        check(def != null, "Vault Door structure definition must be defined");

        Map<BlockPos, VoxelShape> closedShapes = def.getClosedShapes();
        Map<BlockPos, VoxelShape> openShapes = def.getOpenShapes();
        Map<BlockPos, PartRole> roles = def.getRoles();

        // Check 5x5 footprint: x in [-2, 2], y in [0, 4]
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 4; y++) {
                BlockPos pos = new BlockPos(x, y, 0);
                check(roles.containsKey(pos), "Footprint must contain block at " + pos);
                check(closedShapes.containsKey(pos) && !closedShapes.get(pos).isEmpty(),
                        "Closed state must be solid at " + pos);

                boolean isOpening = (x >= -1 && x <= 1) && (y >= 1 && y <= 3);
                if (isOpening) {
                    VoxelShape shape = openShapes.get(pos);
                    check(shape == null || shape.isEmpty(),
                            "3x3 circular opening at " + pos + " must be passable when open");
                } else {
                    check(openShapes.containsKey(pos) && !openShapes.get(pos).isEmpty(),
                            "Outer frame at " + pos + " must remain solid when open");
                }
            }
        }

        // Controller at bottom middle (0, 0, 0)
        check(roles.get(BlockPos.ZERO) == PartRole.CONTROLLER,
                "Controller must be located at (0, 0, 0)");

        // 4. Timing: openTime = 120 ticks (6 seconds)
        check(decl.getOpenTime() == 120, "Vault Door open time must be 120 ticks (6 seconds)");

        // 5. Pull (0..40 ticks) and Slide (40..120 ticks) animation transforms
        float[] trans = new float[3];
        float[] rot = new float[3];
        float[] orig = new float[3];

        // Check pivot origin for Door and Label is (0, 2.5, 0)
        decl.getOrigin("Door", orig);
        check(Math.abs(orig[0] - 0f) < EPS && Math.abs(orig[1] - 2.5f) < EPS && Math.abs(orig[2] - 0f) < EPS,
                "Door pivot origin must be (0, 2.5, 0)");
        decl.getOrigin("Label", orig);
        check(Math.abs(orig[0] - 0f) < EPS && Math.abs(orig[1] - 2.5f) < EPS && Math.abs(orig[2] - 0f) < EPS,
                "Label pivot origin must be (0, 2.5, 0)");

        // Tick 0 (fully closed): pull = 0, slide = 0, roll = 0
        decl.getTranslation("Door", 0f, false, trans);
        decl.getRotation("Door", 0f, rot);
        check(Math.abs(trans[0]) < EPS && Math.abs(trans[1]) < EPS && Math.abs(trans[2]) < EPS,
                "Door translation at tick 0 must be (0, 0, 0)");
        check(Math.abs(rot[0]) < EPS, "Door roll rotation at tick 0 must be 0");

        // Tick 40 (pulled out, slide not yet started): pull = 1.0 (-pull = -1.0), roll = 0
        decl.getTranslation("Door", 40f, false, trans);
        decl.getRotation("Door", 40f, rot);
        check(Math.abs(trans[0] - (-1.0f)) < EPS, "Door pull at tick 40 must be -1.0f");
        check(Math.abs(rot[0]) < EPS, "Door roll at tick 40 must be 0 (slide not started)");

        // 6. Rolling cog math: diameter = 4.25D, circumference = diameter * Math.PI
        // Tick 120 (fully open): pull = 1.0, slideDist = 5.0
        double diameter = 4.25D;
        double circumference = diameter * Math.PI;
        float expectedRollDeg = (float) (360.0 * 5.0 / circumference);

        decl.getRotation("Door", 120f, rot);
        check(Math.abs(rot[0] - expectedRollDeg) < 0.01F,
                "Door roll at tick 120 must match rolling cog formula: " + expectedRollDeg + ", got " + rot[0]);

        // 7. 7 skins: Vault 101, 87, 106, 81, 111, 2, 99
        check(decl.hasSkins(), "Vault Door must declare hasSkins = true");
        check(decl.getSkinCount() == 7, "Vault Door skin count must be 7, got " + decl.getSkinCount());

        DoorSkin[] skins = decl.getSkins();
        check(skins != null && skins.length == 7, "Vault Door skins array must have length 7");
        check("default".equals(skins[0].getId()), "Skin 0 must be default (Vault 101)");
        check("skin_87".equals(skins[1].getId()), "Skin 1 must be skin_87 (Vault 87)");
        check("skin_106".equals(skins[2].getId()), "Skin 2 must be skin_106 (Vault 106)");
        check("skin_81".equals(skins[3].getId()), "Skin 3 must be skin_81 (Vault 81)");
        check("skin_111".equals(skins[4].getId()), "Skin 4 must be skin_111 (Vault 111)");
        check("skin_2".equals(skins[5].getId()), "Skin 5 must be skin_2 (Vault 2)");
        check("skin_99".equals(skins[6].getId()), "Skin 6 must be skin_99 (Vault 99)");

        // 8. Collision progress gate: 30 ticks threshold (1.5s)
        check(Math.abs(decl.getCollisionProgress((byte) 3, 20f / 120f) - 0.0f) < EPS,
                "Opening progress < 30/120 must be impassable (collisionProgress = 0)");
        check(Math.abs(decl.getCollisionProgress((byte) 3, 40f / 120f) - 1.0f) < EPS,
                "Opening progress > 30/120 must be passable (collisionProgress = 1)");
        check(Math.abs(decl.getCollisionProgress((byte) 2, 40f / 120f) - 1.0f) < EPS,
                "Closing progress > 30/120 must remain passable (collisionProgress = 1)");
        check(Math.abs(decl.getCollisionProgress((byte) 2, 20f / 120f) - 0.0f) < EPS,
                "Closing progress < 30/120 must be impassable (collisionProgress = 0)");

        helper.succeed();
    }
}
