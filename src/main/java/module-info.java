module com.example.votingsystem {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.votingsystem to javafx.fxml;
    exports com.example.votingsystem;
}