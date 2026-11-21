package pe.edu.ucsm.gdi.permicarga.vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.SQLException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import pe.edu.ucsm.gdi.permicarga.config.Configuracion;
import pe.edu.ucsm.gdi.permicarga.datos.Conexion;
import pe.edu.ucsm.gdi.permicarga.datos.Repositorio;
import pe.edu.ucsm.gdi.permicarga.datos.Resultado;
import pe.edu.ucsm.gdi.permicarga.modelo.Sesion;
import pe.edu.ucsm.gdi.permicarga.util.Dialogos;

/**
 * Pantalla de ingreso al sistema.
 *
 * <p>Es la primera ventana que ve el usuario. Ademas de pedir las credenciales,
 * comprueba que la base de datos responda: si el servidor no esta levantado o
 * los datos de conexion del archivo de configuracion son incorrectos, conviene
 * decirlo aqui y con claridad, y no dejar que el usuario descubra el problema
 * mas adelante con un mensaje incomprensible.</p>
 *
 * <p>La contrasena nunca se guarda ni se transmite en claro a la tabla: el
 * procedimiento SP_USUARIO_AUTENTICAR la compara contra el resumen SHA-256
 * almacenado.</p>
 *
 * @author Grupo de Gestion de Datos e Informacion - UCSM
 */
public final class VentanaAcceso extends JDialog {

    private static final long serialVersionUID = 1L;

    private final JTextField txtUsuario = new JTextField(16);
    private final JPasswordField txtClave = new JPasswordField(16);
    private final JLabel lblEstado = new JLabel(" ", SwingConstants.CENTER);

    /** Queda en verdadero si el usuario ingreso correctamente. */
    private boolean ingreso = false;

    /** Arma la ventana de acceso. */
    public VentanaAcceso() {
        super((java.awt.Frame) null, "PermiCarga Marietha - Acceso", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setIconImage(Estilo.icono(64).getImage());

        setLayout(new BorderLayout());
        add(armarPortada(), BorderLayout.NORTH);
        add(armarFormulario(), BorderLayout.CENTER);
        add(armarPie(), BorderLayout.SOUTH);

        pack();
        setSize(430, getHeight());
        setResizable(false);
        setLocationRelativeTo(null);
    }

    /** @return {@code true} si se abrio sesion correctamente */
    public boolean ingreso() {
        return ingreso;
    }

    /** Construye la franja azul superior con el nombre del sistema. */
    private JPanel armarPortada() {
        Estilo.FondoDegradado portada = new Estilo.FondoDegradado();
        portada.setLayout(new BoxLayout(portada, BoxLayout.Y_AXIS));
        portada.setBorder(BorderFactory.createEmptyBorder(22, 20, 20, 20));
        portada.setPreferredSize(new Dimension(430, 132));

        JLabel nombre = new JLabel("PermiCarga Marietha");
        nombre.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 22));
        nombre.setForeground(Color.WHITE);
        nombre.setAlignmentX(CENTER_ALIGNMENT);

        JLabel detalle = new JLabel("Gestion de permisos de carga sobredimensionada");
        detalle.setFont(Estilo.NORMAL);
        detalle.setForeground(new Color(198, 216, 236));
        detalle.setAlignmentX(CENTER_ALIGNMENT);

        JLabel empresa = new JLabel("Transportes y Maquinarias Marietha E.I.R.L.");
        empresa.setFont(Estilo.PEQUENA);
        empresa.setForeground(new Color(168, 193, 219));
        empresa.setAlignmentX(CENTER_ALIGNMENT);

        portada.add(Box.createVerticalGlue());
        portada.add(nombre);
        portada.add(Box.createVerticalStrut(6));
        portada.add(detalle);
        portada.add(Box.createVerticalStrut(10));
        portada.add(empresa);
        portada.add(Box.createVerticalGlue());
        return portada;
    }

    /** Construye los campos de usuario y contrasena. */
    private JPanel armarFormulario() {
        JPanel centro = new JPanel(new GridBagLayout());
        centro.setBackground(Color.WHITE);
        centro.setBorder(BorderFactory.createEmptyBorder(20, 34, 10, 34));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 4, 5, 4);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 2;
        JLabel titulo = new JLabel("Ingrese sus credenciales");
        titulo.setFont(Estilo.SUBTITULO);
        titulo.setForeground(Estilo.AZUL);
        centro.add(titulo, g);

        g.gridwidth = 1;
        g.gridy = 1;
        g.gridx = 0;
        centro.add(new JLabel("Usuario:"), g);
        g.gridx = 1;
        centro.add(txtUsuario, g);

        g.gridy = 2;
        g.gridx = 0;
        centro.add(new JLabel("Contrasena:"), g);
        g.gridx = 1;
        centro.add(txtClave, g);

        g.gridy = 3;
        g.gridx = 0;
        g.gridwidth = 2;
        lblEstado.setFont(Estilo.PEQUENA);
        lblEstado.setForeground(Estilo.ROJO);
        centro.add(lblEstado, g);

        KeyAdapter intro = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    entrar();
                }
            }
        };
        txtUsuario.addKeyListener(intro);
        txtClave.addKeyListener(intro);

        return centro;
    }

    /** Construye la zona inferior con los botones y la nota de conexion. */
    private JPanel armarPie() {
        JPanel pie = new JPanel(new BorderLayout(0, 6));
        pie.setBackground(Color.WHITE);
        pie.setBorder(BorderFactory.createEmptyBorder(0, 34, 16, 34));

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        botones.setOpaque(false);

        JButton salir = Estilo.boton("Salir");
        salir.addActionListener(e -> {
            ingreso = false;
            dispose();
        });

        JButton entrar = Estilo.botonPrincipal("Ingresar");
        entrar.addActionListener(e -> entrar());

        botones.add(salir);
        botones.add(entrar);
        getRootPane().setDefaultButton(entrar);
        pie.add(botones, BorderLayout.NORTH);

        JLabel conexion = new JLabel(
                "Servidor: " + resumirUrl(Configuracion.obtener().url()),
                SwingConstants.CENTER);
        conexion.setFont(Estilo.PEQUENA);
        conexion.setForeground(Estilo.GRIS);
        pie.add(conexion, BorderLayout.SOUTH);

        return pie;
    }

    /** Valida las credenciales contra la base de datos. */
    private void entrar() {
        String usuario = txtUsuario.getText().trim();
        String clave = new String(txtClave.getPassword());

        if (usuario.isEmpty() || clave.isEmpty()) {
            lblEstado.setText("Escriba su usuario y su contrasena.");
            return;
        }

        lblEstado.setForeground(Estilo.GRIS);
        lblEstado.setText("Verificando...");

        try {
            Resultado r = Repositorio.llamar("SP_USUARIO_AUTENTICAR", usuario, clave);
            if (r.estaVacio()) {
                lblEstado.setForeground(Estilo.ROJO);
                lblEstado.setText("No se pudo validar el acceso.");
                return;
            }

            Sesion.abrir(new Sesion(
                    Integer.parseInt(r.texto(0, "Id_usuario")),
                    r.texto(0, "Usuario"),
                    r.texto(0, "Nombre_completo"),
                    r.texto(0, "Perfil"),
                    r.texto(0, "Ultimo_acceso")));

            ingreso = true;
            dispose();

        } catch (Repositorio.ErrorDeNegocio ex) {
            // Credenciales incorrectas o cuenta desactivada: el mensaje viene
            // redactado desde el procedimiento y se muestra en la propia
            // ventana, que es menos molesto que abrir un cuadro de dialogo.
            lblEstado.setForeground(Estilo.ROJO);
            lblEstado.setText(ex.getMessage());
            txtClave.setText("");
            txtClave.requestFocus();

        } catch (SQLException ex) {
            lblEstado.setText(" ");
            Dialogos.error(this, new SQLException(
                    "No se pudo conectar con la base de datos.\n\n"
                            + "Compruebe que el servidor MySQL este encendido y que "
                            + "los datos del archivo config.properties sean correctos.\n"
                            + "Archivo en uso: " + Configuracion.obtener().origen()
                            + "\n\nDetalle: " + ex.getMessage(), ex));

        } catch (Exception ex) {
            lblEstado.setText(" ");
            Dialogos.error(this, ex);
        }
    }

    /**
     * Acorta la direccion de conexion para mostrarla sin ocupar toda la linea.
     *
     * @param url direccion completa
     * @return direccion sin los parametros
     */
    private static String resumirUrl(String url) {
        int corte = url.indexOf('?');
        String limpia = corte > 0 ? url.substring(0, corte) : url;
        return limpia.replace("jdbc:mariadb://", "").replace("jdbc:mysql://", "");
    }

    /**
     * Comprueba antes de mostrar la ventana que el servidor responda, para
     * poder avisar de inmediato si no hay conexion.
     *
     * @return {@code true} si la base de datos respondio
     */
    public static boolean hayConexion() {
        try {
            Conexion.obtener();
            return Conexion.activa();
        } catch (SQLException ex) {
            return false;
        }
    }
}
