package arcade;

import java.util.ArrayList;
import java.util.List;

import model.Club;
import model.Jugador;

public class Torneo {

    public static final String[] FASES = {"CUARTOS DE FINAL", "SEMIFINAL", "FINAL"};

    private final List<Club> rivales;
    private final List<List<Jugador>> planteles;
    private final List<String> resultados = new ArrayList<>();
    private int fase = 0;
    private boolean eliminado = false;
    private boolean campeon = false;

    public Torneo(List<Club> rivales, List<List<Jugador>> planteles) {
        this.rivales = rivales;
        this.planteles = planteles;
    }

    public Partido nuevoPartido(String nombreDT, List<Jugador> once, Formacion formacion) {
        return new Partido(nombreDT, once, formacion, rivales.get(fase), planteles.get(fase), fase * 2);
    }

    public boolean registrar(Partido p) {
        boolean gano = p.ganoUsuario();
        String marcador = p.getGolesFavor() + "-" + p.getGolesContra()
                + (p.huboTanda() ? " (pen. " + p.getPenFavor() + "-" + p.getPenContra() + ")" : "");
        resultados.add(FASES[fase] + " vs " + p.getRival() + ":  " + marcador + (gano ? "  ✔" : "  ✘"));
        if (!gano) {
            eliminado = true;
        } else if (fase == FASES.length - 1) {
            campeon = true;
        } else {
            fase++;
        }
        return gano;
    }

    public String nombreFase() { return FASES[Math.min(fase, FASES.length - 1)]; }
    public int getFase() { return fase; }
    public boolean hayProxima() { return !eliminado && !campeon; }
    public boolean isEliminado() { return eliminado; }
    public boolean isCampeon() { return campeon; }
    public String rivalActual() { return rivales.get(Math.min(fase, rivales.size() - 1)).getNombre(); }

    public List<String> cuadro() {
        List<String> l = new ArrayList<>();
        for (int i = 0; i < FASES.length; i++) {
            if (i < resultados.size()) l.add(resultados.get(i));
            else if (eliminado) l.add(FASES[i] + ":  —");
            else l.add(FASES[i] + " vs " + rivales.get(i).getNombre() + ":  pendiente");
        }
        return l;
    }
}