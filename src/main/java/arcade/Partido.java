package arcade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import model.Arquero;
import model.Club;
import model.Jugador;


public class Partido {

    private static final String[] TIRADORES_DEFAULT = {"Vega", "Molina", "Cruz", "Salas", "Ponce", "Iriarte"};
    private static final String[] PISTAS = {
            "Notás que el rival se descuida por tu banda izquierda.",
            "Notás huecos en el medio, entre las líneas rivales.",
            "Notás que el rival se descuida por tu banda derecha."};
    private static final int MINUTO_TANDA = 120;

    private final Random rnd = new Random();
    private final String nombreDT;
    private final Jugador[] once = new Jugador[11];
    private final int[] rating = new int[11];

    private final Formacion form;
    private final Formacion formRival;
    private final double[][] propia;

    private final int GK = 0;
    private final int LB, CB1, CB2, RB, MCD, MC, MCO, LW, ST, RW;
    private final int[] defSlots, medSlots, atkSlots;
    private final int biasDef, biasMed, biasAtk;
    private final int[] ordenPenales;

    private final String rival;
    private final int rivAtaque, rivMedio, rivDefensa, rivArquero;
    private final boolean rivArqueroAdelantado;
    private String arqRivalNombre = "el arquero rival";

    private final List<TipoJugada> jugadas = new ArrayList<>();
    private final List<Integer> minutos = new ArrayList<>();
    private int normales;
    private final List<String> cronica = new ArrayList<>();
    private final List<String> tiradoresRival = new ArrayList<>();

    private boolean tanda;
    private int penFavor, penContra;
    private int golesFavor, golesContra;

    private double impulso;
    private int zonaDebil;
    private int tAtq, tDef;            

    private double rivX, rivY, rivPot;
    private int rivPista, rivNumero;
    private String rivTirador = "";
    private boolean rivListo;
    private double bonusPos;           
    private double directo = 0.25;     

    /**
     * @param alineacion   
     * @param formacion   
     * @param rivalClub    
     * @param plantelRival 
     * @param bonusRival   
     */
    public Partido(String nombreDT, List<Jugador> alineacion, Formacion formacion, Club rivalClub,
                   List<Jugador> plantelRival, int bonusRival) {
        this.nombreDT = nombreDT;
        this.form = formacion;
        this.formRival = Formacion.aleatoria(rnd);
        this.propia = formacion.propia();
        for (int i = 0; i < 11; i++) {
            once[i] = alineacion.get(i);
            rating[i] = Ratings.de(once[i]);
        }

        List<Integer> cbs = form.slotsDe("CB");
        CB1 = cbs.get(0);
        CB2 = cbs.get(cbs.size() > 2 ? 2 : 1);
        int lb = form.slotDe("LB", "LM");
        int rb = form.slotDe("RB", "RM");
        LB = lb >= 0 ? lb : CB1;
        RB = rb >= 0 ? rb : CB2;
        int mcd = form.slotDe("MCD", "MC");
        MCD = mcd >= 0 ? mcd : primeroDeLinea(2);
        List<Integer> mcs = form.slotsDe("MC");
        int mco = form.slotDe("MCO");
        if (mco < 0) mco = mcs.size() > 1 ? mcs.get(mcs.size() - 1) : -1;
        MCO = (mco >= 0 && mco != MCD) ? mco : ultimoDeLineaDistinto(2, MCD);
        int mc = -1;
        for (int s : mcs) if (s != MCD && s != MCO) { mc = s; break; }
        if (mc < 0) for (int i = 1; i < 11; i++) if (form.linea(i) == 2 && i != MCD && i != MCO) { mc = i; break; }
        MC = mc >= 0 ? mc : MCD;
        List<Integer> sts = form.slotsDe("ST");
        int lw = form.slotDe("LW", "LM");
        int rw = form.slotDe("RW", "RM");
        int stFallback = sts.isEmpty() ? MCO : sts.get(0);
        LW = lw >= 0 ? lw : stFallback;
        RW = rw >= 0 ? rw : (sts.size() > 1 ? sts.get(sts.size() - 1) : stFallback);
        ST = sts.isEmpty() ? MCO : sts.get(0);

        defSlots = slotsDeLinea(1);
        medSlots = slotsDeLinea(2);
        List<Integer> atk = new ArrayList<>();
        for (int i = 1; i < 11; i++) if (form.linea(i) == 3 || form.casillero(i).equals("MCO")) atk.add(i);
        if (atk.isEmpty()) atk.add(ST);
        atkSlots = atk.stream().mapToInt(Integer::intValue).toArray();
        // cada formación suma o resta puntos a sus líneas: más gente en una línea, más fuerte esa línea
        biasDef = (int) Math.round((form.cantidad(1) - 4) * 2.0);
        biasMed = (int) Math.round((form.cantidad(2) - 3) * 1.5);
        biasAtk = (int) Math.round((form.cantidad(3) - 3) * 1.5);

        List<Integer> orden = new ArrayList<>();
        for (int s : new int[]{ST, MCO, LW, RW, MC, MCD, LB, RB, CB1, CB2}) if (!orden.contains(s)) orden.add(s);
        for (int i = 1; i < 11; i++) if (!orden.contains(i)) orden.add(i);
        orden.add(GK);
        ordenPenales = orden.stream().mapToInt(Integer::intValue).toArray();

        rival = rivalClub.getNombre();
        rivArquero = fuerza(plantelRival, 0, 1, bonusRival);
        rivDefensa = fuerza(plantelRival, 1, 4, bonusRival);
        rivMedio = fuerza(plantelRival, 2, 3, bonusRival);
        rivAtaque = fuerza(plantelRival, 3, 3, bonusRival);

        boolean adelantado = rnd.nextBoolean();
        Jugador mejorArq = null;
        for (Jugador j : plantelRival) {
            if (j instanceof Arquero && (mejorArq == null || Ratings.de(j) > Ratings.de(mejorArq))) mejorArq = j;
        }
        if (mejorArq != null) {
            adelantado = ((Arquero) mejorArq).getJuegaAdelantado();
            arqRivalNombre = Ratings.corto(mejorArq);
        }
        rivArqueroAdelantado = adelantado;

        List<Jugador> ofensivos = new ArrayList<>();
        for (Jugador j : plantelRival) {
            if (Ratings.orden(j) >= 2) ofensivos.add(j);
        }
        ofensivos.sort(Comparator.comparingInt((Jugador j) -> -Ratings.orden(j)).thenComparingInt(j -> -Ratings.de(j)));
        for (int i = 0; i < ofensivos.size() && tiradoresRival.size() < 6; i++) {
            tiradoresRival.add(Ratings.corto(ofensivos.get(i)));
        }
        if (tiradoresRival.isEmpty()) tiradoresRival.addAll(List.of(TIRADORES_DEFAULT));

        armarCalendario();
    }


    private int primeroDeLinea(int linea) {
        for (int i = 1; i < 11; i++) if (form.linea(i) == linea) return i;
        return 1;
    }

    private int ultimoDeLineaDistinto(int linea, int distinto) {
        int r = -1;
        for (int i = 1; i < 11; i++) if (form.linea(i) == linea && i != distinto) r = i;
        return r >= 0 ? r : distinto;
    }

    private int[] slotsDeLinea(int linea) {
        List<Integer> l = new ArrayList<>();
        for (int i = 1; i < 11; i++) if (form.linea(i) == linea) l.add(i);
        return l.stream().mapToInt(Integer::intValue).toArray();
    }

    private static int fuerza(List<Jugador> plantel, int linea, int cuantos, int bonus) {
        List<Integer> r = new ArrayList<>();
        for (Jugador j : plantel) {
            if (Ratings.orden(j) == linea) r.add(Ratings.de(j));
        }
        if (r.isEmpty()) return Math.min(99, 76 + bonus);
        r.sort(Collections.reverseOrder());
        int n = Math.min(cuantos, r.size());
        int s = 0;
        for (int i = 0; i < n; i++) s += r.get(i);
        return Math.min(99, s / n + bonus);
    }


    
    private void armarCalendario() {
        List<TipoJugada> pool = new ArrayList<>(List.of(
                TipoJugada.TENENCIA, TipoJugada.ATAQUE, TipoJugada.DEFENSA, TipoJugada.CONTRAGOLPE,
                TipoJugada.CORNER, TipoJugada.TIRO_LIBRE, TipoJugada.CORNER_CONTRA, TipoJugada.TIRO_LIBRE_CONTRA,
                TipoJugada.MANO_A_MANO, rnd.nextBoolean() ? TipoJugada.PENAL_FAVOR : TipoJugada.PENAL_CONTRA));
        Collections.shuffle(pool, rnd);
        List<TipoJugada> elegidas = new ArrayList<>(pool.subList(0, 8));
        if (!elegidas.contains(TipoJugada.PENAL_FAVOR) && !elegidas.contains(TipoJugada.TIRO_LIBRE)) {
            elegidas.set(rnd.nextInt(8), TipoJugada.TIRO_LIBRE);
        }
        int[][] rangos = {{6, 13}, {16, 24}, {27, 35}, {37, 43}, {50, 57}, {60, 67}, {70, 77}, {80, 88}};
        for (int i = 0; i < 8; i++) {
            if (i == 4) {
                jugadas.add(TipoJugada.ENTRETIEMPO);
                minutos.add(45);
            }
            jugadas.add(elegidas.get(i));
            minutos.add(rangos[i][0] + rnd.nextInt(rangos[i][1] - rangos[i][0] + 1));
        }
        normales = jugadas.size();
    }


    public String getRival() { return rival; }
    public String getNombreDT() { return nombreDT; }
    public Formacion getFormacion() { return form; }
    public Formacion getFormacionRival() { return formRival; }
    public int total() { return jugadas.size(); }

    public int normales() { return normales; }
    public int minutoDe(int idx) { return idx < normales ? minutos.get(idx) : MINUTO_TANDA; }
    public TipoJugada tipoDe(int idx) { return jugadas.get(idx); }
    public boolean enTanda() { return tanda; }
    public boolean huboTanda() { return tanda; }
    public int getPenFavor() { return penFavor; }
    public int getPenContra() { return penContra; }
    public int getGolesFavor() { return golesFavor; }
    public int getGolesContra() { return golesContra; }
    public List<String> getCronica() { return cronica; }
    public double estiloDirecto() { return directo; }

    public boolean ganoUsuario() {
        return golesFavor > golesContra || (golesFavor == golesContra && penFavor > penContra);
    }

    public double posesionBase() {
        double b = 0.5 + aj(medio(), rivMedio) * 1.2 + bonusPos + (tAtq > 0 ? 0.02 : (tAtq < 0 ? -0.03 : 0));
        return clamp(b, 0.32, 0.68);
    }

    public boolean empatado() { return golesFavor == golesContra; }

    public void iniciarTanda() {
        tanda = true;
        agregarRondaTanda();
    }

    public boolean hayMas(int idx) {
        if (idx + 1 < jugadas.size()) return true;
        if (!tanda) return false;
        int rondas = (jugadas.size() - normales) / 2;
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

    public boolean esPatada(int idx) {
        TipoJugada t = jugadas.get(idx);
        return t == TipoJugada.PENAL_FAVOR || t == TipoJugada.TIRO_LIBRE;
    }

    public String nombrePateador(int idx) {
        return ap(jugadas.get(idx) == TipoJugada.TIRO_LIBRE ? pateadorLibre() : slotTirador(idx));
    }

    public String nombreArquero() { return arqRivalNombre; }

    public boolean esAtajada(int idx) { return jugadas.get(idx) == TipoJugada.PENAL_CONTRA; }
    public String nombreArqueroPropio() { return ap(GK); }
    public String nombrePateadorRival() { return rivTirador; }
    public int numeroPateadorRival() { return rivNumero; }
    public int numeroPateador(int idx) {
        int s = jugadas.get(idx) == TipoJugada.TIRO_LIBRE ? pateadorLibre() : slotTirador(idx);
        return 2 + s;   // camiseta ficticia según el casillero
    }


    public int ataque() { return avg(atkSlots) + biasAtk + tAtq; }
    public int medio() { return avg(medSlots) + biasMed; }
    public int defensa() { return avg(defSlots) + biasDef + tDef; }
    public int arquero() { return rating[GK]; }

    public void setTactica(int op) {
        tAtq = op == 0 ? 4 : (op == 2 ? -3 : 0);
        tDef = op == 0 ? -4 : (op == 2 ? 4 : 0);
    }

    public String resumenEquipos() {
        return "Tu equipo (" + form.getNombre() + ")  ATQ " + ataque() + " · MED " + medio() + " · DEF " + defensa()
                + " · ARQ " + arquero()
                + "     |     " + rival + " (" + formRival.getNombre() + ")  ATQ " + rivAtaque + " · MED " + rivMedio
                + " · DEF " + rivDefensa + " · ARQ " + rivArquero;
    }


    private int slotTirador(int idx) {
        if (idx < normales) return ST;
        int ronda = (idx - normales) / 2;
        return ordenPenales[ronda % ordenPenales.length];
    }

    private int pateadorLibre() {
        return rating[MCO] >= rating[ST] ? MCO : ST;
    }

    public Situacion situacion(int idx) {
        TipoJugada t = jugadas.get(idx);
        int min = minutoDe(idx);
        boolean enTanda = idx >= normales;
        int ronda = enTanda ? (idx - normales) / 2 + 1 : 0;
        String pre = (idx == 0 ? "Rueda la pelota contra " + rival + ". " : "");
        switch (t) {
            case TENENCIA -> {
                return new Situacion(t, min, "Tenencia en el mediocampo",
                        pre + "El partido está parejo en el medio. ¿Querés tener la pelota con posesión o jugar "
                                + "más directo? Una buena circulación te da ventaja en la próxima jugada.",
                        new String[]{"Posesión: tocar corto y asociarse", "Equilibrado: cambiar de frente",
                                "Juego directo: pelotazo al 9"});
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
                        new String[]{"Presión alta", "Bloque equilibrado", "Bloque defensivo"});
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
            case CORNER -> {
                return new Situacion(t, min, "¡Córner a favor!",
                        "Pelota parada en campo rival: córner para tu equipo. ¿Cómo lo jugás?",
                        new String[]{"Jugarlo corto", "Centro al primer palo", "Centro al segundo palo",
                                "Centro para la volea"});
            }
            case CORNER_CONTRA -> {
                return new Situacion(t, min, "Córner en contra",
                        rival + " tiene un córner y va a mandar la pelota al área. ¿Cómo marcás?",
                        new String[]{"Marca al hombre", "Defensa en zona", "Dos en los palos y salida rápida"});
            }
            case TIRO_LIBRE -> {
                return new Situacion(t, min, "¡Tiro libre a favor!",
                        "Falta al borde del área. " + ap(pateadorLibre()) + " se perfila para patear con barrera. "
                                + "Apuntá con las flechas y cargá la potencia con ESPACIO.",
                        new String[]{"Izquierda", "Centro", "Derecha"});
            }
            case TIRO_LIBRE_CONTRA -> {
                return new Situacion(t, min, "Tiro libre en contra",
                        "Falta peligrosa al borde de tu área. Patea " + rival + ". ¿Cómo armás la defensa?",
                        new String[]{"Barrera de 5 hombres", "Barrera de 3 y arquero al palo",
                                "Salir a presionar la pelota"});
            }
            case ENTRETIEMPO -> {
                return new Situacion(t, min, "Entretiempo",
                        "Termina el primer tiempo: " + golesFavor + " a " + golesContra + " contra " + rival
                                + ". ¿Con qué mentalidad salís a jugar el segundo?",
                        new String[]{"Ofensiva: salir con todo", "Equilibrada", "Defensiva: cuidar el resultado"});
            }
            case PENAL_FAVOR -> {
                return new Situacion(t, min, enTanda ? "Tanda de penales · ronda " + ronda : "¡Penal a favor!",
                        (enTanda ? "Te toca patear. " : "¡Penal para tu equipo! ") + ap(slotTirador(idx))
                                + " se hace cargo. Apuntá con las flechas y cargá la potencia con ESPACIO.",
                        new String[]{"Izquierda", "Centro", "Derecha"});
            }
            default -> {
                return new Situacion(t, min, enTanda ? "Tanda de penales · ronda " + ronda : "¡Penal en contra!",
                        (enTanda ? "Patea " + rival + ". " : "El árbitro cobra penal para " + rival + ". ")
                                + "Te toca atajar: mové el objetivo con las flechas y apretá ESPACIO para tirarte. "
                                + "Mirá cómo se perfila el pateador... ¡pero puede amagar!",
                        new String[]{"Tirarte a tu izquierda", "Quedarte en el medio", "Tirarte a tu derecha"});
            }
        }
    }


    public Resultado resolver(int idx, int op) {
        Resultado r = switch (jugadas.get(idx)) {
            case TENENCIA -> tenencia(op);
            case ATAQUE -> ataque(op);
            case DEFENSA -> defensa(op);
            case CONTRAGOLPE -> contragolpe(op);
            case MANO_A_MANO -> manoAMano(op);
            case CORNER -> corner(op);
            case CORNER_CONTRA -> cornerContra(op);
            case TIRO_LIBRE_CONTRA -> tiroLibreContra(op);
            case ENTRETIEMPO -> entretiempo(op);
            case PENAL_CONTRA -> penalContra(op);
            default -> patadaDe(idx, (op - 1) * 0.7, 0.55, 0.75);
        };
        registrar(idx, r);
        return r;
    }

    public Resultado patear(int idx, double ax, double ay, double potencia) {
        Resultado r = patadaDe(idx, ax, ay, potencia);
        registrar(idx, r);
        return r;
    }

    private Resultado patadaDe(int idx, double ax, double ay, double potencia) {
        boolean libre = jugadas.get(idx) == TipoJugada.TIRO_LIBRE;
        return resolverPatada(libre, libre ? pateadorLibre() : slotTirador(idx), ax, ay, potencia);
    }

    private void registrar(int idx, Resultado r) {
        if (idx >= normales) {
            penFavor += r.golFavor;
            penContra += r.golContra;
            cronica.add("Penales (ronda " + ((idx - normales) / 2 + 1) + ")  —  " + r.narracion
                    + "  [" + penFavor + "-" + penContra + "]");
        } else {
            golesFavor += r.golFavor;
            golesContra += r.golContra;
            cronica.add("Min " + minutoDe(idx) + "'  —  " + r.narracion);
        }
    }

    private Resultado entretiempo(int op) {
        setTactica(op);
        String texto = switch (op) {
            case 0 -> "Salís con todo al ataque: ganás en ofensiva, pero dejás espacios atrás.";
            case 1 -> "Mantenés el equilibrio del primer tiempo.";
            default -> "Cerrás las líneas para cuidar el resultado: ganás en defensa pero atacás menos.";
        };
        bonusPos += op == 0 ? 0.02 : (op == 2 ? -0.03 : 0);
        return new Resultado("Entretiempo. " + texto, 0, 0, null);
    }

    private Resultado tenencia(int op) {
        double exito, bonus;
        String verbo;
        int[] cadena;
        switch (op) {
            case 0 -> {
                exito = 0.80; bonus = 0.05; verbo = "Pase de"; cadena = new int[]{MCD, MC, MCO};
                bonusPos = 0.06;
                directo = 0.10;
            }
            case 1 -> {
                exito = 0.62; bonus = 0.09; verbo = "Cambio de frente de"; cadena = new int[]{LB, RW};
                bonusPos = 0.02;
                directo = 0.30;
            }
            default -> {
                exito = 0.48; bonus = 0.13; verbo = "Pelotazo de"; cadena = new int[]{GK, ST};
                bonusPos = -0.04;
                directo = 0.80;
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
                if (ok) e.pase(propia[ST][0], propia[ST][1], "Centro de " + ap(LW));
            }
            case 1 -> {
                e = pases(ok, "Pase filtrado de", MC, MCO, ST);
                intro = "Buscás el centro con pases cortos. ";
            }
            default -> {
                e = pases(ok, "Pase de", RB, RW);
                intro = "Atacás por la derecha. ";
                if (ok) e.pase(propia[ST][0], propia[ST][1], "Centro de " + ap(RW));
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
        double pCorte, pGolRival;
        String intro;
        switch (op) {
            case 0 -> {
                pCorte = p(0.50 + aj(medio(), rivMedio));
                pGolRival = 0.48;
                intro = "Presionás alto. ";
            }
            case 1 -> {
                pCorte = p(0.58 + aj((medio() + defensa()) / 2, (rivMedio + rivAtaque) / 2));
                pGolRival = 0.42;
                intro = "Mantenés el bloque equilibrado. ";
            }
            default -> {
                pCorte = p(0.66 + aj(defensa(), rivAtaque));
                pGolRival = 0.36;
                intro = "Retrocedés y cerrás el área. ";
            }
        }
        pGolRival = p(pGolRival + aj(rivAtaque, arquero()));

        boolean corta = rnd.nextDouble() < pCorte;
        int cb = rnd.nextBoolean() ? CB1 : CB2;
        Escena e = new Escena(0.62, 0.50);
        e.pase(0.40, 0.42, rival + " avanza");

        if (corta) {
            e.pase(propia[cb][0] + 0.02, propia[cb][1], "Corta " + ap(cb));
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
        double base = switch (op) {
            case 0 -> 0.42 + (adel ? 0 : 0.08);
            case 1 -> 0.50 + (adel ? 0.12 : -0.06);
            default -> 0.36 + (adel ? 0.18 : -0.08);
        };
        double pGol = p(base + aj(rating[ST], rivArquero) + usarImpulso());
        Escena e = new Escena(propia[MCO][0], propia[MCO][1]);
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

    private Resultado corner(int op) {
        boolean arriba = rnd.nextBoolean();
        double cy = arriba ? 0.03 : 0.97;
        int cobrador = arriba ? LW : RW;
        double pPase, pGol;
        int rematador;
        String intro, verbo;
        Escena e = new Escena(0.99, cy);
        switch (op) {
            case 0 -> {
                pPase = p(0.78 + aj(medio(), rivMedio));
                pGol = p(0.32 + aj(rating[MCO], rivArquero) + usarImpulso());
                rematador = MCO;
                intro = "Córner jugado corto para asociarse. ";
                verbo = "Remata ";
                e.pase(0.93, arriba ? 0.14 : 0.86, "Toque corto de " + ap(cobrador));
            }
            case 1 -> {
                pPase = p(0.60 + aj(ataque(), rivDefensa));
                pGol = p(0.34 + aj(rating[ST], rivArquero) + usarImpulso());
                rematador = ST;
                intro = "Centro cerrado al primer palo. ";
                verbo = "Cabecea ";
            }
            case 2 -> {
                pPase = p(0.56 + aj(ataque(), rivDefensa));
                pGol = p(0.40 + aj(rating[CB1], rivArquero) + usarImpulso());
                rematador = CB1;
                intro = "Centro al segundo palo, donde sube el central. ";
                verbo = "Cabecea ";
            }
            default -> {
                pPase = p(0.50 + aj(ataque(), rivDefensa));
                pGol = p(0.46 + aj(rating[MC], rivArquero) + usarImpulso());
                rematador = MC;
                intro = "Centro retrasado para la volea al borde del área. ";
                verbo = "Voleo de ";
            }
        }
        boolean ok = rnd.nextDouble() < pPase;
        if (!ok) {
            e.pase(0.88, arriba ? 0.36 : 0.64, "Centro de " + ap(cobrador));
            e.fin(Escena.Fin.PERDIDA, "¡LO DESPEJAN!");
            return new Resultado(intro + "La defensa rival despeja el peligro.", 0, 0, e);
        }
        e.pase(op == 3 ? 0.78 : 0.90, op == 3 ? 0.52 : 0.46, "Centro de " + ap(cobrador));
        return rematarNuestro(e, rematador, pGol, intro, verbo + ap(rematador));
    }

    /** Córner en contra: 0 al hombre, 1 zona, 2 dos en los palos y salida rápida. */
    private Resultado cornerContra(int op) {
        boolean arriba = rnd.nextBoolean();
        double cy = arriba ? 0.03 : 0.97;
        double pDespeje, pGolRival;
        String intro;
        switch (op) {
            case 0 -> {
                pDespeje = p(0.58 + aj(defensa(), rivAtaque));
                pGolRival = 0.32;
                intro = "Marcás al hombre. ";
            }
            case 1 -> {
                pDespeje = p(0.62 + aj((defensa() + medio()) / 2, rivAtaque));
                pGolRival = 0.34;
                intro = "Defendés en zona. ";
            }
            default -> {
                pDespeje = p(0.50 + aj(defensa(), rivAtaque));
                pGolRival = 0.26;
                intro = "Dejás dos hombres en los palos y la salida rápida. ";
            }
        }
        pGolRival = p(pGolRival + aj(rivAtaque, arquero()));
        Escena e = new Escena(0.01, cy);
        e.pase(0.10, arriba ? 0.40 : 0.60, "Córner de " + rival);
        int cb = rnd.nextBoolean() ? CB1 : CB2;
        if (rnd.nextDouble() < pDespeje) {
            e.pase(propia[cb][0] + 0.02, propia[cb][1], "Despeja " + ap(cb));
            e.fin(Escena.Fin.RECUPERACION, "¡DESPEJA!");
            if (op == 2) impulso = Math.min(0.15, impulso + 0.10);
            return new Resultado(intro + ap(cb) + " despeja de cabeza." + (op == 2 ? " ¡Salís rápido de contra!" : ""),
                    0, 0, e);
        }
        Escena.Fin fin = definirRival(pGolRival);
        String nombreTirador = tiradorRival();
        tiroAlArco(e, false, fin, "Cabecea " + nombreTirador);
        return new Resultado(intro + narrarRival(fin, nombreTirador), 0, fin == Escena.Fin.GOL_CONTRA ? 1 : 0, e);
    }

    private Resultado tiroLibreContra(int op) {
        double pBarrera, pGolRival;
        String intro;
        switch (op) {
            case 0 -> {
                pBarrera = 0.38;
                pGolRival = 0.16;
                intro = "Armás una barrera de cinco hombres. ";
            }
            case 1 -> {
                pBarrera = 0.24;
                pGolRival = 0.13;
                intro = "Barrera de tres y el arquero cubre el palo. ";
            }
            default -> {
                pBarrera = 0.10;
                pGolRival = 0.12;
                intro = "Salís a presionar la pelota. ";
            }
        }
        pGolRival = p(pGolRival + aj(rivAtaque, arquero()));
        Escena e = new Escena(0.28, 0.50);
        String nombreTirador = tiradorRival();
        if (rnd.nextDouble() < pBarrera) {
            e.remate(0.20, 0.50, "Patea " + nombreTirador);
            e.pase(0.26, 0.56, "¡Pega en la barrera!");
            e.fin(Escena.Fin.RECUPERACION, "¡LA BARRERA LA TAPA!");
            if (op == 2) impulso = Math.min(0.15, impulso + 0.06);
            return new Resultado(intro + "El tiro de " + nombreTirador + " rebota en la barrera.", 0, 0, e);
        }
        Escena.Fin fin = definirRival(pGolRival);
        tiroAlArco(e, false, fin, "Patea " + nombreTirador);
        return new Resultado(intro + narrarRival(fin, nombreTirador), 0, fin == Escena.Fin.GOL_CONTRA ? 1 : 0, e);
    }

    /** Penal en contra: el usuario elige hacia dónde se tira su arquero (-1, 0, 1). */
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

    
    public int prepararPenalContra(int idx) {
        double rr = rnd.nextDouble();
        int lado = rr < 0.38 ? -1 : (rr < 0.62 ? 0 : 1);
        boolean alto = rnd.nextDouble() < (lado == 0 ? 0.25 : 0.45);
        rivX = lado * 0.78 + (lado == 0 ? (rnd.nextDouble() - 0.5) * 0.3 : 0);
        rivY = lado == 0 ? (alto ? 0.70 : 0.30) : (alto ? 0.78 : 0.22);
        rivPot = 0.60 + rnd.nextDouble() * 0.35;
        rivTirador = tiradorRival();
        rivNumero = 7 + rnd.nextInt(5);
        boolean acierta = rnd.nextDouble() < 0.62;
        rivPista = acierta ? lado : (lado == 0 ? (rnd.nextBoolean() ? 1 : -1) : (rnd.nextBoolean() ? 0 : -lado));
        rivListo = true;
        return rivPista;
    }

   
    public Resultado atajar(int idx, double dx, double dy, double momento) {
        if (!rivListo) prepararPenalContra(idx);
        rivListo = false;
        boolean seTiro = momento >= 0;
        dx = clamp(dx, -1.0, 1.0);
        dy = clamp(dy, 0.0, 1.0);

        double sigma = clamp(0.09 - (rivAtaque - 75) * 0.003, 0.05, 0.14);
        double fx = rivX + rnd.nextGaussian() * sigma;
        double fy = rivY + rnd.nextGaussian() * sigma * 0.8;
        if (rivPot > 0.92) fy += (rivPot - 0.92) * 3.5;
        fy = Math.max(0.02, fy);
        boolean leido = false;
        if (seTiro && momento < 0.40 && Math.abs(dx) > 0.25 && rnd.nextDouble() < 0.5) {
            fx = -Math.signum(dx) * Math.max(0.45, Math.abs(fx));       // el pateador lo vio venir y cambia de lado
            leido = true;
        }

        double kx = seTiro ? clamp(dx + rnd.nextGaussian() * 0.07, -0.95, 0.95) : 0.0;
        double ky = seTiro ? clamp(dy + rnd.nextGaussian() * 0.07, 0.08, 0.95) : 0.40;

        boolean dentro = Math.abs(fx) <= 1.0 && fy <= 1.0;
        Escena.Fin fin;
        if (!dentro) {
            boolean roza = Math.abs(fx) <= 1.08 && fy <= 1.08;
            fin = (roza && rnd.nextDouble() < 0.5) ? Escena.Fin.PALO : Escena.Fin.AFUERA;
        } else if ((Math.abs(fx) > 0.94 || fy > 0.94) && rnd.nextDouble() < 0.35) {
            fin = Escena.Fin.PALO;
        } else {
            double reach = 0.46 + (arquero() - 75) * 0.004 - (rivPot - 0.5) * 0.30;
            double borde = Math.max(Math.abs(fx), fy);
            if (borde > 0.70) reach -= (borde - 0.70) * 1.0;
            if (!seTiro) reach *= 0.70;                                 // quedarse parado es peor que arriesgar
            if (seTiro && momento > 0.90) reach *= 0.75;                // se tiró tarde
            double dist = Math.hypot(fx - kx, (fy - ky) * 0.75);
            if (dist < Math.max(0.10, reach)) {
                fin = Escena.Fin.ATAJADA;
                kx = clamp(fx, -0.95, 0.95);                           // llega justo
                ky = clamp(fy, 0.08, 0.95);
            } else {
                fin = Escena.Fin.GOL_CONTRA;
            }
        }
        Escena e = new Escena(0.12, 0.50).patada(false, rivTirador, ap(GK), rivX, rivY, fx, fy, kx, ky, rivPot, -1);
        e.rival = true;
        e.fin(fin, banner(fin));

        String intro = !seTiro ? "Te quedaste parado en el medio. "
                : "Te tirás " + describirSalto(dx, dy) + (leido ? " y " + rivTirador + " te lee y cambia de lado. " : ". ");
        return new Resultado(intro + narrarRival(fin, rivTirador), 0, fin == Escena.Fin.GOL_CONTRA ? 1 : 0, e);
    }

    private static String describirSalto(double dx, double dy) {
        String lado = dx < -0.35 ? "a tu izquierda" : (dx > 0.35 ? "a tu derecha" : "al medio");
        return lado + (dy > 0.55 ? ", arriba" : ", abajo");
    }

    
    private Resultado resolverPatada(boolean libre, int k, double ax, double ay, double pot) {
        ax = clamp(ax, -1.3, 1.3);
        ay = clamp(ay, 0.0, 1.3);
        pot = clamp(pot, 0.0, 1.0);
        int rk = rating[k];

        double ideal = libre ? 0.78 : 0.72;
        double sigma = clamp(0.09 - (rk - 75) * 0.004, 0.04, 0.14) + Math.abs(pot - ideal) * 0.30 + (libre ? 0.08 : 0.0);
        double fx = ax + rnd.nextGaussian() * sigma;
        double fy = ay + rnd.nextGaussian() * sigma * 0.8;
        if (pot > 0.92) fy += (pot - 0.92) * 3.5;     // con toda la fuerza se le puede ir por arriba
        fy = Math.max(0.02, fy);

        double kx, ky;
        double r = rnd.nextDouble();
        if (r < 0.18) {
            kx = 0;
            ky = 0.40;
        } else if (r < 0.18 + clamp(0.34 + aj(rivArquero, rk) * 1.5 + (libre ? 0.08 : 0), 0.20, 0.55)) {
            kx = fx + rnd.nextGaussian() * 0.18;
            ky = fy + rnd.nextGaussian() * 0.14;
        } else {
            kx = (rnd.nextDouble() * 2 - 1) * 0.85;
            ky = 0.1 + rnd.nextDouble() * 0.8;
        }
        kx = clamp(kx, -0.95, 0.95);
        ky = clamp(ky, 0.08, 0.95);

        boolean dentro = Math.abs(fx) <= 1.0 && fy <= 1.0;
        int muro = -1;
        Escena.Fin fin;
        if (libre && Math.abs(fx) < 0.46 && fy < 0.46 + (1 - pot) * 0.12 && rnd.nextDouble() < 0.85) {
            fin = Escena.Fin.PERDIDA;                   // la barrera la tapa
            muro = (int) clamp((fx + 0.46) / 0.92 * 4, 0, 3);
            kx = 0;
            ky = 0.3;
        } else if (!dentro) {
            boolean roza = Math.abs(fx) <= 1.08 && fy <= 1.08;
            fin = (roza && rnd.nextDouble() < 0.5) ? Escena.Fin.PALO : Escena.Fin.AFUERA;
        } else if ((Math.abs(fx) > 0.94 || fy > 0.94) && rnd.nextDouble() < 0.35) {
            fin = Escena.Fin.PALO;
        } else {
            double reach = 0.42 + (rivArquero - 75) * 0.004 - (pot - 0.5) * 0.30 + (pot < 0.3 ? 0.12 : 0)
                    + (libre ? 0.10 : 0);
            double borde = Math.max(Math.abs(fx), fy);
            if (borde > 0.70) reach -= (borde - 0.70) * 2.0;      // los ángulos son muy difíciles de llegar
            double dist = Math.hypot(fx - kx, (fy - ky) * 1.15);
            if (dist < Math.max(libre ? 0.27 : 0.10, reach)) {
                fin = Escena.Fin.ATAJADA;
                kx = clamp(fx, -0.95, 0.95);                      // llega justo a la pelota
                ky = clamp(fy, 0.08, 0.95);
            } else {
                fin = Escena.Fin.GOL_FAVOR;
            }
        }
        String nombre = ap(k);
        Escena e = new Escena(0.5, 0.5).patada(libre, nombre, arqRivalNombre, ax, ay, fx, fy, kx, ky, pot, muro);
        e.fin(fin, fin == Escena.Fin.PERDIDA ? "¡LA BARRERA!" : banner(fin));
        String intro = (libre ? "Ponés la pelota " : "Pateás ") + describirTiro(ax, ay, pot) + ". ";
        String final_ = fin == Escena.Fin.PERDIDA
                ? "El remate de " + nombre + " pega en la barrera."
                : narrarNuestro(fin, nombre);
        return new Resultado(intro + final_, fin == Escena.Fin.GOL_FAVOR ? 1 : 0, 0, e);
    }

    private static String describirTiro(double ax, double ay, double pot) {
        String lado = ax < -0.35 ? "a la izquierda" : (ax > 0.35 ? "a la derecha" : "al medio");
        String alto = ay > 0.62 ? "arriba" : (ay < 0.28 ? "a ras del piso" : "a media altura");
        String fuerza = pot < 0.35 ? "suave" : (pot < 0.65 ? "con medida" : (pot < 0.90 ? "con potencia" : "con toda la fuerza"));
        return lado + ", " + alto + ", " + fuerza;
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
                e.remate(gx + dir * 0.03, rnd.nextBoolean() ? 0.30 : 0.70, texto);
                ladoArq = 0;
            }
        }
        e.arquero(!derecha, ladoArq);
        e.fin(fin, banner(fin));
    }

    private Escena pases(boolean ok, String verbo, int... slots) {
        Escena e = new Escena(propia[slots[0]][0], propia[slots[0]][1]);
        for (int i = 1; i < slots.length; i++) {
            double[] from = propia[slots[i - 1]];
            double[] to = propia[slots[i]];
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


    private String tiradorRival() {
        return tiradoresRival.get(rnd.nextInt(tiradoresRival.size()));
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
