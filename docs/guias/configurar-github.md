# Configuración final de GitHub

La sesión S5 también revisa presentación del repositorio y documentación.

## Topics sugeridos

En GitHub → página principal del repositorio → **About** → engranaje, agregar:

`spring-boot`, `spring-cloud`, `microservices`, `eureka`, `config-server`, `api-gateway`, `postgresql`, `flyway`, `docker`, `java21`, `illari-legal`

## GitHub Pages

El workflow `.github/workflows/docs.yml` publica MkDocs al hacer push a `main`.

Después del primer workflow correcto:

1. Repository **Settings**.
2. **Pages**.
3. Verificar que la rama `gh-pages` esté disponible como origen si GitHub no la selecciona automáticamente.

## Antes de subir

Verificar que `.env` NO aparezca en:

```bash
git status
```

Solo `.env.example` debe permanecer versionado.
