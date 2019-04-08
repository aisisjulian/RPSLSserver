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
    private ArrayList<String> namesList = new ArrayList<>();
    private HashMap<String, ConnThread> clientThreadMap = new HashMap<>();
    private ArrayList<Game> gameList = new ArrayList<>();
    private HashMap<Integer, Game> gameMap = new HashMap<>();
    private Consumer<Serializable> callback;

    static int numClients = 0;
    static int numGames = 0;

    public Server(int port, Consumer<Serializable> callback) {
        this.callback = callback;
        this.port = port;
        namesList.add("NAMESLIST");
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



     class ConnThread extends Thread{
        ObjectOutputStream out;
        private Socket socket;
        private int clientIndex;
        private String played;
        private boolean hasPlayed;
        private boolean isConnected;
        private boolean isPlayingAgain;
        private String screenName;
        private boolean isInGame;
        private ConnThread opponent;
        private Game game;
        private ArrayList<ConnThread> waitingList;
        ConnThread(Socket s){
            this.socket = s;
            this.clientIndex = numClients;
            setDaemon(true);
            this.isConnected = true;
            this.hasPlayed = false;
            this.isPlayingAgain = false;
            this.isInGame = false;
            waitingList = new ArrayList<>();
        }

        public void setConnected( boolean c){ this.isConnected = c; }
        public boolean isConnected(){ return this.isConnected; }
        public boolean isPlayingAgain(){ return this.isPlayingAgain; }
        public void setPlayingAgain(boolean p){ this.isPlayingAgain = p; }
        public int getClientIndex(){ return this.clientIndex; }
        public boolean hasPlayed(){ return this.hasPlayed; }
        public String getPlayed(){ return this.played; }
        public void setHasPlayed(boolean p){ this.hasPlayed = p; }
        public boolean getIsInGame(){ return this.isInGame; }
        public void setIsInGame(boolean g){ this.isInGame = g; }
        public Game getGame(){ return this.game; }

        public void run() {
            try (
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                this.out = out;
                socket.setTcpNoDelay(true);
                send("CONNECTED", clientIndex);

                while (isConnected) {

                    Serializable data = (Serializable) in.readObject();

                    if (data.toString().equals("CONNECTED")) {
                        callback.accept(data);
                    }
                    if (data.toString().split(" ")[0].equals("NAME:")){
                        this.screenName = data.toString().split(" ")[1];
                        namesList.add(this.screenName);
                        clientThreadMap.put(screenName, this);
                        send(namesList, clientIndex);
                    }
                    if (data.toString().split(" ")[0].equals("OPPONENT:")){
                        String oppName = data.toString().split(" ")[1];
                        opponent = clientThreadMap.get(oppName);
                        send("PLAY-REQUEST: " + screenName, clientIndex);
                    }
                    if(data.toString().split(" ")[0].equals("ACCEPTED")){
                        game.addPlayer(opponent);
                        isInGame = true;
                    }
                    if(data.toString().split(" ")[0].equals("WAIT")){
                        game = new Game(numGames, this);
                        gameMap.put(game.getGameID(), this.game);
                    }

                    if(data.toString().split(" ")[0].equals("PLAY-REQUEST:")){
                        String nextPlayerName = data.toString().split(" ")[1];
                        ConnThread nextPlayer = clientThreadMap.get(nextPlayerName);
                        if(isInGame && opponent != nextPlayer){
                            waitingList.add(nextPlayer);
                            send("WAIT", nextPlayer.clientIndex);
                        }
                        else if(!isInGame && opponent == nextPlayer){
                            isInGame = true;
                            game = opponent.getGame();
                            send("ACCEPTED", nextPlayer.clientIndex);
                        }
                    }


                    if (data.toString().equals("disconnected")) {
                        data = data.toString() + " " + screenName;
                        for (int i = clientIndex+1; i < clientThreadList.size(); i++) {
                            clientThreadList.get(i).clientIndex--;
                        }
                        clientThreadList.remove(clientIndex);
                        clientThreadMap.remove(screenName);
                        numClients--;

                        if(isInGame){
                            game.playerDisconnected(this);
                        }

                        callback.accept(data);
                    }

                    if(data.toString().equals("quit")){
                        if(isInGame) {
                            game.playerQuit(this);
                        }
                    }

                    if( game.isActive() ) {
                        if(data.toString().equals("playing again")){
                            isPlayingAgain = true;
                            game.playingAgain();
                            data = data.toString() + " " + game.getPlayerID(this);
                            callback.accept(data);
                        }

                        if (data.toString().equals("rock") || data.toString().equals("paper") ||
                                data.toString().equals("scissors") || data.toString().equals("lizard")
                                || data.toString().equals("spock")) {
                            this.played = data.toString();
                            this.hasPlayed = true;
                            data = game.evalGame();
                            if(data.toString().equals("WIN1") || data.toString().equals("WIN2")){
                                callback.accept(data);
                            }
                        }
//
                    }
                }


            } catch (Exception e) {
                callback.accept("NO CONNECTION " + screenName);
            }

        }

    }

}
