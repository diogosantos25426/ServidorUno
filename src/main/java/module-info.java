module com.mycompany.servidoruno {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.mycompany.servidoruno to javafx.fxml;
    exports com.mycompany.servidoruno;
}
