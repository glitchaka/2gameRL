from pathlib import Path
import re
ROOT=Path(__file__).resolve().parents[1]
def read(p):return (ROOT/p).read_text(encoding='utf-8')
def write(p,s):(ROOT/p).write_text(s,encoding='utf-8',newline='\n')
def rep(p,o,n):
 s=read(p);c=s.count(o)
 if c!=1:raise RuntimeError(f'{p}: expected 1 match got {c}: {o[:120]}')
 write(p,s.replace(o,n,1))

p='src/main/java/com/buttclapdev/twogamerl/studio/RcFeaturePack.java'
s=read(p)
s=s.replace('TextField name=field(t.name),tag=field(t.connectTag);ComboBox<TileAutotileMode>mode=', 'TextField name=field(t.name),tag=field(t.connectTag),tags=field(String.join(",",t.tags));ComboBox<TileAutotileMode>mode=',1)
s=s.replace('t.name=name.getText();t.connectTag=tag.getText().trim();t.autotileMode=', 't.name=name.getText();t.connectTag=tag.getText().trim();t.tags.clear();for(String value:tags.getText().split("[,;]")){String clean=value.trim();if(!clean.isBlank())t.tags.add(clean);}t.autotileMode=',1)
s=s.replace('name.setOnAction(e->save.run());tag.setOnAction(e->save.run());mode.setOnAction(e->save.run());', 'name.setOnAction(e->save.run());tag.setOnAction(e->save.run());tags.setOnAction(e->save.run());mode.setOnAction(e->save.run());',1)
s=s.replace('row("Nombre",name),row("Autotile",mode),row("Conecta por tag",tag),new Label("Variantes', 'row("Nombre",name),row("Tags del tile",tags),row("Autotile",mode),row("Conecta por tag",tag),new Label("Variantes',1)
old='g.setStroke(Color.web("#ffbf55"));g.strokeLine(x+size/2,y,x+size/2,y+size);g.strokeLine(x,y+size/2,x+size,y+size/2);for(FrameShape s:f.shapes)'
new='Asset pivotAsset=app.project().getAssets().get(f.assetKey);double pivotX=pivotAsset==null?.5:pivotAsset.pivotX,pivotY=pivotAsset==null?.5:pivotAsset.pivotY;g.setStroke(Color.web("#ffbf55"));g.strokeLine(x+pivotX*size,y,x+pivotX*size,y+size);g.strokeLine(x,y+pivotY*size,x+size,y+pivotY*size);for(FrameShape s:f.shapes)'
if old not in s:raise RuntimeError('Animation Lab pivot anchor not found')
s=s.replace(old,new,1)
write(p,s)

# Make the technical Bible precise about the pivot overlay.
b='docs/2GAMESCRIPT_BIBLE.md';s=read(b).replace('onion skin previo/siguiente, crosshair de pivot, markers','onion skin previo/siguiente, crosshair del `Asset.pivotX/pivotY`, markers',1);write(b,s)
print('RC phase F final authoring corrections staged')
