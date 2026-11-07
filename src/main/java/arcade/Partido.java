package arcade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static arcade.Formacion.CB1;
import static arcade.Formacion.CB2;
import static arcade.Formacion.GK;
import static arcade.Formacion.LB;
import static arcade.Formacion.LW;
import static arcade.Formacion.MC;
import static arcade.Formacion.MCD;
import static arcade.Formacion.MCO;
import static arcade.Formacion.PROPIA;
import static arcade.Formacion.RB;
import static arcade.Formacion.RW;
import static arcade.Formacion.ST;
import model.arquero;
import model.club;
import model.jugador;


public class Partido {

    private static final String[] TIRADORES_DEFAULT = {"Vega", "Molina", "Cruz", "Salas", "Ponce", "Iriarte"};

    private static final int NORMALES = 6;
 
    private static final int[] ORDEN_PENALES = {ST, MCO, LW, RW, MC, MCD, LB, RB, CB1, CB2, GK};
    private static final int[] MINUTOS = {8, 24, 39, 56, 71, 86};
    private static final String[] PISTAS = {
            "Notás que el rival se descuida por tu banda izquierda.",
            "Notás huecos en el medio, entre las líneas rivales.",
            "Notás que el rival se descuida por tu banda derecha."};

    private final Random rnd = new Random();
    private final String nombreDT;
    private final jugador[] once = new jugador[11];
    private final int[] rating = new int[11];

    private final String rival;
    private final int rivAtaque, rivMedio, rivDefensa, rivArquero;
    private final boolean rivArqueroAdelantado;

    private final List<TipoJugada> jugadas = new ArrayList<>();
    private final List<String> cronica = new ArrayList<>();

    private final List<String> tiradoresRival = new ArrayList<>();
    private boolean tanda;
    private int penFavor, penContra;

    private int golesFavor, golesContra;
    private double impulso;      
    private int tirador = ST; 
    private int zonaDebil;      

    /**
     * @param alineacion  11 jugadores EN ORDEN DE CASILLERO
     * @param rivalClub   club rival 
     * @param plantelRival sus jugadores: de ahí sale su nivel por línea y quién patea
     * @param bonusRival  dificultad extra para las rondas avanzadas
     */
    public Partido(String nombreDT, List<jugador> alineacion, club rivalClub, List<jugador> plantelRival, int bonusRival) {
        this.nombreDT = nombreDT;
        for (int i = 0; i < 11; i++) {
            once[i] = alineacion.get(i);
            rating[i] = Ratings.de(once[i]);
        }
        rival = rivalClub.getNombre();
        rivArquero = fuerza(plantelRival, 0, 1, bonusRival);
        rivDefensa = fuerza(plantelRival, 1, 4, bonusRival);
        rivMedio = fuerza(plantelRival, 2, 3, bonusRival);
        rivAtaque = fuerza(plantelRival, 3, 3, bonusRival);

        boolean adelantado = rnd.nextBoolean();
        jugador mejorArq = null;
        for (jugador j : plantelRival) {
            if (j instanceof arquero && (mejorArq == null || Ratings.de(j) > Ratings.de(mejorArq))) mejorArq = j;
        }
        if (mejorArq != null) adelantado = ((arquero) mejorArq).getJuegaAdelantado();
        rivArqueroAdelantado = adelantado;

        List<jugador> ofensivos = new ArrayList<>();
        for (jugador j : plantelRival) {
            if (Ratings.orden(j) >= 2) ofensivos.add(j);
        }
        ofensivos.sort(Comparator.comparingInt((jugador j) -> -Ratings.orden(j)).thenComparingInt(j -> -Ratings.de(j)));
        for (int i = 0; i < ofensivos.size() && tiradoresRival.size() < 6; i++) {
            tiradoresRival.add(Ratings.corto(ofensivos.get(i)));
        }
        if (tiradoresRival.isEmpty()) tiradoresRival.addAll(List.of(TIRADORES_DEFAULT));

        jugadas.add(TipoJugada.TENENCIA);
        jugadas.add(TipoJugada.ATAQUE);
        jugadas.add(TipoJugada.DEFENSA);
        jugadas.add(TipoJugada.CONTRAGOLPE);
        List<TipoJugada> especiales = new ArrayList<>(List.of(
                TipoJugada.PENAL_FAVOR, TipoJugada.MANO_A_MANO, TipoJugada.PENAL_CONTRA, TipoJugada.ATAQUE));
        Collections.shuffle(especiales, rnd);
        jugadas.add(especiales.get(0));
        jugadas.add(especiales.get(1));
    }

    // Promedio de los mejores `cuantos` de una línea (0 arq, 1 def, 2 medio, 3 del) + bonus.
    private static int fuerza(List<jugador> plantel, int linea, int cuantos, int bonus) {
        List<Integer> r = new ArrayList<>();
        for (jugador j : plantel) {
            if (Ratings.orden(j) == linea) r.add(Ratings.de(j));
        }
        if (r.isEmpty()) return Math.min(99, 76 + bonus);
        r.sort(Collections.reverseOrder());
        int n = Math.min(cuantos, r.size());
        int s = 0;
        for (int i = 0; i < n; i++) s += r.get(i);
        return Math.min(99, s / n + bonus);
    }

    private String tiradorRival() {
        return tiradoresRival.get(rnd.nextInt(tiradoresRival.size()));
    }

    // Quién patea el penal de la jugada idx: el 9 en el tiempo normal, rotando en la tanda. 
    private int slotTirador(int idx) {
        if (idx < NORMALES) return ST;
        int ronda = (idx - NORMALES) / 2;
        return ORDEN_PENALES[ronda % ORDEN_PENALES.length];
    }

    // datos para la UI

    public String getRival() { return rival; }
    public String getNombreDT() { return nombreDT; }
    public int total() { return jugadas.size(); }
    public int normales() { return NORMALES; }
    public boolean enTanda() { return tanda; }
    public boolean huboTanda() { return tanda; }
    public int getPenFavor() { return penFavor; }
    public int getPenContra() { return penContra; }

    public boolean ganoUsuario() {
        return golesFavor > golesContra || (golesFavor == golesContra && penFavor > penContra);
    }

    public boolean hayMas(int idx) {
        if (idx + 1 < jugadas.size()) return true;
        if (!tanda) {
            if (golesFavor != golesContra) return false;
            tanda = true;
            agregarRondaTanda();
            return true;
        }
        int rondas = (jugadas.size() - NORMALES) / 2;
        int restan = Math.max(0, 3 - rondas);
        boolean decidido = rondas >= 3
                ? penFavor != penContra
                : (penFavor > penContra + restan || penContra > penFavor + restan);
        if (decidido) return false;
        agregarRondaTanda();
        return true;
    }

    private void agregarRondaTanda() {
        jugadas.add(TipoJugada.PENAL_FAVOR);
        jugadas.add(TipoJugada.PENAL_CONTRA);
    }
    public int getGolesFavor() { return golesFavor; }
    public int getGolesContra() { return golesContra; }
    public List<String> getCronica() { return cronica; }

    public int ataque() { return avg(LW, ST, RW, MCO); }
    public int medio() { return avg(MCD, MC, MCO); }
    public int defensa() { return avg(LB, CB1, CB2, RB); }
    public int arquero() { return rating[GK]; }

    public String resumenEquipos() {
        return "Tu equipo  ATQ " + ataque() + " · MED " + medio() + " · DEF " + defensa() + " · ARQ " + arquero()
                + "     |     " + rival + "  ATQ " + rivAtaque + " · MED " + rivMedio
                + " · DEF " + rivDefensa + " · ARQ " + rivArquero;
    }

    // situaciones

    public Situacion situacion(int idx) {
        TipoJugada t = jugadas.get(idx);
        int min = idx < NORMALES ? MINUTOS[idx] : 120;
        boolean enTanda = idx >= NORMALES;
        int ronda = enTanda ? (idx - NORMALES) / 2 + 1 : 0;
        String pre = (idx == 0 ? "Rueda la pelota contra " + rival + ". " : "");
        switch (t) {
            case TENENCIA -> {
                return new Situacion(t, min, "Tenencia en el mediocampo",
                        pre + "Tenés la pelota en tu campo y el rival presiona. ¿Cómo la manejás? "
                                + "Una buena circulación te da ventaja en la próxima jugada.",
                        new String[]{"Tocar corto y asociarse", "Cambiar de frente", "Pelotazo largo al 9"});
            }
            case ATAQUE -> {
                zonaDebil = rnd.nextInt(3);
                return new Situacion(t, min, "Ataque en campo rival",
                        "Tu equipo está instalado en campo rival. " + PISTAS[zonaDebil] + " ¿Por dónde atacás?",
                        new String[]{"Por la izquierda", "Por el centro", "Por la derecha"});
            }
            case DEFENSA -> {
                return new Situacion(t, min, "Defensa: el rival se viene",
                        rival + " sale de contra y viene con peligro. ¿Cómo defendés?",
                        new String[]{"Presionar arriba", "Replegar y cerrar el área", "Cortar con falta táctica"});
            }
            case CONTRAGOLPE -> {
                return new Situacion(t, min, "¡Contragolpe!",
                        "Recuperaste la pelota y el rival quedó mal parado, hay espacios. ¿Cómo salís?",
                        new String[]{"Pase largo al 9", "Conducir con el 10 y definir", "Triangular con los extremos"});
            }
            case MANO_A_MANO -> {
                return new Situacion(t, min, "¡Mano a mano!",
                        "¡" + ap(ST) + " queda solo ante el arquero! "
                                + (rivArqueroAdelantado
                                        ? "El arquero rival sale muy adelantado."
                                        : "El arquero rival se queda pegado a la línea.")
                                + " ¿Qué hacés?",
                        new String[]{"Definir de primera", "Gambetear al arquero", "Picarla por arriba"});
            }
            case PENAL_FAVOR -> {
                return new Situacion(t, min, enTanda ? "Tanda de penales · ronda " + ronda : "¡Penal a favor!",
                        (enTanda ? "Te toca patear. " : "¡Penal para tu equipo! ") + ap(slotTirador(idx))
                                + " se hace cargo. ¿Hacia dónde pateás?",
                        new String[]{"A tu izquierda", "Al medio", "A tu derecha"});
            }
            default -> {
                return new Situacion(t, min, enTanda ? "Tanda de penales · ronda " + ronda : "¡Penal en contra!",
                        (enTanda ? "Patea " + rival + ". " : "El árbitro cobra penal para " + rival + ". ")
                                + "Te toca adivinar: " + ap(GK) + " ¿hacia dónde se tira?",
                        new String[]{"Tirarte a tu izquierda", "Quedarte en el medio", "Tirarte a tu derecha"});
            }
        }
    }


    public Resultado resolver(int idx, int op) {
        Resultado r;
        tirador = slotTirador(idx);
        r = switch (jugadas.get(idx)) {
            case TENENCIA -> tenencia(op);
            case ATAQUE -> ataque(op);
            case DEFENSA -> defensa(op);
            case CONTRAGOLPE -> contragolpe(op);
            case MANO_A_MANO -> manoAMano(op);
            case PENAL_FAVOR -> penalFavor(op);
            default -> penalContra(op);
        };
        if (idx >= NORMALES) {
            penFavor += r.golFavor;
            penContra += r.golContra;
            cronica.add("Penales (ronda " + ((idx - NORMALES) / 2 + 1) + ")  —  " + r.narracion
                    + "  [" + penFavor + "-" + penContra + "]");
        } else {
            golesFavor += r.golFavor;
            golesContra += r.golContra;
            cronica.add("Min " + MINUTOS[idx] + "'  —  " + r.narracion);
        }
        return r;
    }

    private Resultado tenencia(int op) {
        double exito;
        double bonus;
        String verbo;
        int[] cadena;
        switch (op) {
            case 0 -> {
                exito = 0.80; bonus = 0.05; verbo = "Pase de"; cadena = new int[]{MCD, MC, MCO};
            }
            case 1 -> {
                exito = 0.62; bonus = 0.09; verbo = "Cambio de frente de"; cadena = new int[]{LB, RW};
            }
            default -> {
                exito = 0.48; bonus = 0.13; verbo = "Pelotazo de"; cadena = new int[]{GK, ST};
            }
        }
        boolean ok = rnd.nextDouble() < p(exito + aj(medio(), rivMedio));
        Escena e = pases(ok, verbo, cadena);
        if (ok) {
            impulso = Math.min(0.15, impulso + bonus);
            e.fin(Escena.Fin.OK, "¡BUENA CIRCULACIÓN!");
            return new Resultado("Salió bien la jugada: tu equipo gana confianza y llega mejor a la próxima.", 0, 0, e);
        }
        return new Resultado("Perdés la pelota, pero el rival no logra aprovecharla.", 0, 0, e);
    }

    private Resultado ataque(int op) {
        boolean debil = (op == zonaDebil);
        boolean centro = (op == 1);
        double pPase = (centro ? 0.62 : 0.76) + (debil ? 0.12 : 0) + aj(ataque(), rivDefensa);
        double pGol = (centro ? 0.42 : 0.30) + (debil ? 0.08 : 0) + aj(rating[ST], rivArquero) + usarImpulso();
        boolean ok = rnd.nextDouble() < p(pPase);

        Escena e;
        String intro;
        switch (op) {
            case 0 -> {
                e = pases(ok, "Pase de", LB, LW);
                intro = "Atacás por la izquierda. ";
                if (ok) e.pase(PROPIA[ST][0], PROPIA[ST][1], "Centro de " + ap(LW));
            }
            case 1 -> {
                e = pases(ok, "Pase filtrado de", MC, MCO, ST);
                intro = "Buscás el centro con pases cortos. ";
            }
            default -> {
                e = pases(ok, "Pase de", RB, RW);
                intro = "Atacás por la derecha. ";
                if (ok) e.pase(PROPIA[ST][0], PROPIA[ST][1], "Centro de " + ap(RW));
            }
        }
        if (!ok) {
            return new Resultado(intro + "La defensa rival llega a cortar el pase.", 0, 0, e);
        }
        return rematarNuestro(e, ST, p(pGol), intro);
    }

    private Resultado contragolpe(int op) {
        double pPase, pGol;
        Escena e;
        boolean ok;
        String intro;
        double imp = usarImpulso();
        switch (op) {
            case 0 -> {
                pPase = p(0.55 + aj(ataque(), rivDefensa));
                pGol = p(0.40 + aj(rating[ST], rivArquero) + imp);
                ok = rnd.nextDouble() < pPase;
                e = pases(ok, "Pase largo de", MCD, ST);
                intro = "Pase largo a la espalda de la defensa. ";
                return ok ? rematarNuestro(e, ST, pGol, intro) : new Resultado(intro + "El pase queda muy largo.", 0, 0, e);
            }
            case 1 -> {
                pPase = p(0.62 + aj(medio(), rivMedio));
                pGol = p(0.32 + aj(rating[MCO], rivArquero) + imp);
                ok = rnd.nextDouble() < pPase;
                e = pases(ok, "Conduce", MCD, MCO);
                intro = "Salís conduciendo con " + ap(MCO) + ". ";
                return ok ? rematarNuestro(e, MCO, pGol, intro) : new Resultado(intro + "Le sacan la pelota en la carrera.", 0, 0, e);
            }
            default -> {
                pPase = p(0.48 + aj(ataque(), rivDefensa));
                pGol = p(0.50 + aj(rating[ST], rivArquero) + imp);
                ok = rnd.nextDouble() < pPase;
                e = pases(ok, "Pase de", MC, LW, ST);
                intro = "Triangulás con los extremos. ";
                return ok ? rematarNuestro(e, ST, pGol, intro) : new Resultado(intro + "Un pase se queda corto y lo cortan.", 0, 0, e);
            }
        }
    }

    private Resultado defensa(int op) {
        double pCorte;
        double pGolRival;
        String intro;
        switch (op) {
            case 0 -> {
                pCorte = p(0.50 + aj(medio(), rivMedio));
                pGolRival = 0.48;
                intro = "Presionás arriba. ";
            }
            case 1 -> {
                pCorte = p(0.66 + aj(defensa(), rivAtaque));
                pGolRival = 0.36;
                intro = "Retrocedés y cerrás el área. ";
            }
            default -> {
                pCorte = 0.80;
                pGolRival = 0.30;
                intro = "Cortás el avance con falta táctica. ";
            }
        }
        pGolRival = p(pGolRival + aj(rivAtaque, arquero()));

        boolean corta = rnd.nextDouble() < pCorte;
        int cb = rnd.nextBoolean() ? CB1 : CB2;
        Escena e = new Escena(0.62, 0.50);
        e.pase(0.40, 0.42, rival + " avanza");

        if (corta) {
            if (op == 2) {
                e.pase(PROPIA[cb][0] + 0.03, PROPIA[cb][1], "Falta de " + ap(cb));
                e.fin(Escena.Fin.RECUPERACION, "¡FALTA TÁCTICA!");
                return new Resultado(intro + "El árbitro frena el juego y el peligro pasa.", 0, 0, e);
            }
            e.pase(PROPIA[cb][0] + 0.02, PROPIA[cb][1], "Corta " + ap(cb));
            e.fin(Escena.Fin.RECUPERACION, "¡RECUPERADA!");
            if (op == 0) impulso = Math.min(0.15, impulso + 0.08);
            return new Resultado(intro + "¡La recuperás!" + (op == 0 ? " Tu equipo queda lanzado hacia adelante." : ""), 0, 0, e);
        }
        e.pase(0.24, 0.46, "Llega al área");
        Escena.Fin fin = definirRival(pGolRival);
        String nombreTirador = tiradorRival();
        tiroAlArco(e, false, fin, "Remata " + nombreTirador);
        return new Resultado(intro + narrarRival(fin, nombreTirador), 0,
                fin == Escena.Fin.GOL_CONTRA ? 1 : 0, e);
    }

    private Resultado manoAMano(int op) {
        boolean adel = rivArqueroAdelantado;
        double base;
        base = switch (op) {
            case 0 -> 0.42 + (adel ? 0 : 0.08);
            case 1 -> 0.50 + (adel ? 0.12 : -0.06);
            default -> 0.36 + (adel ? 0.18 : -0.08);
        };
        double pGol = p(base + aj(rating[ST], rivArquero) + usarImpulso());
        Escena e = new Escena(PROPIA[MCO][0], PROPIA[MCO][1]);
        e.pase(0.86, 0.50, "Pase de " + ap(MCO) + " para " + ap(ST));
        String tx;
        String intro;
        switch (op) {
            case 0 -> {
                tx = "Define " + ap(ST);
                intro = "Definís de primera. ";
            }
            case 1 -> {
                e.pase(0.93, 0.60, "Gambeta de " + ap(ST));
                tx = "Remata " + ap(ST);
                intro = "Intentás gambetear al arquero. ";
            }
            default -> {
                tx = "La pica " + ap(ST);
                intro = "Intentás picarla por arriba. ";
            }
        }
        return rematarNuestro(e, ST, pGol, intro, tx);
    }

    private Resultado penalFavor(int op) {
        int tir = op - 1; // -1 arriba izquierda del pateador, 0 centro, 1 abajo
        double rr = rnd.nextDouble();
        int k = rr < 0.35 ? -1 : (rr < 0.65 ? 0 : 1); 
        double pErrar = clamp(0.10 - (rating[tirador] - 75) * 0.003, 0.03, 0.15);
        Escena.Fin fin;
        if (rnd.nextDouble() < pErrar) {
            fin = Escena.Fin.AFUERA;
        } else if (k == tir) {
            double pAtaja = clamp((tir == 0 ? 0.92 : 0.72) + aj(rivArquero, rating[tirador]), 0.40, 0.95);
            fin = rnd.nextDouble() < pAtaja ? Escena.Fin.ATAJADA : Escena.Fin.GOL_FAVOR;
        } else {
            fin = Escena.Fin.GOL_FAVOR;
        }
        Escena e = new Escena(0.88, 0.50);
        tiroAlArco(e, true, fin, "Patea " + ap(tirador), tir, k);
        String donde = tir == 0 ? "al medio" : (tir < 0 ? "a su izquierda" : "a su derecha");
        String intro = "Pateás " + donde + ". ";
        return new Resultado(intro + narrarNuestro(fin, ap(tirador)), fin == Escena.Fin.GOL_FAVOR ? 1 : 0, 0, e);
    }

    private Resultado penalContra(int op) {
        int d = op - 1; 
        double rr = rnd.nextDouble();
        int s = rr < 0.38 ? -1 : (rr < 0.62 ? 0 : 1); 
        Escena.Fin fin;
        if (rnd.nextDouble() < 0.08) {
            fin = Escena.Fin.AFUERA;
        } else if (s == d) {
            double pAtaja = clamp((d == 0 ? 0.90 : 0.62) + aj(arquero(), rivAtaque), 0.40, 0.95);
            fin = rnd.nextDouble() < pAtaja ? Escena.Fin.ATAJADA : Escena.Fin.GOL_CONTRA;
        } else {
            fin = Escena.Fin.GOL_CONTRA;
        }
        String nombreTirador = tiradorRival();
        Escena e = new Escena(0.12, 0.50);
        tiroAlArco(e, false, fin, "Patea " + nombreTirador, s, d);
        String intro = "Te tirás " + (d == 0 ? "al medio" : (d < 0 ? "a tu izquierda" : "a tu derecha")) + ". ";
        return new Resultado(intro + narrarRival(fin, nombreTirador), 0, fin == Escena.Fin.GOL_CONTRA ? 1 : 0, e);
    }


    private Resultado rematarNuestro(Escena e, int tirador, double pGol, String intro) {
        return rematarNuestro(e, tirador, pGol, intro, "Remata " + ap(tirador));
    }

    private Resultado rematarNuestro(Escena e, int tirador, double pGol, String intro, String textoRemate) {
        Escena.Fin fin = definirNuestro(pGol);
        tiroAlArco(e, true, fin, textoRemate);
        return new Resultado(intro + narrarNuestro(fin, ap(tirador)), fin == Escena.Fin.GOL_FAVOR ? 1 : 0, 0, e);
    }

    private Escena.Fin definirNuestro(double pGol) {
        if (rnd.nextDouble() < pGol) return Escena.Fin.GOL_FAVOR;
        return restoDelRemate();
    }

    private Escena.Fin definirRival(double pGol) {
        if (rnd.nextDouble() < pGol) return Escena.Fin.GOL_CONTRA;
        return restoDelRemate();
    }

    private Escena.Fin restoDelRemate() {
        double r = rnd.nextDouble();
        if (r < 0.50) return Escena.Fin.ATAJADA;
        if (r < 0.80) return Escena.Fin.AFUERA;
        return Escena.Fin.PALO;
    }

    private void tiroAlArco(Escena e, boolean derecha, Escena.Fin fin, String texto) {
        int ladoBola = rnd.nextInt(3) - 1;
        int ladoArq;
        if (fin == Escena.Fin.GOL_FAVOR || fin == Escena.Fin.GOL_CONTRA) {
            ladoArq = ladoBola == 0 ? (rnd.nextBoolean() ? 1 : -1) : -ladoBola; 
        } else {
            ladoArq = rnd.nextInt(3) - 1;
        }
        tiroAlArco(e, derecha, fin, texto, ladoBola, ladoArq);
    }

    private void tiroAlArco(Escena e, boolean derecha, Escena.Fin fin, String texto, int ladoBola, int ladoArq) {
        double dir = derecha ? 1 : -1;
        double gx = derecha ? 1.0 : 0.0;
        switch (fin) {
            case GOL_FAVOR, GOL_CONTRA -> e.remate(gx + dir * 0.025, 0.5 + ladoBola * 0.05, texto);
            case ATAJADA -> e.remate(gx - dir * 0.045, 0.5 + ladoArq * 0.075, texto);
            case PALO -> {
                int s = ladoBola == 0 ? (rnd.nextBoolean() ? 1 : -1) : ladoBola;
                e.remate(gx, 0.5 + s * 0.06, texto);
                e.pase(gx - dir * 0.07, 0.5 + s * 0.14, "¡Pega en el palo!");
                ladoArq = s;
            }
            default -> {
                // AFUERA
                e.remate(gx + dir * 0.03, rnd.nextBoolean() ? 0.30 : 0.70, texto);
                ladoArq = 0;
            }
        }
        e.arquero(!derecha, ladoArq);
        e.fin(fin, banner(fin));
    }

    private Escena pases(boolean ok, String verbo, int... slots) {
        Escena e = new Escena(PROPIA[slots[0]][0], PROPIA[slots[0]][1]);
        for (int i = 1; i < slots.length; i++) {
            double[] from = PROPIA[slots[i - 1]];
            double[] to = PROPIA[slots[i]];
            boolean ultimo = (i == slots.length - 1);
            if (!ok && ultimo) {
                e.pase(from[0] + (to[0] - from[0]) * 0.6, from[1] + (to[1] - from[1]) * 0.6 + 0.04,
                        "¡Se la corta el rival!");
                e.fin(Escena.Fin.PERDIDA, banner(Escena.Fin.PERDIDA));
                return e;
            }
            e.pase(to[0], to[1], verbo + " " + ap(slots[i - 1]));
        }
        return e;
    }


    private String narrarNuestro(Escena.Fin fin, String tirador) {
        return switch (fin) {
            case GOL_FAVOR -> "¡GOOOL! " + tirador + " la manda a la red.";
            case ATAJADA -> "Remata " + tirador + " pero el arquero rival la saca.";
            case PALO -> "¡La pegó en el palo! " + tirador + " no lo puede creer.";
            default -> "El remate de " + tirador + " se va afuera.";
        };
    }

    private String narrarRival(Escena.Fin fin, String tirador) {
        return switch (fin) {
            case GOL_CONTRA -> "Gol de " + rival + ": " + tirador + " no perdona.";
            case ATAJADA -> "¡Se luce " + ap(GK) + " y evita el gol!";
            case PALO -> "¡El palo salva a tu equipo!";
            default -> "El remate de " + tirador + " se va desviado.";
        };
    }

    private static String banner(Escena.Fin fin) {
        return switch (fin) {
            case GOL_FAVOR -> "¡GOOOL!";
            case GOL_CONTRA -> "GOL RIVAL";
            case ATAJADA -> "¡ATAJADA!";
            case AFUERA -> "¡AFUERA!";
            case PALO -> "¡AL PALO!";
            case PERDIDA -> "¡PELOTA PERDIDA!";
            case RECUPERACION -> "¡RECUPERADA!";
            default -> "";
        };
    }


    private String ap(int slot) {
        return Ratings.corto(once[slot]);
    }

    private int avg(int... slots) {
        int sum = 0;
        for (int s : slots) sum += rating[s];
        return sum / slots.length;
    }

    private double usarImpulso() {
        double v = impulso;
        impulso = 0;
        return v;
    }

    /** Ventaja/desventaja por diferencia de nivel: cada punto vale 1%, con tope de 12%. */
    private static double aj(int mio, int suyo) {
        return clamp((mio - suyo) * 0.01, -0.12, 0.12);
    }

    private static double p(double v) {
        return clamp(v, 0.05, 0.95);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
