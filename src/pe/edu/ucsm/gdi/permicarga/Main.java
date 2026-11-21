package pe.edu.ucsm.gdi.permicarga;

import javax.swing.SwingUtilities;

import pe.edu.ucsm.gdi.permicarga.datos.Conexion;
import pe.edu.ucsm.gdi.permicarga.vista.Estilo;
import pe.edu.ucsm.gdi.permicarga.vista.VentanaAcceso;
import pe.edu.ucsm.gdi.permicarga.vista.VentanaPrincipal;

/**
 * Punto de entrada de PermiCarga Marietha.
 *
 * <p>Aplicativo cliente que consume la base de datos de la funcionalidad de
 * Solicitud de Permiso de Carga Sobredimensionada de Transportes y Maquinarias
 * Marietha E.I.R.L.</p>
 *
 * <p>Construido unicamente con la biblioteca estandar de Java. La interfaz usa
 * Swing y el acceso a datos usa JDBC, ambas partes del propio JDK; la unica
 * biblioteca externa es el controlador del gestor de base de datos, que no es
 * un marco de trabajo sino el componente que traduce JDBC al protocolo de red
 * del servidor. No se emplea ningun marco de trabajo ni mapeador objeto
 * relacional, y toda operacion sobre los datos se realiza invocando
 * procedimientos almacenados.</p>
 *
 * <p>Curso de Gestion de Datos e Informacion, Escuela Profesional de Ingenieria
 * de Sistemas, Universidad Catolica de Santa Maria, Arequipa.</p>
 *
 * @author Grupo de Gestion de Datos e Informacion - UCSM
 * @version 1.0
 */
public final class Main {

    /** Clase de arranque: no se instancia. */
    private Main() {
    }

    /**
     * Arranca el aplicativo.
     *
     * <p>Toda la interfaz se construye dentro del hilo de despacho de eventos
     * de Swing, como exige esa biblioteca: construir componentes desde el hilo
     * principal produce fallos intermitentes dificiles de reproducir.</p>
     *
     * @param args no se utilizan
     */
    public static void main(String[] args) {

        // Al cerrar el programa de cualquier manera, incluida la combinacion
        // Ctrl+C en consola, se libera la conexion con el gestor.
        Runtime.getRuntime().addShutdownHook(new Thread(Conexion::cerrar));

        SwingUtilities.invokeLater(() -> {
            Estilo.aplicar();

            VentanaAcceso acceso = new VentanaAcceso();
            acceso.setVisible(true);

            if (!acceso.ingreso()) {
                Conexion.cerrar();
                System.exit(0);
                return;
            }

            new VentanaPrincipal().setVisible(true);
        });
    }
}
