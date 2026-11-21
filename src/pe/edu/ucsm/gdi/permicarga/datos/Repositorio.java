package pe.edu.ucsm.gdi.permicarga.datos;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Unico punto por el que el aplicativo habla con la base de datos.
 *
 * <p>Esta clase solo sabe hacer una cosa: invocar un procedimiento almacenado y
 * traer de vuelta lo que devuelva. En todo el programa no se escribe una sola
 * sentencia SELECT, INSERT, UPDATE ni DELETE. Esa decision no es estetica:</p>
 *
 * <ul>
 *   <li><b>Seguridad.</b> Los valores viajan como parametros de un
 *       {@link CallableStatement}, nunca concatenados dentro de una cadena de
 *       texto. La inyeccion de SQL deja de ser posible porque no hay texto SQL
 *       que manipular en el cliente.</li>
 *   <li><b>Una sola copia de la regla de negocio.</b> Las validaciones viven en
 *       los procedimientos. Si manana se escribe otro cliente, hereda las mismas
 *       reglas sin volver a programarlas.</li>
 *   <li><b>Mensajes de error utiles.</b> Los procedimientos informan los errores
 *       de negocio con SIGNAL SQLSTATE '45000' y un texto en castellano, que
 *       este repositorio distingue de una falla tecnica y entrega a la interfaz
 *       tal cual, para mostrarselo al usuario.</li>
 * </ul>
 *
 * @author Grupo de Gestion de Datos e Informacion - UCSM
 */
public final class Repositorio {

    /** Estado SQL con que los procedimientos informan un error de negocio. */
    private static final String ESTADO_NEGOCIO = "45000";

    /** Clase de utilidad: no se instancia. */
    private Repositorio() {
    }

    /**
     * Invoca un procedimiento almacenado y devuelve su tabla de resultados.
     *
     * @param procedimiento nombre del procedimiento, sin parentesis
     * @param parametros    valores de entrada, en el orden declarado
     * @return tabla con lo que devolvio el procedimiento; vacia si no devuelve
     *         ningun conjunto de resultados
     * @throws ErrorDeNegocio si el procedimiento rechazo la operacion por una
     *                        regla de negocio
     * @throws SQLException   si ocurre una falla tecnica
     */
    public static Resultado llamar(String procedimiento, Object... parametros)
            throws SQLException {

        Connection con = Conexion.obtener();
        String llamada = armarLlamada(procedimiento, parametros.length);

        try (CallableStatement cs = con.prepareCall(llamada)) {
            for (int i = 0; i < parametros.length; i++) {
                cs.setObject(i + 1, parametros[i]);
            }

            boolean hayResultado = cs.execute();
            if (!hayResultado) {
                return Resultado.vacio();
            }

            try (ResultSet rs = cs.getResultSet()) {
                return leer(rs);
            }

        } catch (SQLException ex) {
            // Los procedimientos informan los errores de negocio con el estado
            // 45000. Ese mensaje esta escrito para que lo lea el usuario, de
            // modo que se transporta tal cual; cualquier otro error es una falla
            // tecnica y se deja subir sin tocar.
            if (ESTADO_NEGOCIO.equals(ex.getSQLState())) {
                throw new ErrorDeNegocio(limpiar(ex.getMessage()), ex);
            }
            throw ex;
        }
    }

    /**
     * Invoca un procedimiento del que no se espera tabla de resultados, o del
     * que solo interesa el mensaje de confirmacion.
     *
     * @param procedimiento nombre del procedimiento
     * @param parametros    valores de entrada
     * @return mensaje devuelto por el procedimiento, o texto vacio
     * @throws SQLException si ocurre una falla tecnica
     */
    public static String ejecutar(String procedimiento, Object... parametros)
            throws SQLException {

        Resultado r = llamar(procedimiento, parametros);
        if (r.estaVacio()) {
            return "";
        }
        int c = r.indiceColumna("Mensaje");
        return c >= 0 ? r.texto(0, c) : "";
    }

    /**
     * Arma el texto de invocacion con tantos marcadores como parametros.
     *
     * @param procedimiento nombre del procedimiento
     * @param cuantos       cantidad de parametros
     * @return sentencia de llamada con marcadores de posicion
     */
    private static String armarLlamada(String procedimiento, int cuantos) {
        StringBuilder sb = new StringBuilder("{CALL ").append(procedimiento).append('(');
        for (int i = 0; i < cuantos; i++) {
            sb.append(i == 0 ? "?" : ",?");
        }
        return sb.append(")}").toString();
    }

    /**
     * Vuelca un conjunto de resultados en una estructura independiente de JDBC,
     * para poder cerrar la conexion sin perder los datos.
     *
     * @param rs conjunto de resultados abierto
     * @return tabla con los datos leidos
     * @throws SQLException si falla la lectura
     */
    private static Resultado leer(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int n = meta.getColumnCount();

        List<String> columnas = new ArrayList<>(n);
        for (int i = 1; i <= n; i++) {
            // Se prefiere la etiqueta sobre el nombre fisico, porque es la que
            // lleva el alias legible que puso el procedimiento.
            String etiqueta = meta.getColumnLabel(i);
            columnas.add(etiqueta == null || etiqueta.isEmpty()
                    ? meta.getColumnName(i) : etiqueta);
        }

        List<Object[]> filas = new ArrayList<>();
        while (rs.next()) {
            Object[] fila = new Object[n];
            for (int i = 1; i <= n; i++) {
                fila[i - 1] = rs.getObject(i);
            }
            filas.add(fila);
        }
        return new Resultado(columnas, filas);
    }

    /**
     * Quita el prefijo tecnico que algunos controladores anteponen al mensaje
     * del procedimiento, para que el usuario lea unicamente la explicacion.
     *
     * @param mensaje mensaje original de la excepcion
     * @return mensaje limpio
     */
    private static String limpiar(String mensaje) {
        if (mensaje == null) {
            return "La operacion fue rechazada por la base de datos.";
        }
        int corte = mensaje.lastIndexOf(':');
        String limpio = corte >= 0 && corte < mensaje.length() - 2
                ? mensaje.substring(corte + 1) : mensaje;
        return limpio.trim();
    }

    /**
     * Error previsto por el diseno: la base de datos rechazo la operacion
     * porque incumple una regla de negocio. Su mensaje esta redactado para
     * mostrarse directamente al usuario.
     */
    public static class ErrorDeNegocio extends SQLException {

        private static final long serialVersionUID = 1L;

        /**
         * @param mensaje explicacion redactada por el procedimiento
         * @param causa   excepcion original
         */
        public ErrorDeNegocio(String mensaje, Throwable causa) {
            super(mensaje, ESTADO_NEGOCIO, causa);
        }
    }
}
