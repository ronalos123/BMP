package vista;

import Controlador.NOTASOFTController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import modelo.Cancion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

public class NOTASOFTView {
    private final Stage primaryStage;
    
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
    //angelo
    @FXML private Button btnClasificar;
    //Greco-Xavier
    @FXML private Button btnmostrarFavoritos;
    @FXML private ToggleButton btnFavorito;
    private final Image imgFavoritoTrue = new Image(getClass().getResource("/resources/imagenes/Favorite_True.png").toExternalForm());
    private final Image imgFavoritoFalse = new Image(getClass().getResource("/resources/imagenes/Favorite_False.png").toExternalForm());
    @FXML private ToggleButton btnRepetirUna;
    @FXML private TextField campoBusqueda;
    @FXML private ImageView imagenPortada;
    @FXML private ToggleButton btnmodoOscuro;
    @FXML
public void initialize() {
    // Imagen inicial
    btnFavorito.setGraphic(new ImageView(imgFavoritoFalse));

    // Escuchar cambio de estado
    btnFavorito.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
        if (isSelected) {
            btnFavorito.setGraphic(new ImageView(imgFavoritoTrue));
        } else {
            btnFavorito.setGraphic(new ImageView(imgFavoritoFalse));
        }
    });
}
    public NOTASOFTView(Stage primaryStage) {
        this.primaryStage = primaryStage;
        cargarFXML();
        configurarComponentes();
        configurarEscena();
    }

private void cargarFXML() {
    try {
        // Usa getResource con la ruta absoluta desde el classpath
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/vista/NOTASOFTView.fxml"));
        loader.setController(this);
        root = loader.load();
    } catch (IOException e) {
        throw new RuntimeException("Error al cargar el archivo FXML: " + e.getMessage(), e);
    }
}

    private void configurarComponentes() {
        // Configuración de las columnas
        columnaNombre.setCellValueFactory(cellData -> cellData.getValue().nombreProperty());
        columnaDuracion.setCellValueFactory(cellData -> cellData.getValue().duracionProperty());
        
        // Configurar drag and drop para reordenar canciones
        configurarDragAndDrop();
        
        // Configurar eventos de la barra de progreso
    }
/**
 * Aplica un efecto visual temporal a un botón: 
 * - Cambia a gris al pulsarlo
 * - Vuelve al estilo original al soltarlo o salir con el ratón
 */
public void aplicarEfectoBoton(Button boton) {
    if (boton == null) return;

    // Guardamos el estilo original del botón
    String estiloOriginal = "-fx-background-color: transparent;";

    // Cambiar a gris al presionar
    boton.setOnMousePressed(e -> {
        boton.setStyle("-fx-background-color: #cccccc;");
    });

    // Volver al estilo original al soltar
    boton.setOnMouseReleased(e -> {
        boton.setStyle(estiloOriginal);
    });

    // Opcional: también restaurar estilo si el mouse sale del botón sin soltarse
    boton.setOnMouseExited(e -> {
        boton.setStyle(estiloOriginal);
    });
}

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

                    ArrayList<Cancion> itemsCopy = new ArrayList<>(items);
                    items.setAll(itemsCopy);

                    event.setDropCompleted(true);
                    tablaCanciones.getSelectionModel().select(row.getIndex());
                    event.consume();

                    if (tablaCanciones.getProperties().get("controller") != null) {
                        NOTASOFTController controller = (NOTASOFTController) tablaCanciones.getProperties().get("controller");
                        controller.sincronizarOrdenLista();
                    }
                }
            });

            return row;
        });
    }



    private void configurarEscena() {
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("🎵 BUBBLEMUSICPLAYER - Reproductor de Música");
    }

    // Los métodos públicos (getters y otros) permanecen iguales que en tu versión original
    public ComboBox<String> getSelectorDeListas() { return selectorDeListas; }
    public ComboBox<String> getComboBoxOrdenar() { return comboBoxOrdenar; }
    public TableView<Cancion> getTablaCanciones() { return tablaCanciones;}
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
    public TextField getCampoBusqueda() { return campoBusqueda; }
    public Stage getPrimaryStage() { return primaryStage; }
    public Label getTiempoTranscurridoLabel() { return tiempoTranscurridoLabel; }
    public Label getTiempoTotalLabel() { return tiempoTotalLabel; }
    public Button getBtnInvertir() { return btnInvertir; }
    public ToggleButton getBtnRepetirUna() { return btnRepetirUna; }
    public ToggleButton getBtnFavorito() { return btnFavorito; }
    public ToggleButton getBtnModoOscuro(){return btnmodoOscuro;}
    public Button getClasificar () {return btnClasificar;}
    public Label getNombrePresentacion() { return NombrePresentacion;}
    public Button getBtnmostrarFavoritos() { return btnmostrarFavoritos;}
    public ImageView getImagePortada(){return imagenPortada;}
    public VBox getRoot(){return root; }
    public Button getBtnRenombrarListaSeleccionada(){return btnrenombrarListaSeleccionada;}
    public void mostrarAlerta(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("Información");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
public boolean mostrarConfirmacion(String mensaje) {
    Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
    alerta.setTitle("Confirmación");
    alerta.setHeaderText("¿Estás seguro?");
    alerta.setContentText(mensaje);

    Optional<ButtonType> resultado = alerta.showAndWait();
    return resultado.isPresent() && resultado.get() == ButtonType.OK;
}
    public boolean BarraProgresoPresionado() {
        return barraProgreso.isPressed();
    }
    @FXML
public void ReproducirClick() {
    getTablaCanciones().getSelectionModel().getSelectedItem();  
}
}