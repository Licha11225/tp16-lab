package model;

public class Mediocampista extends Jugador {
    private boolean es5;

    public Mediocampista(int id, String nombre, String apellido, int clubId, boolean es5) {
        super(id, nombre, apellido, clubId);
        this.es5 = es5;
    }

    public boolean getEs5() {
        return es5;
    }

    public void setEs5(boolean es5) {
        this.es5 = es5;
    }
    
}