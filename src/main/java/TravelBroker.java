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

        //if button to book clicked then hotel.reserve and rentalcar.reserve with same id
        //if hotel.abort then retalcar.timestampCleanUP
        //if rentalCar.abort then hotel.timestampCleanUP
        //if answer is commit from both then hotel.book and rentalcar.book
        //now only system can crash, otherwise booking went through
        //to capture system crash, if only one booking return AVAILABILTY/SUCCESS then clean up the other

            try (DatagramSocket dgSocket = new DatagramSocket(4446)) {
                String data = "Dies ist meine erste UDP Nachricht!";
                byte[] rawData = data.getBytes();
                DatagramPacket dgPacket = new DatagramPacket(rawData, rawData.length, tb.localhost, 4445);
                dgSocket.send(dgPacket);
            } catch (Exception e) {

            }

    }
}
