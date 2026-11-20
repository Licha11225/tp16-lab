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
import javax.swing.JProgressBar;
import javax.swing.Timer;

import arcade.Formacion;
import arcade.Partido;
import arcade.Ratings;
import arcade.Resultado;
import arcade.Situacion;
import arcade.TipoJugada;
import arcade.Torneo;
import model.Onceideal;

/**
 * Un partido de la eliminatoria, jugado "en vivo": corre un reloj de 90 minutos, se ve a los jugadores moverse y
 * pasarse la pelota, y en momentos puntuales el partido se frena y el DT decide qué hacer.
 * Los penales y tiros libres propios se patean con las flechas y la barra espaciadora.
 */
public class PanelPartido extends JPanel {

    private enum Estado { INTRO, JUGANDO, DECIDIENDO, RESULTADO, FIN }

    private static final Color AZUL = new Color(0x56ccf2);
    private static final Color DORADO = new Color(0xffd54f);
    /** Milisegundos reales por cada minuto del partido a velocidad x1 (90 minutos ≈ 100 segundos). */
    private static final double MS_POR_MIN_DEFECTO = 1100;
    private static final double[] VELOCIDADES = {1, 2, 4};

    private final VentanaPrincipal v;
    private final CanchaPanel cancha = new CanchaPanel();
    private final PatadaPanel patada = new PatadaPanel();
    private final CardLayout cardsCentro = new CardLayout();
    private final JPanel centro = new JPanel(cardsCentro);

    private final JLabel lblLocal = new JLabel("", JLabel.RIGHT);
    private final JLabel lblMarcador = new JLabel("0 - 0", JLabel.CENTER);
    private final JLabel lblRival = new JLabel("", JLabel.LEFT);
    private final JLabel lblInfo = new JLabel(" ", JLabel.CENTER);
    private final JLabel lblStats = new JLabel(" ", JLabel.CENTER);
    private final JProgressBar barraTiempo = new JProgressBar(0, 90);
    private final JProgressBar barraPosesion = new JProgressBar(0, 100);
    private final JButton btnVelocidad = new JButton("Velocidad x1");

    private final CardLayout cardsSur = new CardLayout();
    private final JPanel sur = new JPanel(cardsSur);
    private final JLabel lblVivo = new JLabel();
    private final JLabel lblTitulo = new JLabel();
    private final JLabel lblDesc = new JLabel();
    private final JPanel panelBotones = new JPanel(new GridLayout(1, 4, 10, 0));
    private final JButton[] btnOpc = new JButton[4];
    private final JLabel lblNarracion = new JLabel();
    private final JButton btnContinuar = new JButton("Seguir jugando");

    private final Timer reloj;
    private Estado estado = Estado.INTRO;
    private Partido partido;
    private Torneo torneo;
    private int idx;
    private boolean hayMasTanda;
    private int golesPrevios;
    private double msJuego;
    private long ultimoNs;
    private long ultimaPosesionMs;
    private int velocidadIdx = 0;
    private final double msPorMin = MS_POR_MIN_DEFECTO;

    public PanelPartido(VentanaPrincipal v) {
        this.v = v;
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 12, 12, 12));

        // ---- norte: marcador, reloj, posesión
        JPanel norte = new JPanel();
        norte.setLayout(new BoxLayout(norte, BoxLayout.Y_AXIS));
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
        lblInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblInfo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        lblInfo.setPreferredSize(new Dimension(700, 22));
        lblStats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        lblStats.setForeground(Color.GRAY);
        lblStats.setFont(lblStats.getFont().deriveFont(11f));
        lblStats.setAlignmentX(Component.CENTER_ALIGNMENT);

        barraTiempo.setStringPainted(true);
        barraTiempo.setString("0'");
        barraPosesion.setStringPainted(true);
        barraPosesion.setValue(50);
        barraPosesion.setString("Posesión 50% - 50%");
        btnVelocidad.addActionListener(e -> {
            velocidadIdx = (velocidadIdx + 1) % VELOCIDADES.length;
            btnVelocidad.setText("Velocidad x" + (int) VELOCIDADES[velocidadIdx]);
        });
        btnVelocidad.setFocusable(false);
        JPanel filaTiempo = new JPanel(new BorderLayout(8, 0));
        filaTiempo.add(barraTiempo, BorderLayout.CENTER);
        filaTiempo.add(btnVelocidad, BorderLayout.EAST);
        filaTiempo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        norte.add(fila);
        norte.add(lblInfo);
        norte.add(Box.createVerticalStrut(4));
        norte.add(filaTiempo);
        norte.add(Box.createVerticalStrut(4));
        norte.add(barraPosesion);
        norte.add(lblStats);
        add(norte, BorderLayout.NORTH);

        // ---- centro: la cancha en vivo o la escena de patada
        centro.add(cancha, "cancha");
        centro.add(patada, "patada");
        add(centro, BorderLayout.CENTER);

        // ---- sur
        lblVivo.setHorizontalAlignment(JLabel.CENTER);
        lblVivo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        JPanel panelVivo = new JPanel(new BorderLayout());
        panelVivo.add(lblVivo, BorderLayout.CENTER);

        JPanel panelOp = new JPanel();
        panelOp.setLayout(new BoxLayout(panelOp, BoxLayout.Y_AXIS));
        lblTitulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        lblTitulo.setForeground(DORADO);
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelBotones.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelBotones.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        for (int i = 0; i < 4; i++) {
            final int op = i;
            btnOpc[i] = new JButton();
            btnOpc[i].setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            btnOpc[i].setPreferredSize(new Dimension(200, 54));
            btnOpc[i].addActionListener(e -> elegir(op));
            panelBotones.add(btnOpc[i]);
        }
        panelOp.add(lblTitulo);
        panelOp.add(Box.createVerticalStrut(4));
        panelOp.add(lblDesc);
        panelOp.add(Box.createVerticalStrut(8));
        panelOp.add(panelBotones);

        JPanel panelRes = new JPanel(new BorderLayout(0, 8));
        lblNarracion.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        btnContinuar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        btnContinuar.setPreferredSize(new Dimension(260, 48));
        btnContinuar.addActionListener(e -> continuar());
        JPanel pie = new JPanel();
        pie.add(btnContinuar);
        panelRes.add(lblNarracion, BorderLayout.CENTER);
        panelRes.add(pie, BorderLayout.SOUTH);

        sur.add(panelVivo, "vivo");
        sur.add(panelOp, "op");
        sur.add(panelRes, "res");
        sur.setPreferredSize(new Dimension(800, 150));
        add(sur, BorderLayout.SOUTH);

        reloj = new Timer(40, e -> tickReloj());
    }

    /** `once` ya viene ordenado por casillero, igual que la formación elegida. */
    public void iniciar(String nombreDT, Onceideal once, Torneo torneo, Formacion formacion) {
        this.torneo = torneo;
        partido = torneo.nuevoPartido(nombreDT, once.getListaJugadores(), formacion);
        idx = 0;
        golesPrevios = 0;
        msJuego = 0;
        hayMasTanda = false;
        estado = Estado.INTRO;
        reloj.stop();
        patada.limpiar();
        cancha.limpiar();
        cancha.limpiarNombres();
        cancha.setFormaciones(formacion, partido.getFormacionRival());
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
        barraTiempo.setValue(0);
        barraTiempo.setString("0'");
        barraPosesion.setValue(50);
        barraPosesion.setString("Posesión 50% - 50%");
        cardsCentro.show(centro, "cancha");
        lblVivo.setText(html("<b>" + torneo.nombreFase() + "</b> — " + nombreDT + " (" + formacion.getNombre()
                + ") vs " + partido.getRival() + " (" + partido.getFormacionRival().getNombre() + ")"));
        cardsSur.show(sur, "vivo");
        cancha.cartel(torneo.nombreFase(), this::comenzar);
    }

    // ------------------------------------------------------------------ reloj

    private void comenzar() {
        estado = Estado.JUGANDO;
        actualizarEstilo();
        cancha.vivoIniciar();
        ultimoNs = System.nanoTime();
        ultimaPosesionMs = 0;
        lblVivo.setText(html("⚽ <b>El partido está en juego.</b> Cuando aparezca una jugada importante, "
                + "se frena y vas a poder decidir qué hacer."));
        cardsSur.show(sur, "vivo");
        reloj.start();
    }

    private void actualizarEstilo() {
        cancha.setEstiloVivo(partido.posesionBase(), partido.estiloDirecto());
    }

    private void tickReloj() {
        long ahora = System.nanoTime();
        double dt = Math.min(100, (ahora - ultimoNs) / 1_000_000.0);
        ultimoNs = ahora;
        if (estado != Estado.JUGANDO) return;

        msJuego += dt * VELOCIDADES[velocidadIdx];
        int min = (int) Math.min(90, msJuego / msPorMin);
        actualizarReloj(min);

        if (idx < partido.normales() && min >= partido.minutoDe(idx)) {
            empezarMomento();
        } else if (min >= 90 && idx >= partido.normales()) {
            finDelTiempo();
        }
    }

    private void actualizarReloj(int min) {
        barraTiempo.setValue(min);
        barraTiempo.setString(min + "'  ·  " + (min <= 45 ? "1er tiempo" : "2do tiempo"));
        lblInfo.setText(torneo.nombreFase() + "   ·   Minuto " + min + "'");
        long ahoraMs = System.currentTimeMillis();
        if (ahoraMs - ultimaPosesionMs > 500) {
            ultimaPosesionMs = ahoraMs;
            int p = (int) Math.round(cancha.getPosesionUsuario() * 100);
            barraPosesion.setValue(p);
            barraPosesion.setString("Posesión " + p + "% - " + (100 - p) + "%");
        }
    }

    // ------------------------------------------------------------------ momentos del partido

    private void empezarMomento() {
        estado = Estado.DECIDIENDO;
        cancha.vivoPausar();
        cancha.cartel(titularDe(partido.tipoDe(idx)), this::mostrarSituacion);
    }

    private static String titularDe(TipoJugada t) {
        return switch (t) {
            case PENAL_FAVOR -> "¡PENAL A FAVOR!";
            case PENAL_CONTRA -> "¡PENAL EN CONTRA!";
            case CORNER -> "¡CÓRNER!";
            case CORNER_CONTRA -> "CÓRNER EN CONTRA";
            case TIRO_LIBRE -> "¡TIRO LIBRE!";
            case TIRO_LIBRE_CONTRA -> "TIRO LIBRE EN CONTRA";
            case ENTRETIEMPO -> "ENTRETIEMPO";
            case CONTRAGOLPE -> "¡CONTRAGOLPE!";
            case MANO_A_MANO -> "¡MANO A MANO!";
            case ATAQUE -> "¡LLEGÁS CON PELIGRO!";
            case DEFENSA -> "¡PELIGRO EN TU AREA!";
            default -> "MEDIOCAMPO";
        };
    }


    private void mostrarSituacion() {
        estado = Estado.DECIDIENDO;
        Situacion s = partido.situacion(idx);
        if (idx >= partido.normales()) {
            lblInfo.setText(torneo.nombreFase() + "   ·   TANDA DE PENALES   ·   "
                    + partido.getPenFavor() + " - " + partido.getPenContra());
        } else {
            lblInfo.setText(torneo.nombreFase() + "   ·   Minuto " + s.minuto + "'");
        }
        lblTitulo.setText(s.titulo.toUpperCase());
        lblDesc.setText(html(s.descripcion));

        if (partido.esPatada(idx)) {
            // escena de espaldas: se apunta con las flechas y la potencia se carga con ESPACIO
            panelBotones.setVisible(false);
            cardsSur.show(sur, "op");
            cardsCentro.show(centro, "patada");
            patada.preparar(partido.tipoDe(idx) == TipoJugada.TIRO_LIBRE, partido.nombrePateador(idx),
                    partido.nombreArquero(), partido.numeroPateador(idx), this::patear);
        } else if (partido.esAtajada(idx)) {
            // penal en contra: el rival patea de espaldas y el usuario elige hacia dónde se tira su arquero
            panelBotones.setVisible(false);
            cardsSur.show(sur, "op");
            cardsCentro.show(centro, "patada");
            int pista = partido.prepararPenalContra(idx);
            patada.prepararArquero(partido.nombrePateadorRival(), partido.nombreArqueroPropio(),
                    partido.numeroPateadorRival(), pista, this::atajar);
        } else {
            cardsCentro.show(centro, "cancha");
            panelBotones.setVisible(true);
            for (int i = 0; i < 4; i++) {
                boolean usa = i < s.opciones.length;
                btnOpc[i].setVisible(usa);
                if (usa) {
                    btnOpc[i].setText(s.opciones[i]);
                    btnOpc[i].setEnabled(true);
                }
            }
            cardsSur.show(sur, "op");
        }
    }

    private void atajar(double dx, double dy, double momento) {
        Resultado r = partido.atajar(idx, dx, dy, momento);
        patada.reproducir(r.escena, () -> terminarResolucion(r));
    }

    private void patear(double ax, double ay, double potencia) {
        Resultado r = partido.patear(idx, ax, ay, potencia);
        patada.reproducir(r.escena, () -> terminarResolucion(r));
    }

    private void elegir(int op) {
        for (JButton b : btnOpc) b.setEnabled(false);
        Resultado r = partido.resolver(idx, op);
        if (r.escena == null) {
            terminarResolucion(r);
        } else {
            cancha.reproducir(r.escena, () -> terminarResolucion(r));
        }
    }

    private void terminarResolucion(Resultado r) {
        lblMarcador.setText(partido.getGolesFavor() + " - " + partido.getGolesContra());
        if (partido.getGolesFavor() + partido.getGolesContra() > golesPrevios) destacarMarcador(r.golFavor > 0);
        golesPrevios = partido.getGolesFavor() + partido.getGolesContra();
        lblStats.setText(partido.resumenEquipos());
        lblNarracion.setText(html(r.narracion));

        if (idx >= partido.normales()) {
            lblInfo.setText(torneo.nombreFase() + "   ·   TANDA DE PENALES   ·   "
                    + partido.getPenFavor() + " - " + partido.getPenContra());
            hayMasTanda = partido.hayMas(idx);   // si siguen igualados, acá se arma otra ronda
            btnContinuar.setText(hayMasTanda ? "Siguiente penal" : "Ver resultado final");
        } else {
            btnContinuar.setText(idx + 1 >= partido.normales() ? "Seguir hasta el final" : "Seguir jugando");
        }
        estado = Estado.RESULTADO;
        cardsSur.show(sur, "res");
    }

    private void continuar() {
        if (idx >= partido.normales()) {         // tanda de penales
            if (!hayMasTanda) {
                terminarPartido();
            } else {
                idx++;
                patada.limpiar();
                mostrarSituacion();
            }
            return;
        }
        idx++;
        patada.limpiar();
        cardsCentro.show(centro, "cancha");
        actualizarEstilo();
        cancha.vivoReanudar();
        ultimoNs = System.nanoTime();
        estado = Estado.JUGANDO;
        cardsSur.show(sur, "vivo");
    }

    private void finDelTiempo() {
        estado = Estado.DECIDIENDO;
        cancha.vivoPausar();
        if (partido.empatado()) {
            partido.iniciarTanda();
            idx = partido.normales();
            cancha.cartel("¡EMPATE! A PENALES", this::mostrarSituacion);
        } else {
            cancha.cartel("FINAL DEL PARTIDO", this::terminarPartido);
        }
    }

    private void terminarPartido() {
        estado = Estado.FIN;
        reloj.stop();
        patada.limpiar();
        cancha.vivoDetener();
        v.irAFinal(partido);
    }

    /** El marcador "salta" un momento cuando hay gol. */
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

    private static String html(String texto) {
        return "<html><div style='width:660px'>" + texto + "</div></html>";
    }
    // ------------------------------------------------------------------ para pruebas
    // ------------------------------------------------------------------ para pruebas
    /** true cuando ya apareció la decisión (botones o escena de patada) y falta que el usuario elija. */}
