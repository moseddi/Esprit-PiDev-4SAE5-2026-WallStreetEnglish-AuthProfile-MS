package tn.esprit.usermanagementservice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("UserManagementServiceApplication Tests")
class UserManagementServiceApplicationTests {

	@MockitoBean
	private RabbitTemplate rabbitTemplate;

	@Test
	@DisplayName("Should load application context successfully")
	void contextLoads() {
		assertThat(true).isTrue();
	}

	@Test
	@DisplayName("Main method should run without errors")
	void mainMethod_ShouldRun() {
		String[] args = {};
		assertDoesNotThrow(() -> UserManagementServiceApplication.main(args));
	}

	private void assertDoesNotThrow(Runnable runnable) {
		try {
			runnable.run();
		} catch (Exception e) {
			throw new AssertionError("Exception thrown: " + e.getMessage());
		}
	}
}