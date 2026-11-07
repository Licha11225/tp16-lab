package main;

import java.awt.Font;
import java.sql.Connection;
import java.sql.DriverManager;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;

import config.ConexionBD;
import dao.clubDAO;
import dao.jugadorDAO;
import dao.onceidealDAO;
import daoimpl.clubdaoimpl;
import daoimpl.clubdaomemoria;
import daoimpl.jugadordaoimpl;
import daoimpl.jugadordaomemoria;
import daoimpl.onceidealdaoimpl;
import daoimpl.onceidealdaomemoria;
import service.juegoservice;
import ui.VentanaPrincipal;

public class Main {

    public static void main(String[] args) {

        FlatDarkLaf.setup();                       // tema oscuro (FlatLightLaf para claro)
        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        UIManager.put("Button.arc", 18);
        UIManager.put("Component.arc", 14);
        UIManager.put("TextComponent.arc", 14);

        boolean hayBD = hayBaseDeDatos();
        clubDAO clubes;
        jugadorDAO jugadores;
        onceidealDAO onces;
        String estado;
        if (hayBD) {
            clubes = new clubdaoimpl();
            jugadores = new jugadordaoimpl();
            onces = new onceidealdaoimpl();
            estado = "Datos: base de datos MySQL";
        } else {
            clubes = new clubdaomemoria();
            jugadores = new jugadordaomemoria();
            onces = new onceidealdaomemoria();
            estado = "Datos: modo demo (sin MySQL, jugadores de ejemplo)";
        }
        juegoservice service = new juegoservice(jugadores, clubes, onces);

        SwingUtilities.invokeLater(() -> new VentanaPrincipal(service, estado).setVisible(true));
    }

    // Si MySQL responde y tiene clubes cargados se usa si no, se juega con datos en memoria.
    private static boolean hayBaseDeDatos() {
        try {
            DriverManager.setLoginTimeout(3);
            try (Connection c = ConexionBD.getConexion()) {
                if (c == null) return false;
            }
            return new clubdaoimpl().obtenerAleatorio() != null;
        } catch (Exception | LinkageError e) {
            return false;
        }
    }
}
