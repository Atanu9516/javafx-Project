module com.example.onlinemcqexam {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    
    exports com.example.onlinemcqexam;
    opens com.example.onlinemcqexam to javafx.fxml;
}
