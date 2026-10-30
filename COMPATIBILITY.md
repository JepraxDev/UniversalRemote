# Matriz de Compatibilidad — Universal Remote (v1.0.0-rc1)

Documento oficial de compatibilidad para televisores, proyectores, protocolos de red e infrarrojos en Universal Remote.

---

## Nivel de Verificación de Compatibilidad

| Nivel | Descripción |
|---|---|
| `VERIFIED_PHYSICAL` | Compatibilidad probada y confirmada en equipos físicos reales. |
| `VERIFIED_PROTOCOL` | Protocolo de red o perfil IR verificado por especificación técnica oficial. |
| `EXPERIMENTAL` | Perfil o protocolo implementado en fase de validación. |
| `NOT_IMPLEMENTED` | Registrado en catálogo para implementación futura. |
| `UNSUPPORTED` | Modelo o variante identificado como no compatible. |

---

## 1. Televisores Compatibles

| Marca | Familia / Serie | Método Recomendado | Protocolo Wi-Fi | Perfil Infrarrojo | Estado |
|---|---|---|---|---|---|
| **LG** | Smart TV webOS 3.0+ / OLED / NanoCell / QNED | Wi-Fi / Infrarrojo | LG webOS WebSocket (`ws://:3000`) | AKB72915206 (NEC 38 kHz) | `VERIFIED_PHYSICAL` |
| **Samsung** | Smart TV 2016+ (Serie K/M/N/R/T/AU/QLED/Neo) | Wi-Fi / Infrarrojo | Samsung WS (`ws://:8001`) | Serie AA59 (Samsung 32-bit 38 kHz) | `VERIFIED_PHYSICAL` |
| **Sony** | Bravia / Android TV / Google TV | Infrarrojo / Wi-Fi | Android TV mDNS (`_androidtvremote2._tcp`) | Bravia SIRC 12-bit (40 kHz) | `VERIFIED_PHYSICAL` |
| **TCL** | Android TV / Google TV / Roku TV | Infrarrojo / Wi-Fi | Android TV mDNS (`_androidtvremote2._tcp`) | Serie RC802N (NEC 38 kHz) | `VERIFIED_PHYSICAL` |
| **Hisense** | VIDAA / Android TV | Infrarrojo | - | Serie EN2B27 (NEC 38 kHz) | `VERIFIED_PHYSICAL` |
| **Xiaomi** | Mi TV / TV Stick / TV Box | Infrarrojo / Wi-Fi | Android TV mDNS (`_androidtvremote2._tcp`) | Mi TV IR (NEC 38 kHz) | `VERIFIED_PHYSICAL` |
| **Philips** | Smart TV / Ambilight | Infrarrojo | - | RC-5 / RC-6 Biphase (36 kHz) | `EXPERIMENTAL` |
| **Panasonic** | Viera TV | Infrarrojo | - | Viera Kaseikyo 48-bit (37 kHz) | `EXPERIMENTAL` |

---

## 2. Proyectores Compatibles

| Marca | Familia / Serie | Método Recomendado | Protocolo Wi-Fi | Perfil Infrarrojo | Estado |
|---|---|---|---|---|---|
| **Epson** | EMP / Serie EB / PowerLite / Home Cinema | Wi-Fi / Infrarrojo | PJLink TCP (`:4352`) | ESC/VP21 (NECext 38 kHz) | `VERIFIED_PHYSICAL` |
| **BenQ** | Serie MS / MX / TH / TK / W | Wi-Fi / Infrarrojo | PJLink TCP (`:4352`) | BenQ Projector (NEC 38 kHz) | `VERIFIED_PHYSICAL` |
| **ViewSonic** | Serie PJD / PG / PX | Wi-Fi / Infrarrojo | PJLink TCP (`:4352`) | Serie PJD (NEC 38 kHz) | `VERIFIED_PHYSICAL` |
| **Optoma** | Serie HD / EH / UHD | Wi-Fi / Infrarrojo | PJLink TCP (`:4352`) | Optoma Universal (NEC 38 kHz) | `VERIFIED_PHYSICAL` |
| **NEC** | Serie NP / VT / ME / UM | Wi-Fi / Infrarrojo | PJLink TCP (`:4352`) | NP Series (NEC 38 kHz) | `VERIFIED_PHYSICAL` |
| **Sony** | Proyectores VPL / Cineza | Wi-Fi / Infrarrojo | PJLink TCP (`:4352`) | SIRC 15-bit (40 kHz) | `EXPERIMENTAL` |
| **Panasonic** | Proyectores PT Series | Wi-Fi / Infrarrojo | PJLink TCP (`:4352`) | Kaseikyo IR (37 kHz) | `EXPERIMENTAL` |

---

## 3. Detalle de Protocolos de Red y Limitaciones

### LG webOS WebSocket
- **Puerto:** `3000` (`ws://`) o `3001` (`wss://`).
- **Autenticación:** Requiere confirmar el mensaje de aviso en la pantalla del televisor en la primera conexión. El token `client-key` se guarda de forma segura en `SecureStorageManager`.
- **Comandos soportados:** Encendido/Apagado (`ssap://system/turnOff`), Volumen (`ssap://audio/volumeUp`/`volumeDown`), Mute (`ssap://audio/setMuted`), Canales (`ssap://tv/channelUp`/`channelDown`), Cruceta D-Pad, OK, Back, Home, Input.

### PJLink TCP (Proyectores)
- **Puerto:** `4352` TCP.
- **Soporte:** Class 1 y Class 2.
- **Autenticación:** Compatible con contraseña opcional mediante algoritmo Digest MD5.
- **Comandos soportados:** Encendido (`POWR 1`), Apagado (`POWR 0`), Selección de entrada (`INPT 11` RGB/VGA, `INPT 31` HDMI 1, `INPT 32` HDMI 2), Silenciado AV (`MUTE 31`/`30`).

### Samsung Smart TV WebSocket
- **Puerto:** `8001` (`ws://`).
- **Autenticación:** Solicitud de autorización en pantalla con guardado de token.
- **Comandos soportados:** Tramas de teclado `RemoteKey` (`KEY_POWER`, `KEY_VOLUP`, `KEY_VOLDOWN`, `KEY_MUTE`, `KEY_CHUP`, `KEY_CHDOWN`, `KEY_UP`, `KEY_DOWN`, `KEY_LEFT`, `KEY_RIGHT`, `KEY_ENTER`, `KEY_RETURN`, `KEY_HOME`, `KEY_SOURCE`, `KEY_MENU`).

### Android TV / Google TV
- **Descubrimiento:** mDNS (`_androidtvremote2._tcp` y `_googlecast._tcp`).
- **Estado:** Se detectan e identifican equipos Android TV / Google TV. Para la transmisión de comandos por red se requiere el emparejamiento con certificado TLS activo de usuario.

---

## 4. Requisitos de Hardware Infrarrojo

- Requiere un celular Android con sensor emisor infrarrojo físico (`ConsumerIrManager`).
- En celulares o emuladores sin emisor IR, la aplicación deshabilita automáticamente la opción infrarroja y mantiene operativo el control por red local Wi-Fi.
