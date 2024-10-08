package gestionmaterias;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class Main extends JFrame {

    private JTextField nombreField;
    private JComboBox<Integer> nivelComboBox;
    private JButton agregarButton;
    private JButton eliminarButton;
    private JTable materiasTable;
    private DefaultTableModel tableModel;

    // Detalles de conexion a Clever Cloud
    private final String url = "jdbc:postgresql://bvaybuhhac35nixbooh2-postgresql.services.clever-cloud.com:50013/bvaybuhhac35nixbooh2";
    private final String user = "u7qloxalfzchqq1ti1lv"; 
    private final String password = "mDk6jP1d4wU4SZP0T2AJUCYOG1QYJz";

    public Main() {
        super("Gestión de Materias");

        setLayout(new BorderLayout());

        crearTablaMaterias();

        JPanel agregarPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel nombreLabel = new JLabel("Nombre de la Materia:");
        nombreField = new JTextField(20);

        gbc.insets = new Insets(10, 10, 5, 10);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        agregarPanel.add(nombreLabel, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        agregarPanel.add(nombreField, gbc);

        JLabel nivelLabel = new JLabel("Nivel de Complejidad:");
        Integer[] niveles = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        nivelComboBox = new JComboBox<>(niveles);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        agregarPanel.add(nivelLabel, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        agregarPanel.add(nivelComboBox, gbc);

        // Botón para agregar materia
        agregarButton = new JButton("Agregar Materia");

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(15, 10, 10, 10);
        agregarPanel.add(agregarButton, gbc);

        agregarButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                agregarMateria();
            }
        });

        JPanel tablaPanel = new JPanel(new BorderLayout());
        tableModel = new DefaultTableModel(new Object[]{"ID", "Nombre", "Nivel de Complejidad"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        materiasTable = new JTable(tableModel);
        materiasTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(materiasTable);
        tablaPanel.add(scrollPane, BorderLayout.CENTER);

        eliminarButton = new JButton("Eliminar Materia");
        eliminarButton.setEnabled(false);

        eliminarButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                eliminarMateria();
            }
        });

        materiasTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && materiasTable.getSelectedRow() != -1) {
                eliminarButton.setEnabled(true);
            } else {
                eliminarButton.setEnabled(false);
            }
        });

        JPanel botonEliminarPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botonEliminarPanel.add(eliminarButton);
        tablaPanel.add(botonEliminarPanel, BorderLayout.SOUTH);

        add(agregarPanel, BorderLayout.NORTH);
        add(tablaPanel, BorderLayout.CENTER);

        cargarMaterias();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void crearTablaMaterias() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS materias (" +
                                "id SERIAL PRIMARY KEY, " +
                                "nombre VARCHAR(100) NOT NULL, " +
                                "nivel_complejidad INTEGER NOT NULL)";
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("Tabla 'materias' creada o ya existente.");
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al crear la tabla 'materias':\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void agregarMateria() {
        String nombre = nombreField.getText().trim();
        int nivel = (Integer) nivelComboBox.getSelectedItem();

        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre de la materia no puede estar vacío.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (obtenerNumeroMaterias() >= 10) {
            JOptionPane.showMessageDialog(this, "Se ha alcanzado el límite de 10 materias.", "Límite Alcanzado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String insertSQL = "INSERT INTO materias (nombre, nivel_complejidad) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            pstmt.setString(1, nombre);
            pstmt.setInt(2, nivel);

            int filasAfectadas = pstmt.executeUpdate();

            if (filasAfectadas > 0) {
                JOptionPane.showMessageDialog(this, "Materia agregada exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                nombreField.setText("");
                nivelComboBox.setSelectedIndex(0);
                cargarMaterias();
            } else {
                JOptionPane.showMessageDialog(this, "No se pudo agregar la materia.", "Error", JOptionPane.ERROR_MESSAGE);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al conectar con la base de datos:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarMateria() {
        int filaSeleccionada = materiasTable.getSelectedRow();
        if (filaSeleccionada == -1) {
            JOptionPane.showMessageDialog(this, "Por favor, selecciona una materia para eliminar.", "Sin Selección", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirmacion = JOptionPane.showConfirmDialog(this, "¿Estás seguro de que deseas eliminar esta materia?", "Confirmar Eliminación", JOptionPane.YES_NO_OPTION);
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        int id = (Integer) tableModel.getValueAt(filaSeleccionada, 0);

        String deleteSQL = "DELETE FROM materias WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {

            pstmt.setInt(1, id);

            int filasAfectadas = pstmt.executeUpdate();

            if (filasAfectadas > 0) {
                JOptionPane.showMessageDialog(this, "Materia eliminada exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                cargarMaterias();
            } else {
                JOptionPane.showMessageDialog(this, "No se pudo eliminar la materia.", "Error", JOptionPane.ERROR_MESSAGE);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al conectar con la base de datos:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarMaterias() {
        tableModel.setRowCount(0);

        String selectSQL = "SELECT id, nombre, nivel_complejidad FROM materias ORDER BY nivel_complejidad ASC";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSQL)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String nombre = rs.getString("nombre");
                int nivel = rs.getInt("nivel_complejidad");

                tableModel.addRow(new Object[]{id, nombre, nivel});
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al cargar las materias:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int obtenerNumeroMaterias() {
        String countSQL = "SELECT COUNT(*) FROM materias";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSQL)) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al contar las materias:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        return 0;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main());
    }
}
