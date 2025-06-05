package modelo;

/**
 * ListaReproduccion - Implementa una lista doblemente enlazada para manejar
 * canciones. Permite agregar, eliminar, buscar y ordenar canciones, así como
 * obtener metadatos de los archivos de audio usando la biblioteca JAudioTagger.
 */

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javafx.scene.image.Image;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

public class ListaReproduccion implements Serializable {

    private static final long serialVersionUID = 1L; // Para compatibilidad con la serialización

    private Nodo cabeza;                // Referencia al primer nodo de la lista
    private List<Cancion> canciones;    // Lista auxiliar (no enlazada) de canciones

    // Constructor: Inicializa la lista vacía
    public ListaReproduccion() {
        cabeza = null;
    }

    /* ========================
     *  MÉTODOS BÁSICOS
     * ======================== */

    public Nodo getCabeza() {
        return cabeza;
    }

    public boolean vacia() {
        return cabeza == null;
    }

    /**
     * Retorna los nombres de todas las canciones de la lista.
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
     * Busca la ruta de una canción por su nombre.
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

    public String getRutaPorNombre(String nombreCancion) {
        return getRutaCancion(nombreCancion);
    }

    /**
     * Vacía la lista por completo.
     */
    public void vaciarLista() {
        cabeza = null;
    }

    /* ========================
     *  MANIPULACIÓN DE CANCIONES
     * ======================== */

    /**
     * Agrega una nueva canción al final de la lista.
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
     * Elimina una canción por su nombre.
     */
    public boolean eliminarCancion(String nombre) {
        if (vacia()) return false;

        Nodo actual = cabeza;
        while (actual != null) {
            if (nombre.equals(actual.getNombreCancion())) {
                if (actual == cabeza) {
                    cabeza = actual.getSiguiente();
                    if (cabeza != null) cabeza.setAnterior(null);
                } else if (actual.getSiguiente() == null) {
                    actual.getAnterior().setSiguiente(null);
                } else {
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
     * Cuenta el número total de canciones en la lista.
     */
    public int contarCanciones() {
        int res = 0;
        Nodo ac = cabeza;
        while (ac != null) {
            res++;
            ac = ac.getSiguiente();
        }
        return res;
    }

    /* ========================
     *  NAVEGACIÓN
     * ======================== */

    public String getSiguienteCancion(String nombreActual) {
        Nodo temp = cabeza;
        while (temp != null) {
            if (temp.getNombreCancion().equals(nombreActual)) {
                return (temp.getSiguiente() != null) ? temp.getSiguiente().getNombreCancion() : null;
            }
            temp = temp.getSiguiente();
        }
        return null;
    }

    public String getCancionAnterior(String nombreActual) {
        Nodo temp = cabeza;
        while (temp != null) {
            if (temp.getNombreCancion().equals(nombreActual)) {
                return (temp.getAnterior() != null) ? temp.getAnterior().getNombreCancion() : null;
            }
            temp = temp.getSiguiente();
        }
        return null;
    }

    public String getUltimaCancion(String nombreCancionActual) {
        if (vacia()) return null;
        Nodo temp = cabeza;
        while (temp.getSiguiente() != null) {
            temp = temp.getSiguiente();
        }
        return temp.getNombreCancion();
    }

    /* ========================
     *  ORDENAMIENTO
     * ======================== */

    public void ordenAlfabetico() {
        ordenarPorCriterio(Comparator.comparing(Nodo::getNombreCancion, String.CASE_INSENSITIVE_ORDER));
    }

    public void ordenFormato() {
        ordenarPorCriterio((a, b) -> {
            String extA = a.getRutaCancion().substring(a.getRutaCancion().lastIndexOf('.') + 1);
            String extB = b.getRutaCancion().substring(b.getRutaCancion().lastIndexOf('.') + 1);
            return extA.compareToIgnoreCase(extB);
        });
    }

    public void ordenArtista() {
        ordenarPorCriterio((a, b) -> obtenerArtista(a).compareToIgnoreCase(obtenerArtista(b)));
    }

    public void ordenDuracion() {
        ordenarPorCriterio(Comparator.comparingInt(this::obtenerDuracion));
    }

    private void ordenarPorCriterio(Comparator<Nodo> criterio) {
        if (vacia()) return;

        List<Nodo> nodos = new ArrayList<>();
        Nodo actual = cabeza;
        while (actual != null) {
            nodos.add(actual);
            actual = actual.getSiguiente();
        }

        nodos.sort(criterio);
        reconstruirLista(nodos);
    }

    private void reconstruirLista(List<Nodo> nodos) {
        cabeza = nodos.get(0);
        cabeza.setAnterior(null);

        for (int i = 1; i < nodos.size(); i++) {
            nodos.get(i - 1).setSiguiente(nodos.get(i));
            nodos.get(i).setAnterior(nodos.get(i - 1));
        }
        nodos.get(nodos.size() - 1).setSiguiente(null);
    }

    /**
     * Invierte el orden de la lista.
     */
    public void invertirLista() {
        if (vacia() || cabeza.getSiguiente() == null) return;

        Nodo actual = cabeza;
        Nodo previo = null;
        Nodo siguiente;

        while (actual != null) {
            siguiente = actual.getSiguiente();
            actual.setSiguiente(previo);
            actual.setAnterior(siguiente);
            previo = actual;
            actual = siguiente;
        }
        cabeza = previo;
    }

    /* ========================
     *  METADATOS
     * ======================== */

    private String obtenerArtista(Nodo nodo) {
        try {
            AudioFile audioFile = AudioFileIO.read(new File(nodo.getRutaCancion()));
            return audioFile.getTag().getFirst(FieldKey.ARTIST);
        } catch (Exception e) {
            return "Desconocido";
        }
    }

    private int obtenerDuracion(Nodo nodo) {
        try {
            AudioFile audioFile = AudioFileIO.read(new File(nodo.getRutaCancion()));
            return audioFile.getAudioHeader().getTrackLength();
        } catch (Exception e) {
            return 0;
        }
    }

    public String obtenerArtistaLegible(String ruta) {
        try {
            AudioFile audioFile = AudioFileIO.read(new File(ruta));
            String artista = audioFile.getTag().getFirst(FieldKey.ARTIST);
            return (artista != null && !artista.trim().isEmpty()) ? artista : "Desconocido";
        } catch (Exception e) {
            return "Desconocido";
        }
    }

    public String obtenerDuracionLegible(String rutaCancion) {
        try {
            AudioFile audioFile = AudioFileIO.read(new File(rutaCancion));
            int segundos = audioFile.getAudioHeader().getTrackLength();
            return String.format("%d:%02d", segundos / 60, segundos % 60);
        } catch (Exception e) {
            System.err.println("Error al obtener duración: " + e.getMessage());
            return "0:00";
        }
    }

    /**
     * Obtiene la imagen de portada de una canción.
     */
public Image obtenerPortada(String ruta) {
    try {
        AudioFile audioFile = AudioFileIO.read(new File(ruta));
        Tag tag = audioFile.getTag();
        if (tag != null && tag.getFirstArtwork() != null) {
            byte[] imagen = tag.getFirstArtwork().getBinaryData();
            return new Image(new ByteArrayInputStream(imagen));
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return null; // Imagen por defecto si falla
}

    /* ========================
     *  ACCESOR ADICIONAL
     * ======================== */

    public List<Cancion> getCanciones() {
        return canciones;
    }
}
