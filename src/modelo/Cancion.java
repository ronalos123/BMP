/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modelo;

/**
 *
 * @author Ronald
 */
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Cancion {
    private final StringProperty nombre;
    private final StringProperty duracion;
    private final String ruta;
    public Cancion(String nombre, String duracion, String ruta) {
        this.nombre = new SimpleStringProperty(nombre);
        this.duracion = new SimpleStringProperty(duracion);
        this.ruta = ruta;
    }

    // Getters y properties
    public String getNombre() { return nombre.get(); }
    public StringProperty nombreProperty() { return nombre; }
    
    public String getDuracion() { return duracion.get(); }
    public StringProperty duracionProperty() { return duracion; }
    public String getRuta() { return ruta; }

}