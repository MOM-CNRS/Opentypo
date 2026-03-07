package fr.cnrs.opentypo.presentation.bean;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Adapter pour convertir jakarta.servlet.http.Part (h:inputFile) en MultipartFile (Spring).
 */
public class PartToMultipartFileAdapter implements MultipartFile {

    private final jakarta.servlet.http.Part part;

    public PartToMultipartFileAdapter(jakarta.servlet.http.Part part) {
        this.part = part;
    }

    @Override
    public String getName() {
        return part.getName();
    }

    @Override
    public String getOriginalFilename() {
        return part.getSubmittedFileName();
    }

    @Override
    public String getContentType() {
        return part.getContentType();
    }

    @Override
    public boolean isEmpty() {
        return part.getSize() == 0;
    }

    @Override
    public long getSize() {
        return part.getSize();
    }

    @Override
    public byte[] getBytes() throws IOException {
        return part.getInputStream().readAllBytes();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return part.getInputStream();
    }

    @Override
    public void transferTo(File dest) throws IOException {
        part.write(dest.getAbsolutePath());
    }
}
