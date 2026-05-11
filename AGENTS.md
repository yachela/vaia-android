# VAIA Android App

## Build Commands

```bash
./gradlew test                    # Unit tests
./gradlew lint                   # Static analysis
./gradlew assembleDebug          # Debug APK
```

CI order (from `.github/workflows/ci.yml`): test → lint → build

## UI/Design Workflow

### Auditorías de UI

Cuando se requiera auditar pantallas o cambios de diseño:

1. **Cargar skill**: Usar `mobile-android-design` skill
2. **Explorar pantallas**: Leer archivos en `app/src/main/java/com/vaia/presentation/ui/`
3. **Analizar**: Verificar uso de Material 3, accesibilidad, colors
4. **Reportar**: Documentar hallazgos

### Commits de UI

Para cambios de diseño/UI, seguir conventional commits en español:

```
feat(theme): activar dynamic color por defecto
fix(accessibility): agregar content descriptions
docs(design): agregar design.md con sistema de diseño
```

### Design System

- **Archivo**: `design.md` - Sistema de diseño completo
- **Colors**: `app/src/main/java/com/vaia/presentation/ui/theme/Color.kt`
- **Theme**: `app/src/main/java/com/vaia/presentation/ui/theme/Theme.kt`
- **Components**: `app/src/main/java/com/vaia/presentation/ui/common/`

## API Configuration

- **Production**: hardcoded in `app/build.gradle.kts:29`
- **Local dev**: override in `local.properties`:
  - Emulator: `API_BASE_URL=http://10.0.2.2:8000/api/`
  - Physical device: `API_BASE_URL=http://192.168.x.x:8000/api/`

### Error Handling

El backend devuelve errores en formato:

```json
{
  "success": false,
  "message": "Error al crear la cuenta. Por favor, intentá más tarde."
}
```

El `success: false` indica error, y `message` se muestra en Toast.

- **ErrorInterceptor**: `app/src/main/java/com/vaia/data/network/ErrorInterceptor.kt`
- **NetworkModule**: `app/src/main/java/com/vaia/di/NetworkModule.kt` (agrega el interceptor al OkHttpClient)

### API Layers

- **DTOs**: `app/src/main/java/com/vaia/data/api/DTOs.kt`
- **Service**: `app/src/main/java/com/vaia/data/api/VaiaApiService.kt` (definiciones de endpoints)
- **MockInterceptor**: `app/src/main/java/com/vaia/data/MockInterceptor.kt` (respuestas mock para demo)

## Testing

- Location: `app/src/test/java/com/vaia/`
- Custom rule: `app/src/test/java/com/vaia/testutils/MainDispatcherRule.kt` (sets coroutines dispatcher)
- Framework: JUnit 4

## Architecture

- Single-module Android app (Clean Architecture)
- Layers: `presentation/` (UI + ViewModels), `domain/` (use cases + models), `data/` (repositories + API)
- Entry point: `MainActivity.kt`
- Packages: `com.vaia.data.*`, `com.vaia.domain.*`, `com.vaia.presentation.*`

## Toolchain

- Gradle 8.13, Kotlin 1.9.24, Compose compiler 1.5.14
- KSP for Hilt/Room annotation processing
- Min SDK 24, Target SDK 34

## Key Files

- `app/build.gradle.kts` - App config, dependencies
- `settings.gradle.kts` - Plugin versions
- `local.properties` - SDK path, local API URL
- `app/src/main/java/com/vaia/di/` - Hilt modules