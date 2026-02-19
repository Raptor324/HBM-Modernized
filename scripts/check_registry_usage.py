#!/usr/bin/env python3
"""
Проверяет синхронизацию ссылок между ModBlocks/ModItems и MainRegistry.

Скрипт:
1) Ищет все объявления RegistryObject в ModBlocks.java и ModItems.java.
2) Ищет все добавления через event.accept(...) в MainRegistry.java.
3) Выводит:
   - какие символы из ModBlocks/ModItems ни разу не добавлены в MainRegistry;
   - какие символы добавлены больше одного раза.
"""

from __future__ import annotations

import argparse
import re
from collections import Counter
from pathlib import Path


DECLARATION_RE = re.compile(
    r"public\s+static\s+final\s+RegistryObject<[^>]+>\s+([A-Z0-9_]+)\s*="
)
ACCEPT_CALL_RE = re.compile(r"event\.accept\((.*?)\);", re.DOTALL)
REF_RE = re.compile(r"Mod(Items|Blocks)\.([A-Z0-9_]+)")


def read_text(path: Path) -> str:
    if not path.exists():
        raise FileNotFoundError(f"File not found: {path}")
    return path.read_text(encoding="utf-8")


def extract_declarations(file_path: Path) -> set[str]:
    text = read_text(file_path)
    return set(DECLARATION_RE.findall(text))


def extract_accept_references(main_registry_path: Path) -> tuple[Counter[str], Counter[str]]:
    text = read_text(main_registry_path)
    item_counts: Counter[str] = Counter()
    block_counts: Counter[str] = Counter()

    for accept_body in ACCEPT_CALL_RE.findall(text):
        for match in REF_RE.finditer(accept_body):
            namespace, symbol = match.groups()
            if namespace == "Items":
                item_counts[symbol] += 1
            else:
                block_counts[symbol] += 1

    return item_counts, block_counts


def print_section(title: str, values: list[str]) -> None:
    print(f"\n{title}:")
    if not values:
        print("  - none")
        return
    for value in values:
        print(f"  - {value}")


def main() -> int:
    parser = argparse.ArgumentParser(
        description=(
            "Проверка символов ModBlocks/ModItems в MainRegistry: "
            "пропуски и дубли event.accept(...)."
        )
    )
    parser.add_argument(
        "--root",
        type=Path,
        default=Path("."),
        help="Корень проекта (по умолчанию текущая директория).",
    )
    parser.add_argument(
        "--fail-on-issues",
        action="store_true",
        help="Вернуть exit code 1, если найдены пропуски или дубли.",
    )
    args = parser.parse_args()

    root = args.root.resolve()
    mod_blocks = root / "src/main/java/com/hbm_m/block/ModBlocks.java"
    mod_items = root / "src/main/java/com/hbm_m/item/ModItems.java"
    main_registry = root / "src/main/java/com/hbm_m/main/MainRegistry.java"

    block_declarations = extract_declarations(mod_blocks)
    item_declarations = extract_declarations(mod_items)
    item_counts, block_counts = extract_accept_references(main_registry)

    missing_blocks = sorted(name for name in block_declarations if block_counts.get(name, 0) == 0)
    missing_items = sorted(name for name in item_declarations if item_counts.get(name, 0) == 0)

    duplicate_blocks = sorted(name for name, count in block_counts.items() if count > 1)
    duplicate_items = sorted(name for name, count in item_counts.items() if count > 1)

    print("Registry usage check report")
    print(f"- ModBlocks declarations: {len(block_declarations)}")
    print(f"- ModItems declarations: {len(item_declarations)}")
    print(f"- MainRegistry ModBlocks accepts: {sum(block_counts.values())}")
    print(f"- MainRegistry ModItems accepts: {sum(item_counts.values())}")

    print_section("Missing ModBlocks in MainRegistry", missing_blocks)
    print_section("Missing ModItems in MainRegistry", missing_items)
    print_section("Duplicated ModBlocks in MainRegistry (count > 1)", duplicate_blocks)
    print_section("Duplicated ModItems in MainRegistry (count > 1)", duplicate_items)

    has_issues = bool(missing_blocks or missing_items or duplicate_blocks or duplicate_items)
    if args.fail_on_issues and has_issues:
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
