package org.eclipse.aether.internal.impl;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.checksum.ChecksumPolicy;
import org.eclipse.aether.spi.connector.checksum.ChecksumPolicyProvider;
import org.eclipse.aether.transfer.TransferResource;


public class StubChecksumPolicyProvider implements ChecksumPolicyProvider {

    public ChecksumPolicy newChecksumPolicy(RepositorySystemSession session, RemoteRepository repository, TransferResource resource, String policy) {
        // FIXME Auto-generated method stub
        return null;
    }

    public String getEffectiveChecksumPolicy(RepositorySystemSession session, String policy1, String policy2) {
        // FIXME Auto-generated method stub
        return null;
    }

}
