import javax.swing.*;
import javax.xml.crypto.Data;
import java.awt.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.toedter.calendar.JDateChooser;
import org.utils.*;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UI extends JFrame {

    private JDateChooser startDateChooser;
    private JDateChooser endDateChooser;
    private JComboBox<Integer> carComboBox;
    private JComboBox<Integer> roomComboBox;
    private JButton confirmButton;

    private ObjectMapper objectMapper = new ObjectMapper();

    private Boolean carEnabled = false;

    private Boolean roomEnabled = false;

    private int myPort;

    private int travelBrokerPort;

    private final static Logger LOGGER = Logger.getLogger(UI.class.getName());

    public UI(int travelBrokerPort) {
        int[] possiblePorts = {5000, 5001, 5002, 5003, 5004};
        boolean portSet = false;
        for (int i = 0; i < possiblePorts.length; i++){
            try{
                myPort = possiblePorts[i];
                LOGGER.log(Level.INFO, "UI Port now set on: " + possiblePorts[i]);
                portSet = true;
                break;
            }catch(Exception e){
                LOGGER.log(Level.INFO, "Port " + possiblePorts[i] + "already taken, trying next!");
            }
        }

        if(!portSet){
            LOGGER.log(Level.SEVERE, "UI didn't find any open Port! Program will now shut down.");
            System.exit(0);
        }

        //store the port of corresponding travelBroker instance
        this.travelBrokerPort = travelBrokerPort;

        objectMapper.registerModule(new JavaTimeModule());

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
        Integer[] cars = {};
        carComboBox = new JComboBox<>(cars);
        carComboBox.setEnabled(false);
        constraints.gridx = 0;
        constraints.gridy = 2;
        formPanel.add(carLabel, constraints);
        constraints.gridx = 1;
        formPanel.add(carComboBox, constraints);

        // Zimmerauswahl
        JLabel roomLabel = new JLabel("Hotelzimmer:");
        Integer[] rooms = {};
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
            //AvailabilityData data = new AvailabilityData(LocalDate.of(2023,4,2), LocalDate.of(2023,4,10));
            byte[] parsedData;

            LocalDate startDate = getSelectedDate(startDateChooser);
            LocalDate endDate = getSelectedDate(endDateChooser);
            int selectedCar = (int) carComboBox.getSelectedItem();
            int selectedRoom = (int) roomComboBox.getSelectedItem();


            //store data in wrapperclass to send it better
            BookingData bookingData = new BookingData(startDate, endDate, selectedCar, selectedRoom);
            try(DatagramSocket dgSocket = new DatagramSocket(Participant.uiPort)){
            parsedData = objectMapper.writeValueAsBytes(bookingData);
            UDPMessage message = new UDPMessage(UUID.randomUUID(), parsedData, SendingInformation.UI, Operations.PREPARE, Participant.uiPort);
            parsedData = objectMapper.writeValueAsBytes(message);

            DatagramPacket dgOut = new DatagramPacket(parsedData, parsedData.length, Participant.localhost, travelBrokerPort);

            dgSocket.send(dgOut);

            }catch (Exception ex){
                LOGGER.log(Level.SEVERE, "There was an error with sending the package", ex);
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

            AvailabilityData availabilityData = new AvailabilityData(startDate, endDate, false, false);

            try(DatagramSocket dgSocket = new DatagramSocket(Participant.uiPort)){
                UUID randomUUID = UUID.randomUUID();
                byte[] parsedAvailability = objectMapper.writeValueAsBytes(availabilityData);
                UDPMessage message = new UDPMessage(randomUUID, parsedAvailability, SendingInformation.UI, Operations.AVAILIBILITY, Participant.uiPort);

                byte[] parsedMessage = objectMapper.writeValueAsBytes(message);
                DatagramPacket dgOut = new DatagramPacket(parsedMessage, parsedMessage.length, Participant.localhost, travelBrokerPort);

                dgSocket.send(dgOut);

                byte[] buffer = new byte[65507];
                DatagramPacket dgIn = new DatagramPacket(buffer, buffer.length);
                for (int i = 0; i < 4; i++) {
                    //blocking for 5 sec two times
                    dgSocket.setSoTimeout(5000);
                    try{
                        dgSocket.receive(dgIn);
                        String data = new String(dgIn.getData(), 0, dgIn.getLength());
                        UDPMessage dataObject = objectMapper.readValue(data, UDPMessage.class);
                        byte[] messageData = dataObject.getData();
                        data = new String(messageData, 0, messageData.length);

                        if(dataObject.getSender() == SendingInformation.RENTALCAR){
                            ArrayList<Car> availableCars = objectMapper.readValue(data, new TypeReference<ArrayList<Car>>() {});
                            carEnabled = true;
                            carComboBox.removeAllItems();
                            for(int l = 0; l < availableCars.size(); l++)
                                carComboBox.addItem(availableCars.get(l).getId());
                            carComboBox.setEnabled(true);
                        } else if (dataObject.getSender() == SendingInformation.HOTEL) {
                            ArrayList<Room> availableRooms = objectMapper.readValue(data,new TypeReference<ArrayList<Room>>() {});
                            roomEnabled = true;
                            roomComboBox.removeAllItems();
                            for(int n = 0; n < availableRooms.size(); n++)
                                roomComboBox.addItem(availableRooms.get(n).getId());
                            roomComboBox.setEnabled(true);
                        }if(carEnabled && roomEnabled){
                            confirmButton.setEnabled(true);
                            break;
                        }


                    }catch (SocketTimeoutException ste){
                        AvailabilityData availabilityResend = null;
                        if(i < 3){
                            if(!carEnabled && !roomEnabled){
                                dgSocket.send(dgOut);
                            }else {
                                if(!carEnabled){
                                    availabilityResend = new AvailabilityData(startDate, endDate, false, true);
                                }
                                if (!roomEnabled){
                                    availabilityResend = new AvailabilityData(startDate, endDate, true, false);
                                }
                                parsedAvailability = objectMapper.writeValueAsBytes(availabilityResend);
                                message = new UDPMessage(randomUUID, parsedAvailability, SendingInformation.UI, Operations.AVAILIBILITY, Participant.uiPort);
                                parsedMessage = objectMapper.writeValueAsBytes(message);
                                DatagramPacket dgResend = new DatagramPacket(parsedMessage, parsedMessage.length, Participant.localhost, travelBrokerPort);
                                dgSocket.send(dgResend);
                            }
                        }else {
                            LOGGER.log(Level.SEVERE, "The services aren't responding to the availability");
                        }
                    }
                }
            }catch(Exception e){
                LOGGER.log(Level.SEVERE, "An Exception occured", e);
            }

        } else {
            /*carComboBox.setEnabled(false);
            roomComboBox.setEnabled(false);
            confirmButton.setEnabled(false);*/
        }
    }

    private void addInputListeners() {
        startDateChooser.addPropertyChangeListener("date", e -> checkInputFields());
        endDateChooser.addPropertyChangeListener("date", e -> checkInputFields());
    }

    public static void main(String[] args) {
        /*SwingUtilities.invokeLater(() -> {
            UI ui = new UI();
            ui.setVisible(true);
        });
        ObjectMapper objectMapper2 = new ObjectMapper();
        while(true){
            try (DatagramSocket dgSocket = new DatagramSocket(travelBrokerPort)) {
                byte[] buffer = new byte[65507];
                DatagramPacket dgPacket = new DatagramPacket(buffer, buffer.length);
                System.out.println("Listening on Port "+ Participant.travelBrokerPort);
                dgSocket.receive(dgPacket);
                String data = new String(dgPacket.getData(), 0, dgPacket.getLength());
                UDPMessage dataObject = objectMapper2.readValue(data, UDPMessage.class);
                if(dataObject.getOperation()== Operations.AVAILIBILITY){
                    System.out.println("Lets Goooooo!");
                }
            }catch(Exception e){}

        }*/
    }

}
