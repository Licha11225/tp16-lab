package dao;

import java.sql.SQLException;

import model.onceideal;

public interface onceidealDAO {
    void insertarOnce(onceideal once) throws SQLException;
}