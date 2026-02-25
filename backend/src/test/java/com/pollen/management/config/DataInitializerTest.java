package com.pollen.management.config;

import com.pollen.management.entity.User;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedPassword");
    }

    @Test
    void shouldCreateAllFourDefaultAccountsWhenNoneExist() throws Exception {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        dataInitializer.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(4)).save(captor.capture());

        List<User> saved = captor.getAllValues();
        assertThat(saved).extracting(User::getUsername)
                .containsExactly("admin", "leader", "teacher", "intern");
        assertThat(saved).extracting(User::getRole)
                .containsExactly(Role.ADMIN, Role.LEADER, Role.VICE_LEADER, Role.INTERN);
        assertThat(saved).allMatch(u -> Boolean.TRUE.equals(u.getEnabled()));
        assertThat(saved).allMatch(u -> u.getPassword().startsWith("$2a$"));
    }

    @Test
    void shouldSkipCreationWhenAccountAlreadyExists() throws Exception {
        when(userRepository.existsByUsername("admin")).thenReturn(true);
        when(userRepository.existsByUsername("leader")).thenReturn(true);
        when(userRepository.existsByUsername("teacher")).thenReturn(false);
        when(userRepository.existsByUsername("intern")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        dataInitializer.run();

        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    void shouldNotCreateAnyAccountsWhenAllExist() throws Exception {
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        dataInitializer.run();

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldEncodePasswordWithPasswordEncoder() throws Exception {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        dataInitializer.run();

        verify(passwordEncoder, times(4)).encode("admin123");
    }
}
