package pe.edu.ucsm.gdi.permicarga.vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;

import pe.edu.ucsm.gdi.permicarga.datos.Repositorio;
import pe.edu.ucsm.gdi.permicarga.datos.Resultado;
import pe.edu.ucsm.gdi.permicarga.util.Dialogos;

/**
 * Formulario de alta y modificacion de empresas transportistas.
 *
 * <p>Las comprobaciones de formato que se hacen aqui no sustituyen a las de la
 * base de datos, las anticipan. El RUC se verifica igualmente con una
 * restriccion CHECK en la tabla; comprobarlo tambien en el formulario evita
 * un viaje al servidor para decirle al usuario algo que se sabe de antemano.</p>
 *
 * @author Grupo de Gestion de Datos e Informacion - UCSM
 */
public final class DialogoEmpresa extends JDialog {

    private static final long serialVersionUID = 1L;

    private final JTextField txtRuc = new JTextField(14);
    private final JTextField txtNombre = new JTextField(30);
    private final JTextField txtRepresentante = new JTextField(26);
    private final JTextField txtDomicilio = new JTextField(34);
    private final JTextField txtMtc = new JTextField(14);
    private final JTextField txtTelefono = new JTextField(12);

    /** Clave del registro en edicion, o {@code null} si es un alta. */
    private final String claveOriginal;

    /** Queda en verdadero si el usuario guardo. */
    private boolean guardado = false;

    /**
     * @param padre ventana sobre la que se centra
     * @param ruc   RUC a editar, o {@code null} para registrar uno nuevo
     */
    public DialogoEmpresa(Window padre, String ruc) {
        super(padre, ruc == null ? "Registrar empresa transportista"
                : "Modificar empresa transportista", ModalityType.APPLICATION_MODAL);
        this.claveOriginal = ruc;

        setLayout(new BorderLayout(0, 10));
        ((JPanel) getContentPane()).setBorder(
                BorderFactory.createEmptyBorder(14, 16, 12, 16));

        add(Estilo.titulo(ruc == null ? "Nueva empresa" : "Empresa " + ruc),
                BorderLayout.NORTH);
        add(armarFormulario(), BorderLayout.CENTER);
        add(armarBotones(), BorderLayout.SOUTH);

        if (ruc != null) {
            cargar(ruc);
            txtRuc.setEnabled(false);
            txtRuc.setToolTipText("El RUC identifica a la empresa y no puede cambiarse.");
        }

        pack();
        setResizable(false);
        setLocationRelativeTo(padre);
        Dialogos.cerrarConEscape(this);
    }

    /** @return {@code true} si el usuario guardo los cambios */
    public boolean seGuardo() {
        return guardado;
    }

    /** Construye los campos del formulario. */
    private JPanel armarFormulario() {
        Formulario f = new Formulario();
        f.setBorder(Estilo.grupo("Datos registrales"));
        f.fila("RUC:", txtRuc, "Registro MTC:", txtMtc);
        f.fila("Razon social:", txtNombre);
        f.fila("Representante legal:", txtRepresentante);
        f.fila("Domicilio legal:", txtDomicilio);
        f.fila("Telefono:", txtTelefono);
        f.nota("El RUC debe tener 11 digitos. El registro MTC son 8 digitos "
                + "seguidos de 3 letras. El telefono es opcional, 9 digitos.");
        return f;
    }

    /** Construye la barra de botones. */
    private JPanel armarBotones() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        p.setOpaque(false);

        JButton cancelar = Estilo.boton("Cancelar");
        cancelar.addActionListener(e -> dispose());

        JButton guardar = Estilo.botonPrincipal("Guardar");
        guardar.addActionListener(e -> guardar());

        p.add(cancelar);
        p.add(guardar);
        getRootPane().setDefaultButton(guardar);
        return p;
    }

    /**
     * Trae los datos actuales del registro que se va a modificar.
     *
     * @param ruc clave del registro
     */
    private void cargar(String ruc) {
        try {
            Resultado r = Repositorio.llamar("SP_EMPRESA_OBTENER", ruc);
            if (!r.estaVacio()) {
                txtRuc.setText(r.texto(0, "RUC_de_la_empresa"));
                txtNombre.setText(r.texto(0, "Nombre_empresa_transporte"));
                txtRepresentante.setText(r.texto(0, "Representante"));
                txtDomicilio.setText(r.texto(0, "Domicilio"));
                txtMtc.setText(r.texto(0, "Registro_MTC"));
                txtTelefono.setText(r.texto(0, "Telefono"));
            }
        } catch (Exception ex) {
            Dialogos.error(this, ex);
        }
    }

    /** Valida lo escrito y lo envia al procedimiento correspondiente. */
    private void guardar() {
        String ruc = txtRuc.getText().trim();
        String nombre = txtNombre.getText().trim();
        String repr = txtRepresentante.getText().trim();
        String domic = txtDomicilio.getText().trim();
        String mtc = txtMtc.getText().trim().toUpperCase();
        String telef = txtTelefono.getText().trim();

        if (!ruc.matches("\\d{11}")) {
            Dialogos.advertencia(this, "El RUC debe tener exactamente 11 digitos.");
            txtRuc.requestFocus();
            return;
        }
        if (nombre.isEmpty() || repr.isEmpty() || domic.isEmpty()) {
            Dialogos.advertencia(this,
                    "La razon social, el representante y el domicilio son obligatorios.");
            return;
        }
        if (!mtc.matches("\\d{8}[A-Z]{3}")) {
            Dialogos.advertencia(this,
                    "El registro MTC debe tener 8 digitos seguidos de 3 letras.");
            txtMtc.requestFocus();
            return;
        }
        if (!telef.isEmpty() && !telef.matches("\\d{9}")) {
            Dialogos.advertencia(this,
                    "El telefono debe tener 9 digitos, o dejarse vacio.");
            txtTelefono.requestFocus();
            return;
        }

        try {
            String mensaje = claveOriginal == null
                    ? Repositorio.ejecutar("SP_EMPRESA_INSERTAR",
                            ruc, nombre, repr, domic, mtc, telef)
                    : Repositorio.ejecutar("SP_EMPRESA_MODIFICAR",
                            ruc, nombre, repr, domic, mtc, telef);

            guardado = true;
            Dialogos.exito(this, mensaje);
            dispose();

        } catch (Exception ex) {
            Dialogos.error(this, ex);
        }
    }
}
