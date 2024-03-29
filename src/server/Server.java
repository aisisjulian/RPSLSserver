package server;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

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
            if(clientThreadList.get(i).isConnected) {
                send("NO CONNECTION", i);
                clientThreadList.get(i).socket.close();
            }
        }
        numClients = 0;
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
        public void setGame(Game g ){ this.game = g; }

        public void run() {
            try (
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                this.out = out;
                socket.setTcpNoDelay(true);
             //   send("CONNECTED", clientIndex);

                while (isConnected) {

                    Serializable data = (Serializable) in.readObject();
                    System.out.println(data.toString());
                  //  if (data.toString().equals("CONNECTED")) {
                     //   callback.accept(data);
                    //}
                    if (data.toString().split(" ")[0].equals("NAME:")){
                        boolean taken = false;
                        this.screenName = data.toString().split(" ")[1];
                        for(int i = 0; i < namesList.size(); i++){
                            if(screenName.equals(namesList.get(i))){
                                taken = true;
                                send("TAKEN", clientIndex);
                                break;
                            }
                        }
                        if(!taken){
                            send("CONNECTED", clientIndex);
                            callback.accept("CONNECTED " + screenName);
                            send(namesList, clientIndex);
                            namesList.add(this.screenName);
                            clientThreadMap.put(screenName, this);
                            String name = "name " + screenName;
                            for(int i = 0; i < clientThreadList.size(); i++){
                                if (i != clientIndex) {
                                    send(name, i);
                                }
                            }
                        }
                    }
                    if (data.toString().equals("PLAY-NEXT")){
                        if(waitingList.size() > 0 && this == waitingList.get(0).opponent){
                            String oppName = waitingList.get(0).screenName;
                            waitingList.remove(0);
                            opponent = clientThreadMap.get(oppName);
                            isInGame = true;
                            send("ACCEPTED " + screenName, opponent.clientIndex);
                        }
                    }
                    if (data.toString().split(" ")[0].equals("OPPONENT:")){
                        String oppName = data.toString().split(" ")[1];
                        opponent = clientThreadMap.get(oppName);
                        send("PLAY-REQUEST: " + screenName, opponent.clientIndex);
                        callback.accept(data + " " + screenName);
                    }
                    if(data.toString().split(" ")[0].equals("ACCEPTED")){
                        game = new Game(numGames, this);
                        gameList.add(this.game);
                        game.addPlayer(opponent);
                        isInGame = true;
                        opponent.setGame(game);
                        game.startGame();
                        numGames++;
                        System.out.println(screenName + " ACCEPTED RECEIVED");
                        callback.accept("START " + screenName + " " + opponent.screenName);
                    }

                    if(data.toString().split(" ")[0].equals("PLAY-REQUEST:")){
                        String nextPlayerName = data.toString().split(" ")[1];
                        ConnThread nextPlayer = clientThreadMap.get(nextPlayerName);
                        if(isInGame && opponent != nextPlayer){
                            waitingList.add(nextPlayer);
                            send("PLAYER-WAITING", this.clientIndex);
                            send("WAIT", nextPlayer.clientIndex);
                        }
                        else if(!isInGame && opponent == nextPlayer){
                            isInGame = true;
                            if(waitingList.size() > 0 && opponent == waitingList.get(0)){
                                waitingList.remove(0);
                                if(waitingList.size() == 0){
                                    send("NO-PLAYERS-WAITING", this.clientIndex);
                                }
                            }
                            send("ACCEPTED", nextPlayer.clientIndex);
                        }
                    }

                    if (data.toString().equals("DISCONNECTED")) {
                        data = data.toString() + " " + screenName;
                        for (int i = clientIndex+1; i < clientThreadList.size(); i++) {
                            clientThreadList.get(i).clientIndex--;
                        }
                        clientThreadList.remove(clientIndex);
                        clientThreadMap.remove(screenName);
                        numClients--;
                        namesList.remove(screenName);
                        isConnected = false;
                        for(int i = 0; i < clientThreadList.size(); i++){
                            send("remove " + screenName, i);
                            send("DISCONNECTED " + screenName, i);
                        }

                        if(isInGame){
                            game.playerDisconnected(this);
                            isInGame = false;
                        }

                        callback.accept(data);
                    }
                    if(data.toString().split(" ")[0].equals("DISCONNECTED")){
                        String playerName = data.toString().split(" ")[1];
                        for(int i = 0; i < waitingList.size(); i++){
                            if(waitingList.get(i).screenName.equals(playerName)){
                                waitingList.remove(i);
                                break;
                            }
                        }
                        if(waitingList.size() == 0){
                            send("NO-PLAYERS-WAITING", this.clientIndex);
                        }
                    }

                    if(data.toString().equals("quit")){
                        if(this.opponent != null){
                            send("OPPONENT-QUIT", this.opponent.clientIndex);
                            this.opponent.opponent = null;
                            this.opponent = null;
                        }
                        if(isInGame) {
                            gameList.remove(this.game);
                            numGames--;
                            game.playerQuit(this);
                            isInGame = false;
                        }
                    }
                    if(isInGame) {
                        if (data.toString().equals("rock") || data.toString().equals("paper") ||
                                data.toString().equals("scissors") || data.toString().equals("lizard")
                                || data.toString().equals("spock")) {
                            this.played = data.toString();
                            this.hasPlayed = true;
                            data = game.evalGame();
                            if(data.toString().equals("END")){
                                gameList.remove(this.game);
                                numGames--;
                                game.resetGame();
                                callback.accept("END " + screenName + " " + opponent.screenName);
                                this.opponent.isInGame = false;
                                this.isInGame = false;
                            }
                        }
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
                send("OPPONENT-DISCONNECTED", p2);
            }
            else if(p2 == playerDisconnected){
                p2.setConnected(false);
                send("OPPONENT-DISCONNECTED", p1);
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

        void resetGame(){
            p1 = null;
            p2 = null;
            isActive = false;
            numPlayers = 0;
        }

        void startGame(){
            isActive = true;
            send("start", p1);
            send("start", p2);
        }

        Serializable evalGame(){
            Serializable data =  " ";
            if (p1.hasPlayed() && p2.hasPlayed()) {
                send(p2.getPlayed(), p1);
                send(p1.getPlayed(), p2);
                int winner = findWinner(p1.getPlayed(), p2.getPlayed());
                if (winner == 2) {
                    send("WIN", p2);
                    send("LOSE", p1);
                } else if (winner == 1) {
                    send("WIN", p1);
                    send("LOSE", p2);
                } else {
                    send("TIE", p1);
                    send("TIE", p2);
                }
                data = "END";
                p1.setHasPlayed(false);
                p2.setHasPlayed(false);
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