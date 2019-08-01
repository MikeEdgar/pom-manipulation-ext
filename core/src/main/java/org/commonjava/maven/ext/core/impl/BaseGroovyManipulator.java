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
package org.commonjava.maven.ext.core.impl;

import groovy.lang.GroovyShell;
import groovy.lang.MissingMethodException;
import groovy.lang.Script;
import org.apache.commons.io.FileUtils;
import org.codehaus.groovy.control.CompilationFailedException;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.commonjava.maven.ext.common.ManipulationException;
import org.commonjava.maven.ext.common.model.Project;
import org.commonjava.maven.ext.core.ManipulationSession;
import org.commonjava.maven.ext.core.groovy.BaseScript;
import org.commonjava.maven.ext.core.groovy.InvocationStage;
import org.commonjava.maven.ext.core.groovy.PMEInvocationPoint;
import org.commonjava.maven.ext.core.state.GroovyState;
import org.commonjava.maven.ext.io.FileIO;
import org.commonjava.maven.ext.io.ModelIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * {@link Manipulator} implementation that can resolve a remote groovy file and execute it on executionRoot. Configuration
 * is stored in a {@link GroovyState} instance, which is in turn stored in the {@link ManipulationSession}.
 */
public abstract class BaseGroovyManipulator
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    protected ModelIO modelBuilder;

    protected FileIO fileIO;

    protected ManipulationSession session;

    BaseGroovyManipulator( ModelIO modelIO, FileIO fileIO )
    {
        this.modelBuilder = modelIO;
        this.fileIO = fileIO;
    }

    public abstract int getExecutionIndex();


    /**
     * Splits the value on ',', then wraps each value in {@link SimpleArtifactRef#parse(String)} and prints a warning / skips in the event of a
     * parsing error. Returns null if the input value is null.
     * @param value a comma separated list of GAVTC to parse
     * @return a collection of parsed ArtifactRef.
     * @throws ManipulationException if an error occurs.
     */
    public List<File> parseGroovyScripts( final String value ) throws ManipulationException
    {
        if ( isEmpty( value ) )
        {
            return Collections.emptyList();
        }
        else
        {
            final List<File> result = new ArrayList<>();

            logger.debug( "Processing groovy scripts {} ", value );
            try
            {
                final String[] scripts = value.split( "," );
                for ( final String script : scripts )
                {
                    File found;
                    if ( script.startsWith( "http" ) || script.startsWith( "file" ))
                    {
                        logger.info( "Attempting to read URL {} ", script );
                        found = fileIO.resolveURL( new URL( script ) );
                    }
                    else
                    {
                        final ArtifactRef ar = SimpleArtifactRef.parse( script );
                        logger.info( "Attempting to read GAV {} with classifier {} and type {} ", ar.asProjectVersionRef(), ar.getClassifier(), ar.getType() );
                        found = modelBuilder.resolveRawFile( ar );
                    }
                    result.add( found );
                }
            }
            catch ( IOException e )
            {
                throw new ManipulationException( "Unable to parse groovyScripts", e );
            }
            return result;
        }
    }


    void applyGroovyScript( List<Project> projects, Project project, File groovyScript ) throws ManipulationException
    {
        final GroovyShell shell = new GroovyShell( );
        final Script script;
        final InvocationStage stage;

        try
        {
            script = shell.parse( groovyScript );

            PMEInvocationPoint invocationPoint = script.getClass().getAnnotation( PMEInvocationPoint.class );
            if ( invocationPoint != null )
            {
                logger.debug( "InvocationPoint is {}", invocationPoint.invocationPoint().toString() );
                stage = invocationPoint.invocationPoint();
            }
            else
            {
                throw new ManipulationException( "Mandatory annotation '@PMEInvocationPoint(invocationPoint = ' not declared" );
            }
            if ( script instanceof BaseScript )
            {
                ((BaseScript) script).setValues( modelBuilder, session, projects, project, stage);
            }
            else
            {
                throw new ManipulationException( "Cannot cast " + groovyScript + " to a BaseScript to set values." );
            }
        }
        catch (MissingMethodException e)
        {
            try
            {
                logger.debug ( "Failure when injecting into script {} ", FileUtils.readFileToString( groovyScript, Charset.defaultCharset() ), e );
            }
            catch ( IOException e1 )
            {
                logger.debug ("Unable to read script file {} for debugging! {} ", groovyScript, e1);
            }
            throw new ManipulationException( "Unable to inject values into base script", e );
        }
        catch (CompilationFailedException e)
        {
            try
            {
                logger.debug ( "Failure when parsing script {} ", FileUtils.readFileToString( groovyScript, Charset.defaultCharset() ), e );
            }
            catch ( IOException e1 )
            {
                logger.debug ("Unable to read script file {} for debugging! {} ", groovyScript, e1);
            }
            throw new ManipulationException( "Unable to parse script", e );
        }
        catch ( IOException e )
        {
            throw new ManipulationException( "Unable to parse script", e );
        }

        if ( getExecutionIndex() == stage.getStageValue() || stage == InvocationStage.BOTH )
        {
            try
            {
                logger.info ("Executing {} on {} at invocation point {}", groovyScript, project, stage);

                script.run();

                logger.info ("Completed {}.", groovyScript);
            }
            catch ( Exception e )
            {
                throw new ManipulationException( "Unable to parse script", e );
            }
        }
        else
        {
            logger.debug( "Ignoring script {} as invocation point {} does not match index {}", groovyScript, stage, getExecutionIndex() );
        }
    }
}
