package dao;

import java.sql.SQLException;

import model.Club;

public interface ClubDAO {
    Club obtenerAleatorio() throws SQLException; 
    Club buscarPorId(int id) throws SQLException;
}