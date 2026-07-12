# TODO: Tareas Pendientes – VAIA Android

## 🤖 Track 2: Sugerencias de IA y Smart Budget (Servidor)

- [ ] **Diagnosticar error 500/503 en Azure:**
  - Los endpoints `/suggestions` y `/budget-advice` del servidor Azure (`vaia-api-awdzg7d9hpfxbmdr.brazilsouth-01.azurewebsites.net`) están fallando.
  - El frontend ya cuenta con un **motor de fallback local/offline** activo que calcula el presupuesto y ofrece sugerencias locales, pero la integración con la API real sigue rota en el backend.
  - **Instrucciones para debuggear:**
    1. Revisar los archivos de log en Azure App Service (PHP/Nginx logs en `/home/LogFiles/`).
    2. Comprobar que las variables de entorno de la API Key de Gemini/OpenAI estén configuradas y vigentes.
    3. Inspeccionar el **Logcat** en Android Studio filtrando por el tag `[API_DIAGNOSTIC]` o `OkHttp` (ya que se habilitó `HttpLoggingInterceptor.Level.BODY` para ver los requests y responses completos).
