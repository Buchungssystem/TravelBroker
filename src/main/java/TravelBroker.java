import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.std.CollectionSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.utils.*;
import org.w3c.dom.html.HTMLOptionElement;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.time.LocalDate;
import java.util.*;

import static com.fasterxml.jackson.databind.type.LogicalType.Map;

public class TravelBroker {
    public static List<Room> availableRooms = Collections.synchronizedList(new ArrayList<Room>());

    List<Car> availableCars = (ArrayList<Car>) Collections.synchronizedList(new ArrayList<Car>());

    public static void getAvailableItems(int i) {

    }


    public void prepare() {

    }



    public static void main(String[] args) {

        //Transaktion Id Context:
        /* stores TransaktionID as Key with arrayList as value
        * arrayList contains always 3 fields: 0: Statement, 1: Car Recieved?, 2: Room Recieved?
        * 1, 2: '' for no answer || COMMIT or ABORT as answer
        *
        *
        * if global Commit or Abort set Statement to Commit or Abort
        * waiting for every P to send back OK
        *
        * Once OK in 1 and 2 of arrayList -> delete entry - Transaktion finished
        * */

        Map<UUID, ArrayList<String>> transaktionContext = new HashMap<UUID, ArrayList<String>>();


        ThreadUI t = new ThreadUI();
        t.start();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        while (true) {
            try (DatagramSocket dgSocket = new DatagramSocket(Participant.travelBrokerPort)) {

                //buffer for recieving data
                byte[] buffer = new byte[65507];
                //DatagramPacket for recieving data
                DatagramPacket dgPacketIn = new DatagramPacket(buffer, buffer.length);
                System.out.println("Listening on Port " + Participant.travelBrokerPort);

                //recieving and parsing UDP Message
                dgSocket.receive(dgPacketIn);
                String data = new String(dgPacketIn.getData(), 0, dgPacketIn.getLength());
                UDPMessage dataObject = objectMapper.readValue(data, UDPMessage.class);

                //parsed Message for sending data
                byte[] parsedMessage;



                switch (dataObject.getOperation()) {
                    case PREPARE -> {
                        System.out.println("prepare send!");
                        if (dataObject.getSender() == SendingInformation.UI) {
                            dataObject.setSender(SendingInformation.TRAVELBROKER);

                            //store TransaktionID for context on further processing
                            UUID transaktionId = dataObject.getTransaktionNumber();
                            transaktionContext.put(transaktionId, new ArrayList<>());
                            // at arrayList(0): Statement
                            transaktionContext.get(transaktionId).add(Operations.PREPARE.toString());
                            // no Car response yet
                            transaktionContext.get(transaktionId).add("");
                            // no Room response yet
                            transaktionContext.get(transaktionId).add("");

                            parsedMessage = objectMapper.writeValueAsBytes(dataObject);
                            DatagramPacket dgOutHotel = new DatagramPacket(parsedMessage, parsedMessage.length, Participant.localhost, Participant.hotelPort);
                            DatagramPacket dgOutCar = new DatagramPacket(parsedMessage, parsedMessage.length, Participant.localhost, Participant.rentalCarPort);
                            dgSocket.send(dgOutHotel);
                            dgSocket.send(dgOutCar);
                        }else {
                            System.out.println("Die Atze kenn ich nich!");
                        }

                    }
                    case ABORT -> {

                        UUID transaktionNumber = dataObject.getTransaktionNumber();
                        transaktionContext.get(transaktionNumber).set(0, Operations.ABORT.toString());

                        if(dataObject.getSender() == SendingInformation.HOTEL){
                            System.out.println("abort recieved - hotel");
                            UDPMessage carResponse = new UDPMessage(transaktionNumber, new byte[0], SendingInformation.TRAVELBROKER, Operations.ABORT);
                            byte[] parsedCarResponse = objectMapper.writeValueAsBytes(carResponse);
                            DatagramPacket dgOutCarAbort = new DatagramPacket(parsedCarResponse, parsedCarResponse.length, Participant.localhost, Participant.rentalCarPort);

                            dgSocket.send(dgOutCarAbort);
                        }else if (dataObject.getSender() == SendingInformation.RENTALCAR){
                            System.out.println("abort recieved - RentalCar");
                            UDPMessage hotelResponse = new UDPMessage(transaktionNumber, new byte[0], SendingInformation.TRAVELBROKER, Operations.ABORT);
                            byte[] parsedHotelResponse = objectMapper.writeValueAsBytes(hotelResponse);
                            DatagramPacket dgOutHotelAbort = new DatagramPacket(parsedHotelResponse, parsedHotelResponse.length, Participant.localhost, Participant.hotelPort);

                            dgSocket.send(dgOutHotelAbort);
                        }
                    }
                    case READY -> {
                        UUID transaktionNumber = dataObject.getTransaktionNumber();
                        if(dataObject.getSender() == SendingInformation.HOTEL){
                            System.out.println("Hotel Ready recieved");
                            transaktionContext.get(transaktionNumber).set(2, Operations.READY.toString());
                        } else if (dataObject.getSender() == SendingInformation.RENTALCAR) {
                            System.out.println("RentalCar Ready recieved");
                            transaktionContext.get(transaktionNumber).set(1, Operations.READY.toString());
                        }

                        String carTransaktionContext = transaktionContext.get(transaktionNumber).get(1);
                        String hotelTransaktionContext = transaktionContext.get(transaktionNumber).get(2);
                        UDPMessage hotelResponse;
                        UDPMessage carResponse;
                        byte[] parsedHotelResponse;
                        byte[] parsedCarResponse;
                        DatagramPacket dgOutHotelReady;
                        DatagramPacket dgOutCarReady;

                        if(carTransaktionContext.equals(Operations.READY.toString()) && hotelTransaktionContext.equals(Operations.READY.toString())){
                            System.out.println("jetzt gehts schon voll rein in den Global Commit");
                            //global Commit
                            transaktionContext.get(transaktionNumber).set(0, Operations.COMMIT.toString());

                            hotelResponse = new UDPMessage(transaktionNumber, new byte[0], SendingInformation.TRAVELBROKER, Operations.COMMIT);
                            carResponse = new UDPMessage(transaktionNumber, new byte[0], SendingInformation.TRAVELBROKER, Operations.COMMIT);

                            parsedHotelResponse = objectMapper.writeValueAsBytes(hotelResponse);
                            parsedCarResponse = objectMapper.writeValueAsBytes(carResponse);

                            dgOutHotelReady = new DatagramPacket(parsedHotelResponse, parsedHotelResponse.length, Participant.localhost, Participant.hotelPort);
                            dgOutCarReady = new DatagramPacket(parsedCarResponse, parsedCarResponse.length, Participant.localhost, Participant.rentalCarPort);

                            System.out.println("sending global commit hotel..");
                            dgSocket.send(dgOutHotelReady);
                            System.out.println(dgOutHotelReady.getPort());
                            System.out.println("sending global commit Car..");
                            dgSocket.send(dgOutCarReady);
                        }

                    }
                    case OK -> {
                        UUID transaktionNumber = dataObject.getTransaktionNumber();
                        if(dataObject.getSender() == SendingInformation.HOTEL){
                            System.out.println("okay hotel");
                            transaktionContext.get(transaktionNumber).set(2, Operations.OK.toString());
                        } else if (dataObject.getSender() == SendingInformation.RENTALCAR) {
                            System.out.println("okay RentalCar");
                            transaktionContext.get(transaktionNumber).set(1, Operations.OK.toString());
                        }
                        String contextCar = transaktionContext.get(transaktionNumber).get(1);
                        String contextRoom = transaktionContext.get(transaktionNumber).get(2);
                        if(contextRoom.equals(Operations.OK.toString()) && contextCar.equals(Operations.OK.toString())){
                            transaktionContext.remove(transaktionNumber);
                            System.out.println("Wie geil ist das denn bitte?!");
                        }
                    }
                    case AVAILIBILITY -> {
                        if (dataObject.getSender() == SendingInformation.UI) {
                            AvailabilityData availabilityData = objectMapper.readValue(dataObject.getData(), AvailabilityData.class);

                            dataObject.setSender(SendingInformation.TRAVELBROKER);
                            parsedMessage = objectMapper.writeValueAsBytes(dataObject);
                            if (!availabilityData.isSendHotel()){
                                DatagramPacket dgOutHotel = new DatagramPacket(parsedMessage, parsedMessage.length, Participant.localhost, Participant.hotelPort);
                                dgSocket.send(dgOutHotel);
                            }
                            if(!availabilityData.isSendRentalCar()){
                                System.out.println("wtf");
                                DatagramPacket dgOutCar = new DatagramPacket(parsedMessage, parsedMessage.length, Participant.localhost, Participant.rentalCarPort);
                                dgSocket.send(dgOutCar);
                            }

                        } else if (dataObject.getSender() == SendingInformation.RENTALCAR) {
                            parsedMessage = objectMapper.writeValueAsBytes(dataObject);
                            DatagramPacket dgOutUI = new DatagramPacket(parsedMessage, parsedMessage.length, Participant.localhost, Participant.uiPort);
                            dgSocket.send(dgOutUI);
                        } else if (dataObject.getSender() == SendingInformation.HOTEL) {
                            parsedMessage = objectMapper.writeValueAsBytes(dataObject);
                            DatagramPacket dgOutUI = new DatagramPacket(parsedMessage, parsedMessage.length, Participant.localhost, Participant.uiPort);
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
