package arcade;

import model.Arquero;
import model.Defensor;
import model.Delantero;
import model.Jugador;
import model.Mediocampista;

public final class Ratings {

    private Ratings() {
    }

    public static int de(Jugador j) {
        int base = 66 + Math.floorMod(j.getNombre().toLowerCase().hashCode(), 25); // 66..90
        boolean rasgo = false;
        switch (j) {
            case Arquero a -> rasgo = a.getJuegaAdelantado();
            case Defensor d -> rasgo = d.getEsZagueroCentral();
            case Mediocampista m -> rasgo = m.getEs5();
            case Delantero f -> rasgo = f.getEs9();
            default -> {
            }
        }
        return Math.min(95, base + (rasgo ? 3 : 0));
    }

    public static String posicion(Jugador j) {
        if (j instanceof Arquero) return "Arquero";
        if (j instanceof Defensor) return "Defensor";
        if (j instanceof Mediocampista) return "Mediocampista";
        return "Delantero";
    }

    public static int orden(Jugador j) {
        if (j instanceof Arquero) return 0;
        if (j instanceof Defensor) return 1;
        if (j instanceof Mediocampista) return 2;
        return 3;
    }

    public static String corto(Jugador j) {
        String a = j.getApellido();
        return (a != null && !a.isBlank()) ? a.trim() : apellido(j.getNombre());
    }

    public static String apellido(String nombre) {
        if (nombre == null) return "";
        String n = nombre.trim();
        int i = n.lastIndexOf(' ');
        return i < 0 ? n : n.substring(i + 1);
    }
}
