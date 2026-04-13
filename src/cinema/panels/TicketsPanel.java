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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class TicketsPanel extends JPanel {

    private DefaultTableModel model;
    private JTable table;
    private JButton refresh;

    public TicketsPanel() {
        setLayout(new BorderLayout());

        // Table
        String[] columns = {"Ticket ID", "User", "Movie", "Hall", "Time", "Seat", "Price", "Status"};
        model = new DefaultTableModel(columns, 0);
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel();
        refresh = new JButton("Refresh");
        JButton addBtn = new JButton("Add");
        JButton editBtn = new JButton("Edit");
        JButton deleteBtn = new JButton("Delete");

        buttonPanel.add(refresh);
        buttonPanel.add(addBtn);
        buttonPanel.add(editBtn);
        buttonPanel.add(deleteBtn);
        add(buttonPanel, BorderLayout.SOUTH);

        // Actions
        refresh.addActionListener(e -> loadTickets());
        refresh.doClick();

        addBtn.addActionListener(e -> openTicketDialog(null));
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Select a ticket to edit"); return; }
            int ticketId = (int) model.getValueAt(row, 0);
            openTicketDialog(ticketId);
        });
        deleteBtn.addActionListener(e -> deleteTicket());
    }

    private void loadTickets() {
        String sql = "SELECT t.id, u.name AS user_name, m.title AS movie_title, h.name AS hall_name, " +
                "sc.start_time, s.row_number, s.seat_number, sc.price, t.status " +
                "FROM tickets t " +
                "JOIN users u ON t.user_id = u.id " +
                "JOIN screenings sc ON t.screening_id = sc.id " +
                "JOIN movies m ON sc.movie_id = m.id " +
                "JOIN halls h ON sc.hall_id = h.id " +
                "JOIN seats s ON t.seat_id = s.id";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            model.setRowCount(0);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

            while (rs.next()) {
                String seatLabel = "Row " + rs.getInt("row_number") + " Seat " + rs.getInt("seat_number");
                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("user_name"),
                        rs.getString("movie_title"),
                        rs.getString("hall_name"),
                        sdf.format(rs.getTimestamp("start_time")),
                        seatLabel,
                        rs.getDouble("price"),
                        rs.getString("status")
                });
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading tickets: " + ex.getMessage());
        }
    }

    private void openTicketDialog(Integer ticketId) {
        try (Connection conn = DatabaseConnection.getConnection()) {

            // Users
            List<User> users = new ArrayList<>();
            ResultSet rsUsers = conn.createStatement().executeQuery("SELECT id, name FROM users");
            while (rsUsers.next()) users.add(new User(rsUsers.getInt("id"), rsUsers.getString("name")));
            JComboBox<User> userCombo = new JComboBox<>(users.toArray(new User[0]));

            // Screenings
            List<Screening> screenings = new ArrayList<>();
            ResultSet rsScreenings = conn.createStatement().executeQuery(
                    "SELECT sc.id, m.title AS movie_title, h.name AS hall_name, sc.start_time, sc.price, h.id AS hall_id " +
                            "FROM screenings sc " +
                            "JOIN movies m ON sc.movie_id = m.id " +
                            "JOIN halls h ON sc.hall_id = h.id"
            );
            while (rsScreenings.next()) {
                screenings.add(new Screening(
                        rsScreenings.getInt("id"),
                        rsScreenings.getString("movie_title"),
                        rsScreenings.getString("hall_name"),
                        rsScreenings.getTimestamp("start_time"),
                        rsScreenings.getDouble("price"),
                        rsScreenings.getInt("hall_id")
                ));
            }
            JComboBox<Screening> screeningCombo = new JComboBox<>(screenings.toArray(new Screening[0]));

            // Seats dropdown (only available seats)
            JComboBox<Seat> seatCombo = new JComboBox<>();


            Runnable loadSeats = () -> {
                seatCombo.removeAllItems();
                Screening selectedScreening = (Screening) screeningCombo.getSelectedItem();
                if (selectedScreening == null) return;
                try {
                    PreparedStatement pst = conn.prepareStatement(
                            "SELECT s.id, s.row_number, s.seat_number FROM seats s " +
                                    "WHERE s.hall_id = ? AND s.id NOT IN " +
                                    "(SELECT seat_id FROM tickets WHERE screening_id = ? AND status='BOOKED') " +
                                    "ORDER BY s.row_number, s.seat_number"
                    );
                    pst.setInt(1, selectedScreening.hallId);
                    pst.setInt(2, selectedScreening.id);
                    ResultSet rsSeats = pst.executeQuery();
                    while (rsSeats.next()) {
                        seatCombo.addItem(new Seat(rsSeats.getInt("id"),
                                rsSeats.getInt("row_number"), rsSeats.getInt("seat_number")));
                    }
                } catch (SQLException ex) { ex.printStackTrace(); }
            };

            loadSeats.run();
            screeningCombo.addActionListener(e -> loadSeats.run());

            // Pre-fill values if editing
            if (ticketId != null) {
                PreparedStatement pst = conn.prepareStatement("SELECT user_id, screening_id, seat_id FROM tickets WHERE id=?");
                pst.setInt(1, ticketId);
                ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    int userId = rs.getInt("user_id");
                    int screeningId = rs.getInt("screening_id");
                    int seatId = rs.getInt("seat_id");
                    for (int i = 0; i < users.size(); i++) if (users.get(i).id == userId) userCombo.setSelectedIndex(i);
                    for (int i = 0; i < screenings.size(); i++) if (screenings.get(i).id == screeningId) screeningCombo.setSelectedIndex(i);
                    loadSeats.run();
                    for (int i = 0; i < seatCombo.getItemCount(); i++)
                        if (seatCombo.getItemAt(i).id == seatId) seatCombo.setSelectedIndex(i);
                }
            }

            Object[] fields = {"User:", userCombo, "Screening:", screeningCombo, "Seat:", seatCombo};
            int option = JOptionPane.showConfirmDialog(this, fields, ticketId == null ? "Add Ticket" : "Edit Ticket", JOptionPane.OK_CANCEL_OPTION);
            if (option != JOptionPane.OK_OPTION) return;

            User selectedUser = (User) userCombo.getSelectedItem();
            Screening selectedScreening = (Screening) screeningCombo.getSelectedItem();
            Seat selectedSeat = (Seat) seatCombo.getSelectedItem();
            if (selectedSeat == null) { JOptionPane.showMessageDialog(this, "No seat selected"); return; }

            if (ticketId == null) {
                PreparedStatement pst = conn.prepareStatement(
                        "INSERT INTO tickets (user_id, screening_id, seat_id, booked_at, status) VALUES (?, ?, ?, NOW(), 'BOOKED')"
                );
                pst.setInt(1, selectedUser.id);
                pst.setInt(2, selectedScreening.id);
                pst.setInt(3, selectedSeat.id);
                pst.executeUpdate();
            } else {
                PreparedStatement pst = conn.prepareStatement(
                        "UPDATE tickets SET user_id=?, screening_id=?, seat_id=? WHERE id=?"
                );
                pst.setInt(1, selectedUser.id);
                pst.setInt(2, selectedScreening.id);
                pst.setInt(3, selectedSeat.id);
                pst.setInt(4, ticketId);
                pst.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "Ticket saved!");
            refresh.doClick();

        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private void deleteTicket() {
        int row = table.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select a ticket"); return; }
        int ticketId = (int) model.getValueAt(row, 0);
        int option = JOptionPane.showConfirmDialog(this, "Delete ticket?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (option != JOptionPane.YES_OPTION) return;
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement pst = conn.prepareStatement("DELETE FROM tickets WHERE id=?");
            pst.setInt(1, ticketId);
            pst.executeUpdate();
            refresh.doClick();
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    // --- Helper classes ---
    private static class User { int id; String name; User(int id,String name){this.id=id;this.name=name;} public String toString(){return name;} }
    private static class Screening { int id; String movieTitle,hallName; Timestamp startTime; double price; int hallId; Screening(int id,String movieTitle,String hallName,Timestamp startTime,double price,int hallId){this.id=id;this.movieTitle=movieTitle;this.hallName=hallName;this.startTime=startTime;this.price=price;this.hallId=hallId;} public String toString(){return movieTitle+" - "+hallName+" - "+startTime;} }
    private static class Seat { int id,row,number; Seat(int id,int row,int number){this.id=id;this.row=row;this.number=number;} public String toString(){return "Row "+row+" Seat "+number;} }
}