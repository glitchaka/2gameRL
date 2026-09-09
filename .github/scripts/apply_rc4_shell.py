from pathlib import Path


def replace(path, old, new):
    p=Path(path)
    s=p.read_text(encoding='utf-8')
    if old not in s:
        raise SystemExit(f'missing token in {path}: {old[:100]}')
    p.write_text(s.replace(old,new),encoding='utf-8',newline='\n')

# Asset grid selection belongs to the file tile itself, not its thumbnail.
replace(
    'src/main/java/com/buttclapdev/twogamerl/studio/AssetExplorerPane.java',
    'box.setAlignment(Pos.TOP_LEFT);box.setPrefSize(132,126);box.getStyleClass().add("resource-asset-tile");if(selectedKeys.contains(asset.key))box.getStyleClass().add("selected");',
    'box.setAlignment(Pos.TOP_LEFT);box.setPrefSize(132,126);box.getStyleClass().add("resource-asset-tile");box.getProperties().put("asset-key",asset.key);if(selectedKeys.contains(asset.key))box.getStyleClass().add("selected");'
)
replace(
    'src/main/java/com/buttclapdev/twogamerl/studio/AssetExplorerPane.java',
    'if(favoritesMode){breadcrumbs.getChildren().addAll(new Label("›"),breadcrumb("Favoritos",""){{setOnAction(e->{favoritesMode=true;rebuild();});}});return;}',
    'if(favoritesMode){Button favorites=breadcrumb("Favoritos","");favorites.setOnAction(e->{favoritesMode=true;rebuild();});breadcrumbs.getChildren().addAll(new Label("›"),favorites);return;}'
)

# Product/version identity.
replace('pom.xml','<version>2.3.0-rc3</version>','<version>2.3.0-rc4</version>')
replace('src/main/java/com/buttclapdev/twogamerl/studio/StudioApp.java','2gameRL Studio 2.3 RC3 · ','2gameRL Studio 2.3 RC4 · ')

# Human-facing help must not expose implementation enum constants.
replace(
    'src/main/java/com/buttclapdev/twogamerl/studio/ProjectSettingsDialog.java',
    'Label explain=hint("PIXEL_PERFECT usa la resolución lógica y amplía por factores enteros con nearest-neighbor. En una pantalla 1920×1080, un juego 320×180 se muestra a 6×. Si no cabe exacto, queda centrado con letterbox en vez de deformarse.");',
    'Label explain=hint("Píxel perfecto usa la resolución lógica y amplía por factores enteros sin suavizado. En una pantalla 1920×1080, un juego 320×180 se muestra a 6×. Si no cabe exacto, queda centrado con bandas en vez de deformarse.");'
)

# Run the existing RC gate on this branch once the implementation is complete.
p=Path('.github/workflows/rc-validation.yml')
s=p.read_text(encoding='utf-8')
needle="      - 'hotfix/2.3.0-rc3-resource-explorer'"
if "hotfix/2.3.0-rc4-studio-shell" not in s:
    if needle not in s: raise SystemExit('rc validation branch token missing')
    s=s.replace(needle,needle+"\n      - 'hotfix/2.3.0-rc4-studio-shell'")
p.write_text(s,encoding='utf-8',newline='\n')
