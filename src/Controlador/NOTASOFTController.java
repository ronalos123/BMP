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
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextInputDialog;
import javafx.scene.media.AudioClip;
import javafx.scene.media.MediaPlayer;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import modelo.Cancion;
import modelo.SesionGuardada;
import vista.NOTASOFTView;

public class NOTASOFTController extends Application {
private final AudioClip PopBurbuja = new AudioClip(getClass().getResource("/resources/sonidos/BurbujaPop.mp3").toExternalForm());
    // Componentes principales del controlador
    private GestorDeListas gestor;                              // Maneja las listas de reproducción
    private Reproductor reproductor;                            // Controla la reproducción de audio
    private NOTASOFTView vista;                                 // Interfaz gráfica
    private static final String ARCHIVO_LISTAS = "listas.dat";  // Archivo para persistencia
    private Timeline actualizadorProgreso;                      // Actualiza la barra de progreso
private static final String ARCHIVO_SESION = "sesion.dat";
private SesionGuardada sesionGuardada;
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
        cargarSesion(); // Cargar sesión previa


        // Configurar acción al cerrar la ventana
        primaryStage.setOnCloseRequest(event -> {
            gestor.guardarListas(ARCHIVO_LISTAS);
            guardarSesion();
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
        vista.getBtnFavorito().setOnAction(e -> favorito());
        vista.getBtnmostrarFavoritos().setOnAction(e -> mostrarFavoritos());
vista.getTablaCanciones().setOnMouseClicked(e -> {
    if (e.getClickCount() == 1) {
        reproducirAlTocar();
    }
});
        //angelo
        vista.getClasificar().setOnAction(e -> {
            Stage stage = (Stage) vista.getClasificar().getScene().getWindow();
            clasificarCancionPorMetadatos(stage);
        });
        vista.aplicarEfectoBoton(vista.getBtnAnterior());
        vista.aplicarEfectoBoton(vista.getBtnSiguiente());
        vista.aplicarEfectoBoton(vista.getBtnReproducir());
        vista.aplicarEfectoBoton(vista.getBtnPausa());
        vista.aplicarEfectoBoton(vista.getBtnReanudar());
                vista.aplicarEfectoBoton(vista.getBtnEliminar());
                vista.aplicarEfectoBoton(vista.getBtnAleatorio());
                vista.aplicarEfectoBoton(vista.getBtnAgregarCancion());
                vista.aplicarEfectoBoton(vista.getBtnAgregarCarpeta());
                vista.aplicarEfectoBoton(vista.getClasificar());
                vista.aplicarEfectoBoton(vista.getBtnNuevaLista());
                vista.aplicarEfectoBoton(vista.getBtnEliminarLista());
                vista.aplicarEfectoBoton(vista.getBtnInvertir());
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
                if(nombre.equals("Favoritos")){
         vista.getBtnAgregarCancion().setDisable(true);
        vista.getBtnAgregarCarpeta().setDisable(true);
        vista.getBtnEliminarLista().setDisable(true);
       vista.getBtnFavorito().setDisable(true);

                }else{
                  vista.getBtnAgregarCancion().setDisable(false);
        vista.getBtnAgregarCarpeta().setDisable(false);
        vista.getBtnEliminarLista().setDisable(false);
        vista.getBtnFavorito().setDisable(false); 

                }
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
        boolean confirmado = vista.mostrarConfirmacion("¿Deseas eliminar la lista: " + nombre + "? Esta acción no se puede deshacer.");

        if (confirmado) {
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
        } else {
            vista.mostrarAlerta("Eliminación cancelada.");
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
    String listaActual = vista.getSelectorDeListas().getValue();
    if (listaActual == null) {
        vista.mostrarAlerta("No hay lista seleccionada");
        return;
    }
    Cancion seleccionada = vista.getTablaCanciones().getSelectionModel().getSelectedItem();
    if (seleccionada == null && !vista.getTablaCanciones().getItems().isEmpty()) {
        vista.getTablaCanciones().getSelectionModel().select(0);
        seleccionada = vista.getTablaCanciones().getItems().get(0);
    }
    if (seleccionada != null) {
        if(gestor.esFavorita(seleccionada)){
            vista.getBtnFavorito().setSelected(true);
        }else{
        vista.getBtnFavorito().setSelected(false);
        }
        try {
            // Si ya había un reproductor activo, lo detenemos
            reproductor.detener();
            vista.getNombrePresentacion().setText(seleccionada.getNombre());
            // Reiniciamos el Timeline si es necesario
            inicializarTimelineSiNecesario();

            // Iniciar reproducción de la nueva canción
            reproductor.reproducir(seleccionada.getRuta());
        } catch (Exception e) {
            vista.mostrarAlerta("Error al reproducir: " + e.getMessage());
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
    reproductor.detener();  // Llama al detener real del Reproductor
    vista.getBarraProgreso().setProgress(0);
    vista.getTiempoTranscurridoLabel().setText("00:00");
    vista.getTiempoTotalLabel().setText("00:00");

    if (actualizadorProgreso != null) {
        actualizadorProgreso.stop();
    }
}
// Método auxiliar para reiniciar el Timeline si fue detenido
private void inicializarTimelineSiNecesario() {
    if (actualizadorProgreso == null) {
        actualizadorProgreso = new Timeline(
                new KeyFrame(Duration.millis(100), e -> {
                    if (reproductor.getMediaPlayer() != null && !vista.BarraProgresoPresionado()) {
                        vista.getBarraProgreso().setProgress(reproductor.getProgreso());
                        vista.getTiempoTranscurridoLabel().setText(reproductor.formatearTiempo(reproductor.getMediaPlayer().getCurrentTime()));
                        vista.getTiempoTotalLabel().setText(reproductor.formatearTiempo(reproductor.getMediaPlayer().getTotalDuration()));
                    }
                })
        );
        actualizadorProgreso.setCycleCount(Animation.INDEFINITE);
    } else if (actualizadorProgreso.getStatus() == Animation.Status.STOPPED) {
        actualizadorProgreso.getKeyFrames().clear();
        actualizadorProgreso.getKeyFrames().add(new KeyFrame(Duration.millis(100), e -> {
            if (reproductor.getMediaPlayer() != null && !vista.BarraProgresoPresionado()) {
                vista.getBarraProgreso().setProgress(reproductor.getProgreso());
                vista.getTiempoTranscurridoLabel().setText(reproductor.formatearTiempo(reproductor.getMediaPlayer().getCurrentTime()));
                vista.getTiempoTotalLabel().setText(reproductor.formatearTiempo(reproductor.getMediaPlayer().getTotalDuration()));
            }
        }));
        actualizadorProgreso.setCycleCount(Animation.INDEFINITE);
    }
    actualizadorProgreso.play();
}

    /**
     * Elimina la canción seleccionada
     */
private void eliminarCancion() {
    String nombreLista = vista.getSelectorDeListas().getValue();
    Cancion seleccionada = vista.getTablaCanciones().getSelectionModel().getSelectedItem();

    if (nombreLista != null && seleccionada != null) {
        boolean confirmado = vista.mostrarConfirmacion("¿Deseas eliminar la canción: " + seleccionada.getNombre() + "?");

        if (!confirmado) {
            vista.mostrarAlerta("Eliminación cancelada.");
            return;
        }

        ListaReproduccion lista = gestor.getLista(nombreLista);
        ObservableList<Cancion> canciones = vista.getTablaCanciones().getItems();
        int indiceActual = canciones.indexOf(seleccionada);

        boolean esLaCancionActual = false;

        if (reproductor.getMediaPlayer() != null) {
            String rutaReproductor = reproductor.getMediaPlayer().getMedia().getSource();
            String rutaCancionSeleccionada = new File(seleccionada.getRuta()).toURI().toString();
            esLaCancionActual = rutaReproductor.equals(rutaCancionSeleccionada);
        }

        if (lista.eliminarCancion(seleccionada.getNombre())) {
            canciones.remove(seleccionada);
            vista.getNombrePresentacion().setText("");
            listaCompletaCanciones.removeIf(c -> c.getNombre().equals(seleccionada.getNombre()));
            buscarCancion();

            if (esLaCancionActual) {
                reproductor.detener();
                vista.getBarraProgreso().setProgress(0);
                vista.getTiempoTranscurridoLabel().setText("00:00");
                vista.getTiempoTotalLabel().setText("00:00");
                
                // Reproducir la siguiente canción si existe
                if (!canciones.isEmpty()) {
                    // Si el índice eliminado está dentro del rango, reproducir la que sigue
                    if (indiceActual >= canciones.size()) {
                        indiceActual = canciones.size() - 1; // en caso de que se haya eliminado la última
                    }

                    Cancion siguiente = canciones.get(indiceActual);
                    vista.getTablaCanciones().getSelectionModel().select(siguiente);
                        reproducirCancionSeleccionada();
                    
                }
            }

        } else {
            vista.mostrarAlerta("No se pudo eliminar la canción.");
        }
    } else {
        vista.mostrarAlerta("Por favor, selecciona una canción para eliminar.");
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
                ? "-fx-background-color: #cccccc; -fx-text-fill: white;"
                : "-fx-background-color: transparent; -fx-text-fill: white;";
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
                Duration nuevaPosicion = reproductor.getMediaPlayer().getTotalDuration().multiply(progreso);
                System.out.println("Nuevaposciocion: "+nuevaPosicion);
                reproductor.getMediaPlayer().seek(nuevaPosicion);
                
System.out.println("Nuevaposciocion formateada: "+reproductor.formatearTiempo(nuevaPosicion));
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
    
/**
 * Guarda la sesión actual (canción, posición, volumen, etc.)
 */
private void guardarSesion() {
    Cancion cancionSeleccionada = vista.getTablaCanciones().getSelectionModel().getSelectedItem();
    if (reproductor.getMediaPlayer() != null && cancionSeleccionada != null) {
        String listaActual = vista.getSelectorDeListas().getValue();
        Duration tiempo = reproductor.getMediaPlayer().getCurrentTime();
        double volumen = reproductor.getVolumenActual();
        boolean reproduciendo = reproductor.getMediaPlayer().getStatus() == MediaPlayer.Status.PLAYING;
        boolean pausado = reproductor.getMediaPlayer().getStatus() == MediaPlayer.Status.PAUSED;

        sesionGuardada = new SesionGuardada(
            listaActual,
            cancionSeleccionada.getNombre(),
            tiempo.toMillis(),
            volumen,
            reproduciendo,
            pausado
        );

        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(ARCHIVO_SESION))) {
            out.writeObject(sesionGuardada);
        } catch (IOException e) {
            System.out.println("Error al guardar la sesión: " + e.getMessage());
        }
    }
}
    
/**
 * Carga la sesión guardada si existe
 */
private void cargarSesion() {
    try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(ARCHIVO_SESION))) {
        sesionGuardada = (SesionGuardada) in.readObject();
        if (sesionGuardada != null && gestor.existeLista(sesionGuardada.getListaActual())) {
            vista.getSelectorDeListas().setValue(sesionGuardada.getListaActual());
            cargarListaSeleccionada();

            // Seleccionar la canción que estaba sonando
            for (Cancion cancion : vista.getTablaCanciones().getItems()) {
                if (cancion.getNombre().equals(sesionGuardada.getCancionActual())) {
                    vista.getTablaCanciones().getSelectionModel().select(cancion);
                    break;
                }
            }

// Reproducir la canción y ajustar posición/volumen
if (vista.getTablaCanciones().getSelectionModel().getSelectedItem() != null) {
    Cancion cancion = vista.getTablaCanciones().getSelectionModel().getSelectedItem();
        if(gestor.esFavorita(cancion)){
            vista.getBtnFavorito().setSelected(true);
        }else{
        vista.getBtnFavorito().setSelected(false);
        }
    reproductor.reproducir(cancion.getRuta());
    vista.getNombrePresentacion().setText(cancion.getNombre());
    // Esperar a que el MediaPlayer esté listo para configurar la posición
    if (reproductor.getMediaPlayer() != null) {
        reproductor.getMediaPlayer().setOnReady(() -> {
            reproductor.setPosicion(Duration.millis(sesionGuardada.getTiempoTranscurrido()));
            reproductor.setVolumen(sesionGuardada.getVolumen());

            if (sesionGuardada.isPausado()) {
                reproductor.pausar();
            } else if (sesionGuardada.isReproduciendo()) {
                reproductor.getMediaPlayer().play();  // Reanudar con play()
            }
        });
    }
}
        }
    } catch (Exception e) {
        System.out.println("No hay sesión guardada o error al cargar: " + e.getMessage());
    }
}
//Anghelo
public void clasificarCancionPorMetadatos(Stage stage) {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Seleccionar canciones para clasificar");
    fileChooser.getExtensionFilters().addAll(
        new FileChooser.ExtensionFilter("Archivos de Audio", "*.mp3", "*.m4a", "*.wav")
    );

    List<File> archivos = fileChooser.showOpenMultipleDialog(stage);
    if (archivos == null || archivos.isEmpty()) return;

    for (File archivo : archivos) {
        try {
            javafx.scene.media.Media media = new javafx.scene.media.Media(archivo.toURI().toString());

            final boolean[] procesado = {false};  // bandera para evitar múltiples ejecuciones

            media.getMetadata().addListener((MapChangeListener<String, Object>) cambio -> {
                if (cambio.wasAdded() && !procesado[0]) {
                    procesado[0] = true;

                    Platform.runLater(() -> {
                        String titulo = (String) media.getMetadata().get("title");
                        String artista = (String) media.getMetadata().get("artist");
                        String genero = (String) media.getMetadata().get("genre");

                        String nombreLista = (genero != null && !genero.trim().isEmpty()) ? genero : artista;
                        if (nombreLista == null || nombreLista.trim().isEmpty()) {
                            nombreLista = "Desconocido";
                        }

                        boolean esNuevaLista = false;

                        if (!gestor.existeLista(nombreLista)) {
                            gestor.crearLista(nombreLista);
                            vista.getSelectorDeListas().getItems().add(nombreLista);
                            esNuevaLista = true;
                        }

                        if (!gestor.existeCancionEnLista(nombreLista, archivo.getAbsolutePath())) {
                            gestor.agregarCancionALista(
                                nombreLista,
                                (titulo != null ? titulo : archivo.getName()),
                                archivo.getAbsolutePath()
                            );

                            if (nombreLista.equals(vista.getSelectorDeListas().getValue())) {
                                String duracion = gestor.getLista(nombreLista).obtenerDuracionLegible(archivo.getAbsolutePath());
                                vista.getTablaCanciones().getItems().add(
                                    new Cancion(
                                        (titulo != null ? titulo : archivo.getName()),
                                        duracion,
                                        archivo.getAbsolutePath()
                                    )
                                );
                            }
                        }

                        if (esNuevaLista) {
                            vista.mostrarAlerta("Se creó la lista: " + nombreLista + " y se agregó la canción.");
                        }
                    });
                }
            });

        } catch (Exception e) {
            vista.mostrarAlerta("Error al leer metadatos de: " + archivo.getName() + "\n" + e.getMessage());
        }
    }
}
//Favoritos
public void favorito (){
    String listaActual = vista.getSelectorDeListas().getValue();
    if (listaActual == null) {
        vista.mostrarAlerta("No hay lista seleccionada");
        return;
    }
    Cancion seleccionada = vista.getTablaCanciones().getSelectionModel().getSelectedItem();
    if (seleccionada == null && !vista.getTablaCanciones().getItems().isEmpty()) {
        vista.mostrarAlerta("Selecciona una cancion para agregar");
        vista.getBtnFavorito().setSelected(false);
        return;
    }
    if(vista.getBtnFavorito().isSelected()){
        System.out.println("esta pulsada");
        PopBurbuja.play();
        gestor.agregarFav(seleccionada);
    }else{
        System.out.println("No esta pulsada");
        gestor.eliminarFav(seleccionada);
    }
}

public void mostrarFavoritos() {
    vista.getSelectorDeListas().getSelectionModel().select("Favoritos");
}
public void reproducirAlTocar() {
    Cancion seleccionada = vista.getTablaCanciones().getSelectionModel().getSelectedItem();
    if (seleccionada != null) {
        reproducirCancionSeleccionada();
    }
}
}