package pe.edu.ucsm.gdi.permicarga.vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import pe.edu.ucsm.gdi.permicarga.datos.Repositorio;
import pe.edu.ucsm.gdi.permicarga.datos.Resultado;
import pe.edu.ucsm.gdi.permicarga.modelo.Sesion;
import pe.edu.ucsm.gdi.permicarga.util.Dialogos;

/**
 * Modulo central del aplicativo: las autorizaciones de circulacion.
 *
 * <p>La pantalla se divide en dos. Arriba estan las autorizaciones, que se
 * pueden filtrar por situacion o buscar por texto. Abajo, el detalle de la que
 * este seleccionada: su conjunto vehicular, sus tramos de ruta y su
 * distribucion de peso por eje.</p>
 *
 * <p>Esa division refleja el modelo relacional. La autorizacion es una fila de
 * una tabla, pero las unidades, las rutas y los ejes son tablas aparte porque
 * cada autorizacion tiene un numero variable de ellos; fue justamente la
 * primera forma normal la que separo el grupo repetitivo de los pesos por eje.
 * La pantalla deja ver esa estructura en lugar de esconderla.</p>
 *
 * @author Grupo de Gestion de Datos e Informacion - UCSM
 */
public final class PanelAutorizaciones extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final String[] SITUACIONES = {
        "TODAS", "VIGENTE", "POR VENCER", "PROGRAMADA", "VENCIDA", "ANULADA"
    };

    private final JTable tabla = new JTable();
    private final JTable tablaUnidades = new JTable();
    private final JTable tablaRutas = new JTable();
    private final JTable tablaEjes = new JTable();

    private final JComboBox<String> cboSituacion = new JComboBox<>(SITUACIONES);
    private final JTextField txtBuscar = new JTextField(22);
    private final JLabel lblRecuento = Estilo.ayuda(" ");
    private final JLabel lblDetalle = Estilo.ayuda("Seleccione una autorizacion.");

    private final JButton btnNueva = Estilo.botonPrincipal("Registrar");
    private final JButton btnEditar = Estilo.boton("Modificar");
    private final JButton btnAnular = Estilo.boton("Anular");
    private final JButton btnReactivar = Estilo.boton("Reactivar");

    private final JButton btnAgregarUnidad = Estilo.boton("Agregar unidad");
    private final JButton btnQuitarUnidad = Estilo.boton("Quitar unidad");
    private final JButton btnAgregarRuta = Estilo.boton("Agregar tramo");
    private final JButton btnQuitarRuta = Estilo.boton("Quitar tramo");

    /** Arma el modulo y carga el listado inicial. */
    public PanelAutorizaciones() {
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        setBackground(Estilo.FONDO);

        add(armarCabecera(), BorderLayout.NORTH);
        add(armarCentro(), BorderLayout.CENTER);
        add(armarPie(), BorderLayout.SOUTH);

        conectarAcciones();
        aplicarPerfil();
        recargar();
    }

    /** Construye la zona superior con el titulo y los filtros. */
    private JPanel armarCabecera() {
        JPanel norte = new JPanel(new BorderLayout(0, 8));
        norte.setOpaque(false);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        JLabel t = Estilo.titulo("Autorizaciones de circulacion");
        t.setAlignmentX(LEFT_ALIGNMENT);
        JLabel d = Estilo.ayuda("Permisos emitidos por Provias Nacional para "
                + "el traslado de carga sobredimensionada.");
        d.setAlignmentX(LEFT_ALIGNMENT);
        textos.add(t);
        textos.add(Box.createVerticalStrut(2));
        textos.add(d);
        norte.add(textos, BorderLayout.WEST);

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        filtros.setOpaque(false);
        filtros.add(new JLabel("Situacion:"));
        filtros.add(cboSituacion);
        filtros.add(new JLabel("Buscar:"));
        txtBuscar.setToolTipText("Numero, empresa, origen, destino o placa. Intro para buscar.");
        filtros.add(txtBuscar);
        JButton refrescar = Estilo.boton("Actualizar");
        refrescar.addActionListener(e -> recargar());
        filtros.add(refrescar);
        norte.add(filtros, BorderLayout.EAST);

        return norte;
    }

    /** Construye el area dividida entre el listado y el detalle. */
    private JSplitPane armarCentro() {
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane arriba = new JScrollPane(tabla);
        arriba.setBorder(BorderFactory.createLineBorder(new Color(214, 219, 225)));
        arriba.getViewport().setBackground(Color.WHITE);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.setFont(Estilo.NORMAL);
        pestanas.addTab("Conjunto vehicular", armarDetalleUnidades());
        pestanas.addTab("Tramos de ruta", armarDetalleRutas());
        pestanas.addTab("Peso por eje", armarDetalleEjes());

        JPanel abajo = new JPanel(new BorderLayout(0, 6));
        abajo.setOpaque(false);
        JPanel tituloDetalle = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tituloDetalle.setOpaque(false);
        tituloDetalle.add(lblDetalle);
        abajo.add(tituloDetalle, BorderLayout.NORTH);
        abajo.add(pestanas, BorderLayout.CENTER);

        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT, arriba, abajo);
        division.setResizeWeight(0.58);
        division.setBorder(null);
        division.setDividerSize(8);
        return division;
    }

    /** Pestana con las unidades declaradas. */
    private JPanel armarDetalleUnidades() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        JScrollPane sc = new JScrollPane(tablaUnidades);
        sc.getViewport().setBackground(Color.WHITE);
        p.add(sc, BorderLayout.CENTER);

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        barra.setOpaque(false);
        barra.add(btnAgregarUnidad);
        barra.add(btnQuitarUnidad);
        barra.add(Estilo.ayuda("Una autorizacion debe declarar al menos una unidad."));
        p.add(barra, BorderLayout.SOUTH);
        return p;
    }

    /** Pestana con los tramos de ruta. */
    private JPanel armarDetalleRutas() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        JScrollPane sc = new JScrollPane(tablaRutas);
        sc.getViewport().setBackground(Color.WHITE);
        p.add(sc, BorderLayout.CENTER);

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        barra.setOpaque(false);
        barra.add(btnAgregarRuta);
        barra.add(btnQuitarRuta);
        barra.add(Estilo.ayuda("Formato del tramo: Departamento-CodigoRuta, "
                + "por ejemplo Arequipa-PE-1S."));
        p.add(barra, BorderLayout.SOUTH);
        return p;
    }

    /** Pestana con la distribucion de peso por eje. */
    private JPanel armarDetalleEjes() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        JScrollPane sc = new JScrollPane(tablaEjes);
        sc.getViewport().setBackground(Color.WHITE);
        p.add(sc, BorderLayout.CENTER);

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        barra.setOpaque(false);
        barra.add(Estilo.ayuda("La distribucion por eje solo se declara cuando el "
                + "conjunto supera los 48000 kg."));
        p.add(barra, BorderLayout.SOUTH);
        return p;
    }

    /** Construye la barra inferior de botones. */
    private JPanel armarPie() {
        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        botones.add(btnNueva);
        botones.add(btnEditar);
        botones.add(btnAnular);
        botones.add(btnReactivar);
        pie.add(botones, BorderLayout.WEST);

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 6));
        derecha.setOpaque(false);
        derecha.add(lblRecuento);
        pie.add(derecha, BorderLayout.EAST);
        return pie;
    }

    /** Asocia cada control con su accion. */
    private void conectarAcciones() {
        cboSituacion.addActionListener(e -> recargar());
        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    recargar();
                }
            }
        });

        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarDetalle();
            }
        });

        btnNueva.addActionListener(e -> {
            DialogoAutorizacion d = new DialogoAutorizacion(
                    SwingUtilities.getWindowAncestor(this), null);
            d.setVisible(true);
            if (d.seGuardo()) {
                recargar();
            }
        });

        btnEditar.addActionListener(e -> {
            String n = seleccionada();
            if (n != null) {
                DialogoAutorizacion d = new DialogoAutorizacion(
                        SwingUtilities.getWindowAncestor(this), n);
                d.setVisible(true);
                if (d.seGuardo()) {
                    recargar();
                }
            }
        });

        btnAnular.addActionListener(e -> anular());
        btnReactivar.addActionListener(e -> reactivar());
        btnAgregarUnidad.addActionListener(e -> agregarUnidad());
        btnQuitarUnidad.addActionListener(e -> quitarUnidad());
        btnAgregarRuta.addActionListener(e -> agregarRuta());
        btnQuitarRuta.addActionListener(e -> quitarRuta());
    }

    /** Habilita o deshabilita botones segun el perfil de la sesion. */
    private void aplicarPerfil() {
        Sesion s = Sesion.actual();
        boolean edita = s != null && s.puedeEditar();
        boolean baja = s != null && s.puedeDarDeBaja();

        btnNueva.setEnabled(edita);
        btnEditar.setEnabled(edita);
        btnAgregarUnidad.setEnabled(edita);
        btnAgregarRuta.setEnabled(edita);
        btnQuitarUnidad.setEnabled(edita);
        btnQuitarRuta.setEnabled(edita);
        btnAnular.setEnabled(baja);
        btnReactivar.setEnabled(baja);
    }

    /** Vuelve a pedir el listado a la base y repinta la rejilla. */
    public void recargar() {
        try {
            String filtro = txtBuscar.getText().trim();
            Resultado r = filtro.isEmpty()
                    ? Repositorio.llamar("SP_AUTORIZACION_LISTAR",
                            String.valueOf(cboSituacion.getSelectedItem()))
                    : Repositorio.llamar("SP_AUTORIZACION_BUSCAR", filtro);

            tabla.setModel(new DefaultTableModel(r.matriz(), r.titulos()) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isCellEditable(int fila, int columna) {
                    return false;
                }
            });
            Estilo.formatearTabla(tabla);
            lblRecuento.setText(r.cantidadFilas() == 1
                    ? "1 autorizacion" : r.cantidadFilas() + " autorizaciones");
            limpiarDetalle();

        } catch (Exception ex) {
            Dialogos.error(this, ex);
        }
    }

    /** Carga las tres tablas de detalle para la autorizacion seleccionada. */
    private void cargarDetalle() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            limpiarDetalle();
            return;
        }
        String numero = String.valueOf(tabla.getValueAt(fila, 0));
        lblDetalle.setText("Detalle de la autorizacion " + numero);

        try {
            poner(tablaUnidades,
                    Repositorio.llamar("SP_AUTORIZACION_VEHICULOS_LISTAR", numero));
            poner(tablaRutas,
                    Repositorio.llamar("SP_AUTORIZACION_RUTAS_LISTAR", numero));
            poner(tablaEjes,
                    Repositorio.llamar("SP_AUTORIZACION_EJES_LISTAR", numero));
        } catch (Exception ex) {
            Dialogos.error(this, ex);
        }
    }

    /** Vacia las tablas de detalle. */
    private void limpiarDetalle() {
        lblDetalle.setText("Seleccione una autorizacion para ver su detalle.");
        for (JTable t : new JTable[] {tablaUnidades, tablaRutas, tablaEjes}) {
            t.setModel(new DefaultTableModel());
        }
    }

    /**
     * Vuelca un resultado en una tabla de detalle.
     *
     * @param destino tabla a llenar
     * @param r       datos a mostrar
     */
    private void poner(JTable destino, Resultado r) {
        destino.setModel(new DefaultTableModel(r.matriz(), r.titulos()) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        });
        Estilo.formatearTabla(destino);
    }

    /**
     * Devuelve el numero de la autorizacion seleccionada.
     *
     * @return numero, o {@code null} si no hay seleccion
     */
    private String seleccionada() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            Dialogos.advertencia(this, "Seleccione primero una autorizacion de la lista.");
            return null;
        }
        return String.valueOf(tabla.getValueAt(fila, 0));
    }

    /** Anula la autorizacion seleccionada, pidiendo el motivo. */
    private void anular() {
        String numero = seleccionada();
        if (numero == null) {
            return;
        }
        String motivo = Dialogos.pedirTexto(this,
                "Indique el motivo por el que se anula la autorizacion " + numero + ":");
        if (motivo == null) {
            return;
        }
        if (!Dialogos.confirmar(this,
                "Se anulara la autorizacion " + numero + ".\n\n"
                        + "El permiso dejara de figurar como vigente, pero su "
                        + "historial se conserva y podra consultarse.\n\n"
                        + "Desea continuar?")) {
            return;
        }
        try {
            Dialogos.exito(this,
                    Repositorio.ejecutar("SP_AUTORIZACION_BAJA", numero, motivo));
            recargar();
        } catch (Exception ex) {
            Dialogos.error(this, ex);
        }
    }

    /** Reactiva la autorizacion seleccionada. */
    private void reactivar() {
        String numero = seleccionada();
        if (numero == null) {
            return;
        }
        try {
            Dialogos.exito(this,
                    Repositorio.ejecutar("SP_AUTORIZACION_REACTIVAR", numero));
            recargar();
        } catch (Exception ex) {
            Dialogos.error(this, ex);
        }
    }

    /** Agrega una unidad al conjunto declarado. */
    private void agregarUnidad() {
        String numero = seleccionada();
        if (numero == null) {
            return;
        }
        try {
            Resultado flota = Repositorio.llamar("SP_COMBO_VEHICULOS");
            String[] opciones = new String[flota.cantidadFilas()];
            for (int i = 0; i < flota.cantidadFilas(); i++) {
                opciones[i] = flota.texto(i, 0) + "  -  " + flota.texto(i, 1)
                        + "  (" + flota.texto(i, 2) + " kg)";
            }
            Object elegida = javax.swing.JOptionPane.showInputDialog(this,
                    "Unidad a declarar en la autorizacion " + numero + ":",
                    "Agregar unidad al conjunto",
                    javax.swing.JOptionPane.QUESTION_MESSAGE, null, opciones,
                    opciones.length > 0 ? opciones[0] : null);
            if (elegida == null) {
                return;
            }
            String placa = String.valueOf(elegida).split("\\s+-\\s+")[0].trim();
            Dialogos.exito(this, Repositorio.ejecutar(
                    "SP_AUTORIZACION_VEHICULO_AGREGAR", numero, placa));
            cargarDetalle();

        } catch (Exception ex) {
            Dialogos.error(this, ex);
        }
    }

    /** Quita una unidad del conjunto declarado. */
    private void quitarUnidad() {
        String numero = seleccionada();
        if (numero == null) {
            return;
        }
        int fila = tablaUnidades.getSelectedRow();
        if (fila < 0) {
            Dialogos.advertencia(this,
                    "Seleccione la unidad que desea quitar del conjunto.");
            return;
        }
        String placa = String.valueOf(tablaUnidades.getValueAt(fila, 0));
        if (!Dialogos.confirmar(this,
                "Quitar la unidad " + placa + " del conjunto de " + numero + "?")) {
            return;
        }
        try {
            Dialogos.exito(this, Repositorio.ejecutar(
                    "SP_AUTORIZACION_VEHICULO_QUITAR", numero, placa));
            cargarDetalle();
        } catch (Exception ex) {
            Dialogos.error(this, ex);
        }
    }

    /** Agrega un tramo de ruta. */
    private void agregarRuta() {
        String numero = seleccionada();
        if (numero == null) {
            return;
        }
        String ruta = Dialogos.pedirTexto(this,
                "Tramo a declarar, con formato Departamento-CodigoRuta\n"
                        + "(por ejemplo Arequipa-PE-1S):");
        if (ruta == null || ruta.trim().isEmpty()) {
            return;
        }
        try {
            Dialogos.exito(this, Repositorio.ejecutar(
                    "SP_AUTORIZACION_RUTA_AGREGAR", numero, ruta.trim()));
            cargarDetalle();
        } catch (Exception ex) {
            Dialogos.error(this, ex);
        }
    }

    /** Quita un tramo de ruta. */
    private void quitarRuta() {
        String numero = seleccionada();
        if (numero == null) {
            return;
        }
        int fila = tablaRutas.getSelectedRow();
        if (fila < 0) {
            Dialogos.advertencia(this, "Seleccione el tramo que desea quitar.");
            return;
        }
        String ruta = String.valueOf(tablaRutas.getValueAt(fila, 0));
        if (!Dialogos.confirmar(this, "Quitar el tramo " + ruta + " de " + numero + "?")) {
            return;
        }
        try {
            Dialogos.exito(this, Repositorio.ejecutar(
                    "SP_AUTORIZACION_RUTA_QUITAR", numero, ruta));
            cargarDetalle();
        } catch (Exception ex) {
            Dialogos.error(this, ex);
        }
    }
}
