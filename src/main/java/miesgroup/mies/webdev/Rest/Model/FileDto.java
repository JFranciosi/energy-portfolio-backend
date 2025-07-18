package miesgroup.mies.webdev.Rest.Model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import miesgroup.mies.webdev.Model.PDFFile;

import java.io.Serializable;

public class FileDto implements Serializable {
    private int id;
    private String fileName;
    private String idPod;
    private String uploadDate; // Puoi cambiare in LocalDateTime se vuoi (occhio a Jackson!)

    // Costruttore da entità PDFFile
    public FileDto(PDFFile file) {
        this.id = file.getIdFile();
        this.fileName = file.getFileName();
        this.idPod = file.getIdPod();
        this.uploadDate = file.getUploadDate() != null
                ? file.getUploadDate().toString()
                : null;
    }
// Costruttore per deserializzazione JSON
    @JsonCreator
    public FileDto(
            @JsonProperty("id") int id,
            @JsonProperty("fileName") String fileName,
            @JsonProperty("idPod") String idPod,
            @JsonProperty("uploadDate") String uploadDate
    ) {
        this.id = id;
        this.fileName = fileName;
        this.idPod = idPod;
        this.uploadDate = uploadDate;
    }

    // Getter e Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getIdPod() { return idPod; }
    public void setIdPod(String idPod) { this.idPod = idPod; }

    public String getUploadDate() { return uploadDate; }
    public void setUploadDate(String uploadDate) { this.uploadDate = uploadDate; }
}
