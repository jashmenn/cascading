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

package cascading.pipe.assembly;

import cascading.operation.Identity;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.pipe.SubAssembly;
import cascading.tuple.Fields;

/**
 * Class Rename is a {@link SubAssembly} that will rename the fromFields to the names in toFields.
 * <p/>
 * Note that if any input field names are not given, they will retain their names.
 */
public class Rename extends SubAssembly
  {
  /**
   * Rename the fromFields in the current Tuple to the given toFields.
   * <p/>
   * <pre>
   * incoming: {"first", "middle", "last"} -> from:{"middle"} to:{"initial"} -> outgoing:{"first", "last", "initial"}
   * </pre>
   *
   * @param previous   of type Pipe
   * @param fromFields of type Fields
   * @param toFields   of type Fields
   */
  public Rename( Pipe previous, Fields fromFields, Fields toFields )
    {
    if( fromFields.isDefined() && fromFields.size() != toFields.size() )
      throw new IllegalArgumentException( "fields arguments must be same size, from: " + fromFields.printVerbose() + " to: " + toFields.printVerbose() );

    if( !toFields.isDefined() )
      throw new IllegalArgumentException( "toFields must define field names: " + toFields.printVerbose() );

    setTails( new Each( previous, fromFields, new Identity( toFields ), Fields.SWAP ) );
    }
  }