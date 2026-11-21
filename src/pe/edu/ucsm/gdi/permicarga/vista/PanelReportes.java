package pe.edu.ucsm.gdi.permicarga.vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import pe.edu.ucsm.gdi.permicarga.datos.Repositorio;
import pe.edu.ucsm.gdi.permicarga.datos.Resultado;
import pe.edu.ucsm.gdi.permicarga.util.Dialogos;

/**
 * Modulo de reportes.
 *
 * <p>Ofrece los quince reportes que resuelve la base de datos. El catalogo de
 * reportes es una tabla de este archivo, no una pantalla por reporte: cada
 * entrada declara el procedimiento a invocar, que parametro pedir y para que
 * sirve el reporte. Anadir uno nuevo es escribir el procedimiento en la base de
 * datos y agregar una linea aqui.</p>
 *
 * <p>El resultado puede exportarse a un archivo CSV, que es el formato que la
 * empresa necesita para seguir trabajando los datos en una hoja de calculo.</p>
 *
 * @author Grupo de Gestion de Datos e Informacion - UCSM
 */
public final class PanelReportes extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Catalogo de reportes disponibles. */
    private static final Reporte[] CATALOGO = {
        new Reporte("01. Autorizaciones vigentes",
                "SP_REP_01_VIGENTES", Parametro.NINGUNO, null,
                "Permisos con los que la empresa puede circular hoy."),

        new Reporte("02. Autorizaciones proximas a vencer",
                "SP_REP_02_POR_VENCER", Parametro.NUMERO, "7",
                "Permisos que vencen dentro del numero de dias indicado."),

        new Reporte("03. Conjunto vehicular declarado",
                "SP_REP_03_CONJUNTO_VEHICULAR", Parametro.NINGUNO, null,
                "Unidades que amparan cada permiso y peso total del conjunto."),

        new Reporte("04. Conjuntos sobre 48 toneladas",
                "SP_REP_04_SOBRE_48_TONELADAS", Parametro.NINGUNO, null,
                "Traslados que obligan a declarar ejes y tramitar bonificacion."),

        new Reporte("05. Rutas mas transitadas",
                "SP_REP_05_RUTAS_TRANSITADAS", Parametro.NINGUNO, null,
                "Corredores donde se concentra la operacion."),

        new Reporte("06. Flota sin utilizar",
                "SP_REP_06_FLOTA_SIN_USO", Parametro.NINGUNO, null,
                "Unidades registradas que nunca se declararon en un permiso."),

        new Reporte("07. Resumen por empresa",
                "SP_REP_07_RESUMEN_POR_EMPRESA", Parametro.NINGUNO, null,
                "Actividad de cada transportista registrado."),

        new Reporte("08. Distribucion de peso por eje",
                "SP_REP_08_PESO_POR_EJE", Parametro.TEXTO, "",
                "Peso declarado por eje. Deje vacio para ver todas las autorizaciones."),

        new Reporte("09. Traslados con apoyo policial",
                "SP_REP_09_APOYO_POLICIAL", Parametro.NINGUNO, null,
                "Permisos que exigen coordinar escolta con la autoridad."),

        new Reporte("10. Historial de una unidad",
                "SP_REP_10_HISTORIAL_UNIDAD", Parametro.TEXTO, "",
                "Traslados de una placa. Deje vacio para ver toda la flota."),

        new Reporte("11. Aprovechamiento de bonificaciones",
                "SP_REP_11_BONIFICACIONES", Parametro.NINGUNO, null,
                "Bonificaciones tramitadas y cuantas veces se usaron."),

        new Reporte("12. Carga de trabajo mensual",
                "SP_REP_12_CARGA_MENSUAL", Parametro.NINGUNO, null,
                "Traslados y tonelaje movido por mes."),

        new Reporte("13. Destinos mas frecuentes",
                "SP_REP_13_DESTINOS_FRECUENTES", Parametro.NINGUNO, null,
                "Destinos habituales, para planificar retornos con carga."),

        new Reporte("14. Bitacora de auditoria",
                "SP_REP_14_AUDITORIA", Parametro.TABLA_Y_FECHA, "",
                "Operaciones registradas automaticamente por los triggers."),

        new Reporte("15. Unidades de baja y su historial",
                "SP_REP_15_BAJAS_CON_HISTORIAL", Parametro.NINGUNO, null,
                "Comprueba que la baja logica conserva la historia de la unidad."),
    };

    private final JComboBox<Reporte> cboReporte = new JComboBox<>(CATALOGO);
    private final JLabel lblParametro = new JLabel("Parametro:");
    private final JTextField txtParametro = new JTextField(14);
    private final JTextField txtDesde = new JTextField(10);
    private final JLabel lblDesde = new JLabel("Desde:");
    private final JLabel lblDescripcion = Estilo.ayuda(" ");
    private final JLabel lblRecuento = Estilo.ayuda(" ");
    private final JTable tabla = new JTable();
    private final JButton btnExportar = Estilo.boton("Exportar a CSV");

    /** Arma el modulo y ejecuta el primer reporte. */
    public PanelReportes() {
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        setBackground(Estilo.FONDO);

        add(armarCabecera(), BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(214, 219, 225)));
        scroll.getViewport().setBackground(Color.WHITE);
        add(scroll, BorderLayout.CENTER);

        add(armarPie(), BorderLayout.SOUTH);

        cboReporte.addActionListener(e -> alCambiarReporte());
        btnExportar.addActionListener(e -> exportar());

        alCambiarReporte();
        ejecutar();
    }

    /** Construye la zona superior con el selector de reporte. */
    private JPanel armarCabecera() {
        JPanel norte = new JPanel();
        norte.setLayout(new BoxLayout(norte, BoxLayout.Y_AXIS));
        norte.setOpaque(false);

        JPanel titulo = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titulo.setOpaque(false);
        titulo.add(Estilo.titulo("Reportes"));
        norte.add(titulo);
        norte.add(Box.createVerticalStrut(2));

        JPanel ayuda = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        ayuda.setOpaque(false);
        ayuda.add(Estilo.ayuda("Quince consultas sobre la base de datos. "
                + "Cada una se resuelve con un procedimiento almacenado."));
        norte.add(ayuda);
        norte.add(Box.createVerticalStrut(8));

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        barra.setOpaque(false);
        barra.add(new JLabel("Reporte:"));
        cboReporte.setFont(Estilo.NORMAL);
        barra.add(cboReporte);
        barra.add(lblParametro);
        barra.add(txtParametro);
        barra.add(lblDesde);
        barra.add(txtDesde);
        JButton ejecutar = Estilo.botonPrincipal("Ejecutar");
        ejecutar.addActionListener(e -> ejecutar());
        barra.add(ejecutar);
        norte.add(barra);
        norte.add(Box.createVerticalStrut(4));

        JPanel desc = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        desc.setOpaque(false);
        desc.add(lblDescripcion);
        norte.add(desc);

        return norte;
    }

    /** Construye la barra inferior. */
    private JPanel armarPie() {
        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);

        JPanel izq = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        izq.setOpaque(false);
        izq.add(btnExportar);
        pie.add(izq, BorderLayout.WEST);

        JPanel der = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 6));
        der.setOpaque(false);
        der.add(lblRecuento);
        pie.add(der, BorderLayout.EAST);
        return pie;
    }

    /** Muestra u oculta los campos de parametro segun el reporte elegido. */
    private void alCambiarReporte() {
        Reporte r = (Reporte) cboReporte.getSelectedItem();
        if (r == null) {
            return;
        }
        lblDescripcion.setText(r.descripcion);

        boolean pideParametro = r.parametro != Parametro.NINGUNO;
        boolean pideFecha = r.parametro == Parametro.TABLA_Y_FECHA;

        lblParametro.setVisible(pideParametro);
        txtParametro.setVisible(pideParametro);
        lblDesde.setVisible(pideFecha);
        txtDesde.setVisible(pideFecha);

        switch (r.parametro) {
            case NUMERO:
                lblParametro.setText("Dias:");
                break;
            case TEXTO:
                lblParametro.setText(r.procedimiento.contains("HISTORIAL")
                        ? "Placa:" : "Autorizacion:");
                break;
            case TABLA_Y_FECHA:
                lblParametro.setText("Tabla:");
                txtDesde.setText(LocalDate.now().minusDays(30).toString());
                break;
            default:
                break;
        }
        if (r.valorInicial != null) {
            txtParametro.setText(r.valorInicial);
        }
        revalidate();
        repaint();
    }

    /** Invoca el procedimiento del reporte elegido y muestra el resultado. */
    private void ejecutar() {
        Reporte r = (Reporte) cboReporte.getSelectedItem();
        if (r == null) {
            return;
        }
        try {
            Resultado res;
            switch (r.parametro) {
                case NUMERO:
                    int dias;
                    try {
                        dias = Integer.parseInt(txtParametro.getText().trim());
                    } catch (NumberFormatException ex) {
                        Dialogos.advertencia(this,
                                "Indique un numero entero de dias, por ejemplo 7.");
                        return;
                    }
                    res = Repositorio.llamar(r.procedimiento, dias);
                    break;

                case TEXTO:
                    res = Repositorio.llamar(r.procedimiento,
                            txtParametro.getText().trim());
                    break;

                case TABLA_Y_FECHA:
                    String desde = txtDesde.getText().trim();
                    java.sql.Date fecha = null;
                    if (!desde.isEmpty()) {
                        try {
                            fecha = java.sql.Date.valueOf(LocalDate.parse(desde));
                        } catch (Exception ex) {
                            Dialogos.advertencia(this,
                                    "La fecha debe escribirse como AAAA-MM-DD.");
                            return;
                        }
                    }
                    res = Repositorio.llamar(r.procedimiento,
                            txtParametro.getText().trim(), fecha);
                    break;

                default:
                    res = Repositorio.llamar(r.procedimiento);
                    break;
            }

            tabla.setModel(new DefaultTableModel(res.matriz(), res.titulos()) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isCellEditable(int fila, int columna) {
                    return false;
                }
            });
            Estilo.formatearTabla(tabla);

            lblRecuento.setText(res.estaVacio()
                    ? "El reporte no devolvio resultados"
                    : res.cantidadFilas() + " fila(s)");
            btnExportar.setEnabled(!res.estaVacio());

        } catch (Exception ex) {
            Dialogos.error(this, ex);
        }
    }

    /** Guarda el contenido de la rejilla en un archivo CSV. */
    private void exportar() {
        if (tabla.getRowCount() == 0) {
            Dialogos.advertencia(this, "Ejecute primero un reporte con resultados.");
            return;
        }
        Reporte r = (Reporte) cboReporte.getSelectedItem();
        String sugerido = (r == null ? "reporte" : r.procedimiento.toLowerCase())
                + "_" + LocalDate.now() + ".csv";

        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Guardar el reporte");
        selector.setSelectedFile(new File(sugerido));
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File destino = selector.getSelectedFile();
        // Se escribe en UTF-8 con marca de orden de bytes, porque es lo que
        // necesita Excel en Windows para mostrar bien las tildes.
        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(destino), StandardCharsets.UTF_8))) {

            out.write('﻿');
            for (int c = 0; c < tabla.getColumnCount(); c++) {
                out.write(c == 0 ? "" : ";");
                out.write(celdaCsv(tabla.getColumnName(c)));
            }
            out.newLine();

            for (int f = 0; f < tabla.getRowCount(); f++) {
                for (int c = 0; c < tabla.getColumnCount(); c++) {
                    out.write(c == 0 ? "" : ";");
                    Object v = tabla.getValueAt(f, c);
                    out.write(celdaCsv(v == null ? "" : String.valueOf(v)));
                }
                out.newLine();
            }

            Dialogos.exito(this, "El reporte se guardo en:\n" + destino.getAbsolutePath());

        } catch (Exception ex) {
            Dialogos.error(this, ex);
        }
    }

    /**
     * Prepara un valor para el archivo CSV, entrecomillandolo si hace falta.
     *
     * @param valor contenido de la celda
     * @return celda lista para escribir
     */
    private static String celdaCsv(String valor) {
        String v = valor.replace("\"", "\"\"");
        return v.contains(";") || v.contains("\"") || v.contains("\n")
                ? "\"" + v + "\"" : v;
    }

    /** Clase de parametro que pide cada reporte. */
    private enum Parametro {
        /** El reporte no recibe parametros. */
        NINGUNO,
        /** Recibe un numero entero, como una cantidad de dias. */
        NUMERO,
        /** Recibe un texto, como una placa o un numero de autorizacion. */
        TEXTO,
        /** Recibe el nombre de una tabla y una fecha de inicio. */
        TABLA_Y_FECHA
    }

    /** Entrada del catalogo de reportes. */
    private static final class Reporte {

        private final String titulo;
        private final String procedimiento;
        private final Parametro parametro;
        private final String valorInicial;
        private final String descripcion;

        Reporte(String titulo, String procedimiento, Parametro parametro,
                String valorInicial, String descripcion) {
            this.titulo = titulo;
            this.procedimiento = procedimiento;
            this.parametro = parametro;
            this.valorInicial = valorInicial;
            this.descripcion = descripcion;
        }

        @Override
        public String toString() {
            return titulo;
        }
    }
}
