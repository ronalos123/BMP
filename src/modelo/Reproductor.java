package modelo;

/*
 * Reproductor.java - Controla la reproducción de archivos de audio.
 * Maneja la creación del MediaPlayer, control de reproducción, temporización
 * y propiedades observables para la interfaz de usuario.
 */

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import java.io.File;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

/**
 * Clase que gestiona la reproducción de archivos de audio.
 * Proporciona métodos para controlar la reproducción y propiedades
 * observables para sincronización con la interfaz gráfica.
 * 
 * @author Notasoft
 * @version 1.0
 */
public class Reproductor {

    // Reproducción
    private MediaPlayer mediaPlayer;                  // Reproductor de JavaFX
    private String rutaActual;                        // Ruta del archivo actual
    private Timeline timelineTemporizador;            // Temporizador para actualizar UI
    private boolean repetirUna = false;               // Modo repetición

    // Propiedades para la UI
    private final StringProperty tiempoTranscurrido = new SimpleStringProperty("00:00");
    private final StringProperty tiempoTotal       = new SimpleStringProperty("00:00");

    // Volumen
    private double ultimoVolumen                    = 1.0;
    private final SimpleDoubleProperty volumenProperty = new SimpleDoubleProperty(ultimoVolumen);

    // Manejador de fin de canción
    private Runnable onEndOfMediaHandler;

    /**
     * Constructor - Inicializa volumen y temporizador.
     */
    public Reproductor() {
        this.volumenProperty.set(1.0);
        inicializarTemporizador();

        // Escucha cambios en el volumen
        volumenProperty.addListener((obs, oldVal, newVal) -> {
            this.ultimoVolumen = newVal.doubleValue();
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(newVal.doubleValue());
            }
        });
    }

    /**
     * Inicializa el temporizador para actualizar el tiempo cada segundo.
     */
    private void inicializarTemporizador() {
        timelineTemporizador = new Timeline(
            new KeyFrame(Duration.seconds(1), e -> actualizarTiempo())
        );
        timelineTemporizador.setCycleCount(Animation.INDEFINITE);
    }

    /**
     * Actualiza el tiempo transcurrido y total en la interfaz.
     */
    private void actualizarTiempo() {
        if (mediaPlayer != null) {
            tiempoTranscurrido.set(formatearTiempo(mediaPlayer.getCurrentTime()));
            tiempoTotal.set(formatearTiempo(mediaPlayer.getTotalDuration()));
        }
    }

    /**
     * Convierte una duración a formato MM:SS.
     * @param tiempo Duración a formatear
     * @return Cadena con formato MM:SS
     */
    public String formatearTiempo(Duration tiempo) {
        if (tiempo == null || tiempo.isUnknown()) return "0:00";

        long segundosTotales = (long) Math.floor(tiempo.toSeconds());
        long minutos         = segundosTotales / 60;
        long segundos        = segundosTotales % 60;

        return String.format("%d:%02d", minutos, segundos);
    }

    /**
     * Obtiene la duración total del audio formateada.
     * @return Cadena con formato MM:SS
     */
    public String getDuracionFormateada() {
        return (mediaPlayer != null && mediaPlayer.getTotalDuration() != null)
            ? formatearTiempo(mediaPlayer.getTotalDuration())
            : "00:00";
    }

    /**
     * Reinicia los tiempos mostrados en la interfaz.
     */
    public void reiniciarTiempos() {
        tiempoTranscurrido.set("00:00");
        tiempoTotal.set("00:00");
    }

    // ======== PROPIEDADES OBSERVABLES ========

    public StringProperty tiempoTranscurridoProperty() { return tiempoTranscurrido; }
    public StringProperty tiempoTotalProperty()       { return tiempoTotal; }

    // ======== CONTROL DE REPRODUCCIÓN ========

    /**
     * Reproduce una canción especificada por su ruta.
     * @param rutaCancion Ruta absoluta del archivo
     */
    public void reproducir(String rutaCancion) {
        if (mediaPlayer != null && rutaCancion.equals(rutaActual)
                && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            return;
        }

        detener();

        try {
            Media media = new Media(new File(rutaCancion).toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            rutaActual  = rutaCancion;

            mediaPlayer.setVolume(volumenProperty.get());

            mediaPlayer.setOnReady(() -> {
                tiempoTranscurrido.set("00:00");
                tiempoTotal.set(formatearTiempo(mediaPlayer.getTotalDuration()));
                timelineTemporizador.play();
                mediaPlayer.play();
            });

            mediaPlayer.setOnEndOfMedia(() -> Platform.runLater(() -> {
                if (!repetirUna) {
                    if (onEndOfMediaHandler != null) onEndOfMediaHandler.run();
                } else {
                    reiniciarReproduccion();
                }
            }));

        } catch (Exception e) {
            System.err.println("Error al reproducir: " + e.getMessage());
        }
    }

    /**
     * Establece un manejador para cuando finalice la reproducción.
     * @param handler Acción a ejecutar
     */
    public void setOnEndOfMedia(Runnable handler) {
        this.onEndOfMediaHandler = handler;
    }

    /**
     * Pausa la reproducción.
     */
    public void pausar() {
        if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
            timelineTemporizador.pause();
        }
    }

    /**
     * Reanuda la reproducción si está pausada o detenida.
     */
    public void reanudar() {
        if (mediaPlayer != null) {
            switch (mediaPlayer.getStatus()) {
                case PAUSED:
                    mediaPlayer.play();
                    timelineTemporizador.play();
                    break;
                case STOPPED:
                case READY:
                    mediaPlayer.seek(mediaPlayer.getCurrentTime());
                    mediaPlayer.play();
                    timelineTemporizador.play();
                    break;
            }
        }
    }

    /**
     * Detiene la reproducción y libera recursos.
     */
    public void detener() {
        if (mediaPlayer != null) {
            timelineTemporizador.stop();
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
            rutaActual  = null;
            reiniciarTiempos();
        }
    }

    /**
     * Reinicia la reproducción desde el inicio.
     */
    public void reiniciarReproduccion() {
        if (mediaPlayer != null) {
            mediaPlayer.seek(Duration.ZERO);
            reiniciarTiempos();
        }
    }

    // ======== MODO REPETICIÓN ========

    public void setModoRepeticion(boolean repetir) {
        this.repetirUna = repetir;
        if (mediaPlayer != null) {
            mediaPlayer.setCycleCount(repetir ? MediaPlayer.INDEFINITE : 1);
        }
    }

    public boolean isModoRepeticionActivo() {
        return repetirUna;
    }

    public boolean getModoRepeticion() {
        return repetirUna;
    }

    // ======== CONFIGURACIÓN DE AUDIO ========

    public DoubleProperty volumenProperty() {
        return volumenProperty;
    }

    public double getVolumenActual() {
        return volumenProperty.get();
    }

    public void setVolumen(double volumen) {
        this.ultimoVolumen = volumen;
        volumenProperty.set(volumen);
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volumen);
        }
    }

    /**
     * Establece una posición específica de reproducción.
     * @param duracion Nueva posición
     */
    public void setPosicion(Duration duracion) {
        if (mediaPlayer != null && duracion != null) {
            mediaPlayer.seek(duracion);
        }
    }

    // ======== MÉTODOS DE CONSULTA ========

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public Duration getDuracion() {
        return (mediaPlayer != null) ? mediaPlayer.getTotalDuration() : null;
    }

    public Duration getTiempoActual() {
        return (mediaPlayer != null) ? mediaPlayer.getCurrentTime() : null;
    }

    public double getProgreso() {
        if (mediaPlayer != null && !mediaPlayer.getTotalDuration().isUnknown()) {
            return mediaPlayer.getCurrentTime().toMillis() / 
                   mediaPlayer.getTotalDuration().toMillis();
        }
        return 0;
    }
}
