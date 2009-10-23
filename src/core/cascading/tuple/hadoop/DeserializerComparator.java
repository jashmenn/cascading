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

package cascading.tuple.hadoop;

import cascading.CascadingException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.InputBuffer;
import org.apache.hadoop.io.RawComparator;
import org.apache.hadoop.io.serializer.Deserializer;

import java.io.IOException;

/** Class DeserializerComparator is the base class for all Cascading comparator classes. */
public abstract class DeserializerComparator<T> extends Configured implements RawComparator<T>
  {
  InputBuffer buffer = new InputBuffer();
  Deserializer<T> deserializer;

  private T key1;
  private T key2;

  @Override
  public void setConf( Configuration conf )
    {
    super.setConf( conf );

    TupleSerialization tupleSerialization = new TupleSerialization( conf );

    try
      {
      setDeserializer( tupleSerialization );
      }
    catch( IOException exception )
      {
      throw new CascadingException( "unable to create deserializer", exception );
      }
    }

  abstract void setDeserializer( TupleSerialization tupleSerialization ) throws IOException;

  void setDeserializer( Deserializer<T> deserializer ) throws IOException
    {
    this.deserializer = deserializer;
    this.deserializer.open( buffer );
    }

  public int compare( byte[] b1, int s1, int l1, byte[] b2, int s2, int l2 )
    {
    try
      {

      buffer.reset( b1, s1, l1 );
      key1 = deserializer.deserialize( key1 );

      buffer.reset( b2, s2, l2 );
      key2 = deserializer.deserialize( key2 );

      }
    catch( IOException e )
      {
      throw new CascadingException( e );
      }

    try
      {
      return compare( key1, key2 );
      }
    catch( ClassCastException exception )
      {
      throw new CascadingException( "unable to compare Tuples, likely a CoGroup is being attempted on fields of " +
        "different types or custom comparators are incorrectly set on Fields", exception );
      }
    }
  }