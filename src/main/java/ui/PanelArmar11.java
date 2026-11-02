package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import arcade.Formacion;
import arcade.Ratings;
import exception.ClubIncorrectoException;
import exception.EntradaVaciaException;
import exception.JugadorDuplicadoException;
import exception.PosicionInvalidaException;
import model.club;
import model.jugador;
import model.onceideal;
import service.juegoservice;

/** Paso 1: armar el 11. En cada turno sale un club al azar y hay que elegir un jugador válido para la posición. */
public class PanelArmar11 extends JPanel {

    private final VentanaPrincipal v;
    private final juegoservice service;

    private final CanchaPanel cancha = new CanchaPanel();
    private final JLabel lblTurno = new JLabel();
    private final JLabel lblPosicion = new JLabel();
    private final JLabel lblClub = new JLabel();
    private final JLabel lblError = new JLabel(" ");
    private final DefaultListModel<jugador> modelo = new DefaultListModel<>();
    private final JList<jugador> lista = new JList<>(modelo);
    private final JButton btnAlinear = new JButton("Alinear jugador");
    private final JButton btnPartido = new JButton("¡A JUGAR EL PARTIDO!");

    private onceideal once;
    private club clubTurno;
    private int turno;

    public PanelArmar11(VentanaPrincipal v, juegoservice service) {
        this.v = v;
        this.service = service;
        setLayout(new BorderLayout(12, 0));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(cancha, BorderLayout.CENTER);

        JPanel lateral = new JPanel();
        lateral.setLayout(new BoxLayout(lateral, BoxLayout.Y_AXIS));
        lateral.setPreferredSize(new Dimension(330, 400));

        lblTurno.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        lblTurno.setForeground(Color.GRAY);
        lblPosicion.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        lblClub.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        lblClub.setForeground(new Color(0x56ccf2));
        lblError.setForeground(new Color(0xff6b6b));

        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(javax.swing.JList<?> l, Object val, int i,
                                                          boolean sel, boolean foc) {
                super.getListCellRendererComponent(l, val, i, sel, foc);
                if (val instanceof jugador j) {
                    // Modificado para mostrar el apellido en lugar del nombre
                    setText(j.getApellido() + "   ·   " + Ratings.posicion(j) + "   ·   OVR " + Ratings.de(j));
                }
                setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
                return this;
            }
        });
        lista.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) alinear();
            }
        });
        JScrollPane scroll = new JScrollPane(lista);
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnAlinear.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        btnAlinear.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        btnAlinear.addActionListener(e -> alinear());

        btnPartido.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        btnPartido.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        btnPartido.setBackground(new Color(0x27ae60));
        btnPartido.setForeground(Color.WHITE);
        btnPartido.setVisible(false);
        btnPartido.addActionListener(e -> irAlPartido());

        for (JLabel l : new JLabel[]{lblTurno, lblPosicion, lblClub, lblError}) {
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        btnAlinear.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnPartido.setAlignmentX(Component.LEFT_ALIGNMENT);

        lateral.add(lblTurno);
        lateral.add(Box.createVerticalStrut(4));
        lateral.add(lblPosicion);
        lateral.add(Box.createVerticalStrut(8));
        lateral.add(new JLabel("Club sorteado:"));
        lateral.add(lblClub);
        lateral.add(Box.createVerticalStrut(10));
        lateral.add(scroll);
        lateral.add(Box.createVerticalStrut(6));
        lateral.add(lblError);
        lateral.add(Box.createVerticalStrut(6));
        lateral.add(btnAlinear);
        lateral.add(Box.createVerticalStrut(6));
        lateral.add(btnPartido);
        add(lateral, BorderLayout.EAST);
    }

    public void iniciar(String nombreDT) {
        once = new onceideal();
        once.setNombreUsuario(nombreDT);
        turno = 0;
        cancha.limpiar();
        cancha.limpiarNombres();
        cancha.setMostrarRival(false);
        lista.setEnabled(true);
        btnAlinear.setVisible(true);
        btnPartido.setVisible(false);
        siguienteTurno();
    }

    private void siguienteTurno() {
        lblError.setText(" ");
        if (turno >= 11) {
            cancha.setResaltado(-1);
            lblTurno.setText("¡Equipo completo!");
            lblPosicion.setText("Tu 11 está listo");
            lblClub.setText("—");
            modelo.clear();
            lista.setEnabled(false);
            btnAlinear.setVisible(false);
            btnPartido.setVisible(true);
            return;
        }
        try {
            clubTurno = service.sortearClubAleatorio();
            if (clubTurno == null) {
                lblError.setText("No hay clubes cargados.");
                return;
            }
            modelo.clear();
            for (jugador j : service.listarJugadoresDeClub(clubTurno.getId())) {
                modelo.addElement(j);
            }
        } catch (Exception ex) {
            lblError.setText("<html>Error de datos: " + ex.getMessage() + "</html>");
            return;
        }
        lblTurno.setText("TURNO " + (turno + 1) + " / 11");
        lblPosicion.setText("Posición: " + Formacion.ETIQUETAS[turno]);
        lblClub.setText(clubTurno.getNombre().toUpperCase());
        cancha.setResaltado(turno);
    }

    private void alinear() {
        if (turno >= 11) return;
        try {
            jugador elegido = lista.getSelectedValue();
            if (elegido == null) {
                throw new EntradaVaciaException("Elegí un jugador de la lista.");
            }
            service.agregarJugadorAOnce(once, elegido, clubTurno, Formacion.CASILLEROS[turno]);
            
            // Modificado para usar directamente el atributo getApellido() en la cancha
            cancha.setNombre(turno, elegido.getApellido());
            
            turno++;
            siguienteTurno();
        } catch (EntradaVaciaException | ClubIncorrectoException | JugadorDuplicadoException
                     | PosicionInvalidaException ex) {
            lblError.setText("<html>" + ex.getMessage() + "</html>");
        } catch (Exception ex) {
            lblError.setText("<html>Error inesperado: " + ex.getMessage() + "</html>");
        }
    }

    private void irAlPartido() {
        try {
            service.guardarOnceDefinitivo(once);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo guardar el 11 en la base de datos:\n" + ex.getMessage()
                            + "\n\nIgual podés jugar el partido.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
        }
        v.irAPartido(once);
    }
}