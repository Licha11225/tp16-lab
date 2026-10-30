package model;

public class defensor extends jugador {
    private boolean esZagueroCentral;

    

    public defensor(int id, String nombre, int clubId, boolean esZagueroCentral) {
        super(id, nombre, clubId);
        this.esZagueroCentral = esZagueroCentral;
    }

    public boolean getEsZagueroCentral() {
        return esZagueroCentral;
    }

    public void setEsZagueroCentral(boolean esZagueroCentral) {
        this.esZagueroCentral = esZagueroCentral;
    }

    @Override
    public boolean esUbicacionValida(String posicionCasillero) {
        if (posicionCasillero == null) return false;
        String pos = posicionCasillero.toUpperCase();
        return pos.equals("CB") || pos.equals("LB") || pos.equals("RB");
    }
}