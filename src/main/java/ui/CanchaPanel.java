package ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JPanel;
import javax.swing.Timer;

import arcade.Escena;
import arcade.Formacion;

/**
 * Cancha dibujada con Java2D. Sirve para:
 *  - armar el 11 (formación elegida, casillero resaltado),
 *  - mostrar el partido "en vivo" (los 22 jugadores se mueven y se pasan la pelota),
 *  - reproducir las escenas de cada situación (pases, remates, arquero, carteles, confeti).
 */
public class CanchaPanel extends JPanel {

    private enum Fase { QUIETO, VIVO, PAUSA, MOVER, BANNER, FIN }

    private static final double SALTO_ARQUERO = 0.09;
    private static final Color[] CONFETI = {
            new Color(0xffd54f), new Color(0xff5252), new Color(0x56ccf2), new Color(0x6fcf97), Color.WHITE};

    private Formacion fProp = Formacion.TODAS.get(0);
    private Formacion fRiv = Formacion.TODAS.get(0);
    private final Vivo vivo = new Vivo();
    private boolean modoVivo;

    private final String[] nombres = new String[11];
    private boolean mostrarRival = false;
    private int resaltado = -1;

    private Escena escena;
    private Fase fase = Fase.QUIETO;
    private final Timer timer;
    private final Timer pulso;          // hace latir el aro del casillero resaltado
    private final Random rnd = new Random();
    private long ini, ultimoTick;
    private int tramo;
    private double sx, sy, bx = 0.5, by = 0.5;
    private boolean verBola;
    private String etiqueta = "";
    private double kProg;
    private int remateIdx = -1;
    private Runnable alTerminar;
    private String bannerTxt = "";
    private Color bannerColor = Color.WHITE;

    // efectos
    private final List<double[]> estela = new ArrayList<>();    // x, y, altura
    private final List<double[]> confeti = new ArrayList<>();   // px, py, vx, vy, rot, vrot, color, vida
    private double alt, giro, shake, flash, bannerMs, bannerDur = 1500;

    // geometría de la cancha
    private int fx, fy, fw, fh;

    public CanchaPanel() {
        setPreferredSize(new Dimension(700, 430));
        setBackground(new Color(0x15181c));
        vivo.setFormaciones(fProp.propia(), fRiv.rival());
        timer = new Timer(16, e -> tick());
        pulso = new Timer(40, e -> {
            if (!timer.isRunning()) repaint();
        });
    }

    // ------------------------------------------------------------------ API: armado

    /** Formación propia y del rival (el rival puede ser null si todavía no se muestra). */
    public void setFormaciones(Formacion propia, Formacion rival) {
        fProp = propia;
        fRiv = rival != null ? rival : propia;
        vivo.setFormaciones(fProp.propia(), fRiv.rival());
        repaint();
    }

    public void setNombre(int slot, String nombre) {
        nombres[slot] = nombre;
        repaint();
    }

    public void limpiarNombres() {
        for (int i = 0; i < nombres.length; i++) nombres[i] = null;
        repaint();
    }

    public void setMostrarRival(boolean b) {
        mostrarRival = b;
        repaint();
    }

    public void setResaltado(int slot) {
        resaltado = slot;
        if (slot >= 0) pulso.start(); else pulso.stop();
        repaint();
    }

    // ------------------------------------------------------------------ API: partido en vivo

    /** Arranca la simulación visual: los jugadores se mueven y se pasan la pelota. */
    public void vivoIniciar() {
        detener();
        escena = null;
        modoVivo = true;
        vivo.reiniciar();
        vivo.corriendo = true;
        bx = vivo.bx;
        by = vivo.by;
        verBola = true;
        bannerTxt = "";
        fase = Fase.VIVO;
        ultimoTick = System.nanoTime();
        timer.start();
    }

    /** Congela el juego (hay que decidir algo). */
    public void vivoPausar() {
        vivo.corriendo = false;
    }

    public void vivoReanudar() {
        if (!modoVivo) return;
        vivo.corriendo = true;
        vivo.enFormacion = false;
        if (fase != Fase.VIVO) return;
        verBola = true;
    }

    public void vivoDetener() {
        modoVivo = false;
        vivo.corriendo = false;
        detener();
        verBola = false;
        repaint();
    }

    /** Cómo juega el equipo: probabilidad de conservar la pelota y tendencia al juego directo. */
    public void setEstiloVivo(double posesion, double directo) {
        vivo.posesion = posesion;
        vivo.directo = directo;
    }

    /** Porcentaje de tiempo con la pelota que tuvo el usuario (0 a 1). */
    public double getPosesionUsuario() {
        return vivo.posesionMedida();
    }

    // ------------------------------------------------------------------ API: escenas

    /** Cartel a pantalla de cancha (ej. "SEMIFINAL") sin pelota; al terminar corre `fin`. */
    public void cartel(String texto, Runnable fin) {
        reproducir(new Escena(0.5, 0.5).fin(Escena.Fin.OK, texto), fin);
        verBola = false;
    }

    public void reproducir(Escena e, Runnable fin) {
        detener();
        escena = e;
        alTerminar = fin;
        tramo = 0;
        sx = bx = e.x0;
        sy = by = e.y0;
        verBola = true;
        etiqueta = "";
        kProg = 0;
        bannerTxt = "";
        remateIdx = -1;
        estela.clear();
        confeti.clear();
        alt = 0;
        shake = 0;
        flash = 0;
        vivo.enFormacion = true;        // los jugadores vuelven a su lugar y se acercan a la jugada
        vivo.corriendo = false;
        for (int i = 0; i < e.tramos.size(); i++) {
            if (e.tramos.get(i).remate) {
                remateIdx = i;
                break;
            }
        }
        fase = Fase.PAUSA;
        ini = System.nanoTime();
        ultimoTick = ini;
        timer.start();
        repaint();
    }

    public void limpiar() {
        modoVivo = false;
        vivo.corriendo = false;
        vivo.enFormacion = false;
        detener();
        escena = null;
        verBola = false;
        etiqueta = "";
        bannerTxt = "";
        kProg = 0;
        estela.clear();
        confeti.clear();
        alt = 0;
        repaint();
    }

    // ------------------------------------------------------------------ animación

    private void detener() {
        timer.stop();
        fase = Fase.QUIETO;
        alTerminar = null;
    }

    /** Un paso de la animación (package-private para poder probarla sin ventana). */
    void tick() {
        long now = System.nanoTime();
        double ms = (now - ini) / 1_000_000.0;
        double dt = Math.min(50, (now - ultimoTick) / 1_000_000.0);
        ultimoTick = now;

        shake *= 0.90;
        flash = Math.max(0, flash - dt / 450.0);
        actualizarConfeti(dt);

        boolean enEscena = fase == Fase.PAUSA || fase == Fase.MOVER || fase == Fase.BANNER;
        if (enEscena) {
            vivo.bx = bx;
            vivo.by = by;
        }
        vivo.actualizar(dt);
        if (fase == Fase.VIVO) {
            bx = vivo.bx;
            by = vivo.by;
            alt = vivo.alt;
            if (vivo.enVuelo()) {
                giro += 0.45;
                estela.add(new double[]{bx, by, alt});
                if (estela.size() > 16) estela.remove(0);
            } else if (!estela.isEmpty()) {
                estela.remove(0);
            }
        } else if (fase != Fase.MOVER && !estela.isEmpty()) {
            estela.remove(0);
        }

        switch (fase) {
            case PAUSA -> {
                if (ms >= 400) {
                    ini = now;
                    if (escena.tramos.isEmpty()) {
                        iniciarBanner();
                    } else {
                        fase = Fase.MOVER;
                    }
                }
            }
            case MOVER -> {
                Escena.Tramo t = escena.tramos.get(tramo);
                double k = Math.min(1.0, ms / t.ms);
                double s = k * k * (3 - 2 * k);
                bx = sx + (t.x - sx) * s;
                by = sy + (t.y - sy) * s;
                double dist = Math.hypot((t.x - sx) * 1.7, t.y - sy);
                alt = Math.sin(Math.PI * k) * (t.remate ? 14 : Math.min(34, dist * 55));
                giro += 0.45;
                estela.add(new double[]{bx, by, alt});
                if (estela.size() > 16) estela.remove(0);
                etiqueta = t.texto;
                if (escena.hayArquero) {
                    if (tramo < remateIdx) kProg = 0;
                    else if (tramo == remateIdx) kProg = s;
                    else kProg = 1;
                }
                if (k >= 1.0) {
                    sx = t.x;
                    sy = t.y;
                    alt = 0;
                    tramo++;
                    ini = now;
                    if (tramo >= escena.tramos.size()) iniciarBanner();
                }
            }
            case BANNER -> {
                bannerMs = ms;
                if (ms >= bannerDur) terminarEscena();
            }
            case VIVO -> { }
            default -> timer.stop();
        }
        repaint();
    }

    private void terminarEscena() {
        Runnable r = alTerminar;
        alTerminar = null;
        Escena.Fin fin = escena.fin;
        bannerTxt = "";
        kProg = 0;
        if (modoVivo) {
            vivo.bx = bx;
            vivo.by = by;
            vivo.reanudarDespuesDe(fin);   // la pelota queda donde corresponde, pero el juego sigue congelado
            vivo.corriendo = false;
            bx = vivo.bx;
            by = vivo.by;
            verBola = true;
            escena = null;
            fase = Fase.VIVO;
        } else {
            fase = Fase.FIN;
            timer.stop();
        }
        if (r != null) r.run();
    }

    private void iniciarBanner() {
        fase = Fase.BANNER;
        ini = System.nanoTime();
        bannerMs = 0;
        bannerDur = escena.fin == Escena.Fin.GOL_FAVOR ? 2300 : 1500;
        etiqueta = "";
        bannerTxt = escena.banner;
        switch (escena.fin) {
            case GOL_FAVOR -> {
                bannerColor = new Color(0xffd54f);
                flash = 1;
                lanzarConfeti();
            }
            case GOL_CONTRA -> {
                bannerColor = new Color(0xff5252);
                shake = 1;
            }
            case ATAJADA -> bannerColor = new Color(0x4dd0e1);
            case PALO -> {
                bannerColor = new Color(0xffab40);
                shake = 0.7;
            }
            case PERDIDA -> bannerColor = new Color(0xff8a65);
            case RECUPERACION -> bannerColor = new Color(0x81c784);
            case AFUERA -> bannerColor = new Color(0xb0bec5);
            default -> bannerColor = new Color(0xe0e0e0);
        }
    }

    private void lanzarConfeti() {
        for (int i = 0; i < 120; i++) {
            double px, py, vx, vy;
            if (i % 2 == 0) {                       // explosión desde el arco rival
                double a = Math.PI + (rnd.nextDouble() - 0.2) * 1.6 - 0.8;
                double v = 0.25 + rnd.nextDouble() * 0.55;
                px = X(0.97);
                py = Y(0.5);
                vx = Math.cos(a) * v;
                vy = -Math.abs(Math.sin(a)) * v - 0.1;
            } else {                                // lluvia desde arriba
                px = fx + rnd.nextDouble() * fw;
                py = fy - 10;
                vx = (rnd.nextDouble() - 0.5) * 0.15;
                vy = 0.05 + rnd.nextDouble() * 0.2;
            }
            confeti.add(new double[]{px, py, vx, vy, rnd.nextDouble() * 6, (rnd.nextDouble() - 0.5) * 0.02,
                    rnd.nextInt(CONFETI.length), 1700});
        }
    }

    private void actualizarConfeti(double dt) {
        for (int i = confeti.size() - 1; i >= 0; i--) {
            double[] p = confeti.get(i);
            p[0] += p[2] * dt;
            p[1] += p[3] * dt;
            p[3] += 0.0012 * dt;
            p[2] *= 0.995;
            p[4] += p[5] * dt;
            p[7] -= dt;
            if (p[7] <= 0) confeti.remove(i);
        }
    }

    // ------------------------------------------------------------------ dibujo

    private int X(double x) { return fx + (int) Math.round(x * fw); }
    private int Y(double y) { return fy + (int) Math.round(y * fh); }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int m = 34;
        int availW = getWidth() - 2 * m;
        int availH = getHeight() - 2 * m;
        if (availW < 100 || availH < 60) {
            g.dispose();
            return;
        }
        double ratio = 1.7;
        if ((double) availW / availH > ratio) {
            fh = availH;
            fw = (int) (availH * ratio);
        } else {
            fw = availW;
            fh = (int) (availW / ratio);
        }
        fx = (getWidth() - fw) / 2;
        fy = (getHeight() - fh) / 2;

        if (shake > 0.02) {
            g.translate((rnd.nextDouble() - 0.5) * 2 * shake * 7, (rnd.nextDouble() - 0.5) * 2 * shake * 7);
        }
        dibujarCampo(g);
        dibujarJugadores(g);
        dibujarPelota(g);
        if (flash > 0.01) {
            g.setColor(new Color(255, 255, 255, (int) (flash * 90)));
            g.fillRect(fx, fy, fw, fh);
        }
        dibujarConfeti(g);
        dibujarBanner(g);
        g.dispose();
    }

    private void dibujarCampo(Graphics2D g) {
        int franjas = 12;
        for (int i = 0; i < franjas; i++) {
            g.setColor(i % 2 == 0 ? new Color(0x2e7d32) : new Color(0x338a37));
            int x1 = fx + fw * i / franjas;
            int x2 = fx + fw * (i + 1) / franjas;
            g.fillRect(x1, fy, x2 - x1, fh);
        }
        // arcos
        int gw = Math.max(10, (int) (fw * 0.022));
        int gy1 = Y(0.44), gy2 = Y(0.56);
        g.setColor(new Color(255, 255, 255, 60));
        g.fillRect(fx - gw, gy1, gw, gy2 - gy1);
        g.fillRect(fx + fw, gy1, gw, gy2 - gy1);
        g.setColor(new Color(255, 255, 255, 140));
        g.setStroke(new BasicStroke(1.2f));
        g.drawRect(fx - gw, gy1, gw, gy2 - gy1);
        g.drawRect(fx + fw, gy1, gw, gy2 - gy1);

        // líneas
        g.setColor(new Color(255, 255, 255, 220));
        g.setStroke(new BasicStroke(2f));
        g.drawRect(fx, fy, fw, fh);
        g.drawLine(X(0.5), fy, X(0.5), fy + fh);
        int rc = (int) (fh * 0.14);
        g.drawOval(X(0.5) - rc, Y(0.5) - rc, rc * 2, rc * 2);
        g.fillOval(X(0.5) - 3, Y(0.5) - 3, 6, 6);
        g.drawRect(fx, Y(0.22), X(0.16) - fx, Y(0.78) - Y(0.22));
        g.drawRect(X(0.84), Y(0.22), fx + fw - X(0.84), Y(0.78) - Y(0.22));
        g.drawRect(fx, Y(0.37), X(0.055) - fx, Y(0.63) - Y(0.37));
        g.drawRect(X(0.945), Y(0.37), fx + fw - X(0.945), Y(0.63) - Y(0.37));
        g.fillOval(X(0.11) - 3, Y(0.5) - 3, 6, 6);
        g.fillOval(X(0.89) - 3, Y(0.5) - 3, 6, 6);
    }

    private void dibujarJugadores(Graphics2D g) {
        int r = 14;
        double latido = 0.5 + 0.5 * Math.sin(System.nanoTime() / 1.2e8);
        Font fCod = new Font(Font.SANS_SERIF, Font.BOLD, 10);
        Font fNom = new Font(Font.SANS_SERIF, Font.BOLD, 12);

        // quién tiene la pelota: en vivo lo sabe la simulación; en una escena, el propio más cercano
        int poseedorEq = -1, poseedor = -1;
        if (verBola && fase == Fase.VIVO && vivo.corriendo) {
            poseedorEq = vivo.eq;
            poseedor = vivo.idx;
        } else if (verBola && fase == Fase.MOVER) {
            double mejor = 0.14;
            for (int i = 1; i < 11; i++) {
                double d = Math.hypot((bx - vivo.pos[0][i][0]) * 1.7, by - vivo.pos[0][i][1]);
                if (d < mejor) {
                    mejor = d;
                    poseedorEq = 0;
                    poseedor = i;
                }
            }
        }

        // rival
        if (mostrarRival) {
            for (int i = 0; i < 11; i++) {
                double x = vivo.pos[1][i][0];
                double y = vivo.pos[1][i][1];
                if (i == 0 && escena != null && escena.hayArquero && !escena.arqueroPropio) {
                    y += escena.arqueroLado * SALTO_ARQUERO * kProg;
                }
                int cx = X(x), cy = Y(y);
                if (poseedorEq == 1 && poseedor == i) {
                    g.setColor(new Color(255, 120, 120, 90 + (int) (latido * 120)));
                    g.setStroke(new BasicStroke(2.5f));
                    g.draw(new Ellipse2D.Double(cx - r - 6, cy - r - 6, 2 * r + 12, 2 * r + 12));
                }
                g.setColor(new Color(0, 0, 0, 70));
                g.fill(new Ellipse2D.Double(cx - r + 2, cy - r + 3, 2 * r, 2 * r));
                g.setColor(i == 0 ? new Color(0xf2994a) : new Color(0xeb5757));
                g.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(1.5f));
                g.draw(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
                g.setFont(fCod);
                FontMetrics fm = g.getFontMetrics();
                String pos = fRiv.casillero(i);
                g.drawString(pos, cx - fm.stringWidth(pos) / 2, cy + fm.getAscent() / 2 - 1);
            }
        }

        // propios
        for (int i = 0; i < 11; i++) {
            double x = vivo.pos[0][i][0];
            double y = vivo.pos[0][i][1];
            if (i == 0 && escena != null && escena.hayArquero && escena.arqueroPropio) {
                y += escena.arqueroLado * SALTO_ARQUERO * kProg;
            }
            int cx = X(x), cy = Y(y);
            boolean lleno = nombres[i] != null;

            if (i == resaltado) {
                g.setColor(new Color(0xffd54f));
                g.setStroke(new BasicStroke(3f));
                int rr = r + 5 + (int) Math.round(latido * 4);
                g.draw(new Ellipse2D.Double(cx - rr, cy - rr, 2 * rr, 2 * rr));
            }
            if (poseedorEq == 0 && i == poseedor) {
                g.setColor(new Color(255, 255, 255, 70 + (int) (latido * 110)));
                g.setStroke(new BasicStroke(2.5f));
                int rr = r + 6;
                g.draw(new Ellipse2D.Double(cx - rr, cy - rr, 2 * rr, 2 * rr));
            }
            g.setColor(new Color(0, 0, 0, 70));
            g.fill(new Ellipse2D.Double(cx - r + 2, cy - r + 3, 2 * r, 2 * r));
            if (lleno) {
                g.setColor(i == 0 ? new Color(0xf2c94c) : new Color(0x2f80ed));
            } else {
                g.setColor(new Color(255, 255, 255, 70));
            }
            g.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(1.5f));
            g.draw(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));

            g.setFont(fCod);
            FontMetrics fm = g.getFontMetrics();
            String pos = fProp.casillero(i);
            g.setColor(lleno && i == 0 ? new Color(0x222222) : Color.WHITE);
            g.drawString(pos, cx - fm.stringWidth(pos) / 2, cy + fm.getAscent() / 2 - 1);

            if (lleno) {
                g.setFont(fNom);
                fm = g.getFontMetrics();
                String n = nombres[i];
                int tx = cx - fm.stringWidth(n) / 2;
                int ty = cy + r + 14;
                g.setColor(new Color(0, 0, 0, 170));
                g.drawString(n, tx + 1, ty + 1);
                g.setColor(Color.WHITE);
                g.drawString(n, tx, ty);
            }
        }
    }

    private void dibujarPelota(Graphics2D g) {
        if (!verBola) return;
        int px = X(bx), py = Y(by);

        // estela
        int n = estela.size();
        for (int i = 0; i < n; i++) {
            double[] e = estela.get(i);
            double f = (i + 1.0) / n;
            int rr = (int) (2 + f * 5);
            g.setColor(new Color(255, 255, 255, (int) (f * 90)));
            g.fillOval(X(e[0]) - rr, Y(e[1]) - (int) e[2] - rr, rr * 2, rr * 2);
        }

        int sh = (int) Math.max(4, 14 - alt * 0.15);
        g.setColor(new Color(0, 0, 0, (int) Math.max(30, 90 - alt * 1.2)));
        g.fillOval(px - sh / 2 + 1, py - sh / 4 + 4, sh, Math.max(4, sh / 2));

        int pyb = py - (int) Math.round(alt);
        double esc = 1 + alt / 90.0;
        int rad = (int) Math.round(7 * esc);
        g.setColor(Color.WHITE);
        g.fillOval(px - rad, pyb - rad, rad * 2, rad * 2);
        g.setColor(new Color(0x222222));
        g.setStroke(new BasicStroke(1.5f));
        g.drawOval(px - rad, pyb - rad, rad * 2, rad * 2);
        for (int k = 0; k < 3; k++) {
            double a = giro + k * 2.094;
            int ox = (int) Math.round(Math.cos(a) * rad * 0.5);
            int oy = (int) Math.round(Math.sin(a) * rad * 0.5);
            g.fillOval(px + ox - 2, pyb + oy - 2, 4, 4);
        }

        if (etiqueta != null && !etiqueta.isEmpty()) {
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(etiqueta);
            int bw = tw + 16, bh = fm.getHeight() + 6;
            int bxx = Math.max(4, Math.min(getWidth() - bw - 4, px - bw / 2));
            int byy = Math.max(4, pyb - 22 - bh);
            g.setColor(new Color(0, 0, 0, 170));
            g.fill(new RoundRectangle2D.Double(bxx, byy, bw, bh, 12, 12));
            g.setColor(Color.WHITE);
            g.drawString(etiqueta, bxx + 8, byy + 3 + fm.getAscent());
        }
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
        if (bannerTxt == null || bannerTxt.isEmpty()) return;
        double e = rebote(Math.min(1.0, bannerMs / 350.0));
        float alpha = (float) Math.max(0.0, Math.min(1.0, (bannerDur - bannerMs) / 300.0));

        Graphics2D g = (Graphics2D) g0.create();
        g.setComposite(AlphaComposite.SrcOver.derive(alpha));
        g.translate(fx + fw / 2.0, fy + fh / 2.0);
        g.scale(Math.max(0.01, e), Math.max(0.01, e));

        int bh = (int) (fh * 0.30);
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(-fw / 2, -bh / 2, fw, bh);

        int size = Math.max(28, fh / 7);
        FontMetrics fm;
        do {   
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, size));
            fm = g.getFontMetrics();
            size -= 2;
        } while (fm.stringWidth(bannerTxt) > fw * 0.92 && size > 12);
        int tx = -fm.stringWidth(bannerTxt) / 2;
        int ty = (fm.getAscent() - fm.getDescent()) / 2;
        g.setColor(Color.BLACK);
        g.drawString(bannerTxt, tx + 3, ty + 3);
        g.setColor(bannerColor);
        g.drawString(bannerTxt, tx, ty);
        g.dispose();
    }
}
