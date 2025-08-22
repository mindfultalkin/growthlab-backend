package com.mindfultalk.growthlab.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "content")
public class ContentMaster {
    
    @Id
    @Column(name = "content_id")
    private int contentId;
    
    @Column(name = "content_name", length = 500, nullable = true)
    private String contentName;

	@Column(name = "content_desc", length = 500, nullable = true)
    private String contentDesc;

    @Column(name = "content_origin", length = 500, nullable = true)
    private String contentOrigin;

    @Column(name = "content_topic", length = 255, nullable = true)
    private String contentTopic;

    @Column(name = "uuid", nullable = false, unique = true)
    private  UUID uuid;

	public ContentMaster() {
		
	}

	public ContentMaster(int contentId, String contentName, String contentDesc, String contentOrigin, String contentTopic, UUID uuid) {
		super();
		this.contentId = contentId;
		this.contentName = contentName;
		this.contentDesc = contentDesc;
		this.contentOrigin = contentOrigin;
		this.contentTopic = contentTopic;
		this.uuid = uuid;
	}

	// Getters and Setters
	public int getContentId() {
		return contentId;
	}

	public void setContentId(int contentId) {
		this.contentId = contentId;
	}
	
	public String getContentName() {
		return contentName;
	}

	public void setContentName(String contentName) {
		this.contentName = contentName;
	}

	public String getContentDesc() {
		return contentDesc;
	}

	public void setContentDesc(String contentDesc) {
		this.contentDesc = contentDesc;
	}

	public String getContentOrigin() {
		return contentOrigin;
	}

	public void setContentOrigin(String contentOrigin) {
		this.contentOrigin = contentOrigin;
	}

	public String getContentTopic() {
		return contentTopic;
	}

	public void setContentTopic(String contentTopic) {
		this.contentTopic = contentTopic;
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	@Override
	public String toString() {
		return "ContentMaster [contentId=" + contentId + ", contentName=" + contentName + ", contentDesc=" + contentDesc
				+ ", contentOrigin=" + contentOrigin + ", contentTopic=" + contentTopic + ", uuid=" + uuid + "]";
	}

	// Method to ensure UUID and generate contentId before persisting
    @PrePersist
    private void ensureUuid() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID();
        }

    }
    
}