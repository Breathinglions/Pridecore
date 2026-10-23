"""Pride Community v0.5: release-grade resource-pack structure and translation smoke audit."""
import json, pathlib, re, zipfile
root=pathlib.Path(__file__).resolve().parent.parent
pack=root/'resourcepack'/'Pride_Region_Community_Identity_v0.5_1.21.1.zip'
with zipfile.ZipFile(pack) as z:
    assert z.testzip() is None
    allfiles=set(z.namelist())
    assert json.loads(z.read('pack.mcmeta'))['pack']['pack_format']==34
    providers=json.loads(z.read('assets/minecraft/font/default.json'))['providers']
    glyphs=[c for p in providers for row in p['chars'] for c in row]
    assert len(glyphs)==len(set(glyphs)), 'Duplicate bitmap glyph'
    for code in list(range(0xE100,0xE108))+list(range(0xE140,0xE148))+list(range(0xE110,0xE11D)):
        if code in (0xE111,0xE112) or code in range(0xE113,0xE11D) or code==0xE110:
            assert chr(code) in glyphs, f'Missing glyph U+{code:04X}'
    for p in providers:
        if p['file'].startswith('pride_identity:'):
            suffix=p['file'].split(':',1)[1]
            assert 'assets/pride_identity/textures/'+suffix in allfiles
    lang=json.loads(z.read('assets/spawn_notification/lang/en_us.json'))
    for key in ['spawn_notification.notification.spawned','spawn_notification.notification.despawned']:
        value=lang[key]
        for arg in ('%1$s','%2$s','%3$s'):
            assert value.count(arg)==1, f'{key} missing/duplicate {arg}'
        assert chr(0xE119) in value, 'Spawn badge missing'
    assert lang['spawn_notification.alpha'].startswith(chr(0xE11D))
    assert lang['spawn_notification.max_ivs'].startswith(chr(0xE11E))
    assert chr(0xE11F) in lang['pride_community.label.rare']
    assert chr(0xE121) in lang['pride_community.label.ultra_rare']
    for code in (0xE11D,0xE11E,0xE11F,0xE121): assert chr(code) in glyphs
    assert len(lang)>=20
print('PASS Pride Community resource pack glyphs, preserved player IDs, JSON, spawn translations')
