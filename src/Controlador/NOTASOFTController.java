package Controlador;

/*
 * Controlador principal para la aplicación de reproducción musical NOTASOFT.
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
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.media.AudioClip;
import javafx.scene.media.MediaPlayer;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import modelo.Cancion;
import modelo.SesionGuardada;
import vista.NOTASOFTView;

public class NOTASOFTController extends Application {
    // Efecto de sonido para acciones de favoritos
    private final AudioClip PopBurbuja = new AudioClip(getClass().getResource("/resources/sonidos/BurbujaPop.mp3").toExternalForm());
    
    // Componentes principales del controlador
    private GestorDeListas gestor;                              // Maneja las listas de reproducción
    private Reproductor reproductor;                            // Controla la reproducción de audio
    private NOTASOFTView vista;                                 // Interfaz gráfica
    private static final String ARCHIVO_LISTAS = "listas.dat";  // Archivo para persistencia de listas
    private Timeline actualizadorProgreso;                      // Actualiza la barra de progreso
    private static final String ARCHIVO_SESION = "sesion.dat";  // Archivo para persistencia de sesión
    private SesionGuardada sesionGuardada;                      // Datos de la sesión actual
    private ObservableList<Cancion> listaCompletaCanciones = FXCollections.observableArrayList(); // Lista completa de canciones

    /* ***********************
     * MÉTODOS PRINCIPALES
     * ***********************/

    /**
     * Método principal de inicio de la aplicación JavaFX
     * @param primaryStage Escenario principal de la aplicación
     */
    @Override
    public void start(Stage primaryStage) {
        // Inicialización de componentes principales
        gestor = new GestorDeListas();
        reproductor = new Reproductor();
        vista = new NOTASOFTView(primaryStage);

        // Configuración inicial
        configurarBindings();
        configurarAtajosTeclado();
        gestor.cargarListas(ARCHIVO_LISTAS);
        inicializarEventos();
        if (!gestor.existeLista("Favoritos")) {
          vista.getSelectorDeListas().getItems().add("Favoritos");
          gestor.guardarListas(ARCHIVO_LISTAS);
          gestor.cargarListas("Favoritos");
          //vista.getSelectorDeListas().setValue(Favoritos);  
        }
        cargarSesion(); // Cargar sesión previa si existe

        // Configurar acción al cerrar la ventana
        primaryStage.setOnCloseRequest(event -> {
            gestor.guardarListas(ARCHIVO_LISTAS);
            guardarSesion();
            reproductor.detener();
            Platform.exit();
            System.exit(0);
        });
        
        // Establecer tamaño mínimo de la ventana
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
                    && !vista.barraProgresoPresionado()) {
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

    /**
     * Configura los atajos de teclado globales
     */
    private void configurarAtajosTeclado() {
        vista.configurarAtajosGlobales(event -> {
            Platform.runLater(() -> {
                switch (event.getCode()) {
                    case RIGHT:
                        siguienteCancion();
                        resaltarBoton(vista.getBtnSiguiente());
                        break;
                    case LEFT:
                        anteriorCancion();
                        resaltarBoton(vista.getBtnAnterior());
                        break;
                    case SPACE:
                        manejarPausaReanudar();
                        break;
                }
            });
        });
        
        // Forzar foco inicial en la tabla de canciones
        Platform.runLater(() -> vista.getTablaCanciones().requestFocus());
    }

    /**
     * Maneja la acción de pausar/reanudar
     */
    private void manejarPausaReanudar() {
        if (reproductor.getMediaPlayer() != null) {
            MediaPlayer.Status status = reproductor.getMediaPlayer().getStatus();
            if (status == MediaPlayer.Status.PLAYING) {
                reproductor.pausar();
                resaltarBoton(vista.getBtnPausa());
            } else {
                reproductor.reanudar();
                resaltarBoton(vista.getBtnReanudar());
            }
        }
    }

    /**
     * Efecto visual para resaltar botones al ser accionados
     * @param boton Botón a resaltar
     */
    private void resaltarBoton(Button boton) {
        boton.setStyle("-fx-background-color: #cccccc;");
        new Timeline(new KeyFrame(Duration.millis(200), e -> {
            boton.setStyle("-fx-background-color: transparent;");
        })).play();
    }

    /* ***********************
     * MANEJO DE EVENTOS
     * ***********************/

    /**
     * Inicializa los eventos de los controles de la interfaz
     */
    private void inicializarEventos() {
        // Configurar eventos de los botones principales
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
        vista.getBtnModoOscuro().setOnAction(e -> modoOscuro());
        vista.getBtnmostrarFavoritos().setOnAction(e -> mostrarFavoritos());
        vista.getBtnRenombrarListaSeleccionada().setOnAction(e -> renombrarListaSeleccionada());
        
        // Reproducir al hacer clic en una canción
        vista.getTablaCanciones().setOnMouseClicked(e -> {
            if (e.getClickCount() == 1) {
                reproducirAlTocar();
            }
        });
        
        // Configurar clasificación por metadatos
        vista.getClasificar().setOnAction(e -> {
            Stage stage = (Stage) vista.getClasificar().getScene().getWindow();
            clasificarCancionPorMetadatos(stage);
        });
        
        // Aplicar efectos visuales a los botones
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
        vista.aplicarEfectoBoton(vista.getBtnRenombrarListaSeleccionada());
        
        // Configurar bucle de repetición
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
        
        // Cargar listas iniciales en el selector
        vista.getSelectorDeListas().getItems().addAll(gestor.getNombresDeListas());
        if (!vista.getSelectorDeListas().getItems().isEmpty()) {
            vista.getSelectorDeListas().setValue(vista.getSelectorDeListas().getItems().get(0));
            cargarListaSeleccionada();
        }

        // Configurar opciones de ordenamiento
        vista.getComboBoxOrdenar().setOnAction(e -> ordenador());

        // Configurar búsqueda en tiempo real
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
            String nombreLimpio = nombre.trim();
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
                // Configurar estado de botones según el tipo de lista
                if (nombre.equals("Favoritos")) {
                    vista.getBtnAgregarCancion().setDisable(true);
                    vista.getBtnAgregarCarpeta().setDisable(true);
                    vista.getBtnEliminarLista().setDisable(true);
                    vista.getBtnFavorito().setDisable(true);
                    vista.getBtnmostrarFavoritos().setDisable(true);
                    vista.getBtnRenombrarListaSeleccionada().setDisable(true);
                    vista.getBtnEliminar().setDisable(lista.vacia());
                } else {
                    vista.getBtnAgregarCancion().setDisable(false);
                    vista.getBtnAgregarCarpeta().setDisable(false);
                    vista.getBtnEliminarLista().setDisable(false);
                    vista.getBtnFavorito().setDisable(false);
                    vista.getBtnmostrarFavoritos().setDisable(false);
                    vista.getBtnRenombrarListaSeleccionada().setDisable(false);
                    
                    boolean listaVacia = lista.vacia();
                    vista.getBtnFavorito().setDisable(listaVacia);
                    vista.getBtnEliminar().setDisable(listaVacia);
                    
                    if (!listaVacia && lista.getRutaCancion(vista.getNombrePresentacion().getText()) == null) {
                        vista.getBtnFavorito().setDisable(true);
                    }
                }
                
                // Cargar canciones en la lista completa
                for (String nombreCancion : lista.getNombresCanciones()) {
                    String ruta = lista.getRutaCancion(nombreCancion);
                    String duracion = lista.obtenerDuracionLegible(ruta);
                    listaCompletaCanciones.add(new Cancion(nombreCancion, duracion, ruta));
                }
            }

            // Mostrar la lista completa en la tabla
            vista.getTablaCanciones().setItems(listaCompletaCanciones);
            vista.getTablaCanciones().getVisibleLeafColumn(0).setText("Cancion             Total: "+ gestor.nroDeMusicasEn(nombre));
            
            // Seleccionar y hacer scroll a la canción actual
            String nombreActual = vista.getNombrePresentacion().getText();
            Platform.runLater(() -> {
                for (Cancion cancion : listaCompletaCanciones) {
                    if (cancion.getNombre().equals(nombreActual)) {
                        vista.getTablaCanciones().getSelectionModel().select(cancion);
                        vista.getTablaCanciones().scrollTo(cancion);
                        break;
                    }
                }
            });
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
                Cancion seleccionada = vista.getTablaCanciones().getSelectionModel().getSelectedItem();
                if (seleccionada != null) {
                    reproductor.detener();
                    vista.getBarraProgreso().setProgress(0);
                    vista.getNombrePresentacion().setText("");
                    vista.getImagePortada().setImage(new Image("/resources/imagenes/disco-de-musica-con-nota-musical.png"));
                    vista.getTiempoTranscurridoLabel().setText("00:00");
                    vista.getTiempoTotalLabel().setText("00:00");
                }
                
                gestor.eliminarLista(nombre);
                vista.getSelectorDeListas().getItems().remove(nombre);
                vista.getTablaCanciones().getItems().clear();

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
                
                // Verificar si la canción ya existe en la lista
                if (lista.getRutaPorNombre(nombreCancion) != null) {
                    vista.mostrarAlerta("La cancion ("+nombreCancion+") no se agrego por que ya existe en la lista ("+nombreLista+")");
                    continue;
                }
                
                lista.agregarCancion(nombreCancion, ruta);
                listaCompletaCanciones.add(new Cancion(nombreCancion, duracion, ruta));
                cargarListaSeleccionada();
            }
            buscarCancion(); // Actualizar la búsqueda
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
                        || archivo.getName().endsWith(".m4a"))) {

                    String ruta = archivo.getAbsolutePath();
                    String nombreCancion = archivo.getName();
                    String duracion = lista.obtenerDuracionLegible(ruta);
                    
                    if (lista.getRutaPorNombre(nombreCancion) != null) {
                        vista.mostrarAlerta("La cancion ("+nombreCancion+") no se agrego por que ya existe en la lista ("+nombreLista+")");
                        continue;
                    }
                    
                    lista.agregarCancion(nombreCancion, ruta);
                    listaCompletaCanciones.add(new Cancion(nombreCancion, duracion, ruta));
                    cargarListaSeleccionada();
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
        
        // Configurar estado del botón de favoritos según la lista
        vista.getBtnFavorito().setDisable(listaActual.equals("Favoritos"));
        
        if (seleccionada != null) {
            vista.getBtnFavorito().setSelected(gestor.esFavorita(seleccionada));
            
            try {
                reproductor.detener();
                vista.getNombrePresentacion().setText(seleccionada.getNombre());
                inicializarTimelineSiNecesario();
                reproductor.reproducir(seleccionada.getRuta());
                mostrarPortada(seleccionada);
            } catch (Exception e) {
                vista.mostrarAlerta("Error al reproducir: " + e.getMessage());
                siguienteCancion();
            }
        } else {
            cargarListaSeleccionada();
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
            vista.getTablaCanciones().getSelectionModel().select(0);
            seleccionada = vista.getTablaCanciones().getItems().get(0);
        }

        if (seleccionada != null) {
            String nombreNueva = siguiente
                    ? lista.getSiguienteCancion(seleccionada.getNombre())
                    : lista.getCancionAnterior(seleccionada.getNombre());

            if (nombreNueva != null) {
                for (int i = 0; i < vista.getTablaCanciones().getItems().size(); i++) {
                    if (vista.getTablaCanciones().getItems().get(i).getNombre().equals(nombreNueva)) {
                        vista.getTablaCanciones().getSelectionModel().select(i);
                        reproducirCancionSeleccionada();
                        break;
                    }
                }
            } else {
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
        vista.getTiempoTranscurridoLabel().setText("00:00");
        vista.getTiempoTotalLabel().setText("00:00");

        if (actualizadorProgreso != null) {
            actualizadorProgreso.stop();
        }
    }

    /**
     * Reinicia el Timeline si fue detenido
     */
    private void inicializarTimelineSiNecesario() {
        if (actualizadorProgreso == null) {
            actualizadorProgreso = new Timeline(
                new KeyFrame(Duration.millis(100), e -> {
                    if (reproductor.getMediaPlayer() != null && !vista.barraProgresoPresionado()) {
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
                if (reproductor.getMediaPlayer() != null && !vista.barraProgresoPresionado()) {
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
                vista.getTablaCanciones().getVisibleLeafColumn(0).setText("Cancion             Total: "+ gestor.nroDeMusicasEn(nombreLista));
                
                if (esLaCancionActual) {
                    reproductor.detener();
                    vista.getBarraProgreso().setProgress(0);
                    vista.getTiempoTranscurridoLabel().setText("00:00");
                    vista.getTiempoTotalLabel().setText("00:00");
                    
                    if (!canciones.isEmpty()) {
                        vista.getBtnEliminar().setDisable(false);
                        if (indiceActual >= canciones.size()) {
                            indiceActual = canciones.size() - 1;
                        }

                        Cancion siguiente = canciones.get(indiceActual);
                        vista.getTablaCanciones().getSelectionModel().select(siguiente);
                        reproducirCancionSeleccionada();
                    } else {
                       vista.getBtnEliminar().setDisable(true); 
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

        cargarListaSeleccionada();
    }

    /**
     * Busca canciones según el texto ingresado
     */
    private void buscarCancion() {
        String textoBusqueda = vista.getCampoBusqueda().getText().trim().toLowerCase();

        if (textoBusqueda.isEmpty()) {
            vista.getTablaCanciones().setItems(listaCompletaCanciones);
        } else {
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
                progreso = Math.max(0, Math.min(1, progreso));
                Duration nuevaPosicion = reproductor.getMediaPlayer().getTotalDuration().multiply(progreso);
                reproductor.getMediaPlayer().seek(nuevaPosicion);
                vista.getBarraProgreso().setProgress(progreso);
                vista.getTiempoTranscurridoLabel().setText(reproductor.formatearTiempo(nuevaPosicion));
            }
        });

        //Manejar arrastre
        barra.setOnMouseDragged(event -> {
          if (reproductor.getMediaPlayer() != null) {
                double progreso = event.getX() / barra.getWidth();
                progreso = Math.max(0, Math.min(1, progreso));
                Duration nuevaPosicion = reproductor.getMediaPlayer().getTotalDuration().multiply(progreso);
                reproductor.getMediaPlayer().seek(nuevaPosicion);
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
        }else{
            String listaActual = vista.getSelectorDeListas().getValue();
            double volumen = reproductor.getVolumenActual();
            sesionGuardada = new SesionGuardada(
                listaActual,
                "",
                0,
                volumen,
                false,
                false
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
                vista.getNombrePresentacion().setText(sesionGuardada.getCancionActual());
                cargarListaSeleccionada();

                // Seleccionar la canción que estaba sonando
                for (Cancion cancion : vista.getTablaCanciones().getItems()) {
                    if (cancion.getNombre().equals(sesionGuardada.getCancionActual())) {
                        vista.getTablaCanciones().getSelectionModel().select(cancion);
                        vista.getTablaCanciones().scrollTo(cancion);
                        mostrarPortada(cancion);
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
                    
                    if (reproductor.getMediaPlayer() != null) {
                        reproductor.getMediaPlayer().setOnReady(() -> {
                            reproductor.setPosicion(Duration.millis(sesionGuardada.getTiempoTranscurrido()));
                            reproductor.setVolumen(sesionGuardada.getVolumen());

                            if (sesionGuardada.isPausado()) {
                                reproductor.pausar();
                            } else if (sesionGuardada.isReproduciendo()) {
                                reproductor.getMediaPlayer().play();
                            }
                        });
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("No hay sesión guardada o error al cargar: " + e.getMessage());
            vista.getBarraProgreso().setProgress(0);
        }
    }

    /**
     * Clasifica canciones por metadatos (género/artista)
     */
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
                final boolean[] procesado = {false};

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

    /**
     * Maneja la marcación/desmarcación de favoritos
     */
    public void favorito() {
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
        
        if(vista.getBtnFavorito().isSelected()) {
            PopBurbuja.play();
            gestor.agregarFav(seleccionada);
        } else {
            gestor.eliminarFav(seleccionada);
        }
    }

    /**
     * Muestra la lista de favoritos
     */
    public void mostrarFavoritos() {
        vista.getSelectorDeListas().getSelectionModel().select("Favoritos");
    }

    /**
     * Reproduce la canción al tocarla en la tabla
     */
    public void reproducirAlTocar() {
        Cancion seleccionada = vista.getTablaCanciones().getSelectionModel().getSelectedItem();
        if (seleccionada != null) {
            reproducirCancionSeleccionada();
        }
    }

    /**
     * Muestra la portada del álbum si está disponible
     */
    public void mostrarPortada(Cancion cancion) {
        String nombre = vista.getSelectorDeListas().getValue();
        ListaReproduccion lista = gestor.getLista(nombre);
        if(lista.obtenerPortada(cancion.getRuta()) == null) {
            vista.getImagePortada().setImage(new Image("/resources/imagenes/disco-de-musica-con-nota-musical.png"));
        } else {
            vista.getImagePortada().setImage(lista.obtenerPortada(cancion.getRuta()));
        }
    }

    /**
     * Activa/desactiva el modo oscuro
     */
    private void modoOscuro() {
        if (vista.getBtnModoOscuro().isSelected()) {
            activarModoOscuro();
        } else {
            activarModoClaro();
        }
    }

    /**
     * Configura la interfaz en modo oscuro
     */
    private void activarModoOscuro() {
        vista.getRoot().setStyle("-fx-background-color: #888888;");
        vista.getNombrePresentacion().setStyle("-fx-text-fill: white;");
        vista.getTablaCanciones().setStyle("-fx-control-inner-background: #424242;");
        vista.getCampoBusqueda().setStyle("-fx-background-color: #424242; -fx-text-fill: white;");
        vista.getComboBoxOrdenar().setStyle("-fx-background-color: #BFBFBF;");
        vista.getSelectorDeListas().setStyle("-fx-background-color: #BFBFBF;");
        vista.getBtnModoOscuro().setText("☀ Modo Claro");
        vista.getBtnModoOscuro().setStyle(
            "-fx-background-color: transparent;" + 
            "-fx-text-fill: white;" + 
            "-fx-border-color: transparent;"      
        );
    }

    /**
     * Configura la interfaz en modo claro
     */
    private void activarModoClaro() {
        vista.getRoot().setStyle("-fx-background-color:  #e9eef2;");
        vista.getNombrePresentacion().setStyle("-fx-text-fill: black;");
        vista.getTablaCanciones().setStyle("-fx-control-inner-background: white;");
        vista.getCampoBusqueda().setStyle(",-fx-background-color: #94b3c8;");
        vista.getComboBoxOrdenar().setStyle("-fx-background-color: #94b3c8;");
        vista.getSelectorDeListas().setStyle("-fx-background-color: #a4d7f4;");
        vista.getBtnModoOscuro().setText("🌙 Modo Oscuro");
        vista.getBtnModoOscuro().setStyle(
            "-fx-background-color: transparent;" + 
            "-fx-text-fill: black;" + 
            "-fx-border-color: transparent;"      
        );
    }

    /**
     * Renombra la lista seleccionada
     */
    private void renombrarListaSeleccionada() {
        String nombreActual = vista.getSelectorDeListas().getValue();
        
        if (nombreActual != null) {
            TextInputDialog dialogo = new TextInputDialog(nombreActual);
            dialogo.setTitle("Renombrar Lista");
            dialogo.setHeaderText("Cambiar el nombre de la lista");
            dialogo.setContentText("Nuevo nombre:");

            Optional<String> resultado = dialogo.showAndWait();
            resultado.ifPresent(nuevoNombre -> {
                nuevoNombre = nuevoNombre.trim();
                if (!nuevoNombre.isEmpty() && !nuevoNombre.equals(nombreActual)) {
                    if (!gestor.existeLista(nuevoNombre)) {
                        gestor.renombrarLista(nombreActual, nuevoNombre);
                        ObservableList<String> items = vista.getSelectorDeListas().getItems();
                        int index = items.indexOf(nombreActual);
                        if (index != -1) {
                            items.set(index, nuevoNombre);
                        }
                        vista.getSelectorDeListas().setValue(nuevoNombre);
                    } else {
                        vista.mostrarAlerta("Ya existe una lista con este nombre");
                    }
                } else {
                    vista.mostrarAlerta("Selecciona un nuevo nombre no vacio y no existente");
                }
            });
        }
    }
}