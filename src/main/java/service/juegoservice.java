package service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import arcade.Ratings;
import dao.clubDAO;
import dao.jugadorDAO;
import dao.onceidealDAO;
import exception.ClubIncorrectoException;
import exception.EntradaVaciaException;
import exception.JugadorDuplicadoException;
import exception.JugadorNoEncontradoException;
import exception.PosicionInvalidaException;
import model.alineable;
import model.club;
import model.jugador;
import model.onceideal;

public class juegoservice {
    
    private final jugadorDAO jugadorDAO;
    private final clubDAO clubDAO;
    private final onceidealDAO onceIdealDAO;

    // Constructor para inyectar los DAOs
    public juegoservice(jugadorDAO jugadorDAO, clubDAO clubDAO, onceidealDAO onceIdealDAO) {
        this.jugadorDAO = jugadorDAO;
        this.clubDAO = clubDAO;
        this.onceIdealDAO = onceIdealDAO;
    }

    public club sortearClubAleatorio() throws Exception {
        return clubDAO.obtenerAleatorio(); 
    }

    public List<jugador> listarJugadoresDeClub(int clubId) throws Exception {
        List<jugador> resultado = new ArrayList<>();
        for (jugador j : jugadorDAO.listarTodos()) {
            // Evitamos unboxing innecesario comparando de forma segura
            if (j.getClubId() != null && j.getClubId().equals(clubId)) {
                resultado.add(j);
            }
        }
        resultado.sort(Comparator.comparingInt(Ratings::orden).thenComparing(jugador::getNombre));
        return resultado;
    }

    public jugador buscarJugadorPorNombre(String nombre) throws EntradaVaciaException, JugadorNoEncontradoException, Exception {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new EntradaVaciaException("Debe ingresar el nombre de un jugador. El campo no puede estar vacío.");
        }

        jugador jugador = jugadorDAO.buscarPorNombre(nombre);

        if (jugador == null) {
            throw new JugadorNoEncontradoException("El jugador '" + nombre + "' no existe en los registros.");
        }

        return jugador;
    }

    public void agregarJugadorAOnce(onceideal once, alineable jugador, club clubTurno, String casillero) 
            throws ClubIncorrectoException, JugadorDuplicadoException, PosicionInvalidaException {
        
        jugador ficha = (jugador) jugador;

        if (!ficha.getClubId().equals(clubTurno.getId())) {
            throw new ClubIncorrectoException("El jugador " + ficha.getNombre() + " no pertenece a " + clubTurno.getNombre());
        }

        for (jugador j : once.getListaJugadores()) {
            if (j.getId().equals(ficha.getId())) {
                throw new JugadorDuplicadoException(ficha.getNombre() + " ya fue agregado a tu 11. No podés repetirlo.");
            }
        }

        if (!jugador.esUbicacionValida(casillero)) {
            throw new PosicionInvalidaException(ficha.getNombre() + " no puede ser alineado en la posición " + casillero);
        }

        once.agregarJugador(ficha);
    }

    public boolean validarAlineacionCompleta(onceideal once) {
        return once.getListaJugadores().size() == 11;
    }

    public void guardarOnceDefinitivo(onceideal once) throws Exception {
        if (!validarAlineacionCompleta(once)) {
            throw new Exception("Fallo al guardar: El equipo no está completo. Faltan jugadores.");
        }
        
        onceIdealDAO.insertarOnce(once);
    }
}