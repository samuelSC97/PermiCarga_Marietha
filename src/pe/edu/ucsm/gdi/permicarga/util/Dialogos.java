package pe.edu.ucsm.gdi.permicarga.util;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

import pe.edu.ucsm.gdi.permicarga.datos.Repositorio;

/**
 * Mensajes que el aplicativo muestra al usuario.
 *
 * <p>Centralizarlos aqui persigue que todos tengan el mismo aspecto y, sobre
 * todo, el mismo criterio: distinguir un error de negocio, que el usuario puede
 * corregir, de una falla tecnica, ante la cual no puede hacer nada salvo avisar
 * a quien administra el sistema.</p>
 *
 * @author Grupo de Gestion de Datos e Informacion - UCSM
 */
public final class Dialogos {

    /** Clase de utilidad: no se instancia. */
    private Dialogos() {
    }

    /**
     * Informa que una operacion se completo.
     *
     * @param padre   componente sobre el que se centra el aviso
     * @param mensaje texto a mostrar
     */
    public static void exito(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "Operacion realizada",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Advierte de un dato faltante o incorrecto antes de enviarlo al gestor.
     *
     * @param padre   componente sobre el que se centra el aviso
     * @param mensaje texto a mostrar
     */
    public static void advertencia(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "Revise los datos",
                JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Muestra el fallo de una operacion.
     *
     * <p>Si la base de datos rechazo la operacion por una regla de negocio, su
     * mensaje ya viene redactado en castellano y se muestra tal cual. Si se
     * trata de una falla tecnica se avisa en terminos generales y se incluye el
     * detalle, que es lo que necesita quien administra el sistema.</p>
     *
     * @param padre  componente sobre el que se centra el aviso
     * @param fallo  excepcion ocurrida
     */
    public static void error(Component padre, Exception fallo) {
        if (fallo instanceof Repositorio.ErrorDeNegocio) {
            JOptionPane.showMessageDialog(padre, fallo.getMessage(),
                    "No se pudo completar la operacion", JOptionPane.WARNING_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(padre,
                    "Ocurrio un problema al comunicarse con la base de datos.\n\n"
                            + "Detalle tecnico: " + fallo.getMessage(),
                    "Error del sistema", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Pide confirmacion antes de una operacion que cambia datos.
     *
     * @param padre    componente sobre el que se centra la pregunta
     * @param pregunta texto de la pregunta
     * @return {@code true} si el usuario confirmo
     */
    public static boolean confirmar(Component padre, String pregunta) {
        return JOptionPane.showConfirmDialog(padre, pregunta, "Confirme la operacion",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)
                == JOptionPane.YES_OPTION;
    }

    /**
     * Solicita un texto breve, como el motivo de una anulacion.
     *
     * @param padre     componente sobre el que se centra la pregunta
     * @param pregunta  texto de la pregunta
     * @return lo escrito por el usuario, o {@code null} si cancelo
     */
    public static String pedirTexto(Component padre, String pregunta) {
        return JOptionPane.showInputDialog(padre, pregunta, "Dato requerido",
                JOptionPane.QUESTION_MESSAGE);
    }

    /**
     * Hace que la tecla Escape cierre un formulario sin guardar.
     *
     * <p>Es lo que cualquier usuario espera de una ventana emergente, y evita
     * tener que buscar el boton Cancelar con el raton.</p>
     *
     * @param dialogo formulario al que se le asocia la tecla
     */
    public static void cerrarConEscape(JDialog dialogo) {
        JRootPane raiz = dialogo.getRootPane();
        raiz.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cerrarFormulario");
        raiz.getActionMap().put("cerrarFormulario", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                dialogo.dispose();
            }
        });
    }
}
