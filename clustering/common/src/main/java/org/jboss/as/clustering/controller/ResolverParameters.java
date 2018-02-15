/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.controller;


import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;

public class ResolverParameters {

    private final String value;
    private final PathAddress address;
    private final Resource resource;

    public ResolverParameters(PathAddress address, Resource resource, String value) {
        this.value = value;
        this.address = address;
        this.resource = resource;
    }

    public ResolverParameters(OperationContext context, String value) {
        this.value = value;
        this.address = context.getCurrentAddress();
        this.resource = context.readResource(PathAddress.EMPTY_ADDRESS, false);
    }

    public PathAddress getAddress() {
        return address;
    }

    public Resource getResource() {
        return resource;
    }

    public String getValue() {
        return value;
    }

    public String getAddressValue() {
        if (this.address != null && this.address.size() > 0) {
            return this.address.getLastElement().getValue();
        }
        return null;
    }
}
