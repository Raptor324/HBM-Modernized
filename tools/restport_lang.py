"""Pflegt datagen/RestportLang.java: Uebersetzungen 1:1 aus den .lang-Dateien des Originals.

Aufruf: python tools/restport_lang.py portKey=origKey [portKey=origKey ...]
        portKey=!English|Russisch  fuer direkte Texte.
Bereits vorhandene portKeys werden ersetzt. Fehlt der Originalschluessel in ru_RU, wird en_US genommen.
"""
import os, re, sys
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
ORIG = os.path.join(os.path.dirname(ROOT), 'Hbm-s-Nuclear-Tech-GIT-master', 'Hbm-s-Nuclear-Tech-GIT-master', 'src', 'main', 'resources', 'assets', 'hbm', 'lang')
JAVA = os.path.join(ROOT, 'src', 'main', 'java', 'com', 'hbm_m', 'datagen', 'RestportLang.java')
BS = chr(92)
PAT = re.compile(r'e\("((?:[^"\\]|\\.)*)", "((?:[^"\\]|\\.)*)", "((?:[^"\\]|\\.)*)"\);')


def load(name):
    d = {}
    with open(os.path.join(ORIG, name), encoding='utf-8', errors='ignore') as f:
        for line in f:
            line = line.rstrip('\n').rstrip('\r')
            if '=' in line and not line.startswith('#'):
                k, v = line.split('=', 1)
                d[k.strip()] = v
    return d


def jesc(s):
    return s.replace(BS, BS * 2).replace('"', BS + '"')


def main(pairs):
    en, ru = load('en_US.lang'), load('ru_RU.lang')
    entries = {}
    if os.path.exists(JAVA):
        for m in PAT.finditer(open(JAVA, encoding='utf-8').read()):
            entries[m.group(1)] = (m.group(2), m.group(3))
    missing = []
    for p in pairs:
        pk, ok = p.split('=', 1)
        if ok.startswith('!'):
            t = ok[1:].split('|')
            entries[pk] = (jesc(t[0]), jesc(t[1] if len(t) > 1 else t[0]))
            continue
        if ok not in en:
            missing.append(ok)
            continue
        entries[pk] = (jesc(en[ok]), jesc(ru.get(ok, en[ok])))
    lines = ['        e("%s", "%s", "%s");' % (k, v[0], v[1]) for k, v in sorted(entries.items())]
    head = (
        'package com.hbm_m.datagen;\n\n'
        'import java.util.function.BiConsumer;\n\n'
        '/**\n'
        ' * Uebersetzungen der Restport-Runden, 1:1 aus {@code en_US.lang}/{@code ru_RU.lang} des Originals.\n'
        ' * Wird von {@code tools/restport_lang.py} gepflegt - nicht von Hand editieren.\n'
        ' */\n'
        'public final class RestportLang {\n\n'
        '    private RestportLang() {}\n\n'
        '    private static BiConsumer<String, String> out;\n'
        '    private static boolean russian;\n\n'
        '    private static void e(String key, String en, String ru) {\n'
        '        out.accept(key, russian ? ru : en);\n'
        '    }\n\n'
        '    private static final java.util.Set<String> KEYS = new java.util.HashSet<>();\n\n'
        '    public static boolean has(String key) {\n'
        '        if (KEYS.isEmpty()) {\n'
        '            BiConsumer<String, String> prev = out;\n'
        '            boolean prevRu = russian;\n'
        '            addAll((k, v) -> KEYS.add(k), false);\n'
        '            out = prev;\n'
        '            russian = prevRu;\n'
        '        }\n'
        '        return KEYS.contains(key);\n'
        '    }\n\n'
        '    public static void addAll(BiConsumer<String, String> consumer, boolean ru) {\n'
        '        out = consumer;\n'
        '        russian = ru;\n')
    tail = '    }\n}\n'
    open(JAVA, 'w', encoding='utf-8').write(head + '\n'.join(lines) + '\n' + tail)
    if missing:
        print('FEHLT im Original:', ' '.join(missing))
    print(len(entries), 'Eintraege')


if __name__ == '__main__':
    main(sys.argv[1:])
