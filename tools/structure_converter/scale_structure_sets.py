#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Масштабирует spacing/separation всех structure_set на sqrt(5):
плотность random_spread ~ 1/spacing^2, поэтому структура появляется в 5 раз реже.

Запуск: python tools/structure_converter/scale_structure_sets.py
(вызывать после convert.py — idempotent не является: применяет коэффициент к текущим значениям).
"""
import json, glob, math, os

K = math.sqrt(5)
HERE = os.path.dirname(os.path.abspath(__file__))
DST = os.path.normpath(os.path.join(HERE, "..", "..", "src", "main", "resources", "data", "hbm_m", "worldgen", "structure_set"))

for f in glob.glob(os.path.join(DST, "*.json")):
    with open(f, encoding="utf8") as fh:
        d = json.load(fh)
    pl = d.get("placement", {})
    if pl.get("type") == "minecraft:random_spread":
        pl["spacing"] = round(pl.get("spacing", 32) * K)
        pl["separation"] = max(3, round(pl.get("separation", 8) * K))
        with open(f, "w", encoding="utf8") as fh:
            json.dump(d, fh, indent=2, ensure_ascii=False)
            fh.write("\n")
        print("scaled:", os.path.basename(f))
