package dao;

import java.sql.SQLException; 
import java.util.List;

import model.jugador;

public interface jugadorDAO { 
    void insertar(jugador jugador) throws SQLException; 
    jugador buscarPorId(int id) throws SQLException; 
    List<jugador> listarTodos() throws SQLException;
    jugador buscarPorNombre(String nombre) throws SQLException;
    List<jugador> obtenerJugadoresUnicos() throws SQLException;
}