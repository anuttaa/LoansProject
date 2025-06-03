package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.ClientHandler;
import server.service.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static final int PORT = 5555;

    public static void main(String[] args) {
        UserService userService = new UserService();
        BankService bankService = new BankService();
        LoanService loanService = new LoanService();
        LoanTypeService loanTypeService = new LoanTypeService();
        PaymentService paymentService = new PaymentService();
        LoanStatisticsService loanStatisticsService = new LoanStatisticsService();
        userService.initializeFirstAdmin();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Сервер запущен на порту " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Подключился клиент: " + clientSocket.getInetAddress());

                new Thread(new ClientHandler(clientSocket, userService, paymentService, loanService, loanTypeService, bankService, loanStatisticsService)).start();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
