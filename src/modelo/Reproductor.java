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

    // Componentes de reproducción
    private MediaPlayer mediaPlayer;      // Reproductor de medios de JavaFX
    private String rutaActual;            // Ruta del archivo actualmente en reproducción
    private Timeline timelineTemporizador; // Temporizador para actualizar la UI
    private boolean repetirUna = false;    //Switch de repeticion una cancion
    // Propiedades observables para la UI
    private final StringProperty tiempoTranscurrido = new SimpleStringProperty("00:00");
    private final StringProperty tiempoTotal = new SimpleStringProperty("00:00");
    
    // Configuración de audio
    private double ultimoVolumen = 1.0;   // Volumen actual (0.0 a 1.0)
    private final SimpleDoubleProperty volumenProperty = new SimpleDoubleProperty(ultimoVolumen);
    private Runnable onEndOfMediaHandler;  //maneja el evento de finalizacion de cancion
    /* ***********************
     * CONSTRUCTOR E INICIALIZACIÓN
     * ***********************/

    /**
     * Constructor - Inicializa el reproductor y el temporizador.
     */
    public Reproductor() {
        this.ultimoVolumen = 1.0;
        this.volumenProperty.set(1.0);
        inicializarTemporizador();
        
        volumenProperty.addListener((obs, oldVal, newVal) -> {
            this.ultimoVolumen = newVal.doubleValue();
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(newVal.doubleValue());
            }
        });
    }

    /**
     * Configura el temporizador para actualizar los tiempos de reproducción.
     */
    private void inicializarTemporizador() {
        timelineTemporizador = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> actualizarTiempo())
        );
        timelineTemporizador.setCycleCount(Animation.INDEFINITE);
    }

    /* ***********************
     * MANEJO DE TIEMPOS
     * ***********************/

    /**
     * Actualiza las propiedades de tiempo para la UI.
     */
    private void actualizarTiempo() {
        if (mediaPlayer != null) {
            tiempoTranscurrido.set(formatearTiempo(mediaPlayer.getCurrentTime()));
            tiempoTotal.set(formatearTiempo(mediaPlayer.getTotalDuration()));
        }
    }

    /**
     * Formatea una duración a formato MM:SS.
     * @param tiempo Duración a formatear
     * @return String con el tiempo formateado
     */
    public String formatearTiempo(Duration tiempo) {
        if (tiempo == null || tiempo.isUnknown()) {
            return "0:00";
        }

        long segundosTotales = (long) Math.floor(tiempo.toSeconds());
        long minutos = segundosTotales / 60;
        long segundos = segundosTotales % 60;

        return String.format("%d:%02d", minutos, segundos);
    }

    /**
     * Obtiene la duración formateada como string 
     * @return 00:00 si n hay duracion y "3:15" en string que es la duracion de la cancion
     */
    public String getDuracionFormateada() {
        if (mediaPlayer != null && mediaPlayer.getTotalDuration() != null) {
            return formatearTiempo(mediaPlayer.getTotalDuration());
        }
        return "00:00";
    }

    /**
     * Reinicia completamente los tiempos
     */
    public void reiniciarTiempos() {
        tiempoTranscurrido.set("00:00");
        if (mediaPlayer != null && mediaPlayer.getTotalDuration() != null) {
            tiempoTotal.set(formatearTiempo(mediaPlayer.getTotalDuration()));
        } else {
            tiempoTotal.set("00:00");
        }
    }
    
    /* ***********************
     * PROPIEDADES PARA LA UI
     * ***********************/

    /**
     * Obtiene la propiedad observable del tiempo transcurrido.
     * @return StringProperty con formato MM:SS
     */
    public StringProperty tiempoTranscurridoProperty() {
        return tiempoTranscurrido;
    }

    /**
     * Obtiene la propiedad observable del tiempo total.
     * @return StringProperty con formato MM:SS
     */
    public StringProperty tiempoTotalProperty() {
        return tiempoTotal;
    }

    /* ***********************
     * CONTROL DE REPRODUCCIÓN
     * ***********************/

    /**
     * Reproduce un archivo de audio.
     * @param rutaCancion Ruta absoluta del archivo a reproducir
     */
    public void reproducir(String rutaCancion) {
        if (mediaPlayer != null && rutaCancion.equals(rutaActual)
                && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            return;
        }

        detener();

        try {
            Media archivoAudio = new Media(new File(rutaCancion).toURI().toString());
            mediaPlayer = new MediaPlayer(archivoAudio);
            rutaActual = rutaCancion;

            // Configurar el modo de repetición
            mediaPlayer.setCycleCount(repetirUna ? MediaPlayer.INDEFINITE : 1);

            mediaPlayer.setVolume(volumenProperty.get());

            mediaPlayer.setOnReady(() -> {
                tiempoTranscurrido.set("00:00");
                tiempoTotal.set(formatearTiempo(mediaPlayer.getTotalDuration()));
                timelineTemporizador.play();
                mediaPlayer.play();
            });

            // Configuración clave para el cambio automático de canción
            mediaPlayer.setOnEndOfMedia(() -> {
                Platform.runLater(() -> {
                    if (!repetirUna) {
                        // Notificar al controlador para cambiar de canción
                        if (onEndOfMediaHandler != null) {
                            onEndOfMediaHandler.run();
                        }
                    }
                });
            });

        } catch (Exception e) {
            System.err.println("Error al reproducir el archivo: " + e.getMessage());
        }
    }

     /**
     * Establece un manejador (handler) que se ejecutará cuando el medio (audio) 
     * llegue al final de su reproducción.
     * 
     * @param handler Objeto Runnable que contiene la lógica a ejecutar al finalizar el medio.
     *                Puede ser una lambda, una referencia a método o una clase que implemente Runnable.
     *                Ejemplo: () -> System.out.println("Reproducción finalizada");
     */
    public void setOnEndOfMedia(Runnable handler) {
        this.onEndOfMediaHandler = handler;
    }

    /**
     * Pausa la reproducción actual.
     */
    public void pausar() {
        if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
            timelineTemporizador.pause();
        }
    }

    /**
     * Reanuda la reproducción pausada.
     */
    public void reanudar() {
        if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PAUSED) {
            mediaPlayer.play();
            timelineTemporizador.play();
        }
    }

    /**
     * Detiene completamente la reproducción y libera recursos.
     */
    public void detener() {
        if (mediaPlayer != null) {
            timelineTemporizador.stop();
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
            rutaActual = null;
            tiempoTranscurrido.set("00:00");
            tiempoTotal.set("00:00");
        }
    }
    
    /**
     * hace que el media se reinicie util para mejorar la optimizacion
     */
    public void reiniciarReproduccion() {
        if (mediaPlayer != null) {
            mediaPlayer.seek(Duration.ZERO);
            reiniciarTiempos();
        }
    }

    /* ***********************
    * CONTROL DE REPETICIÓN
    * ***********************/

    /**
     * Activa/desactiva el modo de repetición para una canción
     * @param repetir true para activar repetición, false para desactivar
     */
    public void setModoRepeticion(boolean repetir) {
        this.repetirUna = repetir;
        if (mediaPlayer != null) {
            mediaPlayer.setCycleCount(repetir ? MediaPlayer.INDEFINITE : 1);
        }
    }

    /**
     * Verifica si el modo repetición está activado
     * @return true si está activado, false si no
     */
    public boolean isModoRepeticionActivo() {
        return repetirUna;
    }
    
    /* ***********************
     * CONFIGURACIÓN DE AUDIO
     * ***********************/

        
    /**
     * Obtiene la propiedad observable del volumen
     * @return propiedad de volumen
     */
    public DoubleProperty volumenProperty() {
        return volumenProperty;
    }
    
    /**
     * Obtiene el volumen actual
     * @return el volumen actual
     */
    public double getVolumenActual() {
        return volumenProperty.get();
    }
    
    /**
     * Establece el volumen de reproducción.
     *
     * @param volumen Valor entre 0.0 (silencio) y 1.0 (máximo)
     */
    public void setVolumen(double volumen) {
        this.ultimoVolumen = volumen;
        this.volumenProperty.set(volumen);
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volumen);
        }
    }
    
    /**
     * Establece la posición de reproducción
     *
     * @param duracion La nueva posición deseada
     */
    public void setPosicion(Duration duracion) {
        if (mediaPlayer != null && duracion != null) {
            mediaPlayer.seek(duracion);
        }
    }

    /* ***********************
     * MÉTODOS DE CONSULTA
     * ***********************/

    /**
     * Obtiene el MediaPlayer actual.
     * @return Instancia del MediaPlayer o null si no hay reproducción
     */
    public MediaPlayer getMediaPlayer() {
        return this.mediaPlayer;
    }

    /**
     * Obtiene la duración total del audio actual.
     * @return Duración o null si no hay reproducción
     */
    public Duration getDuracion() {
        return (mediaPlayer != null) ? mediaPlayer.getTotalDuration() : null;
    }

    /**
     * Obtiene el tiempo actual de reproducción.
     * @return Tiempo actual o null si no hay reproducción
     */
    public Duration getTiempoActual() {
        return (mediaPlayer != null) ? mediaPlayer.getCurrentTime() : null;
    }

    /**
     * Calcula el progreso de reproducción (0.0 a 1.0).
     * @return Progreso entre 0 (inicio) y 1 (fin)
     */
    public double getProgreso() {
        if (mediaPlayer != null && !mediaPlayer.getTotalDuration().isUnknown()) {
            return mediaPlayer.getCurrentTime().toMillis() / 
                   mediaPlayer.getTotalDuration().toMillis();
        }
        return 0;
    }
    
    /**
     * Obtiene el estado actual del modo repetición
     * @return true si está activado, false si no
     */
    public boolean getModoRepeticion() {
        return repetirUna;
    }    
}
