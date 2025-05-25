package server;

import com.google.gson.*;
import config.LocalDateAdapter;
import exeption.AuthExeption;
import jakarta.persistence.EntityNotFoundException;
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
import java.util.stream.Collectors;

public class ClientHandler implements Runnable {
    Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
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
    private Long currentUserId;
    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    public ClientHandler(Socket clientSocket, UserService userService, PaymentService paymentService, LoanService loanService, LoanTypeService loanTypeService, BankService bankService) throws IOException {
        this.clientSocket = clientSocket;
        this.userService = userService;
        this.paymentService = paymentService;
        this.loanService = loanService;
        this.loanTypeService = loanTypeService;
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

    private String handleDeleteUser(JsonObject data, String unused) {
        try {
            if (!data.has("data") || !data.getAsJsonObject("data").has("userId")) {
                return errorResponse("userId пустой");
            }

            Long userId = data.getAsJsonObject("data").get("userId").getAsLong();

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
            return errorResponse("Генерация расписания не удалась: " + e.getMessage());
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

            if (data.has("bankName")) {
                String bankName = data.get("bankName").getAsString();
                loans = loanService.getLoansByLoanTypeBankName(bankName).stream()
                        .map(loanService::convertToDTO)
                        .collect(Collectors.toList());
            } else if (data.has("clientId")) {
                Long clientId = data.get("clientId").getAsLong();
                loans = loanService.getLoansByClientId(clientId).stream()
                        .map(loanService::convertToDTO)
                        .collect(Collectors.toList());
            } else {
                loans = loanService.getAllLoansWithBankInfo();
            }

            JsonArray loansArray = new JsonArray();
            loans.forEach(loan -> loansArray.add(gson.toJsonTree(loan)));

            JsonObject response = new JsonObject();
            response.addProperty("status", "success");
            response.add("loans", loansArray);
            return gson.toJson(response);

        } catch (Exception e) {
            return errorResponse("Ошибка при получении кредитов: " + e.getMessage());
        }
    }

    private String handleTakeLoan(JsonObject data, String unused) {
        try {
            // 1. Проверка обязательных полей
            if (!data.has("userId") || !data.has("loanTypeId") ||
                    !data.has("amount") || !data.has("termMonths")) {
                return errorResponse("Не все обязательные поля заполнены");
            }

            // 2. Создаем LoanDTO
            LoanDTO loanDTO = new LoanDTO();
            loanDTO.setClientId(data.get("userId").getAsLong());
            loanDTO.setLoanTypeId(data.get("loanTypeId").getAsLong());
            loanDTO.setLoanAmount(data.get("amount").getAsBigDecimal());
            loanDTO.setTermMonths(data.get("termMonths").getAsInt());

            // 3. Создаем кредит
            Loan createdLoan = loanService.createLoan(loanDTO);

            // 4. Формируем ответ
            JsonObject response = new JsonObject();
            response.addProperty("status", "success");
            response.add("loan", convertLoanToJson(createdLoan));
            return gson.toJson(response);

        } catch (EntityNotFoundException e) {
            return errorResponse(e.getMessage());
        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage());
        } catch (Exception e) {
            LOG.error("Ошибка при оформлении кредита", e);
            return errorResponse("Ошибка сервера при оформлении кредита");
        }
    }

    private JsonObject convertLoanToJson(Loan loan) {
        JsonObject json = new JsonObject();
        json.addProperty("id", loan.getLoanId());
        json.addProperty("amount", loan.getLoanAmount());
        json.addProperty("termMonths", loan.getTermMonths());
        json.addProperty("startDate", loan.getStartDate().toString());
        json.addProperty("endDate", loan.getEndDate().toString());
        json.addProperty("status", loan.getStatus());

        // Добавляем информацию о типе кредита
        JsonObject typeJson = new JsonObject();
        typeJson.addProperty("id", loan.getLoanType().getLoanTypeId());
        typeJson.addProperty("name", loan.getLoanType().getLoanTypeName());
        typeJson.addProperty("rate", loan.getLoanType().getInterestRate());
        json.add("loanType", typeJson);

        return json;
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

    private String errorResponse(String message) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "error");
        response.addProperty("message", message);
        return gson.toJson(response);
    }
}



