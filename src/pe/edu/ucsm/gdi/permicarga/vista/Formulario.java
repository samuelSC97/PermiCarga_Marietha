package pe.edu.ucsm.gdi.permicarga.vista;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Armador de formularios con campos alineados.
 *
 * <p>Coloca cada campo con su rotulo en una rejilla, de modo que todos los
 * formularios del aplicativo queden alineados igual sin repetir en cada uno el
 * ajuste de las restricciones de posicion.</p>
 *
 * @author Grupo de Gestion de Datos e Informacion - UCSM
 */
public final class Formulario extends JPanel {

    private static final long serialVersionUID = 1L;

    private final GridBagConstraints gbc = new GridBagConstraints();
    private int filaActual = 0;

    /** Crea un formulario vacio. */
    public Formulario() {
        setLayout(new GridBagLayout());
        setOpaque(false);
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.anchor = GridBagConstraints.WEST;
    }

    /**
     * Agrega una fila con un rotulo y su campo, ocupando todo el ancho.
     *
     * @param rotulo texto del rotulo
     * @param campo  componente de entrada
     * @return este mismo formulario, para encadenar llamadas
     */
    public Formulario fila(String rotulo, JComponent campo) {
        return fila(rotulo, campo, 3);
    }

    /**
     * Agrega una fila indicando cuantas columnas ocupa el campo.
     *
     * @param rotulo   texto del rotulo
     * @param campo    componente de entrada
     * @param columnas columnas que ocupa el campo
     * @return este mismo formulario
     */
    public Formulario fila(String rotulo, JComponent campo, int columnas) {
        gbc.gridx = 0;
        gbc.gridy = filaActual;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JLabel l = new JLabel(rotulo);
        l.setFont(Estilo.NORMAL);
        add(l, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = columnas;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(campo, gbc);

        filaActual++;
        return this;
    }

    /**
     * Agrega una fila con dos campos, cada uno con su rotulo.
     *
     * @param rotulo1 rotulo del primer campo
     * @param campo1  primer campo
     * @param rotulo2 rotulo del segundo campo
     * @param campo2  segundo campo
     * @return este mismo formulario
     */
    public Formulario fila(String rotulo1, JComponent campo1,
                           String rotulo2, JComponent campo2) {
        gbc.gridy = filaActual;
        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JLabel l1 = new JLabel(rotulo1);
        l1.setFont(Estilo.NORMAL);
        add(l1, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(campo1, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JLabel l2 = new JLabel(rotulo2);
        l2.setFont(Estilo.NORMAL);
        add(l2, gbc);

        gbc.gridx = 3;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(campo2, gbc);

        filaActual++;
        return this;
    }

    /**
     * Agrega una linea de texto explicativo que ocupa todo el ancho.
     *
     * @param texto contenido de la nota
     * @return este mismo formulario
     */
    public Formulario nota(String texto) {
        gbc.gridx = 0;
        gbc.gridy = filaActual;
        gbc.gridwidth = 4;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(Estilo.ayuda(texto), gbc);
        filaActual++;
        return this;
    }

    /**
     * Fija el ancho preferido de un campo, para que la rejilla no lo estire.
     *
     * @param campo componente a dimensionar
     * @param ancho ancho en pixeles
     * @return el mismo componente
     */
    public static JComponent ancho(JComponent campo, int ancho) {
        Dimension d = campo.getPreferredSize();
        campo.setPreferredSize(new Dimension(ancho, d.height));
        return campo;
    }
}
