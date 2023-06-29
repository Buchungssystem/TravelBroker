import org.utils.Participant;
import org.utils.Operations;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.time.LocalDate;
import java.util.UUID;

public class TravelBroker extends Participant {

    @Override
    public Operations Vote() {
        return null;
    }

    @Override
    public void book() {

    }

    @Override
    public byte[] getAvailableItems(LocalDate localDate, LocalDate localDate1, UUID pTransaktionnumber) {
        return null;
    }

    public static void main(String[] args) {

        TravelBroker tb = new TravelBroker();

            try (DatagramSocket dgSocket = new DatagramSocket(4446)) {
                String data = "Dies ist meine erste UDP Nachricht!";
                byte[] rawData = data.getBytes();
                DatagramPacket dgPacket = new DatagramPacket(rawData, rawData.length, tb.localhost, 4445);
                dgSocket.send(dgPacket);
            } catch (Exception e) {

            }

    }
}
