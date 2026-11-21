package pe.edu.ucsm.gdi.permicarga.vista;

import javax.swing.SwingUtilities;

/**
 * Modulo de mantenimiento de la flota vehicular.
 *
 * @author Grupo de Gestion de Datos e Informacion - UCSM
 */
public final class PanelVehiculos extends PanelMantenimiento {

    private static final long serialVersionUID = 1L;

    /** Arma el modulo y carga el listado inicial. */
    public PanelVehiculos() {
        super("Flota vehicular",
              "Unidades disponibles para declararse en una autorizacion de carga.");
        recargar();
    }

    @Override
    protected String procedimientoListar() {
        return "SP_VEHICULO_LISTAR";
    }

    @Override
    protected String procedimientoBuscar() {
        return "SP_VEHICULO_BUSCAR";
    }

    @Override
    protected String columnaClave() {
        return "Placa";
    }

    @Override
    protected String procedimientoBaja() {
        return "SP_VEHICULO_BAJA";
    }

    @Override
    protected String procedimientoReactivar() {
        return "SP_VEHICULO_REACTIVAR";
    }

    @Override
    protected String preguntaBaja(String clave) {
        return "Se retirara de la flota operativa la unidad " + clave + ".\n\n"
                + "La unidad dejara de ofrecerse al declarar un conjunto vehicular, "
                + "pero su historial de traslados se conserva intacto.\n\n"
                + "Desea continuar?";
    }

    @Override
    protected void alRegistrar() {
        DialogoVehiculo d = new DialogoVehiculo(
                SwingUtilities.getWindowAncestor(this), null);
        d.setVisible(true);
        if (d.seGuardo()) {
            recargar();
        }
    }

    @Override
    protected void alModificar(String clave) {
        DialogoVehiculo d = new DialogoVehiculo(
                SwingUtilities.getWindowAncestor(this), clave);
        d.setVisible(true);
        if (d.seGuardo()) {
            recargar();
        }
    }
}
