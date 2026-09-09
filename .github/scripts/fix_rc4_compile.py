from pathlib import Path
p=Path('src/main/java/com/buttclapdev/twogamerl/studio/SceneTools222.java')
s=p.read_text(encoding='utf-8')
old='options.getChildren().addAll(insert,new Label("Patrón"),stampPicker)'
new='options.getChildren().addAll(insert,List.of(new Label("Patrón"),stampPicker))'
if old not in s: raise SystemExit('compile fix token missing')
p.write_text(s.replace(old,new),encoding='utf-8',newline='\n')
