package pe.edu.ucsm.gdi.permicarga.vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import pe.edu.ucsm.gdi.permicarga.datos.Conexion;
import pe.edu.ucsm.gdi.permicarga.datos.Repositorio;
import pe.edu.ucsm.gdi.permicarga.datos.Resultado;
import pe.edu.ucsm.gdi.permicarga.modelo.Sesion;

/**
 * Pantalla de inicio del aplicativo.
 *
 * <p>Resume en cuatro indicadores el estado de la operacion, para que el
 * administrador sepa al entrar si hay algo que atender ese dia: permisos
 * vigentes, permisos por vencer, unidades en flota y traslados que necesitan
 * escolta policial.</p>
 *
 * @author Grupo de Gestion de Datos e Informacion - UCSM
 */
public final class PanelInicio extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel valVigentes = new JLabel("-", SwingConstants.CENTER);
    private final JLabel valPorVencer = new JLabel("-", SwingConstants.CENTER);
    private final JLabel valFlota = new JLabel("-", SwingConstants.CENTER);
    private final JLabel valEscolta = new JLabel("-", SwingConstants.CENTER);

    /** Arma la pantalla y carga los indicadores. */
    public PanelInicio() {
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        setBackground(Estilo.FONDO);

        add(armarBienvenida(), BorderLayout.NORTH);
        add(armarIndicadores(), BorderLayout.CENTER);
        add(armarPie(), BorderLayout.SOUTH);

        recargar();
    }

    /** Construye el saludo al usuario que ingreso. */
    private JPanel armarBienvenida() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        Sesion s = Sesion.actual();
        String nombre = s == null ? "" : s.nombreCompleto();
        String perfil = s == null ? "" : s.perfil();

        JLabel saludo = Estilo.titulo("Bienvenido, " + nombre);
        saludo.setAlignmentX(LEFT_ALIGNMENT);

        JLabel sub = Estilo.ayuda("Perfil " + perfil
                + "   |   Ingreso anterior: " + (s == null ? "-" : s.ultimoAcceso()));
        sub.setAlignmentX(LEFT_ALIGNMENT);

        p.add(saludo);
        p.add(Box.createVerticalStrut(3));
        p.add(sub);
        return p;
    }

    /** Construye las cuatro tarjetas de indicadores. */
    private JPanel armarIndicadores() {
        JPanel rejilla = new JPanel(new GridLayout(2, 2, 14, 14));
        rejilla.setOpaque(false);

        rejilla.add(tarjeta("Permisos vigentes hoy", valVigentes, Estilo.VERDE,
                "Autorizaciones con las que se puede circular en este momento."));
        rejilla.add(tarjeta("Vencen en 7 dias", valPorVencer, Estilo.AMBAR,
                "Permisos que conviene renovar antes de que caduquen."));
        rejilla.add(tarjeta("Unidades en flota", valFlota, Estilo.AZUL,
                "Vehiculos activos disponibles para declarar en un traslado."));
        rejilla.add(tarjeta("Requieren escolta", valEscolta, Estilo.ROJO,
                "Traslados que exigen coordinar apoyo policial."));
        return rejilla;
    }

    /**
     * Construye una tarjeta de indicador.
     *
     * @param titulo      rotulo superior
     * @param valor       etiqueta donde ira el numero
     * @param color       color del numero
     * @param explicacion linea inferior
     * @return panel ya formateado
     */
    private JPanel tarjeta(String titulo, JLabel valor, Color color, String explicacion) {
        JPanel t = new JPanel(new BorderLayout(0, 6));
        t.setBackground(Color.WHITE);
        t.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(222, 226, 231)),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));

        JLabel rotulo = new JLabel(titulo);
        rotulo.setFont(Estilo.SUBTITULO);
        rotulo.setForeground(Estilo.GRIS);

        valor.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 40));
        valor.setForeground(color);

        JLabel nota = Estilo.ayuda(explicacion);

        t.add(rotulo, BorderLayout.NORTH);
        t.add(valor, BorderLayout.CENTER);
        t.add(nota, BorderLayout.SOUTH);
        t.setPreferredSize(new Dimension(260, 140));
        return t;
    }

    /** Construye la nota inferior sobre el servidor. */
    private JPanel armarPie() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.setOpaque(false);
        p.add(Estilo.ayuda("Los indicadores se calculan en el servidor mediante "
                + "procedimientos almacenados. Conectado a "
                + Conexion.descripcionServidor() + "."));
        return p;
    }

    /** Vuelve a calcular los cuatro indicadores. */
    public void recargar() {
        valVigentes.setText(contar("SP_REP_01_VIGENTES"));
        valEscolta.setText(contar("SP_REP_09_APOYO_POLICIAL"));
        try {
            valPorVencer.setText(String.valueOf(
                    Repositorio.llamar("SP_REP_02_POR_VENCER", 7).cantidadFilas()));
        } catch (Exception ex) {
            valPorVencer.setText("-");
        }
        try {
            valFlota.setText(String.valueOf(
                    Repositorio.llamar("SP_COMBO_VEHICULOS").cantidadFilas()));
        } catch (Exception ex) {
            valFlota.setText("-");
        }
    }

    /**
     * Cuenta las filas que devuelve un procedimiento sin parametros.
     *
     * @param procedimiento nombre del procedimiento
     * @return cantidad de filas, o un guion si no se pudo consultar
     */
    private static String contar(String procedimiento) {
        try {
            Resultado r = Repositorio.llamar(procedimiento);
            return String.valueOf(r.cantidadFilas());
        } catch (Exception ex) {
            return "-";
        }
    }
}
