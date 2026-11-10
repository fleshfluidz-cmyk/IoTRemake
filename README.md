# Smart Parking IoT - Unidad 3 (TI3042)

Sistema de Estacionamiento Inteligente que integra simulación de microcontroladores ESP32 en Wokwi, base de datos NoSQL en la nube con **Amazon DynamoDB**, y una aplicación móvil nativa desarrollada en **Android Studio (Kotlin)** bajo estándares de seguridad **ISO 27000**.

---

## 🏛️ Arquitectura del Sistema
```
[ ESP32 + HC-SR04 ] ──(MQTT)──▶ [ Broker HiveMQ ] ──(Bridge)──▶ [ AWS DynamoDB ]
                                                                        │
[ ESP32 + LEDs ]    ◀──(MQTT)───┘                                       │ (AWS SDK)
(Actuador Físico)                                                       ▼
                                                             [ App Android Studio ]
```

1. **Nodo Sensor (Wokwi):** ESP32 con sensor ultrasónico HC-SR04. Determina estado **OCUPADO** ($\le 50\text{ cm}$) o **LIBRE** ($> 50\text{ cm}$) y transmite tramas JSON por MQTT.
2. **Nodo Actuador (Wokwi):** ESP32 con LEDs de señalización física (LED Verde = Disponible, LED Rojo = Ocupado).
3. **Persistencia en la Nube:** AWS DynamoDB (Tabla `Estacionamiento`, Partition Key `id_plaza = "plaza_01"`).
4. **Aplicación Móvil:** Android Studio (Kotlin) con consumo en tiempo real de DynamoDB y caché local offline.

---

## 🔒 Cumplimiento de Estándares de Seguridad (Familia ISO 27000)

* **ISO/IEC 27001 (Cifrado en Tránsito):** Comunicación cifrada con protocolo **TLS 1.2+ / HTTPS** en la API de DynamoDB.
* **ISO/IEC 27002 (Mínimo Privilegio):** Autenticación mediante tokens de sesión temporales gestionados por **AWS STS** con permisos restringidos exclusivamente a la tabla del proyecto.
* **ISO/IEC 27701 (Protección de Datos Personales):** Principio de **minimización de datos**; el sistema no recopila información personal identificable (PII) de conductores, únicamente telemetría física.
* **Integridad:** Codificación en tramas estructuradas JSON con validación estricta de tipos.
* **Continuidad Operativa (Fail-Safe):** La aplicación implementa almacenamiento temporal mediante `SharedPreferences` para operar en modo offline si se interrumpe la conexión a Internet.

---

## 📁 Estructura del Repositorio
* `app/`: Código fuente de la aplicación móvil en Android Studio (Kotlin).
* `app/src/main/java/com/example/smartparking/MainActivity.kt`: Lógica de sincronización con DynamoDB y almacenamiento offline.
* `app/src/main/res/layout/activity_main.xml`: Interfaz gráfica con indicador dinámico de color.

---

## 🚀 Configuración y Ejecución
1. Abrir la carpeta del proyecto en **Android Studio**.
2. En `MainActivity.kt`, ingresar credenciales de AWS (`AWS_ACCESS_KEY`, `AWS_SECRET_KEY`, `AWS_SESSION_TOKEN`).
3. Ejecutar en el emulador de Android Studio (`▶ Run`).
