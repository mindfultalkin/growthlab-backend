package com.mindfultalk.growthlab.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.regions.Region;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.UUID;

@Service
public class S3StorageService {

	private final S3Client s3Client;
	private final String bucketName;
	private final Region region;
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(S3StorageService.class);

	@Autowired
	public S3StorageService(
	    S3Client s3Client, 
	    @Value("${aws.s3.bucket}") String bucketName,
	    @Value("${aws.region}") String regionName) {
	    this.s3Client = s3Client;
	    this.bucketName = bucketName;
	    this.region = Region.of(regionName);
	}

    /**
     * Upload file to S3 with a hierarchical path
     * 
     * @param file The file to upload
     * @param cohortId The cohort ID
     * @param userId The user ID
     * @param programId The program ID
     * @param stageId The stage ID
     * @param unitId The unit ID
     * @param subconceptId The subconcept ID
     * @param type The type (submitted or corrected)
     * @return The S3 key of the uploaded file
     * @throws IOException If there's an error reading the file
     */
    public String uploadFile(MultipartFile file, String cohortId, String userId, 
                             String programId, String stageId, String unitId, 
                             String subconceptId, String type) throws IOException {
        
        String uuid = UUID.randomUUID().toString();
        String fileName = uuid + "_" + file.getOriginalFilename();
        
        // Create a hierarchical path
        String s3Key = String.format("cohort-%s/user-%s/assignments/%s/%s/%s/%s/%s/%s", 
                cohortId, userId, programId, stageId, unitId, subconceptId, type, fileName);
        
        logger.info("Uploading file to S3: {}", s3Key);
        
        PutObjectRequest putObjRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(file.getContentType())
                .acl(ObjectCannedACL.PUBLIC_READ)  // Make the object publicly readable
                .build();
        
        s3Client.putObject(putObjRequest, RequestBody.fromBytes(file.getBytes()));
        
        return s3Key;
    }

    /**
     * Download file from S3
     * 
     * @param s3Key The S3 key of the file
     * @return The file input stream
     */
    public ResponseInputStream<GetObjectResponse> downloadFile(String s3Key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
        
        return s3Client.getObject(getObjectRequest);
    }
    
 // Generate a direct public URL instead of a presigned URL with credentials
    public String generatePublicUrl(String s3Key) {
        // Construct a direct URL to the public object
        return String.format("https://%s.s3.%s.amazonaws.com/%s", 
                bucketName, 
                region.toString(), 
                s3Key);
    }

    /**
     * Generate a presigned URL for a file in S3
     * 
     * @param s3Key The S3 key of the file
     * @param expirationMinutes URL expiration time in minutes
     * @return The presigned URL
     */
    public URL generatePresignedUrl(String s3Key, int expirationMinutes) {
        // Use the region object that you've already initialized in the constructor
        software.amazon.awssdk.services.s3.presigner.S3Presigner presigner = 
            software.amazon.awssdk.services.s3.presigner.S3Presigner.builder()
                .region(region)  // Use the region field from the class
                .build();
        
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
        
        software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest presignRequest = 
                software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest.builder()
                    .signatureDuration(java.time.Duration.ofMinutes(expirationMinutes))
                    .getObjectRequest(getObjectRequest)
                    .build();
            
            return presigner.presignGetObject(presignRequest).url();
        }
        
 // Method to update an existing object's ACL to public-read
    public void makeFilePublic(String s3Key) {
        PutObjectAclRequest aclRequest = PutObjectAclRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .acl(ObjectCannedACL.PUBLIC_READ)
                .build();
        
        s3Client.putObjectAcl(aclRequest);
    }
    /**
     * Delete a file from S3
     * 
     * @param s3Key The S3 key of the file
     */
    public void deleteFile(String s3Key) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
        
        s3Client.deleteObject(deleteObjectRequest);
    }
}