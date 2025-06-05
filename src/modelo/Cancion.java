package modelo;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * La clase Cancion representa una canción individual con nombre, duración y ruta.
 * Utiliza propiedades JavaFX para facilitar el enlace con la interfaz gráfica.
 * 
 * Características:
 * Propiedades observables: nombre y duración
 * Ruta del archivo de audio
 * Uso en TableView, listas de reproducción, favoritos, etc.
 * 
 */
public class Cancion {

    // Propiedades observables para JavaFX
    private final StringProperty nombre;
    private final StringProperty duracion;

    // Ruta absoluta del archivo de audio
    private final String ruta;

    /**
     * Constructor de Cancion.
     *
     * @param nombre   Nombre de la canción
     * @param duracion Duración de la canción en formato mm:ss
     * @param ruta     Ruta absoluta del archivo de audio
     */
    public Cancion(String nombre, String duracion, String ruta) {
        this.nombre = new SimpleStringProperty(nombre);
        this.duracion = new SimpleStringProperty(duracion);
        this.ruta = ruta;
    }

    /* ***********************
     * Métodos de acceso (getters y propiedades observables)
     * ***********************/

    /**
     * Obtiene el nombre de la canción.
     *
     * @return Nombre de la canción
     */
    public String getNombre() {
        return nombre.get();
    }

    /**
     * Devuelve la propiedad del nombre (para bindings en JavaFX).
     *
     * @return Propiedad nombre
     */
    public StringProperty nombreProperty() {
        return nombre;
    }

    /**
     * Obtiene la duración de la canción.
     *
     * @return Duración en formato mm:ss
     */
    public String getDuracion() {
        return duracion.get();
    }

    /**
     * Devuelve la propiedad de duración (para bindings en JavaFX).
     *
     * @return Propiedad duración
     */
    public StringProperty duracionProperty() {
        return duracion;
    }

    /**
     * Obtiene la ruta absoluta del archivo de la canción.
     *
     * @return Ruta del archivo de audio
     */
    public String getRuta() {
        return ruta;
    }
}
