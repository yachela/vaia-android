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
5. Ejecuta la aplicación

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
