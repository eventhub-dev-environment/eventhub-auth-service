package com.eventhub.auth_service.infrastructure.adapter.in.web;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayAccessFilterTest {

    private static final String GATEWAY_SECRET = "test-gateway-secret";

    @Test
    void blocksInternalAuthorizationRequestWithoutGatewaySecret() throws Exception {
        GatewayAccessFilter filter = configuredFilter();
        MockHttpServletRequest request = request(null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void allowsInternalAuthorizationRequestWithGatewaySecret() throws Exception {
        GatewayAccessFilter filter = configuredFilter();
        MockHttpServletRequest request = request(GATEWAY_SECRET);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        assertThat(chain.getRequest()).isSameAs(request);
    }

    private GatewayAccessFilter configuredFilter() {
        GatewayAccessFilter filter = new GatewayAccessFilter();
        ReflectionTestUtils.setField(filter, "expectedSecret", GATEWAY_SECRET);
        return filter;
    }

    private MockHttpServletRequest request(String secret) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/v1/authz/validate");
        if (secret != null) {
            request.addHeader("X-Gateway-Secret", secret);
        }
        return request;
    }
}
