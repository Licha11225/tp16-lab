package arcade;

import java.util.ArrayList;
import java.util.List;


public class Escena {

    public enum Fin { OK, GOL_FAVOR, GOL_CONTRA, ATAJADA, AFUERA, PALO, PERDIDA, RECUPERACION }

    public static class Tramo {
        public final double x, y;
        public final String texto;
        public final int ms;
        public final boolean remate;

        Tramo(double x, double y, String texto, int ms, boolean remate) {
            this.x = x;
            this.y = y;
            this.texto = texto;
            this.ms = ms;
            this.remate = remate;
        }
    }

    public final double x0, y0;
    public final List<Tramo> tramos = new ArrayList<>();
    public Fin fin = Fin.OK;
    public String banner = "";

    public boolean hayArquero;
    public boolean arqueroPropio;
    public int arqueroLado;

    public Escena(double x0, double y0) {
        this.x0 = x0;
        this.y0 = y0;
    }

    public Escena pase(double x, double y, String texto) {
        tramos.add(new Tramo(x, y, texto, 700, false));
        return this;
    }

    public Escena remate(double x, double y, String texto) {
        tramos.add(new Tramo(x, y, texto, 450, true));
        return this;
    }

    public Escena arquero(boolean propio, int lado) {
        this.hayArquero = true;
        this.arqueroPropio = propio;
        this.arqueroLado = lado;
        return this;
    }

    public Escena fin(Fin fin, String banner) {
        this.fin = fin;
        this.banner = banner;
        return this;
    }
}
