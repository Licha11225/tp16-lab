package daoimpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import dao.JugadorDAO;
import model.Jugador;

public class Jugadordaomemoria implements JugadorDAO {

    private final List<Jugador> jugadores = DatosDemo.jugadores();

    @Override
    public void insertar(Jugador j) {
        j.setId(jugadores.size() + 1);
        jugadores.add(j);
    }

    @Override
    public Jugador buscarPorId(int id) {
        for (Jugador j : jugadores) {
            if (j.getId() == id) return j;
        }
        return null;
    }

    @Override
    public List<Jugador> listarTodos() {
        return new ArrayList<>(jugadores);
    }

    @Override
    public Jugador buscarPorNombre(String nombre) {
        for (Jugador j : jugadores) {
            if (j.getNombre().equalsIgnoreCase(nombre.trim())) return j;
        }
        return null;
    }
    @Override
    public List<Jugador> obtenerJugadoresUnicos() {
        List<Jugador> unicos = new ArrayList<>();
        for (Jugador j : jugadores) {
            boolean encontrado = false;
            for (Jugador u : unicos) {
                if (Objects.equals(u.getId(), j.getId())) {
                    encontrado = true;
                    break;
                }
            }
            if (!encontrado) {
                unicos.add(j);
            }
        }
        return unicos;
    }
}
