package server;

import java.io.Serializable;

public class Game {

    private int ID;
    private int numPlayers;
    private Server.ConnThread p1;
    private Server.ConnThread p2;
    private boolean isActive;

    Game(int ID){
        this.ID = ID;
        isActive = false;
        numPlayers = 0;
    }


    Game(int ID, Server.ConnThread p1){
        this.ID = ID;
        this.p1 = p1;
        numPlayers = 1;
        isActive = false;
    }

    Game(int ID, Server.ConnThread p1, Server.ConnThread p2){
        this.ID = ID;
        this.p1 = p1;
        this.p2 = p2;
        this.numPlayers = 2;
        isActive = false;
    }

    public String getPlayerID(Server.ConnThread player){
        if(player == p1){
            return "PLAYER 1";
        }
        else if(player == p2){
            return "PLAYER 2";
        }
        return "";
    }

    public int getNumPlayers(){ return this.numPlayers; }
    public Server.ConnThread getPlayer1(){
        return p1;
    }
    public Server.ConnThread getPlayer2(){
        return p2;
    }


    public boolean isActive(){ return this.isActive; }
    public int getGameID(){ return ID; }
    public void send(Serializable data, Server.ConnThread player){
        try {
            player.out.writeObject(data);
        }catch(Exception e){
            System.out.println("unable to send data: " + data.toString());
        }
    }

    void addPlayer(Server.ConnThread player){
        if(numPlayers == 0){
            p1 = player;
        }
        else if(numPlayers == 1){
            p2 = player;
        }
        numPlayers++;
        if(numPlayers == 2){
            isActive = true;
        }
    }


    void playerDisconnected(Server.ConnThread playerDisconnected){
        if(p1 == playerDisconnected){
            p1.setConnected(false);
            send("playerDisconnected", p2);
        }
        else if(p2 == playerDisconnected){
            p2.setConnected(false);
            send("playerDisconnected", p2);
        }
        numPlayers--;
    }

    void playerQuit(Server.ConnThread playerQuit){
        if(p1 == playerQuit){
            p1.setConnected(false);
            send("opponent quit", p2);
        }
        else if(p2 == playerQuit){
            p2.setConnected(false);
            send("opponent quit", p2);
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
