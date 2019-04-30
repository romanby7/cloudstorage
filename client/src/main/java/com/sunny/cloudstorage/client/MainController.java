package com.sunny.cloudstorage.client;


import com.sunny.cloudstorage.common.Constants;
import com.sunny.cloudstorage.common.FileCommand;
import com.sunny.cloudstorage.common.FileMessage;
import com.sunny.cloudstorage.common.FileRequest;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    final private FileChooser fileChooser = new FileChooser();

    Client client;

    @FXML
    ListView<String> filesListServer;

    @FXML
    public ListView<String> filesListClient;

    @Override
    public void initialize(URL location, ResourceBundle resources) {


        client = new Client();

        //Network.start();

        Thread t = new Thread(() -> {
            try {
                while (true) {

                    client.start(this);

                    //                    AbstractMessage am = Network.readObject();
//                    if (am instanceof FileMessage) {
//                        FileMessage fm = (FileMessage) am;
//                        Files.write(Paths.get("client_storage/" + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
//                        refreshLocalFilesList();
//                    }
//                    if (am instanceof FilesListMessage) {
//                        FilesListMessage flm = (FilesListMessage) am;
//                        refreshServerFilesList(flm.getFilesList());
//                    }
//                    if (am instanceof FileRequest) {
//                        FileRequest fr = (FileRequest) am;
//                        switch (fr.getFileCommand()) {
//                            case DELETE:
//                                deleteFile(fr.getFilename());
//                                break;
//                            case SEND_PARTIAL_DATA:
//                                receiveFramesFromServer(fr);
//                                break;
//                            case LIST_FILES:
//                                refreshLocalFilesList();
//                                break;
//                        }
//                    }
                }
            } catch (InterruptedException e ) {
                e.printStackTrace();
            }
            finally {
                client.stop();
            }
        });
        t.setDaemon(true);
        t.start();
        refreshLocalFilesList();

//        client.getObjectEncoderCtx().writeAndFlush(new FileRequest(FileCommand.LIST_FILES));
    }

    void receiveFramesFromServer(FileRequest fm) throws IOException {
        processBytes(fm.getFileMessage(), "client_storage/");
    }


    private void processBytes(FileMessage fm, String pathPart) {
        Path path = Paths.get(pathPart + fm.getFilename());

        boolean append = fm.getMessageNumber() != 1;
        try (FileOutputStream fileOutputStream = new FileOutputStream(path.toFile(), append)) {
            fileOutputStream.write(fm.getData());
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(pathPart + path.getFileName() + ": " + fm.getMessageNumber());


    }

    public void pressOnDownloadBtn(ActionEvent actionEvent) {
        String fileName = filesListServer.getSelectionModel().getSelectedItem();
        if (fileName != null) {
            client.getClientHandlerCtx().writeAndFlush(new FileRequest(FileCommand.DOWNLOAD, fileName));
        }

    }

    public void pressOnSendData(ActionEvent actionEvent) {

        String fileName = filesListClient.getSelectionModel().getSelectedItem();
        if (fileName != null ) {
            Path path = Paths.get("client_storage/" + fileName);
            try {
                long fileSize = Files.size(path);
                if ( fileSize > Constants.FRAME_SIZE ) {
                    client.getClientHandlerCtx().writeAndFlush(new FileRequest(FileCommand.DELETE, fileName));
                    sendClientDataFrames(path);

                }
                else {
                    //client.getObjectEncoderCtx().writeAndFlush(new FileRequest(FileCommand.SEND, new FileMessage(path)));
                    client.getClientHandlerCtx().writeAndFlush(new FileRequest(FileCommand.SEND, new FileMessage(path)));
                }
                client.getClientHandlerCtx().writeAndFlush(new FileRequest(FileCommand.LIST_FILES));

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendClientDataFrames(Path path) throws IOException {

        byte[] byteBuf = new byte[Constants.FRAME_CHUNK_SIZE];
        FileInputStream fis = new FileInputStream(path.toFile());
        int read = 0;
        FileMessage fileMessage = new FileMessage(path, byteBuf, 1);
        FileRequest fileRequest = new FileRequest(FileCommand.SEND_PARTIAL_DATA, fileMessage);

        while((read = fis.read(byteBuf)) > 0) {
            if (read < Constants.FRAME_CHUNK_SIZE ) {
                byteBuf = Arrays.copyOf(byteBuf, read);
                fileMessage.setData(byteBuf);
            }
            client.getClientHandlerCtx().writeAndFlush(fileRequest);
            fileMessage.setMessageNumber(fileMessage.getMessageNumber() + 1);

        }

        fis.close();
    }

    void refreshLocalFilesList() {
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

    void refreshServerFilesList(List<String> list) {
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

    void deleteFile(String fileName) {
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
            client.getClientHandlerCtx().writeAndFlush(new FileRequest(FileCommand.DELETE, fileName));
        }
    }
}
