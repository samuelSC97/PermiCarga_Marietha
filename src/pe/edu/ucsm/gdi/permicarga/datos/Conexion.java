package pe.edu.ucsm.gdi.permicarga.datos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import pe.edu.ucsm.gdi.permicarga.config.Configuracion;

/**
 * Administra la conexion con el gestor de base de datos.
 *
 * <p>El aplicativo mantiene una sola conexion abierta durante la sesion de
 * trabajo. Para un cliente de escritorio de un solo usuario esta es la opcion
 * correcta: un conjunto de conexiones (pool) resolveria un problema de
 * concurrencia que aqui no existe, y a cambio anadiria una dependencia externa,
 * cosa que la consigna no permite.</p>
 *
 * <p>No se usa ninguna biblioteca de acceso a datos ni mapeador objeto
 * relacional. Se trabaja con la interfaz JDBC que forma parte de la biblioteca
 * estandar de Java, y con el controlador del gestor, que es el componente que
 * traduce las llamadas JDBC al protocolo de red del servidor.</p>
 *
 * @author Grupo de Gestion de Datos e Informacion - UCSM
 */
public final class Conexion {

    /** Conexion viva con el gestor, o {@code null} si todavia no se abrio. */
    private static Connection conexion;

    /** Clase de utilidad: no se instancia. */
    private Conexion() {
    }

    /**
     * Devuelve la conexion abierta, estableciendola si hiciera falta.
     *
     * @return conexion lista para usar
     * @throws SQLException si el gestor no responde o las credenciales fallan
     */
    public static synchronized Connection obtener() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            abrir();
        }
        return conexion;
    }

    /**
     * Establece la conexion con los parametros del archivo de configuracion.
     *
     * @throws SQLException si no se puede conectar
     */
    private static void abrir() throws SQLException {
        Configuracion cfg = Configuracion.obtener();

        // El controlador se registra por su nombre de clase, que tambien se lee
        // de la configuracion. De ese modo el aplicativo puede trabajar con el
        // controlador de MariaDB o con el oficial de MySQL sin recompilar: basta
        // con dejar el archivo .jar correspondiente en la carpeta lib y cambiar
        // dos lineas del archivo de configuracion.
        try {
            Class.forName(cfg.controlador());
        } catch (ClassNotFoundException ex) {
            throw new SQLException(
                    "No se encontro el controlador JDBC indicado en la configuracion ("
                            + cfg.controlador() + "). Verifique que el archivo .jar del "
                            + "controlador este en la carpeta lib.", ex);
        }

        DriverManager.setLoginTimeout(cfg.esperaConexion());
        conexion = DriverManager.getConnection(cfg.url(), cfg.usuario(), cfg.clave());

        // Cada operacion de mantenimiento se confirma por si sola dentro de su
        // procedimiento almacenado, de modo que la confirmacion automatica es
        // el comportamiento deseado.
        conexion.setAutoCommit(true);
    }

    /**
     * Comprueba si la conexion esta establecida y responde.
     *
     * @return {@code true} si la base de datos responde
     */
    public static synchronized boolean activa() {
        try {
            return conexion != null && !conexion.isClosed() && conexion.isValid(3);
        } catch (SQLException ex) {
            return false;
        }
    }

    /**
     * Devuelve una descripcion del servidor al que se esta conectado, para
     * mostrarla en la barra de estado de la ventana principal.
     *
     * @return nombre y version del gestor, o un aviso si no hay conexion
     */
    public static synchronized String descripcionServidor() {
        try {
            if (!activa()) {
                return "sin conexion";
            }
            return conexion.getMetaData().getDatabaseProductName() + " "
                    + conexion.getMetaData().getDatabaseProductVersion();
        } catch (SQLException ex) {
            return "sin conexion";
        }
    }

    /** Cierra la conexion al terminar la sesion de trabajo. */
    public static synchronized void cerrar() {
        if (conexion != null) {
            try {
                conexion.close();
            } catch (SQLException ex) {
                // Al cerrar el programa no hay nada que se pueda hacer con el
                // error, y molestar al usuario con un aviso no aporta nada.
            }
            conexion = null;
        }
    }
}
