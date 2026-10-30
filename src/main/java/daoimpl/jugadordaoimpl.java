package daoimpl;

import config.ConexionBD;
import dao.jugadorDAO;
import model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class jugadordaoimpl implements jugadorDAO {

    @Override
    public void insertar(jugador jugador) throws SQLException { // Cambiado a jugador (minúscula)
        String sql = "INSERT INTO jugador (nombre, club_id, tipo, rasgo_especial) VALUES (?, ?, ?, ?)";

        String tipo = "";
        boolean rasgo = false;

        // Se cambiaron las clases a minúscula según tu estructura
        if (jugador instanceof arquero) {
            tipo = "ARQUERO";
            rasgo = ((arquero) jugador).getJuegaAdelantado();
        } else if (jugador instanceof defensor) {
            tipo = "DEFENSOR";
            rasgo = ((defensor) jugador).getEsZagueroCentral();
        } else if (jugador instanceof mediocampista) {
            tipo = "MEDIOCAMPISTA";
            rasgo = ((mediocampista) jugador).getEs5();
        } else if (jugador instanceof delantero) {
            tipo = "DELANTERO";
            rasgo = ((delantero) jugador).getEs9();
        }

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, jugador.getNombre());
            ps.setInt(2, jugador.getClubId());
            ps.setString(3, tipo);
            ps.setBoolean(4, rasgo);

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    jugador.setId(rs.getInt(1));
                }
            }
        }
    }

    @Override
    public jugador buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM jugador WHERE id = ?";
        
        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearJugador(rs);
                }
            }
        }
        return null;
    }
    
    @Override
    public jugador buscarPorNombre(String nombre) throws SQLException {
        String sql = "SELECT * FROM jugador WHERE nombre = ?";
        
        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nombre);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearJugador(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<jugador> listarTodos() throws SQLException {
        List<jugador> lista = new ArrayList<>();
        String sql = "SELECT * FROM jugador";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearJugador(rs));
            }
        }
        return lista;
    }

    private jugador mapearJugador(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String nombre = rs.getString("nombre");
        int clubId = rs.getInt("club_id");
        String tipo = rs.getString("tipo");
        boolean rasgo = rs.getBoolean("rasgo_especial");

        switch (tipo) {
            case "ARQUERO":
                return new arquero(id, nombre, clubId, rasgo);
            case "DEFENSOR":
                return new defensor(id, nombre, clubId, rasgo);
            case "MEDIOCAMPISTA":
                return new mediocampista(id, nombre, clubId, rasgo);
            case "DELANTERO":
                return new delantero(id, nombre, clubId, rasgo);
            default:
                throw new IllegalArgumentException("Tipo desconocido en BDD: " + tipo);
        }
    }
}