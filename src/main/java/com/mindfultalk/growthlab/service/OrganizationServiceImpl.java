package com.mindfultalk.growthlab.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.*;
import com.mindfultalk.growthlab.dto.*;
import com.mindfultalk.growthlab.model.*;
import com.mindfultalk.growthlab.repository.*;
import com.mindfultalk.growthlab.util.RandomStringUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.slf4j.*;

@Service
@Transactional
public class OrganizationServiceImpl implements OrganizationService {
	
    @Autowired
    private OrganizationRepository organizationRepository;
    
    @Autowired
    private  CohortProgramRepository cohortProgramRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private UserCohortMappingRepository userCohortMappingRepository;

    @Autowired
    private CohortRepository cohortRepository;
    
    private static final Logger log = LoggerFactory.getLogger(OrganizationServiceImpl.class);


    // Temporary store for OTPs (in a real-world app, use a more secure solution like Redis)
    private Map<String, String> otpStorage = new HashMap<>();

 // Generate a random 6-letter password
    private String generatePassword() {
        return RandomStringUtil.generateRandomAlphabetic(6); // Generates a random string of 6 letters
    }
 
 // Method to generate a random OTP
    private String generateOTP() {
        return RandomStringUtil.generateRandomNumeric(6); // Generates a random 6-digit OTP
    }

    // Method to send OTP via email
    private void sendOTPEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Your OTP for Password Reset");
        message.setText("Your OTP for resetting the password is: " + otp);
        mailSender.send(message);
    }

 // Method to handle forgotten password - only requires email
    public void sendForgotPasswordOTP(String email) {
        Organization organization = getOrganizationByEmail(email);
        if (organization != null) {
            String otp = generateOTP();
            otpStorage.put(email, otp); // Store OTP in memory (temporary solution)
            sendOTPEmail(email, otp); // Send OTP via email
        } else {
            throw new RuntimeException("Admin email not found. Please check and try again.");
        }
    }
 // Verify OTP and generate a new password
    @Override
    @CacheEvict(value = {"organizations", "organizationByEmail"}, key = "#email")
    public String verifyOTPAndGenerateNewPassword(String email, String otp) {
        Organization organization = getOrganizationByEmail(email);
        if (organization == null) {
            throw new RuntimeException("Admin email not found. Please check and try again.");
        }

        if (!otpStorage.containsKey(email) || !otpStorage.get(email).equals(otp)) {
            throw new RuntimeException("Invalid or expired OTP. Please try again.");
        }

        // OTP is valid; generate and update password
        String newPassword = generatePassword();
        String encodedPassword = passwordEncoder.encode(newPassword);
        organization.setOrgPassword(encodedPassword);

        organizationRepository.save(organization); // Save updated password
        sendNewPasswordEmail(email, newPassword); // Send new password to user

        otpStorage.remove(email); // Clear the OTP after use
        
        return newPassword;
    }

    
 // Reset password using old password (no OTP required)
    @Override
    @CacheEvict(value = {"organizations", "organizationByEmail"}, key = "#email")
    public void resetPasswordWithOldPassword(String email, String oldPassword, String newPassword) {
        Organization organization = getOrganizationByEmail(email);
        if (organization == null) {
            throw new RuntimeException("Admin email not found. Please check and try again.");
        }

        // Verify old password
        if (!passwordEncoder.matches(oldPassword, organization.getOrgPassword())) {
            throw new RuntimeException("Current password is incorrect. Please try again.");
        }

        // Update password
        String encodedPassword = passwordEncoder.encode(newPassword);
        organization.setOrgPassword(encodedPassword);
        organization.setUpdatedAt(OffsetDateTime.now(ZoneId.of("Asia/Kolkata")));

        organizationRepository.save(organization);
        
        // Send confirmation email
        sendPasswordChangeConfirmationEmail(email);
    }

 // Method to send password change confirmation
    private void sendPasswordChangeConfirmationEmail(String to) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Password Changed Successfully");
        message.setText("Your password has been changed successfully. If you did not make this change, please contact support immediately.");
        mailSender.send(message);
    }
   
 // Method to send the new password to the admin's email
    private void sendNewPasswordEmail(String to, String newPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Your New Password");
        message.setText("Your password has been reset. Your new password is: " + newPassword + 
                        "\n\nFor security reasons, we recommend changing this password after logging in.");
        mailSender.send(message);
    }

    // Save or update an organization
    @Transactional
    @CacheEvict(value = {"organizations", "organizationByEmail"}, allEntries = true)
    public Organization saveOrganization(Organization organization) {
        System.out.println("Attempting to save organization: " + organization);

        String plainPassword = organization.getOrgPassword();
        if (plainPassword == null || plainPassword.isEmpty()) {
            plainPassword = generatePassword(); // Generate a new plain password
        }

        String encodedPassword = passwordEncoder.encode(plainPassword); // Hash the password
        organization.setOrgPassword(encodedPassword); // Store hashed password

        Organization savedOrganization = organizationRepository.save(organization);
        System.out.println("Saved organization: " + savedOrganization);

        sendWelcomeEmail(savedOrganization, plainPassword); // Send the plain password in email

        return savedOrganization;
    }

    @Async
    private void sendWelcomeEmail(Organization organization, String plainPassword) {
        String adminName = organization.getOrganizationAdminName();
        String adminEmail = organization.getOrganizationAdminEmail();
        String orgAdminUrl = "https://courses-admin.mindfultalk.in"; 
        String supportEmail = "support@mindfultalk.in"; 

        String subject = "Welcome to Mindfultalk!";
        String messageText = "Hello " + adminName + ",\n\n" +
                "Welcome to Mindfultalk! We are excited to partner with your organization in improving business and professional communication. " +
                "Our platform is designed to help professionals and learners articulate ideas effectively and make complex knowledge clear and actionable.\n\n" +
                "Here are your first steps as an Admin:\n" +
                "1. Create Cohorts: In your admin dashboard, set up cohorts to organize learners across different programs. " +
                "If you need help, please reach out to " + supportEmail + ".\n" +
                "2. Add Learners: Add learners to your organization and assign them to cohorts. A learner can belong to multiple cohorts. " +
                "You can also bulk-upload learners for convenience.\n" +
                "3. Track Progress: Under Reports, you can monitor learner progress as they engage with the assigned programs.\n\n" +
                "To get started, log in to your dashboard with the following credentials:\n" +
                "User ID: " + adminEmail + "\n" +
                "Password: " + plainPassword + "\n" +
                "Organization Dashboard: " + orgAdminUrl + "\n\n" +
                "If you have any questions or need support, our team is here to help at " + supportEmail + ".\n\n" +
                "Best regards,\n" +
                "The Mindfultalk Team";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(adminEmail);
        message.setSubject(subject);
        message.setText(messageText);
        mailSender.send(message);
    }



    @Override
    @Cacheable(value = "organizationByEmail", key = "#email")
    public Organization getOrganizationByEmail(String email) {
        System.out.println("OrganizationByEmail");
        return organizationRepository.findByOrganizationAdminEmail(email);
    }

    // Retrieve all organizations
    @Override
    @Cacheable(value = "organizations", key = "'all'")
    public List<Organization> getAllOrganizations() {
        return organizationRepository.findAll();
    }

    // Retrieve an organization by ID
    @Override
    @Cacheable(value = "organizations", key = "#organizationId")
    public Optional<Organization> getOrganizationById(String organizationId) {
        return organizationRepository.findById(organizationId);
    }



 // Delete an organization by ID
    @Override
    @CacheEvict(value = {"organizations", "organizationByEmail"}, allEntries = true)
    public void deleteOrganization(String organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new EntityNotFoundException("Organization not found"));
        organization.setDeletedAt(OffsetDateTime.now(ZoneId.of("Asia/Kolkata")));
        organizationRepository.save(organization);
    }

    @Override
    @CacheEvict(value = {"organizations", "organizationByEmail"}, allEntries = true)
    public void hardDeleteOrganization(String organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new EntityNotFoundException("Organization not found"));
        
        // Permanently remove
        organizationRepository.delete(organization);
    }

    
 // Create a new organization
    @Override
    @CacheEvict(value = {"organizations", "organizationByEmail"}, allEntries = true)
    public Organization createOrganization(Organization organization) {
        // Check if the email is already taken
        if (organizationRepository.existsByOrganizationAdminEmail(organization.getOrganizationAdminEmail())) {
            throw new DataIntegrityViolationException("The email address is already in use. Please use a different email.");
        }
     // Assign default logo if missing
        if (organization.getLogo() == null || organization.getLogo().isBlank()) {
            organization.setLogo("images/assets/default-logo.png");
        }
        // Generate organizationId before saving the organization
        organization.setOrganizationId(generateUniqueOrganizationId(organization.getOrganizationName()));
        
     // Generate plain password and hash it
        String plainPassword = RandomStringUtil.generateRandomAlphanumeric(6);
        String hashedPassword = passwordEncoder.encode(plainPassword);
        organization.setOrgPassword(hashedPassword);

        // Save organization
        Organization savedOrganization = organizationRepository.save(organization);

        // Send plain password via email
        sendWelcomeEmail(savedOrganization, plainPassword);

        return savedOrganization;
    }

    // Method to generate a unique organizationId
    private String generateUniqueOrganizationId(String organizationName) {
        if (organizationName == null || organizationName.isEmpty()) {
            throw new IllegalArgumentException("Organization name cannot be null or empty.");
        }

        // Remove spaces and get the first 4 characters
        String nameWithoutSpaces = organizationName.replaceAll("\\s+", "");
        String baseOrgId = nameWithoutSpaces.length() >= 4
                ? nameWithoutSpaces.substring(0, 4).toUpperCase()
                : String.format("%-4s", nameWithoutSpaces).replace(' ', 'X').toUpperCase();

        // Check if the organizationId already exists
        String organizationId = baseOrgId;
        int counter = 1;

        // Keep appending the counter until the organizationId is unique
        while (organizationRepository.existsById(organizationId)) {
            organizationId = baseOrgId + counter;
            counter++;
        }

        return organizationId;
    }

    // Create multiple organizations
    @Override
    @CacheEvict(value = {"organizations", "organizationByEmail"}, allEntries = true)
    public List<Organization> createOrganizations(List<Organization> organizations) {
        return organizationRepository.saveAll(organizations);
    }
    
    
 // Update an existing organization
    @Override
    @CachePut(value = "organizations", key = "#organizationId")
    @CacheEvict(value = {"organizations", "organizationByEmail"}, key = "'all'")
    public Organization updateOrganization(String organizationId, Organization updatedOrganization) {
        return organizationRepository.findById(organizationId)
            .map(existingOrganization -> {
                // Validate updated fields
                if (updatedOrganization.getUpdatedAt() != null &&
                        updatedOrganization.getUpdatedAt().isBefore(existingOrganization.getCreatedAt())) {
                    throw new IllegalArgumentException("Updated date must be after the organization creation date.");
                }

                // Update fields other than organizationId
                existingOrganization.setOrganizationName(updatedOrganization.getOrganizationName());
                existingOrganization.setOrganizationAdminName(updatedOrganization.getOrganizationAdminName());
                existingOrganization.setOrganizationAdminEmail(updatedOrganization.getOrganizationAdminEmail());
                existingOrganization.setOrganizationAdminPhone(updatedOrganization.getOrganizationAdminPhone());
                existingOrganization.setIndustry(updatedOrganization.getIndustry());
                existingOrganization.setLogo(updatedOrganization.getLogo());
                existingOrganization.setSignature(updatedOrganization.getSignature());
                existingOrganization.setThemeColors(updatedOrganization.getThemeColors());

                // Hash and update password if provided
                if (updatedOrganization.getOrgPassword() != null && !updatedOrganization.getOrgPassword().isEmpty()) {
                    String hashedPassword = passwordEncoder.encode(updatedOrganization.getOrgPassword());
                    existingOrganization.setOrgPassword(hashedPassword);
                }

                // Automatically update the timestamp
                existingOrganization.setUpdatedAt(OffsetDateTime.now(ZoneId.of("Asia/Kolkata")));

                 // Handle soft delete
                    if (updatedOrganization.getDeletedAt() != null) {
                        existingOrganization.setDeletedAt(updatedOrganization.getDeletedAt());
                    }
                 // Save updated organization
                    Organization savedOrganization = organizationRepository.save(existingOrganization);

                    // Log the update operation
                    log.info("Organization updated: ID={}, UpdatedFields={}", organizationId, updatedOrganization);

                    return savedOrganization;
                })
                .orElseThrow(() -> new EntityNotFoundException("Organization not found with id: " + organizationId));
    }
    
    @Override
    public boolean verifyPassword(String plainPassword, String encodedPassword) {
        return passwordEncoder.matches(plainPassword, encodedPassword);
    }

    
    
    @Override
    @Cacheable(value = "organizationPrograms", key = "#organizationId")
    public List<Program> getProgramsByOrganizationId(String organizationId) {
        return cohortProgramRepository.findProgramsByOrganizationId(organizationId);
    }
    
    @Override
    @Cacheable(value = "organizationCohorts", key = "#organizationId")
    public List<Cohort> getCohortsByOrganizationId(String organizationId) {
        return cohortRepository.findByOrganizationOrganizationId(organizationId);
    }

    @Override
    @Cacheable(value = "programsWithCohorts", key = "#organizationId")
    public List<ProgramResponseDTO> getProgramsWithCohorts(String organizationId) {
        List<CohortProgram> cohortPrograms = cohortProgramRepository.findCohortsByOrganizationId(organizationId);

        Map<String, ProgramResponseDTO> programMap = new LinkedHashMap<>();

        for (CohortProgram cohortProgram : cohortPrograms) {
            // Extract program and cohort details
            Program program = cohortProgram.getProgram();
            Cohort cohort = cohortProgram.getCohort();

            // Check if the program is already in the map
            ProgramResponseDTO programResponse = programMap.computeIfAbsent(
                program.getProgramId(),
                id -> {
                    ProgramResponseDTO dto = new ProgramResponseDTO();
                    dto.setProgramId(program.getProgramId());
                    dto.setProgramName(program.getProgramName());
                    dto.setProgramDesc(program.getProgramDesc());
                    dto.setCohorts(new ArrayList<>());
                    return dto;
                }
            );

            // Add the cohort to the program
            CohortDTO cohortDTO = new CohortDTO();
            cohortDTO.setCohortId(cohort.getCohortId());
            cohortDTO.setCohortName(cohort.getCohortName());
            programResponse.getCohorts().add(cohortDTO);
        }

        // Convert the map to a list of ProgramResponseDTO
        return new ArrayList<>(programMap.values());
    }

    @Override
    public long calculateDaysToEnd(OffsetDateTime cohortEndDate) {
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Kolkata"));
        return ChronoUnit.DAYS.between(now, cohortEndDate);
    }

    @Override
    @Cacheable(value = "cohortDetails", key = "#cohortId")
    public Map<String, Object> getCohortDetailsWithDaysToEnd(String cohortId) {
        Cohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new RuntimeException("Cohort not found"));
        long daysToEnd = calculateDaysToEnd(cohort.getCohortEndDate());
        Map<String, Object> details = new HashMap<>();
        details.put("cohort", cohort);
        details.put("daysToEnd", daysToEnd);
        return details;
    }


    /**
     * Notify users and organizations about the cohort end date.
     */
    @Override
    @Scheduled(cron = "0 0 8 * * ?") // Run daily at 8 AM
    public void notifyCohortEndDates() {
        OffsetDateTime today = OffsetDateTime.now(ZoneId.systemDefault());

        List<Cohort> cohorts = cohortRepository.findAll();

        for (Cohort cohort : cohorts) {
            if (cohort.getCohortEndDate() != null) {
                long daysToEnd = today.until(cohort.getCohortEndDate(), ChronoUnit.DAYS);

                if (daysToEnd == 15 || daysToEnd == 5) {
                    notifyOrganizationAdmin(cohort);
                    notifyUsersInCohort(cohort);
                }
            }
        }
    }

    private void notifyOrganizationAdmin(Cohort cohort) {
        Organization organization = cohort.getOrganization();
        if (organization != null) {
        	OffsetDateTime today = OffsetDateTime.now(ZoneId.systemDefault());
            long daysToEnd = today.until(cohort.getCohortEndDate(), ChronoUnit.DAYS);
        	
            String adminEmail = organization.getOrganizationAdminEmail();
            String subject = "Cohort End Date Reminder";
            String message = "Dear " + organization.getOrganizationAdminName() + ",\n\n" +
                             "This is a reminder that the cohort \"" + cohort.getCohortName() + 
                             "\" will end in " + daysToEnd + " days on " + cohort.getCohortEndDate() + ".\n\n" +
                             "Please ensure all related tasks are completed before this date.\n\n" +
                             "Best regards,\n"+
                             "The Mindfultalk Team";
            sendEmail(adminEmail, subject, message);
        }
    }

    private void notifyUsersInCohort(Cohort cohort) {
    	OffsetDateTime today = OffsetDateTime.now(ZoneId.systemDefault());
        long daysToEnd = today.until(cohort.getCohortEndDate(), ChronoUnit.DAYS);
        
        List<UserCohortMapping> userMappings = userCohortMappingRepository.findByCohort(cohort);

        for (UserCohortMapping mapping : userMappings) {
            User user = mapping.getUser();
            if (user != null) {
                String userEmail = user.getUserEmail(); // Assuming User entity has an email field.
                String subject = "Cohort End Date Reminder";
                String message = "Dear " + user.getUserName() + ",\n\n" +
                                 "This is a reminder that your cohort \"" + cohort.getCohortName() + 
                                 "\" will end in " + daysToEnd + " days on " + cohort.getCohortEndDate() + ".\n\n" +
                                 "Please ensure you complete all tasks before this date.\n\n" +
                                 "Best regards,\n" +
                                 "The Mindfultalk Team";

                sendEmail(userEmail, subject, message);
            }
        }
        
    }

    private void sendEmail(String to, String subject, String message) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(to);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);
        mailSender.send(mailMessage);
    }
}
