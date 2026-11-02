package daoimpl;

import model.arquero;
import model.club;
import model.defensor;
import model.delantero;
import model.jugador;
import model.mediocampista;

import java.util.ArrayList;
import java.util.List;

public final class DatosDemo {

    private static final String[] CLUBES = {
            "Halcones FC", "Real Tormenta", "Deportivo Aurora", "Atlético Cóndor", "Unión Marea", "Club Dragones"};
    private static final String[] NOMBRES = {
            "Tomás", "Lucas", "Mateo", "Bruno", "Franco", "Nicolás", "Diego", "Santiago", "Joaquín", "Emiliano", "Martín",
            "Gonzalo", "Facundo", "Julián", "Ramiro", "Agustín", "Lautaro", "Ignacio", "Maximiliano", "Sebastián",
            "Federico", "Ezequiel"};
    private static final String[] APELLIDOS = {
            "Rivas", "Medina", "Ortega", "Bravo", "Salinas", "Cabrera", "Funes", "Ledesma", "Quiroga", "Peralta", "Roldán",
            "Sosa", "Acuña", "Barrios", "Correa", "Duarte", "Escobar", "Fariña", "Giménez", "Herrera", "Ibarra", "Juárez",
            "Luna", "Maldonado", "Navarro", "Olivera", "Paz", "Rojas", "Silva", "Toledo", "Vargas", "Villalba", "Zárate"};

    private DatosDemo() {
    }

    public static List<club> clubes() {
        List<club> lista = new ArrayList<>();
        for (int c = 0; c < CLUBES.length; c++) {
            lista.add(new club(c + 1, CLUBES[c], ""));
        }
        return lista;
    }

    public static List<jugador> jugadores() {
        List<jugador> lista = new ArrayList<>();
        for (int c = 0; c < CLUBES.length; c++) {
            for (int k = 0; k < 11; k++) {
                int n = c * 11 + k;
                int id = n + 1;
                
                String nombre = NOMBRES[n % NOMBRES.length];
                String apellido = APELLIDOS[n % APELLIDOS.length];
                
                int clubId = c + 1;
                
                if (k == 0) {
                    lista.add(new arquero(id, nombre, apellido, clubId, n % 3 == 0));
                } else if (k <= 4) {
                    lista.add(new defensor(id, nombre, apellido, clubId, k == 2 || k == 3));
                } else if (k <= 7) {
                    lista.add(new mediocampista(id, nombre, apellido, clubId, k == 5));
                } else {
                    lista.add(new delantero(id, nombre, apellido, clubId, k == 9));
                }
            }
        }
        return lista;
    }
}