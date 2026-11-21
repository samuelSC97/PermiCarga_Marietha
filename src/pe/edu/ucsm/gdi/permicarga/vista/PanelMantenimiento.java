package pe.edu.ucsm.gdi.permicarga.vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import pe.edu.ucsm.gdi.permicarga.datos.Repositorio;
import pe.edu.ucsm.gdi.permicarga.datos.Resultado;
import pe.edu.ucsm.gdi.permicarga.modelo.Sesion;
import pe.edu.ucsm.gdi.permicarga.util.Dialogos;

/**
 * Estructura comun de los modulos de mantenimiento.
 *
 * <p>Los modulos de empresas y de vehiculos hacen lo mismo con datos distintos:
 * muestran una rejilla, permiten buscar, registrar, modificar y dar de baja. En
 * lugar de repetir ese armazon en cada uno, se escribe aqui una sola vez y cada
 * modulo concreto aporta unicamente lo que le es propio: que procedimientos
 * invocar y que formulario abrir.</p>
 *
 * <p>Los botones se habilitan segun el perfil de la sesion: un usuario de
 * consulta ve la informacion pero no puede modificarla, y solo el administrador
 * puede dar de baja.</p>
 *
 * @author Grupo de Gestion de Datos e Informacion - UCSM
 */
public abstract class PanelMantenimiento extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Rejilla donde se listan los registros. */
    protected final JTable tabla = new JTable();

    /** Caja de texto para la busqueda. */
    protected final JTextField txtBuscar = new JTextField(22);

    /** Casilla que incorpora al listado los registros dados de baja. */
    protected final JCheckBox chkInactivos = new JCheckBox("Mostrar dados de baja");

    /** Etiqueta con el recuento de registros mostrados. */
    protected final JLabel lblRecuento = Estilo.ayuda(" ");

    private final JButton btnNuevo = Estilo.botonPrincipal("Registrar");
    private final JButton btnEditar = Estilo.boton("Modificar");
    private final JButton btnBaja = Estilo.boton("Dar de baja");
    private final JButton btnReactivar = Estilo.boton("Reactivar");
    private final JButton btnRefrescar = Estilo.boton("Actualizar");

    /**
     * Arma la pantalla.
     *
     * @param titulo      nombre del modulo
     * @param descripcion linea explicativa bajo el titulo
     */
    protected PanelMantenimiento(String titulo, String descripcion) {
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        setBackground(Estilo.FONDO);

        add(armarCabecera(titulo, descripcion), BorderLayout.NORTH);
        add(armarTabla(), BorderLayout.CENTER);
        add(armarPie(), BorderLayout.SOUTH);

        conectarAcciones();
        aplicarPerfil();
    }

    /** @return nombre del procedimiento que lista los registros */
    protected abstract String procedimientoListar();

    /** @return nombre del procedimiento que busca por texto */
    protected abstract String procedimientoBuscar();

    /** @return titulo de la columna que contiene la clave primaria */
    protected abstract String columnaClave();

    /** Abre el formulario de alta. */
    protected abstract void alRegistrar();

    /**
     * Abre el formulario de modificacion.
     *
     * @param clave clave del registro seleccionado
     */
    protected abstract void alModificar(String clave);

    /** @return nombre del procedimiento que da de baja */
    protected abstract String procedimientoBaja();

    /** @return nombre del procedimiento que reactiva */
    protected abstract String procedimientoReactivar();

    /**
     * @return pregunta de confirmacion antes de dar de baja
     * @param clave clave del registro seleccionado
     */
    protected abstract String preguntaBaja(String clave);

    /** Construye la zona superior con el titulo y la busqueda. */
    private JPanel armarCabecera(String titulo, String descripcion) {
        JPanel norte = new JPanel(new BorderLayout(0, 8));
        norte.setOpaque(false);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new javax.swing.BoxLayout(textos, javax.swing.BoxLayout.Y_AXIS));
        JLabel t = Estilo.titulo(titulo);
        t.setAlignmentX(LEFT_ALIGNMENT);
        JLabel d = Estilo.ayuda(descripcion);
        d.setAlignmentX(LEFT_ALIGNMENT);
        textos.add(t);
        textos.add(Box.createVerticalStrut(2));
        textos.add(d);
        norte.add(textos, BorderLayout.WEST);

        JPanel busqueda = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        busqueda.setOpaque(false);
        busqueda.add(new JLabel("Buscar:"));
        txtBuscar.setToolTipText("Escriba y pulse Intro. Deje vacio para ver todos.");
        busqueda.add(txtBuscar);
        chkInactivos.setOpaque(false);
        chkInactivos.setFont(Estilo.NORMAL);
        busqueda.add(chkInactivos);
        busqueda.add(btnRefrescar);
        norte.add(busqueda, BorderLayout.EAST);

        return norte;
    }

    /** Construye la rejilla de resultados. */
    private JScrollPane armarTabla() {
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(214, 219, 225)));
        scroll.getViewport().setBackground(Color.WHITE);
        return scroll;
    }

    /** Construye la barra inferior de botones. */
    private JPanel armarPie() {
        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        botones.add(btnNuevo);
        botones.add(btnEditar);
        botones.add(btnBaja);
        botones.add(btnReactivar);
        pie.add(botones, BorderLayout.WEST);

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 6));
        derecha.setOpaque(false);
        derecha.add(lblRecuento);
        pie.add(derecha, BorderLayout.EAST);

        return pie;
    }

    /** Asocia cada boton con su accion. */
    private void conectarAcciones() {
        btnRefrescar.addActionListener(e -> recargar());
        chkInactivos.addActionListener(e -> recargar());

        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    recargar();
                }
            }
        });

        btnNuevo.addActionListener(e -> alRegistrar());

        btnEditar.addActionListener(e -> {
            String clave = claveSeleccionada();
            if (clave != null) {
                alModificar(clave);
            }
        });

        btnBaja.addActionListener(e -> darDeBaja());
        btnReactivar.addActionListener(e -> reactivar());

        // Doble clic sobre una fila abre la modificacion, que es lo que la
        // mayoria de usuarios intenta de forma espontanea.
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && Sesion.actual() != null
                        && Sesion.actual().puedeEditar()) {
                    String clave = claveSeleccionada();
                    if (clave != null) {
                        alModificar(clave);
                    }
                }
            }
        });
    }

    /** Habilita o deshabilita botones segun el perfil de la sesion. */
    private void aplicarPerfil() {
        Sesion s = Sesion.actual();
        boolean edita = s != null && s.puedeEditar();
        boolean baja = s != null && s.puedeDarDeBaja();

        btnNuevo.setEnabled(edita);
        btnEditar.setEnabled(edita);
        btnBaja.setEnabled(baja);
        btnReactivar.setEnabled(baja);

        if (!edita) {
            btnNuevo.setToolTipText("Su perfil solo permite consultar informacion.");
            btnEditar.setToolTipText("Su perfil solo permite consultar informacion.");
        }
        if (!baja) {
            btnBaja.setToolTipText("Solo el perfil ADMINISTRADOR puede dar de baja.");
            btnReactivar.setToolTipText("Solo el perfil ADMINISTRADOR puede reactivar.");
        }
    }

    /**
     * Vuelve a pedir los datos a la base y repinta la rejilla.
     */
    public void recargar() {
        try {
            String filtro = txtBuscar.getText().trim();
            Resultado r = filtro.isEmpty()
                    ? Repositorio.llamar(procedimientoListar(), chkInactivos.isSelected())
                    : Repositorio.llamar(procedimientoBuscar(), filtro);

            tabla.setModel(new DefaultTableModel(r.matriz(), r.titulos()) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isCellEditable(int fila, int columna) {
                    // La rejilla es de solo lectura: todo cambio pasa por un
                    // formulario y, desde el, por un procedimiento almacenado.
                    return false;
                }
            });
            Estilo.formatearTabla(tabla);

            lblRecuento.setText(r.cantidadFilas() == 1
                    ? "1 registro" : r.cantidadFilas() + " registros");

        } catch (Exception ex) {
            Dialogos.error(this, ex);
        }
    }

    /**
     * Devuelve la clave del registro seleccionado, avisando si no hay ninguno.
     *
     * @return clave seleccionada, o {@code null} si no hay seleccion
     */
    protected String claveSeleccionada() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            Dialogos.advertencia(this, "Seleccione primero un registro de la lista.");
            return null;
        }
        int col = columnaIndice(columnaClave());
        if (col < 0) {
            return null;
        }
        Object v = tabla.getValueAt(fila, col);
        return v == null ? null : String.valueOf(v);
    }

    /**
     * Busca el indice de una columna por su titulo.
     *
     * @param titulo titulo buscado
     * @return indice, o -1 si no existe
     */
    protected int columnaIndice(String titulo) {
        for (int i = 0; i < tabla.getColumnCount(); i++) {
            if (tabla.getColumnName(i).equalsIgnoreCase(titulo)) {
                return i;
            }
        }
        return -1;
    }

    /** Da de baja el registro seleccionado, previa confirmacion. */
    private void darDeBaja() {
        String clave = claveSeleccionada();
        if (clave == null) {
            return;
        }
        if (!Dialogos.confirmar(this, preguntaBaja(clave))) {
            return;
        }
        try {
            String mensaje = Repositorio.ejecutar(procedimientoBaja(), clave);
            Dialogos.exito(this, mensaje);
            recargar();
        } catch (Exception ex) {
            Dialogos.error(this, ex);
        }
    }

    /** Reactiva el registro seleccionado. */
    private void reactivar() {
        String clave = claveSeleccionada();
        if (clave == null) {
            return;
        }
        try {
            String mensaje = Repositorio.ejecutar(procedimientoReactivar(), clave);
            Dialogos.exito(this, mensaje);
            recargar();
        } catch (Exception ex) {
            Dialogos.error(this, ex);
        }
    }
}
