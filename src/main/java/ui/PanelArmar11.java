package ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
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
import javax.swing.DefaultComboBoxModel;
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
    private final JComboBox<String> comboPosicionesLibres;
    
    private final JButton btnPartido = new JButton("¡A JUGAR LA ELIMINATORIA!");
    private final JButton btnOtroClub = new JButton("Sortear otro club");

    // Componentes para las sugerencias flotantes
    private final javax.swing.JPopupMenu popupSugerencias = new javax.swing.JPopupMenu();
    private final javax.swing.DefaultListModel<Jugador> modeloSugerencias = new javax.swing.DefaultListModel<>();
    private final JList<Jugador> listaSugerencias = new JList<>(modeloSugerencias);

    // Paso 1: elegir la formación
    private final CardLayout cardsLateral = new CardLayout();
    private final JPanel contLateral = new JPanel(cardsLateral);
    private final DefaultListModel<Formacion> modeloFormaciones = new DefaultListModel<>();
    private final JList<Formacion> listaFormaciones = new JList<>(modeloFormaciones);
    private final JLabel lblFormacionDesc = new JLabel(" ");
    private Formacion formacion = Formacion.TODAS.get(0);
    private boolean formacionConfirmada = false;

    private String nombreDT = "DT";
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

        // Selector libre con las 11 posiciones tácticas
        comboPosicionesLibres = new JComboBox<>(formacion.etiquetas());
        comboPosicionesLibres.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        comboPosicionesLibres.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && formacionConfirmada) {
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
        lateral.add(Box.createVerticalGlue());
        JButton btnCambiarFormacion = new JButton("← Cambiar formación");
        btnCambiarFormacion.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        btnCambiarFormacion.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnCambiarFormacion.addActionListener(e -> volverAElegirFormacion());
        lateral.add(btnCambiarFormacion);

        contLateral.setPreferredSize(new Dimension(330, 500));
        contLateral.add(armarPanelFormacion(), "formacion");
        contLateral.add(lateral, "armado");
        add(contLateral, BorderLayout.EAST);
    }

    /** Paso 1: lista de formaciones con vista previa en la cancha. */
    private JPanel armarPanelFormacion() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("ELEGÍ TU FORMACIÓN");
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        titulo.setForeground(new Color(0xffd54f));
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel ayuda = new JLabel("<html><div style='width:290px'>Define las posiciones de tu 11. Por ejemplo, en un "
                + "3-5-2 jugás con 3 CB y volantes por banda (LM y RM). Mirá la vista previa en la cancha.</div></html>");
        ayuda.setForeground(Color.GRAY);
        ayuda.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (Formacion f : Formacion.TODAS) modeloFormaciones.addElement(f);
        listaFormaciones.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaFormaciones.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        listaFormaciones.setFixedCellHeight(30);
        listaFormaciones.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            Formacion f = listaFormaciones.getSelectedValue();
            if (f == null) return;
            formacion = f;
            cancha.setFormaciones(f, f);
            cancha.limpiarNombres();
            lblFormacionDesc.setText("<html><div style='width:290px'><b>" + f.getNombre() + "</b><br>" + f.descripcion() + "</div></html>");
        });
        JScrollPane scroll = new JScrollPane(listaFormaciones);
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        scroll.setPreferredSize(new Dimension(300, 260));

        lblFormacionDesc.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnConfirmar = new JButton("Confirmar formación");
        btnConfirmar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        btnConfirmar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        btnConfirmar.setBackground(new Color(0x27ae60));
        btnConfirmar.setForeground(Color.WHITE);
        btnConfirmar.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnConfirmar.addActionListener(e -> confirmarFormacion());

        p.add(titulo);
        p.add(Box.createVerticalStrut(6));
        p.add(ayuda);
        p.add(Box.createVerticalStrut(10));
        p.add(scroll);
        p.add(Box.createVerticalStrut(10));
        p.add(lblFormacionDesc);
        p.add(Box.createVerticalStrut(12));
        p.add(btnConfirmar);
        return p;
    }

    private void confirmarFormacion() {
        Formacion f = listaFormaciones.getSelectedValue();
        if (f == null) return;
        formacion = f;
        reiniciarArmado();
        comboPosicionesLibres.setModel(new DefaultComboBoxModel<>(formacion.etiquetas()));
        formacionConfirmada = true;
        comboPosicionesLibres.setSelectedIndex(0);
        cancha.setFormaciones(formacion, formacion);
        cancha.limpiarNombres();
        cardsLateral.show(contLateral, "armado");
        siguienteTurno();
    }

    private void volverAElegirFormacion() {
        if (jugadoresAlineadosCount > 0) {
            int r = JOptionPane.showConfirmDialog(this,
                    "Si cambiás de formación se borran los jugadores que ya elegiste. ¿Continuar?",
                    "Cambiar formación", JOptionPane.YES_NO_OPTION);
            if (r != JOptionPane.YES_OPTION) return;
        }
        mostrarElegirFormacion();
    }

    private void mostrarElegirFormacion() {
        formacionConfirmada = false;
        reiniciarArmado();
        popupSugerencias.setVisible(false);
        cancha.limpiar();
        cancha.setMostrarRival(false);
        cancha.setResaltado(-1);
        cancha.limpiarNombres();
        listaFormaciones.setSelectedValue(formacion, true);
        cancha.setFormaciones(formacion, formacion);
        lblFormacionDesc.setText("<html><div style='width:290px'><b>" + formacion.getNombre() + "</b><br>" + formacion.descripcion() + "</div></html>");
        cardsLateral.show(contLateral, "formacion");
    }

    private void reiniciarArmado() {
        once = new Onceideal();
        once.setNombreUsuario(nombreDT);
        jugadoresAlineadosCount = 0;
        for (int i = 0; i < 11; i++) {
            casillerosOcupados[i] = false;
            porCasillero[i] = null;
        }
        txtBuscarJugador.setEnabled(true);
        txtBuscarJugador.setText("");
        comboPosicionesLibres.setEnabled(true);
        btnOtroClub.setVisible(false);
        btnPartido.setVisible(false);
    }

    public void iniciar(String nombreDT) {
        this.nombreDT = nombreDT;
        mostrarElegirFormacion();
    }

    private void siguienteTurno() {
        lblError.setText(" ");
        txtBuscarJugador.setText("");
        popupSugerencias.setVisible(false);

        if (jugadoresAlineadosCount >= 11) {
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
                if (!casillerosOcupados[i] && Formacion.sirve(j, formacion.casillero(i))) return true;
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

            String posicionSeleccionada = formacion.casillero(posIndex);

            if (!Formacion.sirve(elegido, posicionSeleccionada)) {
                throw new PosicionInvalidaException(Ratings.corto(elegido) + " no puede jugar en la posición " + posicionSeleccionada + ".");
            }

            service.agregarJugadorAOnce(once, elegido, clubTurno, posicionSeleccionada);
            
            casillerosOcupados[posIndex] = true;
            porCasillero[posIndex] = elegido;
            cancha.setNombre(posIndex, Ratings.corto(elegido));
            
            jugadoresAlineadosCount++;
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
        v.irAPartido(once, formacion);
    }
}