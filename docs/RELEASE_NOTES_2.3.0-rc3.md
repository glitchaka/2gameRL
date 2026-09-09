# 2gameRL Studio 2.3.0-rc3

RC de corrección de experiencia de autoría sobre 2.3.0-rc2.

## Asset Browser convertido en explorador de proyecto

- Se elimina la vista de árbol como interfaz principal de imágenes.
- Los recursos se presentan como archivos con miniatura en cuadrícula o lista.
- Carpetas visibles y navegables mediante doble clic y breadcrumbs.
- Carpetas vacías y anidadas persistentes dentro del proyecto.
- Crear, renombrar y eliminar carpetas desde el propio navegador.
- Arrastrar uno o varios assets a otra carpeta.
- Arrastrar carpetas personalizadas dentro de otras carpetas.
- `Ctrl+A` selecciona los archivos visibles; `Backspace` sube una carpeta; `F2` renombra la carpeta seleccionada.
- Favoritos y movimiento por menú contextual.
- Las categorías visuales antiguas se conservan como carpetas de organización inicial, pero dejan de ser una jerarquía de árbol obligatoria.

## Identidad visual

- Graphite Brass pasa a ser el tema predeterminado.
- Preferencias antiguas `MIDNIGHT` migran automáticamente a Graphite Brass.
- El acento azul del tema predeterminado deja de sobrescribir la paleta grafito/latón de `studio.css`.
- Título de ventana actualizado a `2gameRL Studio 2.3 RC3`.

## Persistencia

`rc-2.3.properties` conserva ahora también el registro de carpetas de assets, incluyendo carpetas vacías. Los proyectos 2.2.x y RC anteriores siguen siendo legibles porque estas claves son opcionales.
