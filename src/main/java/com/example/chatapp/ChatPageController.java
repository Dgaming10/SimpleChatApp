package com.example.chatapp;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.ContextMenuEvent;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ChatPageController {
    @FXML
    private ChoiceBox<String> groupChooser;

    @FXML
    private TextArea txtArea;

    @FXML
    private TextField writeTextArea;

    @FXML
    private Button sendBtn;

    @FXML
    private Button leaveBtn;

    private static String username = "";
    private static MulticastSocket socket;
    private static int port = -1;
    private static Thread receivingThread = null;
    private static InetAddress group = null;

    public void initialize() {
        sendBtn.setDisable(true);
        leaveBtn.setDisable(true);

        groupChooser.setOnAction(e -> onGroupSelected());
    }

    @FXML
    protected void onSendClick() {
        String txt = writeTextArea.getText().trim();
        if (txt.isEmpty()) {
            return;
        }

        String msg = username + ": " + txt;
        byte[] buffer = msg.getBytes();
        try {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, port);
            socket.send(packet);
            ChatAppServer.sendMessage(username, port, txt);
            writeTextArea.clear();
        } catch (IOException e) {
            handleException(e);
        }
    }

    protected void onGroupSelected() {
        txtArea.clear();
        String selectedGroup = groupChooser.getValue();

        if (port != -1) {
            leaveGroup();
        } else {
            sendBtn.setDisable(false);
            leaveBtn.setDisable(false);
        }

        port = Integer.parseInt(selectedGroup.split(" ")[3]);
        joinGroup();

        // Start a new thread to receive messages
        receivingThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    byte[] buf = new byte[1024];
                    DatagramPacket p = new DatagramPacket(buf, buf.length, group, port);
                    socket.receive(p);
                    String msg = new String(buf, 0, p.getLength(), StandardCharsets.UTF_8);
                    handleReceivedMessage(msg);
                }
            } catch (IOException e) {
                handleException(e);
            }
        });
        receivingThread.start();

        List<String> msgs = ChatAppServer.retrieveMessages(port);
        msgs.forEach(this::handleReceivedMessage);
    }
    protected static void closeWindow(Event event) {
        if (port != -1) {
            leaveGroup();
        }
        Platform.exit();
    }


    protected void loadData(String username) throws UnknownHostException {
        ChatPageController.username = username;
        HashMap<String,List<Integer>> groups = ChatAppServer.getUserGroups(username);
        group = InetAddress.getByName("228.5.6.7");
        for (String currentGroup : groups.keySet())
        {
            for(Integer port : groups.get(currentGroup))
            {
                groupChooser.getItems().add(currentGroup + " -> port " + port);
            }
        }

        sendBtn.setDisable(true);
        leaveBtn.setDisable(true);

        groupChooser.setOnAction(e -> {
            System.out.println("INIT");
            txtArea.clear();
            String selectedGroup = groupChooser.getValue();

            if (port != -1) {
                leaveGroup();
            }
            else {
                sendBtn.setDisable(false);
                leaveBtn.setDisable(false);
            }

            port = Integer.parseInt(selectedGroup.split(" ")[3]);

            joinGroup();

            receivingThread = new Thread(() -> {
                try {
                    System.out.println("STARTING");
                    while (!Thread.currentThread().isInterrupted()) {
                        byte[] buf = new byte[1024];
                        DatagramPacket p = new DatagramPacket(buf, buf.length, group, port);
                        String msg;
                        System.out.println("ARRIVED AT BLOCKING");
                        socket.receive(p);
                        msg = new String(buf, 0, p.getLength(), StandardCharsets.UTF_8);
                        System.out.println("RECEIVED: " + msg);
                        if (msg.split(" - ",2)[0].equals(username)) {
                            continue;
                        }
                        if(msg.split(" - ",2).length == 2 && !msg.split(" - ",2)[0].contains(":"))
                        {
                            msg = msg.split(" - ",2)[0] + " " + msg.split(" - ",2)[1];
                        }
                        if (msg.split(":",2)[0].equals(username)) {
                            System.out.println(msg.split(":",2)[1]);
                            msg = "You:" + msg.split(":",2)[1];
                            System.out.println(Arrays.toString(msg.split(":",2)) + "," + msg);
                        }

                        txtArea.insertText(txtArea.getLength(), msg + "\n");
                    }
                    System.out.println("FINISH");
                } catch (IOException lp) {
                    System.out.println(lp.getMessage());
                    System.out.println("HEY BRO");
                }
            });

            receivingThread.start();
            List<String> msgs = ChatAppServer.retrieveMessages(port);
            for(String m : msgs)
            {
                if(m.split(":",2)[0].equals(username))
                {
                    m = "You:" + m.split(":",2)[1];
                }
                txtArea.insertText(txtArea.getLength(),m + "\n");
            }
        });
    }


    protected static void leaveGroup() {
        String leaveMSG = username + " - HAS LEFT THE CHAT!";
        DatagramPacket packet = new DatagramPacket(leaveMSG.getBytes(), leaveMSG.length(), group, port);
        try {
            socket.send(packet);
            if (receivingThread != null && receivingThread.isAlive()) {
                receivingThread.interrupt();
            }
            socket.close();
            port = -1;
        } catch (IOException e) {
            handleException(e);
        }
    }

    protected void joinGroup() {
        try {
            socket = new MulticastSocket(port);
            socket.joinGroup(group);
            String joinMSG = username + " - HAS JOINED THE CHAT!";
            DatagramPacket packet = new DatagramPacket(joinMSG.getBytes(), joinMSG.length(), group, port);
            socket.send(packet);
        } catch (IOException e) {
            handleException(e);
        }
    }

    private static void handleException(Exception e) {
        // Handle exceptions gracefully (e.g., display an error message)
        e.printStackTrace();
    }

    protected void handleReceivedMessage(String msg) {
        if (msg.startsWith(username + ":")) {
            msg = "You" + msg.substring(username.length()) + "\n";
        }
        String finalMsg = msg;
        Platform.runLater(() -> txtArea.appendText(finalMsg + "\n"));
    }

    @FXML
    protected void onLeaveBtnClick()
    {
        int tmpPort = port;
        System.out.println("CLICKED!");
        leaveGroup();
        ChatAppServer.leaveGroup(username,tmpPort);

        groupChooser.getItems().remove(groupChooser.getValue());
        groupChooser.setValue(groupChooser.getItems().get(0));
    }


    public void onChatClick(ContextMenuEvent contextMenuEvent) {
    }
}