package tn.esprit.authservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Extended Tests")
class EmailServiceExtendedTest {

    @Mock private JavaMailSender mailSender;
    @InjectMocks private EmailService emailService;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(
                emailService, "mailFrom", "noreply@wallstreetenglish.com");
    }

    @Nested
    @DisplayName("sendResetLink - content validation")
    class ContentValidationTests {

        @Test
        @DisplayName("Email subject contains expected text")
        void sendResetLink_SubjectContainsExpectedText() {
            doNothing().when(mailSender).send(any(SimpleMailMessage.class));
            emailService.sendResetLink("user@test.com", "http://localhost/reset?token=abc");

            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
            assertTrue(captor.getValue().getSubject().contains("Reset your Wall Street English password"));
        }

        @Test
        @DisplayName("Email body contains expiry notice")
        void sendResetLink_BodyContainsExpiryNotice() {
            doNothing().when(mailSender).send(any(SimpleMailMessage.class));
            emailService.sendResetLink("user@test.com", "http://localhost/reset?token=abc");

            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
            assertTrue(captor.getValue().getText().contains("24 hours"));
        }

        @Test
        @DisplayName("Email body contains reset link")
        void sendResetLink_BodyContainsResetLink() {
            doNothing().when(mailSender).send(any(SimpleMailMessage.class));
            String link = "http://localhost:4200/reset-password?token=xyz789";
            emailService.sendResetLink("user@test.com", link);

            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
            assertTrue(captor.getValue().getText().contains(link));
        }

        @Test
        @DisplayName("From address is always set")
        void sendResetLink_FromAddressIsSet() {
            doNothing().when(mailSender).send(any(SimpleMailMessage.class));
            emailService.sendResetLink("user@test.com", "http://localhost/reset");

            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
            assertNotNull(captor.getValue().getFrom());
            assertFalse(captor.getValue().getFrom().isEmpty());
        }

        @Test
        @DisplayName("To address matches the recipient argument")
        void sendResetLink_ToAddressMatchesRecipient() {
            doNothing().when(mailSender).send(any(SimpleMailMessage.class));
            emailService.sendResetLink("recipient@example.com", "http://link");

            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
            assertEquals("recipient@example.com", captor.getValue().getTo()[0]);
        }
    }

    @Nested
    @DisplayName("sendResetLink - exception handling")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("Should not throw on MailException")
        void sendResetLink_MailException_DoesNotPropagate() {
            doThrow(new org.springframework.mail.MailSendException("SMTP error"))
                    .when(mailSender).send(any(SimpleMailMessage.class));
            assertDoesNotThrow(() -> emailService.sendResetLink("user@test.com", "http://link"));
        }

        @Test
        @DisplayName("Should not throw on generic RuntimeException from sender")
        void sendResetLink_RuntimeException_DoesNotPropagate() {
            doThrow(new RuntimeException("Unexpected"))
                    .when(mailSender).send(any(SimpleMailMessage.class));
            assertDoesNotThrow(() -> emailService.sendResetLink("user@test.com", "http://link"));
        }

        @Test
        @DisplayName("mailSender.send is always called exactly once")
        void sendResetLink_AlwaysCallsSendOnce() {
            doNothing().when(mailSender).send(any(SimpleMailMessage.class));
            emailService.sendResetLink("a@b.com", "http://x");
            verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        }
    }
}