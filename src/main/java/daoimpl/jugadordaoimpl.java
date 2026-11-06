package daoimpl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import config.ConexionBD;
import dao.jugadorDAO;
import model.arquero;
import model.defensor;
import model.delantero;
import model.jugador;
import model.mediocampista;

public class jugadordaoimpl implements jugadorDAO {

    @Override
    public List<jugador> obtenerJugadoresUnicos() {
        try {
            return listarTodos();
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public List<jugador> listarTodos() throws SQLException {
        Map<Integer, jugador> mapaUnicos = new LinkedHashMap<>();
        String sql = "SELECT j.*, jp.posicion FROM jugador j LEFT JOIN jugador_posicion jp ON j.id = jp.jugador_id";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String posicionBD = rs.getString("posicion");

                jugador j = mapaUnicos.get(id);
                if (j == null) {
                    j = mapearJugador(rs);
                    mapaUnicos.put(id, j);
                }
                if (posicionBD != null && !posicionBD.isEmpty()) {
                    j.agregarPosicion(posicionBD);
                }
            }
        }
        return new ArrayList<>(mapaUnicos.values());
    }

    @Override
    public void insertar(jugador jugador) throws SQLException {
        String sql = "INSERT INTO jugador (nombre, apellido, club_id, posicion, juega_adelantado, es_zaguero_central, es_5, es_9) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

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
            ps.setString(2, jugador.getApellido());
            ps.setInt(3, jugador.getClubId());
            ps.setString(4, posicion);
            ps.setBoolean(5, juegaAdelantado);
            ps.setBoolean(6, esZagueroCentral);
            ps.setBoolean(7, es5);
            ps.setBoolean(8, es9);

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
        jugador j = null;
        
        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (j == null) {
                        j = mapearJugador(rs);
                    }
                    String posicionBD = rs.getString("posicion");
                    if (posicionBD != null && !posicionBD.isEmpty()) {
                        j.agregarPosicion(posicionBD);
                    }
                }
            }
        }
        return j;
    }
    
    @Override
    public jugador buscarPorNombre(String nombre) throws SQLException {
        String sql = "SELECT j.*, jp.posicion FROM jugador j LEFT JOIN jugador_posicion jp ON j.id = jp.jugador_id WHERE j.nombre = ?";
        jugador j = null;
        
        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nombre);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (j == null) {
                        j = mapearJugador(rs);
                    }
                    String posicionBD = rs.getString("posicion");
                    if (posicionBD != null && !posicionBD.isEmpty()) {
                        j.agregarPosicion(posicionBD);
                    }
                }
            }
        }
        return j;
    }

    private jugador mapearJugador(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String nombre = rs.getString("nombre");
        String apellido = rs.getString("apellido");
        int clubId = rs.getInt("club_id");
        
        boolean juegaAdelantado = rs.getBoolean("juega_adelantado");
        boolean esZagueroCentral = rs.getBoolean("es_zaguero_central");
        boolean es5 = rs.getBoolean("es_5");
        boolean es9 = rs.getBoolean("es_9");

        String tipoBase = rs.getString("posicion");
        if (tipoBase == null) tipoBase = "";
        tipoBase = tipoBase.toUpperCase().trim();

        if (tipoBase.contains("DEFENSOR") || tipoBase.equals("CB") || tipoBase.equals("LB") || tipoBase.equals("RB") || tipoBase.equals("LWB") || tipoBase.equals("RWB")) {
            return new defensor(id, nombre, apellido, clubId, esZagueroCentral);
        } else if (tipoBase.contains("MEDIO") || tipoBase.equals("MC") || tipoBase.equals("MCD") || tipoBase.equals("MCO") || tipoBase.equals("LM") || tipoBase.equals("RM")) {
            return new mediocampista(id, nombre, apellido, clubId, es5);
        } else if (tipoBase.contains("DELANTERO") || tipoBase.equals("ST") || tipoBase.equals("LW") || tipoBase.equals("RW") || tipoBase.equals("CF")) {
            return new delantero(id, nombre, apellido, clubId, es9);
        } else {
            return new arquero(id, nombre, apellido, clubId, juegaAdelantado);
        }
    }
}