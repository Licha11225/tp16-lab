package dao;

import model.jugador; // Importado en minúscula
import java.sql.SQLException;
import java.util.List;

public interface jugadorDAO { // El nombre de la interfaz ahora coincide con tu archivo
    void insertar(jugador jugador) throws SQLException; // Recibe jugador en minúscula
    jugador buscarPorId(int id) throws SQLException; // Retorna jugador en minúscula
    List<jugador> listarTodos() throws SQLException;
    jugador buscarPorNombre(String nombre) throws SQLException;
}