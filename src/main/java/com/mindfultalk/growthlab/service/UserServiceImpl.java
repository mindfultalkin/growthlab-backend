package com.mindfultalk.growthlab.service;

import com.mindfultalk.growthlab.model.*;
import com.mindfultalk.growthlab.dto.*;
import com.mindfultalk.growthlab.repository.*;
import com.opencsv.CSVReader;

import jakarta.transaction.Transactional;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CohortService cohortService; 

    @Autowired
    private ProgramService programService; 

    @Autowired
    private UserCohortMappingRepository userCohortMappingRepository; 

    @Autowired
    private CohortProgramRepository cohortProgramRepository;
    
    @Autowired
    private UserCohortMappingService userCohortMappingService;
    
    @Autowired
    private CohortRepository cohortRepository;
    
    @Autowired
    private OrganizationRepository organizationRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    // The default password that every new user is assigned
    private final String DEFAULT_PASSWORD = "Welcome123";
    
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    
    @Override
    @Cacheable(value = "users", key = "'all_users'")
    public List<UserGetDTO> getAllUsers() {
        logger.info("Fetching all users from database - cache miss");
        return userRepository.findAll().stream()
                .map(this::convertToUserDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Cacheable(value = "user", key = "#userId")
    public Optional<User> findByUserId(String userId) {
        logger.info("Fetching user by ID from database - cache miss: {}", userId);
        return userRepository.findById(userId);
    }
    
    @Override
    @Cacheable(value = "userDto", key = "#userId")
    public Optional<UserGetDTO> getUserById(String userId) {
        logger.info("Fetching user DTO by ID from database - cache miss: {}", userId);
        Optional<User> userOpt = userRepository.findById(userId);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return Optional.of(convertToUserDTO(user));
        } else {
            return Optional.empty();
        }
    }
    
    @Override
    @Cacheable(value = "usersByOrg", key = "#organizationId")
    public List<UserGetDTO> getUsersByOrganizationId(String organizationId) {
        logger.info("Fetching users by organization ID from database - cache miss: {}", organizationId);
        return userRepository.findByOrganizationOrganizationId(organizationId).stream()
                .map(this::convertToUserDTO)
                .collect(Collectors.toList());
    }

    
 // New method to fetch user details based on selected program
    @Override
    @Cacheable(value = "userProgram", key = "#userId + '_' + #programId")
    public UserDTO getUserDetailsWithProgram(String userId, String programId) {
        logger.info("Fetching user details with program from database - cache miss: userId={}, programId={}", userId, programId);
        
        UserDTO userDTO = new UserDTO();
        User user = findByUserId(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Set basic user details
        userDTO.setUserId(user.getUserId());
        userDTO.setUserName(user.getUserName());
        userDTO.setUserEmail(user.getUserEmail());
        userDTO.setUserPhoneNumber(user.getUserPhoneNumber());
        userDTO.setUserAddress(user.getUserAddress());
        userDTO.setUserType(user.getUserType());
        userDTO.setStatus(user.getStatus());
        userDTO.setCreatedAt(user.getCreatedAt());
        userDTO.setDeactivatedAt(user.getDeactivatedAt());
        userDTO.setDeactivatedReason(user.getDeactivatedReason());
        
        // Set organization details in UserDTO
        userDTO.setOrganization(convertOrganizationToDTO(user.getOrganization()));
        
        // Fetch UserCohortMapping based on userId and programId
        Optional<UserCohortMapping> userCohortMappingOpt = userCohortMappingService.findByUserUserIdAndProgramId(userId, programId);
        
        if (userCohortMappingOpt.isPresent()) {
            UserCohortMapping userCohortMapping = userCohortMappingOpt.get();
            
            // Set cohort details in CohortDTO
            CohortDTO cohortDTO = new CohortDTO();
            cohortDTO.setCohortId(userCohortMapping.getCohort().getCohortId());
            cohortDTO.setCohortName(userCohortMapping.getCohort().getCohortName());
            cohortDTO.setCohortStartDate(userCohortMapping.getCohort().getCohortStartDate());
            cohortDTO.setCohortEndDate(userCohortMapping.getCohort().getCohortEndDate());
            userDTO.setCohort(cohortDTO);
            
            // Fetch the program from CohortProgramRepository
            Optional<CohortProgram> cohortProgramOpt = cohortProgramRepository.findByCohortCohortId(userCohortMapping.getCohort().getCohortId());

            if (cohortProgramOpt.isPresent()) {
                CohortProgram cohortProgram = cohortProgramOpt.get();

                // Set program details in ProgramDTO
                ProgramDTO programDTO = new ProgramDTO();
                programDTO.setProgramId(cohortProgram.getProgram().getProgramId());
                programDTO.setProgramName(cohortProgram.getProgram().getProgramName());
                programDTO.setProgramDesc(cohortProgram.getProgram().getProgramDesc());
                programDTO.setStagesCount(cohortProgram.getProgram().getStages());
                programDTO.setUnitCount(cohortProgram.getProgram().getUnitCount());
                userDTO.setProgram(programDTO);
            } else {
                throw new IllegalArgumentException("Program not found for cohortId: " + userCohortMapping.getCohort().getCohortId());
            }
        } else {
            throw new IllegalArgumentException("UserCohortMapping not found for userId: " + userId + " and programId: " + programId);
        }

        return userDTO;
    }

    private String sanitizeUserId(String userId) {
        return userId != null ? userId.replaceAll("\\s+", "") : null;
    }

    
    @Override
    @CacheEvict(value = {"users", "usersByOrg", "userCohorts", "cohortUsers"}, allEntries = true)
    @Caching(put = {
        @CachePut(value = "user", key = "#result.userId"),
        @CachePut(value = "userDto", key = "#result.userId")
    })
    public User createUser(UsercreateDTO userDTO) {
        User user = userDTO.getUser();
        String cohortId = userDTO.getCohortId();
        String plainPassword = DEFAULT_PASSWORD;

     // Sanitize userId
        user.setUserId(sanitizeUserId(user.getUserId()));
        
     // Validate userType
        try {
            UserType.fromString(user.getUserType()); // Throws exception if invalid
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid userType: " + user.getUserType() + ". Allowed values are 'learner' or 'mentor'.");
        }
        
        user.setUserPassword(passwordEncoder.encode(plainPassword));

        // Ensure UUID is always set
        if (user.getUuid() == null) {
            user.setUuid(UUID.randomUUID());
        }

        // Ensure status is set (default ACTIVE if null)
        if (user.getStatus() == null) {
            user.setStatus("ACTIVE");
        }

        // Ensure createdBy is set (default → "admin" if missing)
        if (user.getCreatedBy() == null || user.getCreatedBy().isEmpty()) {
            user.setCreatedBy("admin"); 
        }
        
        User savedUser = userRepository.save(user);

           // Handle UserCohortMapping creation
        Cohort cohort = cohortRepository.findById(cohortId) 
                .orElseThrow(() -> new IllegalArgumentException("Cohort not found with ID: " + cohortId));

        UserCohortMapping userCohortMapping = new UserCohortMapping();
        userCohortMapping.setUser(savedUser);
        userCohortMapping.setCohort(cohort);
        userCohortMapping.setLeaderboardScore(0); 
        userCohortMapping.setUuid(UUID.randomUUID());

        // Save the UserCohortMapping to the repository
        userCohortMappingRepository.save(userCohortMapping);
        
     // Fetch program details from CohortProgram
        List<String> programNames = new ArrayList<>();
        List<String> cohortNames = new ArrayList<>();
        cohortProgramRepository.findByCohortCohortId(cohort.getCohortId())
                .ifPresent(cohortProgram -> {
                    programNames.add(cohortProgram.getProgram().getProgramName());
                    cohortNames.add(cohort.getCohortName());
                });

     // If the email is present, send credentials to the user
        if (user.getUserEmail() != null && !user.getUserEmail().isEmpty()) {
            sendWelcomeEmail(savedUser, plainPassword, programNames, cohortNames);
        }

        return savedUser;
    }
   
    @Override
    @CacheEvict(value = {"users", "usersByOrg", "userCohorts", "cohortUsers"}, allEntries = true)
    public Map<String, Object> parseAndCreateUsersFromCsv(CSVReader csvReader, List<String> errorMessages, List<String> warnings) {
        List<User> usersToCreate = new ArrayList<>();
        List<UserCohortMapping> userCohortMappingsToCreate = new ArrayList<>();
        Map<String, User> createdUsers = new HashMap<>();
        Set<String> userIdSet = new HashSet<>();
        String[] headerRow;
        String[] line;
        int userCreatedCount = 0;
        int userCohortMappingCreatedCount = 0;

        try {
            // Read header row to map columns dynamically
            headerRow = csvReader.readNext();
            if (headerRow == null) {
                throw new IllegalArgumentException("CSV file is empty or missing header row.");
            }

            // Map column indices to field names
            Map<String, Integer> columnIndexMap = new HashMap<>();
            for (int i = 0; i < headerRow.length; i++) {
                columnIndexMap.put(headerRow[i].trim().toLowerCase(), i);
            }

            // Check for required columns
            List<String> requiredColumns = List.of("userid", "username", "usertype", "organizationid", "cohortid" );
            for (String col : requiredColumns) {
                if (!columnIndexMap.containsKey(col.toLowerCase())) {
                    errorMessages.add("Missing required column: " + col);
                    return Map.of("createdUserCount", userCreatedCount, "createdUserCohortMappingCount", userCohortMappingCreatedCount, "errorCount", errorMessages.size(), "warningCount", warnings.size());
                }
            }

            while ((line = csvReader.readNext()) != null) {
            	String userId = sanitizeUserId(line[columnIndexMap.get("userid")]);
            	String cohortId = line[columnIndexMap.get("cohortid")];

            	// Check if userId exists in the same batch
                if (userIdSet.contains(userId)) {
                	warnings.add("Duplicate userId " + userId + " found in CSV. This user will not be created.");
                    continue; // Skip processing this line
                }
                userIdSet.add(userId);

                // Check if userId already exists in the database
                if (userRepository.existsById(userId)) {
                	warnings.add("UserID: " + userId + " already exists in the database. Skipping.");
                    continue; // Skip processing this line
                }
                
            	
            	// Check if userId exists in the same batch or database
            	try {
                User user = createdUsers.get(userId);
                if (user == null)  {
                    user = new User();
                    user.setUserId(userId);
                    user.setUserName(line[columnIndexMap.get("username")]);
                 // Validate userType
                    String userType = line[columnIndexMap.get("usertype")].toLowerCase();
                    if (!userType.equals("learner") && !userType.equals("mentor")) {
                        errorMessages.add("Invalid userType for UserID " + userId + ": " + userType + ". Allowed values are 'learner' or 'mentor'.");
                        continue;
                    }
                    user.setUserType(userType);
                    user.setUuid(UUID.randomUUID());
                    user.setUserPassword(passwordEncoder.encode(DEFAULT_PASSWORD));

                 // CreatedBy (optional column)
                    String createdBy = columnIndexMap.containsKey("createdby") ? line[columnIndexMap.get("createdby")] : null;
                    user.setCreatedBy((createdBy != null && !createdBy.isEmpty()) ? createdBy : "admin");

                    // Date of Birth (optional column)
                    if (columnIndexMap.containsKey("userdateofbirth")) {
                        String dobStr = line[columnIndexMap.get("userdateofbirth")];
                        if (dobStr != null && !dobStr.isEmpty()) {
                            try {
                                user.setUserDateOfBirth(OffsetDateTime.parse(dobStr));
                            } catch (Exception e) {
                                warnings.add("Invalid date format for DOB of UserID " + userId + ": " + dobStr);
                            }
                        }
                    }

                    // Status (optional column, default ACTIVE)
                    String status = columnIndexMap.containsKey("status") ? line[columnIndexMap.get("status")] : "ACTIVE";
                    user.setStatus((status != null && !status.isEmpty()) ? status : "ACTIVE");

                    // Deactivation details (if provided)
                    if ("DISABLED".equalsIgnoreCase(user.getStatus())) {
                        String reason = columnIndexMap.containsKey("deactivatedreason") ? line[columnIndexMap.get("deactivatedreason")] : "No reason specified";
                        user.setDeactivatedReason(reason);
                        user.setDeactivatedAt(OffsetDateTime.now());
                    }
                    
                    String userEmail = columnIndexMap.containsKey("useremail") ? line[columnIndexMap.get("useremail")] : null;
                 // Validate only non-empty emails
                    if (userEmail != null && !userEmail.isEmpty() && !isValidEmail(userEmail)) {
                    	warnings.add("Invalid email for UserID " + userId + ": " + userEmail);
                        userEmail = null; // Reset invalid email to null
                    }
                    user.setUserEmail(userEmail);

                    String userPhone = columnIndexMap.containsKey("userphonenumber") ? line[columnIndexMap.get("userphonenumber")] : null;
                    if (userPhone != null && !userPhone.isEmpty() && !isValidPhoneNumber(userPhone)) {
                    	warnings.add("Invalid phone number for UserID " + userId + ": " + userPhone);
                        userPhone = null; // Reset invalid phone number to null
                    }
                    user.setUserPhoneNumber(userPhone);
                    user.setUserAddress(columnIndexMap.containsKey("useraddress") ? line[columnIndexMap.get("useraddress")] : null);

                
                    // Fetch organization by ID
                    String organizationId = line[columnIndexMap.get("organizationid")];
                    Organization organization = organizationRepository.findById(organizationId)
                            .orElseThrow(() -> new IllegalArgumentException("Organization not found with ID: " + organizationId ));
                    user.setOrganization(organization);
                    
                // Add user to the list for bulk saving
                usersToCreate.add(user);
                createdUsers.put(userId, user);
                userCreatedCount++;
                }
            } catch (Exception ex) {
                errorMessages.add("Error creating UserID " + userId + ": " + ex.getMessage());
                continue;
            }
    

                try {
                    Cohort cohort = cohortRepository.findById(cohortId)
                    		.orElseThrow(() -> new IllegalArgumentException("Cohort not found with ID: " + cohortId));

             // Check for existing UserCohortMapping before creating a new one
                if (userCohortMappingRepository.existsByUser_UserIdAndCohort_CohortId(userId, cohortId)) {
                    errorMessages.add("UserID " + userId + " is already mapped to CohortID " + cohortId + ". Skipping this mapping.");
                    continue;
                }
                
                // Create the UserCohortMapping
                User user = createdUsers.get(userId);
                UserCohortMapping userCohortMapping = new UserCohortMapping();
                userCohortMapping.setUser(user);
                userCohortMapping.setCohort(cohort);
                userCohortMapping.setLeaderboardScore(0);
                userCohortMapping.setUuid(UUID.randomUUID());

                // Add to the list of mappings to be saved later
                userCohortMappingsToCreate.add(userCohortMapping);
                userCohortMappingCreatedCount++;
            }catch (Exception ex) {
                errorMessages.add("Error mapping UserID " + userId + " to CohortID " + cohortId + ": " + ex.getMessage());
            }
        }

        // Save all new users that don't already exist
        List<User> savedUsers = userRepository.saveAll(usersToCreate);

            // Save the user-cohort mappings
            userCohortMappingRepository.saveAll(userCohortMappingsToCreate);

         
         // Send welcome email for each new user
            for (User savedUser : savedUsers) {
                if (savedUser.getUserEmail() != null && !savedUser.getUserEmail().isEmpty()) {
                    // Collect all assigned cohorts and programs for the user
                    List<UserCohortMapping> userCohortMappings = userCohortMappingRepository.findAllByUserUserId(savedUser.getUserId());
                    List<String> programNames = new ArrayList<>();
                    List<String> cohortNames = new ArrayList<>();
                    
                    for (UserCohortMapping mapping : userCohortMappings) {
                        Cohort cohort = mapping.getCohort();
                        cohortNames.add(cohort.getCohortName());

                        // Fetch program details from CohortProgram
                        cohortProgramRepository.findByCohortCohortId(cohort.getCohortId()).ifPresent(cohortProgram -> 
                            programNames.add(cohortProgram.getProgram().getProgramName())
                        );
                    }

                    sendWelcomeEmail(savedUser, DEFAULT_PASSWORD, programNames, cohortNames);
                }
            }
            return Map.of(
                    "createdUserCount", userCreatedCount,
                    "createdUserCohortMappingCount", userCohortMappingCreatedCount,
                    "errorCount", errorMessages.size(),
                    "warningCount", warnings.size(),
                    "errors", errorMessages,
                    "warnings", warnings
            );
        } catch (Exception e) {
            throw new RuntimeException("Error parsing CSV: " + e.getMessage(), e);
        }
    }

    // Helper function to send welcome email
    private void sendWelcomeEmail(User user, String plainPassword, List<String> programNames, List<String> cohortNames) {
    	try {
            Organization organization = user.getOrganization();
            emailService.sendUserCreationEmail(
                    user.getUserEmail(),
                    user.getUserName(),
                    user.getUserId(),
                    plainPassword,
                    programNames,
                    cohortNames,
                    organization.getOrganizationAdminEmail(),
                    organization.getOrganizationName(),
                    user.getUserType()
            );
        } catch (Exception e) {
            System.err.println("Failed to send welcome email for user: " + user.getUserId() + ", error: " + e.getMessage());
        }
    }
           
    private boolean isValidEmail(String email) {
    	// Return true for null or empty strings
        if (email == null || email.isEmpty()) {
            return true; // No error for empty email
        }
        return email != null && email.matches("^(?!.*\\.\\.)[\\w._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
    	if (phoneNumber == null || phoneNumber.isEmpty()) {
            return true; // No error for empty phoneNumber
        }
        return phoneNumber != null && phoneNumber.matches("^[6-9]\\d{9}$");
    }

    @Override
    @Cacheable(value = "userCohorts", key = "#userId + '_cohortId'")
    public String getCohortIdByUserId(String userId) {
        return userCohortMappingRepository.findByUserUserId(userId)
                .map(userCohortMapping -> userCohortMapping.getCohort().getCohortId())
                .orElseThrow(() -> new IllegalArgumentException("Cohort not found for userId: " + userId));
    }
    
    @Override
    @Cacheable(value = "userCohorts", key = "#userId + '_cohortIds'")
    public List<String> getCohortsByUserId(String userId) {
        List<UserCohortMapping> userCohortMappings = userCohortMappingRepository.findAllByUserUserId(userId);
        if (userCohortMappings.isEmpty()) {
            throw new IllegalArgumentException("No cohorts found for userId: " + userId);
        }
        return userCohortMappings.stream()
                .map(mapping -> mapping.getCohort().getCohortId())
                .collect(Collectors.toList());
    }

    
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "user", key = "#userId"),
        @CacheEvict(value = "userDto", key = "#userId"),
        @CacheEvict(value = "users", key = "'all_users'"),
        @CacheEvict(value = "usersByOrg", key = "#result.organization.organizationId")
    })
    public User updateUser(String userId, User updatedUser) {
        logger.info("Updating user with ID: {}", userId);
        return userRepository.findById(userId)
            .map(user -> {
                // Prevent updates to immutable fields
                if (updatedUser.getUserId() != null && !updatedUser.getUserId().equals(userId)) {
                    logger.error("Attempt to modify userId was ignored");
                    // Ignore the attempt to modify userId
                }
                
                if (updatedUser.getCreatedAt() != null && 
                        !updatedUser.getCreatedAt().equals(user.getCreatedAt())) {
                    logger.warn("Attempt to modify createdAt timestamp was ignored");
                    // Ignore the attempt to modify createdAt
                }
                
                if (updatedUser.getUuid() != null && 
                        !updatedUser.getUuid().equals(user.getUuid())) {
                    logger.warn("Attempt to modify UUID was ignored");
                    // Ignore the attempt to modify UUID
                }
                
                // Validate userType
                String userType = updatedUser.getUserType();
                if (userType != null && !"Mentor".equals(userType) && !"Learner".equals(userType)) {
                    logger.error("Invalid userType provided: {}", userType);
                    throw new IllegalArgumentException("Invalid userType. Only 'Mentor' or 'Learner' are allowed.");
                }

                // Handle user status changes
                if (updatedUser.getStatus() != null) {
                    String currentStatus = user.getStatus();
                    String newStatus = updatedUser.getStatus().toUpperCase();

                    // Validate status is one of the allowed values
                    if (!newStatus.equals("ACTIVE") && !newStatus.equals("DISABLED")) {
                        logger.error("Invalid status value: {}. Only ACTIVE or DISABLED are allowed.", newStatus);
                        throw new IllegalArgumentException("Invalid status value. Only ACTIVE or DISABLED are allowed.");
                    }
                    
                    // Handle deactivation
                    if ("ACTIVE".equals(currentStatus) && "DISABLED".equals(newStatus)) {
                        logger.info("Deactivating user: {}", userId);
                        if (updatedUser.getDeactivatedReason() == null || updatedUser.getDeactivatedReason().trim().isEmpty()) {
                            logger.error("Deactivation reason is required");
                            throw new IllegalArgumentException("Deactivation reason is required");
                        }
                        
                        // Use the disable method to set status, deactivatedAt, and reason 
                        user.disable(updatedUser.getDeactivatedReason());
                        
                        // Deactivate all cohort mappings
                        int deactivatedMappings = 0;
                        for (UserCohortMapping mapping : user.getUserCohortMappings()) {
                            if (mapping.isActive()) {
                                mapping.disable("User account deactivated: " + updatedUser.getDeactivatedReason());
                                userCohortMappingRepository.save(mapping);
                                deactivatedMappings++;
                            }
                        }
                        
                        logger.info("Deactivated {} cohort mappings for user '{}' (ID: {})", 
                                deactivatedMappings, user.getUserName(), userId);
                    }
                    // Handle reactivation
                    else if ("DISABLED".equals(currentStatus) && "ACTIVE".equals(newStatus)) {
                        logger.info("Reactivating user: {}", userId);
                        user.setStatus("ACTIVE");
                        user.setDeactivatedAt(null);
                        user.setDeactivatedReason(null);
                        
                        // NOTE: This does not automatically reactivate the user in any cohorts
                        // as per the logic in reactivateUser method
                        logger.info("User reactivated, but cohort mappings were not automatically reactivated");
                    }
                }

                // Update other user fields
                if (updatedUser.getUserAddress() != null) user.setUserAddress(updatedUser.getUserAddress());
                if (updatedUser.getUserEmail() != null) user.setUserEmail(updatedUser.getUserEmail());
                if (updatedUser.getUserName() != null) user.setUserName(updatedUser.getUserName());
                if (updatedUser.getUserPhoneNumber() != null) user.setUserPhoneNumber(updatedUser.getUserPhoneNumber());
                if (updatedUser.getUserType() != null) user.setUserType(userType);

                // Check if the password is being updated
                if (updatedUser.getUserPassword() != null && !updatedUser.getUserPassword().isEmpty()) {
                    logger.info("Updating password for user: {}", userId);
                    user.setUserPassword(passwordEncoder.encode(updatedUser.getUserPassword()));
                }

                // Organization should not be updateable
                if (updatedUser.getOrganization() != null && 
                        !updatedUser.getOrganization().getOrganizationId().equals(user.getOrganization().getOrganizationId())) {
                    logger.warn("Attempt to modify organization was ignored");
                    // Ignore attempt to modify organization
                }

                logger.info("User updated successfully: {}", userId);
                return userRepository.save(user);
            })
            .orElseThrow(() -> {
                logger.error("User not found with ID: {}", userId);
                return new IllegalArgumentException("User not found");
            });
    }
    
    
    @Override
    @Caching(evict = {
        @CacheEvict(value = "user", key = "#userId"),
        @CacheEvict(value = "userDto", key = "#userId"),
        @CacheEvict(value = "users", key = "'all_users'")
    })
    public String deleteUser(String userId) {
        // First, retrieve the user to get their details before deletion
        Optional<User> userOpt = userRepository.findById(userId);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            userRepository.deleteById(userId);  // Delete the user
            
            // Return a message with the user's name and ID
            return "User '" + user.getUserName() + "' with ID: " + user.getUserId() + " has been deleted.";
        } else {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
    }

    
    @Override
    @CacheEvict(value = {"user", "userDto", "users"}, allEntries = true)
    public String deleteUsers(List<String> userIds) {
        List<User> deletedUsers = new ArrayList<>();
        for (String userId : userIds) {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                deletedUsers.add(user);
                userRepository.deleteById(userId);
            }
        }
        
        int deletedCount = deletedUsers.size();
        
        if (deletedCount == 1) {
            User deletedUser = deletedUsers.get(0);
            return "User '" + deletedUser.getUserName() + "' with ID: " + deletedUser.getUserId() + " has been deleted.";
        } else if (deletedCount > 1) {
            StringBuilder message = new StringBuilder();
            message.append(deletedCount + " users have been deleted. The following users were deleted:\n");
            for (User deletedUser : deletedUsers) {
                message.append("User Name: " + deletedUser.getUserName() + ", User ID: " + deletedUser.getUserId() + "\n");
            }
            return message.toString();
        } else {
            return "No users were deleted.";
        }
    }

    
    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "user", key = "#userId"),
        @CacheEvict(value = "userDto", key = "#userId"),
        @CacheEvict(value = "users", key = "'all_users'")
    })
    public String deactivateUser(String userId) {
        logger.info("Attempting to deactivate user with ID: {}", userId);
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            logger.warn("User not found for deactivation with ID: {}", userId);
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }

        User user = userOpt.get();
        if (!user.isActive()) {
            logger.info("User '{}' (ID: {}) is already deactivated", user.getUserName(), userId);
            return "User '" + user.getUserName() + "' is already deactivated.";
        }

        // Deactivate user
        user.setStatus("DISABLED");
        user.setDeactivatedAt(OffsetDateTime.now());
        userRepository.save(user);
        
        logger.info("User '{}' (ID: {}) has been deactivated", user.getUserName(), userId);

     // Deactivate all cohort mappings
        int deactivatedMappings = 0;
        for (UserCohortMapping mapping : user.getUserCohortMappings()) {
            if (mapping.isActive()) {
                mapping.disable("User account deactivated");
                userCohortMappingRepository.save(mapping);
                deactivatedMappings++;
            }
        }
        
        logger.info("Deactivated {} cohort mappings for user '{}' (ID: {})", 
                deactivatedMappings, user.getUserName(), userId);

        return "User '" + user.getUserName() + "' with ID: " + user.getUserId() + " has been deactivated from all cohorts.";
    }

    @Override
    @Transactional
    @CacheEvict(value = {"userCohortMapping"}, key = "#userId + '_' + #cohortId")
    public String deactivateUserFromCohort(String userId, String cohortId) {
        logger.info("Attempting to deactivate user ID: {} from cohort ID: {}", userId, cohortId);
        
        Optional<UserCohortMapping> mappingOpt = userCohortMappingRepository
                .findByUser_UserIdAndCohort_CohortId(userId, cohortId);

        if (mappingOpt.isEmpty()) {
            logger.warn("User-cohort mapping not found for user ID: {} and cohort ID: {}", userId, cohortId);
            throw new IllegalArgumentException("User not found in specified cohort");
        }

        UserCohortMapping mapping = mappingOpt.get();
        if (!mapping.isActive()) {
            logger.info("User '{}' is already deactivated from cohort '{}'", 
                    mapping.getUser().getUserName(), mapping.getCohort().getCohortName());
            return "User is already deactivated from this cohort.";
        }

        mapping.disable("User deactivated from cohort");
        mapping.setDeactivatedAt(OffsetDateTime.now());
        userCohortMappingRepository.save(mapping);
        
        logger.info("User '{}' has been deactivated from cohort '{}'", 
                mapping.getUser().getUserName(), mapping.getCohort().getCohortName());

        return "User '" + mapping.getUser().getUserName() + "' has been deactivated from cohort '"
                + mapping.getCohort().getCohortName() + "'";
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "user", key = "#userId"),
        @CacheEvict(value = "userDto", key = "#userId"),
        @CacheEvict(value = "users", key = "'all_users'")
    })
    public String reactivateUser(String userId) {
        logger.info("Attempting to reactivate user with ID: {}", userId);
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            logger.warn("User not found for reactivation with ID: {}", userId);
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }

        User user = userOpt.get();
        if (user.isActive()) {
            logger.info("User '{}' (ID: {}) is already active", user.getUserName(), userId);
            return "User '" + user.getUserName() + "' is already active.";
        }

        user.setStatus("ACTIVE");
        user.setDeactivatedAt(null);
        user.setDeactivatedReason(null);
        userRepository.save(user);
        
        logger.info("User '{}' (ID: {}) has been reactivated", user.getUserName(), userId);

        return "User '" + user.getUserName() + "' with ID: " + user.getUserId() + " has been reactivated. Note: This does not automatically reactivate the user in any cohorts.";
    }

    @Override
    @Transactional
    @CachePut(value = "userCohortMapping", key = "#userId + '_' + #cohortId")
    public String reactivateUserInCohort(String userId, String cohortId) {
        logger.info("Attempting to reactivate user ID: {} in cohort ID: {}", userId, cohortId);
        
        // First, check if the user is active
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            logger.warn("User not found for cohort reactivation with ID: {}", userId);
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        
        User user = userOpt.get();
        if (!user.isActive()) {
            logger.warn("Cannot reactivate in cohort - user '{}' (ID: {}) is currently deactivated", 
                    user.getUserName(), userId);
            throw new IllegalArgumentException("User account is deactivated. Please reactivate the user account first.");
        }
        
        Optional<UserCohortMapping> mappingOpt = userCohortMappingRepository
                .findByUser_UserIdAndCohort_CohortId(userId, cohortId);

        if (mappingOpt.isEmpty()) {
            logger.warn("User-cohort mapping not found for user ID: {} and cohort ID: {}", userId, cohortId);
            throw new IllegalArgumentException("User not found in specified cohort");
        }

        UserCohortMapping mapping = mappingOpt.get();
        if (mapping.isActive()) {
            logger.info("User '{}' is already active in cohort '{}'", 
                    mapping.getUser().getUserName(), mapping.getCohort().getCohortName());
            return "User is already active in this cohort.";
        }

        mapping.setStatus("ACTIVE");
        mapping.setDeactivatedAt(null);
        mapping.setDeactivatedReason(null);
        userCohortMappingRepository.save(mapping);
        
        logger.info("User '{}' has been reactivated in cohort '{}'", 
                mapping.getUser().getUserName(), mapping.getCohort().getCohortName());

        return "User '" + mapping.getUser().getUserName() + "' has been reactivated in cohort '"
                + mapping.getCohort().getCohortName() + "'";
    }
 // Helper method to check if a user is active in any cohort
    @Cacheable(value = "userActiveStatus", key = "#userId")
    public boolean isUserActiveInAnyCohort(String userId) {
        logger.debug("Checking if user ID: {} is active in any cohort", userId);
        return userCohortMappingRepository.existsByUserUserIdAndStatusEquals(userId, "ACTIVE");
    }
    
    @Override
    @CacheEvict(value = "user", key = "#userId")
    public boolean resetPassword(String userId, String newPassword) {
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String encodedPassword = passwordEncoder.encode(newPassword);
            user.setUserPassword(encodedPassword);  
            userRepository.save(user);  
            return true;
        } else {
            return false;  
        }
    }
    
    public boolean verifyPassword(String plainPassword, String encodedPassword) {
        return passwordEncoder.matches(plainPassword, encodedPassword);
    }

    
    @Override
    @Cacheable(value = "userProgram", key = "#userId + '_default'")
    public UserDTO getUserDetailsWithProgram(String userId) {
        logger.info("Fetching user details with default program from database - cache miss: {}", userId);
        
        // Fetch the UserCohortMapping
        UserCohortMapping userCohortMapping = userCohortMappingRepository.findByUserUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("UserCohortMapping not found"));

        // Fetch the CohortProgram based on cohortId from UserCohortMapping
        CohortProgram cohortProgram = cohortProgramRepository.findByCohortCohortId(userCohortMapping.getCohort().getCohortId())
                .orElseThrow(() -> new IllegalArgumentException("CohortProgram not found"));

        // Convert User, Cohort, and Program to DTO
        UserDTO userDTO = convertToDTO(userCohortMapping.getUser());
        CohortDTO cohortDTO = cohortService.convertToDTO(userCohortMapping.getCohort());
        ProgramDTO programDTO = programService.convertToDTO(cohortProgram.getProgram());

        // Set cohort and program in UserDTO
        userDTO.setCohort(cohortDTO);
        userDTO.setProgram(programDTO);

        return userDTO;
    }
    

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setUserId(user.getUserId());
        dto.setUserAddress(user.getUserAddress());
        dto.setUserEmail(user.getUserEmail());
        dto.setUserName(user.getUserName());
        dto.setUserPhoneNumber(user.getUserPhoneNumber());
        dto.setUserType(user.getUserType());
        dto.setOrganization(convertOrganizationToDTO(user.getOrganization()));

        return dto;
    }
    @Cacheable(value = "organizationDto", key = "#organization.organizationId")
    private OrganizationDTO convertOrganizationToDTO(Organization organization) {
        OrganizationDTO dto = new OrganizationDTO();
        dto.setOrganizationId(organization.getOrganizationId());
        dto.setOrganizationName(organization.getOrganizationName());
        dto.setOrganizationAdminName(organization.getOrganizationAdminName());
        dto.setOrganizationAdminEmail(organization.getOrganizationAdminEmail());
        dto.setOrganizationAdminPhone(organization.getOrganizationAdminPhone());
        return dto;
    }
    
    private UserGetDTO convertToUserDTO(User user) {
        UserGetDTO dto = new UserGetDTO();
        dto.setUserId(user.getUserId());
        dto.setUserAddress(user.getUserAddress());
        dto.setUserEmail(user.getUserEmail());
        dto.setUserName(user.getUserName());
        dto.setUserPhoneNumber(user.getUserPhoneNumber());
        dto.setUserType(user.getUserType());
        dto.setStatus(user.getStatus());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setDeactivatedAt(user.getDeactivatedAt());
        dto.setDeactivatedReason(user.getDeactivatedReason());
        
        // Set organization
        if (user.getOrganization() != null) {
            dto.setOrganization(convertOrganizationToDTO(user.getOrganization()));
        }

        // Get all UserCohortMappings for this user
        List<UserCohortMapping> userCohortMappings = userCohortMappingRepository.findAllByUserUserId(user.getUserId());
        
        if (!userCohortMappings.isEmpty()) {
            // Get the active or most recent cohort mapping
            // You might want to add a status field to UserCohortMapping to track active/inactive
            UserCohortMapping primaryMapping = userCohortMappings.get(0);
            
            // Set primary cohort
            Cohort primaryCohort = primaryMapping.getCohort();
            if (primaryCohort != null) {
                CohortDTO cohortDTO = cohortService.convertToDTO(primaryCohort);
                dto.setCohort(cohortDTO);

                // Get program for primary cohort
                Optional<CohortProgram> cohortProgramOpt = cohortProgramRepository
                    .findByCohortCohortId(primaryCohort.getCohortId());
                
                if (cohortProgramOpt.isPresent()) {
                    ProgramDTO programDTO = programService.convertToDTO(cohortProgramOpt.get().getProgram());
                    dto.setProgram(programDTO);
                }
            }

            // Add all cohorts and their programs
            List<CohortDTO> allCohorts = new ArrayList<>();
            Set<ProgramDTO> allPrograms = new HashSet<>();

            
            for (UserCohortMapping mapping : userCohortMappings) {
                Cohort cohort = mapping.getCohort();
                if (cohort != null) {
                    CohortDTO cohortDTO = cohortService.convertToDTO(cohort);
                    allCohorts.add(cohortDTO);

                    // Get program for this cohort
                    Optional<CohortProgram> cohortProgramOpt = cohortProgramRepository
                        .findByCohortCohortId(cohort.getCohortId());
                    
                    if (cohortProgramOpt.isPresent()) {
                        ProgramDTO programDTO = programService.convertToDTO(cohortProgramOpt.get().getProgram());
                        allPrograms.add(programDTO);
                    }
                }
            }
            
            dto.setAllCohorts(allCohorts);
            dto.setAllPrograms(new ArrayList<>(allPrograms));
        }

        return dto;
    }
    @Override
    @Cacheable(value = "userDetailsWithCohorts", key = "#userId")
    public UserDetailsWithCohortsAndProgramsDTO getUserDetailsWithCohortsAndPrograms(String userId) {
        logger.info("Fetching user details with cohorts and programs from database - cache miss: {}", userId);
         // Fetch the user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // Convert user to DTO
        UserDetailsWithCohortsAndProgramsDTO userDetailsDTO = new UserDetailsWithCohortsAndProgramsDTO();
        userDetailsDTO.setUserId(user.getUserId());
        userDetailsDTO.setUserName(user.getUserName());
        userDetailsDTO.setUserEmail(user.getUserEmail());
        userDetailsDTO.setUserPhoneNumber(user.getUserPhoneNumber());
        userDetailsDTO.setUserAddress(user.getUserAddress());
        userDetailsDTO.setUserType(user.getUserType());
        userDetailsDTO.setCreatedAt(user.getCreatedAt());
        userDetailsDTO.setStatus(user.getStatus());
        userDetailsDTO.setDeactivatedAt(user.getDeactivatedAt());
        userDetailsDTO.setDeactivatedReason(user.getDeactivatedReason());
        userDetailsDTO.setOrganization(convertOrganizationToDTO(user.getOrganization()));

        // Fetch all cohorts and their programs for the user
        List<UserCohortMapping> userCohortMappings = userCohortMappingRepository.findAllByUserUserId(userId);
        List<CohortProgramDTO> cohortProgramDTOs = new ArrayList<>();

        for (UserCohortMapping userCohortMapping : userCohortMappings) {
            Cohort cohort = userCohortMapping.getCohort();
            Optional<CohortProgram> cohortProgramOpt = cohortProgramRepository.findByCohortCohortId(cohort.getCohortId());

            if (cohortProgramOpt.isPresent()) {
                CohortProgram cohortProgram = cohortProgramOpt.get();
                CohortProgramDTO cohortProgramDTO = new CohortProgramDTO();
                cohortProgramDTO.setCohortId(cohort.getCohortId());
                cohortProgramDTO.setCohortName(cohort.getCohortName());
                cohortProgramDTO.setCohortStartDate(cohort.getCohortStartDate());
                cohortProgramDTO.setCohortEndDate(cohort.getCohortEndDate());

                ProgramDTO programDTO = new ProgramDTO();
                programDTO.setProgramId(cohortProgram.getProgram().getProgramId());
                programDTO.setProgramName(cohortProgram.getProgram().getProgramName());
                programDTO.setProgramDesc(cohortProgram.getProgram().getProgramDesc());
                programDTO.setStagesCount(cohortProgram.getProgram().getStages());
                programDTO.setUnitCount(cohortProgram.getProgram().getUnitCount());

                cohortProgramDTO.setProgram(programDTO);
                cohortProgramDTOs.add(cohortProgramDTO);
            }
        }

        userDetailsDTO.setAllCohortsWithPrograms(cohortProgramDTOs);
        return userDetailsDTO;
    }
}