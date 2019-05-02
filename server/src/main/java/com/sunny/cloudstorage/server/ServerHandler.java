package com.sunny.cloudstorage.server;

import com.sunny.cloudstorage.common.*;
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
            if (msg instanceof FileRequest) {
                FileRequest fr = (FileRequest) msg;
                switch (fr.getFileCommand()) {
                    case LIST_FILES:
                        listFilesOnServer(ctx);
                        break;
                    case DELETE:
                        deleteFileOnServer(fr);
                        break;
                    case SEND_FILE_CHUNK_TO_CLIENT:
                        sendFileChunkToClient(ctx, fr);
                        break;
                    case SEND_FILE_CHUNK_TO_SERVER:
                        saveFileChunkOnServer(ctx, fr);
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

    private void saveFileChunkOnServer(ChannelHandlerContext ctx, FileRequest fr) throws IOException {

        FileMessage fm = fr.getFileMessage();
        String fileName = fm.getFilename();
        long offset = fm.getOffset();
        byte[] data = fm.getData();

        Path path = Paths.get("server_storage/" + fileName);

        RandomAccessFile raf = new RandomAccessFile(path.toFile(), "rw");
        raf.seek(offset);
        raf.write(data);
        raf.close();

        FileMessage fileMessage = new FileMessage(fileName,offset + Constants.FRAME_CHUNK_SIZE);
        FileRequest fileRequest = new FileRequest(FileCommand.SEND_FILE_CHUNK_TO_SERVER, fileMessage);

        ctx.writeAndFlush(fileRequest);

    }

    private void sendFileChunkToClient(ChannelHandlerContext ctx, FileRequest fr) throws IOException {

        String fileName = fr.getFileMessage().getFilename();
        Path path = Paths.get("server_storage/" + fileName);
        if (Files.exists(path)) {

            RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r");
            long offset = fr.getFileMessage().getOffset();
            long length = raf.length();
            if ( utils.isFileChunksCompleted(raf, offset, length)) {
                ctx.writeAndFlush(new FileRequest(FileCommand.FILE_CHUNK_COMPLETED));
                return;
            }

            byte[] byteBuf = utils.readBytes(raf, offset, length);

            FileMessage fileMessage = new FileMessage(fileName, byteBuf, offset);
            FileRequest fileRequest = new FileRequest(FileCommand.SEND_FILE_CHUNK_TO_CLIENT, fileMessage);

            ctx.writeAndFlush(fileRequest);

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

    private void listFilesOnServer(ChannelHandlerContext ctx) {
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

