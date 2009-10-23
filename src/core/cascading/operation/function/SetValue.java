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

package cascading.operation.function;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Filter;
import cascading.operation.FilterCall;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.operation.OperationCall;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;

import java.io.Serializable;

/**
 * Class SetValue is a utility {@link Function} that allows for a Tuple value to be returned based on the outcome
 * of a given {@link Filter} operation.
 * <p/>
 * There are only two possible values, either {@link Filter#isRemove(cascading.flow.FlowProcess, cascading.operation.FilterCall)}
 * returns {@code true} or {@code false}.
 * <p/>
 * If {@code false} is returned, most commonly the {@link Filter} passed and the Tuple should be kept. SetValue will then return
 * the first value in the given values array, by default {@code true}. If the Filter returns {@code true}, the second
 * value in the values array will be returned, by default {@code false}.
 * <p/>
 */
public class SetValue extends BaseOperation implements Function
  {
  /** Field filter */
  private final Filter filter;
  /** Field values */
  private Comparable<? extends Serializable>[] values = new Comparable[]{true, false};

  /**
   * Constructor SetValue creates a new SetValue instance.
   *
   * @param fieldDeclaration of type Fields
   * @param filter           of type Filter
   */
  public SetValue( Fields fieldDeclaration, Filter filter )
    {
    super( fieldDeclaration );
    this.filter = filter;

    verify();
    }

  /**
   * Constructor SetValue creates a new SetValue instance.
   *
   * @param fieldDeclaration of type Fields
   * @param filter           of type Filter
   * @param values           of type Comparable<? extends Serializable>...
   */
  public SetValue( Fields fieldDeclaration, Filter filter, Comparable<? extends Serializable>... values )
    {
    super( fieldDeclaration );
    this.filter = filter;
    this.values = values;

    verify();
    }

  private void verify()
    {
    if( fieldDeclaration.size() != 1 )
      throw new IllegalArgumentException( "fieldDeclaration may only declare one field, was " + fieldDeclaration.print() );

    if( filter == null )
      throw new IllegalArgumentException( "filter may not be null" );

    if( values == null || values.length != 2 )
      throw new IllegalArgumentException( "values argument must contain two values" );
    }

  @Override
  public void prepare( FlowProcess flowProcess, OperationCall operationCall )
    {
    filter.prepare( flowProcess, operationCall );
    }

  @Override
  public void cleanup( FlowProcess flowProcess, OperationCall operationCall )
    {
    filter.cleanup( flowProcess, operationCall );
    }

  @Override
  public void operate( FlowProcess flowProcess, FunctionCall functionCall )
    {
    boolean isRemove = !filter.isRemove( flowProcess, (FilterCall) functionCall );

    int pos = isRemove ? 0 : 1;

    functionCall.getOutputCollector().add( new Tuple( values[ pos ] ) );
    }
  }