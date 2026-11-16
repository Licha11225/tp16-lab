package dao;

import java.sql.SQLException;

import model.Onceideal;

public interface OnceidealDAO {
    void insertarOnce(Onceideal once) throws SQLException;
}