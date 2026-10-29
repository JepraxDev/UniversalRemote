# Verificación — Etapa 3: Control Infrarrojo Real · 29 de octubre de 2026

Histórico de etapas anteriores:
- Etapa 1: [docs/VERIFICATION-STAGE1.md](docs/VERIFICATION-STAGE1.md)
- Etapa 2: [VERIFICATION.md](VERIFICATION.md) (preservado como base)

## Entorno y alcance de la prueba

- **Sistema operativo**: Windows 11.
- **Entorno de desarrollo**: Android Studio 2026.2.1, Gradle 9.6.0, AGP 9.4.1.
- **SDK**: Android SDK 37 (estable), `compileSdk` 37, `minSdk` 26, `targetSdk` 37.
- **Java**: JBR 25 (Java 25 fijado por Gradle Daemon; bytecode generado JVM 17).
- **Emulador de pruebas**: `Medium_Phone_API_37.0`, Android 17 / API 37, `emulator-5554`, x86_64.
- **Dispositivo objetivo físico**: POCO X3 NFC (con emisor ConsumerIrManager integrado).
- **APK generado**: `app/build/outputs/apk/debug/app-debug.apk`, Versión **0.3.0**, versionCode **3**.

---

## Resumen de compilación y verificación

| Comprobación ejecutada | Comando | Resultado |
|---|---|---|
| Compilación de biblioteca y aplicación Debug | `.\gradlew.bat :app:assembleDebug` | **BUILD SUCCESSFUL** (APK generado) |
| Pruebas unitarias de todos los módulos | `.\gradlew.bat testDebugUnitTest` | **63 pruebas unitarias, 0 fallos, 0 errores** |
| Pruebas instrumentadas Compose en emulador | `.\gradlew.bat :app:connectedDebugAndroidTest` | **7 pruebas instrumentadas, 0 fallos, 0 errores** |
| Análisis estático Android Lint | `.\gradlew.bat :app:lintDebug` | **0 errores, 14 advertencias** (actualización de librerías) |
| Instalación y arranque en frío vía ADB | `adb install` + `am start -W` | **Status: ok, LaunchState: COLD**, 1827 ms |

---

## Detalle de pruebas automatizadas

### Pruebas unitarias (63 pruebas)

1. **AndroidInfraredControllerTest (12 pruebas)** (`:core:infrared`):
   - `detectsEmitter`: Detección positiva de hardware infrarrojo.
   - `noEmitterDoesNotThrow`: Ausencia de emisor controlada sin excepciones.
   - `missingServiceDoesNotThrow`: Servicio ConsumerIrManager nulo controlado.
   - `platformFailureIsExplicit`: Captura de fallos de plataforma (`SecurityException` / `RuntimeException`).
   - `transmissionSucceedsWithValidSignal`: Transmisión exitosa y paso de parámetros a `IrHardware`.
   - `missingEmitterReturnsCompatibilityError`: Rechazo de transmisión cuando no hay emisor.
   - `invalidFrequencyReturnsFailure`: Rechazo de frecuencias fuera de 20 kHz – 60 kHz.
   - `unsupportedFrequencyReturnsFailure`: Frecuencias fuera del rango reportado por el emisor.
   - `unknownFrequencyReturnsFailure`: Comportamiento seguro cuando Android no reporta rangos de frecuencia.
   - `invalidPatternReturnsFailure`: Rechazo de patrones con menos de 3 intervalos.
   - `platformExceptionReturnsFailure`: Manejo seguro de excepciones del sistema.
   - `rapidTransmissionsAreRejectedWithBusy`: Protección contra ráfagas y comandos simultáneos dentro del periodo de enfriamiento (250 ms).

2. **IrSignalValidatorTest (8 pruebas)** (`:domain`):
   - Validación de patrones legítimos.
   - Frecuencia inferior a 20 kHz y superior a 60 kHz.
   - Patrón con menos de 3 entradas o más de 1.024 entradas.
   - Duración de pulso negativa, cero o superior a 100.000 µs.
   - Duración total de trama mayor a 1.000.000 µs (1 s).

3. **VerifiedIrCatalogTest (5 pruebas)** (`:protocol:ir`):
   - `necEncoderGeneratesCompliantSignal`: Codificación precisa de trama NEC (32 bits, 67 intervalos, 38 kHz).
   - `sonyEncoderGeneratesCompliantSignal`: Codificación precisa de trama Sony SIRC 12-bit (40 kHz).
   - `catalogContainsPrioritizedBrandsForTvsAndProjectors`: Perfiles presentes para LG, Samsung, Sony, TCL, Hisense, Xiaomi, Epson, BenQ, ViewSonic, Optoma y NEC.
   - `pendingBrandsListIdentifiesMissingProtocolsWithoutFakingSupport`: Documentación técnica de marcas pendientes (Philips RC-5, Panasonic Kaseikyo, Sony Proyector SIRC 15/20-bit).
   - `profilesHaveDistinctIdsAndSources`: Identificadores únicos y fuentes técnicas válidas.

4. **DefaultInfraredRepositoryTest (8 pruebas)** (`:data`):
   - Ejecución de prueba de comando y persistencia de evaluación `UNVERIFIED`.
   - Confirmación afirmativa del usuario actualiza a `CONFIRMED_BY_USER`.
   - Confirmación negativa actualiza a `INCOMPATIBLE`.
   - Rechazo de envío si el dispositivo no existe en persistencia (`NOT_CONFIGURED`).
   - Rechazo de comandos si el método no es infrarrojo (`PROFILE_INCOMPATIBLE`).
   - Bloqueo y solicitud de confirmación para comandos de encendido/apagado si no están confirmados.
   - Transmisión forzada con `confirmed = true`.
   - Envío de comandos ordinarios sin confirmación intermedia.
   - Rechazo de comandos no contemplados en el perfil (`COMMAND_UNSUPPORTED`).

5. **RemoteViewModelTest (6 pruebas)** (`:feature:remote`):
   - Inicio en modo demostración cuando no se especifica dispositivo.
   - Carga exitosa de dispositivo IR con capacidades y perfil.
   - Envío de comandos conectados cuando el control está configurado.
   - Bloqueo de transmisión en modo demostración.
   - Solicitud de diálogo de confirmación para comando `POWER`.
   - Transmisión efectiva tras confirmación manual.
   - Bloqueo de comandos no soportados por las capacidades del equipo.

6. **IrSetupViewModelTest (6 pruebas)** (`:feature:discovery`):
   - Filtrado inicial de perfiles disponibles para la marca y tipo seleccionados.
   - Detección e información de marcas con códigos pendientes.
   - Transmisión de comando de prueba seguro (`MENU`).
   - Confirmación del usuario ("Sí, funcionó") y cambio de estado a `CONFIRMED_BY_USER`.
   - Registro de incompatibilidad ante respuesta negativa ("No funcionó").
   - Registro y almacenamiento del nuevo equipo y su evaluación en Room.

7. **AddDeviceViewModelTest (5 pruebas)** (`:feature:discovery`):
   - Mantenimiento y restauración de selecciones de asistente.
   - Restricción de selección IR según disponibilidad real de hardware.
   - Mantenimiento de selección IR ante disponibilidad positiva.
   - Reversión a Wi-Fi si el hardware desaparece.
   - Límites estrictos de navegación del asistente (pasos 0 a 2).

8. **DeviceMappingTest (3 pruebas)** (`:core:database`):
   - Mapeo completo de `Device` a `DeviceEntity` y viceversa con marcas extensibles.
   - Dispositivos infrarrojos con campos de red nulos.
   - Mapeo de `IrAssessment` y persistencia de estados de validación y lista de comandos.

9. **FilterDevicesTest (4 pruebas)** (`:domain`):
   - Búsqueda por texto y filtros combinados.
   - Ordenación por favoritos y orden alfabético.

10. **GetConnectionMethodsTest (2 pruebas)** (`:domain`):
    - IR solo seleccionable si el emisor está presente.
    - Wi-Fi disponible incluso ante ausencia de IR.

11. **HomeViewModelTest (4 pruebas)** (`:feature:home`):
    - Observación reactiva de dispositivos guardados y favoritos reales.

---

### Pruebas instrumentadas Compose (7 pruebas)

Ejecutadas en `Medium_Phone_API_37.0` (Android 17 / API 37):
1. `NavigationSmokeTest.bottomNavigationAndRealEmptyState`: Navegación inferior y estados vacíos.
2. `NavigationSmokeTest.wizardRestoresSelectionsAndProjectorDemoDoesNotTransmit`: Flujo del asistente, recreación de Activity, paso a resumen Wi-Fi y apertura de control de proyector en modo demostración con botones deshabilitados.
3. `NavigationSmokeTest.tvRemoteRendersChannelsDpadAndDemoState`: Renderizado de control TV en modo demostración con botones deshabilitados.
4. `NavigationSmokeTest.themeIsSavedInDataStoreAndSurvivesRecreation`: Persistencia de tema claro/oscuro en DataStore tras rotación y recreación.
5. `DesignComponentsTest.connectionSelectorExplainsMissingIrAndAllowsWifi`: Explicación visible ante ausencia de emisor IR.
6. `DesignComponentsTest.availableIrCanBeSelectedWithoutClaimingConnection`: Selección de Infrarrojo con emisor disponible inyectado.
7. `DesignComponentsTest.projectorHidesAdditionalCommandsOutsideCapabilities`: Filtrado dinámico de botones según capacidades reportadas.

---

## Análisis estático Android Lint

- **0 errores**.
- **14 advertencias**: Mismas advertencias de actualización de versión de dependencias identificadas en la etapa 2 (conservadas por estabilidad y delimitación de alcance).

---

## Evidencias generadas

- APK compilado: `app/build/outputs/apk/debug/app-debug.apk`
- Reporte Lint HTML: `app/build/reports/lint-results-debug.html`
- Esquema de base de datos v2 exportado: `core/database/schemas/com.universalremote.core.database.DeviceDatabase/2.json`
