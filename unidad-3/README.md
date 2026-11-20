# ILLARI LEGAL

Portal de servicios legales con tres espacios: **cliente**, **abogado** y **administrador**. El frontend está hecho en Angular; los módulos del servidor usan Java y Spring Boot.

[Presentación de la defensa](documentacion/exposicion/ILLARI_LEGAL_U3_Defensa.pptx) y [guion de ambos integrantes](documentacion/exposicion/Guion_de_exposicion_ILLARI_LEGAL_U3.docx).

## Dónde está cada parte

```text
frontend/portal-web/               Interfaz de los tres roles
backend/
  compartido/                     Correlación, registros seguros y formato de eventos
  expedientes/                    Casos, abogados, documentos y agenda jurídica
  microservicios/
    clientes/                     Perfil de clientes de compras
    catalogo-servicios/            Servicios, categorías, precios y disponibilidad
    pedidos/                      Pedidos de uno o varios servicios
    pagos/                        Pago con Yape y confirmación del resultado
infraestructura/
  autenticacion/                  Realm y roles de Keycloak
  puerta-enlace/                  Entrada común a las API (Gateway)
  configuracion-central/          Configuración compartida (Config Server)
  registro-servicios/             Registro y descubrimiento (Eureka)
  acceso-publico/                 HTTPS con Cloudflare y proxy Nginx
  observabilidad/                 Métricas, logs y disponibilidad
  docker/                        Construcción de las imágenes Java
base-de-datos/                    SQL e inicialización de PostgreSQL
herramientas/                     Comandos para iniciar, detener y actualizar
documentacion/                    Guías de ubicación y operación
credenciales/                     Accesos y variables locales; excluidos de Git
pom.xml                          Compilación central de los nueve módulos Java
docker-compose.yml                Servicios y conexiones de Docker
```

## Uso diario

En una copia nueva, empieza con los pasos de preparación y compilación del [manual de primera instalación](documentacion/OPERACION.md#primera-instalación).

Desde PowerShell en la carpeta del proyecto:

```powershell
.\herramientas\proyecto.ps1 iniciar
.\herramientas\proyecto.ps1 estado
.\herramientas\proyecto.ps1 verificar
.\herramientas\proyecto.ps1 detener
```

El portal local está en **http://localhost:4200**. El comando `iniciar` muestra la URL pública y sincroniza los accesos cuando está configurado el túnel.

- [Borrador del producto final de Unidad III](u3-producto.md)
- [Guía para encontrar el código y exponer](documentacion/GUIA-DEL-PROYECTO.md)
- [Organización, código compartido y limpieza](documentacion/ORGANIZACION-DEL-CODIGO.md)
- [Instalación, compilación y actualización de la URL](documentacion/OPERACION.md)
- [Observabilidad, enlaces locales y demostración con Grafana](documentacion/OBSERVABILIDAD.md)
- [Imágenes utilizadas y sus prompts](documentacion/RECURSOS-VISUALES.md)
- [Verificación de los recorridos y del pago con Yape](documentacion/VERIFICACION-DEL-PORTAL.md)
- Los accesos locales están en `credenciales/USUARIOS-ILLARI.md`.

## Espacios del portal

| Rol | Funciones disponibles |
| --- | --- |
| Cliente | Inicio, catálogo con varios servicios, pago con Yape, historial, expedientes propios y descarga de documentos. |
| Abogado | Casos asignados, seguimiento del estado y audiencia, documentos y agenda. |
| Administrador | Catálogo, creación y edición de expedientes, asignación de cliente y abogado, registro de personas con acceso al portal y edición de sus datos. |

El catálogo incluye **Guía de bienvenida digital**, un PDF de uso del portal por **S/ 1.24**. Su descarga se habilita al titular cuando el servidor confirma el pago; los pedidos pendientes no entregan el archivo. El importe y el ambiente se muestran antes de confirmar Yape.

El primer intento de S/ 1 fue rechazado por el proveedor. El portal tiene ahora un umbral de S/ 1.24 para continuar a Yape. El 19 de noviembre de 2026 se confirmó un pago real por S/ 1.24: Mercado Pago lo acreditó, el pedido 15 quedó PAGADA y el PDF se descargó desde la cuenta del titular. Los pedidos anteriores conservan su importe. Este resultado valida esa operación y no establece un mínimo universal del proveedor; ver [diagnóstico y confirmación de Yape](documentacion/INCIDENCIA-YAPE.md).

Las imágenes del portal se crearon con el generador de imágenes integrado. Los archivos optimizados y sus prompts están en `frontend/portal-web/src/assets/images/`.
