package daoimpl;

import dao.jugadorDAO;
import model.jugador;

import java.util.ArrayList;
import java.util.List;

public class jugadordaomemoria implements jugadorDAO {

    private final List<jugador> jugadores = DatosDemo.jugadores();

    @Override
    public void insertar(jugador j) {
        j.setId(jugadores.size() + 1);
        jugadores.add(j);
    }

    @Override
    public jugador buscarPorId(int id) {
        for (jugador j : jugadores) {
            if (j.getId() == id) return j;
        }
        return null;
    }

    @Override
    public List<jugador> listarTodos() {
        return new ArrayList<>(jugadores);
    }

    @Override
    public jugador buscarPorNombre(String nombre) {
        for (jugador j : jugadores) {
            if (j.getNombre().equalsIgnoreCase(nombre.trim())) return j;
        }
        return null;
    }
}
