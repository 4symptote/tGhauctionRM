package com.app.client.controller;

import com.app.client.model.SessionModel;
import com.app.client.network.NetworkClient;
import com.app.client.util.ToastUtil;
import com.app.shared.model.user.Bidder;
import com.app.shared.model.user.Seller;
import com.app.shared.model.user.User;
import com.app.shared.network.Request;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class WalletController {

    @FXML private Label currentBalanceLabel;
    @FXML private Label roleWarningLabel;
    @FXML private TextField amountField;
    @FXML private Button depositBtn;
    @FXML private Button withdrawBtn;

    @FXML
    public void initialize() {
        User user = SessionModel.getInstance().getCurrentUser();

        if (user instanceof Bidder b) {
            currentBalanceLabel.setText(String.format("Available Balance: $%,.2f", b.getBalance()));
            roleWarningLabel.setText("Fund your account to place bids.");
            withdrawBtn.setDisable(true);

        } else if (user instanceof Seller s) {
            currentBalanceLabel.setText(String.format("Total Revenue: $%,.2f", s.getTotalRevenue()));
            roleWarningLabel.setText("Withdraw your auction earnings.");
            depositBtn.setDisable(true);
        }
    }

    @FXML
    private void handleDeposit() {
        processTransaction(Request.RequestType.DEPOSIT);
    }

    @FXML
    private void handleWithdraw() {
        processTransaction(Request.RequestType.WITHDRAW);
    }

    private void processTransaction(Request.RequestType type) {
        try {
            double amount = Double.parseDouble(amountField.getText().trim());

            if (amount <= 0) {
                ToastUtil.showToast("Amount must be greater than 0.", ToastUtil.ToastType.ERROR);
                return;
            }

            NetworkClient.getInstance().sendRequest(new Request(type, amount));

            ((Stage) amountField.getScene().getWindow()).close();

        } catch (NumberFormatException e) {
            ToastUtil.showToast("Please enter a valid numeric amount.", ToastUtil.ToastType.ERROR);
        }
    }
}