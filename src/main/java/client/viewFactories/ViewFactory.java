package client.viewFactories;

import client.MainApp;
import javafx.stage.Stage;

public class ViewFactory {
    private final MainApp mainApp;
    private final Stage primaryStage;

    public ViewFactory(MainApp mainApp, Stage primaryStage) {
        this.mainApp = mainApp;
        this.primaryStage = primaryStage;
    }

    public void showView(ViewType viewType) {
        ViewCreator creator = createViewCreator(viewType);
        creator.showView(mainApp, primaryStage);
    }

    private ViewCreator createViewCreator(ViewType viewType) {
        switch (viewType) {
            case REGISTER: return new RegisterViewCreator();
            case CLIENT_ACCOUNT: return new ClientAccountViewCreator();
            case ADMIN_ACCOUNT: return new AdminAccountViewCreator();
            case DELETE_BY_ID: return new DeleteByIdCreator();
            case MAIN: return new MainViewCreator();
            case EDIT_ACCOUNT: return new EditAccountViewCreator();
            case LOAN_TYPES: return new LoanTypesViewCreator();
            case BANKS: return new BankCreator();
            default: throw new IllegalArgumentException("Unknown view type: " + viewType);
        }
    }

    public enum ViewType {
        REGISTER, CLIENT_ACCOUNT, ADMIN_ACCOUNT, BANKS, DELETE_BY_ID, MAIN, EDIT_ACCOUNT, LOANS, LOAN_TYPES
    }
}
