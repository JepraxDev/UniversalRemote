# Universal Remote — Etapa 3 · Control Infrarrojo Real

Aplicación Android nativa en Kotlin, Jetpack Compose y Material 3 con arquitectura Clean Architecture y MVVM.
Implementa transmisión infrarroja real mediante `ConsumerIrManager` de Android, conectando los botones de los controles visuales con comandos IR compatibles para televisores y proyectores.

APK generado: `app/build/outputs/apk/debug/app-debug.apk` (Versión **0.3.0**, versionCode **3**).

---

## 1. Arquitectura y flujo infrarrojo

El flujo de ejecución respeta Clean Architecture y separación de capas:

```text
RemoteScreen (UI) 
  → RemoteViewModel (State & Event Handling)
    → SendRemoteCommand (Domain UseCase)
      → InfraredRepository / DefaultInfraredRepository (Data Layer)
        → InfraredTransmitter / AndroidInfraredController (Core Infra)
          → IrHardware / ConsumerIrManager.transmit() (Android OS)
```

- **ConsumerIrManager**: Transmisión real de pulsos en microsegundos (`carrierHz`, `patternMicros.toIntArray()`).
- **Validación de señal**: `IrSignalValidator` comprueba frecuencias entre 20 kHz y 60 kHz, límites de duración de pulsos (1 a 100.000 µs), duración total máxima de trama de 1 segundo (1.000.000 µs) y tamaño de patrón de 3 a 1.024 entradas.
- **Protección de concurrencia y ráfagas**: `Mutex` evita accesos simultáneos y un temporizador `nanoTime()` impone una pausa mínima de seguridad (250 ms) entre transmisiones consecutivas para evitar saturación del emisor.
- **Transmisión no bloqueante**: La invocación a la plataforma Android se ejecuta fuera del hilo principal (`Dispatchers.IO`), con comprobación de cancelación mediante `ensureActive()`.

---

## 2. Catálogo de perfiles infrarrojos y compatibilidad de marcas

Los códigos proceden de documentación técnica de fabricantes y capturas analizadas del repositorio comunitario verificado [Flipper-IRDB](https://github.com/Lucaslhm/Flipper-IRDB). No se inventan códigos universales ni se comparten códigos entre marcas distintas.

### Televisores compatibles

| Marca | Familia / Modelo | Protocolo | Frecuencia | Comandos implementados | Fuente técnica |
|---|---|---|---|---|---|
| **LG** *(Prioridad prueba)* | AKB72915206 (Smart TV / WebOS) | NEC | 38.000 Hz | POWER, POWER_ON, POWER_OFF, VOL+/-, CH+/-, MUTE, MENU, D-PAD, OK, BACK, HOME, INPUT, HDMI 1/2 | Flipper-IRDB `TVs/LG/LG_AKB72915206.ir` + Manual de servicio LG |
| **Samsung** | Serie AA59 (Smart TV) | NEC (Samsung 32-bit) | 38.000 Hz | POWER, POWER_ON, POWER_OFF, VOL+/-, CH+/-, MUTE, MENU, D-PAD, OK, BACK, HOME, INPUT, HDMI 1/2 | Flipper-IRDB `TVs/Samsung/Samsung_AA59-00600A.ir` |
| **Sony** | Bravia | SIRC 12-bit | 40.000 Hz | POWER, POWER_ON, POWER_OFF, VOL+/-, CH+/-, MUTE, MENU, D-PAD, OK, BACK, HOME, INPUT | Flipper-IRDB `TVs/Sony/Sony_TV.ir` + Sony SIRC Spec |
| **TCL** | Serie RC802N | NEC | 38.000 Hz | POWER, VOL+/-, MUTE, MENU, D-PAD, OK, BACK, HOME, INPUT | Flipper-IRDB `TVs/TCL/TCL_RC802N.ir` |
| **Hisense** | Serie EN2B27 | NEC | 38.000 Hz | POWER, VOL+/-, CH+/-, MUTE, MENU, D-PAD, OK, BACK, HOME, INPUT | Flipper-IRDB `TVs/Hisense/Hisense_EN2B27.ir` |
| **Xiaomi** | Mi TV IR | NEC | 38.000 Hz | POWER, VOL+/-, MENU, D-PAD, OK, BACK, HOME | Flipper-IRDB `TVs/Xiaomi/Xiaomi_Mi_TV.ir` |

### Proyectores compatibles

| Marca | Familia / Modelo | Protocolo | Frecuencia | Comandos implementados | Fuente técnica |
|---|---|---|---|---|---|
| **Epson** *(Prioridad prueba)* | EMP / Serie EB (ESC/VP21) | NECext | 38.000 Hz | POWER, POWER_ON, POWER_OFF, VOL+/-, MENU, D-PAD, OK, BACK, INPUT, HDMI, VGA, LAN, FREEZE, AV_MUTE | Flipper-IRDB `Projectors/Epson/Epson_EMP822H.ir` + Epson ESC/VP21 |
| **BenQ** | Serie BenQ Projector | NEC | 38.000 Hz | POWER, POWER_ON, POWER_OFF, VOL+/-, MENU, D-PAD, OK, BACK, INPUT, HDMI, VGA, FREEZE, AV_MUTE | Flipper-IRDB `Projectors/Benq/Benq_Projector.ir` |
| **ViewSonic** | Serie PJD | NEC | 38.000 Hz | POWER, VOL+/-, MENU, D-PAD, OK, BACK, INPUT, FREEZE, AV_MUTE | Flipper-IRDB `Projectors/ViewSonic/ViewSonic_PJD5134.ir` |
| **Optoma** | Optoma Universal | NEC | 38.000 Hz | POWER, POWER_OFF, VOL+/-, MENU, D-PAD, OK, INPUT, FREEZE, AV_MUTE | Flipper-IRDB `Projectors/Optoma/Optoma_HD141X.ir` |
| **NEC** | Series NP / VT | NEC | 38.000 Hz | POWER, POWER_ON, POWER_OFF, VOL+/-, MENU, D-PAD, OK, BACK, INPUT, FREEZE, AV_MUTE | Flipper-IRDB `Projectors/NEC/NEC_NP_Series.ir` |

### Marcas y perfiles pendientes (documentación técnica faltante)

Conforme a los lineamientos del proyecto, ninguna marca se presenta como funcional si sus códigos no han podido ser verificados rigurosamente:

- **Philips (TV)**: Pendiente. Utiliza modulación biphase Manchester (RC-5 / RC-6 a 36 kHz). Requiere codificador específico biphase y captura verificada con chasis físico.
- **Panasonic (TV y Proyectores)**: Pendiente. Utiliza protocolo Kaseikyo / Panasonic de 48 bits con paridad OEM. Requiere verificación de la rutina de checksum con equipo físico.
- **Sony (Proyector)**: Pendiente. Emplea tramas SIRC de 15 o 20 bits con código de dispositivo 2. Requiere validación con proyector físico.

---

## 3. Selección y prueba interactiva de perfiles

La aplicación incorpora una pantalla de prueba y validación IR (`IrSetupScreen` / `IrSetupViewModel`):

1. **Flujo guiado**:
   - Tipo de equipo (TV o Proyector).
   - Marca (LG, Samsung, Epson, etc.).
   - Método Infrarrojo.
   - Selección de perfil compatible (muestra familia, protocolo, frecuencia portadora y estado de validación).
2. **Prueba de comando de bajo impacto**:
   - Ejecuta un comando seguro (`MENU` o `VOLUME_UP`), evitando apagar o interrumpir el equipo.
   - Informa el resultado de la transmisión local: *"Señal enviada localmente (38 kHz). Nota: ConsumerIrManager no proporciona confirmación de recepción del equipo."*
3. **Pregunta de confirmación manual**:
   - "¿El dispositivo respondió?"
     - **Sí, funcionó**: Actualiza el estado a `CONFIRMED_BY_USER`, permite asignar nombre/habitación y guarda el equipo en Room.
     - **No funcionó**: Registra el perfil como `INCOMPATIBLE` y permite probar otro perfil o reintentar.
     - **Probar otro perfil**: Permite seleccionar otra variante de la lista de perfiles disponibles.

---

## 4. Control de encendido y seguridad (POWER)

- Se diferencian rigurosamente los comandos `POWER` (alternador/toggle), `POWER_ON` y `POWER_OFF` (discretos).
- Nunca se envían ráfagas automáticas repetidas de encendido/apagado.
- En proyectores o ante comandos de alto impacto (`POWER`, `POWER_OFF`, `AV_MUTE`), la interfaz solicita confirmación mediante un cuadro de diálogo antes de emitir la señal para evitar apagados accidentales o daños en la lámpara durante el ciclo de enfriamiento.

---

## 5. Persistencia y esquema Room v2

- **Room versión 2**: Entidad `DeviceEntity` (equipos guardados) y `IrAssessmentEntity` (evaluación de perfiles, marcas, fecha de última prueba, comandos soportados y estado `UNVERIFIED`, `CONFIRMED_BY_USER`, `INCOMPATIBLE`).
- **Migración incremental**: `DeviceDatabase.MIGRATION_1_2` implementada y exportada en esquemas `1.json` y `2.json`. Sin migraciones destructivas.

---

## 6. Compatibilidad con celulares sin emisor IR

- La característica `android.hardware.consumerir` tiene `android:required="false"` en `AndroidManifest.xml`.
- En celulares o emuladores sin emisor, la aplicación inicia normalmente, muestra el emisor como "No disponible", deshabilita la opción infrarroja con un mensaje explicativo y mantiene habilitada la navegación, la persistencia y la preparación del modo Wi-Fi.
- No se producen excepciones de plataforma ni bloqueos.

---

## 7. Procedimiento de prueba física en POCO X3 NFC

Para realizar la prueba física en el **POCO X3 NFC**:

1. **Instalación**:
   ```powershell
   & "C:\Users\Soporte2\AppData\Local\Android\Sdk\platform-tools\adb.exe" install -r app/build/outputs/apk/debug/app-debug.apk
   & "C:\Users\Soporte2\AppData\Local\Android\Sdk\platform-tools\adb.exe" shell am start -W -n com.universalremote.app/.MainActivity
   ```
2. **Detección de hardware**:
   - Abrir la aplicación y verificar que en la pantalla de Inicio aparece el indicador verde: *"Emisor IR disponible"*.
3. **Configuración de prueba (LG o EPSON)**:
   - Pulsar **Agregar dispositivo**.
   - Elegir **Televisor** (para TV LG) o **Proyector** (para Epson).
   - Elegir la marca (**LG** o **EPSON**).
   - Seleccionar método **Infrarrojo**.
4. **Prueba y confirmación**:
   - En la pantalla de prueba, comprobar que el perfil seleccionado indique 38.000 Hz.
   - Apuntar el emisor superior del celular directamente hacia el receptor del equipo físico a una distancia de 2 a 3 metros.
   - Pulsar **Enviar comando de prueba (MENÚ)**.
   - Observar si se despliega el menú en el televisor o proyector.
   - Si respondió, pulsar **Sí, funcionó**, escribir un nombre (ej. "LG Sala") y pulsar **Guardar control**.
5. **Uso del control remoto**:
   - El control remoto activo mostrará los botones habilitados para ese perfil.
   - Probar volumen y cruceta direccional.
   - Al presionar **Encendido**, confirmar el diálogo de seguridad para transmitir.

---

## 8. Verificación y resultados de compilación

| Tarea | Resultado |
|---|---|
| `:app:assembleDebug` | `BUILD SUCCESSFUL` — APK `app-debug.apk` (0.3.0, v3) |
| `testDebugUnitTest` | **63 pruebas unitarias**, 0 fallos, 0 errores |
| `:app:connectedDebugAndroidTest` | **7 pruebas instrumentadas** en Android 37, 0 fallos |
| `:app:lintDebug` | **0 errores**, 14 advertencias (todas de actualización de dependencias) |
