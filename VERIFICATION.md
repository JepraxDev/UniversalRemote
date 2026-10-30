# Verificación — Etapa 8: Pruebas Finales y Preparación para Distribución · 29 de octubre de 2026

Histórico de etapas anteriores:
- Etapa 1: [docs/VERIFICATION-STAGE1.md](docs/VERIFICATION-STAGE1.md)
- Etapa 2: [VERIFICATION.md](VERIFICATION.md) (Etapa 2)
- Etapas 3 a 7: Conservadas como base para Infrarrojos, Wi-Fi, Compatibilidad Universal, Gestión Avanzada y Optimización

## Entorno y alcance de la prueba

- **Sistema operativo**: Windows 11.
- **Entorno de desarrollo**: Android Studio 2026.2.1, Gradle 9.6.0, AGP 9.4.1.
- **SDK**: Android SDK 37 (estable), `compileSdk` 37, `minSdk` 26, `targetSdk` 37.
- **Java**: JBR 25 (Java 25 fijado por Gradle Daemon; bytecode generado JVM 17).
- **Versión evaluada**: **`1.0.0-rc1`**, versionCode **8**.

---

## Resumen de compilación y verificación final

| Comprobación ejecutada | Comando | Resultado | Artefacto Generado |
|---|---|---|---|
| Compilación de aplicación Debug | `.\gradlew.bat :app:assembleDebug` | **BUILD SUCCESSFUL** | `app/build/outputs/apk/debug/app-debug.apk` |
| Compilación de APK Release Candidato | `.\gradlew.bat :app:assembleRelease` | **BUILD SUCCESSFUL** | `app/build/outputs/apk/release/app-release.apk` |
| Generación de Android App Bundle (AAB) | `.\gradlew.bat :app:bundleRelease` | **BUILD SUCCESSFUL** | `app/build/outputs/bundle/release/app-release.aab` |
| Pruebas unitarias de todos los módulos | `.\gradlew.bat testDebugUnitTest` | **82 pruebas unitarias, 0 fallos** | Reporte de pruebas unitarias |
| Análisis estático Android Lint | `.\gradlew.bat :app:lintDebug` | **0 errores, 14 advertencias** | Reporte Lint HTML |

---

## Detalle de pruebas automatizadas por módulo

### Pruebas unitarias (82 pruebas en total)

1. **Capa Data y Preferencias Hápticas (10 pruebas)** (`:data`):
   - `HapticPreferencesTest`, `BackupRepository`, `RoomDeviceRepository`, `SecureStorageManager`, `DefaultInfraredRepository`, `ProtocolRegistryImpl`, `DeviceCapabilityResolver`.

2. **Capa Domain, Servicio de Compatibilidad y Runner de Acciones Rápidas (21 pruebas)** (`:domain`):
   - `QuickActionRunnerTest`, `DeviceCompatibilityServiceTest`, filtrado y reglas de negocio.

3. **Protocolos de red Wi-Fi (9 pruebas)**:
   - `LgWebOsProtocolTest`, `PjLinkProtocolTest`, `SamsungWsProtocolTest`.

4. **Capa Feature Remote y Controles Adaptativos (6 pruebas)** (`:feature:remote`):
   - Controles adaptativos, personalización, acciones rápidas y alternancia Wi-Fi/IR.

5. **Motor Infrarrojo y Frecuencias (12 pruebas)** (`:core:infrared`):
   - Verificación de emisor `ConsumerIrManager` y control de frecuencia (20 kHz – 60 kHz).

6. **Catálogo de Perfiles Infrarrojos (5 pruebas)** (`:protocol:ir`):
   - Codificadores NEC y Sony SIRC 12-bit verificados.

7. **Asistente de Dispositivos y Búsqueda (11 pruebas)** (`:feature:discovery`):
   - Búsqueda mDNS/SSDP e ingreso manual por IP.

8. **Mapeo de Base de Datos v5 y Filtros (8 pruebas)** (`:domain`, `:core:database`, `:feature:home`):
   - Mapeo de entidad Room v5 con `locationId`, `lastConnectedAt`, `lastUsedAt`, `RemoteLayoutPreference` y `QuickAction`.

---

## Análisis estático Android Lint

- **0 errores**.
- **14 advertencias**: Mismas advertencias de actualización de dependencias conservadas por estabilidad.

---

## Evidencias generadas

- **APK Debug:** `app/build/outputs/apk/debug/app-debug.apk`
- **APK Release Candidate:** `app/build/outputs/apk/release/app-release.apk`
- **AAB Release:** `app/build/outputs/bundle/release/app-release.aab`
- **Documento de Compatibilidad:** `COMPATIBILITY.md`
- **Notas de Lanzamiento:** `RELEASE.md`
- **Esquema Room v5:** `core/database/schemas/com.universalremote.core.database.DeviceDatabase/5.json`
