package modelo;

/**
 * GestorDeListas se encarga de manejar las operaciones de las listas de
 * reproducción. Permite crear, eliminar, renombrar listas, y manipular
 * canciones dentro de ellas.
 *
 * Características principales: - Almacena listas de reproducción en un mapa
 * usando nombres como clave - Proporciona operaciones CRUD para listas y
 * canciones - Soporta serialización para guardar/recuperar listas de archivos -
 * Integra con la clase ListaReproduccion para el manejo interno de canciones
 */
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class GestorDeListas implements Serializable {

    private static final long serialVersionUID = 1L; // Versión para serialización
private ListaReproduccion listaFav = new ListaReproduccion();
    // Mapa que almacena las listas de reproducción (nombre -> lista)
    private Map<String, ListaReproduccion> listas;

    /**
     * Constructor - Inicializa un mapa vacío para almacenar listas
     */
    public GestorDeListas() {
        listas = new HashMap<>();
    }

    /* ***********************
     * OPERACIONES CON LISTAS
     * ***********************/
    /**
     * Crea una nueva lista de reproducción vacía
     *
     * @param nombre Nombre de la nueva lista
     */
    public void crearLista(String nombre) {
        if (!listas.containsKey(nombre)) {
            listas.put(nombre, new ListaReproduccion());
        } else {
            System.out.println("La lista '" + nombre + "' ya existe.");
        }
    }

    /**
     * Elimina una lista de reproducción existente
     *
     * @param nombre Nombre de la lista a eliminar
     */
    public void eliminarLista(String nombre) {
        if (listas.containsKey(nombre)) {
            listas.remove(nombre);
        } else {
            System.out.println("La lista '" + nombre + "' no existe.");
        }
    }

    /**
     * Renombra una lista de reproducción
     *
     * @param nombreAntiguo Nombre actual de la lista
     * @param nombreNuevo Nuevo nombre para la lista
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
     * Verifica si existe una lista con el nombre especificado
     *
     * @param nombre Nombre de la lista a verificar
     * @return true si existe, false si no
     */
    public boolean existeLista(String nombre) {
        return listas.containsKey(nombre);
    }

    /* ***********************
     * OPERACIONES CON CANCIONES
     * ***********************/
    /**
     * Agrega una canción a una lista específica
     *
     * @param nombreLista Lista destino
     * @param nombreCancion Nombre de la canción
     * @param rutaCancion Ruta del archivo de audio
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
     * Elimina una canción de una lista específica
     *
     * @param nombreLista Lista que contiene la canción
     * @param nombreCancion Nombre de la canción a eliminar
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

    /* ***********************
     * CONSULTAS
     * ***********************/
    /**
     * Obtiene una lista de reproducción por su nombre
     *
     * @param nombre Nombre de la lista
     * @return Objeto ListaReproduccion o null si no existe
     */
    public ListaReproduccion getLista(String nombre) {
        return listas.get(nombre);
    }

    /**
     * Obtiene los nombres de todas las canciones en una lista
     *
     * @param nombreLista Lista a consultar
     * @return Lista de nombres o null si la lista no existe
     */
    public List<String> getNombresCanciones(String nombreLista) {
        ListaReproduccion lista = getLista(nombreLista);
        return lista != null ? lista.getNombresCanciones() : null;
    }

    /**
     * Obtiene todas las listas de reproducción
     *
     * @return Mapa con todas las listas (nombre -> lista)
     */
    public Map<String, ListaReproduccion> getListas() {
        return listas;
    }

    /**
     * Obtiene los nombres de todas las listas existentes
     *
     * @return Lista de nombres de listas
     */
    public List<String> getNombresDeListas() {
        return new ArrayList<>(listas.keySet());
    }

    /* ***********************
     * PERSISTENCIA
     * ***********************/
    /**
     * Guarda todas las listas en un archivo
     *
     * @param archivo Ruta del archivo destino
     */
    public void guardarListas(String archivo) {
        listas.put("Favoritos",listaFav);
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(archivo))) {
            out.writeObject(listas);
        } catch (IOException e) {
            System.out.println("Error al guardar las listas: " + e.getMessage());
        }
    }

    /**
     * Carga listas desde un archivo
     *
     * @param archivo Ruta del archivo fuente
     */
@SuppressWarnings("unchecked")
public void cargarListas(String archivo) {
    try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(archivo))) {
        listas = (Map<String, ListaReproduccion>) in.readObject();

        // Recuperar lista de favoritos si está presente
        if (listas.containsKey("Favoritos")) {
            listaFav = listas.get("Favoritos"); // Referencia directa (sin eliminarla del mapa)
        } else {
            listaFav = new ListaReproduccion();
            listas.put("Favoritos", listaFav); // 💡 Asegúrate de agregarla si no estaba
        }

        System.out.println("Listas cargadas: " + listas.keySet());
    } catch (IOException | ClassNotFoundException e) {
        System.out.println("Error al cargar las listas: " + e.getMessage());
    }
}
    
    
    //Anghelo
    public boolean existeCancionEnLista(String nombreLista, String ruta) {
    ListaReproduccion lista = listas.get(nombreLista);
    if (lista == null) return false;

    List<Cancion> canciones = lista.getCanciones();
    if (canciones == null) return false;

    return canciones.stream().anyMatch(c ->
        c != null && c.getRuta() != null && c.getRuta().equals(ruta)
    );
}
//Xavier-Greco
public void agregarFav(Cancion cancion){
        if(!esFavorita(cancion)){
            if(listaFav.getRutaPorNombre(cancion.getNombre())==null){
            listaFav.agregarCancion(cancion.getNombre(), cancion.getRuta());
                System.out.println(listaFav);
            }else{
                return;
            }
         }
    }
    public void eliminarFav(Cancion cancion){
        listaFav.eliminarCancion(cancion.getNombre());
        System.out.println(listaFav);
    }
    public boolean esFavorita(Cancion cancion){
        if(listaFav.getRutaPorNombre(cancion.getNombre())!=null){
            return true;
        }else{
            return false;
        }
    }


}