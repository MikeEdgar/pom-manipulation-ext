/*
 * Copyright (C) 2012 Red Hat, Inc. (jcasey@redhat.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.maven.ext.common.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.commonjava.maven.atlas.ident.ref.TypeAndClassifier;

/**
 * Wrapper around SimpleArtifactRef to also store the scope of the dependency.
 */
@Data
@ToString( callSuper = true )
@EqualsAndHashCode( callSuper = true )
public class SimpleScopedArtifactRef extends SimpleArtifactRef
{
    private final String scope;

    public SimpleScopedArtifactRef( final ProjectVersionRef ref, final TypeAndClassifier tc, final String scope )
    {
        super( ref, tc );
        this.scope = scope;
    }
}
