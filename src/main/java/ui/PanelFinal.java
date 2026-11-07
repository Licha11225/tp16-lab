package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import arcade.Partido;
import arcade.Torneo;

public class PanelFinal extends JPanel {

    private final JLabel lblTitulo = new JLabel("", JLabel.CENTER);
    private final JLabel lblMarcador = new JLabel("", JLabel.CENTER);
    private final JLabel lblCuadro = new JLabel("", JLabel.CENTER);
    private final JTextArea txtCronica = new JTextArea();
    private final JButton btnSiguiente = new JButton();

    public PanelFinal(VentanaPrincipal v) {
        setLayout(new BorderLayout(0, 14));
        setBorder(BorderFactory.createEmptyBorder(30, 60, 30, 60));

        JPanel norte = new JPanel();
        norte.setLayout(new BoxLayout(norte, BoxLayout.Y_AXIS));
        lblTitulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 44));
        lblTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblMarcador.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        lblMarcador.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblCuadro.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblCuadro.setForeground(new Color(0xb0bec5));
        norte.add(lblTitulo);
        norte.add(Box.createVerticalStrut(8));
        norte.add(lblMarcador);
        norte.add(Box.createVerticalStrut(10));
        norte.add(lblCuadro);
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
        JButton btnRevancha = new JButton("Nueva eliminatoria (mismo 11)");
        JButton btnNuevo = new JButton("Armar un 11 nuevo");
        JButton btnMenu = new JButton("Menú");
        btnSiguiente.setBackground(new Color(0x27ae60));
        btnSiguiente.setForeground(Color.WHITE);
        btnSiguiente.setPreferredSize(new Dimension(280, 46));
        btnSiguiente.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        botones.add(btnSiguiente);
        for (JButton b : new JButton[]{btnRevancha, btnNuevo, btnMenu}) {
            b.setPreferredSize(new Dimension(b == btnRevancha ? 230 : 170, 46));
            b.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            botones.add(b);
        }
        btnSiguiente.addActionListener(e -> v.siguienteFase());
        btnRevancha.addActionListener(e -> v.revancha());
        btnNuevo.addActionListener(e -> v.iniciarArmado(v.getNombreDT()));
        btnMenu.addActionListener(e -> v.irAMenu());
        add(botones, BorderLayout.SOUTH);
    }

    public void mostrar(String nombreDT, Partido p, Torneo torneo) {
        if (torneo.isCampeon()) {
            lblTitulo.setText("¡CAMPEÓN!");
            lblTitulo.setForeground(new Color(0xffd54f));
        } else if (torneo.isEliminado()) {
            lblTitulo.setText("ELIMINADO");
            lblTitulo.setForeground(new Color(0xff6b6b));
        } else {
            lblTitulo.setText("¡PASASTE DE FASE!");
            lblTitulo.setForeground(new Color(0x6fcf97));
        }
        lblMarcador.setText(nombreDT + "  " + p.getGolesFavor() + " - " + p.getGolesContra() + "  " + p.getRival()
                + (p.huboTanda() ? "   (pen. " + p.getPenFavor() + " - " + p.getPenContra() + ")" : ""));

        StringBuilder cuadro = new StringBuilder("<html><div style='text-align:center'>");
        for (String linea : torneo.cuadro()) cuadro.append(linea).append("<br>");
        lblCuadro.setText(cuadro.append("</div></html>").toString());

        if (torneo.hayProxima()) {
            btnSiguiente.setText("Jugar " + torneo.nombreFase() + " vs " + torneo.rivalActual());
            btnSiguiente.setVisible(true);
        } else {
            btnSiguiente.setVisible(false);
        }

        StringBuilder sb = new StringBuilder();
        for (String linea : p.getCronica()) {
            sb.append(linea).append("\n\n");
        }
        txtCronica.setText(sb.toString().trim());
        txtCronica.setCaretPosition(0);
    }
}
