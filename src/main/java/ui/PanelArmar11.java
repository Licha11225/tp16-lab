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
    private final JButton btnPartido = new JButton("¡A JUGAR LA ELIMINATORIA!");
    private final JButton btnOtroClub = new JButton("Sortear otro club");

    private onceideal once;
    private club clubTurno;
    private int jugadoresAlineadosCount = 0;
    private final boolean[] casillerosOcupados = new boolean[11];
    // Quién quedó en cada casillero: de acá sale el orden del 11 que usa el partido. 
    private final jugador[] porCasillero = new jugador[11];
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
                refrescarLista();
            }
        });
        comboPosicionesLibres.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object val, int i, boolean sel, boolean foc) {
                super.getListCellRendererComponent(l, val, i, sel, foc);
                if (i >= 0 && casillerosOcupados[i]) {
                    setText(val + "  (ocupado)");
                    setForeground(Color.GRAY);
                }
                return this;
            }
        });

        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(javax.swing.JList<?> l, Object val, int i,
                                                          boolean sel, boolean foc) {
                super.getListCellRendererComponent(l, val, i, sel, foc);
                if (val instanceof jugador j) {
                    setText(Ratings.corto(j) + "   ·   " + String.join("/", j.getPosicionesPermitidas())
                            + "   ·   OVR " + Ratings.de(j));
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
                refrescarLista();
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
        btnOtroClub.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnOtroClub.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnOtroClub.setVisible(false);
        btnOtroClub.addActionListener(e -> siguienteTurno());

        lateral.add(lblTurno);
        lateral.add(Box.createVerticalStrut(8));
        lateral.add(new JLabel("Club sorteado:"));
        lateral.add(lblClub);
        lateral.add(Box.createVerticalStrut(8));
        
        lateral.add(new JLabel("Elegir Posición a Llenar:"));
        lateral.add(comboPosicionesLibres);
        lateral.add(Box.createVerticalStrut(6));
        lateral.add(new JLabel("Filtrar por nombre (opcional):"));
        lateral.add(txtBuscarJugador);
        lateral.add(Box.createVerticalStrut(8));
        
        lateral.add(scroll);
        lateral.add(Box.createVerticalStrut(6));
        lateral.add(lblError);
        lateral.add(Box.createVerticalStrut(6));
        lateral.add(btnAlinear);
        lateral.add(Box.createVerticalStrut(6));
        lateral.add(btnOtroClub);
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
            porCasillero[i] = null;
        }
        comboPosicionesLibres.setSelectedIndex(0);
        btnOtroClub.setVisible(false);
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
            btnOtroClub.setVisible(false);
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
        
        cancha.setResaltado(comboPosicionesLibres.getSelectedIndex());
        refrescarLista();
        // si ningún jugador de este club sirve para los puestos que quedan, se puede volver a sortear
        btnOtroClub.setVisible(!hayAlguienParaLibres());
        if (btnOtroClub.isVisible()) {
            lblError.setText("<html>Ningún jugador de este club sirve para tus puestos libres.</html>");
        }
    }

    private boolean estaAlineado(jugador j) {
        for (jugador a : porCasillero) {
    if (a != null && a.getId().equals(j.getId())) return true;
}
        return false;
    }

    private boolean hayAlguienParaLibres() {
        for (jugador j : jugadoresDelClubActual) {
            if (estaAlineado(j)) continue;
            for (int i = 0; i < 11; i++) {
                if (!casillerosOcupados[i] && j.esUbicacionValida(Formacion.CASILLEROS[i])) return true;
            }
        }
        return false;
    }

    private void seleccionarSiguienteLibre() {
        for (int i = 0; i < 11; i++) {
            if (!casillerosOcupados[i]) {
                comboPosicionesLibres.setSelectedIndex(i);
                return;
            }
        }
    }

    @SuppressWarnings("SizeReplaceableByIsEmpty")
    private void refrescarLista() {
        String texto = txtBuscarJugador.getText().trim().toLowerCase();
        int pos = comboPosicionesLibres.getSelectedIndex();
        modelo.clear();
        if (pos < 0 || once == null) return;
        String casillero = Formacion.CASILLEROS[pos];
        for (jugador j : jugadoresDelClubActual) {
            if (estaAlineado(j) || !j.esUbicacionValida(casillero)) continue;
            String nombreCompleto = (j.getNombre() + " " + j.getApellido()).toLowerCase();
            if (texto.isEmpty() || nombreCompleto.contains(texto)) modelo.addElement(j);
        }
        if (modelo.isEmpty() && !casillerosOcupados[pos] && jugadoresDelClubActual.size() > 0 && texto.isEmpty()) {
            lblError.setText("<html>Este club no tiene jugadores para " + casillero + ". Probá con otra posición.</html>");
        } else if (!btnOtroClub.isVisible()) {
            lblError.setText(" ");
        }
    }

    @SuppressWarnings("UseSpecificCatch")
    private void alinear() {
        if (jugadoresAlineadosCount >= 11) return;
        try {
            jugador elegido = lista.getSelectedValue();
            if (elegido == null) {
                throw new EntradaVaciaException("Seleccioná un jugador de la lista.");
            }
            
            int posIndex = comboPosicionesLibres.getSelectedIndex();
            if (posIndex < 0) {
                throw new PosicionInvalidaException("Seleccioná una posición válida.");
            }
            
            if (casillerosOcupados[posIndex]) {
                throw new PosicionInvalidaException("¡Esta posición ya está ocupada!");
            }
            
            // Obtiene la etiqueta de la posición seleccionada
            String posicionSeleccionada = Formacion.CASILLEROS[posIndex];

            if (!elegido.esUbicacionValida(posicionSeleccionada)) {
                throw new PosicionInvalidaException(Ratings.corto(elegido) + " no puede ser alineado en la posición " + posicionSeleccionada);
            }
            
            // Asigna el jugador a la casilla seleccionada de forma válida
            service.agregarJugadorAOnce(once, elegido, clubTurno, posicionSeleccionada);
            
            casillerosOcupados[posIndex] = true;
            porCasillero[posIndex] = elegido;
            cancha.setNombre(posIndex, Ratings.corto(elegido));
            
            jugadoresAlineadosCount++;
            seleccionarSiguienteLibre();
            siguienteTurno();
        } catch (EntradaVaciaException | ClubIncorrectoException | JugadorDuplicadoException
                     | PosicionInvalidaException ex) {
            lblError.setText("<html>" + ex.getMessage() + "</html>");
        } catch (Exception ex) {
            lblError.setText("<html>Error inesperado: " + ex.getMessage() + "</html>");
        }
    }

    private void irAlPartido() {
        // El partido lee el 11 por casillero se ordena según dónde puso a cada uno
        List<jugador> ordenado = new ArrayList<>();
        for (int i = 0; i < 11; i++) ordenado.add(porCasillero[i]);
        once.setListaJugadores(ordenado);
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