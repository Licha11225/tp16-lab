package daoimpl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import config.ConexionBD;
import dao.OnceidealDAO;
import model.Jugador;
import model.Onceideal;

public class Onceidealdaoimpl implements OnceidealDAO {

    @Override
    public void insertarOnce(Onceideal once) throws SQLException {
        String sqlOnce = "INSERT INTO once_guardado (usuario) VALUES (?)";
        String sqlDetalle = "INSERT INTO once_jugador (once_id, jugador_id) VALUES (?, ?)";

        Connection conn = null;

        try {
            conn = ConexionBD.getConexion();
            
            conn.setAutoCommit(false); 

            try (PreparedStatement psOnce = conn.prepareStatement(sqlOnce, Statement.RETURN_GENERATED_KEYS)) {
                psOnce.setString(1, once.getNombreUsuario());
                psOnce.executeUpdate();

                try (ResultSet rs = psOnce.getGeneratedKeys()) {
                    if (rs.next()) {
                        once.setId(rs.getInt(1)); 
                    } else {
                        throw new SQLException("Fallo al obtener el ID del 11 guardado.");
                    }
                }
            }

            try (PreparedStatement psDetalle = conn.prepareStatement(sqlDetalle)) {
                
                for (Jugador Jugador : once.getListaJugadores()) {
                    psDetalle.setInt(1, once.getId());      
                    psDetalle.setInt(2, Jugador.getId());    
                    psDetalle.addBatch();                    
                }
                psDetalle.executeBatch(); 
            }

            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback(); 
            }
            throw e;  
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }
}