package service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import arcade.Formacion;
import arcade.Ratings;
import arcade.Torneo;
import dao.ClubDAO;
import dao.JugadorDAO;
import dao.OnceidealDAO;
import exception.ClubIncorrectoException;
import exception.EntradaVaciaException;
import exception.JugadorDuplicadoException;
import exception.JugadorNoEncontradoException;
import exception.PosicionInvalidaException;
import model.Alineable;
import model.Club;
import model.Jugador;
import model.Onceideal;

public class Juegoservice {
    
    private final JugadorDAO JugadorDAO;
    private final ClubDAO ClubDAO;
    private final OnceidealDAO onceIdealDAO;

    // Constructor para inyectar los DAOs
    public Juegoservice(JugadorDAO JugadorDAO, ClubDAO ClubDAO, OnceidealDAO onceIdealDAO) {
        this.JugadorDAO = JugadorDAO;
        this.ClubDAO = ClubDAO;
        this.onceIdealDAO = onceIdealDAO;
    }

    public Club sortearClubAleatorio() throws Exception {
        return ClubDAO.obtenerAleatorio(); 
    }

    public List<Jugador> listarJugadoresDeClub(int clubId) throws Exception {
        List<Jugador> resultado = new ArrayList<>();
        for (Jugador j : JugadorDAO.listarTodos()) {
            if (j.getClubId() != null && j.getClubId().equals(clubId)) {
                resultado.add(j);
            }
        }
        resultado.sort(Comparator.comparingInt(Ratings::orden).thenComparing(Jugador::getNombre));
        return resultado;
    }

    public Torneo crearTorneo() throws Exception {
        List<Club> rivales = new ArrayList<>();
        List<List<Jugador>> planteles = new ArrayList<>();
        int intentos = 0;
        while (rivales.size() < Torneo.FASES.length && intentos++ < 100) {
            Club c = ClubDAO.obtenerAleatorio();
            if (c == null) break;
            boolean repetido = false;
            for (Club r : rivales) {
                if (r.getId() == c.getId()) repetido = true;
            }
            if (repetido) continue;
            rivales.add(c);
            planteles.add(listarJugadoresDeClub(c.getId()));
        }
        if (rivales.isEmpty()) {
            throw new Exception("No hay clubes cargados para armar la eliminatoria.");
        }
        for (int i = 0; rivales.size() < Torneo.FASES.length; i++) {
            rivales.add(rivales.get(i));
            planteles.add(planteles.get(i));
        }
        return new Torneo(rivales, planteles);
    }

    public Jugador buscarJugadorPorNombre(String nombre) throws EntradaVaciaException, JugadorNoEncontradoException, Exception {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new EntradaVaciaException("Debe ingresar el nombre de un jugador. El campo no puede estar vacío.");
        }

        Jugador Jugador = JugadorDAO.buscarPorNombre(nombre);

        if (Jugador == null) {
            throw new JugadorNoEncontradoException("El jugador '" + nombre + "' no existe en los registros.");
        }

        return Jugador;
    }

    public void agregarJugadorAOnce(Onceideal once, Alineable jugador, Club clubTurno, String casillero) 
            throws ClubIncorrectoException, JugadorDuplicadoException, PosicionInvalidaException {
        
        Jugador ficha = (Jugador) jugador;

        if (!ficha.getClubId().equals(clubTurno.getId())) {
            throw new ClubIncorrectoException("El jugador " + ficha.getNombre() + " no pertenece a " + clubTurno.getNombre());
        }

        for (Jugador j : once.getListaJugadores()) {
            if (j == null || !j.getId().equals(ficha.getId())) {
            } else {
                throw new JugadorDuplicadoException(ficha.getNombre() + " ya fue agregado a tu 11. No podés repetirlo.");
            }
        }

        if (!Formacion.sirve(ficha, casillero)) {
            throw new PosicionInvalidaException(ficha.getNombre() + " no puede ser alineado en la posición " + casillero);
        }

        once.agregarJugador(ficha);
    }

    public boolean validarAlineacionCompleta(Onceideal once) {
        int cargados = 0;
        for (Jugador j : once.getListaJugadores()) {
            if (j != null) cargados++;
        }
        return cargados == 11;
    }

    public void guardarOnceDefinitivo(Onceideal once) throws Exception {
        if (!validarAlineacionCompleta(once)) {
            throw new Exception("Fallo al guardar: El equipo no está completo. Faltan jugadores.");
        }
        
        onceIdealDAO.insertarOnce(once);
    }
}