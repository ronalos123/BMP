package vista;

import javafx.application.Application;
import Controlador.NOTASOFTController;

/**
 * Clase EJECUTADOR - Punto de entrada principal de la aplicación.
 * 
 * Esta clase se encarga de iniciar la aplicación JavaFX, lanzando
 * el controlador principal NOTASOFTController.
 */
public class EJECUTADOR {

    /**
     * Método main - Punto de inicio de la aplicación JavaFX.
     * 
     * @param args Argumentos de línea de comandos (opcional).
     */
    public static void main(String[] args) {
        // Lanza la aplicación JavaFX con NOTASOFTController como clase principal
        Application.launch(NOTASOFTController.class, args);
    }
}
