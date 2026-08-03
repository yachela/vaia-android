# VAIA - Aplicación Móvil

Aplicación móvil para VAIA (Viajes Asistidos por IA) desarrollada con Kotlin y Jetpack Compose.

## Screenshots

<!-- Add screenshots here -->
<!-- ![Home Screen](docs/screenshots/home.png) -->
<!-- ![Trip Details](docs/screenshots/trip-details.png) -->

## Arquitectura

La aplicación sigue los principios de Clean Architecture con las siguientes capas:

- **Presentation**: UI con Jetpack Compose y ViewModels
- **Domain**: Casos de uso y modelos de dominio
- **Data**: Repositorios, APIs y almacenamiento local

## Características Implementadas

### Autenticación
- Login con email y contraseña
- Registro de nuevos usuarios
- Gestión de tokens JWT
- Almacenamiento seguro con DataStore

### Gestión de Viajes
- Lista de viajes del usuario
- Vista detallada de viajes
- Creación de nuevos viajes
- Información de presupuesto y gastos

### Funcionalidades Pendientes
- Gestión de actividades por viaje
- Gestión de gastos con subida de imágenes
- Navegación completa entre pantallas

## Flujo de Trabajo en Git

Para el desarrollo del proyecto, se utiliza la siguiente estrategia de ramas:
- **`main`**: Rama de desarrollo activa. El equipo realiza pushes y commits directos sobre ella.
- **`production`**: Rama protegida que representa el código estable en producción. Todos los despliegues a producción se realizan desde aquí y los cambios provenientes de `main` deben promoverse mediante **Pull Requests**.

## Configuración del Entorno

### Prerrequisitos
- Android Studio Arctic Fox o superior
- JDK 11 o superior
- Dispositivo Android o emulador con API 24+

### Configuración
1. Clona el repositorio
2. Abre el proyecto en Android Studio
3. Sincroniza Gradle
4. Configura la URL de la API en `local.properties` (o usa el valor por defecto):
   ```properties
   # Emulador Android (por defecto)
   API_BASE_URL=http://10.0.2.2:8000/api/

   # Dispositivo físico (ejemplo)
   # API_BASE_URL=http://192.168.0.4:8000/api/
   ```
5. Configura las keys de Google Maps y Google Drive en `local.properties` (ver sección siguiente).
6. El repositorio incluye un `app/google-services.json` dummy para CI/build/test; reemplázalo por uno real en entornos que necesiten servicios de Firebase/Google.
7. Ejecuta la aplicación

### Google Maps y Google Drive (keys locales)

Google Maps y el Sign-In de Google Drive se configuran mediante dos propiedades en `local.properties`:

```properties
MAPS_API_KEY=<tu_api_key_de_google_maps>
GOOGLE_DRIVE_CLIENT_ID=<tu_oauth_client_id>
```

Detalles importantes:

- **`local.properties` no se versiona** (está en `.gitignore`): cada miembro del equipo debe crear el suyo. Usá `local.properties.example` como plantilla:
  ```bash
  cp local.properties.example local.properties
  ```
- Los **valores reales** de las keys se comparten por un canal seguro (gestor de contraseñas del equipo), **nunca por el repo**. Son las mismas que están cargadas en los **GitHub Secrets** (`MAPS_API_KEY`, `GOOGLE_DRIVE_CLIENT_ID`), que solo usa el CI.
- Si las dejás vacías, el build compila igual, pero el mapa no carga y el Sign-In de Drive no funciona.
- Después de editar `local.properties`, reconstruí para regenerar `BuildConfig` y los `manifestPlaceholders`:
  ```bash
  ./gradlew assembleDebug
  ```

#### Sign-In de Google Drive: SHA-1 en Google Cloud

El Sign-In de Google en Android valida la combinación **package name (`com.vaia`) + SHA-1 del keystore que firma el APK**, no solo el Client ID. Como **cada desarrollador tiene su propio `debug.keystore`**, cada uno tiene un SHA-1 distinto y debe registrarlo:

1. Obtené tu SHA-1 de debug:
   ```bash
   ./gradlew signingReport
   ```
   Copiá la línea `SHA1:` del bloque `Variant: debug`.
2. Agregalo (o pedile a quien administre el proyecto que lo agregue) en **Google Cloud Console → APIs & Services → Credentials → OAuth client de tipo "Android" (package `com.vaia`) → Add fingerprint**. Un mismo client admite varios SHA-1.
3. Como el scope `DriveScopes.DRIVE` es restringido y la consent screen está en modo *Testing*, agregá tu cuenta de Google en **OAuth consent screen → Test users**.
4. Verificá que **Google Drive API** esté habilitada en el proyecto.

> Para distribuir un APK, el SHA-1 relevante es el del keystore que firma ese APK (el de debug si repartís un debug APK, o el de release / Play App Signing en producción).

## Estructura del Proyecto

```
app/src/main/java/com/vaia/
├── data/
│   ├── api/           # Servicios de API y DTOs
│   ├── local/         # Almacenamiento local
│   └── repository/    # Implementaciones de repositorios
├── domain/
│   ├── model/         # Modelos de dominio
│   ├── repository/    # Interfaces de repositorios
│   └── usecase/       # Casos de uso
├── presentation/
│   ├── ui/            # Pantallas Compose
│   └── viewmodel/     # ViewModels
├── di/                # Inyección de dependencias con Hilt
└── VaiaApplication.kt # Clase Application
```

## Tecnologías Utilizadas

- **Kotlin**: Lenguaje principal
- **Jetpack Compose**: UI declarativa
- **Hilt**: Inyección de dependencias
- **Retrofit**: Cliente HTTP
- **Moshi**: Serialización JSON
- **DataStore**: Almacenamiento local
- **Coil**: Carga de imágenes
- **Navigation Compose**: Navegación

## API Backend

La aplicación consume la API REST desarrollada en Laravel que incluye:

- Autenticación con Sanctum
- CRUD de viajes, actividades y gastos
- Subida de imágenes para recibos
- Validación de datos
- Autorización por políticas

## Estado del Desarrollo

**Completado:**
- Estructura Clean Architecture
- Configuración Hilt y navegación
- Pantallas de autenticación (login/registro)
- Pantalla principal de viajes
- Integración con API backend
- Gestión de estado con ViewModels

**En Desarrollo:**
- Pantallas de detalle de viajes
- Gestión de actividades
- Gestión de gastos con imágenes

## Ejecución

1. Asegúrate de que el backend Laravel esté ejecutándose
2. Actualiza la URL base en `NetworkModule.kt`
3. Ejecuta la aplicación desde Android Studio
4. Registra una cuenta o inicia sesión

## Notas Importantes

- La aplicación está configurada para desarrollo local
- Para producción, actualiza las URLs y configura HTTPS
- Las imágenes se almacenan localmente y se suben a la API
- La autenticación persiste entre sesiones usando DataStore
