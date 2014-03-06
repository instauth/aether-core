/*******************************************************************************
 * Copyright (c) 2010, 2013 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.aether.internal.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.SyncContext;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeployResult;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.impl.DependencyCollector;
import org.eclipse.aether.impl.Deployer;
import org.eclipse.aether.impl.Installer;
import org.eclipse.aether.impl.LocalRepositoryProvider;
import org.eclipse.aether.impl.MetadataResolver;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.impl.SyncContextFactory;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.impl.VersionResolver;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallResult;
import org.eclipse.aether.installation.InstallationException;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.resolution.MetadataRequest;
import org.eclipse.aether.resolution.MetadataResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.resolution.VersionRequest;
import org.eclipse.aether.resolution.VersionResolutionException;
import org.eclipse.aether.resolution.VersionResult;
import org.eclipse.aether.spi.locator.Service;
import org.eclipse.aether.spi.locator.ServiceLocator;
import org.eclipse.aether.spi.log.Logger;
import org.eclipse.aether.spi.log.LoggerFactory;
import org.eclipse.aether.spi.log.NullLoggerFactory;
import org.eclipse.aether.util.graph.visitor.FilteringDependencyVisitor;
import org.eclipse.aether.util.graph.visitor.TreeDependencyVisitor;

/**
 */
@Named
@Component( role = RepositorySystem.class )
public class DefaultRepositorySystem
    implements RepositorySystem, Service
{

    @Requirement( role = LoggerFactory.class )
    private Logger logger = NullLoggerFactory.LOGGER;

    @Requirement
    private VersionResolver versionResolver;

    @Requirement
    private VersionRangeResolver versionRangeResolver;

    @Requirement
    private ArtifactResolver artifactResolver;

    public void setArtifactResolver(ArtifactResolver artifactResolver) {
        this.artifactResolver = artifactResolver;
    }

    @Requirement
    private MetadataResolver metadataResolver;

    @Requirement
    private ArtifactDescriptorReader artifactDescriptorReader;

    @Requirement
    private DependencyCollector dependencyCollector;

    @Requirement
    private Installer installer;

    @Requirement
    private Deployer deployer;

    @Requirement
    private LocalRepositoryProvider localRepositoryProvider;

    @Requirement
    private SyncContextFactory syncContextFactory;

    @Requirement
    private RemoteRepositoryManager remoteRepositoryManager;

    public DefaultRepositorySystem()
    {
        // enables default constructor
    	System.out.println("test");
    }

    @Inject
    DefaultRepositorySystem( VersionResolver versionResolver, VersionRangeResolver versionRangeResolver,
                             ArtifactResolver artifactResolver, MetadataResolver metadataResolver,
                             ArtifactDescriptorReader artifactDescriptorReader,
                             DependencyCollector dependencyCollector, Installer installer, Deployer deployer,
                             LocalRepositoryProvider localRepositoryProvider, SyncContextFactory syncContextFactory,
                             RemoteRepositoryManager remoteRepositoryManager, LoggerFactory loggerFactory )
    {
        withVersionResolver( versionResolver );
        withVersionRangeResolver( versionRangeResolver );
        withArtifactResolver( artifactResolver );
        withMetadataResolver( metadataResolver );
        withArtifactDescriptorReader( artifactDescriptorReader );
        withDependencyCollector( dependencyCollector );
        withInstaller( installer );
        withDeployer( deployer );
        withLocalRepositoryProvider( localRepositoryProvider );
        withSyncContextFactory( syncContextFactory );
        withRemoteRepositoryManager( remoteRepositoryManager );
        withLoggerFactory( loggerFactory );
    }

    public void initService( ServiceLocator locator )
    {
        withLoggerFactory( locator.getService( LoggerFactory.class ) );
        withVersionResolver( locator.getService( VersionResolver.class ) );
        withVersionRangeResolver( locator.getService( VersionRangeResolver.class ) );
        withArtifactResolver( locator.getService( ArtifactResolver.class ) );
        withMetadataResolver( locator.getService( MetadataResolver.class ) );
        withArtifactDescriptorReader( locator.getService( ArtifactDescriptorReader.class ) );
        withDependencyCollector( locator.getService( DependencyCollector.class ) );
        withInstaller( locator.getService( Installer.class ) );
        withDeployer( locator.getService( Deployer.class ) );
        withLocalRepositoryProvider( locator.getService( LocalRepositoryProvider.class ) );
        withRemoteRepositoryManager( locator.getService( RemoteRepositoryManager.class ) );
        withSyncContextFactory( locator.getService( SyncContextFactory.class ) );
    }

    public DefaultRepositorySystem withLoggerFactory( LoggerFactory loggerFactory )
    {
        this.logger = NullLoggerFactory.getSafeLogger( loggerFactory, getClass() );
        return this;
    }

    void setLogger( LoggerFactory loggerFactory )
    {
        // plexus support
        withLoggerFactory( loggerFactory );
    }

    public DefaultRepositorySystem withVersionResolver( VersionResolver versionResolver )
    {
        if ( versionResolver == null )
        {
            throw new IllegalArgumentException( "version resolver has not been specified" );
        }
        this.versionResolver = versionResolver;
        return this;
    }

    public void setVersionRangeResolver(VersionRangeResolver versionRangeResolver) {
        this.versionRangeResolver = versionRangeResolver;
    }

    public DefaultRepositorySystem withVersionRangeResolver( VersionRangeResolver versionRangeResolver )
    {
        if ( versionRangeResolver == null )
        {
            throw new IllegalArgumentException( "version range resolver has not been specified" );
        }
        this.versionRangeResolver = versionRangeResolver;
        return this;
    }

    public DefaultRepositorySystem withArtifactResolver( ArtifactResolver artifactResolver )
    {
        if ( artifactResolver == null )
        {
            throw new IllegalArgumentException( "artifact resolver has not been specified" );
        }
        this.artifactResolver = artifactResolver;
        return this;
    }

    public DefaultRepositorySystem withMetadataResolver( MetadataResolver metadataResolver )
    {
        if ( metadataResolver == null )
        {
            throw new IllegalArgumentException( "metadata resolver has not been specified" );
        }
        this.metadataResolver = metadataResolver;
        return this;
    }

    public DefaultRepositorySystem withArtifactDescriptorReader( ArtifactDescriptorReader artifactDescriptorReader )
    {
        if ( artifactDescriptorReader == null )
        {
            throw new IllegalArgumentException( "artifact descriptor reader has not been specified" );
        }
        this.artifactDescriptorReader = artifactDescriptorReader;
        return this;
    }

    public DefaultRepositorySystem withDependencyCollector( DependencyCollector dependencyCollector )
    {
        if ( dependencyCollector == null )
        {
            throw new IllegalArgumentException( "dependency collector has not been specified" );
        }
        this.dependencyCollector = dependencyCollector;
        return this;
    }

    public DefaultRepositorySystem withInstaller( Installer installer )
    {
        if ( installer == null )
        {
            throw new IllegalArgumentException( "installer has not been specified" );
        }
        this.installer = installer;
        return this;
    }

    public DefaultRepositorySystem withDeployer( Deployer deployer )
    {
        if ( deployer == null )
        {
            throw new IllegalArgumentException( "deployer has not been specified" );
        }
        this.deployer = deployer;
        return this;
    }

    public void setLocalRepositoryProvider(LocalRepositoryProvider localRepositoryProvider) {
        this.localRepositoryProvider = localRepositoryProvider;
    }

    public DefaultRepositorySystem withLocalRepositoryProvider( LocalRepositoryProvider localRepositoryProvider )
    {
        if ( localRepositoryProvider == null )
        {
            throw new IllegalArgumentException( "local repository provider has not been specified" );
        }
        this.localRepositoryProvider = localRepositoryProvider;
        return this;
    }

    public DefaultRepositorySystem withSyncContextFactory( SyncContextFactory syncContextFactory )
    {
        if ( syncContextFactory == null )
        {
            throw new IllegalArgumentException( "sync context factory has not been specified" );
        }
        this.syncContextFactory = syncContextFactory;
        return this;
    }

    public void setRemoteRepositoryManager(RemoteRepositoryManager remoteRepositoryManager) {
        this.remoteRepositoryManager = remoteRepositoryManager;
    }
    
    public DefaultRepositorySystem withRemoteRepositoryManager( RemoteRepositoryManager remoteRepositoryManager )
    {
        if ( remoteRepositoryManager == null )
        {
            throw new IllegalArgumentException( "remote repository manager has not been specified" );
        }
        this.remoteRepositoryManager = remoteRepositoryManager;
        return this;
    }

    public VersionResult resolveVersion( RepositorySystemSession session, VersionRequest request )
        throws VersionResolutionException
    {
        validateSession( session );
        return versionResolver.resolveVersion( session, request );
    }

    public VersionRangeResult resolveVersionRange( RepositorySystemSession session, VersionRangeRequest request )
        throws VersionRangeResolutionException
    {
        validateSession( session );
        return versionRangeResolver.resolveVersionRange( session, request );
    }

    public ArtifactDescriptorResult readArtifactDescriptor( RepositorySystemSession session,
                                                            ArtifactDescriptorRequest request )
        throws ArtifactDescriptorException
    {
        validateSession( session );
        return artifactDescriptorReader.readArtifactDescriptor( session, request );
    }

    public ArtifactResult resolveArtifact( RepositorySystemSession session, ArtifactRequest request )
        throws ArtifactResolutionException
    {
        validateSession( session );
        return artifactResolver.resolveArtifact( session, request );
    }

    public List<ArtifactResult> resolveArtifacts( RepositorySystemSession session,
                                                  Collection<? extends ArtifactRequest> requests )
        throws ArtifactResolutionException
    {
        validateSession( session );
        return artifactResolver.resolveArtifacts( session, requests );
    }

    public List<MetadataResult> resolveMetadata( RepositorySystemSession session,
                                                 Collection<? extends MetadataRequest> requests )
    {
        validateSession( session );
        return metadataResolver.resolveMetadata( session, requests );
    }

    public CollectResult collectDependencies( RepositorySystemSession session, CollectRequest request )
        throws DependencyCollectionException
    {
        validateSession( session );
        return dependencyCollector.collectDependencies( session, request );
    }

    public DependencyResult resolveDependencies( RepositorySystemSession session, DependencyRequest request )
        throws DependencyResolutionException
    {
        validateSession( session );

        RequestTrace trace = RequestTrace.newChild( request.getTrace(), request );

        DependencyResult result = new DependencyResult( request );

        DependencyCollectionException dce = null;
        ArtifactResolutionException are = null;

        if ( request.getRoot() != null )
        {
            result.setRoot( request.getRoot() );
        }
        else if ( request.getCollectRequest() != null )
        {
            CollectResult collectResult;
            try
            {
                request.getCollectRequest().setTrace( trace );
                collectResult = dependencyCollector.collectDependencies( session, request.getCollectRequest() );
            }
            catch ( DependencyCollectionException e )
            {
                dce = e;
                collectResult = e.getResult();
            }
            result.setRoot( collectResult.getRoot() );
            result.setCycles( collectResult.getCycles() );
            result.setCollectExceptions( collectResult.getExceptions() );
        }
        else
        {
            throw new IllegalArgumentException( "dependency node or collect request missing" );
        }

        ArtifactRequestBuilder builder = new ArtifactRequestBuilder( trace );
        DependencyFilter filter = request.getFilter();
        DependencyVisitor visitor = ( filter != null ) ? new FilteringDependencyVisitor( builder, filter ) : builder;
        visitor = new TreeDependencyVisitor( visitor );
        result.getRoot().accept( visitor );
        List<ArtifactRequest> requests = builder.getRequests();

        List<ArtifactResult> results;
        try
        {
            results = artifactResolver.resolveArtifacts( session, requests );
        }
        catch ( ArtifactResolutionException e )
        {
            are = e;
            results = e.getResults();
        }
        result.setArtifactResults( results );

        updateNodesWithResolvedArtifacts( results );

        if ( dce != null )
        {
            throw new DependencyResolutionException( result, dce );
        }
        else if ( are != null )
        {
            throw new DependencyResolutionException( result, are );
        }

        return result;
    }

    private void updateNodesWithResolvedArtifacts( List<ArtifactResult> results )
    {
        for ( ArtifactResult result : results )
        {
            Artifact artifact = result.getArtifact();
            if ( artifact != null )
            {
                result.getRequest().getDependencyNode().setArtifact( artifact );
            }
        }
    }

    public InstallResult install( RepositorySystemSession session, InstallRequest request )
        throws InstallationException
    {
        validateSession( session );
        return installer.install( session, request );
    }

    public DeployResult deploy( RepositorySystemSession session, DeployRequest request )
        throws DeploymentException
    {
        validateSession( session );
        return deployer.deploy( session, request );
    }

    public LocalRepositoryManager newLocalRepositoryManager( RepositorySystemSession session,
                                                             LocalRepository localRepository )
    {
        try
        {
            return localRepositoryProvider.newLocalRepositoryManager( session, localRepository );
        }
        catch ( NoLocalRepositoryManagerException e )
        {
            throw new IllegalArgumentException( e.getMessage(), e );
        }
    }

    public SyncContext newSyncContext( RepositorySystemSession session, boolean shared )
    {
        validateSession( session );
        return syncContextFactory.newInstance( session, shared );
    }

    public List<RemoteRepository> newResolutionRepositories( RepositorySystemSession session,
                                                             List<RemoteRepository> repositories )
    {
        validateSession( session );
        repositories =
            remoteRepositoryManager.aggregateRepositories( session, new ArrayList<RemoteRepository>(), repositories,
                                                           true );
        return repositories;
    }

    public RemoteRepository newDeploymentRepository( RepositorySystemSession session, RemoteRepository repository )
    {
        validateSession( session );
        RemoteRepository.Builder builder = new RemoteRepository.Builder( repository );
        Authentication auth = session.getAuthenticationSelector().getAuthentication( repository );
        builder.setAuthentication( auth );
        Proxy proxy = session.getProxySelector().getProxy( repository );
        builder.setProxy( proxy );
        return builder.build();
    }

    private void validateSession( RepositorySystemSession session )
    {
        if ( session == null )
        {
            throw new IllegalArgumentException( "Invalid repository system session: the session may not be null." );
        }
        if ( session.getLocalRepositoryManager() == null )
        {
            invalidSession( "LocalRepositoryManager" );
        }
        if ( session.getSystemProperties() == null )
        {
            invalidSession( "SystemProperties" );
        }
        if ( session.getUserProperties() == null )
        {
            invalidSession( "UserProperties" );
        }
        if ( session.getConfigProperties() == null )
        {
            invalidSession( "ConfigProperties" );
        }
        if ( session.getMirrorSelector() == null )
        {
            invalidSession( "MirrorSelector" );
        }
        if ( session.getProxySelector() == null )
        {
            invalidSession( "ProxySelector" );
        }
        if ( session.getAuthenticationSelector() == null )
        {
            invalidSession( "AuthenticationSelector" );
        }
        if ( session.getArtifactTypeRegistry() == null )
        {
            invalidSession( "ArtifactTypeRegistry" );
        }
        if ( session.getData() == null )
        {
            invalidSession( "Data" );
        }
    }

    private void invalidSession( String name )
    {
        throw new IllegalArgumentException( "Invalid repository system session: " + name + " is not set." );
    }

}
