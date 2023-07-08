import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.std.CollectionSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.utils.*;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TravelBroker {
    public static List<Room> availableRooms = Collections.synchronizedList(new ArrayList<Room>());

    List<Car> availableCars = (ArrayList<Car>) Collections.synchronizedList(new ArrayList<Car>());

    public static void getAvailableItems(int i) {

    }


    public void prepare() {

    }



    public static void main(String[] args) {
        ThreadUI t = new ThreadUI();
        t.start();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        while (true) {
            try (DatagramSocket dgSocket = new DatagramSocket(Participant.travelBrokerPort)) {
                byte[] buffer = new byte[65507];
                //DatagramPacket for recieving data
                DatagramPacket dgPacketIn = new DatagramPacket(buffer, buffer.length);

                System.out.println("Listening on Port " + Participant.travelBrokerPort);
                dgSocket.receive(dgPacketIn);
                String data = new String(dgPacketIn.getData(), 0, dgPacketIn.getLength());
                UDPMessage dataObject = objectMapper.readValue(data, UDPMessage.class);

                switch (dataObject.getOperation()) {
                    case PREPARE -> {

                    }
                    case COMMIT -> {

                    }
                    case ABORT -> {

                    }
                    case READY -> {

                    }
                    case AVAILIBILITY -> {
                        byte[] parsedMessage;
                        if (dataObject.getSender() == SendingInformation.UI) {
                            dataObject.setSender(SendingInformation.TRAVELBROKER);
                            parsedMessage = objectMapper.writeValueAsBytes(dataObject);
                            DatagramPacket dgOutHotel = new DatagramPacket(parsedMessage, parsedMessage.length, Participant.localhost, Participant.hotelPort);

                            dgSocket.send(dgOutHotel);
                        } else if (dataObject.getSender() == SendingInformation.HOTEL) {
                            parsedMessage = objectMapper.writeValueAsBytes(dataObject);
                            DatagramPacket dgOutUI = new DatagramPacket(parsedMessage, parsedMessage.length, Participant.localhost, Participant.uiPort);
                            System.out.println("wir seeeeendeeeen");
                            dgSocket.send(dgOutUI);
                        }
                    }
                }

            } catch (Exception e) {
                System.out.println("tot, weil: " + e);
            }
        }
    }
}
