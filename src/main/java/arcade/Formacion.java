package arcade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import model.Jugador;


public final class Formacion {

    public static final int GK = 0;

    public static final String[] ETIQUETAS = {"GK", "LB", "CB1", "CB2", "RB", "MCD", "MC", "MCO", "LW", "ST", "RW"};
    public static final String[] CASILLEROS = {"GK", "LB", "CB", "CB", "RB", "MCD", "MC", "MCO", "LW", "ST", "RW"};

    /** Cada formación: líneas separadas por "|", de izquierda a derecha (arriba a abajo en la cancha). */
    private static final String[][] CATALOGO = {
            {"4-3-3", "LB CB CB RB|MCD MC MCO|LW ST RW"},
            {"4-4-2", "LB CB CB RB|LM MC MC RM|ST ST"},
            {"4-2-3-1", "LB CB CB RB|MCD MCD|LW MCO RW|ST"},
            {"4-3-1-2", "LB CB CB RB|MC MCD MC|MCO|ST ST"},
            {"4-1-4-1", "LB CB CB RB|MCD|LM MC MC RM|ST"},
            {"4-5-1", "LB CB CB RB|LM MC MCD MC RM|ST"},
            {"4-4-1-1", "LB CB CB RB|LM MC MC RM|MCO|ST"},
            {"4-2-2-2", "LB CB CB RB|MCD MCD|MCO MCO|ST ST"},
            {"4-3-2-1", "LB CB CB RB|MC MCD MC|MCO MCO|ST"},
            {"3-5-2", "CB CB CB|LM MC MCD MC RM|ST ST"},
            {"3-4-3", "CB CB CB|LM MC MC RM|LW ST RW"},
            {"3-4-1-2", "CB CB CB|LM MC MC RM|MCO|ST ST"},
            {"3-4-2-1", "CB CB CB|LM MC MC RM|MCO MCO|ST"},
            {"3-1-4-2", "CB CB CB|MCD|LM MC MC RM|ST ST"},
            {"5-3-2", "LB CB CB CB RB|MC MCD MC|ST ST"},
            {"5-4-1", "LB CB CB CB RB|LM MC MC RM|ST"},
            {"5-2-3", "LB CB CB CB RB|MCD MC|LW ST RW"},
    };

    public static final List<Formacion> TODAS;

    static {
        List<Formacion> l = new ArrayList<>();
        for (String[] f : CATALOGO) l.add(new Formacion(f[0], f[1]));
        TODAS = Collections.unmodifiableList(l);
    }

    private final String nombre;
    private final String lineas;
    private final String[] casilleros = new String[11];
    private final String[] etiquetas = new String[11];
    private final double[][] propia = new double[11][2];
    private final double[][] rival = new double[11][2];

    private Formacion(String nombre, String lineas) {
        this.nombre = nombre;
        this.lineas = lineas;
        String[] filas = lineas.split("\\|");
        casilleros[0] = "GK";
        propia[0] = new double[]{0.06, 0.50};

        int n = 1;
        for (int f = 0; f < filas.length; f++) {
            String[] cods = filas[f].trim().split("\\s+");
            double xBase = filas.length == 1 ? 0.5 : 0.22 + (0.80 - 0.22) * f / (filas.length - 1);
            boolean ancha = esAncha(cods[0]) || esAncha(cods[cods.length - 1]);
            for (int i = 0; i < cods.length; i++) {
                casilleros[n] = cods[i];
                double x = xBase;
                if (cods[i].equals("MCD")) x -= 0.05;                 // el 5 juega más atrás
                if (cods[i].equals("MCO")) x += 0.04;                 // el 10 juega más adelantado
                if (cods.length >= 5 && (cods[i].equals("LB") || cods[i].equals("RB"))) x += 0.07; // carrileros
                propia[n] = new double[]{x, yFila(cods.length, i, ancha)};
                n++;
            }
        }
        if (n != 11) throw new IllegalStateException("La formación " + nombre + " no suma 11 jugadores");

        for (int i = 0; i < 11; i++) {
            rival[i] = new double[]{1.0 - propia[i][0], propia[i][1]};
        }
        rival[0] = new double[]{0.95, 0.50};

        for (int i = 0; i < 11; i++) {
            int total = 0, orden = 0;
            for (int j = 0; j < 11; j++) {
                if (casilleros[j].equals(casilleros[i])) {
                    total++;
                    if (j <= i) orden++;
                }
            }
            etiquetas[i] = total > 1 ? casilleros[i] + orden : casilleros[i];
        }
    }

    private static boolean esAncha(String c) {
        return c.equals("LB") || c.equals("RB") || c.equals("LM") || c.equals("RM") || c.equals("LW") || c.equals("RW");
    }

    private static double yFila(int n, int i, boolean ancha) {
        switch (n) {
            case 1: return 0.50;
            case 2: return ancha ? (i == 0 ? 0.14 : 0.86) : (i == 0 ? 0.37 : 0.63);
            case 3: return ancha ? new double[]{0.14, 0.50, 0.86}[i] : new double[]{0.24, 0.50, 0.76}[i];
            case 4: return new double[]{0.14, 0.38, 0.62, 0.86}[i];
            default: return 0.12 + 0.76 * i / (n - 1);
        }
    }


    public String getNombre() { return nombre; }
    public String casillero(int i) { return casilleros[i]; }
    public String etiqueta(int i) { return etiquetas[i]; }
    public String[] etiquetas() { return etiquetas.clone(); }
    public String[] casilleros() { return casilleros.clone(); }
    public double[][] propia() { return propia; }
    public double[][] rival() { return rival; }

    @Override
    public String toString() { return nombre; }

    public static int linea(String codigo) {
        switch (codigo) {
            case "GK": return 0;
            case "LB": case "CB": case "RB": return 1;
            case "MCD": case "MC": case "MCO": case "LM": case "RM": return 2;
            default: return 3;
        }
    }

    public int linea(int slot) { return linea(casilleros[slot]); }

    public int cantidad(int linea) {
        int c = 0;
        for (String s : casilleros) if (linea(s) == linea) c++;
        return c;
    }

    public int slotDe(String... codigos) {
        for (String c : codigos) {
            for (int i = 0; i < 11; i++) if (casilleros[i].equals(c)) return i;
        }
        return -1;
    }

    public List<Integer> slotsDe(String codigo) {
        List<Integer> l = new ArrayList<>();
        for (int i = 0; i < 11; i++) if (casilleros[i].equals(codigo)) l.add(i);
        return l;
    }

    
    public static boolean sirve(Jugador j, String casillero) {
        if (j.esUbicacionValida(casillero)) return true;
        if (casillero.equals("LM")) return j.esUbicacionValida("LW");
        if (casillero.equals("RM")) return j.esUbicacionValida("RW");
        return false;
    }

    public static Formacion aleatoria(Random rnd) {
        return TODAS.get(rnd.nextInt(TODAS.size()));
    }

    public static Formacion porNombre(String nombre) {
        for (Formacion f : TODAS) if (f.nombre.equals(nombre)) return f;
        return TODAS.get(0);
    }

    public String descripcion() {
        return lineas.replace("|", "  ·  ");
    }
}
