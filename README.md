# PermiCarga Marietha

Aplicativo cliente para la gestión de permisos de circulación de carga
sobredimensionada de **Transportes y Maquinarias Marietha E.I.R.L.**

Proyecto del curso de Gestión de Datos e Información
Escuela Profesional de Ingeniería de Sistemas
Universidad Católica de Santa María · Arequipa, Perú · 2026

---

## Qué es

Una aplicación de escritorio que consume la base de datos desarrollada a lo
largo del proyecto. Permite registrar, consultar, modificar y dar de baja
autorizaciones de circulación, unidades vehiculares y empresas transportistas,
y emitir quince reportes sobre la información registrada.

El trámite se llevaba antes en papel, volviendo a escribir en cada formulario
datos que no cambian de un viaje al siguiente. Esa repetición produjo el hecho
que originó el proyecto: el mismo tracto figuraba declarado con 9 000 kg en una
autorización y con 10 000 kg en otra. El sistema guarda cada dato una sola vez
y lo reutiliza, de modo que esa contradicción ya no es posible.

## Restricciones de diseño

- **Sin frameworks.** Ni Spring, ni Hibernate, ni ningún mapeador
  objeto-relacional. La interfaz usa Swing y el acceso a datos usa JDBC, ambas
  partes de la biblioteca estándar de Java. La única dependencia externa es el
  controlador JDBC, que es el componente que traduce las llamadas JDBC al
  protocolo de red del servidor.
- **Todo el acceso a datos pasa por procedimientos almacenados.** En el código
  Java no existe una sola sentencia `SELECT`, `INSERT`, `UPDATE` ni `DELETE`.
  La regla de negocio vive en la base de datos, no en el cliente.
- **No se borra, se desactiva.** Las tablas maestras llevan una columna
  `Estado` y la baja es lógica, porque una autorización emitida hace años debe
  poder consultarse ante una fiscalización.

## Requisitos

| Componente | Versión mínima |
|---|---|
| Java | 17 (recomendado 21) |
| MySQL | 8.0.16 o superior |
| MariaDB (alternativa) | 10.5 o superior |

> **Sobre la versión de MySQL.** A partir de 8.0.16 el gestor hace cumplir las
> cláusulas `CHECK`. En versiones anteriores se aceptan en la definición de la
> tabla pero se ignoran en silencio, de modo que el diseño lógico y lo que
> realmente protege la base de datos dejan de coincidir.

## Instalación

### 1. Base de datos

Ejecute los seis scripts de `base_de_datos/` **en este orden**, porque cada uno
necesita lo que creó el anterior:

```
01_creacion_bd.sql       Crea la base de datos y sus nueve tablas
02_datos_prueba.sql      Carga más de 300 filas de datos de prueba
03_vistas.sql            Crea las cuatro vistas
04_procedimientos.sql    Crea los procedimientos de mantenimiento
05_reportes.sql          Crea los quince procedimientos de reporte
06_triggers.sql          Crea los veintiún triggers
```

Desde la línea de comandos:

```bash
mysql -u root -p < base_de_datos/01_creacion_bd.sql
mysql -u root -p < base_de_datos/02_datos_prueba.sql
mysql -u root -p < base_de_datos/03_vistas.sql
mysql -u root -p < base_de_datos/04_procedimientos.sql
mysql -u root -p < base_de_datos/05_reportes.sql
mysql -u root -p < base_de_datos/06_triggers.sql
```

> El primer script comienza con `DROP DATABASE IF EXISTS marietha`, de modo que
> la instalación puede repetirse desde cero. Si ya tiene datos propios
> cargados, se perderán.

Los triggers se crean al final, después de la carga de datos, para que la
bitácora de auditoría refleje únicamente la operación real del aplicativo y no
las más de trescientas filas de la carga inicial.

### 2. Cuenta de base de datos

```sql
CREATE USER 'marietha_app'@'localhost' IDENTIFIED BY 'Marietha2026';
GRANT ALL PRIVILEGES ON marietha.* TO 'marietha_app'@'localhost';
FLUSH PRIVILEGES;
```

### 3. Configuración

Edite `config.properties` con los datos de su servidor:

```properties
bd.url=jdbc:mariadb://localhost:3306/marietha
bd.usuario=marietha_app
bd.clave=Marietha2026
bd.controlador=org.mariadb.jdbc.Driver
```

El archivo es externo al programa: cambiar de servidor no exige recompilar.

## Ejecución

```bash
java -jar PermiCargaMarietha.jar
```

O bien `iniciar.bat` en Windows, `iniciar.sh` en Linux y macOS. Ambos
comprueban que Java esté instalado antes de arrancar y funcionan desde
cualquier carpeta, porque se sitúan en su propio directorio en lugar de
depender de una ruta fija.

### Credenciales de prueba

| Usuario | Contraseña | Perfil |
|---|---|---|
| `admin` | `admin123` | ADMINISTRADOR |
| `operador` | `oper123` | OPERADOR |
| `consulta` | `cons123` | CONSULTA |
| `exoperador` | `exop123` | OPERADOR (cuenta desactivada) |

Las contraseñas se almacenan como resumen SHA-256, no en texto plano.
**Cámbielas antes de usar el sistema con información real.**

## Compilación

Requiere el JDK, no solo el JRE.

```bash
./compilar.sh      # Linux y macOS
compilar.bat       # Windows
```

Genera un JAR único que incluye el controlador JDBC, para que se ejecute en
cualquier computadora con Java sin instalar nada más.

## Estructura del proyecto

```
src/pe/edu/ucsm/gdi/permicarga/
├── Main.java                  Punto de entrada
├── config/                    Lectura de la configuración externa
│   └── Configuracion.java
├── datos/                     Capa de acceso a datos
│   ├── Conexion.java          Administra la conexión JDBC
│   ├── Repositorio.java       Único punto de contacto con la base
│   └── Resultado.java         Tabla de resultados genérica
├── modelo/
│   └── Sesion.java            Usuario y perfil de la sesión abierta
├── util/
│   └── Dialogos.java          Mensajes al usuario
└── vista/                     Capa de presentación (Swing)
    ├── VentanaAcceso.java     Ingreso al sistema
    ├── VentanaPrincipal.java  Ventana con los módulos en pestañas
    ├── PanelInicio.java       Indicadores de la operación
    ├── PanelMantenimiento.java  Armazón común de los módulos ABM
    ├── PanelEmpresas.java
    ├── PanelVehiculos.java
    ├── PanelAutorizaciones.java
    ├── PanelReportes.java     Catálogo de los quince reportes
    ├── DialogoEmpresa.java
    ├── DialogoVehiculo.java
    ├── DialogoAutorizacion.java
    ├── Formulario.java        Armador de formularios alineados
    └── Estilo.java            Apariencia común

base_de_datos/                 Los seis scripts SQL
lib/                           Controlador JDBC
docs/                          Manual de usuario y memoria descriptiva
```

## La base de datos

| Elemento | Cantidad |
|---|---|
| Tablas | 9 |
| Vistas | 4 |
| Procedimientos almacenados | 55 (40 de mantenimiento, 15 de reporte) |
| Triggers | 21 |
| Índices | 26 |
| Filas de prueba | más de 300 |

De los quince reportes, trece resuelven con `JOIN`; el de historial de una
unidad enlaza cuatro tablas.

## Licencia

Trabajo académico. Los derechos de autor del software se ceden a la
Universidad Católica de Santa María conforme al documento de cesión incluido
en la entrega.
