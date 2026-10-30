package arcade;

import model.arquero;
import model.defensor;
import model.delantero;
import model.jugador;
import model.mediocampista;

/**
 * Como el modelo no guarda estadísticas, el nivel (OVR) de cada jugador sale de su nombre
 * (siempre es el mismo para el mismo jugador) más un bonus por su rasgo especial.
 */
public final class Ratings {

    private Ratings() {
    }

    public static int de(jugador j) {
        int base = 66 + Math.floorMod(j.getNombre().toLowerCase().hashCode(), 25); // 66..90
        boolean rasgo = false;
        if (j instanceof arquero a) {
            rasgo = a.getJuegaAdelantado();
        } else if (j instanceof defensor d) {
            rasgo = d.getEsZagueroCentral();
        } else if (j instanceof mediocampista m) {
            rasgo = m.getEs5();
        } else if (j instanceof delantero f) {
            rasgo = f.getEs9();
        }
        return Math.min(95, base + (rasgo ? 3 : 0));
    }

    public static String tipo(jugador j) {
        if (j instanceof arquero) return "Arquero";
        if (j instanceof defensor) return "Defensor";
        if (j instanceof mediocampista) return "Mediocampista";
        return "Delantero";
    }

    public static int orden(jugador j) {
        if (j instanceof arquero) return 0;
        if (j instanceof defensor) return 1;
        if (j instanceof mediocampista) return 2;
        return 3;
    }

    public static String apellido(String nombre) {
        if (nombre == null) return "";
        String n = nombre.trim();
        int i = n.lastIndexOf(' ');
        return i < 0 ? n : n.substring(i + 1);
    }
}
