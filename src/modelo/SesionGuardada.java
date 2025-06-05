package modelo;

import java.io.Serializable;

/**
 * Clase que representa una sesión guardada de reproducción.
 * Contiene información del estado actual del reproductor para
 * poder restaurarlo posteriormente.
 * 
 * @author Notasoft
 * @version 1.0
 */
public class SesionGuardada implements Serializable {

    private static final long serialVersionUID = 1L;

    // Nombre de la lista de reproducción activa
    private final String listaActual;

    // Nombre de la canción actual reproducida
    private final String cancionActual;

    // Tiempo transcurrido en milisegundos en la canción actual
    private final double tiempoTranscurrido;

    // Volumen actual (rango 0.0 a 1.0)
    private final double volumen;

    // Indica si el reproductor estaba reproduciendo al guardar sesión
    private final boolean reproduciendo;

    // Indica si el reproductor estaba pausado al guardar sesión
    private final boolean pausado;

    /**
     * Constructor que inicializa una sesión guardada con el estado del reproductor.
     * 
     * @param listaActual      Nombre de la lista de reproducción activa
     * @param cancionActual    Nombre de la canción actualmente reproducida
     * @param tiempoTranscurrido Tiempo transcurrido en milisegundos en la canción actual
     * @param volumen          Volumen actual del reproductor
     * @param reproduciendo    Estado si estaba reproduciendo
     * @param pausado          Estado si estaba pausado
     */
    public SesionGuardada(String listaActual, String cancionActual, double tiempoTranscurrido, 
                          double volumen, boolean reproduciendo, boolean pausado) {
        this.listaActual = listaActual;
        this.cancionActual = cancionActual;
        this.tiempoTranscurrido = tiempoTranscurrido;
        this.volumen = volumen;
        this.reproduciendo = reproduciendo;
        this.pausado = pausado;
    }

    /** 
     * Obtiene el nombre de la lista de reproducción activa.
     * @return Nombre de la lista actual
     */
    public String getListaActual() {
        return listaActual;
    }

    /** 
     * Obtiene el nombre de la canción actual reproducida.
     * @return Nombre de la canción actual
     */
    public String getCancionActual() {
        return cancionActual;
    }

    /**
     * Obtiene el tiempo transcurrido en milisegundos en la canción actual.
     * @return Tiempo transcurrido en ms
     */
    public double getTiempoTranscurrido() {
        return tiempoTranscurrido;
    }

    /**
     * Obtiene el volumen actual del reproductor.
     * @return Volumen entre 0.0 y 1.0
     */
    public double getVolumen() {
        return volumen;
    }

    /**
     * Indica si el reproductor estaba reproduciendo cuando se guardó la sesión.
     * @return true si estaba reproduciendo
     */
    public boolean isReproduciendo() {
        return reproduciendo;
    }

    /**
     * Indica si el reproductor estaba pausado cuando se guardó la sesión.
     * @return true si estaba pausado
     */
    public boolean isPausado() {
        return pausado;
    }
}
