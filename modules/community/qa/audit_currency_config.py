import json, pathlib
base=pathlib.Path(__file__).resolve().parent.parent
for path in (base/'src/main/resources/defaults/config.json', base/'config/pride_community/config.json'):
    obj=json.loads(path.read_text(encoding='utf-8'))
    settings=obj['rewards']
    assert settings['currencySymbol'] == chr(0xE122), 'Currency glyph not correctly encoded'
    assert settings['economyBackend']=='IMPACTOR'
    assert settings['payoutsEnabled'] is False
    assert settings['tiers']['SCRAMBLE'] > 0
    print('PASS PokéDollar config:', path)
