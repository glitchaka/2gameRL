# 2gameRL Studio 2.2.2

## Runtime
- ParticleEmitter2D reparado de extremo a extremo: resolución canónica de presets, diagnóstico de referencias inexistentes, Burst rearmable, dirección configurable y `localSpace` funcional.
- Componentes añadidos a instancias de Prefab se preservan al resolver el Prefab en runtime.
- Camera2D añade smoothing, dead zone y shake; `cameraShake` está disponible desde 2GameScript.
- Audio WAV integrado con SFX concurrentes, música exclusiva, loop y volumen.
- Nine-Slice disponible en Assets y en skins de TextStyle.

## Studio
- ParticlePreset usa un único Nombre/ID canónico y renombrarlo migra referencias.
- Particle Studio con preview continuo/Burst, Sprite/Pixel, dirección, spread, gravedad, escala y opacidad.
- Selectores tipados para ParticleEmitter2D.preset y Camera2D.target.
- Línea, Rectángulo (Shift rellena) y Cuentagotas en el editor de escena.
- Guías de viewport y dead-zone de Camera2D.
- Navegador global de recursos.
- Importación y preview de WAV.
- Editor de márgenes Nine-Slice.
- Validación semántica de referencias de proyecto y autocompletado contextual ampliado.

## 2GameScript
Nuevos comandos: `playSound`, `stopSound`, `playMusic`, `stopMusic`, `cameraShake`.

## Documentación
La Biblia 2.2.2 se reemplaza por una especificación técnica canónica con eventos, comandos, propiedades, componentes, recursos, límites y ejemplos de uso.

## Compatibilidad
Formato de proyecto 8 con lectura compatible de propiedades ausentes en proyectos anteriores.

## Hotfix de partículas 2.2.2

- Pixel/Sprite pasa a ser una propiedad real y persistente de `ParticlePreset`.
- Pixel admite forma (`SQUARE`, `CIRCLE`, `DIAMOND`) y color ARGB.
- Se corrige la semántica local/world-space usando la posición visual interpolada del emisor.
- Se corrige el reinicio de Burst al reactivar `playing` y al recuperar el mismo preset después de una referencia inválida.
- Dirección, dispersión, escala y opacidad usan la misma semántica en preview y runtime.
- Se añaden límites defensivos y diagnóstico ante emisión extrema.
- Se endurece el smoke de Windows para cubrir reinicio, recuperación de referencia, local/world, dirección y flood guard.

