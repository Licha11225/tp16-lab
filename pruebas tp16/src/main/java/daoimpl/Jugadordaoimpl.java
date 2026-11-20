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
import dao.JugadorDAO;
import model.Arquero;
import model.Defensor;
import model.Delantero;
import model.Jugador;
import model.Mediocampista;

public class Jugadordaoimpl implements JugadorDAO {

    private static final String SELECT =
            "SELECT j.id, j.nombre, j.club_id, j.juega_adelantado, j.es_zaguero_central, j.es_5, j.es_9, jp.posicion "
                    + "FROM jugador j LEFT JOIN jugador_posicion jp ON j.id = jp.jugador_id ";

    private static class Fila {
        int id, clubId;
        String nombre;
        boolean adelantado, zaguero, es5, es9;
        final List<String> posiciones = new ArrayList<>();
    }

    @Override
    public List<Jugador> obtenerJugadoresUnicos() {
        try {
            return listarTodos();
        } catch (SQLException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public List<Jugador> listarTodos() throws SQLException {
        return consultar(SELECT + "ORDER BY j.id, jp.posicion", null);
    }

    @Override
    public Jugador buscarPorId(int id) throws SQLException {
        List<Jugador> r = consultar(SELECT + "WHERE j.id = ? ORDER BY jp.posicion", String.valueOf(id));
        return r.isEmpty() ? null : r.get(0);
    }

    @Override
    public Jugador buscarPorNombre(String nombre) throws SQLException {
        List<Jugador> r = consultar(SELECT + "WHERE j.nombre = ? ORDER BY j.id, jp.posicion", nombre);
        return r.isEmpty() ? null : r.get(0);
    }

    private List<Jugador> consultar(String sql, String parametro) throws SQLException {
        Map<Integer, Fila> filas = new LinkedHashMap<>();
        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (parametro != null) ps.setString(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    Fila f = filas.get(id);
                    if (f == null) {
                        f = new Fila();
                        f.id = id;
                        f.nombre = rs.getString("nombre");
                        f.clubId = rs.getInt("club_id");
                        f.adelantado = rs.getBoolean("juega_adelantado");
                        f.zaguero = rs.getBoolean("es_zaguero_central");
                        f.es5 = rs.getBoolean("es_5");
                        f.es9 = rs.getBoolean("es_9");
                        filas.put(id, f);
                    }
                    String pos = rs.getString("posicion");
                    if (pos != null && !pos.isEmpty()) f.posiciones.add(pos.toUpperCase().trim());
                }
            }
        }
        List<Jugador> resultado = new ArrayList<>();
        for (Fila f : filas.values()) resultado.add(construir(f));
        return resultado;
    }

    private Jugador construir(Fila f) {
        String apellido = f.nombre.contains(" ") ? f.nombre.substring(f.nombre.lastIndexOf(' ') + 1) : f.nombre;
        String base = f.posiciones.contains("GK") ? "GK" : (f.posiciones.isEmpty() ? "" : f.posiciones.get(0));

        Jugador j;
        switch (base) {
            case "LB", "CB", "RB" -> j = new Defensor(f.id, f.nombre, apellido, f.clubId, f.zaguero);
 case "MCD", "MC", "MCO", "LM", "RM" -> j = new Mediocampista(f.id, f.nombre, apellido, f.clubId, f.es5);
 case "LW", "ST", "RW" -> j = new Delantero(f.id, f.nombre, apellido, f.clubId, f.es9);
            case "GK" -> j = new Arquero(f.id, f.nombre, apellido, f.clubId, f.adelantado);
            default -> {
                // sin posiciones cargadas: se adivina por las marcas de la tabla
                if (f.es9) j = new Delantero(f.id, f.nombre, apellido, f.clubId, true);
                else if (f.es5) j = new Mediocampista(f.id, f.nombre, apellido, f.clubId, true);
                else if (f.zaguero) j = new Defensor(f.id, f.nombre, apellido, f.clubId, true);
                else j = new Arquero(f.id, f.nombre, apellido, f.clubId, f.adelantado);
            }
        }
        for (String p : f.posiciones) j.agregarPosicion(p);
        return j;
    }

    @Override
    public void insertar(Jugador Jugador) throws SQLException {
        String sqlJugador = "INSERT INTO jugador (nombre, club_id, juega_adelantado, es_zaguero_central, es_5, es_9) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlPosicion = "INSERT INTO jugador_posicion (jugador_id, posicion) VALUES (?, ?)";

        boolean juegaAdelantado = false, esZagueroCentral = false, es5 = false, es9 = false;
        String posicionPorDefecto;
        if (Jugador instanceof Arquero) {
            juegaAdelantado = ((Arquero) Jugador).getJuegaAdelantado();
            posicionPorDefecto = "GK";
        } else if (Jugador instanceof Defensor) {
            esZagueroCentral = ((Defensor) Jugador).getEsZagueroCentral();
            posicionPorDefecto = "CB";
        } else if (Jugador instanceof Mediocampista) {
            es5 = ((Mediocampista) Jugador).getEs5();
            posicionPorDefecto = "MC";
        } else {
            es9 = ((Delantero) Jugador).getEs9();
            posicionPorDefecto = "ST";
        }
        List<String> posiciones = new ArrayList<>(Jugador.getPosicionesPermitidas());
        if (posiciones.isEmpty()) posiciones.add(posicionPorDefecto);

        try (Connection conn = ConexionBD.getConexion()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sqlJugador, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, Jugador.getNombre());
                ps.setInt(2, Jugador.getClubId());
                ps.setBoolean(3, juegaAdelantado);
                ps.setBoolean(4, esZagueroCentral);
                ps.setBoolean(5, es5);
                ps.setBoolean(6, es9);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) Jugador.setId(rs.getInt(1));
                }
                try (PreparedStatement pp = conn.prepareStatement(sqlPosicion)) {
                    for (String pos : posiciones) {
                        pp.setInt(1, Jugador.getId());
                        pp.setString(2, pos);
                        pp.addBatch();
                    }
                    pp.executeBatch();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }
}
