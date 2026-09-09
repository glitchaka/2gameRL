from pathlib import Path


def patch(path,replacements):
    p=Path(path);s=p.read_text(encoding='utf-8')
    for old,new in replacements:
        if old not in s: raise SystemExit(f'missing token in {path}: {old}')
        s=s.replace(old,new)
    p.write_text(s,encoding='utf-8',newline='\n')

patch('src/main/java/com/buttclapdev/twogamerl/studio/StudioApp.java',[
    ('chooser.showOpenDialog(stage)','StudioFileDialogs.open(chooser,stage)'),
    ('chooser.showSaveDialog(stage)','StudioFileDialogs.save(chooser,stage)'),
    ('dc.showDialog(stage)','StudioFileDialogs.directory(dc,stage)'),
])
patch('src/main/java/com/buttclapdev/twogamerl/studio/GraphicsEditorPane.java',[
    ('fc.showOpenMultipleDialog(app.owner())','StudioFileDialogs.openMultiple(fc,app.owner())'),
    ('fc.showOpenDialog(app.owner())','StudioFileDialogs.open(fc,app.owner())'),
])
patch('src/main/java/com/buttclapdev/twogamerl/studio/MenuEditorPane.java',[
    ('fc.showOpenDialog(app.owner())','StudioFileDialogs.open(fc,app.owner())'),
])
