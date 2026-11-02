    package daoimpl;

    import config.ConexionBD;
    import dao.jugadorDAO;
    import model.*;

    import java.sql.*;
    import java.util.ArrayList;
    import java.util.List;

    public class jugadordaoimpl implements jugadorDAO {

    @Override
    public void insertar(jugador jugador) throws SQLException {
        String sql = "INSERT INTO jugador (nombre, club_id, posicion, juega_adelantado, es_zaguero_central, es_5, es_9) VALUES (?, ?, ?, ?, ?, ?, ?)";

        String posicion = "";
        boolean juegaAdelantado = false;
        boolean esZagueroCentral = false;
        boolean es5 = false;
        boolean es9 = false;

        if (jugador instanceof arquero) {
            posicion = "ARQUERO";
            juegaAdelantado = ((arquero) jugador).getJuegaAdelantado();
        } else if (jugador instanceof defensor) {
            posicion = "DEFENSOR";
            esZagueroCentral = ((defensor) jugador).getEsZagueroCentral();
        } else if (jugador instanceof mediocampista) {
            posicion = "MEDIOCAMPISTA";
            es5 = ((mediocampista) jugador).getEs5();
        } else if (jugador instanceof delantero) {
            posicion = "DELANTERO";
            es9 = ((delantero) jugador).getEs9();
        }

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, jugador.getNombre());
            ps.setInt(2, jugador.getClubId());
            ps.setString(3, posicion);
            ps.setBoolean(4, juegaAdelantado);
            ps.setBoolean(5, esZagueroCentral);
            ps.setBoolean(6, es5);
            ps.setBoolean(7, es9);

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
        String sql = "SELECT j.*, jp.posicion FROM jugador j LEFT JOIN jugador_posicion jp ON j.id = jp.jugador_id WHERE j.id = ?";
        
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
        String sql = "SELECT j.*, jp.posicion FROM jugador j LEFT JOIN jugador_posicion jp ON j.id = jp.jugador_id WHERE j.nombre = ?";
        
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
        String sql = "SELECT j.*, jp.posicion FROM jugador j LEFT JOIN jugador_posicion jp ON j.id = jp.jugador_id";

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
        String posicion = rs.getString("posicion");
        
        if (posicion != null) {
            posicion = posicion.toUpperCase().trim();
        } else {
            posicion = "";
        }
        
        boolean juegaAdelantado = rs.getBoolean("juega_adelantado");
        boolean esZagueroCentral = rs.getBoolean("es_zaguero_central");
        boolean es5 = rs.getBoolean("es_5");
        boolean es9 = rs.getBoolean("es_9");

        switch (posicion) {
            case "GK":
            case "ARQUERO":
                return new arquero(id, nombre, clubId, juegaAdelantado);
                
            case "CB":
            case "LB":
            case "RB":
            case "LWB":
            case "RWB":
            case "DEFENSOR":
                return new defensor(id, nombre, clubId, esZagueroCentral);
                
            case "MC":
            case "MCD":
            case "MCO":
            case "LM":
            case "RM":
            case "MEDIOCAMPISTA":
                return new mediocampista(id, nombre, clubId, es5);
                
            case "ST":
            case "LW":
            case "RW":
            case "CF":
            case "DELANTERO":
                return new delantero(id, nombre, clubId, es9);
                
            default:
                throw new IllegalArgumentException("Posición desconocida en BDD: " + posicion);
        }
    }
    }