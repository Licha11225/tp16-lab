package ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.util.Random;

/** Escenas de pelota parada: tiro libre, penal y penal defendido por el usuario. */
public class PanelRemate extends JPanel {
    private boolean tiroLibre = true;
    private boolean penalContra;
    private int aimX = 0;       // -1 izquierda, 0 centro, 1 derecha
    private int aimY = 0;       // -1 alto, 0 medio, 1 bajo
    private int potencia = 70;
    private int arqueroX = 0;
    private int arqueroY = 0;
    private boolean lanzado;
    private boolean carrera;
    private boolean disparado;
    private double progresoBola;
    private int tiroRivalX = 0, tiroRivalY = 0;
    private final Random azar = new Random();
    private final Timer animacion = new Timer(16, e -> {
        if (carrera) progresoBola += 0.035;
        if (disparado) progresoBola += 0.045;
        if (progresoBola >= 1) { progresoBola = 1; ((Timer)e.getSource()).stop(); }
        repaint();
    });

    public PanelRemate() {
        setPreferredSize(new Dimension(700, 430));
        setBackground(new Color(0x2f7f35));
        setFocusable(true);
    }

    public void configurar(boolean tiroLibre, int direccion, int potencia) {
        this.tiroLibre = tiroLibre;
        this.penalContra = false;
        this.aimX = Math.max(-1, Math.min(1, direccion));
        this.potencia = Math.max(20, Math.min(100, potencia));
        this.aimY = 0;
        this.lanzado = false;
        this.carrera = false;
        this.disparado = false;
        this.progresoBola = 0;
        animacion.stop();
        repaint();
    }

    public void configurarPenalContra() {
        tiroLibre = false;
        penalContra = true;
        arqueroX = 0;
        arqueroY = 0;
        tiroRivalX = azar.nextInt(3) - 1;
        tiroRivalY = azar.nextInt(3) - 1;
        lanzado = false;
        carrera = false;
        disparado = false;
        progresoBola = 0;
        animacion.stop();
        repaint();
    }

    public void moverObjetivo(int dx, int dy) {
        if (penalContra) {
            arqueroX = limitar(arqueroX + dx, -1, 1);
            arqueroY = limitar(arqueroY + dy, -1, 1);
        } else {
            aimX = limitar(aimX + dx, -1, 1);
            aimY = limitar(aimY + dy, -1, 1);
        }
        repaint();
    }

    public void setPotencia(int valor) { potencia = limitar(valor, 20, 100); repaint(); }
    public int getPotencia() { return potencia; }
    public int getDireccion() { return aimX + 1; }
    public int getAltura() { return aimY; }
    public int getDireccionArquero() { return arqueroX + 1; }
    public int getAlturaArquero() { return arqueroY; }

    public void iniciarCarrera() {
        carrera = true;
        progresoBola = 0;
        animacion.start();
    }

    public void mostrarDisparo() {
        carrera = false;
        disparado = true;
        progresoBola = 0;
        animacion.start();
    }

    public void detenerAnimacion() { animacion.stop(); carrera = false; }
    private int limitar(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        g.setColor(new Color(0x343a40)); g.fillRect(0, 0, w, (int)(h*.20));
        for (int i=0;i<9;i++) { g.setColor(i%2==0?new Color(0x2d8735):new Color(0x3c9b3f)); g.fillRect(0,(int)(h*.20+i*h*.09),w,(int)(h*.09)+1); }

        int gx=(int)(w*.17), gy=(int)(h*.08), gw=(int)(w*.66), gh=(int)(h*.34);
        g.setColor(new Color(0xb8d5d8)); g.fillRect(gx+4,gy+4,gw-8,gh-6);
        g.setColor(new Color(0x7b9297)); g.setStroke(new BasicStroke(1));
        for(int x=gx+10;x<gx+gw;x+=16)g.drawLine(x,gy+5,x,gy+gh-5);
        for(int y=gy+8;y<gy+gh;y+=13)g.drawLine(gx+5,y,gx+gw-5,y);
        g.setColor(Color.WHITE);g.setStroke(new BasicStroke(7));g.drawRect(gx,gy,gw,gh);

        // Barrera sólo en tiro libre, ubicada entre la pelota y el arco.
        if (tiroLibre) for(int i=0;i<5;i++) jugador(g,(int)(w*(.37+i*.065)),(int)(h*.52),new Color(0xe0a51b),.85);

        // El arquero se desplaza en el penal en contra según las teclas del usuario.
        int ax = w/2 + (int)(arqueroX*w*.19);
        int ay = (int)(h*.29) + (int)(arqueroY*h*.055);
        if (disparado && penalContra) { ax += (int)(arqueroX*w*.08); ay -= 8; }
        jugador(g,ax,ay,new Color(0x12a9a1),1.12);

        int px = tiroLibre ? (int)(w*.22) : w/2;
        int py = tiroLibre ? (int)(h*.80) : (int)(h*.78);
        if (carrera) px += (int)(progresoBola*18);
        jugadorDeEspaldas(g,px,py-20,new Color(0xd83d32),1.15);
        int ballX=px+(tiroLibre?42:0), ballY=py+2;
        if (disparado) {
            int dx = penalContra ? tiroRivalX : aimX;
            int dy = penalContra ? tiroRivalY : aimY;
            int tx = (int)(w*(.50 + dx*.22));
            int ty = (int)(h*(.25 + dy*.075));
            double k = Math.min(1, progresoBola);
            ballX = (int)(px + (tx-px)*k);
            ballY = (int)(py + (ty-py)*k - Math.sin(k*Math.PI)*potencia*.42);
        }
        pelota(g,ballX,ballY,13);

        if (!penalContra && !disparado) {
            int tx=(int)(w*(.50+aimX*.22));
            int ty=(int)(h*(.25+aimY*.075));
            g.setColor(new Color(255,45,30,220));g.setStroke(new BasicStroke(4));
            g.drawLine(ballX,ballY,tx,ty);
            g.fillOval(tx-7,ty-7,14,14);
        }

        g.setColor(new Color(0,0,0,170));g.fillRoundRect(12,12,260,30,10,10);
        g.setColor(Color.WHITE);g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,13));
        String modo = penalContra ? "PENAL EN CONTRA · MOVÉ AL ARQUERO" : tiroLibre ? "TIRO LIBRE · FLECHAS PARA APUNTAR" : "PENAL · FLECHAS PARA APUNTAR";
        g.drawString(modo,22,32);

        // Medidor de potencia.
        if (!penalContra) {
            int bx= w/2-115, by=h-35;
            g.setColor(new Color(0,0,0,170));g.fillRoundRect(bx,by,230,18,8,8);
            g.setColor(new Color(0x27ae60));g.fillRoundRect(bx+2,by+2,(int)(226*potencia/100.0),14,6,6);
            g.setColor(Color.WHITE);g.drawRoundRect(bx,by,230,18,8,8);
            g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,12));
            g.drawString("POTENCIA " + potencia + "%  ·  ESPACIO PARA CARGAR / SOLTAR",bx-85,by-7);
        } else {
            g.setColor(Color.WHITE);g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,13));
            g.drawString("← → LADO   ·   ↑ ↓ ALTURA   ·   ESPACIO: LANZARSE",w/2-190,h-18);
        }
        g.dispose();
    }

    private void jugador(Graphics2D g,int x,int y,Color camiseta,double escala){
        int r=(int)(17*escala);g.setColor(new Color(0,0,0,75));g.fillOval(x-r,y+25,r*2,10);
        g.setColor(new Color(0xd49a70));g.fillOval(x-9,y-31,18,18);
        g.setColor(camiseta);g.fillRoundRect(x-13,y-15,26,34,7,7);
        g.setColor(new Color(0x23395b));g.fillRect(x-12,y+18,10,22);g.fillRect(x+2,y+18,10,22);
        g.setColor(Color.WHITE);g.fillRect(x-13,y+39,11,5);g.fillRect(x+2,y+39,11,5);
    }
    private void jugadorDeEspaldas(Graphics2D g,int x,int y,Color camiseta,double escala){
        jugador(g,x,y,camiseta,escala);g.setColor(new Color(0x4b3024));g.fillArc(x-10,y-33,20,13,0,180);
        g.setColor(Color.WHITE);g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,11));g.drawString("9",x-3,y+7);
    }
    private void pelota(Graphics2D g,int x,int y,int r){
        g.setColor(new Color(0,0,0,90));g.fillOval(x-r,y+r-2,r*2,r/2);
        g.setColor(Color.WHITE);g.fillOval(x-r,y-r,r*2,r*2);g.setColor(Color.BLACK);g.setStroke(new BasicStroke(2));g.drawOval(x-r,y-r,r*2,r*2);
        g.fillPolygon(new Polygon(new int[]{x,x+6,x+4,x-4,x-6},new int[]{y-r+3,y-3,y+6,y+6,y-3},5));
    }
}
