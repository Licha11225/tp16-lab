# TP16 - Sistema de Gestión de Jugadores y Once Ideal

## Descripción del proyecto

Este proyecto consiste en una aplicación desarrollada en Java que permite gestionar jugadores y clubes de fútbol, crear equipos, armar un once ideal y competir con este para demostrar el nivel de tu equipo

El sistema cuenta con una interfaz gráfica, conexión a una base de datos MySQL y diferentes funcionalidades relacionadas con la administración de jugadores y la simulación de partidos.

## Integrantes

- Lavado Janampa Julio Cesar Junior
- Sadañoski D'Aversa Lisandro

## Tecnologías utilizadas

- **Java:** JDK 21 (Java SE 21).
- **MySQL:** MySQL 8.x.
- **Gestor de base de datos:** XAMPP o USBWebServer.
- **Herramienta de desarrollo:** Maven.
- **IDE:** Visual Studio Code / Eclipse / IntelliJ IDEA.

## Requisitos previos

Para ejecutar el proyecto es necesario contar con:

1. Java JDK 21 instalado.
2. XAMPP o USBWebServer instalado.
3. MySQL en funcionamiento.
4. Maven para gestionar y compilar el proyecto.
5. Un IDE compatible con proyectos Java.

## Instalación y ejecución

### 1. Descargar el proyecto

Clonar el repositorio desde GitHub:

```bash
git clone URL_DEL_REPOSITORIO
```

O descargarlo directamente como archivo ZIP y descomprimirlo.

### 2. Iniciar el servidor MySQL

Se debe utilizar alguno de los siguientes programas:

- **XAMPP:** abrir el panel de control e iniciar el servicio MySQL.
- **USBWebServer:** ejecutar el programa y activar el servicio MySQL.

No es necesario iniciar Apache para ejecutar la aplicación Java. Se puede utilizar phpMyAdmin para administrar la base de datos.

### 3. Importar la base de datos

1. Abrir phpMyAdmin desde el navegador.
2. Crear la base de datos correspondiente.
3. Seleccionar la opción **Importar**.
4. Seleccionar el archivo `tp14.sql`, incluido en el proyecto.
5. Ejecutar la importación para crear las tablas y cargar los datos necesarios.

### 4. Configurar la conexión

Verificar los datos de conexión en el archivo:

`src/main/java/config/ConexionBD.java`

Configurar los siguientes parámetros según la instalación local de MySQL:

- Host: localhost.
- Puerto: 3306 (predeterminado).
- Usuario: root (predeterminado en XAMPP).
- Contraseña: según la configuración local.
  Usbwebserver: usbw
  XAMPP: no tiene contraseña
- Base de datos: nombre de la base de datos creada.

### 5. Compilar y ejecutar

Abrir el proyecto desde un IDE compatible con Maven.

Esperar a que Maven descargue las dependencias necesarias y verificar que el proyecto utilice JDK 21.

Luego, ejecutar la clase principal:

`src/main/java/main/Main.java`

También se puede compilar el proyecto desde una terminal ubicada en la carpeta principal:

```bash
mvn clean package
```

Una vez compilado, ejecutar la aplicación desde el IDE utilizando la clase `main.Main`.

## Estructura del proyecto

```text
tp16/
│
├── src/
│   └── main/
│       └── java/
│           ├── arcade/
│           ├── config/
│           ├── dao/
│           ├── daoimpl/
│           ├── exception/
│           ├── main/
│           ├── model/
│           ├── service/
│           └── ui/
│
├── target/
├── pom.xml
├── tp14.sql
└── README.md
```

## Observaciones

- Es necesario tener MySQL iniciado antes de ejecutar la aplicación.
- La conexión a la base de datos debe coincidir con los parámetros configurados en `ConexionBD.java`.
- Se recomienda utilizar Java JDK 21 para evitar incompatibilidades.
- El archivo `tp14.sql` contiene el script SQL necesario para preparar la base de datos.

---

**Trabajo Práctico - TP16**
:::
