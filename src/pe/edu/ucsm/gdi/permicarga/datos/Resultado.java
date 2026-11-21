package pe.edu.ucsm.gdi.permicarga.datos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tabla de resultados devuelta por un procedimiento almacenado.
 *
 * <p>Guarda los nombres de las columnas y sus filas tal como los entrego el
 * gestor. Se eligio esta representacion generica, y no una clase por cada
 * entidad, porque el aplicativo muestra quince reportes distintos cuyas columnas
 * las decide el procedimiento: si cada reporte necesitara su propia clase en
 * Java, anadir un reporte obligaria a recompilar el programa. Asi, agregar un
 * reporte es escribir un procedimiento en la base de datos y registrarlo en el
 * catalogo de reportes.</p>
 *
 * @author Grupo de Gestion de Datos e Informacion - UCSM
 */
public final class Resultado {

    /** Titulos de las columnas, en el orden en que los devolvio el gestor. */
    private final List<String> columnas;

    /** Filas de datos; cada fila tiene tantos elementos como columnas. */
    private final List<Object[]> filas;

    /**
     * Crea una tabla de resultados.
     *
     * @param columnas titulos de columna
     * @param filas    filas de datos
     */
    public Resultado(List<String> columnas, List<Object[]> filas) {
        this.columnas = columnas == null ? new ArrayList<>() : columnas;
        this.filas = filas == null ? new ArrayList<>() : filas;
    }

    /** @return tabla vacia, util cuando una consulta no devuelve nada */
    public static Resultado vacio() {
        return new Resultado(new ArrayList<>(), new ArrayList<>());
    }

    /** @return titulos de las columnas, en orden */
    public List<String> columnas() {
        return Collections.unmodifiableList(columnas);
    }

    /** @return filas de datos */
    public List<Object[]> filas() {
        return Collections.unmodifiableList(filas);
    }

    /** @return cantidad de filas devueltas */
    public int cantidadFilas() {
        return filas.size();
    }

    /** @return cantidad de columnas */
    public int cantidadColumnas() {
        return columnas.size();
    }

    /** @return {@code true} si la consulta no devolvio ninguna fila */
    public boolean estaVacio() {
        return filas.isEmpty();
    }

    /**
     * Devuelve el valor de una celda como texto.
     *
     * @param fila    indice de fila, desde cero
     * @param columna indice de columna, desde cero
     * @return contenido de la celda, o cadena vacia si es nulo
     */
    public String texto(int fila, int columna) {
        if (fila < 0 || fila >= filas.size()) {
            return "";
        }
        Object[] f = filas.get(fila);
        if (columna < 0 || columna >= f.length || f[columna] == null) {
            return "";
        }
        return String.valueOf(f[columna]);
    }

    /**
     * Busca una columna por su nombre, sin distinguir mayusculas.
     *
     * @param nombre titulo de la columna buscada
     * @return indice de la columna, o -1 si no existe
     */
    public int indiceColumna(String nombre) {
        for (int i = 0; i < columnas.size(); i++) {
            if (columnas.get(i).equalsIgnoreCase(nombre)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Devuelve el valor de una celda identificando la columna por su nombre.
     *
     * @param fila   indice de fila, desde cero
     * @param nombre titulo de la columna
     * @return contenido de la celda, o cadena vacia si no se encuentra
     */
    public String texto(int fila, String nombre) {
        int c = indiceColumna(nombre);
        return c < 0 ? "" : texto(fila, c);
    }

    /** @return los datos en forma de matriz, como los espera JTable */
    public Object[][] matriz() {
        Object[][] m = new Object[filas.size()][];
        for (int i = 0; i < filas.size(); i++) {
            m[i] = filas.get(i);
        }
        return m;
    }

    /** @return los titulos en forma de arreglo, como los espera JTable */
    public String[] titulos() {
        return columnas.toArray(new String[0]);
    }
}
