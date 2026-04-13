// Import project-specific classes
package cinema.panels;

// Import project-specific classes
import cinema.database.DatabaseConnection;

// Import necessary libraries for GUI (Swing/AWT) and Database (SQL)
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;


public class ScreeningsPanel extends JPanel {

    public ScreeningsPanel() {
        setLayout(new BorderLayout());

        // Define the columns for the JTable
        String[] columns = {"id", "movie", "hall", "start_time", "price"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);

        // Create the buttons
        JButton refresh = new JButton("Refresh");
        JButton addBtn = new JButton("Add");
        JButton editBtn = new JButton("Edit");
        JButton deleteBtn = new JButton("Delete");

        // SQL for refreshing data with INNER JOIN to fetch related data
        String sql =
                "SELECT s.id, m.title, h.name, s.start_time, s.price " +
                        "FROM screenings s " +
                        "INNER JOIN movies m ON s.movie_id = m.id " +
                        "INNER JOIN halls h ON s.hall_id = h.id";

        // Refresh button action: loads the data from the database
        refresh.addActionListener(e -> {
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                model.setRowCount(0);  // Clear the table

                while (rs.next()) {
                    model.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("name"),
                            rs.getTimestamp("start_time"),
                            rs.getDouble("price")
                    });
                }

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        // Add button action: opens a dialog to add a new screening
        addBtn.addActionListener(e -> {
            JTextField movieField = new JTextField();
            JTextField hallField = new JTextField();
            JTextField priceField = new JTextField();
            JSpinner startTimeSpinner = new JSpinner(new SpinnerDateModel());
            JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(startTimeSpinner, "yyyy-MM-dd HH:mm:ss");
            startTimeSpinner.setEditor(timeEditor);

            Object[] fields = {
                    "Movie:", movieField,
                    "Hall:", hallField,
                    "Start Time:", startTimeSpinner,
                    "Price:", priceField
            };

            int option = JOptionPane.showConfirmDialog(this, fields, "Add Screening", JOptionPane.OK_CANCEL_OPTION);

            if (option == JOptionPane.OK_OPTION) {
                String movie = movieField.getText().trim();
                String hall = hallField.getText().trim();
                String priceStr = priceField.getText().trim();
                java.util.Date startTime = (java.util.Date) startTimeSpinner.getValue();

                // Validate input fields
                if (movie.isEmpty() || hall.isEmpty() || priceStr.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "All fields are required.");
                    return;
                }

                double price;
                try {
                    price = Double.parseDouble(priceStr);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Price must be a valid number.");
                    return;
                }

                try (Connection conn = DatabaseConnection.getConnection()) {
                    String sqlInsert = "INSERT INTO screenings (movie_id, hall_id, start_time, price) VALUES (?, ?, ?, ?)";
                    PreparedStatement pst = conn.prepareStatement(sqlInsert);

                    // Assuming that movie name and hall name uniquely identify the respective IDs
                    int movieId = getMovieIdByName(movie, conn);
                    int hallId = getHallIdByName(hall, conn);

                    if (movieId == -1 || hallId == -1) {
                        JOptionPane.showMessageDialog(this, "Invalid movie or hall.");
                        return;
                    }

                    pst.setInt(1, movieId);
                    pst.setInt(2, hallId);
                    pst.setTimestamp(3, new java.sql.Timestamp(startTime.getTime()));
                    pst.setDouble(4, price);

                    pst.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Screening added successfully!");
                    refresh.doClick();  // Refresh the table
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                }
            }
        });

        // Edit button action: opens a dialog to edit an existing screening
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a row to edit");
                return;
            }

            int id = (int) model.getValueAt(row, 0);
            String movie = (String) model.getValueAt(row, 1);
            String hall = (String) model.getValueAt(row, 2);
            java.util.Date startTime = (java.util.Date) model.getValueAt(row, 3);
            double price = (double) model.getValueAt(row, 4);

            JTextField movieField = new JTextField(movie);
            JTextField hallField = new JTextField(hall);
            JSpinner startTimeSpinner = new JSpinner(new SpinnerDateModel());
            startTimeSpinner.setValue(startTime);
            JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(startTimeSpinner, "yyyy-MM-dd HH:mm:ss");
            startTimeSpinner.setEditor(timeEditor);
            JTextField priceField = new JTextField(String.valueOf(price));

            Object[] fields = {
                    "Movie:", movieField,
                    "Hall:", hallField,
                    "Start Time:", startTimeSpinner,
                    "Price:", priceField
            };

            int option = JOptionPane.showConfirmDialog(this, fields, "Edit Screening", JOptionPane.OK_CANCEL_OPTION);

            if (option == JOptionPane.OK_OPTION) {
                String newMovie = movieField.getText().trim();
                String newHall = hallField.getText().trim();
                String priceStr = priceField.getText().trim();
                java.util.Date newStartTime = (java.util.Date) startTimeSpinner.getValue();

                // Validate input fields
                if (newMovie.isEmpty() || newHall.isEmpty() || priceStr.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "All fields are required.");
                    return;
                }

                double newPrice;
                try {
                    newPrice = Double.parseDouble(priceStr);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Price must be a valid number.");
                    return;
                }

                try (Connection conn = DatabaseConnection.getConnection()) {
                    String sqlUpdate = "UPDATE screenings SET movie_id = ?, hall_id = ?, start_time = ?, price = ? WHERE id = ?";
                    PreparedStatement pst = conn.prepareStatement(sqlUpdate);

                    int movieId = getMovieIdByName(newMovie, conn);
                    int hallId = getHallIdByName(newHall, conn);

                    if (movieId == -1 || hallId == -1) {
                        JOptionPane.showMessageDialog(this, "Invalid movie or hall.");
                        return;
                    }

                    pst.setInt(1, movieId);
                    pst.setInt(2, hallId);
                    pst.setTimestamp(3, new java.sql.Timestamp(newStartTime.getTime()));
                    pst.setDouble(4, newPrice);
                    pst.setInt(5, id);

                    pst.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Screening updated successfully!");
                    refresh.doClick();  // Refresh the table
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                }
            }
        });

        // Delete button action: deletes a screening from the database
        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a row to delete");
                return;
            }

            int id = (int) model.getValueAt(row, 0);

            int option = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this screening?", "Confirm Delete", JOptionPane.YES_NO_OPTION);

            if (option == JOptionPane.YES_OPTION) {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    String sqlDelete = "DELETE FROM screenings WHERE id = ?";
                    PreparedStatement pst = conn.prepareStatement(sqlDelete);
                    pst.setInt(1, id);
                    pst.executeUpdate();

                    JOptionPane.showMessageDialog(this, "Screening deleted successfully!");
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

    private int getMovieIdByName(String movieName, Connection conn) throws SQLException {
        String sql = "SELECT id FROM movies WHERE title = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, movieName);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                } else {
                    return -1;
                }
            }
        }
    }

    private int getHallIdByName(String hallName, Connection conn) throws SQLException {
        String sql = "SELECT id FROM halls WHERE name = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, hallName);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                } else {
                    return -1;
                }
            }
        }
    }
}
