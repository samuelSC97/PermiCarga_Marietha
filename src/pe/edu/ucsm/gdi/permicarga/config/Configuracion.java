package pe.edu.ucsm.gdi.permicarga.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Lee los parametros de conexion desde un archivo externo al programa.
 *
 * <p>Se resolvio asi, y no con los datos escritos dentro del codigo, porque el
 * aplicativo tiene que poder ejecutarse en una maquina distinta a la del
 * desarrollo sin volver a compilarlo: basta con editar el archivo de texto
 * {@code config.properties} que acompana al ejecutable.</p>
 *
 * <p>El archivo se busca en este orden:</p>
 * <ol>
 *   <li>La ruta indicada en la propiedad de sistema {@code permicarga.config}.</li>
 *   <li>Un archivo {@code config.properties} en el directorio de trabajo.</li>
 *   <li>El archivo incluido dentro del propio JAR, que sirve de valor por
 *       defecto para que el programa arranque aunque no se haya configurado.</li>
 * </ol>
 *
 * @author Grupo de Gestion de Datos e Informacion - UCSM
 */
public final class Configuracion {

    /** Nombre del archivo de configuracion. */
    private static final String ARCHIVO = "config.properties";

    /** Instancia unica, cargada la primera vez que se solicita. */
    private static Configuracion instancia;

    /** Propiedades ya leidas. */
    private final Properties props = new Properties();

    /** Ruta desde la que se leyo la configuracion, para poder informarla. */
    private String origen = "valores incluidos en el programa";

    private Configuracion() {
        cargar();
    }

    /**
     * Devuelve la configuracion del programa, cargandola la primera vez.
     *
     * @return instancia unica de configuracion
     */
    public static synchronized Configuracion obtener() {
        if (instancia == null) {
            instancia = new Configuracion();
        }
        return instancia;
    }

    /** Intenta leer el archivo de las tres ubicaciones posibles. */
    private void cargar() {
        String indicada = System.getProperty("permicarga.config");
        if (indicada != null && leerDesde(new File(indicada))) {
            return;
        }
        if (leerDesde(new File(ARCHIVO))) {
            return;
        }
        try (InputStream dentro =
                     Configuracion.class.getResourceAsStream("/" + ARCHIVO)) {
            if (dentro != null) {
                props.load(dentro);
            }
        } catch (IOException ex) {
            // Si tampoco esta dentro del JAR se usan los valores por defecto
            // que devuelve cada metodo de lectura.
        }
    }

    /**
     * Lee la configuracion de un archivo concreto.
     *
     * @param archivo archivo a leer
     * @return {@code true} si se pudo leer
     */
    private boolean leerDesde(File archivo) {
        if (archivo == null || !archivo.isFile()) {
            return false;
        }
        try (InputStream entrada = new FileInputStream(archivo)) {
            props.load(entrada);
            origen = archivo.getAbsolutePath();
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Lee un valor de texto.
     *
     * @param clave        nombre de la propiedad
     * @param porDefecto   valor a usar si no esta definida
     * @return valor configurado o el valor por defecto
     */
    public String texto(String clave, String porDefecto) {
        String valor = props.getProperty(clave);
        return (valor == null || valor.trim().isEmpty()) ? porDefecto : valor.trim();
    }

    /**
     * Lee un valor numerico entero.
     *
     * @param clave      nombre de la propiedad
     * @param porDefecto valor a usar si no esta definida o no es numerica
     * @return valor configurado o el valor por defecto
     */
    public int numero(String clave, int porDefecto) {
        try {
            return Integer.parseInt(texto(clave, String.valueOf(porDefecto)));
        } catch (NumberFormatException ex) {
            return porDefecto;
        }
    }

    /** @return direccion JDBC completa de la base de datos */
    public String url() {
        return texto("bd.url",
                "jdbc:mariadb://localhost:3306/marietha"
                        + "?useUnicode=true&characterEncoding=UTF-8");
    }

    /** @return nombre de la cuenta de base de datos */
    public String usuario() {
        return texto("bd.usuario", "root");
    }

    /** @return contrasena de la cuenta de base de datos */
    public String clave() {
        return texto("bd.clave", "");
    }

    /** @return clase del controlador JDBC a registrar */
    public String controlador() {
        return texto("bd.controlador", "org.mariadb.jdbc.Driver");
    }

    /** @return segundos a esperar al abrir la conexion */
    public int esperaConexion() {
        return numero("bd.espera.segundos", 10);
    }

    /** @return ruta del archivo de configuracion realmente utilizado */
    public String origen() {
        return origen;
    }
}
