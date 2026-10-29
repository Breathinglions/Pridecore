#!/usr/bin/env python3
"""Standalone checks that text customization cannot mutate Cobblemon native actions."""
import importlib.util
import json
import pathlib
import tempfile
import unittest
from zipfile import ZipFile

ROOT=pathlib.Path(__file__).resolve().parents[1]
p=ROOT/"campaign"/"build_native_dialogue_text_overlay.py"
spec=importlib.util.spec_from_file_location("native_text",p)
mod=importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)

class NativeTextTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory()
        self.jar=pathlib.Path(self.temp.name)/"fake.jar"
        self.action="q.player.run_command('pridecore story sync');"
        data={
            "pages":[
                {"id":"main","speaker":"professor",
                 "lines":["Hello, Trainer.", {"type":"expression","expression":"q.player.username"}],
                 "input":{"type":"option","options":[
                     {"value":"continue","text":"Continue","isVisible":"q.player.has_chosen_starter() == 1",
                      "action":[self.action,"q.dialogue.close();"]}]}}
            ]
        }
        with ZipFile(self.jar,"w") as z:
            for v in mod.NATIVE.values():
                z.writestr(v,json.dumps(data))
    def tearDown(self): self.temp.cleanup()
    def test_text_and_choice_change_without_mechanics(self):
        source=mod.template(self.jar)
        self.assertEqual(len(source["dialogues"]),len(mod.NATIVE))
        source["dialogues"]["sequoia"]["pages"]["main"]["lines"]["0"]="Welcome to Hearthleaf!"
        source["dialogues"]["sequoia"]["pages"]["main"]["choices"]["continue"]="Let's go."
        output=mod.apply(self.jar,source)
        obj=json.loads(output[mod.NATIVE["sequoia"]])
        p=obj["pages"][0]
        self.assertEqual(p["lines"][0],"Welcome to Hearthleaf!")
        self.assertEqual(p["lines"][1]["expression"],"q.player.username")
        self.assertEqual(p["input"]["options"][0]["action"][0],self.action)
        self.assertEqual(p["input"]["options"][0]["isVisible"],"q.player.has_chosen_starter() == 1")
    def test_cannot_touch_expression(self):
        c=mod.template(self.jar)
        c["dialogues"]["theo"]["pages"]["main"]["lines"]["1"]="forged"
        with self.assertRaises(ValueError): mod.apply(self.jar,c)
    def test_unknown_page_or_option_refused(self):
        c=mod.template(self.jar)
        c["dialogues"]["avery"]["pages"]["main"]["choices"]["hack"]="Grant items!"
        with self.assertRaises(ValueError): mod.apply(self.jar,c)
    def test_control_char_and_format_injection_refused(self):
        c=mod.template(self.jar)
        c["dialogues"]["mom"]["pages"]["main"]["lines"]["0"]="Welcome\nInjected"
        with self.assertRaises(ValueError): mod.apply(self.jar,c)
    def test_unknown_npc_refused(self):
        c=mod.template(self.jar)
        c["dialogues"]["hacker"]={"pages":{}}
        with self.assertRaises(ValueError): mod.apply(self.jar,c)

if __name__=="__main__":unittest.main()
