# 2gameRL Studio 2.3.0-rc2

RC visual de corrección sobre 2.3.0-rc1.

## Interfaz

- Se reemplaza la paleta azul dominante por una identidad grafito/carbón con acento latón cálido y mucho más contenido.
- Barras, paneles, menús, campos, tarjetas, tabs y superficies usan capas translúcidas y separadores de bajo contraste en lugar de bloques azules opacos.
- Estados hover, selección y foco quedan jerarquizados sin teñir toda la aplicación con el color de acento.
- Se rehace el icono 2RL para coincidir con la nueva identidad visual.
- Se eliminan los cianes incrustados en selección WYSIWYG, editor de spritesheets y previsualizador de partículas.
- Se compacta el toolbar de escena y se redistribuye ancho a la zona de trabajo para evitar etiquetas truncadas en resoluciones de escritorio habituales.
- Scrollbars, menús contextuales, diálogos, campos y tooltips reciben tratamiento visual coherente con la aplicación de escritorio.

## Alcance técnico

- No cambia el formato de proyecto ni la API pública de 2GameScript respecto de rc1.
- Mantiene JavaFX y el pipeline funcional validado en 2.3.0-rc1.
