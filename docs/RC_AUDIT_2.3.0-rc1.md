# Auditoría de Release Candidate — 2gameRL Studio 2.3.0-rc1

## Alcance

Auditoría agresiva realizada contra `docs/VISUAL_PIPELINE_2.2.0.md`, la Biblia técnica, el runtime, persistencia y herramientas de autoría. La RC parte de la 2.2.2 validada y conserva JavaFX como backend, tal como exige la especificación.

## Matriz de cierre

| Requisito | Estado RC | Implementación | Validación prevista |
|---|---|---|---|
| Autotile 4-way/8-way | Implementado | `TileDef`, `AdvancedTileResolver`, Tileset Lab | máscaras, fallback 8→4, gran grilla |
| Variantes aleatorias ponderadas | Implementado | `TileVariant`, resolver determinista | distribución/estabilidad por celda |
| Stamp/patrón | Implementado | `StampPattern`, Stamp Lab, herramienta de escena | persistencia + colocación |
| Animation reverse/FPS | Implementado | `AnimationClip.reverse/fps` | runtime smoke |
| Preview animación | Implementado | Animation Lab | arranque Studio + unit/runtime |
| Onion skin | Implementado | Animation Lab | autoría, sin efecto runtime |
| Pivot preview | Implementado | Animation Lab | overlay visual |
| Hitbox/Hurtbox por frame | Implementado | `FrameShape`, runtime de solapes | daño único/frame + eventos |
| Animator parameters | Implementado | BOOL/INT/FLOAT/TRIGGER | transición + trigger |
| Blend tree 1D/2D | Implementado | nearest-child determinista | runtime smoke |
| Animation Sets 2/4/8 | Implementado | `AnimationSet` | dirección + flip fallback |
| UISkin reutilizable | Implementado | `UISkinAsset`, UI Skin Lab | persistencia + runtime |
| Estados UI pressed/disabled | Implementado | UISkin + MenuButton.enabled | runtime/menu |
| Nine-slice | Ya existía en 2.2.2 | Asset/TextOverlay | regresión 2.2.2 |
| Favoritos/carpetas virtuales | Implementado | Asset metadata + Browser | persistencia |
| Dónde se usa | Ya existía; ampliado | `ResourceIntegrity` | cobertura de RC resources |
| Reemplazo seguro | Implementado | `ResourceIntegrity.replaceAsset` | test transversal |
| Reimportación | Ya existía | GraphicsEditorPane | regresión existente |
| Camera bounds | Implementado | Camera2D min/max | runtime smoke |
| Zoom animado | Implementado | Camera2D.zoomSpeed | runtime smoke |
| Dead zone/smoothing/shake | Ya existía en 2.2.2 | Camera2D | regresión 2.2.2 |
| `while` | Implementado acotado | ScriptProgram | stop condition/wait/runaway |
| Partículas | Cerrado en 2.2.2 | hotfix publicado | batería 2.2.2 obligatoria |
| Audio | Cerrado en 2.2.2 | AudioManager | regresión existente |
| Backend GPU | Fuera del RC | especificación lo separa | no aplica |
| Submáquinas Animator | Fuera del RC | la especificación las marca futuras | no aplica |

## Gate de RC

La rama no se considera candidata publicable hasta superar, en Windows:

1. `mvn clean test package` con tests existentes + RC.
2. Smoke determinista de timers/colisiones.
3. Pipeline visual 2.2.
4. Smoke agresivo 2.2.2 de partículas/cámara/prefab.
5. Smoke 2.3 RC de Animator, direcciones, reverse/FPS, hitbox/hurtbox y cámara.
6. 100 round-trips aleatorios de proyectos RC.
7. sidecar RC corrupto/ausente.
8. grilla autotile 256×256 resuelta repetidamente de forma determinista.
9. runtime general, componentes y ScenePortal.
10. empaquetado de Studio Windows.
11. arranque real del Studio empaquetado.
12. exportación con toolchain embebida.
13. arranque real del juego exportado.
14. auditoría de referencias rotas.

Cualquier fallo bloquea el merge/release y debe corregirse en la rama RC antes de repetir el gate.
