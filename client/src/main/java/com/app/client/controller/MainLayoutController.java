package com.app.client.controller;

import com.app.client.model.SessionModel;
import com.app.client.network.NetworkClient;
import com.app.client.network.ResponseListener;
import com.app.client.util.SceneManager;
import com.app.shared.model.user.Bidder;
import com.app.shared.model.user.Seller;
import com.app.shared.model.user.User;
import com.app.shared.network.Request;
import com.app.shared.network.Response;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class MainLayoutController implements ResponseListener {

    @FXML private StackPane contentArea;
    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;
    @FXML private Label balanceLabel;
    @FXML private Button myListingsBtn;

    @FXML
    public void initialize() {
        NetworkClient.getInstance().addListener(this);

        SceneManager.getInstance().setContentArea(contentArea);

        User currentUser = SessionModel.getInstance().getCurrentUser();
        if (currentUser != null) {
            updateUserUI(currentUser);
        }

        Platform.runLater(this::navToDashboard);
    }

    private void updateUserUI(User user) {
        welcomeLabel.setText(user.getUsername());
        roleLabel.setText(user.getClass().getSimpleName().toUpperCase());

        if (user instanceof Bidder b) {
            balanceLabel.setText(String.format("Balance: $%,.2f", b.getBalance()));
        } else if (user instanceof Seller s) {
            balanceLabel.setText(String.format("Revenue: $%,.2f", s.getTotalRevenue()));
        } else {
            balanceLabel.setText("");
        }

        if (myListingsBtn != null) {
            boolean canSell = user.canSell();
            myListingsBtn.setVisible(canSell);
            myListingsBtn.setManaged(canSell);
        }
    }

    @FXML
    private void navToDashboard() {
        SceneManager.getInstance().switchScene("/view/fxml/DashboardView.fxml");
    }

    @FXML
    private void navToCreate() {
        SceneManager.getInstance().switchScene("/view/fxml/CreateAuctionView.fxml");
    }

    @FXML
    private void navToMyListings() {
        SceneManager.getInstance().switchScene("/view/fxml/SellerListingsView.fxml");
    }

    @FXML
    private void handleLogout() {
        NetworkClient.getInstance().removeListener(this);

        NetworkClient.getInstance().sendRequest(new Request(Request.RequestType.LOGOUT, null));
        SessionModel.getInstance().logout();
        SceneManager.getInstance().logoutToLoginScreen();
    }

    @Override
    public void onResponseReceived(Response response) {
        Platform.runLater(() -> {
            if (response.type() == Response.ResponseType.USER_UPDATED) {
                if (response.success() && response.payload() instanceof User updatedUser) {
                    updateUserUI(updatedUser);
                }
            }
        });
    }
}