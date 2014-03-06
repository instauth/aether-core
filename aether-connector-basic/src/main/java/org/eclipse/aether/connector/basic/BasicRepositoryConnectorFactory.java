/*******************************************************************************
 * Copyright (c) 2013 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.aether.connector.basic;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.RepositoryConnector;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.checksum.ChecksumPolicyProvider;
import org.eclipse.aether.spi.connector.layout.RepositoryLayoutProvider;
import org.eclipse.aether.spi.connector.transport.TransporterProvider;
import org.eclipse.aether.spi.io.FileProcessor;
import org.eclipse.aether.spi.locator.Service;
import org.eclipse.aether.spi.locator.ServiceLocator;
import org.eclipse.aether.spi.log.Logger;
import org.eclipse.aether.spi.log.LoggerFactory;
import org.eclipse.aether.spi.log.NullLoggerFactory;
import org.eclipse.aether.transfer.NoRepositoryConnectorException;

/**
 * A repository connector factory that employs pluggable
 * {@link org.eclipse.aether.spi.connector.transport.TransporterFactory transporters} and
 * {@link org.eclipse.aether.spi.connector.layout.RepositoryLayoutFactory repository layouts} for the transfers.
 */
@Named( "basic" )
@Component( role = RepositoryConnectorFactory.class, hint = "basic" )
public final class BasicRepositoryConnectorFactory
    implements RepositoryConnectorFactory, Service
{

    @Requirement( role = LoggerFactory.class )
    private Logger logger = NullLoggerFactory.LOGGER;

    @Requirement
    private TransporterProvider transporterProvider;
    
    public void setTransporterProvider(TransporterProvider transporterProvider) {
        this.transporterProvider = transporterProvider;
    }

    @Requirement
    private RepositoryLayoutProvider layoutProvider;

    public void setRepositoryLayoutProvider(RepositoryLayoutProvider layoutProvider) {
        this.layoutProvider = layoutProvider;
    }

    @Requirement
    private ChecksumPolicyProvider checksumPolicyProvider;
    
    public void setChecksumPolicyProvider(ChecksumPolicyProvider checksumPolicyProvider) {
        this.checksumPolicyProvider = checksumPolicyProvider;
    }

    @Requirement
    private FileProcessor fileProcessor;
    
    public void setFileProcessor(FileProcessor fileProcessor) {
        this.fileProcessor = fileProcessor;
    }

    private float priority;

    /**
     * Creates an (uninitialized) instance of this connector factory. <em>Note:</em> In case of manual instantiation by
     * clients, the new factory needs to be configured via its various mutators before first use or runtime errors will
     * occur.
     */
    public BasicRepositoryConnectorFactory()
    {
        // enables default constructor
    }

    @Inject
    BasicRepositoryConnectorFactory( TransporterProvider transporterProvider, RepositoryLayoutProvider layoutProvider,
                                     ChecksumPolicyProvider checksumPolicyProvider, FileProcessor fileProcessor,
                                     LoggerFactory loggerFactory )
    {
        withTransporterProvider( transporterProvider );
        withRepositoryLayoutProvider( layoutProvider );
        withChecksumPolicyProvider( checksumPolicyProvider );
        withFileProcessor( fileProcessor );
        withLoggerFactory( loggerFactory );
    }

    public void initService( ServiceLocator locator )
    {
        withLoggerFactory( locator.getService( LoggerFactory.class ) );
        withTransporterProvider( locator.getService( TransporterProvider.class ) );
        withRepositoryLayoutProvider( locator.getService( RepositoryLayoutProvider.class ) );
        withChecksumPolicyProvider( locator.getService( ChecksumPolicyProvider.class ) );
        withFileProcessor( locator.getService( FileProcessor.class ) );
    }

    /**
     * Sets the logger factory to use for this component.
     * 
     * @param loggerFactory The logger factory to use, may be {@code null} to disable logging.
     * @return This component for chaining, never {@code null}.
     */
    public BasicRepositoryConnectorFactory withLoggerFactory( LoggerFactory loggerFactory )
    {
        this.logger = NullLoggerFactory.getSafeLogger( loggerFactory, BasicRepositoryConnector.class );
        return this;
    }

    void setLogger( LoggerFactory loggerFactory )
    {
        // plexus support
        withLoggerFactory( loggerFactory );
    }

    /**
     * Sets the transporter provider to use for this component.
     * 
     * @param transporterProvider The transporter provider to use, must not be {@code null}.
     * @return This component for chaining, never {@code null}.
     */
    public BasicRepositoryConnectorFactory withTransporterProvider( TransporterProvider transporterProvider )
    {
        if ( transporterProvider == null )
        {
            throw new IllegalArgumentException( "transporter provider has not been specified" );
        }
        this.transporterProvider = transporterProvider;
        return this;
    }

    /**
     * Sets the repository layout provider to use for this component.
     * 
     * @param layoutProvider The repository layout provider to use, must not be {@code null}.
     * @return This component for chaining, never {@code null}.
     */
    public BasicRepositoryConnectorFactory withRepositoryLayoutProvider( RepositoryLayoutProvider layoutProvider )
    {
        if ( layoutProvider == null )
        {
            throw new IllegalArgumentException( "repository layout provider has not been specified" );
        }
        this.layoutProvider = layoutProvider;
        return this;
    }

    /**
     * Sets the checksum policy provider to use for this component.
     * 
     * @param checksumPolicyProvider The checksum policy provider to use, must not be {@code null}.
     * @return This component for chaining, never {@code null}.
     */
    public BasicRepositoryConnectorFactory withChecksumPolicyProvider( ChecksumPolicyProvider checksumPolicyProvider )
    {
        if ( checksumPolicyProvider == null )
        {
            throw new IllegalArgumentException( "checksum policy provider has not been specified" );
        }
        this.checksumPolicyProvider = checksumPolicyProvider;
        return this;
    }

    /**
     * Sets the file processor to use for this component.
     * 
     * @param fileProcessor The file processor to use, must not be {@code null}.
     * @return This component for chaining, never {@code null}.
     */
    public BasicRepositoryConnectorFactory withFileProcessor( FileProcessor fileProcessor )
    {
        if ( fileProcessor == null )
        {
            throw new IllegalArgumentException( "file processor has not been specified" );
        }
        this.fileProcessor = fileProcessor;
        return this;
    }

    public float getPriority()
    {
        return priority;
    }

    /**
     * Sets the priority of this component.
     * 
     * @param priority The priority.
     * @return This component for chaining, never {@code null}.
     */
    public BasicRepositoryConnectorFactory withPriority( float priority )
    {
        this.priority = priority;
        return this;
    }

    public RepositoryConnector newInstance( RepositorySystemSession session, RemoteRepository repository )
        throws NoRepositoryConnectorException
    {
        return new BasicRepositoryConnector( session, repository, transporterProvider, layoutProvider,
                                             checksumPolicyProvider, fileProcessor, logger );
    }

}
