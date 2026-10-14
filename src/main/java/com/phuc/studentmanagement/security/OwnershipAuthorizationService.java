package com.phuc.studentmanagement.security;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.phuc.studentmanagement.model.Role;
import com.phuc.studentmanagement.repository.UserRepository;

@Service
public class OwnershipAuthorizationService {
    private final UserRepository userRepository;

    public OwnershipAuthorizationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isOwner(String subject, int studentId) {
        try {
            return isOwner(Integer.parseInt(subject), studentId);
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    public boolean isOwner(int userId, int studentId) {
        return userRepository.findById(userId)
                .filter(user -> user.isEnabled() && Objects.equals(user.getStudentId(), studentId)
                        && user.getRole() == Role.STUDENT)
                .isPresent();
    }
}