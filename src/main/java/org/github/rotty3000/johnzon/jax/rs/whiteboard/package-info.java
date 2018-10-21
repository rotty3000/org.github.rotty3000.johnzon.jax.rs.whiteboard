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

@RequireCDIImplementation
@Requirement(
	name = "osgi.serviceloader.registrar",
	namespace = EXTENDER_NAMESPACE
)
@Capability(
	attribute = {
		"osgi.cdi.extension=JavaJSONB"
	},
	name = "javax.enterprise.inject.spi.Extension",
	namespace = "osgi.serviceloader",
	uses = {
		javax.enterprise.event.Observes.class,
		javax.enterprise.inject.spi.Extension.class
	}
)
@Capability(
	name = "JavaJSONB",
	namespace = "osgi.cdi.extension",
	uses = {
		javax.enterprise.event.Observes.class,
		javax.enterprise.inject.spi.Extension.class
	}
)
@Capability(
	name = "javax.json.bind.spi.JsonbProvider",
	namespace = "osgi.serviceloader",
	uses = {
		javax.enterprise.event.Observes.class,
		javax.json.bind.spi.JsonbProvider.class
	}
)
@Capability(
	attribute = {
		"objectClass:List<String>='javax.enterprise.inject.spi.Extension'",
		"osgi.cdi.extension=JavaJSONB"
	},
	effective = "active",
	namespace = SERVICE_NAMESPACE
)
@Capability(
	attribute = {
		"objectClass:List<String>='javax.ws.rs.ext.MessageBodyReader,javax.ws.rs.ext.MessageBodyWriter'",
		"osgi.jaxrs.media.type=application/json"
	},
	effective = "active",
	namespace = SERVICE_NAMESPACE
)
package org.github.rotty3000.johnzon.jax.rs.whiteboard;

import static org.osgi.namespace.extender.ExtenderNamespace.EXTENDER_NAMESPACE;
import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import org.osgi.service.cdi.annotations.RequireCDIImplementation;
import org.osgi.annotation.bundle.Capability;
import org.osgi.annotation.bundle.Requirement;
