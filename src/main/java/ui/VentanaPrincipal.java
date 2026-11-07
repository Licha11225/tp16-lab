package ui;

import java.awt.CardLayout;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import arcade.Partido;
import arcade.Torneo;
import model.onceideal;
import service.juegoservice;

public class VentanaPrincipal extends JFrame {

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final juegoservice service;

    private final PanelInicio inicio;
    private final PanelArmar11 armar;
    private final PanelPartido partido;
    private final PanelFinal fin;

    private String nombreDT = "DT";
    private onceideal onceActual;
    private Torneo torneo;

    public VentanaPrincipal(juegoservice service, String estadoDatos) {
        super("Fútbol 11 Arcade");
        this.service = service;
        inicio = new PanelInicio(this, estadoDatos);
        armar = new PanelArmar11(this, service);
        partido = new PanelPartido(this);
        fin = new PanelFinal(this);

        root.add(inicio, "inicio");
        root.add(armar, "armar");
        root.add(partido, "partido");
        root.add(fin, "final");
        setContentPane(root);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 700));
        setSize(1080, 760);
        setLocationRelativeTo(null);
    }

    public String getNombreDT() {
        return nombreDT;
    }

    public void irAMenu() {
        cards.show(root, "inicio");
    }

    public void iniciarArmado(String nombre) {
        nombreDT = nombre;
        armar.iniciar(nombre);
        cards.show(root, "armar");
    }

    /** Arranca una eliminatoria nueva (3 rivales de la BDD) con este 11. */
    public void irAPartido(onceideal once) {
        onceActual = once;
        try {
            torneo = service.crearTorneo();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo armar la eliminatoria:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        jugarFase();
    }

    private void jugarFase() {
        partido.iniciar(nombreDT, onceActual, torneo);
        cards.show(root, "partido");
    }

    public void siguienteFase() {
        jugarFase();
    }

    /** Nueva eliminatoria con el mismo 11. */
    public void revancha() {
        irAPartido(onceActual);
    }

    public void irAFinal(Partido p) {
        torneo.registrar(p);
        fin.mostrar(nombreDT, p, torneo);
        cards.show(root, "final");
    }
}
