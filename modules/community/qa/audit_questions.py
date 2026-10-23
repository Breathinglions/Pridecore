import json, pathlib
root=pathlib.Path(__file__).resolve().parent.parent
for path in (root/'src/main/resources/defaults/questions.json',root/'config/pride_community/questions.json'):
    items=json.loads(path.read_text(encoding='utf-8'))
    assert len(items)>=50
    assert len({x['id'] for x in items})==len(items)
    assert {'TRIVIA','SCRAMBLE'} <= {x['kind'] for x in items}
    for q in items:
        assert len(q['prompt']) <= 220
        assert len(q['answer']) <= 80
        assert len(q['prompt'])>0 and len(q['answer'])>0
    print(f'PASS {len(items)} unique quiz entries in {path}')
