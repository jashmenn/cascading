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

package cascading.operation;

import cascading.flow.FlowProcess;
import cascading.flow.Scope;
import cascading.pipe.Each;
import cascading.pipe.Every;
import cascading.pipe.Pipe;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.util.Util;

import java.io.Serializable;

/**
 * Class BaseOperation is the base class of predicate types that are applied to {@link Tuple} streams via
 * the {@link Each} or {@link Every} {@link Pipe}.
 * </p>
 * Specific examples of Operations are {@link Function}, {@link Filter}, {@link Aggregator}, {@link Buffer},
 * and {@link Assertion}.
 */
public abstract class BaseOperation<C> implements Serializable, Operation<C>
  {

  /** Field fieldDeclaration */
  protected Fields fieldDeclaration;
  /** Field numArgs */
  protected int numArgs = ANY;
  /** Field trace */
  protected String trace = Util.captureDebugTrace( getClass() );

  // initialize this operation based on its subclass
  {
  if( this instanceof Filter || this instanceof Assertion )
    fieldDeclaration = Fields.ALL;
  else
    fieldDeclaration = Fields.UNKNOWN;
  }

  /**
   * Constructs a new instance that retuns an {@link Fields#UNKNOWN} {@link Tuple} and accepts any number of arguments.
   * </p>
   * It is a best practice to always declare the field names and number of arguments via one of the other constructors.
   */
  protected BaseOperation()
    {
    }

  /**
   * Constructs a new instance that returns the fields declared in fieldDeclaration and accepts any number of arguments.
   *
   * @param fieldDeclaration of type Fields
   */
  protected BaseOperation( Fields fieldDeclaration )
    {
    this.fieldDeclaration = fieldDeclaration;
    verify();
    }

  /**
   * Constructs a new instance that returns an unknown field set and accepts the given numArgs number of arguments.
   *
   * @param numArgs of type numArgs
   */
  protected BaseOperation( int numArgs )
    {
    this.numArgs = numArgs;
    verify();
    }

  /**
   * Constructs a new instance that returns the fields declared in fieldDeclaration and accepts numArgs number of arguments.
   *
   * @param numArgs          of type numArgs
   * @param fieldDeclaration of type Fields
   */
  protected BaseOperation( int numArgs, Fields fieldDeclaration )
    {
    this.numArgs = numArgs;
    this.fieldDeclaration = fieldDeclaration;
    verify();
    }

  /** Validates the state of this instance. */
  private final void verify()
    {
    if( this instanceof Filter && fieldDeclaration != Fields.ALL )
      throw new IllegalArgumentException( "fieldDeclaration must be set to Fields.ALL for filter operations" );

    if( this instanceof Assertion && fieldDeclaration != Fields.ALL )
      throw new IllegalArgumentException( "fieldDeclaration must be set to Fields.ALL for assertion operations" );

    if( fieldDeclaration == null )
      throw new IllegalArgumentException( "fieldDeclaration may not be null" );

    if( numArgs < 0 )
      throw new IllegalArgumentException( "numArgs may not be negative" );
    }

  /** Method prepare does nothing, and may safely be overridden. */
  public void prepare( FlowProcess flowProcess, OperationCall<C> operationCall )
    {
    // do nothing
    }

  /** Method cleanup does nothing, and may safely be overridden. */
  public void cleanup( FlowProcess flowProcess, OperationCall<C> operationCall )
    {
    // do nothing
    }

  /** @see Operation#getFieldDeclaration() */
  public Fields getFieldDeclaration()
    {
    return fieldDeclaration;
    }

  /** @see Operation#getNumArgs() */
  public int getNumArgs()
    {
    return numArgs;
    }

  /** @see Operation#isSafe() */
  public boolean isSafe()
    {
    return true;
    }

  /**
   * Method getTrace returns the trace of this BaseOperation object.
   *
   * @return the trace (type String) of this BaseOperation object.
   */
  public String getTrace()
    {
    return trace;
    }

  @Override
  public String toString()
    {
    return toStringInternal( (Operation) this );
    }

  public static String toStringInternal( Operation operation )
    {
    StringBuilder buffer = new StringBuilder();

    Class<? extends Operation> type = operation.getClass();
    if( type.getSimpleName().length() != 0 )
      buffer.append( type.getSimpleName() );
    else
      buffer.append( type.getName() ); // should get something for an anonymous inner class

    if( operation.getFieldDeclaration() != null )
      buffer.append( "[decl:" ).append( operation.getFieldDeclaration() ).append( "]" );

    if( operation.getNumArgs() != ANY )
      buffer.append( "[args:" ).append( operation.getNumArgs() ).append( "]" );

    return buffer.toString();
    }

  public static void printOperationInternal( Operation operation, StringBuffer buffer, Scope scope )
    {
    Class<? extends Operation> type = operation.getClass();

    if( type.getSimpleName().length() != 0 )
      buffer.append( type.getSimpleName() );
    else
      buffer.append( type.getName() ); // should get something for an anonymous inner class

    if( scope.getDeclaredFields() != null )
      buffer.append( "[decl:" ).append( scope.getDeclaredFields() ).append( "]" );

    if( operation.getNumArgs() != ANY )
      buffer.append( "[args:" ).append( operation.getNumArgs() ).append( "]" );
    }

  }