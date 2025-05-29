package modelo;

/**
 * ListaReproduccion - Implementa una lista doblemente enlazada para manejar
 * canciones. Permite agregar, eliminar, buscar y ordenar canciones, así como
 * obtener metadatos de los archivos de audio usando la biblioteca JAudioTagger.
 */
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import java.util.Comparator;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ListaReproduccion implements Serializable {

    private static final long serialVersionUID = 1L; // Para compatibilidad en serialización

    private Nodo cabeza; // Referencia al primer nodo de la lista

    /**
     * Constructor - Inicializa una lista vacía
     */
    public ListaReproduccion() {
        cabeza = null;
    }

    /* ***********************
     * OPERACIONES BÁSICAS
     * ***********************/
    /**
     * Obtiene el primer nodo de la lista
     *
     * @return Nodo cabeza de la lista
     */
    public Nodo getCabeza() {
        return cabeza;
    }

    /**
     * Verifica si la lista está vacía
     *
     * @return true si está vacía, false si no
     */
    public boolean vacia() {
        return cabeza == null;
    }

    /**
     * Obtiene todos los nombres de canciones en la lista
     *
     * @return Lista de nombres de canciones
     */
    public List<String> getNombresCanciones() {
        List<String> nombresCanciones = new ArrayList<>();
        Nodo temp = cabeza;
        while (temp != null) {
            nombresCanciones.add(temp.getNombreCancion());
            temp = temp.getSiguiente();
        }
        return nombresCanciones;
    }

    /**
     * Busca la ruta de una canción por su nombre
     *
     * @param nombreCancion Nombre de la canción a buscar
     * @return Ruta del archivo o null si no se encuentra
     */
    public String getRutaCancion(String nombreCancion) {
        Nodo temp = cabeza;
        while (temp != null) {
            if (temp.getNombreCancion().equals(nombreCancion)) {
                return temp.getRutaCancion();
            }
            temp = temp.getSiguiente();
        }
        return null;
    }

    /* ***********************
     * MANIPULACIÓN DE NODOS
     * ***********************/
    /**
     * Agrega una nueva canción al final de la lista
     *
     * @param nombreCancion Nombre de la canción
     * @param rutaCancion Ruta del archivo de audio
     */
public void agregarCancion(String nombreCancion, String rutaCancion) {
    Nodo nuevoNodo = new Nodo(nombreCancion, rutaCancion);
    if (cabeza == null) {
        cabeza = nuevoNodo;
    } else {
        Nodo temp = cabeza;
        while (temp.getSiguiente() != null) {
            temp = temp.getSiguiente();
        }
        temp.setSiguiente(nuevoNodo);
        nuevoNodo.setAnterior(temp);
    }
}

    /**
     * Elimina una canción de la lista
     *
     * @param nombre Nombre de la canción a eliminar
     * @return true si se eliminó, false si no se encontró
     */
    public boolean eliminarCancion(String nombre) {
        if (vacia()) {
            return false;
        }

        Nodo actual = cabeza;
        while (actual != null) {
            if (nombre.equals(actual.getNombreCancion())) {
                // Caso 1: Es el primer nodo
                if (cabeza == actual) {
                    cabeza = cabeza.getSiguiente();
                    if (cabeza != null) {
                        cabeza.setAnterior(null);
                    }
                } // Caso 2: Es el último nodo
                else if (actual.getSiguiente() == null) {
                    actual.getAnterior().setSiguiente(null);
                } // Caso 3: Nodo intermedio
                else {
                    actual.getAnterior().setSiguiente(actual.getSiguiente());
                    actual.getSiguiente().setAnterior(actual.getAnterior());
                }
                return true;
            }
            actual = actual.getSiguiente();
        }
        return false;
    }
    /**
     * Vacía completamente la lista de reproducción
     */
    public void vaciarLista() {
        cabeza = null;
    }

    /* ***********************
     * NAVEGACIÓN ENTRE CANCIONES
     * ***********************/
    /**
     * Obtiene la siguiente canción en la lista
     *
     * @param nombreCancionActual Canción de referencia
     * @return Nombre de la siguiente canción o null si es la última
     */
    public String getSiguienteCancion(String nombreCancionActual) {
        Nodo temp = cabeza;
        while (temp != null) {
            if (temp.getNombreCancion().equals(nombreCancionActual)) {
                return (temp.getSiguiente() != null) ? temp.getSiguiente().getNombreCancion() : null;
            }
            temp = temp.getSiguiente();
        }
        return null;
    }

    /**
     * Obtiene la canción anterior en la lista
     *
     * @param nombreCancionActual Canción de referencia
     * @return Nombre de la canción anterior o null si es la primera
     */
    public String getCancionAnterior(String nombreCancionActual) {
        Nodo temp = cabeza;
        while (temp != null) {
            if (temp.getNombreCancion().equals(nombreCancionActual)) {
                return (temp.getAnterior() != null) ? temp.getAnterior().getNombreCancion() : null;
            }
            temp = temp.getSiguiente();
        }
        return null;
    }

    /**
     * Obtiene la última canción de la lista
     *
     * @param nombreCancionActual Parámetro no usado (podría eliminarse)
     * @return Nombre de la última canción o null si está vacía
     */
    public String getUltimaCancion(String nombreCancionActual) {
        if (vacia()) {
            return null;
        }

        Nodo temp = cabeza;
        while (temp.getSiguiente() != null) {
            temp = temp.getSiguiente();
        }
        return temp.getNombreCancion();
    }

    public String getRutaPorNombre(String nombreCancion) {
        Nodo temp = cabeza;
        while (temp != null) {
            if (temp.getNombreCancion().equals(nombreCancion)) {
                return temp.getRutaCancion();
            }
            temp = temp.getSiguiente();
        }
        return null;
    }

    /* ***********************
     * MÉTODOS DE ORDENACIÓN
     * ***********************/
    /**
     * Ordena las canciones alfabéticamente (A-Z)
     */
    public void ordenAlfabetico() {
        ordenarPorCriterio(Comparator.comparing(Nodo::getNombreCancion, String.CASE_INSENSITIVE_ORDER));
    }

    /**
     * Ordena las canciones por formato (extensión de archivo)
     */
    public void ordenFormato() {
        ordenarPorCriterio((a, b) -> {
            String formatoA = a.getRutaCancion().substring(a.getRutaCancion().lastIndexOf('.') + 1);
            String formatoB = b.getRutaCancion().substring(b.getRutaCancion().lastIndexOf('.') + 1);
            return formatoA.compareToIgnoreCase(formatoB);
        });
    }

    /**
     * Ordena las canciones por artista (usando metadatos)
     */
    public void ordenArtista() {
        ordenarPorCriterio((a, b) -> obtenerArtista(a).compareToIgnoreCase(obtenerArtista(b)));
    }

    /**
     * Ordena las canciones por duración
     */
    public void ordenDuracion() {
        ordenarPorCriterio(Comparator.comparingInt(this::obtenerDuracion));
    }

    /**
     * Método genérico para ordenar la lista según un criterio
     *
     * @param criterio Comparator que define el orden
     */
    private void ordenarPorCriterio(Comparator<Nodo> criterio) {
        if (vacia()) {
            return;
        }

        // Convertir lista enlazada a ArrayList para ordenar
        List<Nodo> nodos = new ArrayList<>();
        Nodo actual = cabeza;
        while (actual != null) {
            nodos.add(actual);
            actual = actual.getSiguiente();
        }

        nodos.sort(criterio);
        reconstruirLista(nodos);
    }

    /**
     * Reconstruye la lista enlazada después de ordenar
     *
     * @param nodos Lista de nodos ordenados
     */
    private void reconstruirLista(List<Nodo> nodos) {
        cabeza = nodos.get(0);
        cabeza.setAnterior(null);

        for (int i = 1; i < nodos.size(); i++) {
            nodos.get(i - 1).setSiguiente(nodos.get(i));
            nodos.get(i).setAnterior(nodos.get(i - 1));
        }
        nodos.get(nodos.size() - 1).setSiguiente(null);
    }
    
    /* ***********************
     * Mover la cancion a donde se desee
     * ***********************/
    
    /* ***********************
     * MANEJO DE METADATOS
     * ***********************/
    /**
     * Obtiene el artista de una canción (metadatos)
     *
     * @param nodo Nodo que contiene la canción
     * @return Nombre del artista o "Desconocido" si no está disponible
     */
    private String obtenerArtista(Nodo nodo) {
        try {
            AudioFile audioFile = AudioFileIO.read(new File(nodo.getRutaCancion()));
            return audioFile.getTag().getFirst(FieldKey.ARTIST);
        } catch (Exception e) {
            return "Desconocido";
        }
    }

    /**
     * Obtiene la duración de una canción en segundos (metadatos)
     *
     * @param nodo Nodo que contiene la canción
     * @return Duración en segundos o 0 si no está disponible
     */
    private int obtenerDuracion(Nodo nodo) {
        try {
            AudioFile audioFile = AudioFileIO.read(new File(nodo.getRutaCancion()));
            return audioFile.getAudioHeader().getTrackLength();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Obtiene el artista de una canción (versión pública)
     *
     * @param ruta Ruta del archivo de audio
     * @return Nombre del artista o "Desconocido" si no está disponible
     */
    public String obtenerArtistaLegible(String ruta) {
        try {
            AudioFile audioFile = AudioFileIO.read(new File(ruta));
            String artista = audioFile.getTag().getFirst(FieldKey.ARTIST);
            return (artista != null && !artista.trim().isEmpty()) ? artista : "Desconocido";
        } catch (Exception e) {
            return "Desconocido";
        }
    }

    /**
     * Obtiene la duración formateada (MM:SS) de una canción
     *
     * @param rutaCancion Ruta del archivo de audio
     * @return String con formato MM:SS o "0:00" si hay error
     */
    public String obtenerDuracionLegible(String rutaCancion) {
        try {
            AudioFile audioFile = AudioFileIO.read(new File(rutaCancion));
            int duracionSegundos = audioFile.getAudioHeader().getTrackLength();
            return String.format("%d:%02d", duracionSegundos / 60, duracionSegundos % 60);
        } catch (Exception e) {
            System.err.println("Error al obtener duración de: " + rutaCancion + " - " + e.getMessage());
            return "0:00";
        }
    }

    /* ***********************
     * OPERACIONES AVANZADAS
     * ***********************/
    /**
     * Invierte el orden de la lista
     */
    public void invertirLista() {
        if (vacia() || cabeza.getSiguiente() == null) {
            return;
        }

        Nodo actual = cabeza;
        Nodo previo = null;
        Nodo siguiente = null;

        while (actual != null) {
            siguiente = actual.getSiguiente();
            actual.setSiguiente(previo);
            actual.setAnterior(siguiente);
            previo = actual;
            actual = siguiente;
        }
        cabeza = previo;
    }
}