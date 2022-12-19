package com.metrics.rest;

import com.howtodoinjava.rest.aws.get_metrics_data;
import com.howtodoinjava.rest.azure.DesCreateTest;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import javax.ws.rs.client.Client;

import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




public class App extends Application<Configuration> {
	private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

	@Override
	public void initialize(Bootstrap<Configuration> b) {
	}

	@Override
	public void run(Configuration c, Environment e) throws Exception 
	{
		LOGGER.info("Registering REST resources");

		e.jersey().register(new AzureMetrics());
        e.jersey().register(new get_metrics_data());
		final Client client = new JerseyClientBuilder(e)
				.build("DemoRESTClient");

        e.jersey().register(RolesAllowedDynamicFeature.class);
	}

	public static void main(String[] args) throws Exception {
		new App().run(args);
	}
}