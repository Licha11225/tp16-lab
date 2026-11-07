package dao;

import java.sql.SQLException;

import model.club;

public interface clubDAO {
    club obtenerAleatorio() throws SQLException; 
    club buscarPorId(int id) throws SQLException;
}