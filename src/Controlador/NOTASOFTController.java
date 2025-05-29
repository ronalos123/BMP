package Controlador;

/*
 * NOTASOFTController - Controlador principal para la aplicación de reproducción musical.
 * Gestiona la interacción entre la vista y los modelos del reproductor.
 */
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import java.io.*;
import java.util.*;
import modelo.GestorDeListas;
import modelo.ListaReproduccion;
import modelo.Reproductor;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.util.Random;
import java.util.stream.Collectors;
import javafx.animation.Animation;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextInputDialog;
import javafx.scene.media.MediaPlayer;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import modelo.Cancion;
import vista.NOTASOFTView;

public class NOTASOFTController extends Application {

    // Componentes principales del controlador
    private GestorDeListas gestor;                              // Maneja las listas de reproducción
    private Reproductor reproductor;                            // Controla la reproducción de audio
    private NOTASOFTView vista;                                 // Interfaz gráfica
    private static final String ARCHIVO_LISTAS = "listas.dat";  // Archivo para persistencia
    private Timeline actualizadorProgreso;                      // Actualiza la barra de progreso
    private ObservableList<Cancion> listaCompletaCanciones = FXCollections.observableArrayList();
    /* ***********************
     * MÉTODOS PRINCIPALES
     * ***********************/
    @Override
    public void start(Stage primaryStage) {
        // Inicialización de componentes
        gestor = new GestorDeListas();
        reproductor = new Reproductor();
        vista = new NOTASOFTView(primaryStage);

        configurarBindings();

        // Cargar listas guardadas
        gestor.cargarListas(ARCHIVO_LISTAS);
        inicializarEventos();

        // Configurar acción al cerrar la ventana
        primaryStage.setOnCloseRequest(event -> {
            gestor.guardarListas(ARCHIVO_LISTAS);
            reproductor.detener();
            Platform.exit();
            System.exit(0);
        });
    primaryStage.setMinWidth(850);   
    primaryStage.setMinHeight(750);
        primaryStage.show();
    }

    /* ***********************
     * CONFIGURACIÓN DE VISTA
     * ***********************/
    /**
     * Configura los bindings entre el modelo y la vista
     */
    private void configurarBindings() {
        // Configurar el binding del volumen
        vista.getSliderVolumen().valueProperty().bindBidirectional(reproductor.volumenProperty());
        // Configurar barra de progreso interactiva
        configurarBarraProgresoInteractiva();

        // Configurar actualización automática
        actualizadorProgreso = new Timeline(
                new KeyFrame(Duration.millis(100), e -> {
                    if (reproductor.getMediaPlayer() != null
                    && !vista.BarraProgresoPresionado()) {
                        vista.getBarraProgreso().setProgress(reproductor.getProgreso());
                        vista.getTiempoTranscurridoLabel().setText(reproductor.formatearTiempo(reproductor.getMediaPlayer().getCurrentTime()));
                        vista.getTiempoTotalLabel().setText(reproductor.formatearTiempo(reproductor.getMediaPlayer().getTotalDuration()));
                    }
                })
        );
        actualizadorProgreso.setCycleCount(Animation.INDEFINITE);
        actualizadorProgreso.play();

        // Configurar el handler para cuando termine la canción
        reproductor.setOnEndOfMedia(() -> {
            if (!reproductor.getModoRepeticion()) {
                siguienteCancion();
            }
        });
    }

    /* ***********************
     * MANEJO DE EVENTOS
     * ***********************/
    /**
     * Inicializa los eventos de los controles de la interfaz
     */
    private void inicializarEventos() {
        // Configurar eventos de los botones
        vista.getBtnInvertir().setOnAction(e -> invertirListaActual());
        vista.getSelectorDeListas().setOnAction(e -> cargarListaSeleccionada());
        vista.getBtnNuevaLista().setOnAction(e -> crearNuevaLista());
        vista.getBtnEliminarLista().setOnAction(e -> eliminarListaActual());
        vista.getBtnAgregarCancion().setOnAction(e -> agregarCancionDesdeEscritorio());
        vista.getBtnAgregarCarpeta().setOnAction(e -> agregarCancionesDesdeCarpeta());
        vista.getBtnReproducir().setOnAction(e -> reproducirCancionSeleccionada());
        vista.getBtnPausa().setOnAction(e -> reproductor.pausar());
        vista.getBtnReanudar().setOnAction(e -> reproductor.reanudar());
        vista.getBtnEliminar().setOnAction(e -> eliminarCancion());
        vista.getBtnDetener().setOnAction(e -> detenerReproduccion());
        vista.getBtnSiguiente().setOnAction(e -> siguienteCancion());
        vista.getBtnAnterior().setOnAction(e -> anteriorCancion());
        vista.getBtnAleatorio().setOnAction(e -> reproducirAleatoria());
        vista.getBtnRepetirUna().setOnAction(e -> repetirUna());

        //Configurar bucle de repeticion
        vista.getBtnRepetirUna().selectedProperty().addListener((obs, oldVal, newVal) -> {
            actualizarEstiloBotonRepetir(newVal);
        });
        
        // Listener para reordenamiento manual (drag & drop)
        vista.getTablaCanciones().getItems().addListener((ListChangeListener<Cancion>) change -> {
            while (change.next()) {
                if (change.wasReplaced() || change.wasPermutated() || change.wasUpdated()) {
                    sincronizarOrdenLista();
                }
            }
        });
        // Cargar listas iniciales
        vista.getSelectorDeListas().getItems().addAll(gestor.getNombresDeListas());
        if (!vista.getSelectorDeListas().getItems().isEmpty()) {
            vista.getSelectorDeListas().setValue(vista.getSelectorDeListas().getItems().get(0));
            cargarListaSeleccionada();
        }

        // Configurar opciones de ordenamiento
        vista.getComboBoxOrdenar().setOnAction(e -> ordenador());

        // Configurar opciones de filtrado
        vista.getCampoBusqueda().textProperty().addListener((obs, oldVal, newVal) -> {
            buscarCancion();
        });
        
        configurarListenerReordenamiento();
    }

    /* ***********************
     * OPERACIONES CON LISTAS
     * ***********************/
    /**
     * Crea una nueva lista de reproducción
     */
    private void crearNuevaLista() {
        TextInputDialog dialogo = new TextInputDialog();
        dialogo.setTitle("Nueva Lista");
        dialogo.setHeaderText("Crear nueva lista de reproducción");
        dialogo.setContentText("Nombre:");

        Optional<String> resultado = dialogo.showAndWait();
        resultado.ifPresent(nombre -> {
            String nombreLimpio = nombre.trim(); // Elimina espacios en blanco al inicio y final
            if (nombreLimpio.isEmpty()) {
                vista.mostrarAlerta("El nombre de la lista no puede estar vacío.");
            } else if (!gestor.existeLista(nombreLimpio)) {
                gestor.crearLista(nombreLimpio);
                if (!vista.getSelectorDeListas().getItems().contains(nombreLimpio)) {
                    vista.getSelectorDeListas().getItems().add(nombreLimpio);
                    vista.getSelectorDeListas().setValue(nombreLimpio);
                }
                cargarListaSeleccionada();
            } else {
                vista.mostrarAlerta("La lista con ese nombre ya existe.");
            }
        });
    }

    /**
     * Carga la lista seleccionada en la vista
     */
    private void cargarListaSeleccionada() {
        String nombre = vista.getSelectorDeListas().getValue();
        if (nombre != null) {
            ListaReproduccion lista = gestor.getLista(nombre);
            listaCompletaCanciones.clear();

            if (lista != null) {
                for (String nombreCancion : lista.getNombresCanciones()) {
                    String ruta = lista.getRutaCancion(nombreCancion);
                    String duracion = lista.obtenerDuracionLegible(ruta);
                    listaCompletaCanciones.add(new Cancion(nombreCancion, duracion, ruta));
                }
            }

            // Mostrar la lista completa
            vista.getTablaCanciones().setItems(listaCompletaCanciones);
        }
    }

    /**
     * Elimina la lista actualmente seleccionada
     */
    private void eliminarListaActual() {
        String nombre = vista.getSelectorDeListas().getValue();
        if (nombre != null) {
            gestor.eliminarLista(nombre);
            vista.getSelectorDeListas().getItems().remove(nombre);
            vista.getTablaCanciones().getItems().clear();
            reproductor.detener();
            vista.getBarraProgreso().setProgress(0);

            if (!vista.getSelectorDeListas().getItems().isEmpty()) {
                vista.getSelectorDeListas().setValue(vista.getSelectorDeListas().getItems().get(0));
                cargarListaSeleccionada();
            } else {
                vista.mostrarAlerta("Ya no hay listas");
            }
        }
    }
    
    /**
     * Reordena la lista de reproducción según el nuevo orden en la tabla
     */
public void sincronizarOrdenLista() {
    String nombreLista = vista.getSelectorDeListas().getValue();
    if (nombreLista == null) return;

    ListaReproduccion lista = gestor.getLista(nombreLista);
    if (lista == null) return;

    // 1. Vaciar la lista actual
    lista.vaciarLista();

    // 2. Reconstruirla con el orden actual de la tabla
    for (Cancion cancion : vista.getTablaCanciones().getItems()) {
        lista.agregarCancion(cancion.getNombre(), cancion.getRuta());
    }
    gestor.guardarListas(ARCHIVO_LISTAS);
}
    
    /**
     * Configura el listener para detectar cambios en el orden de la tabla
     */
private void configurarListenerReordenamiento() {
    vista.getTablaCanciones().getItems().addListener((ListChangeListener<Cancion>) change -> {
        while (change.next()) {
            if (change.wasPermutated() || change.wasReplaced() || change.wasUpdated()) {
                Platform.runLater(() -> {
                    sincronizarOrdenLista();
                });
            }
        }
    });
}


    /* ***********************
     * OPERACIONES CON CANCIONES
     * ***********************/
    /**
     * Agrega una canción desde el sistema de archivos
     */
private void agregarCancionDesdeEscritorio() {
    String nombreLista = vista.getSelectorDeListas().getValue();
    if (nombreLista == null) {
        vista.mostrarAlerta("Selecciona o crea una lista primero.");
        return;
    }

    ListaReproduccion lista = gestor.getLista(nombreLista);
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Seleccionar Canciones");
    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos de Audio", "*.mp3", "*.wav", "*.m4a", "*.wma"));
    List<File> archivos = fileChooser.showOpenMultipleDialog(vista.getPrimaryStage());

    if (archivos != null) {
        for (File archivo : archivos) {
            String ruta = archivo.getAbsolutePath();
            String nombreCancion = archivo.getName();
            String duracion = lista.obtenerDuracionLegible(ruta);
            
            // Agregar solo una vez a la lista de reproducción
            lista.agregarCancion(nombreCancion, ruta);
            
            // Crear la canción una sola vez
            Cancion nuevaCancion = new Cancion(nombreCancion, duracion, ruta);
            
            // Agregar a ambas listas (la vista se actualizará automáticamente)
            listaCompletaCanciones.add(nuevaCancion);
        }
        // Actualizar la búsqueda para reflejar los cambios
        buscarCancion();
    } else {
        vista.mostrarAlerta("No se agregaron archivos de audio");
    }
}

    /**
     * Agrega todas las canciones de una carpeta
     */
private void agregarCancionesDesdeCarpeta() {
    String nombreLista = vista.getSelectorDeListas().getValue();
    if (nombreLista == null) {
        vista.mostrarAlerta("Selecciona o crea una lista primero.");
        return;
    }

    ListaReproduccion lista = gestor.getLista(nombreLista);
    DirectoryChooser chooser = new DirectoryChooser();
    chooser.setTitle("Seleccionar Carpeta de Canciones");
    File carpeta = chooser.showDialog(vista.getPrimaryStage());

    if (carpeta != null && carpeta.isDirectory()) {
        for (File archivo : carpeta.listFiles()) {
            if (archivo.isFile() && (archivo.getName().endsWith(".mp3")
                    || archivo.getName().endsWith(".wav")
                    || archivo.getName().endsWith(".m4a")) ){

                String ruta = archivo.getAbsolutePath();
                String nombreCancion = archivo.getName();
                String duracion = lista.obtenerDuracionLegible(ruta);
                
                lista.agregarCancion(nombreCancion, ruta);
                listaCompletaCanciones.add(new Cancion(nombreCancion, duracion, ruta));
            }
        }
    } else {
        vista.mostrarAlerta("No se agregó alguna carpeta con archivos de audio");
    }
}

    /* ***********************
     * CONTROL DE REPRODUCCIÓN
     * ***********************/
    /**
     * Reproduce la canción seleccionada
     */
    private void reproducirCancionSeleccionada() {
        // Verificar si hay lista seleccionada
        String listaActual = vista.getSelectorDeListas().getValue();
        if (listaActual == null) {
            vista.mostrarAlerta("No hay lista seleccionada");
            return;
        }

        // Obtener la selección actual
        Cancion seleccionada = vista.getTablaCanciones().getSelectionModel().getSelectedItem();

        // Si no hay selección pero hay canciones, seleccionar la primera
        if (seleccionada == null && !vista.getTablaCanciones().getItems().isEmpty()) {
            vista.getTablaCanciones().getSelectionModel().select(0);
            seleccionada = vista.getTablaCanciones().getItems().get(0);
        }

        // Reproducir la canción seleccionada
        if (seleccionada != null) {
            try {
                reproductor.reproducir(seleccionada.getRuta());
            } catch (Exception e) {
                vista.mostrarAlerta("Error al reproducir: " + e.getMessage());
                // Intenta pasar a la siguiente canción si hay error
                siguienteCancion();
            }
        } else {
            vista.mostrarAlerta("No hay canciones en la lista");
        }
    }

    /**
     * Reproduce una canción aleatoria de la lista
     */
    private void reproducirAleatoria() {
        ObservableList<Cancion> items = vista.getTablaCanciones().getItems();
        if (items.isEmpty()) {
            vista.mostrarAlerta("No hay canciones en la lista");
            return;
        }

        int randomIndex = new Random().nextInt(items.size());
        vista.getTablaCanciones().getSelectionModel().select(randomIndex);
        reproducirCancionSeleccionada();
    }

    /**
     * Pasa a la siguiente canción
     */
    private void siguienteCancion() {
        if (reproductor.getModoRepeticion()) {
            reiniciarCancionActual();
        } else {
            cambiarCancion(true);
        }
    }

    /**
     * Regresa a la canción anterior
     */
    private void anteriorCancion() {
        if (reproductor.getModoRepeticion()) {
            reiniciarCancionActual();
        } else {
            cambiarCancion(false);
        }
    }

    /**
     * Reinicia la reproducción de la canción actual
     */
    private void reiniciarCancionActual() {
        Cancion cancionActual = vista.getTablaCanciones().getSelectionModel().getSelectedItem();
        if (cancionActual != null) {
            double volumen = reproductor.getVolumenActual();
            boolean estabaReproduciendo = (reproductor.getMediaPlayer() != null
                    && reproductor.getMediaPlayer().getStatus() == MediaPlayer.Status.PLAYING);

            reproductor.reiniciarReproduccion();

            if (estabaReproduciendo) {
                reproductor.reanudar();
            }
            reproductor.setVolumen(volumen);
        }
    }

    /**
     * Cambia la canción (siguiente o anterior)
     *
     * @param siguiente true para siguiente canción, false para anterior
     */
    private void cambiarCancion(boolean siguiente) {
        String listaActual = vista.getSelectorDeListas().getValue();
        if (listaActual == null) {
            vista.mostrarAlerta("No hay lista seleccionada");
            return;
        }

        ListaReproduccion lista = gestor.getLista(listaActual);
        Cancion seleccionada = vista.getTablaCanciones().getSelectionModel().getSelectedItem();

        if (seleccionada == null && !vista.getTablaCanciones().getItems().isEmpty()) {
            // Si no hay selección pero hay canciones, seleccionar la primera
            vista.getTablaCanciones().getSelectionModel().select(0);
            seleccionada = vista.getTablaCanciones().getItems().get(0);
        }

        if (seleccionada != null) {
            // Obtener el nombre de la siguiente/anterior canción usando la lista enlazada
            String nombreNueva = siguiente
                    ? lista.getSiguienteCancion(seleccionada.getNombre())
                    : lista.getCancionAnterior(seleccionada.getNombre());

            if (nombreNueva != null) {
                // Buscar y seleccionar la canción en la TableView
                for (int i = 0; i < vista.getTablaCanciones().getItems().size(); i++) {
                    if (vista.getTablaCanciones().getItems().get(i).getNombre().equals(nombreNueva)) {
                        vista.getTablaCanciones().getSelectionModel().select(i);
                        reproducirCancionSeleccionada();
                        break;
                    }
                }
            } else {
                // Si no hay siguiente/anterior, reproducir la primera/última
                int index = siguiente ? 0 : vista.getTablaCanciones().getItems().size() - 1;
                vista.getTablaCanciones().getSelectionModel().select(index);
                reproducirCancionSeleccionada();
            }
        }
    }

    /**
     * Detiene la reproducción actual
     */
    private void detenerReproduccion() {
        reproductor.detener();
        vista.getBarraProgreso().setProgress(0);
        if (actualizadorProgreso != null) {
            actualizadorProgreso.stop();
        }
    }

    /**
     * Elimina la canción seleccionada
     */
    private void eliminarCancion() {
        String nombreLista = vista.getSelectorDeListas().getValue();
        Cancion seleccionada = vista.getTablaCanciones().getSelectionModel().getSelectedItem();

        if (nombreLista != null && seleccionada != null) {
            ListaReproduccion lista = gestor.getLista(nombreLista);
            lista.eliminarCancion(seleccionada.getNombre());
            vista.getTablaCanciones().getItems().remove(seleccionada);
            detenerReproduccion();
            listaCompletaCanciones.removeIf(c -> c.getNombre().equals(seleccionada.getNombre()));
            buscarCancion();
        }
    }

    /**
     * Activa/desactiva el modo de repetición para la canción actual
     */
    private void repetirUna() {
        boolean repetir = vista.getBtnRepetirUna().isSelected();
        reproductor.setModoRepeticion(repetir);
        actualizarEstiloBotonRepetir(repetir);

        // Configurar el manejador de fin de reproducción solo una vez
        if (reproductor.getMediaPlayer() != null) {
            reproductor.getMediaPlayer().setOnEndOfMedia(() -> {
                Platform.runLater(() -> {
                    if (!reproductor.getModoRepeticion()) {
                        siguienteCancion();
                    }
                });
            });
        }
    }

    /**
     * Actualiza el estilo visual del botón de repetición
     */
    private void actualizarEstiloBotonRepetir(boolean activado) {
        String estilo = activado
                ? "-fx-background-color: #4CAF50; -fx-text-fill: white;"
                : "-fx-background-color: #9E9E9E; -fx-text-fill: white;";
        vista.getBtnRepetirUna().setStyle(estilo);
    }

    /* ***********************
     * ORDENAMIENTO BÚSQUEDA Y FILTRADO
     * ***********************/
    /**
     * Ordena por lo que requiera el usuario
     */
    private void ordenador() {
        String opcion = vista.getComboBoxOrdenar().getValue();
        String listaActual = vista.getSelectorDeListas().getValue();

        if (opcion == null || listaActual == null) {
            return;
        }

        ListaReproduccion lista = gestor.getLista(listaActual);
        switch (opcion) {
            case "Ordenar por Nombre (A-Z)":
                lista.ordenAlfabetico();
                break;
            case "Ordenar por Formato":
                lista.ordenFormato();
                break;
            case "Ordenar por Artista":
                lista.ordenArtista();
                break;
            case "Ordenar por Duración":
                lista.ordenDuracion();
                break;
        }

        // Actualizar la vista
        cargarListaSeleccionada();
    }

    /**
     * Busca canciones según el texto ingresado
     */
    private void buscarCancion() {
        String textoBusqueda = vista.getCampoBusqueda().getText().trim().toLowerCase();

        if (textoBusqueda.isEmpty()) {
            // Mostrar toda la lista si no hay texto de búsqueda
            vista.getTablaCanciones().setItems(listaCompletaCanciones);
        } else {
            // Filtrar contra la lista completa
            ObservableList<Cancion> resultados = listaCompletaCanciones.stream()
                    .filter(cancion -> cancion.getNombre().toLowerCase().contains(textoBusqueda))
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));

            vista.getTablaCanciones().setItems(resultados);
        }
    }


    /* ***********************
     * OPERACIONES ADICIONALES
     * ***********************/
    /**
     * Invierte el orden de la lista actual
     */
    private void invertirListaActual() {
        String nombreLista = vista.getSelectorDeListas().getValue();
        if (nombreLista == null) {
            vista.mostrarAlerta("Selecciona una lista primero.");
            return;
        }

        ListaReproduccion lista = gestor.getLista(nombreLista);
        if (lista != null) {
            lista.invertirLista();
            cargarListaSeleccionada();
        }
    }

    /**
     * Configura la barra de progreso para ser interactiva
     */
    private void configurarBarraProgresoInteractiva() {
        ProgressBar barra = vista.getBarraProgreso();

        // Manejar clic en la barra de progreso
        barra.setOnMousePressed(event -> {
            if (reproductor.getMediaPlayer() != null) {
                double progreso = event.getX() / barra.getWidth();
                progreso = Math.max(0, Math.min(1, progreso)); // Asegurar rango 0-1

                // Actualizar posición del reproductor
                Duration nuevaPosicion = reproductor.getMediaPlayer().getTotalDuration().multiply(progreso);
                reproductor.getMediaPlayer().seek(nuevaPosicion);

                // Actualizar UI inmediatamente
                vista.getBarraProgreso().setProgress(progreso);
                vista.getTiempoTranscurridoLabel().setText(reproductor.formatearTiempo(nuevaPosicion));
            }
        });

        // Manejar arrastre
        barra.setOnMouseDragged(event -> {
            if (reproductor.getMediaPlayer() != null) {
                double progreso = event.getX() / barra.getWidth();
                progreso = Math.max(0, Math.min(1, progreso)); // Asegurar rango 0-1

                // Actualizar posición del reproductor durante arrastre
                Duration nuevaPosicion = reproductor.getMediaPlayer().getTotalDuration().multiply(progreso);
                reproductor.getMediaPlayer().seek(nuevaPosicion);

                // Actualizar UI durante arrastre
                vista.getBarraProgreso().setProgress(progreso);
                vista.getTiempoTranscurridoLabel().setText(reproductor.formatearTiempo(nuevaPosicion));
            }
        });
    }
}
