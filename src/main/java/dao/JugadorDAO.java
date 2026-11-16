package dao;

import java.sql.SQLException; 
import java.util.List;

import model.Jugador;

public interface JugadorDAO { 
    void insertar(Jugador Jugador) throws SQLException; 
    Jugador buscarPorId(int id) throws SQLException; 
    List<Jugador> listarTodos() throws SQLException;
    Jugador buscarPorNombre(String nombre) throws SQLException;
    List<Jugador> obtenerJugadoresUnicos() throws SQLException;
}