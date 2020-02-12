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
package org.commonjava.maven.ext.integrationtest.invoker;

import org.commonjava.maven.ext.integrationtest.TestUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * @author vdedik@redhat.com
 */
public class DefaultExecutionParser
    implements ExecutionParser
{
    public static final List<ExecutionParserHandler> DEFAULT_HANDLERS = new ArrayList<ExecutionParserHandler>()
    {{
        add( ExecutionParser.SKIP_HANDLER );
        add( ExecutionParser.BUILD_HANDLER );
        add( ExecutionParser.BUILD_RESULT_HANDLER );
        add( ExecutionParser.BUILD_PROFILES_HANDLER );
        add( ExecutionParser.SYSTEM_PROPERTIES_HANDLER );
    }};

    private final List<ExecutionParserHandler> handlers;

    public DefaultExecutionParser( List<ExecutionParserHandler> handlers )
    {
        this.handlers = handlers;
    }

    @Override
    public Collection<Execution> parse( String workingDir )
    {
        final Properties invokerProperties = TestUtils.loadProps( workingDir + "/invoker.properties" );
        Map<Integer, Execution> executions = new TreeMap<>();

        for ( Object rawKey : invokerProperties.keySet() )
        {
            final String key = (String) rawKey;
            String[] parts = key.split( "\\." );
            int id;
            try
            {
                id = Integer.parseInt( parts[parts.length - 1] );
            }
            catch ( NumberFormatException e )
            {
                id = 1;
            }
            Execution execution;
            if ( executions.containsKey( id ) )
            {
                execution = executions.get( id );
            }
            else
            {
                // Create new execution
                execution = new Execution();
                execution.setLocation( workingDir );
            }

            // Run through all handlers
            for ( ExecutionParserHandler handler : handlers )
            {
                handler.handle( execution, new HashMap<String, String>()
                {{
                        put( "key", key );
                        put( "value", invokerProperties.getProperty( key ) );
                }} );
            }

            executions.put( id, execution );
        }
        if ( invokerProperties.keySet().isEmpty() )
        {
            // by default do one execution (maven install)
            Execution execution = new Execution();
            execution.setLocation( workingDir );
            execution.setMvnCommand( "install" );
            executions.put( 1, execution );
        }
        // Run POST_HANDLER
        for ( Execution execution : executions.values() )
        {
            ExecutionParser.POST_HANDLER.handle( execution, null );
        }

        return executions.values();
    }
}
