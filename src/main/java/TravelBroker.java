import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.std.CollectionSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.utils.*;
import org.w3c.dom.html.HTMLOptionElement;

import javax.xml.crypto.Data;
import java.io.File;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.DatagramSocketImpl;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.fasterxml.jackson.databind.type.LogicalType.Map;


public class TravelBroker {

    private static DatagramSocket dgSocket;

    private final static Logger LOGGER = Logger.getLogger(TravelBroker.class.getName());

    private static TransactionContext transactionContext;

    private static Map<UUID, TransactionContext> transactionContextMap = new HashMap<>();

    private static LogWriter<TransactionContext> logWriter = new LogWriter<>();

    //port of the current TravelBroker instance
    private static int myPort;

    //port of the corresponding ui instance
    private static int uiPort = 0;

    private static SharedRessourcesTimerThread sharedRessourcesTimerThread;

    public static void main(String[] args) {


        int[] possiblePorts = {4442, 4443, 4444, 4445, 4446};
        boolean portSet = false;
        for (int i = 0; i < possiblePorts.length; i++){
            try{
                dgSocket = new DatagramSocket(possiblePorts[i]);
                myPort = possiblePorts[i];
                portSet = true;
                break;
            }catch(Exception e){
                LOGGER.log(Level.INFO, "Port " + possiblePorts[i] + "already taken, trying next!");
            }
        }

        if(!portSet){
            LOGGER.log(Level.SEVERE, "TravelBroker didn't find any open Port! Program will now shut down.");
            System.exit(0);
        }

        ThreadUI t = new ThreadUI(myPort);
        t.start();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        //recovery from crash

        //check if logs are empty since we wouldn't need to recover if there are no logs
        if(logWriter.isLogsNotEmpty()){
            File dir = new File(logWriter.getDirectory());
            File[] files = dir.listFiles();

            //loop through all files of logs directory
            for(File currentContextFile : files){
                String name = currentContextFile.getName();
                name = name.substring(0, name.length() - 4);

                //write context back to contextMap
                UUID transactionId = UUID.fromString(name);
                transactionContext = logWriter.readLogFile(transactionId);
                transactionContextMap.put(transactionId, transactionContext);

                LOGGER.log(Level.INFO, "Transaction Context with the ID: " + transactionId + "was restored");
            }

            byte[] parsedResponse = null;

            //loop through context map and react accordingly
            for(Map.Entry<UUID, TransactionContext> entry : transactionContextMap.entrySet()){
                //get transaction current context
                TransactionContext currContext = entry.getValue();

                switch(entry.getValue().getCurrentState()){
                    case GLOBALCOMMIT -> {
                        if(!currContext.isCarFlag()){
                            //send decision to Car since we couldn't send it before the crash

                            UDPMessage carResponse = new UDPMessage(entry.getKey(), new byte[0], SendingInformation.TRAVELBROKER, Operations.COMMIT, myPort);
                            try {
                                parsedResponse = objectMapper.writeValueAsBytes(carResponse);
                                DatagramPacket dpCar = new DatagramPacket(parsedResponse, parsedResponse.length, Participant.localhost, Participant.rentalCarPort);

                                dgSocket.send(dpCar);
                                LOGGER.log(Level.INFO, "Send the decission Commit to RentalCar");

                                boolean hotelFlag = currContext.isHotelFlag();

                                transactionContext = new TransactionContext(States.READY, true, hotelFlag);
                                logWriter.write(entry.getKey(), transactionContext);
                                transactionContextMap.put(entry.getKey(), transactionContext);
                            }catch(Exception e){
                                LOGGER.log(Level.SEVERE, "There was an error with sending the decision - commit recovery", e);
                            }
                        } else if (!currContext.isHotelFlag()) {
                            //send decision to Hotel since we couldn't send it before the crash
                            UDPMessage hotelResponse = new UDPMessage(entry.getKey(), new byte[0], SendingInformation.TRAVELBROKER, Operations.COMMIT, myPort);
                            try {
                                parsedResponse = objectMapper.writeValueAsBytes(hotelResponse);
                                DatagramPacket dpHotel = new DatagramPacket(parsedResponse, parsedResponse.length, Participant.localhost, Participant.hotelPort);

                                dgSocket.send(dpHotel);
                                LOGGER.log(Level.INFO, "Send the decission Commit to Hotel");

                                boolean carFlag = currContext.isCarFlag();

                                transactionContext = new TransactionContext(States.READY, carFlag, true);
                                logWriter.write(entry.getKey(), transactionContext);
                                transactionContextMap.put(entry.getKey(), transactionContext);
                            }catch(Exception e){
                                LOGGER.log(Level.SEVERE, "There was an error with sending the decision - commit recovery", e);
                            }
                        }
                    }
                    case ABORT -> {
                        if(!currContext.isCarFlag()){
                            //send decision to Car since we couldn't send it before the crash
                            UDPMessage carResponse = new UDPMessage(entry.getKey(), new byte[0], SendingInformation.TRAVELBROKER, Operations.ABORT, myPort);
                            try {
                                parsedResponse = objectMapper.writeValueAsBytes(carResponse);
                                DatagramPacket dpCar = new DatagramPacket(parsedResponse, parsedResponse.length, Participant.localhost, Participant.rentalCarPort);

                                dgSocket.send(dpCar);
                                LOGGER.log(Level.INFO, "Send the decission Abort to RentalCar");

                                boolean hotelFlag = currContext.isHotelFlag();

                                transactionContext = new TransactionContext(States.ABORT, true, hotelFlag);
                                logWriter.write(entry.getKey(), transactionContext);
                                transactionContextMap.put(entry.getKey(), transactionContext);
                            }catch(Exception e){
                                LOGGER.log(Level.SEVERE, "There was an error with sending the decision - abort recovery", e);
                            }
                        } else if (!currContext.isHotelFlag()) {
                            //send decision to Hotel since we couldn't send it before the crash
                            UDPMessage hotelResponse = new UDPMessage(entry.getKey(), new byte[0], SendingInformation.TRAVELBROKER, Operations.ABORT, myPort);
                            try {
                                parsedResponse = objectMapper.writeValueAsBytes(hotelResponse);
                                DatagramPacket dpHotel = new DatagramPacket(parsedResponse, parsedResponse.length, Participant.localhost, Participant.hotelPort);

                                dgSocket.send(dpHotel);
                                LOGGER.log(Level.INFO, "Send the decission Abort to Hotel");

                                boolean carFlag = currContext.isCarFlag();

                                transactionContext = new TransactionContext(States.READY, carFlag, true);
                                logWriter.write(entry.getKey(), transactionContext);
                                transactionContextMap.put(entry.getKey(), transactionContext);
                            }catch(Exception e){
                                LOGGER.log(Level.SEVERE, "There was an error with sending the decision - Abort recovery", e);
                            }
                        }
                    }
                    case READY -> {
                        //if the state is still ready that means we didn't receive the ready response from one or both of our participants
                        //otherwise it would be set to globalCommit
                        //so we send a abort to both

                        UDPMessage abortMessage = new UDPMessage(entry.getKey(), new byte[0], SendingInformation.TRAVELBROKER, Operations.ABORT, myPort);
                        try {
                            parsedResponse = objectMapper.writeValueAsBytes(abortMessage);
                            DatagramPacket dpHotel = new DatagramPacket(parsedResponse, parsedResponse.length, Participant.localhost, Participant.hotelPort);
                            DatagramPacket dpCar = new DatagramPacket(parsedResponse, parsedResponse.length, Participant.localhost, Participant.rentalCarPort);

                            dgSocket.send(dpHotel);
                            dgSocket.send(dpCar);
                        }catch (Exception e){
                            LOGGER.log(Level.SEVERE, "There was an error with sending the decision - Ready recovery");
                        }

                    }
                    case PREPARE -> {
                        //if the state is prepare we send an abort to both participants
                        UDPMessage abortMessage = new UDPMessage(entry.getKey(), new byte[0], SendingInformation.TRAVELBROKER, Operations.ABORT, myPort);
                        try {
                            parsedResponse = objectMapper.writeValueAsBytes(abortMessage);
                            DatagramPacket dpHotel = new DatagramPacket(parsedResponse, parsedResponse.length, Participant.localhost, Participant.hotelPort);
                            DatagramPacket dpCar = new DatagramPacket(parsedResponse, parsedResponse.length, Participant.localhost, Participant.rentalCarPort);

                            dgSocket.send(dpHotel);
                            dgSocket.send(dpCar);
                        }catch (Exception e){
                            LOGGER.log(Level.SEVERE, "There was an error with sending the decision - Prepare recovery");
                        }
                    }
                    case OK -> {
                        //since we are still in OK phase we missed the second ok from the other participant
                        //so we delete the log

                        logWriter.delete(entry.getKey());
                        transactionContextMap.remove(entry.getKey());
                    }
                }
            }
        }

        //actual 2PC Protocol implementation
        while (true) {
            try{
                //buffer for receiving data
                byte[] buffer = new byte[65507];

                //DatagramPacket for receiving data
                DatagramPacket dgPacketIn = new DatagramPacket(buffer, buffer.length);
                LOGGER.log(Level.INFO, "TravelBroker listening on Port: " + myPort);

                //receiving and parsing UDP Message
                dgSocket.receive(dgPacketIn);
                String data = new String(dgPacketIn.getData(), 0, dgPacketIn.getLength());
                UDPMessage dataObject = objectMapper.readValue(data, UDPMessage.class);

                //parsed Message for sending data
                byte[] parsedMessage;

                //store TransaktionID for context on further processing
                UUID transactionId = dataObject.getTransaktionNumber();

                //decide what to do next for 2PC based on sent operation
                switch (dataObject.getOperation()) {
                    case PREPARE -> {
                        //check if the Request came from the UI
                        if (dataObject.getSender() == SendingInformation.UI) {
                            LOGGER.log(Level.INFO, "2PC: Prepare - " + transactionId);

                            //set SendingInformation to TravelBroker cause it just gets forwarded to the participants from here
                            dataObject.setSender(SendingInformation.TRAVELBROKER);
                            dataObject.setOriginPort(myPort);

                            parsedMessage = objectMapper.writeValueAsBytes(dataObject);
                            DatagramPacket dgOutHotel = new DatagramPacket(parsedMessage, parsedMessage.length, Participant.localhost, Participant.hotelPort);
                            DatagramPacket dgOutCar = new DatagramPacket(parsedMessage, parsedMessage.length, Participant.localhost, Participant.rentalCarPort);
                            //send Prepare with bookingData to hotel and rentalCar
                            dgSocket.send(dgOutHotel);
                            dgSocket.send(dgOutCar);

                            //start timerThread to wait if there was a response received. If the timer stops before there comes a response the coordinator sends a abort
                            sharedRessourcesTimerThread  = new SharedRessourcesTimerThread(false);
                            TimerThread timerThread = new TimerThread(transactionId, 20, myPort, true, sharedRessourcesTimerThread);
                            timerThread.start();

                            transactionContext = new TransactionContext(States.PREPARE, false, false, sharedRessourcesTimerThread);
                            logWriter.write(transactionId, transactionContext);
                            transactionContextMap.put(transactionId, transactionContext);
                        }else {
                            LOGGER.log(Level.SEVERE, "Sender wasn't authenticated");
                        }

                    }
                    case ABORT -> {

                        if(dataObject.getSender() == SendingInformation.HOTEL){
                            LOGGER.log(Level.INFO, "2PC: Abort - Hotel" + transactionId);
                            //since there was an abort send we abort the full transaction so the timerThread can also be canceled
                            sharedRessourcesTimerThread = transactionContextMap.get(transactionId).getSharedRessourcesTimerThread();

                            sharedRessourcesTimerThread.setInterrupt(true);

                            //if Abort from hotel is received we can already send a abort to rental car, since the coordinator would decide to
                            //send a global abort anyways. This way its faster.
                            UDPMessage carResponse = new UDPMessage(transactionId, new byte[0], SendingInformation.TRAVELBROKER, Operations.ABORT, myPort);
                            byte[] parsedCarResponse = objectMapper.writeValueAsBytes(carResponse);
                            DatagramPacket dgOutCarAbort = new DatagramPacket(parsedCarResponse, parsedCarResponse.length, Participant.localhost, Participant.rentalCarPort);

                            dgSocket.send(dgOutCarAbort);
                            transactionContext = new TransactionContext(States.ABORT, false, true, sharedRessourcesTimerThread);
                            logWriter.write(transactionId, transactionContext);
                            transactionContextMap.put(transactionId, transactionContext);
                        }else if (dataObject.getSender() == SendingInformation.RENTALCAR){
                            LOGGER.log(Level.INFO, "2PC: Abort - RentalCar" + transactionId);
                            //since there was an abort send we abort the full transaction so the timerThread can also be canceled

                            sharedRessourcesTimerThread = transactionContextMap.get(transactionId).getSharedRessourcesTimerThread();

                            sharedRessourcesTimerThread.setInterrupt(true);

                            //if Abort from rentalCar is received we can already send a abort to Hotel, since the coordinator would decide to
                            //send a global abort anyways. This way its faster.
                            UDPMessage hotelResponse = new UDPMessage(transactionId, new byte[0], SendingInformation.TRAVELBROKER, Operations.ABORT, myPort);
                            byte[] parsedHotelResponse = objectMapper.writeValueAsBytes(hotelResponse);
                            DatagramPacket dgOutHotelAbort = new DatagramPacket(parsedHotelResponse, parsedHotelResponse.length, Participant.localhost, Participant.hotelPort);

                            dgSocket.send(dgOutHotelAbort);
                            transactionContext = new TransactionContext(States.ABORT, true, false, sharedRessourcesTimerThread);
                            logWriter.write(transactionId, transactionContext);
                            transactionContextMap.put(transactionId, transactionContext);
                        }
                    }
                    case READY -> {
                        TransactionContext currContext;
                        currContext = transactionContextMap.get(transactionId);
                        sharedRessourcesTimerThread = currContext.getSharedRessourcesTimerThread();
                        //check which participant answered with ready and set corresponding context and boolean
                        if(dataObject.getSender() == SendingInformation.HOTEL){
                            LOGGER.log(Level.INFO, "2PC: Ready from hotel - " + transactionId);
                            if(currContext.isCarFlag()){
                                // since car received and we are in the hotelReceived-block both sent ready
                                transactionContext = new TransactionContext(States.READY, true, true, sharedRessourcesTimerThread);
                            }else {
                                // only car is ready
                                transactionContext = new TransactionContext(States.READY, false, true, sharedRessourcesTimerThread);
                            }
                            logWriter.write(transactionId, transactionContext);
                            transactionContextMap.put(transactionId, transactionContext);

                        } else if (dataObject.getSender() == SendingInformation.RENTALCAR) {
                            LOGGER.log(Level.INFO, "2PC: Ready from RentalCar - " + transactionId);
                            if(currContext.isHotelFlag()){
                                // since hotel received and we are in the carReceived-block both sent ready
                                transactionContext = new TransactionContext(States.READY, true, true, sharedRessourcesTimerThread);
                            }else {
                                // only hotel is ready
                                transactionContext = new TransactionContext(States.READY, true, false, sharedRessourcesTimerThread);
                            }
                            logWriter.write(transactionId, transactionContext);
                            transactionContextMap.put(transactionId, transactionContext);
                        }


                        //get context of transactionId and check if both sent ready
                        currContext = transactionContextMap.get(transactionId);

                        if(currContext.isCarFlag() && currContext.isHotelFlag()){
                            LOGGER.log(Level.INFO, "2PC: Global Commit - " + transactionId);

                            //since both participants answered, the timer of the transaction has to be stopped
                            sharedRessourcesTimerThread = transactionContextMap.get(transactionId).getSharedRessourcesTimerThread();

                            sharedRessourcesTimerThread.setInterrupt(true);

                            //send global commit since both answered ready
                            UDPMessage hotelResponse;
                            UDPMessage carResponse;
                            byte[] parsedHotelResponse;
                            byte[] parsedCarResponse;
                            DatagramPacket dgOutHotelReady;
                            DatagramPacket dgOutCarReady;

                            //set context to global commit
                            transactionContext = new TransactionContext(States.GLOBALCOMMIT, false, false, Operations.COMMIT);
                            logWriter.write(transactionId, transactionContext);
                            transactionContextMap.put(transactionId, transactionContext);

                            hotelResponse = new UDPMessage(transactionId, new byte[0], SendingInformation.TRAVELBROKER, Operations.COMMIT, myPort);
                            carResponse = new UDPMessage(transactionId, new byte[0], SendingInformation.TRAVELBROKER, Operations.COMMIT, myPort);

                            parsedHotelResponse = objectMapper.writeValueAsBytes(hotelResponse);
                            parsedCarResponse = objectMapper.writeValueAsBytes(carResponse);

                            dgOutHotelReady = new DatagramPacket(parsedHotelResponse, parsedHotelResponse.length, Participant.localhost, Participant.hotelPort);
                            dgOutCarReady = new DatagramPacket(parsedCarResponse, parsedCarResponse.length, Participant.localhost, Participant.rentalCarPort);

                            //send global commit
                            dgSocket.send(dgOutHotelReady);
                            //store that we send globalCommit to Hotel
                            transactionContext = new TransactionContext(States.GLOBALCOMMIT, false, true, Operations.COMMIT);
                            logWriter.write(transactionId, transactionContext);
                            transactionContextMap.put(transactionId, transactionContext);

                            //Fehlerfall 3
                            //System.exit(0);

                            dgSocket.send(dgOutCarReady);
                            //store that we send globalCommit to RentalCar
                            transactionContext = new TransactionContext(States.GLOBALCOMMIT, false, false, Operations.COMMIT);
                            logWriter.write(transactionId, transactionContext);
                            transactionContextMap.put(transactionId, transactionContext);
                        }

                    }
                    case OK -> {
                        TransactionContext currContext;
                        UUID transaktionId = dataObject.getTransaktionNumber();
                        if(dataObject.getSender() == SendingInformation.HOTEL){
                            LOGGER.log(Level.INFO, "2PC: Ok from Hotel - " + transactionId);
                            currContext = transactionContextMap.get(transaktionId);
                            if(currContext.isCarFlag()){
                                //since Car is already received the LogFile gets deleted
                                logWriter.delete(transactionId);
                                transactionContextMap.remove(transaktionId);
                            }else{
                                transactionContext = new TransactionContext(States.OK, false, true, currContext.getDecission());
                            }
                        } else if (dataObject.getSender() == SendingInformation.RENTALCAR) {
                            LOGGER.log(Level.INFO, "2PC: Ok from RentalCar - " + transactionId);
                            currContext = transactionContextMap.get(transaktionId);
                            if(currContext.isHotelFlag()){
                                //since Hotel is already received the LogFile gets deleted
                                logWriter.delete(transactionId);
                                transactionContextMap.remove(transaktionId);
                            }else {
                                transactionContext = new TransactionContext(States.OK, true, false, currContext.getDecission());
                            }
                        }

                        transactionContextMap.put(transactionId, transactionContext);

                        //get current Context to validate if both sent OK
                        currContext = transactionContextMap.get(transaktionId);

                        if(currContext.isHotelFlag() && currContext.isCarFlag()){
                            //remove entry from local transactionContext
                            transactionContextMap.remove(transaktionId);
                            //remove persistent logfile
                            logWriter.delete(transaktionId);
                            LOGGER.log(Level.INFO, "2PC: completed - Booked successfully and deleted corresponding transaction context! - " + transactionId);
                        }else {
                            logWriter.write(transactionId, transactionContext);
                        }
                    }
                    case ABORTFROMTIMER -> {
                        //the timerThread of a prepare state waitet 60 seconds and wasn't cancelled
                        //that means we have to send a global abort, since one or both of the participants didn't answer the prepare request
                        LOGGER.log(Level.INFO, "Abort from Timer received");

                        UDPMessage hotelAbort = new UDPMessage(transactionId, new byte[0], SendingInformation.TRAVELBROKER, Operations.ABORT, myPort);
                        byte[] parsedHotelAbort = objectMapper.writeValueAsBytes(hotelAbort);
                        DatagramPacket dpHotelAbort = new DatagramPacket(parsedHotelAbort, parsedHotelAbort.length, Participant.localhost, Participant.hotelPort);

                        UDPMessage carAbort = new UDPMessage(transactionId, new byte[0], SendingInformation.TRAVELBROKER, Operations.ABORT, myPort);
                        byte[] parsedCarAbort = objectMapper.writeValueAsBytes(carAbort);
                        DatagramPacket dpCarAbort = new DatagramPacket(parsedCarAbort, parsedCarAbort.length, Participant.localhost, Participant.rentalCarPort);

                        //send abort to both participants
                        dgSocket.send(dpHotelAbort);
                        dgSocket.send(dpCarAbort);
                    }
                    case REQUESTDECISION -> {
                        //one of the participants didn't get the decision cause of an crash
                        //he now will recover from the crash by reqeusting the decision again
                        transactionContext = transactionContextMap.get(transactionId);
                        UDPMessage message;
                        if(transactionContext.getDecission() == Operations.COMMIT){
                            //the decision was commit
                            message = new UDPMessage(transactionId, new byte[0], SendingInformation.TRAVELBROKER, Operations.COMMIT, myPort);
                        }else {
                            //else the decision was abort
                            message = new UDPMessage(transactionId, new byte[0], SendingInformation.TRAVELBROKER, Operations.ABORT, myPort);
                        }

                        parsedMessage = objectMapper.writeValueAsBytes(message);
                        DatagramPacket dpDecision = null;
                        if(dataObject.getSender() == SendingInformation.HOTEL){
                            LOGGER.log(Level.INFO, "Hotel: Request decission was received");
                            // send to hotel
                            dpDecision = new DatagramPacket(parsedMessage, parsedMessage.length, Participant.localhost, Participant.hotelPort);
                        }else if(dataObject.getSender() == SendingInformation.RENTALCAR){
                            LOGGER.log(Level.INFO, "RentalCar: Request decission was received");
                            // send to RentalCar
                            dpDecision = new DatagramPacket(parsedMessage, parsedMessage.length, Participant.localhost, Participant.rentalCarPort);
                        }

                        dgSocket.send(dpDecision);
                    }
                    case AVAILIBILITY -> {
                        //since we always get a availability request from the ui first we store the ui port here if not already set
                        if(uiPort == 0){
                            uiPort = dataObject.getOriginPort();
                        }

                        //check if request comes from the UI
                        if (dataObject.getSender() == SendingInformation.UI) {
                            LOGGER.log(Level.INFO, "Availability: request from UI");
                            //set port to the port of travelBroker instance so that the service knows who to respond
                            dataObject.setOriginPort(myPort);

                            //retrieve availability data such as startDate and endDate of the availability request and who to send to the request
                            AvailabilityData availabilityData = objectMapper.readValue(dataObject.getData(), AvailabilityData.class);

                            //set sender from UI to TravelBroker
                            dataObject.setSender(SendingInformation.TRAVELBROKER);
                            parsedMessage = objectMapper.writeValueAsBytes(dataObject);

                            //check who to send the availability request
                            if (!availabilityData.isSendHotel()){
                                DatagramPacket dgOutHotel = new DatagramPacket(parsedMessage, parsedMessage.length, Participant.localhost, Participant.hotelPort);
                                dgSocket.send(dgOutHotel);
                            }
                            if(!availabilityData.isSendRentalCar()){
                                DatagramPacket dgOutCar = new DatagramPacket(parsedMessage, parsedMessage.length, Participant.localhost, Participant.rentalCarPort);
                                dgSocket.send(dgOutCar);
                            }

                        } else if (dataObject.getSender() == SendingInformation.RENTALCAR) {
                            LOGGER.log(Level.INFO, "Availability: response RentalCar");
                            parsedMessage = objectMapper.writeValueAsBytes(dataObject);
                            DatagramPacket dgOutUI = new DatagramPacket(parsedMessage, parsedMessage.length, Participant.localhost, uiPort);
                            dgSocket.send(dgOutUI);
                        } else if (dataObject.getSender() == SendingInformation.HOTEL) {
                            LOGGER.log(Level.INFO, "Availability: response Hotel");
                            parsedMessage = objectMapper.writeValueAsBytes(dataObject);
                            DatagramPacket dgOutUI = new DatagramPacket(parsedMessage, parsedMessage.length, Participant.localhost, uiPort);
                            dgSocket.send(dgOutUI);
                        }
                    }
                }

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "The Socket or the objectMapper threw an error", e);
            }
        }
    }
}
