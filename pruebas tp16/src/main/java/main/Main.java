package main;

import java.awt.Font;
import java.sql.Connection;
import java.sql.DriverManager;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;

import config.ConexionBD;
import dao.ClubDAO;
import dao.JugadorDAO;
import dao.OnceidealDAO;
import daoimpl.Clubdaoimpl;
import daoimpl.Clubdaomemoria;
import daoimpl.Jugadordaoimpl;
import daoimpl.Jugadordaomemoria;
import daoimpl.Onceidealdaoimpl;
import daoimpl.Onceidealdaomemoria;
import service.Juegoservice;
import ui.VentanaPrincipal;

public class Main {

    public static void main(String[] args) {

        FlatDarkLaf.setup();                       // tema oscuro (FlatLightLaf para claro)
        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        UIManager.put("Button.arc", 18);
        UIManager.put("Component.arc", 14);
        UIManager.put("TextComponent.arc", 14);

        boolean hayBD = hayBaseDeDatos();
        ClubDAO clubes;
        JugadorDAO jugadores;
        OnceidealDAO onces;
        String estado;
        if (hayBD) {
            clubes = new Clubdaoimpl();
            jugadores = new Jugadordaoimpl();
            onces = new Onceidealdaoimpl();
            estado = "Datos: base de datos MySQL";
        } else {
            clubes = new Clubdaomemoria();
            jugadores = new Jugadordaomemoria();
            onces = new Onceidealdaomemoria();
            estado = "Datos: modo demo (sin MySQL, jugadores de ejemplo)";
        }
        Juegoservice service = new Juegoservice(jugadores, clubes, onces);

        SwingUtilities.invokeLater(() -> new VentanaPrincipal(service, estado).setVisible(true));
    }

    // Si MySQL responde y tiene clubes cargados se usa si no, se juega con datos en memoria.
    private static boolean hayBaseDeDatos() {
        try {
            DriverManager.setLoginTimeout(3);
            try (Connection c = ConexionBD.getConexion()) {
                if (c == null) return false;
            }
            return new Clubdaoimpl().obtenerAleatorio() != null;
        } catch (Exception | LinkageError e) {
            return false;
        }
    }
}
