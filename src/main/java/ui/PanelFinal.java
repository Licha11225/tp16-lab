package ui;

import arcade.Partido;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

public class PanelFinal extends JPanel {

    private final JLabel lblTitulo = new JLabel("", JLabel.CENTER);
    private final JLabel lblMarcador = new JLabel("", JLabel.CENTER);
    private final JTextArea txtCronica = new JTextArea();

    public PanelFinal(VentanaPrincipal v) {
        setLayout(new BorderLayout(0, 14));
        setBorder(BorderFactory.createEmptyBorder(30, 60, 30, 60));

        JPanel norte = new JPanel();
        norte.setLayout(new BoxLayout(norte, BoxLayout.Y_AXIS));
        lblTitulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 44));
        lblTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblMarcador.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        lblMarcador.setAlignmentX(Component.CENTER_ALIGNMENT);
        norte.add(lblTitulo);
        norte.add(Box.createVerticalStrut(8));
        norte.add(lblMarcador);
        add(norte, BorderLayout.NORTH);

        txtCronica.setEditable(false);
        txtCronica.setLineWrap(true);
        txtCronica.setWrapStyleWord(true);
        txtCronica.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        txtCronica.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JScrollPane scroll = new JScrollPane(txtCronica);
        scroll.setBorder(BorderFactory.createTitledBorder("Resumen del partido"));
        add(scroll, BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        JButton btnRevancha = new JButton("Revancha (mismo 11)");
        JButton btnNuevo = new JButton("Armar un 11 nuevo");
        JButton btnMenu = new JButton("Menú");
        for (JButton b : new JButton[]{btnRevancha, btnNuevo, btnMenu}) {
            b.setPreferredSize(new Dimension(190, 46));
            b.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            botones.add(b);
        }
        btnRevancha.addActionListener(e -> v.revancha());
        btnNuevo.addActionListener(e -> v.iniciarArmado(v.getNombreDT()));
        btnMenu.addActionListener(e -> v.irAMenu());
        add(botones, BorderLayout.SOUTH);
    }

    public void mostrar(String nombreDT, Partido p) {
        int gf = p.getGolesFavor();
        int gc = p.getGolesContra();
        if (gf > gc) {
            lblTitulo.setText("¡GANASTE!");
            lblTitulo.setForeground(new Color(0xffd54f));
        } else if (gf == gc) {
            lblTitulo.setText("EMPATE");
            lblTitulo.setForeground(new Color(0xe0e0e0));
        } else {
            lblTitulo.setText("DERROTA");
            lblTitulo.setForeground(new Color(0xff6b6b));
        }
        lblMarcador.setText(nombreDT + "  " + gf + " - " + gc + "  " + p.getRival());
        StringBuilder sb = new StringBuilder();
        for (String linea : p.getCronica()) {
            sb.append(linea).append("\n\n");
        }
        txtCronica.setText(sb.toString().trim());
        txtCronica.setCaretPosition(0);
    }
}
