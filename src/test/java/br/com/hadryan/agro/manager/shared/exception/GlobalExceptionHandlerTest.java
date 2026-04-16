package br.com.hadryan.agro.manager.shared.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exercises every branch in {@link GlobalExceptionHandler} through a test
 * controller. Each endpoint throws a specific exception so we can assert the
 * resulting status code and ProblemDetail body.
 */
@WebMvcTest(GlobalExceptionHandlerTest.TestController.class)
@ActiveProfiles("test")
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.TestController.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mvc;

    // Instantiated directly rather than autowired: @WebMvcTest slices don't
    // always expose the ObjectMapper bean, and we only need it here to
    // serialize a single tiny payload.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void notFound_returns404() throws Exception {
        mvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value(containsString("User")))
                .andExpect(jsonPath("$.instance").value("/test/not-found"))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()));
    }

    @Test
    void business_returns422() throws Exception {
        mvc.perform(get("/test/business"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Business Rule Violated"))
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void conflict_returns409() throws Exception {
        mvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    void bodyValidation_returns400WithFieldErrors() throws Exception {
        TestPayload invalid = new TestPayload("", "not-an-email");
        mvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").exists());
    }

    @Test
    void malformedBody_returns400() throws Exception {
        mvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Malformed Request"));
    }

    @Test
    void dataIntegrity_returns409() throws Exception {
        mvc.perform(get("/test/data-integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Data Integrity Violation"));
    }

    @Test
    void optimisticLock_returns409() throws Exception {
        mvc.perform(get("/test/optimistic-lock"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Concurrent Modification"));
    }

    @Test
    void unhandled_returns500WithoutLeakingMessage() throws Exception {
        mvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                // The client should NOT see the raw exception message.
                .andExpect(jsonPath("$.detail").value(
                        "An unexpected error occurred. Please try again later."));
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @org.springframework.web.bind.annotation.GetMapping("/not-found")
        void notFound() {
            throw NotFoundException.of("User", 42);
        }

        @org.springframework.web.bind.annotation.GetMapping("/business")
        void business() {
            throw new BusinessException("Crop is already closed");
        }

        @org.springframework.web.bind.annotation.GetMapping("/conflict")
        void conflict() {
            throw new ConflictException("Email already in use");
        }

        @PostMapping("/validate")
        void validate(@Valid @RequestBody TestPayload payload) {
            // reached only when validation passes — which the tests don't exercise
        }

        @org.springframework.web.bind.annotation.GetMapping("/data-integrity")
        void dataIntegrity() {
            throw new DataIntegrityViolationException("duplicate key");
        }

        @org.springframework.web.bind.annotation.GetMapping("/optimistic-lock")
        void optimisticLock() {
            throw new OptimisticLockingFailureException("stale");
        }

        @org.springframework.web.bind.annotation.GetMapping("/boom")
        void boom() {
            throw new IllegalStateException("sensitive internal detail");
        }
    }

    record TestPayload(@NotBlank String name, @Email String email) {
    }
}