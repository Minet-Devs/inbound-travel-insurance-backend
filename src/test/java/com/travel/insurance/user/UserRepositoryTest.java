package com.travel.insurance.user;

import com.travel.insurance.common.crypto.EncryptedLocalDateConverter;
import com.travel.insurance.common.crypto.EncryptedStringConverter;
import com.travel.insurance.common.crypto.EnvEncryptionKeyProvider;
import com.travel.insurance.common.crypto.FieldEncryptionService;
import com.travel.insurance.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JpaAuditingConfig.class, EnvEncryptionKeyProvider.class, FieldEncryptionService.class,
        EncryptedStringConverter.class, EncryptedLocalDateConverter.class})
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User newUser(String email) {
        User user = new User();
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setEmail(email);
        user.setPassword("hashed");
        user.setRole(Role.ADMIN);
        return user;
    }

    @Test
    void savesAndFindsByEmail() {
        userRepository.saveAndFlush(newUser("jane@example.com"));

        Optional<User> found = userRepository.findByEmail("jane@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isNotNull();
        assertThat(found.get().getCreatedDate()).isNotNull();
        assertThat(found.get().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void softDeleteHidesRowFromQueries() {
        User user = userRepository.saveAndFlush(newUser("gone@example.com"));

        userRepository.delete(user);
        userRepository.flush();

        assertThat(userRepository.findByEmail("gone@example.com")).isEmpty();
        assertThat(userRepository.findById(user.getId())).isEmpty();
    }
}
