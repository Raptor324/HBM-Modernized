#!/usr/bin/env python3
"""Transform the Blockbench hazmat.obj export into the vanilla-model space
used by AbstractObjArmorLayer (the t51.obj convention).

Source coordinates: block units, Y UP (feet = 0, head top = ~2.03),
front of the player at -Z (goggles/canister placement in the source).
Target coordinates: vanilla model pixels, Y DOWN (neck/head pivot = 0,
feet = +24), front at -Z (verified against GasMaskModels snout cubes at -Z).

    x = 16 * x_block            (kept; see group swap below)
    y = 24 - 16 * y_block       (global flip, no special head handling:
                                 helmet block 1.47..2.03 -> -8.55..0.55,
                                 matching the t51.obj helmet -8.65..0.78)
    z = 16 * z_block            (front stays -Z)

Group remap: the Blockbench export is X-mirrored relative to the working
t51.obj convention (t51 "RightArm" sits at NEGATIVE X). Swapping the
Left/Right group names achieves the same mirror without touching geometry
(rendering uses armorCutoutNoCull, so winding does not matter):
  armorLeftArm -> RightArm, armorRightArm -> LeftArm, same for legs/boots.

  mask, goggles, "filter connection", connector -> Helmet (merged)
  filter1  -> Filter (rendered only when a filter is installed)
  filter3  -> dropped (near-duplicate of filter1, would z-fight)
  armorBody, canister -> Chest (the back-mounted air tank rides the body)
"""
import sys
from collections import OrderedDict

SRC = r"C:/Users/user/Desktop/hazmat/hazmat.obj"
DST = r"C:/Projects/HBM-Modernized/src/main/resources/assets/hbm_m/models/armor/hazmat.obj"
SCALE = 16.0

GROUP_MAP = {
    "mask": "Helmet",
    "goggles": "Helmet",
    "filter connection": "Helmet",
    "connector": "Helmet",
    "filter1": "Filter",
    "filter3": None,  # dropped
    "armorBody": "Chest",
    "canister": "Chest",
    "armorLeftArm": "RightArm",
    "armorRightArm": "LeftArm",
    "armorLeftLeg": "RightLeg",
    "armorRightLeg": "LeftLeg",
    "armorLeftBoot": "RightBoot",
    "armorRightBoot": "LeftBoot",
}

verts, texcoords, normals = [], [], []
# output group -> list of transformed face lines; keep first-seen group order
faces_by_group = OrderedDict()
current = None
dropped_groups = set()

def fix_winding(face_args):
    """Normalize face winding to agree with its vertex normals (the t51/envsuit
    convention: geometric CCW-outside). Blockbench exports ~2/3 of the faces
    inverted, which culls the OUTSIDE faces in the item render path (cull is
    enabled there, unlike the entity layer's armorCutoutNoCull)."""
    idx = []
    for tok in face_args:
        parts = tok.split('/')
        idx.append((int(parts[0]) - 1, int(parts[2]) - 1 if len(parts) > 2 and parts[2] else None))
    pts = [verts[vi] for vi, _ in idx]
    nx = ny = nz = 0.0
    for i in range(len(pts)):
        c, n2 = pts[i], pts[(i + 1) % len(pts)]
        nx += (c[1] - n2[1]) * (c[2] + n2[2])
        ny += (c[2] - n2[2]) * (c[0] + n2[0])
        nz += (c[0] - n2[0]) * (c[1] + n2[1])
    dot = 0.0
    for vi, ni in idx:
        if ni is not None and ni < len(normals):
            vn = normals[ni]
            dot = nx * vn[0] + ny * vn[1] + nz * vn[2]
            break
    if dot < 0:
        return " ".join(reversed(face_args))
    return " ".join(face_args)

with open(SRC, "r", encoding="utf-8") as f:
    for raw in f:
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("v "):
            x, y, z = map(float, line.split()[1:4])
            verts.append((x * SCALE, 24.0 - y * SCALE, z * SCALE))
        elif line.startswith("vt "):
            texcoords.append(tuple(line.split()[1:3]))
        elif line.startswith("vn "):
            normals.append(tuple(map(float, line.split()[1:4])))
        elif line.startswith("o "):
            name = line[2:].strip()
            current = GROUP_MAP.get(name, False)
            if current is None:
                dropped_groups.add(name)
            elif current is False:
                print(f"WARNING: unknown object '{name}' - dropped", file=sys.stderr)
                dropped_groups.add(name)
        elif line.startswith("f ") and current:
            faces_by_group.setdefault(current, []).append(fix_winding(line[2:].split()))
        # usemtl / mtllib: re-emitted by us below

with open(DST, "w", encoding="utf-8", newline="\n") as out:
    out.write("# HBM-Modernized hazmat suit - transformed from Blockbench export\n")
    out.write("mtllib hazmat.mtl\n")
    for v in verts:
        out.write("v %.6f %.6f %.6f\n" % v)
    for vt in texcoords:
        out.write("vt %s %s\n" % vt)
    for vn in normals:
        out.write("vn %s %s %s\n" % vn)
    for group, flines in faces_by_group.items():
        out.write("o %s\n" % group)
        # Anvil convention: material "default" with a DEFERRED map_Kd (#default),
        # so each item JSON's "default" texture key is honored at bake time.
        out.write("usemtl default\n")
        for fl in flines:
            out.write("f %s\n" % fl)

total_faces = sum(len(v) for v in faces_by_group.values())
print(f"OK: {len(verts)} verts, {total_faces} faces into {DST}")
print(f"groups: { {k: len(v) for k, v in faces_by_group.items()} }")
if dropped_groups:
    print(f"dropped objects: {sorted(dropped_groups)}")
