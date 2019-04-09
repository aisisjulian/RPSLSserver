package server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.ServerSocket;

/*this is aris comment*/
//this is aris comments 4/5
public class serverFX extends Application {

    private Server server;
    private boolean player1Connected = false;
    private boolean player2Connected = false;
    private ServerSocket ss;
    private TextArea messages = new TextArea();

    private TextField inputPort = new TextField();
    private Label portInputLabel = new Label("Port #:");

    private Button serverOn = new Button("ON");
    private Button serverOff = new Button("OFF");

    private boolean isServerOn = false;
    private Label status = new Label();

    private Label numConnectedLabel = new Label("Clients Connected: ");
    private Label numConnected = new Label(" ~ ");

    private int c1score = 0, c2score = 0;

    private Label hasWon = new Label("NO WINNER YET");
    private Label c1 = new Label ("**** PLAYER 1 ****");
    private Label c1Conn = new Label("NOT CONNECTED");
    private Label c1PointsLabel = new Label("Points:");
    private Label c1Pts = new Label(" ~ ");
    private Label c1PlayAgainLabel = new Label("Playing Again? ");
    private Label c1pa = new Label(" ~ ");
    private Label c2 = new Label ("**** PLAYER 2 ****");
    private Label c2Conn = new Label("NOT CONNECTED");
    private Label c2PointsLabel = new Label("Points:");
    private Label c2Pts = new Label("  ~  ");
    private Label c2PlayAgainLabel = new Label("Playing Again? ");
    private Label c2pa = new Label(" ~ ");

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        launch(args);
    }

    private Parent createStartContent() {
        BorderPane serverPane = new BorderPane();

        Label title = new Label("ROCK ~ PAPER ~ SCISSORS ~ LIZARD ~ SPOCK");
        Label welcome = new Label("(server)");
        title.setPrefSize(400, 30);
        welcome.setPrefSize(300, 30);
        title.setAlignment(Pos.CENTER);
        welcome.setAlignment(Pos.CENTER);
        status.setText("welcome :-)");
        status.setPadding(new Insets(30));
        status.setPrefSize(400, 100);
        status.setTextAlignment(TextAlignment.CENTER);
        status.setWrapText(true);
        status.setAlignment(Pos.CENTER);
        VBox heading = new VBox(5, title, welcome, status);
        heading.setAlignment(Pos.CENTER);

        portInputLabel.setPrefSize(60, 30);
        inputPort.setPrefSize(100, 30);
        serverOn.setDisable(true);
        serverOff.setDisable(true);
        serverOn.setPrefSize(100, 30);
        serverOff.setPrefSize(100, 30);


        numConnected.setPrefSize(30, 40);
        numConnected.setAlignment(Pos.CENTER);
        numConnectedLabel.setPrefSize(150, 40);
        HBox numConnBox = new HBox(numConnectedLabel, numConnected);
        numConnBox.setAlignment(Pos.CENTER);
        hasWon.setPrefSize(150, 40);
        hasWon.setAlignment(Pos.CENTER);
        hasWon.setWrapText(true);
        VBox gameInfo = new VBox(10, numConnBox, hasWon);
        gameInfo.setAlignment(Pos.CENTER);

        HBox serverInput = new HBox(5, portInputLabel, inputPort);
        serverInput.setAlignment(Pos.CENTER);
        HBox userOptions = new HBox(10, serverOn, serverOff);
        userOptions.setAlignment(Pos.CENTER);
        VBox centerBox = new VBox(10, serverInput, userOptions, gameInfo);
        centerBox.setAlignment(Pos.TOP_CENTER);
        centerBox.setPadding(new Insets(30));
        serverPane.setCenter(centerBox);
        serverPane.setTop(heading);
        serverPane.setPrefSize(800, 600);
        serverPane.setPadding(new Insets(10));

        c1.setPrefSize(200, 50);
        c1.setAlignment(Pos.CENTER);
        c1.setPadding(new Insets(10));
        c1Conn.setPrefSize(200, 40);
        c1Conn.setWrapText(true);
        c1Conn.setAlignment(Pos.CENTER);
        c1PointsLabel.setPrefSize(100, 20);
        c1PointsLabel.setAlignment(Pos.CENTER_RIGHT);
        c1Pts.setAlignment(Pos.CENTER_LEFT);
        c1Pts.setPrefSize(100, 20);
        c1Pts.setText(" " + c1score + " ");
        HBox c1Points = new HBox(c1PointsLabel, c1Pts);
        c1Points.setAlignment(Pos.CENTER);
        c1PlayAgainLabel.setPrefSize(150, 20);
        c1PlayAgainLabel.setAlignment(Pos.CENTER_RIGHT);
        c1pa.setPrefSize(50, 20);
        c1pa.setAlignment(Pos.CENTER_LEFT);
        HBox c1PlayAgain = new HBox(c1PlayAgainLabel, c1pa);
        c1PlayAgain.setAlignment(Pos.CENTER);
        VBox client1Info = new VBox(10, c1, c1Conn, c1Points, c1PlayAgain);
        client1Info.setAlignment(Pos.CENTER);
        serverPane.setLeft(client1Info);


        c2.setPrefSize(200, 50);
        c2.setAlignment(Pos.CENTER);
        c2.setPadding(new Insets(10));
        c2Conn.setPrefSize(200, 40);
        c2Conn.setWrapText(true);
        c2Conn.setAlignment(Pos.CENTER);
        c2PointsLabel.setPrefSize(100, 20);
        c2PointsLabel.setAlignment(Pos.CENTER_RIGHT);
        c2Pts.setAlignment(Pos.CENTER_LEFT);
        c2Pts.setPrefSize(100, 20);
        c2Pts.setText(" " + c2score + " ");
        HBox c2Points = new HBox(c2PointsLabel, c2Pts);
        c2Points.setAlignment(Pos.CENTER);
        c2PlayAgainLabel.setPrefSize(150, 20);
        c2PlayAgainLabel.setAlignment(Pos.CENTER_RIGHT);
        c2pa.setPrefSize(50, 20);
        c2pa.setAlignment(Pos.CENTER_LEFT);
        HBox c2PlayAgain = new HBox(c2PlayAgainLabel, c2pa);
        c2PlayAgain.setAlignment(Pos.CENTER);
        VBox client2Info = new VBox(10, c2, c2Conn, c2Points, c2PlayAgain);
        client2Info.setAlignment(Pos.CENTER);
        serverPane.setRight(client2Info);
        return serverPane;

    }


    @Override
    public void start(Stage primaryStage){
        // TODO Auto-generated method stub
        Scene startScene = new Scene(createStartContent());
        inputPort.setOnAction(actionEvent -> {
            try {
                server = createServer(Integer.valueOf(inputPort.getText()));
                inputPort.clear();
                inputPort.setVisible(false);
                portInputLabel.setVisible(false);
                serverOn.setDisable(false);
                isServerOn = true;
            }
            catch(Exception e){

            }

        });

        serverOn.setOnAction(event -> {
            serverOn.setDisable(true);
                try {
                    if(isServerOn) {
                        this.ss = new ServerSocket(server.getPort());
                        startServer();
                        Server.numClients = 0;
                        serverOff.setDisable(false);
                        status.setText("Server: ON");
                    }
                 }
                catch (IOException e) {
                    inputPort.setVisible(true);
                    portInputLabel.setVisible(true);
                    serverOff.setDisable(true);
                    status.setText("connection failed");
                }
        });

        serverOff.setOnAction(event -> {
            try {
                isServerOn = false;
                server.closeConn();
                this.ss.close();
                inputPort.setVisible(true);
                portInputLabel.setVisible(true);
                status.setText("Server: OFF");
                serverOff.setDisable(true);
            }
            catch (Exception e){
                status.setText("FAILED TO TURN SERVER OFF");
            }
            serverOff.setDisable(true);
            serverOn.setDisable(true);
        });

        primaryStage.setScene(startScene);
        primaryStage.show();
    }

    public void startServer() {
           Runnable task = () -> server.startConn(this.ss);
           Thread t = new Thread(task);
           t.setDaemon(true);
           t.start();
    }

   private Server createServer(int port) {
        return new Server(port, data-> {
            Platform.runLater(()->{
                numConnected.setText(" " + Server.numClients);
                System.out.println(Server.numClients);
                if(data.toString().equals("NO CONNECTION")){
                    messages.appendText(data.toString() + "\n");
                }
                else if(data.toString().equals("CONNECTED")){
                    messages.appendText(data.toString() + "\n");
                    if(Server.numClients == 1 && !player1Connected){
                        status.setText("PLAYER 1 CONNECTED :-)");
                        c1Conn.setText("CONNECTED");
                        player1Connected = true;
                    }else if(Server.numClients == 2 && player2Connected){
                        status.setText("PLAYER 1 CONNECTED :-)");
                        c1Conn.setText("CONNECTED");
                        player1Connected = true;
                    }
                    else{
                        status.setText("GAME STARTED");
                        c2Conn.setText("CONNECTED");
                        hasWon.setText("NO WINNER YET");
                        c1pa.setText(" ~ ");
                        c1pa.setText(" ~ ");
                        player2Connected = true;
                    }
                    numConnected.setText("" + Server.numClients);
                }
                if(data.toString().equals("disconnected PLAYER 1")){
                        status.setText("waiting for players to connect...");
                        c1Conn.setText("NOT CONNECTED");
                        player1Connected = false;
                        c1Pts.setText(" ~ ");
                        c1score = 0;
                        c2Pts.setText(" ~ ");
                        c2score = 0;
                        hasWon.setText("NO WINNER YET");
                }
                else if(data.toString().equals("disconnected PLAYER 2")){
                    status.setText("waiting for players to connect...");
                    c2Conn.setText("NOT CONNECTED");
                    player2Connected = false;
                    c1Pts.setText(" ~ ");
                    c1score = 0;
                    c2Pts.setText(" ~ ");
                    c2score = 0;
                    c1pa.setText(" ~ ");
                    c1pa.setText(" ~ ");
                    hasWon.setText("NO WINNER YET");
                }
                if(data.toString().equals("PLAYER 1")){
                    c1score++;
                   c1Pts.setText(" " + c1score);
                }
                if(data.toString().equals("PLAYER 2")){
                    c2score++;
                   c2Pts.setText(" " + c2score);
                }
                if(data.toString().equals("WIN1")){
                    hasWon.setText("WINNER: PLAYER 1 !!!");
                }
                if(data.toString().equals("WIN2")){
                    hasWon.setText("WINNER: PLAYER 2 !!!");
                }
                if(data.toString().equals("playing again PLAYER 1")){
                    c1pa.setText("✔️");
                }
                if(data.toString().equals("playing again PLAYER 2")){
                    c1pa.setText("✔️");
                }
                System.out.println(data);
            });
        });
    }
}