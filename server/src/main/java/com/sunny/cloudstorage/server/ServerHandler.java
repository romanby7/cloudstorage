package com.sunny.cloudstorage.server;

import com.sunny.cloudstorage.common.Constants;
import com.sunny.cloudstorage.common.FileCommand;
import com.sunny.cloudstorage.common.FileMessage;
import com.sunny.cloudstorage.common.Utils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class ServerHandler extends ChannelInboundHandlerAdapter {

    private final Utils utils = new Utils();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg == null) {
                return;
            }
            if (msg instanceof FileMessage) {
                FileMessage fm = (FileMessage) msg;
                switch (fm.getFileCommand()) {
                    case LIST_SERVER_FILES:
                        listFilesOnServer(ctx);
                        break;
                    case DELETE:
                        deleteFileOnServer(fm);
                        break;
                    case SEND_FILE_CHUNK_TO_CLIENT:
                        sendFileChunkToClient(ctx, fm);
                        break;
                    case SEND_FILE_CHUNK_TO_SERVER:
                        saveFileChunkOnServer(ctx, fm);
                        break;
                    case FILE_CHUNK_COMPLETED:
                        listFilesOnServer(ctx);
                        break;
                    default:
                        break;
                }
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    private void saveFileChunkOnServer(ChannelHandlerContext ctx, FileMessage fm) throws IOException {

        String fileName = fm.getFilename();
        long offset = fm.getOffset();
        byte[] data = fm.getData();

        Path path = Paths.get("server_storage/" + fileName);

        RandomAccessFile raf = new RandomAccessFile(path.toFile(), "rw");
        raf.seek(offset);
        raf.write(data);
        raf.close();

        FileMessage fileMessage = new FileMessage(FileCommand.SEND_FILE_CHUNK_TO_SERVER, fileName, offset + Constants.FRAME_CHUNK_SIZE);

        ctx.writeAndFlush(fileMessage);

    }

    private void sendFileChunkToClient(ChannelHandlerContext ctx, FileMessage fm) throws IOException {

        String fileName = fm.getFilename();
        Path path = Paths.get("server_storage/" + fileName);
        if (Files.exists(path)) {

            RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r");
            long offset = fm.getOffset();
            long length = raf.length();
            if ( utils.isFileChunksCompleted(raf, offset, length)) {
                ctx.writeAndFlush(new FileMessage(FileCommand.FILE_CHUNK_COMPLETED));
                return;
            }

            byte[] byteBuf = utils.readBytes(raf, offset, length);

            FileMessage fileMessage = new FileMessage(FileCommand.SEND_FILE_CHUNK_TO_CLIENT, fileName, byteBuf, offset);

            ctx.writeAndFlush(fileMessage);

        }
    }

    private void deleteFileOnServer(FileMessage fm) {
        String path = "server_storage/" + fm.getFilename();
        try {
            if (Files.exists(Paths.get(path))) {
                Files.delete(Paths.get(path));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listFilesOnServer(ChannelHandlerContext ctx) {
        ArrayList<String> filesList = new ArrayList<>();
        try {
            Files.list(Paths.get("server_storage")).map((Path p) -> p.getFileName().toString()).forEach((String o) -> filesList.add(o));
        } catch (IOException e) {
            e.printStackTrace();
        }

        FileMessage fm = new FileMessage(FileCommand.LIST_SERVER_FILES, filesList);
        ctx.writeAndFlush(fm);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }



}

