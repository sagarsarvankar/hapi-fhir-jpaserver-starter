package custom.multitenancy;

import custom.helper.CommonHelper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(
		HttpServletRequest requestDetails,
		HttpServletResponse responseDetails,
		FilterChain chain)
		throws ServletException, IOException {

		try {

			String tenantname = requestDetails.getHeader(CommonHelper.TENANT_HEADER_NAME);
			String tenantId = CommonHelper.GetTenantNameBasedOnHeader(tenantname);

			TenantContext.setCurrentTenant(tenantId);

			chain.doFilter(requestDetails,responseDetails);

		} finally {

			TenantContext.clear();
		}
	}
}
