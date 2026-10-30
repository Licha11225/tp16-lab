package model;

public class club {
    private int id;
    private String nombre;
    private String liga;

   

    public club(int id, String nombre, String liga) {
        this.id = id;
        this.nombre = nombre;
        this.liga = liga;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getLiga() {
        return liga;
    }

    public void setLiga(String liga) {
        this.liga = liga;
    }
}
