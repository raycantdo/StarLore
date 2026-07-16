module com.starlore.starlore {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.starlore.starlore to javafx.fxml;
    exports com.starlore.starlore;
}