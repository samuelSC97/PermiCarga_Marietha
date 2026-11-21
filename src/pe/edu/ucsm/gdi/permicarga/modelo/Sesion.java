package pe.edu.ucsm.gdi.permicarga.modelo;

/**
 * Datos del usuario que tiene abierta la sesion de trabajo.
 *
 * <p>El perfil determina que puede hacer cada cuenta dentro del aplicativo:</p>
 *
 * <ul>
 *   <li><b>ADMINISTRADOR</b>: todo, incluida la baja de registros y la consulta
 *       de la bitacora de auditoria.</li>
 *   <li><b>OPERADOR</b>: registrar y modificar, pero no dar de baja.</li>
 *   <li><b>CONSULTA</b>: unicamente ver informacion y emitir reportes.</li>
 * </ul>
 *
 * <p>Conviene precisar el alcance de este control. Es una comodidad de la
 * interfaz: oculta y deshabilita lo que la cuenta no debe usar, para que nadie
 * se equivoque. No es una medida de seguridad, porque quien tenga las
 * credenciales de la base de datos puede saltarselo. La proteccion real son los
 * permisos que el gestor concede a cada cuenta de base de datos, y las reglas
 * que los procedimientos hacen cumplir pase lo que pase.</p>
 *
 * @author Grupo de Gestion de Datos e Informacion - UCSM
 */
public final class Sesion {

    /** Sesion abierta en este momento, o {@code null} si nadie ingreso. */
    private static Sesion actual;

    private final int id;
    private final String usuario;
    private final String nombreCompleto;
    private final String perfil;
    private final String ultimoAcceso;

    /**
     * @param id             identificador interno de la cuenta
     * @param usuario        nombre con el que ingreso
     * @param nombreCompleto nombre de la persona
     * @param perfil         ADMINISTRADOR, OPERADOR o CONSULTA
     * @param ultimoAcceso   fecha y hora del ingreso anterior
     */
    public Sesion(int id, String usuario, String nombreCompleto,
                  String perfil, String ultimoAcceso) {
        this.id = id;
        this.usuario = usuario;
        this.nombreCompleto = nombreCompleto;
        this.perfil = perfil;
        this.ultimoAcceso = ultimoAcceso;
    }

    /** @return sesion abierta, o {@code null} si no hay ninguna */
    public static Sesion actual() {
        return actual;
    }

    /**
     * Registra la sesion recien abierta.
     *
     * @param sesion datos del usuario que ingreso
     */
    public static void abrir(Sesion sesion) {
        actual = sesion;
    }

    /** Cierra la sesion de trabajo. */
    public static void cerrar() {
        actual = null;
    }

    /** @return identificador interno de la cuenta */
    public int id() {
        return id;
    }

    /** @return nombre con el que ingreso */
    public String usuario() {
        return usuario;
    }

    /** @return nombre completo de la persona */
    public String nombreCompleto() {
        return nombreCompleto;
    }

    /** @return perfil asignado a la cuenta */
    public String perfil() {
        return perfil;
    }

    /** @return fecha y hora del ingreso anterior */
    public String ultimoAcceso() {
        return ultimoAcceso == null || ultimoAcceso.isEmpty()
                ? "primer ingreso" : ultimoAcceso;
    }

    /** @return {@code true} si la cuenta puede registrar y modificar */
    public boolean puedeEditar() {
        return "ADMINISTRADOR".equals(perfil) || "OPERADOR".equals(perfil);
    }

    /** @return {@code true} si la cuenta puede dar de baja registros */
    public boolean puedeDarDeBaja() {
        return "ADMINISTRADOR".equals(perfil);
    }

    /** @return {@code true} si la cuenta puede consultar la auditoria */
    public boolean puedeVerAuditoria() {
        return "ADMINISTRADOR".equals(perfil);
    }
}
