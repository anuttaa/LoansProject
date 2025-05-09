package server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import exeption.AuthExeption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.DTO.BankDTO;
import server.DTO.LoanDTO;
import server.DTO.PaymentScheduleDTO;
import server.DTO.UserDTO;
import server.Entities.Bank;
import server.Entities.Loan;
import server.Entities.Payment;
import server.Entities.User;
import server.service.BankService;
import server.service.LoanService;
import server.service.PaymentService;
import server.service.UserService;


import java.io.*;
import java.net.*;
import java.util.*;
import java.util.function.BiFunction;

public class ClientHandler implements Runnable {
    private static final Gson gson = new Gson();
    private static final Map<String, BiFunction<JsonObject, String, String>> handlers = new HashMap<>();

    private final Socket clientSocket;
    private final BufferedReader reader;
    private final PrintWriter writer;

    private final UserService userService;
    private final PaymentService paymentService;
    private final LoanService loanService;
    private final BankService bankService;
    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    public ClientHandler(Socket clientSocket, UserService userService, PaymentService paymentService, LoanService loanService, BankService bankService) throws IOException {
        this.clientSocket = clientSocket;
        this.userService = userService;
        this.paymentService = paymentService;
        this.loanService = loanService;
        this.bankService = bankService;
        this.reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        this.writer = new PrintWriter(clientSocket.getOutputStream(), true);

        handlers.put("register", this::handleRegister);
        handlers.put("login", this::handleLogin);
        handlers.put("addBank", this::handleAddBank);
        handlers.put("findUserById", this::handleFindUserById);
        handlers.put("updateUser", this::handleUpdateUser);
        handlers.put("deleteUser", this::handleDeleteUser);
        handlers.put("generateSchedule", this::handleGenerateInitialSchedule);
        handlers.put("regenerateSchedule", this::handleRegenerateSchedule);
        handlers.put("calculateEffectiveInterestRate", this::handleCalculateEffectiveInterestRate);
        handlers.put("createLoan", this::handleCreateLoan);
        handlers.put("addClientToLoan", this::handleAddClientToLoan);
        handlers.put("findAllUsers", this::handleFindAllUsers);
        handlers.put("GET_BANKS", this::handleGetBanks);
    }

    @Override
    public void run() {
        try {
            String message;
            while ((message = reader.readLine()) != null) {
                JsonObject data = gson.fromJson(message, JsonObject.class);
                String command = data.get("command").getAsString();
                String response = dispatch(command, data);
                writer.println(response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
                writer.close();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String dispatch(String command, JsonObject data) {
        BiFunction<JsonObject, String, String> handler = handlers.get(command);
        if (handler == null) {
            return errorResponse("Unknown command: " + command);
        }
        return handler.apply(data, "");
    }

    private String handleRegister(JsonObject data, String unused) {  // Убрали второй параметр
        try {
            System.out.println("[SERVER] Received full request: " + data);

            if (!data.has("user")) {
                System.err.println("[SERVER] User object is missing");
                return errorResponse("User data is missing");
            }

            JsonObject userJson = data.getAsJsonObject("user");
            UserDTO userDTO = gson.fromJson(userJson, UserDTO.class);

            if (userDTO.getUsername() == null || userDTO.getPassword() == null) {
                return errorResponse("Username and password are required");
            }

            User registeredUser = userService.register(userDTO);
            return successResponse(registeredUser);

        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse("Registration error: " + e.getMessage());
        }
    }

    private String successResponse(User user) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "success");
        response.add("user", gson.toJsonTree(user));
        return response.toString();
    }

    private String handleLogin(JsonObject data, String unused) {
        try {
            JsonObject authData = data.getAsJsonObject("data");
            if (authData == null) {
                return errorResponse("Authentication data is missing");
            }

            String username = authData.get("username").getAsString();
            String password = authData.get("password").getAsString();

            UserDTO userDTO = userService.login(username, password);

            JsonObject response = new JsonObject();
            response.addProperty("status", "success");
            response.add("user", gson.toJsonTree(userDTO));
            return gson.toJson(response);

        } catch (NullPointerException e) {
            return errorResponse("Username and password are required");
        } catch (AuthExeption e) {
            return errorResponse("Login failed: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse("Internal server error");
        }
    }

    private String handleAddBank(JsonObject data, String unused) {
        try {
            if (data.has("bankName") && data.has("address") && data.has("phone") && data.has("email")) {
                String bankName = data.get("bankName").getAsString();
                String address = data.get("address").getAsString();
                String phone = data.get("phone").getAsString();
                String email = data.get("email").getAsString();

                BankDTO bankDTO = new BankDTO();
                bankDTO.setBankName(bankName);
                bankDTO.setAddress(address);
                bankDTO.setPhone(phone);
                bankDTO.setEmail(email);

                Bank createdBank = bankService.createBank(bankDTO);
                return gson.toJson(createdBank);
            } else {
                return errorResponse("Missing required fields in the request");
            }
        } catch (Exception e) {
            return errorResponse("Error processing request: " + e.getMessage());
        }
    }


    private String handleFindUserById(JsonObject data, String unused) {
        try {
            LOG.debug("Received data: {}", data.toString());

            if (data.has("userId") && data.get("userId") != null && !data.get("userId").isJsonNull()) {
                Long userId = data.get("userId").getAsLong();
                LOG.debug("Extracted userId: {}", userId);

                Optional<UserDTO> userOptional = userService.findById(userId);

                if (!userOptional.isPresent()) {
                    return errorResponse("User not found");
                }

                UserDTO userDTO = userOptional.get();

                return gson.toJson(userDTO);
            } else {
                return errorResponse("UserId нет в запросе");
            }
        } catch (Exception e) {
            return errorResponse("Ошибка: " + e.getMessage());
        }
    }

    private String handleUpdateUser(JsonObject data, String unused) {
        UserDTO userDTO = gson.fromJson(data.get("user"), UserDTO.class);
        try {
            UserDTO updated = userService.update(userDTO);
            return gson.toJson(updated);
        } catch (RuntimeException e) {
            return errorResponse("Обновление не удалось: " + e.getMessage());
        }
    }

    private String handleDeleteUser(JsonObject data, String unused) {
        try {
            if (!data.has("userId") || data.get("userId").isJsonNull()) {
                return errorResponse("userId пустой");
            }

            Long userId = data.get("userId").getAsLong();

            UserDTO userDTO = new UserDTO();
            userDTO.setUserId(userId);
            userService.delete(userDTO);

            JsonObject result = new JsonObject();
            result.addProperty("status", "success");
            result.addProperty("message", "Пользователь удален");
            return gson.toJson(result);
        } catch (RuntimeException e) {
            return errorResponse("Не удалилось: " + e.getMessage());
        }
    }


    private String handleGenerateInitialSchedule(JsonObject data, String unused) {
        try {
            Loan loan = gson.fromJson(data.get("loan"), Loan.class);
            List<PaymentScheduleDTO> schedule = paymentService.generateInitialSchedule(loan);
            return gson.toJson(schedule);
        } catch (Exception e) {
            return errorResponse("Генерация расписания не удалась: " + e.getMessage());
        }
    }

    private String handleRegenerateSchedule(JsonObject data, String unused) {
        try {
            Loan loan = gson.fromJson(data.get("loan"), Loan.class);
            List<Payment> payments = Arrays.asList(gson.fromJson(data.get("payments"), Payment[].class));
            List<PaymentScheduleDTO> schedule = paymentService.regenerateSchedule(loan, payments);
            return gson.toJson(schedule);
        } catch (Exception e) {
            return errorResponse("SГенерация расписания не удалась: " + e.getMessage());
        }
    }

    private String handleCalculateEffectiveInterestRate(JsonObject data, String unused) {
        Long loanId = data.get("loanId").getAsLong();
        try {
            Loan loan = loanService.getLoanById(loanId);

            double rate = loanService.calculateEffectiveInterestRate(loan);

            JsonObject result = new JsonObject();
            result.addProperty("effectiveRate", rate);
            return gson.toJson(result);

        } catch (NoSuchElementException e) {
            return errorResponse("Не найден кредит с ID: " + loanId);
        } catch (Exception e) {
            return errorResponse("Расчет не удался: " + e.getMessage());
        }
    }

    private String handleCreateLoan(JsonObject data, String unused) {
        try {
            LoanDTO loanDTO = gson.fromJson(data.get("loan"), LoanDTO.class);
            Long bankId = data.get("bankId").getAsLong();

            Loan created = loanService.createLoan(loanDTO, bankId);
            return gson.toJson(created);
        } catch (RuntimeException e) {
            return errorResponse("Невозможно создать кредит: " + e.getMessage());
        }
    }

    private String handleAddClientToLoan(JsonObject data, String unused) {
        try {
            Long loanId = data.get("loanId").getAsLong();
            Long clientId = data.get("clientId").getAsLong();

            loanService.addClientToLoan(loanId, clientId);

            JsonObject result = new JsonObject();
            result.addProperty("status", "success");
            result.addProperty("message", "Клиенту добавлен кредит");
            return gson.toJson(result);
        } catch (RuntimeException e) {
            return errorResponse("Ошибка добавления кредита клиенту: " + e.getMessage());
        }
    }

    private String handleFindAllUsers(JsonObject data, String unused) {
        try {
            List<UserDTO> userDTOs = userService.findAll();
            JsonArray usersArray = new JsonArray();

            for (UserDTO userDTO : userDTOs) {
                JsonObject userJson = new JsonObject();
                userJson.addProperty("userId", userDTO.getUserId());
                userJson.addProperty("username", userDTO.getUsername());
                userJson.addProperty("fullName", userDTO.getFullName());
                userJson.addProperty("password", userDTO.getPassword());
                userJson.addProperty("birthDate", userDTO.getBirthDate().toString());
                userJson.addProperty("phone", userDTO.getPhone());
                userJson.addProperty("address", userDTO.getAddress());
                userJson.addProperty("roleId", userDTO.getRoleId());
                userJson.addProperty("email", userDTO.getEmail());

                JsonArray bankIdsArray = new JsonArray();
                for (Long bankId : userDTO.getBankIds()) {
                    bankIdsArray.add(bankId);
                }
                userJson.add("bankIds", bankIdsArray);

                usersArray.add(userJson);
            }

            JsonObject response = new JsonObject();
            response.addProperty("status", "success");
            response.add("users", usersArray);
            return gson.toJson(response);
        } catch (Exception e) {
            return errorResponse("Невозможно найти пользователей: " + e.getMessage());
        }
    }


    private String handleGetBanks(JsonObject data, String unused) {
        try {
            List<BankDTO> banks = bankService.findAll();

            JsonArray banksArray = new JsonArray();
            for (BankDTO bank : banks) {
                JsonObject bankJson = new JsonObject();
                bankJson.addProperty("id", bank.getBankId());
                bankJson.addProperty("name", bank.getBankName());
                bankJson.addProperty("address", bank.getAddress());
                bankJson.addProperty("phone", bank.getPhone());
                bankJson.addProperty("email", bank.getEmail());
                banksArray.add(bankJson);
            }

            JsonObject responseData = new JsonObject();
            responseData.add("banks", banksArray);

            return new Gson().toJson(responseData);
        } catch (Exception e) {
            LOG.error("Ошибка при получении списка банков", e);
            return errorResponse("Ошибка при получении списка банков: " + e.getMessage());
        }
    }

    private String errorResponse(String message) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "error");
        response.addProperty("message", message);
        return gson.toJson(response);
    }
}



