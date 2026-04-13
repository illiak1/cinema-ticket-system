// Package declaration
package cinema.panels;

// Import project-specific classes
import cinema.database.DatabaseConnection;
import cinema.exception.InputValidator;
import cinema.exception.InvalidInputException;

// Import necessary libraries for GUI (Swing/AWT), Database (SQL) and Date
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Date;

public class MoviesPanel extends JPanel {

    public MoviesPanel() {
        setLayout(new BorderLayout());

        // Define table columns
        String[] columns = {"id", "title", "description", "duration_minutes", "rating", "release_date", "image_path"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);

        // Create buttons for refresh, add, edit, delete
        JButton refresh = new JButton("Refresh");
        JButton addBtn = new JButton("Add");
        JButton editBtn = new JButton("Edit");
        JButton deleteBtn = new JButton("Delete");

        // Refresh button action: loads the data from the database
        refresh.addActionListener(e -> {
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM movies")) {

                model.setRowCount(0);  // Clear the table

                while (rs.next()) {
                    model.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getInt("duration_minutes"),
                            rs.getDouble("rating"),
                            rs.getDate("release_date"),
                            rs.getString("image_path")
                    });
                }

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        // Call refresh action on panel load to populate the table initially
        refresh.doClick(); // This triggers the refresh action immediately after the panel is shown

        // Add button action: opens a dialog to add a new movie
        addBtn.addActionListener(e -> {
            JTextField titleField = new JTextField();
            JTextArea descriptionArea = new JTextArea();
            JTextField durationField = new JTextField();
            JTextField ratingField = new JTextField();
            JTextField imagePathField = new JTextField();

            // Using JSpinner for date input (SpinnerDateModel)
            SpinnerDateModel dateModel = new SpinnerDateModel();
            JSpinner dateSpinner = new JSpinner(dateModel);
            JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd");
            dateSpinner.setEditor(dateEditor);

            JScrollPane descriptionScrollPane = new JScrollPane(descriptionArea);
            descriptionArea.setPreferredSize(new Dimension(200, 100));  // Adjust as needed

            Object[] fields = {
                    "Title:", titleField,
                    "Description:", descriptionScrollPane,
                    "Duration (minutes):", durationField,
                    "Rating:", ratingField,
                    "Release Date:", dateSpinner,
                    "Image Path:", imagePathField
            };

            int option = JOptionPane.showConfirmDialog(this, fields, "Add Movie", JOptionPane.OK_CANCEL_OPTION);

            if (option == JOptionPane.OK_OPTION) {
                String title = titleField.getText().trim();
                String description = descriptionArea.getText().trim();
                String durationStr = durationField.getText().trim();
                String ratingStr = ratingField.getText().trim();
                String imagePath = imagePathField.getText().trim();

                java.util.Date releaseDate = (Date) dateSpinner.getValue();  // Get the selected date
                try {
                    InputValidator.validateReleaseDate(releaseDate);
                    InputValidator.validateNonEmpty(title, "Title");
                    InputValidator.validateNonEmpty(description, "Description");
                    InputValidator.validateNonEmpty(durationStr, "Duration");
                    InputValidator.validateNonEmpty(ratingStr, "Rating");
                    InputValidator.validateNonEmpty(imagePath, "Image Path");
                    InputValidator.validatePositiveInteger(durationStr); // Validate duration
                    InputValidator.validateRating(ratingStr); // Validate rating

                } catch (InvalidInputException ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage());
                    return;
                }

                // Convert java.util.Date to java.sql.Date
                java.sql.Date sqlReleaseDate = new java.sql.Date(releaseDate.getTime());

                try (Connection conn = DatabaseConnection.getConnection()) {
                    String sql = "INSERT INTO movies (title, description, duration_minutes, rating, release_date, image_path) VALUES (?, ?, ?, ?, ?, ?)";
                    PreparedStatement pst = conn.prepareStatement(sql);
                    pst.setString(1, title);
                    pst.setString(2, description);
                    pst.setInt(3, Integer.parseInt(durationStr));
                    pst.setDouble(4, Double.parseDouble(ratingStr));
                    pst.setDate(5, sqlReleaseDate);  // Use the converted java.sql.Date
                    pst.setString(6, imagePath);

                    pst.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Movie added successfully!");
                    refresh.doClick();  // Refresh the table after adding a movie
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                }
            }
        });

        // Edit button action: opens a dialog to edit the selected movie
        editBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) {
                int movieId = (int) model.getValueAt(selectedRow, 0);

                JTextField titleField = new JTextField((String) model.getValueAt(selectedRow, 1));
                JTextArea descriptionArea = new JTextArea((String) model.getValueAt(selectedRow, 2));
                JTextField durationField = new JTextField(String.valueOf(model.getValueAt(selectedRow, 3)));
                JTextField ratingField = new JTextField(String.valueOf(model.getValueAt(selectedRow, 4)));
                JTextField releaseDateField = new JTextField(String.valueOf(model.getValueAt(selectedRow, 5)));
                JTextField imagePathField = new JTextField((String) model.getValueAt(selectedRow, 6));

                Object[] fields = {
                        "Title:", titleField,
                        "Description:", descriptionArea,
                        "Duration (minutes):", durationField,
                        "Rating:", ratingField,
                        "Release Date (YYYY-MM-DD):", releaseDateField,
                        "Image Path:", imagePathField
                };

                int option = JOptionPane.showConfirmDialog(this, fields, "Edit Movie", JOptionPane.OK_CANCEL_OPTION);

                if (option == JOptionPane.OK_OPTION) {
                    String title = titleField.getText().trim();
                    String description = descriptionArea.getText().trim();
                    String durationStr = durationField.getText().trim();
                    String ratingStr = ratingField.getText().trim();
                    String releaseDateStr = releaseDateField.getText().trim();
                    String imagePath = imagePathField.getText().trim();

                    try {
                        InputValidator.validateNonEmpty(title, "Title");
                        InputValidator.validateNonEmpty(description, "Description");
                        InputValidator.validateNonEmpty(durationStr, "Duration");
                        InputValidator.validateNonEmpty(ratingStr, "Rating");
                        InputValidator.validateNonEmpty(imagePath, "Image Path");
                        InputValidator.validatePositiveInteger(durationStr);
                        InputValidator.validateRating(ratingStr);
                    } catch (InvalidInputException ex) {
                        JOptionPane.showMessageDialog(this, ex.getMessage());
                        return;
                    }

                    // Update the movie in the database
                    try (Connection conn = DatabaseConnection.getConnection()) {
                        String sql = "UPDATE movies SET title = ?, description = ?, duration_minutes = ?, rating = ?, release_date = ?, image_path = ? WHERE id = ?";
                        PreparedStatement pst = conn.prepareStatement(sql);
                        pst.setString(1, title);
                        pst.setString(2, description);
                        pst.setInt(3, Integer.parseInt(durationStr));
                        pst.setDouble(4, Double.parseDouble(ratingStr));
                        pst.setDate(5, java.sql.Date.valueOf(releaseDateStr));
                        pst.setString(6, imagePath);
                        pst.setInt(7, movieId);

                        pst.executeUpdate();
                        JOptionPane.showMessageDialog(this, "Movie updated successfully!");
                        refresh.doClick();  // Refresh the table after editing a movie
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a movie to edit.");
            }
        });

        // Delete button action: deletes the selected movie
        deleteBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) {
                int movieId = (int) model.getValueAt(selectedRow, 0);

                int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this movie?", "Delete Movie", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    try (Connection conn = DatabaseConnection.getConnection()) {
                        String sql = "DELETE FROM movies WHERE id = ?";
                        PreparedStatement pst = conn.prepareStatement(sql);
                        pst.setInt(1, movieId);
                        pst.executeUpdate();
                        JOptionPane.showMessageDialog(this, "Movie deleted successfully!");
                        refresh.doClick();  // Refresh the table after deleting a movie
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a movie to delete.");
            }
        });

        // Add the components to the panel
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(refresh);
        buttonPanel.add(addBtn);
        buttonPanel.add(editBtn);
        buttonPanel.add(deleteBtn);

        add(buttonPanel, BorderLayout.SOUTH);
    }
}
