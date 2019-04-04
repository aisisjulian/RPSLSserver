import org.junit.jupiter.api.BeforeEach;
import server.Server;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.function.BinaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ServerTest {

    Server server;
    @BeforeEach
    public void serverConstructorTest() {
        final String data = " ";
        server = new Server(8080, d->{});
        assertNotNull(server, "server constructor failed");
    }

    @Test
    public void getPortTest() {
        assertEquals( server.getPort(), 8080, "getPortTest() failed");
    }

    @Test
    public void findWinnerTest1() {
        int winner = server.findWinner("rock", "paper");
        assertEquals(winner, 2, "findWinnerTest() failed");
    }

    @Test
    public void findWinnerTest2() {
        int winner = server.findWinner("paper", "rock");
        assertEquals(winner, 1, "findWinnerTest() failed");
    }
    @Test
    public void findWinnerTest3() {
        int winner = server.findWinner("scissors", "paper");
        assertEquals(winner, 1, "findWinnerTest() failed");
    }
    @Test
    public void findWinnerTest4() {
        int winner = server.findWinner("scissors", "rock");
        assertEquals(winner, 2, "findWinnerTest() failed");
    }
}