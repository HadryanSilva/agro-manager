package br.com.hadryan.agro.manager.account.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.hadryan.agro.manager.account.domain.Account;
import br.com.hadryan.agro.manager.account.service.AccountService;
import br.com.hadryan.agro.manager.shared.exception.GlobalExceptionHandler;
import br.com.hadryan.agro.manager.shared.exception.NotFoundException;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Slice test for {@link AccountController}. The service is mocked so the
 * test focuses purely on the HTTP contract: status codes, payload shape,
 * Location header. Real service behaviour is covered in
 * {@code AccountServiceTest}.
 */
@WebMvcTest(AccountController.class)
@ActiveProfiles("test")
@Import(GlobalExceptionHandler.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AccountService service;

    /**
     * Builds an Account with the given id + name via reflection. Production
     * code goes through the {@code create()} factory, which doesn't let us
     * set an id (that's the DB's job). For HTTP-slice tests we need to
     * forge a "persisted" entity so we stuff the id in directly.
     */
    private Account newAccountWithId(Long id, String name) {
        Account account = Account.create(name);
        setField(account, "id", id);
        return account;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            // id lives on BaseEntity, so walk up the class hierarchy to find it.
            Class<?> type = target.getClass();
            while (type != null) {
                try {
                    Field f = type.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    f.set(target, value);
                    return;
                } catch (NoSuchFieldException ignored) {
                    type = type.getSuperclass();
                }
            }
            throw new IllegalStateException("Field not found: " + fieldName);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void create_returns201WithLocationHeader() throws Exception {
        Account created = newAccountWithId(1L, "Fazenda Santa Luzia");
        given(service.create(any())).willReturn(created);

        mvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Fazenda Santa Luzia"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/accounts/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Fazenda Santa Luzia"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void create_withBlankName_returns400() throws Exception {
        mvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists());
    }

    @Test
    void get_returns200() throws Exception {
        given(service.findById(1L)).willReturn(newAccountWithId(1L, "Acme"));

        mvc.perform(get("/api/accounts/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("Acme"));
    }

    @Test
    void get_missing_returns404() throws Exception {
        given(service.findById(999L)).willThrow(NotFoundException.of("Account", 999));

        mvc.perform(get("/api/accounts/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    void update_returns200() throws Exception {
        given(service.rename(any(), any())).willReturn(newAccountWithId(1L, "Renamed"));

        mvc.perform(put("/api/accounts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Renamed"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"));
    }

    @Test
    void suspend_returns200WithSuspendedStatus() throws Exception {
        Account suspended = newAccountWithId(1L, "Acme");
        suspended.suspend();
        given(service.suspend(1L)).willReturn(suspended);

        mvc.perform(post("/api/accounts/1/suspend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }
}