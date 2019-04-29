package com.sunny.cloudstorage.server;


import com.sunny.cloudstorage.common.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;

public class MainHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg == null) {
                return;
            }
            if (msg instanceof FileRequest) {
                FileRequest fr = (FileRequest) msg;
                switch (fr.getFileCommand()) {
                    case DOWNLOAD:
                        sendFileToClient(ctx, fr.getFilename());
                        break;
                    case LIST_FILES:
                        listFiles(ctx);
                        break;
                    case DELETE:
                        deleteFileOnServer(fr);
                        listFiles(ctx);
                        break;
                    case SEND:
                        saveFileOnServer(fr);
                        listFiles(ctx);
                        break;
                    case SEND_PARTIAL_DATA:
                        savePartialDataOnServer(fr);
                        break;
                }
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    private void sendFileToClient(ChannelHandlerContext ctx, String fileName) throws IOException {
        Path path = Paths.get("server_storage/" + fileName);
        if (Files.exists(path)) {
            if (Files.size(path) > Constants.FRAME_SIZE) {
                sendServerDataFrames(ctx, path);
                ctx.writeAndFlush(new FileRequest(FileCommand.LIST_FILES));
            } else {
                FileMessage fm = new FileMessage(path);
                ctx.writeAndFlush(fm);
            }
        }
    }

    private void sendServerDataFrames(ChannelHandlerContext ctx, Path path) throws IOException {
        byte[] byteBuf = new byte[Constants.FRAME_CHUNK_SIZE];

        FileMessage fileMessage = new FileMessage(path, byteBuf, 1);
        FileRequest fileRequest = new FileRequest(FileCommand.SEND_PARTIAL_DATA, fileMessage);

        FileInputStream fis = new FileInputStream(path.toFile());
        int read;
        while ((read = fis.read(byteBuf)) > 0) {
            if (read < Constants.FRAME_CHUNK_SIZE) {
                byteBuf = Arrays.copyOf(byteBuf, read);
                fileMessage.setData(byteBuf);
            }
            ctx.writeAndFlush(fileRequest);
            fileMessage.setMessageNumber(fileMessage.getMessageNumber() + 1);
        }

        System.out.println("server_storage/" + path.getFileName() +  ", server last frame number: " + fileMessage.getMessageNumber());
        System.out.println("server_storage/" + path.getFileName() +  ": closing file stream.");

        fis.close();
    }

    private void deleteFileOnServer(FileRequest fr) {
        String path = "server_storage/" + fr.getFilename();
        try {
            if (Files.exists(Paths.get(path))) {
                Files.delete(Paths.get(path));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveFileOnServer(FileRequest fr) {
        deleteFileOnServer(fr);
        FileMessage fm = fr.getFileMessage();
        String path = "server_storage/" + fm.getFilename();
        try {
            Files.write(Paths.get(path), fm.getData(), StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void savePartialDataOnServer(FileRequest fr) {
        FileMessage fm = fr.getFileMessage();
        Utils.processBytes(fm, "server_storage/");
    }

    private void listFiles(ChannelHandlerContext ctx) {
        ArrayList<String> filesList = new ArrayList<>();
        try {
            Files.list(Paths.get("server_storage")).map((Path p) -> p.getFileName().toString()).forEach((String o) -> filesList.add(o));
        } catch (IOException e) {
            e.printStackTrace();
        }

        FilesListMessage filesListMessage = new FilesListMessage(filesList);
        ctx.writeAndFlush(filesListMessage);

    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }



}

