# Verificación histórica de etapa 1 — 29 de octubre de 2026

## Entorno

- Windows 11 amd64.
- Android Studio 2026.2.1, build `262.9437.185.2621.16467767`.
- JBR/OpenJDK 25.0.3; Gradle 9.6.0; AGP 9.4.1; Kotlin integrado 2.2.10.
- Android SDK 37.0, revisión 2, estable (`PreviewSdkInt=0`); Build Tools 36.0.0.
- Emulador `Medium_Phone_API_37.0`, Android 17/API 37, x86_64, ADB `emulator-5554`.
- El listado real de características del emulador no contiene `android.hardware.consumerir`.

## Resultados

| Comprobación | Resultado |
|---|---|
| Resolución/sincronización de dependencias mediante Gradle | Correcta; repositorios Google Maven y Maven Central |
| `:app:assembleDebug testDebugUnitTest` | `BUILD SUCCESSFUL`, 2 min 29 s |
| Pruebas unitarias | 13 ejecutadas, 0 fallos, 0 errores |
| `:app:connectedDebugAndroidTest :app:lintDebug` | Gradle `BUILD SUCCESSFUL`, 1 min 4 s |
| Pruebas instrumentadas Compose | 3 ejecutadas, 0 fallos, 0 errores, 0 omitidas |
| Android Lint | 0 errores, 23 advertencias |
| Instalación manual posterior del APK con `adb install -r` | `Success` |
| Inicio manual con `adb shell am start -W -n com.universalremote.app/.MainActivity` | `Status: ok`, `LaunchState: COLD`, 1267 ms; proceso activo |

La ejecución instrumentada limpia sus APK al terminar. Se reinstaló el APK Debug para verificar también el inicio manual independiente de las pruebas.

Las 23 advertencias de Lint son: 14 avisos de versiones más recientes, una etiqueta de Activity redundante, un calificador de recursos `v26` innecesario y siete colores sin uso heredados de la plantilla. No se ocultaron mediante baseline. Se mantienen versiones estables resueltas y probadas, sin exigir que sean las últimas publicadas.

La herramienta de shell marcó código 1 en la ejecución combinada de instrumentación/Lint por salida stderr interpretada como `NativeCommandError` por PowerShell (aviso de `sun.misc.Unsafe` de protobuf bajo Java 25). El registro de **Gradle** termina en `BUILD SUCCESSFUL`; los XML de pruebas y el informe Lint confirman los resultados indicados. No es un fallo de compilación ni de una prueba de la aplicación.

## Cobertura ejecutada

- **DeviceMappingTest (2):** conservación de todos los campos, marca futura no enumerada y opcionales nulos en dispositivo IR.
- **GetConnectionMethodsTest (2):** IR solamente con emisor; Wi-Fi seleccionable ante ausencia de emisor, servicio ausente o error de plataforma.
- **AndroidInfraredControllerTest (6):** emisor presente/ausente, servicio ausente, excepción de plataforma, transmisión diferida con hardware y error explícito sin hardware.
- **HomeViewModelTest (3):** estado vacío real sin IR, lista registrada observada y error de persistencia recuperable mediante reintento.
- **NavigationSmokeTest (3):** inicio/lista/vuelta, agregar/continuar/control deshabilitado/vuelta con IR no disponible y selección de tema conservada tras recrear Activity.

## Evidencias locales generadas

- `build-stage1.log`
- `verification-stage1.log`
- `core/database/build/test-results/testDebugUnitTest/`
- `core/infrared/build/test-results/testDebugUnitTest/`
- `domain/build/test-results/testDebugUnitTest/`
- `feature/home/build/test-results/testDebugUnitTest/`
- `app/build/outputs/androidTest-results/connected/debug/`
- `app/build/reports/androidTests/connected/debug/`
- `app/build/reports/lint-results-debug.html`
- `app/build/outputs/apk/debug/app-debug.apk`
- `core/database/schemas/com.universalremote.core.database.DeviceDatabase/1.json`

Los logs y directorios build son artefactos locales ignorados por Git; el esquema Room debe conservarse en control de versiones.

## Límites de la verificación

No había teléfonos físicos conectados por ADB. No se verificó un POCO X3 NFC real ni una emisión IR física. La detección positiva y las fallas de plataforma se comprobaron con el punto de sustitución `IrHardwareProbe`; el arranque sin IR se verificó realmente en el emulador. No se ejecutó en API 26, aunque el APK declara minSdk 26 y Lint no reporta errores de API. Las conexiones, el descubrimiento y la transmisión están pendientes por alcance de etapa 1.
