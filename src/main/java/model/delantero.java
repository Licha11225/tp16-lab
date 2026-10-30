package model;

public class delantero extends jugador {
    private boolean es9;


    public delantero(int id, String nombre, int clubId, boolean es9) {
        super(id, nombre, clubId);
        this.es9 = es9;
    }

    public boolean getEs9() {
        return es9;
    }

    public void setEs9(boolean es9) {
        this.es9 = es9;
    }

    @Override
    public boolean esUbicacionValida(String posicionCasillero) {
        if (posicionCasillero == null) return false;
        String pos = posicionCasillero.toUpperCase();
        return pos.equals("ST") || pos.equals("LW") || pos.equals("RW");
    }
}