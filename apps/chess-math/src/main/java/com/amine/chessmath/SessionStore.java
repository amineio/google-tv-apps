package com.amine.chessmath;

import android.util.AtomicFile;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/** No backup or network copies. AtomicFile retains the old revision on failed writes. */
public final class SessionStore implements SessionController.Persistence {
    private final AtomicFile file;
    public SessionStore(File directory) { file=new AtomicFile(new File(directory,"session-v1.json")); }
    @Override public Session load() throws Exception {
        if(!file.getBaseFile().exists() && !new File(file.getBaseFile()+".bak").exists()) return null;
        return SessionCodec.decode(new String(file.readFully(),StandardCharsets.UTF_8));
    }
    @Override public void save(Session session) throws Exception {
        byte[] bytes=SessionCodec.encode(session).getBytes(StandardCharsets.UTF_8);
        FileOutputStream stream=null;
        try {
            stream=file.startWrite(); stream.write(bytes); stream.flush(); stream.getFD().sync();
            file.finishWrite(stream);
        } catch(Exception failure) {
            if(stream!=null)file.failWrite(stream);
            throw failure;
        }
    }
}
