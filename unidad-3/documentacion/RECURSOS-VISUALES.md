# Imágenes del portal

Las fotografías se crearon con la herramienta integrada **image_gen**, sin configurar claves de API en el proyecto. Son escenas ilustrativas de despachos, sin personas ni documentos reales.

| Recurso | Uso | Archivos |
| --- | --- | --- |
| Despacho de bienvenida | Portada de acceso | `frontend/portal-web/src/assets/images/illari-legal-welcome-v2-1122.jpg` y versión de 640 px |
| Asesoría | Inicio del cliente, catálogo y resumen del pago | `frontend/portal-web/src/assets/images/portada-asesoria-1200.jpg` y versión de 640 px |
| Expedientes | Inicio del abogado, administración y recursos documentales | `frontend/portal-web/src/assets/images/portada-expedientes-1200.jpg` y versión de 640 px |

Los PNG intermedios se retiraron durante la limpieza. Se conservan los JPEG optimizados usados por Angular y los prompts de las imágenes. Los iconos siguen siendo SVG del propio código, en `app/compartido/icono.ts`.

## Prompts de las imágenes nuevas

### Asesoría

Use case: photorealistic-natural. Asset type: landscape editorial photograph for a professional Peruvian legal services client portal. A refined modern legal consultation workspace, close view of an open blank contract folder, a fountain pen and two closed navy legal books on a walnut meeting table, a comfortable empty client chair softly blurred in the background, warm daylight. Tasteful navy blue, warm ivory, muted brass palette, realistic natural wood and paper textures, calm and welcoming, premium authentic office photography, wide 3:2 composition with the objects on the right half and quieter dark navy space on the left for HTML overlay. No people, no legible writing, no logos, no watermarks, no UI, no borders. Photograph only.

### Expedientes

Use case: photorealistic-natural. Asset type: landscape editorial photograph for a lawyer workspace dashboard in a legal services portal. A real elegant legal library office, close view of neatly stacked dark navy and warm beige case folders and law books on a dark walnut desk with a small brass desk lamp, bookshelf softly blurred in background, no people. Restrained warm side light, deep navy shadows and muted gold highlights, authentic paper edges and book fabric, professional and calm high-end editorial photography. Wide 3:2 composition, objects mainly in the right half, ample darker low-detail space on the left for separately rendered HTML text. Absolutely no readable text on books or folders, no lettering, no logos, no watermark, no user interface, no frame. Photograph only.

El prompt de bienvenida está en `assets/images/illari-legal-welcome-v2.prompt.txt`, dentro del frontend.
