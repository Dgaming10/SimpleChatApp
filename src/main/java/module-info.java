module com.example.chatapp {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires java.sql;
    requires mysql.connector.j;
    requires com.google.common;

    opens com.example.chatapp to javafx.fxml;
    exports com.example.chatapp;
}