module com.example.onlinemcqexam {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.example.onlinemcqexam to javafx.fxml;
    exports com.example.onlinemcqexam;
}
