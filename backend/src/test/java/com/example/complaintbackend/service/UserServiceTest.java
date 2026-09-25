package com.example.complaintbackend.service;

import com.example.complaintbackend.dto.auth.UserResponse;
import com.example.complaintbackend.entity.Role;
import com.example.complaintbackend.entity.User;
import com.example.complaintbackend.exception.ResourceNotFoundException;
import com.example.complaintbackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User sampleUser;
    private User sampleAgent;

    @BeforeEach
    void setUp() {
        sampleUser = new User("citizen1", "citizen1@example.com", "pass", "John Doe", Role.ROLE_USER);
        sampleUser.setId(1L);
        sampleUser.setEnabled(true);

        sampleAgent = new User("agent_priya", "priya@example.com", "pass", "Priya Sharma", Role.ROLE_SUPPORT_AGENT);
        sampleAgent.setId(2L);
        sampleAgent.setEnabled(true);
    }

    @Test
    @DisplayName("getAllUsers() should return list of mapped UserResponses")
    void testGetAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(sampleUser, sampleAgent));

        List<UserResponse> result = userService.getAllUsers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUsername()).isEqualTo("citizen1");
        assertThat(result.get(1).getUsername()).isEqualTo("agent_priya");
        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getUserEntity() should return user when ID exists")
    void testGetUserEntity_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        User result = userService.getUserEntity(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("citizen1");
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getUserEntity() should throw ResourceNotFoundException when user does not exist")
    void testGetUserEntity_NotFound_ThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserEntity(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id : '999'");

        verify(userRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("getUserById() should return mapped UserResponse")
    void testGetUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        UserResponse result = userService.getUserById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getFullName()).isEqualTo("John Doe");
        assertThat(result.getRole()).isEqualTo(Role.ROLE_USER);
    }

    @Test
    @DisplayName("getSupportAgents() should query repository by ROLE_SUPPORT_AGENT")
    void testGetSupportAgents() {
        when(userRepository.findByRole(Role.ROLE_SUPPORT_AGENT)).thenReturn(List.of(sampleAgent));

        List<UserResponse> result = userService.getSupportAgents();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("agent_priya");
        assertThat(result.get(0).getRole()).isEqualTo(Role.ROLE_SUPPORT_AGENT);
        verify(userRepository, times(1)).findByRole(Role.ROLE_SUPPORT_AGENT);
    }

    @Test
    @DisplayName("updateUserRole() should change role, save entity and return updated UserResponse")
    void testUpdateUserRole() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse result = userService.updateUserRole(1L, Role.ROLE_ADMIN);

        assertThat(result.getRole()).isEqualTo(Role.ROLE_ADMIN);
        assertThat(sampleUser.getRole()).isEqualTo(Role.ROLE_ADMIN);
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    @DisplayName("updateUserStatus() should change enabled status, save entity and return updated UserResponse")
    void testUpdateUserStatus() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse result = userService.updateUserStatus(1L, false);

        assertThat(result.isEnabled()).isFalse();
        assertThat(sampleUser.isEnabled()).isFalse();
        verify(userRepository, times(1)).save(sampleUser);
    }
}
