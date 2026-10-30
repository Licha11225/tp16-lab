package daoimpl;

import config.ConexionBD;
import dao.onceidealDAO;
import model.jugador;
import model.onceideal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class onceidealdaoimpl implements onceidealDAO {

    @Override
    public void insertarOnce(onceideal once) throws SQLException {
        String sqlOnce = "INSERT INTO once_guardado (usuario) VALUES (?)";
        String sqlDetalle = "INSERT INTO once_jugador (once_id, jugador_id) VALUES (?, ?)";

        Connection conn = null;

        try {
            conn = ConexionBD.getConexion();
            
            // Apagamos el AutoCommit para iniciar la transacción manualmente
            conn.setAutoCommit(false); 

            // 1. Guardar el registro principal en 'once_guardado'
            try (PreparedStatement psOnce = conn.prepareStatement(sqlOnce, Statement.RETURN_GENERATED_KEYS)) {
                psOnce.setString(1, once.getNombreUsuario());
                psOnce.executeUpdate();

                // Recuperar el ID que MySQL le asignó a este Once
                try (ResultSet rs = psOnce.getGeneratedKeys()) {
                    if (rs.next()) {
                        once.setId(rs.getInt(1)); 
                    } else {
                        throw new SQLException("Fallo al obtener el ID del 11 guardado.");
                    }
                }
            }

            // 2. Guardar los 11 jugadores en la tabla intermedia 'once_jugador'
            try (PreparedStatement psDetalle = conn.prepareStatement(sqlDetalle)) {
                
                // CAMBIO AQUÍ: 'jugador' en minúscula
                for (jugador jugador : once.getListaJugadores()) {
                    psDetalle.setInt(1, once.getId());       // El ID del once que acabamos de crear
                    psDetalle.setInt(2, jugador.getId());    // El ID del jugador actual de la lista
                    psDetalle.addBatch();                    // Lo sumamos al lote (batch)
                }
                // Ejecutamos todos los inserts juntos para mayor rendimiento
                psDetalle.executeBatch(); 
            }

            // Si todo salió bien, confirmamos (Commit) los cambios en la BDD
            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                // Si hubo un error en cualquier punto, deshacemos todo lo que hicimos (Rollback)
                conn.rollback(); 
            }
            throw e; // Volvemos a lanzar la excepción para que el Service o Main se enteren
        } finally {
            if (conn != null) {
                // Volvemos a dejar la conexión como estaba y la cerramos
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }
}