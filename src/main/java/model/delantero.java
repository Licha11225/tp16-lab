package model;

public class delantero extends jugador {
    private boolean es9;

    public delantero(int id, String nombre, String apellido, int clubId, boolean es9) {
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