module com.starlore.starlore {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires javafx.media;


    opens com.starlore.starlore to javafx.fxml;
    exports com.starlore.starlore;
}
