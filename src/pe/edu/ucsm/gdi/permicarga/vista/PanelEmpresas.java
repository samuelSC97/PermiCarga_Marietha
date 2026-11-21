package pe.edu.ucsm.gdi.permicarga.vista;

import javax.swing.SwingUtilities;

/**
 * Modulo de mantenimiento de empresas transportistas.
 *
 * <p>Aporta al armazon comun unicamente lo que distingue a esta entidad: los
 * procedimientos que debe invocar y el formulario que debe abrir.</p>
 *
 * @author Grupo de Gestion de Datos e Informacion - UCSM
 */
public final class PanelEmpresas extends PanelMantenimiento {

    private static final long serialVersionUID = 1L;

    /** Arma el modulo y carga el listado inicial. */
    public PanelEmpresas() {
        super("Empresas transportistas",
              "Empresas que solicitan permisos de circulacion ante Provias Nacional.");
        recargar();
    }

    @Override
    protected String procedimientoListar() {
        return "SP_EMPRESA_LISTAR";
    }

    @Override
    protected String procedimientoBuscar() {
        return "SP_EMPRESA_BUSCAR";
    }

    @Override
    protected String columnaClave() {
        return "RUC";
    }

    @Override
    protected String procedimientoBaja() {
        return "SP_EMPRESA_BAJA";
    }

    @Override
    protected String procedimientoReactivar() {
        return "SP_EMPRESA_REACTIVAR";
    }

    @Override
    protected String preguntaBaja(String clave) {
        return "Se dara de baja a la empresa con RUC " + clave + ".\n\n"
                + "La empresa dejara de aparecer para emitir permisos nuevos, "
                + "pero sus autorizaciones historicas se conservan.\n\n"
                + "Desea continuar?";
    }

    @Override
    protected void alRegistrar() {
        DialogoEmpresa d = new DialogoEmpresa(
                SwingUtilities.getWindowAncestor(this), null);
        d.setVisible(true);
        if (d.seGuardo()) {
            recargar();
        }
    }

    @Override
    protected void alModificar(String clave) {
        DialogoEmpresa d = new DialogoEmpresa(
                SwingUtilities.getWindowAncestor(this), clave);
        d.setVisible(true);
        if (d.seGuardo()) {
            recargar();
        }
    }
}
