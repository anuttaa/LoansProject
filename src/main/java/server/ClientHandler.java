package server;

import com.google.gson.*;
import config.HibernateProxyTypeAdapter;
import config.LocalDateAdapter;
import enums.PaymentType;
import exeption.AuthExeption;
import exeption.PaymentValidationException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.DTO.*;
import server.Entities.*;
import server.service.*;

import java.io.*;
import java.math.BigDecimal;
import java.net.*;
import java.time.LocalDate;
import java.util.*;
import java.util.function.BiFunction;

public class ClientHandler implements Runnable {
    Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .registerTypeAdapter(HibernateProxy.class, new HibernateProxyTypeAdapter())
            .registerTypeAdapter(BigDecimal.class, (JsonDeserializer<BigDecimal>)
                    (json, type, context) -> {
                        try {
                            return new BigDecimal(json.getAsString());
                        } catch (NumberFormatException e) {
                            throw new JsonParseException("Invalid BigDecimal value: " + json.getAsString());
                        }
                    })
            .create();
    private static final Map<String, BiFunction<JsonObject, String, String>> handlers = new HashMap<>();

    private final Socket clientSocket;
    private final BufferedReader reader;
    private final PrintWriter writer;

    private final UserService userService;
    private final PaymentService paymentService;
    private final LoanService loanService;
    private final LoanTypeService loanTypeService;
    private final BankService bankService;
    private final LoanStatisticsService loanStatisticsService;
    private Long currentUserId;
    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    public ClientHandler(Socket clientSocket, UserService userService, PaymentService paymentService, LoanService loanService, LoanTypeService loanTypeService, BankService bankService, LoanStatisticsService loanStatisticsService) throws IOException {
        this.clientSocket = clientSocket;
        this.userService = userService;
        this.paymentService = paymentService;
        this.loanService = loanService;
        this.loanTypeService = loanTypeService;
        this.bankService = bankService;
        this.loanStatisticsService = loanStatisticsService;
        this.reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        this.writer = new PrintWriter(clientSocket.getOutputStream(), true);

        handlers.put("register", this::handleRegister);
        handlers.put("login", this::handleLogin);
        handlers.put("addBank", this::handleAddBank);
        handlers.put("findUserById", this::handleFindUserById);
        handlers.put("updateUser", this::handleUpdateUser);
        handlers.put("deleteUser", this::handleDeleteUser);
        handlers.put("calculateEffectiveInterestRate", this::handleCalculateEffectiveInterestRate);
        handlers.put("createLoanType", this::handleCreateLoanType);
        handlers.put("addClientToLoan", this::handleAddClientToLoan);
        handlers.put("findAllUsers", this::handleFindAllUsers);
        handlers.put("getBanks", this::handleGetBanks);
        handlers.put("getClientLoans", this::handleGetClientLoans);
        handlers.put("getLoanTypesByBank", this::handleGetLoanTypesByBank);
        handlers.put("getCurrentUser", this::handleGetCurrentUser);
        handlers.put("takeLoan", this::handleTakeLoan);
        handlers.put("getLoanTypes", this::handleGetLoanTypes);
        handlers.put("getFilteredLoanTypes", this::handleGetFilteredLoanTypes);
        handlers.put("processPayment", this::handleCreatePayment);
        handlers.put("getRemainingDebt", this::handleGetRemainingDebt);
        handlers.put("getNextPayment", this::handleGetNextPayment);
        handlers.put("getLoanDetails", this::handleGetLoanDetails);
        handlers.put("getPaymentSchedule", this::handleGetPaymentSchedule);
        handlers.put("getPaymentHistory", this::handleGetPaymentHistory);
        handlers.put("deleteLoan", this::handleDeleteLoan);
        handlers.put("getAllLoans", this::handleGetAllLoans);
        handlers.put("updateBank", this::handleUpdateBank);
        handlers.put("deleteBank", this::handleDeleteBank);
        handlers.put("updateLoan", this::handleUpdateLoan);
        handlers.put("updateLoanType", this::handleUpdateLoanType);
        handlers.put("getLoanStatistics", this::handleGetLoanStatistics);
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String message;
            while ((message = reader.readLine()) != null) {
                try {
                    JsonObject data = gson.fromJson(message, JsonObject.class);
                    if (data == null || !data.has("command")) {
                        writer.println(errorResponse("Неверный запрос: 'command' обязателен"));
                        continue;
                    }

                    String command = data.get("command").getAsString();
                    String response = dispatch(command, data);

                    writer.println(response);
                    if (writer.checkError()) {
                        throw new IOException("Клиент отключен");
                    }

                } catch (JsonSyntaxException e) {
                    writer.println(errorResponse("Неверный JSON: " + e.getMessage()));
                } catch (Exception e) {
                    LOG.error("Ошибка обработки запроса: {}", e.getMessage(), e);
                    writer.println(errorResponse("Ошибка сервера: " + e.getMessage()));
                }
            }

        } catch (SocketTimeoutException e) {
            LOG.warn("Client timeout: {}", clientSocket.getInetAddress());
        } catch (IOException e) {
            LOG.error("Ошибка клиента: {}", e.getMessage());
        } finally {
            try {
                clientSocket.close();
                LOG.info("Клиент отключен: {}", clientSocket.getInetAddress());
            } catch (IOException e) {
                LOG.error("Ошибка закрытия  сокета: {}", e.getMessage());
            }
        }
    }

    private String dispatch(String command, JsonObject data) {
        BiFunction<JsonObject, String, String> handler = handlers.get(command);
        if (handler == null) {
            LOG.warn("Unknown command: {}. Available commands: {}", command, handlers.keySet());
            return errorResponse("Unknown command: " + command);
        }
        return handler.apply(data, "");
    }

    private String handleRegister(JsonObject data, String unused) {
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
            return errorResponse("Ошибка регистрации: " + e.getMessage());
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
            return errorResponse("Логин и пароль обязательны");
        } catch (AuthExeption e) {
            return errorResponse("Вход не выполнен: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse("Ошибка сервера");
        }
    }

    private String handleAddBank(JsonObject data, String unused) {
        try {
            System.out.println("Incoming data: " + data.toString());
            JsonObject bankData = data.getAsJsonObject("data");

            // Проверяем обязательное поле bankName
            if (!bankData.has("bankName") || bankData.get("bankName").isJsonNull()
                    || bankData.get("bankName").getAsString().trim().isEmpty()) {
                return errorResponse("Название банка обязательно для заполнения");
            }

            // Получаем значения полей
            String bankName = bankData.get("bankName").getAsString().trim();
            String address = bankData.has("address") && !bankData.get("address").isJsonNull()
                    ? bankData.get("address").getAsString().trim() : null;
            String phone = bankData.has("phone") && !bankData.get("phone").isJsonNull()
                    ? bankData.get("phone").getAsString().trim() : null;
            String email = bankData.has("email") && !bankData.get("email").isJsonNull()
                    ? bankData.get("email").getAsString().trim() : null;

            // Создаем DTO
            BankDTO bankDTO = new BankDTO();
            bankDTO.setBankName(bankName);
            bankDTO.setAddress(address);
            bankDTO.setPhone(phone);
            bankDTO.setEmail(email);

            Bank createdBank = bankService.createBank(bankDTO);

            // Формируем успешный ответ
            JsonObject response = new JsonObject();
            response.addProperty("status", "success");
            response.add("bank", gson.toJsonTree(createdBank));
            return response.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse("Ошибка обработки запроса: " + e.getMessage());
        }
    }

    private String handleFindUserById(JsonObject data, String unused) {
        try {
            LOG.debug("Полученные данные: {}", data.toString());

            if (data.has("userId") && data.get("userId") != null && !data.get("userId").isJsonNull()) {
                Long userId = data.get("userId").getAsLong();
                LOG.debug("Получен userId: {}", userId);

                Optional<UserDTO> userOptional = userService.findById(userId);

                if (!userOptional.isPresent()) {
                    return errorResponse("Пользователь не найден");
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
        try {
            if (!data.has("data")) {
                return errorResponse("Данные User не найдены");
            }

            JsonObject userData = data.getAsJsonObject("data");
            UserDTO userDTO = gson.fromJson(userData, UserDTO.class);

            if (userDTO.getUsername() == null || userDTO.getEmail() == null) {
                return errorResponse("Имя пользователя и email обязательны");
            }

            UserDTO updatedUser = userService.update(userDTO);

            JsonObject response = new JsonObject();
            response.addProperty("status", "success");
            response.add("user", gson.toJsonTree(updatedUser));
            return gson.toJson(response);

        } catch (RuntimeException e) {
            return errorResponse("Обновление не выполнено: " + e.getMessage());
        }
    }

    private String handleUpdateBank(JsonObject data, String unused) {
        try {
            // 1. Проверка наличия данных
            if (!data.has("data")) {
                return errorResponse("Данные Bank не найдены");
            }

            // 2. Парсинг входных данных
            JsonObject bankData = data.getAsJsonObject("data");
            BankDTO bankDTO = gson.fromJson(bankData, BankDTO.class);

            // 3. Валидация обязательных полей
            if (bankDTO.getBankId() == null) {
                return errorResponse("ID банка обязателен");
            }
            if (bankDTO.getBankName() == null || bankDTO.getBankName().isEmpty()) {
                return errorResponse("Название банка обязательно");
            }
            if (bankDTO.getEmail() == null || bankDTO.getEmail().isEmpty()) {
                return errorResponse("Email обязателен");
            }

            // 4. Обновление банка через сервис
            BankDTO updatedBank = bankService.updateBank(bankDTO.getBankId(), bankDTO);

            // 5. Формирование успешного ответа
            JsonObject response = new JsonObject();
            response.addProperty("status", "success");
            response.add("bank", gson.toJsonTree(updatedBank));
            return gson.toJson(response);

        } catch (NoSuchElementException e) {
            LOG.error("Bank not found: " + e.getMessage());
            return errorResponse("Банк не найден");
        } catch (Exception e) {
            LOG.error("Error updating bank: " + e.getMessage());
            return errorResponse("Ошибка при обновлении банка: " + e.getMessage());
        }
    }

    private String handleDeleteUser(JsonObject data, String unused) {
        try {
            if (!data.has("data") || !data.getAsJsonObject("data").has("userId")) {
                return errorResponse("Не указан ID пользователя");
            }

            Long userId = data.getAsJsonObject("data").get("userId").getAsLong();

            UserDTO userDTO = new UserDTO();
            userDTO.setUserId(userId);

            userService.delete(userDTO);

            return newSuccessResponse("Удалено");

        } catch (EntityNotFoundException e) {
            return errorResponse("Пользователь не найден");
        } catch (IllegalStateException e) {
            return errorResponse(e.getMessage());
        } catch (Exception e) {
            LOG.error("Ошибка при удалении пользователя", e);
            return errorResponse("Ошибка сервера: " + e.getMessage());
        }
    }

    private String handleGetPaymentSchedule(JsonObject data, String unused) {
        try {
            Long loanId = data.get("loanId").getAsLong();

            // Получаем график платежей из сервиса
            List<PaymentScheduleDTO> schedule = paymentService.generateSchedule(loanId);

            // Формируем ответ
            JsonObject response = new JsonObject();
            response.addProperty("status", "success");
            response.add("schedule", gson.toJsonTree(schedule));

            // Логирование
            LOG.info("Successfully generated payment schedule for loan ID: {}", loanId);
            return gson.toJson(response);

        } catch (EntityNotFoundException e) {
            LOG.warn("Loan not found for schedule generation: {}", e.getMessage());
            return errorResponse("Кредит не найден: " + e.getMessage());

        } catch (IllegalStateException e) {
            LOG.error("Invalid loan state for schedule generation", e);
            return errorResponse("Невозможно сгенерировать график: " + e.getMessage());

        } catch (Exception e) {
            LOG.error("Error generating payment schedule", e);
            return errorResponse("Ошибка генерации графика: " + e.getMessage());
        }
    }

    private String handleGetPaymentHistory(JsonObject data, String unused) {
        try {
            Long loanId = data.get("loanId").getAsLong();

            // Получаем историю платежей из сервиса
            List<PaymentDTO> payments = paymentService.getPaymentHistory(loanId);

            // Формируем ответ
            JsonObject response = new JsonObject();
            response.addProperty("status", "success");
            response.add("payments", gson.toJsonTree(payments));

            // Логирование успешного выполнения
            LOG.info("Successfully retrieved payment history for loan ID: {}", loanId);
            return gson.toJson(response);

        } catch (EntityNotFoundException e) {
            LOG.warn("Payment history not found for loan: {}", e.getMessage());
            return errorResponse("История платежей не найдена: " + e.getMessage());

        } catch (JsonSyntaxException e) {
            LOG.error("JSON parsing error in payment history", e);
            return errorResponse("Ошибка формата данных");

        } catch (Exception e) {
            LOG.error("Error retrieving payment history", e);
            return errorResponse("Ошибка при получении истории платежей: " + e.getMessage());
        }
    }

    private String handleDeleteLoan(JsonObject data, String unused) {
        try {
            Long loanId = data.get("loanId").getAsLong();
            Long currentUserId = data.get("userId").getAsLong();

            String errorMessage = loanService.deleteLoan(loanId, currentUserId);

            JsonObject response = new JsonObject();

            if (errorMessage == null) {
                response.addProperty("status", "success");
                response.addProperty("message", "Кредит успешно удален");
            } else {
                response.addProperty("status", "error");
                response.addProperty("message", errorMessage);
            }

            return gson.toJson(response);

        } catch (JsonSyntaxException e) {
            LOG.error("Ошибка формата JSON при удалении кредита", e);
            return "{\"status\":\"error\",\"message\":\"Неверный формат данных\"}";
        } catch (Exception e) {
            LOG.error("Системная ошибка при удалении кредита", e);
            return "{\"status\":\"error\",\"message\":\"Внутренняя ошибка сервера\"}";
        }
    }

    private String handleGetRemainingDebt(JsonObject data, String unused) {
        try {
            Long loanId = data.get("loanId").getAsLong();
            BigDecimal remainingDebt = paymentService.calculateRemainingDebt(
                    loanService.getLoanById(loanId));

            JsonObject response = new JsonObject();
            response.addProperty("status", "success");
            response.addProperty("amount", remainingDebt.toString());
            return gson.toJson(response);
        } catch (Exception e) {
            return errorResponse("Ошибка расчета остатка: " + e.getMessage());
        }
    }

    private String handleGetNextPayment(JsonObject data, String unused) {
        try {
            Long loanId = data.get("loanId").getAsLong();
            Loan loan = loanService.getLoanById(loanId);
            PaymentScheduleDTO nextPayment = paymentService.getNextPaymentSchedule(loan);

            JsonObject response = new JsonObject();
            response.addProperty("status", "success");
            if (nextPayment != null) {
                response.add("nextPayment", gson.toJsonTree(nextPayment));
            }
            return gson.toJson(response);
        } catch (Exception e) {
            return errorResponse("Ошибка получения следующего платежа: " + e.getMessage());
        }
    }

    private String handleCalculateEffectiveInterestRate(JsonObject data, String unused) {
        Long loanId = data.get("loanId").getAsLong();
        try {
            Loan loan = loanService.getLoanById(loanId);

            BigDecimal rate = loanService.calculateEffectiveInterestRate(loan);

            JsonObject result = new JsonObject();
            result.addProperty("effectiveRate", rate);
            return gson.toJson(result);

        } catch (NoSuchElementException e) {
            return errorResponse("Не найден кредит с ID: " + loanId);
        } catch (Exception e) {
            return errorResponse("Расчет не удался: " + e.getMessage());
        }
    }

    private String handleAddClientToLoan(JsonObject data, String unused) {
        try {
            Long loanId = data.get("loanId").getAsLong();
            Long clientId = data.get("clientId").getAsLong();

            loanService.assignLoanToClient(loanId, clientId);

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
                bankJson.addProperty("bankId", bank.getBankId());
                bankJson.addProperty("bankName", bank.getBankName());
                bankJson.addProperty("address", bank.getAddress());
                bankJson.addProperty("phone", bank.getPhone() != null ? bank.getPhone() : "");
                bankJson.addProperty("email", bank.getEmail() != null ? bank.getEmail() : "");
                banksArray.add(bankJson);
            }

            JsonObject response = new JsonObject();
            response.addProperty("status", "success");
            response.add("banks", banksArray);

            return new Gson().toJson(response);
        } catch (Exception e) {
            LOG.error("Ошибка при получении списка банков", e);
            return errorResponse("Ошибка сервера при получении банков");
        }
    }

    private String handleGetCurrentUser(JsonObject data, String unused) {
        try {
            if (!data.has("userId")) {
                return errorResponse("User ID обязателен");
            }

            Long userId = data.get("userId").getAsLong();
            Optional<UserDTO> userOptional = userService.findById(userId);

            if (!userOptional.isPresent()) {
                return errorResponse("User не найден");
            }

            UserDTO userDTO = userOptional.get();

            JsonObject response = new JsonObject();
            response.addProperty("status", "success");
            response.add("user", gson.toJsonTree(userDTO));
            System.out.println("Response from server: " + response);
            return gson.toJson(response);

        } catch (Exception e) {
            return errorResponse("Ошибка получения текущего пользователя: " + e.getMessage());
        }
    }

    private String handleCreateAdmin(JsonObject data) {
        try {
            JsonObject userJson = data.getAsJsonObject("user");
            UserDTO adminDTO = gson.fromJson(userJson, UserDTO.class);
            adminDTO.setRoleId(1L);

            User newAdmin = userService.register(adminDTO);
            return successResponse(newAdmin);

        } catch (Exception e) {
            return errorResponse("Ошибка создания администратора: " + e.getMessage());
        }
    }

    private String handleGetClientLoans(JsonObject data, String unused) {
        try {
            List<LoanDTO> loans;

            // Определяем тип запроса
            if (data.has("bankName")) {
                String bankName = data.get("bankName").getAsString();
                loans = loanService.getLoanDTOsByBankName(bankName);
            } else if (data.has("userId")) {
                LOG.info("In userId");
                Long clientId = data.get("userId").getAsLong();
                loans = loanService.getLoanDTOsByClientId(clientId);
            } else {
                LOG.info("In all");
                loans = loanService.getAllLoansWithBankInfo();
            }

            // Преобразуем список DTO в JSON
            JsonArray loansArray = new JsonArray();
            for (LoanDTO loan : loans) {
                JsonObject loanJson = new JsonObject();
                // Основные поля кредита
                loanJson.addProperty("id", loan.getLoanId());
                loanJson.addProperty("amount", loan.getLoanAmount());
                loanJson.addProperty("termMonths", loan.getTermMonths());
                loanJson.addProperty("status", loan.getStatus());
                loanJson.addProperty("startDate", loan.getStartDate().toString());

                // Обработка endDate (может быть null)
                if (loan.getEndDate() != null) {
                    loanJson.addProperty("endDate", loan.getEndDate().toString());
                } else {
                    loanJson.add("endDate", JsonNull.INSTANCE);
                }

                // Информация о типе кредита
                JsonObject typeJson = new JsonObject();
                typeJson.addProperty("id", loan.getLoanTypeId());
                typeJson.addProperty("name", loan.getLoanTypeName());
                typeJson.addProperty("rate", loan.getInterestRate());

                // Дополнительная информация о банке
                typeJson.addProperty("bankId", loan.getBankId());

                loanJson.add("loanType", typeJson);

                loansArray.add(loanJson);
            }

            // Формируем ответ
            JsonObject response = new JsonObject();
            response.addProperty("status", "success");
            response.add("loans", loansArray);
            return gson.toJson(response);

        } catch (Exception e) {
            LOG.error("Ошибка при получении кредитов", e);
            return errorResponse("Ошибка сервера при получении кредитов");
        }
    }

    private String handleTakeLoan(JsonObject data, String unused) {
        try {
            // 1. Проверка входных данных
            if (!data.has("loanData") || data.get("loanData").isJsonNull()) {
                return errorResponse("Отсутствует объект loanData в запросе");
            }

            JsonObject loanData = data.getAsJsonObject("loanData");

            // 2. Валидация обязательных полей
            String[] requiredFields = {"userId", "loanTypeId", "amount", "termMonths"};
            for (String field : requiredFields) {
                if (!loanData.has(field)) {
                    return errorResponse("Отсутствует обязательное поле: " + field);
                }
                if (loanData.get(field).isJsonNull()) {
                    return errorResponse("Поле " + field + " не может быть null");
                }
            }

            // 3. Парсинг данных
            BigDecimal amount = new BigDecimal(loanData.get("amount").getAsString());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return errorResponse("Сумма кредита должна быть положительной");
            }

            int termMonths = loanData.get("termMonths").getAsInt();
            if (termMonths <= 0) {
                return errorResponse("Срок кредита должен быть положительным");
            }

            // 4. Создание DTO
            LoanDTO loanDTO = new LoanDTO();
            loanDTO.setClientId(loanData.get("userId").getAsLong());
            loanDTO.setLoanTypeId(loanData.get("loanTypeId").getAsLong());
            loanDTO.setLoanAmount(amount);
            loanDTO.setTermMonths(termMonths);

            // 5. Создание кредита и получение JSON
            JsonObject response = loanService.createLoanAndReturnJson(loanDTO);
            return new Gson().toJson(response);

        } catch (NumberFormatException e) {
            return errorResponse("Некорректный формат числового поля");
        } catch (EntityNotFoundException e) {
            return errorResponse(e.getMessage());
        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage());
        } catch (Exception e) {
            LOG.error("Ошибка при оформлении кредита", e);
            return errorResponse("Внутренняя ошибка сервера при оформлении кредита");
        }
    }

    private JsonObject convertLoanToJson(Loan loan) {
        JsonObject loanJson = new JsonObject();
        loanJson.addProperty("id", loan.getLoanId());
        loanJson.addProperty("amount", loan.getLoanAmount());
        loanJson.addProperty("termMonths", loan.getTermMonths());
        loanJson.addProperty("startDate", loan.getStartDate().toString());
        loanJson.addProperty("status", loan.getStatus());

        // Теперь loanType и bank уже загружены благодаря JOIN FETCH
        LoanType loanType = loan.getLoanType();
        JsonObject typeJson = new JsonObject();
        typeJson.addProperty("id", loanType.getLoanTypeId());
        typeJson.addProperty("name", loanType.getLoanTypeName());
        typeJson.addProperty("rate", loanType.getInterestRate());

        if (loanType.getBank() != null) {
            typeJson.addProperty("bankId", loanType.getBank().getBankId());
            typeJson.addProperty("bankName", loanType.getBank().getBankName());
        }

        loanJson.add("loanType", typeJson);

        return loanJson;
    }

    private String handleGetLoanTypesByBank(JsonObject data, String unused) {
        try {
            Long bankId = data.get("bankId").getAsLong();
            List<LoanTypeDTO> loanTypes = loanTypeService.getLoanTypesByBankId(bankId);

            JsonObject response = new JsonObject();
            response.addProperty("status", "success");
            response.add("loanTypes", gson.toJsonTree(loanTypes));
            return response.toString(); // Явно преобразуем в строку

        } catch (Exception e) {
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("status", "error");
            errorResponse.addProperty("message", "Ошибка при получении типов кредитов: " + e.getMessage());
            return errorResponse.toString();
        }
    }

    private String handleGetLoanTypes(JsonObject data, String unused) {
        try {
            // Получаем DTO объекты из сервиса
            List<LoanTypeDTO> loanTypes = loanTypeService.getAllLoanTypesWithBankInfo();

            // Создаем JSON ответ
            JsonObject response = new JsonObject();
            response.addProperty("status", "success");

            JsonArray loanTypesArray = new JsonArray();
            for (LoanTypeDTO dto : loanTypes) {
                JsonObject loanJson = new JsonObject();
                loanJson.addProperty("loanTypeId", dto.getLoanTypeId());
                loanJson.addProperty("loanTypeName", dto.getLoanTypeName());
                loanJson.addProperty("interestRate", dto.getInterestRate());
                loanJson.addProperty("bankId", dto.getBankId());
                loanJson.addProperty("bankName", dto.getBankName());
                loanTypesArray.add(loanJson);
            }

            response.add("loanTypes", loanTypesArray);
            return gson.toJson(response);

        } catch (Exception e) {
            return errorResponse("Ошибка при получении типов кредитов: " + e.getMessage());
        }
    }

    private String handleGetFilteredLoanTypes(JsonObject data, String unused) {
        try {
            Long bankId = data.has("bankId") && !data.get("bankId").isJsonNull()
                    ? data.get("bankId").getAsLong() : null;
            String namePart = data.has("namePart") ? data.get("namePart").getAsString() : null;
            BigDecimal minRate = data.has("minRate") && !data.get("minRate").isJsonNull()
                    ? data.get("minRate").getAsBigDecimal() : null;
            BigDecimal maxRate = data.has("maxRate") && !data.get("maxRate").isJsonNull()
                    ? data.get("maxRate").getAsBigDecimal() : null;

            List<LoanTypeDTO> loanTypes = loanTypeService.getLoanTypesWithFilters(
                    bankId, namePart, minRate, maxRate);

            JsonObject response = new JsonObject();
            response.addProperty("status", "success");
            response.add("loanTypes", convertLoanTypesToJson(loanTypes));
            return gson.toJson(response);
        } catch (Exception e) {
            return errorResponse("Ошибка при фильтрации кредитов: " + e.getMessage());
        }
    }

    private String handleCreateLoanType(JsonObject data, String unused) {
        try {
            // 1. Получаем вложенный объект data
            JsonObject loanData = data.getAsJsonObject("data");

            // 2. Проверяем наличие всех обязательных полей
            if (!loanData.has("bankId") || !loanData.has("loanTypeName") || !loanData.has("interestRate")) {
                return errorResponse("Необходимо указать bankId, loanTypeName и interestRate");
            }

            // 3. Извлекаем и валидируем данные
            long bankId = loanData.get("bankId").getAsLong();
            String loanTypeName = loanData.get("loanTypeName").getAsString();

            // 4. Проверяем название кредита
            if (loanTypeName.trim().isEmpty()) {
                return errorResponse("Название типа кредита не может быть пустым");
            }

            // 5. Проверяем процентную ставку
            BigDecimal interestRate;
            try {
                interestRate = new BigDecimal(loanData.get("interestRate").getAsString());
                if (interestRate.compareTo(BigDecimal.ZERO) <= 0) {
                    return errorResponse("Процентная ставка должна быть положительной");
                }
            } catch (NumberFormatException e) {
                return errorResponse("Некорректный формат процентной ставки");
            }

            // 6. Создаем DTO
            LoanTypeDTO dto = LoanTypeDTO.builder()
                    .bankId(bankId)
                    .loanTypeName(loanTypeName)
                    .interestRate(interestRate)
                    .build();

            // 7. Создаем тип кредита
            LoanType created = loanTypeService.createLoanType(dto);

            // 8. Формируем ответ
            JsonObject response = new JsonObject();
            response.addProperty("status", "success");
            response.add("loanType", convertLoanTypeToJson(created));
            return gson.toJson(response);

        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse("Ошибка при создании типа кредита: " + e.getMessage());
        }
    }

    private JsonArray convertLoanTypesToJson(List<LoanTypeDTO> loanTypes) {
        JsonArray array = new JsonArray();
        for (LoanTypeDTO dto : loanTypes) {
            array.add(convertLoanTypeToJson(dto));
        }
        return array;
    }

    private JsonObject convertLoanTypeToJson(LoanTypeDTO dto) {
        JsonObject obj = new JsonObject();
        obj.addProperty("loanTypeId", dto.getLoanTypeId());
        obj.addProperty("loanTypeName", dto.getLoanTypeName());
        obj.addProperty("interestRate", dto.getInterestRate());

        JsonObject bankObj = new JsonObject();
        bankObj.addProperty("bankId", dto.getBankId());
        bankObj.addProperty("bankName", dto.getBankName());
        obj.add("bank", bankObj);

        return obj;
    }

    private JsonObject convertLoanTypeToJson(LoanType loanType) {
        JsonObject obj = new JsonObject();
        obj.addProperty("loanTypeId", loanType.getLoanTypeId());
        obj.addProperty("loanTypeName", loanType.getLoanTypeName());
        obj.addProperty("interestRate", loanType.getInterestRate());

        JsonObject bankObj = new JsonObject();
        bankObj.addProperty("bankId", loanType.getBank().getBankId());
        bankObj.addProperty("bankName", loanType.getBank().getBankName());
        obj.add("bank", bankObj);

        return obj;
    }

    private String handleCreatePayment(JsonObject data, String unused) {
        if (data == null) {
            return errorResponse("Отсутствуют данные запроса");
        }

        JsonElement paymentDataElement = data.get("paymentData");
        if (paymentDataElement == null || paymentDataElement.isJsonNull()) {
            return errorResponse("Отсутствует объект paymentData");
        }

        if (!paymentDataElement.isJsonObject()) {
            return errorResponse("paymentData должен быть объектом");
        }

        JsonObject paymentData = paymentDataElement.getAsJsonObject();

        String[] requiredFields = {"loanId", "amount", "type"};
        for (String field : requiredFields) {
            if (!paymentData.has(field)) {
                return errorResponse("Отсутствует обязательное поле: " + field);
            }

            JsonElement fieldElement = paymentData.get(field);
            if (fieldElement == null || fieldElement.isJsonNull()) {
                return errorResponse("Поле " + field + " не может быть null");
            }
        }

        long loanId;
        try {
            loanId = paymentData.get("loanId").getAsLong();
        } catch (UnsupportedOperationException | NumberFormatException e) {
            return errorResponse("Некорректный формат loanId. Ожидается число");
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(paymentData.get("amount").getAsString());
        } catch (NumberFormatException | UnsupportedOperationException e) {
            return errorResponse("Некорректный формат amount. Ожидается число");
        }

        PaymentType paymentType;
        try {
            String typeStr = paymentData.get("type").getAsString();
            paymentType = PaymentType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return errorResponse("Недопустимый paymentType. Допустимые значения: " +
                    Arrays.toString(PaymentType.values()));
        } catch (UnsupportedOperationException e) {
            return errorResponse("Некорректный формат paymentType");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return errorResponse("Сумма платежа должна быть положительной");
        }

        try {
            Payment payment = paymentService.createPayment(loanId, amount, paymentType);

            JsonObject response = new JsonObject();
            response.addProperty("status", "success");
            response.addProperty("message", "Платеж успешно создан");
            response.addProperty("paymentId", payment.getPaymentId());
            return gson.toJson(response);

        } catch (EntityNotFoundException e) {
            return errorResponse("Кредит не найден");
        } catch (PaymentValidationException e) {
            return errorResponse(e.getMessage());
        } catch (Exception e) {
            LOG.error("Системная ошибка при создании платежа", e);
            return errorResponse("Внутренняя ошибка сервера");
        }
    }

    private String handleGetLoanDetails(JsonObject data, String unused) {
        try {
            // Валидация входных данных
            if (!data.has("loanId") || data.get("loanId").isJsonNull()) {
                return errorResponse("Не указан ID кредита");
            }

            Long loanId = data.get("loanId").getAsLong();

            // Получаем данные кредита из сервиса
            Loan loan = loanService.getLoanById(loanId);

            // Конвертируем Loan в LoanDetailsDTO
            LoanDetailsDTO loanDetails = convertToLoanDetailsDTO(loan);

            // Получаем актуальный график платежей
            List<PaymentScheduleDTO> schedule = paymentService.generateSchedule(loanId);
            loanDetails.setSchedule(schedule);

            // Получаем остаток долга
            BigDecimal remainingDebt = paymentService.calculateRemainingDebt(loan);
            loanDetails.setRemainingDebt(remainingDebt);

            // Получаем следующий платеж (если есть)
            PaymentScheduleDTO nextPayment = paymentService.getNextPaymentSchedule(loan);
            loanDetails.setNextPayment(nextPayment);

            // Формируем ответ
            JsonObject response = new JsonObject();
            response.addProperty("status", "success");
            response.add("loan", gson.toJsonTree(loanDetails));

            return gson.toJson(response);

        } catch (EntityNotFoundException e) {
            return errorResponse("Кредит не найден: " + e.getMessage());
        } catch (Exception e) {
            LOG.error("Ошибка при получении данных кредита", e);
            return errorResponse("Ошибка сервера при получении данных кредита");
        }
    }

    private LoanDetailsDTO convertToLoanDetailsDTO(Loan loan) {
        LoanDetailsDTO dto = new LoanDetailsDTO();

        // Копируем основные поля
        dto.setLoanId(loan.getLoanId());
        dto.setLoanAmount(loan.getLoanAmount());
        dto.setTermMonths(loan.getTermMonths());
        dto.setStartDate(loan.getStartDate());
        dto.setEndDate(loan.getEndDate());
        dto.setStatus(loan.getStatus());

        // Обрабатываем связанные сущности через DTO
        if (loan.getLoanType() != null) {
            LoanTypeDTO typeDto = new LoanTypeDTO();
            typeDto.setLoanTypeId(loan.getLoanType().getLoanTypeId());
            typeDto.setLoanTypeName(loan.getLoanType().getLoanTypeName());
            typeDto.setInterestRate(loan.getLoanType().getInterestRate());
            dto.setLoanType(typeDto);
        }

        if (loan.getClient() != null) {
            ClientDTO clientDto = new ClientDTO();
            clientDto.setUserId(loan.getClient().getUserId());
            clientDto.setFullName(loan.getClient().getFullName());
            // Не включаем roles, чтобы избежать циклической зависимости
            dto.setClient(clientDto);
        }

        if (loan.getLoanType().getBank() != null) {
            BankDTO bankDto = new BankDTO();
            bankDto.setBankId(loan.getLoanType().getBank().getBankId());
            bankDto.setBankName(loan.getLoanType().getBank().getBankName());
            dto.setBank(bankDto);
        }

        return dto;
    }

    private String handleGetAllLoans(JsonObject data, String unused) {
        try {
            LoanService loanService = new LoanService();
            List<LoanDTO> loans = loanService.getAllLoansWithBankInfo();

            JsonObject response = new JsonObject();
            response.addProperty("status", "success");
            response.add("loans", gson.toJsonTree(loans));

            LOG.info("Successfully retrieved all loans");
            return gson.toJson(response);

        } catch (Exception e) {
            LOG.error("Error retrieving loans", e);
            return errorResponse("Ошибка при получении списка кредитов: " + e.getMessage());
        }
    }

    private String handleDeleteBank(JsonObject data, String unused) {
        try {
            LOG.info("Incoming request: {}", data.toString());

            if (data == null || data.isJsonNull()) {
                return errorResponse("Empty request");
            }

            Set<String> keys = data.keySet();
            if (!keys.contains("command") || !keys.contains("bankId")) {
                return errorResponse("Required fields are missing");
            }

            // 4. Проверка типа bankId (альтернативный способ)
            try {
                long bankId = data.get("bankId").getAsLong();

                if (bankId <= 0) {
                    return errorResponse("Invalid bank ID");
                }

                LOG.info("Deleting bank ID: {}", bankId);
                new BankDeletionService().deleteBankCascading(bankId);

                return newSuccessResponse("Bank deleted");

            } catch (ClassCastException | IllegalStateException e) {
                return errorResponse("bankId must be a number");
            }

        } catch (Exception e) {
            LOG.error("Deletion error", e);
            return errorResponse("Processing error");
        }
    }

    private String newSuccessResponse(String message) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "success");
        response.addProperty("message", message);
        return gson.toJson(response);
    }

    private JsonObject successResponse() {
        JsonObject response = new JsonObject();
        response.addProperty("status", "success");
        return response;
    }

    private String handleUpdateLoan(JsonObject data, String unused) {
        try {
            // 1. Проверка наличия данных
            if (!data.has("data")) {
                return errorResponse("Данные Loan не найдены");
            }

            // 2. Извлекаем и объединяем данные из корня и объекта data
            JsonObject loanData = data.getAsJsonObject("data").deepCopy();

            // Добавляем поля из корня, если они отсутствуют в data
            if (data.has("status") && !loanData.has("status")) {
                loanData.addProperty("status", data.get("status").getAsString());
            }
            if (data.has("endDate") && !loanData.has("endDate")) {
                loanData.addProperty("endDate", data.get("endDate").getAsString());
            }
            if (data.has("userId") && !loanData.has("userId")) {
                loanData.addProperty("userId", data.get("userId").getAsInt());
            }

            // 3. Преобразование суммы
            if (loanData.has("amount")) {
                try {
                    String amountStr = loanData.get("amount").getAsString();
                    loanData.addProperty("amount", new BigDecimal(amountStr));
                } catch (NumberFormatException e) {
                    return errorResponse("Некорректный формат суммы");
                }
            }

            // 4. Парсинг DTO
            LoanDTO loanDTO = gson.fromJson(loanData, LoanDTO.class);

            // 5. Валидация обязательных полей
            if (loanDTO.getLoanId() == null) {
                return errorResponse("ID кредита обязательно");
            }
            if (loanDTO.getLoanTypeId() == null) {
                return errorResponse("Тип кредита обязателен");
            }
            if (loanDTO.getLoanAmount() == null || loanDTO.getLoanAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return errorResponse("Сумма кредита должна быть положительной");
            }
            if (loanDTO.getTermMonths() == null || loanDTO.getTermMonths() <= 0) {
                return errorResponse("Срок кредита должен быть положительным");
            }
            if (loanDTO.getStartDate() == null) {
                return errorResponse("Дата начала обязательна");
            }
            if (loanDTO.getStatus() == null || loanDTO.getStatus().isEmpty()) {
                return errorResponse("Статус кредита обязателен");
            }

            // 6. Обновление кредита через сервис
            LoanDTO updatedLoan = loanService.updateLoan(loanDTO.getLoanId(), loanDTO);

            // 7. Формирование ответа
            JsonObject response = new JsonObject();
            response.addProperty("status", "success");
            response.add("loan", gson.toJsonTree(updatedLoan));
            return gson.toJson(response);

        } catch (NoSuchElementException e) {
            LOG.error("Loan not found: " + e.getMessage());
            return errorResponse("Кредит не найден");
        } catch (IllegalArgumentException e) {
            LOG.error("Validation error: " + e.getMessage());
            return errorResponse("Некорректные данные: " + e.getMessage());
        } catch (Exception e) {
            LOG.error("Error updating loan: " + e.getMessage());
            return errorResponse("Ошибка при обновлении кредита: " + e.getMessage());
        }
    }

    private String handleUpdateLoanType(JsonObject data, String unused) {
        try {
            // 1. Проверка наличия данных
            if (!data.has("loanTypeData")) {
                return errorResponse("Данные для обновления не найдены");
            }

            // 2. Парсинг данных
            JsonObject loanTypeData = data.getAsJsonObject("loanTypeData");
            LoanTypeDTO loanTypeDTO = gson.fromJson(loanTypeData, LoanTypeDTO.class);

            // 3. Валидация
            if (loanTypeDTO.getLoanTypeId() == null) {
                return errorResponse("ID типа кредита обязательно");
            }
            if (loanTypeDTO.getLoanTypeName() == null || loanTypeDTO.getLoanTypeName().isEmpty()) {
                return errorResponse("Название типа кредита обязательно");
            }
            if (loanTypeDTO.getInterestRate() == null || loanTypeDTO.getInterestRate().compareTo(BigDecimal.ZERO) <= 0) {
                return errorResponse("Процентная ставка должна быть положительной");
            }
            if (loanTypeDTO.getBankName() == null) {
                return errorResponse("Банк обязателен");
            }

            // 4. Обновление через сервис
            LoanTypeDTO updatedLoanType = loanTypeService.updateLoanType(loanTypeDTO);

            // 5. Формирование ответа
            JsonObject response = new JsonObject();
            response.addProperty("status", "success");
            response.add("loanType", gson.toJsonTree(updatedLoanType));
            return gson.toJson(response);

        } catch (NoSuchElementException e) {
            LOG.error("Loan type not found: " + e.getMessage());
            return errorResponse("Тип кредита не найден");
        } catch (IllegalArgumentException e) {
            LOG.error("Validation error: " + e.getMessage());
            return errorResponse("Некорректные данные: " + e.getMessage());
        } catch (Exception e) {
            LOG.error("Error updating loan type: " + e.getMessage());
            return errorResponse("Ошибка при обновлении типа кредита: " + e.getMessage());
        }
    }

    private String handleGetLoanStatistics(JsonObject data, String unused) {
        try {
            // Получаем статистику из сервиса
            LoanStatisticsDTO stats = loanStatisticsService.getLoanStatistics();

            // Создаем JSON ответ
            JsonObject response = new JsonObject();
            response.addProperty("status", "success");

            // Основная статистика
            JsonObject statistics = new JsonObject();
            statistics.addProperty("avgRate", stats.getAverageRate());
            statistics.addProperty("popularBank", stats.getMostPopularBank());
            statistics.addProperty("totalLoans", stats.getTotalLoans());
            statistics.addProperty("totalAmount", stats.getTotalAmount());

            // Кредиты по банкам
            JsonArray loansByBankArray = new JsonArray();
            for (BankLoanCountDTO bankCount : stats.getLoansByBank()) {
                JsonObject bankJson = new JsonObject();
                bankJson.addProperty("bankName", bankCount.getBankName());
                bankJson.addProperty("count", bankCount.getLoanCount());
                loansByBankArray.add(bankJson);
            }
            statistics.add("loansByBank", loansByBankArray);

            // Распределение ставок
            JsonArray ratesDistributionArray = new JsonArray();
            for (RateRangeCountDTO rateRange : stats.getRatesDistribution()) {
                JsonObject rangeJson = new JsonObject();
                rangeJson.addProperty("range", rateRange.getRateRange());
                rangeJson.addProperty("count", rateRange.getCount());
                ratesDistributionArray.add(rangeJson);
            }
            statistics.add("ratesDistribution", ratesDistributionArray);

            // Динамика ставок по месяцам
            JsonArray ratesTrendArray = new JsonArray();
            for (MonthlyRateDTO monthlyRate : stats.getRatesTrend()) {
                JsonObject monthJson = new JsonObject();
                monthJson.addProperty("month", monthlyRate.getMonth());
                monthJson.addProperty("avgRate", monthlyRate.getAverageRate());
                ratesTrendArray.add(monthJson);
            }
            statistics.add("ratesTrend", ratesTrendArray);

            response.add("statistics", statistics);
            return gson.toJson(response);

        } catch (Exception e) {
            return errorResponse("Ошибка при получении статистики кредитов: " + e.getMessage());
        }
    }

    private String errorResponse(String message) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "error");
        response.addProperty("message", message);
        return gson.toJson(response);
    }
}



