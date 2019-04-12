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
import java.util.ArrayList;
import java.util.HashMap;

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

    private HashMap<String, clientDisplay> clientDisplayMap = new HashMap<>();
    private ArrayList<clientDisplay> clientDisplayList = new ArrayList<>();

    private VBox clientInfoBox;
    private BorderPane serverPane;
    private Scene startScene;

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        launch(args);
    }

    private VBox createClientInfoTable(){

        Label nameHeader = new Label ("User");
        nameHeader.setPrefSize(100, 30);
        nameHeader.setAlignment(Pos.CENTER);
        nameHeader.setTextAlignment(TextAlignment.CENTER);
        Label connHeader = new Label("Connected?");
        connHeader.setPrefSize(100, 30);
        connHeader.setAlignment(Pos.CENTER);
        Label oppHeader = new Label("Opponent");
        oppHeader.setPrefSize(100, 30);
        oppHeader.setAlignment(Pos.CENTER);
        Label statusHeader = new Label("Status");
        statusHeader.setPrefSize(100, 30);
        statusHeader.setAlignment(Pos.CENTER);

        HBox headers = new HBox(3, nameHeader, connHeader, oppHeader, statusHeader);
        headers.setAlignment(Pos.CENTER);
        this.clientInfoBox = new VBox(3, headers);

        for(int i = 0; i < clientDisplayList.size(); i++){
            clientInfoBox.getChildren().add(clientDisplayList.get(i).getClientDisplay());
        }
        return this.clientInfoBox;
    }


    private Parent createStartContent() {
        this.serverPane = new BorderPane();
        this.serverPane.setPrefSize(600, 600);

        Label title = new Label("ROCK ~ PAPER ~ SCISSORS ~ LIZARD ~ SPOCK");
        Label welcome = new Label("(server)");
        title.setPrefSize(400, 30);
        welcome.setPrefSize(300, 30);
        title.setAlignment(Pos.CENTER);
        welcome.setAlignment(Pos.CENTER);
        status.setText("welcome :-)");
        status.setPrefSize(400, 40);
        status.setTextAlignment(TextAlignment.CENTER);
        status.setWrapText(true);
        status.setAlignment(Pos.CENTER);

        portInputLabel.setPrefSize(60, 30);
        inputPort.setPrefSize(100, 30);
        serverOn.setDisable(true);
        serverOff.setDisable(true);
        serverOn.setPrefSize(100, 30);
        serverOff.setPrefSize(100, 30);


        numConnected.setPrefSize(30, 40);
        numConnected.setAlignment(Pos.CENTER);
        numConnectedLabel.setPrefSize(150, 30);
        HBox numConnBox = new HBox(numConnectedLabel, numConnected);
        numConnBox.setAlignment(Pos.CENTER);
        VBox gameInfo = new VBox(10, numConnBox);
        gameInfo.setAlignment(Pos.CENTER);

        HBox serverInput = new HBox(5, portInputLabel, inputPort);
        serverInput.setAlignment(Pos.CENTER);
        HBox userOptions = new HBox(10, serverOn, serverOff);
        userOptions.setAlignment(Pos.CENTER);
        VBox centerBox = new VBox(10, serverInput, userOptions, gameInfo);
        centerBox.setAlignment(Pos.TOP_CENTER);
        VBox heading = new VBox(5, title, welcome, status, centerBox);
        heading.setAlignment(Pos.CENTER);
        serverPane.setTop(heading);
        createClientInfoTable();
        this.clientInfoBox.setAlignment(Pos.TOP_CENTER);
        serverPane.setCenter(this.clientInfoBox);

        return this.serverPane;

    }


    @Override
    public void start(Stage primaryStage){
        // TODO Auto-generated method stub
        this.startScene = new Scene(createStartContent());
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

        primaryStage.setScene(this.startScene);
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
                else if(data.toString().split(" ")[0].equals("CONNECTED")){
                    messages.appendText(data.toString() + "\n");
                    String playerName = data.toString().split(" ")[1];
                    clientDisplay c = new clientDisplay(playerName);
                    c.connectedLabel.setText("✔");
                    clientDisplayList.add(c);
                    clientDisplayMap.put(playerName, c);
                    numConnected.setText("" + Server.numClients);
                    serverPane.setCenter(createClientInfoTable());
                }
                if(data.toString().split(" ")[0].equals("DISCONNECTED")){
                    messages.appendText(data.toString() + "\n");
                    String playerName = data.toString().split(" ")[1];
                    clientDisplayList.remove(clientDisplayMap.get(playerName));
                    clientDisplayMap.remove(playerName);
                    serverPane.setCenter(createClientInfoTable());
                }
                if(data.toString().split(" ")[0].equals("START")){
                    String playerName = data.toString().split(" ")[1];
                    String oppName = data.toString().split(" ")[2];
                    clientDisplayMap.get(playerName).statusLabel.setText("*playing*");
                    clientDisplayMap.get(oppName).statusLabel.setText("*playing*");
                    serverPane.setCenter(createClientInfoTable());
                }
                if(data.toString().split(" ")[0].equals("END")){
                    String playerName = data.toString().split(" ")[1];
                    String oppName = data.toString().split(" ")[2];
                    clientDisplayMap.get(playerName).statusLabel.setText("...");
                    clientDisplayMap.get(oppName).statusLabel.setText("...");
                    clientDisplayMap.get(playerName).oppLabel.setText(" ~ ");
                    clientDisplayMap.get(oppName).oppLabel.setText(" ~ ");
                    serverPane.setCenter(createClientInfoTable());
                }
                if(data.toString().split(" ")[0].equals("OPPONENT:")){
                    String oppName = data.toString().split(" ")[1];
                    String playerName = data.toString().split(" ")[2];
                    clientDisplayMap.get(playerName).oppLabel.setText(oppName);
                    serverPane.setCenter(createClientInfoTable());
                }
                System.out.println(data);
            });
        });
    }

    class clientDisplay {

            HBox clientStats;
            String connected, status, name, opponent;
            Label screenNameLabel, connectedLabel, oppLabel, statusLabel;

        clientDisplay(String name){
           this.name = name;
           this.opponent = " ~ ";
           this.connected = " ~ ";
           this.status = " ~ ";
           this.opponent = " ~ ";
           screenNameLabel = new Label(this.name);
           screenNameLabel.setPrefSize(100, 30);
           screenNameLabel.setAlignment(Pos.CENTER);
           connectedLabel = new Label(connected);
           connectedLabel.setPrefSize(100, 30);
           connectedLabel.setAlignment(Pos.CENTER);
           oppLabel = new Label(opponent);
           oppLabel.setPrefSize(100, 30);
           oppLabel.setAlignment(Pos.CENTER);
           statusLabel = new Label(status);
           statusLabel.setPrefSize(100, 30);
           statusLabel.setAlignment(Pos.CENTER);
           clientStats = new HBox(3, this.screenNameLabel, this.connectedLabel, this.oppLabel, this.statusLabel);
           clientStats.setAlignment(Pos.CENTER);
        }

        Parent getClientDisplay(){
            this.clientStats = new HBox(3, this.screenNameLabel, this.connectedLabel, this.oppLabel, this.statusLabel);
            this.clientStats.setAlignment(Pos.CENTER);
            return this.clientStats;
        }


    }
}