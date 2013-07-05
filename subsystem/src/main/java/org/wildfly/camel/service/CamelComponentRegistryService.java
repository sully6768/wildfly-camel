/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.camel.service;

import static org.wildfly.camel.CamelLogger.LOGGER;
import static org.wildfly.camel.CamelMessages.MESSAGES;

import org.apache.camel.spi.ComponentResolver;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;
import org.wildfly.camel.CamelComponentRegistry;
import org.wildfly.camel.CamelConstants;

/**
 * The {@link CamelComponentRegistry} service
 *
 * Ths implementation creates a jboss-msc {@link org.jboss.msc.service.Service}.
 *
 * JBoss services can create a dependency on the {@link ComponentResolver} service like this
 *
 * <code>
        ServiceName serviceName = CamelConstants.CAMEL_COMPONENT_BASE_NAME.append(componentName);
        builder.addDependency(serviceName, ComponentResolver.class, service.injectedComponentResolver);
 * </code>
 *
 * @author Thomas.Diesler@jboss.com
 * @since 05-Jul-2013
 */
public class CamelComponentRegistryService extends AbstractService<CamelComponentRegistry> {

    private CamelComponentRegistry componentRegistry;

    public static ServiceController<CamelComponentRegistry> addService(ServiceTarget serviceTarget, ServiceVerificationHandler verificationHandler) {
        CamelComponentRegistryService service = new CamelComponentRegistryService();
        ServiceBuilder<CamelComponentRegistry> builder = serviceTarget.addService(CamelConstants.CAMEL_COMPONENT_REGISTRY_NAME, service);
        builder.addListener(verificationHandler);
        return builder.install();
    }

    // Hide ctor
    private CamelComponentRegistryService() {
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        final ServiceContainer serviceContainer = startContext.getController().getServiceContainer();
        final ServiceTarget serviceTarget = startContext.getChildTarget();
        componentRegistry = new DefaultCamelComponentRegistry(serviceContainer, serviceTarget);
    }

    @Override
    public CamelComponentRegistry getValue() {
        return componentRegistry;
    }

    class DefaultCamelComponentRegistry implements CamelComponentRegistry {

        private final ServiceContainer serviceContainer;
        private final ServiceTarget serviceTarget;

        DefaultCamelComponentRegistry(ServiceContainer serviceContainer, ServiceTarget serviceTarget) {
            this.serviceContainer = serviceContainer;
            this.serviceTarget = serviceTarget;
        }

        @Override
        public ComponentResolver getComponentResolver(String name) {
            ServiceController<?> controller = serviceContainer.getService(getInternalServiceName(name));
            return controller != null ? (ComponentResolver) controller.getValue() : null;
        }

        @Override
        public CamelComponentRegistration registerCamelComponent(final String name, final ComponentResolver resolver) {
            if (getComponentResolver(name) != null)
                throw MESSAGES.camelComponentAlreadyRegistered(name);

            LOGGER.infoRegisterCamelComponent(name);

            // Install the {@link ComponentResolver} as {@link Service}
            ValueService<ComponentResolver> service = new ValueService<ComponentResolver>(new ImmediateValue<ComponentResolver>(resolver));
            ServiceBuilder<ComponentResolver> builder = serviceTarget.addService(getInternalServiceName(name), service);
            final ServiceController<ComponentResolver> controller = builder.install();

            return new CamelComponentRegistration() {

                @Override
                public ComponentResolver getComponentResolver() {
                    return resolver;
                }

                @Override
                public void unregister() {
                    controller.setMode(Mode.REMOVE);
                }
            };
        }

        private ServiceName getInternalServiceName(String name) {
            return CamelConstants.CAMEL_COMPONENT_BASE_NAME.append(name);
        }
    }
}
