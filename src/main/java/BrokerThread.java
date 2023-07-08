import org.utils.Operations;
import org.utils.SendingInformation;
import org.utils.UDPMessage;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.UUID;

public class BrokerThread extends Thread {

    UUID id;

    DatagramPacket dgPacket;
    Operations hotelMessage;
    Operations rentalCarMessage;

    int counter = 0;

    BrokerThread(DatagramPacket dgPacket, UUID id) {
        this.id = id;
        this.dgPacket = dgPacket;
    }

        @Override
        public void run() {
            System.out.println(": Lets Goooo");
            this.id = id;
            String uiData = new String(dgPacket.getData(), 0, dgPacket.getLength());
            UDPMessage dataObject = objectMapper.readValue(uiData, UDPMessage.class);
            if (dataObject.getOperation() == UI) {
                UUID bookingID = UUID.randomUUID();

                try (DatagramSocket dgSocket = new DatagramSocket(4446)) {
                    String data = "Dies ist meine erste UDP Nachricht!";
                    byte[] rawData = data.getBytes();
                    // start_2PC in Logfile schreiben
                    //send an alle Teilnehmer
                    DatagramPacket rentalCarPacket = new DatagramPacket(rawData, rawData.length, tb.localhost, 4445, id, bookingID);
                    dgSocket.send(rentalCarPacket);
                    //mit OperationsPrepare
                    // wartet auf alle antworten
                    while (true) {
                        // hier Zeitabfrage -> wenn nach gewisser Zeit keine Antworten von beiden Teilnehmern kommt, dann send Global_Abort
                        // schreibe GLOBAL_ABORT in Logfile
                        // hier wenn hotelMessage und rentalCarMessage =! COMMIT
                        dgSocket.receive();
                        String data = new String(dgPacket.getData(), 0, dgPacket.getLength());
                        UDPMessage dataObject = objectMapper.readValue(data, transactionId, UDPMessage.class);
                        if (transactionId == id) {
                            counter += 1;
                            switch (dataObject.getOperation()) {
                                case COMMIT -> {
                                    if (dataObject.getSender() == SendingInformation.HOTEL) {
                                        hotelMessage = Operations.COMMIT;
                                    }
                                    if (dataObject.getSender() == SendingInformation.HOTEL) {
                                        rentalCarMessage = Operations.COMMIT;
                                    }
                                    if (hotelMessage == Operations.COMMIT && rentalCarMessage == Operations.COMMIT) {
                                        // send globalCommit
                                        // write global Commit in Logfile
                                        // kill thread?
                                    }

                                }
                                case ABORT -> {
                                    if (counter == 1) {
                                        //send globalAbort an alle
                                        //kill thread?
                                    } else {
                                        System.out.println("Abort wurde schon geschickt!");
                                    }

                                }
                            }
                        } else {
                            System.out.println("Paket kommt nicht von Partizipant");
                            //Ignore
                        }

                    }
                }
            }
        }

        }






