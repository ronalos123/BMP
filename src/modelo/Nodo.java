package modelo;

/*
 * Nodo.java - Implementa un nodo para lista doblemente enlazada de canciones.
 * Cada nodo contiene información de una canción y referencias al nodo anterior
 * y siguiente en la lista de reproducción.
 */
import java.io.Serializable;

/**
 * Clase que representa un nodo en una lista doblemente enlazada de canciones.
 * Implementa Serializable para permitir la persistencia de las listas de reproducción.
 */
public class Nodo implements Serializable {

    private static final long serialVersionUID = 1L; // Control de versión para serialización

    // Campos de la clase
    private final String nombreCancion;  // Nombre de la canción (no cambia después de creado)
    private final String rutaCancion;    // Ruta absoluta del archivo de audio (no cambia)
    private Nodo siguiente;              // Referencia al siguiente nodo en la lista
    private Nodo anterior;               // Referencia al nodo anterior en la lista

    /* ***********************
     * CONSTRUCTOR
     * ***********************/

    /**
     * Crea un nuevo nodo con la información de una canción.
     * 
     * @param nombreCancion Nombre descriptivo de la canción
     * @param rutaCancion   Ruta absoluta del archivo de audio
     */
    public Nodo(String nombreCancion, String rutaCancion) {
        this.nombreCancion = nombreCancion;
        this.rutaCancion = rutaCancion;
        this.siguiente = null;
        this.anterior = null;
    }

    /* ***********************
     * GETTERS Y SETTERS
     * ***********************/

    /**
     * Obtiene el nombre de la canción almacenada en este nodo.
     * 
     * @return Nombre de la canción
     */
    public String getNombreCancion() {
        return nombreCancion;
    }

    /**
     * Obtiene la ruta del archivo de audio.
     * 
     * @return Ruta absoluta del archivo
     */
    public String getRutaCancion() {
        return rutaCancion;
    }

    /**
     * Obtiene la referencia al siguiente nodo en la lista.
     * 
     * @return Siguiente nodo o null si es el último
     */
    public Nodo getSiguiente() {
        return siguiente;
    }

    /**
     * Establece la referencia al siguiente nodo en la lista.
     * 
     * @param siguiente Nodo que será el siguiente en la secuencia
     */
    public void setSiguiente(Nodo siguiente) {
        this.siguiente = siguiente;
    }

    /**
     * Obtiene la referencia al nodo anterior en la lista.
     * 
     * @return Nodo anterior o null si es el primero
     */
    public Nodo getAnterior() {
        return anterior;
    }

    /**
     * Establece la referencia al nodo anterior en la lista.
     * 
     * @param anterior Nodo que será el anterior en la secuencia
     */
    public void setAnterior(Nodo anterior) {
        this.anterior = anterior;
    }

    /* ***********************
     * MÉTODOS SOBREESCRITOS
     * ***********************/

    /**
     * Representación en String del nodo (solo muestra el nombre de la canción).
     * 
     * @return Nombre de la canción
     */
    @Override
    public String toString() {
        return nombreCancion;
    }
}