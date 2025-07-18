package miesgroup.mies.webdev.Model.file;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "filepdf") // Nome corretto della tabella
public class PDFFile extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment su MySQL
    @Column(name = "Id_File") // Nome corretto della colonna ID
    private Integer idFile;

    @Column(name = "File_Name", nullable = false, unique = true)
    private String fileName;

    @Lob
    @Column(name = "file_Data", columnDefinition = "LONGBLOB")
    private byte[] fileData;

    @Column(name = "upload_date")
    private LocalDateTime uploadDate;

    @Column(name = "id_pod")
    private String idPod;

    // Getters e Setters

    public LocalDateTime getUploadDate() { return uploadDate; }

    public void setUploadDate(LocalDateTime uploadDate) { this.uploadDate = uploadDate; }

    public Integer getIdFile() {
        return idFile;
    }

    public void setIdFile(Integer idFile) {
        this.idFile = idFile;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }

    public String getIdPod() {
        return idPod;
    }

    public void setIdPod(String idPod) {
        this.idPod = idPod;
    }
}
