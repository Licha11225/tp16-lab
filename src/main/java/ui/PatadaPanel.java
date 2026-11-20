package ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

import arcade.Escena;


public class PatadaPanel extends JPanel {

    public interface Disparo {
        void patear(double ax, double ay, double potencia);
    }

    public interface Atajada {
        void atajar(double dx, double dy, double momento);
    }

    private enum Modo { QUIETO, APUNTAR, ATAJAR, ESPERA, ANIM, FIN }

    private static final Color DORADO = new Color(0xffd54f);
    private static final Color[] CONFETI = {
            new Color(0xffd54f), new Color(0xff5252), new Color(0x56ccf2), new Color(0x6fcf97), Color.WHITE};
    private static final Color[] PUBLICO = {
            new Color(0x3a4a5e), new Color(0x6b3a3a), new Color(0x3a6b4f), new Color(0x7a6a3a), new Color(0x59406b),
            new Color(0x8a8a9a), new Color(0xb04040)};
    private static final Color[] CARTELES = {
            new Color(0xc62828), new Color(0x1565c0), new Color(0xf9a825), new Color(0x2e7d32), new Color(0x6a1b9a)};
    private static final String[] TEXTOS_CARTEL = {"FÚTBOL 11", "ARCADE", "TP 16", "DT", "COPA"};

    private static final double CARRERA = 420;      // ms de carrerilla antes de pegarle
    private static final double SWEET_MIN = 0.66, SWEET_MAX = 0.84;

    private Modo modo = Modo.QUIETO;
    private final Timer timer;
    private final Random rnd = new Random();
    private final long t0 = System.nanoTime();
    private long ultimo, ini;

    private boolean libre;
    private String tirador = "", arquero = "";
    private int numero = 9;
    private Disparo disparo;

    private Atajada atajada;
    private boolean rival;                  // true cuando patea el rival
    private int pista;                      // hacia dónde se perfila el pateador rival (-1, 0, 1)
    private double dx, dy = 0.4, transcurrido, momentoSalto = -1, msSalto;
    private boolean saltoHecho;
    private double kSx, kSy, kSang, kSest;  // dónde quedó el arquero cuando el rival patea
    private static final double CARRERA_RIVAL = 2300;   // tiempo que tiene el usuario para decidir

    private boolean izq, der, arr, aba, cargando;
    private double ax = 0, ay = 0.5, pot = 0;

    private Escena esc;
    private Runnable alTerminar;
    private double ms;
    private double dur, tImpacto, tBanner, bannerDur;
    private boolean impactoHecho, hayRebote;
    private double destX, destY, rebX, rebY;
    private int muro;
    private double giro, shake, flash;
    private final List<double[]> confeti = new ArrayList<>();
    private final int[][] publico = new int[420][3];

    private double W, H, cx, gw, gh, gyB, bolaX, bolaY, muroY, muroH;

    public PatadaPanel() {
        setPreferredSize(new Dimension(700, 430));
        setBackground(new Color(0x15181c));
        setFocusable(true);
        Random r = new Random(11);
        for (int i = 0; i < publico.length; i++) {
            publico[i][0] = r.nextInt(1000);
            publico[i][1] = r.nextInt(1000);
            publico[i][2] = r.nextInt(PUBLICO.length);
        }
        timer = new Timer(16, e -> tick());
        teclas();
    }

    // ------------------------------------------------------------------ teclado

    private void teclas() {
        tecla(KeyEvent.VK_LEFT, "izq", b -> izq = b);
        tecla(KeyEvent.VK_RIGHT, "der", b -> der = b);
        tecla(KeyEvent.VK_UP, "arr", b -> arr = b);
        tecla(KeyEvent.VK_DOWN, "aba", b -> aba = b);
        tecla(KeyEvent.VK_A, "izqA", b -> izq = b);
        tecla(KeyEvent.VK_D, "derD", b -> der = b);
        tecla(KeyEvent.VK_W, "arrW", b -> arr = b);
        tecla(KeyEvent.VK_S, "abaS", b -> aba = b);
        tecla(KeyEvent.VK_SPACE, "espacio", b -> {
            if (modo == Modo.ATAJAR) {
                if (b && !saltoHecho) tirarse();
                return;
            }
            if (modo != Modo.APUNTAR) return;
            if (b) {
                cargando = true;
            } else if (cargando) {
                soltar();
            }
        });
    }

    private void tecla(int vk, String nombre, java.util.function.Consumer<Boolean> accion) {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(vk, 0, false), nombre + ".p");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(vk, 0, true), nombre + ".r");
        getActionMap().put(nombre + ".p", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { accion.accept(true); }
        });
        getActionMap().put(nombre + ".r", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { accion.accept(false); }
        });
    }
    /** Para probar sin teclado: el arquero se tira a (x, y) cuando pasó esa fracción de la carrerilla. */



    private void tirarse() {
        saltoHecho = true;
        momentoSalto = Math.max(0, Math.min(1, transcurrido / CARRERA_RIVAL));
        msSalto = 0;
        izq = der = arr = aba = false;
    }
    /** Para probar sin teclado: mueve la mira y patea. */

    private void soltar() {
        cargando = false;
        izq = der = arr = aba = false;
        modo = Modo.ESPERA;
        double p = Math.max(0.05, pot);
        if (disparo != null) disparo.patear(ax, ay, p);
    }

    // ------------------------------------------------------------------ API

    /**
     * Penal en contra: patea el rival (de espaldas, con camiseta roja) y el usuario controla a su arquero.
     * `pista` es hacia dónde se perfila el pateador (puede ser un amague). El rival patea al terminar el tiempo.
     */
    public void prepararArquero(String pateador, String arquero, int numero, int pista, Atajada atajada) {
        this.libre = false;
        this.rival = true;
        this.tirador = pateador;
        this.arquero = arquero;
        this.numero = numero;
        this.pista = pista;
        this.atajada = atajada;
        this.disparo = null;
        esc = null;
        ms = 0;
        dx = 0;
        dy = 0.4;
        transcurrido = 0;
        momentoSalto = -1;
        msSalto = 0;
        saltoHecho = false;
        izq = der = arr = aba = cargando = false;
        shake = flash = 0;
        confeti.clear();
        modo = Modo.ATAJAR;
        ultimo = System.nanoTime();
        timer.start();
        repaint();
        javax.swing.SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    /** Muestra la escena y espera que el usuario apunte y cargue la potencia. */
    public void preparar(boolean libre, String tirador, String arquero, int numero, Disparo disparo) {
        this.rival = false;
        this.pista = 0;
        this.libre = libre;
        this.tirador = tirador;
        this.arquero = arquero;
        this.numero = numero;
        this.disparo = disparo;
        esc = null;
        ms = 0;
        ax = 0;
        ay = 0.5;
        pot = 0;
        cargando = false;
        izq = der = arr = aba = false;
        shake = flash = 0;
        confeti.clear();
        modo = Modo.APUNTAR;
        ultimo = System.nanoTime();
        timer.start();
        repaint();
        javax.swing.SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    /** Anima el resultado de la patada. */
    public void reproducir(Escena e, Runnable fin) {
        esc = e;
        alTerminar = fin;
        libre = e.libre;
        rival = e.rival;
        geo();
        calcular();
        confeti.clear();
        impactoHecho = false;
        shake = flash = 0;
        modo = Modo.ANIM;
        ms = 0;
        ini = System.nanoTime();
        ultimo = ini;
        timer.start();
        repaint();
    }

    public void limpiar() {
        timer.stop();
        modo = Modo.QUIETO;
        esc = null;
        rival = false;
        saltoHecho = false;
        izq = der = arr = aba = cargando = false;
        confeti.clear();
        repaint();
    }

    /** Cuánto dura la carrerilla antes del remate: cuando patea el rival ya pasó (la vivió el usuario antes). */
    private double carreraMs() {
        return (esc != null && esc.rival) ? 0 : CARRERA;
    }

    // ------------------------------------------------------------------ geometría

    private void geo() {
        W = getWidth();
        H = getHeight();
        cx = W / 2.0;
        gw = Math.min(W * 0.46, H * 0.80);
        gh = gw * 0.34;
        gyB = H * 0.40;
        bolaX = cx;
        bolaY = libre ? H * 0.80 : H * 0.84;
        muroY = H * 0.64;
        muroH = H * 0.30;
    }

    private double sx(double ux) { return cx + ux * gw / 2.0; }
    private double sy(double uy) { return gyB - uy * gh; }
    private double muroX(int i) { return cx + (i - 1.5) * muroH * 0.20; }

    /** Calcula adónde va la pelota y cómo rebota según cómo terminó la jugada. */
    private void calcular() {
        dur = 760 - 460 * esc.potencia;
        tImpacto = carreraMs() + dur;
        hayRebote = false;
        rebX = rebY = 0;
        muro = Math.max(0, esc.muro);
        switch (esc.fin) {
            case PERDIDA -> {
                destX = muroX(muro);
                destY = muroY - muroH * 0.62;
                rebX = (rnd.nextBoolean() ? 1 : -1) * W * 0.05;
                rebY = H * 0.10;
                hayRebote = true;
            }
            case ATAJADA -> {
                destX = sx(esc.tiroX);
                destY = sy(esc.tiroY);
                rebX = (rnd.nextBoolean() ? 1 : -1) * gw * 0.25;
                rebY = gh * 1.0;
                hayRebote = true;
            }
            case PALO -> {
                destX = sx(Math.max(-1.0, Math.min(1.0, esc.tiroX)));
                destY = sy(Math.min(1.0, esc.tiroY));
                if (Math.abs(esc.tiroX) <= 1.0 && esc.tiroY > 0.94) destY = sy(1.0);       // travesaño
                rebX = -Math.signum(esc.tiroX == 0 ? 1 : esc.tiroX) * gw * 0.12;
                rebY = gh * 1.1;
                hayRebote = true;
            }
            default -> {
                destX = sx(esc.tiroX);
                destY = sy(esc.tiroY);
            }
        }
        tBanner = tImpacto + (hayRebote ? 520 : 260);
        bannerDur = esc.fin == Escena.Fin.GOL_FAVOR ? 2300 : 1500;
    }

    // ------------------------------------------------------------------ animación

    void tick() {
        long now = System.nanoTime();
        double dt = Math.min(50, (now - ultimo) / 1_000_000.0);
        ultimo = now;
        shake *= 0.90;
        flash = Math.max(0, flash - dt / 450.0);
        for (int i = confeti.size() - 1; i >= 0; i--) {
            double[] p = confeti.get(i);
            p[0] += p[2] * dt;
            p[1] += p[3] * dt;
            p[3] += 0.0012 * dt;
            p[4] += p[5] * dt;
            p[7] -= dt;
            if (p[7] <= 0) confeti.remove(i);
        }
        if (null != modo) switch (modo) {
            case APUNTAR -> {
                double v = dt / 1000.0;
                if (izq) ax -= 1.25 * v;
                if (der) ax += 1.25 * v;
                if (arr) ay += 1.0 * v;
                if (aba) ay -= 1.0 * v;
                ax = Math.max(-1.25, Math.min(1.25, ax));
                ay = Math.max(0.03, Math.min(1.25, ay));
                if (cargando) pot = Math.min(1.0, pot + dt / 1150.0);
                }
            case ATAJAR -> {
                double v = dt / 1000.0;
                if (!saltoHecho) {
                    if (izq) dx -= 1.25 * v;
                    if (der) dx += 1.25 * v;
                    if (arr) dy += 1.0 * v;
                    if (aba) dy -= 1.0 * v;
                    dx = Math.max(-1.0, Math.min(1.0, dx));
                    dy = Math.max(0.05, Math.min(1.0, dy));
                }       transcurrido += dt;
                if (saltoHecho) msSalto += dt;
                if (transcurrido >= CARRERA_RIVAL) finalizarAtajada();
                }
            case ANIM -> {
                ms = (now - ini) / 1_000_000.0;
                if (ms > carreraMs() && ms < tImpacto) giro += 0.5;
                if (!impactoHecho && ms >= tImpacto) {
                    impactoHecho = true;
                    impacto();
                }   if (ms >= tBanner + bannerDur) {
                    modo = Modo.FIN;
                    timer.stop();
                    Runnable r = alTerminar;
                    alTerminar = null;
                    if (r != null) r.run();
                }
            }
            default -> {
            }
        }
        repaint();
    }

    /** Se acabó el tiempo: el rival patea, y el arquero queda donde esté (parado o en pleno salto). */
    private void finalizarAtajada() {
        double[] k = estadoArquero(0);
        kSx = k[0];
        kSy = k[1];
        kSang = k[2];
        kSest = k[3];
        modo = Modo.ESPERA;
        if (atajada != null) atajada.atajar(dx, dy, saltoHecho ? momentoSalto : -1);
    }

    private void impacto() {
        switch (esc.fin) {
            case GOL_FAVOR -> {
                flash = 1;
                for (int i = 0; i < 120; i++) {
                    double a = -Math.PI / 2 + (rnd.nextDouble() - 0.5) * 2.8;
                    double v = 0.25 + rnd.nextDouble() * 0.6;
                    confeti.add(new double[]{cx + (rnd.nextDouble() - 0.5) * gw, gyB - gh * 0.5,
                            Math.cos(a) * v, Math.sin(a) * v, rnd.nextDouble() * 6,
                            (rnd.nextDouble() - 0.5) * 0.02, rnd.nextInt(CONFETI.length), 1800});
                }
            }
            case PALO -> shake = 0.8;
            default -> { }
        }
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        geo();
        if (W < 200 || H < 150) {
            g.dispose();
            return;
        }
        if (shake > 0.02) {
            g.translate((rnd.nextDouble() - 0.5) * 2 * shake * 7, (rnd.nextDouble() - 0.5) * 2 * shake * 7);
        }
        double swayT = (System.nanoTime() - t0) / 1_000_000.0;
        boolean anim = esc != null && (modo == Modo.ANIM || modo == Modo.FIN);
        double tt = anim ? ms : 0;

        dibujarFondo(g, tt);
        dibujarRed(g, tt);
        dibujarArquero(g, swayT, tt);
        dibujarPostes(g);
        if (libre) dibujarBarrera(g, tt);
        dibujarPateador(g, tt);
        double[] b = bola(tt);
        dibujarBola(g, b, tt);
        if (!rival && (modo == Modo.APUNTAR || modo == Modo.ESPERA)) dibujarMira(g, swayT);
        if (!rival && (modo == Modo.APUNTAR || modo == Modo.ESPERA)) dibujarPotencia(g);
        if (rival && (modo == Modo.ATAJAR || modo == Modo.ESPERA)) dibujarAtajar(g, swayT);
        if (modo == Modo.ANIM && esc != null && ms < carreraMs()) {
            texto(g, "¡PATEA " + esc.tiradorNombre.toUpperCase() + "!", W / 2, H * 0.06, (int) Math.max(18, H / 20), DORADO);
        }
        if (flash > 0.01) {
            g.setColor(new Color(255, 255, 255, (int) (flash * 90)));
            g.fillRect(0, 0, (int) W, (int) H);
        }
        dibujarConfeti(g);
        dibujarBanner(g);
        g.dispose();
    }

    private void dibujarFondo(Graphics2D g, double tt) {
        // cielo y tribuna
        g.setPaint(new GradientPaint(0, 0, new Color(0x0a1220), 0, (float) gyB, new Color(0x1a2c46)));
        g.fillRect(0, 0, (int) W, (int) gyB);
        double festejo = (esc != null && esc.fin == Escena.Fin.GOL_FAVOR && tt > tImpacto) ? 1 : 0;
        double zonaPublico = gyB - H * 0.075;
        for (int[] p : publico) {
            double y = p[1] / 1000.0 * zonaPublico;
            if (festejo > 0) y -= Math.abs(Math.sin((tt - tImpacto) / 140.0 + p[0])) * 5;
            g.setColor(PUBLICO[p[2]]);
            g.fillOval((int) (p[0] / 1000.0 * W), (int) y, 6, 6);
        }
        // carteles publicitarios detrás del arco
        double bh = H * 0.065;
        double by0 = gyB - bh;
        int n = 6;
        double bw = W / (double) n;
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, (int) Math.max(11, bh * 0.45)));
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i < n; i++) {
            g.setColor(CARTELES[i % CARTELES.length]);
            g.fill(new RoundRectangle2D.Double(i * bw + 2, by0, bw - 4, bh, 4, 4));
            g.setColor(Color.WHITE);
            String t = TEXTOS_CARTEL[i % TEXTOS_CARTEL.length];
            g.drawString(t, (int) (i * bw + (bw - fm.stringWidth(t)) / 2), (int) (by0 + bh * 0.68));
        }
        // pasto con franjas en perspectiva
        for (int k = 0; k < 9; k++) {
            double y1 = gyB + (H - gyB) * Math.pow(k / 9.0, 1.35);
            double y2 = gyB + (H - gyB) * Math.pow((k + 1) / 9.0, 1.35);
            g.setColor(k % 2 == 0 ? new Color(0x2e7d32) : new Color(0x388e3c));
            g.fillRect(0, (int) y1, (int) W, (int) Math.ceil(y2 - y1));
        }
        // líneas: de gol, del área chica y del área grande (en perspectiva)
        g.setColor(new Color(255, 255, 255, 230));
        g.setStroke(new BasicStroke(2.5f));
        g.drawLine(0, (int) gyB, (int) W, (int) gyB);
        double yChica = gyB + H * 0.08;
        g.setStroke(new BasicStroke(2f));
        g.drawLine((int) (cx - gw * 0.78), (int) gyB, (int) (cx - gw * 0.95), (int) yChica);
        g.drawLine((int) (cx + gw * 0.78), (int) gyB, (int) (cx + gw * 0.95), (int) yChica);
        g.drawLine((int) (cx - gw * 0.95), (int) yChica, (int) (cx + gw * 0.95), (int) yChica);
        double yGrande = gyB + H * 0.30;
        g.drawLine((int) (cx - gw * 1.55), (int) gyB, (int) (cx - gw * 2.05), (int) yGrande);
        g.drawLine((int) (cx + gw * 1.55), (int) gyB, (int) (cx + gw * 2.05), (int) yGrande);
        g.drawLine((int) (cx - gw * 2.05), (int) yGrande, (int) (cx + gw * 2.05), (int) yGrande);
        if (!libre) g.fillOval((int) (bolaX - 5), (int) (bolaY + H * 0.03), 10, 5);   // punto del penal
    }

    private void dibujarRed(Graphics2D g, double tt) {
        g.setColor(new Color(0, 0, 0, 80));
        g.fill(new RoundRectangle2D.Double(cx - gw / 2, gyB - gh, gw, gh, 2, 2));
        double amp = 0, bx = destX, by = destY;
        if (esc != null && (esc.fin == Escena.Fin.GOL_FAVOR) && tt > tImpacto) {
            double u = tt - tImpacto;
            amp = 11 * Math.exp(-u / 320.0) * Math.cos(u / 45.0);
        }
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(255, 255, 255, 75));
        int filas = 10, cols = 30, pasos = 30;
        double x0 = cx - gw / 2, y0 = gyB - gh;
        for (int j = 0; j <= filas; j++) {
            Path2D p = new Path2D.Double();
            for (int s = 0; s <= pasos; s++) {
                double x = x0 + gw * s / pasos, y = y0 + gh * j / filas;
                y += onda(x, y, bx, by, amp);
                if (s == 0) p.moveTo(x, y); else p.lineTo(x, y);
            }
            g.draw(p);
        }
        for (int i = 0; i <= cols; i++) {
            Path2D p = new Path2D.Double();
            for (int s = 0; s <= pasos; s++) {
                double x = x0 + gw * i / cols, y = y0 + gh * s / pasos;
                y += onda(x, y, bx, by, amp);
                if (s == 0) p.moveTo(x, y); else p.lineTo(x, y);
            }
            g.draw(p);
        }
    }

    private double onda(double x, double y, double bx, double by, double amp) {
        if (amp == 0) return 0;
        double d2 = (x - bx) * (x - bx) + (y - by) * (y - by);
        double sig = gw * 0.12;
        return amp * Math.exp(-d2 / (2 * sig * sig));
    }

    private void dibujarPostes(Graphics2D g) {
        double shk = (esc != null && esc.fin == Escena.Fin.PALO && ms > tImpacto)
                ? Math.sin((ms - tImpacto) / 25.0) * 3 * Math.exp(-(ms - tImpacto) / 300.0) : 0;
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(Math.max(5f, (float) (gw * 0.014)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double x0 = cx - gw / 2, x1 = cx + gw / 2, yT = gyB - gh;
        g.draw(new java.awt.geom.Line2D.Double(x0 + shk, gyB, x0 + shk, yT));
        g.draw(new java.awt.geom.Line2D.Double(x0, yT + shk, x1, yT + shk));
        g.draw(new java.awt.geom.Line2D.Double(x1 + shk, yT, x1 + shk, gyB));
    }

    /**
     * Dónde está el arquero: {x, y, ángulo, estirado}. Parado (con balanceo) o saltando hacia el objetivo elegido.
     * En una patada rival, después del remate sigue desde donde estaba hasta donde lo lleva el resultado.
     */
    private double[] estadoArquero(double tt) {
        double kh = gh * 0.85;
        double swayT = (System.nanoTime() - t0) / 1_000_000.0;
        boolean anim = esc != null && (modo == Modo.ANIM || modo == Modo.FIN);
        double idleY = gyB - kh * 0.5;
        if (rival && anim) {
            double kp = Math.min(1, tt / 380.0);
            kp = 1 - (1 - kp) * (1 - kp);
            double tx = sx(Math.max(-0.95, Math.min(0.95, esc.arqX)));
            double ty = sy(Math.max(0.08, Math.min(0.95, esc.arqY)));
            double angF = Math.max(-1.2, Math.min(1.2, esc.arqX * 1.25));
            // si no se tiró, el resultado lo deja en el medio: no hay movimiento
            return new double[]{kSx + (tx - kSx) * kp, kSy + (ty - kSy) * kp, kSang + (angF - kSang) * kp,
                    kSest + (1 - kSest) * kp * (Math.abs(esc.arqX) < 0.01 && esc.arqY < 0.41 ? 0 : 1)};
        }
        if (rival) {                              // el usuario controla: parado o en pleno salto
            if (!saltoHecho) {
                return new double[]{cx + Math.sin(swayT / 170.0) * gw * 0.035, idleY, 0, 0};
            }
            double kp = Math.min(1, msSalto / 420.0);
            kp = 1 - (1 - kp) * (1 - kp);
            double tx = sx(Math.max(-0.95, Math.min(0.95, dx)));
            double ty = sy(Math.max(0.08, Math.min(0.95, dy)));
            return new double[]{cx + (tx - cx) * kp, idleY + (ty - idleY) * kp,
                    Math.max(-1.2, Math.min(1.2, dx * 1.25)) * kp, kp};
        }
        double sway = (!anim || tt < carreraMs()) ? Math.sin(swayT / 170.0) * gw * 0.035 : 0;
        double idleX = cx + sway;
        double kp = 0, kx = idleX, ky = idleY, ang = 0;
        if (anim && esc.patada && tt > carreraMs() + 100) {
            kp = Math.min(1, (tt - carreraMs() - 100) / 420.0);
            kp = 1 - (1 - kp) * (1 - kp);
            double tx = sx(Math.max(-0.95, Math.min(0.95, esc.arqX)));
            double ty = sy(Math.max(0.08, Math.min(0.95, esc.arqY)));
            kx = idleX + (tx - idleX) * kp;
            ky = idleY + (ty - idleY) * kp;
            ang = Math.max(-1.2, Math.min(1.2, esc.arqX * 1.25)) * kp;
        }
        return new double[]{kx, ky, ang, kp};
    }

    private void dibujarArquero(Graphics2D g, double tt, double tt1) {
        double kw = gw * 0.085, kh = gh * 0.85;
        double[] k = estadoArquero(tt);
        // el arquero propio va de dorado; el rival, de naranja
        Color c = rival ? new Color(0xf2c94c) : new Color(0xf2994a);
        figura(g, k[0], k[1], k[2], k[3], c, kw, kh, true, 0, 0);
    }

    private void dibujarBarrera(Graphics2D g, double tt) {
        double kw = muroH * 0.22;
        for (int i = 0; i < 4; i++) {
            double sac = (esc != null && esc.fin == Escena.Fin.PERDIDA && i == muro && tt > tImpacto)
                    ? Math.sin((tt - tImpacto) / 28.0) * 5 * Math.exp(-(tt - tImpacto) / 260.0) : 0;
            figura(g, muroX(i) + sac, muroY - muroH / 2, 0, 0, new Color(0xe53935), kw, muroH, false, 0, 1);
        }
    }

    /**
     * Muñeco visto de frente (arquero y barrera): torso, cabeza, piernas y, si es arquero, brazos que se estiran.
     * modoFigura 1 = jugador de barrera (con las manos cubriendo).
     */
    private void figura(Graphics2D g, double cx0, double cy0, double ang, double estira, Color c,
                        double kw, double kh, boolean arquero, double fase, int modoFigura) {
        AffineTransform old = g.getTransform();
        g.translate(cx0, cy0);
        g.rotate(ang);
        g.setStroke(new BasicStroke((float) (kw * 0.30), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0x37474f));
        g.drawLine((int) (-kw * 0.22), (int) (kh * 0.18), (int) (-kw * 0.30), (int) (kh * 0.50));
        g.drawLine((int) (kw * 0.22), (int) (kh * 0.18), (int) (kw * 0.30), (int) (kh * 0.50));
        g.setColor(c);
        g.fill(new RoundRectangle2D.Double(-kw / 2, -kh * 0.36, kw, kh * 0.58, kw * 0.4, kw * 0.4));
        if (modoFigura == 1) {            // camiseta a rayas y manos cubriendo
            g.setColor(new Color(255, 255, 255, 120));
            for (int s = -1; s <= 1; s++) g.fill(new java.awt.geom.Rectangle2D.Double(s * kw * 0.26 - kw * 0.06, -kh * 0.36, kw * 0.12, kh * 0.58));
            g.setColor(new Color(0xf1c27d));
            g.fill(new Ellipse2D.Double(-kw * 0.18, kh * 0.08, kw * 0.36, kw * 0.30));
        }
        g.setColor(new Color(0xf1c27d));
        g.fill(new Ellipse2D.Double(-kw * 0.36, -kh * 0.36 - kw * 0.74, kw * 0.72, kw * 0.74));
        g.setColor(new Color(0x4e342e));
        g.fill(new java.awt.geom.Arc2D.Double(-kw * 0.38, -kh * 0.36 - kw * 0.78, kw * 0.76, kw * 0.6, 0, 180,
                java.awt.geom.Arc2D.CHORD));
        if (arquero) {
            g.setStroke(new BasicStroke((float) (kw * 0.24), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int s = -1; s <= 1; s += 2) {
                double axx = s * (kw * 0.85 + (kw * 0.45 - kw * 0.85) * estira);
                double ayy = kh * 0.05 + (-kh * 0.95 - kh * 0.05) * estira;
                g.setColor(c);
                g.drawLine((int) (s * kw * 0.5), (int) (-kh * 0.28), (int) axx, (int) ayy);
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(axx - kw * 0.22, ayy - kw * 0.22, kw * 0.44, kw * 0.44));
            }
        }
        g.setTransform(old);
    }

    /** El pateador visto de espalda: camiseta con su número, short, medias y botines. */
    private void dibujarPateador(Graphics2D g, double tt) {
        double h = H * 0.50;
        double baseX = bolaX - W * 0.075, baseY = H * 0.985;
        double carrera = 0, golpe = 0;
        boolean anim = esc != null && (modo == Modo.ANIM || modo == Modo.FIN);
        if (anim) {
            double cm = carreraMs();
            carrera = cm <= 0 ? 1 : Math.min(1, tt / cm);
            golpe = tt >= cm ? Math.min(1, (tt - cm) / 220.0) : 0;
        } else if (rival && (modo == Modo.ATAJAR || modo == Modo.ESPERA)) {
            carrera = Math.min(1, transcurrido / CARRERA_RIVAL);      // el rival viene corriendo
        }
        // la pista: a medida que se acerca, el cuerpo y la carrera se inclinan hacia el lado que va a patear
        double inclina = rival ? pista * Math.min(1, carrera * 1.3) : 0;
        double x = baseX + carrera * W * 0.045 + inclina * W * 0.012;
        double y = baseY - carrera * H * 0.035;
        double hh = h * (1 - carrera * 0.05);
        double cuerpoY = y - hh;  
        double tw = hh * 0.30;

        g.setColor(new Color(0, 0, 0, 70));
        g.fill(new Ellipse2D.Double(x - tw * 1.1, y - hh * 0.02, tw * 2.2, hh * 0.05));

        double paso = (anim && tt < carreraMs()) ? Math.sin(tt / 45.0)
                : (rival && modo == Modo.ATAJAR ? Math.sin(transcurrido / 140.0) * (1 - carrera * 0.3) : 0);
        double xT = x + inclina * tw * 0.30;       // el torso se inclina un poco más que las piernas
        Color camiseta = rival ? new Color(0xe53935) : new Color(0x2f80ed);

        for (int s = -1; s <= 1; s += 2) {
            double mov = s * paso * hh * 0.05;
            double kick = (s == 1) ? golpe * hh * 0.18 : 0;     // la derecha le pega a la pelota
            double topY = y - hh * 0.50;
            double botY = y - kick * 0.4;
            double px = x + s * tw * 0.26 + (s == 1 ? golpe * tw * 0.5 : 0);
            g.setStroke(new BasicStroke((float) (hh * 0.092), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(0xf1c27d));
            g.draw(new java.awt.geom.Line2D.Double(px, topY, px + mov, botY - hh * 0.09));
            g.setColor(Color.WHITE);                                           // medias
            g.draw(new java.awt.geom.Line2D.Double(px + mov * 0.7, botY - hh * 0.24, px + mov, botY - hh * 0.04));
            g.setColor(new Color(0x111111));                                  // botín
            g.fill(new RoundRectangle2D.Double(px + mov - hh * 0.06, botY - hh * 0.045, hh * 0.12, hh * 0.05, 5, 5));
        }
  
        g.setColor(new Color(0xeeeeee));
        g.fill(new RoundRectangle2D.Double(x - tw * 0.62, y - hh * 0.64, tw * 1.24, hh * 0.17, 10, 10));

        Path2D torso = new Path2D.Double();
        torso.moveTo(xT - tw * 0.78, y - hh * 0.88);
        torso.lineTo(xT + tw * 0.78, y - hh * 0.88);
        torso.lineTo(xT + tw * 0.62, y - hh * 0.62);
        torso.lineTo(xT - tw * 0.62, y - hh * 0.62);
        torso.closePath();
        g.setColor(camiseta);
        g.fill(torso);
        g.setStroke(new BasicStroke((float) (hh * 0.078), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(camiseta);
        g.draw(new java.awt.geom.Line2D.Double(xT - tw * 0.80, y - hh * 0.86, xT - tw * 1.18 - paso * 4, y - hh * 0.66));
        g.draw(new java.awt.geom.Line2D.Double(xT + tw * 0.80, y - hh * 0.86, xT + tw * 1.18 + paso * 4, y - hh * 0.66));
        g.setColor(new Color(0xf1c27d));
        g.fill(new Ellipse2D.Double(xT - tw * 1.18 - paso * 4 - hh * 0.035, y - hh * 0.66 - hh * 0.03, hh * 0.07, hh * 0.07));
        g.fill(new Ellipse2D.Double(xT + tw * 1.18 + paso * 4 - hh * 0.035, y - hh * 0.66 - hh * 0.03, hh * 0.07, hh * 0.07));
        g.setColor(new Color(0xf1c27d));
        g.fill(new Ellipse2D.Double(xT - tw * 0.16, y - hh * 0.93, tw * 0.32, hh * 0.07));
        g.setColor(new Color(0x3e2723));
        g.fill(new Ellipse2D.Double(xT - hh * 0.07, cuerpoY + hh * 0.015, hh * 0.14, hh * 0.15));

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, (int) Math.max(12, hh * 0.15)));
        FontMetrics fm = g.getFontMetrics();
        String n = String.valueOf(numero);
        g.setColor(Color.WHITE);
        g.drawString(n, (int) (xT - fm.stringWidth(n) / 2.0), (int) (y - hh * 0.72));
    }

    private double[] bola(double tt) {
        double r0 = Math.max(10, H * 0.026);
        double r1 = r0 * 0.5;
        if (esc == null || modo == Modo.APUNTAR || modo == Modo.ESPERA || tt < carreraMs()) return new double[]{bolaX, bolaY, r0, 1};
        double u = Math.min(1, (tt - carreraMs()) / dur);
        double e = 1 - (1 - u) * (1 - u);
        double ex = destX, ey = destY;
        double lift = libre ? H * 0.07 : H * 0.02;
        double curva = libre ? (ex < cx ? 1 : -1) * W * 0.07 * esc.potencia : 0;
        double mx = (bolaX + ex) / 2 + curva, my = (bolaY + ey) / 2 - lift;
        // curva de Bézier cuadrática
        double x = (1 - e) * (1 - e) * bolaX + 2 * (1 - e) * e * mx + e * e * ex;
        double y = (1 - e) * (1 - e) * bolaY + 2 * (1 - e) * e * my + e * e * ey;
        double r = r0 + (r1 - r0) * e;
        double vis = 1;
        if (u >= 1) {
            double a = Math.min(1, (tt - tImpacto) / 450.0);
            a = 1 - (1 - a) * (1 - a);
            switch (esc.fin) {
                case GOL_FAVOR -> {                       // se mete en la red y cae
                    x = ex + (ex - cx) * 0.03 * a;
                    y = ey + gh * 0.12 * a;
                    r = r1 * (1 - 0.15 * a);
                }
                case AFUERA -> {                          // sigue de largo hacia la tribuna
                    double dx = (ex - bolaX), dy = (ey - bolaY);
                    x = ex + dx * 0.35 * a;
                    y = ey + dy * 0.35 * a;
                    r = r1 * (1 - 0.45 * a);
                    vis = 1 - a;
                }
                default -> {
                    if (hayRebote) {
                        x = ex + rebX * a;
                        y = ey + rebY * a - Math.sin(Math.PI * a) * H * 0.05;
                        r = r1 + (r0 * 0.95 - r1) * a;
                    }
                }
            }
        }
        return new double[]{x, y, r, vis};
    }

    private void dibujarBola(Graphics2D g, double[] b, double tt) {
        if (b[3] <= 0.02) return;
        double x = b[0], y = b[1], r = b[2];
        boolean quieta = esc == null || tt < carreraMs();
        if (quieta) {
            g.setColor(new Color(0, 0, 0, 80));
            g.fill(new Ellipse2D.Double(x - r, y + r * 0.55, r * 2, r * 0.7));
        } else {
            // estela
            for (int i = 1; i <= 5; i++) {
                double u = Math.max(0, (tt - carreraMs() - i * 18) / dur);
                if (u <= 0 || u > 1) {
                }
            }
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.SrcOver.derive((float) Math.max(0, Math.min(1, b[3]))));
        g2.setColor(Color.WHITE);
        g2.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
        g2.setColor(new Color(0x222222));
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
        for (int k = 0; k < 3; k++) {
            double a = giro + k * 2.094;
            g2.fill(new Ellipse2D.Double(x + Math.cos(a) * r * 0.5 - 2, y + Math.sin(a) * r * 0.5 - 2, 4, 4));
        }
        g2.dispose();
    }

    // ------------------------------------------------------------------ interfaz: mira, flecha y potencia

    private void dibujarMira(Graphics2D g, double swayT) {
        double mx = sx(ax), my = sy(ay);
        double pulso = 0.5 + 0.5 * Math.sin(swayT / 160.0);

        // flecha desde la pelota hasta la mira (como una guía de la trayectoria)
        double cxC = (bolaX + mx) / 2 + (libre ? (mx < cx ? 1 : -1) * W * 0.05 * Math.max(0.3, pot) : 0);
        double cyC = (bolaY + my) / 2 - (libre ? H * 0.05 : H * 0.01);
        QuadCurve2D curva = new QuadCurve2D.Double(bolaX, bolaY - 6, cxC, cyC, mx, my + 10);
        float grosor = (float) (3 + pot * 8);
        g.setStroke(new BasicStroke(grosor, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f,
                new float[]{14f, 10f}, (float) (swayT / 30.0 % 24)));
        g.setColor(new Color(230, 40, 40, 190));
        g.draw(curva);
        double ang = Math.atan2(my - cyC, mx - cxC);
        Path2D punta = new Path2D.Double();
        double L = 22 + pot * 10;
        punta.moveTo(mx, my + 10);
        punta.lineTo(mx - L * Math.cos(ang - 0.45), my + 10 - L * Math.sin(ang - 0.45));
        punta.lineTo(mx - L * Math.cos(ang + 0.45), my + 10 - L * Math.sin(ang + 0.45));
        punta.closePath();
        g.setStroke(new BasicStroke(1f));
        g.fill(punta);

        // mira: aro amarillo con centro rojo
        double R = 15 + pulso * 3;
        g.setStroke(new BasicStroke(3f));
        g.setColor(new Color(0xffd54f));
        g.draw(new Ellipse2D.Double(mx - R, my - R, R * 2, R * 2));
        g.setColor(new Color(0xe53935));
        g.fill(new Ellipse2D.Double(mx - 6, my - 6, 12, 12));
        g.setStroke(new BasicStroke(2f));
        g.setColor(new Color(255, 255, 255, 220));
        g.drawLine((int) (mx - R - 6), (int) my, (int) (mx - R + 4), (int) my);
        g.drawLine((int) (mx + R - 4), (int) my, (int) (mx + R + 6), (int) my);
        g.drawLine((int) mx, (int) (my - R - 6), (int) mx, (int) (my - R + 4));
        g.drawLine((int) mx, (int) (my + R - 4), (int) mx, (int) (my + R + 6));

        // textos
        int fs = (int) Math.max(18, H / 22);
        texto(g, libre ? "TIRO LIBRE: ELEGÍ DÓNDE PATEAR" : "PENAL: ELEGÍ DÓNDE PATEAR", W / 2, H * 0.045, fs, DORADO);
        texto(g, "← ↑ → ↓  mueven la mira        ESPACIO (mantené) carga la potencia · al soltarlo patea",
                W / 2, H * 0.045 + fs * 1.15, Math.max(12, fs / 2 + 1), new Color(0xcfd8dc));
        texto(g, "Patea: " + tirador + "      Arquero: " + arquero, W / 2, H * 0.975, Math.max(12, fs / 2), new Color(0xeceff1));
    }

    /** Interfaz de cuando el usuario ataja: objetivo de salto, barra de tiempo y consignas. */
    private void dibujarAtajar(Graphics2D g, double swayT) {
        double mx = sx(dx), my = sy(dy);
        Color cian = new Color(0x4dd0e1);
        double frac = Math.max(0, 1 - transcurrido / CARRERA_RIVAL);
        if (!saltoHecho && modo == Modo.ATAJAR) {
            double pulso = 0.5 + 0.5 * Math.sin(swayT / 160.0);
            double R = 18 + pulso * 3;
            g.setStroke(new BasicStroke(3f));
            g.setColor(cian);
            g.draw(new Ellipse2D.Double(mx - R, my - R, R * 2, R * 2));
            g.setColor(new Color(cian.getRed(), cian.getGreen(), cian.getBlue(), 90));
            g.fill(new Ellipse2D.Double(mx - 7, my - 7, 14, 14));
            g.setStroke(new BasicStroke(2f));
            g.drawLine((int) (mx - R - 7), (int) my, (int) (mx - R + 5), (int) my);
            g.drawLine((int) (mx + R - 5), (int) my, (int) (mx + R + 7), (int) my);
            g.drawLine((int) mx, (int) (my - R - 7), (int) mx, (int) (my - R + 5));
            g.drawLine((int) mx, (int) (my + R - 5), (int) mx, (int) (my + R + 7));
        }
        int fs = (int) Math.max(18, H / 22);
        texto(g, saltoHecho ? "¡TE TIRASTE!" : "PENAL EN CONTRA: ¡ATAJÁ!", W / 2, H * 0.045, fs,
                saltoHecho ? new Color(0x69f0ae) : cian);
        texto(g, "← ↑ → ↓ elegí hacia dónde te tirás        ESPACIO = tirarte (esperá a leer al pateador, pero no tanto)",
                W / 2, H * 0.045 + fs * 1.15, Math.max(12, fs / 2 + 1), new Color(0xcfd8dc));
        texto(g, "Patea: " + tirador + "      Tu arquero: " + arquero, W / 2, H * 0.975, Math.max(12, fs / 2),
                new Color(0xeceff1));

        // barra de tiempo vertical (a la derecha, como la de potencia): cuando se vacía, el rival patea
        double bw = Math.max(20, W * 0.028), bh = H * 0.52;
        double bx = W - W * 0.075 - bw, by = H * 0.27;
        g.setColor(new Color(0, 0, 0, 150));
        g.fill(new RoundRectangle2D.Double(bx - 8, by - 8, bw + 16, bh + 16, 14, 14));
        Color col = frac > 0.5 ? new Color(0x66bb6a) : (frac > 0.22 ? new Color(0xffca28) : new Color(0xef5350));
        g.setColor(col);
        g.fill(new RoundRectangle2D.Double(bx, by + bh * (1 - frac), bw, bh * frac, 8, 8));
        // zona de arriba = "muy pronto": si te tirás ahí, el pateador te lee y cambia de lado
        g.setColor(new Color(229, 57, 53, 70));
        g.fill(new java.awt.geom.Rectangle2D.Double(bx, by, bw, bh * 0.40));
        g.setStroke(new BasicStroke(2f));
        g.setColor(new Color(255, 255, 255, 220));
        g.draw(new RoundRectangle2D.Double(bx, by, bw, bh, 8, 8));
        g.drawLine((int) (bx - 10), (int) (by + bh * 0.40), (int) (bx + bw + 10), (int) (by + bh * 0.40));
        texto(g, "TIEMPO", bx + bw / 2, by - 22, Math.max(11, (int) (H / 42)), Color.WHITE);
        texto(g, "¡TE LEEN!", bx + bw / 2, by + bh * 0.20, Math.max(9, (int) (H / 56)), new Color(0xffcdd2));
    }

    private void dibujarPotencia(Graphics2D g) {
        double bw = Math.max(20, W * 0.028), bh = H * 0.52;
        double x = W - W * 0.075 - bw, y = H * 0.27;
        // marco y degradé verde -> amarillo -> rojo
        g.setColor(new Color(0, 0, 0, 150));
        g.fill(new RoundRectangle2D.Double(x - 8, y - 8, bw + 16, bh + 16, 14, 14));
        g.setPaint(new GradientPaint(0, (float) (y + bh), new Color(0x43a047), 0, (float) y, new Color(0xe53935)));
        g.fill(new RoundRectangle2D.Double(x, y, bw, bh, 8, 8));
        // lo que todavía no se cargó se oscurece
        g.setColor(new Color(10, 10, 10, 200));
        double vacio = bh * (1 - pot);
        g.fill(new RoundRectangle2D.Double(x, y, bw, vacio, 8, 8));
        // zona óptima
        double yMax = y + bh * (1 - SWEET_MAX), yMin = y + bh * (1 - SWEET_MIN);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2.5f));
        g.drawLine((int) (x - 12), (int) yMax, (int) (x + bw + 12), (int) yMax);
        g.drawLine((int) (x - 12), (int) yMin, (int) (x + bw + 12), (int) yMin);
        // indicador
        double yi = y + bh * (1 - pot);
        Path2D flecha = new Path2D.Double();
        flecha.moveTo(x - 4, yi);
        flecha.lineTo(x - 18, yi - 9);
        flecha.lineTo(x - 18, yi + 9);
        flecha.closePath();
        g.setColor(DORADO);
        g.fill(flecha);
        g.setStroke(new BasicStroke(2f));
        g.setColor(new Color(255, 255, 255, 230));
        g.draw(new RoundRectangle2D.Double(x, y, bw, bh, 8, 8));
        texto(g, "POTENCIA", x + bw / 2, y - 22, Math.max(11, (int) (H / 42)), Color.WHITE);
        boolean enZona = pot >= SWEET_MIN && pot <= SWEET_MAX;
        texto(g, (int) Math.round(pot * 100) + "%", x + bw / 2, y + bh + 24, Math.max(13, (int) (H / 34)),
                enZona ? new Color(0x69f0ae) : (pot > SWEET_MAX ? new Color(0xff8a80) : Color.WHITE));
        if (cargando && pot > 0.93) texto(g, "¡DEMASIADA!", x + bw / 2, y + bh + 46, Math.max(11, (int) (H / 44)), new Color(0xff8a80));
    }

    private void texto(Graphics2D g, String t, double cx0, double cy0, int size, Color c) {
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, size));
        FontMetrics fm = g.getFontMetrics();
        int x = (int) (cx0 - fm.stringWidth(t) / 2.0);
        int y = (int) (cy0 + fm.getAscent() / 2.0 - fm.getDescent() / 2.0);
        g.setColor(new Color(0, 0, 0, 180));
        g.drawString(t, x + 2, y + 2);
        g.setColor(c);
        g.drawString(t, x, y);
    }

    private void dibujarConfeti(Graphics2D g) {
        for (double[] p : confeti) {
            float a = (float) Math.min(1.0, p[7] / 400.0);
            Color c = CONFETI[(int) p[6]];
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (a * 255)));
            AffineTransform old = g.getTransform();
            g.translate(p[0], p[1]);
            g.rotate(p[4]);
            g.fillRect(-4, -2, 8, 4);
            g.setTransform(old);
        }
    }

    private static double rebote(double t) {
        double c1 = 1.70158, c3 = c1 + 1;
        return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
    }

    private void dibujarBanner(Graphics2D g0) {
        if (esc == null || modo == Modo.APUNTAR || modo == Modo.ESPERA || ms < tBanner || esc.banner == null
                || esc.banner.isEmpty()) {
            return;
        }
        double u = ms - tBanner;
        double e = rebote(Math.min(1.0, u / 350.0));
        float alpha = (float) Math.max(0.0, Math.min(1.0, (bannerDur - u) / 300.0));
        Color c = switch (esc.fin) {
            case GOL_FAVOR -> DORADO;
            case GOL_CONTRA -> new Color(0xff5252);
            case ATAJADA -> new Color(0x4dd0e1);
            case PALO -> new Color(0xffab40);
            case PERDIDA -> new Color(0xff8a65);
            default -> new Color(0xb0bec5);
        };
        Graphics2D g = (Graphics2D) g0.create();
        g.setComposite(AlphaComposite.SrcOver.derive(alpha));
        g.translate(W / 2, H * 0.50);
        g.scale(Math.max(0.01, e), Math.max(0.01, e));
        int bh = (int) (H * 0.22);
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect((int) (-W * 0.4), -bh / 2, (int) (W * 0.8), bh);
        int size = (int) Math.max(28, H / 7);
        FontMetrics fm;
        do {
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, size));
            fm = g.getFontMetrics();
            size -= 2;
        } while (fm.stringWidth(esc.banner) > W * 0.74 && size > 12);
        int tx0 = -fm.stringWidth(esc.banner) / 2;
        int ty0 = (fm.getAscent() - fm.getDescent()) / 2;
        g.setColor(Color.BLACK);
        g.drawString(esc.banner, tx0 + 3, ty0 + 3);
        g.setColor(c);
        g.drawString(esc.banner, tx0, ty0);
        g.dispose();
    }
}
