/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.clustering.ejb;

import org.jboss.as.controller.LegacySubsystemURN;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;

/**
 * Enumerates the schema versions for the distributable-ejb subsystem.
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public enum DistributableEjbSchema implements PersistentSubsystemSchema<DistributableEjbSchema> {

    VERSION_1_0(1, 0), // WildFly 27
    ;
    static final DistributableEjbSchema CURRENT = VERSION_1_0;

    private final VersionedNamespace<IntVersion, DistributableEjbSchema> namespace;

    DistributableEjbSchema(int major, int minor) {
        this.namespace = new LegacySubsystemURN<>(DistributableEjbExtension.SUBSYSTEM_NAME, new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, DistributableEjbSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public PersistentResourceXMLDescription getXMLDescription() {
        return DistributableEjbXMLDescriptionFactory.INSTANCE.apply(this);
    }
}
