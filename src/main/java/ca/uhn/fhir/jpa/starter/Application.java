package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.batch2.jobs.config.Batch2JobsConfig;
import ca.uhn.fhir.jpa.batch2.JpaBatch2Config;
import ca.uhn.fhir.jpa.starter.annotations.OnEitherVersion;
import ca.uhn.fhir.jpa.starter.cdshooks.StarterCdsHooksConfig;
import ca.uhn.fhir.jpa.starter.cr.StarterCrDstu3Config;
import ca.uhn.fhir.jpa.starter.cr.StarterCrR4Config;
import ca.uhn.fhir.jpa.starter.mdm.MdmConfig;
import ca.uhn.fhir.jpa.subscription.channel.config.SubscriptionChannelConfig;
import ca.uhn.fhir.jpa.subscription.match.config.SubscriptionProcessorConfig;
import ca.uhn.fhir.jpa.subscription.match.config.WebsocketDispatcherConfig;
import ca.uhn.fhir.jpa.subscription.submit.config.SubscriptionSubmitterConfig;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.wellknown.WellKnownServlet;
import custom.helper.CommonHelper;
import custom.helper.HapiPropertiesConfig;
import custom.interceptor.*;
import custom.metadataex.CustomCapabilityStatementProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;

import java.io.IOException;

@ServletComponentScan(basePackageClasses = {RestfulServer.class})
@SpringBootApplication(exclude = {ElasticsearchRestClientAutoConfiguration.class, ThymeleafAutoConfiguration.class})
@ComponentScan(basePackages = {"ca.uhn.fhir.wellknown","ca.uhn.fhir.jpa.starter", "custom", "custom.multitenancy", "custom.helper"})
// "custom.helper", "custom", "custom.multitenancy"
@Import({
	StarterCrR4Config.class,
	StarterCrDstu3Config.class,
	StarterCdsHooksConfig.class,
	SubscriptionSubmitterConfig.class,
	SubscriptionProcessorConfig.class,
	SubscriptionChannelConfig.class,
	WebsocketDispatcherConfig.class,
	MdmConfig.class,
	JpaBatch2Config.class,
	Batch2JobsConfig.class
})


public class Application extends SpringBootServletInitializer {

	// public static String CAPABILITY_STATEMENT_FILE_NAME = "capabilitystatement.json";

	public static void main(String[] args) {

		SpringApplication.run(Application.class, args);

		// Server is now accessible at eg. http://localhost:8080/fhir/metadata
		// UI is now accessible at http://localhost:8080/
	}


	@Autowired
	AutowireCapableBeanFactory beanFactory;

	@Bean
	@Conditional(OnEitherVersion.class)
	public ServletRegistrationBean hapiServletRegistration(RestfulServer restfulServer) throws IOException {
		ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean();
		beanFactory.autowireBean(restfulServer);

		servletRegistrationBean.setServlet(restfulServer);
		servletRegistrationBean.addUrlMappings("/fhir/*");
		servletRegistrationBean.setLoadOnStartup(1);

		// Register CapabilityStatementEx
		// restfulServer.registerInterceptor(new CapabilityStatementEx());
		// Register CapabilityStatementEx

		// Below BulkInterceptor is from Inferno
		// restfulServer.registerInterceptor(new BulkInterceptor(restfulServer.getFhirContext()));

		//restfulServer.registerProvider(new BulkExportProvider());

		//CommonHelper.GetMoreConfigFromConfig();

		// Register AuthorizationInterceptorEx
		if (EnableAuthorizationInterceptor()){
			AuthorizationInterceptorEx authorizationInterceptor = new AuthorizationInterceptorEx();
			restfulServer.registerInterceptor(authorizationInterceptor);

			restfulServer.registerInterceptor(new GranularScopePostResponseInterceptor());
			//
			//ConsentInterceptor consentInterceptor = new ConsentInterceptor();
			//consentInterceptor.registerConsentService(new RuleFilteringConsentService(authorizationInterceptor));
			//restfulServer.registerInterceptor(consentInterceptor);
			//
		}
		// Register AuthorizationInterceptorEx


		// This will load custom capabilitystatement
		CustomCapabilityStatementProvider customCapabilityStatementProvider = new CustomCapabilityStatementProvider(restfulServer);
		restfulServer.setServerConformanceProvider(customCapabilityStatementProvider);
		// This will load custom capabilitystatement

		// Multitenancy
		restfulServer.registerInterceptor(new TenantIdentificationInterceptor());
		restfulServer.registerInterceptor(new TenantContextCleanupInterceptor());
		// Multitenancy

		return servletRegistrationBean;
	}

	private static boolean EnableAuthorizationInterceptor() {
		boolean returnValue = true;

		try
		{
			HapiPropertiesConfig hapiConfig = new HapiPropertiesConfig();
			String getSecurityValue = hapiConfig.getEnablesecurity();

			if (getSecurityValue.toLowerCase().equals("false")){
				returnValue = false;
			}
		}
		catch (Exception e){

		}

		return returnValue;
	}

	private static void LoadHapiPropertiesConfig()
	{
		HapiPropertiesConfig hapiConfig = new HapiPropertiesConfig();
		int kk = 0;
	}


	@Bean
	public ServletRegistrationBean<WellKnownServlet> wellKnownServlet() {
		// Register the servlet with the desired URL pattern
		ServletRegistrationBean<WellKnownServlet> bean = new ServletRegistrationBean<>(new WellKnownServlet(), "/fhir/.well-known/*");
		bean.setLoadOnStartup(1);
		return bean;
	}

}


