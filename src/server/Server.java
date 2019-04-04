package server;

import java.io.*;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.function.Consumer;

public class Server {

    private int port;
    private ArrayList<ConnThread> clientThreadList = new ArrayList<>();
    private HashMap<ConnThread, String> clientThreadMap = new HashMap<>();
    private Consumer<Serializable> callback;
    static int numClients = 0;
    private boolean player1Connected = false;
    private boolean player2Connected = false;
    private boolean player1IsPlayingAgain = false;
    private boolean player2IsPlayAgain = false;

    public Server(int port, Consumer<Serializable> callback) {
        this.callback = callback;
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void startConn(ServerSocket ss){
        try{
            while(true) {
              ConnThread t = new ConnThread(ss.accept());
              t.setDaemon(true);
              clientThreadList.add(t);
              numClients++;
              t.start();
            }
        }
        catch(IOException e){
        }
    }

    public void send(Serializable data, int client) throws Exception{
        clientThreadList.get(client).out.writeObject(data);
    }

    public void closeConn() throws Exception{
        for(int i = 0; i < clientThreadList.size(); i++){
            send("NO CONNECTION", i);
            clientThreadList.get(i).socket.close();
        }
    }

    public int findWinner(String p1, String p2){
        if(p1.equals(p2)){
            return -1;
        }
        else if(p1.equals("rock") && p2.equals("paper")){
            return 2;
        }
        else if(p2.equals("rock") && p1.equals("paper")){
            return 1;
        }
        else if(p1.equals("rock") && p2.equals("scissors")){
            return 1;
        }
        else if(p2.equals("rock") && p1.equals("scissors")){
            return 2;
        }
        else if(p1.equals("rock") && p2.equals("lizard")){
            return 1;
        }
        else if(p2.equals("rock") && p1.equals("lizard")){
            return 2;
        }
        else if(p1.equals("rock") && p2.equals("spock")){
            return 2;
        }
        else if(p2.equals("rock") && p1.equals("spock")){
            return 1;
        }
        else if(p1.equals("paper") && p2.equals("scissors")){
            return 2;
        }
        else if(p2.equals("paper") && p1.equals("scissors")){
            return 1;
        }
        else if(p2.equals("paper") && p1.equals("lizard")){
            return 1;
        }
        else if(p1.equals("paper") && p2.equals("lizard")){
            return 1;
        }
        else if(p2.equals("paper") && p1.equals("spock")){
            return 2;
        }
        else if(p1.equals("paper") && p2.equals("spock")){
            return 1;
        }
        else if(p2.equals("scissors") && p1.equals("lizard")){
            return 2;
        }
        else if(p1.equals("scissors") && p2.equals("lizard")){
            return 1;
        }
        else if(p2.equals("scissors") && p1.equals("spock")){
            return 1;
        }
        else if(p1.equals("scissors") && p2.equals("spock")){
            return 2;
        }
        else if(p2.equals("lizard") && p1.equals("spock")){
            return 2;
        }
        else if(p1.equals("lizard") && p2.equals("spock")){
            return 1;
        }
        else{
            return -1;
        }
    }

     class ConnThread extends Thread{
        private ObjectOutputStream out;
        private Socket socket;
        private int clientID;
        private String played;
        private boolean hasPlayed;
        private boolean isConnected;
        private int points;
        private String playerID = "";

        ConnThread(Socket s){
            this.socket = s;
            this.clientID = numClients;
            if(!player1Connected){
                player1Connected = true;
                playerID = "PLAYER 1";
                clientThreadMap.put(this, "PLAYER 1");
            }
            else if(!player2Connected){
                player2Connected = true;
                playerID = "PLAYER 2";
                clientThreadMap.put(this, "PLAYER 2");
            }
            setDaemon(true);
            this.isConnected = true;
            this.hasPlayed = false;
            this.points = 0;
        }
        public void run() {
            try (
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                this.out = out;
                socket.setTcpNoDelay(true);
                send("CONNECTED", clientID);
                if(numClients == 2){
                    for(int i = 0; i < clientThreadList.size(); i++){
                        send("start", i);
                    }
                }

                while (isConnected) {

                    Serializable data = (Serializable) in.readObject();

                    if (data.toString().equals("CONNECTED")) {
                        callback.accept(data);
                    }
                    if (data.toString().equals("disconnected")) {
                        if(this.playerID.equals("PLAYER 1")){
                            player1Connected = false;
                        }
                        else if(this.playerID.equals("PLAYER 2")){
                            player2Connected = false;
                        }
                        data = data.toString() + " " + playerID;

                        for (int i = clientID+1; i < clientThreadList.size(); i++) {
                            clientThreadList.get(i).clientID--;
                        }
                        clientThreadList.remove(clientID);
                        clientThreadMap.remove(playerID);
                        numClients--;
                        for (int i = 0; i < clientThreadList.size(); i++) {
                            send("playerDisconnected", i);
                        }

                        callback.accept(data);
                    }

                    if(data.toString().equals("quit")){
                        if(clientID == 0){
                            send("opponent quit", 1);
                        }
                        else if(clientID == 1){
                            send("opponent quit", 0);
                        }
                    }

                    if(numClients >=2 ) {
                        if(data.toString().equals("playing again")){
                            if(playerID.equals("PLAYER 1")){
                                player1IsPlayingAgain = true;
                            }else if(playerID.equals("PLAYER 2")){
                                player2IsPlayAgain = true;
                            }
                            data = data.toString() + " " + playerID;
                            callback.accept(data);
                            if(player1IsPlayingAgain && player2IsPlayAgain){
                                send("start", 0);
                                send("start", 1);
                                player2IsPlayAgain = false;
                                player1IsPlayingAgain = false;
                            }
                        }

                        if (data.toString().equals("rock") || data.toString().equals("paper") ||
                                data.toString().equals("scissors") || data.toString().equals("lizard")
                                || data.toString().equals("spock")) {
                            this.played = data.toString();
                            this.hasPlayed = true;
                        }
                        if (clientThreadList.get(0).hasPlayed && clientThreadList.get(1).hasPlayed) {
                            send(clientThreadList.get(0).played, 1);
                            send(clientThreadList.get(1).played, 0);
                            int winner = findWinner(clientThreadList.get(0).played, clientThreadList.get(1).played);
                            if (winner == 2) {
                                send("winner", 1);
                                clientThreadList.get(1).points++;
                                data = clientThreadList.get(1).playerID;
                                send("loser", 0);
                            } else if (winner == 1) {
                                send("winner", 0);
                                clientThreadList.get(0).points++;
                                data = clientThreadList.get(0).playerID;
                                send("loser", 1);
                            } else {
                                send("tie", 0);
                                send("tie", 1);
                            }

                            clientThreadList.get(0).hasPlayed = false;
                            clientThreadList.get(1).hasPlayed = false;
                            callback.accept(data);
                        }
                        if (clientThreadList.get(0).points == 3) {
                            send("WIN", 0);
                            send("LOSE", 1);
                            data = "WIN " + clientThreadList.get(0).playerID;
                            callback.accept(data);
                            clientThreadList.get(0).points = 0;
                            clientThreadList.get(1).points = 0;
                        }
                        if (clientThreadList.get(1).points == 3) {
                            send("WIN", 1);
                            send("LOSE", 0);
                            data = "WIN" + clientThreadList.get(1).playerID;
                            callback.accept(data);
                            clientThreadList.get(0).points = 0;
                            clientThreadList.get(1).points = 0;
                        }
                    }
                }


            } catch (Exception e) {
                callback.accept("NO CONNECTION " + clientID);
            }

        }

    }

}
