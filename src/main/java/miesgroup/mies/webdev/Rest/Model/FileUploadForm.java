package miesgroup.mies.webdev.Rest.Model;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

import java.util.ArrayList;
import java.util.List;

public class FileUploadForm {

    // --- Per upload multiplo ---
    @FormParam("files")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    private List<byte[]> files;

    @FormParam("fileNames")
    @PartType(MediaType.TEXT_PLAIN)
    private List<String> fileNames;

    // --- Per upload singolo (retrocompatibilità) ---
    @FormParam("fileData")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    private byte[] fileData;

    @FormParam("fileName")
    @PartType(MediaType.TEXT_PLAIN)
    private String fileName;

    // --- Utility: conversione in lista di SingleFile ---
    public List<SingleFile> getSingleFileList() {
        List<SingleFile> list = new ArrayList<>();
        if (files != null && fileNames != null && files.size() == fileNames.size()) {
            for (int i = 0; i < files.size(); i++) {
                list.add(new SingleFile(fileNames.get(i), files.get(i)));
            }
        } else if (fileData != null && fileName != null) {
            list.add(new SingleFile(fileName, fileData));
        }
        return list;
    }

    // Getters e setters...
    public List<byte[]> getFiles() {
        return files;
    }

    public void setFiles(List<byte[]> files) {
        this.files = files;
    }

    public List<String> getFileNames() {
        return fileNames;
    }

    public void setFileNames(List<String> fileNames) {
        this.fileNames = fileNames;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
