# WilTechFinance

Hola bienvenido al repositorio de **WilTechFinance**, una aplicación nativa para Android desarrollada en Java. Este proyecto nació como una solución 
real para el control de finanzas personales, flujos de efectivo y analítica visual de ingresos y gastos, sin depender de servicios externos ni procesos 
lentos. La idea principal fue crear una herramienta limpia, segura y, sobre todo, altamente visual para que cualquier persona pueda entender en segundos 
en qué se le va el dinero.

---

## Capturas de Pantalla App

### 1. Lista de Transacciones (Historial)
<img width="469" height="712" alt="Captura de pantalla 2026-07-03 134958" src="https://github.com/user-attachments/assets/ed02ce80-1fb8-4d6d-9c36-afacb09d9cd0" />

---

### 2. Formulario de registro (Ingresos y Gastos)
<img width="465" height="710" alt="Captura de pantalla 2026-07-03 134745" src="https://github.com/user-attachments/assets/48db0496-3905-4664-b211-2b7bea8aff21" />

---
<img width="466" height="703" alt="Captura de pantalla 2026-07-03 134826" src="https://github.com/user-attachments/assets/d897242b-2a29-4859-bb58-a37ec8efe869" />

---
<img width="475" height="714" alt="Captura de pantalla 2026-07-03 134838" src="https://github.com/user-attachments/assets/9a77ad7f-7219-459c-a093-81651114c146" />


## Características Clave del Proyecto

* **Autenticación Controlada:** Acceso protegido mediante credenciales de usuario y validación por PIN dinámico.
* **Formulario Inteligente en Vivo:** Registro de movimientos (Ingresos y Gastos) con un formateador automático que añade los puntos de miles a medida que el usuario digita, adaptado al formato de moneda colombiano.
* **Historial Cronológico Limpio:** Sección de movimientos que agrupa automáticamente las transacciones bajo etiquetas dinámicas ("HOY", "AYER" o fechas anteriores) para evitar el desorden visual.
* **Plano Cartesiano Propio:** En lugar de usar librerías externas genéricas o pesadas, programé desde cero un componente visual personalizado (`PlanoCartesianoView`) que dibuja las gráficas de líneas y picos usando vectores nativos en tiempo real.

---
## Escalabilidad Cloud y Geolocalización (Módulo 3)

La aplicación evolucionó para incorporar arquitectura en la nube, control de roles corporativos y ubicación geográfica inteligente:

- **Infraestructura Cloud (Firebase):** Migración completa a `Cloud Firestore` para base de datos en tiempo real y `Firebase Authentication` para la gestión segura de accesos.
- **Gestión Documental Multimedia:** Integración de `Firebase Storage` mediante la moderna API `ActivityResultLauncher`. Permite a los usuarios tomar fotos de sus facturas en tiempo real o elegirlas de la galería para adjuntarlas como respaldo a cada transacción.
- **Control de Acceso basado en Roles (RBAC) - Modo Auditoría:** Creación de un rol de "Administrador" jerárquico. Al iniciar sesión como Admin, se despliega una interfaz especializada que permite seleccionar a otros usuarios para entrar en modo de "Suplantación o Auditoría". En este modo, el administrador puede revisar saldos, eliminar transacciones y ver el comportamiento geográfico del empleado seleccionado.
- **Mapas y Geocodificación Inversa:** Integración del Google Maps SDK y `FusedLocationProviderClient`. Los usuarios pueden marcar puntos de interés en el mapa manteniendo presionada la pantalla; la app utiliza `Geocoder` para traducir de forma automática las coordenadas (Lat/Lng) a direcciones físicas legibles en la vida real.
- **Notificaciones:** Implementación de notificaciones locales al registrar movimientos y configuración del servicio `FirebaseMessagingService` preparado para recibir alertas Push desde la nube (FCM).
- **Diseño Responsivo (Landscape):** Soporte total para orientación horizontal, implementando contenedores en `ScrollView` para asegurar que las interfaces complejas se adapten dinámicamente sin cortarse.

---

## 🔑 Credenciales de Prueba (Testing)
Para probar el sistema de jerarquías (Modo Auditoría), por favor utilizar las siguientes cuentas de prueba:

**Rol Administrador (Acceso total):**
- **Correo:** [TU_CORREO_ADMIN_AQUI]
- **Contraseña:** [TU_CLAVE_ADMIN_AQUI]

**Rol Estándar (Empleado/Usuario normal):**
- **Correo:** [CORREO_USUARIO_NORMAL_AQUI]
- **Contraseña:** [CLAVE_USUARIO_NORMAL_AQUI]

--

## Tecnologías Utilizadas

- **Lenguaje de Programación:** Java nativo para Android.
- **Bases de Datos:** Firebase Cloud Firestore y SQLite (Control de versiones local).
- **Arquitectura Backend:** Firebase (Auth, Storage, Cloud Messaging).
- **Geolocalización:** Google Maps SDK, Fused Location Provider, Geocoder.
- **Diseño de Interfaz (UI/UX):** Componentes XML nativos, `ConstraintLayout`, `ScrollView` y gráficos vectoriales nativos (Canvas).

--


## Desarrollador:
* **Wilmar López** - *Creador y Desarrollador Principal*
* **CESDE - Globant** -
