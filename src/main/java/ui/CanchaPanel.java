package ui;

import arcade.Escena;
import arcade.Formacion;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/** Cancha dibujada con Java2D. Sirve para armar el 11 y para reproducir las cinemáticas del partido. */
public class CanchaPanel extends JPanel {

    private enum Fase { QUIETO, PAUSA, MOVER, BANNER, FIN }

    private static final double SALTO_ARQUERO = 0.09;

    private final String[] nombres = new String[11];
    private boolean mostrarRival = false;
    private int resaltado = -1;

    private Escena escena;
    private Fase fase = Fase.QUIETO;
    private final Timer timer;
    private long ini;
    private int tramo;
    private double sx, sy, bx, by;
    private boolean verBola;
    private String etiqueta = "";
    private double kProg;
    private int remateIdx = -1;
    private Runnable alTerminar;
    private String bannerTxt = "";
    private Color bannerColor = Color.WHITE;

    // geometría de la cancha (se recalcula en cada paint)
    private int fx, fy, fw, fh;

    public CanchaPanel() {
        setPreferredSize(new Dimension(700, 430));
        setBackground(new Color(0x15181c));
        timer = new Timer(16, e -> tick());
    }

    // ------------------------------------------------------------------ API

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
        repaint();
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
        for (int i = 0; i < e.tramos.size(); i++) {
            if (e.tramos.get(i).remate) {
                remateIdx = i;
                break;
            }
        }
        fase = Fase.PAUSA;
        ini = System.nanoTime();
        timer.start();
        repaint();
    }

    public void limpiar() {
        detener();
        escena = null;
        verBola = false;
        etiqueta = "";
        bannerTxt = "";
        kProg = 0;
        repaint();
    }

    // ------------------------------------------------------------------ animación

    private void detener() {
        timer.stop();
        fase = Fase.QUIETO;
        alTerminar = null;
    }

    /** Un paso de la animación (público para poder probarla sin ventana). */
    void tick() {
        long now = System.nanoTime();
        double ms = (now - ini) / 1_000_000.0;
        switch (fase) {
            case PAUSA:
                if (ms >= 400) {
                    ini = now;
                    if (escena.tramos.isEmpty()) {
                        iniciarBanner();
                    } else {
                        fase = Fase.MOVER;
                    }
                }
                break;
            case MOVER:
                Escena.Tramo t = escena.tramos.get(tramo);
                double k = Math.min(1.0, ms / t.ms);
                double s = k * k * (3 - 2 * k);
                bx = sx + (t.x - sx) * s;
                by = sy + (t.y - sy) * s;
                etiqueta = t.texto;
                if (escena.hayArquero) {
                    if (tramo < remateIdx) kProg = 0;
                    else if (tramo == remateIdx) kProg = s;
                    else kProg = 1;
                }
                if (k >= 1.0) {
                    sx = t.x;
                    sy = t.y;
                    tramo++;
                    ini = now;
                    if (tramo >= escena.tramos.size()) iniciarBanner();
                }
                break;
            case BANNER:
                if (ms >= 1500) {
                    fase = Fase.FIN;
                    timer.stop();
                    Runnable r = alTerminar;
                    alTerminar = null;
                    if (r != null) r.run();
                }
                break;
            default:
                timer.stop();
        }
        repaint();
    }

    private void iniciarBanner() {
        fase = Fase.BANNER;
        ini = System.nanoTime();
        etiqueta = "";
        bannerTxt = escena.banner;
        switch (escena.fin) {
            case GOL_FAVOR: bannerColor = new Color(0xffd54f); break;
            case GOL_CONTRA: bannerColor = new Color(0xff5252); break;
            case ATAJADA: bannerColor = new Color(0x4dd0e1); break;
            case PALO: bannerColor = new Color(0xffab40); break;
            case PERDIDA: bannerColor = new Color(0xff8a65); break;
            case RECUPERACION: bannerColor = new Color(0x81c784); break;
            case AFUERA: bannerColor = new Color(0xb0bec5); break;
            default: bannerColor = new Color(0xe0e0e0);
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

        dibujarCampo(g);
        dibujarJugadores(g);
        dibujarPelota(g);
        dibujarBanner(g);
        g.dispose();
    }

    private void dibujarCampo(Graphics2D g) {
        // pasto con franjas
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
        // rival
        if (mostrarRival) {
            for (int i = 0; i < 11; i++) {
                double x = Formacion.RIVAL[i][0];
                double y = Formacion.RIVAL[i][1];
                if (i == 0 && escena != null && escena.hayArquero && !escena.arqueroPropio) {
                    y += escena.arqueroLado * SALTO_ARQUERO * kProg;
                }
                g.setColor(new Color(0, 0, 0, 70));
                g.fill(new Ellipse2D.Double(X(x) - r + 2, Y(y) - r + 3, 2 * r, 2 * r));
                g.setColor(i == 0 ? new Color(0xf2994a) : new Color(0xeb5757));
                g.fill(new Ellipse2D.Double(X(x) - r, Y(y) - r, 2 * r, 2 * r));
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(1.5f));
                g.draw(new Ellipse2D.Double(X(x) - r, Y(y) - r, 2 * r, 2 * r));
            }
        }
        // propios
        Font fDot = new Font(Font.SANS_SERIF, Font.BOLD, 10);
        Font fNom = new Font(Font.SANS_SERIF, Font.BOLD, 12);
        for (int i = 0; i < 11; i++) {
            double x = Formacion.PROPIA[i][0];
            double y = Formacion.PROPIA[i][1];
            if (i == 0 && escena != null && escena.hayArquero && escena.arqueroPropio) {
                y += escena.arqueroLado * SALTO_ARQUERO * kProg;
            }
            int cx = X(x), cy = Y(y);
            boolean lleno = nombres[i] != null;

            if (i == resaltado) {
                g.setColor(new Color(0xffd54f));
                g.setStroke(new BasicStroke(3f));
                g.draw(new Ellipse2D.Double(cx - r - 5, cy - r - 5, 2 * (r + 5), 2 * (r + 5)));
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

            g.setFont(fDot);
            FontMetrics fm = g.getFontMetrics();
            String pos = Formacion.CASILLEROS[i];
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
        g.setColor(new Color(0, 0, 0, 90));
        g.fillOval(px - 6, py - 3, 14, 14);
        g.setColor(Color.WHITE);
        g.fillOval(px - 7, py - 7, 14, 14);
        g.setColor(new Color(0x222222));
        g.setStroke(new BasicStroke(1.5f));
        g.drawOval(px - 7, py - 7, 14, 14);
        g.fillOval(px - 2, py - 2, 4, 4);

        if (etiqueta != null && !etiqueta.isEmpty()) {
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(etiqueta);
            int bw = tw + 16, bh = fm.getHeight() + 6;
            int bxx = Math.max(4, Math.min(getWidth() - bw - 4, px - bw / 2));
            int byy = Math.max(4, py - 22 - bh);
            g.setColor(new Color(0, 0, 0, 170));
            g.fill(new RoundRectangle2D.Double(bxx, byy, bw, bh, 12, 12));
            g.setColor(Color.WHITE);
            g.drawString(etiqueta, bxx + 8, byy + 3 + fm.getAscent());
        }
    }

    private void dibujarBanner(Graphics2D g) {
        if (bannerTxt == null || bannerTxt.isEmpty()) return;
        int bh = (int) (fh * 0.30);
        int by0 = fy + (fh - bh) / 2;
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(fx, by0, fw, bh);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(28, fh / 7)));
        FontMetrics fm = g.getFontMetrics();
        int tx = fx + (fw - fm.stringWidth(bannerTxt)) / 2;
        int ty = by0 + (bh + fm.getAscent() - fm.getDescent()) / 2;
        g.setColor(Color.BLACK);
        g.drawString(bannerTxt, tx + 3, ty + 3);
        g.setColor(bannerColor);
        g.drawString(bannerTxt, tx, ty);
    }
}
