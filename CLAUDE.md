# VAIA Android

App móvil de VAIA (Viajes Asistidos por IA). Kotlin + Jetpack Compose, módulo único, Clean Architecture.

## Build y test

Esta máquina no tiene Java en el PATH. Exportar el JBR de Android Studio antes de cualquier comando Gradle:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

```bash
./gradlew :app:testDebugUnitTest    # tests unitarios
./gradlew :app:compileDebugKotlin   # chequeo rápido de compilación
./gradlew lint                      # análisis estático
./gradlew :app:assembleDebug        # APK debug
./gradlew :app:installDebug         # instalar en emulador/dispositivo
```

`adb`: `~/Library/Android/sdk/platform-tools/adb` (tampoco está en el PATH).

**Antes de dar por terminado un cambio, correr `:app:testDebugUnitTest` y `:app:assembleDebug`.**

CI (`.github/workflows/ci.yml`): corre en push/PR a `main` y `develop`. Orden: test → lint → build.

## Configuración local

`local.properties` está gitignored. Ver `local.properties.example`. Claves que lee `app/build.gradle.kts` y expone como `BuildConfig`:

| Clave | Default si falta |
|---|---|
| `API_BASE_URL` | `http://10.0.2.2:8000/api/` (localhost del emulador) |
| `MAPS_API_KEY` | `""` — Places se desactiva sin crashear |
| `GOOGLE_DRIVE_CLIENT_ID` | `""` |

Dispositivo físico: `API_BASE_URL=http://192.168.x.x:8000/api/`. En CI las keys se inyectan desde secrets.

## Arquitectura

```
app/src/main/java/com/vaia/
├── presentation/   ui/ (Compose, una carpeta por feature), viewmodel/, navigation/
├── domain/         model/, repository/ (interfaces), usecase/
├── data/           api/, repository/ (impls), local/ (Room, DataStore), network/, sync/, integration/
├── di/             NetworkModule, RepositoryModule (Hilt) + AppContainer
├── worker/         WorkManager (recordatorios de actividades)
└── fcm/            push
```

- Las interfaces de repositorio viven en `domain/repository/`, las implementaciones en `data/repository/`.
- Convivencia de Hilt (`di/NetworkModule.kt`, `di/RepositoryModule.kt`) con un `AppContainer` manual. Al agregar una dependencia, seguir el patrón del vecino más cercano en vez de asumir cuál manda.

## Room

- `data/local/db/VaiaDatabase.kt`, actualmente `version = 8`.
- **Usa `fallbackToDestructiveMigration()`**: cualquier cambio de schema borra la base local del usuario. Si tocás una `@Entity`, subí el `version` y avisá explícitamente que los datos locales se pierden.

## Red y errores

El backend responde errores como:

```json
{ "success": false, "message": "Error al crear la cuenta. Por favor, intentá más tarde." }
```

- `data/network/ErrorInterceptor.kt` — traduce respuestas a excepciones; el `message` se muestra al usuario.
- `presentation/ui/common/AppExceptionMapper.kt` — mapea excepciones a texto de UI. Tiene tests.
- `data/api/VaiaApiService.kt` (endpoints), `data/api/DTOs.kt`, `data/MockInterceptor.kt` (modo demo).

## Tests

- `app/src/test/java/com/vaia/`, JUnit 4.
- `testutils/MainDispatcherRule.kt` para ViewModels con corrutinas.
- `isReturnDefaultValues = true`: se puede usar `android.util.Log` en código bajo test sin mockearlo.
- Cobertura actual concentrada en ViewModels, repos con lógica offline y cálculos de dominio (monedas, insights, fechas). Un cambio en esas áreas debería traer test.

## Convenciones

- **Código en inglés** (clases, funciones, variables). **Texto de UI en español rioplatense** — voseo: "intentá", "revisá", "agregá". Nunca "intenta"/"revisa".
- Commits: conventional commits **con descripción en español**.
  ```
  fix(presupuesto): moneda propia por viaje para que el total sea comparable
  feat(theme): activar dynamic color por defecto
  ```
- Material 3. Colores y tipografía salen del theme (`presentation/ui/theme/`), no hardcodeados. Componentes compartidos en `presentation/ui/common/`.

## Diseño y auditorías de UI

- Sistema de diseño: `design.md`. Colores en `presentation/ui/theme/Color.kt`, theme en `Theme.kt`.
- Para auditar pantallas: leer los archivos de `presentation/ui/<feature>/` y verificar uso de Material 3, tokens del theme (nada de colores literales), content descriptions y contraste.
- Las auditorías se documentan en `docs/audit/`.

## Git

- `main`: rama de desarrollo activa.
- `production`: protegida, se promueve desde `main` vía PR aprobado.
- Trabajar en branch (`fix/...`, `feat/...`) y abrir PR contra `main`.

## Toolchain

Gradle 8.11 · AGP 8.7.3 · Kotlin 2.0.21 (plugin Compose de Kotlin, sin `composeCompiler` aparte) · KSP para Hilt y Room · JDK 17 en CI · minSdk 24 / targetSdk 34 / compileSdk 34 · `jvmTarget = 1.8` con core library desugaring.
