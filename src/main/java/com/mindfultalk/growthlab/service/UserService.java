package com.mindfultalk.growthlab.service;

import com.mindfultalk.growthlab.dto.*;
import com.mindfultalk.growthlab.model.*;
import com.opencsv.CSVReader;
import java.util.*;

public interface UserService {
    List<UserGetDTO> getAllUsers();
    Optional<User> findByUserId(String userId);
    Optional<UserGetDTO> getUserById(String userId);
    UserDTO getUserDetailsWithProgram(String userId, String programId);
    List<UserGetDTO> getUsersByOrganizationId(String organizationId);
    User createUser(UsercreateDTO userDTO);
    User updateUser(String userId, User user);
    String deleteUser(String userId);
    String deleteUsers(List<String> userIds);
	UserDTO getUserDetailsWithProgram(String userId);
	String getCohortIdByUserId(String userId);
	List<String> getCohortsByUserId(String userId);
	UserDetailsWithCohortsAndProgramsDTO getUserDetailsWithCohortsAndPrograms(String userId);
	//List<User> parseAndCreateUsersFromCsv(CSVReader csvReader, List<String> errorMessages);
	Map<String, Object> parseAndCreateUsersFromCsv(CSVReader csvReader, List<String> errorMessages, List<String> warnings);
    boolean verifyPassword(String plainPassword, String encodedPassword);
	boolean resetPassword(String userId, String newPassword);
    
	// new methods for active and deactivate users 
    String deactivateUser(String userId);
    String deactivateUserFromCohort(String userId, String cohortId);
    String reactivateUser(String userId);
    String reactivateUserInCohort(String userId, String cohortId);
}