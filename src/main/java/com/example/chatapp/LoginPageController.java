package com.example.chatapp;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginPageController {
    @FXML
    private Button loginButton;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField usernameField;

    @FXML
    protected void onLoginPress() {
        boolean loginSuccess = performLogin(usernameField.getText(), passwordField.getText());

        if (loginSuccess) {
            navigateToChatPage(usernameField.getText());
        } else {
            displayLoginError();
        }
        usernameField.clear();
        passwordField.clear();
    }

    @FXML
    protected void onLinkPress() {
        navigateToRegistrationPage();
    }

    private boolean performLogin(String username, String password) {
        return ChatAppServer.tryLogin(username, password);
    }

    private void navigateToChatPage(String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("chatPage.fxml"));
            Parent root = loader.load();
            ChatPageController chatPageController = loader.getController();
            chatPageController.loadData(username);

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateToRegistrationPage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("registerPage.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void displayLoginError() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Login Failed");
        alert.setContentText("Please check your username and password.");
        alert.showAndWait();
    }
}
