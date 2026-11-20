package ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

public class PanelInicio extends JPanel {

    private final JTextField txtNombre = new JTextField();
    private final JLabel lblError = new JLabel(" ");

    public PanelInicio(VentanaPrincipal v, String estadoDatos) {
        setLayout(new java.awt.GridBagLayout());
        JPanel caja = new JPanel();
        caja.setLayout(new BoxLayout(caja, BoxLayout.Y_AXIS));
        caja.setOpaque(false);

        JLabel titulo = new JLabel("FÚTBOL 11");
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 64));
        titulo.setForeground(new Color(0xffd54f));
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("ARCADE");
        sub.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        sub.setForeground(new Color(0x56ccf2));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel desc = new JLabel("Armá tu 11 y jugá un partido tomando las decisiones clave.");
        desc.setAlignmentX(Component.CENTER_ALIGNMENT);

        txtNombre.putClientProperty("JTextField.placeholderText", "Tu nombre de DT");
        txtNombre.setMaximumSize(new Dimension(320, 40));
        txtNombre.setPreferredSize(new Dimension(320, 40));
        txtNombre.setHorizontalAlignment(JTextField.CENTER);
        txtNombre.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblError.setForeground(new Color(0xff6b6b));
        lblError.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btnJugar = new JButton("JUGAR");
        btnJugar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        btnJugar.setMaximumSize(new Dimension(320, 52));
        btnJugar.setPreferredSize(new Dimension(320, 52));
        btnJugar.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnJugar.putClientProperty("JButton.buttonType", "roundRect");
        btnJugar.setBackground(new Color(0x27ae60));
        btnJugar.setForeground(Color.WHITE);

        JButton btnSalir = new JButton("Salir");
        btnSalir.setMaximumSize(new Dimension(320, 40));
        btnSalir.setPreferredSize(new Dimension(320, 40));
        btnSalir.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblEstado = new JLabel(estadoDatos);
        lblEstado.setForeground(Color.GRAY);
        lblEstado.setAlignmentX(Component.CENTER_ALIGNMENT);

        caja.add(titulo);
        caja.add(sub);
        caja.add(Box.createVerticalStrut(14));
        caja.add(desc);
        caja.add(Box.createVerticalStrut(30));
        caja.add(txtNombre);
        caja.add(Box.createVerticalStrut(4));
        caja.add(lblError);
        caja.add(Box.createVerticalStrut(10));
        caja.add(btnJugar);
        caja.add(Box.createVerticalStrut(10));
        caja.add(btnSalir);
        caja.add(Box.createVerticalStrut(30));
        caja.add(lblEstado);
        add(caja);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        Runnable jugar = () -> {
            String n = txtNombre.getText().trim();
            if (n.isEmpty()) {
                lblError.setText("Ingresá tu nombre de DT para empezar.");
                return;
            }
            lblError.setText(" ");
            v.iniciarArmado(n);
        };
        btnJugar.addActionListener(e -> jugar.run());
        txtNombre.addActionListener(e -> jugar.run());
        btnSalir.addActionListener(e -> System.exit(0));
    }
}
