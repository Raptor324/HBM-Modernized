"""Uebernimmt alle Soundereignisse des Originals 1:1 (Schluessel kleingeschrieben) in sounds.json,
kopiert die zugehoerigen .ogg-Dateien (Pfade kleingeschrieben, 1.20 verlangt das) und erzeugt
sound/HbmSoundsNT.java mit der Registrierung aller Ereignisse, die ModSounds noch nicht kennt.
"""
import json, os, re, shutil
from collections import OrderedDict

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
ORIG = os.path.join(os.path.dirname(ROOT), 'Hbm-s-Nuclear-Tech-GIT-master', 'Hbm-s-Nuclear-Tech-GIT-master', 'src', 'main', 'resources', 'assets', 'hbm')
PORT = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'hbm_m')
JAVA = os.path.join(ROOT, 'src', 'main', 'java', 'com', 'hbm_m', 'sound')

orig = json.load(open(os.path.join(ORIG, 'sounds.json'), encoding='utf8'), object_pairs_hook=OrderedDict)
port = json.load(open(os.path.join(PORT, 'sounds.json'), encoding='utf8'), object_pairs_hook=OrderedDict)

registered = set(re.findall(r'registerSoundEvents\("([^"]+)"\)', open(os.path.join(JAVA, 'ModSounds.java'), encoding='utf8').read()))

added, copied, missing_files = [], 0, []
for key, entry in orig.items():
    lk = key.lower()
    if lk in port:
        continue
    new = OrderedDict()
    if 'category' in entry:
        new['category'] = entry['category']
    sounds = []
    for s in entry.get('sounds', []):
        if isinstance(s, str):
            name = s
            d = None
        else:
            d = OrderedDict(s)
            name = d['name']
        src = os.path.join(ORIG, 'sounds', name + '.ogg')
        lname = name.lower()
        dst = os.path.join(PORT, 'sounds', lname + '.ogg')
        if os.path.exists(src):
            if not os.path.exists(dst):
                os.makedirs(os.path.dirname(dst), exist_ok=True)
                shutil.copyfile(src, dst)
                copied += 1
        else:
            missing_files.append(name)
        if d is None:
            sounds.append('hbm_m:' + lname)
        else:
            d['name'] = 'hbm_m:' + lname
            sounds.append(d)
    new['sounds'] = sounds
    port[lk] = new
    added.append(lk)

json.dump(port, open(os.path.join(PORT, 'sounds.json'), 'w', encoding='utf8'), indent=2, ensure_ascii=False)

all_keys = [k.lower() for k in orig.keys()]
to_register = [k for k in all_keys if k not in registered]
java = [
    'package com.hbm_m.sound;', '',
    'import java.util.HashMap;', 'import java.util.Locale;', 'import java.util.Map;', '',
    'import com.hbm_m.lib.RefStrings;', '',
    'import dev.architectury.registry.registries.RegistrySupplier;', '',
    'import net.minecraft.core.registries.BuiltInRegistries;',
    'import net.minecraft.resources.ResourceLocation;',
    'import net.minecraft.sounds.SoundEvent;', '',
    '/**',
    ' * Alle Soundereignisse des Originals unter ihrem Originalnamen (kleingeschrieben, 1.20 verlangt',
    ' * das). Erzeugt von {@code tools/restport_sounds.py} - nicht von Hand editieren.',
    ' * {@link #get(String)} nimmt den Originalschluessel ({@code "hbm:weapon.mukeExplosion"} oder',
    ' * {@code "weapon.mukeExplosion"}) und findet auch Ereignisse, die {@link ModSounds} schon registriert.',
    ' */',
    'public final class HbmSoundsNT {', '',
    '    private HbmSoundsNT() {}', '',
    '    private static final Map<String, RegistrySupplier<SoundEvent>> EVENTS = new HashMap<>();', '',
    '    private static final String[] KEYS = {',
]
for i in range(0, len(to_register), 4):
    java.append('            ' + ', '.join('"%s"' % k for k in to_register[i:i + 4]) + ',')
java += [
    '    };', '',
    '    static {',
    '        for (String key : KEYS) {',
    '            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, key);',
    '            EVENTS.put(key, ModSounds.SOUND_EVENTS.register(key, () -> SoundEvent.createVariableRangeEvent(id)));',
    '        }',
    '    }', '',
    '    /** Laedt die Klasse vor {@code SOUND_EVENTS.register()}. */',
    '    public static void init() { }', '',
    '    public static SoundEvent get(String originalKey) {',
    '        String key = originalKey.toLowerCase(Locale.ROOT);',
    '        if (key.startsWith("hbm:")) key = key.substring(4);',
    '        RegistrySupplier<SoundEvent> sup = EVENTS.get(key);',
    '        if (sup != null && sup.isPresent()) return sup.get();',
    '        return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, key));',
    '    }',
    '}', '',
]
open(os.path.join(JAVA, 'HbmSoundsNT.java'), 'w', encoding='utf8').write('\n'.join(java))
print('sounds.json +', len(added), 'Ereignisse,', copied, 'Dateien kopiert,', len(to_register), 'neu registriert')
if missing_files:
    print('Dateien fehlen im Original:', missing_files[:20], len(missing_files))
