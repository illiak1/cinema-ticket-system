// Package declaration
package cinema.panels;

// Import project-specific classes
import cinema.database.DatabaseConnection;

// Import necessary libraries for GUI (Swing/AWT) and Database (SQL)
// and utility classes for data collection
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class SeatsPanel extends JPanel {

    public SeatsPanel() {
        setLayout(new BorderLayout());

        // Define the columns for the JTable
        String[] columns = {"id", "hall_id", "row", "seat", "type"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);

        // Create the buttons
        JButton refresh = new JButton("Refresh");
        JButton addBtn = new JButton("Add");
        JButton editBtn = new JButton("Edit");
        JButton deleteBtn = new JButton("Delete");

        // SQL for refreshing data
        String sql = "SELECT * FROM seats";

        // Refresh button action: loads the data from the database
        refresh.addActionListener(e -> {
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                model.setRowCount(0);  // Clear the table

                while (rs.next()) {
                    model.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getInt("hall_id"),
                            rs.getInt("row_number"),
                            rs.getInt("seat_number"),
                            rs.getString("seat_type")
                    });
                }

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        // Add button action: opens a dialog to add a new seat
        addBtn.addActionListener(e -> {
            JTextField hallField = new JTextField();
            JTextField rowField = new JTextField();
            JTextField seatField = new JTextField();
            JTextField typeField = new JTextField();

            Object[] fields = {
                    "Hall ID:", hallField,
                    "Row:", rowField,
                    "Seat Number:", seatField,
                    "Seat Type:", typeField
            };

            int option = JOptionPane.showConfirmDialog(this, fields, "Add Seat", JOptionPane.OK_CANCEL_OPTION);

            if (option == JOptionPane.OK_OPTION) {
                String hallIdStr = hallField.getText().trim();
                String rowStr = rowField.getText().trim();
                String seatStr = seatField.getText().trim();
                String type = typeField.getText().trim();

                // Validate input fields
                if (hallIdStr.isEmpty() || rowStr.isEmpty() || seatStr.isEmpty() || type.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "All fields are required.");
                    return;
                }

                int hallId, row, seat;
                try {
                    hallId = Integer.parseInt(hallIdStr);
                    row = Integer.parseInt(rowStr);
                    seat = Integer.parseInt(seatStr);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Hall ID, Row, and Seat Number must be valid integers.");
                    return;
                }

                try (Connection conn = DatabaseConnection.getConnection()) {
                    String sqlInsert = "INSERT INTO seats (hall_id, row_number, seat_number, seat_type) VALUES (?, ?, ?, ?)";
                    PreparedStatement pst = conn.prepareStatement(sqlInsert);
                    pst.setInt(1, hallId);
                    pst.setInt(2, row);
                    pst.setInt(3, seat);
                    pst.setString(4, type);

                    pst.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Seat added successfully!");
                    refresh.doClick();  // Refresh the table
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                }
            }
        });

        // Edit button action: opens a dialog to edit an existing seat
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a row to edit");
                return;
            }

            int id = (int) model.getValueAt(row, 0);
            int hallId = (int) model.getValueAt(row, 1);
            int rowNumber = (int) model.getValueAt(row, 2);
            int seatNumber = (int) model.getValueAt(row, 3);
            String type = (String) model.getValueAt(row, 4);

            JTextField hallField = new JTextField(String.valueOf(hallId));
            JTextField rowField = new JTextField(String.valueOf(rowNumber));
            JTextField seatField = new JTextField(String.valueOf(seatNumber));
            JTextField typeField = new JTextField(type);

            Object[] fields = {
                    "Hall ID:", hallField,
                    "Row:", rowField,
                    "Seat Number:", seatField,
                    "Seat Type:", typeField
            };

            int option = JOptionPane.showConfirmDialog(this, fields, "Edit Seat", JOptionPane.OK_CANCEL_OPTION);

            if (option == JOptionPane.OK_OPTION) {
                int newHallId, newRow, newSeat;
                String newType = typeField.getText().trim();

                // Validate input fields
                if (newType.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Seat type cannot be empty.");
                    return;
                }

                try {
                    newHallId = Integer.parseInt(hallField.getText().trim());
                    newRow = Integer.parseInt(rowField.getText().trim());
                    newSeat = Integer.parseInt(seatField.getText().trim());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Hall ID, Row, and Seat Number must be valid integers.");
                    return;
                }

                try (Connection conn = DatabaseConnection.getConnection()) {
                    String sqlUpdate = "UPDATE seats SET hall_id = ?, row_number = ?, seat_number = ?, seat_type = ? WHERE id = ?";
                    PreparedStatement pst = conn.prepareStatement(sqlUpdate);
                    pst.setInt(1, newHallId);
                    pst.setInt(2, newRow);
                    pst.setInt(3, newSeat);
                    pst.setString(4, newType);
                    pst.setInt(5, id);

                    pst.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Seat updated successfully!");
                    refresh.doClick();  // Refresh the table
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                }
            }
        });

        // Delete button action: deletes a seat from the database
        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a row to delete");
                return;
            }

            int id = (int) model.getValueAt(row, 0);

            int option = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this seat?", "Confirm Delete", JOptionPane.YES_NO_OPTION);

            if (option == JOptionPane.YES_OPTION) {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    String sqlDelete = "DELETE FROM seats WHERE id = ?";
                    PreparedStatement pst = conn.prepareStatement(sqlDelete);
                    pst.setInt(1, id);
                    pst.executeUpdate();

                    JOptionPane.showMessageDialog(this, "Seat deleted successfully!");
                    refresh.doClick();  // Refresh the table
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                }
            }
        });

        // Layout for the panel
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(refresh);
        buttonPanel.add(addBtn);
        buttonPanel.add(editBtn);
        buttonPanel.add(deleteBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Initial table load
        refresh.doClick();

        // Add the panel to the current panel
        add(panel, BorderLayout.CENTER);
    }
}
