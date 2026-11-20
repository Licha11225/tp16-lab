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

/**
 * Escena frontal de pelota parada inspirada en las referencias pixel-art.
 * La dirección se representa con una flecha y la potencia con la longitud.
 */
public class PanelRemate extends JPanel {
    private int direccion=1;
    private int potencia=70;
    private boolean tiroLibre=true;

    public PanelRemate() {
        setPreferredSize(new Dimension(700,430));
        setBackground(new Color(0x2f7f35));
    }
    public void configurar(boolean tiroLibre,int direccion,int potencia){
        this.tiroLibre=tiroLibre;
        this.direccion=direccion;
        this.potencia=potencia;
        repaint();
    }
    @Override protected void paintComponent(Graphics g0){
        super.paintComponent(g0);
        Graphics2D g=(Graphics2D)g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        int w=getWidth(),h=getHeight();
        // Césped en franjas y tribuna
        g.setColor(new Color(0x343a40)); g.fillRect(0,0,w,(int)(h*.25));
        for(int i=0;i<8;i++){g.setColor(i%2==0?new Color(0x2d8735):new Color(0x3c9b3f));g.fillRect(0,(int)(h*.25+i*h*.094),w,(int)(h*.094)+1);}
        // Arco y red
        int gx=(int)(w*.17), gy=(int)(h*.08), gw=(int)(w*.66), gh=(int)(h*.34);
        g.setColor(new Color(0xeeeeee, true)); g.fillRect(gx,gy,gw,gh);
        g.setColor(new Color(0xb8d5d8)); g.fillRect(gx+5,gy+5,gw-10,gh-8);
        g.setColor(new Color(0x7b9297)); g.setStroke(new BasicStroke(1));
        for(int x=gx+10;x<gx+gw;x+=16)g.drawLine(x,gy+5,x,gy+gh-5);
        for(int y=gy+8;y<gy+gh;y+=13)g.drawLine(gx+5,y,gx+gw-5,y);
        g.setColor(Color.WHITE);g.setStroke(new BasicStroke(7));g.drawRect(gx,gy,gw,gh);
        // Arquero
        int ax=w/2, ay=(int)(h*.29);
        jugador(g,ax,ay,new Color(0x12a9a1),1.15);
        // Barrera defensiva
        int baseY=(int)(h*.57), count=tiroLibre?5:0;
        for(int i=0;i<count;i++){
            int x=(int)(w*(.34+i*.08));
            jugador(g,x,baseY,new Color(0xe0a51b),1.0);
        }
        // Pateador y pelota
        int px=(int)(w*.22), py=(int)(h*.80);
        jugadorDeEspaldas(g,px,py-22,new Color(0xd83d32),1.25);
        pelota(g,px+42,py+3,14);
        // Flecha de dirección hacia el arco
        int targetX=(int)(w*(.50+direccion*.22));
        int targetY=(int)(h*(.42-(potencia/100.0)*.13));
        int sx=px+48, sy=py;
        double factor = .45 + potencia / 100.0 * .55;
        targetX = sx + (int)((targetX - sx) * factor);
        targetY = sy + (int)((targetY - sy) * factor);
        g.setColor(new Color(255,31,18,220));
        g.setStroke(new BasicStroke(8,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g.drawLine(sx,sy,targetX,targetY+20);
        Polygon head=new Polygon(new int[]{targetX,targetX-20,targetX+20},new int[]{targetY,targetY+26,targetY+26},3);
        g.fill(head);
        g.setColor(new Color(0,0,0,150));g.fillRoundRect(12,12,190,28,10,10);
        g.setColor(Color.WHITE);g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,13));
        g.drawString(tiroLibre?"TIRO LIBRE":"PENAL",22,31);
        g.dispose();
    }
    private void jugador(Graphics2D g,int x,int y,Color camiseta,double escala){
        int r=(int)(17*escala);
        g.setColor(new Color(0,0,0,75));g.fillOval(x-r,y+25,r*2,10);
        g.setColor(new Color(0xd49a70));g.fillOval(x-9,y-31,18,18);
        g.setColor(camiseta);g.fillRoundRect(x-13,y-15,26,34,7,7);
        g.setColor(new Color(0x23395b));g.fillRect(x-12,y+18,10,22);g.fillRect(x+2,y+18,10,22);
        g.setColor(Color.WHITE);g.fillRect(x-13,y+39,11,5);g.fillRect(x+2,y+39,11,5);
    }

    /** Pateador visto desde atrás, como en las referencias del usuario. */
    private void jugadorDeEspaldas(Graphics2D g,int x,int y,Color camiseta,double escala){
        int r=(int)(17*escala);
        g.setColor(new Color(0,0,0,75));g.fillOval(x-r,y+25,r*2,10);
        g.setColor(new Color(0xd49a70));g.fillOval(x-9,y-31,18,18);
        // Pelo en la nuca: no se dibujan ojos ni rasgos frontales.
        g.setColor(new Color(0x4b3024));g.fillArc(x-10,y-33,20,13,0,180);
        g.setColor(camiseta);g.fillRoundRect(x-13,y-15,26,34,7,7);
        g.setColor(new Color(0xffffff));g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,11));
        g.drawString("9",x-3,y+7);
        g.setColor(new Color(0x23395b));g.fillRect(x-12,y+18,10,22);g.fillRect(x+2,y+18,10,22);
        g.setColor(Color.WHITE);g.fillRect(x-13,y+39,11,5);g.fillRect(x+2,y+39,11,5);
    }

    private void pelota(Graphics2D g,int x,int y,int r){
        g.setColor(new Color(0,0,0,90));g.fillOval(x-r,y+r-2,r*2,r/2);
        g.setColor(Color.WHITE);g.fillOval(x-r,y-r,r*2,r*2);
        g.setColor(Color.BLACK);g.setStroke(new BasicStroke(2));g.drawOval(x-r,y-r,r*2,r*2);
        g.fillPolygon(new int[]{x,x+6,x+4,x-4,x-6},new int[]{y-r+3,y-3,y+6,y+6,y-3},5);
        for(int i=0;i<5;i++){double a=i*Math.PI*2/5;g.fillOval(x+(int)(Math.cos(a)*r*.65)-3,y+(int)(Math.sin(a)*r*.65)-3,6,6);}
    }
}
