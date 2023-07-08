import javax.swing.*;

public class ThreadUI extends Thread{

    public ThreadUI(){

    }

    @Override
    public void run(){
        SwingUtilities.invokeLater(() -> {
            UI ui = new UI();
            ui.setVisible(true);
        });

    }
}
