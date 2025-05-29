package modelo;

import java.io.Serializable;
import javafx.util.Duration;

public class SesionGuardada implements Serializable {
    private static final long serialVersionUID = 1L;

    private String listaActual;
    private String cancionActual;
    private double tiempoTranscurrido; // en milisegundos
    private double volumen;
    private boolean reproduciendo;
    private boolean pausado;

    public SesionGuardada(String listaActual, String cancionActual, double tiempoTranscurrido, double volumen, boolean reproduciendo, boolean pausado) {
        this.listaActual = listaActual;
        this.cancionActual = cancionActual;
        this.tiempoTranscurrido = tiempoTranscurrido;
        this.volumen = volumen;
        this.reproduciendo = reproduciendo;
        this.pausado = pausado;
    }

    public String getListaActual() { return listaActual; }
    public String getCancionActual() { return cancionActual; }
    public double getTiempoTranscurrido() { return tiempoTranscurrido; }
    public double getVolumen() { return volumen; }
    public boolean isReproduciendo() { return reproduciendo; }
    public boolean isPausado() { return pausado; }
}