package model;

import java.util.ArrayList;
import java.util.List;

public abstract class jugador implements alineable {
    private int id;
    private String nombre;
    private int clubId;
    private String apellido;
    
    private final List<String> posicionesPermitidas = new ArrayList<>();

    public jugador(int id, String nombre, String apellido, int clubId) {
        this.id = id;
        this.nombre = nombre;
        this.clubId = clubId;
        this.apellido = apellido;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }
    
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Integer getClubId() {
        return clubId;
    }

    public void setClubId(Integer clubId) {
        this.clubId = clubId;
    }

    public List<String> getPosicionesPermitidas() {
        return posicionesPermitidas;
    }

    public void agregarPosicion(String pos) {
        if (pos != null) {
            String p = pos.toUpperCase().trim();
            if (!posicionesPermitidas.contains(p)) {
                posicionesPermitidas.add(p);
            }
        }
    }

    @Override
    public boolean esUbicacionValida(String posicionCasillero) {
        if (posicionCasillero == null) return false;
        String pos = posicionCasillero.toUpperCase().trim();
        // Validación estricta consultando únicamente las posiciones que tiene en la BDD
        return posicionesPermitidas.contains(pos);
    }

    @Override
    public String toString() {
        return "Jugador{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", apellido='" + apellido + '\'' +
                ", clubId=" + clubId +
                ", posiciones=" + posicionesPermitidas +
                '}';
    }
}