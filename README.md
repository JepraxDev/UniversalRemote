# Universal Remote — Rediseño UX: Acceso Inmediato y Conexión Simplificada (v1.0.0-rc1)

Aplicación Android nativa en Kotlin, Jetpack Compose y Material 3 con arquitectura Clean Architecture y MVVM.
Transforma la experiencia de usuario hacia un acceso directo e inmediato: el usuario puede elegir su televisor o proyector y abrir el control remoto con un solo toque, con reconexión automática en segundo plano para Wi-Fi y carga instantánea del perfil guardado para Infrarrojo.

- **APK Debug (Pruebas locales):** `app/build/outputs/apk/debug/app-debug.apk`
- **APK Release (Candidata a versión final):** `app/build/outputs/apk/release/app-release.apk`
- **AAB Bundle (Publicación en Google Play):** `app/build/outputs/bundle/release/app-release.aab`
- **Versión de evaluación:** `1.0.0-rc1` (versionCode **8**).

---

## 1. Novedades del Rediseño UX

- **Apertura de Control en Un Toque (`OpenRemoteUseCase`):**
  - Al presionar cualquier parte principal de una tarjeta o el botón prominente **`[📱 Abrir control]`**, la app navega directamente a la pantalla de control remoto (`RemoteScreen`) e inicia la reconexión de fondo en Wi-Fi o carga el perfil IR grabado.
- **Normalización de Nombres Limpios (`DeviceDisplayNameResolver`):**
  - Sanitiza identificadores mDNS/técnicos largos (ej. `MiTV-AESP0-ff5b3a8f...` se muestra como `Android TV (TV Stick Mi)`). Mantiene los identificadores técnicos únicamente para las conexiones internas de red sin saturar la vista.
- **Pantalla Inicio Rediseñada (`HomeScreen`):**
  - Buscador rápido `"Buscar dispositivo..."`.
  - Dos tarjetas de filtrado grandes: **Televisor** *(Smart TV, Android TV, TV Stick)* y **Proyector** *(Epson, BenQ, ViewSonic, etc.)*.
  - Sección *"Dispositivos disponibles"* con acceso a *"Buscar en la red"*.
  - Tarjetas inferiores de acceso rápido: `Buscar dispositivos`, `Agregar dispositivo`, `Guía rápida`.
- **Tarjetas Compactas e Intuitivas (`DeviceCard`):**
  - Muestran icono grande, nombre corto legible, subtítulo `Marca · Modelo`, etiqueta `📍 Ubicación/Aula`, insignia `🟢 Wi-Fi · IP` / `🟣 Infrarrojo · Disponible`, botón azul **`[📱 Abrir control]`** y menú de 3 puntos (*Renombrar*, *Editar conexión*, *Cambiar ubicación*, *Ver detalles*, *Eliminar*).
- **Asistente Abreviado de Adición (`AddDeviceScreen`):**
  - Flujo de 2 pasos sencillos (*"¿Qué deseas controlar?"* -> *"¿Cómo quieres conectarlo?"*), priorizando la búsqueda automática y abriendo el control al finalizar.

---

## 2. Resultados de Compilación y Verificación

| Tarea | Resultado |
|---|---|
| `:app:assembleDebug` | **BUILD SUCCESSFUL** — `app-debug.apk` (v1.0.0-rc1, v8) |
| `:app:assembleRelease` | **BUILD SUCCESSFUL** — `app-release.apk` (v1.0.0-rc1, v8) |
| `:app:bundleRelease` | **BUILD SUCCESSFUL** — `app-release.aab` (v1.0.0-rc1, v8) |
| `testDebugUnitTest` | **87 pruebas unitarias**, 0 fallos, 0 errores |
| `:app:lintDebug` | **0 errores**, 14 advertencias (actualización de dependencias) |
