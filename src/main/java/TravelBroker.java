import org.utils.Participant;
import org.utils.Operations;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

public class TravelBroker extends Participant {


    @Override
    public Operations Vote() {
        return null;
    }

    @Override
    public byte[] book(LocalDate localDate, LocalDate localDate1) {
        return new byte[0];
    }

    @Override
    public ArrayList<Object> getAvailableItems(LocalDate localDate, LocalDate localDate1) {
        return null;
    }




    public static void main(String[] args) {

        TravelBroker tb = new TravelBroker();
        //first.start();
        //second.start();

        while (true){
            try (DatagramSocket dgSocket = new DatagramSocket(4445)) {
                byte[] buffer = new byte[65507];
                DatagramPacket dgPacket = new DatagramPacket(buffer, buffer.length);
                System.out.println("Listening on Port 4445..");
                if (dgSocket.receive(dgPacket)) {
                    BrokerThread thread = new BrokerThread(dgPacket, UUID.randomUUID());
                    thread.start();
                }
                //else listen weiterhin am port
            } catch (Exception e) {

            }

        }

           /* try (DatagramSocket dgSocket = new DatagramSocket(4446)) {
                String data = "Dies ist meine erste UDP Nachricht!";
                byte[] rawData = data.getBytes();
                DatagramPacket dgPacket = new DatagramPacket(rawData, rawData.length, tb.localhost, 4445);
                dgSocket.send(dgPacket);
            } catch (Exception e) {

            }*/

    }
}
