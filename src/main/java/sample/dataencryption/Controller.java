package sample.dataencryption;

import javafx.animation.AnimationTimer;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.*;
import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

public class Controller {
    @FXML
    private ListView<DriveInfo> flashDriveListView;
    @FXML
    private Label timeElapsedLabel;
    @FXML
    private Label pathLabel;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label progressLabel;
    @FXML
    private RadioButton bit128;
    @FXML
    private RadioButton bit192;
    @FXML
    private RadioButton bit256;
    @FXML
    private AnchorPane rootPane;
    @FXML
    private Menu help;
    private Timeline timeline;
    private Timeline timer;
    private static final String ALGORITHM = "AES";
    private SecretKey secretKey;
    private Thread encryptionThread;
    private Thread decryptionThread;
    private AnimationTimer animationTimer;
    private long totalBytesToEncrypt = 0;
    private long encryptedBytes = 0;


    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setOnCloseRequest(e -> {
                if (encryptionThread != null && encryptionThread.isAlive()) {
                    e.consume();

                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Зупинити шифрацію даних");
                    alert.setHeaderText(null);
                    alert.setContentText("Ви точно хочете прервати шифрацію даних? Вся зашифрована інформація залишиться!");
                    Optional<ButtonType> result = alert.showAndWait();

                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        encryptionThread.interrupt();
                        try {
                            encryptionThread.join();
                        } catch (InterruptedException ex) {
                            throw new RuntimeException(ex);
                        }
                        stage.close();
                    }
                }
            });
        });
    }

    /* --- Виведення всіх накопичувачів в системі --- */
    public class DriveInfo {
        private String name;
        private String description;
        private Image icon;
        private long totalSpace;
        private long freeSpace;

        public DriveInfo(String name, String description, long totalSpace, long freeSpace, Image icon) {
            this.name = name;
            this.description = description;
            this.totalSpace = totalSpace;
            this.freeSpace = freeSpace;
            this.icon = icon;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public long getTotalSpace() {
            return totalSpace;
        }

        public long getFreeSpace() {
            return freeSpace;
        }

        public Image getIcon() {
            return icon;
        }

        @Override
        public String toString() {
            return " " + description + " (" + name + ")" + " | Total space: " + totalSpace +
                    " | Free space: " + freeSpace;
        }
    }

    public class DriveInfoCell extends ListCell<DriveInfo> {
        private HBox content;
        private ImageView iconImageView;
        private Label nameLabel;
        private Label descriptionLabel;
        private Label spaceLabel;

        public DriveInfoCell() {
            iconImageView = new ImageView();
            iconImageView.setFitWidth(16);
            iconImageView.setFitHeight(16);

            nameLabel = new Label();
            nameLabel.setStyle("-fx-font-weight: bold");

            descriptionLabel = new Label();
            spaceLabel = new Label();

            content = new HBox(3);
            content.setAlignment(Pos.CENTER_LEFT);
            content.getChildren().addAll(iconImageView, descriptionLabel, nameLabel, spaceLabel);
        }

        private String formatSize(long size) {
            final int unit = 1024;
            if (size < unit) return size + " B";
            int exp = (int) (Math.log(size) / Math.log(unit));
            String pre = "KMGT".charAt(exp - 1) + "";
            return String.format("%.1f %sB", size / Math.pow(unit, exp), pre);
        }

        @Override
        protected void updateItem(DriveInfo driveInfo, boolean empty) {
            super.updateItem(driveInfo, empty);
            if (empty || driveInfo == null) {
                setGraphic(null);
            } else {
                iconImageView.setImage(driveInfo.getIcon());
                nameLabel.setText("(" + driveInfo.getName() + ")");
                descriptionLabel.setText(driveInfo.getDescription());
                String formattedTotalSpace = formatSize(driveInfo.getTotalSpace());
                String formattedFreeSpace = formatSize(driveInfo.getFreeSpace());
                spaceLabel.setText("| Total space: " + formattedTotalSpace + " | Free space: " + formattedFreeSpace);
                setGraphic(content);
            }
        }
    }

    @FXML
    private void scanSystemClicked() throws FileNotFoundException {
        flashDriveListView.getItems().clear();
        File[] drives = File.listRoots();
        FileSystemView fsv = FileSystemView.getFileSystemView();
        boolean flashDriveFound = false;

        for (File drive : drives) {
            String driveName = drive.getAbsolutePath();
            if (drive.isDirectory() && drive.canRead() && driveName.matches(".*[A-Za-z]:\\\\$")) {
                String driveDescription = fsv.getSystemTypeDescription(drive);
                long totalSpace = drive.getTotalSpace();
                long freeSpace = drive.getFreeSpace();
                String iconPath = "D:\\Учеба\\DataEncryption\\src\\image\\disc.png";
                Image icon = new Image(new FileInputStream(iconPath));
                DriveInfo driveInfo = new DriveInfo(driveName, driveDescription, totalSpace, freeSpace, icon);
                flashDriveListView.setCellFactory(listView -> new DriveInfoCell());
                flashDriveListView.getItems().add(driveInfo);
                flashDriveFound = true;
            }
        }

        if (!flashDriveFound) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No Flash Drives Found");
            alert.setHeaderText(null);
            alert.setContentText("No flash drives are currently connected to the computer.");
            alert.showAndWait();
        }
    }

    /* --- Показ About Us в MenuBar --- */
    @FXML
    private void showAboutDialog() {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.UTILITY);
        dialogStage.setResizable(false);

        Label headerLabel = new Label("About Us");
        headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px");

        Image image = new Image("D:\\Учеба\\DataEncryption\\src\\image\\dev.png");
        ImageView imageView = new ImageView(image);

        Label aboutLabel = new Label("All rights reserved. © This application was created by Infernum.");
        aboutLabel.setStyle("-fx-font-weight: bold");

        VBox vbox = new VBox(headerLabel, imageView, aboutLabel);
        vbox.setAlignment(Pos.CENTER);
        vbox.setSpacing(10);

        Scene scene = new Scene(vbox, 400, 200);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    @FXML
    private void showInstructionDialog() {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.UTILITY);
        dialogStage.setResizable(false);

        Label headerLabel = new Label("Instruction");
        headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px");

        Label instructionLabel = new Label(" 1. Спочатку необхідно просканувати систему, щоб у відповідне вікно було занесено" +
                " всі накопичувачі.\n 2. Необхідно вибрати відповідний тип ключа для шифрування: від 128 до 256 bit.\n " +
                "3. Необхідно натиснути на кнопку 'Розпочати шифрування' та чекати поки шифрація буде завершена.\n " +
                "4. Після завершення шифрації - на накопичувачі АВТОМАТИЧНО створюється дешифратор.");

        VBox vbox = new VBox(headerLabel, instructionLabel);
        vbox.setAlignment(Pos.CENTER);
        vbox.setSpacing(10);

        Scene scene = new Scene(vbox, 600, 175);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    /* --- Показ часу та прогресу шифрації в додатку --- */
    @FXML
    private void startTime() {
         animationTimer = new AnimationTimer() {
            private long startTime;

            @Override
            public void handle(long now) {
                long elapsedTime = (now - startTime) / 1_000_000_000; // переведення в секунди
                timeElapsedLabel.setText("Минуло часу: " + formatTime(elapsedTime));
                updateProgressBar(progressBar);
            }

            @Override
            public void start() {
                startTime = System.nanoTime();
                super.start();
            }
        };
        animationTimer.start();
    }

    private String formatTime(double time) {
        long minutes = (long) (time / 60);
        long seconds = (long) (time % 60);
        return String.format("%02d:%02d", minutes, seconds);
    }

    /* --- Генерування ключа та створення SecretKey --- */
    private SecretKey generateSecretKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);

            if (bit128.isSelected()) {
                keyGenerator.init(128);
            } else if (bit192.isSelected()) {
                keyGenerator.init(192);
            } else if (bit256.isSelected()) {
                keyGenerator.init(256);
            }

            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    private SecretKey getSecretKey() {
        if (secretKey == null) {
            secretKey = generateSecretKey();
        }
        return secretKey;
    }

    /* --- Процес шифрування даних --- */
    private void encryptFlashDrive(File flashDrive) {
        if (flashDrive.isDirectory()) {
            File[] files = flashDrive.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        encryptFlashDrive(file);
                    } else {
                        if (!encryptionThread.isInterrupted()) {
                            totalBytesToEncrypt += file.length();
                            encryptFile(file);
                        } else {
                            break;
                        }
                    }
                }
            }
        }
    }

    private void encryptFile(File file) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());

            File encryptedFile = File.createTempFile("temp", null);
            try (InputStream inputStream = new FileInputStream(file);
                 OutputStream outputStream = new FileOutputStream(encryptedFile);
                 CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher)) {

                byte[] buffer = new byte[32768];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    cipherOutputStream.write(buffer, 0, bytesRead);
                    encryptedBytes += bytesRead;
                    updateProgressBar(progressBar);
                }
            }

            Files.move(encryptedFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File encrypted successfully: " + file.getAbsolutePath());
            String abbreviatedPath = StringUtils.abbreviate(file.getAbsolutePath(), 50);
            Platform.runLater(() -> pathLabel.setText(abbreviatedPath));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void updateProgressBar(ProgressBar progressBar) {
        double progress = (double) encryptedBytes / totalBytesToEncrypt;
        progressBar.setProgress(progress);
        int percentage = (int) (progress * 100);
        Platform.runLater(() -> progressLabel.setText(percentage + "%"));
    }

    @FXML
    private void startCryptClicked() {
        DriveInfo selectedDrive = flashDriveListView.getSelectionModel().getSelectedItem();
        progressBar.setProgress(0);

        if (selectedDrive != null) {
            File flashDrive = new File(selectedDrive.getName());
            startTime();
            Task<Void> encryptionTask = new Task<>() {
                @Override
                protected Void call() {
                    totalBytesToEncrypt = 0;
                    encryptedBytes = 0;
                    encryptFlashDrive(flashDrive);
                    return null;
                }
            };

            encryptionTask.setOnSucceeded(event -> {
                animationTimer.stop();
                progressLabel.setText("100%");
                progressBar.setProgress(1.0);

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Шифрування завершено");
                alert.setHeaderText(null);
                alert.setContentText("Шифрування було завершено!");

                ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
                alert.getButtonTypes().setAll(okButton);
                alert.showAndWait();
            });

            encryptionThread = new Thread(encryptionTask);
            encryptionThread.start();
        }
    }

    /* --- Процес дешифрування даних --- */
    /* private void decryptFile(File file) {
        try {
            Cipher decryptCipher = Cipher.getInstance(ALGORITHM);
            decryptCipher.init(Cipher.DECRYPT_MODE, getSecretKey());

            File decryptedFile = File.createTempFile("temp", null);
            try (InputStream encryptedInputStream = new FileInputStream(file);
                 OutputStream decryptedOutputStream = new FileOutputStream(decryptedFile);
                 CipherInputStream cipherInputStream = new CipherInputStream(encryptedInputStream, decryptCipher)) {
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = cipherInputStream.read(buffer)) != -1) {
                    decryptedOutputStream.write(buffer, 0, bytesRead);
                }
            }

            Files.move(decryptedFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void decryptFlashDrive(File flashDrive) {
        if (flashDrive.isDirectory()) {
            File[] files = flashDrive.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        decryptFlashDrive(file);
                    } else {
                        decryptFile(file);
                    }
                }
            }
        }
    }

    @FXML
    private void startDecryptClicked() {
        DriveInfo selectedDrive = flashDriveListView.getSelectionModel().getSelectedItem();

        if (selectedDrive != null) {
            File flashDrive = new File(selectedDrive.getName());
            startTime();
            Task<Void> decryptTask = new Task<>() {
                @Override
                protected Void call() {
                    decryptFlashDrive(flashDrive);
                    return null;
                }
            };
            decryptionThread = new Thread(decryptTask);
            decryptionThread.start();
        }
    } */
}