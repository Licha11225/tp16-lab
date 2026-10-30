package model;

public class mediocampista extends jugador {
    private boolean es5;

 

    public mediocampista(int id, String nombre, int clubId, boolean es5) {
        super(id, nombre, clubId);
        this.es5 = es5;
    }

    public boolean getEs5() {
        return es5;
    }

    public void setEs5(boolean es5) {
        this.es5 = es5;
    }

    @Override
    public boolean esUbicacionValida(String posicionCasillero) {
        if (posicionCasillero == null) return false;
        String pos = posicionCasillero.toUpperCase();
        return pos.equals("MCD") || pos.equals("MC") || pos.equals("MCO")
            || pos.equals("CDM") || pos.equals("CM") || pos.equals("CAM");
    }
}