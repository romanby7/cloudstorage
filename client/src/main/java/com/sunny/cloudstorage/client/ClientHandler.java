package com.sunny.cloudstorage.client;

import com.sunny.cloudstorage.common.FileMessage;
import com.sunny.cloudstorage.common.FileRequest;
import com.sunny.cloudstorage.common.FilesListMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class ClientHandler extends ChannelInboundHandlerAdapter {

    private MainController mainController;

    public ClientHandler(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        try {
            if (msg instanceof FileMessage) {
                FileMessage fm = (FileMessage) msg;
                Files.write(Paths.get("client_storage/" + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                mainController.refreshLocalFilesList();
            }
            if (msg instanceof FilesListMessage) {
                FilesListMessage flm = (FilesListMessage) msg;
                mainController.refreshServerFilesList(flm.getFilesList());
            }
            if (msg instanceof FileRequest) {
                FileRequest fr = (FileRequest) msg;
                switch (fr.getFileCommand()) {
                    case DELETE:
                        mainController.deleteFile(fr.getFilename());
                        break;
                    case SEND_PARTIAL_DATA:
                        mainController.receiveFramesFromServer(fr);
                        break;
                    case LIST_FILES:
                        mainController.refreshLocalFilesList();
                        break;
                }
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }



}
