package com.example.chatapp;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegisterPage {


    public Button registerBtn;
    public TextField usernameTxt;
    public PasswordField passwordTxt;
    public Hyperlink loginLink;


    @FXML
    protected void onRegisterClick() {
        String username = usernameTxt.getText();
        String password = passwordTxt.getText();

        if (isValidInput(username, password)) {
            if (ChatAppServer.registerUser(username, password)) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Registration successful!");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Username already exists.");
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Invalid input. Username and password can only contain numbers, letters, and !@#$%_");
        }
        usernameTxt.clear();
        passwordTxt.clear();
    }

    @FXML
    protected void onLoginLinkClick()
    {
        navigateToLoginPage();
    }
    private boolean isValidInput(String username, String password) {
        String pattern = "^[a-zA-Z0-9!@#$_%]*$";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcherUsername = regex.matcher(username);
        Matcher matcherPassword = regex.matcher(password);
        return matcherUsername.matches() && matcherPassword.matches();
    }


    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.show();
    }

    private void navigateToLoginPage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("loginPage.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) registerBtn.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
