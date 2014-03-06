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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.UpdatePolicyAnalyzer;
import org.eclipse.aether.internal.test.util.TestLoggerFactory;
import org.eclipse.aether.internal.test.util.TestUtils;
import org.eclipse.aether.repository.MirrorSelector;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class DefaultRemoteRepositoryManagerTest
{

    private DefaultRepositorySystemSession session;

    private DefaultRemoteRepositoryManager manager;

    @Before
    public void setup()
        throws Exception
    {
        session = TestUtils.newSession();
        session.setChecksumPolicy( null );
        session.setUpdatePolicy( null );
        manager = new DefaultRemoteRepositoryManager();
        manager.withUpdatePolicyAnalyzer( new StubUpdatePolicyAnalyzer() );
        manager.withChecksumPolicyProvider( new StubChecksumPolicyProvider() );
        manager.withLoggerFactory( new TestLoggerFactory() );
    }

    @After
    public void teardown()
        throws Exception
    {
        manager = null;
        session = null;
    }

    private RemoteRepository.Builder newRepo( String id, String url, boolean enabled, String updates, String checksums )
    {
        RepositoryPolicy policy = new RepositoryPolicy( enabled, updates, checksums );
        return new RemoteRepository.Builder( id, "test", url ).setPolicy( policy );
    }

    private void assertEqual( RemoteRepository expected, RemoteRepository actual )
    {
        assertEquals( "id", expected.getId(), actual.getId() );
        assertEquals( "url", expected.getUrl(), actual.getUrl() );
        assertEquals( "type", expected.getContentType(), actual.getContentType() );
        assertEqual( expected.getPolicy( false ), actual.getPolicy( false ) );
        assertEqual( expected.getPolicy( true ), actual.getPolicy( true ) );
    }

    private void assertEqual( RepositoryPolicy expected, RepositoryPolicy actual )
    {
        assertEquals( "enabled", expected.isEnabled(), actual.isEnabled() );
        assertEquals( "checksums", expected.getChecksumPolicy(), actual.getChecksumPolicy() );
        assertEquals( "updates", expected.getUpdatePolicy(), actual.getUpdatePolicy() );
    }

    @Test
    public void testGetPolicy()
    {
        RepositoryPolicy snapshotPolicy =
            new RepositoryPolicy( true, RepositoryPolicy.UPDATE_POLICY_ALWAYS, RepositoryPolicy.CHECKSUM_POLICY_IGNORE );
        RepositoryPolicy releasePolicy =
            new RepositoryPolicy( true, RepositoryPolicy.UPDATE_POLICY_NEVER, RepositoryPolicy.CHECKSUM_POLICY_FAIL );

        RemoteRepository repo = new RemoteRepository.Builder( "id", "type", "http://localhost" ) //
        .setSnapshotPolicy( snapshotPolicy ).setReleasePolicy( releasePolicy ).build();

        RepositoryPolicy effectivePolicy = manager.getPolicy( session, repo, true, true );
        assertEquals( true, effectivePolicy.isEnabled() );
        // FIXME - using a stub now
        assertEquals( RepositoryPolicy.CHECKSUM_POLICY_IGNORE, effectivePolicy.getChecksumPolicy() );
        assertEquals( RepositoryPolicy.UPDATE_POLICY_ALWAYS, effectivePolicy.getUpdatePolicy() );
    }

    @Test
    public void testAggregateSimpleRepos()
    {
        RemoteRepository dominant1 = newRepo( "a", "file://", false, "", "" ).build();

        RemoteRepository recessive1 = newRepo( "a", "http://", true, "", "" ).build();
        RemoteRepository recessive2 = newRepo( "b", "file://", true, "", "" ).build();

        List<RemoteRepository> result =
            manager.aggregateRepositories( session, Arrays.asList( dominant1 ),
                                           Arrays.asList( recessive1, recessive2 ), false );

        assertEquals( 2, result.size() );
        assertEqual( dominant1, result.get( 0 ) );
        assertEqual( recessive2, result.get( 1 ) );
    }

    @Test
    public void testAggregateSimpleRepos_MustKeepDisabledRecessiveRepo()
    {
        RemoteRepository dominant = newRepo( "a", "file://", true, "", "" ).build();

        RemoteRepository recessive1 = newRepo( "b", "http://", false, "", "" ).build();

        List<RemoteRepository> result =
            manager.aggregateRepositories( session, Arrays.asList( dominant ), Arrays.asList( recessive1 ), false );

        RemoteRepository recessive2 = newRepo( recessive1.getId(), "http://", true, "", "" ).build();

        result = manager.aggregateRepositories( session, result, Arrays.asList( recessive2 ), false );

        assertEquals( 2, result.size() );
        assertEqual( dominant, result.get( 0 ) );
        assertEqual( recessive1, result.get( 1 ) );
    }

    @Test
    public void testAggregateMirrorRepos_DominantMirrorComplete()
    {
        RemoteRepository dominant1 = newRepo( "a", "http://", false, "", "" ).build();
        RemoteRepository dominantMirror1 =
            newRepo( "x", "file://", false, "", "" ).addMirroredRepository( dominant1 ).build();

        RemoteRepository recessive1 = newRepo( "a", "https://", true, "", "" ).build();
        RemoteRepository recessiveMirror1 =
            newRepo( "x", "http://", true, "", "" ).addMirroredRepository( recessive1 ).build();

        List<RemoteRepository> result =
            manager.aggregateRepositories( session, Arrays.asList( dominantMirror1 ),
                                           Arrays.asList( recessiveMirror1 ), false );

        assertEquals( 1, result.size() );
        assertEqual( dominantMirror1, result.get( 0 ) );
        assertEquals( 1, result.get( 0 ).getMirroredRepositories().size() );
        assertEquals( dominant1, result.get( 0 ).getMirroredRepositories().get( 0 ) );
    }

    @Test
    public void testAggregateMirrorRepos_DominantMirrorIncomplete()
    {
        RemoteRepository dominant1 = newRepo( "a", "http://", false, "", "" ).build();
        RemoteRepository dominantMirror1 =
            newRepo( "x", "file://", false, "", "" ).addMirroredRepository( dominant1 ).build();

        RemoteRepository recessive1 = newRepo( "a", "https://", true, "", "" ).build();
        RemoteRepository recessive2 = newRepo( "b", "https://", true, "", "" ).build();
        RemoteRepository recessiveMirror1 =
            newRepo( "x", "http://", true, "", "" ).setMirroredRepositories( Arrays.asList( recessive1, recessive2 ) ).build();

        List<RemoteRepository> result =
            manager.aggregateRepositories( session, Arrays.asList( dominantMirror1 ),
                                           Arrays.asList( recessiveMirror1 ), false );

        assertEquals( 1, result.size() );
        assertEqual( newRepo( "x", "file://", true, "", "" ).build(), result.get( 0 ) );
        assertEquals( 2, result.get( 0 ).getMirroredRepositories().size() );
        assertEquals( dominant1, result.get( 0 ).getMirroredRepositories().get( 0 ) );
        assertEquals( recessive2, result.get( 0 ).getMirroredRepositories().get( 1 ) );
    }

    @Test
    public void testMirrorAuthentication()
    {
        final RemoteRepository repo = newRepo( "a", "http://", true, "", "" ).build();
        final RemoteRepository mirror =
            newRepo( "a", "http://", true, "", "" ).setAuthentication( new AuthenticationBuilder().addUsername( "test" ).build() ).build();
        session.setMirrorSelector( new MirrorSelector()
        {
            public RemoteRepository getMirror( RemoteRepository repository )
            {
                return mirror;
            }
        } );

        List<RemoteRepository> result =
            manager.aggregateRepositories( session, Collections.<RemoteRepository> emptyList(), Arrays.asList( repo ),
                                           true );

        assertEquals( 1, result.size() );
        assertSame( mirror.getAuthentication(), result.get( 0 ).getAuthentication() );
    }

    @Test
    public void testMirrorProxy()
    {
        final RemoteRepository repo = newRepo( "a", "http://", true, "", "" ).build();
        final RemoteRepository mirror =
            newRepo( "a", "http://", true, "", "" ).setProxy( new Proxy( "http", "host", 2011, null ) ).build();
        session.setMirrorSelector( new MirrorSelector()
        {
            public RemoteRepository getMirror( RemoteRepository repository )
            {
                return mirror;
            }
        } );

        List<RemoteRepository> result =
            manager.aggregateRepositories( session, Collections.<RemoteRepository> emptyList(), Arrays.asList( repo ),
                                           true );

        assertEquals( 1, result.size() );
        assertEquals( "http", result.get( 0 ).getProxy().getType() );
        assertEquals( "host", result.get( 0 ).getProxy().getHost() );
        assertEquals( 2011, result.get( 0 ).getProxy().getPort() );
    }

    @Test
    public void testProxySelector()
    {
        final RemoteRepository repo = newRepo( "a", "http://", true, "", "" ).build();
        final Proxy proxy = new Proxy( "http", "host", 2011, null );
        session.setProxySelector( new ProxySelector()
        {
            public Proxy getProxy( RemoteRepository repository )
            {
                return proxy;
            }
        } );
        session.setMirrorSelector( new MirrorSelector()
        {
            public RemoteRepository getMirror( RemoteRepository repository )
            {
                return null;
            }
        } );

        List<RemoteRepository> result =
            manager.aggregateRepositories( session, Collections.<RemoteRepository> emptyList(), Arrays.asList( repo ),
                                           true );

        assertEquals( 1, result.size() );
        assertEquals( "http", result.get( 0 ).getProxy().getType() );
        assertEquals( "host", result.get( 0 ).getProxy().getHost() );
        assertEquals( 2011, result.get( 0 ).getProxy().getPort() );
    }

    private static class StubUpdatePolicyAnalyzer
        implements UpdatePolicyAnalyzer
    {

        public String getEffectiveUpdatePolicy( RepositorySystemSession session, String policy1, String policy2 )
        {
            return ordinalOfUpdatePolicy( policy1 ) < ordinalOfUpdatePolicy( policy2 ) ? policy1 : policy2;
        }

        private int ordinalOfUpdatePolicy( String policy )
        {
            if ( RepositoryPolicy.UPDATE_POLICY_DAILY.equals( policy ) )
            {
                return 1440;
            }
            else if ( RepositoryPolicy.UPDATE_POLICY_ALWAYS.equals( policy ) )
            {
                return 0;
            }
            else if ( policy != null && policy.startsWith( RepositoryPolicy.UPDATE_POLICY_INTERVAL ) )
            {
                String s = policy.substring( RepositoryPolicy.UPDATE_POLICY_INTERVAL.length() + 1 );
                return Integer.valueOf( s );
            }
            else
            {
                // assume "never"
                return Integer.MAX_VALUE;
            }
        }

        public boolean isUpdatedRequired( RepositorySystemSession session, long lastModified, String policy )
        {
            return false;
        }

    }

}
