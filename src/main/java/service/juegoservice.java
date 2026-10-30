package service;

import dao.clubDAO;
import dao.jugadorDAO;
import dao.onceidealDAO;
import exception.*;
import model.alineable;
import model.club;
import model.jugador;
import model.onceideal;
import arcade.Ratings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class juegoservice {
    
    // Dependencias de los DAOs para interactuar con la base de datos
    private jugadorDAO jugadorDAO;
    private clubDAO clubDAO;
    private onceidealDAO onceIdealDAO;

    // Constructor para inyectar los DAOs
    public juegoservice(jugadorDAO jugadorDAO, clubDAO clubDAO, onceidealDAO onceIdealDAO) {
        this.jugadorDAO = jugadorDAO;
        this.clubDAO = clubDAO;
        this.onceIdealDAO = onceIdealDAO;
    }

    /**
     * Obtiene un club aleatorio desde la BDD para el turno actual.
     */
    public club sortearClubAleatorio() throws Exception {
        return clubDAO.obtenerAleatorio(); 
    }

    /**
     * Devuelve los jugadores de un club, ordenados por posición y nombre (para la lista de la GUI).
     */
    public List<jugador> listarJugadoresDeClub(int clubId) throws Exception {
        List<jugador> resultado = new ArrayList<>();
        for (jugador j : jugadorDAO.listarTodos()) {
            if (j.getClubId().intValue() == clubId) {
                resultado.add(j);
            }
        }
        resultado.sort(Comparator.comparingInt(Ratings::orden).thenComparing(jugador::getNombre));
        return resultado;
    }

    /**
     * Busca un jugador por consola, validando que el input no esté vacío y que exista en MySQL.
     */
    public jugador buscarJugadorPorNombre(String nombre) throws EntradaVaciaException, JugadorNoEncontradoException, Exception {
        // Regla de Negocio: Entrada Vacía
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new EntradaVaciaException("Debe ingresar el nombre de un jugador. El campo no puede estar vacío.");
        }

        // Se asume que jugadorDAO tiene un método buscarPorNombre()
        jugador jugador = jugadorDAO.buscarPorNombre(nombre);

        // Regla de Negocio: Existencia en BDD
        if (jugador == null) {
            throw new JugadorNoEncontradoException("El jugador '" + nombre + "' no existe en los registros.");
        }

        return jugador;
    }

    /**
     * Evalúa todas las reglas de negocio antes de confirmar el fichaje de una carta en el 11.
     */
    public void agregarJugadorAOnce(onceideal once, alineable jugador, club clubTurno, String casillero) 
            throws ClubIncorrectoException, JugadorDuplicadoException, PosicionInvalidaException {
        
        // Casteamos a jugador (minúscula) para poder leer sus atributos comunes (id, nombre, clubId)
        jugador ficha = (jugador) jugador;

        // 1. Regla de Negocio: Validación de Club
        if (!ficha.getClubId().equals(clubTurno.getId())) {
            throw new ClubIncorrectoException("El jugador " + ficha.getNombre() + " no pertenece a " + clubTurno.getNombre());
        }

        // 2. Regla de Negocio: Duplicidad
        for (jugador j : once.getListaJugadores()) {
            if (j.getId().equals(ficha.getId())) {
                throw new JugadorDuplicadoException(ficha.getNombre() + " ya fue agregado a tu 11. No podés repetirlo.");
            }
        }

        // 3. Regla de Negocio: Polimorfismo y Cupo de Posición (Interfaz alineable)
        if (!jugador.esUbicacionValida(casillero)) {
            throw new PosicionInvalidaException(ficha.getNombre() + " no puede ser alineado en la posición " + casillero);
        }

        // Si superó todas las validaciones, lo sumamos al equipo
        once.agregarJugador(ficha);
    }

    /**
     * Verifica que el equipo tenga exactamente 11 jugadores.
     */
    public boolean validarAlineacionCompleta(onceideal once) {
        return once.getListaJugadores().size() == 11;
    }

    /**
     * Guarda el equipo definitivo en la base de datos, siempre y cuando esté completo.
     */
    public void guardarOnceDefinitivo(onceideal once) throws Exception {
        if (!validarAlineacionCompleta(once)) {
            throw new Exception("Fallo al guardar: El equipo no está completo. Faltan jugadores.");
        }
        
        // Si está completo, delegamos la persistencia al DAO
        onceIdealDAO.insertarOnce(once);
    }
}