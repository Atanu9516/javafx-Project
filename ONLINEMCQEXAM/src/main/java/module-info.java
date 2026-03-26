module com.example.onlinemcqexam {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;

    opens com.example.onlinemcqexam to javafx.fxml;
    exports com.example.onlinemcqexam;
}
