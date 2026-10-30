# Epson: implementación y verificación

## Estado
Trabajo pendiente de aceptación física. No se declara compatibilidad universal.
El modelo prioritario es **PowerLite 118 (V11HA03020)**. Su ruta documentada
es LAN RJ45 → PJLink TCP 4352 o Epson Web Control en la IP del proyector.
El teléfono puede usar el punto de acceso de esa misma red; el 118 no necesita
un adaptador inalámbrico cuando está conectado por RJ45.

## Fallos encontrados y correcciones
- La conexión IP guardaba dispositivos sin validarlos y conservaba el protocolo LG.
  Ahora se selecciona protocolo/modelo y se autentica antes de insertar.
- PJLink aceptaba errores como éxito. Se valida saludo, desafío MD5, delimitación
  CR, comando de respuesta y ERR1–ERR4/ERRA, con conexiones y tiempos limitados.
- Descubrimiento limitado a mDNS/SSDP; el nombre Epson se trataba como prueba de
  PJLink. Se retira esa inferencia y se añade comprobación de protocolo en subred.
- El perfil IR genérico confundía LAN con Aspecto y contenía códigos no respaldados
  por su referencia. Se limita a EMP822H y a los códigos de la captura citada.
- Guardado IR accesible sin confirmación: ahora exige transmisión y respuesta física.
- Las credenciales podían almacenarse en texto plano si fallaba el cifrado.
  Ahora se exige Android Keystore/AES-GCM; credenciales antiguas sin cifrar no se leen.

## Arquitectura y archivos principales
- `core/network/src/main/assets/epson/devices.json`: catálogo extensible por modelo,
  protocolos, capacidades, conexiones, fuentes, autenticación, referencias y estado.
- `core/network/.../PjLinkClient.kt`: transporte compartido de protocolo.
- `core/network/.../EpsonDiscoveryService.kt`: interfaz Wi-Fi activa, máscara real,
  búsqueda con autorización (máximo /22, 16 conexiones, 90 segundos), diagnóstico.
- `core/network/.../EpsonDeviceRegistry.kt`, `EpsonWebControlLauncher.kt`.
- `domain/.../EpsonProtocolResolver.kt`: prueba no destructiva y selección documentada.
- `protocol/pjlink/.../PjLinkProtocol.kt`: autenticación, capacidades, consultas y control.
- `feature/discovery`: búsqueda, selección, IP, contraseña y confirmación IR.
- `feature/remote`: reconexión, información, entradas reales y acceso Web Control.
- `data/.../Repositories.kt`, `SecureStorageManager.kt`: inyección/persistencia segura.
- `app/.../MainActivity.kt`: transmite marca/tipo al flujo de conexión.

## Control implementado
PJLink: POWR (consulta/encendido/apagado), CLSS, INF1, INF2, NAME, INST,
INPT, AVMT, ERST, LAMP. Clase 2: INNM y FREZ solo después de consultar soporte.
Entradas: se muestran los identificadores anunciados y, en clase 2, sus nombres;
no se inventa una correspondencia universal HDMI/VGA. Errores de protocolo nunca
se presentan como órdenes confirmadas. La confirmación de protocolo no acredita
una respuesta física observada.

Menú, navegación y ajustes adicionales: interfaz oficial Web Control abierta en
el navegador; no se han inventado endpoints HTTP ni comandos PJLink para esas teclas.

## Matriz de compatibilidad
| Modelo | IR | PJLink | ESC/VP21 | Web Control | Wi-Fi | RJ45 | Estado físico |
|---|---|---|---|---|---|---|---|
| PowerLite 118 | Perfil `EPSON_POWERLITE_118_ORIGINAL_IR` (219863500); códigos documentados/comunitarios, pendiente físico | Documentado e implementado | Transporte por modelo pendiente | Documentado; acceso navegador | Opcional | Sí | Pendiente |
| EB-L260F | Perfil `EPSON_EB_L260F` con 15 capturas raw propias a 38 kHz; Menu/Esc/Aspect/Color Mode/Computer/USB/LAN/Info/Ajustes pendientes de captura | Documentado e implementado (TCP 4352, Clase 2) | `KEY xx` documentado; transporte no implementado | Documentado; acceso navegador | Opcional | Sí | Pendiente |
| EMP822H | Captura comunitaria documentada; implementado | Pendiente | Pendiente | Pendiente | Pendiente | Pendiente | Pendiente |

EB, EX, Home Cinema, Pro Cinema, BrightLink, Pro L y EpiqVision no se marcan
compatibles por pertenecer a una familia. Ampliación pendiente de documentos por modelo
y de la primera comprobación real con el 118.

## Fuentes
- Soporte exacto del 118: https://epson.com.co/Soporte/Proyectores/Epson-PowerLite/Epson-PowerLite-118/s/SPT_V11HA03020
- Manual: https://files.support.epson.com/docid/cpd5/cpd59681/index.html
- LAN/PJLink: https://files.support.epson.com/docid/cpd5/cpd59681/source/network/tasks/selecting_wired_network_settings.html
- Web Control: https://files.support.epson.com/docid/cpd5/cpd59681/source/network/tasks/controlling_web_browser.html
- Wi-Fi opcional: https://files.support.epson.com/docid/cpd5/cpd59681/source/network/concepts/wireless_network_projection_intro.html
- Especificación JBMIA: https://pjlink.jbmia.or.jp/english/data_cl2/PJLink_5-1.pdf
- Captura IR no oficial, exclusivamente EMP822H: https://github.com/Lucaslhm/Flipper-IRDB/blob/main/Projectors/Epson/Epson_EMP822H.ir
- Captura IR del EB-L260F: https://github.com/Lucaslhm/Flipper-IRDB/blob/main/Projectors/Epson/Epson_EB-L260F.ir
- Manual EB-L210W/EB-L260F/EB-L265F/EB-L270F: https://files.support.epson.com/docid/cpd6/cpd63079.pdf
- Especificaciones EB-L260F (tabla ESC/VP21 `KEY xx`, PJLink): https://download3.ebz.epson.net/dsc/f/03/00/14/39/51/b4da59a9bae8a67e69246ac6afb5a5f2c59a6c4d/Specifications_EB-L260F_EN_Rev1.0.pdf
- Conjunto NEC2 Epson dispositivo 131.85: https://github.com/probonopd/irdb/blob/master/codes/Epson/Projector/131,85.csv
- Capturas comunitarias Epson 83 55: https://github.com/Lucaslhm/Flipper-IRDB/tree/main/Projectors/Epson
- Manual del 118 (funciones del control): https://files.support.epson.com/docid/cpd5/cpd59681/index.html

## Perfil IR del control 219863500
`EPSON_POWERLITE_118_ORIGINAL_IR` usa NECext/NEC2, dirección `83 55`
(device 131.85), portadora 38 kHz y una trama por pulsación. Los códigos
proceden de la base comunitaria irdb y de capturas Flipper-IRDB de la familia
Epson; el manual del 118 acredita la función de cada tecla, no sus tramas.
Por eso ningún código se marca como verificado físicamente.

| Botón | Código | Estado |
|---|---|---|
| Power (alternancia) | `90` | DB/captura comunitaria; alterna, un solo envío |
| Standby discreto | `91` | Comunitario; pendiente físico |
| Source Search | `8C` | DB/captura comunitaria |
| HDMI / HDMI 2 | `73` / `77` | DB/captura comunitaria / pendiente físico |
| Computer / USB / LAN | `94` / `76` / `74` | DB/captura comunitaria |
| Menú / Esc / Enter | `9A` / `84` / `85` | DB/captura comunitaria |
| Arriba / Derecha / Abajo / Izquierda | `B0` / `B1` / `B2` / `B3` | DB/captura comunitaria |
| User / Auto | `9F` / `9E` | DB/captura comunitaria |
| ID / Default | `31` / `39` | Comunitario; pendiente físico |
| Color Mode / Aspect | `8F` / `8A` | DB/captura comunitaria |
| E-Zoom + / − | `88` / `89` | DB/captura comunitaria (zoom digital, no lente) |
| Volume + / − | `98` / `99` | DB/captura comunitaria |
| A/V Mute / Freeze | `93` / `92` | DB/captura comunitaria (A/V Mute oculta imagen y sonido) |
| Split / Home | `82` / `95` | Comunitario; pendiente físico |
| 0–9 | `A0`–`A9` | Comunitario; pendiente físico |

`Num` no emite una señal propia: es un modificador. `ID` muestra el ID y,
mantenido junto a un número, lo asigna. `0/All` envía el dígito 0 y permite
operar todos los proyectores. No se añadió un mute de audio sin código.
La app distingue emisor disponible, perfil cargado, comando transmitido,
confirmación del usuario, pendiente, IR no disponible y error. Un `transmit()`
sin excepción no se presenta como éxito del proyector.

## Perfil IR del Epson EB-L260F
`EPSON_EB_L260F` es independiente del PowerLite 118. No usa NECext `83 55` ni
convierte las capturas a NEC: guarda los patrones raw a 38 kHz tal como están en
`Projectors/Epson/Epson_EB-L260F.ir` de Flipper-IRDB, incluidas las repeticiones
capturadas. Cada trama tiene 29 entradas (cabecera + 14 bits) y no coincide con
el protocolo del 118.

| Botón | Estado IR |
|---|---|
| Power (alternancia), Source Search, Freeze, E-Zoom +, E-Zoom − | Captura raw comunitaria |
| A/V Mute (captura `Black_screen`), Split, Home, Enter | Captura raw comunitaria |
| Volume +, Volume −, Arriba, Abajo, Izquierda, Derecha | Captura raw comunitaria |
| Menu, Esc, Aspect, Color Mode, Computer, USB, LAN, Información, Ajustes | Sin captura verificada: deshabilitados |

`Power` es solo alternancia; no hay `POWER_ON`/`POWER_OFF` discretos verificados.
El asistente no puede probar Menu porque no existe captura: prueba **Source Search**
(indicado explícitamente en la pantalla) y nunca usa Power como primera prueba.
Los botones sin código se muestran deshabilitados con la etiqueta «sin código»;
la app no deriva señales por analogía con otros modelos Epson.

Red documentada del EB-L260F: PJLink Clase 2 en TCP/UDP 4352, Epson Web Control y
ESC/VP21 (`KEY 3C` = Menu, etc.). ESC/VP21 se documenta pero **no se implementa**
hasta disponer de transporte y prueba real. El catálogo de red incluye las fuentes
HDMI 1, HDMI 2, Computer, USB, LAN y Screen Mirroring; las entradas reales se
consultan al proyector y no se inventan correspondencias.

## Prueba física EB-L260F
1. `Agregar dispositivo → Proyector → Epson → EB-L260F → Infrarrojo`.
2. Comprobar el emisor IR, apuntar al receptor del proyector y pulsar
   **Probar Source Search** (Menu sigue pendiente de captura).
3. Confirmar si el proyector respondió; solo entonces se guarda el dispositivo.
4. Abrir el control y probar, uno a uno, Power, Freeze, D-Pad, Enter, Volume,
   E-Zoom, A/V Mute, Split y Home. Anotar los que respondan.
5. Para red: elegir EB-L260F, introducir IP y contraseña PJLink si existe,
   detectar protocolo, consultar estado y entradas, y abrir Web Control.
6. Registrar IP, firmware, celular, red, autenticación, comandos y resultado.

## Prueba física PowerLite 118
1. Encender y anotar IP/máscara en Red → Info. red-LAN con cables.
2. Conectar el celular al Wi-Fi institucional con acceso a esa LAN.
3. Abrir `http://IP/` y verificar Epson Web Control; autenticar en el navegador.
4. Revisar contraseña PJLink; para control en espera habilitar el ajuste documentado
   de comunicación en espera. Consultar al administrador si hay VLAN/aislamiento.
5. Universal Remote → Agregar → Proyector → Epson → Buscar; autorizar búsqueda local
   o elegir Conectar mediante IP → PowerLite 118 → Automático/PJLink → puerto 4352.
6. Autenticar, conectar, consultar fabricante/modelo/estado/entradas/errores.
7. Abrir Web Control, probar Menú o Source Search y registrar respuesta observada.
8. Probar selección de entrada y A/V Mute; restaurar estado anterior.
9. IR del 118: `Agregar → Proyector → Epson → IR`, elegir PowerLite 118 y el
   asistente. Comprobar emisor, apuntar al proyector y pulsar Menu; no se prueba
   Power. Guardar solo tras responder «Sí, funcionó».
10. Con el control abierto, probar en este orden: Menu, Esc, Arriba, Abajo, Enter,
    Source Search, HDMI, A/V Mute, Freeze, Volume +, Volume − y, al final, Power.
    Anotar cuáles respondieron. Los botones con «sin confirmar» no se consideran
    verificados hasta observarlos físicamente.
11. Cerrar y abrir control para comprobar reconexión sin reintroducir IP.

Registro real: IP, firmware, celular, red, protocolo, autenticación, comandos,
resultado observado, fecha. Actualmente **sin resultados físicos**: ADB no detectó
ningún dispositivo y no se recibió la IP del proyector.

## Limitaciones pendientes
- Sin transporte ESC/VP21 de red habilitado hasta verificar documentación específica.
- Códigos IR del 118 implementados con trazabilidad, pero no confirmados físicamente;
  no se simula recepción ni compatibilidad.
- Sin descubrimiento UDP PJLink clase 2 ni búsqueda propietaria Epson.
- Diagnóstico básico de red/protocolo; no determina por sí solo reglas VLAN/firewall.
- Reconexión usa IP guardada; redescubrimiento por identidad estable y sustitución
  confirmada de IP todavía pendientes. El nombre o modelo no identifican un equipo único.
- No se han verificado físicamente televisores ni proyectores durante esta intervención.
- Gradle ejecuta configuración/sincronización de dependencias; la acción visual
  Android Studio «Sync Project» no se ejecuta desde esta sesión.

## Verificación automatizada
- Compilación inicial: `assembleDebug`, correcta (`initial-epson-build.log`).
- Verificación integrada: `assembleDebug testDebugUnitTest lintDebug
  --no-daemon --max-workers=2`, correcta (`epson-final-verification.log`).
- Cobertura añadida: fragmentación TCP, desafío MD5, contraseña ausente/incorrecta,
  respuestas de error, puerto no PJLink, timeout, truncamiento, máscaras de subred,
  límites de búsqueda, códigos IR, perfil PowerLite 118, mapeo de botones, pulsos,
  estados del control, bloqueo del guardado sin confirmación y asistente Epson.
- Se mantienen las pruebas existentes de los otros módulos, incluidos televisores.
- `git diff --check`: sin errores de espacio (solo avisos LF/CRLF de Git).
- Prueba adicional de sesión PJLink: consultas, selección de entradas, rechazo de
  entrada no anunciada y reconexión sin enviar Power de prueba: correcta.
  Total de resultados unitarios: 112 pruebas, 0 fallos, 0 errores.
  Lint: 0 errores. Incluye EB-L260F: perfil propio, 15 códigos raw, ausencia de
  Menu/Esc/Aspect/Color Mode/Computer/USB/LAN/Info/Ajustes, rechazo de comandos
  sin captura y conservación del perfil PowerLite 118.
- `connectedDebugAndroidTest`: intentado, bloqueado por `No connected devices!`
  (`epson-device-verification.log`); no hay resultados instrumentados físicos.
- APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Los servidores simulados están exclusivamente en `src/test`; sus resultados no
  representan una prueba física de un Epson.
