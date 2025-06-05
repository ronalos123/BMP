package vista;

import Controlador.NOTASOFTController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import modelo.Cancion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.collections.ObservableList;

/**
 * Clase NOTASOFTView - Vista principal de la aplicación.
 * 
 * Gestiona la interfaz gráfica y sus componentes.
 */
public class NOTASOFTView {

    private final Stage primaryStage;

    // Elementos FXML referenciados
    @FXML private VBox root;
    @FXML private ComboBox<String> selectorDeListas;
    @FXML private ComboBox<String> comboBoxOrdenar;
    @FXML private TableView<Cancion> tablaCanciones;
    @FXML private TableColumn<Cancion, String> columnaNombre;
    @FXML private TableColumn<Cancion, String> columnaDuracion;
    @FXML private ProgressBar barraProgreso;
    @FXML private Slider sliderVolumen;
    @FXML private Label tiempoTranscurridoLabel;
    @FXML private Label tiempoTotalLabel;
    @FXML private Button btnNuevaLista;
    @FXML private Button btnEliminarLista;
    @FXML private Button btnAgregarCancion;
    @FXML private Button btnAgregarCarpeta;
    @FXML private Button btnReproducir;
    @FXML private Button btnPausa;
    @FXML private Button btnReanudar;
    @FXML private Button btnEliminar;
    @FXML private Button btnDetener;
    @FXML private Button btnSiguiente;
    @FXML private Button btnAnterior;
    @FXML private Button btnAleatorio;
    @FXML private Button btnInvertir;
    @FXML private Label NombrePresentacion;
    @FXML private Button btnrenombrarListaSeleccionada;
    @FXML private Button btnClasificar;
    @FXML private Button btnmostrarFavoritos;
    @FXML private ToggleButton btnFavorito;
    @FXML private ToggleButton btnRepetirUna;
    @FXML private ToggleButton btnmodoOscuro;
    @FXML private TextField campoBusqueda;
    @FXML private ImageView imagenPortada;

    // Imágenes para el botón favorito
    private final Image imgFavoritoTrue = new Image(getClass().getResource("/resources/imagenes/Favorite_True.png").toExternalForm());
    private final Image imgFavoritoFalse = new Image(getClass().getResource("/resources/imagenes/Favorite_False.png").toExternalForm());

    /**
     * Constructor de la vista.
     * 
     * @param primaryStage Ventana principal de la aplicación.
     */
    public NOTASOFTView(Stage primaryStage) {
        this.primaryStage = primaryStage;
        cargarFXML();
        configurarComponentes();
        configurarEscena();
    }

    /**
     * Carga el archivo FXML y establece este objeto como controlador.
     */
    private void cargarFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vista/NOTASOFTView.fxml"));
            loader.setController(this);
            root = loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Error al cargar el archivo FXML: " + e.getMessage(), e);
        }
    }

    /**
     * Inicializa y configura los componentes gráficos.
     */
    private void configurarComponentes() {
        // Configurar las columnas de la tabla
        columnaNombre.setCellValueFactory(cellData -> cellData.getValue().nombreProperty());
        columnaDuracion.setCellValueFactory(cellData -> cellData.getValue().duracionProperty());

        // Configurar Drag and Drop para la tabla
        configurarDragAndDrop();

        // Inicializar estado del botón favorito y su escucha
        initializeFavoritoButton();
    }

    /**
     * Inicializa la imagen y comportamiento del botón favorito.
     */
    @FXML
    private void initializeFavoritoButton() {
        btnFavorito.setGraphic(new ImageView(imgFavoritoFalse));
        btnFavorito.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                btnFavorito.setGraphic(new ImageView(imgFavoritoTrue));
            } else {
                btnFavorito.setGraphic(new ImageView(imgFavoritoFalse));
            }
        });
    }

    /**
     * Configura el comportamiento Drag and Drop para reordenar canciones en la tabla.
     */
    private void configurarDragAndDrop() {
        tablaCanciones.setRowFactory(tv -> {
            TableRow<Cancion> row = new TableRow<>();

            row.setOnDragDetected(event -> {
                if (!row.isEmpty()) {
                    Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent cc = new ClipboardContent();
                    cc.putString(String.valueOf(row.getIndex()));
                    db.setContent(cc);
                    event.consume();
                }
            });

            row.setOnDragOver(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasString() && !row.isEmpty()) {
                    int draggedIndex = Integer.parseInt(db.getString());
                    if (draggedIndex != row.getIndex()) {
                        event.acceptTransferModes(TransferMode.MOVE);
                        event.consume();
                    }
                }
            });

            row.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasString()) {
                    int draggedIndex = Integer.parseInt(db.getString());
                    ObservableList<Cancion> items = tablaCanciones.getItems();
                    Cancion draggedItem = items.get(draggedIndex);

                    items.remove(draggedIndex);
                    items.add(row.getIndex(), draggedItem);

                    // Refrescar la lista para actualizar la vista
                    tablaCanciones.getItems().setAll(new ArrayList<>(items));

                    event.setDropCompleted(true);
                    tablaCanciones.getSelectionModel().select(row.getIndex());
                    event.consume();

                    // Sincronizar orden con el controlador, si está asignado
                    Object ctrl = tablaCanciones.getProperties().get("controller");
                    if (ctrl instanceof NOTASOFTController) {
                        ((NOTASOFTController) ctrl).sincronizarOrdenLista();
                    }
                }
            });

            return row;
        });
    }

    /**
     * Configura la escena principal y la muestra.
     */
    private void configurarEscena() {
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("BUBBLEMUSICPLAYER – Inmersión musical en tu burbuja");
    }

    /**
     * Aplica un efecto visual temporal a un botón cuando se pulsa.
     * 
     * @param boton Botón al que aplicar el efecto.
     */
    public void aplicarEfectoBoton(Button boton) {
        if (boton == null) return;

        final String estiloOriginal = "-fx-background-color: transparent;";

        boton.setOnMousePressed(e -> boton.setStyle("-fx-background-color: #cccccc;"));
        boton.setOnMouseReleased(e -> boton.setStyle(estiloOriginal));
        boton.setOnMouseExited(e -> boton.setStyle(estiloOriginal));
    }

    /**
     * Muestra una alerta informativa con un mensaje.
     * 
     * @param mensaje Texto a mostrar.
     */
    public void mostrarAlerta(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("Información");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    /**
     * Muestra un diálogo de confirmación.
     * 
     * @param mensaje Pregunta a mostrar.
     * @return true si el usuario confirma, false en caso contrario.
     */
    public boolean mostrarConfirmacion(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Confirmación");
        alerta.setHeaderText("¿Estás seguro?");
        alerta.setContentText(mensaje);

        Optional<ButtonType> resultado = alerta.showAndWait();
        return resultado.isPresent() && resultado.get() == ButtonType.OK;
    }

    /**
     * Configura atajos globales para controlar eventos de teclado comunes.
     * 
     * @param handler Función para manejar eventos KeyEvent.
     */
    public void configurarAtajosGlobales(Consumer<KeyEvent> handler) {
        Scene scene = getPrimaryStage().getScene();

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.SPACE ||
                event.getCode() == KeyCode.LEFT ||
                event.getCode() == KeyCode.RIGHT) {
                handler.accept(event);
                event.consume();
            }
        });

        tablaCanciones.setFocusTraversable(true);
        tablaCanciones.setOnMouseClicked(e -> tablaCanciones.requestFocus());
    }

    /**
     * Verifica si la barra de progreso está siendo presionada.
     * 
     * @return true si la barra está presionada, false si no.
     */
    public boolean barraProgresoPresionado() {
        return barraProgreso.isPressed();
    }

    /*
     * Getters para acceder a los componentes desde el controlador o la lógica.
     */
    public ComboBox<String> getSelectorDeListas() { return selectorDeListas; }
    public ComboBox<String> getComboBoxOrdenar() { return comboBoxOrdenar; }
    public TableView<Cancion> getTablaCanciones() { return tablaCanciones; }
    public ProgressBar getBarraProgreso() { return barraProgreso; }
    public Slider getSliderVolumen() { return sliderVolumen; }
    public Button getBtnNuevaLista() { return btnNuevaLista; }
    public Button getBtnEliminarLista() { return btnEliminarLista; }
    public Button getBtnAgregarCancion() { return btnAgregarCancion; }
    public Button getBtnAgregarCarpeta() { return btnAgregarCarpeta; }
    public Button getBtnReproducir() { return btnReproducir; }
    public Button getBtnPausa() { return btnPausa; }
    public Button getBtnReanudar() { return btnReanudar; }
    public Button getBtnEliminar() { return btnEliminar; }
    public Button getBtnDetener() { return btnDetener; }
    public Button getBtnSiguiente() { return btnSiguiente; }
    public Button getBtnAnterior() { return btnAnterior; }
    public Button getBtnAleatorio() { return btnAleatorio; }
    public Button getBtnInvertir() { return btnInvertir; }
    public Button getBtnRenombrarListaSeleccionada() { return btnrenombrarListaSeleccionada; }
    public Button getClasificar() { return btnClasificar; }
    public Button getBtnmostrarFavoritos() { return btnmostrarFavoritos; }
    public ToggleButton getBtnFavorito() { return btnFavorito; }
    public ToggleButton getBtnRepetirUna() { return btnRepetirUna; }
    public ToggleButton getBtnModoOscuro() { return btnmodoOscuro; }
    public TextField getCampoBusqueda() { return campoBusqueda; }
    public Label getTiempoTranscurridoLabel() { return tiempoTranscurridoLabel; }
    public Label getTiempoTotalLabel() { return tiempoTotalLabel; }
    public Label getNombrePresentacion() { return NombrePresentacion; }
    public ImageView getImagePortada() { return imagenPortada; }
    public VBox getRoot() { return root; }
    public Stage getPrimaryStage() { return primaryStage; }

    /**
     * Método manejador para el botón Reproducir, ejemplo de acción ligada a la UI.
     */
    @FXML
    public void ReproducirClick() {
        tablaCanciones.getSelectionModel().getSelectedItem();
    }
}
