module sample.dataencryption {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.graphics;
    requires javafx.base;
    requires javafx.media;
    requires jdk.crypto.ec;
    requires java.desktop;
    requires org.apache.commons.lang3;
    requires java.compiler;

    opens sample.dataencryption to javafx.fxml;
    exports sample.dataencryption;
}
