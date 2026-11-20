package model;

public class Defensor extends Jugador {
    private boolean esZagueroCentral;

    

    public Defensor(int id, String nombre, String apellido, int clubId, boolean esZagueroCentral) {
        super(id, nombre, apellido, clubId);
        this.esZagueroCentral = esZagueroCentral;

    }

    public boolean getEsZagueroCentral() {
        return esZagueroCentral;
    }

    public void setEsZagueroCentral(boolean esZagueroCentral) {
        this.esZagueroCentral = esZagueroCentral;
    }
}