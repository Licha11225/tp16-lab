package model;

public class Delantero extends Jugador {
    private boolean es9;

    public Delantero(int id, String nombre, String apellido, int clubId, boolean es9) {
        super(id, nombre, apellido, clubId);
        this.es9 = es9;
    }

    public boolean getEs9() {
        return es9;
    }

    public void setEs9(boolean es9) {
        this.es9 = es9;
    }

}