package com.mindfultalk.growthlab.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "media_files")
public class MediaFile {

	@Id
    @Column(name = "file_id", nullable = false, unique = true, length = 255)
    private String fileId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "uploaded_at", nullable = false)
    @CreationTimestamp
    private OffsetDateTime uploadedAt;

    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    // Getters, Setters, Constructors
  
	public MediaFile() {
		}
	public MediaFile(String fileId, String fileName, String fileType, Long fileSize, String filePath,
			OffsetDateTime uploadedAt, UUID uuid, User user) {
		super();
		this.fileId = fileId;
		this.fileName = fileName;
		this.fileType = fileType;
		this.fileSize = fileSize;
		this.filePath = filePath;
		this.uploadedAt = uploadedAt;
		this.uuid = uuid;
		this.user = user;
	}

		public String getFileId() {
		return fileId;
	}
	public void setFileId(String fileId) {
		this.fileId = fileId;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getFileType() {
		return fileType;
	}
	public void setFileType(String fileType) {
		this.fileType = fileType;
	}
	public Long getFileSize() {
		return fileSize;
	}
	public void setFileSize(Long fileSize) {
		this.fileSize = fileSize;
	}
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	public OffsetDateTime getUploadedAt() {
		return uploadedAt;
	}
	public void setUploadedAt(OffsetDateTime uploadedAt) {
		this.uploadedAt = uploadedAt;
	}
	public UUID getUuid() {
		return uuid;
	}
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}
	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}
	
	
		@Override
	public String toString() {
		return "MediaFile [fileId=" + fileId + ", fileName=" + fileName + ", fileType=" + fileType + ", fileSize="
				+ fileSize + ", filePath=" + filePath + ", uploadedAt=" + uploadedAt + ", uuid=" + uuid + ", user="
				+ user + "]";
	}
		// Method to ensure UUID and generate fileId before persisting
	    @PrePersist
	    private void generateFileId() {
	        if (this.uuid == null) {
	            this.uuid = UUID.randomUUID();
	        }
	        this.fileId = user.getUserId() + "-" + UUID.randomUUID().toString().substring(0, 8);
	    }

}