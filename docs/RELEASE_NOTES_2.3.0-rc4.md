# 2gameRL Studio 2.3.0-rc4

RC4 corrige la capa de autoría visual que todavía conservaba apariencia y semántica de prototipo.

## Navegador de recursos

- El navegador de Gráficos deja de presentar carpetas como tarjetas grandes con un icono.
- Vista predeterminada de detalles con filas de archivo, columnas Nombre / Tipo / Ubicación y miniaturas reales para assets.
- Barra lateral de proyecto sin árbol jerárquico: Assets, Favoritos, carpetas estándar y carpetas raíz creadas por el usuario.
- Navegación por doble clic, breadcrumbs y botón Subir.
- Vista alternativa de miniaturas conserva las carpetas como filas compactas y reserva las tarjetas para archivos gráficos.
- Carpetas persistentes, anidadas y vacías.
- Drag & drop de uno o varios assets entre carpetas.
- Drag & drop de carpetas personalizadas dentro de otras carpetas.
- F2 renombra carpetas; Ctrl+A selecciona los archivos visibles; Backspace sube un nivel.

## Controles del Studio

- Botones principales y herramientas reciben iconografía vectorial SVG y jerarquía visual por contexto.
- Selección, dibujo, relleno, borrado y objeto pasan a herramientas compactas de icono con tooltip.
- Comandos de workspace y acciones frecuentes dejan de depender de caracteres Unicode como sustitutos de iconos.
- Nueva hoja `studio-rc4.css` aplicada a todas las ventanas del Studio.

## Textos de interfaz

- Los ComboBox de enums ya no muestran nombres internos como `FULLSCREEN_BORDERLESS`, `PIXEL_PERFECT`, `KEEP_ASPECT`, etc.
- Las opciones se presentan con etiquetas humanas: “Pantalla completa sin bordes”, “Píxel perfecto”, “Mantener proporción”, “Píxel (sin suavizado)”, etc.
- El texto explicativo de Pantalla y render también elimina nombres de implementación.

## Tecnología

Se mantiene JavaFX. El problema corregido era la capa de controles y autoría, no una imposibilidad del toolkit; RC4 cambia la implementación visual sin introducir una reescritura de motor o runtime.
