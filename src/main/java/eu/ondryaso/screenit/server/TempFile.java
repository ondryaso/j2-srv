package eu.ondryaso.screenit.server;

import fi.iki.elonen.NanoHTTPD;

import java.util.ArrayList;
import java.util.List;

public class TempFile implements NanoHTTPD.TempFileManager {

    private final String tmpdir;

    private final List<NanoHTTPD.TempFile> tempFiles;

    public TempFile() {
        this.tmpdir = System.getProperty("java.io.tmpdir");
        this.tempFiles = new ArrayList<>();
    }

    @Override
    public void clear() {
        for (NanoHTTPD.TempFile file : this.tempFiles) {
            try {
                file.delete();
            } catch (Exception ignored) {
            }
        }

        this.tempFiles.clear();
    }

    @Override
    public NanoHTTPD.TempFile createTempFile() throws Exception {
        NanoHTTPD.DefaultTempFile tempFile = new NanoHTTPD.DefaultTempFile(this.tmpdir);
        this.tempFiles.add(tempFile);
        return tempFile;
    }
}