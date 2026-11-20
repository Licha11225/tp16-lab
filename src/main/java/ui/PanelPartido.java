package ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.Timer;

import arcade.Partido;
import arcade.Ratings;
import arcade.Resultado;
import arcade.Situacion;
import arcade.Torneo;
import arcade.TipoJugada;
import model.Onceideal;

public class PanelPartido extends JPanel {

    private static final Color AZUL = new Color(0x56ccf2);
    private static final Color DORADO = new Color(0xffd54f);

    private final VentanaPrincipal v;
    private final CanchaPanel cancha = new CanchaPanel();
    private final PanelRemate panelRemate = new PanelRemate();
    private final CardLayout centroCards = new CardLayout();
    private final JPanel centro = new JPanel(centroCards);
    private Situacion situacionActual;

    private final JLabel lblLocal = new JLabel("", JLabel.RIGHT);
    private final JLabel lblMarcador = new JLabel("0 - 0", JLabel.CENTER);
    private final JLabel lblRival = new JLabel("", JLabel.LEFT);
    private final JLabel lblInfo = new JLabel(" ", JLabel.CENTER);
    private final JLabel lblStats = new JLabel(" ", JLabel.CENTER);
    private final JLabel lblReloj = new JLabel("00:00  ·  PRIMER TIEMPO", JLabel.CENTER);
    private final JProgressBar barraTiempo = new JProgressBar(0, 90);
    private int minutoPartido;
    // 1 minuto simulado cada 450 ms: el encuentro recorre los 90 minutos sin pedir clics.
    private final Timer relojPartido = new Timer(450, e -> avanzarReloj());

    private final CardLayout cardsSur = new CardLayout();
    private final JPanel sur = new JPanel(cardsSur);
    private final JLabel lblTitulo = new JLabel();
    private final JLabel lblDesc = new JLabel();
    private final JButton[] btnOpc = new JButton[3];
    private final JLabel lblNarracion = new JLabel();
    private final JButton btnContinuar = new JButton("Continuar");
    private final JButton btnEjecutarRemate = new JButton("PATEAR");
    private int direccionElegida = 1;
    private final JSlider sliderPotencia = new JSlider(20, 100, 70);
    private final JLabel lblPotencia = new JLabel("Potencia del disparo: 70%");
    private final JLabel lblAuto = new JLabel("El partido avanza automáticamente");
    private final Random azar = new Random();
    private final Timer autoTimer = new Timer(4800, e -> elegir(azar.nextInt(3)));
    private final Timer avanceTimer = new Timer(1900, e -> continuar());

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
        JPanel filas = new JPanel(new GridLayout(4, 1, 0, 2));
        filas.add(fila);
        filas.add(lblReloj);
        barraTiempo.setStringPainted(true);
        barraTiempo.setString("0' / 90'");
        filas.add(barraTiempo);
        filas.add(lblStats);
        norte.add(filas, BorderLayout.CENTER);
        add(norte, BorderLayout.NORTH);

        centro.add(cancha, "cancha");
        centro.add(panelRemate, "remate");
        add(centro, BorderLayout.CENTER);

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
            btnOpc[i].addActionListener(e -> {
                if (esPelotaParada()) seleccionarDireccion(op);
                else elegir(op);
            });
            botones.add(btnOpc[i]);
        }
        panelOp.add(lblTitulo);
        panelOp.add(Box.createVerticalStrut(4));
        panelOp.add(lblDesc);
        panelOp.add(Box.createVerticalStrut(8));
        panelOp.add(botones);
        sliderPotencia.setVisible(false);
        lblPotencia.setVisible(false);
        sliderPotencia.setMajorTickSpacing(20);
        sliderPotencia.setMinorTickSpacing(5);
        sliderPotencia.setPaintTicks(true);
        sliderPotencia.setPaintLabels(true);
        sliderPotencia.addChangeListener(e -> {
            lblPotencia.setText("Potencia del disparo: " + sliderPotencia.getValue() + "%");
            if (esPelotaParada() && situacionActual != null) {
                panelRemate.configurar(situacionActual.tipo == TipoJugada.TIRO_LIBRE,
                        direccionElegida - 1, sliderPotencia.getValue());
            }
        });
        btnEjecutarRemate.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        btnEjecutarRemate.setBackground(new Color(0x27ae60));
        btnEjecutarRemate.setForeground(Color.WHITE);
        btnEjecutarRemate.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnEjecutarRemate.setVisible(false);
        btnEjecutarRemate.addActionListener(e -> elegir(direccionElegida));
        lblAuto.setForeground(new Color(0x27ae60));
        panelOp.add(lblPotencia);
        panelOp.add(sliderPotencia);
        panelOp.add(lblAuto);
        panelOp.add(btnEjecutarRemate);

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

    public void iniciar(String nombreDT, Onceideal once, Torneo torneo) {
        this.torneo = torneo;
        partido = torneo.nuevoPartido(nombreDT, once.getListaJugadores());
        idx = 0;
        golesPrevios = 0;
        minutoPartido = 0;
        relojPartido.stop();
        barraTiempo.setValue(0);
        barraTiempo.setString("0' / 90'");
        lblReloj.setText("00:00  ·  PRIMER TIEMPO");
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
            relojPartido.start();
            mostrarSituacion();
        });
    }

    private void mostrarSituacion() {
        Situacion s = partido.situacion(idx);
        situacionActual = s;
        if (idx < partido.normales() && s.minuto > minutoPartido) {
            minutoPartido = s.minuto;
            actualizarReloj();
        }
        if (idx >= partido.normales()) {
            lblInfo.setText(torneo.nombreFase() + "   ·   TANDA DE PENALES   ·   " + partido.getPenFavor() + " - " + partido.getPenContra());
        } else {
            lblInfo.setText(torneo.nombreFase() + "   ·   Jugada " + (idx + 1) + " / " + partido.normales()
                    + "   ·   Minuto " + s.minuto + "'");
        }
        lblTitulo.setText(s.titulo.toUpperCase());
        boolean pelotaParada = s.tipo == TipoJugada.PENAL_FAVOR || s.tipo == TipoJugada.PENAL_CONTRA
                || s.tipo == TipoJugada.TIRO_LIBRE;
        sliderPotencia.setVisible(pelotaParada);
        lblPotencia.setVisible(pelotaParada);
        btnEjecutarRemate.setVisible(pelotaParada);
        direccionElegida = 1;
        lblAuto.setText(pelotaParada ? "Elegí dirección y potencia del remate"
                : "La jugada se resolverá automáticamente en unos segundos");
        if (!pelotaParada) autoTimer.restart(); else autoTimer.stop();
        lblDesc.setText(html(s.descripcion));
        for (int i = 0; i < 3; i++) {
            btnOpc[i].setText(pelotaParada
                    ? new String[]{"IZQUIERDA", "CENTRO", "DERECHA"}[i]
                    : s.opciones[i]);
            btnOpc[i].setEnabled(true);
        }
        btnEjecutarRemate.setEnabled(true);
        cancha.limpiar();
        if (pelotaParada) {
            panelRemate.configurar(s.tipo == TipoJugada.TIRO_LIBRE, 0, sliderPotencia.getValue());
            centroCards.show(centro, "remate");
        } else {
            centroCards.show(centro, "cancha");
        }
        cardsSur.show(sur, "op");
    }

    private boolean esPelotaParada() {
        return situacionActual != null && (situacionActual.tipo == TipoJugada.PENAL_FAVOR
                || situacionActual.tipo == TipoJugada.PENAL_CONTRA || situacionActual.tipo == TipoJugada.TIRO_LIBRE);
    }

    private void seleccionarDireccion(int op) {
        direccionElegida = op;
        for (int i = 0; i < btnOpc.length; i++) {
            btnOpc[i].setBackground(i == op ? new Color(0xffd54f) : null);
        }
        panelRemate.configurar(situacionActual.tipo == TipoJugada.TIRO_LIBRE,
                op - 1, sliderPotencia.getValue());
    }

    private void elegir(int op) {
        autoTimer.stop();
        avanceTimer.stop();
        if (situacionActual != null && (situacionActual.tipo == TipoJugada.PENAL_FAVOR
                || situacionActual.tipo == TipoJugada.PENAL_CONTRA || situacionActual.tipo == TipoJugada.TIRO_LIBRE)) {
            panelRemate.configurar(situacionActual.tipo == TipoJugada.TIRO_LIBRE, op - 1, sliderPotencia.getValue());
        }
        centroCards.show(centro, "cancha");
        for (JButton b : btnOpc) b.setEnabled(false);
        btnEjecutarRemate.setEnabled(false);
        Resultado r = partido.resolver(idx, op, sliderPotencia.getValue());
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
            if (hayMas) avanceTimer.restart();
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
        avanceTimer.stop();
        autoTimer.stop();
        if (!hayMas) {
            relojPartido.stop();
            v.irAFinal(partido);
        } else {
            idx++;
            mostrarSituacion();
        }
    }


    private void avanzarReloj() {
        if (minutoPartido >= 90) {
            minutoPartido = 90;
            actualizarReloj();
            relojPartido.stop();
            return;
        }
        minutoPartido++;
        actualizarReloj();
    }

    private void actualizarReloj() {
        barraTiempo.setValue(minutoPartido);
        barraTiempo.setString(minutoPartido + "' / 90'");
        String etapa = minutoPartido < 45 ? "PRIMER TIEMPO"
                : minutoPartido == 45 ? "ENTRETIEMPO" : minutoPartido < 90 ? "SEGUNDO TIEMPO" : "FINAL";
        lblReloj.setText(String.format("%02d:%02d  ·  %s", minutoPartido, 0, etapa));
    }

    private static String html(String texto) {
        return "<html><div style='width:660px'>" + texto + "</div></html>";
    }
}
