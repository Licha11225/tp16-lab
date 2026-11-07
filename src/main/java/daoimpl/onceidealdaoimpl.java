package daoimpl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import config.ConexionBD;
import dao.onceidealDAO;
import model.jugador;
import model.onceideal;

public class onceidealdaoimpl implements onceidealDAO {

    @Override
    public void insertarOnce(onceideal once) throws SQLException {
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
                
                for (jugador jugador : once.getListaJugadores()) {
                    psDetalle.setInt(1, once.getId());      
                    psDetalle.setInt(2, jugador.getId());    
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