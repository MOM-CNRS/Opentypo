package fr.cnrs.opentypo.presentation.bean.candidats;

import org.primefaces.model.file.UploadedFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Adapter pour convertir UploadedFile (PrimeFaces) en MultipartFile (Spring).
 */
public class UploadedFileToMultipartFileAdapter implements MultipartFile {

    private final UploadedFile uploadedFile;

    public UploadedFileToMultipartFileAdapter(UploadedFile uploadedFile) {
        this.uploadedFile = uploadedFile;
    }

    @Override public String getName() { return uploadedFile.getFileName(); }
    @Override public String getOriginalFilename() { return uploadedFile.getFileName(); }
    @Override public String getContentType() { return uploadedFile.getContentType(); }
    @Override public boolean isEmpty() { return uploadedFile.getSize() == 0; }
    @Override public long getSize() { return uploadedFile.getSize(); }
    @Override public byte[] getBytes() throws IOException { return uploadedFile.getContent(); }
    @Override public java.io.InputStream getInputStream() throws IOException { return uploadedFile.getInputStream(); }
    @Override public void transferTo(java.io.File dest) { throw new UnsupportedOperationException("transferTo not supported"); }
}
