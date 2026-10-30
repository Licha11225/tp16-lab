package ui;

import arcade.Partido;
import model.onceideal;
import service.juegoservice;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.Dimension;

public class VentanaPrincipal extends JFrame {

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    private final PanelInicio inicio;
    private final PanelArmar11 armar;
    private final PanelPartido partido;
    private final PanelFinal fin;

    private String nombreDT = "DT";
    private onceideal onceActual;

    public VentanaPrincipal(juegoservice service, String estadoDatos) {
        super("Fútbol 11 Arcade");
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

    public void irAPartido(onceideal once) {
        onceActual = once;
        partido.iniciar(nombreDT, once);
        cards.show(root, "partido");
    }

    public void revancha() {
        irAPartido(onceActual);
    }

    public void irAFinal(Partido p) {
        fin.mostrar(nombreDT, p);
        cards.show(root, "final");
    }
}
