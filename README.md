# WilTechFinance

Hola bienvenido al repositorio de **WilTechFinance**, una aplicación nativa para Android desarrollada en Java. Este proyecto nació como una solución 
real para el control de finanzas personales, flujos de efectivo y analítica visual de ingresos y gastos, sin depender de servicios externos ni procesos 
lentos.La idea principal fue crear una herramienta limpia, segura y, sobre todo, altamente visual para que cualquier persona pueda entender en segundos 
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

### Módulo 2: Nube y Autenticación

3. Creación de Cuenta e Integración en Tiempo Real (App vs Firebase Firestore)
<img width="1365" height="767" alt="CrearCuenta" src="https://github.com/user-attachments/assets/97ca5983-30a6-4b2f-a87b-67832e089591" />


4. Ingreso de Credenciales y Validación por PIN
<img width="270" height="600" alt="Login" src="https://github.com/user-attachments/assets/39744227-b40c-4cb1-bf65-7763b814969d" />

<br/>
<img width="278" height="591" alt="pin" src="https://github.com/user-attachments/assets/816ffeab-fef3-4bf2-a9c4-9537c4541329" />


5. Dashboard de Usuario Autenticado
<img width="280" height="592" alt="Dashboard" src="https://github.com/user-attachments/assets/382986fa-d130-46f8-8804-a78260bdf75c" />



## Características Clave del Proyecto

* **Autenticación Controlada:** Acceso protegido mediante credenciales de usuario y validación por PIN dinámico.
* **Formulario Inteligente en Vivo:** Registro de movimientos (Ingresos y Gastos) con un formateador automático que añade los puntos de miles a medida que el usuario digita, adaptado al formato de moneda colombiano.
* **Historial Cronológico Limpio:** Sección de movimientos que agrupa automáticamente las transacciones bajo etiquetas dinámicas ("HOY", "AYER" o fechas anteriores) para evitar el desorden visual.
* **Plano Cartesiano Propio:** En lugar de usar librerías externas genéricas o pesadas, programé desde cero un componente visual personalizado (`PlanoCartesianoView`) que dibuja las gráficas de líneas y picos usando vectores nativos en tiempo real.
* **Migración a la Nube (Módulo 2):** Transición a una arquitectura híbrida y escalable utilizando el ecosistema de Firebase.
* **Firebase Authentication:** Sistema seguro de registro e inicio de sesión de usuarios.
* **Cloud Firestore en Tiempo Real:** Persistencia de datos en la nube. Sincronización directa entre los registros de la App y la estructura de documentos en la consola de Firebase.

---

## Tecnologías Utilizadas

* **Lenguaje de Programación:** Java nativo para Android.
* **Base de Datos:** SQLite (`SQLiteOpenHelper`) con control de versiones estructurado.
* **Diseño de Interfaz (UI/UX):** Componentes XML nativos, `ConstraintLayout`, `TextWatcher` en tiempo real y gráficos vectoriales mediante sobreescritura de canvas.
* **Backend y Nube:** Firebase Authentication para gestión de usuarios y Cloud Firestore para base de datos NoSQL en tiempo real.

---

## Desarrollador:
* **Wilmar López** - *Creador,Tester y Desarrollador Principal*
* **CESDE - Globant** -
