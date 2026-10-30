# Notas de Lanzamiento y Distribución — Universal Remote v1.0.0-rc1

Documento de distribución, compilación y pruebas para **Universal Remote (Release Candidate 1)**.

---

## Información de la Versión

- **Nombre de Versión:** `1.0.0-rc1`
- **Código de Versión (`versionCode`):** `8`
- **Identificador de Aplicación (`applicationId`):** `com.universalremote.app`
- **SDK Objetivo (`targetSdk`):** `37` (Android 17)
- **SDK Mínimo (`minSdk`):** `26` (Android 8.0 Oreo)
- **Estado de Distribución:** Release Candidate 1 (Candidata a lanzamiento para pruebas físicas y evaluación final)

---

## Instrucciones de Compilación de Artefactos

Ejecuta los siguientes comandos desde la raíz del proyecto para generar los paquetes de prueba y distribución:

### 1. Generar APK Debug (para pruebas locales inmediatas)
```powershell
.\gradlew.bat :app:assembleDebug
```
- **Ubicación del paquete generado:** `app/build/outputs/apk/debug/app-debug.apk`

### 2. Generar APK Release Candidato (firmado para evaluación)
```powershell
.\gradlew.bat :app:assembleRelease
```
- **Ubicación del paquete generado:** `app/build/outputs/apk/release/app-release.apk`

### 3. Generar Android App Bundle (AAB) para Google Play
```powershell
.\gradlew.bat :app:bundleRelease
```
- **Ubicación del paquete generado:** `app/build/outputs/bundle/release/app-release.aab`

---

## Firma y Seguridad del Paquete

- **Firma de Evaluación:** La versión `1.0.0-rc1` está configurada con la firma predeterminada de depuración en el bloque `buildTypes.release` para permitir la instalación directa en teléfonos de prueba sin requerir claves privadas sensibles guardadas en el repositorio.
- **Producción Definitiva:** Para publicar en tiendas oficiales (Google Play Store), reemplace la firma en `app/build.gradle.kts` con el keystore de producción oficial del desarrollador.

---

## Permisos Declarados en Manifest

- `android.permission.TRANSMIT_IR`: Requerido para la emisión de impulsos infrarrojos por hardware.
- `android.permission.INTERNET`: Requerido para abrir conexiones TCP/WebSocket en la red local.
- `android.permission.ACCESS_NETWORK_STATE`: Permite verificar el estado de conexión a la red local.
- `android.permission.ACCESS_WIFI_STATE`: Permite comprobar el estado del adaptador Wi-Fi.
- `android.permission.CHANGE_WIFI_MULTICAST_STATE`: Requerido para enviar y recibir paquetes UDP multicast en el descubrimiento SSDP/UPnP.

---

## Procedimiento de Prueba Física en Celular (POCO X3 NFC)

1. **Instalación de la versión de evaluación vía ADB:**
   ```powershell
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   adb shell am start -W -n com.universalremote.app/.MainActivity
   ```

2. **Prueba Infrarroja (TV LG / Proyector Epson):**
   - Abrir la aplicación y verificar el indicador verde en la pantalla de inicio: *"Emisor IR disponible"*.
   - Seleccionar **Agregar Dispositivo**, elegir la marca (LG o Epson) y el método **Infrarrojo**.
   - Probar el comando de verificación (`MENÚ` o `VOLUMEN`).
   - Apuntar el celular hacia el receptor del equipo a una distancia de 2 a 3 metros.
   - Confirmar funcionamiento afirmativo y guardar.

3. **Prueba Wi-Fi en Red Local:**
   - Conectar el celular a la misma red Wi-Fi que la Smart TV o proyector.
   - Seleccionar **Buscar dispositivos Wi-Fi** o **Agregar manualmente por IP**.
   - Probar comandos de navegación, volumen y cambio de entradas.
