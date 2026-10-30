package dao;

import model.club; // Importado en minúscula
import java.sql.SQLException;

public interface clubDAO { // El nombre de la interfaz ahora coincide con tu archivo
    club obtenerAleatorio() throws SQLException; // Retorna club en minúscula
    club buscarPorId(int id) throws SQLException;
}