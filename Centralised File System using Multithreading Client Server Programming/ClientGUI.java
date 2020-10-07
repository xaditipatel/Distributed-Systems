// Submitted by Aditi Patel

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTextArea;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author DELL
 */
//The class to create client UI and connects to the server
public class ClientGUI extends JFrame {

    //Initializing a panel and objects to create client GUI
    JPanel panel = new JPanel();

    //Initialized text fields to send response
    JTextField inputField;

    //Initialized text area to see the server responses and requests
    JTextArea textArea;

    //Initialized button to send msgs
    JButton btnSend;

    //Initialized button to Exit
    JButton btnExit;

    JScrollPane jScrollPane;

    static Socket socket;
    static DataInputStream din;
    static DataOutputStream dout;

    /**
     * Creates new form ClientGUI
     */
    public ClientGUI() {
        initUI();
    }

    //This method designs the complete client GUI 
    private void initUI() {
        setTitle("Client"); //Defines the title of client window
        setSize(800, 800); //Defines the size of client window
        setLocationRelativeTo(null); //Places the client window in center of the screen
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        getContentPane().add(panel); //Returns the content pane and adds panel to it
        panel.setLayout(null);

        //Adding all the objects on the panel
        addTextArea();
        addTextField();
        addSendButton();
        addExitButton();
    }

    private void addTextField() {
        inputField = new JTextField(20);
        inputField.setBounds(10, 605, 300, 30);
        panel.add(inputField);
    }

    private void addTextArea() {
        textArea = new JTextArea();
        textArea.setColumns(20);
        textArea.setRows(5);
        textArea.setLineWrap(true);

        textArea.setBounds(10, 10, 790, 600);
        jScrollPane = new JScrollPane(textArea,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        jScrollPane.setViewportView(textArea);
        panel.add(jScrollPane);
        panel.add(textArea);
    }

    private void addSendButton() {
        btnSend = new JButton("Send");
        btnSend.setBounds(10, 650, 70, 30);
        btnSend.addActionListener((ActionEvent event) -> {
            try {
                String msg_OUT;
                msg_OUT = inputField.getText().trim();
                dout.writeUTF(msg_OUT);
            } catch (IOException ex) {
                Logger.getLogger(ClientGUI.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        panel.add(btnSend);
    }

    private void addExitButton() {
        btnExit = new JButton("Exit");
        btnExit.setBounds(90, 650, 70, 30);
        btnExit.addActionListener((ActionEvent event) -> {
            try {
                //On clicking the Quit button, first we close the resources like client socket, input and output streams and then kill the application using System.exit method
                //client.closeResources();
                dout.writeUTF("Client disconnected" + socket);
            } catch (IOException ex) {
                Logger.getLogger(ClientGUI.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.exit(0);
        });
        panel.add(btnExit);
    }

    public static void main(String args[]) {

        ClientGUI clientObj = new ClientGUI();
        clientObj.setVisible(true);
        String msg_IN;

        try {
            socket = new Socket("127.0.0.1", 1201);
            din = new DataInputStream(socket.getInputStream());
            dout = new DataOutputStream(socket.getOutputStream());
            while (true) {
                msg_IN = din.readUTF();
                clientObj.textArea.setText(clientObj.textArea.getText().trim() + "\n SERVER >> \t " + msg_IN);
            }

        } catch (IOException ex) {
            clientObj.textArea.setText("Kindly press Exit Button and Reconnect");

            Logger.getLogger(ClientGUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
