package com.sunny.cloudstorage.server;


import com.sunny.cloudstorage.common.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;

public class MainHandler extends ChannelInboundHandlerAdapter {

    private ByteArrayOutputStream baos = new ByteArrayOutputStream();

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
                        downloadFile(ctx, fr.getFilename());
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
                    case SEND_PARTIAL_DATA_END:
                        savePartialDataEndOnServer(fr);
                        listFiles(ctx);
                        break;
                }
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
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
        Utils.processBytes(fm, baos, "server_storage/");
    }

    private void savePartialDataEndOnServer(FileRequest fr) {
        FileMessage fm = fr.getFileMessage();
        String path = "server_storage/" + fm.getFilename();
        try {
            baos.write(fm.getData(), 0, fm.getData().length);
            Files.write(Paths.get(path), baos.toByteArray(),  StandardOpenOption.WRITE, StandardOpenOption.APPEND);
            baos.reset();
        } catch (IOException e) {
            e.printStackTrace();
        }

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

    private void downloadFile(ChannelHandlerContext ctx, String fileName ) throws IOException {
        Path path = Paths.get("server_storage/" + fileName);
        if (Files.exists(path)) {
            if (Files.size(path) > Constants.FRAME_SIZE) {
                sendDataFrames(ctx, path);
            }
            else {
                FileMessage fm = new FileMessage(path);
                ctx.writeAndFlush(fm);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    private void sendDataFrames(ChannelHandlerContext ctx, Path path) throws IOException {
        byte[] byteBuf = new byte[Constants.FRAME_CHUNK_SIZE];
        FileInputStream fis = new FileInputStream(path.toFile());
        byte[] fileBytesBuffer = new byte[Constants.FILE_CHUNK_SIZE];
        int read = 0;
        while((read = fis.read(fileBytesBuffer)) > 0) {
            if (read < Constants.FILE_CHUNK_SIZE ) {
                fileBytesBuffer = Arrays.copyOf(fileBytesBuffer, read);
            }
            byteBuf = Arrays.copyOf(byteBuf, Constants.FRAME_CHUNK_SIZE);

            for (int i = 0; i < fileBytesBuffer.length; i += Constants.FRAME_CHUNK_SIZE) {
                if (fileBytesBuffer.length - i < Constants.FRAME_CHUNK_SIZE) {
                    byteBuf = Arrays.copyOf(byteBuf, fileBytesBuffer.length - i);
                    System.arraycopy(fileBytesBuffer, i, byteBuf, 0, fileBytesBuffer.length - i);
                }
                else {
                    System.arraycopy(fileBytesBuffer, i, byteBuf, 0, Constants.FRAME_CHUNK_SIZE);
                }
                ctx.writeAndFlush(new FileRequest(FileCommand.SEND_PARTIAL_DATA, new FileMessage(path, byteBuf)));
            }

        }

        byteBuf = Arrays.copyOf(byteBuf, 0);
        ctx.writeAndFlush(new FileRequest(FileCommand.SEND_PARTIAL_DATA_END, new FileMessage(path, byteBuf)));

    }
}
