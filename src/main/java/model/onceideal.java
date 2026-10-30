package model;

import java.util.ArrayList;
import java.util.List;

public class onceideal {
    private int id;
    private String nombreUsuario;
    private List<jugador> listaJugadores = new ArrayList<>();

    // Constructor vacío
    public onceideal() {
    }

    // Constructor con parámetros
    public onceideal(String nombreUsuario, List<jugador> listaJugadores) {
        this.nombreUsuario = nombreUsuario;
        this.listaJugadores = listaJugadores != null ? listaJugadores : new ArrayList<>();
    }

    // --- Getters y Setters necesarios para que el DAO funcione ---

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public List<jugador> getListaJugadores() {
        return listaJugadores;
    }

    public void setListaJugadores(List<jugador> listaJugadores) {
        this.listaJugadores = listaJugadores;
    }
    public void agregarJugador(jugador j) {
        if (this.listaJugadores == null) {
            this.listaJugadores = new java.util.ArrayList<>();
        }
        this.listaJugadores.add(j);
    }
}