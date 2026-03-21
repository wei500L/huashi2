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

public final class MockMvcTestSupport {

    private MockMvcTestSupport() {
    }

    public static MockMvc build(WebApplicationContext webApplicationContext) {
        List<Filter> servletFilters = new ArrayList<>(webApplicationContext.getBeansOfType(Filter.class).values());
        servletFilters.removeIf(MockMvcTestSupport::isManagedBySecurityChain);
        servletFilters.sort(AnnotationAwareOrderComparator.INSTANCE);
        return MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .addFilters(servletFilters.toArray(Filter[]::new))
                .build();
    }

    private static boolean isManagedBySecurityChain(Filter filter) {
        return filter instanceof FilterChainProxy
                || filter instanceof JwtAuthenticationFilter
                || filter instanceof InternalApiAuthenticationFilter;
    }
}
