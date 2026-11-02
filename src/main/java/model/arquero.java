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

    @Override
    public boolean esUbicacionValida(String posicionCasillero) {
        if (posicionCasillero == null) return false;
        return posicionCasillero.equalsIgnoreCase("GK");
    }
}