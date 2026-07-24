module com.starlore.starlore {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens com.starlore.starlore to javafx.fxml;
    exports com.starlore.starlore;
}