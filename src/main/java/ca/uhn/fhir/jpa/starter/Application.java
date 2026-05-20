package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.batch2.api.IJobCoordinator;
import ca.uhn.fhir.batch2.api.IJobMaintenanceService;
import ca.uhn.fhir.batch2.api.IJobPersistence;
import ca.uhn.fhir.batch2.jobs.config.Batch2JobsConfig;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.IInterceptorBroadcaster;
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
import custom.helper.CommonTaskScheduler;
import custom.helper.HapiPropertiesConfig;
import custom.interceptor.*;
import custom.metadataex.CustomCapabilityStatementProvider;
import custom.multitenancy.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.*;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.IOException;

@EnableAspectJAutoProxy
@EnableScheduling
@ServletComponentScan(basePackageClasses = {RestfulServer.class})
@SpringBootApplication(exclude = {ThymeleafAutoConfiguration.class, QuartzAutoConfiguration.class})
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

		// Server is now accessible at e.g. http://localhost:8080/fhir/metadata
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

		//To preprocess request
		IncomingRequestPreProcessInterceptor incomingRequestPreProcessInterceptor = new IncomingRequestPreProcessInterceptor();
		restfulServer.registerInterceptor(incomingRequestPreProcessInterceptor);
		//To preprocess request

		// This will load custom capabilitystatement
		CustomCapabilityStatementProvider customCapabilityStatementProvider = new CustomCapabilityStatementProvider(restfulServer);
		restfulServer.setServerConformanceProvider(customCapabilityStatementProvider);
		// This will load custom capabilitystatement

		// Multitenancy
		restfulServer.registerInterceptor(new TenantIdentificationInterceptor());

		// Removing temporarily
		//restfulServer.registerInterceptor(new TenantContextCleanupInterceptor());
		// Multitenancy

		// BulkExport
		//restfulServer.registerInterceptor(new CommonTaskScheduler());
		//IJobMaintenanceService jobMaintenanceService = beanFactory.getBean(IJobMaintenanceService.class);
		//restfulServer.registerInterceptor(new CommonTaskScheduler(jobMaintenanceService));

		//IJobMaintenanceService jobMaintenanceService = beanFactory.getBean(IJobMaintenanceService.class);
		//restfulServer.registerInterceptor(new BulkExportMaintenanceInterceptor(jobMaintenanceService));

		/*
		IJobMaintenanceService jobMaintenanceService = beanFactory.getBean(IJobMaintenanceService.class);
		IInterceptorBroadcaster interceptorBroadcaster = beanFactory.getBean(IInterceptorBroadcaster.class);
		FhirContext fhirContext = restfulServer.getFhirContext();
		*/

		/*
		restfulServer.registerInterceptor(new MultiTenantBulkExportInterceptor(
			jobMaintenanceService, interceptorBroadcaster, fhirContext));
		*/
		/*
		IJobCoordinator jobCoordinator = beanFactory.getBean(IJobCoordinator.class);
		IJobPersistence jobPersistence = beanFactory.getBean(IJobPersistence.class);

		restfulServer.registerInterceptor(new ComprehensiveMultiTenantJobProcessor(jobCoordinator,
			jobPersistence, interceptorBroadcaster, fhirContext));
		*/
		// BulkExport

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

	/*
	@Bean(name = "hapiBatch2TaskExecutor")
	public TaskExecutor hapiBatch2TaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(5);
		executor.setMaxPoolSize(10);

		// This decorator bridges the gap between the scheduler thread and the worker thread
		executor.setTaskDecorator(runnable -> {
			// Captures tenantId from the scheduler loop thread
			String tenantId = TenantContext.getCurrentTenant();
			return () -> {
				try {
					// Applies tenantId to the worker thread
					TenantContext.setCurrentTenant(tenantId);
					runnable.run();
				} finally {
					TenantContext.clear();
				}
			};
		});

		executor.initialize();
		return executor;
	}
	*/
}


