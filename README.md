# Sistema de Monitoreo de Estacionamiento IoT

Proyecto desarrollado para la asignatura Aplicaciones Moviles para IoT (TI3042), correspondiente a la evaluacion sumativa de la Unidad 3: Integracion de Smart Devices con Aplicaciones Externas.

## Descripcion general

El proyecto implementa una solucion de monitoreo de estacionamiento en tiempo real dividida en tres capas: adquisicion mediante microcontroladores, almacenamiento en la nube y visualizacion en una aplicacion movil Android.

El sistema detecta la presencia de un vehiculo utilizando un sensor ultrasonico con un umbral de 50 cm. Si la distancia es menor o igual a 50 cm, el espacio se clasifica como ocupado; de lo contrario, se clasifica como libre. El estado se refleja tanto en actuadores fisicos locales (LEDs de senalizacion) como en una aplicacion movil conectada a una base de datos NoSQL.

## Componentes y tecnologias utilizadas

- Microcontroladores: ESP32 DevKit C v4 simulado en Wokwi.
- Sensor: Sensor de distancia ultrasonico HC-SR04.
- Actuadores: Diodos LED (rojo para ocupado, verde para disponible) con resistencias de 220 ohms.
- Protocolo de transporte: MQTT (Broker HiveMQ en puerto 1883).
- Base de datos en la nube: Amazon DynamoDB (AWS Learner Lab, region us-east-1).
- Aplicacion movil: Android nativo desarrollado en Kotlin (minSdk 24, targetSdk 34).
- Librerias principales:
  - PubSubClient (Arduino/ESP32 para mensajeria MQTT).
  - AWS SDK for Android (com.amazonaws:aws-android-sdk-ddb para consultas a DynamoDB).
  - Kotlin Coroutines (manejo asincrono en segundo plano).

## Arquitectura y flujo de datos

1. Nodo sensor (ESP32):
   - Realiza mediciones de distancia periodicas.
   - Si detecta un cambio de estado entre libre y ocupado, o cada intervalo de comprobacion, publica una trama en formato JSON al tema MQTT `estacionamiento/plaza_01`.
   - En paralelo, envia una trama simplificada al tema `estacionamiento/plaza_01/estado` para el control de los LEDs.

2. Nodo actuador (ESP32):
   - Suscrito al canal de estado.
   - Conmuta el LED rojo (GPIO 26) o el LED verde (GPIO 27) segun la senal recibida.

3. Persistencia en la nube:
   - Los registros se almacenan en la tabla `Estacionamiento` de DynamoDB con la clave de particion `id_plaza` ("plaza_01").
   - Se guardan los campos de estado, distancia medida en centimetros y estampa de tiempo.

4. Aplicacion movil Android:
   - Consulta el estado de la plaza en DynamoDB mediante peticiones GetItem con lectura consistente.
   - Actualiza de forma reactiva el color del indicador (verde o rojo), el texto del estado y la distancia.
   - Permite forzar la actualizacion manual mediante el boton de la interfaz.

## Cumplimiento de estandares de seguridad (Familia ISO 27000)

- ISO/IEC 27001 (Cifrado en transito): Las conexiones con la API de AWS DynamoDB se realizan de forma obligatoria mediante HTTPS bajo TLS 1.2 o superior, evitando la interceptacion de paquetes en la red.
- ISO/IEC 27002 (Control de acceso y minimo privilegio): La aplicacion movil no utiliza credenciales administrativas permanentes. Opera mediante credenciales temporales firmadas por AWS Security Token Service (STS) con permisos restringidos exclusivamente a la lectura del recurso de la tabla.
- ISO/IEC 27701 (Privacidad y proteccion de datos): Se aplica el principio de minimizacion de datos. El sistema unicamente recopila telemetria de distancias fisicas y estados binarios, sin registrar datos personales identificables (PII) de los usuarios o conductores.
- Integridad de la informacion: El intercambio de datos se realiza mediante esquemas JSON tipados. Cualquier mensaje con estructura invalida o datos truncados es descartado antes de procesarse.
- Continuidad operativa y plan de contingencia:
  - En caso de perdida de conexion a Internet en el dispositivo movil, la aplicacion recurre a un almacenamiento temporal en memoria local (SharedPreferences), mostrando el ultimo estado conocido sin interrumpir la ejecucion de la app.
  - Si se pierde la conectividad externa con la nube, el circuito de actuadores en el microcontrolador mantiene su operacion local mediante control directo.
  - El codigo del microcontrolador incorpora reconexion automatica tanto para la interfaz WiFi como para el cliente MQTT.

## Estructura del proyecto

- `app/src/main/java/com/example/smartparking/MainActivity.kt`: Codigo principal de la aplicacion, manejo de peticiones a DynamoDB y almacenamiento en cache local.
- `app/src/main/res/layout/activity_main.xml`: Diseno de interfaz de usuario.
- `app/src/main/AndroidManifest.xml`: Declaracion de permisos de red y componentes.

## Instrucciones de ejecucion

1. Clonar o descargar el repositorio.
2. Abrir la carpeta raiz del proyecto en Android Studio.
3. Permitir que Gradle sincronice las dependencias del proyecto.
4. En `MainActivity.kt`, verificar que las variables `AWS_ACCESS_KEY`, `AWS_SECRET_KEY` y `AWS_SESSION_TOKEN` contengan credenciales activas de AWS.
5. Iniciar un emulador Android (API 24 o superior) o conectar un dispositivo fisico con depuracion USB activada.
6. Ejecutar la aplicacion mediante el boton Run en Android Studio.
