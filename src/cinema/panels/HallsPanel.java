// Package declaration
package cinema.panels;

// Import project-specific classes
import cinema.database.DatabaseConnection;


import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class HallsPanel extends JPanel {

    public HallsPanel() {
        setLayout(new BorderLayout());

        // Define the columns for the JTable
        String[] columns = {"id", "name", "total_seats"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);

        // Create the buttons
        JButton refresh = new JButton("Refresh");
        JButton addBtn = new JButton("Add");
        JButton editBtn = new JButton("Edit");
        JButton deleteBtn = new JButton("Delete");

        // Refresh button action: loads the data from the database
        refresh.addActionListener(e -> {
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM halls")) {

                model.setRowCount(0);  // Clear the table

                while (rs.next()) {
                    model.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getInt("total_seats")
                    });
                }

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        // Add button action: opens a dialog to add a new hall
        addBtn.addActionListener(e -> {
            JTextField nameField = new JTextField();
            JTextField totalSeatsField = new JTextField();

            Object[] fields = {
                    "Name:", nameField,
                    "Total Seats:", totalSeatsField
            };

            int option = JOptionPane.showConfirmDialog(this, fields, "Add Hall", JOptionPane.OK_CANCEL_OPTION);

            if (option == JOptionPane.OK_OPTION) {
                String name = nameField.getText().trim();
                String totalSeatsStr = totalSeatsField.getText().trim();

                // Validate input fields
                if (name.isEmpty() || totalSeatsStr.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "All fields are required.");
                    return;
                }

                int totalSeats;
                try {
                    totalSeats = Integer.parseInt(totalSeatsStr);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Total seats must be a valid number.");
                    return;
                }

                try (Connection conn = DatabaseConnection.getConnection()) {
                    String sql = "INSERT INTO halls (name, total_seats) VALUES (?, ?)";
                    PreparedStatement pst = conn.prepareStatement(sql);
                    pst.setString(1, name);
                    pst.setInt(2, totalSeats);

                    pst.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Hall added successfully!");
                    refresh.doClick();  // Refresh the table
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                }
            }
        });

        // Edit button action: opens a dialog to edit an existing hall
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a row to edit");
                return;
            }

            int id = (int) model.getValueAt(row, 0);
            String name = (String) model.getValueAt(row, 1);
            int totalSeats = (int) model.getValueAt(row, 2);

            JTextField nameField = new JTextField(name);
            JTextField totalSeatsField = new JTextField(String.valueOf(totalSeats));

            Object[] fields = {
                    "Name:", nameField,
                    "Total Seats:", totalSeatsField
            };

            int option = JOptionPane.showConfirmDialog(this, fields, "Edit Hall", JOptionPane.OK_CANCEL_OPTION);

            if (option == JOptionPane.OK_OPTION) {
                String newName = nameField.getText().trim();
                String totalSeatsStr = totalSeatsField.getText().trim();

                // Validate input fields
                if (newName.isEmpty() || totalSeatsStr.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "All fields are required.");
                    return;
                }

                int newTotalSeats;
                try {
                    newTotalSeats = Integer.parseInt(totalSeatsStr);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Total seats must be a valid number.");
                    return;
                }

                try (Connection conn = DatabaseConnection.getConnection()) {
                    String sql = "UPDATE halls SET name = ?, total_seats = ? WHERE id = ?";
                    PreparedStatement pst = conn.prepareStatement(sql);
                    pst.setString(1, newName);
                    pst.setInt(2, newTotalSeats);
                    pst.setInt(3, id);

                    pst.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Hall updated successfully!");
                    refresh.doClick();  // Refresh the table
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                }
            }
        });

        // Delete button action: deletes a hall from the database
        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a row to delete");
                return;
            }

            int id = (int) model.getValueAt(row, 0);

            int option = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this hall?", "Confirm Delete", JOptionPane.YES_NO_OPTION);

            if (option == JOptionPane.YES_OPTION) {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    String sql = "DELETE FROM halls WHERE id = ?";
                    PreparedStatement pst = conn.prepareStatement(sql);
                    pst.setInt(1, id);
                    pst.executeUpdate();

                    JOptionPane.showMessageDialog(this, "Hall deleted successfully!");
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
