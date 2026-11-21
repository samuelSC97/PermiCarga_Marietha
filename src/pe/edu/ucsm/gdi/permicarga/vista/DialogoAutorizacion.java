package pe.edu.ucsm.gdi.permicarga.vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import pe.edu.ucsm.gdi.permicarga.datos.Repositorio;
import pe.edu.ucsm.gdi.permicarga.datos.Resultado;
import pe.edu.ucsm.gdi.permicarga.util.Dialogos;

/**
 * Formulario de alta y modificacion de autorizaciones de circulacion.
 *
 * <p>Reproduce el Formulario de Autorizacion para la Circulacion de Vehiculos
 * Especiales de Provias Nacional, que es el documento que la empresa llena hoy
 * a mano. Los campos estan agrupados igual que en el papel para que a quien lo
 * conoce le resulte familiar.</p>
 *
 * <p>Al registrar solo se captura la cabecera. El conjunto vehicular, los tramos
 * de ruta y la distribucion por eje se declaran despues desde la pantalla
 * principal, porque son listas de longitud variable y porque el numero de la
 * autorizacion tiene que existir antes de poder referenciarlo.</p>
 *
 * @author Grupo de Gestion de Datos e Informacion - UCSM
 */
public final class DialogoAutorizacion extends JDialog {

    private static final long serialVersionUID = 1L;

    private final JTextField txtNumero = new JTextField(12);
    private final JComboBox<ElementoCombo> cboEmpresa = new JComboBox<>();
    private final JTextField txtOrigen = new JTextField(34);
    private final JTextField txtDestino = new JTextField(34);
    private final JTextField txtFechaSalida = new JTextField(11);
    private final JTextField txtFechaLlegada = new JTextField(11);

    private final JTextField txtMercancia = new JTextField(34);
    private final JTextField txtLargoM = new JTextField(7);
    private final JTextField txtAnchoM = new JTextField(7);
    private final JTextField txtAltoM = new JTextField(7);
    private final JTextField txtPesoM = new JTextField(10);

    private final JComboBox<String> cboTipo =
            new JComboBox<>(new String[] {"Normal", "Especial"});
    private final JTextField txtExcAnt = new JTextField(7);
    private final JTextField txtExcPos = new JTextField(7);
    private final JTextField txtLargoT = new JTextField(7);
    private final JTextField txtAnchoT = new JTextField(7);
    private final JTextField txtAltoT = new JTextField(7);

    private final JSpinner spnApoyo = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));
    private final JCheckBox chkPolicial = new JCheckBox("Requiere escolta policial");
    private final JComboBox<ElementoCombo> cboBonificacion = new JComboBox<>();
    private final JTextField txtHorario = new JTextField("De 06:00 am a 06:00 pm", 24);
    private final JTextField txtVigDesde = new JTextField(11);
    private final JTextField txtVigHasta = new JTextField(11);

    private final String claveOriginal;
    private boolean guardado = false;

    /**
     * @param padre  ventana sobre la que se centra
     * @param numero numero a editar, o {@code null} para registrar uno nuevo
     */
    public DialogoAutorizacion(Window padre, String numero) {
        super(padre, numero == null ? "Registrar autorizacion"
                : "Modificar autorizacion", ModalityType.APPLICATION_MODAL);
        this.claveOriginal = numero;

        setLayout(new BorderLayout(0, 10));
        ((JPanel) getContentPane()).setBorder(
                BorderFactory.createEmptyBorder(14, 16, 12, 16));

        add(Estilo.titulo(numero == null
                ? "Nueva autorizacion de circulacion"
                : "Autorizacion " + numero), BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(armarFormulario());
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
        add(armarBotones(), BorderLayout.SOUTH);

        cargarCombos();

        if (numero == null) {
            sugerirValoresIniciales();
        } else {
            cargar(numero);
            txtNumero.setEnabled(false);
            cboEmpresa.setEnabled(false);
            txtNumero.setToolTipText(
                    "El numero identifica a la autorizacion y no puede cambiarse.");
        }

        pack();
        setSize(Math.min(getWidth() + 30, 880), Math.min(getHeight() + 20, 680));
        setLocationRelativeTo(padre);
        Dialogos.cerrarConEscape(this);
    }

    /** @return {@code true} si el usuario guardo los cambios */
    public boolean seGuardo() {
        return guardado;
    }

    /** Construye los campos, agrupados como en el formulario oficial. */
    private JPanel armarFormulario() {
        JPanel contenedor = new JPanel();
        contenedor.setLayout(new javax.swing.BoxLayout(
                contenedor, javax.swing.BoxLayout.Y_AXIS));
        contenedor.setOpaque(false);

        Formulario gen = new Formulario();
        gen.setBorder(Estilo.grupo("Datos generales"));
        gen.fila("Numero:", txtNumero, "Tipo de transporte:", cboTipo);
        gen.fila("Empresa solicitante:", cboEmpresa);
        gen.fila("Origen del traslado:", txtOrigen);
        gen.fila("Destino del traslado:", txtDestino);
        gen.fila("Fecha de salida:", txtFechaSalida, "Fecha de llegada:", txtFechaLlegada);
        gen.nota("Las fechas se escriben en formato AAAA-MM-DD, por ejemplo 2026-11-25.");

        Formulario merc = new Formulario();
        merc.setBorder(Estilo.grupo("Mercancia transportada"));
        merc.fila("Descripcion:", txtMercancia);
        merc.fila("Largo (m):", txtLargoM, "Ancho (m):", txtAnchoM);
        merc.fila("Alto (m):", txtAltoM, "Peso (kg):", txtPesoM);
        merc.nota("Si la unidad viaja vacia escriba Vacio en la descripcion y deje "
                + "las medidas y el peso en cero.");

        Formulario conj = new Formulario();
        conj.setBorder(Estilo.grupo("Dimensiones del conjunto ya cargado"));
        conj.fila("Exceso anterior (m):", txtExcAnt, "Exceso posterior (m):", txtExcPos);
        conj.fila("Largo total (m):", txtLargoT, "Ancho total (m):", txtAnchoT);
        conj.fila("Alto total (m):", txtAltoT, "Vehiculos de apoyo:", spnApoyo);

        Formulario vig = new Formulario();
        vig.setBorder(Estilo.grupo("Condiciones y vigencia"));
        vig.fila("Bonificacion:", cboBonificacion);
        vig.fila("Horario autorizado:", txtHorario);
        vig.fila("Vigente desde:", txtVigDesde, "Vigente hasta:", txtVigHasta);
        chkPolicial.setOpaque(false);
        chkPolicial.setFont(Estilo.NORMAL);
        vig.fila("", chkPolicial);
        vig.nota("Un transporte de tipo Especial exige al menos un vehiculo de apoyo. "
                + "El traslado debe quedar dentro del periodo de vigencia.");

        contenedor.add(gen);
        contenedor.add(javax.swing.Box.createVerticalStrut(8));
        contenedor.add(merc);
        contenedor.add(javax.swing.Box.createVerticalStrut(8));
        contenedor.add(conj);
        contenedor.add(javax.swing.Box.createVerticalStrut(8));
        contenedor.add(vig);
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

    /** Llena los desplegables de empresas y bonificaciones. */
    private void cargarCombos() {
        try {
            Resultado emp = Repositorio.llamar("SP_COMBO_EMPRESAS");
            for (int i = 0; i < emp.cantidadFilas(); i++) {
                cboEmpresa.addItem(new ElementoCombo(
                        emp.texto(i, 0), emp.texto(i, 1) + "  (" + emp.texto(i, 0) + ")"));
            }

            cboBonificacion.addItem(new ElementoCombo("", "Sin bonificacion"));
            Resultado bon = Repositorio.llamar("SP_COMBO_BONIFICACIONES");
            for (int i = 0; i < bon.cantidadFilas(); i++) {
                cboBonificacion.addItem(new ElementoCombo(bon.texto(i, 0),
                        bon.texto(i, 0) + "  (tope " + bon.texto(i, 1) + " kg)"));
            }
        } catch (Exception ex) {
            Dialogos.error(this, ex);
        }
    }

    /** Propone un numero correlativo y fechas razonables para un alta. */
    private void sugerirValoresIniciales() {
        try {
            Resultado r = Repositorio.llamar("SP_SIGUIENTE_NUMERO");
            if (!r.estaVacio()) {
                txtNumero.setText(r.texto(0, 0));
            }
        } catch (Exception ex) {
            // Si no se pudo sugerir, el usuario escribe el numero a mano.
        }
        LocalDate hoy = LocalDate.now();
        txtVigDesde.setText(hoy.toString());
        txtVigHasta.setText(hoy.plusDays(14).toString());
        txtFechaSalida.setText(hoy.toString());
        txtFechaLlegada.setText(hoy.plusDays(3).toString());
        txtLargoM.setText("0.00");
        txtAnchoM.setText("0.00");
        txtAltoM.setText("0.00");
        txtPesoM.setText("0");
        txtExcAnt.setText("0.00");
        txtExcPos.setText("0.00");
    }

    /**
     * Trae los datos actuales de la autorizacion que se va a modificar.
     *
     * @param numero clave del registro
     */
    private void cargar(String numero) {
        try {
            Resultado r = Repositorio.llamar("SP_AUTORIZACION_OBTENER", numero);
            if (r.estaVacio()) {
                return;
            }
            txtNumero.setText(r.texto(0, "Numero_autogenerado"));
            seleccionar(cboEmpresa, r.texto(0, "RUC_de_la_empresa"));
            txtOrigen.setText(r.texto(0, "Origen_traslado"));
            txtDestino.setText(r.texto(0, "Destino_traslado"));
            txtFechaSalida.setText(r.texto(0, "Fecha_salida"));
            txtFechaLlegada.setText(r.texto(0, "Fecha_llegada"));
            txtMercancia.setText(r.texto(0, "Descripcion_de_la_mercancia"));
            txtLargoM.setText(r.texto(0, "Largo_de_mercancia"));
            txtAnchoM.setText(r.texto(0, "Ancho_de_mercancia"));
            txtAltoM.setText(r.texto(0, "Alto_de_mercancia"));
            txtPesoM.setText(r.texto(0, "Peso_de_mercancia"));
            cboTipo.setSelectedItem(r.texto(0, "Tipo_de_transporte"));
            txtExcAnt.setText(r.texto(0, "Exceso_anterior"));
            txtExcPos.setText(r.texto(0, "Exceso_posterior"));
            txtLargoT.setText(r.texto(0, "Largo_total_vehiculo"));
            txtAnchoT.setText(r.texto(0, "Ancho_total_vehiculo"));
            txtAltoT.setText(r.texto(0, "Alto_total_vehiculo"));
            spnApoyo.setValue(entero(r.texto(0, "Numero_vehiculos_apoyo"), 0));
            String apoyo = r.texto(0, "Apoyo_policial");
            chkPolicial.setSelected("1".equals(apoyo) || "true".equalsIgnoreCase(apoyo));
            seleccionar(cboBonificacion, r.texto(0, "Placa_bonificacion"));
            txtHorario.setText(r.texto(0, "Horario_de_autorizacion_valida"));
            txtVigDesde.setText(r.texto(0, "Vigencia_desde"));
            txtVigHasta.setText(r.texto(0, "Vigencia_hasta"));

        } catch (Exception ex) {
            Dialogos.error(this, ex);
        }
    }

    /** Valida lo escrito y lo envia al procedimiento correspondiente. */
    private void guardar() {
        String numero = txtNumero.getText().trim().toUpperCase();
        if (!numero.matches("AS-\\d{7}")) {
            Dialogos.advertencia(this,
                    "El numero debe tener el formato AS- seguido de 7 digitos, "
                            + "por ejemplo AS-1115048.");
            txtNumero.requestFocus();
            return;
        }

        ElementoCombo emp = (ElementoCombo) cboEmpresa.getSelectedItem();
        if (emp == null) {
            Dialogos.advertencia(this, "Seleccione la empresa solicitante.");
            return;
        }

        String origen = txtOrigen.getText().trim();
        String destino = txtDestino.getText().trim();
        String mercancia = txtMercancia.getText().trim();
        if (origen.isEmpty() || destino.isEmpty() || mercancia.isEmpty()) {
            Dialogos.advertencia(this,
                    "El origen, el destino y la descripcion de la mercancia "
                            + "son obligatorios.");
            return;
        }

        Date fSalida = fecha(txtFechaSalida.getText());
        Date fLlegada = fecha(txtFechaLlegada.getText());
        Date vDesde = fecha(txtVigDesde.getText());
        Date vHasta = fecha(txtVigHasta.getText());
        if (fSalida == null || fLlegada == null || vDesde == null || vHasta == null) {
            Dialogos.advertencia(this,
                    "Revise las fechas. Deben escribirse como AAAA-MM-DD, "
                            + "por ejemplo 2026-11-25.");
            return;
        }
        if (fLlegada.before(fSalida)) {
            Dialogos.advertencia(this,
                    "La fecha de llegada no puede ser anterior a la de salida.");
            return;
        }
        if (vHasta.before(vDesde)) {
            Dialogos.advertencia(this,
                    "La vigencia final no puede ser anterior a la inicial.");
            return;
        }

        double largoT = decimal(txtLargoT.getText(), -1);
        double anchoT = decimal(txtAnchoT.getText(), -1);
        double altoT = decimal(txtAltoT.getText(), -1);
        if (largoT <= 0 || anchoT <= 0 || altoT <= 0) {
            Dialogos.advertencia(this,
                    "Las dimensiones totales del conjunto deben ser mayores que cero.");
            return;
        }

        String tipo = String.valueOf(cboTipo.getSelectedItem());
        int apoyo = (Integer) spnApoyo.getValue();
        if ("Especial".equals(tipo) && apoyo == 0) {
            Dialogos.advertencia(this,
                    "Un transporte de tipo Especial requiere al menos "
                            + "un vehiculo de apoyo.");
            return;
        }

        ElementoCombo bon = (ElementoCombo) cboBonificacion.getSelectedItem();
        String placaBon = bon == null ? "" : bon.valor();

        try {
            String mensaje;
            if (claveOriginal == null) {
                mensaje = Repositorio.ejecutar("SP_AUTORIZACION_INSERTAR",
                        numero, emp.valor(), origen, destino, fSalida, fLlegada,
                        mercancia, decimal(txtLargoM.getText(), 0),
                        decimal(txtAnchoM.getText(), 0), decimal(txtAltoM.getText(), 0),
                        entero(txtPesoM.getText(), 0), tipo,
                        decimal(txtExcAnt.getText(), 0), decimal(txtExcPos.getText(), 0),
                        largoT, anchoT, altoT, apoyo, chkPolicial.isSelected(),
                        placaBon, txtHorario.getText().trim(), vDesde, vHasta);
            } else {
                mensaje = Repositorio.ejecutar("SP_AUTORIZACION_MODIFICAR",
                        numero, origen, destino, fSalida, fLlegada,
                        mercancia, decimal(txtLargoM.getText(), 0),
                        decimal(txtAnchoM.getText(), 0), decimal(txtAltoM.getText(), 0),
                        entero(txtPesoM.getText(), 0), tipo,
                        decimal(txtExcAnt.getText(), 0), decimal(txtExcPos.getText(), 0),
                        largoT, anchoT, altoT, apoyo, chkPolicial.isSelected(),
                        placaBon, txtHorario.getText().trim(), vDesde, vHasta);
            }

            guardado = true;
            Dialogos.exito(this, mensaje);
            dispose();

        } catch (Exception ex) {
            Dialogos.error(this, ex);
        }
    }

    /**
     * Selecciona en un desplegable el elemento cuyo valor coincide.
     *
     * @param combo desplegable
     * @param valor valor buscado
     */
    private static void seleccionar(JComboBox<ElementoCombo> combo, String valor) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).valor().equals(valor == null ? "" : valor)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    /**
     * Convierte un texto AAAA-MM-DD en fecha para la base de datos.
     *
     * @param texto fecha escrita por el usuario
     * @return fecha convertida, o {@code null} si el formato es invalido
     */
    private static Date fecha(String texto) {
        try {
            return Date.valueOf(LocalDate.parse(texto.trim()));
        } catch (DateTimeParseException | IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Convierte un texto a numero decimal.
     *
     * @param texto      valor escrito
     * @param porDefecto valor si no es numerico
     * @return numero leido
     */
    private static double decimal(String texto, double porDefecto) {
        try {
            return Double.parseDouble(texto.trim().replace(',', '.'));
        } catch (NumberFormatException | NullPointerException ex) {
            return porDefecto;
        }
    }

    /**
     * Convierte un texto a numero entero.
     *
     * @param texto      valor escrito
     * @param porDefecto valor si no es numerico
     * @return numero leido
     */
    private static int entero(String texto, int porDefecto) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException | NullPointerException ex) {
            return porDefecto;
        }
    }

    /**
     * Elemento de un desplegable: guarda la clave que viaja a la base de datos
     * y muestra en pantalla un texto legible.
     */
    private static final class ElementoCombo {

        private final String valor;
        private final String etiqueta;

        ElementoCombo(String valor, String etiqueta) {
            this.valor = valor == null ? "" : valor;
            this.etiqueta = etiqueta;
        }

        String valor() {
            return valor;
        }

        @Override
        public String toString() {
            return etiqueta;
        }
    }
}
