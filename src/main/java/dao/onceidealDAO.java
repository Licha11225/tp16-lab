package dao;

import model.onceideal; 
import java.sql.SQLException;

public interface onceidealDAO { // El nombre de la interfaz ahora coincide con tu archivo
    void insertarOnce(onceideal once) throws SQLException; // Recibe onceideal en minúscula
}