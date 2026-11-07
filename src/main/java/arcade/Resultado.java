package arcade;

public class Resultado {
    public final String narracion;
    public final int golFavor;
    public final int golContra;
    public final Escena escena;

    public Resultado(String narracion, int golFavor, int golContra, Escena escena) {
        this.narracion = narracion;
        this.golFavor = golFavor;
        this.golContra = golContra;
        this.escena = escena;
    }
}
