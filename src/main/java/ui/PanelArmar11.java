package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
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

/** Paso 1: armar el 11 de forma libre. En cada turno sale un club al azar y eliges la posición que quieras llenar. */
public class PanelArmar11 extends JPanel {

    private final VentanaPrincipal v;
    private final juegoservice service;

    private final CanchaPanel cancha = new CanchaPanel();
    private final JLabel lblTurno = new JLabel();
    private final JLabel lblClub = new JLabel();
    private final JLabel lblError = new JLabel(" ");
    
    private final JTextField txtBuscarJugador = new JTextField();
    private final JComboBox<String> comboPosicionesLibres;
    
    private final DefaultListModel<jugador> modelo = new DefaultListModel<>();
    private final JList<jugador> lista = new JList<>(modelo);
    private final JButton btnAlinear = new JButton("Alinear jugador");
    private final JButton btnPartido = new JButton("¡A JUGAR EL PARTIDO!");

    private onceideal once;
    private club clubTurno;
    private int jugadoresAlineadosCount = 0;
    private final boolean[] casillerosOcupados = new boolean[11];
    private List<jugador> jugadoresDelClubActual = new ArrayList<>();

    public PanelArmar11(VentanaPrincipal v, juegoservice service) {
        this.v = v;
        this.service = service;
        setLayout(new BorderLayout(12, 0));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(cancha, BorderLayout.CENTER);

        JPanel lateral = new JPanel();
        lateral.setLayout(new BoxLayout(lateral, BoxLayout.Y_AXIS));
        lateral.setPreferredSize(new Dimension(330, 500));

        lblTurno.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        lblTurno.setForeground(Color.GRAY);
        lblClub.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        lblClub.setForeground(new Color(0x56ccf2));
        lblError.setForeground(new Color(0xff6b6b));

        // Selector libre con las 11 posiciones tácticas de la formación
        comboPosicionesLibres = new JComboBox<>(Formacion.ETIQUETAS);
        comboPosicionesLibres.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        comboPosicionesLibres.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                int idx = comboPosicionesLibres.getSelectedIndex();
                cancha.setResaltado(idx); // Resalta en la cancha la posición seleccionada
            }
        });

        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(javax.swing.JList<?> l, Object val, int i,
                                                          boolean sel, boolean foc) {
                super.getListCellRendererComponent(l, val, i, sel, foc);
                if (val instanceof jugador j) {
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

        txtBuscarJugador.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        txtBuscarJugador.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filtrarJugadoresPorTexto();
            }
        });

        btnAlinear.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        btnAlinear.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        btnAlinear.addActionListener(e -> alinear());

        btnPartido.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        btnPartido.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        btnPartido.setBackground(new Color(0x27ae60));
        btnPartido.setForeground(Color.WHITE);
        btnPartido.setVisible(false);
        btnPartido.addActionListener(e -> irAlPartido());

        for (JLabel l : new JLabel[]{lblTurno, lblClub, lblError}) {
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        btnAlinear.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnPartido.setAlignmentX(Component.LEFT_ALIGNMENT);

        lateral.add(lblTurno);
        lateral.add(Box.createVerticalStrut(8));
        lateral.add(new JLabel("Club sorteado:"));
        lateral.add(lblClub);
        lateral.add(Box.createVerticalStrut(8));
        
        lateral.add(new JLabel("Elegir Posición a Llenar:"));
        lateral.add(comboPosicionesLibres);
        lateral.add(Box.createVerticalStrut(6));
        lateral.add(new JLabel("Buscar Jugador (Escribe 3+ letras):"));
        lateral.add(txtBuscarJugador);
        lateral.add(Box.createVerticalStrut(8));
        
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
        jugadoresAlineadosCount = 0;
        for (int i = 0; i < 11; i++) {
            casillerosOcupados[i] = false;
        }
        cancha.limpiar();
        cancha.limpiarNombres();
        cancha.setMostrarRival(false);
        lista.setEnabled(true);
        txtBuscarJugador.setEnabled(true);
        comboPosicionesLibres.setEnabled(true);
        btnAlinear.setVisible(true);
        btnPartido.setVisible(false);
        txtBuscarJugador.setText("");
        siguienteTurno();
    }

    private void siguienteTurno() {
        lblError.setText(" ");
        txtBuscarJugador.setText("");
        modelo.clear();

        if (jugadoresAlineadosCount >= 11) {
            cancha.setResaltado(-1);
            lblTurno.setText("¡Equipo completo!");
            lblClub.setText("—");
            modelo.clear();
            lista.setEnabled(false);
            txtBuscarJugador.setEnabled(false);
            comboPosicionesLibres.setEnabled(false);
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
            jugadoresDelClubActual = service.listarJugadoresDeClub(clubTurno.getId());
        } catch (Exception ex) {
            lblError.setText("<html>Error de datos: " + ex.getMessage() + "</html>");
            return;
        }
        lblTurno.setText("JUGADORES: " + jugadoresAlineadosCount + " / 11");
        lblClub.setText(clubTurno.getNombre().toUpperCase());
        
        if (comboPosicionesLibres.getItemCount() > 0) {
            cancha.setResaltado(comboPosicionesLibres.getSelectedIndex());
        }
    }

    private void filtrarJugadoresPorTexto() {
        String texto = txtBuscarJugador.getText().trim().toLowerCase();
        modelo.clear();
        
        if (texto.length() < 3) {
            return; 
        }
        
        for (jugador j : jugadoresDelClubActual) {
            String nombreCompleto = (j.getNombre() + " " + j.getApellido()).toLowerCase();
            if (nombreCompleto.contains(texto)) {
                modelo.addElement(j);
            }
        }
    }

    private void alinear() {
        if (jugadoresAlineadosCount >= 11) return;
        try {
            jugador elegido = lista.getSelectedValue();
            if (elegido == null) {
                throw new EntradaVaciaException("Escribí al menos 3 letras y seleccioná un jugador.");
            }
            
            int posIndex = comboPosicionesLibres.getSelectedIndex();
            if (posIndex < 0) {
                throw new PosicionInvalidaException("Seleccioná una posición válida.");
            }
            
            if (casillerosOcupados[posIndex]) {
                throw new PosicionInvalidaException("¡Esta posición ya está ocupada!");
            }
            
            // Obtenemos la etiqueta de la posición seleccionada (Ej: "LB", "CB", "GK", etc.)
            String posicionSeleccionada = Formacion.CASILLEROS[posIndex];

            // VALIDACIÓN ESTRICTA: Comprueba si el jugador puede ocupar esa posición según sus datos permitidos
            if (!elegido.esUbicacionValida(posicionSeleccionada)) {
                throw new PosicionInvalidaException(elegido.getApellido() + " no puede ser alineado en la posición " + posicionSeleccionada);
            }
            
            // Asigna el jugador a la casilla seleccionada de forma válida
            service.agregarJugadorAOnce(once, elegido, clubTurno, posicionSeleccionada);
            
            casillerosOcupados[posIndex] = true;
            cancha.setNombre(posIndex, elegido.getApellido());
            
            jugadoresAlineadosCount++;
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