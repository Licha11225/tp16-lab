package model;

public class arquero extends jugador {
    private boolean juegaAdelantado;

  

    public arquero(int id, String nombre, String apellido, int clubId, boolean juegaAdelantado) {
        super(id, nombre, apellido, clubId);
        this.juegaAdelantado = juegaAdelantado;
    }

    public boolean getJuegaAdelantado() {
        return juegaAdelantado;
    }

    public void setJuegaAdelantado(boolean juegaAdelantado) {
        this.juegaAdelantado = juegaAdelantado;
    }
}