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

   
    public boolean patada, libre;
    public boolean rival;                   
    public double apuntadoX, apuntadoY;     
    public double tiroX, tiroY;            
    public double arqX, arqY;              
    public double potencia;
    public int muro = -1;                   
    public String tiradorNombre = "", arqueroNombre = "";

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

    public Escena patada(boolean libre, String tirador, String arquero, double apuntadoX, double apuntadoY,
                         double tiroX, double tiroY, double arqX, double arqY, double potencia, int muro) {
        this.patada = true;
        this.libre = libre;
        this.tiradorNombre = tirador;
        this.arqueroNombre = arquero;
        this.apuntadoX = apuntadoX;
        this.apuntadoY = apuntadoY;
        this.tiroX = tiroX;
        this.tiroY = tiroY;
        this.arqX = arqX;
        this.arqY = arqY;
        this.potencia = potencia;
        this.muro = muro;
        return this;
    }

    public Escena fin(Fin fin, String banner) {
        this.fin = fin;
        this.banner = banner;
        return this;
    }
}
