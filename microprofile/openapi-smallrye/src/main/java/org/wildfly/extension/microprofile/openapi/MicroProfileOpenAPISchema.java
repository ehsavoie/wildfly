/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.openapi;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemURN;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;

/**
 * Enumeration of MicroProfile OpenAPI subsystem schema versions.
 * @author Paul Ferraro
 */
public enum MicroProfileOpenAPISchema implements PersistentSubsystemSchema<MicroProfileOpenAPISchema> {

    VERSION_1_0(1, 0), // WildFly 19
    ;
    static final MicroProfileOpenAPISchema CURRENT = VERSION_1_0;

    private final VersionedNamespace<IntVersion, MicroProfileOpenAPISchema> namespace;

    MicroProfileOpenAPISchema(int major, int minor) {
        this.namespace = new SubsystemURN<>(MicroProfileOpenAPIExtension.SUBSYSTEM_NAME, new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, MicroProfileOpenAPISchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public PersistentResourceXMLDescription getXMLDescription() {
        return MicroProfileOpenAPIXMLDescriptionFactory.INSTANCE.apply(this);
    }
}
