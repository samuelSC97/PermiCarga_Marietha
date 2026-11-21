package pe.edu.ucsm.gdi.permicarga.vista;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

import java.awt.image.BufferedImage;

/**
 * Apariencia comun de todas las pantallas.
 *
 * <p>Reune en un solo lugar los colores, las tipografias y los componentes que
 * se repiten, de modo que la aplicacion se vea igual en todas sus ventanas y un
 * cambio de aspecto se haga una sola vez.</p>
 *
 * @author Grupo de Gestion de Datos e Informacion - UCSM
 */
public final class Estilo {

    /** Azul principal, usado en cabeceras y botones de accion. */
    public static final Color AZUL = new Color(21, 62, 108);

    /** Azul mas claro, para elementos secundarios. */
    public static final Color AZUL_CLARO = new Color(226, 235, 245);

    /** Gris del fondo general. */
    public static final Color FONDO = new Color(245, 246, 248);

    /** Gris de los textos secundarios. */
    public static final Color GRIS = new Color(110, 116, 124);

    /** Verde para estados correctos. */
    public static final Color VERDE = new Color(28, 122, 70);

    /** Ambar para advertencias, como un permiso proximo a vencer. */
    public static final Color AMBAR = new Color(176, 110, 10);

    /** Rojo para errores y registros dados de baja. */
    public static final Color ROJO = new Color(168, 42, 42);

    /** Tipografia de titulos. */
    public static final Font TITULO = new Font("SansSerif", Font.BOLD, 17);

    /** Tipografia de subtitulos. */
    public static final Font SUBTITULO = new Font("SansSerif", Font.BOLD, 13);

    /** Tipografia de texto comun. */
    public static final Font NORMAL = new Font("SansSerif", Font.PLAIN, 13);

    /** Tipografia pequena, para la barra de estado. */
    public static final Font PEQUENA = new Font("SansSerif", Font.PLAIN, 11);

    /** Clase de utilidad: no se instancia. */
    private Estilo() {
    }

    /**
     * Aplica la apariencia Nimbus, que forma parte de la biblioteca estandar y
     * da un aspecto homogeneo en Windows, Linux y macOS.
     */
    public static void aplicar() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
            UIManager.put("control", FONDO);
            UIManager.put("nimbusBase", AZUL);
            UIManager.put("nimbusSelectionBackground", AZUL);
            UIManager.put("Table.alternateRowColor", new Color(248, 250, 252));
            UIManager.put("OptionPane.messageFont", NORMAL);
            UIManager.put("OptionPane.buttonFont", NORMAL);
        } catch (Exception ex) {
            // Si Nimbus no estuviera disponible se usa la apariencia por
            // defecto: el programa sigue siendo plenamente utilizable.
        }
    }

    /**
     * Crea un titulo de seccion.
     *
     * @param texto texto del titulo
     * @return etiqueta ya formateada
     */
    public static JLabel titulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(TITULO);
        l.setForeground(AZUL);
        return l;
    }

    /**
     * Crea un texto explicativo secundario.
     *
     * @param texto contenido
     * @return etiqueta ya formateada
     */
    public static JLabel ayuda(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(PEQUENA);
        l.setForeground(GRIS);
        return l;
    }

    /**
     * Crea un boton de accion principal.
     *
     * @param texto rotulo del boton
     * @return boton ya formateado
     */
    public static JButton botonPrincipal(String texto) {
        JButton b = new JButton(texto);
        b.setFont(SUBTITULO);
        b.setBackground(AZUL);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        return b;
    }

    /**
     * Crea un boton secundario, de contorno.
     *
     * @param texto rotulo del boton
     * @return boton ya formateado
     */
    public static JButton boton(String texto) {
        JButton b = new JButton(texto);
        b.setFont(NORMAL);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        return b;
    }

    /**
     * Borde con titulo para agrupar campos de un formulario.
     *
     * @param texto titulo del grupo
     * @return borde listo para asignar
     */
    public static Border grupo(String texto) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(214, 219, 225)),
                        texto, 0, 0, SUBTITULO, AZUL),
                BorderFactory.createEmptyBorder(8, 10, 10, 10));
    }

    /**
     * Da formato a una tabla de resultados: altura de fila legible, cabecera
     * destacada y coloreado de la columna de estado o situacion.
     *
     * @param tabla tabla a formatear
     */
    public static void formatearTabla(JTable tabla) {
        tabla.setRowHeight(24);
        tabla.setFont(NORMAL);
        tabla.setGridColor(new Color(228, 231, 235));
        tabla.setSelectionBackground(AZUL_CLARO);
        tabla.setSelectionForeground(Color.BLACK);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JTableHeader cabecera = tabla.getTableHeader();
        cabecera.setFont(SUBTITULO);
        cabecera.setBackground(AZUL);
        cabecera.setForeground(Color.WHITE);
        cabecera.setReorderingAllowed(false);

        tabla.setDefaultRenderer(Object.class, new RenderizadorEstado());
        ajustarAnchos(tabla);
    }

    /**
     * Ajusta el ancho de cada columna al contenido mas largo que contiene, con
     * un maximo para que una columna de texto libre no desplace a las demas.
     *
     * @param tabla tabla a ajustar
     */
    public static void ajustarAnchos(JTable tabla) {
        for (int c = 0; c < tabla.getColumnCount(); c++) {
            int ancho = 80;
            Component cab = tabla.getTableHeader().getDefaultRenderer()
                    .getTableCellRendererComponent(tabla,
                            tabla.getColumnName(c), false, false, -1, c);
            ancho = Math.max(ancho, cab.getPreferredSize().width + 24);

            int tope = Math.min(tabla.getRowCount(), 40);
            for (int f = 0; f < tope; f++) {
                Component celda = tabla.prepareRenderer(tabla.getCellRenderer(f, c), f, c);
                ancho = Math.max(ancho, celda.getPreferredSize().width + 20);
            }
            tabla.getColumnModel().getColumn(c).setPreferredWidth(Math.min(ancho, 330));
        }
    }

    /**
     * Genera el icono de la aplicacion por codigo, para no depender de archivos
     * externos que puedan extraviarse al copiar la carpeta de entrega.
     *
     * @param lado tamano en pixeles
     * @return icono cuadrado con las iniciales del sistema
     */
    public static ImageIcon icono(int lado) {
        BufferedImage img = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(AZUL);
        g.fillRoundRect(0, 0, lado, lado, lado / 4, lado / 4);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (lado * 0.5)));
        String t = "PC";
        int x = (lado - g.getFontMetrics().stringWidth(t)) / 2;
        int y = (lado - g.getFontMetrics().getHeight()) / 2 + g.getFontMetrics().getAscent();
        g.drawString(t, x, y);
        g.dispose();
        return new ImageIcon(img);
    }

    /**
     * Pinta de color las celdas que informan un estado o una situacion, para
     * que el usuario localice de un vistazo lo vencido o lo dado de baja.
     */
    private static class RenderizadorEstado extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor,
                boolean seleccionada, boolean enfocada, int fila, int columna) {

            Component c = super.getTableCellRendererComponent(
                    tabla, valor, seleccionada, enfocada, fila, columna);

            c.setForeground(Color.BLACK);
            c.setFont(NORMAL);

            String texto = valor == null ? "" : String.valueOf(valor);
            switch (texto) {
                case "VIGENTE":
                case "ACTIVO":
                case "Dentro del tope bonificado":
                    c.setForeground(VERDE);
                    c.setFont(SUBTITULO);
                    break;
                case "POR VENCER":
                case "PROGRAMADA":
                    c.setForeground(AMBAR);
                    c.setFont(SUBTITULO);
                    break;
                case "VENCIDA":
                case "ANULADA":
                case "INACTIVO":
                case "EXCEDE EL TOPE":
                case "EXCEDE INCLUSO CON BONIFICACION":
                    c.setForeground(ROJO);
                    c.setFont(SUBTITULO);
                    break;
                default:
                    break;
            }
            return c;
        }
    }

    /** Panel de fondo con un degradado suave, usado en la ventana de acceso. */
    public static class FondoDegradado extends javax.swing.JPanel {

        private static final long serialVersionUID = 1L;

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new java.awt.GradientPaint(
                    0, 0, AZUL, 0, getHeight(), new Color(12, 38, 68)));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }
}
