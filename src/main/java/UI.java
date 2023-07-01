import javax.swing.*;
import java.awt.*;
import com.toedter.calendar.JDateChooser;
import java.time.LocalDate;
import java.time.ZoneId;

public class UI extends JFrame {

    private JDateChooser startDateChooser;
    private JDateChooser endDateChooser;
    private JComboBox<String> carComboBox;
    private JComboBox<String> roomComboBox;
    private JButton confirmButton;

    public UI() {

        setTitle("Auto- und Zimmerauswahl");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);

        // Panel für das Formular erstellen
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(5, 5, 5, 5);

        // Startdatum-Eingabefeld
        JLabel startDateLabel = new JLabel("Startdatum:");
        startDateChooser = new JDateChooser();
        startDateChooser.setPreferredSize(new Dimension(150, startDateChooser.getPreferredSize().height));
        constraints.gridx = 0;
        constraints.gridy = 0;
        formPanel.add(startDateLabel, constraints);
        constraints.gridx = 1;
        formPanel.add(startDateChooser, constraints);

        // Enddatum-Eingabefeld
        JLabel endDateLabel = new JLabel("Enddatum:");
        endDateChooser = new JDateChooser();
        endDateChooser.setPreferredSize(new Dimension(150, endDateChooser.getPreferredSize().height));
        constraints.gridx = 0;
        constraints.gridy = 1;
        formPanel.add(endDateLabel, constraints);
        constraints.gridx = 1;
        formPanel.add(endDateChooser, constraints);

        // Autoauswahl
        JLabel carLabel = new JLabel("Auto:");
        String[] cars = {"Audi", "Mercedes", "Renault", "BMW"};
        carComboBox = new JComboBox<>(cars);
        carComboBox.setEnabled(false);
        constraints.gridx = 0;
        constraints.gridy = 2;
        formPanel.add(carLabel, constraints);
        constraints.gridx = 1;
        formPanel.add(carComboBox, constraints);

        // Zimmerauswahl
        JLabel roomLabel = new JLabel("Hotelzimmer:");
        String[] rooms = {"Zimmer 1", "Zimmer 2", "Zimmer 4", "Zimmer 6"};
        roomComboBox = new JComboBox<>(rooms);
        roomComboBox.setEnabled(false);
        constraints.gridx = 0;
        constraints.gridy = 3;
        formPanel.add(roomLabel, constraints);
        constraints.gridx = 1;
        formPanel.add(roomComboBox, constraints);

        confirmButton = new JButton("Bestätigen");
        confirmButton.setEnabled(false);
        confirmButton.addActionListener(e -> {
            LocalDate startDate = getSelectedDate(startDateChooser);
            LocalDate endDate = getSelectedDate(endDateChooser);
            String selectedCar = (String) carComboBox.getSelectedItem();
            String selectedRoom = (String) roomComboBox.getSelectedItem();

            if (startDate != null && endDate != null) {
                // Hier können Sie die ausgewählten Daten weiterverarbeiten
                System.out.println("Startdatum: " + startDate);
                System.out.println("Enddatum: " + endDate);
                System.out.println("Ausgewähltes Auto: " + selectedCar);
                System.out.println("Ausgewähltes Zimmer: " + selectedRoom);
            } else {
                JOptionPane.showMessageDialog(UI.this, "Bitte wählen Sie ein Startdatum und ein Enddatum aus.");
            }
        });
        constraints.gridx = 1;
        constraints.gridy = 4;
        constraints.anchor = GridBagConstraints.EAST;
        formPanel.add(confirmButton, constraints);
        addInputListeners();

        // Hauptpanel erstellen und das Formular hinzufügen
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(formPanel, BorderLayout.NORTH);

        // Das Hauptpanel zur JFrame hinzufügen
        add(mainPanel);
    }

    private LocalDate getSelectedDate(JDateChooser dateChooser) {
        if (dateChooser.getDate() != null) {
            return dateChooser.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        } else {
            return null;
        }
    }

    private void checkInputFields() {
        LocalDate startDate = getSelectedDate(startDateChooser);
        LocalDate endDate = getSelectedDate(endDateChooser);

        if (startDate != null && endDate != null) {
            carComboBox.setEnabled(true);
            roomComboBox.setEnabled(true);
            confirmButton.setEnabled(true);
        } else {
            carComboBox.setEnabled(false);
            roomComboBox.setEnabled(false);
            confirmButton.setEnabled(false);
        }
    }

    private void addInputListeners() {
        startDateChooser.addPropertyChangeListener("date", e -> checkInputFields());
        endDateChooser.addPropertyChangeListener("date", e -> checkInputFields());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UI ui = new UI();
            ui.setVisible(true);
        });
    }
}
