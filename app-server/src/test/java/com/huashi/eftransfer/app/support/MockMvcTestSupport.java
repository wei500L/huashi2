package com.huashi.eftransfer.app.support;

import com.huashi.eftransfer.app.common.security.InternalApiAuthenticationFilter;
import com.huashi.eftransfer.app.common.security.JwtAuthenticationFilter;
import jakarta.servlet.Filter;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public final class MockMvcTestSupport {

    public static final String PUBLIC_ASSESSMENT_CSRF_HEADER = "X-Requested-With";
    public static final String PUBLIC_ASSESSMENT_CSRF_VALUE = "XMLHttpRequest";

    private MockMvcTestSupport() {
    }

    public static MockMvc build(WebApplicationContext webApplicationContext) {
        return builder(webApplicationContext)
                .defaultRequest(get("/").header(PUBLIC_ASSESSMENT_CSRF_HEADER, PUBLIC_ASSESSMENT_CSRF_VALUE))
                .build();
    }

    public static MockMvc buildWithoutDefaultHeaders(WebApplicationContext webApplicationContext) {
        return builder(webApplicationContext).build();
    }

    private static org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder builder(
            WebApplicationContext webApplicationContext
    ) {
        List<Filter> servletFilters = new ArrayList<>(webApplicationContext.getBeansOfType(Filter.class).values());
        servletFilters.removeIf(MockMvcTestSupport::isManagedBySecurityChain);
        servletFilters.sort(AnnotationAwareOrderComparator.INSTANCE);
        return MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .addFilters(servletFilters.toArray(Filter[]::new));
    }

    private static boolean isManagedBySecurityChain(Filter filter) {
        return filter instanceof FilterChainProxy
                || filter instanceof JwtAuthenticationFilter
                || filter instanceof InternalApiAuthenticationFilter;
    }
}
