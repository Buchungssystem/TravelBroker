import javax.swing.*;

public class ThreadUI extends Thread{

    private int travelBrokerPort;

    public ThreadUI(int travelBrokerPort){
        this.travelBrokerPort = travelBrokerPort;
    }

    @Override
    public void run(){
        SwingUtilities.invokeLater(() -> {
            UI ui = new UI(travelBrokerPort);
            ui.setVisible(true);
        });

    }
}
