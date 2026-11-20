package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import arcade.Formacion;
import arcade.Ratings;
import exception.ClubIncorrectoException;
import exception.JugadorDuplicadoException;
import exception.PosicionInvalidaException;
import model.Club;
import model.Jugador;
import model.Onceideal;
import service.Juegoservice;

public class PanelArmar11 extends JPanel {

    private final VentanaPrincipal v;
    private final Juegoservice service;

    private final CanchaPanel cancha = new CanchaPanel();
    private final JLabel lblTurno = new JLabel();
    private final JLabel lblClub = new JLabel();
    private final JLabel lblError = new JLabel(" ");
    
    private final JTextField txtBuscarJugador = new JTextField();
    private JComboBox<String> comboPosicionesLibres;
    private final JComboBox<String> comboFormacion = new JComboBox<>(Formacion.nombresDisponibles());
    
    private final JButton btnPartido = new JButton("¡A JUGAR LA ELIMINATORIA!");
    private final JButton btnOtroClub = new JButton("Sortear otro club");

    // Componentes para las sugerencias flotantes
    private final javax.swing.JPopupMenu popupSugerencias = new javax.swing.JPopupMenu();
    private final javax.swing.DefaultListModel<Jugador> modeloSugerencias = new javax.swing.DefaultListModel<>();
    private final JList<Jugador> listaSugerencias = new JList<>(modeloSugerencias);

    private Onceideal once;
    private Club clubTurno;
    private int jugadoresAlineadosCount = 0;
    private final boolean[] casillerosOcupados = new boolean[11];
    private final Jugador[] porCasillero = new Jugador[11];
    private List<Jugador> jugadoresDelClubActual = new ArrayList<>();

    public PanelArmar11(VentanaPrincipal v, Juegoservice service) {
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

        // La formación se elige antes de empezar a ocupar casilleros.
        comboFormacion.setSelectedItem(Formacion.getActual());
        comboFormacion.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        comboFormacion.addActionListener(e -> {
            if (jugadoresAlineadosCount > 0) return;
            Formacion.setActual((String) comboFormacion.getSelectedItem());
            comboPosicionesLibres.removeAllItems();
            for (String etiqueta : Formacion.ETIQUETAS) comboPosicionesLibres.addItem(etiqueta);
            comboPosicionesLibres.setSelectedIndex(0);
            cancha.repaint();
        });

        // Selector libre con las 11 posiciones de la formación elegida.
        comboPosicionesLibres = new JComboBox<>(Formacion.ETIQUETAS);
        comboPosicionesLibres.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        comboPosicionesLibres.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                int idx = comboPosicionesLibres.getSelectedIndex();
                cancha.setResaltado(idx);
                verificarSiSirvePosicionActual();
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

        // Configuración de la lista flotante de sugerencias
        listaSugerencias.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object val, int i, boolean sel, boolean foc) {
                super.getListCellRendererComponent(l, val, i, sel, foc);
                if (val instanceof Jugador j) {
                    setText(Ratings.corto(j) + "  ·  " + String.join("/", j.getPosicionesPermitidas()) + "  ·  OVR " + Ratings.de(j));
                }
                setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                return this;
            }
        });
        listaSugerencias.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 1 || e.getClickCount() == 2) {
                    Jugador seleccionado = listaSugerencias.getSelectedValue();
                    if (seleccionado != null) {
                        txtBuscarJugador.setText(Ratings.corto(seleccionado));
                        popupSugerencias.setVisible(false);
                        alinearPorJugador(seleccionado);
                    }
                }
            }
        });

        txtBuscarJugador.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        txtBuscarJugador.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    popupSugerencias.setVisible(false);
                    alinearPorNombre();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    if (popupSugerencias.isVisible() && modeloSugerencias.getSize() > 0) {
                        listaSugerencias.requestFocus();
                        listaSugerencias.setSelectedIndex(0);
                    }
                } else {
                    actualizarSugerencias();
                }
            }
        });

        btnPartido.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        btnPartido.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        btnPartido.setBackground(new Color(0x27ae60));
        btnPartido.setForeground(Color.WHITE);
        btnPartido.setVisible(false);
        btnPartido.addActionListener(e -> irAlPartido());

        for (JLabel l : new JLabel[]{lblTurno, lblClub, lblError}) {
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        btnPartido.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtBuscarJugador.setAlignmentX(Component.LEFT_ALIGNMENT);
        comboPosicionesLibres.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        btnOtroClub.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnOtroClub.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnOtroClub.setVisible(false);
        btnOtroClub.addActionListener(e -> siguienteTurno());

        lateral.add(lblTurno);
        lateral.add(Box.createVerticalStrut(8));
        lateral.add(new JLabel("Club sorteado:"));
        lateral.add(lblClub);
        lateral.add(Box.createVerticalStrut(12));
        
        lateral.add(new JLabel("Formación táctica:"));
        lateral.add(comboFormacion);
        lateral.add(Box.createVerticalStrut(8));
        lateral.add(new JLabel("Elegir Posición a Llenar:"));
        lateral.add(comboPosicionesLibres);
        lateral.add(Box.createVerticalStrut(8));
        lateral.add(new JLabel("Escribir jugador y pulsar Enter:"));
        lateral.add(txtBuscarJugador);
        lateral.add(Box.createVerticalStrut(8));
        
        lateral.add(lblError);
        lateral.add(Box.createVerticalStrut(10));
        lateral.add(btnOtroClub);
        lateral.add(Box.createVerticalStrut(6));
        lateral.add(btnPartido);
        add(lateral, BorderLayout.EAST);
    }

    public void iniciar(String nombreDT) {
        once = new Onceideal();
        once.setNombreUsuario(nombreDT);
        comboFormacion.setSelectedItem(Formacion.getActual());
        comboFormacion.setEnabled(true);
        jugadoresAlineadosCount = 0;
        for (int i = 0; i < 11; i++) {
            casillerosOcupados[i] = false;
            porCasillero[i] = null;
        }
        comboPosicionesLibres.setSelectedIndex(0);
        comboPosicionesLibres.setEnabled(true);
        btnOtroClub.setVisible(false);
        cancha.limpiar();
        cancha.limpiarNombres();
        cancha.setMostrarRival(false);
        txtBuscarJugador.setEnabled(true);
        btnPartido.setVisible(false);
        txtBuscarJugador.setText("");
        popupSugerencias.setVisible(false);
        siguienteTurno();
    }

    private void siguienteTurno() {
        lblError.setText(" ");
        txtBuscarJugador.setText("");
        popupSugerencias.setVisible(false);

        if (jugadoresAlineadosCount >= 11) {
            comboFormacion.setEnabled(false);
            cancha.setResaltado(-1);
            lblTurno.setText("¡Equipo completo!");
            lblClub.setText("—");
            txtBuscarJugador.setEnabled(false);
            comboPosicionesLibres.setEnabled(false);
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
        verificarSiSirvePosicionActual();
    }

    private boolean estaAlineado(Jugador j) {
        for (Jugador a : porCasillero) {
            if (a != null && a.getId().equals(j.getId())) return true;
        }
        return false;
    }

    private boolean hayAlguienParaLibres() {
        for (Jugador j : jugadoresDelClubActual) {
            if (estaAlineado(j)) continue;
            for (int i = 0; i < 11; i++) {
                if (!casillerosOcupados[i] && j.esUbicacionValida(Formacion.CASILLEROS[i])) return true;
            }
        }
        return false;
    }

    private void verificarSiSirvePosicionActual() {
        int pos = comboPosicionesLibres.getSelectedIndex();
        if (pos < 0) return;
        
        boolean clubSinOpciones = !hayAlguienParaLibres();
        btnOtroClub.setVisible(clubSinOpciones);

        if (clubSinOpciones) {
            lblError.setText("<html>Ningún jugador de este club sirve para tus puestos libres. Podés sortear otro.</html>");
        } else {
            lblError.setText(" ");
        }
    }

    private void seleccionarSiguienteLibre() {
        for (int i = 0; i < 11; i++) {
            if (!casillerosOcupados[i]) {
                comboPosicionesLibres.setSelectedIndex(i);
                return;
            }
        }
    }

    private void actualizarSugerencias() {
        String texto = txtBuscarJugador.getText().trim().toLowerCase();
        modeloSugerencias.clear();

        if (texto.length() < 3) {
            popupSugerencias.setVisible(false);
            return;
        }

        for (Jugador j : jugadoresDelClubActual) {
            if (estaAlineado(j)) continue;
            String nombreCompleto = (j.getNombre() + " " + j.getApellido()).toLowerCase();
            String corto = Ratings.corto(j).toLowerCase();
            
            if (nombreCompleto.contains(texto) || corto.contains(texto)) {
                modeloSugerencias.addElement(j);
            }
        }

        if (modeloSugerencias.getSize() > 0) {
            popupSugerencias.removeAll();
            JScrollPane scroll = new JScrollPane(listaSugerencias);
            // Corrección de paréntesis en setPreferredSize
            scroll.setPreferredSize(new Dimension(txtBuscarJugador.getWidth(), Math.min(modeloSugerencias.getSize() * 30 + 10, 120)));
            popupSugerencias.add(scroll);
            popupSugerencias.show(txtBuscarJugador, 0, txtBuscarJugador.getHeight());
            txtBuscarJugador.requestFocusInWindow();
        } else {
            popupSugerencias.setVisible(false);
        }
    }

    private void alinearPorNombre() {
        String texto = txtBuscarJugador.getText().trim().toLowerCase();
        if (texto.isEmpty()) {
            lblError.setText("Escribí el nombre de un jugador.");
            return;
        }

        Jugador encontrado = null;
        for (int i = 0; i < modeloSugerencias.getSize(); i++) {
            Jugador j = modeloSugerencias.getElementAt(i);
            String corto = Ratings.corto(j).toLowerCase();
            String completo = (j.getNombre() + " " + j.getApellido()).toLowerCase();
            if (corto.equals(texto) || completo.equals(texto) || completo.contains(texto)) {
                encontrado = j;
                break;
            }
        }

        if (encontrado == null && modeloSugerencias.getSize() == 1) {
            encontrado = modeloSugerencias.getElementAt(0);
        }

        if (encontrado != null) {
            alinearPorJugador(encontrado);
        } else {
            lblError.setText("<html>No se encontró ningún jugador coincidente en las sugerencias.</html>");
        }
    }

    @SuppressWarnings("UseSpecificCatch")
    private void alinearPorJugador(Jugador elegido) {
        try {
            int posIndex = comboPosicionesLibres.getSelectedIndex();
            if (posIndex < 0) {
                throw new PosicionInvalidaException("Seleccioná una posición válida.");
            }
            
            if (casillerosOcupados[posIndex]) {
                throw new PosicionInvalidaException("¡Esta posición ya está ocupada! Elegí otra en el selector.");
            }

            String posicionSeleccionada = Formacion.CASILLEROS[posIndex];

            if (!elegido.esUbicacionValida(posicionSeleccionada)) {
                throw new PosicionInvalidaException(Ratings.corto(elegido) + " no puede jugar en la posición " + posicionSeleccionada + ".");
            }

            service.agregarJugadorAOnce(once, elegido, clubTurno, posicionSeleccionada);
            
            casillerosOcupados[posIndex] = true;
            porCasillero[posIndex] = elegido;
            cancha.setNombre(posIndex, Ratings.corto(elegido));
            
            jugadoresAlineadosCount++;
            if (jugadoresAlineadosCount == 1) comboFormacion.setEnabled(false);
            txtBuscarJugador.setText("");
            popupSugerencias.setVisible(false);
            
            if (jugadoresAlineadosCount < 11) {
                seleccionarSiguienteLibre();
            }
            siguienteTurno();

        } catch (ClubIncorrectoException | JugadorDuplicadoException
                 | PosicionInvalidaException ex) {
            lblError.setText("<html>" + ex.getMessage() + "</html>");
        } catch (Exception ex) {
            lblError.setText("<html>Error inesperado: " + ex.getMessage() + "</html>");
        }
    }

    private void irAlPartido() {
        List<Jugador> ordenado = new ArrayList<>();
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