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
package org.eclipse.aether.util;

import static org.junit.Assert.*;

import org.eclipse.aether.util.PrioritizedComponent;
import org.junit.Test;

public class PrioritizedComponentTest
{

    @Test
    public void testIsDisabled()
    {
        assertTrue( new PrioritizedComponent<String>( "", String.class, Float.NaN, 0 ).isDisabled() );
        assertFalse( new PrioritizedComponent<String>( "", String.class, 0, 0 ).isDisabled() );
        assertFalse( new PrioritizedComponent<String>( "", String.class, 1, 0 ).isDisabled() );
        assertFalse( new PrioritizedComponent<String>( "", String.class, -1, 0 ).isDisabled() );
    }

    @Test
    public void testCompareTo()
    {
        assertCompare( 0, Float.NaN, Float.NaN );
        assertCompare( 0, 0, 0 );

        assertCompare( 1, 0, 1 );
        assertCompare( 1, 2, Float.POSITIVE_INFINITY );
        assertCompare( 1, Float.NEGATIVE_INFINITY, -3 );

        assertCompare( 1, Float.NaN, 0 );
        assertCompare( 1, Float.NaN, -1 );
        assertCompare( 1, Float.NaN, Float.NEGATIVE_INFINITY );
        assertCompare( 1, Float.NaN, Float.POSITIVE_INFINITY );

        assertCompare( -1, Float.NaN, 0, 1 );
        assertCompare( -1, 10, 0, 1 );
    }

    private void assertCompare( int expected, float priority1, float priority2 )
    {
        PrioritizedComponent<?> one = new PrioritizedComponent<String>( "", String.class, priority1, 0 );
        PrioritizedComponent<?> two = new PrioritizedComponent<String>( "", String.class, priority2, 0 );
        assertEquals( expected, one.compareTo( two ) );
        assertEquals( -expected, two.compareTo( one ) );
    }

    private void assertCompare( int expected, float priority, int index1, int index2 )
    {
        PrioritizedComponent<?> one = new PrioritizedComponent<String>( "", String.class, priority, index1 );
        PrioritizedComponent<?> two = new PrioritizedComponent<String>( "", String.class, priority, index2 );
        assertEquals( expected, one.compareTo( two ) );
        assertEquals( -expected, two.compareTo( one ) );
    }

}
