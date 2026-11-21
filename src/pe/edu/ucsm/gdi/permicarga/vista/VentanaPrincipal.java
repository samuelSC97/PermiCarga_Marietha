package pe.edu.ucsm.gdi.permicarga.vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import pe.edu.ucsm.gdi.permicarga.config.Configuracion;
import pe.edu.ucsm.gdi.permicarga.datos.Conexion;
import pe.edu.ucsm.gdi.permicarga.modelo.Sesion;

/**
 * Ventana principal del aplicativo.
 *
 * <p>Organiza los modulos en pestanas y mantiene una barra de estado con los
 * datos de la sesion y del servidor. Se eligio una ventana unica con pestanas,
 * en lugar de ventanas independientes, porque el trabajo del administrador
 * consiste en pasar continuamente de una autorizacion a la flota y de alli a un
 * reporte: con pestanas ese recorrido es un clic y no implica buscar ventanas.</p>
 *
 * @author Grupo de Gestion de Datos e Informacion - UCSM
 */
public final class VentanaPrincipal extends JFrame {

    private static final long serialVersionUID = 1L;

    private final JTabbedPane pestanas = new JTabbedPane();
    private final PanelInicio panelInicio = new PanelInicio();
    private final PanelEmpresas panelEmpresas = new PanelEmpresas();
    private final PanelVehiculos panelVehiculos = new PanelVehiculos();
    private final PanelAutorizaciones panelAutorizaciones = new PanelAutorizaciones();
    private final PanelReportes panelReportes = new PanelReportes();

    /** Arma la ventana principal con todos sus modulos. */
    public VentanaPrincipal() {
        super("PermiCarga Marietha - Gestion de permisos de carga sobredimensionada");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setIconImage(Estilo.icono(64).getImage());

        setJMenuBar(armarMenu());
        setLayout(new BorderLayout());
        add(armarEncabezado(), BorderLayout.NORTH);
        add(armarPestanas(), BorderLayout.CENTER);
        add(armarBarraEstado(), BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                salir();
            }
        });

        setSize(1180, 720);
        setMinimumSize(new Dimension(980, 620));
        setLocationRelativeTo(null);
    }

    /** Construye la barra de menus. */
    private JMenuBar armarMenu() {
        JMenuBar barra = new JMenuBar();

        JMenu archivo = new JMenu("Archivo");
        archivo.setFont(Estilo.NORMAL);

        JMenuItem actualizar = new JMenuItem("Actualizar todo");
        actualizar.setAccelerator(KeyStroke.getKeyStroke("F5"));
        actualizar.addActionListener(e -> actualizarTodo());

        JMenuItem cerrarSesion = new JMenuItem("Cerrar sesion");
        cerrarSesion.addActionListener(e -> cerrarSesion());

        JMenuItem salir = new JMenuItem("Salir");
        salir.addActionListener(e -> salir());

        archivo.add(actualizar);
        archivo.addSeparator();
        archivo.add(cerrarSesion);
        archivo.add(salir);

        JMenu ayuda = new JMenu("Ayuda");
        ayuda.setFont(Estilo.NORMAL);
        JMenuItem acerca = new JMenuItem("Acerca de PermiCarga Marietha");
        acerca.addActionListener(e -> mostrarAcercaDe());
        ayuda.add(acerca);

        barra.add(archivo);
        barra.add(ayuda);
        return barra;
    }

    /** Construye la franja azul superior. */
    private JPanel armarEncabezado() {
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setBackground(Estilo.AZUL);
        cabecera.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));

        JLabel titulo = new JLabel("PermiCarga Marietha");
        titulo.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 16));
        titulo.setForeground(Color.WHITE);
        cabecera.add(titulo, BorderLayout.WEST);

        Sesion s = Sesion.actual();
        JLabel usuario = new JLabel(s == null ? ""
                : s.nombreCompleto() + "   |   " + s.perfil());
        usuario.setFont(Estilo.NORMAL);
        usuario.setForeground(new Color(198, 216, 236));
        cabecera.add(usuario, BorderLayout.EAST);

        return cabecera;
    }

    /** Construye las pestanas de los modulos. */
    private JTabbedPane armarPestanas() {
        pestanas.setFont(Estilo.SUBTITULO);
        pestanas.addTab("Inicio", panelInicio);
        pestanas.addTab("Autorizaciones", panelAutorizaciones);
        pestanas.addTab("Flota vehicular", panelVehiculos);
        pestanas.addTab("Empresas", panelEmpresas);
        pestanas.addTab("Reportes", panelReportes);

        // Al volver a la pantalla de inicio se recalculan los indicadores, de
        // modo que reflejen lo que el usuario acaba de registrar.
        pestanas.addChangeListener(e -> {
            if (pestanas.getSelectedComponent() == panelInicio) {
                panelInicio.recargar();
            }
        });
        return pestanas;
    }

    /** Construye la barra de estado inferior. */
    private JPanel armarBarraEstado() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(new Color(235, 238, 242));
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(214, 219, 225)),
                BorderFactory.createEmptyBorder(5, 14, 5, 14)));

        Sesion s = Sesion.actual();
        JLabel izquierda = new JLabel("Sesion: " + (s == null ? "-" : s.usuario())
                + "   |   Perfil: " + (s == null ? "-" : s.perfil()));
        izquierda.setFont(Estilo.PEQUENA);
        izquierda.setForeground(Estilo.GRIS);

        JLabel derecha = new JLabel(
                "Base de datos: " + Conexion.descripcionServidor(),
                SwingConstants.RIGHT);
        derecha.setFont(Estilo.PEQUENA);
        derecha.setForeground(Estilo.GRIS);

        barra.add(izquierda, BorderLayout.WEST);
        barra.add(derecha, BorderLayout.EAST);
        return barra;
    }

    /** Vuelve a pedir los datos de todos los modulos. */
    private void actualizarTodo() {
        panelInicio.recargar();
        panelEmpresas.recargar();
        panelVehiculos.recargar();
        panelAutorizaciones.recargar();
    }

    /** Cierra la sesion y vuelve a la pantalla de acceso. */
    private void cerrarSesion() {
        if (!confirmar("Desea cerrar la sesion de "
                + (Sesion.actual() == null ? "" : Sesion.actual().usuario()) + "?")) {
            return;
        }
        Sesion.cerrar();
        dispose();

        VentanaAcceso acceso = new VentanaAcceso();
        acceso.setVisible(true);
        if (acceso.ingreso()) {
            new VentanaPrincipal().setVisible(true);
        } else {
            Conexion.cerrar();
            System.exit(0);
        }
    }

    /** Cierra el programa, liberando la conexion. */
    private void salir() {
        if (!confirmar("Desea salir de PermiCarga Marietha?")) {
            return;
        }
        Conexion.cerrar();
        dispose();
        System.exit(0);
    }

    /**
     * Pide confirmacion al usuario.
     *
     * @param pregunta texto de la pregunta
     * @return {@code true} si confirmo
     */
    private boolean confirmar(String pregunta) {
        return JOptionPane.showConfirmDialog(this, pregunta, "Confirme",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)
                == JOptionPane.YES_OPTION;
    }

    /** Muestra la ficha del sistema. */
    private void mostrarAcercaDe() {
        JPanel p = new JPanel();
        p.setLayout(new javax.swing.BoxLayout(p, javax.swing.BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        String[] lineas = {
            "PermiCarga Marietha",
            "Gestion de permisos de circulacion para carga sobredimensionada",
            " ",
            "Transportes y Maquinarias Marietha E.I.R.L.",
            "RUC 20607294969  -  Arequipa, Peru",
            " ",
            "Curso de Gestion de Datos e Informacion",
            "Escuela Profesional de Ingenieria de Sistemas",
            "Universidad Catolica de Santa Maria",
            " ",
            "Desarrollado en Java con interfaz Swing y acceso JDBC.",
            "Todo el acceso a datos se realiza mediante procedimientos almacenados.",
            " ",
            "Servidor: " + Conexion.descripcionServidor(),
            "Configuracion: " + Configuracion.obtener().origen(),
        };

        for (int i = 0; i < lineas.length; i++) {
            JLabel l = new JLabel(lineas[i]);
            l.setFont(i == 0 ? Estilo.TITULO : Estilo.NORMAL);
            if (i == 0) {
                l.setForeground(Estilo.AZUL);
            }
            p.add(l);
        }

        JOptionPane.showMessageDialog(this, p, "Acerca de",
                JOptionPane.PLAIN_MESSAGE, Estilo.icono(48));
    }
}
