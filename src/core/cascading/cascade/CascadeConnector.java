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

package cascading.cascade;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import cascading.flow.Flow;
import cascading.tap.CompositeTap;
import cascading.tap.Tap;
import cascading.tuple.TupleEntryCollector;
import cascading.tuple.TupleEntryIterator;
import cascading.util.Util;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.apache.log4j.Logger;
import org.jgrapht.Graphs;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

/**
 * Class CascadeConnector is used to construct a new {@link Cascade} instance from a collection of {@link Flow} instance.
 * <p/>
 * Note order is not significant when adding passing Flow instances to the {@link #connect(String, cascading.flow.Flow[])}
 * method. This connector will order them based on their dependencies, if any.
 */
public class CascadeConnector
  {
  /** Field LOG */
  private static final Logger LOG = Logger.getLogger( CascadeConnector.class );

  /** Field properties */
  private Map<Object, Object> properties;

  /** Constructor CascadeConnector creates a new CascadeConnector instance. */
  public CascadeConnector()
    {
    }

  /**
   * Constructor CascadeConnector creates a new CascadeConnector instance.
   *
   * @param properties of type Map<Object, Object>
   */
  public CascadeConnector( Map<Object, Object> properties )
    {
    this.properties = properties;
    }

  /**
   * Given any number of {@link Flow} objects, it will connect them and return a new {@link Cascade} instance. The name
   * of the Cascade is derived from the given Flow instances.
   *
   * @param flows of type Flow
   * @return Cascade
   */
  public Cascade connect( Flow... flows )
    {
    return connect( null, flows );
    }

  /**
   * Given any number of {@link Flow} objects, it will connect them and return a new {@link Cascade} instance.
   *
   * @param name  of type String
   * @param flows of type Flow
   * @return Cascade
   */
  public Cascade connect( String name, Flow... flows )
    {
    verifyUniqueFlowNames( flows );
    name = name == null ? makeName( flows ) : name;

    SimpleDirectedGraph<Tap, Flow.FlowHolder> tapGraph = new SimpleDirectedGraph<Tap, Flow.FlowHolder>( Flow.FlowHolder.class );
    SimpleDirectedGraph<Flow, Integer> flowGraph = new SimpleDirectedGraph<Flow, Integer>( Integer.class );

    makeTapGraph( tapGraph, flows );
    makeFlowGraph( flowGraph, tapGraph );


    return new Cascade( name, flowGraph );
    }

  private void verifyUniqueFlowNames( Flow[] flows )
    {
    Set<String> set = new HashSet<String>();

    for( Flow flow : flows )
      {
      if( set.contains( flow.getName() ) )
        throw new CascadeException( "all flow names must be unique, found duplicate: " + flow.getName() );

      set.add( flow.getName() );
      }
    }

  private String makeName( Flow[] flows )
    {
    String[] names = new String[flows.length];

    for( int i = 0; i < flows.length; i++ )
      names[ i ] = flows[ i ].getName();

    return Util.join( names, "+" );
    }

  private void makeTapGraph( SimpleDirectedGraph<Tap, Flow.FlowHolder> tapGraph, Flow[] flows )
    {
    for( Flow flow : flows )
      {
      LinkedList<Tap> sources = new LinkedList<Tap>( flow.getSources().values() );
      Collection<Tap> sinks = flow.getSinks().values();

      // account for MultiTap sources
      ListIterator<Tap> iterator = sources.listIterator();
      while( iterator.hasNext() )
        {
        Tap source = iterator.next();

        if( source instanceof CompositeTap )
          {
          iterator.remove();

          for( Tap tap : ( (CompositeTap) source ).getChildTaps() )
            iterator.add( tap );
          }
        }

      for( Tap source : sources )
        tapGraph.addVertex( source );

      for( Tap sink : sinks )
        tapGraph.addVertex( sink );

      for( Tap source : sources )
        {
        for( Tap sink : sinks )
          tapGraph.addEdge( source, sink, flow.getHolder() );
        }
      }
    }

  private void makeFlowGraph( SimpleDirectedGraph<Flow, Integer> jobGraph, SimpleDirectedGraph<Tap, Flow.FlowHolder> tapGraph )
    {
    TopologicalOrderIterator<Tap, Flow.FlowHolder> topoIterator = new TopologicalOrderIterator<Tap, Flow.FlowHolder>( tapGraph );
    int count = 0;

    while( topoIterator.hasNext() )
      {
      Tap source = topoIterator.next();

      if( LOG.isDebugEnabled() )
        LOG.debug( "handling flow source: " + source );

      List<Tap> sinks = Graphs.successorListOf( tapGraph, (Tap) source );

      for( Tap sink : sinks )
        {
        if( LOG.isDebugEnabled() )
          LOG.debug( "handling flow path: " + source + " -> " + sink );

        Flow flow = tapGraph.getEdge( source, sink ).flow;

        jobGraph.addVertex( flow );

        Set<Flow.FlowHolder> previous = tapGraph.incomingEdgesOf( source );

        for( Flow.FlowHolder previousFlow : previous )
          jobGraph.addEdge( previousFlow.flow, flow, count++ );
        }
      }
    }

  /** Specialized type of {@link Tap} that is the root. */
  static class RootTap extends Tap
    {
    /** Field serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** @see Tap#getPath() */
    public Path getPath()
      {
      return null;
      }

    /** @see Tap#makeDirs(JobConf) */
    public boolean makeDirs( JobConf conf ) throws IOException
      {
      return false;
      }

    /** @see Tap#deletePath(JobConf) */
    public boolean deletePath( JobConf conf ) throws IOException
      {
      return false;
      }

    /** @see Tap#pathExists(JobConf) */
    public boolean pathExists( JobConf conf ) throws IOException
      {
      return false;
      }

    /** @see Tap#getPathModified(JobConf) */
    public long getPathModified( JobConf conf ) throws IOException
      {
      return 0;
      }

    public TupleEntryIterator openForRead( JobConf conf ) throws IOException
      {
      return null;
      }

    public TupleEntryCollector openForWrite( JobConf conf ) throws IOException
      {
      return null;
      }
    }

  }
