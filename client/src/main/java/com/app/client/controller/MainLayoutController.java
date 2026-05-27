package com.app.client.controller;

import com.app.client.model.SessionModel;
import com.app.client.network.NetworkClient;
import com.app.client.network.ResponseListener;
import com.app.client.util.SceneManager;
import com.app.client.util.ToastUtil;
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


    @FXML public Button dashboardBtn;
    @FXML private StackPane contentArea;
    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;
    @FXML private Label balanceLabel;
    @FXML private Button myListingsBtn;
    @FXML private Button winningBidsBtn;
    @FXML public Button createAuctionBtn;

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

        if (winningBidsBtn != null) {
            boolean canBid = user.canBid();
            winningBidsBtn.setVisible(canBid);
            winningBidsBtn.setManaged(canBid);
        }
    }

    @FXML
    private void navToDashboard() {
        setActiveNav(dashboardBtn);
        SceneManager.getInstance().switchScene("/view/fxml/DashboardView.fxml");
    }

    @FXML
    private void navToCreate() {
        setActiveNav(createAuctionBtn);
        SceneManager.getInstance().switchScene("/view/fxml/CreateAuctionView.fxml");
    }

    @FXML
    private void navToMyListings() {
        setActiveNav(myListingsBtn);
        SceneManager.getInstance().switchScene("/view/fxml/SellerListingsView.fxml");
    }

    @FXML
    private void navToWinningBids() {
        setActiveNav(winningBidsBtn);
        SceneManager.getInstance().switchScene("/view/fxml/WinningAuctionsView.fxml");
    }

    @FXML
    private void handleLogout() {
        NetworkClient.getInstance().removeListener(this);

        NetworkClient.getInstance().sendRequest(new Request(Request.RequestType.LOGOUT, null));
        SessionModel.getInstance().logout();
        SceneManager.getInstance().logoutToLoginScreen();
    }

    private void setActiveNav(Button clickedButton) {
        dashboardBtn.getStyleClass().remove("active-btn");

        if (myListingsBtn != null) myListingsBtn.getStyleClass().remove("active-btn");
        if (winningBidsBtn != null) winningBidsBtn.getStyleClass().remove("active-btn");

        if (clickedButton != null && !clickedButton.getStyleClass().contains("active-btn")) {
            clickedButton.getStyleClass().add("active-btn");
        }
    }

    @Override
    public void onResponseReceived(Response response) {
        Platform.runLater(() -> {
            if (response.type() == Response.ResponseType.USER_UPDATED) {
                if (response.success() && response.payload() instanceof User updatedUser) {
                    updateUserUI(updatedUser);
                    ToastUtil.showToast(response.message(), ToastUtil.ToastType.INFO);
                }
            }
        });
    }
}