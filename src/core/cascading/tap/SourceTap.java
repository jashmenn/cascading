/*
 * Copyright (c) 2007-2009 Concurrent, Inc. All Rights Reserved.
 *
 * Project and contact information: http://www.cascading.org/
 *
 * This file is part of the Cascading project.
 *
 * Cascading is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Cascading is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cascading.  If not, see <http://www.gnu.org/licenses/>.
 */

package cascading.tap;

import cascading.scheme.Scheme;
import cascading.tuple.Fields;
import cascading.tuple.TupleEntry;
import cascading.tuple.TupleEntryCollector;
import cascading.tuple.TupleEntryIterator;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.OutputCollector;

import java.io.IOException;

/**
 * Class SourceTap is the base class for {@link MultiSourceTap}. Some {@link Tap} instances may only be sources (as opposed
 * to being a sink). These types should subclass SourceTap for convenience.
 */
public abstract class SourceTap extends Tap
  {
  protected SourceTap()
    {
    }

  protected SourceTap( Scheme scheme )
    {
    super( scheme );
    }

  @Override
  public Fields getSinkFields()
    {
    throw new UnsupportedOperationException( "unable to sink tuple streams via a SourceTap instance" );
    }

  @Override
  public void sink( TupleEntry tupleEntry, OutputCollector outputCollector ) throws IOException
    {
    throw new UnsupportedOperationException( "unable to sink tuple streams via a SourceTap instance" );
    }

  @Override
  public final boolean isSink()
    {
    return false;
    }

  /** @see Tap#deletePath(JobConf) */
  public boolean deletePath( JobConf conf ) throws IOException
    {
    throw new UnsupportedOperationException( "unable to delete files via a SourceTap instance" );
    }

  /** @see Tap#makeDirs(JobConf) */
  public boolean makeDirs( JobConf conf ) throws IOException
    {
    throw new UnsupportedOperationException( "unable to make dirs via a SourceTap instance" );
    }

  public TupleEntryIterator openForRead( JobConf conf ) throws IOException
    {
    throw new UnsupportedOperationException( "unable to open for read via a SourceTap instance" );
    }

  public TupleEntryCollector openForWrite( JobConf conf ) throws IOException
    {
    throw new UnsupportedOperationException( "unable to open for write via a SourceTap instance" );
    }
  }
