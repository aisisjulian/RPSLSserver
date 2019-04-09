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
                    System.out.println(data.toString());
                    if (data.toString().equals("CONNECTED")) {
                        callback.accept(data);
                        send(namesList, clientIndex);
                    }
                    if (data.toString().split(" ")[0].equals("NAME:")){
                        this.screenName = data.toString().split(" ")[1];
                        namesList.add(this.screenName);
                        clientThreadMap.put(screenName, this);
                        String name = "name " + screenName;
                        for(int i = 0; i < clientThreadList.size(); i++){
                            if (i != clientIndex) {
                                send(name, i);
                            }
                        }
                    }
                    if (data.toString().split(" ")[0].equals("OPPONENT:")){
                        String oppName = data.toString().split(" ")[1];
                        opponent = clientThreadMap.get(oppName);
                        send("PLAY-REQUEST: " + screenName, opponent.clientIndex);
                    }
                    if(data.toString().split(" ")[0].equals("ACCEPTED")){
                        game = new Game(numGames, this);
                        gameMap.put(game.getGameID(), this.game);
                        game.addPlayer(opponent);
                        isInGame = true;
                        game.startGame();
                        numGames++;
                        System.out.println(screenName + " ACCEPTED RECEIVED");
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
                            if(waitingList.size() > 0 && opponent == waitingList.get(0)){
                                waitingList.remove(0);
                            }
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
                        namesList.remove(screenName);
                        for(int i = 0; i < clientThreadList.size(); i++){
                            send("remove " + screenName, i);
                        }

                        if(isInGame){
                            game.playerDisconnected(this);
                            isInGame = false;
                        }

                        callback.accept(data);
                    }

                    if(data.toString().equals("quit")){
                        if(isInGame) {
                            gameMap.remove(game.getGameID());
                            numGames--;
                            game.playerQuit(this);
                            isInGame = false;
                        }
                    }
                    if(data.toString().equals("PlayNext")){
                        opponent = waitingList.get(0);
                        send("PLAY-REQUEST: " + screenName, opponent.clientIndex);
                    }

                    if(isInGame) {
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

    class Game {

        private int ID;
        private int numPlayers;
        private ConnThread p1;
        private ConnThread p2;
        private boolean isActive;

        Game(int ID){
            this.ID = ID;
            isActive = false;
            numPlayers = 0;
        }


        Game(int ID, ConnThread p1){
            this.ID = ID;
            this.p1 = p1;
            numPlayers = 1;
            isActive = false;
        }

        public String getPlayerID(ConnThread player){
            if(player == p1){
                return "PLAYER 1";
            }
            else if(player == p2){
                return "PLAYER 2";
            }
            return "";
        }

        public int getNumPlayers(){ return this.numPlayers; }

        public boolean isActive(){ return this.isActive; }
        public int getGameID(){ return ID; }

        public void send(Serializable data, ConnThread player){
            try {
                player.out.writeObject(data);
            }catch(Exception e){
                System.out.println("unable to send data: " + data.toString());
            }
        }

        void addPlayer(ConnThread player){
            if(numPlayers == 0){
                p1 = player;
            }
            else if(numPlayers == 1){
                p2 = player;
            }
            numPlayers++;
        }

        void playerDisconnected(ConnThread playerDisconnected){
            if(p1 == playerDisconnected){
                p1.setConnected(false);
                send("playerDisconnected", p2);
            }
            else if(p2 == playerDisconnected){
                p2.setConnected(false);
                send("playerDisconnected", p1);
            }
            numPlayers--;
        }

        void playerQuit(ConnThread playerQuit){
            if(p1 == playerQuit){
                p1.setConnected(false);
                send("opponent quit", p2);
            }
            else if(p2 == playerQuit){
                p2.setConnected(false);
                send("opponent quit", p1);
            }
            numPlayers--;
        }

        void playingAgain(){
            if(p1.isPlayingAgain() && p2.isPlayingAgain()){
                send("start", p1);
                send("start", p2);
                p1.setPlayingAgain(false);
                p2.setPlayingAgain(false);
            }
        }

        void resetGame(){

        }

        void startGame(){
            isActive = true;
            send("start", p1);
            send("start", p2);
        }

        Serializable evalGame(){
            Serializable data =  " ";
            if (p1.hasPlayed() && p2.hasPlayed()) {
                send(p1.getPlayed(), p1);
                send(p2.getPlayed(), p2);
                int winner = findWinner(p1.getPlayed(), p2.getPlayed());
                if (winner == 2) {
                    send("winner", p2);
                    data = "WIN2";
                    send("loser", p1);
                } else if (winner == 1) {
                    send("winner", p1);
                    data = "WIN1";
                    send("loser", p2);
                } else {
                    send("tie", p1);
                    send("tie", p2);
                }

                p1.setHasPlayed(false);
                p2.setHasPlayed(false);
                isActive = false;
            }
            return data;
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
    }
}