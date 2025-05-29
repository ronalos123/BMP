package vista;

/**
 * Clase EJECUTADOR - Punto de entrada principal de la aplicación.
 * 
 * Esta clase se encarga de lanzar la aplicación JavaFX, inicializando
 * la interfaz gráfica a través del controlador NOTASOFTController.
 */
import javafx.application.Application;
import Controlador.NOTASOFTController;

public class EJECUTADOR {

    /**
     * Método main - Inicia la ejecución de la aplicación JavaFX.
     * 
     * @param args Argumentos de línea de comandos (si los hay).
     */
    public static void main(String[] args) {
        Application.launch(NOTASOFTController.class, args);  // Lanza la aplicación usando el controlador como clase principal de JavaFX
    }
}
