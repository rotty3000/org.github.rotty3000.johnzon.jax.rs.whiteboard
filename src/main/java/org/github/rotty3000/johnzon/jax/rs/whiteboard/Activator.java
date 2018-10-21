/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.github.rotty3000.johnzon.jax.rs.whiteboard;

import static org.apache.aries.component.dsl.OSGi.all;
import static org.apache.aries.component.dsl.OSGi.coalesce;
import static org.apache.aries.component.dsl.OSGi.configuration;
import static org.apache.aries.component.dsl.OSGi.configurations;
import static org.apache.aries.component.dsl.OSGi.just;
import static org.apache.aries.component.dsl.OSGi.register;
import static org.apache.aries.component.dsl.OSGi.service;
import static org.apache.aries.component.dsl.OSGi.serviceReferences;
import static org.apache.aries.component.dsl.Utils.highest;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Priority;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.aries.component.dsl.OSGi;
import org.apache.aries.component.dsl.OSGiResult;
import org.apache.johnzon.jaxrs.JohnzonProvider;
import org.apache.johnzon.jaxrs.JsrProvider;
import org.apache.johnzon.jaxrs.jsonb.jaxrs.JsonbJaxrsProvider;
import org.apache.johnzon.mapper.Mapper;
import org.apache.johnzon.mapper.MapperBuilder;
import org.osgi.annotation.bundle.Header;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.Converters;

@Header(name = Constants.BUNDLE_ACTIVATOR, value = "${@class}")
public class Activator implements BundleActivator {
	private static final Converter CONVERTER = Converters.standardConverter();

	private static final OSGi<Entry<Dictionary<String, ?>, JohnzonConfig>> JOHNZON_CONFIGURATION = coalesce(
		all(
			configurations(JohnzonConfig.CONFIG_PID),
			configuration(JohnzonConfig.CONFIG_PID)
		),
		just(Hashtable::new)
	).map(
		properties -> new AbstractMap.SimpleImmutableEntry<>(
			properties,
			CONVERTER.convert(properties).to(JohnzonConfig.class)
		)
	);

	private static final OSGi<Entry<Dictionary<String, ?>, JSONBConfig>> JSONB_CONFIGURATION = coalesce(
		all(
			configurations(JSONBConfig.CONFIG_PID),
			configuration(JSONBConfig.CONFIG_PID)
		),
		just(Hashtable::new)
	).map(
		properties -> new AbstractMap.SimpleImmutableEntry<>(
			properties,
			CONVERTER.convert(properties).to(JSONBConfig.class)
		)
	);

	private static final OSGi<Entry<Dictionary<String, ?>, JSONPConfig>> JSONP_CONFIGURATION = coalesce(
		all(
			configurations(JSONPConfig.CONFIG_PID),
			configuration(JSONPConfig.CONFIG_PID)
		),
		just(Hashtable::new)
	).map(
		properties -> new AbstractMap.SimpleImmutableEntry<>(
			properties,
			CONVERTER.convert(properties).to(JSONPConfig.class)
		)
	);

	private OSGiResult _result;

	@Override
	public void start(BundleContext context) throws Exception {
		_result = all(
			JOHNZON_CONFIGURATION.flatMap(
				entry -> coalesce(
					service(highest(serviceReferences(Mapper.class, entry.getValue().mapper_target()))),
					just(() -> new MapperBuilder().setDoCloseOnStreams(false).build())
				).flatMap(
					mapper -> register(
						new String[]{
							MessageBodyReader.class.getName(),
							MessageBodyWriter.class.getName()
						},
						new JohnzonProviderFactory(mapper, entry.getValue().ignores()),
						getJohnzonRegistrationProperties(entry.getKey(), entry.getValue())
					)
				)
			),
			JSONB_CONFIGURATION.flatMap(
				entry -> register(
					new String[]{
						MessageBodyReader.class.getName(),
						MessageBodyWriter.class.getName()
					},
					new JsonbJaxrsProviderFactory(entry.getValue().ignores()),
					getJSONBRegistrationProperties(entry.getKey(), entry.getValue())
				)
			),
			JSONP_CONFIGURATION.flatMap(
				entry -> register(
					new String[]{
						MessageBodyReader.class.getName(),
						MessageBodyWriter.class.getName()
					},
					new JsonpProviderFactory(),
					getJSONPRegistrationProperties(entry.getKey(), entry.getValue())
				)
			)
		).run(context);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		_result.close();
	}

	@SuppressWarnings("serial")
	private Map<String, ?> getJohnzonRegistrationProperties(
		Dictionary<String, ?> properties, JohnzonConfig config) {

		Enumeration<String> keys = properties.keys();

		return new Hashtable<String, Object>() {{
			while (keys.hasMoreElements()) {
				String key = keys.nextElement();

				if(!key.startsWith(".")) {
					put(key, properties.get(key));
				}
			}

			put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, true);
			put(JaxrsWhiteboardConstants.JAX_RS_NAME, "johnzon.json");

			putIfAbsent("ignores", config.ignores());
			putIfAbsent(
				JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT,
				config.osgi_jaxrs_application_select());
			putIfAbsent(
				JaxrsWhiteboardConstants.JAX_RS_MEDIA_TYPE,
				config.osgi_jaxrs_media_type());
		}};
	}

	@SuppressWarnings("serial")
	private Map<String, ?> getJSONBRegistrationProperties(
		Dictionary<String, ?> properties, JSONBConfig config) {

		Enumeration<String> keys = properties.keys();

		return new Hashtable<String, Object>() {{
			while (keys.hasMoreElements()) {
				String key = keys.nextElement();

				if(!key.startsWith(".")) {
					put(key, properties.get(key));
				}
			}

			put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, true);
			put(JaxrsWhiteboardConstants.JAX_RS_NAME, "johnzon.jaxb-json");

			putIfAbsent("ignores", config.ignores());
			putIfAbsent(
				JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT,
				config.osgi_jaxrs_application_select());
			putIfAbsent(
				JaxrsWhiteboardConstants.JAX_RS_MEDIA_TYPE,
				config.osgi_jaxrs_media_type());
			putIfAbsent(
				Constants.SERVICE_RANKING,
				JsonbJaxrsProvider.class.getAnnotation(Priority.class).value());
		}};
	}

	@SuppressWarnings("serial")
	private Map<String, ?> getJSONPRegistrationProperties(
		Dictionary<String, ?> properties, JSONPConfig config) {

		Enumeration<String> keys = properties.keys();

		return new Hashtable<String, Object>() {{
			while (keys.hasMoreElements()) {
				String key = keys.nextElement();

				if(!key.startsWith(".")) {
					put(key, properties.get(key));
				}
			}

			put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, true);
			put(JaxrsWhiteboardConstants.JAX_RS_NAME, "johnzon.jsonp");

			putIfAbsent(
				JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT,
				config.osgi_jaxrs_application_select());
			putIfAbsent(
				JaxrsWhiteboardConstants.JAX_RS_MEDIA_TYPE,
				config.osgi_jaxrs_media_type());
		}};
	}

	private class JohnzonProviderFactory implements PrototypeServiceFactory<JohnzonProvider<?>> {

		private Mapper mapper;
		private Collection<String> ignores;

		public JohnzonProviderFactory(Mapper mapper, String[] ignores) {
			this.mapper = mapper;
			this.ignores = Arrays.asList(ignores);
		}

		@Override
		public JohnzonProvider<?> getService(
			Bundle bundle, ServiceRegistration<JohnzonProvider<?>> registration) {

			return new JohnzonProvider<>(mapper, ignores);
		}

		@Override
		public void ungetService(
			Bundle bundle, ServiceRegistration<JohnzonProvider<?>> registration, JohnzonProvider<?> service) {
		}

	}

	private class JsonbJaxrsProviderFactory implements PrototypeServiceFactory<JsonbJaxrsProvider<?>> {

		private final Collection<String> ignores;

		public JsonbJaxrsProviderFactory(String[] ignores) {
			this.ignores = Arrays.asList(ignores);
		}

		@Override
		public JsonbJaxrsProvider<?> getService(
			Bundle bundle, ServiceRegistration<JsonbJaxrsProvider<?>> registration) {

			return new ExtendedJsonbJaxrsProvider(ignores);
		}

		@Override
		public void ungetService(
			Bundle bundle, ServiceRegistration<JsonbJaxrsProvider<?>> registration, JsonbJaxrsProvider<?> service) {
		}

	}

	private class JsonpProviderFactory implements PrototypeServiceFactory<JsrProvider> {

		@Override
		public JsrProvider getService(
			Bundle bundle, ServiceRegistration<JsrProvider> registration) {

			return new JsrProvider();
		}

		@Override
		public void ungetService(
			Bundle bundle, ServiceRegistration<JsrProvider> registration, JsrProvider service) {
		}

	}

	private class ExtendedJsonbJaxrsProvider extends JsonbJaxrsProvider<Object> {
		public ExtendedJsonbJaxrsProvider(final Collection<String> ignores) {
			super(ignores);
		}
	}

}
