package ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import arcade.Partido;
import arcade.Ratings;
import arcade.Resultado;
import arcade.Situacion;
import arcade.Torneo;
import model.onceideal;

public class PanelPartido extends JPanel {

    private static final Color AZUL = new Color(0x56ccf2);
    private static final Color DORADO = new Color(0xffd54f);

    private final VentanaPrincipal v;
    private final CanchaPanel cancha = new CanchaPanel();

    private final JLabel lblLocal = new JLabel("", JLabel.RIGHT);
    private final JLabel lblMarcador = new JLabel("0 - 0", JLabel.CENTER);
    private final JLabel lblRival = new JLabel("", JLabel.LEFT);
    private final JLabel lblInfo = new JLabel(" ", JLabel.CENTER);
    private final JLabel lblStats = new JLabel(" ", JLabel.CENTER);

    private final CardLayout cardsSur = new CardLayout();
    private final JPanel sur = new JPanel(cardsSur);
    private final JLabel lblTitulo = new JLabel();
    private final JLabel lblDesc = new JLabel();
    private final JButton[] btnOpc = new JButton[3];
    private final JLabel lblNarracion = new JLabel();
    private final JButton btnContinuar = new JButton("Continuar");

    private Partido partido;
    private Torneo torneo;
    private int idx;
    private boolean hayMas;
    private int golesPrevios;

    public PanelPartido(VentanaPrincipal v) {
        this.v = v;
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 12, 12, 12));

        // marcador
        JPanel norte = new JPanel(new BorderLayout());
        JPanel fila = new JPanel(new GridLayout(1, 3, 12, 0));
        Font fEquipo = new Font(Font.SANS_SERIF, Font.BOLD, 20);
        lblLocal.setFont(fEquipo);
        lblRival.setFont(fEquipo);
        lblRival.setForeground(new Color(0xeb5757));
        lblLocal.setForeground(AZUL);
        lblMarcador.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 36));
        fila.add(lblLocal);
        fila.add(lblMarcador);
        fila.add(lblRival);
        lblInfo.setForeground(DORADO);
        lblStats.setForeground(Color.GRAY);
        lblStats.setFont(lblStats.getFont().deriveFont(11f));
        JPanel filas = new JPanel(new GridLayout(3, 1));
        filas.add(fila);
        filas.add(lblInfo);
        filas.add(lblStats);
        norte.add(filas, BorderLayout.CENTER);
        add(norte, BorderLayout.NORTH);

        add(cancha, BorderLayout.CENTER);

        // panel de decisión
        JPanel panelOp = new JPanel();
        panelOp.setLayout(new BoxLayout(panelOp, BoxLayout.Y_AXIS));
        lblTitulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        lblTitulo.setForeground(DORADO);
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel botones = new JPanel(new GridLayout(1, 3, 10, 0));
        botones.setAlignmentX(Component.LEFT_ALIGNMENT);
        botones.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        for (int i = 0; i < 3; i++) {
            final int op = i;
            btnOpc[i] = new JButton();
            btnOpc[i].setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            btnOpc[i].setPreferredSize(new Dimension(200, 54));
            btnOpc[i].addActionListener(e -> elegir(op));
            botones.add(btnOpc[i]);
        }
        panelOp.add(lblTitulo);
        panelOp.add(Box.createVerticalStrut(4));
        panelOp.add(lblDesc);
        panelOp.add(Box.createVerticalStrut(8));
        panelOp.add(botones);

        JPanel panelRes = new JPanel(new BorderLayout(0, 8));
        lblNarracion.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        btnContinuar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        btnContinuar.setPreferredSize(new Dimension(260, 48));
        btnContinuar.addActionListener(e -> continuar());
        JPanel pie = new JPanel();
        pie.add(btnContinuar);
        panelRes.add(lblNarracion, BorderLayout.CENTER);
        panelRes.add(pie, BorderLayout.SOUTH);

        sur.add(panelOp, "op");
        sur.add(panelRes, "res");
        sur.setPreferredSize(new Dimension(800, 150));
        add(sur, BorderLayout.SOUTH);
    }

    public void iniciar(String nombreDT, onceideal once, Torneo torneo) {
        this.torneo = torneo;
        partido = torneo.nuevoPartido(nombreDT, once.getListaJugadores());
        idx = 0;
        golesPrevios = 0;
        cancha.limpiar();
        cancha.limpiarNombres();
        for (int i = 0; i < 11; i++) {
            cancha.setNombre(i, Ratings.corto(once.getListaJugadores().get(i)));
        }
        cancha.setResaltado(-1);
        cancha.setMostrarRival(true);
        lblLocal.setText(nombreDT);
        lblRival.setText(partido.getRival());
        lblMarcador.setText("0 - 0");
        lblInfo.setText(torneo.nombreFase());
        lblStats.setText(partido.resumenEquipos());

        // cartel de presentación de la fase, y recién después aparece la primera jugada
        btnContinuar.setVisible(false);
        lblNarracion.setText(html("<b>" + torneo.nombreFase() + "</b> — " + nombreDT + " vs " + partido.getRival()));
        cardsSur.show(sur, "res");
        cancha.cartel(torneo.nombreFase(), () -> {
            btnContinuar.setVisible(true);
            mostrarSituacion();
        });
    }

    private void mostrarSituacion() {
        Situacion s = partido.situacion(idx);
        if (idx >= partido.normales()) {
            lblInfo.setText(torneo.nombreFase() + "   ·   TANDA DE PENALES   ·   " + partido.getPenFavor() + " - " + partido.getPenContra());
        } else {
            lblInfo.setText(torneo.nombreFase() + "   ·   Jugada " + (idx + 1) + " / " + partido.normales()
                    + "   ·   Minuto " + s.minuto + "'");
        }
        lblTitulo.setText(s.titulo.toUpperCase());
        lblDesc.setText(html(s.descripcion));
        for (int i = 0; i < 3; i++) {
            btnOpc[i].setText(s.opciones[i]);
            btnOpc[i].setEnabled(true);
        }
        cancha.limpiar();
        cardsSur.show(sur, "op");
    }

    private void elegir(int op) {
        for (JButton b : btnOpc) b.setEnabled(false);
        Resultado r = partido.resolver(idx, op);
        cancha.reproducir(r.escena, () -> {
            lblMarcador.setText(partido.getGolesFavor() + " - " + partido.getGolesContra());
            if (partido.getGolesFavor() + partido.getGolesContra() > golesPrevios) destacarMarcador(r.golFavor > 0);
            golesPrevios = partido.getGolesFavor() + partido.getGolesContra();
            if (partido.enTanda()) {
                lblInfo.setText(torneo.nombreFase() + "   ·   TANDA DE PENALES   ·   "
                        + partido.getPenFavor() + " - " + partido.getPenContra());
            }
            lblNarracion.setText(html(r.narracion));
            hayMas = partido.hayMas(idx);   // si terminó empatado, acá se arma la tanda
            if (!hayMas) btnContinuar.setText("Ver resultado final");
            else if (idx + 1 == partido.normales()) btnContinuar.setText("¡Empate! Vamos a los penales");
            else btnContinuar.setText(idx + 1 > partido.normales() ? "Siguiente penal" : "Siguiente jugada");
            cardsSur.show(sur, "res");
        });
    }

    private void destacarMarcador(boolean favor) {
        lblMarcador.setForeground(favor ? DORADO : new Color(0xff5252));
        lblMarcador.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 46));
        Timer t = new Timer(900, e -> {
            lblMarcador.setForeground(null);
            lblMarcador.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 36));
        });
        t.setRepeats(false);
        t.start();
    }

    private void continuar() {
        if (!hayMas) {
            v.irAFinal(partido);
        } else {
            idx++;
            mostrarSituacion();
        }
    }

    private static String html(String texto) {
        return "<html><div style='width:660px'>" + texto + "</div></html>";
    }
}
