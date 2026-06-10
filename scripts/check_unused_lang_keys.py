#!/usr/bin/env python3
"""
Находит ключи локализации из ModLanguageProvider.java, которые нигде не
используются, кроме самого провайдера и JSON-файлов lang.

Источник ключей:
  - строковые литералы в add("key", "value");
  - add(ModItems.SYMBOL.get()) / add(ModBlocks.SYMBOL.get()) → item./block.hbm_m.*;
  - при --merge-generated-lang: дополняет item./block. из en_us.json (циклы по enum).

Поиск использования: все текстовые файлы в src/,
кроме ModLanguageProvider.java и **/lang/*.json.

Исключения (не проверяются): text.autoconfig, fluid.hbm_m, door.skin,
damage.type, death.attack.

Для остальных: сначала точное совпадение; если его нет — частичное
(подстрока из ≥2 сегментов, разделённых точкой). При частичном совпадении
выводится ключ провайдера и найденные строки с максимальной точностью
(если есть совпадение на 3+ сегментах, 2-сегментные не показываются).
"""

from __future__ import annotations

import argparse
import json
import re
from dataclasses import dataclass
from pathlib import Path

MODID = "hbm_m"

PROVIDER_REL = Path("src/main/java/com/hbm_m/datagen/ModLanguageProvider.java")
MOD_ITEMS_REL = Path("src/main/java/com/hbm_m/item/ModItems.java")
MOD_BLOCKS_REL = Path("src/main/java/com/hbm_m/block/ModBlocks.java")
GENERATED_LANG_REL = Path(
    "src/generated/resources/assets/hbm_m/lang/en_us.json"
)

# Префиксы ключей, которые не проверяем (implicit / autoconfig / datagen-only).
EXCLUDED_PREFIXES = (
    "text.autoconfig",
    f"fluid.{MODID}",
    "door.skin",
    "damage.type",
    "death.attack",
    "tooltip.hbm_m.upgrade.type",
    "tooltip.hbm_m.screwdriver",
)

LITERAL_ADD_RE = re.compile(
    r'add\s*\(\s*"((?:[^"\\]|\\.)*)"\s*,',
)
MOD_REGISTRY_ADD_RE = re.compile(
    r"add\s*\(\s*Mod(Items|Blocks)\.([A-Z0-9_]+)\.get\(\)",
)
ITEM_REGISTER_RE = re.compile(
    r"RegistrySupplier<Item>\s+([A-Z0-9_]+)\s*=\s*ITEMS\.register\(\s*\"([^\"]+)\"",
)
BLOCK_REGISTER_RE = re.compile(
    r"RegistrySupplier<Block>\s+([A-Z0-9_]+)\s*=\s*registerBlock\(\s*\"([^\"]+)\"",
)
BLOCK_REGISTER_ALT_RE = re.compile(
    r"RegistrySupplier<Block>\s+([A-Z0-9_]+)\s*=\s*BLOCKS\.register\(\s*\"([^\"]+)\"",
)
QUOTED_KEY_LIKE_RE = re.compile(r'"([a-z][a-z0-9_.]*)"')

TEXT_SUFFIXES = {
    ".java",
    ".kt",
    ".kts",
    ".json",
    ".mcmeta",
    ".lang",
    ".txt",
    ".md",
    ".xml",
    ".cfg",
    ".accesswidener",
    ".toml",
    ".properties",
    ".mixins.json",
}


@dataclass(frozen=True)
class KeyUsage:
    exact: bool
    partial_matches: tuple[str, ...]


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def is_lang_json(path: Path) -> bool:
    parts = {p.lower() for p in path.parts}
    return path.suffix.lower() == ".json" and "lang" in parts


def is_excluded_key(key: str) -> bool:
    return any(key.startswith(prefix) for prefix in EXCLUDED_PREFIXES)


def build_registry_map(items_text: str, blocks_text: str) -> dict[str, str]:
    """ModItems.SYMBOL / ModBlocks.SYMBOL → registry name."""
    mapping: dict[str, str] = {}
    for symbol, name in ITEM_REGISTER_RE.findall(items_text):
        mapping[f"ModItems.{symbol}"] = name
    for symbol, name in BLOCK_REGISTER_RE.findall(blocks_text):
        mapping[f"ModBlocks.{symbol}"] = name
    for symbol, name in BLOCK_REGISTER_ALT_RE.findall(blocks_text):
        mapping[f"ModBlocks.{symbol}"] = name
    return mapping


def extract_keys_from_provider(
    provider_text: str,
    registry_map: dict[str, str],
) -> set[str]:
    keys: set[str] = set()

    for match in LITERAL_ADD_RE.finditer(provider_text):
        line_start = provider_text.rfind("\n", 0, match.start()) + 1
        line_prefix = provider_text[line_start : match.start()]
        if line_prefix.strip().startswith("//"):
            continue
        keys.add(match.group(1))

    for kind, symbol in MOD_REGISTRY_ADD_RE.findall(provider_text):
        reg = registry_map.get(f"Mod{kind}.{symbol}")
        if not reg:
            continue
        prefix = "item" if kind == "Items" else "block"
        keys.add(f"{prefix}.{MODID}.{reg}")

    return keys


def load_generated_lang_keys(path: Path) -> set[str]:
    if not path.is_file():
        return set()
    data = json.loads(read_text(path))
    return set(data.keys())


def merge_registry_keys_from_json(
    keys: set[str], json_keys: set[str]
) -> set[str]:
    """Добавляет item./block. из datagen, если их ещё нет (циклы enum)."""
    for key in json_keys:
        if key.startswith(f"item.{MODID}.") or key.startswith(f"block.{MODID}."):
            keys.add(key)
    return keys


def collect_search_corpus(
    root: Path,
    provider_path: Path,
    extra_dirs: list[Path],
) -> str:
    chunks: list[str] = []
    search_roots = [root / "src", *extra_dirs]

    for base in search_roots:
        if not base.is_dir():
            continue
        for path in base.rglob("*"):
            if not path.is_file():
                continue
            if path.suffix.lower() not in {s.lower() for s in TEXT_SUFFIXES}:
                continue
            if path.resolve() == provider_path.resolve():
                continue
            if is_lang_json(path):
                continue
            try:
                chunks.append(read_text(path))
            except (OSError, UnicodeDecodeError):
                continue

    return "\n".join(chunks)


def extract_key_like_strings(corpus: str) -> set[str]:
    """Строковые литералы в коде/ресурсах, похожие на ключи перевода."""
    result: set[str] = set()
    for match in QUOTED_KEY_LIKE_RE.finditer(corpus):
        candidate = match.group(1)
        if "." in candidate and not candidate.startswith(("http", "www.")):
            result.add(candidate)
    return result


def has_exact_usage(
    key: str,
    corpus: str,
    *,
    registry_id_fallback: bool,
) -> bool:
    if key in corpus:
        return True

    if key.startswith("itemGroup."):
        suffix = key.rsplit(".", 1)[-1]
        if suffix in corpus:
            return True

    if registry_id_fallback and key.startswith(
        (f"item.{MODID}.", f"block.{MODID}.", f"entity.{MODID}.", f"effect.{MODID}.")
    ):
        reg_name = key.split(".", 2)[-1]
        if f"{MODID}:{reg_name}" in corpus:
            return True

    return False


def find_partial_matches(
    key: str,
    corpus: str,
    corpus_keys: set[str],
) -> tuple[str, ...]:
    """Ищет в коде строки с ≥2-сегментной частью ключа; оставляет только самые точные."""
    parts = key.split(".")
    if len(parts) < 2:
        return ()

    # match -> макс. число сегментов (слов), которым удалось сопоставить ключ.
    match_scores: dict[str, int] = {}

    for start in range(len(parts)):
        for end in range(start + 2, len(parts) + 1):
            segment = ".".join(parts[start:end])
            word_count = end - start

            if f'"{segment}"' in corpus:
                prev = match_scores.get(segment, 0)
                if word_count > prev:
                    match_scores[segment] = word_count

            for candidate in corpus_keys:
                if candidate == key:
                    continue
                if segment in candidate:
                    prev = match_scores.get(candidate, 0)
                    if word_count > prev:
                        match_scores[candidate] = word_count

    if not match_scores:
        return ()

    max_words = max(match_scores.values())
    best = [match for match, words in match_scores.items() if words == max_words]
    return tuple(sorted(best, key=lambda s: (-len(s), s)))


def analyze_key_usage(
    key: str,
    corpus: str,
    corpus_keys: set[str],
    *,
    registry_id_fallback: bool,
) -> KeyUsage:
    if has_exact_usage(key, corpus, registry_id_fallback=registry_id_fallback):
        return KeyUsage(exact=True, partial_matches=())

    partial = find_partial_matches(key, corpus, corpus_keys)
    return KeyUsage(exact=False, partial_matches=partial)


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Ключи из ModLanguageProvider, не используемые вне lang JSON."
    )
    parser.add_argument(
        "--root",
        type=Path,
        default=Path("."),
        help="Корень репозитория (по умолчанию .).",
    )
    parser.add_argument(
        "--merge-generated-lang",
        action=argparse.BooleanOptionalAction,
        default=True,
        help="Добавить item./block. из сгенерированного en_us.json (циклы enum).",
    )
    parser.add_argument(
        "--no-registry-id-fallback",
        action="store_true",
        help="Не считать hbm_m:<id> использованием для item./block.",
    )
    parser.add_argument(
        "--include-implicit",
        action="store_true",
        help=(
            "Проверять item./block./entity./effect. "
            "(по умолчанию пропускаются — implicit MC registry)."
        ),
    )
    parser.add_argument(
        "--fail-on-unused",
        action="store_true",
        help="Exit code 1, если есть неиспользуемые ключи.",
    )
    args = parser.parse_args()

    root = args.root.resolve()
    provider_path = root / PROVIDER_REL
    mod_items_path = root / MOD_ITEMS_REL
    mod_blocks_path = root / MOD_BLOCKS_REL
    generated_lang_path = root / GENERATED_LANG_REL

    if not provider_path.is_file():
        print(f"Ошибка: не найден {provider_path}")
        return 2

    provider_text = read_text(provider_path)
    registry_map = build_registry_map(
        read_text(mod_items_path) if mod_items_path.is_file() else "",
        read_text(mod_blocks_path) if mod_blocks_path.is_file() else "",
    )

    keys = extract_keys_from_provider(provider_text, registry_map)

    if args.merge_generated_lang:
        json_keys = load_generated_lang_keys(generated_lang_path)
        if json_keys:
            keys = merge_registry_keys_from_json(keys, json_keys)
        else:
            print(
                "Предупреждение: en_us.json не найден — dynamic item/block ключи "
                "могут быть неполными. Запустите runData или отключите "
                "--merge-generated-lang."
            )

    implicit_prefixes = (
        f"item.{MODID}.",
        f"block.{MODID}.",
        f"entity.{MODID}.",
        f"effect.{MODID}.",
    )
    if not args.include_implicit:
        keys = {k for k in keys if not k.startswith(implicit_prefixes)}

    excluded = sorted(k for k in keys if is_excluded_key(k))
    keys = {k for k in keys if not is_excluded_key(k)}

    corpus = collect_search_corpus(root, provider_path, extra_dirs=[])
    corpus_keys = extract_key_like_strings(corpus)

    unused_plain: list[str] = []
    unused_with_hints: list[tuple[str, tuple[str, ...]]] = []

    for key in sorted(keys):
        usage = analyze_key_usage(
            key,
            corpus,
            corpus_keys,
            registry_id_fallback=not args.no_registry_id_fallback,
        )
        if usage.exact:
            continue
        if usage.partial_matches:
            unused_with_hints.append((key, usage.partial_matches))
        else:
            unused_plain.append(key)

    total_unused = len(unused_plain) + len(unused_with_hints)

    print(f"Всего ключей для проверки: {len(keys)}")
    print(f"Исключено по префиксам: {len(excluded)}")
    print(f"Не используются вне провайдера/lang: {total_unused}")
    print(f"  — без совпадений: {len(unused_plain)}")
    print(f"  — с частичными совпадениями: {len(unused_with_hints)}")
    print("-" * 50)

    if total_unused == 0:
        print("Неиспользуемых ключей не найдено.")
        return 0

    if unused_with_hints:
        print("Частичные совпадения (нет точного, но найдены похожие):")
        for key, matches in unused_with_hints:
            print(key)
            for match in matches:
                print(f"    -> {match}")
        if unused_plain:
            print("-" * 50)

    if unused_plain:
        print("Без совпадений:")
        for key in unused_plain:
            print(key)

    return 1 if args.fail_on_unused else 0


if __name__ == "__main__":
    raise SystemExit(main())
