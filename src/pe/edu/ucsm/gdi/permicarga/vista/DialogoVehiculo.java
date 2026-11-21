package pe.edu.ucsm.gdi.permicarga.vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import pe.edu.ucsm.gdi.permicarga.datos.Repositorio;
import pe.edu.ucsm.gdi.permicarga.datos.Resultado;
import pe.edu.ucsm.gdi.permicarga.util.Dialogos;

/**
 * Formulario de alta y modificacion de unidades vehiculares.
 *
 * <p>El peso seco es el campo delicado de esta pantalla. Fue su duplicacion a
 * mano en cada formulario en papel lo que produjo la inconsistencia que origino
 * el proyecto: la misma unidad figuraba con dos taras distintas en dos permisos.
 * Aqui el dato se registra una sola vez y todas las autorizaciones lo leen de
 * esta ficha, de modo que ya no puede divergir.</p>
 *
 * @author Grupo de Gestion de Datos e Informacion - UCSM
 */
public final class DialogoVehiculo extends JDialog {

    private static final long serialVersionUID = 1L;

    private final JTextField txtPlaca = new JTextField(10);
    private final JComboBox<String> cboTipo =
            new JComboBox<>(new String[] {"Tracto", "Cama baja", "Semirremolque"});
    private final JTextField txtMarca = new JTextField(18);
    private final JTextField txtModelo = new JTextField(18);
    private final JTextField txtLargo = new JTextField(8);
    private final JTextField txtAncho = new JTextField(8);
    private final JTextField txtAlto = new JTextField(8);
    private final JSpinner spnEjes = new JSpinner(new SpinnerNumberModel(3, 1, 7, 1));
    private final JSpinner spnNeumaticos =
            new JSpinner(new SpinnerNumberModel(10, 2, 40, 2));
    private final JTextField txtPeso = new JTextField(10);

    private final String claveOriginal;
    private boolean guardado = false;

    /**
     * @param padre ventana sobre la que se centra
     * @param placa placa a editar, o {@code null} para registrar una nueva
     */
    public DialogoVehiculo(Window padre, String placa) {
        super(padre, placa == null ? "Registrar unidad vehicular"
                : "Modificar unidad vehicular", ModalityType.APPLICATION_MODAL);
        this.claveOriginal = placa;

        setLayout(new BorderLayout(0, 10));
        ((JPanel) getContentPane()).setBorder(
                BorderFactory.createEmptyBorder(14, 16, 12, 16));

        add(Estilo.titulo(placa == null ? "Nueva unidad" : "Unidad " + placa),
                BorderLayout.NORTH);
        add(armarFormulario(), BorderLayout.CENTER);
        add(armarBotones(), BorderLayout.SOUTH);

        if (placa != null) {
            cargar(placa);
            txtPlaca.setEnabled(false);
            txtPlaca.setToolTipText("La placa identifica a la unidad y no puede cambiarse.");
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
        JPanel contenedor = new JPanel(new BorderLayout(0, 8));
        contenedor.setOpaque(false);

        Formulario ident = new Formulario();
        ident.setBorder(Estilo.grupo("Identificacion"));
        ident.fila("Placa:", txtPlaca, "Tipo de unidad:", cboTipo);
        ident.fila("Marca:", txtMarca, "Modelo:", txtModelo);

        Formulario tecn = new Formulario();
        tecn.setBorder(Estilo.grupo("Ficha tecnica"));
        tecn.fila("Largo (m):", txtLargo, "Ancho (m):", txtAncho);
        tecn.fila("Alto (m):", txtAlto, "Numero de ejes:", spnEjes);
        tecn.fila("Neumaticos:", spnNeumaticos, "Peso seco (kg):", txtPeso);
        tecn.nota("El peso seco es la tara de la unidad sin carga. Se registra una "
                + "sola vez aqui y todas las autorizaciones lo toman de esta ficha.");

        contenedor.add(ident, BorderLayout.NORTH);
        contenedor.add(tecn, BorderLayout.CENTER);
        return contenedor;
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
     * Trae los datos actuales de la unidad que se va a modificar.
     *
     * @param placa clave del registro
     */
    private void cargar(String placa) {
        try {
            Resultado r = Repositorio.llamar("SP_VEHICULO_OBTENER", placa);
            if (!r.estaVacio()) {
                txtPlaca.setText(r.texto(0, "Placa_de_vehiculo"));
                cboTipo.setSelectedItem(r.texto(0, "Descripcion_del_vehiculo"));
                txtMarca.setText(r.texto(0, "Marca_del_vehiculo"));
                txtModelo.setText(r.texto(0, "Modelo_de_vehiculo"));
                txtLargo.setText(r.texto(0, "Largo_de_vehiculo"));
                txtAncho.setText(r.texto(0, "Ancho_de_vehiculo"));
                txtAlto.setText(r.texto(0, "Alto_de_vehiculo"));
                spnEjes.setValue(entero(r.texto(0, "Numero_de_ejes"), 3));
                spnNeumaticos.setValue(entero(r.texto(0, "Numero_de_neumaticos"), 10));
                txtPeso.setText(r.texto(0, "Peso_seco"));
            }
        } catch (Exception ex) {
            Dialogos.error(this, ex);
        }
    }

    /** Valida lo escrito y lo envia al procedimiento correspondiente. */
    private void guardar() {
        String placa = txtPlaca.getText().trim().toUpperCase();
        String tipo = String.valueOf(cboTipo.getSelectedItem());
        String marca = txtMarca.getText().trim();
        String modelo = txtModelo.getText().trim();

        if (!placa.matches("[A-Z0-9]{6}")) {
            Dialogos.advertencia(this,
                    "La placa debe tener exactamente 6 caracteres, letras o digitos.");
            txtPlaca.requestFocus();
            return;
        }
        if (marca.isEmpty() || modelo.isEmpty()) {
            Dialogos.advertencia(this, "La marca y el modelo son obligatorios.");
            return;
        }

        double largo = decimal(txtLargo.getText(), -1);
        double ancho = decimal(txtAncho.getText(), -1);
        double alto = decimal(txtAlto.getText(), -1);
        int peso = entero(txtPeso.getText(), -1);

        if (largo <= 0 || ancho <= 0 || alto <= 0) {
            Dialogos.advertencia(this,
                    "Las dimensiones deben ser numeros mayores que cero. "
                            + "Use el punto como separador decimal.");
            return;
        }
        if (peso <= 0) {
            Dialogos.advertencia(this,
                    "El peso seco debe ser un numero entero de kilogramos mayor que cero.");
            txtPeso.requestFocus();
            return;
        }

        int ejes = (Integer) spnEjes.getValue();
        int neum = (Integer) spnNeumaticos.getValue();
        if (neum < ejes * 2) {
            Dialogos.advertencia(this,
                    "Un eje no puede llevar menos de dos neumaticos. "
                            + "Con " + ejes + " ejes hacen falta al menos " + (ejes * 2) + ".");
            return;
        }

        try {
            String mensaje = claveOriginal == null
                    ? Repositorio.ejecutar("SP_VEHICULO_INSERTAR", placa, tipo, marca,
                            modelo, largo, ancho, alto, ejes, neum, peso)
                    : Repositorio.ejecutar("SP_VEHICULO_MODIFICAR", placa, tipo, marca,
                            modelo, largo, ancho, alto, ejes, neum, peso);

            guardado = true;
            Dialogos.exito(this, mensaje);
            dispose();

        } catch (Exception ex) {
            Dialogos.error(this, ex);
        }
    }

    /**
     * Convierte un texto a numero decimal.
     *
     * @param texto      valor escrito por el usuario
     * @param porDefecto valor a devolver si no es numerico
     * @return numero leido
     */
    private static double decimal(String texto, double porDefecto) {
        try {
            return Double.parseDouble(texto.trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            return porDefecto;
        }
    }

    /**
     * Convierte un texto a numero entero.
     *
     * @param texto      valor escrito por el usuario
     * @param porDefecto valor a devolver si no es numerico
     * @return numero leido
     */
    private static int entero(String texto, int porDefecto) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException ex) {
            return porDefecto;
        }
    }
}
