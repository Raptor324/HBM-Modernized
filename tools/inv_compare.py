"""
Compares inventory slot counts between the 1.7.10 original and this port.

Original tile entities declare their size via super(N) / slots = new ItemStack[N] /
getSizeInventory(); port block entities via a ModItemStackHandler/ItemStackHandler, usually sized
by a SLOT_COUNT constant. Note that many port block entities also keep small helper containers
(e.g. `new SimpleContainer(1)` used to look a smelting recipe up) - those must NOT be mistaken for
the machine inventory, which is what an earlier version of this script did.
"""
import io, os, re, glob, difflib

PORT = r"C:\Users\Luke\Documents\VS Projekte HBM Modernized\HBM-Modernized\src\main\java\com\hbm_m\blockentity"
ORIG = (r"C:\Users\Luke\Documents\VS Projekte HBM Modernized\Hbm-s-Nuclear-Tech-GIT-master"
        r"\Hbm-s-Nuclear-Tech-GIT-master\src\main\java\com\hbm\tileentity")

def norm(name):
    n = name
    for junk in ("TileEntity", "BlockEntity", "Machine", "Menu", "Block"):
        n = n.replace(junk, "")
    return n.lower()

def orig_size(src):
    for pat in (r"\bsuper\(\s*(\d+)\s*\)\s*;",
                r"slots\s*=\s*new\s+ItemStack\[\s*(\d+)\s*\]",
                r"getSizeInventory\s*\(\s*\)\s*\{\s*return\s+(\d+)"):
        m = re.search(pat, src)
        if m:
            return int(m.group(1))
    return None

def consts(src):
    out = {}
    for m in re.finditer(r"(?:static\s+)?final\s+int\s+(\w+)\s*=\s*(\d+)\s*;", src):
        out[m.group(1)] = int(m.group(2))
    return out

def port_size(src):
    c = consts(src)

    # 1) the real machine inventory: a stack handler, literal or constant-sized
    m = re.search(r"(?:ModItemStackHandler|ItemStackHandler)\s*\(\s*([A-Za-z_0-9]+)\s*[,)]", src)
    if m:
        tok = m.group(1)
        if tok.isdigit():
            return int(tok)
        if tok in c:
            return c[tok]

    # 2) size passed straight into the base constructor
    m = re.search(r"super\(\s*[^;]*?pos\s*,\s*state\s*,\s*([A-Za-z_0-9]+)\s*[,)]", src, re.S)
    if m:
        tok = m.group(1)
        if tok.isdigit():
            return int(tok)
        if tok in c:
            return c[tok]

    # 3) a declared slot-count constant on its own
    for key in ("SLOT_COUNT", "SLOTS", "INVENTORY_SIZE"):
        if key in c:
            return c[key]

    # 4) last resort - a plain container, but only if nothing above matched
    m = re.search(r"SimpleContainer\s*\(\s*(\d+)\s*\)", src)
    if m:
        return int(m.group(1))
    return None

def collect(root, sizer):
    out = {}
    for path in glob.glob(os.path.join(root, "**", "*.java"), recursive=True):
        src = io.open(path, encoding="utf-8", errors="ignore").read()
        n = sizer(src)
        if n is not None:
            out[os.path.splitext(os.path.basename(path))[0]] = n
    return out

orig = collect(ORIG, orig_size)
port = collect(PORT, port_size)

orig_norm = {norm(k): (k, v) for k, v in orig.items()}
keys = list(orig_norm.keys())

matched, unmatched = [], []
for pcls, psize in sorted(port.items()):
    hit = orig_norm.get(norm(pcls))
    if hit is None:
        close = difflib.get_close_matches(norm(pcls), keys, n=1, cutoff=0.88)
        hit = orig_norm[close[0]] if close else None
    if hit:
        matched.append((pcls, hit[0], psize, hit[1]))
    else:
        unmatched.append((pcls, psize))

fewer = [r for r in matched if r[2] < r[3]]
more = [r for r in matched if r[2] > r[3]]

print("port=%d  orig=%d  matched=%d" % (len(port), len(orig), len(matched)))
print("\nPORT HAS FEWER SLOTS THAN ORIGINAL (%d):" % len(fewer))
for pcls, ocls, psize, osize in fewer:
    print("  %-44s port=%-3d %-40s orig=%d" % (pcls, psize, ocls, osize))
print("\nPORT HAS MORE SLOTS THAN ORIGINAL (%d):" % len(more))
for pcls, ocls, psize, osize in more:
    print("  %-44s port=%-3d %-40s orig=%d" % (pcls, psize, ocls, osize))
print("\nno counterpart matched: %d" % len(unmatched))
