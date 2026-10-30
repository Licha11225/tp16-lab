package daoimpl;

import config.ConexionBD;
import dao.clubDAO;
import model.club; // Importado en minúscula según la estructura de tu proyecto

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class clubdaoimpl implements clubDAO {

    @Override
    public club obtenerAleatorio() throws SQLException { // Cambiado de Club a club
        // En MySQL, ORDER BY RAND() LIMIT 1 te trae una sola fila al azar
        String sql = "SELECT * FROM club ORDER BY RAND() LIMIT 1";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                // Nota: El modelo club tiene "liga", pero tu tabla SQL no lo tiene. 
                // Por eso le pasamos un string vacío o null.
                return new club(rs.getInt("id"), rs.getString("nombre"), ""); // Cambiado de Club a club
            }
        }
        return null; // Retorna null si la tabla está vacía
    }

    @Override
    public club buscarPorId(int id) throws SQLException { // Cambiado de Club a club
        String sql = "SELECT * FROM club WHERE id = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new club(rs.getInt("id"), rs.getString("nombre"), ""); // Cambiado de Club a club
                }
            }
        }
        return null;
    }
}