package model;

import java.util.ArrayList;
import java.util.List;

import arcade.Formacion;

public class onceideal {
    private int id;
    private String nombreUsuario;
    private List<jugador> listaJugadores = new ArrayList<>();

    public onceideal() {
        for (int i = 0; i < 11; i++) {
            listaJugadores.add(null);
        }
    }

    public onceideal(String nombreUsuario, List<jugador> listaJugadores) {
        this.nombreUsuario = nombreUsuario;
        if (listaJugadores != null) {
            this.listaJugadores = listaJugadores;
        } else {
            this.listaJugadores = new ArrayList<>();
            for (int i = 0; i < 11; i++) {
                this.listaJugadores.add(null);
            }
        }
    }

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

    /**
     * Ubica al jugador en el índice exacto de la formación (0 al 10) según el casillero.
     */
    public void agregarJugadorEnCasillero(String casillero, jugador j) {
        if (this.listaJugadores == null || this.listaJugadores.size() < 11) {
            this.listaJugadores = new ArrayList<>();
            for (int i = 0; i < 11; i++) {
                this.listaJugadores.add(null);
            }
        }
        
        int index = obtenerIndiceCasillero(casillero);
        if (index >= 0 && index < 11) {
            this.listaJugadores.set(index, j);
        } else {
            agregarJugador(j);
        }
    }

    public void agregarJugador(jugador j) {
        if (this.listaJugadores == null) {
            this.listaJugadores = new ArrayList<>();
            for (int i = 0; i < 11; i++) {
                this.listaJugadores.add(null);
            }
        }
        for (int i = 0; i < this.listaJugadores.size(); i++) {
            if (this.listaJugadores.get(i) == null) {
                this.listaJugadores.set(i, j);
                return;
            }
        }
    }

    /**
     * Mapeo ultra robusto para asegurar que cada posición caiga en su casillero exacto.
     */
    private int obtenerIndiceCasillero(String casillero) {
        if (casillero == null) return -1;
        casillero = casillero.trim().toUpperCase();

        for (int i = 0; i < 11; i++) {
            if (Formacion.ETIQUETAS[i].equalsIgnoreCase(casillero) || 
                Formacion.CASILLEROS[i].equalsIgnoreCase(casillero)) {
                return i;
            }
        }

        // Casos de respaldo según la abreviatura
        if (casillero.contains("GK") || casillero.contains("ARQ")) return 0;
        if (casillero.contains("LB")) return 1;
        if (casillero.contains("CB") || casillero.contains("ZAG")) {
            if (this.listaJugadores.get(2) == null) return 2;
            if (this.listaJugadores.get(3) == null) return 3;
            return 2;
        }
        if (casillero.contains("RB")) return 4;
        if (casillero.contains("MCD")) return 5;
        if (casillero.contains("MC")) return 6;
        if (casillero.contains("MCO")) return 7;
        if (casillero.contains("LW")) return 8;
        if (casillero.contains("ST") || casillero.contains("DC")) return 9;
        if (casillero.contains("RW")) return 10;

        return -1;
    }
}