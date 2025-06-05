package modelo;

import java.io.*;
import java.util.*;

/**
 * GestorDeListas se encarga de manejar las operaciones de las listas de
 * reproducción. Permite crear, eliminar, renombrar listas, y manipular
 * canciones dentro de ellas.
 *
 * Características principales:
 * - Almacena listas de reproducción en un mapa usando nombres como clave
 * - Proporciona operaciones CRUD para listas y canciones
 * - Soporta serialización para guardar/recuperar listas de archivos
 * - Integra con la clase ListaReproduccion para el manejo interno de canciones
 */
public class GestorDeListas implements Serializable {

    private static final long serialVersionUID = 1L;

    private Map<String, ListaReproduccion> listas;  // Mapa que almacena listas de reproducción
    private ListaReproduccion listaFav = new ListaReproduccion(); // Lista especial de favoritos

    /**
     * Constructor - Inicializa el mapa de listas.
     */
    public GestorDeListas() {
        listas = new HashMap<>();
    }

    /* ***********************
     * OPERACIONES CON LISTAS
     * ***********************/

    /**
     * Crea una nueva lista de reproducción vacía si no existe ya.
     */
    public void crearLista(String nombre) {
        if (!listas.containsKey(nombre)) {
            listas.put(nombre, new ListaReproduccion());
        } else {
            System.out.println("La lista '" + nombre + "' ya existe.");
        }
    }

    /**
     * Elimina una lista de reproducción existente.
     */
    public void eliminarLista(String nombre) {
        if (listas.containsKey(nombre)) {
            listas.remove(nombre);
        } else {
            System.out.println("La lista '" + nombre + "' no existe.");
        }
    }

    /**
     * Renombra una lista de reproducción si existe.
     */
    public void renombrarLista(String nombreAntiguo, String nombreNuevo) {
        if (listas.containsKey(nombreAntiguo)) {
            ListaReproduccion lista = listas.remove(nombreAntiguo);
            listas.put(nombreNuevo, lista);
        } else {
            System.out.println("La lista '" + nombreAntiguo + "' no existe.");
        }
    }

    /**
     * Verifica si una lista con el nombre dado existe.
     */
    public boolean existeLista(String nombre) {
        return listas.containsKey(nombre);
    }

    /* ***************************
     * OPERACIONES CON CANCIONES
     * ***************************/

    /**
     * Agrega una canción a la lista indicada.
     */
    public void agregarCancionALista(String nombreLista, String nombreCancion, String rutaCancion) {
        ListaReproduccion lista = getLista(nombreLista);
        if (lista != null) {
            lista.agregarCancion(nombreCancion, rutaCancion);
        } else {
            System.out.println("La lista '" + nombreLista + "' no existe.");
        }
    }

    /**
     * Elimina una canción de la lista indicada.
     */
    public void eliminarCancionDeLista(String nombreLista, String nombreCancion) {
        ListaReproduccion lista = getLista(nombreLista);
        if (lista != null) {
            if (!lista.eliminarCancion(nombreCancion)) {
                System.out.println("La canción '" + nombreCancion + "' no se encuentra en la lista '" + nombreLista + "'.");
            }
        } else {
            System.out.println("La lista '" + nombreLista + "' no existe.");
        }
    }

    /* ***************
     * CONSULTAS
     * ***************/

    /**
     * Devuelve la lista de reproducción por su nombre.
     */
    public ListaReproduccion getLista(String nombre) {
        return listas.get(nombre);
    }

    /**
     * Devuelve los nombres de las canciones en una lista.
     */
    public List<String> getNombresCanciones(String nombreLista) {
        ListaReproduccion lista = getLista(nombreLista);
        return lista != null ? lista.getNombresCanciones() : null;
    }

    /**
     * Devuelve el mapa completo de listas.
     */
    public Map<String, ListaReproduccion> getListas() {
        return listas;
    }

    /**
     * Devuelve los nombres de todas las listas creadas.
     */
    public List<String> getNombresDeListas() {
        return new ArrayList<>(listas.keySet());
    }

    /**
     * Devuelve el número de canciones en una lista.
     */
    public int nroDeMusicasEn(String nombreLista) {
        ListaReproduccion lista = listas.get(nombreLista);
        return (lista != null) ? lista.contarCanciones() : 0;
    }

    /**
     * Verifica si una canción ya está en la lista.
     */
    public boolean existeCancionEnLista(String nombreLista, String ruta) {
        ListaReproduccion lista = listas.get(nombreLista);
        if (lista == null) return false;

        List<Cancion> canciones = lista.getCanciones();
        if (canciones == null) return false;

        return canciones.stream().anyMatch(c ->
            c != null && c.getRuta() != null && c.getRuta().equals(ruta)
        );
    }

    /* ***************
     * FAVORITOS
     * ***************/

    /**
     * Agrega una canción a la lista de favoritos si no está ya.
     */
    public void agregarFav(Cancion cancion) {
        if (!esFavorita(cancion)) {
            if (listaFav.getRutaPorNombre(cancion.getNombre()) == null) {
                listaFav.agregarCancion(cancion.getNombre(), cancion.getRuta());
            }
        }
    }

    /**
     * Elimina una canción de la lista de favoritos.
     */
    public void eliminarFav(Cancion cancion) {
        listaFav.eliminarCancion(cancion.getNombre());
    }

    /**
     * Verifica si una canción es favorita.
     */
    public boolean esFavorita(Cancion cancion) {
        return listaFav.getRutaPorNombre(cancion.getNombre()) != null;
    }

    /* **********************
     * PERSISTENCIA
     * **********************/

    /**
     * Guarda todas las listas (incluye favoritos) en un archivo.
     */
    public void guardarListas(String archivo) {
        listas.put("Favoritos", listaFav);
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(archivo))) {
            out.writeObject(listas);
        } catch (IOException e) {
            System.out.println("Error al guardar las listas: " + e.getMessage());
        }
    }

    /**
     * Carga todas las listas desde un archivo. Restaura favoritos si está presente.
     */
    @SuppressWarnings("unchecked")
    public void cargarListas(String archivo) {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(archivo))) {
            listas = (Map<String, ListaReproduccion>) in.readObject();

            // Recupera o crea la lista de favoritos
            if (listas.containsKey("Favoritos")) {
                listaFav = listas.get("Favoritos");
            } else {
                listaFav = new ListaReproduccion();
                listas.put("Favoritos", listaFav);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error al cargar las listas: " + e.getMessage());
        }
    }
}
