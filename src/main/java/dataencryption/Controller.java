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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
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
    private long encryptedBytes;
    private long totalBytesToEncrypt;


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
                String iconPath = "src\\image\\disc.png";
                Image icon = new Image(new FileInputStream(iconPath));
                DriveInfo driveInfo = new DriveInfo(driveName, driveDescription, totalSpace, freeSpace, icon);
                flashDriveListView.setCellFactory(listView -> new DriveInfoCell());
                flashDriveListView.getItems().add(driveInfo);
                flashDriveFound = true;
            }
        }

        if (!flashDriveFound) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Не знайдено накопичувачів");
            alert.setHeaderText(null);
            alert.setContentText("Не вдалось знайти накопичувачі, які під'єднані до комп'ютера");
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

        Image image = new Image("src\\image\\dev.png");
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

        Label instructionLabel = new Label(" 1. Спочатку необхідно просканувати систему, щоб вивести список всіх " +
                "доступних накопичувачів.\n 2. Необхідно вибрати відповідний тип ключа для шифрування: від 128 до 256 bit.\n " +
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
                long elapsedTime = (now - startTime) / 1_000_000_000;
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

                byte[] buffer = new byte[65536];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    cipherOutputStream.write(buffer, 0, bytesRead);
                    encryptedBytes += bytesRead;
                    updateProgressBar(progressBar);
                }
            }

            Files.move(encryptedFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            String abbreviatedPath = StringUtils.abbreviate(file.getAbsolutePath(), 50);
            Platform.runLater(() -> pathLabel.setText(abbreviatedPath));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* --- Оновлення ProgressBar на основі кількості оброблених даних файлу */
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
            totalBytesToEncrypt = 0;
            encryptedBytes = 0;
            Task<Void> encryptionTask = new Task<>() {
                @Override
                protected Void call() {
                    encryptFlashDrive(flashDrive);
                    return null;
                }
            };

            encryptionTask.setOnSucceeded(event -> {
                animationTimer.stop();
                progressLabel.setText("100%");
                progressBar.setProgress(1.0);

                createDecryptor(selectedDrive.getName());

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

    /* --- Процес створення jar-файлу дешифратора даних на накопичувачі */
    private String getDecodeSecretKeyToJar() {
        SecretKey secretKey = getSecretKey();
        byte[] encodedKey = secretKey.getEncoded();
        return Base64.getEncoder().encodeToString(encodedKey);
    }

    private void createDecryptor(String selectedDrive) {
        try {
            String flashDriveName = selectedDrive + File.separator;
            String decodeKey = getDecodeSecretKeyToJar();

            String decryptorCode = "import javax.crypto.*;\n" +
                    "import java.io.*;\n" +
                    "import java.nio.file.Files;\n" +
                    "import java.nio.file.StandardCopyOption;\n" +
                    "import java.util.Base64;\n" +
                    "import javax.crypto.spec.SecretKeySpec;\n" +
                    "import javax.swing.*;\n" +
                    "import java.awt.*;\n" +
                    "import java.awt.event.WindowEvent;\n" +
                    "import java.awt.event.WindowAdapter;\n" +
                    "public class Decryptor {\n" +
                    "    private static final String ALGORITHM = \"AES\";\n" +
                    "    private static SecretKey secretKey;\n" +
                    "    private static JProgressBar progressBar;\n" +
                    "    private static JLabel fileLabel;\n" +
                    "    private static JFrame frame;\n" +
                    "    private static void createInformationFrame() {\n" +
                    "        frame = new JFrame(\"Процес дешифрації даних\");\n" +
                    "        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);\n" +
                    "        frame.setSize(600, 100);\n" +
                    "        frame.setLocationRelativeTo(null);\n\n" +
                    "        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));\n" +
                    "        fileLabel = new JLabel(\"Дешифрування файлу: \");\n" +
                    "        centerPanel.add(fileLabel);\n" +
                    "        progressBar = new JProgressBar(0, 100);\n" +
                    "        progressBar.setStringPainted(true);\n" +
                    "        frame.add(progressBar, BorderLayout.NORTH);\n" +
                    "        frame.add(centerPanel, BorderLayout.CENTER);\n" +
                    "        frame.setVisible(true);\n" +
                    "    }\n" +
                    "    private static void updateProgress(int progress, String fileName) {\n" +
                    "        SwingUtilities.invokeLater(() -> {\n" +
                    "            progressBar.setValue(progress);\n" +
                    "            fileLabel.setText(\"Дешифрування файлу: \" + fileName);\n" +
                    "            progressBar.setValue(progress);\n" +
                    "        });\n" +
                    "    }\n" +
                    "    private static void decryptFile(File file) {\n" +
                    "        try {\n" +
                    "            Cipher cipher = Cipher.getInstance(ALGORITHM);\n" +
                    "            cipher.init(Cipher.DECRYPT_MODE, secretKey);\n" +
                    "            File decryptedFile = File.createTempFile(\"temp\", null);\n" +
                    "            try (InputStream inputStream = new FileInputStream(file);\n" +
                    "                 OutputStream outputStream = new FileOutputStream(decryptedFile);\n" +
                    "                 CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher)) {\n" +
                    "                byte[] buffer = new byte[65536];\n" +
                    "                int bytesRead;\n" +
                    "                long fileSize = file.length();\n" +
                    "                long totalBytesRead = 0;\n" +
                    "                while ((bytesRead = inputStream.read(buffer)) != -1) {\n" +
                    "                    cipherOutputStream.write(buffer, 0, bytesRead);\n" +
                    "                    totalBytesRead += bytesRead;\n" +
                    "                    int progress = (int) ((totalBytesRead * 100) / fileSize);\n" +
                    "                    updateProgress(progress, file.getName());\n" +
                    "                }\n" +
                    "            }\n" +
                    "            Files.move(decryptedFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);\n" +
                    "        } catch (Exception e) {\n" +
                    "            e.printStackTrace();\n" +
                    "        }\n" +
                    "    }\n" +
                    "    public static void main(String[] args) {\n" +
                    "        String username = JOptionPane.showInputDialog(null, \"Введіть логін:\", \"Введення логіна\", JOptionPane.PLAIN_MESSAGE);\n" +
                    "        String password = JOptionPane.showInputDialog(null, \"Введіть пароль:\", \"Введення пароля\", JOptionPane.PLAIN_MESSAGE);\n" +
                    "        if (isValidLogin(username, password)) {\n" +
                    "           String flashDriveName = \"" + flashDriveName + "\";\n" +
                    "           secretKey = new SecretKeySpec(Base64.getDecoder().decode(\"" + decodeKey + "\"), ALGORITHM);\n" +
                    "           File flashDrive = new File(flashDriveName);\n" +
                    "           createInformationFrame();\n" +
                    "           decryptFlashDrive(flashDrive);\n" +
                    "           JOptionPane.showMessageDialog(null, \"Дешифрація успішно завершена\", \"Успішно\", JOptionPane.INFORMATION_MESSAGE);\n" +
                    "           frame.dispose();\n" +
                    "       } else {\n" +
                    "          JOptionPane.showMessageDialog(null, \"WPHCK-001: Неправильний логін або пароль. Автоматичне завершення програми.\", \"Помилка\", JOptionPane.ERROR_MESSAGE);\n" +
                    "       }\n" +
                    "    }\n" +
                    "    private static boolean isValidLogin(String username, String password) {" +
                    "    return username.equals(\"admin\") && password.equals(\"admin\");\n" +
                    "    }\n" +
                    "    private static void decryptFlashDrive(File flashDrive) {\n" +
                    "        if (flashDrive.isDirectory()) {\n" +
                    "            File[] files = flashDrive.listFiles();\n" +
                    "            if (files != null) {\n" +
                    "                for (File file : files) {\n" +
                    "                    if (file.isDirectory()) {\n" +
                    "                        decryptFlashDrive(file);\n" +
                    "                    } else {\n" +
                    "                        decryptFile(file);\n" +
                    "                    }\n" +
                    "                }\n" +
                    "            }\n" +
                    "        }\n" +
                    "    }\n" +
                    "}";

            File decryptorFile = new File("Decryptor.java");
            try (FileWriter fileWriter = new FileWriter(decryptorFile)) {
                fileWriter.write(decryptorCode);
            }

            Process compileProcess = Runtime.getRuntime().exec("javac Decryptor.java");
            compileProcess.waitFor();

            String manifestContent = "Main-Class: Decryptor\n";
            File manifestFile = new File("Manifest.txt");
            try (FileWriter fileWriter = new FileWriter(manifestFile)) {
                fileWriter.write(manifestContent);
            }

            Process jarProcess = Runtime.getRuntime().exec("jar cfm " + flashDriveName + "Decryptor.jar Manifest.txt Decryptor.class");
            jarProcess.waitFor();

            File decryptorClass = new File("Decryptor.class");
            decryptorFile.delete();
            manifestFile.delete();
            decryptorClass.delete();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
