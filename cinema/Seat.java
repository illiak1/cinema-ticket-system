package cinema;

/**
 * The Seat class represents a single seat in a cinema hall.
 * It contains information about the seat's position, type,
 * and booking status.
 */
public class Seat {
    private int id;
    private int hallId;
    private int rowNumber;
    private int seatNumber;
    private String seatType;
    private boolean isBooked;  

    /**
     * Constructor to initialize a Seat object with all properties.
     *
     * @param id         unique seat ID
     * @param hallId     hall ID where the seat belongs
     * @param rowNumber  row number of the seat
     * @param seatNumber seat number within the row
     * @param seatType   type/category of the seat
     * @param isBooked   booking status of the seat
     */
    public Seat(int id, int hallId, int rowNumber, int seatNumber, String seatType, boolean isBooked) {
        this.id = id;
        this.hallId = hallId;
        this.rowNumber = rowNumber;
        this.seatNumber = seatNumber;
        this.seatType = seatType;
        this.isBooked = isBooked;  
    }

    // Getters and setters
    public boolean isBooked() {
        return isBooked;
    }

    public void setBooked(boolean booked) {
        isBooked = booked;
    }

    // Other getters and setters...


    public int getId() {
        return id;
    }

    public int getHallId() {
        return hallId;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public String getSeatType() {
        return seatType;
    }

    //Returns a string representation of the Seat object
    @Override
    public String toString() {
        return "Seat{" +
                "id=" + id +
                ", hallId=" + hallId +
                ", rowNumber=" + rowNumber +
                ", seatNumber=" + seatNumber +
                ", seatType='" + seatType + '\'' +
                '}';
    }
}
