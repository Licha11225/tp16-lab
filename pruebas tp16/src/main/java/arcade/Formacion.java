package arcade;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Formaciones tácticas. Los once índices se conservan para no romper el motor
 * de partido; lo que cambia es el rol y la ubicación de cada casillero.
 */
public final class Formacion {
    public static final int GK=0, LB=1, CB1=2, CB2=3, RB=4, MCD=5, MC=6, MCO=7, LW=8, ST=9, RW=10;
    public static final String[] ETIQUETAS={"GK","LB","CB1","CB2","RB","MCD","MC","MCO","LW","ST","RW"};
    public static final String[] CASILLEROS={"GK","LB","CB","CB","RB","MCD","MC","MCO","LW","ST","RW"};
    public static final double[][] PROPIA=new double[11][2];
    public static final double[][] RIVAL=new double[11][2];

    private static final Map<String, String[]> ROLES = new LinkedHashMap<>();
    private static final Map<String, double[][]> POS = new LinkedHashMap<>();
    private static String actual="4-3-3";

    static {
        registrar("4-3-3", new String[]{"GK","LB","CB","CB","RB","MCD","MC","MCO","LW","ST","RW"},
            new double[][]{{.06,.50},{.22,.14},{.20,.38},{.20,.62},{.22,.86},{.43,.50},{.53,.30},{.53,.70},{.76,.14},{.84,.50},{.76,.86}});
        registrar("4-4-2", new String[]{"GK","LB","CB","CB","RB","LM","MC","MC","RM","ST","ST"},
            new double[][]{{.06,.50},{.22,.14},{.20,.38},{.20,.62},{.22,.86},{.48,.12},{.48,.38},{.48,.62},{.48,.88},{.82,.35},{.82,.65}});
        registrar("4-3-1-2", new String[]{"GK","LB","CB","CB","RB","MC","MC","MC","MCO","ST","ST"},
            new double[][]{{.06,.50},{.22,.14},{.20,.38},{.20,.62},{.22,.86},{.43,.50},{.55,.28},{.64,.50},{.82,.28},{.86,.50},{.82,.72}});
        registrar("3-5-2", new String[]{"GK","CB","CB","CB","RB","LM","MC","MC","RM","ST","ST"},
            new double[][]{{.06,.50},{.22,.18},{.22,.50},{.22,.82},{.50,.70},{.50,.10},{.50,.30},{.50,.50},{.50,.90},{.84,.35},{.84,.65}});
        registrar("5-3-2", new String[]{"GK","LB","CB","CB","RB","CB","MC","MC","MC","ST","ST"},
            new double[][]{{.06,.50},{.22,.10},{.22,.30},{.22,.70},{.22,.90},{.22,.50},{.54,.25},{.54,.50},{.54,.75},{.84,.35},{.84,.65}});
        registrar("3-4-3", new String[]{"GK","CB","CB","CB","RM","LM","MC","MC","LW","ST","RW"},
            new double[][]{{.06,.50},{.22,.20},{.20,.50},{.22,.80},{.48,.90},{.48,.10},{.53,.36},{.53,.64},{.78,.14},{.84,.50},{.78,.86}});
        registrar("4-2-3-1", new String[]{"GK","LB","CB","CB","RB","MCD","MCD","MCO","LW","ST","RW"},
            new double[][]{{.06,.50},{.22,.14},{.20,.38},{.20,.62},{.22,.86},{.40,.35},{.40,.65},{.62,.50},{.72,.14},{.84,.50},{.72,.86}});
        registrar("4-1-4-1", new String[]{"GK","LB","CB","CB","RB","MCD","LM","MC","MC","RM","ST"},
            new double[][]{{.06,.50},{.22,.14},{.20,.38},{.20,.62},{.22,.86},{.40,.50},{.58,.12},{.58,.34},{.58,.66},{.58,.88},{.86,.50}});
        registrar("4-5-1", new String[]{"GK","LB","CB","CB","RB","MCD","LM","MC","RM","MCO","ST"},
            new double[][]{{.06,.50},{.22,.14},{.22,.38},{.22,.62},{.22,.86},{.50,.10},{.50,.30},{.50,.50},{.50,.70},{.50,.90},{.84,.50}});
        registrar("3-4-2-1", new String[]{"GK","CB","CB","CB","LM","MC","MC","RM","MCO","MCO","ST"},
            new double[][]{{.06,.50},{.22,.20},{.20,.50},{.22,.80},{.48,.10},{.50,.36},{.50,.64},{.48,.90},{.68,.35},{.68,.65},{.86,.50}});
        registrar("5-4-1", new String[]{"GK","LB","CB","CB","RB","CB","LM","MC","MC","RM","ST"},
            new double[][]{{.06,.50},{.22,.10},{.22,.30},{.22,.70},{.22,.90},{.22,.50},{.50,.12},{.50,.36},{.50,.64},{.50,.88},{.86,.50}});
        registrar("4-1-2-1-2", new String[]{"GK","LB","CB","CB","RB","MCD","MC","MC","MCO","ST","ST"},
            new double[][]{{.06,.50},{.22,.14},{.20,.38},{.20,.62},{.22,.86},{.40,.50},{.54,.28},{.54,.72},{.68,.50},{.84,.35},{.84,.65}});
        registrar("4-4-1-1", new String[]{"GK","LB","CB","CB","RB","LM","MC","MC","RM","MCO","ST"},
            new double[][]{{.06,.50},{.22,.14},{.20,.38},{.20,.62},{.22,.86},{.47,.12},{.48,.38},{.48,.62},{.47,.88},{.68,.50},{.86,.50}});
        registrar("4-3-2-1", new String[]{"GK","LB","CB","CB","RB","MCD","MC","MC","MCO","MCO","ST"},
            new double[][]{{.06,.50},{.22,.14},{.20,.38},{.20,.62},{.22,.86},{.42,.50},{.54,.32},{.54,.68},{.69,.34},{.69,.66},{.86,.50}});
        registrar("4-2-2-2", new String[]{"GK","LB","CB","CB","RB","MCD","MCD","MCO","MCO","ST","ST"},
            new double[][]{{.06,.50},{.22,.14},{.20,.38},{.20,.62},{.22,.86},{.42,.34},{.42,.66},{.65,.30},{.65,.70},{.84,.35},{.84,.65}});
        registrar("5-2-3", new String[]{"GK","LB","CB","CB","RB","CB","MCD","MC","LW","ST","RW"},
            new double[][]{{.06,.50},{.22,.10},{.22,.30},{.22,.70},{.22,.90},{.22,.50},{.50,.38},{.50,.62},{.82,.14},{.86,.50},{.82,.86}});
        registrar("3-3-3-1", new String[]{"GK","CB","CB","CB","LM","MC","RM","LW","MCO","RW","ST"},
            new double[][]{{.06,.50},{.22,.20},{.20,.50},{.22,.80},{.45,.12},{.48,.50},{.45,.88},{.70,.14},{.68,.50},{.70,.86},{.86,.50}});
        registrar("3-1-4-2", new String[]{"GK","CB","CB","CB","MCD","LM","MC","MC","RM","ST","ST"},
            new double[][]{{.06,.50},{.22,.20},{.20,.50},{.22,.80},{.40,.50},{.54,.12},{.55,.38},{.55,.62},{.54,.88},{.84,.35},{.84,.65}});
        setActual(actual);
    }

    private static void registrar(String nombre, String[] roles, double[][] coords) {
        ROLES.put(nombre, roles);
        POS.put(nombre, coords);
    }
    public static String[] nombresDisponibles(){ return ROLES.keySet().toArray(String[]::new); }
    public static String getActual(){ return actual; }
    public static void setActual(String nombre) {
        if (!ROLES.containsKey(nombre)) nombre="4-3-3";
        actual=nombre;
        String[] roles=ROLES.get(nombre);
        double[][] coords=POS.get(nombre);
        for(int i=0;i<11;i++){
            ETIQUETAS[i]=roles[i] + (roles[i].equals("CB") ? (i==1?"":"") : "");
            CASILLEROS[i]=roles[i];
            PROPIA[i][0]=coords[i][0]; PROPIA[i][1]=coords[i][1];
            RIVAL[i][0]=1.0-coords[i][0]; RIVAL[i][1]=coords[i][1];
        }
        ETIQUETAS[2]="CB1"; ETIQUETAS[3]="CB2";
        if(roles[9].equals("ST") && roles[10].equals("ST")) { ETIQUETAS[9]="ST1"; ETIQUETAS[10]="ST2"; }
    }
    private Formacion(){}
}
