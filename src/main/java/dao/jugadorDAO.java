package dao;

import java.sql.SQLException; // Importado en minúscula
import java.util.List;

import model.jugador;

public interface jugadorDAO { // El nombre de la interfaz ahora coincide con tu archivo
    void insertar(jugador jugador) throws SQLException; // Recibe jugador en minúscula
    jugador buscarPorId(int id) throws SQLException; // Retorna jugador en minúscula
    List<jugador> listarTodos() throws SQLException;
    jugador buscarPorNombre(String nombre) throws SQLException;
    List<jugador> obtenerJugadoresUnicos() throws SQLException;
}