package com.eventhub.auth_service.infrastructure.adapter.in.web;

import com.eventhub.auth_service.application.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalAuthorizationControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private InternalAuthorizationController controller;

    @Test
    void returnsNoContentWhenTokenAuthorizationIsCurrent() {
        when(authService.isAccessTokenCurrent("signed-token")).thenReturn(true);

        var response = controller.validateToken("Bearer signed-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(authService).isAccessTokenCurrent("signed-token");
    }

    @Test
    void rejectsTokenWhenAuthorizationIsStale() {
        when(authService.isAccessTokenCurrent("signed-token")).thenReturn(false);

        var response = controller.validateToken("Bearer signed-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(authService).isAccessTokenCurrent("signed-token");
    }

    @Test
    void rejectsMissingOrMalformedAuthorizationHeader() {
        assertThat(controller.validateToken(null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(controller.validateToken("Basic credentials").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(controller.validateToken("Bearer ").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(authService);
    }
}
