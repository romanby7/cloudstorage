package com.sunny.cloudstorage.client;

import com.sunny.cloudstorage.common.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.ResourceBundle;

public class ClientController implements Initializable {

    final private FileChooser fileChooser = new FileChooser();
    final private Utils utils = new Utils();

    @FXML
    ListView<String> filesListServer;

    @FXML
    public ListView<String> filesListClient;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Network.start();
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    AbstractMessage am = Network.readObject();
                    if (am instanceof FileMessage) {
                        FileMessage fm = (FileMessage) am;
                        Files.write(Paths.get("client_storage/" + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                        refreshLocalFilesList();
                    }
                    if (am instanceof FilesListMessage) {
                        FilesListMessage flm = (FilesListMessage) am;
                        refreshServerFilesList(flm.getFilesList());
                    }
                    if (am instanceof FileRequest) {
                        FileRequest fr = (FileRequest) am;
                        switch (fr.getFileCommand()) {
                            case DELETE:
                                deleteFile(fr.getFilename());
                                break;
                            case LIST_FILES:
                                refreshLocalFilesList();
                                break;
                            case SEND_FILE_CHUNK_TO_CLIENT:
                                saveFileChunkOnClient(fr);
                                break;
                            case FILE_CHUNK_COMPLETED:
                                refreshLocalFilesList();
                                break;
                            case SEND_FILE_CHUNK_TO_SERVER:
                                sendFileChunksToServer(fr);
                                break;
                                default:
                                    break;
                        }
                    }
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            } finally {
                Network.stop();
            }
        });
        t.setDaemon(true);
        t.start();
        refreshLocalFilesList();
        Network.sendMsg(new FileRequest(FileCommand.LIST_FILES));
    }

    private void saveFileChunkOnClient(FileRequest fr) throws IOException {

        FileMessage fm = fr.getFileMessage();
        String fileName = fm.getFilename();
        long offset = fm.getOffset();
        byte[] data = fm.getData();

        Path path = Paths.get("client_storage/" + fileName);

        utils.writeBytes(offset, data, path);

        FileMessage fileMessage = new FileMessage(fileName,offset + Constants.FRAME_CHUNK_SIZE);
        FileRequest fileRequest = new FileRequest(FileCommand.SEND_FILE_CHUNK_TO_CLIENT, fileMessage);

        Network.sendMsg(fileRequest);

    }

    public void pressOnDownloadBtn(ActionEvent actionEvent) {
        String fileName = filesListServer.getSelectionModel().getSelectedItem();
        if (fileName != null) {
            FileMessage fm = new FileMessage(fileName, 0L);
            FileRequest fr = new FileRequest(FileCommand.SEND_FILE_CHUNK_TO_CLIENT, fm);

            Network.sendMsg(fr);
        }
    }

    public void pressOnSendFileToServer(ActionEvent actionEvent) {

        String fileName = filesListClient.getSelectionModel().getSelectedItem();
        if (fileName != null ) {
            try {
                Network.sendMsg(new FileRequest(FileCommand.DELETE, fileName));
                sendFileChunksToServer(new FileRequest(FileCommand.SEND_FILE_CHUNK_TO_SERVER, new FileMessage(fileName, 0L)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendFileChunksToServer(FileRequest fr) throws IOException {

        String fileName = fr.getFileMessage().getFilename();
        Path path = Paths.get("client_storage/" + fileName);
        if (Files.exists(path)) {


            RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r");
            long offset = fr.getFileMessage().getOffset();
            long length = raf.length();

            if ( utils.isFileChunksCompleted(raf, offset, length)) {
                Network.sendMsg(new FileRequest(FileCommand.FILE_CHUNK_COMPLETED));
                return;
            }


            byte[] byteBuf = utils.readBytes(raf, offset, length);

            FileMessage fileMessage = new FileMessage(fileName, byteBuf, offset);
            FileRequest fileRequest = new FileRequest(FileCommand.SEND_FILE_CHUNK_TO_SERVER, fileMessage);

            Network.sendMsg(fileRequest);

        }
    }


    private void refreshLocalFilesList() {
        if (Platform.isFxApplicationThread()) {
            updateFilesList();
        } else {
            Platform.runLater(() -> {
                updateFilesList();
            });
        }
    }

    private void updateFilesList() {
        try {
            filesListClient.getItems().clear();
            Files.list(Paths.get("client_storage")).map((Path p) -> {
                return p.getFileName().toString();
            }).forEach((String o) -> {
                filesListClient.getItems().add(o);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateServerFilesList(List<String> list) {
        filesListServer.getItems().clear();
        for (String o: list
             ) {
            filesListServer.getItems().add(o);
        }

    }

    private void refreshServerFilesList(List<String> list) {
        if (Platform.isFxApplicationThread()) {
            updateServerFilesList(list);
        } else {
            Platform.runLater(() -> {
                updateServerFilesList(list);
            });
        }
    }

    public void pressOnDeleteData(ActionEvent actionEvent) {
        String fileName = filesListClient.getSelectionModel().getSelectedItem();
        deleteFile(fileName);

    }

    private void deleteFile(String fileName) {
        if (fileName == null) {
            return;
        }

        Path path = Paths.get("client_storage/" + fileName);
        if (!Files.exists(path)) {
            return;
        }

        try {
            Files.delete(path);
            refreshLocalFilesList();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void pressOnAddData(ActionEvent actionEvent) {

        File file = fileChooser.showOpenDialog(filesListServer.getScene().getWindow());
        if (file != null) {
            try {
                Files.copy(Paths.get(file.toURI()), Paths.get("client_storage/" + file.getName()) );
                refreshLocalFilesList();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void pressOnDeleteOnServerBtn(ActionEvent actionEvent) {
        String fileName = filesListServer.getSelectionModel().getSelectedItem();
        if (fileName != null) {
            Network.sendMsg(new FileRequest(FileCommand.DELETE, fileName));
            Network.sendMsg(new FileRequest(FileCommand.LIST_FILES));
        }
    }
}
