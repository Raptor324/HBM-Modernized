#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Конвертер Techne-моделей (Java ModelRenderer) в OBJ для forge:obj.

Разбирает ModelRenderer-блоки (addBox/setRotationPoint/setRotation/texture offset),
применяет конвенцию рендера RenderDecoBlock:
    translate(x+0.5, y+1.5, z+0.5) + rotate(180, 0,0,1)
    => world = (0.5 - mx/16, 1.5 - my/16, 0.5 + mz/16)
UV — стандартная раскладка ModelBox (кожа/боксы). Материал один (Material.001),
текстура берётся из JSON-обёртки (map_Kd #default в hbm_base.mtl).

Запуск: python techne_to_obj.py <Model.java> <texW> <texH> <out.obj>
"""
import re, sys, math

def rot_x(v, a):
    x,y,z = v; c,s = math.cos(a), math.sin(a)
    return (x, y*c - z*s, y*s + z*c)
def rot_y(v, a):
    x,y,z = v; c,s = math.cos(a), math.sin(a)
    return (x*c + z*s, y, -x*s + z*c)
def rot_z(v, a):
    x,y,z = v; c,s = math.cos(a), math.sin(a)
    return (x*c - y*s, x*s + y*c, z)

def parse(path):
    src = open(path, encoding='utf8', errors='ignore').read()
    boxes = []
    # порядок: new ModelRenderer(this, u, v) ... addBox(...) ... setRotationPoint(...) ... setRotation(this, rx,ry,rz)
    pat = re.compile(
        r'new ModelRenderer\(this,\s*([\d.]+)F?,\s*([\d.]+)F?\)(?:(?!new ModelRenderer).)*?'
        r'\.addBox\((-?[\d.]+)F?,\s*(-?[\d.]+)F?,\s*(-?[\d.]+)F?,\s*([\d.]+)F?,\s*([\d.]+)F?,\s*([\d.]+)F?\)'
        r'(?:(?!new ModelRenderer).)*?\.setRotationPoint\((-?[\d.]+)F?,\s*(-?[\d.]+)F?,\s*(-?[\d.]+)F?\)'
        r'(?:(?!new ModelRenderer).)*?setRotation\(this\.\w+,\s*(-?[\d.]+)F?,\s*(-?[\d.]+)F?,\s*(-?[\d.]+)F?\)',
        re.S)
    for m in pat.finditer(src):
        u, v = float(m.group(1)), float(m.group(2))
        bx, by, bz, w, h, d = map(float, m.group(3, 4, 5, 6, 7, 8))
        px, py, pz = map(float, m.group(9, 10, 11))
        rx, ry, rz = map(float, m.group(12, 13, 14))
        boxes.append(dict(u=u, v=v, box=(bx, by, bz, w, h, d), point=(px, py, pz), rot=(rx, ry, rz)))
    return boxes

def world(p):
    return (0.5 - p[0] / 16.0, 1.5 - p[1] / 16.0, 0.5 + p[2] / 16.0)

FACES = {
    # имя: (выбор 4 углов в порядке CCW снаружи, uv-функция)
}
def face_verts(b):
    bx, by, bz, w, h, d = b
    x0, y0, z0, x1, y1, z1 = bx, by, bz, bx + w, by + h, bz + d
    return {
        '-z': [(x0,y0,z0),(x0,y1,z0),(x1,y1,z0),(x1,y0,z0)],
        '+z': [(x0,y0,z1),(x1,y0,z1),(x1,y1,z1),(x0,y1,z1)],
        '-x': [(x0,y0,z0),(x0,y0,z1),(x0,y1,z1),(x0,y1,z0)],
        '+x': [(x1,y0,z0),(x1,y1,z0),(x1,y1,z1),(x1,y0,z1)],
        '-y': [(x0,y0,z0),(x1,y0,z0),(x1,y0,z1),(x0,y0,z1)],
        '+y': [(x0,y1,z0),(x0,y1,z1),(x1,y1,z1),(x1,y1,z0)],
    }

def face_uvs(u, v, w, h, d):
    # соответствуют порядку углов face_verts; texel-координаты (tu слева-направо, tv сверху-вниз)
    return {
        '-z': [(u+d,   v+d+h), (u+d,   v+d),   (u+d+w,   v+d),   (u+d+w,   v+d+h)],
        '+z': [(u+2*d+2*w, v+d+h), (u+2*d+w, v+d+h), (u+2*d+w, v+d), (u+2*d+2*w, v+d)],
        '-x': [(u+d,   v+d+h), (u,     v+d+h), (u,     v+d),   (u+d,     v+d)],
        '+x': [(u+d+w, v+d+h), (u+d+w, v+d),   (u+d+w+d, v+d), (u+d+w+d, v+d+h)],
        '-y': [(u+d+2*w, v),   (u+d+w, v),     (u+d+w, v+d),   (u+d+2*w, v+d)],
        '+y': [(u+d,   v),     (u+d,   v+d),   (u+d+w, v+d),   (u+d+w,   v)],
    }

def main(java, tw, th, out):
    boxes = parse(java)
    verts = []   # world coords
    faces = []   # (4 vertex indices 1-based, 4 (tu,tv) normalized)
    for b in boxes:
        bx, by, bz, w, h, d = b['box']
        px, py, pz = b['point']
        rx, ry, rz = b['rot']
        fv = face_verts((bx, by, bz, w, h, d))
        fu = face_uvs(b['u'], b['v'], w, h, d)
        for name in ('-z', '+z', '-x', '+x', '-y', '+y'):
            idx = []
            for corner in fv[name]:
                # локально -> поворот бокса -> + rotation point -> мир
                c = rot_x(corner, rx)
                c = rot_y(c, ry)
                c = rot_z(c, rz)
                c = (c[0] + px, c[1] + py, c[2] + pz)
                verts.append(world(c))
                idx.append(len(verts))
            uvs = [(tu / tw, 1.0 - tv / th) for tu, tv in fu[name]]
            faces.append((idx, uvs))
    with open(out, 'w', encoding='utf8', newline='\n') as f:
        f.write("# Converted from Techne: %s\nmtllib hbm_base.mtl\nusemtl Material.001\n" % java)
        for v in verts:
            f.write("v %.6f %.6f %.6f\n" % v)
        for idx, uvs in faces:
            for tu, tv in uvs:
                f.write("vt %.6f %.6f\n" % (tu, tv))
        base = 1
        for i, (idx, _) in enumerate(faces):
            f.write("f %d/%d %d/%d %d/%d %d/%d\n" % (
                idx[0], base, idx[1], base+1, idx[2], base+2, idx[3], base+3))
            base += 4
    print(out, 'boxes:', len(boxes), 'faces:', len(faces), 'verts:', len(verts))
    xs = [v[0] for v in verts]; ys = [v[1] for v in verts]; zs = [v[2] for v in verts]
    print('  bounds x %.3f..%.3f y %.3f..%.3f z %.3f..%.3f' % (min(xs), max(xs), min(ys), max(ys), min(zs), max(zs)))

if __name__ == '__main__':
    main(*sys.argv[1:5], float(sys.argv[5]), float(sys.argv[6]), sys.argv[7]) if False else None
    main(sys.argv[1], float(sys.argv[2]), float(sys.argv[3]), sys.argv[4])
