package arcade;

public final class Formacion {

    public static final int GK = 0, LB = 1, CB1 = 2, CB2 = 3, RB = 4,
            MCD = 5, MC = 6, MCO = 7, LW = 8, ST = 9, RW = 10;

    public static final String[] ETIQUETAS = {"GK", "LB", "CB1", "CB2", "RB", "MCD", "MC", "MCO", "LW", "ST", "RW"};

    public static final String[] CASILLEROS = {"GK", "LB", "CB", "CB", "RB", "MCD", "MC", "MCO", "LW", "ST", "RW"};

    public static final double[][] PROPIA = {
            {0.06, 0.50},                                            // GK
            {0.22, 0.14}, {0.20, 0.38}, {0.20, 0.62}, {0.22, 0.86}, // LB CB CB RB
            {0.38, 0.50}, {0.52, 0.30}, {0.56, 0.70},               // MCD MC MCO
            {0.76, 0.14}, {0.82, 0.50}, {0.76, 0.86}                // LW ST RW
    };

    public static final double[][] RIVAL = {
            {0.95, 0.50},
            {0.86, 0.14}, {0.88, 0.38}, {0.88, 0.62}, {0.86, 0.86},
            {0.68, 0.14}, {0.68, 0.38}, {0.68, 0.62}, {0.68, 0.86},
            {0.46, 0.38}, {0.46, 0.62}
    };

    private Formacion() {
    }
}
